package nostalgia.memoir.screens.data

import android.content.Context
import androidx.room.withTransaction
import kotlinx.coroutines.runBlocking
import nostalgia.memoir.data.local.MemoirDatabase
import nostalgia.memoir.data.local.MemoirDatabaseProvider
import nostalgia.memoir.data.local.entities.AlbumEntity
import nostalgia.memoir.data.local.entities.AlbumMemberEntity
import nostalgia.memoir.data.local.entities.AlbumMemberStatus
import nostalgia.memoir.data.local.entities.AlbumPhotoCrossRef
import nostalgia.memoir.data.local.entities.AlbumRole
import nostalgia.memoir.data.local.entities.AlbumVisibility
import nostalgia.memoir.data.local.entities.EntryPhotoCrossRef
import nostalgia.memoir.data.local.entities.JournalEntryEntity
import nostalgia.memoir.data.local.entities.PhotoAssetEntity
import nostalgia.memoir.data.local.entities.PhotoTagCrossRef
import nostalgia.memoir.data.local.entities.TagEntity
import nostalgia.memoir.data.local.entities.TagType
import java.util.UUID

private const val LEGACY_ALBUM_PREFS_NAME = "album_store"
private const val LEGACY_MY_ALBUMS_KEY = "my_albums"
private const val LEGACY_SHARED_ALBUMS_KEY = "shared_albums"
private const val LEGACY_ALBUM_PHOTOS_KEY_PREFIX = "album_photos_"
private const val LEGACY_PHOTO_TAGS_KEY_PREFIX = "photo_tags_"
private const val LEGACY_ALBUM_FIELD_SEPARATOR = "|"
private const val LEGACY_ALBUM_ENTRY_SEPARATOR = ";"
private const val LEGACY_JOURNAL_PREFS_NAME = "journal_entries"
private const val LEGACY_JOURNAL_KEY_PREFIX = "journal_"
private const val UI_BRIDGE_OWNER_USER_ID = "ui-bridge-owner"
private const val UI_JOURNAL_ENTRY_ID_PREFIX = "ui-photo-journal:"

internal object BackendUiBridge {

    @Volatile
    private var migrated = false

    private val migrationLock = Any()

    fun ensureMigrated(context: Context) {
        if (migrated) return
        synchronized(migrationLock) {
            if (migrated) return
            runBlocking {
                migrateLegacyState(context.applicationContext)
            }
            migrated = true
        }
    }

    fun listAlbums(context: Context, isShared: Boolean): List<StoredAlbum> =
        runCatching {
            ensureMigrated(context)
            val legacy = loadLegacyAlbums(context, isShared)
            if (legacy.isNotEmpty()) {
                legacy
            } else {
                runBlocking {
                    loadAlbumsFromBackend(context, isShared)
                }
            }
        }.getOrElse {
            loadLegacyAlbums(context, isShared)
        }

    fun searchAlbums(context: Context, query: String): List<StoredAlbum> {
        val normalizedQuery = query.trim()
        if (normalizedQuery.isBlank()) return emptyList()
        return runCatching {
            ensureMigrated(context)
            val legacyAlbums = loadLegacyAlbums(context, isShared = false) + loadLegacyAlbums(context, isShared = true)
            if (legacyAlbums.isNotEmpty()) {
                legacyAlbums
                    .filter { album -> album.name.contains(normalizedQuery, ignoreCase = true) }
                    .sortedWith(
                        compareBy<StoredAlbum> { !it.name.equals(normalizedQuery, ignoreCase = true) }
                            .thenBy { it.name.lowercase() },
                    )
            } else {
                runBlocking {
                    searchAlbumsFromBackend(context, normalizedQuery)
                }
            }
        }.getOrElse {
            (loadLegacyAlbums(context, isShared = false) + loadLegacyAlbums(context, isShared = true))
                .filter { album -> album.name.contains(normalizedQuery, ignoreCase = true) }
                .sortedWith(
                    compareBy<StoredAlbum> { !it.name.equals(normalizedQuery, ignoreCase = true) }
                        .thenBy { it.name.lowercase() },
                )
        }
    }

    fun createAlbum(context: Context, name: String, isShared: Boolean): StoredAlbum {
        val created = createLegacyAlbum(context, name, isShared)
        runCatching {
            ensureMigrated(context)
            runBlocking {
                synchronizeAlbumRecord(context.applicationContext, created)
            }
        }
        return created
    }

    fun deleteAlbum(context: Context, albumId: String) {
        deleteLegacyAlbum(context, albumId)
        runCatching {
            ensureMigrated(context)
            runBlocking {
                deleteAlbumFromBackend(context.applicationContext, albumId)
            }
        }
    }

    fun loadAlbumPhotoPaths(context: Context, albumId: String): Set<String> =
        runCatching {
            ensureMigrated(context)
            if (legacyAlbumExists(context, albumId)) {
                loadLegacyAlbumPhotoPaths(context, albumId)
            } else {
                runBlocking {
                    loadAlbumPhotoPathsFromBackend(context, albumId)
                }
            }
        }.getOrElse {
            loadLegacyAlbumPhotoPaths(context, albumId)
        }

    fun setPhotoInAlbum(context: Context, albumId: String, assetPath: String, present: Boolean) {
        val current = loadLegacyAlbumPhotoPaths(context, albumId).toMutableSet()
        if (present) {
            current.add(assetPath)
        } else {
            current.remove(assetPath)
        }
        saveLegacyAlbumPhotoPaths(context, albumId, current)

        runCatching {
            ensureMigrated(context)
            runBlocking {
                synchronizeAlbumPhotos(context.applicationContext, albumId)
            }
        }
    }

    fun loadPhotoTags(context: Context, assetPath: String): List<StoredPhotoTag> =
        runCatching {
            ensureMigrated(context)
            if (hasLegacyPhotoTagRecord(context, assetPath)) {
                loadLegacyPhotoTags(context, assetPath)
            } else {
                runBlocking {
                    loadPhotoTagsFromBackend(context, assetPath)
                }
            }
        }.getOrElse {
            loadLegacyPhotoTags(context, assetPath)
        }

    fun searchPhotos(context: Context, query: String): List<PhotoSearchResult> {
        val normalizedQuery = query.trim()
        if (normalizedQuery.isBlank()) return emptyList()
        return runCatching {
            ensureMigrated(context)
            runBlocking {
                searchPhotosFromBackend(context, normalizedQuery)
            }
        }.getOrElse {
            mergePhotoSearchResults(
                query = normalizedQuery,
                tagResults = loadLegacyPhotoTagEntries(context)
                    .mapNotNull { (assetPath, tags) ->
                        val matchingTags = tags.filter { tag -> tag.value.contains(normalizedQuery, ignoreCase = true) }
                        if (matchingTags.isEmpty()) {
                            null
                        } else {
                            PhotoSearchResult(assetPath = assetPath, matchingTags = matchingTags)
                        }
                    },
                journalMatches = loadLegacyJournalEntries(context)
                    .mapNotNull { (assetPath, reflectionText) ->
                        val preview = buildJournalMatchPreview(
                            title = null,
                            reflectionText = reflectionText,
                            query = normalizedQuery,
                        ) ?: return@mapNotNull null
                        assetPath to listOf(preview)
                    }
                    .toMap(),
            )
        }
    }

    fun searchPhotosByTags(context: Context, query: String): List<PhotoSearchResult> =
        searchPhotos(context, query)

    fun setTagOnPhoto(context: Context, assetPath: String, tag: StoredPhotoTag, present: Boolean) {
        val current = loadLegacyPhotoTags(context, assetPath).toMutableList()
        val normalizedTag = tag.copy(value = tag.value.trim())
        if (normalizedTag.value.isEmpty()) return

        val existingIndex = current.indexOfFirst { existing ->
            existing.type == normalizedTag.type && existing.value.equals(normalizedTag.value, ignoreCase = true)
        }

        if (present && existingIndex == -1) {
            current += normalizedTag
        } else if (!present && existingIndex != -1) {
            current.removeAt(existingIndex)
        }

        saveLegacyPhotoTags(context, assetPath, current)

        runCatching {
            ensureMigrated(context)
            runBlocking {
                synchronizePhotoTags(context.applicationContext, assetPath)
            }
        }
    }

    fun loadJournalEntry(context: Context, assetPath: String, defaultValue: String): String {
        val legacyValue = loadLegacyJournalEntry(context, assetPath)
        return runCatching {
            ensureMigrated(context)
            if (hasLegacyJournalRecord(context, assetPath)) {
                legacyValue ?: defaultValue
            } else {
                runBlocking {
                    loadJournalEntryFromBackend(context, assetPath)
                        ?: defaultValue
                }
            }
        }.getOrElse {
            legacyValue ?: defaultValue
        }
    }

    fun saveJournalEntry(context: Context, assetPath: String, text: String) {
        saveLegacyJournalEntry(context, assetPath, text)
        runCatching {
            ensureMigrated(context)
            runBlocking {
                synchronizeJournalEntry(context.applicationContext, assetPath)
            }
        }
    }

    internal fun resetForTests() {
        migrated = false
    }

    private suspend fun migrateLegacyState(context: Context) {
        val database = MemoirDatabaseProvider.getInstance(context)
        database.withTransaction {
            migrateAlbums(context, database)
            migratePhotoTags(context, database)
            migrateJournals(context, database)
        }
    }

    private suspend fun migrateAlbums(context: Context, database: MemoirDatabase) {
        val legacyAlbums = loadLegacyAlbums(context, isShared = false) + loadLegacyAlbums(context, isShared = true)
        legacyAlbums.forEachIndexed { index, album ->
            upsertAlbumEntity(
                database = database,
                album = album,
                createdAtHint = System.currentTimeMillis() + index,
            )
            synchronizeAlbumPhotos(database, context, album.id)
        }
    }

    private suspend fun migratePhotoTags(context: Context, database: MemoirDatabase) {
        loadLegacyPhotoTagEntries(context).keys.forEach { assetPath ->
            synchronizePhotoTags(database, context, assetPath)
        }
    }

    private suspend fun migrateJournals(context: Context, database: MemoirDatabase) {
        loadLegacyJournalEntries(context).keys.forEach { assetPath ->
            synchronizeJournalEntry(database, context, assetPath)
        }
    }

    private suspend fun loadAlbumsFromBackend(context: Context, isShared: Boolean): List<StoredAlbum> {
        val database = MemoirDatabaseProvider.getInstance(context)
        val visibility = if (isShared) AlbumVisibility.SHARED else AlbumVisibility.PRIVATE
        return database.albumDao()
            .getByOwnerAndVisibility(UI_BRIDGE_OWNER_USER_ID, visibility.name)
            .map { album -> album.toStoredAlbum() }
    }

    private suspend fun searchAlbumsFromBackend(context: Context, query: String): List<StoredAlbum> {
        val database = MemoirDatabaseProvider.getInstance(context)
        return listOf(AlbumVisibility.PRIVATE, AlbumVisibility.SHARED)
            .flatMap { visibility ->
                database.albumDao()
                    .searchByOwnerAndVisibility(UI_BRIDGE_OWNER_USER_ID, visibility.name, query)
                    .map { album -> album.toStoredAlbum() }
            }
    }

    private suspend fun deleteAlbumFromBackend(context: Context, albumId: String) {
        val database = MemoirDatabaseProvider.getInstance(context)
        database.albumDao().deleteById(albumId)
    }

    private suspend fun loadAlbumPhotoPathsFromBackend(context: Context, albumId: String): Set<String> {
        val database = MemoirDatabaseProvider.getInstance(context)
        return database.albumPhotoDao()
            .getPhotoContentUrisByAlbumId(albumId)
            .filter { it.isUiManagedAssetPath() }
            .toSet()
    }

    private suspend fun loadPhotoTagsFromBackend(context: Context, assetPath: String): List<StoredPhotoTag> {
        val database = MemoirDatabaseProvider.getInstance(context)
        val photo = database.photoAssetDao().getByContentUri(assetPath) ?: return emptyList()
        val tagLinks = database.photoTagDao().getByPhotoIds(listOf(photo.id))
        if (tagLinks.isEmpty()) return emptyList()
        val tagsById = database.tagDao().getByIds(tagLinks.map { it.tagId }.distinct()).associateBy { it.id }
        return tagLinks.mapNotNull { link ->
            tagsById[link.tagId]?.toStoredPhotoTag()
        }.distinct()
    }

    private suspend fun searchPhotosFromBackend(context: Context, query: String): List<PhotoSearchResult> {
        val tagResults = searchPhotosByTagsFromBackend(context, query)
        val journalMatches = searchPhotosByJournalTextFromBackend(context, query)
        return mergePhotoSearchResults(query, tagResults, journalMatches)
    }

    private suspend fun searchPhotosByTagsFromBackend(context: Context, query: String): List<PhotoSearchResult> {
        val database = MemoirDatabaseProvider.getInstance(context)
        val matchedTags = database.tagDao().searchByValue(query)
        if (matchedTags.isEmpty()) return emptyList()

        val tagsById = matchedTags.associateBy { it.id }
        val links = database.photoTagDao().getByTagIds(tagsById.keys.toList())
        if (links.isEmpty()) return emptyList()

        val photosById = database.photoAssetDao().getByIds(links.map { it.photoId }.distinct()).associateBy { it.id }
        return links
            .groupBy { it.photoId }
            .mapNotNull { (photoId, photoLinks) ->
                val photo = photosById[photoId] ?: return@mapNotNull null
                if (!photo.contentUri.isUiManagedAssetPath()) return@mapNotNull null

                val matchingTags = photoLinks
                    .mapNotNull { link -> tagsById[link.tagId]?.toStoredPhotoTag() }
                    .distinct()
                if (matchingTags.isEmpty()) return@mapNotNull null

                PhotoSearchResult(
                    assetPath = photo.contentUri,
                    matchingTags = matchingTags,
                )
            }
    }

    private suspend fun searchPhotosByJournalTextFromBackend(
        context: Context,
        query: String,
    ): Map<String, List<String>> {
        val ftsQuery = buildFullTextQuery(query) ?: return emptyMap()
        val database = MemoirDatabaseProvider.getInstance(context)
        val matchedEntries = database.journalEntryDao().searchByFullText(ftsQuery)
        if (matchedEntries.isEmpty()) return emptyMap()

        val matchedEntryIds = matchedEntries.map { it.id }
        val entryOrder = matchedEntryIds.withIndex().associate { indexed -> indexed.value to indexed.index }
        val entryLinks = database.entryPhotoDao()
            .getByEntryIdsOrdered(matchedEntryIds)
            .sortedWith(
                compareBy<EntryPhotoCrossRef> { entryOrder[it.entryId] ?: Int.MAX_VALUE }
                    .thenBy { it.orderIndex },
            )
        if (entryLinks.isEmpty()) return emptyMap()

        val photosById = database.photoAssetDao()
            .getByIds(entryLinks.map { it.photoId }.distinct())
            .associateBy { it.id }
        val previewsByEntryId = matchedEntries.associate { entry ->
            entry.id to buildJournalMatchPreview(entry.title, entry.reflectionText, query)
        }

        val previewsByAssetPath = linkedMapOf<String, MutableList<String>>()
        entryLinks.forEach { link ->
            val photo = photosById[link.photoId] ?: return@forEach
            if (!photo.contentUri.isUiManagedAssetPath()) return@forEach

            val preview = previewsByEntryId[link.entryId] ?: return@forEach
            val previews = previewsByAssetPath.getOrPut(photo.contentUri) { mutableListOf() }
            if (preview !in previews) {
                previews += preview
            }
        }
        return previewsByAssetPath
    }

    private suspend fun loadJournalEntryFromBackend(context: Context, assetPath: String): String? {
        val database = MemoirDatabaseProvider.getInstance(context)
        return database.journalEntryDao().getById(journalEntryIdForAsset(assetPath))?.reflectionText
    }

    private fun mergePhotoSearchResults(
        query: String,
        tagResults: List<PhotoSearchResult>,
        journalMatches: Map<String, List<String>>,
    ): List<PhotoSearchResult> {
        val resultsByAssetPath = linkedMapOf<String, PhotoSearchResult>()
        tagResults.forEach { result ->
            resultsByAssetPath[result.assetPath] = result
        }
        journalMatches.forEach { (assetPath, previews) ->
            val existing = resultsByAssetPath[assetPath]
            resultsByAssetPath[assetPath] = if (existing == null) {
                PhotoSearchResult(
                    assetPath = assetPath,
                    matchingTags = emptyList(),
                    matchingJournalPreviews = previews,
                )
            } else {
                existing.copy(matchingJournalPreviews = previews)
            }
        }

        return resultsByAssetPath.values.sortedWith(
            compareBy<PhotoSearchResult> { result ->
                result.matchingTags.none { tag -> tag.value.equals(query, ignoreCase = true) }
            }.thenBy { it.matchingTags.isEmpty() }
                .thenBy { it.assetPath.lowercase() },
        )
    }

    private fun buildFullTextQuery(query: String): String? =
        query
            .trim()
            .split(Regex("\\s+"))
            .mapNotNull { token ->
                token
                    .replace(Regex("[^\\p{L}\\p{N}]"), "")
                    .takeIf { it.isNotEmpty() }
                    ?.let { "$it*" }
            }
            .takeIf { it.isNotEmpty() }
            ?.joinToString(" ")

    private fun buildJournalMatchPreview(
        title: String?,
        reflectionText: String,
        query: String,
    ): String? {
        val normalizedQuery = query.trim()
        if (normalizedQuery.isEmpty()) return null

        val titleValue = title?.trim().orEmpty()
        if (titleValue.contains(normalizedQuery, ignoreCase = true)) {
            return titleValue
        }

        val reflectionValue = reflectionText.trim()
        if (!reflectionValue.contains(normalizedQuery, ignoreCase = true)) {
            return null
        }

        val matchIndex = reflectionValue.indexOf(normalizedQuery, ignoreCase = true)
        val previewRadius = 28
        val startIndex = (matchIndex - previewRadius).coerceAtLeast(0)
        val endIndex = (matchIndex + normalizedQuery.length + previewRadius).coerceAtMost(reflectionValue.length)
        val prefix = if (startIndex > 0) "..." else ""
        val suffix = if (endIndex < reflectionValue.length) "..." else ""
        return prefix + reflectionValue.substring(startIndex, endIndex) + suffix
    }

    private suspend fun synchronizeAlbumRecord(context: Context, album: StoredAlbum) {
        val database = MemoirDatabaseProvider.getInstance(context)
        database.withTransaction {
            upsertAlbumEntity(database, album, createdAtHint = System.currentTimeMillis())
        }
    }

    private suspend fun synchronizeAlbumPhotos(context: Context, albumId: String) {
        val database = MemoirDatabaseProvider.getInstance(context)
        database.withTransaction {
            synchronizeAlbumPhotos(database, context, albumId)
        }
    }

    private suspend fun synchronizeAlbumPhotos(database: MemoirDatabase, context: Context, albumId: String) {
        val legacyAlbum = (loadLegacyAlbums(context, false) + loadLegacyAlbums(context, true))
            .firstOrNull { it.id == albumId } ?: return
        upsertAlbumEntity(database, legacyAlbum, createdAtHint = System.currentTimeMillis())

        val desiredAssetPaths = loadLegacyAlbumPhotoPaths(context, albumId).sorted()
        val photoAssetDao = database.photoAssetDao()
        val albumPhotoDao = database.albumPhotoDao()
        val now = System.currentTimeMillis()
        val desiredPhotoIds = desiredAssetPaths.map { assetPath ->
            ensurePhotoAsset(database, assetPath, now).id
        }

        val existingLinks = albumPhotoDao.getLinksByAlbumId(albumId)
        existingLinks
            .filter { it.photoId !in desiredPhotoIds }
            .forEach { staleLink ->
                albumPhotoDao.deleteLink(albumId, staleLink.photoId)
            }

        desiredPhotoIds.forEachIndexed { index, photoId ->
            albumPhotoDao.upsert(
                AlbumPhotoCrossRef(
                    albumId = albumId,
                    photoId = photoId,
                    orderIndex = index,
                    addedAt = now,
                    addedBy = UI_BRIDGE_OWNER_USER_ID,
                ),
            )
        }
    }

    private suspend fun synchronizePhotoTags(context: Context, assetPath: String) {
        val database = MemoirDatabaseProvider.getInstance(context)
        database.withTransaction {
            synchronizePhotoTags(database, context, assetPath)
        }
    }

    private suspend fun synchronizePhotoTags(database: MemoirDatabase, context: Context, assetPath: String) {
        val desiredTags = loadLegacyPhotoTags(context, assetPath)
        val photo = ensurePhotoAsset(database, assetPath, System.currentTimeMillis())
        val tagDao = database.tagDao()
        val photoTagDao = database.photoTagDao()
        val now = System.currentTimeMillis()

        val desiredTagIds = desiredTags.map { tag ->
            ensureTag(database, tag, now).id
        }.toSet()

        val existingLinks = photoTagDao.getByPhotoIds(listOf(photo.id))
        existingLinks
            .filter { it.tagId !in desiredTagIds }
            .forEach { staleLink ->
                photoTagDao.deleteLink(photo.id, staleLink.tagId)
            }

        if (desiredTagIds.isNotEmpty()) {
            photoTagDao.upsertAll(
                desiredTagIds.map { tagId ->
                    PhotoTagCrossRef(photoId = photo.id, tagId = tagId)
                },
            )
        }
    }

    private suspend fun synchronizeJournalEntry(context: Context, assetPath: String) {
        val database = MemoirDatabaseProvider.getInstance(context)
        database.withTransaction {
            synchronizeJournalEntry(database, context, assetPath)
        }
    }

    private suspend fun synchronizeJournalEntry(database: MemoirDatabase, context: Context, assetPath: String) {
        val reflectionText = loadLegacyJournalEntry(context, assetPath) ?: return
        val now = System.currentTimeMillis()
        val photo = ensurePhotoAsset(database, assetPath, now)
        val journalEntryDao = database.journalEntryDao()
        val entryPhotoDao = database.entryPhotoDao()
        val entryId = journalEntryIdForAsset(assetPath)
        val existing = journalEntryDao.getById(entryId)

        if (existing == null) {
            journalEntryDao.insert(
                JournalEntryEntity(
                    id = entryId,
                    createdAt = now,
                    updatedAt = now,
                    entryDateEpochDay = 0L,
                    title = null,
                    reflectionText = reflectionText,
                ),
            )
        } else {
            journalEntryDao.update(
                existing.copy(
                    updatedAt = now,
                    reflectionText = reflectionText,
                ),
            )
        }

        entryPhotoDao.deleteByEntryId(entryId)
        entryPhotoDao.upsertAll(
            listOf(
                EntryPhotoCrossRef(
                    entryId = entryId,
                    photoId = photo.id,
                    orderIndex = 0,
                ),
            ),
        )
    }

    private suspend fun upsertAlbumEntity(
        database: MemoirDatabase,
        album: StoredAlbum,
        createdAtHint: Long,
    ) {
        val albumDao = database.albumDao()
        val memberDao = database.albumMemberDao()
        val existing = albumDao.getById(album.id)
        val now = System.currentTimeMillis()
        val visibility = if (album.isShared) AlbumVisibility.SHARED else AlbumVisibility.PRIVATE
        if (existing == null) {
            albumDao.insert(
                AlbumEntity(
                    id = album.id,
                    createdAt = createdAtHint,
                    updatedAt = now,
                    name = album.name,
                    ownerUserId = UI_BRIDGE_OWNER_USER_ID,
                    visibility = visibility,
                ),
            )
        } else {
            albumDao.update(
                existing.copy(
                    updatedAt = now,
                    name = album.name,
                    ownerUserId = UI_BRIDGE_OWNER_USER_ID,
                    visibility = visibility,
                ),
            )
        }

        if (memberDao.getMember(album.id, UI_BRIDGE_OWNER_USER_ID) == null) {
            memberDao.upsert(
                AlbumMemberEntity(
                    albumId = album.id,
                    memberId = UI_BRIDGE_OWNER_USER_ID,
                    role = AlbumRole.OWNER,
                    status = AlbumMemberStatus.ACTIVE,
                    addedAt = now,
                ),
            )
        }
    }

    private suspend fun ensurePhotoAsset(
        database: MemoirDatabase,
        assetPath: String,
        now: Long,
    ): PhotoAssetEntity {
        val photoAssetDao = database.photoAssetDao()
        val existing = photoAssetDao.getByContentUri(assetPath)
        if (existing != null) return existing

        val created = PhotoAssetEntity(
            id = UUID.randomUUID().toString(),
            createdAt = now,
            updatedAt = now,
            contentUri = assetPath,
            takenAt = null,
            width = null,
            height = null,
            hash = null,
        )
        photoAssetDao.insert(created)
        return created
    }

    private suspend fun ensureTag(
        database: MemoirDatabase,
        tag: StoredPhotoTag,
        now: Long,
    ): TagEntity {
        val type = tag.type.toBackendType()
        val value = tag.value.trim()
        val tagDao = database.tagDao()
        val existing = tagDao.getByTypeAndValue(type, value)
        if (existing != null) {
            if (existing.updatedAt != now) {
                tagDao.update(existing.copy(updatedAt = now))
            }
            return existing
        }

        val created = TagEntity(
            id = UUID.randomUUID().toString(),
            createdAt = now,
            updatedAt = now,
            type = type,
            value = value,
        )
        tagDao.insert(created)
        return created
    }

    private fun AlbumEntity.toStoredAlbum(): StoredAlbum =
        StoredAlbum(
            id = id,
            name = name,
            isShared = visibility == AlbumVisibility.SHARED,
        )

    private fun TagEntity.toStoredPhotoTag(): StoredPhotoTag =
        StoredPhotoTag(
            type = when (type) {
                TagType.PERSON -> StoredPhotoTagType.PERSON
                TagType.LOCATION -> StoredPhotoTagType.LOCATION
                TagType.KEYWORD -> StoredPhotoTagType.KEYWORD
            },
            value = value,
        )

    private fun StoredPhotoTagType.toBackendType(): TagType = when (this) {
        StoredPhotoTagType.PERSON -> TagType.PERSON
        StoredPhotoTagType.LOCATION -> TagType.LOCATION
        StoredPhotoTagType.KEYWORD -> TagType.KEYWORD
    }

    private fun journalEntryIdForAsset(assetPath: String): String =
        UI_JOURNAL_ENTRY_ID_PREFIX + UUID.nameUUIDFromBytes(assetPath.toByteArray()).toString()

    private fun String.isUiManagedAssetPath(): Boolean = !contains("://")
}

internal fun loadLegacyAlbums(context: Context, isShared: Boolean): List<StoredAlbum> {
    val key = if (isShared) LEGACY_SHARED_ALBUMS_KEY else LEGACY_MY_ALBUMS_KEY
    val raw = context.getSharedPreferences(LEGACY_ALBUM_PREFS_NAME, Context.MODE_PRIVATE)
        .getString(key, "") ?: ""
    if (raw.isBlank()) return emptyList()

    return raw.split(LEGACY_ALBUM_ENTRY_SEPARATOR).mapNotNull { entry ->
        val parts = entry.split(LEGACY_ALBUM_FIELD_SEPARATOR)
        if (parts.size >= 2) {
            StoredAlbum(id = parts[0], name = parts[1], isShared = isShared)
        } else {
            null
        }
    }
}

internal fun createLegacyAlbum(context: Context, name: String, isShared: Boolean): StoredAlbum {
    val albums = loadLegacyAlbums(context, isShared)
    val newAlbum = StoredAlbum(
        id = UUID.randomUUID().toString(),
        name = name.trim(),
        isShared = isShared,
    )
    saveLegacyAlbums(context, isShared, albums + newAlbum)
    return newAlbum
}

internal fun deleteLegacyAlbum(context: Context, albumId: String) {
    val myAlbums = loadLegacyAlbums(context, isShared = false)
    val sharedAlbums = loadLegacyAlbums(context, isShared = true)

    if (myAlbums.any { it.id == albumId }) {
        saveLegacyAlbums(context, isShared = false, albums = myAlbums.filter { it.id != albumId })
    } else {
        saveLegacyAlbums(context, isShared = true, albums = sharedAlbums.filter { it.id != albumId })
    }

    context.getSharedPreferences(LEGACY_ALBUM_PREFS_NAME, Context.MODE_PRIVATE)
        .edit()
        .remove(LEGACY_ALBUM_PHOTOS_KEY_PREFIX + albumId)
        .apply()
}

internal fun loadLegacyAlbumPhotoPaths(context: Context, albumId: String): Set<String> {
    val raw = context.getSharedPreferences(LEGACY_ALBUM_PREFS_NAME, Context.MODE_PRIVATE)
        .getString(LEGACY_ALBUM_PHOTOS_KEY_PREFIX + albumId, "") ?: ""
    if (raw.isBlank()) return emptySet()
    return raw.split(",").filter { it.isNotBlank() }.toSet()
}

internal fun saveLegacyAlbumPhotoPaths(context: Context, albumId: String, assetPaths: Set<String>) {
    context.getSharedPreferences(LEGACY_ALBUM_PREFS_NAME, Context.MODE_PRIVATE)
        .edit()
        .putString(LEGACY_ALBUM_PHOTOS_KEY_PREFIX + albumId, assetPaths.joinToString(","))
        .apply()
}

internal fun loadLegacyPhotoTags(context: Context, assetPath: String): List<StoredPhotoTag> {
    val raw = context.getSharedPreferences(LEGACY_ALBUM_PREFS_NAME, Context.MODE_PRIVATE)
        .getString(LEGACY_PHOTO_TAGS_KEY_PREFIX + assetPath, null)
        ?: return emptyList()

    return parseStoredPhotoTags(raw)
}

internal fun saveLegacyPhotoTags(context: Context, assetPath: String, tags: List<StoredPhotoTag>) {
    val prefs = context.getSharedPreferences(LEGACY_ALBUM_PREFS_NAME, Context.MODE_PRIVATE)
    if (tags.isEmpty()) {
        prefs.edit().remove(LEGACY_PHOTO_TAGS_KEY_PREFIX + assetPath).apply()
        return
    }

    val jsonArray = org.json.JSONArray()
    tags.forEach { tag ->
        jsonArray.put(
            org.json.JSONObject()
                .put("type", tag.type.name)
                .put("value", tag.value.trim()),
        )
    }

    prefs.edit()
        .putString(LEGACY_PHOTO_TAGS_KEY_PREFIX + assetPath, jsonArray.toString())
        .apply()
}

internal fun loadLegacyPhotoTagEntries(context: Context): Map<String, List<StoredPhotoTag>> {
    val prefs = context.getSharedPreferences(LEGACY_ALBUM_PREFS_NAME, Context.MODE_PRIVATE)
    return prefs.all.keys
        .filter { key -> key.startsWith(LEGACY_PHOTO_TAGS_KEY_PREFIX) }
        .associate { key ->
            key.removePrefix(LEGACY_PHOTO_TAGS_KEY_PREFIX) to parseStoredPhotoTags(
                prefs.getString(key, "[]") ?: "[]",
            )
        }
}

internal fun loadLegacyJournalEntry(context: Context, assetPath: String): String? =
    context.getSharedPreferences(LEGACY_JOURNAL_PREFS_NAME, Context.MODE_PRIVATE)
        .getString(LEGACY_JOURNAL_KEY_PREFIX + assetPath, null)

internal fun saveLegacyJournalEntry(context: Context, assetPath: String, text: String) {
    context.getSharedPreferences(LEGACY_JOURNAL_PREFS_NAME, Context.MODE_PRIVATE)
        .edit()
        .putString(LEGACY_JOURNAL_KEY_PREFIX + assetPath, text)
        .apply()
}

internal fun loadLegacyJournalEntries(context: Context): Map<String, String> {
    val prefs = context.getSharedPreferences(LEGACY_JOURNAL_PREFS_NAME, Context.MODE_PRIVATE)
    return prefs.all.entries
        .filter { (key, value) -> key.startsWith(LEGACY_JOURNAL_KEY_PREFIX) && value is String }
        .associate { (key, value) ->
            key.removePrefix(LEGACY_JOURNAL_KEY_PREFIX) to (value as String)
        }
}

private fun saveLegacyAlbums(context: Context, isShared: Boolean, albums: List<StoredAlbum>) {
    val key = if (isShared) LEGACY_SHARED_ALBUMS_KEY else LEGACY_MY_ALBUMS_KEY
    val raw = albums.joinToString(LEGACY_ALBUM_ENTRY_SEPARATOR) { "${it.id}${LEGACY_ALBUM_FIELD_SEPARATOR}${it.name}" }
    context.getSharedPreferences(LEGACY_ALBUM_PREFS_NAME, Context.MODE_PRIVATE)
        .edit()
        .putString(key, raw)
        .apply()
}

private fun parseStoredPhotoTags(raw: String): List<StoredPhotoTag> =
    runCatching {
        val jsonArray = org.json.JSONArray(raw)
        buildList {
            for (index in 0 until jsonArray.length()) {
                val item = jsonArray.optJSONObject(index) ?: continue
                val typeName = item.optString("type")
                val value = item.optString("value").trim()
                val type = StoredPhotoTagType.values().firstOrNull { it.name == typeName } ?: continue
                if (value.isNotEmpty()) {
                    add(StoredPhotoTag(type = type, value = value))
                }
            }
        }
    }.getOrDefault(emptyList())

private fun legacyAlbumExists(context: Context, albumId: String): Boolean =
    (loadLegacyAlbums(context, isShared = false) + loadLegacyAlbums(context, isShared = true))
        .any { it.id == albumId }

private fun hasLegacyPhotoTagRecord(context: Context, assetPath: String): Boolean =
    context.getSharedPreferences(LEGACY_ALBUM_PREFS_NAME, Context.MODE_PRIVATE)
        .contains(LEGACY_PHOTO_TAGS_KEY_PREFIX + assetPath)

private fun hasLegacyJournalRecord(context: Context, assetPath: String): Boolean =
    context.getSharedPreferences(LEGACY_JOURNAL_PREFS_NAME, Context.MODE_PRIVATE)
        .contains(LEGACY_JOURNAL_KEY_PREFIX + assetPath)
