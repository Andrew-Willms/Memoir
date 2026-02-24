package nostalgia.memoir.data.repository

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import nostalgia.memoir.data.local.MemoirDatabase
import nostalgia.memoir.data.local.entities.EntryPhotoCrossRef
import nostalgia.memoir.data.local.entities.EntryTagCrossRef
import nostalgia.memoir.data.local.entities.JournalEntryEntity
import nostalgia.memoir.data.local.entities.PhotoAssetEntity
import nostalgia.memoir.data.local.entities.TagEntity
import nostalgia.memoir.data.model.CreateJournalEntryInput
import nostalgia.memoir.data.model.JournalEntryAggregate
import nostalgia.memoir.data.model.LinkedPhotoAsset
import nostalgia.memoir.data.model.PhotoAssetDraft
import nostalgia.memoir.data.model.TagDraft
import nostalgia.memoir.data.model.UpdateJournalEntryInput
import java.util.UUID

class RoomJournalingRepository(
    private val database: MemoirDatabase,
) : JournalingRepository {

    private val journalEntryDao = database.journalEntryDao()
    private val photoAssetDao = database.photoAssetDao()
    private val entryPhotoDao = database.entryPhotoDao()
    private val tagDao = database.tagDao()
    private val entryTagDao = database.entryTagDao()

    override suspend fun createEntryAggregate(input: CreateJournalEntryInput): String {
        val now = System.currentTimeMillis()
        val entryId = UUID.randomUUID().toString()

        database.withTransaction {
            journalEntryDao.insert(
                JournalEntryEntity(
                    id = entryId,
                    createdAt = now,
                    updatedAt = now,
                    entryDateEpochDay = input.entryDateEpochDay,
                    title = sanitizeTitle(input.title),
                    reflectionText = input.reflectionText.trim(),
                ),
            )

            val photoIdsInOrder = resolvePhotoIds(input.photos, now)
            if (photoIdsInOrder.isNotEmpty()) {
                val entryPhotos = photoIdsInOrder.mapIndexed { index, photoId ->
                    EntryPhotoCrossRef(
                        entryId = entryId,
                        photoId = photoId,
                        orderIndex = index,
                    )
                }
                entryPhotoDao.upsertAll(entryPhotos)
            }

            val tagIds = resolveTagIds(input.tags, now)
            if (tagIds.isNotEmpty()) {
                val entryTags = tagIds.map { tagId ->
                    EntryTagCrossRef(
                        entryId = entryId,
                        tagId = tagId,
                    )
                }
                entryTagDao.upsertAll(entryTags)
            }
        }

        return entryId
    }

    override suspend fun updateEntryAggregate(input: UpdateJournalEntryInput): Boolean {
        val now = System.currentTimeMillis()
        val existing = journalEntryDao.getById(input.entryId) ?: return false

        database.withTransaction {
            journalEntryDao.update(
                existing.copy(
                    updatedAt = now,
                    title = sanitizeTitle(input.title),
                    reflectionText = input.reflectionText.trim(),
                ),
            )

            entryPhotoDao.deleteByEntryId(input.entryId)
            val photoIdsInOrder = resolvePhotoIds(input.photos, now)
            if (photoIdsInOrder.isNotEmpty()) {
                entryPhotoDao.upsertAll(
                    photoIdsInOrder.mapIndexed { index, photoId ->
                        EntryPhotoCrossRef(
                            entryId = input.entryId,
                            photoId = photoId,
                            orderIndex = index,
                        )
                    },
                )
            }

            entryTagDao.deleteByEntryId(input.entryId)
            val tagIds = resolveTagIds(input.tags, now)
            if (tagIds.isNotEmpty()) {
                entryTagDao.upsertAll(
                    tagIds.map { tagId ->
                        EntryTagCrossRef(
                            entryId = input.entryId,
                            tagId = tagId,
                        )
                    },
                )
            }
        }

        return true
    }

    override suspend fun getEntryAggregate(entryId: String): JournalEntryAggregate? {
        val entry = journalEntryDao.getById(entryId) ?: return null

        val photoLinks = entryPhotoDao.getByEntryIdOrdered(entryId)
        val orderedPhotos = if (photoLinks.isEmpty()) {
            emptyList()
        } else {
            val photosById = photoAssetDao.getByIds(photoLinks.map { it.photoId }).associateBy { it.id }
            photoLinks.mapNotNull { link ->
                photosById[link.photoId]?.let { photo ->
                    LinkedPhotoAsset(photo = photo, orderIndex = link.orderIndex)
                }
            }
        }

        val tagLinks = entryTagDao.getByEntryId(entryId)
        val tags = if (tagLinks.isEmpty()) {
            emptyList()
        } else {
            val tagsById = tagDao.getByIds(tagLinks.map { it.tagId }).associateBy { it.id }
            tagLinks.mapNotNull { link -> tagsById[link.tagId] }
        }

        return JournalEntryAggregate(
            entry = entry,
            photos = orderedPhotos,
            tags = tags,
        )
    }

    override fun observeAllEntries(): Flow<List<JournalEntryEntity>> = journalEntryDao.observeAll()

    override fun observeEntriesByDateRange(
        startEpochDay: Long,
        endEpochDay: Long,
    ): Flow<List<JournalEntryEntity>> = journalEntryDao.observeByDateRange(startEpochDay, endEpochDay)

    override fun searchEntries(query: String): Flow<List<JournalEntryEntity>> =
        journalEntryDao.search(query.trim())

    override suspend fun getLinkedPhotoUrisForEpochDay(epochDay: Long): List<String> =
        photoAssetDao.getLinkedPhotoUrisForEpochDay(epochDay)

    private suspend fun resolvePhotoIds(photos: List<PhotoAssetDraft>, now: Long): List<String> {
        if (photos.isEmpty()) return emptyList()

        return photos
            .distinctBy { it.contentUri.trim() }
            .map { draft ->
                val normalizedUri = draft.contentUri.trim()
                val existing = photoAssetDao.getByContentUri(normalizedUri)
                if (existing == null) {
                    val created = PhotoAssetEntity(
                        id = UUID.randomUUID().toString(),
                        createdAt = now,
                        updatedAt = now,
                        contentUri = normalizedUri,
                        takenAt = draft.takenAt,
                        width = draft.width,
                        height = draft.height,
                        hash = draft.hash,
                    )
                    photoAssetDao.insert(created)
                    created.id
                } else {
                    val updated = existing.copy(
                        updatedAt = now,
                        takenAt = draft.takenAt ?: existing.takenAt,
                        width = draft.width ?: existing.width,
                        height = draft.height ?: existing.height,
                        hash = draft.hash ?: existing.hash,
                    )
                    if (updated != existing) {
                        photoAssetDao.update(updated)
                    }
                    existing.id
                }
            }
    }

    private suspend fun resolveTagIds(tags: List<TagDraft>, now: Long): List<String> {
        if (tags.isEmpty()) return emptyList()

        return tags
            .map { draft -> draft.copy(value = draft.value.trim()) }
            .filter { draft -> draft.value.isNotEmpty() }
            .distinctBy { draft -> draft.type to draft.value }
            .map { draft ->
                val existing = tagDao.getByTypeAndValue(draft.type, draft.value)
                if (existing == null) {
                    val created = TagEntity(
                        id = UUID.randomUUID().toString(),
                        createdAt = now,
                        updatedAt = now,
                        type = draft.type,
                        value = draft.value,
                    )
                    tagDao.insert(created)
                    created.id
                } else {
                    if (existing.updatedAt != now) {
                        tagDao.update(existing.copy(updatedAt = now))
                    }
                    existing.id
                }
            }
    }

    private fun sanitizeTitle(value: String?): String? {
        val cleaned = value?.trim()
        return if (cleaned.isNullOrEmpty()) null else cleaned
    }
}
