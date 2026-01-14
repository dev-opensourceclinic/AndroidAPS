package app.aaps.ui.compose.graphs

import app.aaps.core.interfaces.overview.graph.BgDataPoint
import app.aaps.core.interfaces.overview.graph.CalculatedGraphDataCache
import app.aaps.core.interfaces.overview.graph.TimeRange
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of CalculatedGraphDataCache using MutableStateFlow.
 * Singleton cache populated by workers, observed by GraphViewModel.
 *
 * Architecture: Independent Series Updates
 * - Each series has its own MutableStateFlow
 * - Workers update their series independently
 * - Each update triggers only that series' flow
 * - ViewModel collects each flow separately for granular recomposition
 *
 * MIGRATION NOTE: This coexists with OverviewDataImpl during migration.
 * Workers populate both. After migration complete, OverviewDataImpl will be deleted.
 */
@Singleton
class CalculatedGraphDataCacheImpl @Inject constructor() : CalculatedGraphDataCache {

    override var calcProgressPct: Int = 100

    // Time range - observable for UI to react to range changes
    private val _timeRangeFlow = MutableStateFlow<TimeRange?>(null)
    override val timeRangeFlow: StateFlow<TimeRange?> = _timeRangeFlow.asStateFlow()

    // Independent series flows - each can update without affecting others
    private val _bgReadingsFlow = MutableStateFlow<List<BgDataPoint>>(emptyList())
    private val _bucketedDataFlow = MutableStateFlow<List<BgDataPoint>>(emptyList())

    override val bgReadingsFlow: StateFlow<List<BgDataPoint>> = _bgReadingsFlow.asStateFlow()
    override val bucketedDataFlow: StateFlow<List<BgDataPoint>> = _bucketedDataFlow.asStateFlow()

    // Update methods - each triggers only its own flow
    override fun updateTimeRange(range: TimeRange?) {
        _timeRangeFlow.value = range
    }

    override fun updateBgReadings(data: List<BgDataPoint>) {
        _bgReadingsFlow.value = data
    }

    override fun updateBucketedData(data: List<BgDataPoint>) {
        _bucketedDataFlow.value = data
    }

    override fun reset() {
        _timeRangeFlow.value = null
        _bgReadingsFlow.value = emptyList()
        _bucketedDataFlow.value = emptyList()
        calcProgressPct = 100
    }
}
