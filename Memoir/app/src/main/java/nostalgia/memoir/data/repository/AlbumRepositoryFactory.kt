package nostalgia.memoir.data.repository

import android.content.Context
import nostalgia.memoir.data.local.MemoirDatabaseProvider

object AlbumRepositoryFactory {

    fun create(context: Context): AlbumRepository {
        val database = MemoirDatabaseProvider.getInstance(context)
        return RoomAlbumRepository(database)
    }
}
