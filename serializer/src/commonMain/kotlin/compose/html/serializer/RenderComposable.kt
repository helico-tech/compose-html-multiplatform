package compose.html.serializer

import androidx.compose.runtime.Composable
import compose.html.HtmlElementNode
import compose.html.composeOnce

fun renderComposableToString(
    prettyPrint: Boolean = true,
    xhtmlCompatible: Boolean = false,
    content: @Composable () -> Unit,
): String {
    val root = HtmlElementNode("root")
    composeOnce(root, content)
    val serializer = TreeSerializer(prettyPrint, xhtmlCompatible)
    return buildString {
        for (child in root.children) {
            serializer.serialize(child, this)
        }
    }
}

fun renderComposableTo(
    out: Appendable,
    prettyPrint: Boolean = true,
    xhtmlCompatible: Boolean = false,
    content: @Composable () -> Unit,
) {
    val root = HtmlElementNode("root")
    composeOnce(root, content)
    val serializer = TreeSerializer(prettyPrint, xhtmlCompatible)
    for (child in root.children) {
        serializer.serialize(child, out)
    }
}

