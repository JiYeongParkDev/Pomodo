package com.example.pomodo_ver3

import android.app.Application
import androidx.room.Room
import com.example.pomodo_ver3.data.database.AppDatabase

class MyApplication : Application() {
    companion object {
        lateinit var database: AppDatabase
            private set
    }

    override fun onCreate() {
        super.onCreate()
        // Room 데이터베이스 초기화
        database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "app_database"
        ).build()
    }
}