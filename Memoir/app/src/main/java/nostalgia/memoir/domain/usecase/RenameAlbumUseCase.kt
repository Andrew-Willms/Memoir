package nostalgia.memoir.domain.usecase

import nostalgia.memoir.data.repository.AlbumRepository

class RenameAlbumUseCase(
    private val repository: AlbumRepository,
) {
    suspend operator fun invoke(albumId: String, newName: String): Boolean =
        repository.renameAlbum(albumId, newName)
}
