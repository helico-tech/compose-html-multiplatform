# Compose HTML Multiplatform Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fork JetBrains Compose HTML into a Kotlin Multiplatform library (JVM + JS) that keeps the existing JS DOM rendering path and adds a common text renderer for one-shot static HTML/SVG generation.

**Architecture:** Composables live in `commonMain` behind an `AttrsBuilder` interface. `TagElement` and `Text` are `expect`/`actual` — JVM uses a `TextApplier` that builds a lightweight `HtmlNode` tree, JS dispatches between the existing `DomApplier` (interactive) and the same `TextApplier` (static) via a `LocalStaticRendering` composition local. A `TreeSerializer` walks the tree to produce HTML/SVG text.

**Tech Stack:** Kotlin Multiplatform (JVM + JS/IR), Compose Runtime, Gradle with Kotlin DSL

**Spec:** `docs/superpowers/specs/2026-04-10-compose-html-multiplatform-design.md`

---

## File Map

```
compose-html-multiplatform/
├── build.gradle.kts                                          # Root build: plugins, repositories
├── settings.gradle.kts                                       # Module includes + dependency resolution
├── gradle.properties                                         # Kotlin/Compose versions
│
├── core/
│   ├── build.gradle.kts                                      # KMP: jvm() + js(IR), compose runtime dep
│   └── src/
│       ├── commonMain/kotlin/compose/html/
│       │   ├── HtmlNode.kt                                   # HtmlNode, HtmlElementNode, HtmlTextNode
│       │   ├── TextApplier.kt                                # AbstractApplier<HtmlNode>
│       │   ├── AttrsBuilder.kt                               # AttrsBuilder<E>, StyleBuilder interfaces
│       │   ├── CommonAttrsScope.kt                           # CommonAttrsScope<E>, CommonStyleScope
│       │   ├── Composables.kt                                # expect TagElement, expect Text, Div, Span, A, P, H1-H6, Ul, Ol, Li, etc.
│       │   └── LocalStaticRendering.kt                       # CompositionLocal<Boolean>
│       ├── commonTest/kotlin/compose/html/
│       │   ├── HtmlNodeTest.kt                               # Tree structure unit tests
│       │   ├── CommonAttrsScopeTest.kt                       # Attribute/style/class accumulation tests
│       │   └── TextApplierTest.kt                            # Applier insert/remove/move tests
│       └── jvmMain/kotlin/compose/html/
│           └── Composables.jvm.kt                            # actual TagElement, actual Text (TextApplier path)
│
├── serializer/
│   ├── build.gradle.kts                                      # KMP: jvm() + js(IR), depends on :core
│   └── src/
│       ├── commonMain/kotlin/compose/html/serializer/
│       │   ├── TreeSerializer.kt                             # serialize(), serializeToString()
│       │   ├── HtmlEscaping.kt                               # escapeHtml(), escapeAttr()
│       │   ├── VoidElements.kt                               # isVoidElement(), HTML_VOID_ELEMENTS set
│       │   └── RenderComposable.kt                           # renderComposableToString, renderComposableTo, composeOnce
│       └── commonTest/kotlin/compose/html/serializer/
│           ├── TreeSerializerTest.kt                         # Serialization output tests
│           ├── HtmlEscapingTest.kt                           # Escaping edge case tests
│           ├── VoidElementsTest.kt                           # Void element detection tests
│           └── RenderComposableTest.kt                       # End-to-end renderComposableToString tests
│
└── svg/
    ├── build.gradle.kts                                      # KMP: jvm() + js(IR), depends on :core
    └── src/
        ├── commonMain/kotlin/compose/html/svg/
        │   ├── Svg.kt                                        # Svg, SvgElement composables
        │   ├── SvgShapes.kt                                  # Circle, Rect, Ellipse, Line, Path, Polygon, Polyline
        │   ├── SvgStructure.kt                               # G, Defs, Symbol, Use, ClipPath, Mask, View, Switch
        │   ├── SvgText.kt                                    # SvgText, TextPath, Tspan
        │   ├── SvgGradients.kt                               # LinearGradient, RadialGradient, Stop, Pattern
        │   ├── SvgAnimation.kt                               # Animate, AnimateMotion, AnimateTransform, Set, Mpath
        │   ├── SvgMisc.kt                                    # Image, Filter, Marker, Title, Desc, SvgA
        │   └── SvgAttrs.kt                                   # fill(), stroke(), strokeWidth(), transform(), etc.
        └── commonTest/kotlin/compose/html/svg/
            └── SvgRenderTest.kt                              # SVG rendering tests (all 35 elements + edge cases)
```

---

### Task 1: Gradle Project Scaffolding

**Files:**
- Create: `build.gradle.kts`
- Create: `settings.gradle.kts`
- Create: `gradle.properties`
- Create: `core/build.gradle.kts`
- Create: `serializer/build.gradle.kts`
- Create: `svg/build.gradle.kts`

- [ ] **Step 1: Create `gradle.properties`**

```properties
kotlin.code.style=official
org.gradle.jvm.args=-Xmx2g
kotlin.version=2.1.10
compose.version=1.7.3
```

- [ ] **Step 2: Create root `build.gradle.kts`**

```kotlin
plugins {
    kotlin("multiplatform") version "2.1.10" apply false
    id("org.jetbrains.compose") version "1.7.3" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.1.10" apply false
}
```

- [ ] **Step 3: Create `settings.gradle.kts`**

```kotlin
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "compose-html-multiplatform"

include(":core")
include(":serializer")
include(":svg")
```

- [ ] **Step 4: Create `core/build.gradle.kts`**

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
            implementation(compose.runtime) // for test composition
        }
    }
}
```

- [ ] **Step 5: Create `serializer/build.gradle.kts`**

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
            implementation(project(":core"))
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
```

- [ ] **Step 6: Create `svg/build.gradle.kts`**

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
            implementation(project(":core"))
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(project(":serializer")) // for renderComposableToString in tests
        }
    }
}
```

- [ ] **Step 7: Install Gradle wrapper**

Run: `cd /home/ralph/compose-html-multiplatform && gradle wrapper --gradle-version 8.11.1`

- [ ] **Step 8: Verify project compiles**

Run: `cd /home/ralph/compose-html-multiplatform && ./gradlew projects`
Expected: All three subprojects listed.

- [ ] **Step 9: Create source directories**

```bash
mkdir -p core/src/{commonMain,commonTest,jvmMain}/kotlin/compose/html
mkdir -p serializer/src/{commonMain,commonTest}/kotlin/compose/html/serializer
mkdir -p svg/src/{commonMain,commonTest}/kotlin/compose/html/svg
```

- [ ] **Step 10: Commit**

```bash
git add -A
git commit -m "chore: scaffold Gradle multiplatform project with core, serializer, svg modules"
```

---

### Task 2: HtmlNode Tree

**Files:**
- Create: `core/src/commonMain/kotlin/compose/html/HtmlNode.kt`
- Create: `core/src/commonTest/kotlin/compose/html/HtmlNodeTest.kt`

- [ ] **Step 1: Write failing test for HtmlNode tree structure**

Create `core/src/commonTest/kotlin/compose/html/HtmlNodeTest.kt`:

```kotlin
package compose.html

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HtmlNodeTest {

    @Test
    fun elementNodeHasCorrectTagName() {
        val node = HtmlElementNode("div")
        assertEquals("div", node.tagName)
        assertNull(node.namespace)
        assertTrue(node.attributes.isEmpty())
        assertTrue(node.inlineStyles.isEmpty())
        assertTrue(node.classes.isEmpty())
        assertTrue(node.children.isEmpty())
    }

    @Test
    fun elementNodeWithNamespace() {
        val node = HtmlElementNode("circle", namespace = "http://www.w3.org/2000/svg")
        assertEquals("circle", node.tagName)
        assertEquals("http://www.w3.org/2000/svg", node.namespace)
    }

    @Test
    fun textNodeHoldsText() {
        val node = HtmlTextNode("hello")
        assertEquals("hello", node.text)
    }

    @Test
    fun childrenCanBeAdded() {
        val parent = HtmlElementNode("div")
        val child = HtmlElementNode("span")
        val text = HtmlTextNode("hello")

        parent.children.add(child)
        parent.children.add(text)

        assertEquals(2, parent.children.size)
        assertIs<HtmlElementNode>(parent.children[0])
        assertIs<HtmlTextNode>(parent.children[1])
    }

    @Test
    fun attributesCanBeSet() {
        val node = HtmlElementNode("a")
        node.attributes["href"] = "https://example.com"
        node.attributes["target"] = "_blank"

        assertEquals("https://example.com", node.attributes["href"])
        assertEquals("_blank", node.attributes["target"])
    }

    @Test
    fun lastWriteWinsForAttributes() {
        val node = HtmlElementNode("div")
        node.attributes["data-x"] = "first"
        node.attributes["data-x"] = "second"

        assertEquals("second", node.attributes["data-x"])
    }

    @Test
    fun classesAccumulate() {
        val node = HtmlElementNode("div")
        node.classes.add("a")
        node.classes.add("b")
        node.classes.addAll(listOf("c", "d"))

        assertEquals(listOf("a", "b", "c", "d"), node.classes)
    }

    @Test
    fun inlineStylesCanBeSet() {
        val node = HtmlElementNode("div")
        node.inlineStyles["color"] = "red"
        node.inlineStyles["padding"] = "10px"

        assertEquals("red", node.inlineStyles["color"])
        assertEquals("10px", node.inlineStyles["padding"])
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd /home/ralph/compose-html-multiplatform && ./gradlew :core:jvmTest`
Expected: FAIL — `HtmlNode`, `HtmlElementNode`, `HtmlTextNode` not found.

- [ ] **Step 3: Implement HtmlNode**

Create `core/src/commonMain/kotlin/compose/html/HtmlNode.kt`:

```kotlin
package compose.html

sealed class HtmlNode {
    val children: MutableList<HtmlNode> = mutableListOf()
}

class HtmlElementNode(
    val tagName: String,
    val namespace: String? = null,
    val attributes: MutableMap<String, String> = mutableMapOf(),
    val inlineStyles: MutableMap<String, String> = mutableMapOf(),
    val classes: MutableList<String> = mutableListOf(),
) : HtmlNode()

class HtmlTextNode(
    var text: String,
) : HtmlNode()
```

- [ ] **Step 4: Run test to verify it passes**

Run: `cd /home/ralph/compose-html-multiplatform && ./gradlew :core:jvmTest`
Expected: PASS — all HtmlNodeTest tests green.

- [ ] **Step 5: Commit**

```bash
git add core/src/commonMain/kotlin/compose/html/HtmlNode.kt core/src/commonTest/kotlin/compose/html/HtmlNodeTest.kt
git commit -m "feat: add HtmlNode sealed class hierarchy (element + text nodes)"
```

---

### Task 3: AttrsBuilder Interface + CommonAttrsScope

**Files:**
- Create: `core/src/commonMain/kotlin/compose/html/AttrsBuilder.kt`
- Create: `core/src/commonMain/kotlin/compose/html/CommonAttrsScope.kt`
- Create: `core/src/commonTest/kotlin/compose/html/CommonAttrsScopeTest.kt`

- [ ] **Step 1: Write failing tests for CommonAttrsScope**

Create `core/src/commonTest/kotlin/compose/html/CommonAttrsScopeTest.kt`:

```kotlin
package compose.html

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CommonAttrsScopeTest {

    @Test
    fun attrSetsAttribute() {
        val node = HtmlElementNode("div")
        val scope = CommonAttrsScope<Any>(node)
        scope.attr("data-x", "hello")

        assertEquals("hello", node.attributes["data-x"])
    }

    @Test
    fun lastAttrWriteWins() {
        val node = HtmlElementNode("div")
        val scope = CommonAttrsScope<Any>(node)
        scope.attr("data-x", "first")
        scope.attr("data-x", "second")

        assertEquals("second", node.attributes["data-x"])
    }

    @Test
    fun classesAccumulate() {
        val node = HtmlElementNode("div")
        val scope = CommonAttrsScope<Any>(node)
        scope.classes("a", "b")
        scope.classes("c", "d")

        assertEquals(listOf("a", "b", "c", "d"), node.classes)
    }

    @Test
    fun idSetsAttribute() {
        val node = HtmlElementNode("div")
        val scope = CommonAttrsScope<Any>(node)
        scope.id("my-id")

        assertEquals("my-id", node.attributes["id"])
    }

    @Test
    fun styleSetsInlineStyles() {
        val node = HtmlElementNode("div")
        val scope = CommonAttrsScope<Any>(node)
        scope.style {
            property("color", "red")
            property("padding", "10px")
        }

        assertEquals("red", node.inlineStyles["color"])
        assertEquals("10px", node.inlineStyles["padding"])
    }

    @Test
    fun multipleStyleBlocksMerge() {
        val node = HtmlElementNode("div")
        val scope = CommonAttrsScope<Any>(node)
        scope.style { property("opacity", "0.4") }
        scope.style { property("padding", "40px") }

        assertEquals("0.4", node.inlineStyles["opacity"])
        assertEquals("40px", node.inlineStyles["padding"])
    }

    @Test
    fun styleLastWriteWins() {
        val node = HtmlElementNode("div")
        val scope = CommonAttrsScope<Any>(node)
        scope.style {
            property("opacity", "0.3")
            property("opacity", "0.2")
        }

        assertEquals("0.2", node.inlineStyles["opacity"])
    }

    @Test
    fun cssCustomPropertyPreservesPrefix() {
        val node = HtmlElementNode("div")
        val scope = CommonAttrsScope<Any>(node)
        scope.style {
            property("--color", "red")
        }

        assertEquals("red", node.inlineStyles["--color"])
    }

    @Test
    fun booleanAttributeEmptyValue() {
        val node = HtmlElementNode("input")
        val scope = CommonAttrsScope<Any>(node)
        scope.attr("disabled", "")

        assertEquals("", node.attributes["disabled"])
    }

    @Test
    fun attrsBuilderInterfaceCompatibility() {
        val node = HtmlElementNode("div")
        val scope: AttrsBuilder<Any> = CommonAttrsScope(node)
        scope.attr("x", "1")
        scope.classes("a")
        scope.style { property("color", "red") }

        assertEquals("1", node.attributes["x"])
        assertEquals(listOf("a"), node.classes)
        assertEquals("red", node.inlineStyles["color"])
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd /home/ralph/compose-html-multiplatform && ./gradlew :core:jvmTest --tests "compose.html.CommonAttrsScopeTest"`
Expected: FAIL — `AttrsBuilder`, `CommonAttrsScope`, `CommonStyleScope` not found.

- [ ] **Step 3: Implement AttrsBuilder interface**

Create `core/src/commonMain/kotlin/compose/html/AttrsBuilder.kt`:

```kotlin
package compose.html

interface AttrsBuilder<E> {
    fun attr(name: String, value: String)
    fun classes(vararg classNames: String)
    fun style(builder: StyleBuilder.() -> Unit)
    fun id(value: String) { attr("id", value) }
}

interface StyleBuilder {
    fun property(name: String, value: String)
}
```

- [ ] **Step 4: Implement CommonAttrsScope**

Create `core/src/commonMain/kotlin/compose/html/CommonAttrsScope.kt`:

```kotlin
package compose.html

class CommonAttrsScope<E>(val node: HtmlElementNode) : AttrsBuilder<E> {

    override fun attr(name: String, value: String) {
        node.attributes[name] = value
    }

    override fun classes(vararg classNames: String) {
        node.classes.addAll(classNames)
    }

    override fun style(builder: StyleBuilder.() -> Unit) {
        CommonStyleScope(node.inlineStyles).builder()
    }
}

class CommonStyleScope(private val styles: MutableMap<String, String>) : StyleBuilder {

    override fun property(name: String, value: String) {
        styles[name] = value
    }
}
```

- [ ] **Step 5: Run tests to verify they pass**

Run: `cd /home/ralph/compose-html-multiplatform && ./gradlew :core:jvmTest`
Expected: PASS — all CommonAttrsScopeTest and HtmlNodeTest tests green.

- [ ] **Step 6: Commit**

```bash
git add core/src/commonMain/kotlin/compose/html/AttrsBuilder.kt core/src/commonMain/kotlin/compose/html/CommonAttrsScope.kt core/src/commonTest/kotlin/compose/html/CommonAttrsScopeTest.kt
git commit -m "feat: add AttrsBuilder interface and CommonAttrsScope implementation"
```

---

### Task 4: TextApplier

**Files:**
- Create: `core/src/commonMain/kotlin/compose/html/TextApplier.kt`
- Create: `core/src/commonTest/kotlin/compose/html/TextApplierTest.kt`

- [ ] **Step 1: Write failing tests for TextApplier**

Create `core/src/commonTest/kotlin/compose/html/TextApplierTest.kt`:

```kotlin
package compose.html

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class TextApplierTest {

    @Test
    fun insertBottomUpAddsChild() {
        val root = HtmlElementNode("root")
        val applier = TextApplier(root)

        val child = HtmlElementNode("div")
        applier.insertBottomUp(0, child)

        assertEquals(1, root.children.size)
        assertIs<HtmlElementNode>(root.children[0])
        assertEquals("div", (root.children[0] as HtmlElementNode).tagName)
    }

    @Test
    fun insertBottomUpAtIndex() {
        val root = HtmlElementNode("root")
        val applier = TextApplier(root)

        val first = HtmlElementNode("first")
        val second = HtmlElementNode("second")
        val inserted = HtmlElementNode("inserted")

        applier.insertBottomUp(0, first)
        applier.insertBottomUp(1, second)
        applier.insertBottomUp(1, inserted)

        assertEquals(3, root.children.size)
        assertEquals("first", (root.children[0] as HtmlElementNode).tagName)
        assertEquals("inserted", (root.children[1] as HtmlElementNode).tagName)
        assertEquals("second", (root.children[2] as HtmlElementNode).tagName)
    }

    @Test
    fun insertTextNode() {
        val root = HtmlElementNode("root")
        val applier = TextApplier(root)

        val text = HtmlTextNode("hello")
        applier.insertBottomUp(0, text)

        assertEquals(1, root.children.size)
        assertIs<HtmlTextNode>(root.children[0])
        assertEquals("hello", (root.children[0] as HtmlTextNode).text)
    }

    @Test
    fun removeChildren() {
        val root = HtmlElementNode("root")
        val applier = TextApplier(root)

        applier.insertBottomUp(0, HtmlElementNode("a"))
        applier.insertBottomUp(1, HtmlElementNode("b"))
        applier.insertBottomUp(2, HtmlElementNode("c"))

        applier.remove(1, 1)

        assertEquals(2, root.children.size)
        assertEquals("a", (root.children[0] as HtmlElementNode).tagName)
        assertEquals("c", (root.children[1] as HtmlElementNode).tagName)
    }

    @Test
    fun removeMultipleChildren() {
        val root = HtmlElementNode("root")
        val applier = TextApplier(root)

        applier.insertBottomUp(0, HtmlElementNode("a"))
        applier.insertBottomUp(1, HtmlElementNode("b"))
        applier.insertBottomUp(2, HtmlElementNode("c"))
        applier.insertBottomUp(3, HtmlElementNode("d"))

        applier.remove(1, 2)

        assertEquals(2, root.children.size)
        assertEquals("a", (root.children[0] as HtmlElementNode).tagName)
        assertEquals("d", (root.children[1] as HtmlElementNode).tagName)
    }

    @Test
    fun onClearRemovesAll() {
        val root = HtmlElementNode("root")
        val applier = TextApplier(root)

        applier.insertBottomUp(0, HtmlElementNode("a"))
        applier.insertBottomUp(1, HtmlElementNode("b"))

        applier.onClear()

        assertEquals(0, root.children.size)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd /home/ralph/compose-html-multiplatform && ./gradlew :core:jvmTest --tests "compose.html.TextApplierTest"`
Expected: FAIL — `TextApplier` not found.

- [ ] **Step 3: Implement TextApplier**

Create `core/src/commonMain/kotlin/compose/html/TextApplier.kt`:

```kotlin
package compose.html

import androidx.compose.runtime.AbstractApplier

class TextApplier(root: HtmlElementNode) : AbstractApplier<HtmlNode>(root) {

    override fun insertTopDown(index: Int, instance: HtmlNode) {
        // no-op — we use bottom-up like the DomApplier
    }

    override fun insertBottomUp(index: Int, instance: HtmlNode) {
        (current as HtmlElementNode).children.add(index, instance)
    }

    override fun remove(index: Int, count: Int) {
        val children = (current as HtmlElementNode).children
        children.subList(index, index + count).clear()
    }

    override fun move(from: Int, to: Int, count: Int) {
        val children = (current as HtmlElementNode).children
        val moved = children.subList(from, from + count).toList()
        children.subList(from, from + count).clear()
        val insertAt = if (to > from) to - count else to
        children.addAll(insertAt, moved)
    }

    override fun onClear() {
        (current as HtmlElementNode).children.clear()
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `cd /home/ralph/compose-html-multiplatform && ./gradlew :core:jvmTest`
Expected: PASS — all TextApplierTest tests green.

- [ ] **Step 5: Commit**

```bash
git add core/src/commonMain/kotlin/compose/html/TextApplier.kt core/src/commonTest/kotlin/compose/html/TextApplierTest.kt
git commit -m "feat: add TextApplier (AbstractApplier<HtmlNode>) for tree construction"
```

---

### Task 5: TreeSerializer — Escaping + Void Elements

**Files:**
- Create: `serializer/src/commonMain/kotlin/compose/html/serializer/HtmlEscaping.kt`
- Create: `serializer/src/commonMain/kotlin/compose/html/serializer/VoidElements.kt`
- Create: `serializer/src/commonTest/kotlin/compose/html/serializer/HtmlEscapingTest.kt`
- Create: `serializer/src/commonTest/kotlin/compose/html/serializer/VoidElementsTest.kt`

- [ ] **Step 1: Write failing tests for escaping**

Create `serializer/src/commonTest/kotlin/compose/html/serializer/HtmlEscapingTest.kt`:

```kotlin
package compose.html.serializer

import kotlin.test.Test
import kotlin.test.assertEquals

class HtmlEscapingTest {

    @Test
    fun escapeHtmlLessThan() {
        assertEquals("&lt;script&gt;", escapeHtml("<script>"))
    }

    @Test
    fun escapeHtmlAmpersand() {
        assertEquals("Tom &amp; Jerry", escapeHtml("Tom & Jerry"))
    }

    @Test
    fun escapeHtmlNoChange() {
        assertEquals("hello world", escapeHtml("hello world"))
    }

    @Test
    fun escapeHtmlAllSpecialChars() {
        assertEquals("&lt;a&gt; &amp; &lt;b&gt;", escapeHtml("<a> & <b>"))
    }

    @Test
    fun escapeAttrQuotes() {
        assertEquals("he said &quot;hi&quot;", escapeAttr("he said \"hi\""))
    }

    @Test
    fun escapeAttrAlsoEscapesHtmlChars() {
        assertEquals("&lt;a&gt; &amp; &quot;b&quot;", escapeAttr("<a> & \"b\""))
    }

    @Test
    fun escapeEmptyString() {
        assertEquals("", escapeHtml(""))
        assertEquals("", escapeAttr(""))
    }
}
```

- [ ] **Step 2: Write failing tests for void elements**

Create `serializer/src/commonTest/kotlin/compose/html/serializer/VoidElementsTest.kt`:

```kotlin
package compose.html.serializer

import compose.html.HtmlElementNode
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VoidElementsTest {

    @Test
    fun brIsVoid() {
        assertTrue(isVoidElement(HtmlElementNode("br")))
    }

    @Test
    fun hrIsVoid() {
        assertTrue(isVoidElement(HtmlElementNode("hr")))
    }

    @Test
    fun imgIsVoid() {
        assertTrue(isVoidElement(HtmlElementNode("img")))
    }

    @Test
    fun inputIsVoid() {
        assertTrue(isVoidElement(HtmlElementNode("input")))
    }

    @Test
    fun wbrIsVoid() {
        assertTrue(isVoidElement(HtmlElementNode("wbr")))
    }

    @Test
    fun divIsNotVoid() {
        assertFalse(isVoidElement(HtmlElementNode("div")))
    }

    @Test
    fun spanIsNotVoid() {
        assertFalse(isVoidElement(HtmlElementNode("span")))
    }

    @Test
    fun svgCircleWithNoChildrenIsVoid() {
        val circle = HtmlElementNode("circle", namespace = "http://www.w3.org/2000/svg")
        assertTrue(isVoidElement(circle))
    }

    @Test
    fun svgGWithNoChildrenIsNotVoid() {
        // <g> is a container element, should not self-close even when empty
        val g = HtmlElementNode("g", namespace = "http://www.w3.org/2000/svg")
        assertFalse(isVoidElement(g))
    }

    @Test
    fun allHtmlVoidElements() {
        val voidTags = listOf("area", "base", "br", "col", "embed", "hr", "img", "input", "link", "meta", "param", "source", "track", "wbr")
        for (tag in voidTags) {
            assertTrue(isVoidElement(HtmlElementNode(tag)), "$tag should be void")
        }
    }
}
```

- [ ] **Step 3: Run tests to verify they fail**

Run: `cd /home/ralph/compose-html-multiplatform && ./gradlew :serializer:jvmTest`
Expected: FAIL — `escapeHtml`, `escapeAttr`, `isVoidElement` not found.

- [ ] **Step 4: Implement HtmlEscaping**

Create `serializer/src/commonMain/kotlin/compose/html/serializer/HtmlEscaping.kt`:

```kotlin
package compose.html.serializer

fun escapeHtml(text: String): String = buildString(text.length) {
    for (ch in text) {
        when (ch) {
            '<' -> append("&lt;")
            '>' -> append("&gt;")
            '&' -> append("&amp;")
            else -> append(ch)
        }
    }
}

fun escapeAttr(text: String): String = buildString(text.length) {
    for (ch in text) {
        when (ch) {
            '<' -> append("&lt;")
            '>' -> append("&gt;")
            '&' -> append("&amp;")
            '"' -> append("&quot;")
            else -> append(ch)
        }
    }
}
```

- [ ] **Step 5: Implement VoidElements**

Create `serializer/src/commonMain/kotlin/compose/html/serializer/VoidElements.kt`:

```kotlin
package compose.html.serializer

import compose.html.HtmlElementNode

private val HTML_VOID_ELEMENTS = setOf(
    "area", "base", "br", "col", "embed", "hr", "img",
    "input", "link", "meta", "param", "source", "track", "wbr",
)

// SVG container elements that should NOT self-close even when empty
private val SVG_CONTAINER_ELEMENTS = setOf(
    "svg", "g", "defs", "symbol", "marker", "clipPath", "mask",
    "pattern", "filter", "linearGradient", "radialGradient",
    "text", "tspan", "textPath", "a", "switch", "view",
)

fun isVoidElement(node: HtmlElementNode): Boolean {
    // HTML void elements are always void
    if (node.namespace == null && node.tagName in HTML_VOID_ELEMENTS) return true
    // SVG leaf elements (non-containers with no children) self-close
    if (node.namespace != null && node.tagName !in SVG_CONTAINER_ELEMENTS) return true
    return false
}
```

- [ ] **Step 6: Run tests to verify they pass**

Run: `cd /home/ralph/compose-html-multiplatform && ./gradlew :serializer:jvmTest`
Expected: PASS — all escaping and void element tests green.

- [ ] **Step 7: Commit**

```bash
git add serializer/src/commonMain/kotlin/compose/html/serializer/HtmlEscaping.kt serializer/src/commonMain/kotlin/compose/html/serializer/VoidElements.kt serializer/src/commonTest/kotlin/compose/html/serializer/HtmlEscapingTest.kt serializer/src/commonTest/kotlin/compose/html/serializer/VoidElementsTest.kt
git commit -m "feat: add HTML escaping and void element detection"
```

---

### Task 6: TreeSerializer

**Files:**
- Create: `serializer/src/commonMain/kotlin/compose/html/serializer/TreeSerializer.kt`
- Create: `serializer/src/commonTest/kotlin/compose/html/serializer/TreeSerializerTest.kt`

- [ ] **Step 1: Write failing tests for TreeSerializer**

Create `serializer/src/commonTest/kotlin/compose/html/serializer/TreeSerializerTest.kt`:

```kotlin
package compose.html.serializer

import compose.html.HtmlElementNode
import compose.html.HtmlTextNode
import kotlin.test.Test
import kotlin.test.assertEquals

class TreeSerializerTest {

    private val serializer = TreeSerializer(prettyPrint = false)
    private val prettySerializer = TreeSerializer(prettyPrint = true)
    private val xhtmlSerializer = TreeSerializer(prettyPrint = false, xhtmlCompatible = true)

    // --- Basic elements ---

    @Test
    fun emptyDiv() {
        val node = HtmlElementNode("div")
        assertEquals("<div></div>", serializer.serializeToString(node))
    }

    @Test
    fun divWithTextChild() {
        val node = HtmlElementNode("div").also {
            it.children.add(HtmlTextNode("hello"))
        }
        assertEquals("<div>hello</div>", serializer.serializeToString(node))
    }

    @Test
    fun nestedElements() {
        val outer = HtmlElementNode("div")
        val inner = HtmlElementNode("span").also {
            it.children.add(HtmlTextNode("text"))
        }
        outer.children.add(inner)
        assertEquals("<div><span>text</span></div>", serializer.serializeToString(node = outer))
    }

    // --- Attributes ---

    @Test
    fun elementWithAttributes() {
        val node = HtmlElementNode("a")
        node.attributes["href"] = "https://example.com"
        node.attributes["target"] = "_blank"
        assertEquals("<a href=\"https://example.com\" target=\"_blank\"></a>", serializer.serializeToString(node))
    }

    @Test
    fun elementWithClasses() {
        val node = HtmlElementNode("div")
        node.classes.addAll(listOf("a", "b", "c"))
        assertEquals("<div class=\"a b c\"></div>", serializer.serializeToString(node))
    }

    @Test
    fun elementWithInlineStyles() {
        val node = HtmlElementNode("div")
        node.inlineStyles["color"] = "red"
        node.inlineStyles["padding"] = "10px"
        val result = serializer.serializeToString(node)
        // Style entries may vary in order due to map, but should contain both
        assertTrue(result.contains("color: red"))
        assertTrue(result.contains("padding: 10px"))
    }

    @Test
    fun elementWithClassesStylesAndAttributes() {
        val node = HtmlElementNode("div")
        node.classes.add("container")
        node.inlineStyles["color"] = "red"
        node.attributes["id"] = "main"
        val result = serializer.serializeToString(node)
        assertTrue(result.contains("class=\"container\""))
        assertTrue(result.contains("style=\"color: red\""))
        assertTrue(result.contains("id=\"main\""))
    }

    @Test
    fun cssCustomPropertyPreservedInOutput() {
        val node = HtmlElementNode("div")
        node.inlineStyles["--my-color"] = "blue"
        val result = serializer.serializeToString(node)
        assertTrue(result.contains("--my-color: blue"))
    }

    @Test
    fun booleanAttributeEmptyValue() {
        val node = HtmlElementNode("input")
        node.attributes["disabled"] = ""
        assertEquals("<input disabled=\"\">", serializer.serializeToString(node))
    }

    // --- Escaping ---

    @Test
    fun textContentIsEscaped() {
        val node = HtmlElementNode("div").also {
            it.children.add(HtmlTextNode("<script>alert('xss')</script>"))
        }
        assertEquals("<div>&lt;script&gt;alert('xss')&lt;/script&gt;</div>", serializer.serializeToString(node))
    }

    @Test
    fun attributeValuesAreEscaped() {
        val node = HtmlElementNode("div")
        node.attributes["data-x"] = "a\"b"
        assertEquals("<div data-x=\"a&quot;b\"></div>", serializer.serializeToString(node))
    }

    // --- Void elements ---

    @Test
    fun htmlVoidElementSelfCloses() {
        val br = HtmlElementNode("br")
        assertEquals("<br>", serializer.serializeToString(br))
    }

    @Test
    fun htmlVoidElementXhtml() {
        val br = HtmlElementNode("br")
        assertEquals("<br />", xhtmlSerializer.serializeToString(br))
    }

    @Test
    fun imgWithAttributes() {
        val img = HtmlElementNode("img")
        img.attributes["src"] = "photo.jpg"
        img.attributes["alt"] = "A photo"
        assertEquals("<img src=\"photo.jpg\" alt=\"A photo\">", serializer.serializeToString(img))
    }

    @Test
    fun nonVoidEmptyElementGetsClosingTag() {
        val div = HtmlElementNode("div")
        assertEquals("<div></div>", serializer.serializeToString(div))
    }

    // --- SVG ---

    @Test
    fun svgCircleSelfCloses() {
        val circle = HtmlElementNode("circle", namespace = "http://www.w3.org/2000/svg")
        circle.attributes["cx"] = "50"
        circle.attributes["cy"] = "50"
        circle.attributes["r"] = "40"
        assertEquals("<circle cx=\"50\" cy=\"50\" r=\"40\">", serializer.serializeToString(circle))
    }

    @Test
    fun svgCircleXhtml() {
        val circle = HtmlElementNode("circle", namespace = "http://www.w3.org/2000/svg")
        circle.attributes["cx"] = "50"
        assertEquals("<circle cx=\"50\" />", xhtmlSerializer.serializeToString(circle))
    }

    @Test
    fun svgCamelCasePreserved() {
        val node = HtmlElementNode("clipPath", namespace = "http://www.w3.org/2000/svg")
        node.attributes["id"] = "clip1"
        // clipPath is a container, should get closing tag
        assertEquals("<clipPath id=\"clip1\"></clipPath>", serializer.serializeToString(node))
    }

    @Test
    fun svgNestedStructure() {
        val svg = HtmlElementNode("svg", namespace = "http://www.w3.org/2000/svg")
        svg.attributes["xmlns"] = "http://www.w3.org/2000/svg"
        svg.attributes["viewBox"] = "0 0 100 100"

        val circle = HtmlElementNode("circle", namespace = "http://www.w3.org/2000/svg")
        circle.attributes["cx"] = "50"
        circle.attributes["cy"] = "50"
        circle.attributes["r"] = "40"
        svg.children.add(circle)

        val result = serializer.serializeToString(svg)
        assertEquals("<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 100 100\"><circle cx=\"50\" cy=\"50\" r=\"40\"></svg>", result)
    }

    // --- Pretty printing ---

    @Test
    fun prettyPrintNested() {
        val div = HtmlElementNode("div")
        val span = HtmlElementNode("span").also {
            it.children.add(HtmlTextNode("hi"))
        }
        div.children.add(span)

        val expected = "<div>\n  <span>hi</span>\n</div>\n"
        assertEquals(expected, prettySerializer.serializeToString(div))
    }

    @Test
    fun prettyPrintTextOnly() {
        val div = HtmlElementNode("div")
        div.children.add(HtmlTextNode("hello"))

        // Text-only children: no extra newlines
        assertEquals("<div>hello</div>\n", prettySerializer.serializeToString(div))
    }

    @Test
    fun prettyPrintDeeplyNested() {
        val outer = HtmlElementNode("div")
        val middle = HtmlElementNode("div")
        val inner = HtmlElementNode("span").also {
            it.children.add(HtmlTextNode("deep"))
        }
        middle.children.add(inner)
        outer.children.add(middle)

        val expected = "<div>\n  <div>\n    <span>deep</span>\n  </div>\n</div>\n"
        assertEquals(expected, prettySerializer.serializeToString(outer))
    }

    // --- Text nodes ---

    @Test
    fun emptyTextNode() {
        val node = HtmlElementNode("div").also {
            it.children.add(HtmlTextNode(""))
        }
        assertEquals("<div></div>", serializer.serializeToString(node))
    }

    @Test
    fun adjacentTextNodes() {
        val node = HtmlElementNode("div").also {
            it.children.add(HtmlTextNode("hello "))
            it.children.add(HtmlTextNode("world"))
        }
        assertEquals("<div>hello world</div>", serializer.serializeToString(node))
    }

    // Helper
    private fun assertTrue(condition: Boolean, message: String = "") {
        kotlin.test.assertTrue(condition, message)
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `cd /home/ralph/compose-html-multiplatform && ./gradlew :serializer:jvmTest --tests "compose.html.serializer.TreeSerializerTest"`
Expected: FAIL — `TreeSerializer` not found.

- [ ] **Step 3: Implement TreeSerializer**

Create `serializer/src/commonMain/kotlin/compose/html/serializer/TreeSerializer.kt`:

```kotlin
package compose.html.serializer

import compose.html.HtmlElementNode
import compose.html.HtmlNode
import compose.html.HtmlTextNode

class TreeSerializer(
    private val prettyPrint: Boolean = true,
    private val xhtmlCompatible: Boolean = false,
    private val indent: String = "  ",
) {
    fun serialize(root: HtmlNode, out: Appendable) {
        serializeNode(root, out, depth = 0)
    }

    fun serializeToString(node: HtmlNode): String {
        return buildString { serialize(node, this) }
    }

    private fun serializeNode(node: HtmlNode, out: Appendable, depth: Int) {
        when (node) {
            is HtmlTextNode -> out.append(escapeHtml(node.text))
            is HtmlElementNode -> serializeElement(node, out, depth)
        }
    }

    private fun serializeElement(node: HtmlElementNode, out: Appendable, depth: Int) {
        if (prettyPrint) out.appendIndent(depth)

        out.append("<").append(node.tagName)

        // Classes -> class attribute
        if (node.classes.isNotEmpty()) {
            out.append(" class=\"")
            out.append(escapeAttr(node.classes.joinToString(" ")))
            out.append("\"")
        }

        // Inline styles -> style attribute
        if (node.inlineStyles.isNotEmpty()) {
            val styleStr = node.inlineStyles.entries.joinToString("; ") { (k, v) -> "$k: $v" }
            out.append(" style=\"")
            out.append(escapeAttr(styleStr))
            out.append("\"")
        }

        // Remaining attributes
        for ((key, value) in node.attributes) {
            out.append(" ").append(key).append("=\"")
            out.append(escapeAttr(value))
            out.append("\"")
        }

        // Self-closing for void/empty elements
        if (node.children.isEmpty() && isVoidElement(node)) {
            if (xhtmlCompatible) {
                out.append(" />")
            } else {
                out.append(">")
            }
            if (prettyPrint) out.append("\n")
            return
        }

        out.append(">")

        val hasElementChildren = node.children.any { it is HtmlElementNode }
        if (prettyPrint && hasElementChildren) out.append("\n")

        for (child in node.children) {
            serializeNode(child, out, depth + 1)
        }

        if (prettyPrint && hasElementChildren) out.appendIndent(depth)
        out.append("</").append(node.tagName).append(">")
        if (prettyPrint) out.append("\n")
    }

    private fun Appendable.appendIndent(depth: Int) {
        repeat(depth) { append(indent) }
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `cd /home/ralph/compose-html-multiplatform && ./gradlew :serializer:jvmTest`
Expected: PASS — all TreeSerializerTest, HtmlEscapingTest, VoidElementsTest green.

- [ ] **Step 5: Commit**

```bash
git add serializer/src/commonMain/kotlin/compose/html/serializer/TreeSerializer.kt serializer/src/commonTest/kotlin/compose/html/serializer/TreeSerializerTest.kt
git commit -m "feat: add TreeSerializer for HTML/SVG tree-to-text rendering"
```

---

### Task 7: LocalStaticRendering + composeOnce + Entry Points

**Files:**
- Create: `core/src/commonMain/kotlin/compose/html/LocalStaticRendering.kt`
- Create: `serializer/src/commonMain/kotlin/compose/html/serializer/RenderComposable.kt`

- [ ] **Step 1: Implement LocalStaticRendering**

Create `core/src/commonMain/kotlin/compose/html/LocalStaticRendering.kt`:

```kotlin
package compose.html

import androidx.compose.runtime.compositionLocalOf

val LocalStaticRendering = compositionLocalOf { true }
```

- [ ] **Step 2: Implement RenderComposable entry points**

Create `serializer/src/commonMain/kotlin/compose/html/serializer/RenderComposable.kt`:

```kotlin
package compose.html.serializer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composition
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Recomposer
import compose.html.HtmlElementNode
import compose.html.LocalStaticRendering
import compose.html.TextApplier

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

internal fun composeOnce(root: HtmlElementNode, content: @Composable () -> Unit) {
    val recomposer = Recomposer(kotlinx.coroutines.Dispatchers.Unconfined)
    val composition = Composition(TextApplier(root), recomposer)
    try {
        composition.setContent {
            CompositionLocalProvider(LocalStaticRendering provides true) {
                content()
            }
        }
    } finally {
        composition.dispose()
        recomposer.cancel()
    }
}
```

Note: The exact `Recomposer` construction may need adjustment based on the Compose runtime version. The key constraint is single-pass — `setContent` runs once and we dispose immediately. The `Dispatchers.Unconfined` avoids thread-hopping.

- [ ] **Step 3: Commit**

```bash
git add core/src/commonMain/kotlin/compose/html/LocalStaticRendering.kt serializer/src/commonMain/kotlin/compose/html/serializer/RenderComposable.kt
git commit -m "feat: add renderComposableToString/renderComposableTo entry points and composeOnce"
```

---

### Task 8: Composables — expect/actual TagElement + Text + HTML Wrappers

**Files:**
- Create: `core/src/commonMain/kotlin/compose/html/Composables.kt`
- Create: `core/src/jvmMain/kotlin/compose/html/Composables.jvm.kt`
- Create: `serializer/src/commonTest/kotlin/compose/html/serializer/RenderComposableTest.kt`

- [ ] **Step 1: Write failing end-to-end tests**

Create `serializer/src/commonTest/kotlin/compose/html/serializer/RenderComposableTest.kt`:

```kotlin
package compose.html.serializer

import compose.html.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RenderComposableTest {

    @Test
    fun renderEmptyDiv() {
        val html = renderComposableToString(prettyPrint = false) {
            Div()
        }
        assertEquals("<div></div>", html)
    }

    @Test
    fun renderDivWithText() {
        val html = renderComposableToString(prettyPrint = false) {
            Div { Text("hello") }
        }
        assertEquals("<div>hello</div>", html)
    }

    @Test
    fun renderDivWithAttributes() {
        val html = renderComposableToString(prettyPrint = false) {
            Div({ attr("data-x", "1"); classes("a", "b") }) {
                Text("content")
            }
        }
        assertEquals("<div class=\"a b\" data-x=\"1\">content</div>", html)
    }

    @Test
    fun renderDivWithStyles() {
        val html = renderComposableToString(prettyPrint = false) {
            Div({ style { property("color", "red") } }) {
                Text("styled")
            }
        }
        assertEquals("<div style=\"color: red\">styled</div>", html)
    }

    @Test
    fun renderNestedElements() {
        val html = renderComposableToString(prettyPrint = false) {
            Div {
                Span { Text("inner") }
            }
        }
        assertEquals("<div><span>inner</span></div>", html)
    }

    @Test
    fun renderMultipleChildren() {
        val html = renderComposableToString(prettyPrint = false) {
            Div {
                Span { Text("a") }
                Span { Text("b") }
            }
        }
        assertEquals("<div><span>a</span><span>b</span></div>", html)
    }

    @Test
    fun renderConditional() {
        val showExtra = true
        val html = renderComposableToString(prettyPrint = false) {
            Div {
                Text("always")
                if (showExtra) {
                    Span { Text("extra") }
                }
            }
        }
        assertEquals("<div>always<span>extra</span></div>", html)
    }

    @Test
    fun renderConditionalFalse() {
        val showExtra = false
        val html = renderComposableToString(prettyPrint = false) {
            Div {
                Text("always")
                if (showExtra) {
                    Span { Text("extra") }
                }
            }
        }
        assertEquals("<div>always</div>", html)
    }

    @Test
    fun renderList() {
        val items = listOf("x", "y", "z")
        val html = renderComposableToString(prettyPrint = false) {
            Ul {
                items.forEach { item ->
                    Li { Text(item) }
                }
            }
        }
        assertEquals("<ul><li>x</li><li>y</li><li>z</li></ul>", html)
    }

    @Test
    fun renderEscapedText() {
        val html = renderComposableToString(prettyPrint = false) {
            Div { Text("<script>alert('xss')</script>") }
        }
        assertEquals("<div>&lt;script&gt;alert('xss')&lt;/script&gt;</div>", html)
    }

    @Test
    fun renderVoidElement() {
        val html = renderComposableToString(prettyPrint = false) {
            Br()
        }
        assertEquals("<br>", html)
    }

    @Test
    fun renderPrettyPrint() {
        val html = renderComposableToString(prettyPrint = true) {
            Div {
                Span { Text("hi") }
            }
        }
        assertEquals("<div>\n  <span>hi</span>\n</div>\n", html)
    }

    @Test
    fun renderMultipleRootElements() {
        val html = renderComposableToString(prettyPrint = false) {
            Div { Text("a") }
            Div { Text("b") }
        }
        assertEquals("<div>a</div><div>b</div>", html)
    }

    @Test
    fun renderToAppendable() {
        val sb = StringBuilder()
        renderComposableTo(sb, prettyPrint = false) {
            Div { Text("streamed") }
        }
        assertEquals("<div>streamed</div>", sb.toString())
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `cd /home/ralph/compose-html-multiplatform && ./gradlew :serializer:jvmTest --tests "compose.html.serializer.RenderComposableTest"`
Expected: FAIL — `Div`, `Span`, `Text`, `Ul`, `Li`, `Br` not found.

- [ ] **Step 3: Implement expect declarations and thin wrappers**

Create `core/src/commonMain/kotlin/compose/html/Composables.kt`:

```kotlin
package compose.html

import androidx.compose.runtime.Composable

@Composable
expect fun TagElement(
    tagName: String,
    namespace: String?,
    attrs: (AttrsBuilder<*>.() -> Unit)?,
    content: @Composable () -> Unit,
)

@Composable
expect fun Text(value: String)

// --- Thin wrappers ---

@Composable
fun TagElement(
    tagName: String,
    attrs: (AttrsBuilder<*>.() -> Unit)? = null,
    content: @Composable () -> Unit = {},
) {
    TagElement(tagName, null, attrs, content)
}

@Composable fun Div(attrs: (AttrsBuilder<*>.() -> Unit)? = null, content: @Composable () -> Unit = {}) =
    TagElement("div", attrs = attrs, content = content)

@Composable fun Span(attrs: (AttrsBuilder<*>.() -> Unit)? = null, content: @Composable () -> Unit = {}) =
    TagElement("span", attrs = attrs, content = content)

@Composable fun P(attrs: (AttrsBuilder<*>.() -> Unit)? = null, content: @Composable () -> Unit = {}) =
    TagElement("p", attrs = attrs, content = content)

@Composable fun A(attrs: (AttrsBuilder<*>.() -> Unit)? = null, content: @Composable () -> Unit = {}) =
    TagElement("a", attrs = attrs, content = content)

@Composable fun H1(attrs: (AttrsBuilder<*>.() -> Unit)? = null, content: @Composable () -> Unit = {}) =
    TagElement("h1", attrs = attrs, content = content)

@Composable fun H2(attrs: (AttrsBuilder<*>.() -> Unit)? = null, content: @Composable () -> Unit = {}) =
    TagElement("h2", attrs = attrs, content = content)

@Composable fun H3(attrs: (AttrsBuilder<*>.() -> Unit)? = null, content: @Composable () -> Unit = {}) =
    TagElement("h3", attrs = attrs, content = content)

@Composable fun H4(attrs: (AttrsBuilder<*>.() -> Unit)? = null, content: @Composable () -> Unit = {}) =
    TagElement("h4", attrs = attrs, content = content)

@Composable fun H5(attrs: (AttrsBuilder<*>.() -> Unit)? = null, content: @Composable () -> Unit = {}) =
    TagElement("h5", attrs = attrs, content = content)

@Composable fun H6(attrs: (AttrsBuilder<*>.() -> Unit)? = null, content: @Composable () -> Unit = {}) =
    TagElement("h6", attrs = attrs, content = content)

@Composable fun Ul(attrs: (AttrsBuilder<*>.() -> Unit)? = null, content: @Composable () -> Unit = {}) =
    TagElement("ul", attrs = attrs, content = content)

@Composable fun Ol(attrs: (AttrsBuilder<*>.() -> Unit)? = null, content: @Composable () -> Unit = {}) =
    TagElement("ol", attrs = attrs, content = content)

@Composable fun Li(attrs: (AttrsBuilder<*>.() -> Unit)? = null, content: @Composable () -> Unit = {}) =
    TagElement("li", attrs = attrs, content = content)

@Composable fun Img(attrs: (AttrsBuilder<*>.() -> Unit)? = null) =
    TagElement("img", attrs = attrs) {}

@Composable fun Br(attrs: (AttrsBuilder<*>.() -> Unit)? = null) =
    TagElement("br", attrs = attrs) {}

@Composable fun Hr(attrs: (AttrsBuilder<*>.() -> Unit)? = null) =
    TagElement("hr", attrs = attrs) {}

@Composable fun Input(attrs: (AttrsBuilder<*>.() -> Unit)? = null) =
    TagElement("input", attrs = attrs) {}

@Composable fun Button(attrs: (AttrsBuilder<*>.() -> Unit)? = null, content: @Composable () -> Unit = {}) =
    TagElement("button", attrs = attrs, content = content)

@Composable fun Form(attrs: (AttrsBuilder<*>.() -> Unit)? = null, content: @Composable () -> Unit = {}) =
    TagElement("form", attrs = attrs, content = content)

@Composable fun Label(attrs: (AttrsBuilder<*>.() -> Unit)? = null, content: @Composable () -> Unit = {}) =
    TagElement("label", attrs = attrs, content = content)

@Composable fun Table(attrs: (AttrsBuilder<*>.() -> Unit)? = null, content: @Composable () -> Unit = {}) =
    TagElement("table", attrs = attrs, content = content)

@Composable fun Thead(attrs: (AttrsBuilder<*>.() -> Unit)? = null, content: @Composable () -> Unit = {}) =
    TagElement("thead", attrs = attrs, content = content)

@Composable fun Tbody(attrs: (AttrsBuilder<*>.() -> Unit)? = null, content: @Composable () -> Unit = {}) =
    TagElement("tbody", attrs = attrs, content = content)

@Composable fun Tr(attrs: (AttrsBuilder<*>.() -> Unit)? = null, content: @Composable () -> Unit = {}) =
    TagElement("tr", attrs = attrs, content = content)

@Composable fun Th(attrs: (AttrsBuilder<*>.() -> Unit)? = null, content: @Composable () -> Unit = {}) =
    TagElement("th", attrs = attrs, content = content)

@Composable fun Td(attrs: (AttrsBuilder<*>.() -> Unit)? = null, content: @Composable () -> Unit = {}) =
    TagElement("td", attrs = attrs, content = content)

@Composable fun Nav(attrs: (AttrsBuilder<*>.() -> Unit)? = null, content: @Composable () -> Unit = {}) =
    TagElement("nav", attrs = attrs, content = content)

@Composable fun Header(attrs: (AttrsBuilder<*>.() -> Unit)? = null, content: @Composable () -> Unit = {}) =
    TagElement("header", attrs = attrs, content = content)

@Composable fun Footer(attrs: (AttrsBuilder<*>.() -> Unit)? = null, content: @Composable () -> Unit = {}) =
    TagElement("footer", attrs = attrs, content = content)

@Composable fun Main(attrs: (AttrsBuilder<*>.() -> Unit)? = null, content: @Composable () -> Unit = {}) =
    TagElement("main", attrs = attrs, content = content)

@Composable fun Section(attrs: (AttrsBuilder<*>.() -> Unit)? = null, content: @Composable () -> Unit = {}) =
    TagElement("section", attrs = attrs, content = content)

@Composable fun Article(attrs: (AttrsBuilder<*>.() -> Unit)? = null, content: @Composable () -> Unit = {}) =
    TagElement("article", attrs = attrs, content = content)

@Composable fun Aside(attrs: (AttrsBuilder<*>.() -> Unit)? = null, content: @Composable () -> Unit = {}) =
    TagElement("aside", attrs = attrs, content = content)

@Composable fun Pre(attrs: (AttrsBuilder<*>.() -> Unit)? = null, content: @Composable () -> Unit = {}) =
    TagElement("pre", attrs = attrs, content = content)

@Composable fun Code(attrs: (AttrsBuilder<*>.() -> Unit)? = null, content: @Composable () -> Unit = {}) =
    TagElement("code", attrs = attrs, content = content)

@Composable fun Em(attrs: (AttrsBuilder<*>.() -> Unit)? = null, content: @Composable () -> Unit = {}) =
    TagElement("em", attrs = attrs, content = content)

@Composable fun Strong(attrs: (AttrsBuilder<*>.() -> Unit)? = null, content: @Composable () -> Unit = {}) =
    TagElement("strong", attrs = attrs, content = content)

@Composable fun Small(attrs: (AttrsBuilder<*>.() -> Unit)? = null, content: @Composable () -> Unit = {}) =
    TagElement("small", attrs = attrs, content = content)
```

- [ ] **Step 4: Implement JVM actual**

Create `core/src/jvmMain/kotlin/compose/html/Composables.jvm.kt`:

```kotlin
package compose.html

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode

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
        update = {
            set(value) { text = it }
        },
    )
}
```

- [ ] **Step 5: Run tests to verify they pass**

Run: `cd /home/ralph/compose-html-multiplatform && ./gradlew :serializer:jvmTest --tests "compose.html.serializer.RenderComposableTest"`
Expected: PASS — all RenderComposableTest tests green.

Note: If `composeOnce` has issues with the `Recomposer` construction, adjust the implementation. The key is: create composition, set content, dispose. No recomposition loop.

- [ ] **Step 6: Commit**

```bash
git add core/src/commonMain/kotlin/compose/html/Composables.kt core/src/jvmMain/kotlin/compose/html/Composables.jvm.kt serializer/src/commonTest/kotlin/compose/html/serializer/RenderComposableTest.kt
git commit -m "feat: add expect/actual composables (TagElement, Text) and HTML wrappers (Div, Span, etc.)"
```

---

### Task 9: SVG Composables

**Files:**
- Create: `svg/src/commonMain/kotlin/compose/html/svg/Svg.kt`
- Create: `svg/src/commonMain/kotlin/compose/html/svg/SvgShapes.kt`
- Create: `svg/src/commonMain/kotlin/compose/html/svg/SvgStructure.kt`
- Create: `svg/src/commonMain/kotlin/compose/html/svg/SvgText.kt`
- Create: `svg/src/commonMain/kotlin/compose/html/svg/SvgGradients.kt`
- Create: `svg/src/commonMain/kotlin/compose/html/svg/SvgAnimation.kt`
- Create: `svg/src/commonMain/kotlin/compose/html/svg/SvgMisc.kt`
- Create: `svg/src/commonMain/kotlin/compose/html/svg/SvgAttrs.kt`
- Create: `svg/src/commonTest/kotlin/compose/html/svg/SvgRenderTest.kt`

- [ ] **Step 1: Write failing SVG rendering tests**

Create `svg/src/commonTest/kotlin/compose/html/svg/SvgRenderTest.kt`:

```kotlin
package compose.html.svg

import compose.html.AttrsBuilder
import compose.html.serializer.renderComposableToString
import compose.html.serializer.renderComposableTo
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SvgRenderTest {

    @Test
    fun svgRoot() {
        val result = renderComposableToString(prettyPrint = false) {
            Svg(viewBox = "0 0 100 100")
        }
        assertTrue(result.contains("<svg"))
        assertTrue(result.contains("xmlns=\"http://www.w3.org/2000/svg\""))
        assertTrue(result.contains("viewBox=\"0 0 100 100\""))
        assertTrue(result.contains("</svg>"))
    }

    @Test
    fun circle() {
        val result = renderComposableToString(prettyPrint = false) {
            Svg { Circle(50, 50, 40) }
        }
        assertTrue(result.contains("<circle"))
        assertTrue(result.contains("cx=\"50\""))
        assertTrue(result.contains("cy=\"50\""))
        assertTrue(result.contains("r=\"40\""))
    }

    @Test
    fun rect() {
        val result = renderComposableToString(prettyPrint = false) {
            Svg { Rect(10, 20, 100, 50) }
        }
        assertTrue(result.contains("<rect"))
        assertTrue(result.contains("x=\"10\""))
        assertTrue(result.contains("y=\"20\""))
        assertTrue(result.contains("width=\"100\""))
        assertTrue(result.contains("height=\"50\""))
    }

    @Test
    fun path() {
        val result = renderComposableToString(prettyPrint = false) {
            Svg { Path("M 10 10 L 90 90") }
        }
        assertTrue(result.contains("<path"))
        assertTrue(result.contains("d=\"M 10 10 L 90 90\""))
    }

    @Test
    fun ellipse() {
        val result = renderComposableToString(prettyPrint = false) {
            Svg { Ellipse(50, 50, 40, 20) }
        }
        assertTrue(result.contains("<ellipse"))
        assertTrue(result.contains("cx=\"50\""))
        assertTrue(result.contains("rx=\"40\""))
        assertTrue(result.contains("ry=\"20\""))
    }

    @Test
    fun line() {
        val result = renderComposableToString(prettyPrint = false) {
            Svg { Line(0, 0, 100, 100) }
        }
        assertTrue(result.contains("<line"))
        assertTrue(result.contains("x1=\"0\""))
        assertTrue(result.contains("y1=\"0\""))
        assertTrue(result.contains("x2=\"100\""))
        assertTrue(result.contains("y2=\"100\""))
    }

    @Test
    fun polygon() {
        val result = renderComposableToString(prettyPrint = false) {
            Svg { Polygon("0,100 50,25 50,75 100,0") }
        }
        assertTrue(result.contains("<polygon"))
        assertTrue(result.contains("points=\"0,100 50,25 50,75 100,0\""))
    }

    @Test
    fun polyline() {
        val result = renderComposableToString(prettyPrint = false) {
            Svg { Polyline("0,0 50,50 100,0") }
        }
        assertTrue(result.contains("<polyline"))
        assertTrue(result.contains("points=\"0,0 50,50 100,0\""))
    }

    @Test
    fun gGrouping() {
        val result = renderComposableToString(prettyPrint = false) {
            Svg {
                G({ attr("transform", "translate(10,10)") }) {
                    Circle(0, 0, 5)
                }
            }
        }
        assertTrue(result.contains("<g"))
        assertTrue(result.contains("</g>"))
        assertTrue(result.contains("<circle"))
    }

    @Test
    fun defsAndLinearGradient() {
        val result = renderComposableToString(prettyPrint = false) {
            Svg {
                Defs {
                    LinearGradient("grad1") {
                        Stop({ attr("offset", "0%"); attr("stop-color", "red") })
                        Stop({ attr("offset", "100%"); attr("stop-color", "blue") })
                    }
                }
            }
        }
        assertTrue(result.contains("<defs>"))
        assertTrue(result.contains("<linearGradient"))
        assertTrue(result.contains("id=\"grad1\""))
        assertTrue(result.contains("<stop"))
        assertTrue(result.contains("</linearGradient>"))
        assertTrue(result.contains("</defs>"))
    }

    @Test
    fun camelCaseElementNames() {
        val result = renderComposableToString(prettyPrint = false) {
            Svg {
                ClipPath("c1") {}
                LinearGradient("g1") {}
                RadialGradient("g2") {}
            }
        }
        assertTrue(result.contains("<clipPath"))
        assertTrue(result.contains("</clipPath>"))
        assertTrue(result.contains("<linearGradient"))
        assertTrue(result.contains("<radialGradient"))
    }

    @Test
    fun svgText() {
        val result = renderComposableToString(prettyPrint = false) {
            Svg {
                SvgText({ attr("x", "10"); attr("y", "20") }) {
                    compose.html.Text("Hello SVG")
                }
            }
        }
        assertTrue(result.contains("<text"))
        assertTrue(result.contains("Hello SVG"))
        assertTrue(result.contains("</text>"))
    }

    @Test
    fun fillAndStrokeAttrs() {
        val result = renderComposableToString(prettyPrint = false) {
            Svg {
                Circle(50, 50, 40, attrs = {
                    fill("blue")
                    stroke("black")
                    strokeWidth(2)
                })
            }
        }
        assertTrue(result.contains("fill=\"blue\""))
        assertTrue(result.contains("stroke=\"black\""))
        assertTrue(result.contains("stroke-width=\"2\""))
    }

    @Test
    fun nestedSvgStructure() {
        val result = renderComposableToString(prettyPrint = false) {
            Svg(viewBox = "0 0 200 200") {
                Defs {
                    LinearGradient("bg") {
                        Stop({ attr("offset", "0%"); attr("stop-color", "#fff") })
                    }
                }
                G {
                    Rect(0, 0, 200, 200, { fill("url(#bg)") })
                    Circle(100, 100, 50, { stroke("red"); strokeWidth(3) })
                }
            }
        }
        assertTrue(result.contains("<svg"))
        assertTrue(result.contains("<defs>"))
        assertTrue(result.contains("<linearGradient"))
        assertTrue(result.contains("<g>"))
        assertTrue(result.contains("<rect"))
        assertTrue(result.contains("<circle"))
    }

    @Test
    fun use() {
        val result = renderComposableToString(prettyPrint = false) {
            Svg {
                Defs {
                    Symbol("icon") {
                        Circle(10, 10, 5)
                    }
                }
                Use({ attr("href", "#icon") })
            }
        }
        assertTrue(result.contains("<symbol"))
        assertTrue(result.contains("id=\"icon\""))
        assertTrue(result.contains("<use"))
        assertTrue(result.contains("href=\"#icon\""))
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `cd /home/ralph/compose-html-multiplatform && ./gradlew :svg:jvmTest`
Expected: FAIL — `Svg`, `Circle`, etc. not found.

- [ ] **Step 3: Implement SVG root and SvgAttrs**

Create `svg/src/commonMain/kotlin/compose/html/svg/Svg.kt`:

```kotlin
package compose.html.svg

import androidx.compose.runtime.Composable
import compose.html.AttrsBuilder
import compose.html.TagElement

const val SVG_NS = "http://www.w3.org/2000/svg"

@Composable
fun Svg(
    viewBox: String? = null,
    attrs: (AttrsBuilder<*>.() -> Unit)? = null,
    content: @Composable () -> Unit = {},
) {
    TagElement("svg", SVG_NS, attrs = {
        attr("xmlns", SVG_NS)
        viewBox?.let { attr("viewBox", it) }
        attrs?.invoke(this)
    }, content = content)
}

@Composable
fun SvgElement(
    tagName: String,
    attrs: (AttrsBuilder<*>.() -> Unit)? = null,
    content: @Composable () -> Unit = {},
) {
    TagElement(tagName, SVG_NS, attrs, content)
}
```

Create `svg/src/commonMain/kotlin/compose/html/svg/SvgAttrs.kt`:

```kotlin
package compose.html.svg

import compose.html.AttrsBuilder

fun AttrsBuilder<*>.fill(value: String) = attr("fill", value)
fun AttrsBuilder<*>.stroke(value: String) = attr("stroke", value)
fun AttrsBuilder<*>.strokeWidth(value: Number) = attr("stroke-width", value.toString())
fun AttrsBuilder<*>.transform(value: String) = attr("transform", value)
fun AttrsBuilder<*>.opacity(value: Number) = attr("opacity", value.toString())
fun AttrsBuilder<*>.fillOpacity(value: Number) = attr("fill-opacity", value.toString())
fun AttrsBuilder<*>.strokeOpacity(value: Number) = attr("stroke-opacity", value.toString())
fun AttrsBuilder<*>.fillRule(value: String) = attr("fill-rule", value)
fun AttrsBuilder<*>.strokeLinecap(value: String) = attr("stroke-linecap", value)
fun AttrsBuilder<*>.strokeLinejoin(value: String) = attr("stroke-linejoin", value)
fun AttrsBuilder<*>.strokeDasharray(value: String) = attr("stroke-dasharray", value)
fun AttrsBuilder<*>.strokeDashoffset(value: Number) = attr("stroke-dashoffset", value.toString())
```

- [ ] **Step 4: Implement SVG shapes**

Create `svg/src/commonMain/kotlin/compose/html/svg/SvgShapes.kt`:

```kotlin
package compose.html.svg

import androidx.compose.runtime.Composable
import compose.html.AttrsBuilder

@Composable
fun Circle(
    cx: Number, cy: Number, r: Number,
    attrs: (AttrsBuilder<*>.() -> Unit)? = null,
) {
    SvgElement("circle", attrs = {
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
    SvgElement("rect", attrs = {
        attr("x", x.toString())
        attr("y", y.toString())
        attr("width", width.toString())
        attr("height", height.toString())
        attrs?.invoke(this)
    })
}

@Composable
fun Ellipse(
    cx: Number, cy: Number, rx: Number, ry: Number,
    attrs: (AttrsBuilder<*>.() -> Unit)? = null,
) {
    SvgElement("ellipse", attrs = {
        attr("cx", cx.toString())
        attr("cy", cy.toString())
        attr("rx", rx.toString())
        attr("ry", ry.toString())
        attrs?.invoke(this)
    })
}

@Composable
fun Line(
    x1: Number, y1: Number, x2: Number, y2: Number,
    attrs: (AttrsBuilder<*>.() -> Unit)? = null,
) {
    SvgElement("line", attrs = {
        attr("x1", x1.toString())
        attr("y1", y1.toString())
        attr("x2", x2.toString())
        attr("y2", y2.toString())
        attrs?.invoke(this)
    })
}

@Composable
fun Path(
    d: String,
    attrs: (AttrsBuilder<*>.() -> Unit)? = null,
) {
    SvgElement("path", attrs = {
        attr("d", d)
        attrs?.invoke(this)
    })
}

@Composable
fun Polygon(
    points: String,
    attrs: (AttrsBuilder<*>.() -> Unit)? = null,
) {
    SvgElement("polygon", attrs = {
        attr("points", points)
        attrs?.invoke(this)
    })
}

@Composable
fun Polyline(
    points: String,
    attrs: (AttrsBuilder<*>.() -> Unit)? = null,
) {
    SvgElement("polyline", attrs = {
        attr("points", points)
        attrs?.invoke(this)
    })
}
```

- [ ] **Step 5: Implement SVG structure elements**

Create `svg/src/commonMain/kotlin/compose/html/svg/SvgStructure.kt`:

```kotlin
package compose.html.svg

import androidx.compose.runtime.Composable
import compose.html.AttrsBuilder

@Composable
fun G(
    attrs: (AttrsBuilder<*>.() -> Unit)? = null,
    content: @Composable () -> Unit = {},
) {
    SvgElement("g", attrs, content)
}

@Composable
fun Defs(
    attrs: (AttrsBuilder<*>.() -> Unit)? = null,
    content: @Composable () -> Unit = {},
) {
    SvgElement("defs", attrs, content)
}

@Composable
fun Symbol(
    id: String,
    attrs: (AttrsBuilder<*>.() -> Unit)? = null,
    content: @Composable () -> Unit = {},
) {
    SvgElement("symbol", attrs = {
        attr("id", id)
        attrs?.invoke(this)
    }, content = content)
}

@Composable
fun Use(
    attrs: (AttrsBuilder<*>.() -> Unit)? = null,
    content: @Composable () -> Unit = {},
) {
    SvgElement("use", attrs, content)
}

@Composable
fun ClipPath(
    id: String,
    attrs: (AttrsBuilder<*>.() -> Unit)? = null,
    content: @Composable () -> Unit = {},
) {
    SvgElement("clipPath", attrs = {
        attr("id", id)
        attrs?.invoke(this)
    }, content = content)
}

@Composable
fun Mask(
    id: String? = null,
    attrs: (AttrsBuilder<*>.() -> Unit)? = null,
    content: @Composable () -> Unit = {},
) {
    SvgElement("mask", attrs = {
        id?.let { attr("id", it) }
        attrs?.invoke(this)
    }, content = content)
}

@Composable
fun View(
    id: String,
    viewBox: String,
    attrs: (AttrsBuilder<*>.() -> Unit)? = null,
) {
    SvgElement("view", attrs = {
        attr("id", id)
        attr("viewBox", viewBox)
        attrs?.invoke(this)
    })
}

@Composable
fun Switch(
    attrs: (AttrsBuilder<*>.() -> Unit)? = null,
    content: @Composable () -> Unit = {},
) {
    SvgElement("switch", attrs, content)
}
```

- [ ] **Step 6: Implement SVG text elements**

Create `svg/src/commonMain/kotlin/compose/html/svg/SvgText.kt`:

```kotlin
package compose.html.svg

import androidx.compose.runtime.Composable
import compose.html.AttrsBuilder

@Composable
fun SvgText(
    attrs: (AttrsBuilder<*>.() -> Unit)? = null,
    content: @Composable () -> Unit = {},
) {
    SvgElement("text", attrs, content)
}

@Composable
fun TextPath(
    attrs: (AttrsBuilder<*>.() -> Unit)? = null,
    content: @Composable () -> Unit = {},
) {
    SvgElement("textPath", attrs, content)
}

@Composable
fun Tspan(
    attrs: (AttrsBuilder<*>.() -> Unit)? = null,
    content: @Composable () -> Unit = {},
) {
    SvgElement("tspan", attrs, content)
}
```

- [ ] **Step 7: Implement SVG gradient elements**

Create `svg/src/commonMain/kotlin/compose/html/svg/SvgGradients.kt`:

```kotlin
package compose.html.svg

import androidx.compose.runtime.Composable
import compose.html.AttrsBuilder

@Composable
fun LinearGradient(
    id: String,
    attrs: (AttrsBuilder<*>.() -> Unit)? = null,
    content: @Composable () -> Unit = {},
) {
    SvgElement("linearGradient", attrs = {
        attr("id", id)
        attrs?.invoke(this)
    }, content = content)
}

@Composable
fun RadialGradient(
    id: String,
    attrs: (AttrsBuilder<*>.() -> Unit)? = null,
    content: @Composable () -> Unit = {},
) {
    SvgElement("radialGradient", attrs = {
        attr("id", id)
        attrs?.invoke(this)
    }, content = content)
}

@Composable
fun Stop(
    attrs: (AttrsBuilder<*>.() -> Unit)? = null,
) {
    SvgElement("stop", attrs)
}

@Composable
fun Pattern(
    id: String,
    attrs: (AttrsBuilder<*>.() -> Unit)? = null,
    content: @Composable () -> Unit = {},
) {
    SvgElement("pattern", attrs = {
        attr("id", id)
        attrs?.invoke(this)
    }, content = content)
}
```

- [ ] **Step 8: Implement SVG animation elements**

Create `svg/src/commonMain/kotlin/compose/html/svg/SvgAnimation.kt`:

```kotlin
package compose.html.svg

import androidx.compose.runtime.Composable
import compose.html.AttrsBuilder

@Composable
fun Animate(attrs: (AttrsBuilder<*>.() -> Unit)? = null) {
    SvgElement("animate", attrs)
}

@Composable
fun AnimateMotion(
    attrs: (AttrsBuilder<*>.() -> Unit)? = null,
    content: @Composable () -> Unit = {},
) {
    SvgElement("animateMotion", attrs, content)
}

@Composable
fun AnimateTransform(attrs: (AttrsBuilder<*>.() -> Unit)? = null) {
    SvgElement("animateTransform", attrs)
}

@Composable
fun Set(attrs: (AttrsBuilder<*>.() -> Unit)? = null) {
    SvgElement("set", attrs)
}

@Composable
fun Mpath(attrs: (AttrsBuilder<*>.() -> Unit)? = null) {
    SvgElement("mpath", attrs)
}
```

- [ ] **Step 9: Implement SVG misc elements**

Create `svg/src/commonMain/kotlin/compose/html/svg/SvgMisc.kt`:

```kotlin
package compose.html.svg

import androidx.compose.runtime.Composable
import compose.html.AttrsBuilder

@Composable
fun Image(attrs: (AttrsBuilder<*>.() -> Unit)? = null) {
    SvgElement("image", attrs)
}

@Composable
fun Filter(
    id: String? = null,
    attrs: (AttrsBuilder<*>.() -> Unit)? = null,
    content: @Composable () -> Unit = {},
) {
    SvgElement("filter", attrs = {
        id?.let { attr("id", it) }
        attrs?.invoke(this)
    }, content = content)
}

@Composable
fun Marker(
    id: String? = null,
    attrs: (AttrsBuilder<*>.() -> Unit)? = null,
    content: @Composable () -> Unit = {},
) {
    SvgElement("marker", attrs = {
        id?.let { attr("id", it) }
        attrs?.invoke(this)
    }, content = content)
}

@Composable
fun Title(content: @Composable () -> Unit = {}) {
    SvgElement("title", content = content)
}

@Composable
fun Desc(content: @Composable () -> Unit = {}) {
    SvgElement("desc", content = content)
}

@Composable
fun SvgA(
    attrs: (AttrsBuilder<*>.() -> Unit)? = null,
    content: @Composable () -> Unit = {},
) {
    SvgElement("a", attrs, content)
}
```

- [ ] **Step 10: Run tests to verify they pass**

Run: `cd /home/ralph/compose-html-multiplatform && ./gradlew :svg:jvmTest`
Expected: PASS — all SvgRenderTest tests green.

- [ ] **Step 11: Commit**

```bash
git add svg/src/commonMain/kotlin/compose/html/svg/ svg/src/commonTest/kotlin/compose/html/svg/SvgRenderTest.kt
git commit -m "feat: add all 35 SVG composables with attribute extensions and rendering tests"
```

---

### Task 10: Full Integration Test + Final Verification

**Files:**
- Modify: `core/src/commonTest/kotlin/compose/html/RenderComposableTest.kt` (add integration tests)

- [ ] **Step 1: Add full integration tests combining HTML + SVG + streaming**

Append to `core/src/commonTest/kotlin/compose/html/RenderComposableTest.kt` (or create a separate file if cleaner):

Note: These tests require the svg module as a test dependency. If that's not set up, add `implementation(project(":svg"))` to `core`'s `commonTest` dependencies. Alternatively, create a top-level `integration-test` module. For simplicity, add these tests to `svg/src/commonTest/kotlin/compose/html/svg/SvgRenderTest.kt` since that module already depends on both `:core` and `:serializer`.

Add to the existing `SvgRenderTest.kt` (imports for `renderComposableTo` and `compose.html.*` HTML composables are already present via the existing imports):

```kotlin
    @Test
    fun fullHtmlPageWithSvg() {
        val html = renderComposableToString(prettyPrint = false) {
            Div({ classes("page") }) {
                compose.html.H1 { compose.html.Text("Chart") }
                Svg(viewBox = "0 0 200 100") {
                    Rect(0, 0, 200, 100, { fill("#eee") })
                    Circle(100, 50, 30, { fill("blue"); stroke("navy"); strokeWidth(2) })
                }
                compose.html.P { compose.html.Text("A simple chart.") }
            }
        }
        assertTrue(html.contains("<div class=\"page\">"))
        assertTrue(html.contains("<h1>Chart</h1>"))
        assertTrue(html.contains("<svg"))
        assertTrue(html.contains("<circle"))
        assertTrue(html.contains("fill=\"blue\""))
        assertTrue(html.contains("<p>A simple chart.</p>"))
    }

    @Test
    fun renderToAppendable() {
        val sb = StringBuilder()
        renderComposableTo(sb, prettyPrint = false) {
            Svg(viewBox = "0 0 10 10") {
                Circle(5, 5, 3)
            }
        }
        assertTrue(sb.toString().contains("<svg"))
        assertTrue(sb.toString().contains("<circle"))
    }

    @Test
    fun xhtmlCompatibleOutput() {
        val html = renderComposableToString(prettyPrint = false, xhtmlCompatible = true) {
            Svg {
                Circle(10, 10, 5)
            }
        }
        assertTrue(html.contains("<circle cx=\"10\" cy=\"10\" r=\"5\" />"))
    }
```

- [ ] **Step 2: Run all tests across all modules**

Run: `cd /home/ralph/compose-html-multiplatform && ./gradlew jvmTest`
Expected: PASS — all tests across core, serializer, and svg modules.

- [ ] **Step 3: Commit**

```bash
git add svg/src/commonTest/kotlin/compose/html/svg/SvgRenderTest.kt
git commit -m "test: add full integration tests combining HTML and SVG rendering"
```

- [ ] **Step 4: Run JS tests**

Run: `cd /home/ralph/compose-html-multiplatform && ./gradlew jsTest`
Expected: PASS (or skip if JS actual is not implemented yet — Task 11 handles that).

---

### Task 11: JS Actual (Dual-Path Dispatch)

**Files:**
- Create: `core/src/jsMain/kotlin/compose/html/Composables.js.kt`

This task wires up the JS target so it can use both the existing DOM path (interactive) and the text path (static rendering via `renderComposableToString`).

- [ ] **Step 1: Implement JS actual with dual-path dispatch**

Create `core/src/jsMain/kotlin/compose/html/Composables.js.kt`:

```kotlin
package compose.html

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode

@Composable
actual fun TagElement(
    tagName: String,
    namespace: String?,
    attrs: (AttrsBuilder<*>.() -> Unit)?,
    content: @Composable () -> Unit,
) {
    // Static rendering path — same as JVM
    // The interactive DOM path (DomApplier) is a future task when porting
    // the existing Compose HTML JS code into this fork
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
        update = {
            set(value) { text = it }
        },
    )
}
```

Note: This implements the static text rendering path for JS. The interactive DOM path (using `DomApplier`, `DomNodeWrapper`, etc.) would be added later when porting the existing Compose HTML JS internals into this fork. For now, JS gets the same text rendering capability as JVM.

- [ ] **Step 2: Run JS tests**

Run: `cd /home/ralph/compose-html-multiplatform && ./gradlew jsTest`
Expected: PASS — all commonTest tests run on JS target.

- [ ] **Step 3: Run all tests on all targets**

Run: `cd /home/ralph/compose-html-multiplatform && ./gradlew check`
Expected: PASS — JVM and JS tests all green.

- [ ] **Step 4: Commit**

```bash
git add core/src/jsMain/kotlin/compose/html/Composables.js.kt
git commit -m "feat: add JS actual composables (static text rendering path)"
```

---

## Summary

| Task | What it builds | Tests |
|------|---------------|-------|
| 1 | Gradle scaffolding (3 modules, KMP, Compose) | `./gradlew projects` |
| 2 | `HtmlNode` sealed hierarchy | 9 unit tests |
| 3 | `AttrsBuilder` interface + `CommonAttrsScope` | 10 unit tests |
| 4 | `TextApplier` (AbstractApplier) | 6 unit tests |
| 5 | Escaping + void element detection | 17 unit tests |
| 6 | `TreeSerializer` | 18 unit tests |
| 7 | `LocalStaticRendering` + `composeOnce` + entry points | (tested via Task 8) |
| 8 | `expect`/`actual` composables + HTML wrappers | 14 end-to-end tests |
| 9 | All 35 SVG composables | 16 SVG rendering tests |
| 10 | Full integration (HTML + SVG combined) | 3 integration tests |
| 11 | JS actual (static path) | JS target runs all commonTest |
