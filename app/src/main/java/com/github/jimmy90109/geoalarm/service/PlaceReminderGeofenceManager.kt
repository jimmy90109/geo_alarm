package com.github.jimmy90109.geoalarm.service

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.github.jimmy90109.geoalarm.data.PlaceReminderDataRepository
import com.github.jimmy90109.geoalarm.data.PlaceReminderWithItems
import com.github.jimmy90109.geoalarm.data.PlaceTriggerType
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class PlaceReminderGeofenceManager(
    private val context: Context,
    private val repository: PlaceReminderDataRepository,
) {
    private val appContext = context.applicationContext
    private val geofencingClient: GeofencingClient =
        LocationServices.getGeofencingClient(appContext)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun syncEnabledReminders() {
        scope.launch {
            if (!hasRequiredLocationPermissions()) {
                Log.w(TAG, "Skipping place reminder geofence sync because location permission is missing")
                return@launch
            }
            val enabledReminders = repository.getEnabledRemindersOneShot()
            removeAllManagedGeofences {
                if (enabledReminders.isNotEmpty()) addGeofences(enabledReminders)
            }
        }
    }

    fun syncReminder(reminderId: String) {
        scope.launch {
            val reminder = repository.getReminder(reminderId)
            removeGeofences(listOf(requestId(reminderId))) {
                if (reminder != null && reminder.reminder.enabled && hasRequiredLocationPermissions()) {
                    addGeofences(listOf(reminder))
                }
            }
        }
    }

    fun removeReminder(reminderId: String) {
        removeGeofences(listOf(requestId(reminderId)))
    }

    private fun hasRequiredLocationPermissions(): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val background = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(
                appContext,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        return fine && background
    }

    @SuppressLint("MissingPermission")
    private fun addGeofences(reminders: List<PlaceReminderWithItems>) {
        val geofences = reminders.map { reminderWithItems ->
            val reminder = reminderWithItems.reminder
            val transition = when (reminder.triggerType) {
                PlaceTriggerType.ENTER -> Geofence.GEOFENCE_TRANSITION_ENTER
                PlaceTriggerType.DWELL -> Geofence.GEOFENCE_TRANSITION_DWELL
            }
            Geofence.Builder()
                .setRequestId(requestId(reminder.id))
                .setCircularRegion(
                    reminder.latitude,
                    reminder.longitude,
                    reminder.radiusMeters.toFloat(),
                )
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setNotificationResponsiveness(30_000)
                .setTransitionTypes(transition)
                .apply {
                    if (reminder.triggerType == PlaceTriggerType.DWELL) {
                        setLoiteringDelay((reminder.dwellMinutes ?: 3) * 60_000)
                    }
                }
                .build()
        }
        if (geofences.isEmpty()) return

        val request = GeofencingRequest.Builder()
            .setInitialTrigger(0)
            .addGeofences(geofences)
            .build()
        geofencingClient.addGeofences(request, pendingIntent())
            .addOnSuccessListener {
                Log.d(TAG, "Registered place reminder geofences count=${geofences.size}")
            }
            .addOnFailureListener { error ->
                Log.e(TAG, "Failed to register place reminder geofences", error)
            }
    }

    private fun removeAllManagedGeofences(onComplete: () -> Unit) {
        scope.launch {
            val requestIds = repository.getEnabledRemindersOneShot().map { requestId(it.reminder.id) }
            if (requestIds.isEmpty()) {
                onComplete()
            } else {
                removeGeofences(requestIds, onComplete)
            }
        }
    }

    private fun removeGeofences(requestIds: List<String>, onComplete: () -> Unit = {}) {
        if (requestIds.isEmpty()) {
            onComplete()
            return
        }
        geofencingClient.removeGeofences(requestIds)
            .addOnCompleteListener { onComplete() }
    }

    private fun pendingIntent(): PendingIntent {
        val intent = Intent(appContext, GeofenceBroadcastReceiver::class.java).apply {
            action = ACTION_PLACE_REMINDER_GEOFENCE
        }
        return PendingIntent.getBroadcast(
            appContext,
            PLACE_REMINDER_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
        )
    }

    companion object {
        const val ACTION_PLACE_REMINDER_GEOFENCE =
            "com.github.jimmy90109.geoalarm.ACTION_PLACE_REMINDER_GEOFENCE"
        const val REQUEST_ID_PREFIX = "place_reminder:"
        private const val PLACE_REMINDER_REQUEST_CODE = 40_901
        private const val TAG = "PlaceReminderGeofence"

        fun requestId(reminderId: String): String = "$REQUEST_ID_PREFIX$reminderId"

        fun reminderIdFromRequestId(requestId: String): String? =
            requestId.takeIf { it.startsWith(REQUEST_ID_PREFIX) }
                ?.removePrefix(REQUEST_ID_PREFIX)
                ?.takeIf { it.isNotBlank() }
    }
}
