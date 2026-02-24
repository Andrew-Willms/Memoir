package nostalgia.memoir.domain.usecase

import nostalgia.memoir.data.model.AddEntryToAlbumInput
import nostalgia.memoir.data.repository.AlbumRepository

class AddEntryToAlbumUseCase(
    private val repository: AlbumRepository,
) {
    suspend operator fun invoke(input: AddEntryToAlbumInput): Boolean = repository.addEntryToAlbum(input)
}
