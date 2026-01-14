package app.aaps.ui.compose.graphs

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import app.aaps.core.interfaces.overview.graph.BgDataPoint
import app.aaps.core.ui.compose.AapsTheme
import app.aaps.ui.compose.graphs.viewmodels.GraphViewModel
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStart
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoScrollState
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoZoomState
import com.patrykandpatrick.vico.compose.common.component.rememberTextComponent
import com.patrykandpatrick.vico.compose.common.component.shapeComponent
import com.patrykandpatrick.vico.compose.common.shape.rounded
import com.patrykandpatrick.vico.core.cartesian.Scroll
import com.patrykandpatrick.vico.core.cartesian.Zoom
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import com.patrykandpatrick.vico.core.cartesian.decoration.HorizontalBox
import com.patrykandpatrick.vico.core.cartesian.layer.LineCartesianLayer
import com.patrykandpatrick.vico.core.common.Fill
import com.patrykandpatrick.vico.core.common.component.ShapeComponent
import com.patrykandpatrick.vico.core.common.shape.CorneredShape
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.time.Instant

/** Series identifiers */
private const val SERIES_REGULAR = "regular"
private const val SERIES_BUCKETED = "bucketed"

/** Convert timestamp to x-value (whole minutes from start) */
private fun timestampToX(timestamp: Long, minTimestamp: Long): Double =
    ((timestamp - minTimestamp) / 60000).toDouble()

/**
 * BG Graph using Vico with independent series updates.
 *
 * Architecture:
 * - Each series (regular, bucketed) collected independently
 * - Separate LaunchedEffect per series
 * - Series registry tracks current data
 * - Chart rebuilds only when specific series changes
 *
 * Rendering:
 * - Regular BG: White outlined circles (STROKE style)
 * - Bucketed BG: Filled circles colored by range
 */
@OptIn(kotlin.time.ExperimentalTime::class)
@Composable
fun BgGraphCompose(
    viewModel: GraphViewModel,
    modifier: Modifier = Modifier
) {
    // Collect flows independently - each triggers recomposition only when it changes
    val bgReadings by viewModel.bgReadingsFlow.collectAsState()
    val bucketedData by viewModel.bucketedDataFlow.collectAsState()
    val derivedTimeRange by viewModel.derivedTimeRange.collectAsState()
    val chartConfig = viewModel.chartConfig

    // Show loading state if no time range yet
    if (derivedTimeRange == null) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(200.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("Loading graph data...", color = MaterialTheme.colorScheme.onSurface)
        }
        return
    }

    val (minTimestamp, _) = derivedTimeRange!!

    // Single model producer shared by all series
    val modelProducer = remember { CartesianChartModelProducer() }

    // Series registry - tracks current data for each series
    // Using mutableStateMapOf for Compose-aware updates
    val seriesRegistry = remember { mutableStateMapOf<String, List<BgDataPoint>>() }

    // Colors from theme (stable - won't change)
    val regularColor = AapsTheme.generalColors.originalBgValue
    val lowColor = AapsTheme.generalColors.bgLow
    val inRangeColor = AapsTheme.generalColors.bgInRange
    val highColor = AapsTheme.generalColors.bgHigh

    // Function to rebuild chart from registry
    suspend fun rebuildChart() {
        val regularPoints = seriesRegistry[SERIES_REGULAR] ?: emptyList()
        val bucketedPoints = seriesRegistry[SERIES_BUCKETED] ?: emptyList()

        if (regularPoints.isEmpty() && bucketedPoints.isEmpty()) return

        modelProducer.runTransaction {
            lineSeries {
                // Series 1: REGULAR points (always first if present)
                if (regularPoints.isNotEmpty()) {
                    series(
                        x = regularPoints.map { timestampToX(it.timestamp, minTimestamp) },
                        y = regularPoints.map { it.value }
                    )
                }
                // Series 2: BUCKETED points
                if (bucketedPoints.isNotEmpty()) {
                    series(
                        x = bucketedPoints.map { timestampToX(it.timestamp, minTimestamp) },
                        y = bucketedPoints.map { it.value }
                    )
                }
            }
        }
    }

    // LaunchedEffect for REGULAR series - only runs when bgReadings changes
    LaunchedEffect(bgReadings, minTimestamp) {
        seriesRegistry[SERIES_REGULAR] = bgReadings
        rebuildChart()
    }

    // LaunchedEffect for BUCKETED series - only runs when bucketedData changes
    LaunchedEffect(bucketedData, minTimestamp) {
        seriesRegistry[SERIES_BUCKETED] = bucketedData
        rebuildChart()
    }

    // Build lookup map for BUCKETED points: x-value -> BgDataPoint (for PointProvider)
    val bucketedLookup = remember(bucketedData, minTimestamp) {
        bucketedData.associateBy { timestampToX(it.timestamp, minTimestamp) }
    }

    // Create point provider for BUCKETED series (colors by range)
    val bucketedPointProvider = remember(bucketedLookup, lowColor, inRangeColor, highColor) {
        BucketedPointProvider(bucketedLookup, lowColor, inRangeColor, highColor)
    }

    // Time formatter for X axis
    val timeFormatter = remember(minTimestamp) {
        val dateFormat = SimpleDateFormat("HH", Locale.getDefault())
        CartesianValueFormatter { _, value, _ ->
            val timestamp = minTimestamp + (value * 60000).toLong()
            dateFormat.format(Date(timestamp))
        }
    }

    // Line for REGULAR: White outlined circles (STROKE style)
    val regularLine = remember(regularColor) {
        LineCartesianLayer.Line(
            fill = LineCartesianLayer.LineFill.single(Fill(Color.Transparent.toArgb())),
            areaFill = null,
            pointProvider = LineCartesianLayer.PointProvider.single(
                LineCartesianLayer.Point(
                    component = ShapeComponent(
                        fill = Fill(Color.Transparent.toArgb()),
                        shape = CorneredShape.Pill,
                        strokeFill = Fill(regularColor.copy(alpha = 0.3f).toArgb()),
                        strokeThicknessDp = 1f
                    ),
                    sizeDp = 6f
                )
            )
        )
    }

    // Line for BUCKETED: Filled circles with PointProvider for range coloring
    val bucketedLine = remember(bucketedPointProvider) {
        LineCartesianLayer.Line(
            fill = LineCartesianLayer.LineFill.single(Fill(Color.Transparent.toArgb())),
            areaFill = null,
            pointProvider = bucketedPointProvider
        )
    }

    // Build lines list based on available data
    val lines = remember(bgReadings.isNotEmpty(), bucketedData.isNotEmpty(), regularLine, bucketedLine) {
        buildList {
            if (bgReadings.isNotEmpty()) add(regularLine)
            if (bucketedData.isNotEmpty()) add(bucketedLine)
        }
    }

    // Target range background (static - from chartConfig)
    val targetRangeColor = AapsTheme.generalColors.bgTargetRangeArea
    val targetRangeBoxComponent = shapeComponent(Fill(targetRangeColor.toArgb()), CorneredShape.rounded(0.dp))
    val targetRangeBox = remember(chartConfig.lowMark, chartConfig.highMark, targetRangeBoxComponent) {
        HorizontalBox(
            y = { chartConfig.lowMark..chartConfig.highMark },
            box = targetRangeBoxComponent
        )
    }
    val decorations = remember(targetRangeBox) { listOf(targetRangeBox) }

    // X-axis item placer: whole hour intervals
    val bottomAxisItemPlacer = remember(minTimestamp) {
        val instant = Instant.fromEpochMilliseconds(minTimestamp)
        val localDateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
        val minutesIntoHour = localDateTime.minute
        val offsetToNextHour = if (minutesIntoHour == 0) 0 else 60 - minutesIntoHour

        HorizontalAxis.ItemPlacer.aligned(
            spacing = { 60 },
            offset = { offsetToNextHour }
        )
    }

    CartesianChartHost(
        chart = rememberCartesianChart(
            rememberLineCartesianLayer(
                lineProvider = LineCartesianLayer.LineProvider.series(lines)
            ),
            startAxis = VerticalAxis.rememberStart(
                label = rememberTextComponent(
                    color = MaterialTheme.colorScheme.onSurface
                )
            ),
            bottomAxis = HorizontalAxis.rememberBottom(
                valueFormatter = timeFormatter,
                itemPlacer = bottomAxisItemPlacer,
                label = rememberTextComponent(
                    color = MaterialTheme.colorScheme.onSurface
                )
            ),
            decorations = decorations
        ),
        modelProducer = modelProducer,
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp),
        scrollState = rememberVicoScrollState(
            scrollEnabled = true,
            initialScroll = remember { Scroll.Absolute.End }
        ),
        zoomState = rememberVicoZoomState(
            zoomEnabled = true,
            initialZoom = remember { Zoom.x(360.0) }
        )
    )
}
