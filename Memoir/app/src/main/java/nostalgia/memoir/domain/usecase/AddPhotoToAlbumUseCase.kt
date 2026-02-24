package nostalgia.memoir.domain.usecase

import nostalgia.memoir.data.model.AddPhotoToAlbumInput
import nostalgia.memoir.data.repository.AlbumRepository

class AddPhotoToAlbumUseCase(
    private val repository: AlbumRepository,
) {
    suspend operator fun invoke(input: AddPhotoToAlbumInput): Boolean = repository.addPhotoToAlbum(input)
}
