package app.aaps.core.ui.compose.preference

import androidx.compose.runtime.compositionLocalOf
import app.aaps.core.interfaces.protection.PasswordCheck
import app.aaps.core.keys.interfaces.PreferenceVisibilityContext

/**
 * CompositionLocal for providing PasswordCheck to preference composables.
 * Used by AdaptivePreferenceItem to render password/PIN preferences.
 */
val LocalPasswordCheck = compositionLocalOf<PasswordCheck?> { null }

/**
 * CompositionLocal for providing PreferenceVisibilityContext to preference composables.
 * Used by AdaptivePreferenceList and PreferenceContentExtensions to evaluate visibility conditions.
 */
val LocalVisibilityContext = compositionLocalOf<PreferenceVisibilityContext?> { null }
