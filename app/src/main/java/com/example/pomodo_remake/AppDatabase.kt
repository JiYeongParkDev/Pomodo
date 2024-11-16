package com.example.pomodo_remake

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// 데이터베이스 인스터스를 관리한다.
@Database(entities = [TimerRecords::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
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
                        insertOrUpdateRecord(TimerRecords("2024-10-02", 45 * 60 * 1000L))
                        insertOrUpdateRecord(TimerRecords("2024-10-03", 120 * 60 * 1000L))
                        insertOrUpdateRecord(TimerRecords("2024-10-07", 30 * 60 * 1000L))
                        insertOrUpdateRecord(TimerRecords("2024-10-08", 90 * 60 * 1000L))
                    }
                }
            }
        }
    }
}