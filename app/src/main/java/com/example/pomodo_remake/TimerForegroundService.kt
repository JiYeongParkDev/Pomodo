package com.example.pomodo_remake

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat


class TimerForegroundService : Service(){
    companion object {
        // Foreground Service에서 사용할 고유 채널 및 액션 ID
        const val CHANNEL_ID = "TimerServiceChannel"
        const val NOTIFICATION_ID = 1
        const val ACTION_UPDATE = "ACTION_UPDATE" // 알림 업데이트를 위한 액션
        const val EXTRA_IS_FOCUS_TIMER = "IS_FOCUS_TIMER" // 집중 타이머 여부
        const val EXTRA_FOCUS_REMAINING = "FOCUS_REMAINING" // 집중 타이머 남은 시간
        const val EXTRA_BREAK_REMAINING = "BREAK_REMAINING" // 휴식 타이머 남은 시간
        const val EXTRA_REPEAT_COUNT = "REPEAT_COUNT" // 남은 반복 횟수
        const val EXTRA_TOTAL_FOCUS_TIME = "EXTRA_TOTAL_FOCUS_TIME"  // 총 집중 시간 추가

    }

    private var totalFocusTimeInSeconds: Long = 0 // 총 집중 시간 (초 단위)

    override fun onCreate() {
        super.onCreate()
        // Foreground Service를 위한 Notification Channel 생성
        createNotificationChannel()

        // SharedPreferences에서 총 집중 시간 불러오기
        val sharedPreferences = getSharedPreferences("TimerPreferences", Context.MODE_PRIVATE)
        totalFocusTimeInSeconds = sharedPreferences.getLong("totalFocusTimeInSeconds", 0L)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("TimerForegroundService", "onStartCommand called")

        if (intent?.action == ACTION_UPDATE) {
            val isFocusTimer = intent.getBooleanExtra(EXTRA_IS_FOCUS_TIMER, true)
            val focusRemaining = intent.getLongExtra(EXTRA_FOCUS_REMAINING, 0L)
            val breakRemaining = intent.getLongExtra(EXTRA_BREAK_REMAINING, 0L)
            val repeatCount = intent.getIntExtra(EXTRA_REPEAT_COUNT, 0)

            Log.d("TimerForegroundService", "Data received: isFocusTimer=$isFocusTimer, focusRemaining=$focusRemaining, breakRemaining=$breakRemaining, repeatCount=$repeatCount")

            if (isFocusTimer) {
                totalFocusTimeInSeconds++
                saveFocusTimeToPreferences(totalFocusTimeInSeconds)
            }

            val notification = if (isFocusTimer) {
                createNotification("집중 타이머", focusRemaining, totalFocusTimeInSeconds)
            } else {
                createNotification("휴식 타이머", breakRemaining, totalFocusTimeInSeconds)
            }

            startForeground(NOTIFICATION_ID, notification)

            // 브로드캐스트로 총 집중 시간 전송
            val updateIntent = Intent(ACTION_UPDATE)
            updateIntent.putExtra(EXTRA_TOTAL_FOCUS_TIME, totalFocusTimeInSeconds)
            sendBroadcast(updateIntent)  // Broadcast 전송

}
        return START_STICKY
    }


    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,                      // 채널 ID
            "Timer Service Channel",         // 채널 이름
            NotificationManager.IMPORTANCE_LOW // 알림 중요도 (LOW: 소음 없이 표시)
        )
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    private fun createNotification(
        title: String,        // 알림 제목 (예: "집중 타이머" 또는 "휴식 타이머")
        remainingTime: Long,  // 남은 시간 (밀리초 단위)
        totalFocusTime: Long      // 총 집중 시간 추가
    ): Notification {
        val minutes = remainingTime / 1000 / 60
        val seconds = remainingTime / 1000 % 60
        val focusMinutes = totalFocusTime / 60
        val focusSeconds = totalFocusTime % 60
        //val contentText = "${minutes}분 ${seconds}초 남음\n남은 반복 횟수: ${repeatCount}회"
        val contentText = "${minutes}분 ${seconds}초 남음"



        Log.d("TimerForegroundService", "Creating notification with title: $title, content: $contentText")


        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)                 // 알림 제목 설정
            .setContentText(contentText)            // 알림 본문 설정
            .setStyle(NotificationCompat.BigTextStyle().bigText(contentText)) // 본문 확장 가능
            .setSmallIcon(R.drawable.timer)      // 알림 아이콘
            .setPriority(NotificationCompat.PRIORITY_LOW) // 낮은 우선순위
            .setOngoing(true)                       // 알림을 고정 상태로 설정
            .build()
    }
    private fun saveFocusTimeToPreferences(timeInSeconds: Long) {
        val sharedPreferences = getSharedPreferences("TimerPreferences", Context.MODE_PRIVATE)
        sharedPreferences.edit().putLong("totalFocusTimeInSeconds", timeInSeconds).apply()
    }





}