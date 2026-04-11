# Compose HTML Multiplatform

A Kotlin Multiplatform fork of [JetBrains Compose HTML](https://github.com/JetBrains/compose-multiplatform/tree/master/html) that lets you render the same composable UI to:

- **Static HTML/SVG strings** (JVM + JS) — for server-side rendering, static site generation, email templates
- **Interactive browser DOM** (JS) — via Compose HTML's existing `DomApplier`, unchanged
- **`org.w3c.dom.Document`** (JVM) — for XML/XHTML pipelines, XPath queries, PDF renderers

Write once, render everywhere.

```kotlin
// Same composable code renders to all three backends
@Composable
fun Greeting(name: String) {
    Div({ classes("greeting") }) {
        H1 { Text("Hello, $name!") }
        P { Text("Welcome to Compose HTML Multiplatform.") }
    }
}
```

## Why?

[Compose HTML](https://github.com/JetBrains/compose-multiplatform/tree/master/html) is Kotlin/JS-only — you can't use it for server-side rendering, static site generation, or any JVM workflow. This fork makes it multiplatform by adding a lightweight `TextApplier` that builds an in-memory HTML tree, which can then be serialized to a string or converted to a W3C DOM `Document`.

The JS target keeps the original `DomApplier` for interactive rendering — a `LocalStaticRendering` composition local switches between paths.

## Modules

| Module | Targets | Purpose |
|--------|---------|---------|
| `core` | JVM, JS | Composables (`Div`, `Span`, `H1`, ...), `HtmlNode` tree, `TextApplier`, JS DOM bridge, `renderComposableToDocument` (JVM), `renderComposable(Element)` (JS) |
| `serializer` | JVM, JS | `TreeSerializer` + `renderComposableToString` / `renderComposableTo` entry points |
| `svg` | JVM, JS | ~35 SVG composables: `Svg`, `Circle`, `Rect`, `Path`, `G`, `Defs`, `LinearGradient`, and more |

## Installation

Published to GitHub Packages at `https://maven.pkg.github.com/helico-tech/compose-html-multiplatform`.

### `settings.gradle.kts`

```kotlin
dependencyResolutionManagement {
    repositories {
        mavenCentral()
        maven {
            url = uri("https://maven.pkg.github.com/helico-tech/compose-html-multiplatform")
            credentials {
                username = providers.gradleProperty("gpr.user").orNull
                    ?: System.getenv("GITHUB_ACTOR")
                password = providers.gradleProperty("gpr.key").orNull
                    ?: System.getenv("GITHUB_TOKEN")
            }
        }
    }
}
```

**Note:** GitHub Packages requires authentication even for public packages. Consumers need any GitHub personal access token with `read:packages` scope — they don't need access to the repo itself.

### `build.gradle.kts`

```kotlin
plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose") version "1.10.3"
    id("org.jetbrains.kotlin.plugin.compose") version "2.3.20"
}

kotlin {
    jvm()
    js(IR) { browser() }

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation("nl.helico.compose:core:0.1.0")
            implementation("nl.helico.compose:serializer:0.1.0")
            implementation("nl.helico.compose:svg:0.1.0") // optional, for SVG
        }
    }
}
```

## Usage

### Static HTML string (JVM or JS)

```kotlin
import compose.html.*
import compose.html.serializer.renderComposableToString

fun main() {
    val html = renderComposableToString {
        Div({ classes("container") }) {
            H1 { Text("Hello World") }
            P({ style { property("color", "red") } }) {
                Text("Static HTML from Compose.")
            }
            Ul {
                listOf("apple", "banana", "cherry").forEach {
                    Li { Text(it) }
                }
            }
        }
    }
    println(html)
}
```

Output:

```html
<div class="container">
  <h1>Hello World</h1>
  <p style="color: red">Static HTML from Compose.</p>
  <ul>
    <li>apple</li>
    <li>banana</li>
    <li>cherry</li>
  </ul>
</div>
```

### Streaming to an `Appendable`

```kotlin
import compose.html.serializer.renderComposableTo
import java.io.FileWriter

FileWriter("output.html").use { writer ->
    renderComposableTo(writer, prettyPrint = true) {
        Div { Text("Streamed directly to disk") }
    }
}
```

### SVG rendering

```kotlin
import compose.html.*
import compose.html.svg.*
import compose.html.serializer.renderComposableToString

val svg = renderComposableToString {
    Svg(viewBox = "0 0 200 200") {
        Defs {
            LinearGradient("bg") {
                Stop({ attr("offset", "0%"); attr("stop-color", "#4a90e2") })
                Stop({ attr("offset", "100%"); attr("stop-color", "#764ba2") })
            }
        }
        Rect(0, 0, 200, 200, { fill("url(#bg)") })
        Circle(100, 100, 50, {
            fill("white")
            stroke("navy")
            strokeWidth(3)
        })
        SvgText({
            attr("x", "100")
            attr("y", "105")
            attr("text-anchor", "middle")
            attr("font-size", "16")
        }) {
            Text("Hello SVG")
        }
    }
}
```

### Conditional rendering and lists

```kotlin
@Composable
fun TodoList(items: List<Todo>, showCompleted: Boolean) {
    Div({ classes("todos") }) {
        H2 { Text("Todo List") }
        Ul {
            items.forEach { todo ->
                if (todo.completed && !showCompleted) return@forEach
                Li({
                    classes(if (todo.completed) "done" else "pending")
                }) {
                    Text(todo.title)
                }
            }
        }
    }
}

val html = renderComposableToString {
    TodoList(
        items = listOf(
            Todo("Write README", completed = true),
            Todo("Publish to Maven", completed = false),
        ),
        showCompleted = true,
    )
}
```

### Rendering to `org.w3c.dom.Document` (JVM)

Gives you a real W3C DOM you can serialize with `Transformer`, query with XPath, or pass to any library expecting a DOM.

```kotlin
import compose.html.*
import org.w3c.dom.Document
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult
import java.io.StringWriter

val doc: Document = renderComposableToDocument {
    Svg(viewBox = "0 0 100 100") {
        Circle(50, 50, 40, { fill("blue") })
    }
}

// Serialize as XML with the JDK's Transformer
val transformer = TransformerFactory.newInstance().newTransformer()
val writer = StringWriter()
transformer.transform(DOMSource(doc), StreamResult(writer))
println(writer.toString())
```

### Interactive browser DOM (JS)

The JS target keeps Compose HTML's original interactive rendering via `DomApplier`. Use `renderComposable(Element)` to mount a composable into a real DOM node:

```kotlin
import compose.html.*
import kotlinx.browser.document

fun main() {
    val root = document.getElementById("root") as org.w3c.dom.Element
    renderComposable(root) {
        Div {
            H1 { Text("Interactive Compose HTML") }
            P { Text("Rendered to real DOM, reactive to state changes.") }
        }
    }
}
```

## Architecture

```
                         Your composables
                                │
                   ┌────────────┴────────────┐
                   │                         │
                   ▼                         ▼
         LocalStaticRendering = true    = false (JS only)
                   │                         │
                   ▼                         ▼
              TextApplier               DomApplier
                   │                         │
                   ▼                         ▼
             HtmlNode tree           Real browser DOM
                   │
        ┌──────────┼──────────┐
        ▼          ▼          ▼
  TreeSerializer  Document   custom
    (String)     converter   walker
                  (JVM)
```

The `TextApplier` builds a lightweight `HtmlElementNode` / `HtmlTextNode` tree during a **single-pass** composition. No recomposition, no effects, no snapshot observation — just pure composables in, tree out. This keeps it thread-safe for server-side use where snapshots would leak state between concurrent requests.

### Single-pass constraint

Because static rendering disposes the composition immediately after `setContent`, these APIs **do not work** in static mode:

- `LaunchedEffect`, `SideEffect`, `DisposableEffect`
- `mutableStateOf` changes (there's no recomposer to observe them)
- `snapshotFlow`, `derivedStateOf` changes

These **do** work:

- `remember` (resolved once during the single pass)
- `CompositionLocalProvider`
- Any pure function of inputs: `if`/`when`, loops, nested composables

## Composable reference

### HTML

`Div`, `Span`, `P`, `A`, `H1`–`H6`, `Ul`, `Ol`, `Li`, `Img`, `Br`, `Hr`, `Input`, `Button`, `Form`, `Label`, `Table`, `Thead`, `Tbody`, `Tr`, `Th`, `Td`, `Nav`, `Header`, `Footer`, `Main`, `Section`, `Article`, `Aside`, `Pre`, `Code`, `Em`, `Strong`, `Small`, `Text`

Plus the generic `TagElement(tagName, attrs, content)` for any custom tag.

### SVG

`Svg`, `Circle`, `Rect`, `Ellipse`, `Line`, `Path`, `Polygon`, `Polyline`, `G`, `Defs`, `Symbol`, `Use`, `ClipPath`, `Mask`, `View`, `Switch`, `SvgText`, `TextPath`, `Tspan`, `LinearGradient`, `RadialGradient`, `Stop`, `Pattern`, `Animate`, `AnimateMotion`, `AnimateTransform`, `Set`, `Mpath`, `Image`, `Filter`, `Marker`, `Title`, `Desc`, `SvgA`

SVG attribute extensions on `AttrsBuilder`: `fill`, `stroke`, `strokeWidth`, `transform`, `opacity`, `fillOpacity`, `strokeOpacity`, `fillRule`, `strokeLinecap`, `strokeLinejoin`, `strokeDasharray`, `strokeDashoffset`.

### Attribute DSL

```kotlin
Div({
    id("main")
    classes("container", "dark")
    attr("data-testid", "main-content")
    style {
        property("color", "red")
        property("padding", "1rem")
    }
}) {
    Text("content")
}
```

## Testing

209 tests across JVM and JS targets:

- **108 JVM tests** — node tree, serializer, escaping, void elements, end-to-end rendering, SVG, Document conversion
- **112 JS tests** — same 97 `commonTest` tests run on JS, plus 11 `DomRenderTest` + 4 `SvgDomRenderTest` verifying the interactive DOM path

Run all tests:

```bash
./gradlew jvmTest jsTest
```

## Building

Requires JDK 21 (configured in `gradle.properties`).

```bash
./gradlew build
./gradlew publishToMavenLocal  # publish to ~/.m2
```

## License

Same license as the upstream [JetBrains Compose Multiplatform](https://github.com/JetBrains/compose-multiplatform) — Apache 2.0. This fork inherits Compose HTML's code as a JS dependency.

## Acknowledgements

Built on top of JetBrains' excellent [Compose HTML](https://github.com/JetBrains/compose-multiplatform/tree/master/html) library.
