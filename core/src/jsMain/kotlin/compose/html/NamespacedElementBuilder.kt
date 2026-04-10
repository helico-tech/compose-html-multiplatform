package compose.html

import org.jetbrains.compose.web.dom.ElementBuilder
import kotlinx.browser.document
import org.w3c.dom.Element

internal class NamespacedElementBuilder<TElement : Element>(
    private val tagName: String,
    private val namespace: String,
) : ElementBuilder<TElement> {

    @Suppress("UNCHECKED_CAST")
    override fun create(): TElement {
        return document.createElementNS(namespace, tagName) as TElement
    }
}
