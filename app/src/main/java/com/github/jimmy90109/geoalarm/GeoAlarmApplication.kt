package com.github.jimmy90109.geoalarm

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import androidx.appfunctions.service.AppFunctionConfiguration
import com.github.jimmy90109.geoalarm.appfunctions.AppFunctionsEntryPoint
import com.github.jimmy90109.geoalarm.appfunctions.GeoAlarmFunctions
import com.github.jimmy90109.geoalarm.data.AppDatabase
import com.github.jimmy90109.geoalarm.data.AlarmRepository
import com.github.jimmy90109.geoalarm.data.SettingsRepository
import dagger.hilt.android.EntryPointAccessors
import com.google.android.libraries.places.api.Places
import dagger.hilt.android.HiltAndroidApp
import kotlin.getValue

@HiltAndroidApp
class GeoAlarmApplication : Application(), AppFunctionConfiguration.Provider {
    val database by lazy { AppDatabase.getDatabase(this) }
    val repository by lazy { AlarmRepository(database.alarmDao(), database.scheduleDao()) }
    val settingsRepository by lazy { SettingsRepository(this) }
    override val appFunctionConfiguration: AppFunctionConfiguration by lazy {
        val entryPoint = EntryPointAccessors.fromApplication(this, AppFunctionsEntryPoint::class.java)
        AppFunctionConfiguration.Builder()
            .addEnclosingClassFactory(GeoAlarmFunctions::class.java) {
                GeoAlarmFunctions(entryPoint.startAlarmUseCase())
            }
            .build()
    }

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
