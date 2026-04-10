package compose.html.svg

import androidx.compose.runtime.Composable
import compose.html.AttrsBuilder
import compose.html.TagElement

const val SVG_NS = "http://www.w3.org/2000/svg"

@Composable
fun Svg(
    viewBox: String? = null,
    attrs: (AttrsBuilder<*>.() -> Unit)? = null,
    content: @Composable () -> Unit = {},
) {
    TagElement("svg", SVG_NS, attrs = {
        attr("xmlns", SVG_NS)
        viewBox?.let { attr("viewBox", it) }
        attrs?.invoke(this)
    }, content = content)
}

@Composable
fun SvgElement(
    tagName: String,
    attrs: (AttrsBuilder<*>.() -> Unit)? = null,
    content: @Composable () -> Unit = {},
) {
    TagElement(tagName, SVG_NS, attrs, content)
}
