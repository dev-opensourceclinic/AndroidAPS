package app.aaps.core.ui.compose

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Color scheme for general UI elements across the app.
 * Provides consistent color coding for common elements like IOB, COB, etc.
 *
 * **Usage:**
 * - Treatment screens
 * - Overview screen
 * - General UI elements
 *
 * **Color Assignment:**
 * - activeInsulinText: Blue - Active Insulin On Board (IOB) text color
 * - calculator: Green - Bolus calculator icon and related elements
 * - future: Green - Future/scheduled items color
 * - invalidated: Red - Invalid/deleted items color
 * - inProgress: Amber - Items with temporary modifications (e.g., profile with percentage/timeshift)
 * - onInProgress: Dark text color for use on inProgress background
 * - ttEatingSoon: Orange (carbs color) - Eating Soon temp target badge
 * - ttActivity: Blue (exercise color) - Activity temp target badge
 * - ttHypoglycemia: Red (low color) - Hypoglycemia temp target badge
 * - ttCustom: Purple - Custom temp target badge
 *
 * Colors match the existing theme attribute colors for consistency with the rest of the app.
 *
 * @property activeInsulinText Color for active insulin (IOB) text
 * @property calculator Color for calculator icon and elements
 * @property futureRecord Color for future/scheduled items
 * @property invalidatedRecord Color for invalid/deleted items
 * @property inProgress Color for items with temporary modifications (matches ribbonWarningColor)
 * @property onInProgress Text color for use on inProgress background (matches ribbonTextWarningColor)
 * @property ttEatingSoon Color for Eating Soon temp target badge
 * @property ttActivity Color for Activity temp target badge
 * @property ttHypoglycemia Color for Hypoglycemia temp target badge
 * @property ttCustom Color for Custom/Automation/Manual temp target badge
 */
data class GeneralColors(
    val activeInsulinText: Color,
    val calculator: Color,
    val futureRecord: Color,
    val invalidatedRecord: Color,
    val statusNormal: Color,
    val statusWarning: Color,
    val statusCritical: Color,
    val inProgress: Color,
    val onInProgress: Color,
    val ttEatingSoon: Color,
    val ttActivity: Color,
    val ttHypoglycemia: Color,
    val ttCustom: Color,
    val adjusted: Color,
    val onAdjusted: Color,
    val onBadge: Color
)

/**
 * Light mode color scheme for general elements.
 * Colors match the light theme values from colors.xml.
 */
internal val LightGeneralColors = GeneralColors(
    activeInsulinText = Color(0xFF1E88E5),  // iob color
    calculator = Color(0xFF66BB6A),          // colorCalculatorButton
    futureRecord = Color(0xFF66BB6A),        // green for scheduled/future items
    invalidatedRecord = Color(0xFFE53935),   // red for invalid/deleted items
    statusNormal = Color(0xFF4CAF50),        // green for normal status
    statusWarning = Color(0xFFFFC107),       // amber for warning status
    statusCritical = Color(0xFFF44336),      // red for critical status
    inProgress = Color(0xFFF4D700),          // ribbonWarning - amber for modified/temporary items
    onInProgress = Color(0xFF303030),        // ribbonTextWarning - dark text on amber background
    ttEatingSoon = Color(0xFFFB8C00),        // orange/carbs color for Eating Soon (matches TempTargetDialog)
    ttActivity = Color(0xFF42A5F5),          // blue/exercise color for Activity (matches TempTargetDialog)
    ttHypoglycemia = Color(0xFFFF0000),      // red/low color for Hypoglycemia (matches TempTargetDialog)
    ttCustom = Color(0xFF9C27B0),            // purple for Custom/Automation/Manual
    adjusted = Color(0xFF4CAF50),            // green for APS-adjusted target chip
    onAdjusted = Color(0xFFFFFFFF),          // white text on adjusted target
    onBadge = Color(0xFFFFFFFF)              // white text on colored badges
)

/**
 * Dark mode color scheme for general elements.
 * Colors match the dark theme values from colors.xml (night folder).
 */
internal val DarkGeneralColors = GeneralColors(
    activeInsulinText = Color(0xFF1E88E5),  // iob color (same in both modes)
    calculator = Color(0xFF67E86A),          // colorCalculatorButton (night)
    futureRecord = Color(0xFF6AE86D),        // green for scheduled/future items (night)
    invalidatedRecord = Color(0xFFEF5350),   // red for invalid/deleted items (night)
    statusNormal = Color(0xFF81C784),        // lighter green for dark mode
    statusWarning = Color(0xFFFFD54F),       // lighter amber for dark mode
    statusCritical = Color(0xFFE57373),      // lighter red for dark mode
    inProgress = Color(0xFFF4D700),          // ribbonWarning - same in both modes
    onInProgress = Color(0xFF303030),        // ribbonTextWarning - same in both modes
    ttEatingSoon = Color(0xFFFFB74D),        // lighter orange/carbs for Eating Soon (dark mode)
    ttActivity = Color(0xFF64B5F6),          // lighter blue/exercise for Activity (dark mode)
    ttHypoglycemia = Color(0xFFEF5350),      // lighter red/low for Hypoglycemia (dark mode)
    ttCustom = Color(0xFFBA68C8),            // lighter purple for Custom/Automation/Manual (dark mode)
    adjusted = Color(0xFF81C784),            // lighter green for APS-adjusted target chip (dark mode)
    onAdjusted = Color(0xFF000000),          // dark text on adjusted target (dark mode)
    onBadge = Color(0xFFFFFFFF)              // white text on colored badges
)

/**
 * CompositionLocal providing general colors based on current theme (light/dark).
 * Accessed via AapsTheme.generalColors in composables.
 */
internal val LocalGeneralColors = compositionLocalOf { LightGeneralColors }
