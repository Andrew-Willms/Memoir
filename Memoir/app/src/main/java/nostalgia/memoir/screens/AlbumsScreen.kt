package nostalgia.memoir.screens

import androidx.compose.foundation.clickable
<<<<<<< HEAD
=======
import androidx.compose.foundation.layout.fillMaxWidth
>>>>>>> f39bbb794ca1090314fd13ffc6616e78bebb2c5c
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
<<<<<<< HEAD
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
=======
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
>>>>>>> f39bbb794ca1090314fd13ffc6616e78bebb2c5c
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
<<<<<<< HEAD
import androidx.compose.ui.window.Dialog
import nostalgia.memoir.screens.common.PhotoGridContent
import nostalgia.memoir.screens.common.listImagesFromFolder
import nostalgia.memoir.screens.data.StoredAlbum
import nostalgia.memoir.screens.data.createAlbum
import nostalgia.memoir.screens.data.loadMyAlbums
import nostalgia.memoir.screens.data.loadPhotosInAlbum
import nostalgia.memoir.screens.data.loadSharedAlbums
=======
import nostalgia.memoir.screens.common.PhotoGridContent
import nostalgia.memoir.screens.common.listImagesFromFolder

data class Album(
    val id: String,
    val name: String,
    val photoPaths: List<String>,
)

internal val MOCK_MY_ALBUMS: List<Album> = listOf(
    Album(id = "1", name = "Vacation 2024", photoPaths = emptyList()),
    Album(id = "2", name = "Family", photoPaths = emptyList()),
    Album(id = "3", name = "Favorites", photoPaths = emptyList()),
)

internal val MOCK_SHARED_ALBUMS: List<Album> = listOf(
    Album(id = "s1", name = "Trip with Friends", photoPaths = emptyList()),
    Album(id = "s2", name = "Wedding Photos", photoPaths = emptyList()),
)
>>>>>>> f39bbb794ca1090314fd13ffc6616e78bebb2c5c

@Composable
fun AlbumsScreen(
    title: String,
<<<<<<< HEAD
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
=======
    albums: List<Album>,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var selectedAlbum by remember { mutableStateOf<Album?>(null) }

    val albumsWithPhotos = remember(albums, context) {
        val allPhotos = runCatching {
            listImagesFromFolder(context.assets, "photos").sorted().take(12)
        }.getOrElse { emptyList() }
        if (allPhotos.isEmpty()) albums
        else albums.mapIndexed { index, album ->
            val start = (index * allPhotos.size / albums.size).coerceAtMost(allPhotos.lastIndex)
            val end = ((index + 1) * allPhotos.size / albums.size).coerceAtLeast(start + 1)
                .coerceAtMost(allPhotos.size)
            album.copy(photoPaths = allPhotos.slice(start until end))
        }
    }

    var selectedPhotoPath by remember { mutableStateOf<String?>(null) }
>>>>>>> f39bbb794ca1090314fd13ffc6616e78bebb2c5c

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
<<<<<<< HEAD
            val photoPaths = remember(refreshTrigger, album.id) {
                loadPhotosInAlbum(context, album.id).toList().sorted()
            }
            Column(modifier = modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
=======
            Column(modifier = modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
>>>>>>> f39bbb794ca1090314fd13ffc6616e78bebb2c5c
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "← Back",
<<<<<<< HEAD
                        modifier = Modifier.padding(end = 8.dp).clickable { selectedAlbum = null },
                        style = androidx.compose.material3.MaterialTheme.typography.bodyLarge,
                    )
                    Text(
                        text = album.name,
                        style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
=======
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .clickable { selectedAlbum = null },
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Text(
                        text = album.name,
                        style = MaterialTheme.typography.titleLarge,
>>>>>>> f39bbb794ca1090314fd13ffc6616e78bebb2c5c
                        fontWeight = FontWeight.Bold,
                    )
                }
                PhotoGridContent(
                    title = "",
<<<<<<< HEAD
                    photoPaths = photoPaths,
                    modifier = Modifier.fillMaxSize(),
                    emptyMessage = "No photos in this album. Add photos from My Gallery.",
=======
                    photoPaths = album.photoPaths,
                    modifier = Modifier.fillMaxSize(),
                    emptyMessage = "No photos in this album",
>>>>>>> f39bbb794ca1090314fd13ffc6616e78bebb2c5c
                    onPhotoClick = { selectedPhotoPath = it },
                )
            }
        }
        else -> {
<<<<<<< HEAD
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
=======
        Column(modifier = modifier.fillMaxSize()) {
            Text(
                text = title,
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                content = {
                    items(albumsWithPhotos) { album ->
>>>>>>> f39bbb794ca1090314fd13ffc6616e78bebb2c5c
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedAlbum = album }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = album.name,
<<<<<<< HEAD
                                style = androidx.compose.material3.MaterialTheme.typography.bodyLarge,
                            )
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
=======
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        }
                    }
                },
            )
        }
>>>>>>> f39bbb794ca1090314fd13ffc6616e78bebb2c5c
        }
    }
}
