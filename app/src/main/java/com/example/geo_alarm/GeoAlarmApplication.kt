package com.example.geo_alarm

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import com.example.geo_alarm.data.AppDatabase
import com.example.geo_alarm.data.AlarmRepository
import com.example.geo_alarm.data.SettingsRepository
import com.google.android.libraries.places.api.Places

class GeoAlarmApplication : Application() {
    val database by lazy { AppDatabase.getDatabase(this) }
    val repository by lazy { AlarmRepository(database.alarmDao()) }
    val settingsRepository by lazy { SettingsRepository(this) }

    override fun onCreate() {
        super.onCreate()

        // Initialize Places SDK
        if (!Places.isInitialized()) {
            Places.initialize(this, BuildConfig.GOOGLE_MAPS_API_KEY)
        }

        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "geo_alarm_channel",
                "Geo Alarm Notifications",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = "Shows active alarm progress"
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
}
