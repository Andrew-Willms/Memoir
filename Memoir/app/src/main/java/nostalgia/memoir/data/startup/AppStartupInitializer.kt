package nostalgia.memoir.data.startup

import android.content.ContentUris
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.room.withTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import nostalgia.memoir.BuildConfig
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
import java.nio.charset.StandardCharsets
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

class AppStartupInitializer(
    private val context: Context,
    private val databaseProvider: (Context) -> nostalgia.memoir.data.local.MemoirDatabase = MemoirDatabaseProvider::getInstance,
    private val mediaRowsProvider: (suspend (Set<String>?) -> List<ImportedPhotoMetadata>)? = null,
) {

    suspend fun run(forceEnabled: Boolean = BuildConfig.ENABLE_STARTUP_MOCK_SEED) {
        if (!forceEnabled) return

        val database = databaseProvider(context)
        val photoAssetDao = database.photoAssetDao()
        val journalEntryDao = database.journalEntryDao()
        val entryPhotoDao = database.entryPhotoDao()
        val tagDao = database.tagDao()
        val photoTagDao = database.photoTagDao()
        val albumDao = database.albumDao()
        val albumPhotoDao = database.albumPhotoDao()
        val albumMemberDao = database.albumMemberDao()

        val targetFolders = parseTargetFolders(
            BuildConfig.STARTUP_MOCK_PHOTO_FOLDERS,
            BuildConfig.STARTUP_MOCK_PHOTO_FOLDER,
        )

        val mediaRows = mediaRowsProvider?.invoke(targetFolders) ?: queryMediaImages(targetFolders)
        if (mediaRows.isEmpty()) {
            Log.i(TAG, "Startup import finished: no media rows found")
            return
        }

        val now = System.currentTimeMillis()
        val albumNextOrder = mutableMapOf<String, Int>()
        val albumLinkedPhotoIds = mutableMapOf<String, MutableSet<String>>()
        val entryNextOrder = mutableMapOf<String, Int>()
        val entryLinkedPhotoIds = mutableMapOf<String, MutableSet<String>>()
        val tagIdByKeyword = mutableMapOf<String, String>()
        var importedPhotos = 0
        var createdPhotos = 0
        var updatedPhotos = 0
        var createdAlbums = 0
        var createdJournalEntries = 0
        var linkedAlbumPhotos = 0
        var linkedEntryPhotos = 0
        var linkedPhotoTags = 0

        database.withTransaction {
            for (row in mediaRows) {
                importedPhotos += 1

                val existingPhoto = photoAssetDao.getByContentUri(row.contentUri)
                val photoId = if (existingPhoto == null) {
                    val created = PhotoAssetEntity(
                        id = UUID.randomUUID().toString(),
                        createdAt = now,
                        updatedAt = now,
                        contentUri = row.contentUri,
                        takenAt = row.takenAt,
                        width = row.width,
                        height = row.height,
                        hash = null,
                    )
                    photoAssetDao.insert(created)
                    createdPhotos += 1
                    created.id
                } else {
                    val updated = existingPhoto.copy(
                        updatedAt = now,
                        takenAt = row.takenAt ?: existingPhoto.takenAt,
                        width = row.width ?: existingPhoto.width,
                        height = row.height ?: existingPhoto.height,
                    )
                    if (updated != existingPhoto) {
                        photoAssetDao.update(updated)
                        updatedPhotos += 1
                    }
                    existingPhoto.id
                }

                val albumId = stableId(ALBUM_ID_PREFIX, row.bucketName.lowercase(Locale.US))
                val existingAlbum = albumDao.getById(albumId)
                if (existingAlbum == null) {
                    albumDao.insert(
                        AlbumEntity(
                            id = albumId,
                            createdAt = now,
                            updatedAt = now,
                            name = row.bucketName,
                            ownerUserId = STARTUP_OWNER_USER_ID,
                            visibility = AlbumVisibility.PRIVATE,
                        ),
                    )
                    createdAlbums += 1
                } else if (existingAlbum.name != row.bucketName) {
                    albumDao.update(existingAlbum.copy(name = row.bucketName, updatedAt = now))
                }

                val existingOwner = albumMemberDao.getMember(albumId, STARTUP_OWNER_USER_ID)
                if (existingOwner == null) {
                    albumMemberDao.upsert(
                        AlbumMemberEntity(
                            albumId = albumId,
                            memberId = STARTUP_OWNER_USER_ID,
                            role = AlbumRole.OWNER,
                            status = AlbumMemberStatus.ACTIVE,
                            addedAt = now,
                        ),
                    )
                }

                val knownAlbumPhotoIds = albumLinkedPhotoIds.getOrPut(albumId) {
                    albumPhotoDao.getLinksByAlbumId(albumId).map { it.photoId }.toMutableSet()
                }
                if (!knownAlbumPhotoIds.contains(photoId)) {
                    val nextAlbumOrder = albumNextOrder.getOrPut(albumId) {
                        val existingLinks = albumPhotoDao.getLinksByAlbumId(albumId)
                        (existingLinks.maxOfOrNull { it.orderIndex } ?: -1) + 1
                    }
                    albumPhotoDao.upsert(
                        AlbumPhotoCrossRef(
                            albumId = albumId,
                            photoId = photoId,
                            orderIndex = nextAlbumOrder,
                            addedAt = now,
                            addedBy = STARTUP_OWNER_USER_ID,
                        ),
                    )
                    albumNextOrder[albumId] = nextAlbumOrder + 1
                    knownAlbumPhotoIds += photoId
                    linkedAlbumPhotos += 1
                }

                val epochDay = epochDayForMillis(row.takenAt ?: now)
                val entryId = stableId(
                    ENTRY_ID_PREFIX,
                    "${row.bucketName.lowercase(Locale.US)}|$epochDay",
                )
                val existingEntry = journalEntryDao.getById(entryId)
                if (existingEntry == null) {
                    journalEntryDao.insert(
                        JournalEntryEntity(
                            id = entryId,
                            createdAt = now,
                            updatedAt = now,
                            entryDateEpochDay = epochDay,
                            title = "Imported • ${row.bucketName}",
                            reflectionText = "Auto-imported photos from folder ${row.bucketName}",
                        ),
                    )
                    createdJournalEntries += 1
                }

                val knownEntryPhotoIds = entryLinkedPhotoIds.getOrPut(entryId) {
                    entryPhotoDao.getByEntryIdOrdered(entryId).map { it.photoId }.toMutableSet()
                }
                if (!knownEntryPhotoIds.contains(photoId)) {
                    val nextEntryOrder = entryNextOrder.getOrPut(entryId) {
                        val existingLinks = entryPhotoDao.getByEntryIdOrdered(entryId)
                        (existingLinks.maxOfOrNull { it.orderIndex } ?: -1) + 1
                    }
                    entryPhotoDao.upsertAll(
                        listOf(
                            EntryPhotoCrossRef(
                                entryId = entryId,
                                photoId = photoId,
                                orderIndex = nextEntryOrder,
                            ),
                        ),
                    )
                    entryNextOrder[entryId] = nextEntryOrder + 1
                    knownEntryPhotoIds += photoId
                    linkedEntryPhotos += 1
                }

                val folderTagValue = "folder:${row.bucketName.lowercase(Locale.US)}"
                val tagId = tagIdByKeyword.getOrPut(folderTagValue) {
                    val existingTag = tagDao.getByTypeAndValue(TagType.KEYWORD, folderTagValue)
                    if (existingTag != null) {
                        existingTag.id
                    } else {
                        val createdTag = TagEntity(
                            id = UUID.randomUUID().toString(),
                            createdAt = now,
                            updatedAt = now,
                            type = TagType.KEYWORD,
                            value = folderTagValue,
                        )
                        tagDao.insert(createdTag)
                        createdTag.id
                    }
                }
                photoTagDao.upsertAll(listOf(PhotoTagCrossRef(photoId = photoId, tagId = tagId)))
                linkedPhotoTags += 1
            }
        }

        Log.i(
            TAG,
            "Startup import complete: processed=$importedPhotos, createdPhotos=$createdPhotos, " +
                "updatedPhotos=$updatedPhotos, createdAlbums=$createdAlbums, " +
                "createdJournalEntries=$createdJournalEntries, linkedAlbumPhotos=$linkedAlbumPhotos, " +
                "linkedEntryPhotos=$linkedEntryPhotos, linkedPhotoTags=$linkedPhotoTags",
        )
    }

    private suspend fun queryMediaImages(folderNames: Set<String>?): List<ImportedPhotoMetadata> = withContext(Dispatchers.IO) {
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DATE_TAKEN,
            MediaStore.Images.Media.DATE_ADDED,
            MediaStore.Images.Media.WIDTH,
            MediaStore.Images.Media.HEIGHT,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
        )

        val selection: String?
        val selectionArgs: Array<String>?
        if (folderNames.isNullOrEmpty()) {
            selection = null
            selectionArgs = null
        } else {
            val placeholders = folderNames.joinToString(",") { "?" }
            selection = "${MediaStore.Images.Media.BUCKET_DISPLAY_NAME} IN ($placeholders)"
            selectionArgs = folderNames.toTypedArray()
        }
        val sortOrder = "${MediaStore.Images.Media.DATE_TAKEN} DESC, ${MediaStore.Images.Media.DATE_ADDED} DESC"

        try {
            val rows = mutableListOf<ImportedPhotoMetadata>()
            context.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                sortOrder,
            )?.use { cursor ->
                val idIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val takenAtIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)
                val dateAddedIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
                val widthIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH)
                val heightIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT)
                val bucketNameIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idIndex)
                    val contentUri = ContentUris.withAppendedId(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        id,
                    ).toString()

                    val takenAt = if (cursor.isNull(takenAtIndex)) null else cursor.getLong(takenAtIndex)
                    val dateAddedSeconds = if (cursor.isNull(dateAddedIndex)) null else cursor.getLong(dateAddedIndex)
                    val width = if (cursor.isNull(widthIndex)) null else cursor.getInt(widthIndex)
                    val height = if (cursor.isNull(heightIndex)) null else cursor.getInt(heightIndex)
                    val bucketNameRaw = if (cursor.isNull(bucketNameIndex)) null else cursor.getString(bucketNameIndex)
                    val bucketName = bucketNameRaw?.trim().takeUnless { it.isNullOrEmpty() } ?: UNKNOWN_BUCKET

                    rows += ImportedPhotoMetadata(
                        contentUri = contentUri,
                        takenAt = takenAt ?: dateAddedSeconds?.times(1000),
                        width = width,
                        height = height,
                        bucketName = bucketName,
                    )
                }
            }

            return@withContext rows
        } catch (_: SecurityException) {
            Log.w(TAG, "Startup import skipped: missing media read permission")
            return@withContext emptyList()
        }
    }

    private fun parseTargetFolders(csvFolders: String, fallbackSingleFolder: String): Set<String>? {
        val normalized = csvFolders
            .split(',')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toMutableSet()

        if (normalized.isEmpty() && fallbackSingleFolder.isNotBlank()) {
            normalized += fallbackSingleFolder.trim()
        }

        return if (normalized.any { it == IMPORT_ALL_FOLDERS_TOKEN }) null else normalized
    }

    private fun stableId(prefix: String, source: String): String {
        val bytes = "$prefix:$source".toByteArray(StandardCharsets.UTF_8)
        return UUID.nameUUIDFromBytes(bytes).toString()
    }

    private fun epochDayForMillis(timestampMillis: Long): Long {
        val offsetMillis = TimeZone.getDefault().getOffset(timestampMillis)
        return Math.floorDiv(timestampMillis + offsetMillis, MILLIS_PER_DAY)
    }

    data class ImportedPhotoMetadata(
        val contentUri: String,
        val takenAt: Long?,
        val width: Int?,
        val height: Int?,
        val bucketName: String,
    )

    companion object {
        private const val TAG = "AppStartupInitializer"
        private const val STARTUP_OWNER_USER_ID = "startup-importer"
        private const val ALBUM_ID_PREFIX = "startup-album"
        private const val ENTRY_ID_PREFIX = "startup-entry"
        private const val IMPORT_ALL_FOLDERS_TOKEN = "*"
        private const val UNKNOWN_BUCKET = "Imported"
        private const val MILLIS_PER_DAY = 86_400_000L

        fun requiredMediaReadPermission(): String =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                android.Manifest.permission.READ_MEDIA_IMAGES
            } else {
                android.Manifest.permission.READ_EXTERNAL_STORAGE
            }
    }
}
