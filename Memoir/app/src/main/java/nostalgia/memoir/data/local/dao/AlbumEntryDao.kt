package nostalgia.memoir.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import nostalgia.memoir.data.local.entities.AlbumEntryCrossRef
import nostalgia.memoir.data.local.entities.JournalEntryEntity

@Dao
interface AlbumEntryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(link: AlbumEntryCrossRef)

    @Query("DELETE FROM album_entry WHERE albumId = :albumId AND entryId = :entryId")
    suspend fun deleteLink(albumId: String, entryId: String)

    @Query("SELECT * FROM album_entry WHERE albumId = :albumId ORDER BY addedAt DESC")
    suspend fun getLinksByAlbumId(albumId: String): List<AlbumEntryCrossRef>

    @Query(
        """
        SELECT je.*
        FROM album_entry ae
        INNER JOIN journal_entry je ON je.id = ae.entryId
        WHERE ae.albumId = :albumId
        ORDER BY ae.addedAt DESC
        """,
    )
    fun observeEntriesForAlbum(albumId: String): Flow<List<JournalEntryEntity>>
}
