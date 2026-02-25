package nostalgia.memoir.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun PlaceholderSharedAlbumsScreen(modifier: Modifier = Modifier) {
    AlbumsScreen(
        title = "Shared Albums",
<<<<<<< HEAD
        isShared = true,
=======
        albums = MOCK_SHARED_ALBUMS,
>>>>>>> f39bbb794ca1090314fd13ffc6616e78bebb2c5c
        modifier = modifier,
    )
}
