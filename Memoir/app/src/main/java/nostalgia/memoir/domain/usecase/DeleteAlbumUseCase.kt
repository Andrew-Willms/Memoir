package nostalgia.memoir.domain.usecase

import nostalgia.memoir.data.repository.AlbumRepository

class DeleteAlbumUseCase(
    private val repository: AlbumRepository,
) {
    suspend operator fun invoke(albumId: String): Boolean = repository.deleteAlbum(albumId)
}
