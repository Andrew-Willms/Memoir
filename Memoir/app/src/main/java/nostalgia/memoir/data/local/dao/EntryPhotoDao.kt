package nostalgia.memoir.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import nostalgia.memoir.data.local.entities.EntryPhotoCrossRef

@Dao
interface EntryPhotoDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(links: List<EntryPhotoCrossRef>)

    @Query("DELETE FROM entry_photo WHERE entryId = :entryId")
    suspend fun deleteByEntryId(entryId: String)

    @Query("SELECT * FROM entry_photo WHERE entryId = :entryId ORDER BY orderIndex ASC")
    suspend fun getByEntryIdOrdered(entryId: String): List<EntryPhotoCrossRef>
}
