package nostalgia.memoir.domain.usecase

data class AlbumUseCases(
    val createAlbum: CreateAlbumUseCase,
    val renameAlbum: RenameAlbumUseCase,
    val getAlbumAggregate: GetAlbumAggregateUseCase,
    val addPhotoToAlbum: AddPhotoToAlbumUseCase,
    val removePhotoFromAlbum: RemovePhotoFromAlbumUseCase,
    val addEntryToAlbum: AddEntryToAlbumUseCase,
    val removeEntryFromAlbum: RemoveEntryFromAlbumUseCase,
    val upsertAlbumMember: UpsertAlbumMemberUseCase,
    val removeAlbumMember: RemoveAlbumMemberUseCase,
    val observeAlbumsByOwner: ObserveAlbumsByOwnerUseCase,
    val observeAllAlbums: ObserveAllAlbumsUseCase,
    val observePhotosForAlbum: ObservePhotosForAlbumUseCase,
    val observeEntriesForAlbum: ObserveEntriesForAlbumUseCase,
    val observeMembersForAlbum: ObserveMembersForAlbumUseCase,
)
