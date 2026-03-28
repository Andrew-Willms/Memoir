package nostalgia.memoir.screens

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.delay
import nostalgia.memoir.screens.common.AssetImage
import nostalgia.memoir.screens.data.StoredAlbum
import nostalgia.memoir.screens.data.StoredPhotoTag
import nostalgia.memoir.screens.data.StoredPhotoTagType
import nostalgia.memoir.screens.data.addPhotoToAlbum
import nostalgia.memoir.screens.data.addTagToPhoto
import nostalgia.memoir.screens.data.createAlbum
import nostalgia.memoir.screens.data.isPhotoInAlbum
import nostalgia.memoir.screens.data.loadMyAlbums
import nostalgia.memoir.screens.data.loadSharedAlbums
import nostalgia.memoir.screens.data.loadTagsForPhoto
import nostalgia.memoir.screens.data.removePhotoFromAlbum
import nostalgia.memoir.screens.data.removeTagFromPhoto

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
    photoIndex: Int = 0,
    totalPhotos: Int = 0,
    onNext: (() -> Unit)? = null,
    requireJournal: Boolean = false,
) {
    val context = LocalContext.current
    var journalText by remember(assetPath) {
        mutableStateOf(loadJournalEntry(context, assetPath))
    }

    var refreshTrigger by remember { mutableStateOf(0) }
    var showAddMyAlbum by remember { mutableStateOf(false) }
    var showAddSharedAlbum by remember { mutableStateOf(false) }
    var showAddTag by remember { mutableStateOf(false) }
    val myAlbums = remember(refreshTrigger) { loadMyAlbums(context) }
    val sharedAlbums = remember(refreshTrigger) { loadSharedAlbums(context) }
    val photoTags = remember(refreshTrigger, assetPath) { loadTagsForPhoto(context, assetPath) }

    LaunchedEffect(journalText) {
        if (journalText.isNotBlank()) {
            delay(500)
            saveJournalEntry(context, assetPath, journalText)
        }
    }

    fun onAlbumToggle(album: StoredAlbum, checked: Boolean) {
        if (checked) addPhotoToAlbum(context, album.id, assetPath)
        else removePhotoFromAlbum(context, album.id, assetPath)
        refreshTrigger++
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        if (!isNewPhoto) {
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
                        .clickable {
                            saveJournalEntry(context, assetPath, journalText)
                            onBack()
                        },
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
        if (isNewPhoto) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = "New Photo ($photoIndex of $totalPhotos)",
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
        AssetImage(
            assetPath = assetPath,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .aspectRatio(1f),
            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
        )
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
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
            Text(
                text = "Tags",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            if (photoTags.isEmpty()) {
                Text(
                    text = "No tags added yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                TagBubbleRow(
                    tags = photoTags,
                    onTagClick = { tag ->
                        removeTagFromPhoto(context, assetPath, tag)
                        refreshTrigger++
                    },
                )
                Text(
                    text = "Tap a tag to remove it.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            TextButton(onClick = { showAddTag = true }) {
                Text("+ Add tag")
            }
            Text(
                text = "Journal Entry",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            OutlinedTextField(
                value = journalText,
                onValueChange = { journalText = it },
                modifier = Modifier.fillMaxWidth(),
                minLines = 4,
                maxLines = 12,
                placeholder = { Text("Write your thoughts...") },
            )
            if (onNext != null) {
                val defaultText = MOCK_JOURNAL_ENTRIES[assetPath] ?: "Write your thoughts..."
                val hasJournal = journalText.isNotBlank() && journalText != defaultText
                if (requireJournal && !hasJournal) {
                    Text(
                        text = "A journal entry is required before continuing.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                Button(
                    onClick = {
                        saveJournalEntry(context, assetPath, journalText)
                        onNext()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !requireJournal || hasJournal,
                ) {
                    Text(if (photoIndex < totalPhotos) "Next" else "Done")
                }
            }
        }
    }

    if (showAddMyAlbum) {
        AddAlbumDialog(
            onDismiss = { showAddMyAlbum = false },
            onConfirm = { name ->
                createAlbum(context, name, isShared = false)
                refreshTrigger++
                showAddMyAlbum = false
            },
        )
    }
    if (showAddSharedAlbum) {
        AddAlbumDialog(
            onDismiss = { showAddSharedAlbum = false },
            onConfirm = { name ->
                createAlbum(context, name, isShared = true)
                refreshTrigger++
                showAddSharedAlbum = false
            },
        )
    }
    if (showAddTag) {
        AddTagDialog(
            onDismiss = { showAddTag = false },
            onConfirm = { type, value ->
                addTagToPhoto(
                    context = context,
                    assetPath = assetPath,
                    tag = StoredPhotoTag(type = type, value = value),
                )
                refreshTrigger++
                showAddTag = false
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
        Surface(
            shape = RoundedCornerShape(16.dp),
            tonalElevation = 6.dp,
            color = MaterialTheme.colorScheme.surface,
        ) {
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
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                    placeholder = { Text("Album name") },
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    TextButton(
                        onClick = { if (albumName.trim().isNotBlank()) onConfirm(albumName.trim()) },
                        enabled = albumName.trim().isNotBlank(),
                    ) { Text("Create") }
                }
            }
        }
    }
}

@Composable
private fun AddTagDialog(
    onDismiss: () -> Unit,
    onConfirm: (StoredPhotoTagType, String) -> Unit,
) {
    var selectedType by remember { mutableStateOf(StoredPhotoTagType.PERSON) }
    var tagValue by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            tonalElevation = 6.dp,
            color = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "Add Tag",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                StoredPhotoTagType.values().forEach { type ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedType = type },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = selectedType == type,
                            onClick = { selectedType = type },
                        )
                        Text(
                            text = type.displayName,
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                }
                OutlinedTextField(
                    value = tagValue,
                    onValueChange = { tagValue = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(selectedType.placeholder) },
                    singleLine = true,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    TextButton(
                        onClick = { onConfirm(selectedType, tagValue.trim()) },
                        enabled = tagValue.trim().isNotBlank(),
                    ) { Text("Add") }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TagBubbleRow(
    tags: List<StoredPhotoTag>,
    onTagClick: (StoredPhotoTag) -> Unit,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        tags.forEach { tag ->
            TagBubble(
                tag = tag,
                modifier = Modifier.clickable { onTagClick(tag) },
            )
        }
    }
}

@Composable
private fun TagBubble(
    tag: StoredPhotoTag,
    modifier: Modifier = Modifier,
) {
    val containerColor = when (tag.type) {
        StoredPhotoTagType.PERSON -> MaterialTheme.colorScheme.secondaryContainer
        StoredPhotoTagType.LOCATION -> MaterialTheme.colorScheme.tertiaryContainer
        StoredPhotoTagType.KEYWORD -> MaterialTheme.colorScheme.primaryContainer
    }
    val contentColor = when (tag.type) {
        StoredPhotoTagType.PERSON -> MaterialTheme.colorScheme.onSecondaryContainer
        StoredPhotoTagType.LOCATION -> MaterialTheme.colorScheme.onTertiaryContainer
        StoredPhotoTagType.KEYWORD -> MaterialTheme.colorScheme.onPrimaryContainer
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(999.dp),
        color = containerColor,
    ) {
        Text(
            text = "${tag.type.displayName}: ${tag.value}",
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            color = contentColor,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

private val StoredPhotoTagType.displayName: String
    get() = when (this) {
        StoredPhotoTagType.PERSON -> "Person"
        StoredPhotoTagType.LOCATION -> "Location"
        StoredPhotoTagType.KEYWORD -> "Keyword"
    }

private val StoredPhotoTagType.placeholder: String
    get() = when (this) {
        StoredPhotoTagType.PERSON -> "Who is in the photo?"
        StoredPhotoTagType.LOCATION -> "Where was this taken?"
        StoredPhotoTagType.KEYWORD -> "Add a keyword"
    }
