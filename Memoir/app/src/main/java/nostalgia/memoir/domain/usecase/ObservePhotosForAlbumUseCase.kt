package nostalgia.memoir.domain.usecase

import kotlinx.coroutines.flow.Flow
import nostalgia.memoir.data.local.entities.PhotoAssetEntity
import nostalgia.memoir.data.repository.AlbumRepository

class ObservePhotosForAlbumUseCase(
    private val repository: AlbumRepository,
) {
    operator fun invoke(albumId: String): Flow<List<PhotoAssetEntity>> =
        repository.observePhotosForAlbum(albumId)
}
