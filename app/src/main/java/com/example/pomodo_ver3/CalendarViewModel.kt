package com.example.pomodo_ver3

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pomodo_ver3.data.dao.TimerRecordsDao
import kotlinx.coroutines.launch
import java.time.YearMonth
import java.util.Locale

class CalendarViewModel(private val timerRecordsDao: TimerRecordsDao) : ViewModel() {
    private val _focusTimes: MutableLiveData<Map<String, Int>> = MutableLiveData()
    val focusTimes: LiveData<Map<String, Int>> get() = _focusTimes

    // 특정 월의 집중 시간을 로드
    fun loadFocusTimesForMonth(year: Int, month: Int) {
        viewModelScope.launch {
            val startDate = String.format(Locale.US, "%04d-%02d-01", year, month)
            val endDate = String.format(Locale.US,"%04d-%02d-%02d", year, month, YearMonth.of(year, month).lengthOfMonth())

            // 특정 월의 데이터만 가져오기
            val records = timerRecordsDao.getRecordsForDateRange(startDate, endDate)

            // 데이터를 맵으로 변환
            val focusTimesMap = records.associate {
                it.date to (it.totalFocusTime / 60_000).toInt() // 밀리초를 분으로 변환
            }

            // LiveData 업데이트
            _focusTimes.postValue(focusTimesMap)
        }
    }

}