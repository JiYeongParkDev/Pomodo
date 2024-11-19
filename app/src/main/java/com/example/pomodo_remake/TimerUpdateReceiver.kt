package com.example.pomodo_remake

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.TextView
import java.util.Locale

//totalFocusTime값을 MainAcivitity에 전달,하고 그 값을 TextView나 UI에 반영하는 작업을 위해 만들었음
class TimerUpdateReceiver (private val totalFocusTimeView: TextView) : BroadcastReceiver(){
    override fun onReceive(context: Context?, intent: Intent?) {
        // 브로드캐스트에서 총 집중 시간을 가져옴
        val totalFocusTime = intent?.getLongExtra(TimerForegroundService.EXTRA_TOTAL_FOCUS_TIME, 0L) ?: 0L

        // 받은 totalFocusTime을 UI 업데이트 메서드로 전달
        Log.d("TimerUpdateReceiver", "Received total focus time: $totalFocusTime")
        updateTotalFocusTimeUI(totalFocusTime)
    }

    // totalFocusTime을 UI 업데이트
    private fun updateTotalFocusTimeUI(totalFocusTime: Long) {
        val hours = totalFocusTime / 3600
        val minutes = (totalFocusTime % 3600) / 60
        val seconds = totalFocusTime % 60

        // totalFocusTimeView는 MainActivity에서 전달받은 TextView
        totalFocusTimeView.text = String.format(Locale.getDefault(),"%02d:%02d:%02d", hours, minutes, seconds)
    }

}