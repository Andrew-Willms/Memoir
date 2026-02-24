package nostalgia.memoir.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "album",
    indices = [
        Index(value = ["ownerUserId"]),
        Index(value = ["name"]),
    ],
)
data class AlbumEntity(
    @PrimaryKey val id: String,
    val createdAt: Long,
    val updatedAt: Long,
    val name: String,
    val ownerUserId: String,
    val visibility: AlbumVisibility,
)
