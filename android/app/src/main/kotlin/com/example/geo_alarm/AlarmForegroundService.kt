package com.example.geo_alarm

import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder

class AlarmForegroundService : Service() {
    companion object {
        const val ACTION_START_FOREGROUND = "START_FOREGROUND"
        const val ACTION_UPDATE_NOTIFICATION = "UPDATE_NOTIFICATION"
        const val ACTION_STOP_FOREGROUND = "STOP_FOREGROUND"
        private var isRunning = false
        private var serviceInstance: AlarmForegroundService? = null
    }

    private lateinit var liveNotificationHelper: LiveNotificationHelper

    override fun onCreate() {
        super.onCreate()
        liveNotificationHelper = LiveNotificationHelper()
        liveNotificationHelper.initialize(this)
        serviceInstance = this
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_FOREGROUND -> {
                val alarmName = intent.getStringExtra("alarmName") ?: "地理鬧鐘"
                val distance = intent.getDoubleExtra("distance", 0.0)
                val progress = intent.getIntExtra("progress", 0)

                startForegroundService(alarmName, distance, progress)
                isRunning = true
            }
            ACTION_UPDATE_NOTIFICATION -> {
                val distance = intent.getDoubleExtra("distance", 0.0)
                val progress = intent.getIntExtra("progress", 0)
                val isArrived = intent.getBooleanExtra("isArrived", false)

                updateNotification(distance, progress, isArrived)
            }
            ACTION_STOP_FOREGROUND -> {
                stopForegroundService()
                isRunning = false
            }
        }
        return START_STICKY // Restart if killed
    }

    private fun startForegroundService(alarmName: String, distance: Double, progress: Int) {
        val distanceText = formatDistance(distance)
        val notification = liveNotificationHelper.createForegroundNotification(
            alarmName,
            distanceText,
            progress,
            showCloseButton = false // No close button during travel
        )
        startForeground(LiveNotificationHelper.NOTIFICATION_ID, notification)
    }

    private fun updateNotification(distance: Double, progress: Int, isArrived: Boolean = false) {
        if (isRunning) {
            val distanceText = if (isArrived) "🎯 已抵達目的地" else formatDistance(distance)
            val notification = liveNotificationHelper.createForegroundNotification(
                "",
                distanceText,
                progress,
                showCloseButton = isArrived // Show close button only when arrived
            )

            // Update the foreground notification
            startForeground(LiveNotificationHelper.NOTIFICATION_ID, notification)

            // Additional Android 16 debug
            if (Build.VERSION.SDK_INT >= 35) {
                android.util.Log.d("AlarmService", "Updated foreground notification for Android 16, isArrived: $isArrived")
            }
        }
    }

    private fun stopForegroundService() {
        stopForeground(true)
        stopSelf()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        serviceInstance = null
    }

    private fun formatDistance(distance: Double): String {
        return if (distance >= 1000) {
            "距離目的地 ${String.format("%.2f", distance / 1000)} 公里"
        } else {
            "距離目的地 ${distance.toInt()} 公尺"
        }
    }
}