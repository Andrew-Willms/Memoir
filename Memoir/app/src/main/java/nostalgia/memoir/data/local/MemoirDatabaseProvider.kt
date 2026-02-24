package nostalgia.memoir.data.local

import android.content.Context
import androidx.room.Room

object MemoirDatabaseProvider {

    @Volatile
    private var instance: MemoirDatabase? = null

    fun getInstance(context: Context): MemoirDatabase {
        return instance ?: synchronized(this) {
            instance ?: Room
                .databaseBuilder(
                    context.applicationContext,
                    MemoirDatabase::class.java,
                    MemoirDatabase.DATABASE_NAME,
                )
                .build()
                .also { created -> instance = created }
        }
    }
}
