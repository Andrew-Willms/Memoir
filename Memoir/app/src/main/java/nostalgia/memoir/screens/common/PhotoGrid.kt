package nostalgia.memoir.screens.common

import android.content.res.AssetManager
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

internal fun listImagesFromFolder(assets: AssetManager, folder: String): List<String> =
    assets.list(folder)
        ?.filter { name ->
            val ext = name.substringAfterLast('.', "").lowercase()
            ext in setOf("jpg", "jpeg", "png", "webp", "gif")
        }
        ?.map { "$folder/$it" }
        ?: emptyList()

@Composable
fun PhotoGridContent(
    title: String,
    photoPaths: List<String>,
    modifier: Modifier = Modifier,
    columns: Int = 2,
    emptyMessage: String = "No photos",
    onPhotoClick: (String) -> Unit = {},
) {
    Column(modifier = modifier.fillMaxSize()) {
        if (title.isNotBlank()) {
            Text(
                text = title,
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
        }

        if (photoPaths.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = emptyMessage,
                    modifier = Modifier.padding(24.dp),
                )
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
