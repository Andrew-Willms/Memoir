package nostalgia.memoir.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "album_photo",
    primaryKeys = ["albumId", "photoId"],
    foreignKeys = [
        ForeignKey(
            entity = AlbumEntity::class,
            parentColumns = ["id"],
            childColumns = ["albumId"],
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
        Index(value = ["albumId"]),
        Index(value = ["photoId"], unique = true),
        Index(value = ["orderIndex"]),
        Index(value = ["addedAt"]),
    ],
)
data class AlbumPhotoCrossRef(
    val albumId: String,
    val photoId: String,
    val orderIndex: Int,
    val addedAt: Long,
    val addedBy: String?,
)
