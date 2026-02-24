package nostalgia.memoir.data.local.converters

import androidx.room.TypeConverter
import nostalgia.memoir.data.local.entities.AlbumMemberStatus
import nostalgia.memoir.data.local.entities.AlbumRole
import nostalgia.memoir.data.local.entities.AlbumVisibility
import nostalgia.memoir.data.local.entities.TagType

class RoomTypeConverters {

    @TypeConverter
    fun fromTagType(value: TagType): String = value.name

    @TypeConverter
    fun toTagType(value: String): TagType = TagType.valueOf(value)

    @TypeConverter
    fun fromAlbumVisibility(value: AlbumVisibility): String = value.name

    @TypeConverter
    fun toAlbumVisibility(value: String): AlbumVisibility = AlbumVisibility.valueOf(value)

    @TypeConverter
    fun fromAlbumRole(value: AlbumRole): String = value.name

    @TypeConverter
    fun toAlbumRole(value: String): AlbumRole = AlbumRole.valueOf(value)

    @TypeConverter
    fun fromAlbumMemberStatus(value: AlbumMemberStatus): String = value.name

    @TypeConverter
    fun toAlbumMemberStatus(value: String): AlbumMemberStatus = AlbumMemberStatus.valueOf(value)
}
