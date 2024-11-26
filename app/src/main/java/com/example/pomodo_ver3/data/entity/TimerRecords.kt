package com.example.pomodo_ver3.data.entity


import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class TimerRecords(
    @PrimaryKey val date: String, //날짜 (yyyy-MM-dd 형식)
    val totalFocusTime: Long  // 총 집중 시간(밀리초)
)