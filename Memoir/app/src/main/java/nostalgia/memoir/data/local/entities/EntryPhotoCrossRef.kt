package nostalgia.memoir.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "entry_photo",
    primaryKeys = ["entryId", "photoId"],
    foreignKeys = [
        ForeignKey(
            entity = JournalEntryEntity::class,
            parentColumns = ["id"],
            childColumns = ["entryId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = PhotoAssetEntity::class,
            parentColumns = ["id"],
            childColumns = ["photoId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["entryId"]),
        Index(value = ["photoId"]),
        Index(value = ["orderIndex"]),
    ],
)
data class EntryPhotoCrossRef(
    val entryId: String,
    val photoId: String,
    val orderIndex: Int,
)
