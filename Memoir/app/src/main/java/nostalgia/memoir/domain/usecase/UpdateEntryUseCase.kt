package nostalgia.memoir.domain.usecase

import nostalgia.memoir.data.model.UpdateJournalEntryInput
import nostalgia.memoir.data.repository.JournalingRepository

class UpdateEntryUseCase(
    private val repository: JournalingRepository,
) {
    suspend operator fun invoke(input: UpdateJournalEntryInput): Boolean =
        repository.updateEntryAggregate(input)
}
