package nostalgia.memoir.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import nostalgia.memoir.data.local.entities.TagEntity
import nostalgia.memoir.data.local.entities.TagType

@Dao
interface TagDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(tag: TagEntity)

    @Update
    suspend fun update(tag: TagEntity)

    @Query("SELECT * FROM tag WHERE type = :type AND value = :value LIMIT 1")
    suspend fun getByTypeAndValue(type: TagType, value: String): TagEntity?

    @Query("SELECT * FROM tag WHERE id IN (:tagIds)")
    suspend fun getByIds(tagIds: List<String>): List<TagEntity>
}
