package nostalgia.memoir.screens.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class StoredPhotoTag(
    val type: StoredPhotoTagType,
    val value: String,
)

enum class StoredPhotoTagType {
    PERSON,
    LOCATION,
    KEYWORD,
}

private const val PREFS_NAME = "album_store"
private const val KEY_PHOTO_TAGS = "photo_tags_"
private const val FIELD_TYPE = "type"
private const val FIELD_VALUE = "value"

fun loadTagsForPhoto(context: Context, assetPath: String): List<StoredPhotoTag> {
    val raw = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .getString(KEY_PHOTO_TAGS + assetPath, null)
        ?: return emptyList()

    return runCatching {
        val jsonArray = JSONArray(raw)
        buildList {
            for (index in 0 until jsonArray.length()) {
                val item = jsonArray.optJSONObject(index) ?: continue
                val typeName = item.optString(FIELD_TYPE)
                val value = item.optString(FIELD_VALUE).trim()
                val type = StoredPhotoTagType.values().firstOrNull { it.name == typeName } ?: continue
                if (value.isNotEmpty()) {
                    add(StoredPhotoTag(type = type, value = value))
                }
            }
        }
    }.getOrDefault(emptyList())
}

fun addTagToPhoto(context: Context, assetPath: String, tag: StoredPhotoTag) {
    val normalizedValue = tag.value.trim()
    if (normalizedValue.isEmpty()) return

    val current = loadTagsForPhoto(context, assetPath).toMutableList()
    val alreadyPresent = current.any { existing ->
        existing.type == tag.type && existing.value.equals(normalizedValue, ignoreCase = true)
    }
    if (alreadyPresent) return

    current += tag.copy(value = normalizedValue)
    saveTagsForPhoto(context, assetPath, current)
}

fun removeTagFromPhoto(context: Context, assetPath: String, tag: StoredPhotoTag) {
    val updated = loadTagsForPhoto(context, assetPath)
        .filterNot { existing ->
            existing.type == tag.type && existing.value.equals(tag.value.trim(), ignoreCase = true)
        }

    saveTagsForPhoto(context, assetPath, updated)
}

private fun saveTagsForPhoto(context: Context, assetPath: String, tags: List<StoredPhotoTag>) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    if (tags.isEmpty()) {
        prefs.edit().remove(KEY_PHOTO_TAGS + assetPath).apply()
        return
    }

    val jsonArray = JSONArray()
    tags.forEach { tag ->
        jsonArray.put(
            JSONObject()
                .put(FIELD_TYPE, tag.type.name)
                .put(FIELD_VALUE, tag.value.trim()),
        )
    }

    prefs.edit()
        .putString(KEY_PHOTO_TAGS + assetPath, jsonArray.toString())
        .apply()
}
