package nostalgia.memoir.data.local.converters

import androidx.room.TypeConverter
import nostalgia.memoir.data.local.entities.TagType

class RoomTypeConverters {

    @TypeConverter
    fun fromTagType(value: TagType): String = value.name

    @TypeConverter
    fun toTagType(value: String): TagType = TagType.valueOf(value)
}
