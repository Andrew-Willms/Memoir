package nostalgia.memoir.domain.usecase

import nostalgia.memoir.data.repository.JournalingRepository

class GetLinkedPhotoUrisForEpochDayUseCase(
    private val repository: JournalingRepository,
) {
    suspend operator fun invoke(epochDay: Long): List<String> =
        repository.getLinkedPhotoUrisForEpochDay(epochDay)
}
