package com.github.jimmy90109.geoalarm

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.util.Log
import androidx.appfunctions.service.AppFunctionConfiguration
import androidx.glance.appwidget.updateAll
import com.github.jimmy90109.geoalarm.appfunctions.AppFunctionsEntryPoint
import com.github.jimmy90109.geoalarm.appfunctions.GeoAlarmFunctions
import com.github.jimmy90109.geoalarm.data.AppDatabase
import com.github.jimmy90109.geoalarm.data.AlarmRepository
import com.github.jimmy90109.geoalarm.data.LocalPlaceReminderAttachmentStore
import com.github.jimmy90109.geoalarm.data.PlaceReminderRepository
import com.github.jimmy90109.geoalarm.data.SettingsRepository
import com.github.jimmy90109.geoalarm.service.PlaceReminderGeofenceManager
import dagger.hilt.android.EntryPointAccessors
import com.google.android.libraries.places.api.Places
import com.github.jimmy90109.geoalarm.widget.GeoAlarmGlanceWidget
import dagger.hilt.android.HiltAndroidApp
import kotlin.getValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

@HiltAndroidApp
class GeoAlarmApplication : Application(), AppFunctionConfiguration.Provider {
    companion object {
        private const val TAG = "GeoAlarmApplication"
    }
    val database by lazy { AppDatabase.getDatabase(this) }
    val repository by lazy { AlarmRepository(database.alarmDao(), database.scheduleDao()) }
    val placeReminderRepository by lazy {
        PlaceReminderRepository(database.placeReminderDao(), LocalPlaceReminderAttachmentStore(this))
    }
    val settingsRepository by lazy { SettingsRepository(this) }
    private val appScope by lazy { CoroutineScope(SupervisorJob() + Dispatchers.IO) }

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
            Places.initializeWithNewPlacesApiEnabled(this, BuildConfig.GOOGLE_MAPS_API_KEY)
        }

        createNotificationChannel()
        PlaceReminderGeofenceManager(this, placeReminderRepository).syncEnabledReminders()
        observeAlarmChangesAndRefreshWidgets()
    }

    private fun observeAlarmChangesAndRefreshWidgets() {
        appScope.launch {
            repository.allAlarms
                .drop(1) // Skip initial load; only refresh on actual data change.
                .collect { alarms ->
                    Log.d(
                        TAG,
                        "Alarm flow changed count=${alarms.size} " +
                            "alarms=${alarms.joinToString(prefix = "[", postfix = "]") { "${it.id}:${it.iconKey}:${it.name}" }}"
                    )
                    GeoAlarmGlanceWidget().updateAll(this@GeoAlarmApplication)
                }
        }
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
