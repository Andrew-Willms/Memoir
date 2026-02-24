package nostalgia.memoir.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "journal_entry",
    indices = [
        Index(value = ["entryDateEpochDay"]),
        Index(value = ["updatedAt"]),
    ],
)
data class JournalEntryEntity(
    @PrimaryKey val id: String,
    val createdAt: Long,
    val updatedAt: Long,
    val entryDateEpochDay: Long,
    val title: String?,
    val reflectionText: String,
)
