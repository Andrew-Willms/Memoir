package nostalgia.memoir.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import nostalgia.memoir.data.local.MemoirDatabase
import nostalgia.memoir.data.local.entities.TagType
import nostalgia.memoir.data.model.CreateJournalEntryInput
import nostalgia.memoir.data.model.PhotoAssetDraft
import nostalgia.memoir.data.model.TagDraft
import nostalgia.memoir.data.model.UpdateJournalEntryInput
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RoomJournalingRepositoryTest {

    private lateinit var database: MemoirDatabase
    private lateinit var repository: RoomJournalingRepository

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            MemoirDatabase::class.java,
        ).allowMainThreadQueries().build()

        repository = RoomJournalingRepository(database)
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun createEntryAggregate_persistsEntryPhotosAndTags() = runBlocking {
        val input = CreateJournalEntryInput(
            entryDateEpochDay = 19_000L,
            title = "Day One",
            reflectionText = "Great hike today",
            photos = listOf(
                PhotoAssetDraft(contentUri = "content://photos/1", width = 400, height = 300),
                PhotoAssetDraft(contentUri = "content://photos/2", width = 800, height = 600),
            ),
            tags = listOf(
                TagDraft(type = TagType.LOCATION, value = "Waterloo Park"),
                TagDraft(type = TagType.KEYWORD, value = "hike"),
            ),
        )

        val entryId = repository.createEntryAggregate(input)
        val aggregate = repository.getEntryAggregate(entryId)

        assertNotNull(aggregate)
        assertEquals("Day One", aggregate!!.entry.title)
        assertEquals(2, aggregate.photos.size)
        assertEquals(listOf(0, 1), aggregate.photos.map { it.orderIndex })
        assertEquals(2, aggregate.tags.size)
        assertTrue(aggregate.tags.any { it.type == TagType.LOCATION && it.value == "Waterloo Park" })
        assertTrue(aggregate.tags.any { it.type == TagType.KEYWORD && it.value == "hike" })
    }

    @Test
    fun updateEntryAggregate_replacesLinksAndUpdatesText() = runBlocking {
        val createdEntryId = repository.createEntryAggregate(
            CreateJournalEntryInput(
                entryDateEpochDay = 19_001L,
                title = "Initial",
                reflectionText = "Initial reflection",
                photos = listOf(
                    PhotoAssetDraft(contentUri = "content://photos/a"),
                    PhotoAssetDraft(contentUri = "content://photos/b"),
                ),
                tags = listOf(
                    TagDraft(type = TagType.PERSON, value = "Sam"),
                    TagDraft(type = TagType.KEYWORD, value = "old"),
                ),
            ),
        )

        val updated = repository.updateEntryAggregate(
            UpdateJournalEntryInput(
                entryId = createdEntryId,
                title = "Updated",
                reflectionText = "Updated reflection",
                photos = listOf(PhotoAssetDraft(contentUri = "content://photos/c")),
                tags = listOf(TagDraft(type = TagType.KEYWORD, value = "new")),
            ),
        )

        val aggregate = repository.getEntryAggregate(createdEntryId)

        assertTrue(updated)
        assertNotNull(aggregate)
        assertEquals("Updated", aggregate!!.entry.title)
        assertEquals("Updated reflection", aggregate.entry.reflectionText)
        assertEquals(1, aggregate.photos.size)
        assertEquals("content://photos/c", aggregate.photos.first().photo.contentUri)
        assertEquals(1, aggregate.tags.size)
        assertEquals("new", aggregate.tags.first().value)
    }

    @Test
    fun searchEntries_matchesReflectionAndTags() = runBlocking {
        repository.createEntryAggregate(
            CreateJournalEntryInput(
                entryDateEpochDay = 19_002L,
                title = "Beach day",
                reflectionText = "Sunny afternoon at the beach",
                photos = listOf(PhotoAssetDraft(contentUri = "content://photos/beach")),
                tags = listOf(TagDraft(type = TagType.LOCATION, value = "Santa Cruz")),
            ),
        )

        repository.createEntryAggregate(
            CreateJournalEntryInput(
                entryDateEpochDay = 19_002L,
                title = "Gym",
                reflectionText = "Strength training",
                photos = listOf(PhotoAssetDraft(contentUri = "content://photos/gym")),
                tags = listOf(TagDraft(type = TagType.KEYWORD, value = "fitness")),
            ),
        )

        val byReflection = repository.searchEntries("beach").first()
        val byTag = repository.searchEntries("Santa Cruz").first()
        val none = repository.searchEntries("nonexistent").first()

        assertEquals(1, byReflection.size)
        assertEquals("Beach day", byReflection.first().title)
        assertEquals(1, byTag.size)
        assertEquals("Beach day", byTag.first().title)
        assertTrue(none.isEmpty())
    }

    @Test
    fun observePhotosByTag_filtersPhotosByTag() = runBlocking {
        repository.createEntryAggregate(
            CreateJournalEntryInput(
                entryDateEpochDay = 19_300L,
                title = "Tagged photos",
                reflectionText = "Contains a person tag",
                photos = listOf(
                    PhotoAssetDraft(contentUri = "content://photos/person-1"),
                    PhotoAssetDraft(contentUri = "content://photos/person-2"),
                ),
                tags = listOf(TagDraft(type = TagType.PERSON, value = "Sam")),
            ),
        )

        repository.createEntryAggregate(
            CreateJournalEntryInput(
                entryDateEpochDay = 19_301L,
                title = "Other photos",
                reflectionText = "Location only",
                photos = listOf(PhotoAssetDraft(contentUri = "content://photos/location-only")),
                tags = listOf(TagDraft(type = TagType.LOCATION, value = "Toronto")),
            ),
        )

        val photosWithSam = repository.observePhotosByTag(TagType.PERSON, "Sam").first()

        assertEquals(2, photosWithSam.size)
        assertTrue(photosWithSam.all { it.contentUri.contains("person-") })
    }

    @Test
    fun observeEntriesByDateRange_filtersCorrectly() = runBlocking {
        repository.createEntryAggregate(
            CreateJournalEntryInput(
                entryDateEpochDay = 19_010L,
                title = "In range A",
                reflectionText = "A",
                photos = emptyList(),
                tags = emptyList(),
            ),
        )
        repository.createEntryAggregate(
            CreateJournalEntryInput(
                entryDateEpochDay = 19_011L,
                title = "In range B",
                reflectionText = "B",
                photos = emptyList(),
                tags = emptyList(),
            ),
        )
        repository.createEntryAggregate(
            CreateJournalEntryInput(
                entryDateEpochDay = 19_020L,
                title = "Out of range",
                reflectionText = "C",
                photos = emptyList(),
                tags = emptyList(),
            ),
        )

        val ranged = repository.observeEntriesByDateRange(19_010L, 19_011L).first()

        assertEquals(2, ranged.size)
        assertTrue(ranged.all { it.entryDateEpochDay in 19_010L..19_011L })
    }

    @Test
    fun getLinkedPhotoUrisForEpochDay_returnsOnlyLinkedForDay() = runBlocking {
        repository.createEntryAggregate(
            CreateJournalEntryInput(
                entryDateEpochDay = 19_100L,
                title = "Today",
                reflectionText = "Two photos",
                photos = listOf(
                    PhotoAssetDraft(contentUri = "content://photos/today-1"),
                    PhotoAssetDraft(contentUri = "content://photos/today-2"),
                ),
                tags = emptyList(),
            ),
        )

        repository.createEntryAggregate(
            CreateJournalEntryInput(
                entryDateEpochDay = 19_101L,
                title = "Another day",
                reflectionText = "Other photo",
                photos = listOf(PhotoAssetDraft(contentUri = "content://photos/other-day")),
                tags = emptyList(),
            ),
        )

        val uris = repository.getLinkedPhotoUrisForEpochDay(19_100L)

        assertEquals(2, uris.size)
        assertTrue(uris.contains("content://photos/today-1"))
        assertTrue(uris.contains("content://photos/today-2"))
    }

    @Test
    fun observeAllEntries_returnsPersistedEntriesOnOpen() = runBlocking {
        repository.createEntryAggregate(
            CreateJournalEntryInput(
                entryDateEpochDay = 19_200L,
                title = "First",
                reflectionText = "One",
                photos = emptyList(),
                tags = emptyList(),
            ),
        )
        repository.createEntryAggregate(
            CreateJournalEntryInput(
                entryDateEpochDay = 19_201L,
                title = "Second",
                reflectionText = "Two",
                photos = emptyList(),
                tags = emptyList(),
            ),
        )

        val allEntries = repository.observeAllEntries().first()

        assertEquals(2, allEntries.size)
        assertTrue(allEntries.any { it.title == "First" })
        assertTrue(allEntries.any { it.title == "Second" })
    }
}
