package com.github.jimmy90109.geoalarm.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.github.jimmy90109.geoalarm.utils.WakeLocker
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent

class GeofenceBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Log.d("GeofenceReceiver", "onReceive called with action: ${intent.action}")

        val geofencingEvent = GeofencingEvent.fromIntent(intent)
        if (geofencingEvent == null) {
            Log.e("GeofenceReceiver", "GeofencingEvent is null")
            return
        }

        if (geofencingEvent.hasError()) {
            val errorMessage = geofencingEvent.errorCode
            Log.e("GeofenceReceiver", "Geofencing Error: $errorMessage")
            return
        }

        val geofenceTransition = geofencingEvent.geofenceTransition
        Log.d("GeofenceReceiver", "Geofence Transition Detected: $geofenceTransition")

        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
            val triggeringGeofences = geofencingEvent.triggeringGeofences
            Log.i("GeofenceReceiver", "Geofence Triggered: $triggeringGeofences")

            // Check which geofence was triggered
            val isDestinationGeofence = triggeringGeofences?.any { 
                it.requestId == GeoAlarmService.GEOFENCE_DESTINATION_ID 
            } ?: false
            
            val isWarningGeofence = triggeringGeofences?.any { 
                it.requestId == GeoAlarmService.GEOFENCE_WARNING_ID 
            } ?: false

            when {
                isDestinationGeofence -> {
                    // Destination reached - acquire WakeLock and trigger arrival
                    Log.i("GeofenceReceiver", "Destination geofence triggered!")
                    WakeLocker.acquire(context)
                    
                    val serviceIntent = Intent(context, GeoAlarmService::class.java).apply {
                        action = GeoAlarmService.ACTION_GEOFENCE_TRIGGERED
                    }
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        context.startForegroundService(serviceIntent)
                    } else {
                        context.startService(serviceIntent)
                    }
                }
                isWarningGeofence -> {
                    // Warning geofence (5km) - switch to MID zone, no WakeLock needed
                    Log.i("GeofenceReceiver", "Warning geofence triggered (5km)")
                    
                    val serviceIntent = Intent(context, GeoAlarmService::class.java).apply {
                        action = GeoAlarmService.ACTION_WARNING_GEOFENCE_TRIGGERED
                    }
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        context.startForegroundService(serviceIntent)
                    } else {
                        context.startService(serviceIntent)
                    }
                }
                else -> {
                    Log.d("GeofenceReceiver", "Unknown geofence triggered")
                }
            }
        } else {
            Log.d("GeofenceReceiver", "Unhandled transition type: $geofenceTransition")
        }
    }
}
