package nostalgia.memoir.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "photo_tag",
    primaryKeys = ["photoId", "tagId"],
    foreignKeys = [
        ForeignKey(
            entity = PhotoAssetEntity::class,
            parentColumns = ["id"],
            childColumns = ["photoId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = TagEntity::class,
            parentColumns = ["id"],
            childColumns = ["tagId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["photoId"]),
        Index(value = ["tagId"]),
    ],
)
data class PhotoTagCrossRef(
    val photoId: String,
    val tagId: String,
)
