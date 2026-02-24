package nostalgia.memoir.data.local

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

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
                .addMigrations(MIGRATION_1_2)
                .build()
                .also { created -> instance = created }
        }
    }

    private val MIGRATION_1_2: Migration = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `album` (
                    `id` TEXT NOT NULL,
                    `createdAt` INTEGER NOT NULL,
                    `updatedAt` INTEGER NOT NULL,
                    `name` TEXT NOT NULL,
                    `ownerUserId` TEXT NOT NULL,
                    `visibility` TEXT NOT NULL,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent(),
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_album_ownerUserId` ON `album` (`ownerUserId`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_album_name` ON `album` (`name`)")

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `album_entry` (
                    `albumId` TEXT NOT NULL,
                    `entryId` TEXT NOT NULL,
                    `addedAt` INTEGER NOT NULL,
                    `addedBy` TEXT,
                    PRIMARY KEY(`albumId`, `entryId`),
                    FOREIGN KEY(`albumId`) REFERENCES `album`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                    FOREIGN KEY(`entryId`) REFERENCES `journal_entry`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent(),
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_album_entry_albumId` ON `album_entry` (`albumId`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_album_entry_entryId` ON `album_entry` (`entryId`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_album_entry_addedAt` ON `album_entry` (`addedAt`)")

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `album_member` (
                    `albumId` TEXT NOT NULL,
                    `memberId` TEXT NOT NULL,
                    `role` TEXT NOT NULL,
                    `status` TEXT NOT NULL,
                    `addedAt` INTEGER NOT NULL,
                    PRIMARY KEY(`albumId`, `memberId`),
                    FOREIGN KEY(`albumId`) REFERENCES `album`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent(),
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_album_member_albumId` ON `album_member` (`albumId`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_album_member_memberId` ON `album_member` (`memberId`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_album_member_status` ON `album_member` (`status`)")
        }
    }
}
