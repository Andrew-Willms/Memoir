package nostalgia.memoir.domain.usecase

import kotlinx.coroutines.flow.Flow
import nostalgia.memoir.data.local.entities.AlbumEntity
import nostalgia.memoir.data.repository.AlbumRepository

class ObserveAlbumsByOwnerUseCase(
    private val repository: AlbumRepository,
) {
    operator fun invoke(ownerUserId: String): Flow<List<AlbumEntity>> =
        repository.observeAlbumsByOwner(ownerUserId)
}
