package com.example.pomodo_ver3.data.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.pomodo_ver3.data.entity.TimerRecords

@Dao
interface TimerRecordsDao {
    // 데이터를 삽입하거나 동일한 날짜가 있으면 업데이트
    //OnConflictStrategy.REPLACE를 통해서 중복키가 있을 경우 기존 데이터를 덮어쓴다.
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateRecord(record: TimerRecords)

    // 특정 날짜에 해당하는 데이터를 가져온다.
    @Query("SELECT * FROM TimerRecords WHERE date = :date")
    suspend fun getRecordByDate(date: String): TimerRecords?

    // 모든 데이터를 가져와서 LiveData로 반환한다.
    //UI에서 자동으로 변경 사항을 반영할 수 있다.
    @Query("SELECT * FROM TimerRecords")
    fun getAllRecords(): LiveData<List<TimerRecords>>

    //특정 날짜 레코드 삭제
    @Query("DELETE FROM TimerRecords WHERE date = :targetDate")
    suspend fun deleteRecordByDate(targetDate: String)

    //특정 월의 데이터만 가져오는 함수(기존 데이터를 모두 가져오고 싶지 않으면 사용)
    @Query("SELECT * FROM TimerRecords WHERE date BETWEEN :startDate AND :endDate")
    suspend fun getRecordsForDateRange(startDate: String, endDate: String): List<TimerRecords>





}