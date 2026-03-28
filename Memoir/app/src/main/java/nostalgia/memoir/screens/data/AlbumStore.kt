package nostalgia.memoir.screens.data

import android.content.Context

data class StoredAlbum(
    val id: String,
    val name: String,
    val isShared: Boolean,
)

fun loadMyAlbums(context: Context): List<StoredAlbum> =
    BackendUiBridge.listAlbums(context, isShared = false)

fun loadSharedAlbums(context: Context): List<StoredAlbum> =
    BackendUiBridge.listAlbums(context, isShared = true)

fun searchAlbums(context: Context, query: String): List<StoredAlbum> =
    BackendUiBridge.searchAlbums(context, query)

fun loadPhotosInAlbum(context: Context, albumId: String): Set<String> =
    BackendUiBridge.loadAlbumPhotoPaths(context, albumId)

fun isPhotoInAlbum(context: Context, albumId: String, assetPath: String): Boolean =
    assetPath in loadPhotosInAlbum(context, albumId)

fun addPhotoToAlbum(context: Context, albumId: String, assetPath: String) {
    BackendUiBridge.setPhotoInAlbum(context, albumId, assetPath, present = true)
}

fun removePhotoFromAlbum(context: Context, albumId: String, assetPath: String) {
    BackendUiBridge.setPhotoInAlbum(context, albumId, assetPath, present = false)
}

fun deleteAlbum(context: Context, albumId: String) {
    BackendUiBridge.deleteAlbum(context, albumId)
}

fun createAlbum(context: Context, name: String, isShared: Boolean): StoredAlbum =
    BackendUiBridge.createAlbum(context, name, isShared)
