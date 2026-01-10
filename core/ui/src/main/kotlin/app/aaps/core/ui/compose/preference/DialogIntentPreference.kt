package app.aaps.core.ui.compose.preference

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import app.aaps.core.keys.interfaces.IntentPreferenceKey
import app.aaps.core.keys.interfaces.PreferenceItem
import app.aaps.core.keys.interfaces.withClick
import app.aaps.core.ui.compose.OkCancelDialog

/**
 * An intent preference that shows a confirmation dialog before executing.
 * Automatically manages dialog state and rendering.
 *
 * @param intentKey The base intent key to wrap
 * @param titleResId String resource for dialog title
 * @param messageResId String resource for dialog message
 * @param onConfirm Action to execute when user confirms (receives no parameters)
 */
class DialogIntentPreference(
    val intentKey: IntentPreferenceKey,
    val titleResId: Int,
    val messageResId: Int,
    val onConfirm: () -> Unit
) : PreferenceItem {

    /**
     * Renders this preference with dialog handling.
     * AdaptivePreferenceList calls this automatically.
     */
    @Composable
    fun Render(
        onRenderKey: @Composable (IntentPreferenceKey) -> Unit
    ) {
        val (showDialog, setShowDialog) = remember { mutableStateOf(false) }

        if (showDialog) {
            OkCancelDialog(
                title = stringResource(titleResId),
                message = stringResource(messageResId),
                onConfirm = {
                    onConfirm()
                    setShowDialog(false)
                },
                onDismiss = { setShowDialog(false) }
            )
        }

        // Render the key with click handler that shows dialog
        onRenderKey(
            intentKey.withClick { setShowDialog(true) }
        )
    }
}

/**
 * Extension function to create a DialogIntentPreference from an IntentPreferenceKey.
 * Provides a fluent API for attaching dialogs to intent keys.
 *
 * Example usage:
 * ```
 * OverviewIntentKey.CopyStatusLightsFromNS.withDialog(
 *     titleResId = R.string.statuslights,
 *     messageResId = R.string.copy_existing_values,
 *     onConfirm = { overview.applyStatusLightsFromNsExec() }
 * )
 * ```
 */
fun IntentPreferenceKey.withDialog(
    titleResId: Int,
    messageResId: Int,
    onConfirm: () -> Unit
): DialogIntentPreference = DialogIntentPreference(
    intentKey = this,
    titleResId = titleResId,
    messageResId = messageResId,
    onConfirm = onConfirm
)
