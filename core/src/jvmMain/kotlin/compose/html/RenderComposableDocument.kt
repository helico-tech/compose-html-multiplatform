package compose.html

import androidx.compose.runtime.Composable
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Renders composable content into a [org.w3c.dom.Document].
 *
 * Uses the same single-pass composition as [renderComposableToString],
 * then converts the resulting [HtmlNode] tree to W3C DOM nodes.
 *
 * ```kotlin
 * val doc = renderComposableToDocument {
 *     Div({ id("main") }) {
 *         H1 { Text("Hello") }
 *     }
 * }
 * // doc is a standard org.w3c.dom.Document — use Transformer to serialize,
 * // XPath to query, or pass to any library expecting a DOM.
 * ```
 */
fun renderComposableToDocument(
    content: @Composable () -> Unit,
): Document {
    val root = HtmlElementNode("root")
    composeOnce(root, content)

    val doc = DocumentBuilderFactory.newInstance().apply {
        isNamespaceAware = true
    }.newDocumentBuilder().newDocument()

    for (child in root.children) {
        doc.appendChild(convertNode(doc, child))
    }
    return doc
}

private fun convertNode(doc: Document, node: HtmlNode): Node = when (node) {
    is HtmlTextNode -> doc.createTextNode(node.text)
    is HtmlElementNode -> convertElementNode(doc, node)
}

private fun convertElementNode(doc: Document, node: HtmlElementNode): Element {
    val element = if (node.namespace != null) {
        doc.createElementNS(node.namespace, node.tagName)
    } else {
        doc.createElement(node.tagName)
    }

    // Classes → class attribute
    if (node.classes.isNotEmpty()) {
        element.setAttribute("class", node.classes.joinToString(" "))
    }

    // Inline styles → style attribute
    if (node.inlineStyles.isNotEmpty()) {
        val styleStr = node.inlineStyles.entries.joinToString("; ") { (k, v) -> "$k: $v" }
        element.setAttribute("style", styleStr)
    }

    // Remaining attributes
    for ((key, value) in node.attributes) {
        element.setAttribute(key, value)
    }

    // Recurse children
    for (child in node.children) {
        element.appendChild(convertNode(doc, child))
    }

    return element
}
