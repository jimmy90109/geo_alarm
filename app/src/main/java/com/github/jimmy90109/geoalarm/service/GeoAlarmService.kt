package com.github.jimmy90109.geoalarm.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.app.NotificationManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.github.jimmy90109.geoalarm.GeoAlarmApplication
import com.github.jimmy90109.geoalarm.MainActivity
import com.github.jimmy90109.geoalarm.R
import com.github.jimmy90109.geoalarm.data.MonitoringMethod
import com.github.jimmy90109.geoalarm.utils.WakeLocker
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.*

class GeoAlarmService : Service(), LocationListener {

    companion object {
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        const val ACTION_START_TEST = "ACTION_START_TEST"  // For testing notifications
        const val ACTION_GEOFENCE_TRIGGERED = "ACTION_GEOFENCE_TRIGGERED"
        const val ACTION_NOTIFICATION_DISMISSED = "ACTION_NOTIFICATION_DISMISSED"

        // Output Actions
        const val ACTION_CANCEL_ALARM =
            "ACTION_CANCEL_ALARM" // User clicked "Cancel" on notification

        const val EXTRA_ALARM_ID = "EXTRA_ALARM_ID"
        const val EXTRA_NAME = "EXTRA_NAME"
        const val EXTRA_DEST_LAT = "EXTRA_DEST_LAT"
        const val EXTRA_DEST_LNG = "EXTRA_DEST_LNG"
        const val EXTRA_RADIUS = "EXTRA_RADIUS"
        const val EXTRA_START_LAT = "EXTRA_START_LAT"
        const val EXTRA_START_LNG = "EXTRA_START_LNG"

        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "geo_alarm_channel"
    }

    private lateinit var locationManager: LocationManager
    private lateinit var geofencingClient: com.google.android.gms.location.GeofencingClient
    private val settingsRepository by lazy { (application as GeoAlarmApplication).settingsRepository }
    private val repository by lazy { (application as GeoAlarmApplication).repository }
    private var isServiceRunning = false
    private var currentMonitoringMethod = MonitoringMethod.GEOFENCE

    // Alarm Data
    private var alarmName: String = "Alarm"
    private var alarmId: String = ""
    private var destLat: Double = 0.0
    private var destLng: Double = 0.0
    private var radius: Double = 100.0
    private var startLat: Double = 0.0
    private var startLng: Double = 0.0

    private var totalDistance: Float = 0f
    private var isArrived = false

    private var vibrator: Vibrator? = null
    private var testJob: Job? = null  // For test mode

    override fun onCreate() {
        super.onCreate()
        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        geofencingClient = LocationServices.getGeofencingClient(this)


        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION") getSystemService(VIBRATOR_SERVICE) as Vibrator
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                if (!isServiceRunning) {
                    alarmId = intent.getStringExtra(EXTRA_ALARM_ID) ?: ""
                    alarmName = intent.getStringExtra(EXTRA_NAME) ?: "GeoAlarm"
                    destLat = intent.getDoubleExtra(EXTRA_DEST_LAT, 0.0)
                    destLng = intent.getDoubleExtra(EXTRA_DEST_LNG, 0.0)
                    radius = intent.getDoubleExtra(EXTRA_RADIUS, 100.0)
                    startLat = intent.getDoubleExtra(EXTRA_START_LAT, 0.0)
                    startLng = intent.getDoubleExtra(EXTRA_START_LNG, 0.0)

                    val startLocation = Location("").apply {
                        latitude = startLat
                        longitude = startLng
                    }
                    val destLocation = Location("").apply {
                        latitude = destLat
                        longitude = destLng
                    }

                    // totalDistance will be set on first GPS update
                    totalDistance = 0f

                    startForegroundService()

                    // Check monitoring method
                    CoroutineScope(Dispatchers.Main).launch {
                        settingsRepository.monitoringMethod.collect { method ->
                            currentMonitoringMethod = method
                            if (method == MonitoringMethod.GPS) {
                                startLocationUpdates()
                            } else {
                                startGeofencing()
                            }
                            // Only listen to the first value then cancel collection to avoid switching mid-service if user somehow changes it (though UI prevents it)
                            // Actually, UI prevents it, so just taking first value is safe.
                            this.cancel()
                        }
                    }
                    isServiceRunning = true
                }
            }

            ACTION_GEOFENCE_TRIGGERED -> {
                // Keep the WakeLock held or re-acquire it to ensure we can vibrate and show notification
                // The receiver acquired it, so we are good, but let's make sure we hold it until user interaction or sufficient time
                // Using the singleton, it's already held.
                triggerArrival()
            }

            ACTION_NOTIFICATION_DISMISSED -> {
                if (isServiceRunning && !isArrived) {
                    startForegroundService() // Restores notification
                }
                WakeLocker.release()
            }

            ACTION_START_TEST -> {
                if (!isServiceRunning) {
                    alarmName = "Test Alarm"
                    alarmId = "test-alarm-id"
                    totalDistance = 1000f  // 1000m simulated distance
                    isArrived = false

                    startForegroundService()
                    startTestMode()
                    isServiceRunning = true
                }
            }

            ACTION_STOP -> {
                testJob?.cancel()
                if (::geofencingClient.isInitialized) {
                    geofencingClient.removeGeofences(getGeofencePendingIntent())
                }
                WakeLocker.release()
                stopSelf()
            }
        }
        return START_STICKY
    }

    private fun startTestMode() {
        testJob = CoroutineScope(Dispatchers.Main).launch {
            // Simulate progress from 0% to 100% over 10 seconds
            val totalSteps = 10
            for (i in 0..totalSteps) {
                if (!isActive) break

                val progress = (i * 100) / totalSteps
                val remainingDistance = ((totalSteps - i) * 100)  // 1000m -> 0m

                android.util.Log.d(
                    "GeoAlarmService",
                    "[TEST] Progress: $progress%, Remaining: ${remainingDistance}m"
                )

                if (progress >= 100) {
                    // Trigger arrival
                    isArrived = true
                    triggerArrival()
                } else {
                    updateNotification(progress, remainingDistance)
                }

                delay(1000)  // Update every 1 second
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun startForegroundService() {
        val notification = buildNotification(0, 0, currentMonitoringMethod)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, FOREGROUND_SERVICE_TYPE_LOCATION)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    @SuppressLint("MissingPermission")
    private fun startGeofencing() {
        val geofence = Geofence.Builder().setRequestId(alarmId)
            .setCircularRegion(destLat, destLng, radius.toFloat())
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            // Critical for immediate response: 
            // Setting responsiveness to 0ms tells the system to prioritize this geofence
            // and trigger the intent as soon as possible, potentially ignoring some power savings.
            .setNotificationResponsiveness(0) 
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER).build()

        // Debug: Log Last Known Location
        try {
            val lastLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            Log.d(
                "GeoAlarmService", "StartGeofencing - Last Known Location: $lastLocation"
            )
            if (lastLocation != null) {
                val dist = FloatArray(1)
                Location.distanceBetween(
                    lastLocation.latitude,
                    lastLocation.longitude,
                    destLat,
                    destLng,
                    dist,
                )
                Log.d(
                    "GeoAlarmService",
                    "StartGeofencing - Distance to dest: ${dist[0]}m, Radius: $radius"
                )
            }
        } catch (e: SecurityException) {
            Log.e("GeoAlarmService", "Failed to get last known location", e)
        }

        val geofencingRequest =
            GeofencingRequest.Builder().setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                .addGeofence(geofence).build()

        geofencingClient.addGeofences(geofencingRequest, getGeofencePendingIntent())
            .addOnSuccessListener {
                Log.d("GeoAlarmService", "Geofence added")
                updateNotificationForGeofence()
            }.addOnFailureListener { e ->
                Log.e("GeoAlarmService", "Geofence failed", e)
                // Fallback to GPS?
                startLocationUpdates()
            }
    }

    private fun getGeofencePendingIntent(): PendingIntent {
        val intent = Intent(this, GeofenceBroadcastReceiver::class.java)
        return PendingIntent.getBroadcast(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
        )
    }

    private fun updateNotificationForGeofence() {
        val notification = buildNotification(
            0,
            0,
            MonitoringMethod.GEOFENCE,
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, notification)
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        // Request updates every 10 seconds or 10 meters
        try {
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                10000L,
                10f,
                this,
            )
            locationManager.requestLocationUpdates(
                LocationManager.NETWORK_PROVIDER,
                5000L,
                10f,
                this,
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onLocationChanged(location: Location) {
        val destLocation = Location("").apply {
            latitude = destLat
            longitude = destLng
        }

        val distanceToDest = location.distanceTo(destLocation)
        val remainingDist = distanceToDest - radius

        if (remainingDist <= 0) {
            // Arrived!
            if (!isArrived) {
                isArrived = true
                triggerArrival()
            }
        } else {
            // Set totalDistance on first GPS update, or update if user moved further away
            if (totalDistance == 0f || remainingDist > totalDistance) {
                totalDistance = remainingDist.toFloat()
            }

            // Calculate progress
            // progress = 1 - (remainingDist / totalDistance)
            val progressFraction = 1.0f - (remainingDist.toFloat() / totalDistance)
            val progressPercent = (progressFraction * 100).toInt().coerceIn(0, 100)

            Log.d(
                "GeoAlarmService",
                "Distance: ${distanceToDest.toInt()}m, Remaining: ${remainingDist.toInt()}m, Total: ${totalDistance.toInt()}m, Progress: $progressPercent%"
            )

            updateNotification(progressPercent, remainingDist.toInt())
        }
    }

    private fun updateNotification(progress: Int, remainingDistance: Int) {
        if (isArrived) return

        val notification = buildNotification(
            progress, remainingDistance, MonitoringMethod.GPS,
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, notification)
    }

    private fun triggerArrival() {
        // Vibrate
        val vibrationPattern = longArrayOf(0, 500, 200, 500) // wait 0, vib 500, sleep 200, vib 500
        vibrator?.vibrate(
            VibrationEffect.createWaveform(
                vibrationPattern,
                0,
            )
        ) // 0 means repeat at index 0

        val notification = buildArrivalNotification()
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, notification)
        
        // Use WakeLock to turn screen on if possible
        WakeLocker.acquire(this)
    }

    private fun buildNotification(
        progress: Int, remainingDistance: Int, method: MonitoringMethod,
    ): Notification {
        // Cancel Action:
        // Intent that opens app and also maybe broadcasts to MainActivity to turn off toggle?
        // User requested: "Cancel button -> Open App -> Close Alarm"

        val cancelIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            action = ACTION_CANCEL_ALARM
            putExtra(EXTRA_ALARM_ID, alarmId)
        }
        val cancelPendingIntent = PendingIntent.getActivity(
            this,
            0,
            cancelIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        // Delete Intent (Handle Slide Away)
        val deleteIntent = Intent(this, GeoAlarmService::class.java).apply {
            action = ACTION_NOTIFICATION_DISMISSED
        }
        val deletePendingIntent = PendingIntent.getService(
            this,
            0,
            deleteIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        // Content intent to open app when tapping notification body
        val contentIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            this,
            1,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val builder =
            NotificationCompat.Builder(this, CHANNEL_ID).setSmallIcon(R.drawable.ic_notification)
                .setOnlyAlertOnce(true).setOngoing(true)
                .setCategory(NotificationCompat.CATEGORY_PROGRESS)
                .setRequestPromotedOngoing(true) // Android 14 compat
                .setContentIntent(contentPendingIntent)
                .setDeleteIntent(deletePendingIntent) // Listen for dismissal
                .addAction(
                    android.R.drawable.ic_menu_close_clear_cancel,
                    getString(R.string.notification_cancel),
                    cancelPendingIntent,
                )

        if (method == MonitoringMethod.GEOFENCE) {
            builder.setContentTitle(getString(R.string.notification_title, alarmName))
                .setContentText(getString(R.string.notification_monitoring_geofence))
                .setShortCriticalText(alarmName)
        } else {
            builder.setContentTitle(getString(R.string.notification_title, alarmName))
                .setContentText(
                    getString(
                        R.string.notification_distance, remainingDistance, progress
                    )
                ).setProgress(100, progress, false).setShortCriticalText(alarmName)
        }

        return builder.build()
    }

    private fun buildArrivalNotification(): Notification {
        // "Turn Off" button -> Open App (same as cancel effectively for the user action flow)
        // User said: "Turn Off button -> Stop Vibrate -> Open App"

        val turnOffIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            action = ACTION_CANCEL_ALARM // Re-use this action as it implies "Stop/Done"
            putExtra(EXTRA_ALARM_ID, alarmId)
        }
        val turnOffPendingIntent = PendingIntent.getActivity(
            this,
            1,
            turnOffIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_arrived_title, alarmName))
            .setContentText(getString(R.string.notification_arrived_text))
            .setSmallIcon(R.drawable.ic_notification).setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM).addAction(
                android.R.drawable.ic_lock_power_off,
                getString(R.string.notification_turn_off),
                turnOffPendingIntent
            ).setOngoing(true).setOnlyAlertOnce(false)  // Allow heads-up to show again
            .setFullScreenIntent(turnOffPendingIntent, true) // Important for waking up
            .setContentIntent(turnOffPendingIntent).build()
    }

    override fun onDestroy() {
        super.onDestroy()
        isServiceRunning = false
        locationManager.removeUpdates(this)
        vibrator?.cancel()
        if (::geofencingClient.isInitialized) {
            geofencingClient.removeGeofences(getGeofencePendingIntent())
        }
        WakeLocker.release()
    }

    // Unused overrides
    override fun onProviderEnabled(provider: String) {}
    override fun onProviderDisabled(provider: String) {}
    override fun onStatusChanged(provider: String?, status: Int, extras: android.os.Bundle?) {}

    override fun onBind(intent: Intent?): IBinder? = null
}
