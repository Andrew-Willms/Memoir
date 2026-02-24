package nostalgia.memoir.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import nostalgia.memoir.data.local.entities.JournalEntryEntity

@Dao
interface JournalEntryDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entry: JournalEntryEntity)

    @Update
    suspend fun update(entry: JournalEntryEntity)

    @Query("SELECT * FROM journal_entry WHERE id = :entryId LIMIT 1")
    suspend fun getById(entryId: String): JournalEntryEntity?

    @Query("SELECT * FROM journal_entry WHERE id IN (:entryIds)")
    suspend fun getByIds(entryIds: List<String>): List<JournalEntryEntity>

    @Query(
        """
        SELECT *
        FROM journal_entry
        WHERE entryDateEpochDay BETWEEN :startEpochDay AND :endEpochDay
        ORDER BY entryDateEpochDay DESC, updatedAt DESC
        """,
    )
    fun observeByDateRange(startEpochDay: Long, endEpochDay: Long): Flow<List<JournalEntryEntity>>

    @Query(
        """
        SELECT *
        FROM journal_entry
        ORDER BY updatedAt DESC
        """,
    )
    fun observeAll(): Flow<List<JournalEntryEntity>>

    @Query(
        """
        SELECT DISTINCT je.*
        FROM journal_entry je
        LEFT JOIN entry_photo ep ON ep.entryId = je.id
        LEFT JOIN photo_tag pt ON pt.photoId = ep.photoId
        LEFT JOIN tag t ON t.id = pt.tagId
        WHERE je.reflectionText LIKE '%' || :query || '%'
            OR je.title LIKE '%' || :query || '%'
            OR t.value LIKE '%' || :query || '%'
        ORDER BY je.updatedAt DESC
        """,
    )
    fun search(query: String): Flow<List<JournalEntryEntity>>
}
