package app.aaps.core.interfaces.overview.graph

/**
 * Domain models for calculated graph data.
 * These are view-agnostic - no GraphView or Vico dependencies.
 * Populated by workers, consumed by ViewModels/UI.
 */

/**
 * BG range classification for coloring
 */
enum class BgRange {

    HIGH,      // Above high mark
    IN_RANGE,  // Within target range
    LOW        // Below low mark
}

/**
 * BG data point type for different rendering styles and colors
 */
enum class BgType {

    REGULAR,         // Regular CGM reading (outlined circle, white)
    BUCKETED,        // 5-min bucketed average (filled circle, colored by range)
    IOB_PREDICTION,  // IOB-based prediction (filled concentric circles, blue)
    COB_PREDICTION,  // COB-based prediction (filled concentric circles, orange)
    A_COB_PREDICTION,// Absorbed COB prediction (filled concentric circles, lighter orange)
    UAM_PREDICTION,  // Unannounced meals prediction (filled concentric circles, yellow)
    ZT_PREDICTION    // Zero-temp prediction (filled concentric circles, cyan)
}

/**
 * Time range for graph display
 */
data class TimeRange(
    val fromTime: Long,
    val toTime: Long,
    val endTime: Long // includes predictions
)

/**
 * Individual BG data point
 */
data class BgDataPoint(
    val timestamp: Long,
    val value: Double,             // Already converted to user's units (mg/dL or mmol/L)
    val range: BgRange,            // Range classification (high/in-range/low)
    val type: BgType,              // Type determines rendering style and color
    val filledGap: Boolean = false, // For bucketed data - if true, render semi-transparent
)