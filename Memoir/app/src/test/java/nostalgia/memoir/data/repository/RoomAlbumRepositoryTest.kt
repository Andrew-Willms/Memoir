package nostalgia.memoir.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import nostalgia.memoir.data.local.MemoirDatabase
import nostalgia.memoir.data.local.entities.AlbumMemberStatus
import nostalgia.memoir.data.local.entities.AlbumRole
import nostalgia.memoir.data.local.entities.AlbumVisibility
import nostalgia.memoir.data.local.entities.TagType
import nostalgia.memoir.data.model.AddEntryToAlbumInput
import nostalgia.memoir.data.model.AddPhotoToAlbumInput
import nostalgia.memoir.data.model.CreateAlbumInput
import nostalgia.memoir.data.model.CreateJournalEntryInput
import nostalgia.memoir.data.model.PhotoAssetDraft
import nostalgia.memoir.data.model.TagDraft
import nostalgia.memoir.data.model.UpsertAlbumMemberInput
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RoomAlbumRepositoryTest {

    private lateinit var database: MemoirDatabase
    private lateinit var albumRepository: RoomAlbumRepository
    private lateinit var journalingRepository: RoomJournalingRepository

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            MemoirDatabase::class.java,
        ).allowMainThreadQueries().build()

        albumRepository = RoomAlbumRepository(database)
        journalingRepository = RoomJournalingRepository(database)
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun createAlbum_createsOwnerMemberAndAggregate() = runBlocking {
        val albumId = albumRepository.createAlbum(
            CreateAlbumInput(
                name = "Vietnam Trip",
                ownerUserId = "user-1",
                visibility = AlbumVisibility.PRIVATE,
            ),
        )

        val aggregate = albumRepository.getAlbumAggregate(albumId)

        assertNotNull(aggregate)
        assertEquals("Vietnam Trip", aggregate!!.album.name)
        assertEquals("user-1", aggregate.album.ownerUserId)
        assertTrue(aggregate.photos.isEmpty())
        assertEquals(1, aggregate.members.size)
        assertEquals(AlbumRole.OWNER, aggregate.members.first().role)
        assertEquals(AlbumMemberStatus.ACTIVE, aggregate.members.first().status)
    }

    @Test
    fun addAndRemoveEntryFromAlbum_updatesAggregate() = runBlocking {
        val entryId = journalingRepository.createEntryAggregate(
            CreateJournalEntryInput(
                entryDateEpochDay = 20_000L,
                title = "Journal",
                reflectionText = "Reflection",
                photos = emptyList(),
                tags = listOf(TagDraft(type = TagType.KEYWORD, value = "travel")),
            ),
        )

        val albumId = albumRepository.createAlbum(
            CreateAlbumInput(
                name = "Family",
                ownerUserId = "user-1",
            ),
        )

        val added = albumRepository.addEntryToAlbum(
            AddEntryToAlbumInput(
                albumId = albumId,
                entryId = entryId,
                addedBy = "user-1",
            ),
        )

        assertTrue(added)
        val aggregateAfterAdd = albumRepository.getAlbumAggregate(albumId)
        assertNotNull(aggregateAfterAdd)
        assertTrue(aggregateAfterAdd!!.photos.isEmpty())
        assertEquals(1, aggregateAfterAdd.entries.size)
        assertEquals(entryId, aggregateAfterAdd.entries.first().entry.id)

        val removed = albumRepository.removeEntryFromAlbum(albumId, entryId)
        assertTrue(removed)
        val aggregateAfterRemove = albumRepository.getAlbumAggregate(albumId)
        assertNotNull(aggregateAfterRemove)
        assertTrue(aggregateAfterRemove!!.entries.isEmpty())
    }

    @Test
    fun upsertMember_andObserveByOwner_work() = runBlocking {
        val ownerAlbum = albumRepository.createAlbum(
            CreateAlbumInput(name = "Owner Album", ownerUserId = "owner-1"),
        )
        albumRepository.createAlbum(
            CreateAlbumInput(name = "Other Album", ownerUserId = "owner-2"),
        )

        val upserted = albumRepository.upsertAlbumMember(
            UpsertAlbumMemberInput(
                albumId = ownerAlbum,
                memberId = "friend-1",
                role = AlbumRole.EDITOR,
                status = AlbumMemberStatus.ACTIVE,
            ),
        )

        assertTrue(upserted)

        val ownerAlbums = albumRepository.observeAlbumsByOwner("owner-1").first()
        assertEquals(1, ownerAlbums.size)
        assertEquals("Owner Album", ownerAlbums.first().name)

        val members = albumRepository.observeMembersForAlbum(ownerAlbum).first()
        assertEquals(2, members.size)
        assertTrue(members.any { it.memberId == "friend-1" && it.role == AlbumRole.EDITOR })
    }

    @Test
    fun addAndRemovePhotoFromAlbum_updatesAggregatePhotos() = runBlocking {
        val journalEntryId = journalingRepository.createEntryAggregate(
            CreateJournalEntryInput(
                entryDateEpochDay = 20_100L,
                title = "Photo source",
                reflectionText = "Source",
                photos = listOf(
                    PhotoAssetDraft(contentUri = "content://album-photos/1"),
                    PhotoAssetDraft(contentUri = "content://album-photos/2"),
                ),
                tags = emptyList(),
            ),
        )

        val sourceAggregate = journalingRepository.getEntryAggregate(journalEntryId)
        assertNotNull(sourceAggregate)
        val photoId = sourceAggregate!!.photos.first().photo.id

        val albumId = albumRepository.createAlbum(
            CreateAlbumInput(name = "Album Photos", ownerUserId = "owner-1"),
        )

        val added = albumRepository.addPhotoToAlbum(
            AddPhotoToAlbumInput(albumId = albumId, photoId = photoId, addedBy = "owner-1"),
        )
        assertTrue(added)

        val aggregateAfterAdd = albumRepository.getAlbumAggregate(albumId)
        assertNotNull(aggregateAfterAdd)
        assertEquals(1, aggregateAfterAdd!!.photos.size)
        assertEquals(photoId, aggregateAfterAdd.photos.first().photo.id)

        val removed = albumRepository.removePhotoFromAlbum(albumId, photoId)
        assertTrue(removed)

        val aggregateAfterRemove = albumRepository.getAlbumAggregate(albumId)
        assertNotNull(aggregateAfterRemove)
        assertTrue(aggregateAfterRemove!!.photos.isEmpty())
    }

    @Test
    fun photoBelongsToSingleAlbum_whenRelinkedMovesAlbum() = runBlocking {
        val journalEntryId = journalingRepository.createEntryAggregate(
            CreateJournalEntryInput(
                entryDateEpochDay = 20_200L,
                title = "Single album photo",
                reflectionText = "Source",
                photos = listOf(PhotoAssetDraft(contentUri = "content://album-photos/single")),
                tags = emptyList(),
            ),
        )

        val photoId = journalingRepository.getEntryAggregate(journalEntryId)!!.photos.first().photo.id

        val albumA = albumRepository.createAlbum(CreateAlbumInput(name = "A", ownerUserId = "owner-1"))
        val albumB = albumRepository.createAlbum(CreateAlbumInput(name = "B", ownerUserId = "owner-1"))

        assertTrue(albumRepository.addPhotoToAlbum(AddPhotoToAlbumInput(albumId = albumA, photoId = photoId)))
        assertTrue(albumRepository.addPhotoToAlbum(AddPhotoToAlbumInput(albumId = albumB, photoId = photoId)))

        val albumAAggregate = albumRepository.getAlbumAggregate(albumA)!!
        val albumBAggregate = albumRepository.getAlbumAggregate(albumB)!!

        assertTrue(albumAAggregate.photos.isEmpty())
        assertEquals(1, albumBAggregate.photos.size)
        assertEquals(photoId, albumBAggregate.photos.first().photo.id)
    }
}
