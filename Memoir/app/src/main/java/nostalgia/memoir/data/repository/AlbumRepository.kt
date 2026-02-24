package nostalgia.memoir.data.repository

import kotlinx.coroutines.flow.Flow
import nostalgia.memoir.data.local.entities.AlbumEntity
import nostalgia.memoir.data.local.entities.AlbumMemberEntity
import nostalgia.memoir.data.local.entities.JournalEntryEntity
import nostalgia.memoir.data.model.AddEntryToAlbumInput
import nostalgia.memoir.data.model.AlbumAggregate
import nostalgia.memoir.data.model.CreateAlbumInput
import nostalgia.memoir.data.model.UpsertAlbumMemberInput

interface AlbumRepository {
    suspend fun createAlbum(input: CreateAlbumInput): String
    suspend fun renameAlbum(albumId: String, newName: String): Boolean
    suspend fun getAlbumAggregate(albumId: String): AlbumAggregate?
    suspend fun addEntryToAlbum(input: AddEntryToAlbumInput): Boolean
    suspend fun removeEntryFromAlbum(albumId: String, entryId: String): Boolean
    suspend fun upsertAlbumMember(input: UpsertAlbumMemberInput): Boolean
    suspend fun removeAlbumMember(albumId: String, memberId: String): Boolean
    fun observeAlbumsByOwner(ownerUserId: String): Flow<List<AlbumEntity>>
    fun observeAllAlbums(): Flow<List<AlbumEntity>>
    fun observeEntriesForAlbum(albumId: String): Flow<List<JournalEntryEntity>>
    fun observeMembersForAlbum(albumId: String): Flow<List<AlbumMemberEntity>>
}
