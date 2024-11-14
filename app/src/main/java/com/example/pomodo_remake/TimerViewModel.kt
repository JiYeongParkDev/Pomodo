package com.example.pomodo_remake

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

//타이머 뷰 모델
//UI와 데이터베이스를 연결하는 중간다리 영ㄱ할
// LiveData를 사용해 데이터가 변경될 때 UI에 자동으로 반영되도록 설정한다.
class TimerViewModel(application: Application) : AndroidViewModel(application) {
    private val timerRecordsDao = AppDatabase.getDatabase(application).timerRecordsDao()

    val allRecords: LiveData<List<TimerRecords>> = timerRecordsDao.getAllRecords()

    //삽인 또는 업데이트
    fun insertOrUpdateRecord(record: TimerRecords) {
        viewModelScope.launch {
            timerRecordsDao.insertOrUpdateRecord(record)
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

}

