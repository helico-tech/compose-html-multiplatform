package compose.html.svg

import androidx.compose.runtime.Composable
import compose.html.AttrsBuilder

@Composable fun Animate(attrs: (AttrsBuilder<*>.() -> Unit)? = null) { SvgElement("animate", attrs) }

@Composable
fun AnimateMotion(attrs: (AttrsBuilder<*>.() -> Unit)? = null, content: @Composable () -> Unit = {}) {
    SvgElement("animateMotion", attrs, content)
}

@Composable fun AnimateTransform(attrs: (AttrsBuilder<*>.() -> Unit)? = null) { SvgElement("animateTransform", attrs) }

@Composable fun Set(attrs: (AttrsBuilder<*>.() -> Unit)? = null) { SvgElement("set", attrs) }

@Composable fun Mpath(attrs: (AttrsBuilder<*>.() -> Unit)? = null) { SvgElement("mpath", attrs) }
