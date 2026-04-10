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
