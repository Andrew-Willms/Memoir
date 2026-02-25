package nostalgia.memoir.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import nostalgia.memoir.screens.common.PhotoGridContent
import nostalgia.memoir.screens.common.listImagesFromFolder
import nostalgia.memoir.ui.theme.MemoirTheme

@Composable
fun PlaceholderHomeScreen(
    modifier: Modifier = Modifier,
    onPhotoClick: (String) -> Unit = {},
) {
    val context = LocalContext.current
    val photos = remember {
        runCatching {
            listImagesFromFolder(context.assets, "photos").sorted()
        }.getOrElse { emptyList() }
    }

    PhotoGridContent(
        title = "My Gallery",
        photoPaths = photos,
        modifier = modifier.fillMaxSize(),
        emptyMessage = "Add photos to app/src/main/assets/photos/\n(.jpg, .png, etc.)",
        onPhotoClick = onPhotoClick,
    )
}

@Preview(showBackground = true)
@Composable
private fun PlaceholderHomeScreenPreview() {
    MemoirTheme {
        PlaceholderHomeScreen(modifier = Modifier.fillMaxSize())
    }
}
