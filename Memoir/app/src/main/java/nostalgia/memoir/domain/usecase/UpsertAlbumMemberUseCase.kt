package nostalgia.memoir.domain.usecase

import nostalgia.memoir.data.model.UpsertAlbumMemberInput
import nostalgia.memoir.data.repository.AlbumRepository

class UpsertAlbumMemberUseCase(
    private val repository: AlbumRepository,
) {
    suspend operator fun invoke(input: UpsertAlbumMemberInput): Boolean =
        repository.upsertAlbumMember(input)
}
