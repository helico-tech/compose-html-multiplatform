package compose.html

class CommonAttrsScope<E>(val node: HtmlElementNode) : AttrsBuilder<E> {

    override fun attr(name: String, value: String) {
        node.attributes[name] = value
    }

    override fun classes(vararg classNames: String) {
        node.classes.addAll(classNames)
    }

    override fun style(builder: StyleBuilder.() -> Unit) {
        CommonStyleScope(node.inlineStyles).builder()
    }
}

class CommonStyleScope(private val styles: MutableMap<String, String>) : StyleBuilder {

    override fun property(name: String, value: String) {
        styles[name] = value
    }
}
