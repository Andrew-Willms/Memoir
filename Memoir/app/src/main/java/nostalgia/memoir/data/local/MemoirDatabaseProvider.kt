package nostalgia.memoir.data.local

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
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
                .addMigrations(MIGRATION_2_3)
                .addMigrations(MIGRATION_3_4)
                .addMigrations(MIGRATION_4_5)
                .addMigrations(MIGRATION_5_6)
                .addCallback(JOURNAL_ENTRY_FTS_CALLBACK)
                .build()
                .also { created -> instance = created }
        }
    }

    internal fun resetForTests() {
        instance?.close()
        instance = null
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

    private val MIGRATION_2_3: Migration = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `album_photo` (
                    `albumId` TEXT NOT NULL,
                    `photoId` TEXT NOT NULL,
                    `orderIndex` INTEGER NOT NULL,
                    `addedAt` INTEGER NOT NULL,
                    `addedBy` TEXT,
                    PRIMARY KEY(`albumId`, `photoId`),
                    FOREIGN KEY(`albumId`) REFERENCES `album`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                    FOREIGN KEY(`photoId`) REFERENCES `photo_asset`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent(),
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_album_photo_albumId` ON `album_photo` (`albumId`)")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_album_photo_photoId` ON `album_photo` (`photoId`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_album_photo_orderIndex` ON `album_photo` (`orderIndex`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_album_photo_addedAt` ON `album_photo` (`addedAt`)")
        }
    }

    private val MIGRATION_3_4: Migration = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `photo_tag` (
                    `photoId` TEXT NOT NULL,
                    `tagId` TEXT NOT NULL,
                    PRIMARY KEY(`photoId`, `tagId`),
                    FOREIGN KEY(`photoId`) REFERENCES `photo_asset`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                    FOREIGN KEY(`tagId`) REFERENCES `tag`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent(),
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_photo_tag_photoId` ON `photo_tag` (`photoId`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_photo_tag_tagId` ON `photo_tag` (`tagId`)")

            db.execSQL("UPDATE tag SET type = 'LOCATION' WHERE type = 'PLACE'")
            db.execSQL("DELETE FROM tag WHERE type NOT IN ('PERSON', 'LOCATION', 'KEYWORD')")

            db.execSQL(
                """
                INSERT OR IGNORE INTO photo_tag(photoId, tagId)
                SELECT ep.photoId, et.tagId
                FROM entry_tag et
                INNER JOIN entry_photo ep ON ep.entryId = et.entryId
                INNER JOIN tag t ON t.id = et.tagId
                """.trimIndent(),
            )
        }
    }

    private val MIGRATION_4_5: Migration = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("DROP INDEX IF EXISTS `index_album_photo_photoId`")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_album_photo_photoId` ON `album_photo` (`photoId`)")
        }
    }

    private val MIGRATION_5_6: Migration = object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE VIRTUAL TABLE IF NOT EXISTS `journal_entry_fts`
                USING FTS4(`entryId`, `title`, `reflectionText`, tokenize=unicode61)
                """.trimIndent(),
            )
            installJournalEntryFtsTriggers(db)
            db.execSQL(
                """
                INSERT INTO `journal_entry_fts`(`entryId`, `title`, `reflectionText`)
                SELECT `id`, `title`, `reflectionText`
                FROM `journal_entry`
                """.trimIndent(),
            )
        }
    }

    private val JOURNAL_ENTRY_FTS_CALLBACK = object : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            installJournalEntryFtsTriggers(db)
        }

        override fun onOpen(db: SupportSQLiteDatabase) {
            installJournalEntryFtsTriggers(db)
        }
    }

    private fun installJournalEntryFtsTriggers(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TRIGGER IF NOT EXISTS `journal_entry_fts_insert`
            AFTER INSERT ON `journal_entry`
            BEGIN
                INSERT INTO `journal_entry_fts`(`entryId`, `title`, `reflectionText`)
                VALUES (new.`id`, new.`title`, new.`reflectionText`);
            END
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE TRIGGER IF NOT EXISTS `journal_entry_fts_update`
            AFTER UPDATE ON `journal_entry`
            BEGIN
                DELETE FROM `journal_entry_fts` WHERE `entryId` = old.`id`;
                INSERT INTO `journal_entry_fts`(`entryId`, `title`, `reflectionText`)
                VALUES (new.`id`, new.`title`, new.`reflectionText`);
            END
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE TRIGGER IF NOT EXISTS `journal_entry_fts_delete`
            AFTER DELETE ON `journal_entry`
            BEGIN
                DELETE FROM `journal_entry_fts` WHERE `entryId` = old.`id`;
            END
            """.trimIndent(),
        )
    }
}
