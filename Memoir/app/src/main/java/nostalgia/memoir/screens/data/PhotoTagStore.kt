package nostalgia.memoir.screens.data

import android.content.Context

data class StoredPhotoTag(
    val type: StoredPhotoTagType,
    val value: String,
)

data class PhotoSearchResult(
    val assetPath: String,
    val matchingTags: List<StoredPhotoTag>,
    val matchingJournalPreviews: List<String> = emptyList(),
)

enum class StoredPhotoTagType {
    PERSON,
    LOCATION,
    KEYWORD,
}

fun loadTagsForPhoto(context: Context, assetPath: String): List<StoredPhotoTag> =
    BackendUiBridge.loadPhotoTags(context, assetPath)

fun addTagToPhoto(context: Context, assetPath: String, tag: StoredPhotoTag) {
    BackendUiBridge.setTagOnPhoto(context, assetPath, tag, present = true)
}

fun removeTagFromPhoto(context: Context, assetPath: String, tag: StoredPhotoTag) {
    BackendUiBridge.setTagOnPhoto(context, assetPath, tag, present = false)
}

fun searchPhotos(context: Context, query: String): List<PhotoSearchResult> =
    BackendUiBridge.searchPhotos(context, query)

fun searchPhotosByTags(context: Context, query: String): List<PhotoSearchResult> =
    searchPhotos(context, query)
