package nostalgia.memoir

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import nostalgia.memoir.screens.*
import nostalgia.memoir.ui.theme.MemoirTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MemoirTheme {
                MainNavigation()
            }
        }
    }
}

enum class AppDestinations(
    val label: String,
    val drawableId: Int,
) {
    HOME("Home", R.drawable.ic_launcher_background),
    YOUR_ALBUMS("Your Albums", R.drawable.ic_launcher_background),
    CAMERA("Camera", R.drawable.ic_launcher_background),
    SHARED_ALBUMS("Shared", R.drawable.ic_launcher_background),
    SEARCH("Search", R.drawable.ic_launcher_background),
}

@PreviewScreenSizes
@Composable
fun MainNavigation() {
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.HOME) }

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            AppDestinations.entries.forEach {
                item(
                    icon = {
                        Icon(
                            ImageVector.vectorResource(it.drawableId),
                            contentDescription = it.label
                        )
                    },
                    label = { Text(it.label) },
                    selected = it == currentDestination,
                    onClick = { currentDestination = it }
                )
            }
        }
    ) {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            when (currentDestination) {
                AppDestinations.HOME -> PlaceholderHomeScreen(modifier = Modifier.fillMaxSize().padding(innerPadding))
                AppDestinations.YOUR_ALBUMS -> PlaceholderYourAlbumsScreen(Modifier.fillMaxSize().padding(innerPadding))
                AppDestinations.CAMERA -> PlaceholderCameraScreen(Modifier.fillMaxSize().padding(innerPadding))
                AppDestinations.SHARED_ALBUMS -> PlaceholderSharedAlbumsScreen(Modifier.fillMaxSize().padding(innerPadding))
                AppDestinations.SEARCH ->
                    if (BuildConfig.DEBUG) {
                        DatabaseSelfTestScreen(Modifier.fillMaxSize().padding(innerPadding))
                    } else {
                        PlaceholderSearchScreen(Modifier.fillMaxSize().padding(innerPadding))
                    }
            }
        }
    }
}