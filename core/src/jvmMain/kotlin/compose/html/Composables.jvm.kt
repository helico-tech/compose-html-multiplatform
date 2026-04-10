package compose.html

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode

@Composable
actual fun TagElement(
    tagName: String,
    namespace: String?,
    attrs: (AttrsBuilder<*>.() -> Unit)?,
    content: @Composable () -> Unit,
) {
    ComposeNode<HtmlElementNode, TextApplier>(
        factory = { HtmlElementNode(tagName, namespace) },
        update = {
            set(attrs) { attrsBlock ->
                attributes.clear()
                inlineStyles.clear()
                classes.clear()
                attrsBlock?.let { CommonAttrsScope<Any>(this).it() }
            }
        },
        content = content,
    )
}

@Composable
actual fun Text(value: String) {
    ComposeNode<HtmlTextNode, TextApplier>(
        factory = { HtmlTextNode(value) },
        update = {
            set(value) { text = it }
        },
    )
}
