package nostalgia.memoir.data.repository

import android.content.Context
import nostalgia.memoir.data.local.MemoirDatabaseProvider

object JournalingRepositoryFactory {

    fun create(context: Context): JournalingRepository {
        val database = MemoirDatabaseProvider.getInstance(context)
        return RoomJournalingRepository(database)
    }
}
