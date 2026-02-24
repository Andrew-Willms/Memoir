package nostalgia.memoir.domain.usecase

import kotlinx.coroutines.flow.Flow
import nostalgia.memoir.data.local.entities.PhotoAssetEntity
import nostalgia.memoir.data.local.entities.TagType
import nostalgia.memoir.data.repository.JournalingRepository

class ObservePhotosByTagUseCase(
    private val repository: JournalingRepository,
) {
    operator fun invoke(type: TagType, value: String): Flow<List<PhotoAssetEntity>> =
        repository.observePhotosByTag(type, value)
}
