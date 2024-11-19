package com.example.pomodo_remake

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    var date: String,  // 할 일의 날짜 (예: "2024-11-05")
    var title: String,  // 할 일의 제목
    var status: Int     // 상태 표현 (0: 미정, 1: 상태1, 2: 상태2, 3: 상태3)
)
