# JS DOM Integration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add the interactive DOM rendering path to the JS target by depending on `compose.html.core` and bridging our `AttrsBuilder` to Compose HTML's `AttrsScope`, so JS gets both interactive DOM rendering and static text rendering.

**Architecture:** The JS `actual` of `TagElement` checks `LocalStaticRendering` — when true, it uses our `TextApplier`/`HtmlNode` path (same as JVM); when false, it delegates to Compose HTML's `TagElement` via `DomApplier`. A `DomAttrsScope` adapter bridges our `AttrsBuilder` lambdas to Compose HTML's `AttrsScope`. A `NamespacedElementBuilder` handles SVG elements that need `createElementNS`.

**Tech Stack:** Kotlin/JS, Compose Runtime, Compose HTML (`html-core`)

**Spec:** `docs/superpowers/specs/2026-04-10-js-dom-integration-design.md`

---

## File Map

```
core/
├── build.gradle.kts                                                  # MODIFY: add jsMain compose.html.core dep
└── src/
    └── jsMain/kotlin/compose/html/
        ├── Composables.js.kt                                         # MODIFY: dual-path dispatch
        ├── DomAttrsScope.kt                                          # CREATE: AttrsBuilder→AttrsScope adapter
        ├── NamespacedElementBuilder.kt                               # CREATE: createElementNS for SVG
        └── RenderComposableDom.kt                                    # CREATE: JS-only renderComposable(Element)
```

---

### Task 1: Add compose.html.core dependency

**Files:**
- Modify: `core/build.gradle.kts`

- [ ] **Step 1: Add jsMain dependency**

Edit `core/build.gradle.kts` to add the `jsMain` source set with `compose.html.core`:

```kotlin
plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

kotlin {
    jvm()
    js(IR) {
        browser()
    }

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(compose.runtime)
        }
        jsMain.dependencies {
            api(compose.html.core)
        }
    }
}
```

- [ ] **Step 2: Verify it compiles**

Run: `cd /home/ralph/compose-html-multiplatform && ./gradlew :core:compileKotlinJs`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add core/build.gradle.kts
git commit -m "chore: add compose.html.core as JS dependency"
```

---

### Task 2: DomAttrsScope adapter

**Files:**
- Create: `core/src/jsMain/kotlin/compose/html/DomAttrsScope.kt`

- [ ] **Step 1: Implement DomAttrsScope and DomStyleScope**

Create `core/src/jsMain/kotlin/compose/html/DomAttrsScope.kt`:

```kotlin
package compose.html

import org.jetbrains.compose.web.attributes.AttrsScope
import org.jetbrains.compose.web.css.StyleScope
import org.w3c.dom.Element

class DomAttrsScope<TElement : Element>(
    private val delegate: AttrsScope<TElement>,
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
    private val delegate: StyleScope,
) : StyleBuilder {

    override fun property(name: String, value: String) {
        delegate.property(name, value)
    }
}
```

- [ ] **Step 2: Verify it compiles**

Run: `cd /home/ralph/compose-html-multiplatform && ./gradlew :core:compileKotlinJs`
Expected: BUILD SUCCESSFUL

Note: If `AttrsScope` or `StyleScope` import paths differ, check the actual package names in the compose.html.core dependency. Common alternatives:
- `org.jetbrains.compose.web.attributes.AttrsScope`
- `org.jetbrains.compose.web.css.StyleScope`
- `org.jetbrains.compose.web.dom.AttrsScope` (less likely)

Adjust imports as needed until it compiles.

- [ ] **Step 3: Commit**

```bash
git add core/src/jsMain/kotlin/compose/html/DomAttrsScope.kt
git commit -m "feat: add DomAttrsScope adapter bridging AttrsBuilder to Compose HTML's AttrsScope"
```

---

### Task 3: NamespacedElementBuilder

**Files:**
- Create: `core/src/jsMain/kotlin/compose/html/NamespacedElementBuilder.kt`

- [ ] **Step 1: Implement NamespacedElementBuilder**

Create `core/src/jsMain/kotlin/compose/html/NamespacedElementBuilder.kt`:

```kotlin
package compose.html

import org.jetbrains.compose.web.dom.ElementBuilder
import kotlinx.browser.document
import org.w3c.dom.Element

internal class NamespacedElementBuilder<TElement : Element>(
    private val tagName: String,
    private val namespace: String,
) : ElementBuilder<TElement> {

    @Suppress("UNCHECKED_CAST")
    override fun create(): TElement {
        return document.createElementNS(namespace, tagName) as TElement
    }
}
```

- [ ] **Step 2: Verify it compiles**

Run: `cd /home/ralph/compose-html-multiplatform && ./gradlew :core:compileKotlinJs`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add core/src/jsMain/kotlin/compose/html/NamespacedElementBuilder.kt
git commit -m "feat: add NamespacedElementBuilder for SVG createElementNS support"
```

---

### Task 4: Dual-path dispatch in JS actual

**Files:**
- Modify: `core/src/jsMain/kotlin/compose/html/Composables.js.kt`

- [ ] **Step 1: Rewrite Composables.js.kt with dual-path dispatch**

Replace the entire contents of `core/src/jsMain/kotlin/compose/html/Composables.js.kt`:

```kotlin
package compose.html

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode
import org.jetbrains.compose.web.dom.ElementBuilder
import org.w3c.dom.Element

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
        val builder: ElementBuilder<Element> = if (namespace != null) {
            NamespacedElementBuilder(tagName, namespace)
        } else {
            ElementBuilder.createBuilder(tagName)
        }

        org.jetbrains.compose.web.dom.TagElement(
            elementBuilder = builder,
            applyAttrs = attrs?.let { attrsBlock ->
                { DomAttrsScope(this).attrsBlock() }
            },
            content = { content() },
        )
    }
}

@Composable
actual fun Text(value: String) {
    if (LocalStaticRendering.current) {
        ComposeNode<HtmlTextNode, TextApplier>(
            factory = { HtmlTextNode(value) },
            update = {
                set(value) { text = it }
            },
        )
    } else {
        org.jetbrains.compose.web.dom.Text(value)
    }
}
```

- [ ] **Step 2: Verify it compiles**

Run: `cd /home/ralph/compose-html-multiplatform && ./gradlew :core:compileKotlinJs`
Expected: BUILD SUCCESSFUL

If there are type mismatches (e.g., `ElementBuilder` generics, `content` lambda signature), adjust accordingly. Key things to watch:
- `org.jetbrains.compose.web.dom.TagElement` expects `content: (@Composable ElementScope<TElement>.() -> Unit)?` — wrapping as `{ content() }` drops the `ElementScope` receiver, which is correct
- `ElementBuilder.createBuilder<Element>(tagName)` may need an explicit type parameter
- The `applyAttrs` lambda runs in `AttrsScope<Element>` context — `DomAttrsScope(this)` wraps it

- [ ] **Step 3: Verify existing JVM tests still pass**

Run: `cd /home/ralph/compose-html-multiplatform && ./gradlew jvmTest`
Expected: BUILD SUCCESSFUL — all 97 JVM tests pass unchanged

- [ ] **Step 4: Commit**

```bash
git add core/src/jsMain/kotlin/compose/html/Composables.js.kt
git commit -m "feat: add dual-path dispatch in JS actual — DOM path via Compose HTML, static path via TextApplier"
```

---

### Task 5: JS-only renderComposable entry point

**Files:**
- Create: `core/src/jsMain/kotlin/compose/html/RenderComposableDom.kt`

- [ ] **Step 1: Implement renderComposable for DOM**

Create `core/src/jsMain/kotlin/compose/html/RenderComposableDom.kt`:

```kotlin
package compose.html

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composition
import androidx.compose.runtime.CompositionLocalProvider
import org.w3c.dom.Element

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

- [ ] **Step 2: Verify it compiles**

Run: `cd /home/ralph/compose-html-multiplatform && ./gradlew :core:compileKotlinJs`
Expected: BUILD SUCCESSFUL

Note: If `org.jetbrains.compose.web.renderComposable` has a different signature (e.g., returns `Unit` instead of `Composition`, or the parameter name differs), check the actual API. The function is in the `org.jetbrains.compose.web` package from the `internal-html-core-runtime` module. Adjust as needed.

- [ ] **Step 3: Verify all tests still pass**

Run: `cd /home/ralph/compose-html-multiplatform && ./gradlew jvmTest`
Expected: BUILD SUCCESSFUL — all 97 tests pass

- [ ] **Step 4: Commit**

```bash
git add core/src/jsMain/kotlin/compose/html/RenderComposableDom.kt
git commit -m "feat: add JS-only renderComposable(Element) entry point for interactive DOM rendering"
```

---

### Task 6: Final verification

- [ ] **Step 1: Full JS compilation**

Run: `cd /home/ralph/compose-html-multiplatform && ./gradlew :core:compileKotlinJs :svg:compileKotlinJs :serializer:compileKotlinJs`
Expected: BUILD SUCCESSFUL — all three modules compile for JS

- [ ] **Step 2: Full JVM test suite**

Run: `cd /home/ralph/compose-html-multiplatform && ./gradlew jvmTest`
Expected: BUILD SUCCESSFUL — all 97 tests pass

- [ ] **Step 3: Commit (if any fixes were needed)**

```bash
git add -A
git commit -m "fix: resolve any JS compilation issues from DOM integration"
```

Only run this step if fixes were applied. Skip if everything compiled cleanly.

---

## Summary

| Task | What it does | Files |
|------|-------------|-------|
| 1 | Add `compose.html.core` JS dependency | `core/build.gradle.kts` |
| 2 | `DomAttrsScope` + `DomStyleScope` adapter | `DomAttrsScope.kt` (new) |
| 3 | `NamespacedElementBuilder` for SVG | `NamespacedElementBuilder.kt` (new) |
| 4 | Dual-path dispatch in JS `TagElement` + `Text` | `Composables.js.kt` (rewrite) |
| 5 | `renderComposable(Element)` JS entry point | `RenderComposableDom.kt` (new) |
| 6 | Final verification | — |
