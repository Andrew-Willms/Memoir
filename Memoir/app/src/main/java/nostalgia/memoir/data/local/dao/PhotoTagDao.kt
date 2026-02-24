package nostalgia.memoir.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import nostalgia.memoir.data.local.entities.PhotoTagCrossRef

@Dao
interface PhotoTagDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(links: List<PhotoTagCrossRef>)

    @Query("SELECT * FROM photo_tag WHERE photoId IN (:photoIds)")
    suspend fun getByPhotoIds(photoIds: List<String>): List<PhotoTagCrossRef>
}
