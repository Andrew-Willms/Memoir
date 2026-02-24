package nostalgia.memoir.domain.usecase

data class JournalingUseCases(
    val createEntry: CreateEntryUseCase,
    val updateEntry: UpdateEntryUseCase,
    val getEntryAggregate: GetEntryAggregateUseCase,
    val observeAllEntries: ObserveAllEntriesUseCase,
    val observeEntriesByDateRange: ObserveEntriesByDateRangeUseCase,
    val searchEntries: SearchEntriesUseCase,
    val getLinkedPhotoUrisForEpochDay: GetLinkedPhotoUrisForEpochDayUseCase,
)
