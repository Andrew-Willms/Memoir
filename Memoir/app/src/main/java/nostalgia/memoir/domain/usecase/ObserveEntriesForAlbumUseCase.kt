package nostalgia.memoir.domain.usecase

import kotlinx.coroutines.flow.Flow
import nostalgia.memoir.data.local.entities.JournalEntryEntity
import nostalgia.memoir.data.repository.AlbumRepository

class ObserveEntriesForAlbumUseCase(
    private val repository: AlbumRepository,
) {
    operator fun invoke(albumId: String): Flow<List<JournalEntryEntity>> =
        repository.observeEntriesForAlbum(albumId)
}
