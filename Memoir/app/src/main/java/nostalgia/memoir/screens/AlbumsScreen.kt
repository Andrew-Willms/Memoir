package nostalgia.memoir.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import nostalgia.memoir.screens.common.PhotoGridContent
import nostalgia.memoir.screens.common.listImagesFromFolder
import nostalgia.memoir.screens.data.StoredAlbum
import nostalgia.memoir.screens.data.createAlbum
import nostalgia.memoir.screens.data.loadMyAlbums
import nostalgia.memoir.screens.data.loadPhotosInAlbum
import nostalgia.memoir.screens.data.loadSharedAlbums
import nostalgia.memoir.screens.data.renameAlbum

@Composable
fun AlbumsScreen(
    title: String,
    isShared: Boolean,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var refreshTrigger by remember { mutableStateOf(0) }
    val albums = remember(refreshTrigger, isShared) {
        if (isShared) loadSharedAlbums(context) else loadMyAlbums(context)
    }

    var selectedAlbum by remember { mutableStateOf<StoredAlbum?>(null) }
    var selectedPhotoPath by remember { mutableStateOf<String?>(null) }
    var showAddAlbum by remember { mutableStateOf(false) }
    var albumToRename by remember { mutableStateOf<StoredAlbum?>(null) }

    when {
        selectedPhotoPath != null -> {
            PhotoDetailScreen(
                assetPath = selectedPhotoPath!!,
                onBack = { selectedPhotoPath = null },
                modifier = modifier.fillMaxSize(),
            )
        }
        selectedAlbum != null -> {
            val album = selectedAlbum!!
            val photoPaths = remember(refreshTrigger, album.id) {
                loadPhotosInAlbum(context, album.id).toList().sorted()
            }
            Column(modifier = modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "← Back",
                        modifier = Modifier.padding(end = 8.dp).clickable { selectedAlbum = null },
                        style = androidx.compose.material3.MaterialTheme.typography.bodyLarge,
                    )
                    Text(
                        text = album.name,
                        style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                }
                PhotoGridContent(
                    title = "",
                    photoPaths = photoPaths,
                    modifier = Modifier.fillMaxSize(),
                    emptyMessage = "No photos in this album. Add photos from My Gallery.",
                    onPhotoClick = { selectedPhotoPath = it },
                )
            }
        }
        else -> {
            Column(modifier = modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = title,
                        style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    TextButton(onClick = { showAddAlbum = true }) {
                        Text("+ Add album")
                    }
                }
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items(albums) { album ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedAlbum = album }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = album.name,
                                style = androidx.compose.material3.MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.weight(1f),
                            )
                            TextButton(onClick = { albumToRename = album }) {
                                Text("Rename")
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddAlbum) {
        AddAlbumDialog(
            onDismiss = { showAddAlbum = false },
            onConfirm = { name ->
                createAlbum(context, name, isShared)
                refreshTrigger++
                showAddAlbum = false
            },
        )
    }

    albumToRename?.let { album ->
        RenameAlbumDialog(
            currentName = album.name,
            onDismiss = { albumToRename = null },
            onConfirm = { newName ->
                renameAlbum(context, album.id, newName)
                refreshTrigger++
                albumToRename = null
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
                style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
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

@Composable
private fun RenameAlbumDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var albumName by remember { mutableStateOf(currentName) }
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth(),
        ) {
            Text(
                text = "Rename Album",
                style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
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
                    enabled = albumName.trim().isNotBlank() && albumName.trim() != currentName,
                ) { Text("Rename") }
            }
        }
    }
}
