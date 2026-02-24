package nostalgia.memoir.data.model

import nostalgia.memoir.data.local.entities.AlbumEntity
import nostalgia.memoir.data.local.entities.AlbumMemberEntity
import nostalgia.memoir.data.local.entities.AlbumMemberStatus
import nostalgia.memoir.data.local.entities.AlbumRole
import nostalgia.memoir.data.local.entities.AlbumVisibility
import nostalgia.memoir.data.local.entities.JournalEntryEntity

data class CreateAlbumInput(
    val name: String,
    val ownerUserId: String,
    val visibility: AlbumVisibility = AlbumVisibility.PRIVATE,
)

data class AddEntryToAlbumInput(
    val albumId: String,
    val entryId: String,
    val addedBy: String? = null,
)

data class UpsertAlbumMemberInput(
    val albumId: String,
    val memberId: String,
    val role: AlbumRole,
    val status: AlbumMemberStatus,
)

data class LinkedAlbumEntry(
    val entry: JournalEntryEntity,
    val addedAt: Long,
    val addedBy: String?,
)

data class AlbumAggregate(
    val album: AlbumEntity,
    val entries: List<LinkedAlbumEntry>,
    val members: List<AlbumMemberEntity>,
)
