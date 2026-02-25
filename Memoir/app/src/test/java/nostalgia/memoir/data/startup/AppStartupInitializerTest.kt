package nostalgia.memoir.data.startup

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import nostalgia.memoir.data.local.MemoirDatabase
import nostalgia.memoir.data.local.entities.TagType
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AppStartupInitializerTest {

    private lateinit var database: MemoirDatabase

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            MemoirDatabase::class.java,
        ).allowMainThreadQueries().build()
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun startupImport_populatesPhotoMetadataAndRelationships() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.app.Application>()
        val importer = AppStartupInitializer(
            context = context,
            databaseProvider = { database },
            mediaRowsProvider = {
                listOf(
                    AppStartupInitializer.ImportedPhotoMetadata(
                        contentUri = "content://media/external/images/media/1001",
                        takenAt = 1_700_000_000_000,
                        width = 4032,
                        height = 3024,
                        bucketName = "sample_imgs",
                    ),
                    AppStartupInitializer.ImportedPhotoMetadata(
                        contentUri = "content://media/external/images/media/1002",
                        takenAt = 1_700_000_100_000,
                        width = 1920,
                        height = 1080,
                        bucketName = "sample_imgs",
                    ),
                )
            },
        )

        importer.run(forceEnabled = true)

        val photoAssetDao = database.photoAssetDao()
        val albumDao = database.albumDao()
        val albumPhotoDao = database.albumPhotoDao()
        val albumMemberDao = database.albumMemberDao()
        val journalEntryDao = database.journalEntryDao()
        val entryPhotoDao = database.entryPhotoDao()
        val tagDao = database.tagDao()
        val photoTagDao = database.photoTagDao()

        val first = photoAssetDao.getByContentUri("content://media/external/images/media/1001")
        val second = photoAssetDao.getByContentUri("content://media/external/images/media/1002")
        assertNotNull(first)
        assertNotNull(second)
        assertEquals(4032, first!!.width)
        assertEquals(3024, first.height)
        assertEquals(1920, second!!.width)
        assertEquals(1080, second.height)

        val albums = albumDao.observeAll().first()
        assertEquals(1, albums.size)
        assertEquals("sample_imgs", albums.first().name)

        val albumId = albums.first().id
        val albumPhotos = albumPhotoDao.observePhotosForAlbum(albumId).first()
        assertEquals(2, albumPhotos.size)

        val members = albumMemberDao.observeMembers(albumId).first()
        assertEquals(1, members.size)
        assertEquals("startup-importer", members.first().memberId)

        val entries = journalEntryDao.observeAll().first()
        assertEquals(1, entries.size)
        assertTrue(entries.first().title?.contains("Imported") == true)

        val entryPhotoLinks = entryPhotoDao.getByEntryIdOrdered(entries.first().id)
        assertEquals(2, entryPhotoLinks.size)

        val folderTag = tagDao.getByTypeAndValue(TagType.KEYWORD, "folder:sample_imgs")
        assertNotNull(folderTag)
        val folderTagId = requireNotNull(folderTag).id

        val tagLinks = photoTagDao.getByPhotoIds(listOf(first.id, second.id))
        assertEquals(2, tagLinks.size)
        assertTrue(tagLinks.all { it.tagId == folderTagId })
    }

    @Test
    fun startupImport_isIdempotentWhenRunTwice() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.app.Application>()
        val importer = AppStartupInitializer(
            context = context,
            databaseProvider = { database },
            mediaRowsProvider = {
                listOf(
                    AppStartupInitializer.ImportedPhotoMetadata(
                        contentUri = "content://media/external/images/media/2001",
                        takenAt = 1_700_100_000_000,
                        width = 1280,
                        height = 720,
                        bucketName = "MemoirMock",
                    ),
                )
            },
        )

        importer.run(forceEnabled = true)
        importer.run(forceEnabled = true)

        val photoAssetDao = database.photoAssetDao()
        val albumDao = database.albumDao()
        val albumPhotoDao = database.albumPhotoDao()
        val journalEntryDao = database.journalEntryDao()
        val entryPhotoDao = database.entryPhotoDao()
        val tagDao = database.tagDao()
        val photoTagDao = database.photoTagDao()

        val photo = photoAssetDao.getByContentUri("content://media/external/images/media/2001")
        assertNotNull(photo)

        val albums = albumDao.observeAll().first()
        assertEquals(1, albums.size)
        val albumId = albums.first().id
        assertEquals(1, albumPhotoDao.getLinksByAlbumId(albumId).size)

        val entries = journalEntryDao.observeAll().first()
        assertEquals(1, entries.size)
        assertEquals(1, entryPhotoDao.getByEntryIdOrdered(entries.first().id).size)

        val folderTag = tagDao.getByTypeAndValue(TagType.KEYWORD, "folder:memoirmock")
        assertNotNull(folderTag)
        assertEquals(1, photoTagDao.getByPhotoIds(listOf(photo!!.id)).size)
    }

    @Test
    fun startupImport_printsDatabaseContents() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.app.Application>()
        val importer = AppStartupInitializer(
            context = context,
            databaseProvider = { database },
            mediaRowsProvider = {
                listOf(
                    AppStartupInitializer.ImportedPhotoMetadata(
                        contentUri = "content://media/external/images/media/3001",
                        takenAt = 1_700_200_000_000,
                        width = 4000,
                        height = 3000,
                        bucketName = "sample_imgs",
                    ),
                    AppStartupInitializer.ImportedPhotoMetadata(
                        contentUri = "content://media/external/images/media/3002",
                        takenAt = 1_700_200_100_000,
                        width = 3024,
                        height = 3024,
                        bucketName = "sample_imgs",
                    ),
                    AppStartupInitializer.ImportedPhotoMetadata(
                        contentUri = "content://media/external/images/media/3003",
                        takenAt = 1_700_300_000_000,
                        width = 1920,
                        height = 1080,
                        bucketName = "MemoirMock",
                    ),
                )
            },
        )

        importer.run(forceEnabled = true)

        val photoAssetDao = database.photoAssetDao()
        val albumDao = database.albumDao()
        val albumPhotoDao = database.albumPhotoDao()
        val albumMemberDao = database.albumMemberDao()
        val journalEntryDao = database.journalEntryDao()
        val entryPhotoDao = database.entryPhotoDao()
        val tagDao = database.tagDao()
        val photoTagDao = database.photoTagDao()

        val photos = listOfNotNull(
            photoAssetDao.getByContentUri("content://media/external/images/media/3001"),
            photoAssetDao.getByContentUri("content://media/external/images/media/3002"),
            photoAssetDao.getByContentUri("content://media/external/images/media/3003"),
        )
        val albums = albumDao.observeAll().first()
        val entries = journalEntryDao.observeAll().first()
        val sampleTag = tagDao.getByTypeAndValue(TagType.KEYWORD, "folder:sample_imgs")
        val mockTag = tagDao.getByTypeAndValue(TagType.KEYWORD, "folder:memoirmock")
        val tagLinks = photoTagDao.getByPhotoIds(photos.map { it.id })

        println("=== STARTUP IMPORT DATABASE DUMP ===")
        println("photo_asset count=${photos.size}")
        photos.forEach { photo ->
            println("photo_asset: id=${photo.id}, uri=${photo.contentUri}, takenAt=${photo.takenAt}, width=${photo.width}, height=${photo.height}")
        }

        println("album count=${albums.size}")
        albums.forEach { album ->
            println("album: id=${album.id}, name=${album.name}, owner=${album.ownerUserId}, visibility=${album.visibility}")
            val memberRows = albumMemberDao.observeMembers(album.id).first()
            memberRows.forEach { member ->
                println("album_member: albumId=${member.albumId}, memberId=${member.memberId}, role=${member.role}, status=${member.status}")
            }
            val albumLinks = albumPhotoDao.getLinksByAlbumId(album.id)
            albumLinks.forEach { link ->
                println("album_photo: albumId=${link.albumId}, photoId=${link.photoId}, orderIndex=${link.orderIndex}, addedBy=${link.addedBy}")
            }
        }

        println("journal_entry count=${entries.size}")
        entries.forEach { entry ->
            println("journal_entry: id=${entry.id}, epochDay=${entry.entryDateEpochDay}, title=${entry.title}")
            val entryLinks = entryPhotoDao.getByEntryIdOrdered(entry.id)
            entryLinks.forEach { link ->
                println("entry_photo: entryId=${link.entryId}, photoId=${link.photoId}, orderIndex=${link.orderIndex}")
            }
        }

        println("tag(sample_imgs)=${sampleTag?.id} value=${sampleTag?.value}")
        println("tag(memoirmock)=${mockTag?.id} value=${mockTag?.value}")
        tagLinks.forEach { link ->
            println("photo_tag: photoId=${link.photoId}, tagId=${link.tagId}")
        }
        println("=== END STARTUP IMPORT DATABASE DUMP ===")

        assertTrue(photos.isNotEmpty())
    }
}
