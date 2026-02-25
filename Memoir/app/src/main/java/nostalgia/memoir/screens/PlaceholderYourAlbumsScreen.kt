package nostalgia.memoir.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun PlaceholderYourAlbumsScreen(modifier: Modifier = Modifier) {
    AlbumsScreen(
        title = "Your Albums",
        albums = MOCK_MY_ALBUMS,
        modifier = modifier,
    )
}
