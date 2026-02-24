package nostalgia.memoir.domain.usecase

import nostalgia.memoir.data.repository.AlbumRepository

class RemoveEntryFromAlbumUseCase(
    private val repository: AlbumRepository,
) {
    suspend operator fun invoke(albumId: String, entryId: String): Boolean =
        repository.removeEntryFromAlbum(albumId, entryId)
}
