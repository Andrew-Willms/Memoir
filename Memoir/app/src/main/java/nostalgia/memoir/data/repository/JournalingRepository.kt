package nostalgia.memoir.data.repository

import kotlinx.coroutines.flow.Flow
import nostalgia.memoir.data.local.entities.JournalEntryEntity
import nostalgia.memoir.data.local.entities.PhotoAssetEntity
import nostalgia.memoir.data.local.entities.TagType
import nostalgia.memoir.data.model.CreateJournalEntryInput
import nostalgia.memoir.data.model.JournalEntryAggregate
import nostalgia.memoir.data.model.UpdateJournalEntryInput

interface JournalingRepository {
    suspend fun createEntryAggregate(input: CreateJournalEntryInput): String
    suspend fun updateEntryAggregate(input: UpdateJournalEntryInput): Boolean
    suspend fun getEntryAggregate(entryId: String): JournalEntryAggregate?
    fun observeAllEntries(): Flow<List<JournalEntryEntity>>
    fun observeEntriesByDateRange(startEpochDay: Long, endEpochDay: Long): Flow<List<JournalEntryEntity>>
    fun searchEntries(query: String): Flow<List<JournalEntryEntity>>
    fun observePhotosByTag(type: TagType, value: String): Flow<List<PhotoAssetEntity>>
    suspend fun getLinkedPhotoUrisForEpochDay(epochDay: Long): List<String>
}
