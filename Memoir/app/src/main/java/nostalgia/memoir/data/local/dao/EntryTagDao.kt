package nostalgia.memoir.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import nostalgia.memoir.data.local.entities.EntryTagCrossRef

@Dao
interface EntryTagDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(links: List<EntryTagCrossRef>)

    @Query("DELETE FROM entry_tag WHERE entryId = :entryId")
    suspend fun deleteByEntryId(entryId: String)

    @Query("SELECT * FROM entry_tag WHERE entryId = :entryId")
    suspend fun getByEntryId(entryId: String): List<EntryTagCrossRef>
}
