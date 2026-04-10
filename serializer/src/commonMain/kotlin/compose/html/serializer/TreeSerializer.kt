package compose.html.serializer

import compose.html.HtmlElementNode
import compose.html.HtmlNode
import compose.html.HtmlTextNode

class TreeSerializer(
    private val prettyPrint: Boolean = true,
    private val xhtmlCompatible: Boolean = false,
    private val indent: String = "  ",
) {
    fun serialize(root: HtmlNode, out: Appendable) {
        serializeNode(root, out, depth = 0)
    }

    fun serializeToString(node: HtmlNode): String {
        return buildString { serialize(node, this) }
    }

    private fun serializeNode(node: HtmlNode, out: Appendable, depth: Int) {
        when (node) {
            is HtmlTextNode -> out.append(escapeHtml(node.text))
            is HtmlElementNode -> serializeElement(node, out, depth)
        }
    }

    private fun serializeElement(node: HtmlElementNode, out: Appendable, depth: Int) {
        if (prettyPrint) out.appendIndent(depth)

        out.append("<").append(node.tagName)

        if (node.classes.isNotEmpty()) {
            out.append(" class=\"")
            out.append(escapeAttr(node.classes.joinToString(" ")))
            out.append("\"")
        }

        if (node.inlineStyles.isNotEmpty()) {
            val styleStr = node.inlineStyles.entries.joinToString("; ") { (k, v) -> "$k: $v" }
            out.append(" style=\"")
            out.append(escapeAttr(styleStr))
            out.append("\"")
        }

        for ((key, value) in node.attributes) {
            out.append(" ").append(key).append("=\"")
            out.append(escapeAttr(value))
            out.append("\"")
        }

        if (node.children.isEmpty() && isVoidElement(node)) {
            if (xhtmlCompatible) {
                out.append(" />")
            } else {
                out.append(">")
            }
            if (prettyPrint) out.append("\n")
            return
        }

        out.append(">")

        val hasElementChildren = node.children.any { it is HtmlElementNode }
        if (prettyPrint && hasElementChildren) out.append("\n")

        for (child in node.children) {
            serializeNode(child, out, depth + 1)
        }

        if (prettyPrint && hasElementChildren) out.appendIndent(depth)
        out.append("</").append(node.tagName).append(">")
        if (prettyPrint) out.append("\n")
    }

    private fun Appendable.appendIndent(depth: Int) {
        repeat(depth) { append(indent) }
    }
}
