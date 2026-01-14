package app.aaps.ui.compose.overview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.aaps.ui.compose.graphs.BgGraphCompose
import app.aaps.ui.compose.graphs.viewmodels.GraphViewModel

/**
 * Overview graphs section using Vico charts.
 *
 * Architecture: Independent Series Updates
 * - Each series (BG readings, bucketed, etc.) has its own StateFlow
 * - BgGraphCompose collects each flow separately
 * - Only the changed series triggers chart update
 *
 * PHASE 1: BG graphs only (readings + bucketed)
 */
@Composable
fun OverviewGraphsSection(
    graphViewModel: GraphViewModel,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // BG Graph - handles its own loading state internally
        BgGraphCompose(
            viewModel = graphViewModel,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
