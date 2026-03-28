package nostalgia.memoir.screens.data

import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import nostalgia.memoir.data.local.MemoirDatabase
import nostalgia.memoir.data.local.MemoirDatabaseProvider
import nostalgia.memoir.data.local.entities.TagType
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BackendUiBridgeIntegrationTest {

    private val context = ApplicationProvider.getApplicationContext<android.app.Application>()

    @Before
    fun setup() {
        clearLegacyPrefs()
        BackendUiBridge.resetForTests()
        MemoirDatabaseProvider.resetForTests()
        context.deleteDatabase(MemoirDatabase.DATABASE_NAME)
    }

    @After
    fun teardown() {
        MemoirDatabaseProvider.resetForTests()
        BackendUiBridge.resetForTests()
        clearLegacyPrefs()
        context.deleteDatabase(MemoirDatabase.DATABASE_NAME)
    }

    @Test
    fun albumsPersistToBackendAndKeepMultiAlbumPhotoBehavior() = runBlocking {
        val myAlbum = createAlbum(context, name = "Family", isShared = false)
        val sharedAlbum = createAlbum(context, name = "Trip", isShared = true)

        addPhotoToAlbum(context, myAlbum.id, "photos/1.jpg")
        addPhotoToAlbum(context, sharedAlbum.id, "photos/1.jpg")

        val myAlbums = loadMyAlbums(context)
        val sharedAlbums = loadSharedAlbums(context)

        assertEquals(listOf("Family"), myAlbums.map { it.name })
        assertEquals(listOf("Trip"), sharedAlbums.map { it.name })
        assertTrue(isPhotoInAlbum(context, myAlbum.id, "photos/1.jpg"))
        assertTrue(isPhotoInAlbum(context, sharedAlbum.id, "photos/1.jpg"))

        val database = MemoirDatabaseProvider.getInstance(context)
        val albumPhotosByAlbum = database.albumPhotoDao().getLinksByAlbumId(myAlbum.id)
        val sharedPhotosByAlbum = database.albumPhotoDao().getLinksByAlbumId(sharedAlbum.id)

        assertEquals(1, albumPhotosByAlbum.size)
        assertEquals(1, sharedPhotosByAlbum.size)
        assertEquals(albumPhotosByAlbum.first().photoId, sharedPhotosByAlbum.first().photoId)

        deleteAlbum(context, myAlbum.id)

        assertTrue(loadMyAlbums(context).isEmpty())
        assertEquals(setOf("photos/1.jpg"), loadPhotosInAlbum(context, sharedAlbum.id))
        assertNotNull(database.albumDao().getById(sharedAlbum.id))
        assertEquals(null, database.albumDao().getById(myAlbum.id))
    }

    @Test
    fun tagsAndSearchPersistToBackend() = runBlocking {
        addTagToPhoto(context, "photos/2.jpg", StoredPhotoTag(StoredPhotoTagType.PERSON, "Sam"))
        addTagToPhoto(context, "photos/2.jpg", StoredPhotoTag(StoredPhotoTagType.LOCATION, "Toronto"))
        addTagToPhoto(context, "photos/3.jpg", StoredPhotoTag(StoredPhotoTagType.KEYWORD, "sunset"))

        val loadedTags = loadTagsForPhoto(context, "photos/2.jpg")
        val searchResults = searchPhotos(context, "Sam")

        assertEquals(2, loadedTags.size)
        assertEquals(1, searchResults.size)
        assertEquals("photos/2.jpg", searchResults.first().assetPath)
        assertEquals("Sam", searchResults.first().matchingTags.first().value)

        removeTagFromPhoto(context, "photos/2.jpg", StoredPhotoTag(StoredPhotoTagType.PERSON, "Sam"))
        assertTrue(loadTagsForPhoto(context, "photos/2.jpg").none { it.value == "Sam" })

        val database = MemoirDatabaseProvider.getInstance(context)
        val photo = database.photoAssetDao().getByContentUri("photos/2.jpg")
        assertNotNull(photo)

        val remainingLinks = database.photoTagDao().getByPhotoIds(listOf(requireNotNull(photo).id))
        val remainingTags = database.tagDao().getByIds(remainingLinks.map { it.tagId })

        assertEquals(1, remainingTags.size)
        assertEquals(TagType.LOCATION, remainingTags.first().type)
        assertEquals("Toronto", remainingTags.first().value)
    }

    @Test
    fun tagReadOrderMatchesInsertionOrder() = runBlocking {
        addTagToPhoto(context, "photos/5.jpg", StoredPhotoTag(StoredPhotoTagType.PERSON, "Ava"))
        addTagToPhoto(context, "photos/5.jpg", StoredPhotoTag(StoredPhotoTagType.LOCATION, "Seattle"))
        addTagToPhoto(context, "photos/5.jpg", StoredPhotoTag(StoredPhotoTagType.KEYWORD, "Ocean"))

        val loadedTags = loadTagsForPhoto(context, "photos/5.jpg")

        assertEquals(
            listOf("Ava", "Seattle", "Ocean"),
            loadedTags.map { it.value },
        )
    }

    @Test
    fun albumSearchMatchesLegacyOrderAndExactMatchFirst() = runBlocking {
        createAlbum(context, name = "Seattle Trip", isShared = false)
        createAlbum(context, name = "Mexico", isShared = true)
        createAlbum(context, name = "Seattle", isShared = false)

        val searchResults = searchAlbums(context, "Seattle")

        assertEquals(
            listOf("Seattle", "Seattle Trip"),
            searchResults.map { it.name },
        )
    }

    @Test
    fun legacyPrefsMigrateIntoBackendForAlbumsTagsAndJournals() = runBlocking {
        seedLegacyAlbumPrefs()
        seedLegacyTagPrefs()
        seedLegacyJournalPrefs()

        assertEquals(listOf("Legacy Album"), loadMyAlbums(context).map { it.name })
        assertEquals(setOf("photos/9.jpg"), loadPhotosInAlbum(context, "legacy-album-id"))
        assertEquals("Legacy reflection", loadJournalEntry(context, "photos/9.jpg", "Write your thoughts..."))
        assertEquals(1, searchPhotos(context, "beach").size)

        val database = MemoirDatabaseProvider.getInstance(context)
        val migratedAlbum = database.albumDao().getById("legacy-album-id")
        val migratedPhoto = database.photoAssetDao().getByContentUri("photos/9.jpg")
        val migratedEntry = database.journalEntryDao().getById(
            "ui-photo-journal:" + java.util.UUID.nameUUIDFromBytes("photos/9.jpg".toByteArray()).toString(),
        )

        assertNotNull(migratedAlbum)
        assertNotNull(migratedPhoto)
        assertNotNull(migratedEntry)
        assertEquals("Legacy reflection", migratedEntry!!.reflectionText)

        val photoLinks = database.albumPhotoDao().getLinksByAlbumId("legacy-album-id")
        assertEquals(1, photoLinks.size)

        val tagLinks = database.photoTagDao().getByPhotoIds(listOf(requireNotNull(migratedPhoto).id))
        val tags = database.tagDao().getByIds(tagLinks.map { it.tagId })
        assertTrue(tags.any { it.value == "beach" && it.type == TagType.KEYWORD })
    }

    @Test
    fun photoSearchReturnsJournalMatchesFromBackend() = runBlocking {
        saveJournalEntry(
            context,
            "photos/7.jpg",
            "We watched the northern lights dancing over the lake all night.",
        )

        val results = searchPhotos(context, "northern")

        assertEquals(1, results.size)
        assertEquals("photos/7.jpg", results.first().assetPath)
        assertTrue(results.first().matchingTags.isEmpty())
        assertTrue(results.first().matchingJournalPreviews.first().contains("northern", ignoreCase = true))

        val database = MemoirDatabaseProvider.getInstance(context)
        val matchedEntries = database.journalEntryDao().searchByFullText("northern*")
        assertEquals(1, matchedEntries.size)
        assertEquals("We watched the northern lights dancing over the lake all night.", matchedEntries.first().reflectionText)
    }

    @Test
    fun photoSearchMergesTagAndJournalMatchesForSamePhoto() = runBlocking {
        addTagToPhoto(context, "photos/8.jpg", StoredPhotoTag(StoredPhotoTagType.KEYWORD, "Sunset"))
        saveJournalEntry(
            context,
            "photos/8.jpg",
            "Sunset reflections and late snacks after the hike.",
        )

        val results = searchPhotos(context, "sunset")

        assertEquals(1, results.size)
        assertEquals("photos/8.jpg", results.first().assetPath)
        assertEquals(listOf("Sunset"), results.first().matchingTags.map { it.value })
        assertTrue(results.first().matchingJournalPreviews.isNotEmpty())
    }

    private fun clearLegacyPrefs() {
        context.getSharedPreferences("album_store", android.content.Context.MODE_PRIVATE).edit().clear().commit()
        context.getSharedPreferences("journal_entries", android.content.Context.MODE_PRIVATE).edit().clear().commit()
    }

    private fun seedLegacyAlbumPrefs() {
        context.getSharedPreferences("album_store", android.content.Context.MODE_PRIVATE)
            .edit()
            .putString("my_albums", "legacy-album-id|Legacy Album")
            .putString("album_photos_legacy-album-id", "photos/9.jpg")
            .commit()
    }

    private fun seedLegacyTagPrefs() {
        val tags = JSONArray()
            .put(JSONObject().put("type", "KEYWORD").put("value", "beach"))
            .put(JSONObject().put("type", "PERSON").put("value", "Ava"))

        context.getSharedPreferences("album_store", android.content.Context.MODE_PRIVATE)
            .edit()
            .putString("photo_tags_photos/9.jpg", tags.toString())
            .commit()
    }

    private fun seedLegacyJournalPrefs() {
        context.getSharedPreferences("journal_entries", android.content.Context.MODE_PRIVATE)
            .edit()
            .putString("journal_photos/9.jpg", "Legacy reflection")
            .commit()
    }
}
