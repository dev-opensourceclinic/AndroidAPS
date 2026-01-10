/*
 * Adaptive Password Preference for Jetpack Compose
 */

package app.aaps.core.ui.compose.preference

import android.content.Context
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import app.aaps.core.interfaces.configuration.Config
import app.aaps.core.interfaces.protection.PasswordCheck
import app.aaps.core.keys.interfaces.IntPreferenceKey
import app.aaps.core.keys.interfaces.PreferenceVisibilityContext
import app.aaps.core.keys.interfaces.Preferences
import app.aaps.core.keys.interfaces.StringPreferenceKey
import app.aaps.core.ui.R

/**
 * Composable password/PIN preference that opens a dialog to set the password.
 * Automatically detects if it's a password or PIN from the StringPreferenceKey flags.
 *
 * @param preferences The Preferences instance
 * @param config The Config instance
 * @param stringKey The StringPreferenceKey (should have isPassword=true or isPin=true)
 * @param passwordCheck The PasswordCheck service for setting passwords
 * @param context Android context for the dialog
 * @param titleResId Optional title resource ID. If 0, uses stringKey.titleResId
 * @param visibilityKey Optional IntPreferenceKey that controls visibility
 * @param visibilityValue The value that visibilityKey must equal for this preference to be visible
 * @param visibilityContext Optional context for evaluating runtime visibility conditions
 */
@Composable
fun AdaptivePasswordPreferenceItem(
    preferences: Preferences,
    config: Config,
    stringKey: StringPreferenceKey,
    passwordCheck: PasswordCheck,
    context: Context,
    titleResId: Int = 0,
    visibilityKey: IntPreferenceKey? = null,
    visibilityValue: Int? = null,
    visibilityContext: PreferenceVisibilityContext? = null
) {
    val effectiveTitleResId = if (titleResId != 0) titleResId else stringKey.titleResId

    // Skip if no title resource is available
    if (effectiveTitleResId == 0) return

    // Check conditional visibility based on visibilityKey
    if (visibilityKey != null && visibilityValue != null) {
        val currentValue by rememberPreferenceIntState(preferences, visibilityKey)
        if (currentValue != visibilityValue) return
    }

    // Check standard visibility
    val visibility = calculatePreferenceVisibility(
        preferenceKey = stringKey,
        preferences = preferences,
        config = config,
        visibilityContext = visibilityContext
    )

    if (!visibility.visible) return

    // Use mutable state to track if password is set, updated via callbacks
    var hasValue by remember { mutableStateOf(preferences.get(stringKey).isNotEmpty()) }
    val isPin = stringKey.isPin

    val summary = when {
        hasValue -> "••••••••"
        isPin    -> stringResource(R.string.pin_not_set)
        else     -> stringResource(R.string.password_not_set)
    }

    Preference(
        title = { Text(stringResource(effectiveTitleResId)) },
        summary = { Text(summary) },
        enabled = visibility.enabled,
        onClick = if (visibility.enabled) {
            {
                passwordCheck.setPassword(
                    context = context,
                    labelId = effectiveTitleResId,
                    preference = stringKey,
                    ok = { hasValue = true },
                    clear = { hasValue = false },
                    pinInput = isPin
                )
            }
        } else null
    )
}
