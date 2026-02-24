package nostalgia.memoir.data.startup

import android.content.ContentUris
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import nostalgia.memoir.BuildConfig
import nostalgia.memoir.data.local.MemoirDatabaseProvider
import nostalgia.memoir.data.local.entities.PhotoAssetEntity
import java.util.UUID

class AppStartupInitializer(
    private val context: Context,
) {

    suspend fun run() {
        if (!BuildConfig.ENABLE_STARTUP_MOCK_SEED) return

        val database = MemoirDatabaseProvider.getInstance(context)
        val photoAssetDao = database.photoAssetDao()

        importMockPhotosFromFolder(
            folderName = BuildConfig.STARTUP_MOCK_PHOTO_FOLDER,
            onPhotoFound = { contentUri, takenAt, width, height ->
                val now = System.currentTimeMillis()
                val existing = photoAssetDao.getByContentUri(contentUri)
                if (existing == null) {
                    photoAssetDao.insert(
                        PhotoAssetEntity(
                            id = UUID.randomUUID().toString(),
                            createdAt = now,
                            updatedAt = now,
                            contentUri = contentUri,
                            takenAt = takenAt,
                            width = width,
                            height = height,
                            hash = null,
                        ),
                    )
                } else {
                    photoAssetDao.update(
                        existing.copy(
                            updatedAt = now,
                            takenAt = takenAt ?: existing.takenAt,
                            width = width ?: existing.width,
                            height = height ?: existing.height,
                        ),
                    )
                }
            },
        )
    }

    private suspend fun importMockPhotosFromFolder(
        folderName: String,
        onPhotoFound: suspend (contentUri: String, takenAt: Long?, width: Int?, height: Int?) -> Unit,
    ) = withContext(Dispatchers.IO) {
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DATE_TAKEN,
            MediaStore.Images.Media.WIDTH,
            MediaStore.Images.Media.HEIGHT,
        )

        val selection = "${MediaStore.Images.Media.BUCKET_DISPLAY_NAME} = ?"
        val selectionArgs = arrayOf(folderName)
        val sortOrder = "${MediaStore.Images.Media.DATE_TAKEN} DESC"

        try {
            context.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                sortOrder,
            )?.use { cursor ->
                val idIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val takenAtIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)
                val widthIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH)
                val heightIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idIndex)
                    val contentUri = ContentUris.withAppendedId(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        id,
                    ).toString()

                    val takenAt = if (cursor.isNull(takenAtIndex)) null else cursor.getLong(takenAtIndex)
                    val width = if (cursor.isNull(widthIndex)) null else cursor.getInt(widthIndex)
                    val height = if (cursor.isNull(heightIndex)) null else cursor.getInt(heightIndex)

                    onPhotoFound(contentUri, takenAt, width, height)
                }
            }
        } catch (_: SecurityException) {
            return@withContext
        }
    }

    companion object {
        fun requiredMediaReadPermission(): String =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                android.Manifest.permission.READ_MEDIA_IMAGES
            } else {
                android.Manifest.permission.READ_EXTERNAL_STORAGE
            }
    }
}
