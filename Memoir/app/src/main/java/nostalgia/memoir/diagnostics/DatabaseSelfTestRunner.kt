package nostalgia.memoir.diagnostics

import android.content.Context
import androidx.room.Room
import kotlinx.coroutines.flow.first
import nostalgia.memoir.data.local.MemoirDatabase
import nostalgia.memoir.data.local.entities.TagType
import nostalgia.memoir.data.model.CreateJournalEntryInput
import nostalgia.memoir.data.model.PhotoAssetDraft
import nostalgia.memoir.data.model.TagDraft
import nostalgia.memoir.data.model.UpdateJournalEntryInput
import nostalgia.memoir.data.repository.RoomJournalingRepository

data class DatabaseSelfTestResult(
    val name: String,
    val passed: Boolean,
    val details: String,
)

class DatabaseSelfTestRunner(
    private val context: Context,
) {

    suspend fun runAll(): List<DatabaseSelfTestResult> {
        return listOf(
            runTest("Create entry aggregate") { repository ->
                val entryId = repository.createEntryAggregate(
                    CreateJournalEntryInput(
                        entryDateEpochDay = 19_000L,
                        title = "Day One",
                        reflectionText = "Great hike today",
                        photos = listOf(
                            PhotoAssetDraft(contentUri = "content://photos/1"),
                            PhotoAssetDraft(contentUri = "content://photos/2"),
                        ),
                        tags = listOf(
                            TagDraft(type = TagType.PLACE, value = "Waterloo Park"),
                            TagDraft(type = TagType.KEYWORD, value = "hike"),
                        ),
                    ),
                )

                val aggregate = repository.getEntryAggregate(entryId)
                require(aggregate != null) { "Expected created aggregate" }
                require(aggregate.photos.size == 2) { "Expected 2 linked photos" }
                require(aggregate.tags.size == 2) { "Expected 2 linked tags" }
            },
            runTest("Update entry aggregate") { repository ->
                val entryId = repository.createEntryAggregate(
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
                        entryId = entryId,
                        title = "Updated",
                        reflectionText = "Updated reflection",
                        photos = listOf(PhotoAssetDraft(contentUri = "content://photos/c")),
                        tags = listOf(TagDraft(type = TagType.KEYWORD, value = "new")),
                    ),
                )

                require(updated) { "Expected update=true" }
                val aggregate = repository.getEntryAggregate(entryId)
                require(aggregate != null) { "Expected aggregate after update" }
                require(aggregate.entry.title == "Updated") { "Expected updated title" }
                require(aggregate.photos.size == 1) { "Expected replaced photo links" }
                require(aggregate.tags.size == 1) { "Expected replaced tag links" }
            },
            runTest("Search entries") { repository ->
                repository.createEntryAggregate(
                    CreateJournalEntryInput(
                        entryDateEpochDay = 19_002L,
                        title = "Beach day",
                        reflectionText = "Sunny afternoon at the beach",
                        photos = emptyList(),
                        tags = listOf(TagDraft(type = TagType.PLACE, value = "Santa Cruz")),
                    ),
                )

                val reflectionMatch = repository.searchEntries("beach").first()
                val tagMatch = repository.searchEntries("Santa Cruz").first()
                val none = repository.searchEntries("nonexistent").first()

                require(reflectionMatch.size == 1) { "Expected reflection search match" }
                require(tagMatch.size == 1) { "Expected tag search match" }
                require(none.isEmpty()) { "Expected no matches for nonexistent query" }
            },
            runTest("Date range query") { repository ->
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
                require(ranged.size == 2) { "Expected exactly 2 entries in date range" }
            },
            runTest("Linked photo URIs by day") { repository ->
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
                require(uris.size == 2) { "Expected exactly 2 linked URIs" }
                require(uris.contains("content://photos/today-1")) { "Missing expected URI 1" }
                require(uris.contains("content://photos/today-2")) { "Missing expected URI 2" }
            },
            runTest("Observe all persisted entries") { repository ->
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
                require(allEntries.size == 2) { "Expected 2 persisted entries" }
            },
        )
    }

    private suspend fun runTest(
        name: String,
        block: suspend (RoomJournalingRepository) -> Unit,
    ): DatabaseSelfTestResult {
        val database = Room.inMemoryDatabaseBuilder(
            context,
            MemoirDatabase::class.java,
        ).build()

        return try {
            val repository = RoomJournalingRepository(database)
            block(repository)
            DatabaseSelfTestResult(name = name, passed = true, details = "Passed")
        } catch (error: Throwable) {
            DatabaseSelfTestResult(
                name = name,
                passed = false,
                details = error.message ?: error::class.java.simpleName,
            )
        } finally {
            database.close()
        }
    }
}
