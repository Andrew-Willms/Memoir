package nostalgia.memoir.data.repository

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import nostalgia.memoir.data.local.MemoirDatabase
import nostalgia.memoir.data.local.entities.AlbumEntity
import nostalgia.memoir.data.local.entities.AlbumEntryCrossRef
import nostalgia.memoir.data.local.entities.AlbumMemberEntity
import nostalgia.memoir.data.local.entities.AlbumMemberStatus
import nostalgia.memoir.data.local.entities.AlbumPhotoCrossRef
import nostalgia.memoir.data.local.entities.AlbumRole
import nostalgia.memoir.data.local.entities.JournalEntryEntity
import nostalgia.memoir.data.local.entities.PhotoAssetEntity
import nostalgia.memoir.data.model.AddEntryToAlbumInput
import nostalgia.memoir.data.model.AddPhotoToAlbumInput
import nostalgia.memoir.data.model.AlbumAggregate
import nostalgia.memoir.data.model.CreateAlbumInput
import nostalgia.memoir.data.model.LinkedAlbumEntry
import nostalgia.memoir.data.model.LinkedAlbumPhoto
import nostalgia.memoir.data.model.UpsertAlbumMemberInput
import java.util.UUID

class RoomAlbumRepository(
    private val database: MemoirDatabase,
) : AlbumRepository {

    private val albumDao = database.albumDao()
    private val albumPhotoDao = database.albumPhotoDao()
    private val albumEntryDao = database.albumEntryDao()
    private val albumMemberDao = database.albumMemberDao()
    private val journalEntryDao = database.journalEntryDao()
    private val photoAssetDao = database.photoAssetDao()

    override suspend fun createAlbum(input: CreateAlbumInput): String {
        val now = System.currentTimeMillis()
        val albumId = UUID.randomUUID().toString()
        val name = input.name.trim()

        require(name.isNotEmpty()) { "Album name cannot be blank" }

        database.withTransaction {
            albumDao.insert(
                AlbumEntity(
                    id = albumId,
                    createdAt = now,
                    updatedAt = now,
                    name = name,
                    ownerUserId = input.ownerUserId,
                    visibility = input.visibility,
                ),
            )

            albumMemberDao.upsert(
                AlbumMemberEntity(
                    albumId = albumId,
                    memberId = input.ownerUserId,
                    role = AlbumRole.OWNER,
                    status = AlbumMemberStatus.ACTIVE,
                    addedAt = now,
                ),
            )
        }

        return albumId
    }

    override suspend fun renameAlbum(albumId: String, newName: String): Boolean {
        val existing = albumDao.getById(albumId) ?: return false
        val cleaned = newName.trim()
        if (cleaned.isEmpty()) return false

        albumDao.update(
            existing.copy(
                name = cleaned,
                updatedAt = System.currentTimeMillis(),
            ),
        )
        return true
    }

    override suspend fun getAlbumAggregate(albumId: String): AlbumAggregate? {
        val album = albumDao.getById(albumId) ?: return null

        val photoLinks = albumPhotoDao.getLinksByAlbumId(albumId)
        val photosById = if (photoLinks.isEmpty()) {
            emptyMap()
        } else {
            photoAssetDao.getByIds(photoLinks.map { it.photoId }).associateBy { it.id }
        }

        val linkedPhotos = photoLinks.mapNotNull { link ->
            photosById[link.photoId]?.let { photo ->
                LinkedAlbumPhoto(
                    photo = photo,
                    orderIndex = link.orderIndex,
                    addedAt = link.addedAt,
                    addedBy = link.addedBy,
                )
            }
        }

        val links = albumEntryDao.getLinksByAlbumId(albumId)
        val entriesById = if (links.isEmpty()) {
            emptyMap()
        } else {
            journalEntryDao.getByIds(links.map { it.entryId }).associateBy { it.id }
        }

        val linkedEntries = links.mapNotNull { link ->
            entriesById[link.entryId]?.let { entry ->
                LinkedAlbumEntry(
                    entry = entry,
                    addedAt = link.addedAt,
                    addedBy = link.addedBy,
                )
            }
        }

        val members = albumMemberDao.observeMembers(albumId).first()

        return AlbumAggregate(
            album = album,
            photos = linkedPhotos,
            entries = linkedEntries,
            members = members,
        )
    }

    override suspend fun addPhotoToAlbum(input: AddPhotoToAlbumInput): Boolean {
        val album = albumDao.getById(input.albumId) ?: return false
        val photo = photoAssetDao.getById(input.photoId) ?: return false
        val now = System.currentTimeMillis()
        val orderIndex = input.orderIndex ?: albumPhotoDao.nextOrderIndex(input.albumId)

        albumPhotoDao.upsert(
            AlbumPhotoCrossRef(
                albumId = album.id,
                photoId = photo.id,
                orderIndex = orderIndex,
                addedAt = now,
                addedBy = input.addedBy,
            ),
        )

        albumDao.update(album.copy(updatedAt = now))
        return true
    }

    override suspend fun removePhotoFromAlbum(albumId: String, photoId: String): Boolean {
        val album = albumDao.getById(albumId) ?: return false
        albumPhotoDao.deleteLink(albumId, photoId)
        albumDao.update(album.copy(updatedAt = System.currentTimeMillis()))
        return true
    }

    override suspend fun addEntryToAlbum(input: AddEntryToAlbumInput): Boolean {
        val album = albumDao.getById(input.albumId) ?: return false
        val entry = journalEntryDao.getById(input.entryId) ?: return false

        albumEntryDao.upsert(
            AlbumEntryCrossRef(
                albumId = album.id,
                entryId = entry.id,
                addedAt = System.currentTimeMillis(),
                addedBy = input.addedBy,
            ),
        )

        albumDao.update(album.copy(updatedAt = System.currentTimeMillis()))
        return true
    }

    override suspend fun removeEntryFromAlbum(albumId: String, entryId: String): Boolean {
        val album = albumDao.getById(albumId) ?: return false
        albumEntryDao.deleteLink(albumId, entryId)
        albumDao.update(album.copy(updatedAt = System.currentTimeMillis()))
        return true
    }

    override suspend fun upsertAlbumMember(input: UpsertAlbumMemberInput): Boolean {
        val album = albumDao.getById(input.albumId) ?: return false
        albumMemberDao.upsert(
            AlbumMemberEntity(
                albumId = input.albumId,
                memberId = input.memberId,
                role = input.role,
                status = input.status,
                addedAt = System.currentTimeMillis(),
            ),
        )
        albumDao.update(album.copy(updatedAt = System.currentTimeMillis()))
        return true
    }

    override suspend fun removeAlbumMember(albumId: String, memberId: String): Boolean {
        val album = albumDao.getById(albumId) ?: return false
        albumMemberDao.deleteMember(albumId, memberId)
        albumDao.update(album.copy(updatedAt = System.currentTimeMillis()))
        return true
    }

    override fun observeAlbumsByOwner(ownerUserId: String): Flow<List<AlbumEntity>> =
        albumDao.observeByOwner(ownerUserId)

    override fun observeAllAlbums(): Flow<List<AlbumEntity>> =
        albumDao.observeAll()

    override fun observePhotosForAlbum(albumId: String): Flow<List<PhotoAssetEntity>> =
        albumPhotoDao.observePhotosForAlbum(albumId)

    override fun observeEntriesForAlbum(albumId: String): Flow<List<JournalEntryEntity>> =
        albumEntryDao.observeEntriesForAlbum(albumId)

    override fun observeMembersForAlbum(albumId: String): Flow<List<AlbumMemberEntity>> =
        albumMemberDao.observeMembers(albumId)
}
