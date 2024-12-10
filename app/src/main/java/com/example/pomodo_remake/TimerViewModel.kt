package com.example.pomodo_remake

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import java.time.YearMonth
import java.util.Locale

//타이머 뷰 모델
//UI와 데이터베이스를 연결하는 중간다리 역할
// LiveData를 사용해 데이터가 변경될 때 UI에 자동으로 반영되도록 설정한다.
class TimerViewModel(application: Application) : AndroidViewModel(application) {
    private val timerRecordsDao = AppDatabase.getDatabase(application).timerRecordsDao()

    val allRecords: LiveData<List<TimerRecords>> = timerRecordsDao.getAllRecords()

    // 내부적으로 관리되는 MutableLiveData
    private val _focusTimes = MutableLiveData<Map<String, Int>>()
    // 외부에서 읽기 전용으로 접근할 수 있는 LiveData
    val focusTimes: LiveData<Map<String, Int>> get() = _focusTimes

    //삽인 또는 업데이트
    fun insertOrUpdateRecord(record: TimerRecords) {
        viewModelScope.launch {
            timerRecordsDao.insertOrUpdateRecord(record)

            // 데이터 삽입 후 해당 월의 데이터 로드
            val year = record.date.substring(0, 4).toInt()
            val month = record.date.substring(5, 7).toInt()
            loadFocusTimesForMonth(year, month)
        }
    }

    //특정 날짜의 기록 가져오기
    suspend fun getRecordByDate(date: String): TimerRecords? {
        return timerRecordsDao.getRecordByDate(date)
    }

    // 특정 날짜의 기록 삭제
    fun deleteRecordByDate(date: String) {
        viewModelScope.launch {
            timerRecordsDao.deleteRecordByDate(date)
        }
    }

    fun loadFocusTimesForMonth(year: Int, month: Int) {
        viewModelScope.launch {
            val startDate = String.format(Locale.US, "%04d-%02d-01", year, month)
            val endDate = String.format(
                Locale.US,
                "%04d-%02d-%02d",
                year,
                month,
                YearMonth.of(year, month).lengthOfMonth()
            )

            // 데이터베이스에서 월별 데이터 가져오기
            val records = timerRecordsDao.getRecordsForDateRange(startDate, endDate)

            // 레코드를 맵 형태로 변환 (초 단위로 변환)
            val focusTimesMap = records.associate {
                it.date to (it.totalFocusTime / 1000).toInt() // 밀리초 -> 초 단위로 변환
            }

            // LiveData 업데이트
            _focusTimes.postValue(focusTimesMap)
        }
    }


    fun updateFocusTime(date: String, additionalTime: Long) {
        viewModelScope.launch {
            // 기존 데이터 가져오기
            val record = timerRecordsDao.getRecordByDate(date) ?: TimerRecords(date, 0)

            // 추가된 시간을 기존 데이터에 더하기
            val updatedRecord = record.copy(totalFocusTime = record.totalFocusTime + additionalTime)

            // Room DB에 업데이트
            timerRecordsDao.insertOrUpdateRecord(updatedRecord)
        }
    }







}

