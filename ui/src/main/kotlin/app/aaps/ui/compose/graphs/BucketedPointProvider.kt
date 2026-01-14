package app.aaps.ui.compose.graphs

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import app.aaps.core.interfaces.overview.graph.BgDataPoint
import app.aaps.core.interfaces.overview.graph.BgRange
import com.patrykandpatrick.vico.core.cartesian.data.LineCartesianLayerModel
import com.patrykandpatrick.vico.core.cartesian.layer.LineCartesianLayer
import com.patrykandpatrick.vico.core.common.Fill
import com.patrykandpatrick.vico.core.common.component.ShapeComponent
import com.patrykandpatrick.vico.core.common.data.ExtraStore
import com.patrykandpatrick.vico.core.common.shape.CorneredShape

/**
 * PointProvider for BUCKETED BG data - colors points by range (LOW/IN_RANGE/HIGH).
 * Uses lookup map keyed by x-value to find the original BgDataPoint.
 */
@Immutable
class BucketedPointProvider(
    private val dataLookup: Map<Double, BgDataPoint>,
    lowColor: Color,
    inRangeColor: Color,
    highColor: Color
) : LineCartesianLayer.PointProvider {

    // Pre-build point components for efficiency
    private val lowPoint = createFilledPoint(lowColor)
    private val inRangePoint = createFilledPoint(inRangeColor)
    private val highPoint = createFilledPoint(highColor)

    private fun createFilledPoint(color: Color) = LineCartesianLayer.Point(
        component = ShapeComponent(
            fill = Fill(color.toArgb()),
            shape = CorneredShape.Pill
        ),
        sizeDp = 6f
    )

    override fun getPoint(
        entry: LineCartesianLayerModel.Entry,
        seriesIndex: Int,
        extraStore: ExtraStore
    ): LineCartesianLayer.Point? {
        val dataPoint = dataLookup[entry.x] ?: return inRangePoint // fallback

        return when (dataPoint.range) {
            BgRange.LOW      -> lowPoint
            BgRange.IN_RANGE -> inRangePoint
            BgRange.HIGH     -> highPoint
        }
    }

    override fun getLargestPoint(extraStore: ExtraStore): LineCartesianLayer.Point = inRangePoint
}
