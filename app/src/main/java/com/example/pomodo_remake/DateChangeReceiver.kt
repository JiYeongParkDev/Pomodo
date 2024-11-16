package com.example.pomodo_remake

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager

//날짜가 바뀌면 알림이 간다.( 총집중시간이 초기화되어야 하니까)
class DateChangeReceiver : BroadcastReceiver(){
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_DATE_CHANGED) {
            Log.d("DATE_CHANGE", "Date changed detected.")

            // MainActivity에 날짜 변경 알림 전달
            val localIntent = Intent("com.example.pomodo_remake.ACTION_DATE_CHANGED")
            LocalBroadcastManager.getInstance(context).sendBroadcast(localIntent)
        }
    }
}