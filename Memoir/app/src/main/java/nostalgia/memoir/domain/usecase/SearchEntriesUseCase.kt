package nostalgia.memoir.domain.usecase

import kotlinx.coroutines.flow.Flow
import nostalgia.memoir.data.local.entities.JournalEntryEntity
import nostalgia.memoir.data.repository.JournalingRepository

class SearchEntriesUseCase(
    private val repository: JournalingRepository,
) {
    operator fun invoke(query: String): Flow<List<JournalEntryEntity>> =
        repository.searchEntries(query)
}
