package compose.html.serializer

import androidx.compose.runtime.BroadcastFrameClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composition
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Recomposer
import compose.html.HtmlElementNode
import compose.html.LocalStaticRendering
import compose.html.TextApplier
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

fun renderComposableToString(
    prettyPrint: Boolean = true,
    xhtmlCompatible: Boolean = false,
    content: @Composable () -> Unit,
): String {
    val root = HtmlElementNode("root")
    composeOnce(root, content)
    val serializer = TreeSerializer(prettyPrint, xhtmlCompatible)
    return buildString {
        for (child in root.children) {
            serializer.serialize(child, this)
        }
    }
}

fun renderComposableTo(
    out: Appendable,
    prettyPrint: Boolean = true,
    xhtmlCompatible: Boolean = false,
    content: @Composable () -> Unit,
) {
    val root = HtmlElementNode("root")
    composeOnce(root, content)
    val serializer = TreeSerializer(prettyPrint, xhtmlCompatible)
    for (child in root.children) {
        serializer.serialize(child, out)
    }
}

internal fun composeOnce(root: HtmlElementNode, content: @Composable () -> Unit) {
    val clock = BroadcastFrameClock()
    val recomposer = Recomposer(kotlinx.coroutines.Dispatchers.Unconfined + clock)
    val composition = Composition(TextApplier(root), recomposer)
    runBlocking(kotlinx.coroutines.Dispatchers.Unconfined + clock) {
        val recomposeJob = launch { recomposer.runRecomposeAndApplyChanges() }
        composition.setContent {
            CompositionLocalProvider(LocalStaticRendering provides true) {
                content()
            }
        }
        recomposer.close()
        recomposeJob.join()
    }
    // Note: we intentionally do NOT call composition.dispose() here because
    // dispose() triggers applier.clear(), which would remove the built tree
    // that we need for serialization.
}
