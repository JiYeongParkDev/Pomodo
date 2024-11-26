package com.example.pomodo_ver3.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.pomodo_ver3.data.entity.CalendarEmoji

@Dao
interface CalendarEmojiDao {
    // 날짜별 이모지를 추가하거나 업데이트 (중복 시 기존 데이터 대체)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEmojiForDate(calendarEmoji: CalendarEmoji)

    // 특정 날짜의 이모지를 가져오는 메서드
    @Query("SELECT * FROM calendar_emoji WHERE date = :date")
    suspend fun getEmojiForDate(date: String): CalendarEmoji?

    // 모든 이모지를 가져오는 메서드 (옵션)
    @Query("SELECT * FROM calendar_emoji")
    suspend fun getAllEmojis(): List<CalendarEmoji>
}