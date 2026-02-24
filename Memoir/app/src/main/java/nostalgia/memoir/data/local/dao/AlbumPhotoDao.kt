package nostalgia.memoir.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import nostalgia.memoir.data.local.entities.AlbumPhotoCrossRef
import nostalgia.memoir.data.local.entities.PhotoAssetEntity

@Dao
interface AlbumPhotoDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(link: AlbumPhotoCrossRef)

    @Query("DELETE FROM album_photo WHERE albumId = :albumId AND photoId = :photoId")
    suspend fun deleteLink(albumId: String, photoId: String)

    @Query("SELECT * FROM album_photo WHERE albumId = :albumId ORDER BY orderIndex ASC")
    suspend fun getLinksByAlbumId(albumId: String): List<AlbumPhotoCrossRef>

    @Query("SELECT COALESCE(MAX(orderIndex), -1) + 1 FROM album_photo WHERE albumId = :albumId")
    suspend fun nextOrderIndex(albumId: String): Int

    @Query(
        """
        SELECT pa.*
        FROM album_photo ap
        INNER JOIN photo_asset pa ON pa.id = ap.photoId
        WHERE ap.albumId = :albumId
        ORDER BY ap.orderIndex ASC
        """,
    )
    fun observePhotosForAlbum(albumId: String): Flow<List<PhotoAssetEntity>>
}
