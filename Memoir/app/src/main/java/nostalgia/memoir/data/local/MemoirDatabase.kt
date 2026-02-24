package nostalgia.memoir.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import nostalgia.memoir.data.local.converters.RoomTypeConverters
import nostalgia.memoir.data.local.dao.EntryPhotoDao
import nostalgia.memoir.data.local.dao.EntryTagDao
import nostalgia.memoir.data.local.dao.JournalEntryDao
import nostalgia.memoir.data.local.dao.PhotoAssetDao
import nostalgia.memoir.data.local.dao.TagDao
import nostalgia.memoir.data.local.entities.EntryPhotoCrossRef
import nostalgia.memoir.data.local.entities.EntryTagCrossRef
import nostalgia.memoir.data.local.entities.JournalEntryEntity
import nostalgia.memoir.data.local.entities.PhotoAssetEntity
import nostalgia.memoir.data.local.entities.TagEntity

@Database(
    entities = [
        JournalEntryEntity::class,
        PhotoAssetEntity::class,
        EntryPhotoCrossRef::class,
        TagEntity::class,
        EntryTagCrossRef::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(RoomTypeConverters::class)
abstract class MemoirDatabase : RoomDatabase() {
    abstract fun journalEntryDao(): JournalEntryDao
    abstract fun photoAssetDao(): PhotoAssetDao
    abstract fun entryPhotoDao(): EntryPhotoDao
    abstract fun tagDao(): TagDao
    abstract fun entryTagDao(): EntryTagDao

    companion object {
        const val DATABASE_NAME: String = "memoir.db"
    }
}
