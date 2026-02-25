package nostalgia.memoir.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import nostalgia.memoir.screens.common.AssetImage

internal val MOCK_JOURNAL_ENTRIES: Map<String, String> = buildMap {
    put("photos/1.jpg", "A beautiful day at the beach. The waves were perfect.")
    put("photos/2.jpg", "Family dinner – everyone together again.")
    put("photos/3.jpg", "Sunset over the mountains. Worth the hike.")
    put("photos/4.jpg", "Birthday party memories. Best cake ever!")
    put("photos/5.jpg", "First day of the trip. So excited!")
    put("photos/6.jpg", "Found this hidden gem. Need to come back.")
}

private fun getJournalEntryForPhoto(assetPath: String): String =
    MOCK_JOURNAL_ENTRIES[assetPath] ?: "Write your thoughts about this moment..."

@Composable
fun PhotoDetailScreen(
    assetPath: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var journalText by remember(assetPath) {
        mutableStateOf(getJournalEntryForPhoto(assetPath))
    }

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "← Back",
                modifier = Modifier
                    .padding(end = 8.dp)
                    .clickable { onBack() },
                style = MaterialTheme.typography.bodyLarge,
            )
        }
        AssetImage(
            assetPath = assetPath,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .aspectRatio(1f),
            contentScale = ContentScale.Crop,
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "Journal Entry",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            OutlinedTextField(
                value = journalText,
                onValueChange = { journalText = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                minLines = 4,
                maxLines = 12,
                placeholder = { Text("Write your thoughts...") },
            )
        }
    }
}
