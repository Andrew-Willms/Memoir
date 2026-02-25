package nostalgia.memoir.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun PlaceholderYourAlbumsScreen(modifier: Modifier = Modifier) {
    AlbumsScreen(
        title = "Your Albums",
<<<<<<< HEAD
        isShared = false,
=======
        albums = MOCK_MY_ALBUMS,
>>>>>>> f39bbb794ca1090314fd13ffc6616e78bebb2c5c
        modifier = modifier,
    )
}
