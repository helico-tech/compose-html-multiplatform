package compose.html

import compose.html.svg.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class RenderComposableDocumentTest {

    @Test
    fun emptyDiv() {
        val doc = renderComposableToDocument { Div() }
        val div = doc.documentElement
        assertEquals("div", div.tagName)
        assertEquals(0, div.childNodes.length)
    }

    @Test
    fun divWithText() {
        val doc = renderComposableToDocument { Div { Text("hello") } }
        val div = doc.documentElement
        assertEquals("hello", div.textContent)
    }

    @Test
    fun nestedElements() {
        val doc = renderComposableToDocument {
            Div {
                Span { Text("inner") }
            }
        }
        val div = doc.documentElement
        val span = div.firstChild
        assertNotNull(span)
        assertEquals("span", span.nodeName)
        assertEquals("inner", span.textContent)
    }

    @Test
    fun attributesOnElement() {
        val doc = renderComposableToDocument {
            Div({ attr("data-x", "1"); id("main") })
        }
        val div = doc.documentElement
        assertEquals("1", div.getAttribute("data-x"))
        assertEquals("main", div.getAttribute("id"))
    }

    @Test
    fun classesOnElement() {
        val doc = renderComposableToDocument {
            Div({ classes("a", "b", "c") })
        }
        assertEquals("a b c", doc.documentElement.getAttribute("class"))
    }

    @Test
    fun inlineStyleOnElement() {
        val doc = renderComposableToDocument {
            Div({ style { property("color", "red") } })
        }
        assertEquals("color: red", doc.documentElement.getAttribute("style"))
    }

    @Test
    fun multipleRootElementsThrows() {
        // W3C Document can only have one document element. If the composition
        // produces multiple root elements, we should wrap them — for now,
        // document this constraint with a test that expects failure.
        try {
            renderComposableToDocument {
                Div { Text("a") }
                Span { Text("b") }
            }
            kotlin.test.fail("Expected DOMException for multiple root elements")
        } catch (e: org.w3c.dom.DOMException) {
            assertEquals(org.w3c.dom.DOMException.HIERARCHY_REQUEST_ERR, e.code)
        }
    }

    @Test
    fun listRendering() {
        val doc = renderComposableToDocument {
            Ul {
                listOf("x", "y", "z").forEach { Li { Text(it) } }
            }
        }
        val ul = doc.documentElement
        assertEquals("ul", ul.tagName)
        assertEquals(3, ul.childNodes.length)
        assertEquals("x", ul.childNodes.item(0).textContent)
        assertEquals("y", ul.childNodes.item(1).textContent)
        assertEquals("z", ul.childNodes.item(2).textContent)
    }

    @Test
    fun svgWithNamespace() {
        val doc = renderComposableToDocument {
            Svg(viewBox = "0 0 100 100") {
                Circle(50, 50, 40)
            }
        }
        val svg = doc.documentElement
        assertEquals("svg", svg.tagName)
        assertEquals("http://www.w3.org/2000/svg", svg.namespaceURI)
        assertEquals("0 0 100 100", svg.getAttribute("viewBox"))

        val circle = svg.firstChild as org.w3c.dom.Element
        assertEquals("circle", circle.tagName)
        assertEquals("http://www.w3.org/2000/svg", circle.namespaceURI)
        assertEquals("50", circle.getAttribute("cx"))
    }

    @Test
    fun svgFillAndStroke() {
        val doc = renderComposableToDocument {
            Svg {
                Circle(10, 10, 5, attrs = {
                    fill("blue")
                    stroke("black")
                    strokeWidth(2)
                })
            }
        }
        val circle = doc.documentElement.firstChild as org.w3c.dom.Element
        assertEquals("blue", circle.getAttribute("fill"))
        assertEquals("black", circle.getAttribute("stroke"))
        assertEquals("2", circle.getAttribute("stroke-width"))
    }

    @Test
    fun svgCamelCaseTags() {
        val doc = renderComposableToDocument {
            Svg {
                ClipPath("c1") {}
                LinearGradient("g1") {}
            }
        }
        val svg = doc.documentElement
        val clipPath = svg.childNodes.item(0) as org.w3c.dom.Element
        val linearGradient = svg.childNodes.item(1) as org.w3c.dom.Element
        assertEquals("clipPath", clipPath.tagName)
        assertEquals("linearGradient", linearGradient.tagName)
    }
}
