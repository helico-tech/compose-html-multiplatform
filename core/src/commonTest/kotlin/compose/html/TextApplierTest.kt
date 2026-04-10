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
