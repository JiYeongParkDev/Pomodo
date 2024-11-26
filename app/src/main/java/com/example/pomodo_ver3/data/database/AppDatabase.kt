package com.example.pomodo_ver3.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.pomodo_ver3.data.dao.CalendarEmojiDao
import com.example.pomodo_ver3.data.dao.TaskDao
import com.example.pomodo_ver3.data.dao.TimerRecordsDao
import com.example.pomodo_ver3.data.entity.CalendarEmoji
import com.example.pomodo_ver3.data.entity.Task
import com.example.pomodo_ver3.data.entity.TimerRecords
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [Task::class, CalendarEmoji::class, TimerRecords::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun taskDao(): TaskDao
    abstract fun calendarEmojiDao(): CalendarEmojiDao
    abstract fun timerRecordsDao(): TimerRecordsDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database"
                )
                    .addCallback(DatabaseCallback(context)) // 초기 데이터 삽입용 Callback
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(private val context: Context) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                val database = getDatabase(context)
                CoroutineScope(Dispatchers.IO).launch {
                    insertInitialData(database)
                }
            }

            // 초기 데이터를 삽입하는 함수
            private suspend fun insertInitialData(database: AppDatabase) {
                // TimerRecords 초기 데이터 삽입
                database.timerRecordsDao().apply {
                    insertOrUpdateRecord(TimerRecords(date = "2024-10-02", totalFocusTime = 45 * 60 * 1000L))
                    insertOrUpdateRecord(TimerRecords(date = "2024-10-03", totalFocusTime = 120 * 60 * 1000L))
                    insertOrUpdateRecord(TimerRecords(date = "2024-10-07", totalFocusTime = 30 * 60 * 1000L))
                    insertOrUpdateRecord(TimerRecords(date = "2024-10-08", totalFocusTime = 90 * 60 * 1000L))
                }

                // CalendarEmoji 초기 데이터 삽입
                database.calendarEmojiDao().apply {
                    insertEmojiForDate(CalendarEmoji(date = "2024-10-02", emoji = "😊"))
                    insertEmojiForDate(CalendarEmoji(date = "2024-10-03", emoji = "😍"))
                }

                // Task 초기 데이터 삽입
                database.taskDao().apply {
                    insertTask(Task(date = "2024-10-02", title = "Sample Task 1", status = 0))
                    insertTask(Task(date = "2024-10-03", title = "Sample Task 2", status = 1))
                }
            }
        }
    }
}