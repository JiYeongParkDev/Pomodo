package com.example.pomodo_ver3.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "calendar_emoji")
data class CalendarEmoji(
    @PrimaryKey val date: String,  // 날짜를 저장할 필드 (예: "2024-11-14" 형식)
    val emoji: String              // 선택한 이모지를 저장할 필드
)