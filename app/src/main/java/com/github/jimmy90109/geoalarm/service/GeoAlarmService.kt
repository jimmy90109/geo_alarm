package com.github.jimmy90109.geoalarm.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.res.Configuration
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import com.github.jimmy90109.geoalarm.ArrivalAlarmActivity
import androidx.core.app.NotificationCompat
import com.github.jimmy90109.geoalarm.GeoAlarmApplication
import com.github.jimmy90109.geoalarm.MainActivity
import com.github.jimmy90109.geoalarm.R
import com.github.jimmy90109.geoalarm.data.DistanceUnitPreference
import com.github.jimmy90109.geoalarm.utils.AudioUtils
import com.github.jimmy90109.geoalarm.utils.DistanceFormatter
import com.github.jimmy90109.geoalarm.utils.DistanceUnitResolver
import com.github.jimmy90109.geoalarm.utils.HyperIslandHelper
import com.github.jimmy90109.geoalarm.utils.WakeLocker
import com.github.jimmy90109.geoalarm.widget.GeoAlarmGlanceWidget
import com.github.jimmy90109.geoalarm.widget.GeoAlarmWidgetRuntimeStore
import androidx.glance.appwidget.updateAll
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import android.media.MediaPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class GeoAlarmService : Service() {

    companion object {
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        const val ACTION_TEST = "ACTION_TEST"
        const val ACTION_GEOFENCE_TRIGGERED = "ACTION_GEOFENCE_TRIGGERED"
        const val ACTION_NOTIFICATION_DISMISSED = "ACTION_NOTIFICATION_DISMISSED"
        const val ACTION_WARNING_GEOFENCE_TRIGGERED = "ACTION_WARNING_GEOFENCE_TRIGGERED"

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

        // UI Update Broadcast
        const val ACTION_PROGRESS_UPDATE = "com.github.jimmy90109.geoalarm.ACTION_PROGRESS_UPDATE"
        const val EXTRA_PROGRESS = "EXTRA_PROGRESS"
        const val EXTRA_REMAINING_DISTANCE = "EXTRA_REMAINING_DISTANCE"
        const val EXTRA_CANCEL_SOURCE = "EXTRA_CANCEL_SOURCE"
        const val CANCEL_SOURCE_ARRIVAL_TURN_OFF = "CANCEL_SOURCE_ARRIVAL_TURN_OFF"

        private const val LIVE_UPDATE_NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "geo_alarm_channel"

        // Distance thresholds for adaptive monitoring
        const val FAR_DISTANCE_THRESHOLD = 5000f   // > 5km: Geofence only (power saving)
        const val NEAR_DISTANCE_THRESHOLD = 2000f  // <= 2km: High accuracy GPS

        // GPS update intervals
        const val MID_UPDATE_INTERVAL = 60_000L   // 1 minute for 2-5km range
        const val NEAR_UPDATE_INTERVAL = 15_000L  // 15 seconds for <= 2km range

        // Geofence IDs
        const val GEOFENCE_DESTINATION_ID = "dest_geofence"
        const val GEOFENCE_WARNING_ID = "warning_5km_geofence"
    }

    // Monitoring zones for adaptive tracking
    enum class MonitoringZone { FAR, MID, NEAR }

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var geofencingClient: GeofencingClient
    private var locationCallback: LocationCallback? = null

    private val settingsRepository by lazy { (application as GeoAlarmApplication).settingsRepository }
    private val repository by lazy { (application as GeoAlarmApplication).repository }
    private var isServiceRunning = false
    private var currentZone = MonitoringZone.FAR
    private var distanceUnitPreference = DistanceUnitPreference.AUTO
    private var latestProgress = 0
    private var latestRemainingDistance = 0

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
    private var mediaPlayer: MediaPlayer? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private val geofenceActionMutex = Mutex()
    private var testJob: Job? = null  // For test mode

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        geofencingClient = LocationServices.getGeofencingClient(this)

        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION") getSystemService(VIBRATOR_SERVICE) as Vibrator
        }
        serviceScope.launch {
            settingsRepository.distanceUnitPreferenceFlow.collect { preference ->
                val changed = preference != distanceUnitPreference
                distanceUnitPreference = preference
                if (changed && isServiceRunning && !isArrived) {
                    updateNotificationForZone(
                        currentZone,
                        latestProgress,
                        latestRemainingDistance,
                    )
                }
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (isServiceRunning && !isArrived) {
            updateNotificationForZone(currentZone, latestProgress, latestRemainingDistance)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (GeoAlarmServiceStartPolicy.requiresImmediateForeground(intent?.action) && !isServiceRunning) {
            startForegroundService()
        }

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

                    // totalDistance will be set on first GPS update
                    totalDistance = 0f

                    startForegroundService()
                    GeoAlarmWidgetRuntimeStore.saveProgress(this, 0, -1)
                    CoroutineScope(Dispatchers.IO).launch { GeoAlarmGlanceWidget().updateAll(this@GeoAlarmService) }
                    startSmartHybridMonitoring()
                    isServiceRunning = true
                }
            }

            ACTION_GEOFENCE_TRIGGERED -> {
                handleGeofenceAction(ACTION_GEOFENCE_TRIGGERED)
            }

            ACTION_WARNING_GEOFENCE_TRIGGERED -> {
                handleGeofenceAction(ACTION_WARNING_GEOFENCE_TRIGGERED)
            }

            ACTION_NOTIFICATION_DISMISSED -> {
                if (isServiceRunning) {
                    if (isArrived) {
                        // Arrival notification dismissed, re-push it so user can turn off vibration/ringtone
                        updateForegroundNotification(buildArrivalNotification())
                    } else {
                        // Zone notification dismissed, restore foreground service
                        startForegroundService()
                    }
                }
                WakeLocker.release()
            }

            ACTION_STOP -> {
                cleanUpAndStop(
                    intent.getStringExtra(EXTRA_ALARM_ID).orEmpty().ifEmpty { alarmId }
                )
            }

            ACTION_TEST -> {
                if (!isServiceRunning) {
                    alarmId = intent.getStringExtra(EXTRA_ALARM_ID) ?: "test"
                    alarmName = intent.getStringExtra(EXTRA_NAME) ?: "Test Alarm"
                    destLat = 0.0
                    destLng = 0.0
                    radius = 100.0
                    startLat = 0.0
                    startLng = 0.0
                    totalDistance = 1000f

                    startForegroundService()
                    isServiceRunning = true

                    // Simulate progress updates and arrival after 10 seconds
                    testJob = serviceScope.launch {
                        for (i in 1..10) {
                            delay(1000)
                            val progress = i * 10
                            val remaining = ((10 - i) * 100)
                            if (remaining <= 0) {
                                isArrived = true
                                triggerArrival()
                                return@launch
                            }
                            broadcastProgress(progress, remaining)
                            
                            // Update notification with progress
                            val zone = when {
                                progress < 30 -> MonitoringZone.FAR
                                progress < 70 -> MonitoringZone.MID
                                else -> MonitoringZone.NEAR
                            }
                            updateNotificationForZone(zone, progress, remaining)
                        }
                    }
                }
            }
        }
        return START_STICKY
    }

    private fun handleGeofenceAction(action: String) {
        serviceScope.launch {
            geofenceActionMutex.withLock {
                if (!isServiceRunning) {
                    val activeAlarm = withContext(Dispatchers.IO) {
                        runCatching {
                            GeoAlarmServiceStartPolicy.selectActiveAlarm(
                                repository.getAllAlarmsOneShot()
                            )
                        }.onFailure { error ->
                            Log.e("GeoAlarmService", "Failed to restore active alarm", error)
                        }.getOrNull()
                    }
                    if (activeAlarm == null) {
                        Log.w(
                            "GeoAlarmService",
                            "Ignoring stale geofence action because there is no unique active alarm",
                        )
                        cleanUpAndStop(alarmId)
                        return@withLock
                    }

                    alarmId = activeAlarm.id
                    alarmName = activeAlarm.name
                    destLat = activeAlarm.latitude
                    destLng = activeAlarm.longitude
                    radius = activeAlarm.radius
                    isServiceRunning = true

                    // Replace the temporary foreground notification with restored alarm details.
                    startForegroundService()
                }

                when (action) {
                    ACTION_GEOFENCE_TRIGGERED -> {
                        if (!isArrived) {
                            isArrived = true
                            triggerArrival()
                        }
                    }

                    ACTION_WARNING_GEOFENCE_TRIGGERED -> {
                        Log.d(
                            "GeoAlarmService",
                            "Warning geofence triggered, switching to MID zone",
                        )
                        if (!isArrived && currentZone == MonitoringZone.FAR) {
                            switchToZone(MonitoringZone.MID)
                        }
                    }
                }
            }
        }
    }

    private fun cleanUpAndStop(stoppedAlarmId: String) {
        testJob?.cancel()
        serviceScope.cancel()
        stopGpsUpdates()
        removeAllGeofences()
        vibrator?.cancel()
        mediaPlayer?.apply {
            if (isPlaying) stop()
            release()
        }
        mediaPlayer = null
        AudioUtils.abandonAudioFocus(this)
        WakeLocker.release()
        GeoAlarmWidgetRuntimeStore.clear(this)
        CoroutineScope(Dispatchers.IO).launch {
            GeoAlarmGlanceWidget().updateAll(this@GeoAlarmService)
        }
        stopForeground(STOP_FOREGROUND_REMOVE)
        sendBroadcast(
            Intent(GeoAlarmContract.ACTION_ALARM_STOPPED).apply {
                setPackage(packageName)
                putExtra(EXTRA_ALARM_ID, stoppedAlarmId)
            }
        )
        stopSelf()
    }

    /**
     * Start the smart hybrid monitoring system:
     * 1. Always register destination geofence as "safety net"
     * 2. Register 5km warning geofence for FAR->MID transition
     * 3. Get initial location to determine starting zone
     */
    @SuppressLint("MissingPermission")
    private fun startSmartHybridMonitoring() {
        // Register destination geofence (always active as backup)
        registerDestinationGeofence()

        // Register 5km warning geofence
        registerWarningGeofence()

        // Get initial location to determine starting zone
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                val distance = calculateDistanceToDestination(location)
                Log.d("GeoAlarmService", "Initial distance: ${distance}m")

                val initialZone = determineZone(distance)
                switchToZone(initialZone)

                // Set initial total distance for progress calculation (Distance - Radius)
                // This ensures we start at 0% progress relative to the perimeter
                val remainingDist = distance - radius.toFloat()
                if (totalDistance == 0f || remainingDist > totalDistance) {
                    totalDistance = remainingDist
                }
            } else {
                // No last known location, start with FAR zone (power saving)
                Log.d("GeoAlarmService", "No last known location, starting in FAR zone")
                switchToZone(MonitoringZone.FAR)
            }
        }.addOnFailureListener { e ->
            Log.e("GeoAlarmService", "Failed to get last location", e)
            switchToZone(MonitoringZone.FAR)
        }
    }

    private fun determineZone(distance: Float): MonitoringZone {
        return when {
            distance > FAR_DISTANCE_THRESHOLD -> MonitoringZone.FAR
            distance > NEAR_DISTANCE_THRESHOLD -> MonitoringZone.MID
            else -> MonitoringZone.NEAR
        }
    }

    /**
     * Switch to a new monitoring zone, updating GPS settings accordingly
     */
    private fun switchToZone(newZone: MonitoringZone) {
        if (newZone == currentZone && locationCallback != null) {
            return // Already in this zone with active monitoring
        }

        Log.d("GeoAlarmService", "Switching from $currentZone to $newZone")
        currentZone = newZone

        when (newZone) {
            MonitoringZone.FAR -> {
                // Power saving mode - GPS off, rely on geofences
                stopGpsUpdates()
                updateNotificationForZone(MonitoringZone.FAR, 0, 0)
                broadcastProgress(0, -1) // Signal FAR state to UI
            }

            MonitoringZone.MID -> {
                // Balanced mode - GPS at 1 minute interval
                startGpsUpdates(
                    interval = MID_UPDATE_INTERVAL,
                    priority = Priority.PRIORITY_BALANCED_POWER_ACCURACY
                )
            }

            MonitoringZone.NEAR -> {
                // High accuracy mode - GPS at 15 second interval + WakeLock
                WakeLocker.acquire(this)
                startGpsUpdates(
                    interval = NEAR_UPDATE_INTERVAL, priority = Priority.PRIORITY_HIGH_ACCURACY
                )
            }
        }
    }

    /**
     * Update location request dynamically based on current distance
     */
    private fun updateLocationRequest(distance: Float) {
        val newZone = determineZone(distance)
        if (newZone != currentZone) {
            switchToZone(newZone)
        }
    }

    @SuppressLint("MissingPermission")
    private fun registerDestinationGeofence() {
        val geofence = Geofence.Builder().setRequestId(GEOFENCE_DESTINATION_ID)
            .setCircularRegion(destLat, destLng, radius.toFloat())
            .setExpirationDuration(Geofence.NEVER_EXPIRE).setNotificationResponsiveness(0)
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER).build()

        val geofencingRequest =
            GeofencingRequest.Builder().setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                .addGeofence(geofence).build()

        geofencingClient.addGeofences(geofencingRequest, getDestinationGeofencePendingIntent())
            .addOnSuccessListener {
                Log.d("GeoAlarmService", "Destination geofence added (radius: ${radius}m)")
            }.addOnFailureListener { e ->
                Log.e("GeoAlarmService", "Destination geofence failed", e)
            }
    }

    @SuppressLint("MissingPermission")
    private fun registerWarningGeofence() {
        val geofence = Geofence.Builder().setRequestId(GEOFENCE_WARNING_ID)
            .setCircularRegion(destLat, destLng, FAR_DISTANCE_THRESHOLD)
            .setExpirationDuration(Geofence.NEVER_EXPIRE).setNotificationResponsiveness(0)
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER).build()

        val geofencingRequest =
            GeofencingRequest.Builder().setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                .addGeofence(geofence).build()

        geofencingClient.addGeofences(geofencingRequest, getWarningGeofencePendingIntent())
            .addOnSuccessListener {
                Log.d("GeoAlarmService", "Warning geofence added (5km radius)")
            }.addOnFailureListener { e ->
                Log.e("GeoAlarmService", "Warning geofence failed", e)
            }
    }

    private fun getDestinationGeofencePendingIntent(): PendingIntent {
        val intent = Intent(this, GeofenceBroadcastReceiver::class.java).apply {
            action = ACTION_GEOFENCE_TRIGGERED
        }
        return PendingIntent.getBroadcast(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
        )
    }

    private fun getWarningGeofencePendingIntent(): PendingIntent {
        val intent = Intent(this, GeofenceBroadcastReceiver::class.java).apply {
            action = ACTION_WARNING_GEOFENCE_TRIGGERED
        }
        return PendingIntent.getBroadcast(
            this,
            1,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
        )
    }

    private fun removeAllGeofences() {
        if (::geofencingClient.isInitialized) {
            geofencingClient.removeGeofences(getDestinationGeofencePendingIntent())
            geofencingClient.removeGeofences(getWarningGeofencePendingIntent())
        }
    }

    @SuppressLint("MissingPermission")
    private fun startGpsUpdates(interval: Long, priority: Int) {
        // Stop any existing updates first
        stopGpsUpdates()

        val locationRequest =
            LocationRequest.Builder(priority, interval).setMinUpdateIntervalMillis(interval / 2)
                .setWaitForAccurateLocation(false).build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    onLocationChanged(location)
                }
            }
        }

        fusedLocationClient.requestLocationUpdates(
            locationRequest, locationCallback!!, Looper.getMainLooper()
        )

        Log.d("GeoAlarmService", "GPS updates started: interval=${interval}ms, priority=$priority")
    }

    private fun stopGpsUpdates() {
        locationCallback?.let {
            fusedLocationClient.removeLocationUpdates(it)
            locationCallback = null
            Log.d("GeoAlarmService", "GPS updates stopped")
        }
    }

    private fun calculateDistanceToDestination(location: Location): Float {
        val destLocation = Location("").apply {
            latitude = destLat
            longitude = destLng
        }
        return location.distanceTo(destLocation)
    }

    private fun onLocationChanged(location: Location) {
        val distanceToDest = calculateDistanceToDestination(location)
        val remainingDist = distanceToDest - radius.toFloat()
        val remainingMeters = remainingDist.toInt()

        if (remainingDist <= 0) {
            // Arrived!
            if (!isArrived) {
                isArrived = true
                triggerArrival()
            }
        } else {
            // Set totalDistance on first GPS update, or update if user moved further away
            if (totalDistance == 0f || remainingDist > totalDistance) {
                totalDistance = remainingDist
            }

            // Calculate progress
            val progressFraction = 1.0f - (remainingDist / totalDistance)
            val progressPercent = (progressFraction * 100).toInt().coerceIn(0, 100)

            Log.d(
                "GeoAlarmService",
                "Zone: $currentZone, Distance: ${distanceToDest.toInt()}m, Remaining: ${remainingMeters}m, Progress: $progressPercent%"
            )

            // Update zone based on current distance
            updateLocationRequest(remainingDist)

            // Update notification with progress
            updateNotificationForZone(currentZone, progressPercent, remainingMeters)

            // Broadcast to UI
            broadcastProgress(progressPercent, remainingMeters)
        }
    }

    @SuppressLint("MissingPermission")
    private fun startForegroundService() {
        latestProgress = 0
        latestRemainingDistance = 0
        val notification = buildZoneNotification(currentZone, 0, 0)
        updateForegroundNotification(notification)
    }

    @SuppressLint("MissingPermission")
    private fun updateForegroundNotification(notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(LIVE_UPDATE_NOTIFICATION_ID, notification, FOREGROUND_SERVICE_TYPE_LOCATION)
        } else {
            startForeground(LIVE_UPDATE_NOTIFICATION_ID, notification)
        }
    }

    private fun updateNotificationForZone(
        zone: MonitoringZone, progress: Int, remainingDistance: Int
    ) {
        if (isArrived) return

        currentZone = zone
        latestProgress = progress
        latestRemainingDistance = remainingDistance
        val notification = buildZoneNotification(zone, progress, remainingDistance)
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(LIVE_UPDATE_NOTIFICATION_ID, notification)
    }

    private fun triggerArrival() {
        // Stop GPS updates
        stopGpsUpdates()

        // Broadcast Arrival State (100% progress, 0 distance)
        broadcastProgress(100, 0)

        // Vibrate
        val vibrationPattern = longArrayOf(0, 500, 200, 500) // wait 0, vib 500, sleep 200, vib 500
        vibrator?.vibrate(
            VibrationEffect.createWaveform(
                vibrationPattern,
                0,
            )
        ) // 0 means repeat at index 0

        // Play ringtone based on settings
        serviceScope.launch {
            val ringtoneSettings = settingsRepository.ringtoneSettingsFlow.first()
            if (ringtoneSettings.enabled) {
                if (AudioUtils.isHeadphoneConnected(this@GeoAlarmService)) {
                    // Headphones connected - play via media channel
                    mediaPlayer = AudioUtils.playRingtoneViaMedia(this@GeoAlarmService, ringtoneSettings.ringtoneUri)
                    Log.d("GeoAlarmService", "Playing ringtone via headphones")
                } else {
                    // No headphones - vibration only (already triggered above)
                    Log.d("GeoAlarmService", "No headphones connected, vibration only")
                }
            }
            // When disabled: vibration only (already triggered above)
        }

        updateForegroundNotification(buildArrivalNotification())

        // Use WakeLock to turn screen on if possible
        WakeLocker.acquire(this)
    }

    @SuppressLint("StringFormatInvalid")
    private fun buildZoneNotification(
        zone: MonitoringZone, progress: Int, remainingDistance: Int
    ): Notification {
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

        val deleteIntent = Intent(this, GeoAlarmService::class.java).apply {
            action = ACTION_NOTIFICATION_DISMISSED
        }
        val deletePendingIntent = PendingIntent.getService(
            this,
            0,
            deleteIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val contentIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            this,
            1,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val progressStyle = NotificationCompat.ProgressStyle()
        val builder =
            NotificationCompat.Builder(this, CHANNEL_ID).setSmallIcon(R.drawable.ic_notification)
                .setOnlyAlertOnce(true).setOngoing(true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC) // Show on lock screen
                .setCategory(NotificationCompat.CATEGORY_NAVIGATION) // Treat as navigation/live activity
                .setRequestPromotedOngoing(true)
                .setContentIntent(contentPendingIntent).setDeleteIntent(deletePendingIntent)
                .addAction(
                    android.R.drawable.ic_menu_close_clear_cancel,
                    getString(R.string.notification_cancel),
                    cancelPendingIntent,
                )

        val distanceUnitSystem = DistanceUnitResolver.resolve(this, distanceUnitPreference)
        val displayLocale = resources.configuration.locales[0]
        val formattedRemainingDistance = DistanceFormatter.format(
            context = this,
            distanceMeters = remainingDistance.toDouble(),
            system = distanceUnitSystem,
            locale = displayLocale,
        )
        val formattedFarThreshold = getString(
            R.string.distance_greater_than,
            DistanceFormatter.format(
                context = this,
                distanceMeters = FAR_DISTANCE_THRESHOLD.toDouble(),
                system = distanceUnitSystem,
                locale = displayLocale,
            ),
        )

        when (zone) {
            MonitoringZone.FAR -> {
                progressStyle.setProgressIndeterminate(true)
                builder.setProgress(0, 0, true)
                    .setContentTitle(getString(R.string.notification_title))
                    .setContentText(getString(R.string.notification_power_saving))
                    .setShortCriticalText(formattedFarThreshold)
            }

            MonitoringZone.MID -> {
                progressStyle.setProgress(progress)
                builder.setProgress(100, progress, false)
                    .setContentTitle(getString(R.string.notification_title))
                    .setContentText(
                        getString(
                            R.string.notification_distance,
                            formattedRemainingDistance,
                            progress,
                        )
                    ).setSubText(getString(R.string.notification_balanced))
                    .setShortCriticalText("$progress%")
            }

            MonitoringZone.NEAR -> {
                progressStyle.setProgress(progress)
                builder.setProgress(100, progress, false)
                    .setContentTitle(getString(R.string.notification_title))
                    .setContentText(
                        getString(
                            R.string.notification_distance,
                            formattedRemainingDistance,
                            progress,
                        )
                    ).setSubText(getString(R.string.notification_high_accuracy))
                    .setShortCriticalText("$progress%")
            }
        }
        builder.setStyle(progressStyle)

        // Apply Xiaomi HyperOS Dynamic Island extras if supported
        HyperIslandHelper.applyProgressExtras(
            this,
            builder,
            progress,
            zone,
            cancelPendingIntent,
            formattedRemainingDistance,
        )

        val notification = builder.build()
        logPromotedOngoingEligibility(notification)
        return notification
    }

    private fun buildArrivalNotification(): Notification {
        val turnOffIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            action = ACTION_CANCEL_ALARM
            putExtra(EXTRA_ALARM_ID, alarmId)
            putExtra(EXTRA_CANCEL_SOURCE, CANCEL_SOURCE_ARRIVAL_TURN_OFF)
        }
        val turnOffPendingIntent = PendingIntent.getActivity(
            this,
            1,
            turnOffIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val showArrivalIntent = Intent(this, ArrivalAlarmActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_ALARM_ID, alarmId)
        }
        val showArrivalPendingIntent = PendingIntent.getActivity(
            this,
            3,
            showArrivalIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        // Delete intent for when notification is swiped away
        val deleteIntent = Intent(this, GeoAlarmService::class.java).apply {
            action = ACTION_NOTIFICATION_DISMISSED
        }
        val deletePendingIntent = PendingIntent.getService(
            this,
            2, // Different request code from zone notification
            deleteIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notificationManager = getSystemService(NotificationManager::class.java)
        val canUseFullScreenIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            notificationManager.canUseFullScreenIntent()
        } else {
            true
        }
        Log.d("GeoAlarmService", "Arrival canUseFullScreenIntent=$canUseFullScreenIntent")

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_arrived_title))
            .setContentText(getString(R.string.notification_arrived_text))
            .setSmallIcon(R.drawable.ic_notification).setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM).addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                getString(R.string.notification_turn_off),
                turnOffPendingIntent
            ).setOngoing(true).setOnlyAlertOnce(false)
            .setContentIntent(turnOffPendingIntent)
            .setDeleteIntent(deletePendingIntent)

        if (canUseFullScreenIntent) {
            builder.setFullScreenIntent(showArrivalPendingIntent, true)
        }

        // Apply Xiaomi HyperOS Dynamic Island extras if supported
        HyperIslandHelper.applyArrivalExtras(this, builder, turnOffPendingIntent)

        return builder.build()
    }

    private fun logPromotedOngoingEligibility(notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
            val notificationManager = getSystemService(NotificationManager::class.java)
            Log.d(
                "GeoAlarmService",
                "Live Update promotable=${notification.hasPromotableCharacteristics()}, canPostPromoted=${notificationManager.canPostPromotedNotifications()}",
            )
        }
    }

    private fun broadcastProgress(progress: Int, remainingDistance: Int) {
        GeoAlarmWidgetRuntimeStore.saveProgress(this, progress, remainingDistance)
        CoroutineScope(Dispatchers.IO).launch { GeoAlarmGlanceWidget().updateAll(this@GeoAlarmService) }
        val intent = Intent(ACTION_PROGRESS_UPDATE).apply {
            setPackage(packageName) // Explicitly set package for security and receiver matching
            putExtra(EXTRA_PROGRESS, progress)
            putExtra(EXTRA_REMAINING_DISTANCE, remainingDistance)
        }
        sendBroadcast(intent)
        Log.d("GeoAlarmService", "Broadcasting progress: $progress%, $remainingDistance m")
    }

    override fun onDestroy() {
        super.onDestroy()
        isServiceRunning = false
        stopGpsUpdates()
        removeAllGeofences()
        vibrator?.cancel()
        mediaPlayer?.apply {
            if (isPlaying) stop()
            release()
        }
        mediaPlayer = null
        serviceScope.cancel() // Cancel all coroutines
        WakeLocker.release()
        GeoAlarmWidgetRuntimeStore.clear(this)
        CoroutineScope(Dispatchers.IO).launch { GeoAlarmGlanceWidget().updateAll(this@GeoAlarmService) }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
