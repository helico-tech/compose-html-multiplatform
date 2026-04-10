package compose.html.serializer

import compose.html.HtmlElementNode
import compose.html.HtmlTextNode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TreeSerializerTest {

    private val serializer = TreeSerializer(prettyPrint = false)
    private val prettySerializer = TreeSerializer(prettyPrint = true)
    private val xhtmlSerializer = TreeSerializer(prettyPrint = false, xhtmlCompatible = true)

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
        assertEquals("<div><span>text</span></div>", serializer.serializeToString(outer))
    }

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
}
