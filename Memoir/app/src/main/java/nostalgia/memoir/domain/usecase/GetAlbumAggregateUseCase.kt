package nostalgia.memoir.domain.usecase

import nostalgia.memoir.data.model.AlbumAggregate
import nostalgia.memoir.data.repository.AlbumRepository

class GetAlbumAggregateUseCase(
    private val repository: AlbumRepository,
) {
    suspend operator fun invoke(albumId: String): AlbumAggregate? = repository.getAlbumAggregate(albumId)
}
