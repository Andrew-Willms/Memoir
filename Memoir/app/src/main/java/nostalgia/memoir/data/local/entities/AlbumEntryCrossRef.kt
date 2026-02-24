package nostalgia.memoir.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "album_entry",
    primaryKeys = ["albumId", "entryId"],
    foreignKeys = [
        ForeignKey(
            entity = AlbumEntity::class,
            parentColumns = ["id"],
            childColumns = ["albumId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = JournalEntryEntity::class,
            parentColumns = ["id"],
            childColumns = ["entryId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["albumId"]),
        Index(value = ["entryId"]),
        Index(value = ["addedAt"]),
    ],
)
data class AlbumEntryCrossRef(
    val albumId: String,
    val entryId: String,
    val addedAt: Long,
    val addedBy: String?,
)
