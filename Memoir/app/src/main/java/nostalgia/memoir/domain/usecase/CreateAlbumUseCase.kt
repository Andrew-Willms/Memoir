package nostalgia.memoir.domain.usecase

import nostalgia.memoir.data.model.CreateAlbumInput
import nostalgia.memoir.data.repository.AlbumRepository

class CreateAlbumUseCase(
    private val repository: AlbumRepository,
) {
    suspend operator fun invoke(input: CreateAlbumInput): String = repository.createAlbum(input)
}
