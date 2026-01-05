package com.example.geo_alarm.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.geo_alarm.data.SettingsRepository
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent

class GeofenceBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Log.d("GeofenceReceiver", "onReceive called with action: ${intent.action}")

        // Acquire WakeLock immediately to ensure the device stays awake
        // while we process the Geofence event and start the Service.
        // This is crucial for Android 8.0+ (Oreo) background execution limits.
        com.example.geo_alarm.utils.WakeLocker.acquire(context)

        val geofencingEvent = GeofencingEvent.fromIntent(intent)
        if (geofencingEvent == null) {
            Log.e("GeofenceReceiver", "GeofencingEvent is null")
            com.example.geo_alarm.utils.WakeLocker.release()
            return
        }

        if (geofencingEvent.hasError()) {
            val errorMessage = geofencingEvent.errorCode
            Log.e("GeofenceReceiver", "Geofencing Error: $errorMessage")
            com.example.geo_alarm.utils.WakeLocker.release()
            return
        }

        val geofenceTransition = geofencingEvent.geofenceTransition
        Log.d("GeofenceReceiver", "Geofence Transition Detected: $geofenceTransition")

        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
            val triggeringGeofences = geofencingEvent.triggeringGeofences
            Log.i("GeofenceReceiver", "Geofence Triggered: $triggeringGeofences")

            // Trigger Service to show Arrival Notification
            val serviceIntent = Intent(context, GeoAlarmService::class.java).apply {
                action = GeoAlarmService.ACTION_GEOFENCE_TRIGGERED
            }
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        } else {
            Log.d("GeofenceReceiver", "Unhandled transition type: $geofenceTransition")
            com.example.geo_alarm.utils.WakeLocker.release()
        }
    }
}
