package com.example.pomodo_ver3.data.database

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.pomodo_ver3.data.dao.TaskDao
import com.example.pomodo_ver3.data.dao.CalendarEmojiDao
import com.example.pomodo_ver3.data.entity.Task
import com.example.pomodo_ver3.data.entity.CalendarEmoji

@Database(entities = [Task::class, CalendarEmoji::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun calendarEmojiDao(): CalendarEmojiDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // 1에서 2로의 마이그레이션 정의
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                Log.d("RoomMigration", "MIGRATION_1_2 applied") // 디버그 로그 추가
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `calendar_emoji` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `date` TEXT NOT NULL,
                        `emoji` TEXT NOT NULL
                    )
                    """
                )
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "task_database"
                )
                    .addMigrations(MIGRATION_1_2) // 마이그레이션 추가
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}