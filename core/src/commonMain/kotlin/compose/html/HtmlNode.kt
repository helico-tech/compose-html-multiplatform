package compose.html

sealed class HtmlNode {
    val children: MutableList<HtmlNode> = mutableListOf()
}

class HtmlElementNode(
    val tagName: String,
    val namespace: String? = null,
    val attributes: MutableMap<String, String> = mutableMapOf(),
    val inlineStyles: MutableMap<String, String> = mutableMapOf(),
    val classes: MutableList<String> = mutableListOf(),
) : HtmlNode()

class HtmlTextNode(
    var text: String,
) : HtmlNode()
