package nostalgia.memoir.domain.usecase

import nostalgia.memoir.data.repository.AlbumRepository

class RemovePhotoFromAlbumUseCase(
    private val repository: AlbumRepository,
) {
    suspend operator fun invoke(albumId: String, photoId: String): Boolean =
        repository.removePhotoFromAlbum(albumId, photoId)
}
