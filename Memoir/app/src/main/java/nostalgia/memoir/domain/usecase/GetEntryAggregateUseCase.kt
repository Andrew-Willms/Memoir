package nostalgia.memoir.domain.usecase

import nostalgia.memoir.data.model.JournalEntryAggregate
import nostalgia.memoir.data.repository.JournalingRepository

class GetEntryAggregateUseCase(
    private val repository: JournalingRepository,
) {
    suspend operator fun invoke(entryId: String): JournalEntryAggregate? =
        repository.getEntryAggregate(entryId)
}
