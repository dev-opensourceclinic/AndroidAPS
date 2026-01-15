# Graph Parallel Migration

## Current Status

| Phase | Description                                  | Status |
|-------|----------------------------------------------|--------|
| 1     | Foundation (domain models, cache, ViewModel) | ✅ Done |
| 2     | Worker modifications (BG, Bucketed)          | ✅ Done |
| 3     | Vico Composables (BgGraphCompose)            | ✅ Done |
| 4     | Integration (MainScreen)                     | ✅ Done |
| 5     | Secondary Graph Data Flows                   | ✅ Done |

**✅ BG Graph Data Migration: COMPLETED**
- BG Graph fully working with real data, zoom, and scroll
- Ready to add more graph elements

**✅ Phase 5: Secondary Graph Data Flows: COMPLETED**
- Extracted data from PrepareIobAutosensGraphDataWorker to cache flows
- 9 graph-level flows (IOB, AbsIOB, COB, Activity, BGI, Deviations, Ratio, DevSlope, VarSens)
- Data layer only - ready for future graph composables

---

## Architecture

### Module Organization

| Module             | Content                                                 |
|--------------------|---------------------------------------------------------|
| `:core:interfaces` | Domain models, cache interface                          |
| `:ui`              | Graph Composables, GraphViewModel, Cache implementation |
| `:workflow`        | Worker modifications                                    |

### Data Flow (Independent Series)

```
PrepareBgDataWorker       → cache.bgReadingsFlow  ─┐
                                                   ├→ BgGraphCompose (separate LaunchedEffect per series)
PrepareBucketedDataWorker → cache.bucketedDataFlow ┘

Each series update triggers ONLY its own LaunchedEffect.
Time range derived from all series (recalculates as data arrives).
```

---

## Key Implementation Patterns

### 1. Independent Series Updates Pattern

Each series has its own StateFlow - workers update independently, UI collects separately:

```kotlin
// Cache interface - each series is independent
interface CalculatedGraphDataCache {
    val timeRangeFlow: StateFlow<TimeRange?>
    val bgReadingsFlow: StateFlow<List<BgDataPoint>>
    val bucketedDataFlow: StateFlow<List<BgDataPoint>>

    fun updateTimeRange(range: TimeRange?)
    fun updateBgReadings(data: List<BgDataPoint>)
    fun updateBucketedData(data: List<BgDataPoint>)
}

// Workers update their series independently
calculatedGraphDataCache.updateBgReadings(bgDataPoints)
calculatedGraphDataCache.updateBucketedData(bucketedDataPoints)

// ViewModel exposes flows directly (no combine for series)
val bgReadingsFlow: StateFlow<List<BgDataPoint>> = cache.bgReadingsFlow
val bucketedDataFlow: StateFlow<List<BgDataPoint>> = cache.bucketedDataFlow

// Derived time range recalculates as series arrive
val derivedTimeRange: StateFlow<Pair<Long, Long>?> = combine(
    cache.bgReadingsFlow, cache.bucketedDataFlow, cache.timeRangeFlow
) { bg, bucketed, cacheRange -> /* calculate bounds */ }
```

**Benefits:**

- True independence - each series updates without affecting others
- Granular recomposition - only changed series triggers UI update
- No RxBus dependency - pure Kotlin Flow architecture
- Time range recalculates automatically as data arrives

### 2. Composable Series Registry Pattern

Each series has its own LaunchedEffect - updates only when that series changes:

```kotlin
@Composable
fun BgGraphCompose(viewModel: GraphViewModel) {
    // Collect each flow independently
    val bgReadings by viewModel.bgReadingsFlow.collectAsState()
    val bucketedData by viewModel.bucketedDataFlow.collectAsState()
    val derivedTimeRange by viewModel.derivedTimeRange.collectAsState()

    val modelProducer = remember { CartesianChartModelProducer() }
    val seriesRegistry = remember { mutableStateMapOf<String, List<BgDataPoint>>() }

    // LaunchedEffect per series - only runs when its data changes
    LaunchedEffect(bgReadings, minTimestamp) {
        seriesRegistry["regular"] = bgReadings
        rebuildChart(seriesRegistry, modelProducer)
    }

    LaunchedEffect(bucketedData, minTimestamp) {
        seriesRegistry["bucketed"] = bucketedData
        rebuildChart(seriesRegistry, modelProducer)
    }
}
```

**Key points:**

- `mutableStateMapOf` for Compose-aware registry
- Each LaunchedEffect keyed to its specific series
- Shared `modelProducer` - Vico handles internal diffing
- `minTimestamp` as key ensures x-values recalculate when time range shifts

### 3. Worker Dual Output Pattern

Workers populate BOTH old (GraphView) and new (Compose) systems:

- Keep existing `OverviewData` mutations (marked `MIGRATION: DELETE`)
- Add new `calculatedGraphDataCache.updateXxx()` calls (marked `MIGRATION: KEEP`)
- Both outputs generated in same pass (efficient)

### 4. X-Value Calculation (Critical)

**Use whole minutes, not timestamps or fractional hours:**

```kotlin
val x = ((timestamp - minTimestamp) / 60000).toDouble()  // Whole minutes
```

**Why this matters:**

1. **Raw timestamps cause repeating labels:** Vico creates ticks by incrementing x by 1. With
   millisecond timestamps, ticks are 1ms apart - all format to same time (e.g., "16:04" repeated
   everywhere)
2. **Fractional hours cause precision errors:** `(timestamp - start) / 3600000.0` creates infinite
   decimals (0.083333...). Vico requires max 4 decimal places.
3. **Whole minutes work:** Integer division first, then convert to Double. 5-minute BG readings =
   5.0, 10.0, etc. (no decimals)

### 5. X-Axis Labels at Whole Hours

```kotlin
val minutesIntoHour = Instant.fromEpochMilliseconds(startTime)
    .toLocalDateTime(TimeZone.currentSystemDefault()).minute
val offsetToNextHour = if (minutesIntoHour == 0) 0 else 60 - minutesIntoHour

HorizontalAxis.ItemPlacer.aligned(spacing = { 60 }, offset = { offsetToNextHour })
```

### 5. Time Range in First Worker

PrepareBucketedDataWorker runs first - must set `timeRange`:

```kotlin
if (calculatedGraphDataCache.timeRangeFlow.value == null) {
    calculatedGraphDataCache.updateTimeRange(TimeRange(
        fromTime = toTime - T.hours(Constants.GRAPH_TIME_RANGE_HOURS.toLong()).msecs(),
        toTime = toTime,
        endTime = toTime
    ))
}
```
All following migrated Workers must adjust time range to support new system
---

## Domain Models

**File:** `core/interfaces/.../graph/CalculationResults.kt`

Key types:

- `BgDataPoint` - timestamp, value, range (HIGH/IN_RANGE/LOW), type (REGULAR/BUCKETED)
- `BgType` - REGULAR, BUCKETED, IOB_PREDICTION, COB_PREDICTION, etc.
- `BgRange` - HIGH, IN_RANGE, LOW (for color mapping in UI)
- `TimeRange` - fromTime, toTime, endTime

---

## Files Modified/Created

### New Files

- `ui/.../compose/graphs/viewmodels/GraphViewModel.kt`
- `ui/.../compose/graphs/BgGraphCompose.kt`
- `ui/.../compose/graphs/BucketedPointProvider.kt`
- `ui/.../compose/graphs/CalculatedGraphDataCacheImpl.kt`
- `ui/.../compose/overview/OverviewScreen.kt`
- `ui/.../compose/overview/OverviewGraphsSection.kt`
- `core/interfaces/.../graph/CalculationResults.kt`
- `core/interfaces/.../graph/CalculatedGraphDataCache.kt`

### Modified Workers

- `workflow/.../PrepareBgDataWorker.kt` - Dual output, 24h data
- `workflow/.../PrepareBucketedDataWorker.kt` - Creates BgDataPoint, sets timeRange

### Theme

- `core/ui/.../GeneralColors.kt` - Added `bgHigh`, `bgInRange`, `bgLow`, `bgTargetRangeArea`

---

## Cleanup Guide (After Migration Complete)

### Search Patterns

- `MIGRATION: DELETE` - Delete all marked code
- `MIGRATION: KEEP (replace with)` - Uncomment replacement code

### Files to Delete

- `plugins/main/.../overview/OverviewDataImpl.kt`
- `plugins/main/.../overview/graphData/GraphData.kt`
- `core/graphview/` (entire module)

### Dependencies to Remove

```gradle
implementation(project(":core:graphview"))
api(libs.jjoe64.graphview)
```

### Cleanup Checklist

- [ ] Delete `MIGRATION: DELETE` code blocks
- [ ] Uncomment `MIGRATION: KEEP (replace with)` code
- [ ] Delete OverviewDataImpl, GraphData, graphview module
- [ ] Remove GraphView from build.gradle.kts
- [ ] Update worker data classes (remove OverviewData parameter)
- [ ] Delete graph code from OverviewFragment
- [ ] Clean build compiles
- [ ] All tests pass

---

## Phase 5: Secondary Graph Data Flows

**Status:** ✅ Done

**Goal:** Extract data from `PrepareIobAutosensGraphDataWorker` to cache flows. Data layer only - no UI.

### Architecture Decision: One Flow Per Graph

Data is produced in a single worker loop but consumed by different graphs. Group flows by graph consumer:

| Graph | Flow | Data Contents |
|-------|------|---------------|
| IOB | `iobGraphFlow` | iob line + predictions points |
| AbsIOB | `absIobGraphFlow` | absolute iob line |
| COB | `cobGraphFlow` | cob line + failover points |
| Activity | `activityGraphFlow` | activity line + prediction line |
| BGI | `bgiGraphFlow` | bgi line + prediction line |
| Deviations | `deviationsGraphFlow` | deviation bars (with color type) |
| Ratio | `ratioGraphFlow` | autosens ratio line |
| DevSlope | `devSlopeGraphFlow` | dsMax + dsMin lines |
| VarSens | `varSensGraphFlow` | variable sensitivity line |

**Total: 9 graph-level flows**

### Domain Models (to add in CalculationResults.kt)

```kotlin
/** Generic data point for line graphs */
data class GraphDataPoint(
    val timestamp: Long,
    val value: Double
)

/** Deviation bar with color classification */
data class DeviationDataPoint(
    val timestamp: Long,
    val value: Double,
    val deviationType: DeviationType
)

enum class DeviationType {
    POSITIVE,    // Green - above expected
    NEGATIVE,    // Red - below expected
    EQUAL,       // Black/gray - as expected
    UAM,         // UAM color
    CSF          // Gray - carb absorption
}

/** COB failover marker point */
data class CobFailOverPoint(
    val timestamp: Long,
    val cobValue: Double
)

// Graph-level data classes
data class IobGraphData(
    val iob: List<GraphDataPoint>,
    val predictions: List<GraphDataPoint>
)

data class AbsIobGraphData(
    val absIob: List<GraphDataPoint>
)

data class CobGraphData(
    val cob: List<GraphDataPoint>,
    val failOverPoints: List<CobFailOverPoint>
)

data class ActivityGraphData(
    val activity: List<GraphDataPoint>,
    val activityPrediction: List<GraphDataPoint>
)

data class BgiGraphData(
    val bgi: List<GraphDataPoint>,
    val bgiPrediction: List<GraphDataPoint>
)

data class DeviationsGraphData(
    val deviations: List<DeviationDataPoint>
)

data class RatioGraphData(
    val ratio: List<GraphDataPoint>
)

data class DevSlopeGraphData(
    val dsMax: List<GraphDataPoint>,
    val dsMin: List<GraphDataPoint>
)

data class VarSensGraphData(
    val varSens: List<GraphDataPoint>
)
```

### Cache Interface Additions (CalculatedGraphDataCache.kt)

```kotlin
// Secondary graph flows (one per graph)
val iobGraphFlow: StateFlow<IobGraphData?>
val absIobGraphFlow: StateFlow<AbsIobGraphData?>
val cobGraphFlow: StateFlow<CobGraphData?>
val activityGraphFlow: StateFlow<ActivityGraphData?>
val bgiGraphFlow: StateFlow<BgiGraphData?>
val deviationsGraphFlow: StateFlow<DeviationsGraphData?>
val ratioGraphFlow: StateFlow<RatioGraphData?>
val devSlopeGraphFlow: StateFlow<DevSlopeGraphData?>
val varSensGraphFlow: StateFlow<VarSensGraphData?>

// Update methods
fun updateIobGraph(data: IobGraphData)
fun updateAbsIobGraph(data: AbsIobGraphData)
fun updateCobGraph(data: CobGraphData)
fun updateActivityGraph(data: ActivityGraphData)
fun updateBgiGraph(data: BgiGraphData)
fun updateDeviationsGraph(data: DeviationsGraphData)
fun updateRatioGraph(data: RatioGraphData)
fun updateDevSlopeGraph(data: DevSlopeGraphData)
fun updateVarSensGraph(data: VarSensGraphData)
```

### Worker Modifications (PrepareIobAutosensGraphDataWorker.kt)

**Critical: Use cache time range (24h), not overviewData range**

All workers must use the same 24h time range for consistency:

```kotlin
// MIGRATION: KEEP - Get time range from cache (24h)
val cacheTimeRange = calculatedGraphDataCache.timeRangeFlow.value
val newFromTime = cacheTimeRange?.fromTime ?: fromTime  // fallback to legacy
val newEndTime = cacheTimeRange?.endTime ?: endTime

// Use newFromTime/newEndTime for Compose data collection
// Keep original fromTime/endTime for legacy GraphView output
```

**Dual loop approach:**
- Legacy arrays: use `fromTime`/`endTime` (overviewData, 6h default)
- Compose lists: use `newFromTime`/`newEndTime` (cache, 24h)

Or **single extended loop:**
- Extend loop to 24h
- Both legacy and Compose get same data
- Legacy GraphView will just display the user's selected range anyway

Add cache updates at end of `doWorkAndLog()`:

```kotlin
// MIGRATION: KEEP - Compose cache updates
calculatedGraphDataCache.updateIobGraph(IobGraphData(
    iob = iobListCompose,
    predictions = iobPredictionsListCompose
))
// ... similar for other 8 graphs
```

### Files to Modify

| File | Changes |
|------|---------|
| `core/interfaces/.../graph/CalculationResults.kt` | Add domain models |
| `core/interfaces/.../graph/CalculatedGraphDataCache.kt` | Add 9 flows + update methods |
| `ui/.../compose/graphs/CalculatedGraphDataCacheImpl.kt` | Implement 9 flows |
| `workflow/.../PrepareIobAutosensGraphDataWorker.kt` | Inject cache, use 24h range, add cache updates |

### Worker Injection Addition

```kotlin
class PrepareIobAutosensGraphDataWorker(...) : LoggingWorker(...) {
    // ... existing injections ...
    @Inject lateinit var calculatedGraphDataCache: CalculatedGraphDataCache  // ADD THIS
```

### Time Range Pattern (from PrepareBgDataWorker)

```kotlin
// Keep original range for legacy GraphView
val fromTimeOld = data.overviewData.fromTime
val endTimeOld = data.overviewData.endTime

// Get 24h range for Compose from cache
val cacheRange = calculatedGraphDataCache.timeRangeFlow.value
val fromTimeNew = cacheRange?.fromTime ?: fromTimeOld
val endTimeNew = cacheRange?.endTime ?: endTimeOld

// Use extended range for data loop (covers both systems)
val fromTime = min(fromTimeOld, fromTimeNew)
val endTime = max(endTimeOld, endTimeNew)
```

### Migration Markers (consistency)

Use same markers as BG workers:
```kotlin
// MIGRATION: KEEP - Core dependencies
// MIGRATION: DELETE - Remove after migration
// ========== MIGRATION: DELETE - Start GraphView-specific code ==========
// ========== MIGRATION: KEEP - Start Compose/Vico code ==========
```

### Reset Method Update (CalculatedGraphDataCacheImpl)

**Pattern: Non-nullable flows with empty data default** (consistent with BG flows)

```kotlin
// Initialize with empty data (not null)
private val _iobGraphFlow = MutableStateFlow(IobGraphData(emptyList(), emptyList()))

override fun reset() {
    // existing BG flows...
    _timeRangeFlow.value = null
    _bgReadingsFlow.value = emptyList()
    _bucketedDataFlow.value = emptyList()

    // Secondary graph flows - reset to empty data (not null)
    _iobGraphFlow.value = IobGraphData(emptyList(), emptyList())
    _absIobGraphFlow.value = AbsIobGraphData(emptyList())
    _cobGraphFlow.value = CobGraphData(emptyList(), emptyList())
    // ... etc

    calcProgressPct = 100
}
```

### Checklist

- [x] Add domain models to CalculationResults.kt
- [x] Add flows to CalculatedGraphDataCache interface
- [x] Implement flows in CalculatedGraphDataCacheImpl
- [x] Inject CalculatedGraphDataCache in PrepareIobAutosensGraphDataWorker
- [x] Modify worker to use cache's 24h time range
- [x] Add dual output (9 cache updates) to worker
- [x] Compile and verify no errors

---

## Future Phases

### Phase 6: Secondary Graph Composables

**Step Graph Rendering:**
- Original GraphView used manual intermediate points (2 Y-values at same X) for step effect
- Vico doesn't support duplicate X values - requires function (one Y per X)
- Solution: Use `PointConnector.Square` for step graphs

**TODO:** Create custom `StepPointConnector` (copy of `Square` initially), adjust based on actual rendering results. The original had horizontal→vertical steps, `Square` does vertical→horizontal. May need to tweak or accept the difference.

| Graph | Connector | Reason |
|-------|-----------|--------|
| IOB | `StepPointConnector` | Step graph (original had step logic) |
| AbsIOB | `StepPointConnector` | Step graph (original had step logic) |
| COB | `StepPointConnector` | Step graph (original had step logic) |
| Activity | Default | Smooth line |
| BGI | Default | Smooth line |
| Deviations | N/A | Bar chart |
| Ratio | Default | Smooth line |
| DevSlope | Default | Smooth line |
| VarSens | Default | Smooth line |

- [ ] Create IobGraphCompose.kt
- [ ] Create CobGraphCompose.kt
- [ ] Create ActivityGraphCompose.kt
- [ ] Create BgiGraphCompose.kt
- [ ] Create DeviationsGraphCompose.kt
- [ ] Create RatioGraphCompose.kt
- [ ] Create DevSlopeGraphCompose.kt
- [ ] Create VarSensGraphCompose.kt

### Phase 7: Additional BG Graph Elements

- [ ] Add predictions to BG graph (IOB, COB predictions)
- [ ] Add target range area to BG graph
- [ ] Add temp basal overlays
- [ ] Add bolus markers
- [ ] Add carbs markers

### Phase 8: Integration & Cleanup

- [ ] Integrate secondary graphs into Overview
- [ ] Profile graph integration
- [ ] Delete legacy GraphView code
