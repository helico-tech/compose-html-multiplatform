package compose.html.svg

import androidx.compose.runtime.Composable
import compose.html.AttrsBuilder

@Composable fun Image(attrs: (AttrsBuilder<*>.() -> Unit)? = null) { SvgElement("image", attrs) }

@Composable
fun Filter(id: String? = null, attrs: (AttrsBuilder<*>.() -> Unit)? = null, content: @Composable () -> Unit = {}) {
    SvgElement("filter", attrs = { id?.let { attr("id", it) }; attrs?.invoke(this) }, content = content)
}

@Composable
fun Marker(id: String? = null, attrs: (AttrsBuilder<*>.() -> Unit)? = null, content: @Composable () -> Unit = {}) {
    SvgElement("marker", attrs = { id?.let { attr("id", it) }; attrs?.invoke(this) }, content = content)
}

@Composable fun Title(content: @Composable () -> Unit = {}) { SvgElement("title", content = content) }

@Composable fun Desc(content: @Composable () -> Unit = {}) { SvgElement("desc", content = content) }

@Composable
fun SvgA(attrs: (AttrsBuilder<*>.() -> Unit)? = null, content: @Composable () -> Unit = {}) {
    SvgElement("a", attrs, content)
}
