# Memoir Android Module: Journaling Database Architecture

This module now includes a complete Room-backed journaling data layer in Kotlin, focused on offline-first local transactions.

## What was implemented

### 1) Room schema for journaling
Under `app/src/main/java/nostalgia/memoir/data/local/entities`:

- `JournalEntryEntity` (`journal_entry`)
  - fields: `id`, `createdAt`, `updatedAt`, `entryDateEpochDay`, `title`, `reflectionText`
- `PhotoAssetEntity` (`photo_asset`)
  - fields: `id`, `createdAt`, `updatedAt`, `contentUri` (unique), `takenAt`, `width`, `height`, `hash`
- `EntryPhotoCrossRef` (`entry_photo`)
  - many-to-many join between entries and photos
  - fields: `entryId`, `photoId`, `orderIndex`
- `TagEntity` (`tag`)
  - fields: `id`, `createdAt`, `updatedAt`, `type`, `value`
  - unique index on `(type, value)`
- `EntryTagCrossRef` (`entry_tag`)
  - many-to-many join between entries and tags
  - fields: `entryId`, `tagId`
- `TagType` enum
  - `PERSON`, `PLACE`, `KEYWORD`, `ALBUM`, `EVENT`

### 2) Type converters
Under `app/src/main/java/nostalgia/memoir/data/local/converters`:

- `RoomTypeConverters` for storing/loading `TagType`.

### 3) DAO layer
Under `app/src/main/java/nostalgia/memoir/data/local/dao`:

- `JournalEntryDao`
  - insert, update, get by id
  - observe by date range
  - keyword search across reflection/title/tag values
- `PhotoAssetDao`
  - insert, update, lookup by URI, batch lookup by IDs
  - query linked photo URIs for a given day
- `EntryPhotoDao`
  - upsert links, delete links by entry, ordered links by entry
- `TagDao`
  - insert, update, lookup by `(type, value)`, batch lookup by IDs
- `EntryTagDao`
  - upsert links, delete links by entry, lookup links by entry

### 4) Database + provider
Under `app/src/main/java/nostalgia/memoir/data/local`:

- `MemoirDatabase`
  - Room database with all journaling entities
  - `exportSchema = true`
- `MemoirDatabaseProvider`
  - singleton database builder for app-wide access

### 5) Repository and transactional write logic
Under `app/src/main/java/nostalgia/memoir/data/repository`:

- `JournalingRepository` interface
- `RoomJournalingRepository` implementation
  - `createEntryAggregate(...)`
    - single DB transaction
    - inserts `JournalEntry`
    - upserts photos by `contentUri`
    - writes ordered `EntryPhoto` links
    - upserts tags by `(type, value)`
    - writes `EntryTag` links
  - `updateEntryAggregate(...)`
    - single DB transaction
    - updates entry text/title/timestamp
    - replaces photo links and tag links from current input
  - `getEntryAggregate(...)`
    - returns entry + ordered photos + tags
  - `observeEntriesByDateRange(...)`, `searchEntries(...)`, `getLinkedPhotoUrisForEpochDay(...)`
- `JournalingRepositoryFactory`
  - helper to build repository from Android `Context`

### 6) Use-case layer
Under `app/src/main/java/nostalgia/memoir/domain/usecase`:

- `CreateEntryUseCase`
- `UpdateEntryUseCase`
- `GetEntryAggregateUseCase`
- `ObserveEntriesByDateRangeUseCase`
- `SearchEntriesUseCase`
- `GetLinkedPhotoUrisForEpochDayUseCase`
- `JournalingUseCases` bundle
- `JournalingUseCaseFactory`

### 7) Build configuration updates
- Added Room dependencies (`room-runtime`, `room-ktx`, `room-compiler`)
- Added Kotlin Android + KAPT plugins
- Enabled Room schema export args in `kapt { arguments { ... } }`
- Added `app/schemas/` directory for schema JSON output

## Notes and best-practice decisions

- Writes are done in `withTransaction` for offline-first consistency.
- Schema is normalized with explicit join tables for many-to-many relations.
- Tags are upserted by semantic uniqueness `(type, value)`.
- Photos are upserted by stable `contentUri`.
- Entry-photo ordering is preserved via `orderIndex`.

## What is intentionally not included yet

- UI screens / ViewModels / Compose wiring
- MediaStore querying implementation
- Network sync / outbox
- Album-member sharing tables (`Album`, `AlbumMember`) for collaboration workflows

This keeps the scope aligned with journaling database architecture first.

## How to test this backend

## Startup mock-photo import (emulator)

A startup initializer now runs from `MemoirApplication` and can import photo metadata into `photo_asset` from a MediaStore folder on emulator startup.

Code locations:

- `app/src/main/java/nostalgia/memoir/MemoirApplication.kt`
- `app/src/main/java/nostalgia/memoir/data/startup/AppStartupInitializer.kt`

Behavior:

- enabled for `debug` builds (`ENABLE_STARTUP_MOCK_SEED=true`)
- disabled for `release` builds
- reads MediaStore images where `BUCKET_DISPLAY_NAME == "MemoirMock"`
- upserts into `photo_asset` by `contentUri` (idempotent across launches)

### Set up mock photos in emulator

1. Put sample images in a local folder on your machine (e.g. `./mock_photos`).
2. Push them into emulator Pictures folder:

```bash
adb push ./mock_photos /sdcard/Pictures/MemoirMock
```

3. Launch app in debug.
4. Grant photo/media permission when prompted (`READ_MEDIA_IMAGES` on Android 13+, `READ_EXTERNAL_STORAGE` on older versions).

If permission is not granted yet, startup import safely skips and retries on next app launch.

### Do databases auto-populate when app launches?

No. By default, Room creates an empty database file on first access.

Data appears only when your code executes writes (for example, calling `CreateEntryUseCase` / `createEntryAggregate(...)`).

This project currently provides the data layer and use-cases, but no startup seed routine has been added.

### Automated tests (recommended first)

The `test` folder now contains Room integration tests:

- `app/src/test/java/nostalgia/memoir/data/repository/RoomJournalingRepositoryTest.kt`

These tests use an in-memory Room DB (Robolectric) and verify:

- creating an entry aggregate (entry + photos + tags)
- updating an entry aggregate and replacing join links
- searching entries by reflection text and tag values
- date-range entry querying
- linked photo URI lookup for a specific day

Run tests:

```bash
./gradlew :app:testDebugUnitTest
```

### Build verification

```bash
./gradlew :app:compileDebugKotlin
./gradlew :app:assembleDebug
```

### Manual emulator verification flow

Once you wire UI/ViewModel calls into use-cases, test this sequence:

1. Launch app on emulator (mock photo import runs in debug).
2. Trigger a save path that calls `CreateEntryUseCase`.
3. Restart app and load entries (ensures persistence).
4. Edit one entry through `UpdateEntryUseCase`.
5. Run a search and date filter through your query use-cases.

Expected result:

- data persists between launches (until app data is cleared)
- entries/photos/tags relationships remain consistent
- queries return only relevant data by date/search term

### Persisting data when app opens (how this works in practice)

- Room stores data in on-device SQLite (`memoir.db`), so journals persist across app restarts automatically.
- Startup import only upserts `photo_asset`; it does not overwrite journal entries.
- To render persisted journals immediately on open, use the new `observeAllEntries` flow (`ObserveAllEntriesUseCase`).
- Albums persistence will work the same way once Album tables are added (currently journaling schema is implemented; album tables are not yet in this module).

### Optional: inspect the database directly on emulator

Use Android Studio App Inspection:

- View -> Tool Windows -> App Inspection -> Database Inspector
- Select process, open `memoir.db`, inspect `journal_entry`, `photo_asset`, `entry_photo`, `tag`, `entry_tag`

This is the fastest way to verify join-table correctness while developing.
