package com.example.pomodo_remake

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.pomodo_remake.CalendarEmoji

@Dao
interface CalendarEmojiDao {

    // 날짜별 이모지를 추가하거나 업데이트하는 메서드
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEmojiForDate(calendarEmoji: CalendarEmoji)

    // 특정 날짜의 이모지를 가져오는 메서드
    @Query("SELECT * FROM calendar_emoji WHERE date = :date")
    suspend fun getEmojiForDate(date: String): CalendarEmoji?
}