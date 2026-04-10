package compose.html.svg

import compose.html.renderComposable
import kotlinx.browser.document
import org.w3c.dom.HTMLElement
import kotlin.test.Test
import kotlin.test.assertEquals

class SvgDomRenderTest {

    private fun renderToDiv(content: @androidx.compose.runtime.Composable () -> Unit): HTMLElement {
        val root = document.createElement("div") as HTMLElement
        document.body?.appendChild(root)
        renderComposable(root) { content() }
        return root
    }

    @Test
    fun svgRendersToDOM() {
        val root = renderToDiv {
            Svg(viewBox = "0 0 100 100") {
                Circle(50, 50, 40)
            }
        }
        val svg = root.firstElementChild!!
        assertEquals("svg", svg.tagName.lowercase())
        assertEquals("0 0 100 100", svg.getAttribute("viewBox"))

        val circle = svg.firstElementChild!!
        assertEquals("circle", circle.tagName.lowercase())
        assertEquals("50", circle.getAttribute("cx"))
        assertEquals("50", circle.getAttribute("cy"))
        assertEquals("40", circle.getAttribute("r"))
    }

    @Test
    fun svgNamespaceIsCorrect() {
        val root = renderToDiv {
            Svg {
                Rect(0, 0, 100, 50)
            }
        }
        val svg = root.firstElementChild!!
        assertEquals("http://www.w3.org/2000/svg", svg.namespaceURI)
        val rect = svg.firstElementChild!!
        assertEquals("http://www.w3.org/2000/svg", rect.namespaceURI)
    }

    @Test
    fun svgFillAndStrokeRenderToDOM() {
        val root = renderToDiv {
            Svg {
                Circle(10, 10, 5, attrs = {
                    fill("blue")
                    stroke("black")
                    strokeWidth(2)
                })
            }
        }
        val circle = root.firstElementChild!!.firstElementChild!!
        assertEquals("blue", circle.getAttribute("fill"))
        assertEquals("black", circle.getAttribute("stroke"))
        assertEquals("2", circle.getAttribute("stroke-width"))
    }

    @Test
    fun svgCamelCaseTagsRenderToDOM() {
        val root = renderToDiv {
            Svg {
                ClipPath("c1") {}
                LinearGradient("g1") {}
            }
        }
        val svg = root.firstElementChild!!
        val clipPath = svg.children.item(0)!!
        val linearGradient = svg.children.item(1)!!
        assertEquals("clipPath", clipPath.tagName)
        assertEquals("linearGradient", linearGradient.tagName)
    }
}
