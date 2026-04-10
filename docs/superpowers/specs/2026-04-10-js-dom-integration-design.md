# JS DOM Integration — Design Spec

## Overview

Add the interactive DOM rendering path to the JS target by depending on `compose.html.core` and bridging our `AttrsBuilder` interface to Compose HTML's `AttrsScope`. The JS `actual` of `TagElement` dispatches between the DOM path (interactive) and the `TextApplier` path (static) based on `LocalStaticRendering`.

## What changes

### 1. Build dependency

Add `compose.html.core` as a JS-only dependency in `core/build.gradle.kts`:

```kotlin
jsMain.dependencies {
    api(compose.html.core)
}
```

Using `api` so that consumers of our library on JS automatically get Compose HTML's DOM types (`Element`, `HTMLElement`, etc.) without needing to add it themselves.

### 2. AttrsScope adapter

Create `core/src/jsMain/kotlin/compose/html/DomAttrsScope.kt` — an adapter that implements our `AttrsBuilder<E>` and delegates to Compose HTML's `AttrsScope<TElement>`:

```kotlin
class DomAttrsScope<TElement : Element>(
    private val delegate: AttrsScope<TElement>
) : AttrsBuilder<Any> {

    override fun attr(name: String, value: String) {
        delegate.attr(name, value)
    }

    override fun classes(vararg classNames: String) {
        delegate.classes(*classNames)
    }

    override fun style(builder: StyleBuilder.() -> Unit) {
        delegate.style {
            DomStyleScope(this).builder()
        }
    }
}

class DomStyleScope(
    private val delegate: StyleScope
) : StyleBuilder {

    override fun property(name: String, value: String) {
        delegate.property(name, value)
    }
}
```

This is a one-way bridge: our `AttrsBuilder` lambdas can be applied to Compose HTML's `AttrsScope`. The user's code calls `attr()`, `classes()`, `style { property() }` which get forwarded to the real DOM attribute/style system.

### 3. JS actual — dual-path dispatch

Rewrite `core/src/jsMain/kotlin/compose/html/Composables.js.kt`:

```kotlin
@Composable
actual fun TagElement(
    tagName: String,
    namespace: String?,
    attrs: (AttrsBuilder<*>.() -> Unit)?,
    content: @Composable () -> Unit,
) {
    if (LocalStaticRendering.current) {
        // Static text path — same as JVM
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
    } else {
        // Interactive DOM path — delegate to Compose HTML
        org.jetbrains.compose.web.dom.TagElement(
            elementBuilder = ElementBuilder.createBuilder(tagName),
            applyAttrs = attrs?.let { attrsBlock ->
                { DomAttrsScope(this).attrsBlock() }
            },
            content = content?.let { { it() } },
        )
    }
}

@Composable
actual fun Text(value: String) {
    if (LocalStaticRendering.current) {
        ComposeNode<HtmlTextNode, TextApplier>(
            factory = { HtmlTextNode(value) },
            update = { set(value) { text = it } },
        )
    } else {
        org.jetbrains.compose.web.dom.Text(value)
    }
}
```

Note on namespace: Compose HTML's `ElementBuilder.createBuilder` uses `document.createElement`. For SVG elements that need `document.createElementNS`, we need a custom `ElementBuilder` that handles the namespace. We'll create a small `NamespacedElementBuilder`:

```kotlin
private class NamespacedElementBuilder<TElement : Element>(
    private val tagName: String,
    private val namespace: String,
) : ElementBuilder<TElement> {
    override fun create(): TElement {
        return document.createElementNS(namespace, tagName).unsafeCast<TElement>()
    }
}
```

The dispatch then becomes:
```kotlin
val builder = if (namespace != null) {
    NamespacedElementBuilder(tagName, namespace)
} else {
    ElementBuilder.createBuilder(tagName)
}
```

### 4. JS-only interactive entry point

Create `core/src/jsMain/kotlin/compose/html/RenderComposableDom.kt`:

```kotlin
fun renderComposable(
    root: Element,
    content: @Composable () -> Unit,
): Composition {
    return org.jetbrains.compose.web.renderComposable(root) {
        CompositionLocalProvider(LocalStaticRendering provides false) {
            content()
        }
    }
}
```

This delegates to Compose HTML's `renderComposable` (which sets up `DomApplier`, `GlobalSnapshotManager`, etc.) and wraps the content with `LocalStaticRendering = false` so that our composables take the DOM path.

### 5. Content lambda bridging

There's an API mismatch: Compose HTML's `TagElement` expects `content: @Composable ElementScope<TElement>.() -> Unit` (scoped to `ElementScope`), but our composables use plain `@Composable () -> Unit`. The bridge ignores `ElementScope` since our common API doesn't expose it:

```kotlin
content = { content() }  // drops the ElementScope receiver
```

Users who need `ElementScope` features (like `DomSideEffect`, `DisposableRefEffect`) can use Compose HTML's own composables directly on JS — our common wrappers are for the cross-platform subset.

## What doesn't change

- All `commonMain` code (composables, HtmlNode, TextApplier, AttrsBuilder)
- JVM path (TextApplier only)
- `renderComposableToString` / `renderComposableTo` on both targets
- SVG module (works through TagElement dispatch on both paths)
- All existing tests

## Testing

- Existing JVM tests: unchanged, still pass
- Existing JS tests (static path via `renderComposableToString`): still pass
- New JS browser tests (optional): use Compose HTML's test infrastructure to verify DOM rendering works through our composables

## Dependencies added

- `org.jetbrains.compose.html:html-core` (JS only) — brings in `DomApplier`, `AttrsScope`, `ElementBuilder`, `renderComposable`, and all DOM rendering infrastructure
