package nostalgia.memoir.diagnostics

import android.content.Context
import androidx.room.Room
import kotlinx.coroutines.flow.first
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
import nostalgia.memoir.data.model.UpdateJournalEntryInput
import nostalgia.memoir.data.repository.RoomAlbumRepository
import nostalgia.memoir.data.repository.RoomJournalingRepository

data class DatabaseSelfTestResult(
    val name: String,
    val passed: Boolean,
    val details: String,
)

data class DatabaseSelfTestSuite(
    val name: String,
    val results: List<DatabaseSelfTestResult>,
)

class DatabaseSelfTestRunner(
    private val context: Context,
) {

    suspend fun runAllSuites(): List<DatabaseSelfTestSuite> {
        return listOf(
            DatabaseSelfTestSuite(name = "Journaling", results = runJournalingTests()),
            DatabaseSelfTestSuite(name = "Albums", results = runAlbumTests()),
        )
    }

    private suspend fun runJournalingTests(): List<DatabaseSelfTestResult> {
        return listOf(
            runJournalingTest("Create entry aggregate") { repository ->
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
            runJournalingTest("Update entry aggregate") { repository ->
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
            runJournalingTest("Search entries") { repository ->
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
            runJournalingTest("Date range query") { repository ->
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
            runJournalingTest("Linked photo URIs by day") { repository ->
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
            runJournalingTest("Observe all persisted entries") { repository ->
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

    private suspend fun runAlbumTests(): List<DatabaseSelfTestResult> {
        return listOf(
            runAlbumTest("Create album with owner member") { albumRepository, _ ->
                val albumId = albumRepository.createAlbum(
                    CreateAlbumInput(
                        name = "Vietnam Trip",
                        ownerUserId = "owner-1",
                        visibility = AlbumVisibility.PRIVATE,
                    ),
                )

                val aggregate = albumRepository.getAlbumAggregate(albumId)
                require(aggregate != null) { "Expected created album aggregate" }
                require(aggregate.members.any { it.memberId == "owner-1" && it.role == AlbumRole.OWNER }) {
                    "Expected active owner membership"
                }
            },
            runAlbumTest("Add/remove entry link in album") { albumRepository, journalingRepository ->
                val entryId = journalingRepository.createEntryAggregate(
                    CreateJournalEntryInput(
                        entryDateEpochDay = 21_000L,
                        title = "Trip note",
                        reflectionText = "Entry for album linking",
                        photos = emptyList(),
                        tags = emptyList(),
                    ),
                )
                val albumId = albumRepository.createAlbum(
                    CreateAlbumInput(name = "Family", ownerUserId = "owner-1"),
                )

                val added = albumRepository.addEntryToAlbum(
                    AddEntryToAlbumInput(
                        albumId = albumId,
                        entryId = entryId,
                        addedBy = "owner-1",
                    ),
                )
                require(added) { "Expected addEntryToAlbum=true" }

                val aggregateAfterAdd = albumRepository.getAlbumAggregate(albumId)
                require(aggregateAfterAdd != null && aggregateAfterAdd.entries.size == 1) {
                    "Expected one linked entry"
                }

                val removed = albumRepository.removeEntryFromAlbum(albumId, entryId)
                require(removed) { "Expected removeEntryFromAlbum=true" }
                val aggregateAfterRemove = albumRepository.getAlbumAggregate(albumId)
                require(aggregateAfterRemove != null && aggregateAfterRemove.entries.isEmpty()) {
                    "Expected no linked entries after remove"
                }
            },
            runAlbumTest("Add/remove direct photos in album") { albumRepository, journalingRepository ->
                val entryId = journalingRepository.createEntryAggregate(
                    CreateJournalEntryInput(
                        entryDateEpochDay = 21_100L,
                        title = "Photo Source",
                        reflectionText = "Source entry",
                        photos = listOf(
                            PhotoAssetDraft(contentUri = "content://album-photo-tests/1"),
                            PhotoAssetDraft(contentUri = "content://album-photo-tests/2"),
                        ),
                        tags = emptyList(),
                    ),
                )
                val photoId = journalingRepository.getEntryAggregate(entryId)!!.photos.first().photo.id

                val albumId = albumRepository.createAlbum(
                    CreateAlbumInput(name = "Album Photos", ownerUserId = "owner-1"),
                )

                val added = albumRepository.addPhotoToAlbum(
                    AddPhotoToAlbumInput(
                        albumId = albumId,
                        photoId = photoId,
                        addedBy = "owner-1",
                    ),
                )
                require(added) { "Expected addPhotoToAlbum=true" }

                val aggregateAfterAdd = albumRepository.getAlbumAggregate(albumId)
                require(aggregateAfterAdd != null && aggregateAfterAdd.photos.size == 1) {
                    "Expected one linked album photo"
                }

                val removed = albumRepository.removePhotoFromAlbum(albumId, photoId)
                require(removed) { "Expected removePhotoFromAlbum=true" }

                val aggregateAfterRemove = albumRepository.getAlbumAggregate(albumId)
                require(aggregateAfterRemove != null && aggregateAfterRemove.photos.isEmpty()) {
                    "Expected no linked album photos after remove"
                }
            },
            runAlbumTest("Observe albums by owner") { albumRepository, _ ->
                albumRepository.createAlbum(CreateAlbumInput(name = "Owner Album", ownerUserId = "owner-a"))
                albumRepository.createAlbum(CreateAlbumInput(name = "Other Album", ownerUserId = "owner-b"))

                val ownerAlbums = albumRepository.observeAlbumsByOwner("owner-a").first()
                require(ownerAlbums.size == 1) { "Expected exactly one owner album" }
                require(ownerAlbums.first().name == "Owner Album") { "Unexpected owner album name" }
            },
            runAlbumTest("Upsert member and observe members") { albumRepository, _ ->
                val albumId = albumRepository.createAlbum(
                    CreateAlbumInput(name = "Shared Album", ownerUserId = "owner-1"),
                )

                val upserted = albumRepository.upsertAlbumMember(
                    UpsertAlbumMemberInput(
                        albumId = albumId,
                        memberId = "friend-1",
                        role = AlbumRole.EDITOR,
                        status = AlbumMemberStatus.ACTIVE,
                    ),
                )
                require(upserted) { "Expected upsertAlbumMember=true" }

                val members = albumRepository.observeMembersForAlbum(albumId).first()
                require(members.any { it.memberId == "friend-1" && it.role == AlbumRole.EDITOR }) {
                    "Expected editor member in album"
                }
            },
        )
    }

    private suspend fun runJournalingTest(
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

    private suspend fun runAlbumTest(
        name: String,
        block: suspend (RoomAlbumRepository, RoomJournalingRepository) -> Unit,
    ): DatabaseSelfTestResult {
        val database = Room.inMemoryDatabaseBuilder(
            context,
            MemoirDatabase::class.java,
        ).build()

        return try {
            val albumRepository = RoomAlbumRepository(database)
            val journalingRepository = RoomJournalingRepository(database)
            block(albumRepository, journalingRepository)
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
