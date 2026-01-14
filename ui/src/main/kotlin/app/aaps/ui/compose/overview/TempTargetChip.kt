package app.aaps.ui.compose.overview

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import app.aaps.core.ui.compose.AapsTheme
import app.aaps.ui.compose.main.TempTargetChipState

@Composable
fun TempTargetChip(
    targetText: String,
    state: TempTargetChipState,
    progress: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor = when (state) {
        TempTargetChipState.Active   -> AapsTheme.generalColors.inProgress
        TempTargetChipState.Adjusted -> AapsTheme.generalColors.adjusted
        TempTargetChipState.None     -> MaterialTheme.colorScheme.surfaceVariant
    }
    val contentColor = when (state) {
        TempTargetChipState.Active   -> AapsTheme.generalColors.onInProgress
        TempTargetChipState.Adjusted -> AapsTheme.generalColors.onAdjusted
        TempTargetChipState.None     -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = containerColor,
        modifier = modifier.fillMaxWidth()
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Icon(
                    painter = painterResource(app.aaps.core.ui.R.drawable.ic_crosstarget),
                    contentDescription = null,
                    tint = contentColor
                )
                Text(
                    text = targetText,
                    color = contentColor,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
            if (progress > 0f) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(3.dp),
                    color = contentColor.copy(alpha = 0.7f),
                    trackColor = contentColor.copy(alpha = 0.2f)
                )
            }
        }
    }
}
