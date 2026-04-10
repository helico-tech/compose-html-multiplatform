package compose.html.svg

import compose.html.serializer.renderComposableToString
import compose.html.serializer.renderComposableTo
import kotlin.test.Test
import kotlin.test.assertTrue

class SvgRenderTest {

    @Test
    fun svgRoot() {
        val result = renderComposableToString(prettyPrint = false) { Svg(viewBox = "0 0 100 100") }
        assertTrue(result.contains("<svg"), result)
        assertTrue(result.contains("xmlns=\"http://www.w3.org/2000/svg\""), result)
        assertTrue(result.contains("viewBox=\"0 0 100 100\""), result)
        assertTrue(result.contains("</svg>"), result)
    }

    @Test
    fun circle() {
        val result = renderComposableToString(prettyPrint = false) { Svg { Circle(50, 50, 40) } }
        assertTrue(result.contains("<circle"), result)
        assertTrue(result.contains("cx=\"50\""), result)
        assertTrue(result.contains("cy=\"50\""), result)
        assertTrue(result.contains("r=\"40\""), result)
    }

    @Test
    fun rect() {
        val result = renderComposableToString(prettyPrint = false) { Svg { Rect(10, 20, 100, 50) } }
        assertTrue(result.contains("<rect"), result)
        assertTrue(result.contains("x=\"10\""), result)
        assertTrue(result.contains("width=\"100\""), result)
    }

    @Test
    fun path() {
        val result = renderComposableToString(prettyPrint = false) { Svg { Path("M 10 10 L 90 90") } }
        assertTrue(result.contains("d=\"M 10 10 L 90 90\""), result)
    }

    @Test
    fun ellipse() {
        val result = renderComposableToString(prettyPrint = false) { Svg { Ellipse(50, 50, 40, 20) } }
        assertTrue(result.contains("<ellipse"), result)
        assertTrue(result.contains("rx=\"40\""), result)
        assertTrue(result.contains("ry=\"20\""), result)
    }

    @Test
    fun line() {
        val result = renderComposableToString(prettyPrint = false) { Svg { Line(0, 0, 100, 100) } }
        assertTrue(result.contains("<line"), result)
        assertTrue(result.contains("x1=\"0\""), result)
        assertTrue(result.contains("x2=\"100\""), result)
    }

    @Test
    fun polygon() {
        val result = renderComposableToString(prettyPrint = false) { Svg { Polygon("0,100 50,25 50,75 100,0") } }
        assertTrue(result.contains("points=\"0,100 50,25 50,75 100,0\""), result)
    }

    @Test
    fun polyline() {
        val result = renderComposableToString(prettyPrint = false) { Svg { Polyline("0,0 50,50 100,0") } }
        assertTrue(result.contains("points=\"0,0 50,50 100,0\""), result)
    }

    @Test
    fun gGrouping() {
        val result = renderComposableToString(prettyPrint = false) {
            Svg { G({ attr("transform", "translate(10,10)") }) { Circle(0, 0, 5) } }
        }
        assertTrue(result.contains("<g"), result)
        assertTrue(result.contains("</g>"), result)
        assertTrue(result.contains("<circle"), result)
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
        assertTrue(result.contains("<defs>"), result)
        assertTrue(result.contains("<linearGradient"), result)
        assertTrue(result.contains("id=\"grad1\""), result)
        assertTrue(result.contains("<stop"), result)
        assertTrue(result.contains("</linearGradient>"), result)
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
        assertTrue(result.contains("<clipPath"), result)
        assertTrue(result.contains("</clipPath>"), result)
        assertTrue(result.contains("<linearGradient"), result)
        assertTrue(result.contains("<radialGradient"), result)
    }

    @Test
    fun svgText() {
        val result = renderComposableToString(prettyPrint = false) {
            Svg { SvgText({ attr("x", "10"); attr("y", "20") }) { compose.html.Text("Hello SVG") } }
        }
        assertTrue(result.contains("<text"), result)
        assertTrue(result.contains("Hello SVG"), result)
        assertTrue(result.contains("</text>"), result)
    }

    @Test
    fun fillAndStrokeAttrs() {
        val result = renderComposableToString(prettyPrint = false) {
            Svg { Circle(50, 50, 40, attrs = { fill("blue"); stroke("black"); strokeWidth(2) }) }
        }
        assertTrue(result.contains("fill=\"blue\""), result)
        assertTrue(result.contains("stroke=\"black\""), result)
        assertTrue(result.contains("stroke-width=\"2\""), result)
    }

    @Test
    fun nestedSvgStructure() {
        val result = renderComposableToString(prettyPrint = false) {
            Svg(viewBox = "0 0 200 200") {
                Defs { LinearGradient("bg") { Stop({ attr("offset", "0%"); attr("stop-color", "#fff") }) } }
                G {
                    Rect(0, 0, 200, 200, { fill("url(#bg)") })
                    Circle(100, 100, 50, { stroke("red"); strokeWidth(3) })
                }
            }
        }
        assertTrue(result.contains("<svg"), result)
        assertTrue(result.contains("<defs>"), result)
        assertTrue(result.contains("<linearGradient"), result)
        assertTrue(result.contains("<g>"), result)
        assertTrue(result.contains("<rect"), result)
        assertTrue(result.contains("<circle"), result)
    }

    @Test
    fun use() {
        val result = renderComposableToString(prettyPrint = false) {
            Svg {
                Defs { Symbol("icon") { Circle(10, 10, 5) } }
                Use({ attr("href", "#icon") })
            }
        }
        assertTrue(result.contains("<symbol"), result)
        assertTrue(result.contains("id=\"icon\""), result)
        assertTrue(result.contains("<use"), result)
        assertTrue(result.contains("href=\"#icon\""), result)
    }

    @Test
    fun fullHtmlPageWithSvg() {
        val html = renderComposableToString(prettyPrint = false) {
            compose.html.Div({ classes("page") }) {
                compose.html.H1 { compose.html.Text("Chart") }
                Svg(viewBox = "0 0 200 100") {
                    Rect(0, 0, 200, 100, { fill("#eee") })
                    Circle(100, 50, 30, { fill("blue"); stroke("navy"); strokeWidth(2) })
                }
                compose.html.P { compose.html.Text("A simple chart.") }
            }
        }
        assertTrue(html.contains("<div class=\"page\">"), html)
        assertTrue(html.contains("<h1>Chart</h1>"), html)
        assertTrue(html.contains("<svg"), html)
        assertTrue(html.contains("<circle"), html)
        assertTrue(html.contains("fill=\"blue\""), html)
        assertTrue(html.contains("<p>A simple chart.</p>"), html)
    }

    @Test
    fun renderToAppendable() {
        val sb = StringBuilder()
        renderComposableTo(sb, prettyPrint = false) {
            Svg(viewBox = "0 0 10 10") { Circle(5, 5, 3) }
        }
        assertTrue(sb.toString().contains("<svg"), sb.toString())
        assertTrue(sb.toString().contains("<circle"), sb.toString())
    }

    @Test
    fun xhtmlCompatibleOutput() {
        val html = renderComposableToString(prettyPrint = false, xhtmlCompatible = true) {
            Svg { Circle(10, 10, 5) }
        }
        assertTrue(html.contains(" />"), html)
    }
}
