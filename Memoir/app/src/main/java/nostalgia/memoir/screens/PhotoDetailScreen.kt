package nostalgia.memoir.screens

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.delay
import nostalgia.memoir.screens.common.AssetImage
import nostalgia.memoir.screens.data.StoredAlbum
import nostalgia.memoir.screens.data.addPhotoToAlbum
import nostalgia.memoir.screens.data.createAlbum
import nostalgia.memoir.screens.data.isPhotoInAlbum
import nostalgia.memoir.screens.data.loadMyAlbums
import nostalgia.memoir.screens.data.loadSharedAlbums
import nostalgia.memoir.screens.data.removePhotoFromAlbum

private const val PREFS_NAME = "journal_entries"
private const val KEY_PREFIX = "journal_"
private val MOCK_JOURNAL_ENTRIES = mapOf(
    "photos/1.jpg" to "A beautiful day at the beach.",
    "photos/2.jpg" to "Family dinner – everyone together.",
    "photos/3.jpg" to "Sunset over the mountains.",
)

private fun loadJournalEntry(context: Context, assetPath: String): String {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    prefs.getString(KEY_PREFIX + assetPath, null)?.let { return it }
    return MOCK_JOURNAL_ENTRIES[assetPath] ?: "Write your thoughts..."
}

private fun saveJournalEntry(context: Context, assetPath: String, text: String) {
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .edit()
        .putString(KEY_PREFIX + assetPath, text)
        .apply()
}

@Composable
fun PhotoDetailScreen(
    assetPath: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    isNewPhoto: Boolean = false,
    photoIndex: Int = 1,
    totalPhotos: Int = 1,
    onNext: (() -> Unit)? = null,
    requireJournal: Boolean = false,
) {
    val context = LocalContext.current
    var journalText by remember(assetPath) {
        mutableStateOf(if (isNewPhoto) "" else loadJournalEntry(context, assetPath))
    }

    var refreshTrigger by remember { mutableStateOf(0) }
    var showAddMyAlbum by remember { mutableStateOf(false) }
    var showAddSharedAlbum by remember { mutableStateOf(false) }
    val myAlbums = remember(refreshTrigger) { loadMyAlbums(context) }
    val sharedAlbums = remember(refreshTrigger) { loadSharedAlbums(context) }

    val canProceed = !requireJournal || journalText.trim().isNotBlank()

    LaunchedEffect(journalText) {
        if (journalText.isNotBlank()) {
            delay(500)
            saveJournalEntry(context, assetPath, journalText)
        }
    }

    fun onAlbumToggle(album: StoredAlbum, checked: Boolean) {
        if (checked) {
            addPhotoToAlbum(context, album.id, assetPath)
        } else {
            removePhotoFromAlbum(context, album.id, assetPath)
        }
        refreshTrigger += 1
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (!isNewPhoto) {
                Text(
                    text = "← Back",
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .clickable {
                            saveJournalEntry(context, assetPath, journalText)
                            onBack()
                        },
                    style = MaterialTheme.typography.bodyLarge,
                )
            }

            if (isNewPhoto) {
                Text(
                    text = "New Photo • $photoIndex of $totalPhotos",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
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
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (!isNewPhoto) {
                Text(
                    text = "Add to albums",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )

                Text(
                    text = "Your Albums",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                myAlbums.forEach { album ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val current = isPhotoInAlbum(context, album.id, assetPath)
                                onAlbumToggle(album, !current)
                            },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(
                            checked = isPhotoInAlbum(context, album.id, assetPath),
                            onCheckedChange = { onAlbumToggle(album, it) },
                        )
                        Text(text = album.name, modifier = Modifier.padding(start = 8.dp))
                    }
                }
                TextButton(onClick = { showAddMyAlbum = true }) {
                    Text("+ Add album")
                }

                Text(
                    text = "Shared Albums",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                sharedAlbums.forEach { album ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val current = isPhotoInAlbum(context, album.id, assetPath)
                                onAlbumToggle(album, !current)
                            },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(
                            checked = isPhotoInAlbum(context, album.id, assetPath),
                            onCheckedChange = { onAlbumToggle(album, it) },
                        )
                        Text(text = album.name, modifier = Modifier.padding(start = 8.dp))
                    }
                }
                TextButton(onClick = { showAddSharedAlbum = true }) {
                    Text("+ Add album")
                }
            }

            Text(
                text = "Journal Entry" + if (requireJournal) " (required)" else "",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )

            OutlinedTextField(
                value = journalText,
                onValueChange = { journalText = it },
                modifier = Modifier.fillMaxWidth(),
                minLines = 4,
                maxLines = 12,
                placeholder = {
                    Text(
                        if (isNewPhoto) "Write your thoughts about this new photo..."
                        else "Write your thoughts...",
                    )
                },
            )

            if (onNext != null) {
                Button(
                    onClick = {
                        saveJournalEntry(context, assetPath, journalText)
                        onNext()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = canProceed,
                ) {
                    Text(text = if (photoIndex < totalPhotos) "Next" else "Done")
                }
            }
        }
    }

    if (showAddMyAlbum) {
        AddAlbumDialog(
            onDismiss = { showAddMyAlbum = false },
            onConfirm = { name ->
                createAlbum(context, name, isShared = false)
                refreshTrigger += 1
                showAddMyAlbum = false
            },
        )
    }

    if (showAddSharedAlbum) {
        AddAlbumDialog(
            onDismiss = { showAddSharedAlbum = false },
            onConfirm = { name ->
                createAlbum(context, name, isShared = true)
                refreshTrigger += 1
                showAddSharedAlbum = false
            },
        )
    }
}

@Composable
private fun AddAlbumDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var albumName by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth(),
        ) {
            Text(
                text = "New Album",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            OutlinedTextField(
                value = albumName,
                onValueChange = { albumName = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                placeholder = { Text("Album name") },
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onDismiss) { Text("Cancel") }
                TextButton(
                    onClick = {
                        val trimmed = albumName.trim()
                        if (trimmed.isNotBlank()) onConfirm(trimmed)
                    },
                    enabled = albumName.trim().isNotBlank(),
                ) {
                    Text("Create")
                }
            }
        }
    }
}
