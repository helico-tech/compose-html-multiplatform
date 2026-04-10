package compose.html

import kotlinx.browser.document
import org.w3c.dom.HTMLElement
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DomRenderTest {

    private fun renderToDiv(content: @androidx.compose.runtime.Composable () -> Unit): HTMLElement {
        val root = document.createElement("div") as HTMLElement
        document.body?.appendChild(root)
        val composition = renderComposable(root) { content() }
        // Compose applies changes synchronously with Unconfined dispatcher
        return root
    }

    // --- Basic HTML elements ---

    @Test
    fun divRendersToDOM() {
        val root = renderToDiv { Div() }
        assertEquals("<div></div>", root.innerHTML)
    }

    @Test
    fun divWithTextRendersToDOM() {
        val root = renderToDiv { Div { Text("hello") } }
        assertEquals("<div>hello</div>", root.innerHTML)
    }

    @Test
    fun nestedElementsRenderToDOM() {
        val root = renderToDiv {
            Div {
                Span { Text("inner") }
            }
        }
        assertEquals("<div><span>inner</span></div>", root.innerHTML)
    }

    @Test
    fun multipleChildrenRenderToDOM() {
        val root = renderToDiv {
            Div {
                Span { Text("a") }
                Span { Text("b") }
            }
        }
        assertEquals("<div><span>a</span><span>b</span></div>", root.innerHTML)
    }

    // --- Attributes ---

    @Test
    fun attributesRenderToDOM() {
        val root = renderToDiv {
            Div({ attr("data-x", "1") })
        }
        val div = root.firstElementChild as HTMLElement
        assertEquals("1", div.getAttribute("data-x"))
    }

    @Test
    fun classesRenderToDOM() {
        val root = renderToDiv {
            Div({ classes("a", "b", "c") })
        }
        val div = root.firstElementChild as HTMLElement
        assertEquals("a b c", div.getAttribute("class"))
    }

    @Test
    fun inlineStyleRendersToDOM() {
        val root = renderToDiv {
            Div({ style { property("color", "red") } })
        }
        val div = root.firstElementChild as HTMLElement
        assertTrue(div.getAttribute("style")?.contains("color") == true || div.style.color == "red")
    }

    @Test
    fun idRendersToDOM() {
        val root = renderToDiv {
            Div({ id("my-div") }) { Text("content") }
        }
        val div = root.firstElementChild as HTMLElement
        assertEquals("my-div", div.id)
    }

    // --- Void elements ---

    @Test
    fun brRendersToDOM() {
        val root = renderToDiv { Br() }
        assertEquals("BR", root.firstElementChild?.tagName)
    }

    @Test
    fun imgRendersToDOM() {
        val root = renderToDiv { Img({ attr("src", "photo.jpg"); attr("alt", "A photo") }) }
        val img = root.firstElementChild as HTMLElement
        assertEquals("photo.jpg", img.getAttribute("src"))
        assertEquals("A photo", img.getAttribute("alt"))
    }

    // --- List rendering ---

    @Test
    fun listRendersToDOM() {
        val items = listOf("x", "y", "z")
        val root = renderToDiv {
            Ul {
                items.forEach { item ->
                    Li { Text(item) }
                }
            }
        }
        assertEquals("<ul><li>x</li><li>y</li><li>z</li></ul>", root.innerHTML)
    }
}
