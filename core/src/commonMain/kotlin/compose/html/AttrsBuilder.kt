package compose.html

interface AttrsBuilder<E> {
    fun attr(name: String, value: String)
    fun classes(vararg classNames: String)
    fun style(builder: StyleBuilder.() -> Unit)
    fun id(value: String) { attr("id", value) }
}

interface StyleBuilder {
    fun property(name: String, value: String)
}
