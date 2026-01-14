package app.aaps.core.interfaces.overview.graph

import kotlinx.coroutines.flow.StateFlow

/**
 * Cache for calculated graph data. Populated by workers, observed by ViewModel via StateFlow.
 * This replaces OverviewDataImpl for the new Compose graph system.
 *
 * Architecture: Independent Series Updates
 * - Each series has its own StateFlow
 * - Workers update their series independently
 * - ViewModel/UI collects each flow separately
 * - Only the changed series triggers recomposition
 *
 * MIGRATION NOTE: During migration, workers populate BOTH this cache and OverviewDataImpl.
 * After migration complete, OverviewDataImpl will be deleted.
 */
interface CalculatedGraphDataCache {

    // Time range - observable, recalculated as series arrive
    val timeRangeFlow: StateFlow<TimeRange?>
    fun updateTimeRange(range: TimeRange?)

    // Calculation progress (0-100)
    var calcProgressPct: Int

    // BG data - each series independent
    val bgReadingsFlow: StateFlow<List<BgDataPoint>>
    val bucketedDataFlow: StateFlow<List<BgDataPoint>>

    // Update methods for workers - each triggers only its flow
    fun updateBgReadings(data: List<BgDataPoint>)
    fun updateBucketedData(data: List<BgDataPoint>)

    // TODO: Add other graph data in future phases
    // val iobDataFlow: StateFlow<List<IobDataPoint>>
    // fun updateIobData(data: List<IobDataPoint>)
    // etc.

    fun reset()
}
