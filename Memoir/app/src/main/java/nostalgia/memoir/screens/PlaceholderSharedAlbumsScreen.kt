package nostalgia.memoir.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun PlaceholderSharedAlbumsScreen(modifier: Modifier = Modifier) {
    AlbumsScreen(
        title = "Shared Albums",
        isShared = true,
        modifier = modifier,
    )
}
