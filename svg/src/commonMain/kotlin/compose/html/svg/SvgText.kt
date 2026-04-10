package compose.html.svg

import androidx.compose.runtime.Composable
import compose.html.AttrsBuilder

@Composable
fun SvgText(attrs: (AttrsBuilder<*>.() -> Unit)? = null, content: @Composable () -> Unit = {}) {
    SvgElement("text", attrs, content)
}

@Composable
fun TextPath(attrs: (AttrsBuilder<*>.() -> Unit)? = null, content: @Composable () -> Unit = {}) {
    SvgElement("textPath", attrs, content)
}

@Composable
fun Tspan(attrs: (AttrsBuilder<*>.() -> Unit)? = null, content: @Composable () -> Unit = {}) {
    SvgElement("tspan", attrs, content)
}
