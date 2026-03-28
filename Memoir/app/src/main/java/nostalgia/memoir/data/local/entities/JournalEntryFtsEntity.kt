package nostalgia.memoir.data.local.entities

import androidx.room.Entity
import androidx.room.Fts4

@Fts4(tokenizer = "unicode61")
@Entity(tableName = "journal_entry_fts")
data class JournalEntryFtsEntity(
    val entryId: String,
    val title: String?,
    val reflectionText: String,
)
