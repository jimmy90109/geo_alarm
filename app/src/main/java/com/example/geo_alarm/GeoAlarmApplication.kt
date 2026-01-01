package com.example.geo_alarm

import android.app.Application
import com.example.geo_alarm.data.AppDatabase
import com.example.geo_alarm.data.AlarmRepository

class GeoAlarmApplication : Application() {
    val database by lazy { AppDatabase.getDatabase(this) }
    val repository by lazy { AlarmRepository(database.alarmDao()) }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(
                "geo_alarm_channel",
                "Geo Alarm Notifications",
                android.app.NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Shows active alarm progress"
            }
            val notificationManager = getSystemService(android.app.NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
}
