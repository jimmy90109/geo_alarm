package com.example.geo_alarm

import android.app.IntentService
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat

class AlarmNotificationService : IntentService("AlarmNotificationService") {
    companion object {
        const val CHANNEL_ID = "alarm_live_update"
        const val NOTIFICATION_ID = 1001
        const val ACTION_STOP_ALARM = "com.example.geo_alarm.ACTION_STOP_ALARM"
    }

    override fun onHandleIntent(intent: Intent?) {
        if (intent?.action == ACTION_STOP_ALARM) {
            stopAlarm()
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.cancel(NOTIFICATION_ID)
            return
        }
        showLiveUpdateNotification()
    }

    private fun showLiveUpdateNotification() {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Alarm Live Update",
                NotificationManager.IMPORTANCE_HIGH
            )
            nm.createNotificationChannel(channel)
        }

        val stopIntent = Intent(this, AlarmNotificationService::class.java).apply {
            action = ACTION_STOP_ALARM
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 0, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("鬧鐘進行中")
            .setContentText("點擊關閉鬧鐘")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .addAction(
                R.mipmap.ic_launcher,
                "關閉鬧鐘",
                stopPendingIntent
            )
            .setAutoCancel(false)

        // Note: requestPromotedOngoing() is not available in current AndroidX version
        // The notification will still work as an ongoing notification with setOngoing(true)

        nm.notify(NOTIFICATION_ID, builder.build())
    }

    private fun stopAlarm() {
        // TODO: 可透過 MethodChannel 通知 Flutter 關閉鬧鐘
    }
}