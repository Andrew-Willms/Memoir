package nostalgia.memoir.data.model

import nostalgia.memoir.data.local.entities.JournalEntryEntity
import nostalgia.memoir.data.local.entities.PhotoAssetEntity
import nostalgia.memoir.data.local.entities.TagEntity
import nostalgia.memoir.data.local.entities.TagType

data class TagDraft(
    val type: TagType,
    val value: String,
)

data class PhotoAssetDraft(
    val contentUri: String,
    val takenAt: Long? = null,
    val width: Int? = null,
    val height: Int? = null,
    val hash: String? = null,
)

data class CreateJournalEntryInput(
    val entryDateEpochDay: Long,
    val title: String?,
    val reflectionText: String,
    val photos: List<PhotoAssetDraft>,
    val tags: List<TagDraft>,
)

data class UpdateJournalEntryInput(
    val entryId: String,
    val title: String?,
    val reflectionText: String,
    val photos: List<PhotoAssetDraft>,
    val tags: List<TagDraft>,
)

data class LinkedPhotoAsset(
    val photo: PhotoAssetEntity,
    val orderIndex: Int,
)

data class JournalEntryAggregate(
    val entry: JournalEntryEntity,
    val photos: List<LinkedPhotoAsset>,
    val tags: List<TagEntity>,
)
