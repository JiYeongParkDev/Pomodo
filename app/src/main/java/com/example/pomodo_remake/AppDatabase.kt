package com.example.pomodo_remake

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// 데이터베이스 인스터스를 관리한다.
@Database(entities = [TimerRecords::class, Task::class, CalendarEmoji::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun timerRecordsDao(): TimerRecordsDao
    abstract fun taskDao(): TaskDao
    abstract fun calendarEmojiDao(): CalendarEmojiDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // 마이그레이션 정의
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                Log.d("RoomMigration", "MIGRATION_1_2 applied")
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
                    "app_database"
                )
                    .addMigrations(MIGRATION_1_2) // 마이그레이션 추가
                    .addCallback(DatabaseCallback(context))  // 총 집중 시간에 샘플 데이터 넣어놓은 것
                    .build()
                INSTANCE = instance
                instance
            }
        }


        private class DatabaseCallback(private val context: Context) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                // CoroutineScope를 사용하여 총 집중 시간 데이터를 삽입
                val database = getDatabase(context)
                CoroutineScope(Dispatchers.IO).launch {
                    database.timerRecordsDao().apply {
                        insertOrUpdateRecord(TimerRecords("2024-10-07", 15 * 60 * 1000L)) // 15분 (1초 ~ 30분 색상: #F0F8FF)
                        insertOrUpdateRecord(TimerRecords("2024-10-08", 45 * 60 * 1000L)) // 45분 (30분 1초 ~ 1시간 색상: #CBEAF9)
                        insertOrUpdateRecord(TimerRecords("2024-10-09", 90 * 60 * 1000L)) // 1시간 30분 (1시간 1초 ~ 2시간 색상: #B7E1F9)
                        insertOrUpdateRecord(TimerRecords("2024-10-10", 3 * 60 * 60 * 1000L)) // 3시간 (2시간 1초 ~ 4시간 색상: #A2D6F8)
                        insertOrUpdateRecord(TimerRecords("2024-10-11", 5 * 60 * 60 * 1000L)) // 5시간 (4시간 1초 ~ 6시간 색상: #8DCBF7)
                        insertOrUpdateRecord(TimerRecords("2024-10-21", 7 * 60 * 60 * 1000L)) // 7시간 (6시간 1초 ~ 8시간 색상: #79C0F7)
                        insertOrUpdateRecord(TimerRecords("2024-10-22", 10 * 60 * 60 * 1000L)) // 10시간 (8시간 1초 ~ 20시간 색상: #64B5F6)

                    }
                }
            }
        }
    }
}