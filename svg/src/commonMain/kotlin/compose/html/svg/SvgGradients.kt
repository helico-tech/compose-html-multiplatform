package compose.html.svg

import androidx.compose.runtime.Composable
import compose.html.AttrsBuilder

@Composable
fun LinearGradient(id: String, attrs: (AttrsBuilder<*>.() -> Unit)? = null, content: @Composable () -> Unit = {}) {
    SvgElement("linearGradient", attrs = { attr("id", id); attrs?.invoke(this) }, content = content)
}

@Composable
fun RadialGradient(id: String, attrs: (AttrsBuilder<*>.() -> Unit)? = null, content: @Composable () -> Unit = {}) {
    SvgElement("radialGradient", attrs = { attr("id", id); attrs?.invoke(this) }, content = content)
}

@Composable
fun Stop(attrs: (AttrsBuilder<*>.() -> Unit)? = null) {
    SvgElement("stop", attrs)
}

@Composable
fun Pattern(id: String, attrs: (AttrsBuilder<*>.() -> Unit)? = null, content: @Composable () -> Unit = {}) {
    SvgElement("pattern", attrs = { attr("id", id); attrs?.invoke(this) }, content = content)
}
