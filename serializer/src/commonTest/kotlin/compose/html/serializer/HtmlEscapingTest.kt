package compose.html.serializer

import kotlin.test.Test
import kotlin.test.assertEquals

class HtmlEscapingTest {

    @Test
    fun escapeHtmlLessThan() {
        assertEquals("&lt;script&gt;", escapeHtml("<script>"))
    }

    @Test
    fun escapeHtmlAmpersand() {
        assertEquals("Tom &amp; Jerry", escapeHtml("Tom & Jerry"))
    }

    @Test
    fun escapeHtmlNoChange() {
        assertEquals("hello world", escapeHtml("hello world"))
    }

    @Test
    fun escapeHtmlAllSpecialChars() {
        assertEquals("&lt;a&gt; &amp; &lt;b&gt;", escapeHtml("<a> & <b>"))
    }

    @Test
    fun escapeAttrQuotes() {
        assertEquals("he said &quot;hi&quot;", escapeAttr("he said \"hi\""))
    }

    @Test
    fun escapeAttrAlsoEscapesHtmlChars() {
        assertEquals("&lt;a&gt; &amp; &quot;b&quot;", escapeAttr("<a> & \"b\""))
    }

    @Test
    fun escapeEmptyString() {
        assertEquals("", escapeHtml(""))
        assertEquals("", escapeAttr(""))
    }
}
