package compose.html

import androidx.compose.runtime.BroadcastFrameClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composition
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Recomposer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Runs a single-pass composition into an [HtmlElementNode] tree.
 * No recomposition, no snapshot observation, no effect processing.
 * The resulting tree is available via [root]'s children after this returns.
 */
fun composeOnce(root: HtmlElementNode, content: @Composable () -> Unit) {
    val clock = BroadcastFrameClock()
    val context = kotlinx.coroutines.Dispatchers.Unconfined + clock + Job()
    val scope = CoroutineScope(context)
    val recomposer = Recomposer(context)
    val composition = Composition(TextApplier(root), recomposer)

    scope.launch { recomposer.runRecomposeAndApplyChanges() }

    composition.setContent {
        CompositionLocalProvider(LocalStaticRendering provides true) {
            content()
        }
    }

    clock.sendFrame(0L)
    recomposer.close()
    // Note: we intentionally do NOT call composition.dispose() here because
    // dispose() triggers applier.clear(), which would remove the built tree.
}
