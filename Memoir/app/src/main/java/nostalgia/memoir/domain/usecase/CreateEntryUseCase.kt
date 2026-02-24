package nostalgia.memoir.domain.usecase

import nostalgia.memoir.data.model.CreateJournalEntryInput
import nostalgia.memoir.data.repository.JournalingRepository

class CreateEntryUseCase(
    private val repository: JournalingRepository,
) {
    suspend operator fun invoke(input: CreateJournalEntryInput): String =
        repository.createEntryAggregate(input)
}
