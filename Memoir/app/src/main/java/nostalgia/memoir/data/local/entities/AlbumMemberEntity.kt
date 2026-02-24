package nostalgia.memoir.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "album_member",
    primaryKeys = ["albumId", "memberId"],
    foreignKeys = [
        ForeignKey(
            entity = AlbumEntity::class,
            parentColumns = ["id"],
            childColumns = ["albumId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["albumId"]),
        Index(value = ["memberId"]),
        Index(value = ["status"]),
    ],
)
data class AlbumMemberEntity(
    val albumId: String,
    val memberId: String,
    val role: AlbumRole,
    val status: AlbumMemberStatus,
    val addedAt: Long,
)
