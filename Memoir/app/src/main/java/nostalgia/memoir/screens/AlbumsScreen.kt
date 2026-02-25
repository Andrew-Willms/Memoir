package nostalgia.memoir.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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

@Composable
fun AlbumsScreen(
    title: String,
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

    if (selectedAlbum != null) {
        val album = selectedAlbum!!
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
                        .clickable { selectedAlbum = null },
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    text = album.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
            PhotoGridContent(
                title = "",
                photoPaths = album.photoPaths,
                modifier = Modifier.fillMaxSize(),
                emptyMessage = "No photos in this album",
            )
        }
    } else {
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
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedAlbum = album }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = album.name,
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        }
                    }
                },
            )
        }
    }
}
