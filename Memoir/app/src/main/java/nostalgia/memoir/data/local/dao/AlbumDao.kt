package nostalgia.memoir.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import nostalgia.memoir.data.local.entities.AlbumEntity

@Dao
interface AlbumDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(album: AlbumEntity)

    @Update
    suspend fun update(album: AlbumEntity)

    @Query("SELECT * FROM album WHERE id = :albumId LIMIT 1")
    suspend fun getById(albumId: String): AlbumEntity?

    @Query(
        """
        SELECT *
        FROM album
        WHERE ownerUserId = :ownerUserId
        ORDER BY updatedAt DESC
        """,
    )
    fun observeByOwner(ownerUserId: String): Flow<List<AlbumEntity>>

    @Query(
        """
        SELECT *
        FROM album
        ORDER BY updatedAt DESC
        """,
    )
    fun observeAll(): Flow<List<AlbumEntity>>
}
