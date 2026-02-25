package nostalgia.memoir.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import nostalgia.memoir.ui.theme.MemoirTheme

/**
 * Hardcoded list of photo filenames in assets/photos/.
 * Add your demo images to app/src/main/assets/photos/ and list them here.
 */
private val DEMO_PHOTO_FILES = listOf(
    "1.jpg",
    "2.jpg",
    "3.jpg",
    "4.jpg",
    "5.jpg",
    "6.jpg",
)

@Composable
fun PlaceholderHomeScreen(
    photoFiles: List<String> = DEMO_PHOTO_FILES,
    modifier: Modifier = Modifier,
    onPhotoClick: (String) -> Unit = {},
) {
    val context = LocalContext.current
    val photos = remember(photoFiles) {
        val available = context.assets.list("photos")?.toSet() ?: emptySet()
        photoFiles.filter { it in available }
    }

    if (photos.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "Add photos to app/src/main/assets/photos/\n(1.jpg, 2.jpg, ...)",
                modifier = Modifier.padding(24.dp),
            )
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            items(photos) { filename ->
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data("file:///android_asset/photos/$filename")
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    modifier = Modifier
                        .aspectRatio(1f)
                        .padding(2.dp)
                        .clickable { onPhotoClick(filename) },
                    contentScale = ContentScale.Crop,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PlaceholderHomeScreenPreview() {
    MemoirTheme {
        PlaceholderHomeScreen(
            photoFiles = emptyList(),
            modifier = Modifier.fillMaxSize(),
        )
    }
}
