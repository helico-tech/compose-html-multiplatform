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
