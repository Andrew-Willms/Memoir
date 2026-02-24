package nostalgia.memoir.domain.usecase

import kotlinx.coroutines.flow.Flow
import nostalgia.memoir.data.local.entities.AlbumMemberEntity
import nostalgia.memoir.data.repository.AlbumRepository

class ObserveMembersForAlbumUseCase(
    private val repository: AlbumRepository,
) {
    operator fun invoke(albumId: String): Flow<List<AlbumMemberEntity>> =
        repository.observeMembersForAlbum(albumId)
}
