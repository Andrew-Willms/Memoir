package nostalgia.memoir

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.adaptive.navigationsuite.ExperimentalMaterial3AdaptiveNavigationSuiteApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import nostalgia.memoir.screens.*
import nostalgia.memoir.ui.theme.MemoirTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MemoirTheme {
                MemoirApp()
            }
        }
    }
}

enum class AppDestinations(
    val label: String,
    val drawableId: Int,
) {
    HOME("Home", R.drawable.home_tab_icon),
    YOUR_ALBUMS("Your Albums", R.drawable.your_albums_tab_icon),
    CAMERA("Camera", R.drawable.camera_tab_icon),
    SHARED_ALBUMS("Shared", R.drawable.shared_albums_tab_icon),
    SEARCH("Search", R.drawable.search_tab_icon),
}

@Composable
fun MemoirApp() {
    val context = LocalContext.current
    var showOnboarding by remember {
        mutableStateOf(!hasCompletedNewPhotosOnboarding(context))
    }

    if (showOnboarding) {
        NewPhotosOnboardingScreen(
            onComplete = { showOnboarding = false },
            modifier = Modifier.fillMaxSize(),
        )
    } else {
        MainNavigation()
    }
}

@OptIn(ExperimentalMaterial3AdaptiveNavigationSuiteApi::class)
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
                            ImageBitmap.imageResource(it.drawableId),
                            contentDescription = it.label,
                            tint = Color.Unspecified
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