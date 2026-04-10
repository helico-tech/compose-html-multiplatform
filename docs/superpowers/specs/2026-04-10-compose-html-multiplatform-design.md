# Compose HTML Multiplatform — Design Spec

## Overview

A fork of JetBrains Compose HTML that becomes Kotlin Multiplatform (JVM + JS), keeping the existing JS DOM rendering path intact while adding a common text renderer for one-shot static HTML/SVG generation.

**Targets:** JVM + JS
**Rendering mode:** One-shot static (no recomposition, no event listeners, no state management)
**Output:** HTML and SVG strings via `Appendable` streaming or `String` convenience

## Architecture

The system has seven layers:

1. `HtmlNode` tree — lightweight common node model
2. `TextApplier` — Compose `Applier` that builds the `HtmlNode` tree
3. Common composables — `Div`, `Span`, `A`, `Text`, etc.
4. SVG composables — ~35 SVG elements ported to common
5. `TreeSerializer` — walks the tree and writes to `Appendable`
6. Entry points — `renderComposableToString`, `renderComposableTo`, `renderComposable`
7. JS `DomApplier` — existing interactive DOM rendering, untouched

## Module Structure

```
compose-html-multiplatform/
├── core/
│   └── src/
│       ├── commonMain/       # Composables, HtmlNode tree, TextApplier, attribute scopes
│       ├── jsMain/           # DomApplier, DomNodeWrapper, renderComposable(root)
│       └── jvmMain/          # (empty — all text rendering logic is in commonMain)
├── svg/
│   └── src/
│       ├── commonMain/       # SVG composables (Circle, Rect, Path, G, etc.)
│       └── jsMain/           # SVG-specific DOM bindings (if any)
├── serializer/
│   └── src/
│       └── commonMain/       # TreeSerializer — walks HtmlNode tree to Appendable
└── build.gradle.kts          # KMP: targets jvm() + js(IR)
```

### Key decisions

- `core/commonMain` holds the composable API, the `HtmlNode` lightweight tree, the `TextApplier`, and the `renderComposableToString` / `renderComposableTo` entry points.
- `core/jsMain` holds the existing `DomApplier`, `DomNodeWrapper`, `DomElementWrapper`, `GlobalSnapshotManager`, and the interactive `renderComposable(root)` entry point.
- `svg/commonMain` holds all ~35 SVG composables, lifted from the current JS-only `html/svg` module.
- `serializer` is a separate module so the tree-to-text logic is cleanly isolated.
- JS gets both paths: interactive DOM rendering and static text rendering.
- JVM gets text rendering only.

## HtmlNode Tree

The common node type that the `TextApplier` builds during composition. Lives in `core/commonMain`.

```kotlin
sealed class HtmlNode {
    val children: MutableList<HtmlNode> = mutableListOf()
}

class HtmlElementNode(
    val tagName: String,
    val namespace: String? = null,           // null = HTML, "http://www.w3.org/2000/svg" for SVG
    val attributes: MutableMap<String, String> = mutableMapOf(),
    val inlineStyles: MutableMap<String, String> = mutableMapOf(),
    val classes: MutableList<String> = mutableListOf(),
) : HtmlNode()

class HtmlTextNode(
    var text: String,
) : HtmlNode()
```

### Design rationale

- `sealed class` with only two node types: elements and text. No comments or processing instructions needed for static output.
- `namespace` distinguishes HTML from SVG elements. SVG composables set `namespace = SVG_NS`. The serializer uses this to emit `xmlns` on the root `<svg>` element.
- `attributes` is a flat `String -> String` map. The composable's `AttrsScope` resolves typed attributes into this map at composition time.
- `inlineStyles` is separate from `attributes` so the serializer can emit them as a single `style="..."` attribute.
- `classes` is a list so the serializer can join them into `class="..."`.
- No event listeners, no property updaters — those are JS-only concerns.

## TextApplier

The Compose `Applier` that builds the `HtmlNode` tree during one-shot composition. Lives in `core/commonMain`.

```kotlin
class TextApplier(root: HtmlElementNode) : AbstractApplier<HtmlNode>(root) {
    override fun insertTopDown(index: Int, instance: HtmlNode) {
        // no-op — we use bottom-up like the DomApplier
    }

    override fun insertBottomUp(index: Int, instance: HtmlNode) {
        (current as HtmlElementNode).children.add(index, instance)
    }

    override fun remove(index: Int, count: Int) {
        (current as HtmlElementNode).children.remove(index, count)
    }

    override fun move(from: Int, to: Int, count: Int) {
        (current as HtmlElementNode).children.move(from, to, count)
    }

    override fun onClear() {
        (current as HtmlElementNode).children.clear()
    }
}
```

Mirrors the `DomApplier`'s bottom-up insertion strategy. Since it's one-shot, `remove`/`move`/`onClear` will rarely be called, but are implemented for correctness.

## Common Attribute Scopes

The current `AttrsScope` is JS-only because it writes to `DomElementWrapper`. A common version writes to the `HtmlNode`'s attribute/style/class maps:

```kotlin
// commonMain
class CommonAttrsScope<E>(val node: HtmlElementNode) {
    fun attr(name: String, value: String) {
        node.attributes[name] = value
    }

    fun classes(vararg classNames: String) {
        node.classes.addAll(classNames)
    }

    fun style(builder: CommonStyleScope.() -> Unit) {
        CommonStyleScope(node.inlineStyles).builder()
    }

    fun id(value: String) = attr("id", value)
}

class CommonStyleScope(val styles: MutableMap<String, String>) {
    fun property(name: String, value: String) {
        styles[name] = value
    }

    fun width(value: String) = property("width", value)
    fun height(value: String) = property("height", value)
}
```

On JS, the existing `AttrsScope` delegates to `DomElementWrapper` as today. The composables use `expect`/`actual` to resolve which scope type they interact with:

```kotlin
// commonMain
expect class PlatformAttrsScope<E> {
    fun attr(name: String, value: String)
    fun classes(vararg classNames: String)
    fun style(builder: PlatformStyleScope.() -> Unit)
}

// jsMain
actual typealias PlatformAttrsScope<E> = AttrsScope<E>  // existing impl

// jvmMain
actual typealias PlatformAttrsScope<E> = CommonAttrsScope<E>
```

The JS target gets both — `AttrsScope` for interactive DOM use and `CommonAttrsScope` for the static text rendering path. The entry point determines which applier and scope are active.

## Common Attrs Interface

Both `CommonAttrsScope` (text path) and `AttrsScope` (JS DOM path) implement a common interface so that composables in `commonMain` can accept attrs lambdas without depending on either concrete type:

```kotlin
// commonMain
interface AttrsBuilder<E> {
    fun attr(name: String, value: String)
    fun classes(vararg classNames: String)
    fun style(builder: StyleBuilder.() -> Unit)
    fun id(value: String) = attr("id", value)
}

interface StyleBuilder {
    fun property(name: String, value: String)
}
```

`CommonAttrsScope` implements `AttrsBuilder` (writing to `HtmlNode` maps). On JS, the existing `AttrsScope` is extended to also implement `AttrsBuilder` (delegating to its DOM-based internals). This allows thin wrappers in `commonMain` to use `AttrsBuilder` as their parameter type.

## Common Composables

Composables move from `jsMain` to `commonMain`. `TagElement` is `expect`/`actual` because the underlying node type and applier differ per platform:

```kotlin
// commonMain — expect declaration
@Composable
expect fun TagElement(
    tagName: String,
    namespace: String? = null,
    attrs: (AttrsBuilder<*>.() -> Unit)? = null,
    content: @Composable () -> Unit = {},
)

// Thin wrappers — in commonMain, use the common AttrsBuilder interface
@Composable fun Div(attrs: (AttrsBuilder<*>.() -> Unit)? = null, content: @Composable () -> Unit = {}) =
    TagElement("div", attrs = attrs, content = content)

@Composable fun Span(attrs: (AttrsBuilder<*>.() -> Unit)? = null, content: @Composable () -> Unit = {}) =
    TagElement("span", attrs = attrs, content = content)

@Composable fun A(attrs: (AttrsBuilder<*>.() -> Unit)? = null, content: @Composable () -> Unit = {}) =
    TagElement("a", attrs = attrs, content = content)

// Text node — also expect/actual (different node types per platform)
@Composable
expect fun Text(value: String)
```

### JVM actual

```kotlin
// jvmMain — uses TextApplier + HtmlNode
@Composable
actual fun TagElement(
    tagName: String,
    namespace: String?,
    attrs: (AttrsBuilder<*>.() -> Unit)?,
    content: @Composable () -> Unit,
) {
    ComposeNode<HtmlElementNode, TextApplier>(
        factory = { HtmlElementNode(tagName, namespace) },
        update = {
            set(attrs) { attrsBlock ->
                attributes.clear()
                inlineStyles.clear()
                classes.clear()
                attrsBlock?.let { CommonAttrsScope<Any>(this).it() }
            }
        },
        content = content,
    )
}

@Composable
actual fun Text(value: String) {
    ComposeNode<HtmlTextNode, TextApplier>(
        factory = { HtmlTextNode(value) },
        update = { set(value) { text = it } },
    )
}
```

### JS actual — dual-path dispatch

```kotlin
// jsMain — dispatches based on rendering mode
@Composable
actual fun TagElement(
    tagName: String,
    namespace: String?,
    attrs: (AttrsBuilder<*>.() -> Unit)?,
    content: @Composable () -> Unit,
) {
    if (LocalStaticRendering.current) {
        // HtmlElementNode path (same as JVM actual)
    } else {
        // existing DomElementWrapper path via DomApplier
    }
}
```

A `LocalStaticRendering` composition local boolean tells JS composables which path to take. Set by the entry point: `renderComposable` sets false, `renderComposableToString` sets true.

The thin wrappers (`Div`, `Span`, etc.) remain in `commonMain` and just call `TagElement` via the common `AttrsBuilder` interface.

## SVG Composables

All ~35 existing SVG composables are ported to `svg/commonMain` using `TagElement` with the SVG namespace:

```kotlin
const val SVG_NS = "http://www.w3.org/2000/svg"

@Composable
fun Svg(
    viewBox: String? = null,
    attrs: (AttrsBuilder<*>.() -> Unit)? = null,
    content: @Composable () -> Unit = {},
) {
    TagElement("svg", namespace = SVG_NS, attrs = {
        attr("xmlns", SVG_NS)
        viewBox?.let { attr("viewBox", it) }
        attrs?.invoke(this)
    }, content = content)
}

@Composable
fun Circle(
    cx: Number, cy: Number, r: Number,
    attrs: (AttrsBuilder<*>.() -> Unit)? = null,
) {
    TagElement("circle", namespace = SVG_NS, attrs = {
        attr("cx", cx.toString())
        attr("cy", cy.toString())
        attr("r", r.toString())
        attrs?.invoke(this)
    })
}

@Composable
fun Rect(
    x: Number, y: Number, width: Number, height: Number,
    attrs: (AttrsBuilder<*>.() -> Unit)? = null,
) {
    TagElement("rect", namespace = SVG_NS, attrs = {
        attr("x", x.toString())
        attr("y", y.toString())
        attr("width", width.toString())
        attr("height", height.toString())
        attrs?.invoke(this)
    })
}

@Composable
fun Path(
    d: String,
    attrs: (AttrsBuilder<*>.() -> Unit)? = null,
) {
    TagElement("path", namespace = SVG_NS, attrs = {
        attr("d", d)
        attrs?.invoke(this)
    })
}
```

The same pattern applies to: `Ellipse`, `Line`, `Polygon`, `Polyline`, `G`, `Defs`, `Symbol`, `Use`, `ClipPath`, `Mask`, `LinearGradient`, `RadialGradient`, `Stop`, `Pattern`, `SvgText`, `TextPath`, `Tspan`, `Animate`, `AnimateMotion`, `AnimateTransform`, `Set`, `Mpath`, `Image`, `Filter`, `Marker`, `Title`, `Desc`, `SvgA`, `View`, `Switch`.

### SVG attribute extensions

```kotlin
fun AttrsBuilder<*>.fill(value: String) = attr("fill", value)
fun AttrsBuilder<*>.stroke(value: String) = attr("stroke", value)
fun AttrsBuilder<*>.strokeWidth(value: Number) = attr("stroke-width", value.toString())
fun AttrsBuilder<*>.transform(value: String) = attr("transform", value)
fun AttrsBuilder<*>.opacity(value: Number) = attr("opacity", value.toString())
```

On the JS interactive path, SVG composables go through `TagElement` -> `DomApplier`, which uses `document.createElementNS(SVG_NS, tagName)` to create proper SVG DOM elements.

## Tree Serializer

Lives in `serializer/commonMain`. Walks the `HtmlNode` tree and writes to an `Appendable`. Handles both HTML and SVG uniformly. No dependency on kotlinx.html for SVG tags.

```kotlin
class TreeSerializer(
    private val prettyPrint: Boolean = true,
    private val xhtmlCompatible: Boolean = false,
) {
    fun serialize(root: HtmlNode, out: Appendable) {
        serializeNode(root, out, depth = 0)
    }

    fun serializeToString(root: HtmlNode): String {
        return buildString { serialize(root, this) }
    }

    private fun serializeNode(node: HtmlNode, out: Appendable, depth: Int) {
        when (node) {
            is HtmlTextNode -> out.append(escapeHtml(node.text))
            is HtmlElementNode -> serializeElement(node, out, depth)
        }
    }

    private fun serializeElement(node: HtmlElementNode, out: Appendable, depth: Int) {
        if (prettyPrint) out.indent(depth)

        // Opening tag
        out.append("<").append(node.tagName)

        // Classes -> class attribute
        if (node.classes.isNotEmpty()) {
            out.append(" class=\"").append(escapeAttr(node.classes.joinToString(" "))).append("\"")
        }

        // Inline styles -> style attribute
        if (node.inlineStyles.isNotEmpty()) {
            val styleStr = node.inlineStyles.entries.joinToString("; ") { "${it.key}: ${it.value}" }
            out.append(" style=\"").append(escapeAttr(styleStr)).append("\"")
        }

        // Remaining attributes
        for ((key, value) in node.attributes) {
            out.append(" ").append(key).append("=\"").append(escapeAttr(value)).append("\"")
        }

        // Self-closing for void/empty elements
        if (node.children.isEmpty() && isVoidElement(node)) {
            out.append(if (xhtmlCompatible) " />" else ">")
            if (prettyPrint) out.append("\n")
            return
        }

        out.append(">")

        // Children
        val hasElementChildren = node.children.any { it is HtmlElementNode }
        if (prettyPrint && hasElementChildren) out.append("\n")

        for (child in node.children) {
            serializeNode(child, out, depth + 1)
        }

        if (prettyPrint && hasElementChildren) out.indent(depth)
        out.append("</").append(node.tagName).append(">")
        if (prettyPrint) out.append("\n")
    }
}
```

### Void element handling

HTML void elements (`br`, `hr`, `img`, `input`, etc.) self-close. SVG elements with no children also self-close (e.g. `<circle ... />`). The `isVoidElement` helper checks against both the HTML void element list and SVG leaf elements.

### Escaping

`escapeHtml` handles `<`, `>`, `&` in text content. `escapeAttr` additionally handles `"` in attribute values.

## Entry Points

The public API for rendering. Lives in `core/commonMain` (common entry points) and `core/jsMain` (DOM entry point).

```kotlin
// core/commonMain

fun renderComposableToString(
    prettyPrint: Boolean = true,
    xhtmlCompatible: Boolean = false,
    content: @Composable () -> Unit,
): String {
    val root = HtmlElementNode("root")
    composeOnce(root, content)
    val serializer = TreeSerializer(prettyPrint, xhtmlCompatible)
    return buildString {
        for (child in root.children) {
            serializer.serialize(child, this)
        }
    }
}

fun renderComposableTo(
    out: Appendable,
    prettyPrint: Boolean = true,
    xhtmlCompatible: Boolean = false,
    content: @Composable () -> Unit,
) {
    val root = HtmlElementNode("root")
    composeOnce(root, content)
    val serializer = TreeSerializer(prettyPrint, xhtmlCompatible)
    for (child in root.children) {
        serializer.serialize(child, out)
    }
}

// Internal: runs composition once, no recomposition
internal fun composeOnce(root: HtmlElementNode, content: @Composable () -> Unit) {
    val applier = TextApplier(root)
    val composition = Composition(applier, createRecomposer())
    composition.setContent {
        CompositionLocalProvider(LocalStaticRendering provides true) {
            content()
        }
    }
    composition.dispose()
}
```

```kotlin
// core/jsMain — existing interactive entry point, unchanged

fun renderComposable(root: Element, content: @Composable () -> Unit): Composition {
    // existing DomApplier-based implementation stays as-is
}
```

### Virtual root node

The virtual `root` node is never serialized — only its children are. This way `renderComposableToString { Div { } }` produces `<div></div>`, not `<root><div></div></root>`.

## Usage Examples

```kotlin
// Generate an HTML page
val html = renderComposableToString {
    Div({ classes("container") }) {
        H1 { Text("Hello World") }
        P({ style { property("color", "red") } }) {
            Text("Static HTML from Compose")
        }
    }
}

// Generate an SVG
val svg = renderComposableToString {
    Svg(viewBox = "0 0 100 100") {
        Circle(cx = 50, cy = 50, r = 40, attrs = {
            fill("blue")
            stroke("black")
            strokeWidth(2)
        })
    }
}

// Stream to a file (JVM)
FileWriter("output.html").use { writer ->
    renderComposableTo(writer) {
        Div { Text("Streamed output") }
    }
}
```

## Composition Model: Single-Pass, No Effects

The text renderer runs composition exactly once — no recomposition, no snapshot observation, no effect processing.

**What this means:**
- `LaunchedEffect`, `SideEffect`, `DisposableEffect` are not supported in static rendering. They will be ignored (never executed).
- `mutableStateOf`, `derivedStateOf`, `snapshotFlow` have no effect — there is no recomposer to observe changes.
- Composables must be pure functions of their inputs: parameters in, tree out.

**Why:**
- The Compose snapshot system uses thread-local state (`Snapshot.current`). On a server handling concurrent requests, snapshot state would leak between threads/requests.
- `Recomposer` is tightly coupled to a single coroutine context. Running it on a server adds complexity with no benefit for static output.
- Static HTML/SVG generation has no interactive state — there is nothing to recompose.

**Implementation:** `composeOnce` creates a `Composition`, calls `setContent` once, and immediately disposes. No `Recomposer` loop is started.

```kotlin
internal fun composeOnce(root: HtmlElementNode, content: @Composable () -> Unit) {
    val applier = TextApplier(root)
    val composition = Composition(applier, createRecomposer())
    composition.setContent {
        CompositionLocalProvider(LocalStaticRendering provides true) {
            content()
        }
    }
    composition.dispose()
}
```

`CompositionLocalProvider` and `remember` without keys still work — they are resolved during the single composition pass. Conditional logic (`if`/`when` on parameters) works. Only reactive state changes are unsupported.

## Testing Strategy

### Test levels

| Level | What it tests | Source set | Assertion style |
|-------|--------------|------------|-----------------|
| Node tree construction | `TextApplier` builds correct `HtmlNode` tree | `core/src/commonTest` | Assert tree structure (tag names, attrs, children) |
| Serializer output | `TreeSerializer` produces correct HTML/SVG strings | `serializer/src/commonTest` | String comparison on hand-built trees |
| End-to-end rendering | `renderComposableToString` with real composables | `core/src/commonTest` | String comparison of final output |
| JS DOM rendering | Existing browser-based tests unchanged | `core/src/jsTest` | `innerHTML`/`outerHTML` + DOM property inspection |

### Edge cases to test (derived from reference implementation)

**Attribute handling:**
- Multiple `classes()` calls accumulate: `classes("a", "b")` then `classes("c", "d")` produces `class="a b c d"`
- `attr("class", ...)` overrides `classes()` calls
- Last-write-wins for duplicate attributes: `attr("x", "1")` then `attr("x", "2")` yields `x="2"`
- Boolean attributes render as empty value: `disabled` produces `disabled=""`
- Attribute ordering is preserved in output

**Inline style handling:**
- Multiple `style {}` blocks merge: `"opacity: 0.4; padding: 40px;"`
- Last-write-wins within a single style block
- `attr("style", ...)` overrides `style {}` calls
- CSS custom properties preserve `--` prefix: `--color: red` not `color: red`
- Style format: `"property: value;"` with space after colon

**Void elements:**
- HTML void elements self-close: `<br>`, `<hr>`, `<img>`, `<input>`, `<col>`, `<area>`, `<embed>`, `<param>`, `<source>`, `<track>`, `<wbr>`
- SVG leaf elements with no children self-close: `<circle ... />`
- XHTML mode produces `<br />` vs HTML mode `<br>`
- Non-void elements with no children still get closing tag: `<div></div>`

**SVG-specific:**
- CamelCase element names preserved: `clipPath`, `linearGradient`, `radialGradient`, `textPath`, `animateMotion`, `animateTransform`
- `xmlns` attribute on root `<svg>` element for standalone SVG output
- Numeric vs CSS-unit attribute values: `cx="50"` vs `cx="50px"`
- Path `d` attribute with multiline data preserved
- `viewBox` attribute with space-separated values
- Nested SVG structures: `<svg><defs><linearGradient>...</linearGradient></defs></svg>`

**Text content:**
- HTML escaping in text nodes: `<`, `>`, `&` escaped
- Attribute value escaping: `"` escaped in attribute values
- Empty text nodes
- Adjacent text nodes

**Pretty printing:**
- Indentation of nested elements
- No indentation when `prettyPrint = false`
- Mixed inline text and element children
- Deeply nested structures

**Composable behavior (single-pass):**
- Conditional rendering: `if (condition) { Div { } }` produces correct output
- List rendering: `items.forEach { Div { Text(it) } }`
- `remember` resolves during single pass
- `CompositionLocalProvider` values propagate correctly
- Nested composable functions compose correctly

### What we do NOT test

- Recomposition (not supported)
- Event listeners (not applicable to static rendering)
- DOM mutation/lifecycle (JS-only, covered by existing tests)
- CSS computed styles (no browser in text rendering)

## What stays the same

The JS interactive experience. Existing Compose HTML code targeting the browser continues to work identically via the `DomApplier` path.

## What's new

JVM and JS can both generate static HTML/SVG strings from the same composable code.

## Dependencies

- `org.jetbrains.compose.runtime:runtime` — Compose runtime (Composition, Applier, ComposeNode, CompositionLocal)
- `org.jetbrains.kotlinx:kotlinx-coroutines-core` — coroutine support for the Compose runtime
- No dependency on kotlinx.html for SVG serialization — the `TreeSerializer` handles all tag writing directly
