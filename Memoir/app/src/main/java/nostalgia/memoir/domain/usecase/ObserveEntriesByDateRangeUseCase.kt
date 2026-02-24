package nostalgia.memoir.domain.usecase

import kotlinx.coroutines.flow.Flow
import nostalgia.memoir.data.local.entities.JournalEntryEntity
import nostalgia.memoir.data.repository.JournalingRepository

class ObserveEntriesByDateRangeUseCase(
    private val repository: JournalingRepository,
) {
    operator fun invoke(startEpochDay: Long, endEpochDay: Long): Flow<List<JournalEntryEntity>> =
        repository.observeEntriesByDateRange(startEpochDay, endEpochDay)
}
