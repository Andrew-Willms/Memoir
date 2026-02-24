package nostalgia.memoir.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "photo_asset",
    indices = [
        Index(value = ["contentUri"], unique = true),
        Index(value = ["takenAt"]),
    ],
)
data class PhotoAssetEntity(
    @PrimaryKey val id: String,
    val createdAt: Long,
    val updatedAt: Long,
    val contentUri: String,
    val takenAt: Long?,
    val width: Int?,
    val height: Int?,
    val hash: String?,
)
