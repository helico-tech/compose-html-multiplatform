package compose.html.serializer

import compose.html.*
import kotlin.test.Test
import kotlin.test.assertEquals

class RenderComposableTest {

    @Test
    fun renderEmptyDiv() {
        val html = renderComposableToString(prettyPrint = false) {
            Div()
        }
        assertEquals("<div></div>", html)
    }

    @Test
    fun renderDivWithText() {
        val html = renderComposableToString(prettyPrint = false) {
            Div { Text("hello") }
        }
        assertEquals("<div>hello</div>", html)
    }

    @Test
    fun renderDivWithAttributes() {
        val html = renderComposableToString(prettyPrint = false) {
            Div({ attr("data-x", "1"); classes("a", "b") }) {
                Text("content")
            }
        }
        assertEquals("<div class=\"a b\" data-x=\"1\">content</div>", html)
    }

    @Test
    fun renderDivWithStyles() {
        val html = renderComposableToString(prettyPrint = false) {
            Div({ style { property("color", "red") } }) {
                Text("styled")
            }
        }
        assertEquals("<div style=\"color: red\">styled</div>", html)
    }

    @Test
    fun renderNestedElements() {
        val html = renderComposableToString(prettyPrint = false) {
            Div {
                Span { Text("inner") }
            }
        }
        assertEquals("<div><span>inner</span></div>", html)
    }

    @Test
    fun renderMultipleChildren() {
        val html = renderComposableToString(prettyPrint = false) {
            Div {
                Span { Text("a") }
                Span { Text("b") }
            }
        }
        assertEquals("<div><span>a</span><span>b</span></div>", html)
    }

    @Test
    fun renderConditional() {
        val showExtra = true
        val html = renderComposableToString(prettyPrint = false) {
            Div {
                Text("always")
                if (showExtra) {
                    Span { Text("extra") }
                }
            }
        }
        assertEquals("<div>always<span>extra</span></div>", html)
    }

    @Test
    fun renderConditionalFalse() {
        val showExtra = false
        val html = renderComposableToString(prettyPrint = false) {
            Div {
                Text("always")
                if (showExtra) {
                    Span { Text("extra") }
                }
            }
        }
        assertEquals("<div>always</div>", html)
    }

    @Test
    fun renderList() {
        val items = listOf("x", "y", "z")
        val html = renderComposableToString(prettyPrint = false) {
            Ul {
                items.forEach { item ->
                    Li { Text(item) }
                }
            }
        }
        assertEquals("<ul><li>x</li><li>y</li><li>z</li></ul>", html)
    }

    @Test
    fun renderEscapedText() {
        val html = renderComposableToString(prettyPrint = false) {
            Div { Text("<script>alert('xss')</script>") }
        }
        assertEquals("<div>&lt;script&gt;alert('xss')&lt;/script&gt;</div>", html)
    }

    @Test
    fun renderVoidElement() {
        val html = renderComposableToString(prettyPrint = false) {
            Br()
        }
        assertEquals("<br>", html)
    }

    @Test
    fun renderPrettyPrint() {
        val html = renderComposableToString(prettyPrint = true) {
            Div {
                Span { Text("hi") }
            }
        }
        assertEquals("<div>\n  <span>hi</span>\n</div>\n", html)
    }

    @Test
    fun renderMultipleRootElements() {
        val html = renderComposableToString(prettyPrint = false) {
            Div { Text("a") }
            Div { Text("b") }
        }
        assertEquals("<div>a</div><div>b</div>", html)
    }

    @Test
    fun renderToAppendable() {
        val sb = StringBuilder()
        renderComposableTo(sb, prettyPrint = false) {
            Div { Text("streamed") }
        }
        assertEquals("<div>streamed</div>", sb.toString())
    }
}
