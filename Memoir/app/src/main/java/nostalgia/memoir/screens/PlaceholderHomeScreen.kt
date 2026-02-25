package nostalgia.memoir.screens

import android.content.res.AssetManager
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import nostalgia.memoir.ui.theme.MemoirTheme

private val IMAGE_EXTENSIONS = setOf("jpg", "jpeg", "png", "webp", "gif")

private fun listImagesFromFolder(assets: AssetManager, folder: String): List<String> =
    assets.list(folder)
        ?.filter { name ->
            val ext = name.substringAfterLast('.', "").lowercase()
            ext in IMAGE_EXTENSIONS
        }
        ?.map { "$folder/$it" }
        ?: emptyList()

@Composable
fun PlaceholderHomeScreen(
    modifier: Modifier = Modifier,
    onPhotoClick: (String) -> Unit = {},
) {
    val context = LocalContext.current
    val photos = remember {
        listImagesFromFolder(context.assets, "photos").sorted()
    }

    if (photos.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "Add photos to app/src/main/assets/photos/\n(.jpg, .png, etc.)",
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
            items(photos) { assetPath ->
                AssetImage(
                    assetPath = assetPath,
                    modifier = Modifier
                        .aspectRatio(1f)
                        .padding(2.dp)
                        .clickable { onPhotoClick(assetPath) },
                    contentScale = ContentScale.Crop,
                )
            }
        }
    }
}

@Composable
private fun AssetImage(
    assetPath: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
) {
    val context = LocalContext.current
    var bitmap by remember(assetPath) { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(assetPath) {
        bitmap = withContext(Dispatchers.IO) {
            runCatching {
                context.assets.open(assetPath).use { stream ->
                    BitmapFactory.decodeStream(stream)?.asImageBitmap()
                }
            }.getOrNull()
        }
    }

    bitmap?.let { bmp ->
        Image(
            bitmap = bmp,
            contentDescription = null,
            modifier = modifier,
            contentScale = contentScale,
        )
    } ?: Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(modifier = Modifier.padding(16.dp))
    }
}

@Preview(showBackground = true)
@Composable
private fun PlaceholderHomeScreenPreview() {
    MemoirTheme {
        PlaceholderHomeScreen(modifier = Modifier.fillMaxSize())
    }
}
