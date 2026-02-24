# Backend Guide: Interacting with Memoir Data Layer

This guide explains how a backend-focused developer should use the Android data layer that has been implemented in `Memoir/`.

It covers:
- what the data layer owns,
- which functions are available,
- how to call them safely,
- and common usage flows.

---

## 1) What this layer does

The data layer is a local-first persistence layer built on Room (SQLite) with two main domains:

1. **Journaling**
   - journal entries
   - photo assets
   - photo tags (`PERSON`, `LOCATION`, `KEYWORD`)
2. **Albums**
   - albums
   - direct album-photo links
   - optional album-entry links
   - album membership

The main public APIs are repository interfaces:
- `JournalingRepository`
- `AlbumRepository`

Repository files:
- `Memoir/app/src/main/java/nostalgia/memoir/data/repository/JournalingRepository.kt`
- `Memoir/app/src/main/java/nostalgia/memoir/data/repository/AlbumRepository.kt`

---

## 2) Quick wiring (how to get instances)

Use the repository factories from app `Context`:

```kotlin
val journalingRepo = JournalingRepositoryFactory.create(context)
val albumRepo = AlbumRepositoryFactory.create(context)
```

Files:
- `Memoir/app/src/main/java/nostalgia/memoir/data/repository/JournalingRepositoryFactory.kt`
- `Memoir/app/src/main/java/nostalgia/memoir/data/repository/AlbumRepositoryFactory.kt`

If you use the domain layer, use use-case factories:

```kotlin
val journaling = JournalingUseCaseFactory.create(context)
val albums = AlbumUseCaseFactory.create(context)
```

Files:
- `Memoir/app/src/main/java/nostalgia/memoir/domain/usecase/JournalingUseCaseFactory.kt`
- `Memoir/app/src/main/java/nostalgia/memoir/domain/usecase/AlbumUseCaseFactory.kt`

---

## 3) Data contracts you pass in/out

### Journaling input models
File: `Memoir/app/src/main/java/nostalgia/memoir/data/model/JournalingModels.kt`

- `CreateJournalEntryInput`
- `UpdateJournalEntryInput`
- `PhotoAssetDraft`
- `TagDraft`

`TagDraft.type` must be one of:
- `PERSON`
- `LOCATION`
- `KEYWORD`

Tag enum file:
- `Memoir/app/src/main/java/nostalgia/memoir/data/local/entities/TagType.kt`

### Album input models
File: `Memoir/app/src/main/java/nostalgia/memoir/data/model/AlbumModels.kt`

- `CreateAlbumInput`
- `AddPhotoToAlbumInput`
- `AddEntryToAlbumInput`
- `UpsertAlbumMemberInput`

---

## 4) Function descriptions (all together)

### 4.1) Journaling function guide

Repository interface:
- `Memoir/app/src/main/java/nostalgia/memoir/data/repository/JournalingRepository.kt`

### `suspend fun createEntryAggregate(input): String`
Creates one journal entry and its linked photos/tags in one transaction.

Use when:
- user saves a new entry.

Returns:
- created `entryId`.

### `suspend fun updateEntryAggregate(input): Boolean`
Updates entry text/title and replaces entry photo links + photo-tag associations from current payload.

Use when:
- user edits an entry.

Returns:
- `true` if entry existed and updated.

### `suspend fun getEntryAggregate(entryId): JournalEntryAggregate?`
Loads one entry with ordered photos and deduped tags derived from linked photos.

### `fun observeAllEntries(): Flow<List<JournalEntryEntity>>`
Stream all entries ordered by recency.

### `fun observeEntriesByDateRange(startEpochDay, endEpochDay)`
Stream entries for date-range filtering.

### `fun searchEntries(query)`
Searches reflection/title and tag values (via photo-tag relationships).

### `fun observePhotosByTag(type, value)`
Returns photos that match a specific tag type/value.

This is the primary API for “filter on a tag”.

### `suspend fun getLinkedPhotoUrisForEpochDay(epochDay)`
Returns photo URIs linked to entries for a specific day.

Useful for “today view” and “already annotated” checks.

### 4.2) Album function guide

Repository interface:
- `Memoir/app/src/main/java/nostalgia/memoir/data/repository/AlbumRepository.kt`

### `suspend fun createAlbum(input): String`
Creates an album and auto-creates owner membership (`OWNER`, `ACTIVE`).

### `suspend fun renameAlbum(albumId, newName): Boolean`
Renames existing album.

### `suspend fun getAlbumAggregate(albumId): AlbumAggregate?`
Loads album + direct photos + attached entries + members.

### `suspend fun addPhotoToAlbum(input): Boolean`
Adds a direct photo link to album (`album_photo`).

### `suspend fun removePhotoFromAlbum(albumId, photoId): Boolean`
Removes direct photo link from album.

### `suspend fun addEntryToAlbum(input): Boolean`
Attaches a journal entry to an album (`album_entry`).

### `suspend fun removeEntryFromAlbum(albumId, entryId): Boolean`
Removes entry attachment from album.

### `suspend fun upsertAlbumMember(input): Boolean`
Upserts membership role/status.

### `suspend fun removeAlbumMember(albumId, memberId): Boolean`
Removes album member row.

### `observe*` album read APIs
- `observeAlbumsByOwner(ownerUserId)`
- `observeAllAlbums()`
- `observePhotosForAlbum(albumId)`
- `observeEntriesForAlbum(albumId)`
- `observeMembersForAlbum(albumId)`

### 4.3) Additional business context (photos, albums, journals, tags)

Use these function descriptions when thinking in product/business flows rather than table-level terms.

#### Journals

- `createEntryAggregate(input)`
  - Business action: **Save a journal moment**.
  - A “journal moment” includes reflection text, linked photos, and tags attached to those photos.

- `updateEntryAggregate(input)`
  - Business action: **Edit an existing journal moment**.
  - Replaces the current photo and tag link set for that journal.

- `getEntryAggregate(entryId)`
  - Business action: **Open a journal detail page**.
  - Returns the journal plus the photos/tags users expect to see together.

- `observeAllEntries()` / `observeEntriesByDateRange(...)`
  - Business action: **Drive journal feed and date-filtered history views**.

#### Photos + tags

- `observePhotosByTag(type, value)`
  - Business action: **Find photos by person/location/keyword**.
  - This is the main search API for tag-based discovery.

- `getLinkedPhotoUrisForEpochDay(epochDay)`
  - Business action: **Find photos already used in journals on a specific day**.

#### Albums

- `createAlbum(input)`
  - Business action: **Start a curated collection**.

- `addPhotoToAlbum(input)` / `removePhotoFromAlbum(...)`
  - Business action: **Curate the album photo set**.

- `upsertAlbumMember(input)`
  - Business action: **Invite or update collaborators**.
  - Useful for transitions like `INVITED` → `ACTIVE`, or role changes (`VIEWER`/`EDITOR`/`OWNER`).

---

## 5) Practical usage flows (all examples together)

### Flow A: Create journal entry with tags + photos

```kotlin
val entryId = journalingRepo.createEntryAggregate(
    CreateJournalEntryInput(
        entryDateEpochDay = epochDay,
        title = "Morning walk",
        reflectionText = "Felt great",
        photos = listOf(
            PhotoAssetDraft(contentUri = "content://.../1"),
            PhotoAssetDraft(contentUri = "content://.../2"),
        ),
        tags = listOf(
            TagDraft(type = TagType.PERSON, value = "Sam"),
            TagDraft(type = TagType.LOCATION, value = "Waterloo Park"),
        ),
    )
)
```

### Flow B: Filter photos by tag

```kotlin
journalingRepo.observePhotosByTag(TagType.PERSON, "Sam")
```

### Flow C: Create album and add direct photos

```kotlin
val albumId = albumRepo.createAlbum(
    CreateAlbumInput(name = "Vietnam", ownerUserId = userId)
)

albumRepo.addPhotoToAlbum(
    AddPhotoToAlbumInput(albumId = albumId, photoId = somePhotoId)
)
```

### Flow D: Attach existing journal entry to album

```kotlin
albumRepo.addEntryToAlbum(
    AddEntryToAlbumInput(albumId = albumId, entryId = entryId, addedBy = userId)
)
```

### Flow E: Create journal entry with business semantics

```kotlin
val morningEntryId = journalingRepo.createEntryAggregate(
    CreateJournalEntryInput(
        entryDateEpochDay = epochDay,
        title = "Morning Walk",
        reflectionText = "Walked with Sam through Waterloo Park.",
        photos = listOf(
            PhotoAssetDraft(contentUri = "content://media/external/images/1001"),
            PhotoAssetDraft(contentUri = "content://media/external/images/1002"),
        ),
        tags = listOf(
            TagDraft(type = TagType.PERSON, value = "Sam"),
            TagDraft(type = TagType.LOCATION, value = "Waterloo Park"),
            TagDraft(type = TagType.KEYWORD, value = "morning"),
        ),
    )
)
```

### Flow F: Add multiple photos to an album

```kotlin
val albumId = albumRepo.createAlbum(
    CreateAlbumInput(name = "Summer Highlights", ownerUserId = ownerUserId)
)

albumRepo.addPhotoToAlbum(
    AddPhotoToAlbumInput(
        albumId = albumId,
        photoId = firstPhotoId,
        addedBy = ownerUserId,
    )
)

albumRepo.addPhotoToAlbum(
    AddPhotoToAlbumInput(
        albumId = albumId,
        photoId = secondPhotoId,
        addedBy = ownerUserId,
    )
)
```

### Flow G: Invite members to an album

```kotlin
albumRepo.upsertAlbumMember(
    UpsertAlbumMemberInput(
        albumId = albumId,
        memberId = "user-42",
        role = AlbumRole.EDITOR,
        status = AlbumMemberStatus.INVITED,
    )
)
```

Member accepts invite:

```kotlin
albumRepo.upsertAlbumMember(
    UpsertAlbumMemberInput(
        albumId = albumId,
        memberId = "user-42",
        role = AlbumRole.EDITOR,
        status = AlbumMemberStatus.ACTIVE,
    )
)
```

### Flow H: Search photos based on tags

```kotlin
val photosWithSam: kotlinx.coroutines.flow.Flow<List<PhotoAssetEntity>> =
    journalingRepo.observePhotosByTag(TagType.PERSON, "Sam")

val photosAtPark: kotlinx.coroutines.flow.Flow<List<PhotoAssetEntity>> =
    journalingRepo.observePhotosByTag(TagType.LOCATION, "Waterloo Park")

val sunsetPhotos: kotlinx.coroutines.flow.Flow<List<PhotoAssetEntity>> =
    journalingRepo.observePhotosByTag(TagType.KEYWORD, "sunset")
```

### Flow I: Search by date (journals + journal-linked photos)

Journals in a date range:

```kotlin
val monthEntries = journalingRepo.observeEntriesByDateRange(startEpochDay, endEpochDay)
```

Photo URIs linked to journals on one specific day:

```kotlin
val todayLinkedPhotoUris = journalingRepo.getLinkedPhotoUrisForEpochDay(epochDay)
```

Collecting one of these flows in a coroutine:

```kotlin
scope.launch {
    journalingRepo.observeEntriesByDateRange(startEpochDay, endEpochDay)
        .collect { entries ->
            // update business state / UI state
        }
}
```

---

## 6) Important behavior notes

1. **Transactions**
   - Aggregate writes are transaction-protected in repository implementations.

2. **Tag model**
   - Tags are associated to **photos** (`photo_tag`), not entries.
   - Entry-level tags are computed from entry’s linked photos.

3. **Album-photo model**
   - Albums have direct photo links through `album_photo`.
   - Entries can also be attached independently via `album_entry`.

4. **Migrations**
   - DB is versioned and includes migrations up to v4.
   - Migration logic is in `MemoirDatabaseProvider`.

---

## 7) Where to look when debugging

- DB declaration: `Memoir/app/src/main/java/nostalgia/memoir/data/local/MemoirDatabase.kt`
- Migrations: `Memoir/app/src/main/java/nostalgia/memoir/data/local/MemoirDatabaseProvider.kt`
- Journaling implementation: `Memoir/app/src/main/java/nostalgia/memoir/data/repository/RoomJournalingRepository.kt`
- Album implementation: `Memoir/app/src/main/java/nostalgia/memoir/data/repository/RoomAlbumRepository.kt`
- Runtime debug diagnostics: `Memoir/app/src/main/java/nostalgia/memoir/diagnostics/DatabaseSelfTestRunner.kt`
- Unit tests:
  - `Memoir/app/src/test/java/nostalgia/memoir/data/repository/RoomJournalingRepositoryTest.kt`
  - `Memoir/app/src/test/java/nostalgia/memoir/data/repository/RoomAlbumRepositoryTest.kt`

---

## 8) Recommended backend integration rules

- Treat repository interfaces as the stable contract.
- Keep all write operations in repository/use-case layer (not UI).
- Use `Flow`-returning APIs for live lists/screens.
- Validate enum values and IDs at API boundaries.
- Prefer adding new functions to repository interface first, then implementing in Room repository and tests.
