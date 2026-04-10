package compose.html

import org.jetbrains.compose.web.attributes.AttrsScope
import org.jetbrains.compose.web.css.StyleScope
import org.w3c.dom.Element

class DomAttrsScope<TElement : Element>(
    private val delegate: AttrsScope<TElement>,
) : AttrsBuilder<Any> {

    override fun attr(name: String, value: String) {
        delegate.attr(name, value)
    }

    override fun classes(vararg classNames: String) {
        delegate.classes(*classNames)
    }

    override fun style(builder: StyleBuilder.() -> Unit) {
        delegate.style {
            DomStyleScope(this).builder()
        }
    }
}

class DomStyleScope(
    private val delegate: StyleScope,
) : StyleBuilder {

    override fun property(name: String, value: String) {
        delegate.property(name, value)
    }
}
