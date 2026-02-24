package nostalgia.memoir.domain.usecase

import nostalgia.memoir.data.repository.AlbumRepository

class RemoveAlbumMemberUseCase(
    private val repository: AlbumRepository,
) {
    suspend operator fun invoke(albumId: String, memberId: String): Boolean =
        repository.removeAlbumMember(albumId, memberId)
}
