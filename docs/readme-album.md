# Memoir Album Database Backend

This document describes the new Album database architecture and APIs added to the Android module.

## Scope completed

Implemented Album persistence and interaction APIs on top of Room:

- Album table and enums
- Album-photo join table (direct album photo ownership)
- Album-entry join table (many-to-many with journal entries)
- Album-member table (for owner/editor/viewer + status)
- DAOs
- Repository and use-cases
- Unit tests
- Debug diagnostics integration (albums listed below journaling tests)

---

## 1) Album schema

### Entities

- `AlbumEntity`
  - file: `Memoir/app/src/main/java/nostalgia/memoir/data/local/entities/AlbumEntity.kt`
  - fields: `id`, `createdAt`, `updatedAt`, `name`, `ownerUserId`, `visibility`

- `AlbumEntryCrossRef`
  - file: `Memoir/app/src/main/java/nostalgia/memoir/data/local/entities/AlbumEntryCrossRef.kt`
  - links `album` ↔ `journal_entry`
  - fields: `albumId`, `entryId`, `addedAt`, `addedBy`

- `AlbumPhotoCrossRef`
  - file: `Memoir/app/src/main/java/nostalgia/memoir/data/local/entities/AlbumPhotoCrossRef.kt`
  - links `album` ↔ `photo_asset`
  - fields: `albumId`, `photoId`, `orderIndex`, `addedAt`, `addedBy`
  - `photoId` is unique, so a photo belongs to one album at a time

- `AlbumMemberEntity`
  - file: `Memoir/app/src/main/java/nostalgia/memoir/data/local/entities/AlbumMemberEntity.kt`
  - fields: `albumId`, `memberId`, `role`, `status`, `addedAt`

### Enums

- `AlbumVisibility`: `PRIVATE`, `SHARED`
- `AlbumRole`: `OWNER`, `EDITOR`, `VIEWER`
- `AlbumMemberStatus`: `INVITED`, `ACTIVE`, `REMOVED`

files:
- `AlbumVisibility.kt`
- `AlbumRole.kt`
- `AlbumMemberStatus.kt`

---

## 2) Room integration

### Database registration

`MemoirDatabase` now includes album entities and DAOs.

- file: `Memoir/app/src/main/java/nostalgia/memoir/data/local/MemoirDatabase.kt`
- schema version bumped from `1` → `3`

### Migration

Added migration `1 -> 2` to create:
- `album`
- `album_entry`
- `album_member`

Added migration `2 -> 3` to create:
- `album_photo`

- file: `Memoir/app/src/main/java/nostalgia/memoir/data/local/MemoirDatabaseProvider.kt`

### Type converters

`RoomTypeConverters` now supports:
- `AlbumVisibility`
- `AlbumRole`
- `AlbumMemberStatus`

- file: `Memoir/app/src/main/java/nostalgia/memoir/data/local/converters/RoomTypeConverters.kt`

---

## 3) Album DAOs

- `AlbumDao`
  - create/update/get
  - observe all albums
  - observe albums by owner
  - file: `Memoir/app/src/main/java/nostalgia/memoir/data/local/dao/AlbumDao.kt`

- `AlbumEntryDao`
  - upsert/delete album-entry links
  - list links by album
  - observe entry rows for an album
  - file: `Memoir/app/src/main/java/nostalgia/memoir/data/local/dao/AlbumEntryDao.kt`

- `AlbumPhotoDao`
  - upsert/delete album-photo links
  - list links by album with ordering
  - observe photo rows for an album
  - file: `Memoir/app/src/main/java/nostalgia/memoir/data/local/dao/AlbumPhotoDao.kt`

- `AlbumMemberDao`
  - upsert/delete members
  - observe members for an album
  - file: `Memoir/app/src/main/java/nostalgia/memoir/data/local/dao/AlbumMemberDao.kt`

Also added `JournalEntryDao.getByIds(...)` to build album aggregates efficiently.

---

## 4) Repository + model APIs

### Models

Album input/output models:

- `CreateAlbumInput`
- `AddEntryToAlbumInput`
- `AddPhotoToAlbumInput`
- `UpsertAlbumMemberInput`
- `LinkedAlbumPhoto`
- `LinkedAlbumEntry`
- `AlbumAggregate`

- file: `Memoir/app/src/main/java/nostalgia/memoir/data/model/AlbumModels.kt`

### Repository

- Interface: `AlbumRepository`
  - file: `Memoir/app/src/main/java/nostalgia/memoir/data/repository/AlbumRepository.kt`
- Implementation: `RoomAlbumRepository`
  - file: `Memoir/app/src/main/java/nostalgia/memoir/data/repository/RoomAlbumRepository.kt`
- Factory: `AlbumRepositoryFactory`
  - file: `Memoir/app/src/main/java/nostalgia/memoir/data/repository/AlbumRepositoryFactory.kt`

Implemented operations:

- `createAlbum`
- `renameAlbum`
- `getAlbumAggregate`
- `addEntryToAlbum`
- `addPhotoToAlbum`
- `removeEntryFromAlbum`
- `removePhotoFromAlbum`
- `upsertAlbumMember`
- `removeAlbumMember`
- `observeAlbumsByOwner`
- `observeAllAlbums`
- `observePhotosForAlbum`
- `observeEntriesForAlbum`
- `observeMembersForAlbum`

Behavior details:

- Creating an album also creates an active owner membership (`OWNER`, `ACTIVE`).
- Adding/removing entries updates album `updatedAt`.
- Adding/removing photos updates album `updatedAt`.
- Member upserts/removals update album `updatedAt`.

---

## 5) Domain use-cases

Added dedicated album use-cases and bundle/factory:

- `CreateAlbumUseCase`
- `RenameAlbumUseCase`
- `GetAlbumAggregateUseCase`
- `AddEntryToAlbumUseCase`
- `AddPhotoToAlbumUseCase`
- `RemoveEntryFromAlbumUseCase`
- `RemovePhotoFromAlbumUseCase`
- `UpsertAlbumMemberUseCase`
- `RemoveAlbumMemberUseCase`
- `ObserveAlbumsByOwnerUseCase`
- `ObserveAllAlbumsUseCase`
- `ObservePhotosForAlbumUseCase`
- `ObserveEntriesForAlbumUseCase`
- `ObserveMembersForAlbumUseCase`
- `AlbumUseCases`
- `AlbumUseCaseFactory`

files:
- `Memoir/app/src/main/java/nostalgia/memoir/domain/usecase/*Album*.kt`

---

## 6) Unit tests

Added album repository unit tests:

- file: `Memoir/app/src/test/java/nostalgia/memoir/data/repository/RoomAlbumRepositoryTest.kt`

Coverage includes:

- create album creates owner membership
- add/remove direct album-photo behavior
- one-photo-one-album constraint behavior
- add/remove entry link behavior
- observe albums by owner
- upsert member and observe members

---

## 7) Debug diagnostics screen integration

Album self-tests were added to the runtime debug diagnostics runner and shown below journaling tests.

- runner: `Memoir/app/src/main/java/nostalgia/memoir/diagnostics/DatabaseSelfTestRunner.kt`
- UI: `Memoir/app/src/main/java/nostalgia/memoir/MainActivity.kt`

The debug screen now executes two suites:
1. Journaling
2. Albums

Each test displays `✅` or `❌` plus details.

---

## 8) Verification commands run

Executed successfully:

```bash
cd Memoir
./gradlew :app:compileDebugKotlin :app:testDebugUnitTest :app:assembleDebug
```

All compile/test/build steps passed.
