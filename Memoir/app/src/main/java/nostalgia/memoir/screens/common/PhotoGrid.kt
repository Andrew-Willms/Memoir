package nostalgia.memoir.screens.common

<<<<<<< HEAD
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
=======
import android.content.res.AssetManager
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
>>>>>>> f39bbb794ca1090314fd13ffc6616e78bebb2c5c
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
<<<<<<< HEAD
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

internal fun listImagesFromFolder(assets: android.content.res.AssetManager, folder: String): List<String> =
=======
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val MAX_THUMBNAIL_SIZE = 512

internal fun AssetManager.decodeScaledBitmap(assetPath: String): android.graphics.Bitmap? {
    open(assetPath).use { stream ->
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeStream(stream, null, bounds)
        val (w, h) = bounds.outWidth to bounds.outHeight
        if (w <= 0 || h <= 0) return null
        val sampleSize = maxOf(1, maxOf(w, h) / MAX_THUMBNAIL_SIZE)
        open(assetPath).use { decodeStream ->
            val opts = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inJustDecodeBounds = false
            }
            return BitmapFactory.decodeStream(decodeStream, null, opts)
        }
    }
}

internal fun listImagesFromFolder(assets: AssetManager, folder: String): List<String> =
>>>>>>> f39bbb794ca1090314fd13ffc6616e78bebb2c5c
    assets.list(folder)
        ?.filter { name ->
            val ext = name.substringAfterLast('.', "").lowercase()
            ext in setOf("jpg", "jpeg", "png", "webp", "gif")
        }
        ?.map { "$folder/$it" }
        ?: emptyList()

@Composable
<<<<<<< HEAD
=======
fun AssetImage(
    assetPath: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
) {
    val context = LocalContext.current
    var bitmap by remember(assetPath) { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(assetPath) {
        bitmap = withContext(Dispatchers.IO) {
            runCatching {
                context.assets.decodeScaledBitmap(assetPath)?.asImageBitmap()
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

@Composable
>>>>>>> f39bbb794ca1090314fd13ffc6616e78bebb2c5c
fun PhotoGridContent(
    title: String,
    photoPaths: List<String>,
    modifier: Modifier = Modifier,
    columns: Int = 2,
    emptyMessage: String = "No photos",
    onPhotoClick: (String) -> Unit = {},
) {
<<<<<<< HEAD
    androidx.compose.foundation.layout.Column(modifier = modifier.fillMaxSize()) {
=======
    Column(modifier = modifier.fillMaxSize()) {
>>>>>>> f39bbb794ca1090314fd13ffc6616e78bebb2c5c
        if (title.isNotBlank()) {
            Text(
                text = title,
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
        }
        if (photoPaths.isEmpty()) {
<<<<<<< HEAD
            androidx.compose.foundation.layout.Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = androidx.compose.ui.Alignment.Center,
            ) {
                Text(text = emptyMessage, modifier = Modifier.padding(24.dp))
=======
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = emptyMessage,
                    modifier = Modifier.padding(24.dp),
                )
>>>>>>> f39bbb794ca1090314fd13ffc6616e78bebb2c5c
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(columns),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items(photoPaths) { assetPath ->
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
}
