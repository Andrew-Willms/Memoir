package nostalgia.memoir.domain.usecase

import kotlinx.coroutines.flow.Flow
import nostalgia.memoir.data.local.entities.AlbumEntity
import nostalgia.memoir.data.repository.AlbumRepository

class ObserveAllAlbumsUseCase(
    private val repository: AlbumRepository,
) {
    operator fun invoke(): Flow<List<AlbumEntity>> = repository.observeAllAlbums()
}
