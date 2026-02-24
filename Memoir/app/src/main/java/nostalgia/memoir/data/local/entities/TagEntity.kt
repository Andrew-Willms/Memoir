package nostalgia.memoir.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "tag",
    indices = [
        Index(value = ["type", "value"], unique = true),
    ],
)
data class TagEntity(
    @PrimaryKey val id: String,
    val createdAt: Long,
    val updatedAt: Long,
    val type: TagType,
    val value: String,
)
