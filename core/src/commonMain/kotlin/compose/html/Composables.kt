package compose.html

import androidx.compose.runtime.Composable

@Composable
expect fun TagElement(
    tagName: String,
    namespace: String?,
    attrs: (AttrsBuilder<*>.() -> Unit)?,
    content: @Composable () -> Unit,
)

@Composable
expect fun Text(value: String)

// Convenience overload without namespace
@Composable
fun TagElement(
    tagName: String,
    attrs: (AttrsBuilder<*>.() -> Unit)? = null,
    content: @Composable () -> Unit = {},
) {
    TagElement(tagName, null, attrs, content)
}

// HTML thin wrappers
@Composable fun Div(attrs: (AttrsBuilder<*>.() -> Unit)? = null, content: @Composable () -> Unit = {}) =
    TagElement("div", attrs = attrs, content = content)

@Composable fun Span(attrs: (AttrsBuilder<*>.() -> Unit)? = null, content: @Composable () -> Unit = {}) =
    TagElement("span", attrs = attrs, content = content)

@Composable fun P(attrs: (AttrsBuilder<*>.() -> Unit)? = null, content: @Composable () -> Unit = {}) =
    TagElement("p", attrs = attrs, content = content)

@Composable fun A(attrs: (AttrsBuilder<*>.() -> Unit)? = null, content: @Composable () -> Unit = {}) =
    TagElement("a", attrs = attrs, content = content)

@Composable fun H1(attrs: (AttrsBuilder<*>.() -> Unit)? = null, content: @Composable () -> Unit = {}) =
    TagElement("h1", attrs = attrs, content = content)

@Composable fun H2(attrs: (AttrsBuilder<*>.() -> Unit)? = null, content: @Composable () -> Unit = {}) =
    TagElement("h2", attrs = attrs, content = content)

@Composable fun H3(attrs: (AttrsBuilder<*>.() -> Unit)? = null, content: @Composable () -> Unit = {}) =
    TagElement("h3", attrs = attrs, content = content)

@Composable fun H4(attrs: (AttrsBuilder<*>.() -> Unit)? = null, content: @Composable () -> Unit = {}) =
    TagElement("h4", attrs = attrs, content = content)

@Composable fun H5(attrs: (AttrsBuilder<*>.() -> Unit)? = null, content: @Composable () -> Unit = {}) =
    TagElement("h5", attrs = attrs, content = content)

@Composable fun H6(attrs: (AttrsBuilder<*>.() -> Unit)? = null, content: @Composable () -> Unit = {}) =
    TagElement("h6", attrs = attrs, content = content)

@Composable fun Ul(attrs: (AttrsBuilder<*>.() -> Unit)? = null, content: @Composable () -> Unit = {}) =
    TagElement("ul", attrs = attrs, content = content)

@Composable fun Ol(attrs: (AttrsBuilder<*>.() -> Unit)? = null, content: @Composable () -> Unit = {}) =
    TagElement("ol", attrs = attrs, content = content)

@Composable fun Li(attrs: (AttrsBuilder<*>.() -> Unit)? = null, content: @Composable () -> Unit = {}) =
    TagElement("li", attrs = attrs, content = content)

@Composable fun Img(attrs: (AttrsBuilder<*>.() -> Unit)? = null) =
    TagElement("img", attrs = attrs) {}

@Composable fun Br(attrs: (AttrsBuilder<*>.() -> Unit)? = null) =
    TagElement("br", attrs = attrs) {}

@Composable fun Hr(attrs: (AttrsBuilder<*>.() -> Unit)? = null) =
    TagElement("hr", attrs = attrs) {}

@Composable fun Input(attrs: (AttrsBuilder<*>.() -> Unit)? = null) =
    TagElement("input", attrs = attrs) {}

@Composable fun Button(attrs: (AttrsBuilder<*>.() -> Unit)? = null, content: @Composable () -> Unit = {}) =
    TagElement("button", attrs = attrs, content = content)

@Composable fun Form(attrs: (AttrsBuilder<*>.() -> Unit)? = null, content: @Composable () -> Unit = {}) =
    TagElement("form", attrs = attrs, content = content)

@Composable fun Label(attrs: (AttrsBuilder<*>.() -> Unit)? = null, content: @Composable () -> Unit = {}) =
    TagElement("label", attrs = attrs, content = content)

@Composable fun Table(attrs: (AttrsBuilder<*>.() -> Unit)? = null, content: @Composable () -> Unit = {}) =
    TagElement("table", attrs = attrs, content = content)

@Composable fun Thead(attrs: (AttrsBuilder<*>.() -> Unit)? = null, content: @Composable () -> Unit = {}) =
    TagElement("thead", attrs = attrs, content = content)

@Composable fun Tbody(attrs: (AttrsBuilder<*>.() -> Unit)? = null, content: @Composable () -> Unit = {}) =
    TagElement("tbody", attrs = attrs, content = content)

@Composable fun Tr(attrs: (AttrsBuilder<*>.() -> Unit)? = null, content: @Composable () -> Unit = {}) =
    TagElement("tr", attrs = attrs, content = content)

@Composable fun Th(attrs: (AttrsBuilder<*>.() -> Unit)? = null, content: @Composable () -> Unit = {}) =
    TagElement("th", attrs = attrs, content = content)

@Composable fun Td(attrs: (AttrsBuilder<*>.() -> Unit)? = null, content: @Composable () -> Unit = {}) =
    TagElement("td", attrs = attrs, content = content)

@Composable fun Nav(attrs: (AttrsBuilder<*>.() -> Unit)? = null, content: @Composable () -> Unit = {}) =
    TagElement("nav", attrs = attrs, content = content)

@Composable fun Header(attrs: (AttrsBuilder<*>.() -> Unit)? = null, content: @Composable () -> Unit = {}) =
    TagElement("header", attrs = attrs, content = content)

@Composable fun Footer(attrs: (AttrsBuilder<*>.() -> Unit)? = null, content: @Composable () -> Unit = {}) =
    TagElement("footer", attrs = attrs, content = content)

@Composable fun Main(attrs: (AttrsBuilder<*>.() -> Unit)? = null, content: @Composable () -> Unit = {}) =
    TagElement("main", attrs = attrs, content = content)

@Composable fun Section(attrs: (AttrsBuilder<*>.() -> Unit)? = null, content: @Composable () -> Unit = {}) =
    TagElement("section", attrs = attrs, content = content)

@Composable fun Article(attrs: (AttrsBuilder<*>.() -> Unit)? = null, content: @Composable () -> Unit = {}) =
    TagElement("article", attrs = attrs, content = content)

@Composable fun Aside(attrs: (AttrsBuilder<*>.() -> Unit)? = null, content: @Composable () -> Unit = {}) =
    TagElement("aside", attrs = attrs, content = content)

@Composable fun Pre(attrs: (AttrsBuilder<*>.() -> Unit)? = null, content: @Composable () -> Unit = {}) =
    TagElement("pre", attrs = attrs, content = content)

@Composable fun Code(attrs: (AttrsBuilder<*>.() -> Unit)? = null, content: @Composable () -> Unit = {}) =
    TagElement("code", attrs = attrs, content = content)

@Composable fun Em(attrs: (AttrsBuilder<*>.() -> Unit)? = null, content: @Composable () -> Unit = {}) =
    TagElement("em", attrs = attrs, content = content)

@Composable fun Strong(attrs: (AttrsBuilder<*>.() -> Unit)? = null, content: @Composable () -> Unit = {}) =
    TagElement("strong", attrs = attrs, content = content)

@Composable fun Small(attrs: (AttrsBuilder<*>.() -> Unit)? = null, content: @Composable () -> Unit = {}) =
    TagElement("small", attrs = attrs, content = content)
