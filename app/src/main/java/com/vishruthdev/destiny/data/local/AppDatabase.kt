package com.vishruthdev.destiny.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [HabitEntity::class, HabitCompletionEntity::class, RevisionTopicEntity::class, RevisionCompletionEntity::class],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun habitDao(): HabitDao
}

object DatabaseProvider {
    @Volatile
    private var instance: AppDatabase? = null
    private val migration1To2 = object : Migration(1, 2) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `revision_topics` (
                    `id` TEXT NOT NULL,
                    `name` TEXT NOT NULL,
                    `createdAtMillis` INTEGER NOT NULL,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent()
            )
            database.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `revision_completions` (
                    `topicId` TEXT NOT NULL,
                    `revisionDay` INTEGER NOT NULL,
                    `completedAtMillis` INTEGER NOT NULL,
                    PRIMARY KEY(`topicId`, `revisionDay`)
                )
                """.trimIndent()
            )
        }
    }
    private val migration2To3 = object : Migration(2, 3) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL(
                "ALTER TABLE revision_topics ADD COLUMN startDateMillis INTEGER NOT NULL DEFAULT 0"
            )
            database.execSQL(
                "ALTER TABLE revision_topics ADD COLUMN revisionHour INTEGER NOT NULL DEFAULT 0"
            )
            database.execSQL(
                "ALTER TABLE revision_topics ADD COLUMN revisionMinute INTEGER NOT NULL DEFAULT 0"
            )
            database.execSQL(
                "UPDATE revision_topics SET startDateMillis = createdAtMillis + 86400000"
            )
        }
    }

    fun get(context: Context): AppDatabase {
        return instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "destiny_db"
            )
                .addMigrations(migration1To2)
                .addMigrations(migration2To3)
                .build()
                .also { instance = it }
        }
    }
}
