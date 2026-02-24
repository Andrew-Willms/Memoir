package nostalgia.memoir.domain.usecase

import android.content.Context
import nostalgia.memoir.data.repository.AlbumRepositoryFactory

object AlbumUseCaseFactory {

    fun create(context: Context): AlbumUseCases {
        val repository = AlbumRepositoryFactory.create(context)
        return AlbumUseCases(
            createAlbum = CreateAlbumUseCase(repository),
            renameAlbum = RenameAlbumUseCase(repository),
            getAlbumAggregate = GetAlbumAggregateUseCase(repository),
            addEntryToAlbum = AddEntryToAlbumUseCase(repository),
            removeEntryFromAlbum = RemoveEntryFromAlbumUseCase(repository),
            upsertAlbumMember = UpsertAlbumMemberUseCase(repository),
            removeAlbumMember = RemoveAlbumMemberUseCase(repository),
            observeAlbumsByOwner = ObserveAlbumsByOwnerUseCase(repository),
            observeAllAlbums = ObserveAllAlbumsUseCase(repository),
            observeEntriesForAlbum = ObserveEntriesForAlbumUseCase(repository),
            observeMembersForAlbum = ObserveMembersForAlbumUseCase(repository),
        )
    }
}
