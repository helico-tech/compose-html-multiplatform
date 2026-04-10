package compose.html

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode
import org.jetbrains.compose.web.dom.ElementBuilder
import org.w3c.dom.Element

@Composable
actual fun TagElement(
    tagName: String,
    namespace: String?,
    attrs: (AttrsBuilder<*>.() -> Unit)?,
    content: @Composable () -> Unit,
) {
    if (LocalStaticRendering.current) {
        // Static text path — same as JVM
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
    } else {
        // Interactive DOM path — delegate to Compose HTML
        val builder: ElementBuilder<Element> = if (namespace != null) {
            NamespacedElementBuilder(tagName, namespace)
        } else {
            ElementBuilder.createBuilder(tagName)
        }

        org.jetbrains.compose.web.dom.TagElement(
            elementBuilder = builder,
            applyAttrs = attrs?.let { attrsBlock ->
                { DomAttrsScope(this).attrsBlock() }
            },
            content = { content() },
        )
    }
}

@Composable
actual fun Text(value: String) {
    if (LocalStaticRendering.current) {
        ComposeNode<HtmlTextNode, TextApplier>(
            factory = { HtmlTextNode(value) },
            update = {
                set(value) { text = it }
            },
        )
    } else {
        org.jetbrains.compose.web.dom.Text(value)
    }
}
