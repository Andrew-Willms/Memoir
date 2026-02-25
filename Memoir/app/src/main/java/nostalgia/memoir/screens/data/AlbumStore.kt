package nostalgia.memoir.screens.data

import android.content.Context
import java.util.UUID

data class StoredAlbum(
    val id: String,
    val name: String,
    val isShared: Boolean,
)

private const val PREFS_NAME = "album_store"
private const val KEY_MY_ALBUMS = "my_albums"
private const val KEY_SHARED_ALBUMS = "shared_albums"
private const val KEY_ALBUM_PHOTOS = "album_photos_"
private const val SEP = "|"
private const val SEP_ENTRY = ";"

fun loadMyAlbums(context: Context): List<StoredAlbum> =
    loadAlbums(context, KEY_MY_ALBUMS, isShared = false)

fun loadSharedAlbums(context: Context): List<StoredAlbum> =
    loadAlbums(context, KEY_SHARED_ALBUMS, isShared = true)

private fun loadAlbums(context: Context, key: String, isShared: Boolean): List<StoredAlbum> {
    val raw = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .getString(key, "") ?: ""
    if (raw.isBlank()) return emptyList()
    return raw.split(SEP_ENTRY).mapNotNull { entry ->
        val parts = entry.split(SEP)
        if (parts.size >= 2) StoredAlbum(id = parts[0], name = parts[1], isShared = isShared)
        else null
    }
}

fun saveMyAlbums(context: Context, albums: List<StoredAlbum>) {
    saveAlbums(context, KEY_MY_ALBUMS, albums)
}

fun saveSharedAlbums(context: Context, albums: List<StoredAlbum>) {
    saveAlbums(context, KEY_SHARED_ALBUMS, albums)
}

private fun saveAlbums(context: Context, key: String, albums: List<StoredAlbum>) {
    val raw = albums.joinToString(SEP_ENTRY) { "${it.id}${SEP}${it.name}" }
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .edit()
        .putString(key, raw)
        .apply()
}

fun loadPhotosInAlbum(context: Context, albumId: String): Set<String> {
    val raw = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .getString(KEY_ALBUM_PHOTOS + albumId, "") ?: ""
    if (raw.isBlank()) return emptySet()
    return raw.split(",").filter { it.isNotBlank() }.toSet()
}

fun savePhotosInAlbum(context: Context, albumId: String, assetPaths: Set<String>) {
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .edit()
        .putString(KEY_ALBUM_PHOTOS + albumId, assetPaths.joinToString(","))
        .apply()
}

fun isPhotoInAlbum(context: Context, albumId: String, assetPath: String): Boolean =
    assetPath in loadPhotosInAlbum(context, albumId)

fun addPhotoToAlbum(context: Context, albumId: String, assetPath: String) {
    val current = loadPhotosInAlbum(context, albumId).toMutableSet()
    current.add(assetPath)
    savePhotosInAlbum(context, albumId, current)
}

fun removePhotoFromAlbum(context: Context, albumId: String, assetPath: String) {
    val current = loadPhotosInAlbum(context, albumId).toMutableSet()
    current.remove(assetPath)
    savePhotosInAlbum(context, albumId, current)
}

fun deleteAlbum(context: Context, albumId: String) {
    val myAlbums = loadMyAlbums(context)
    val sharedAlbums = loadSharedAlbums(context)

    if (myAlbums.any { it.id == albumId }) {
        saveMyAlbums(context, myAlbums.filter { it.id != albumId })
    } else {
        saveSharedAlbums(context, sharedAlbums.filter { it.id != albumId })
    }

    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .edit()
        .remove(KEY_ALBUM_PHOTOS + albumId)
        .apply()
}

fun createAlbum(context: Context, name: String, isShared: Boolean): StoredAlbum {
    val albums = if (isShared) loadSharedAlbums(context) else loadMyAlbums(context)
    val newAlbum = StoredAlbum(id = UUID.randomUUID().toString(), name = name, isShared = isShared)
    val updated = albums + newAlbum
    if (isShared) saveSharedAlbums(context, updated) else saveMyAlbums(context, updated)
    return newAlbum
}
