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
