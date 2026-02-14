package com.github.jimmy90109.geoalarm

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import com.github.jimmy90109.geoalarm.data.AppDatabase
import com.github.jimmy90109.geoalarm.data.AlarmRepository
import com.github.jimmy90109.geoalarm.data.OnboardingRepository
import com.github.jimmy90109.geoalarm.data.SettingsRepository
import com.github.jimmy90109.geoalarm.utils.SharedPreferenceManager
import com.google.android.libraries.places.api.Places
import kotlin.getValue

class GeoAlarmApplication : Application() {
    val database by lazy { AppDatabase.getDatabase(this) }
    val repository by lazy { AlarmRepository(database.alarmDao(), database.scheduleDao()) }
    val settingsRepository by lazy { SettingsRepository(this) }
    val onboardingRepository by lazy { OnboardingRepository(this) }
    val sharedPreferenceManager by lazy { SharedPreferenceManager(this) }

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
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "Shows active alarm progress"
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
}
