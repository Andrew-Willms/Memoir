package nostalgia.memoir.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import nostalgia.memoir.data.local.entities.PhotoAssetEntity

@Dao
interface PhotoAssetDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(photoAsset: PhotoAssetEntity)

    @Update
    suspend fun update(photoAsset: PhotoAssetEntity)

    @Query("SELECT * FROM photo_asset WHERE contentUri = :contentUri LIMIT 1")
    suspend fun getByContentUri(contentUri: String): PhotoAssetEntity?

    @Query("SELECT * FROM photo_asset WHERE id = :photoId LIMIT 1")
    suspend fun getById(photoId: String): PhotoAssetEntity?

    @Query("SELECT * FROM photo_asset WHERE id IN (:photoIds)")
    suspend fun getByIds(photoIds: List<String>): List<PhotoAssetEntity>

    @Query(
        """
        SELECT pa.contentUri
        FROM photo_asset pa
        INNER JOIN entry_photo ep ON ep.photoId = pa.id
        INNER JOIN journal_entry je ON je.id = ep.entryId
        WHERE je.entryDateEpochDay = :epochDay
        """,
    )
    suspend fun getLinkedPhotoUrisForEpochDay(epochDay: Long): List<String>
}
