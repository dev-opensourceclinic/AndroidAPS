package app.aaps.plugins.sync.nsShared.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.text.HtmlCompat
import app.aaps.core.interfaces.rx.events.EventNSClientNewLog
import app.aaps.plugins.sync.nsShared.viewmodel.NSClientViewModel

@Composable
fun NSClientScreen(
    viewModel: NSClientViewModel,
    onPausedChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val isPaused by viewModel.isPaused.observeAsState(false)
    val url by viewModel.url.observeAsState("")
    val status by viewModel.status.observeAsState("")
    val queue by viewModel.queue.observeAsState("")
    val logList by viewModel.logList.observeAsState(emptyList())

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 5.dp)
    ) {
        // URL Section
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "URL: ",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = url,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(start = 5.dp)
            )
        }

        // Paused Checkbox
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isPaused,
                onCheckedChange = onPausedChanged
            )
            Text(
                text = "Paused",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        // Status and Queue Section
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Status: ",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = status,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(end = 5.dp)
            )

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = "Queue: ",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = queue,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 5.dp)
            )
        }

        HorizontalDivider()

        // Log List
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            items(logList) { log ->
                LogItem(log = log)
            }
        }
    }
}

@Composable
fun LogItem(
    log: EventNSClientNewLog,
    modifier: Modifier = Modifier
) {
    val htmlText = log.toPreparedHtml().toString()
    val spanned = HtmlCompat.fromHtml(htmlText, HtmlCompat.FROM_HTML_MODE_COMPACT)

    Text(
        text = spanned.toString(),
        style = MaterialTheme.typography.bodySmall,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 8.dp)
    )
}
