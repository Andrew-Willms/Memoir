package nostalgia.memoir.screens

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import nostalgia.memoir.screens.common.listImagesFromFolder

private const val PREFS_NAME = "journal_entries"
private const val KEY_ONBOARDING_COMPLETE = "new_photos_onboarding_complete"
private const val NEW_PHOTOS_COUNT = 3

fun hasCompletedNewPhotosOnboarding(context: Context): Boolean =
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .getBoolean(KEY_ONBOARDING_COMPLETE, false)

fun setNewPhotosOnboardingComplete(context: Context) {
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .edit()
        .putBoolean(KEY_ONBOARDING_COMPLETE, true)
        .apply()
}

@Composable
fun NewPhotosOnboardingScreen(
    onComplete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val newPhotoPaths = remember {
        runCatching {
            listImagesFromFolder(context.assets, "photos").sorted().take(NEW_PHOTOS_COUNT)
        }.getOrElse { emptyList() }
    }

    var currentIndex by remember { mutableStateOf(0) }
    val assetPath = newPhotoPaths.getOrNull(currentIndex)

    if (assetPath != null) {
        PhotoDetailScreen(
            assetPath = assetPath,
            onBack = { /* no-op, must complete */ },
            modifier = modifier.fillMaxSize(),
            isNewPhoto = true,
            photoIndex = currentIndex + 1,
            totalPhotos = newPhotoPaths.size,
            onNext = {
                if (currentIndex < newPhotoPaths.size - 1) {
                    currentIndex += 1
                } else {
                    setNewPhotosOnboardingComplete(context)
                    onComplete()
                }
            },
            requireJournal = true,
        )
    } else {
        LaunchedEffect(Unit) {
            setNewPhotosOnboardingComplete(context)
            onComplete()
        }
        Box(modifier = modifier.fillMaxSize())
    }
}
