package nostalgia.memoir.domain.usecase

import kotlinx.coroutines.flow.Flow
import nostalgia.memoir.data.local.entities.JournalEntryEntity
import nostalgia.memoir.data.repository.JournalingRepository

class ObserveAllEntriesUseCase(
    private val repository: JournalingRepository,
) {
    operator fun invoke(): Flow<List<JournalEntryEntity>> = repository.observeAllEntries()
}
