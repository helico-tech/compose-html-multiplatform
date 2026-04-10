package compose.html

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composition
import androidx.compose.runtime.CompositionLocalProvider
import org.w3c.dom.Element

fun renderComposable(
    root: Element,
    content: @Composable () -> Unit,
): Composition {
    return org.jetbrains.compose.web.renderComposable(root) {
        CompositionLocalProvider(LocalStaticRendering provides false) {
            content()
        }
    }
}
