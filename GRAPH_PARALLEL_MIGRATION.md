# Graph Parallel Migration

## Current Status

| Phase | Description                                  | Status |
|-------|----------------------------------------------|--------|
| 1     | Foundation (domain models, cache, ViewModel) | ✅ Done |
| 2     | Worker modifications (BG, Bucketed)          | ✅ Done |
| 3     | Vico Composables (BgGraphCompose)            | ✅ Done |
| 4     | Integration (MainScreen)                     | ✅ Done |
| 5     | Extract to OverviewScreen                    | ✅ Done |

**✅ BG Graph Data Migration: COMPLETED**
- BG Graph fully working with real data, zoom, and scroll
- Overview screen extracted to dedicated composable
- Ready to add more graph elements

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

### 6. Scroll + Zoom Configuration

```kotlin
scrollState = rememberVicoScrollState(
    scrollEnabled = true,
    initialScroll = remember { Scroll.Absolute.End }  // Start at most recent
),
zoomState = rememberVicoZoomState(
    zoomEnabled = true,
    initialZoom = remember { Zoom.x(360.0) }  // 6 hours (360 minutes)
)
```

### 7. Smoothed Values

**Use `.recalculated` not `.value` for bucketed data:**

```kotlin
val valueInUnits = profileUtil.fromMgdlToUnits(inMemoryGlucoseValue.recalculated)
```

- `.recalculated` = `smoothed ?: value` (smoothed with fallback)
- `.value` = original unsmoothed reading

### 8. Time Range in First Worker

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
- `ui/.../compose/overview/ProfileChip.kt`
- `ui/.../compose/overview/TempTargetChip.kt`
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

## Next Steps

### Phase 6: Additional Graph Elements (In Progress)

- [ ] Add predictions to BG graph (IOB, COB predictions)
- [ ] Add target range area to BG graph
- [ ] Add temp basal overlays
- [ ] Add bolus markers
- [ ] Add carbs markers

### Future Phases

- [ ] Create IOB graph (IobGraphCompose.kt)
- [ ] Create COB graph (CobGraphCompose.kt)
- [ ] Add other graph types (Activity, Deviations, Basal, etc.)
- [ ] Profile graph integration
