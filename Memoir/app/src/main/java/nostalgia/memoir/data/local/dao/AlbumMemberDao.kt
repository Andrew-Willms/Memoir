package nostalgia.memoir.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import nostalgia.memoir.data.local.entities.AlbumMemberEntity

@Dao
interface AlbumMemberDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(member: AlbumMemberEntity)

    @Query("DELETE FROM album_member WHERE albumId = :albumId AND memberId = :memberId")
    suspend fun deleteMember(albumId: String, memberId: String)

    @Query("SELECT * FROM album_member WHERE albumId = :albumId ORDER BY addedAt ASC")
    fun observeMembers(albumId: String): Flow<List<AlbumMemberEntity>>

    @Query("SELECT * FROM album_member WHERE albumId = :albumId AND memberId = :memberId LIMIT 1")
    suspend fun getMember(albumId: String, memberId: String): AlbumMemberEntity?
}
