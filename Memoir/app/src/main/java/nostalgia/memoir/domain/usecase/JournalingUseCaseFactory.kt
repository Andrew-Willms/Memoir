package nostalgia.memoir.domain.usecase

import android.content.Context
import nostalgia.memoir.data.repository.JournalingRepositoryFactory

object JournalingUseCaseFactory {

    fun create(context: Context): JournalingUseCases {
        val repository = JournalingRepositoryFactory.create(context)
        return JournalingUseCases(
            createEntry = CreateEntryUseCase(repository),
            updateEntry = UpdateEntryUseCase(repository),
            getEntryAggregate = GetEntryAggregateUseCase(repository),
            observeAllEntries = ObserveAllEntriesUseCase(repository),
            observeEntriesByDateRange = ObserveEntriesByDateRangeUseCase(repository),
            searchEntries = SearchEntriesUseCase(repository),
            getLinkedPhotoUrisForEpochDay = GetLinkedPhotoUrisForEpochDayUseCase(repository),
        )
    }
}
