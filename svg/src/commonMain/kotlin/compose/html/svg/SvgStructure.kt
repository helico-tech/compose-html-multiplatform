package compose.html.svg

import androidx.compose.runtime.Composable
import compose.html.AttrsBuilder

@Composable
fun G(attrs: (AttrsBuilder<*>.() -> Unit)? = null, content: @Composable () -> Unit = {}) {
    SvgElement("g", attrs, content)
}

@Composable
fun Defs(attrs: (AttrsBuilder<*>.() -> Unit)? = null, content: @Composable () -> Unit = {}) {
    SvgElement("defs", attrs, content)
}

@Composable
fun Symbol(id: String, attrs: (AttrsBuilder<*>.() -> Unit)? = null, content: @Composable () -> Unit = {}) {
    SvgElement("symbol", attrs = { attr("id", id); attrs?.invoke(this) }, content = content)
}

@Composable
fun Use(attrs: (AttrsBuilder<*>.() -> Unit)? = null, content: @Composable () -> Unit = {}) {
    SvgElement("use", attrs, content)
}

@Composable
fun ClipPath(id: String, attrs: (AttrsBuilder<*>.() -> Unit)? = null, content: @Composable () -> Unit = {}) {
    SvgElement("clipPath", attrs = { attr("id", id); attrs?.invoke(this) }, content = content)
}

@Composable
fun Mask(id: String? = null, attrs: (AttrsBuilder<*>.() -> Unit)? = null, content: @Composable () -> Unit = {}) {
    SvgElement("mask", attrs = { id?.let { attr("id", it) }; attrs?.invoke(this) }, content = content)
}

@Composable
fun View(id: String, viewBox: String, attrs: (AttrsBuilder<*>.() -> Unit)? = null) {
    SvgElement("view", attrs = { attr("id", id); attr("viewBox", viewBox); attrs?.invoke(this) })
}

@Composable
fun Switch(attrs: (AttrsBuilder<*>.() -> Unit)? = null, content: @Composable () -> Unit = {}) {
    SvgElement("switch", attrs, content)
}
