package com.example.geo_alarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class LiveNotificationHelper {
    companion object {
        const val NOTIFICATION_ID = 100
        const val CHANNEL_ID = "geo_alarm_live_channel"
        const val CHANNEL_NAME = "Geo Alarm Live"
        const val CHANNEL_DESCRIPTION = "Live progress notifications for geo alarms"
    }

    private var context: Context? = null

    fun initialize(context: Context) {
        this.context = context
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && context != null) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH // Required: Must NOT be IMPORTANCE_MIN
            ).apply {
                description = CHANNEL_DESCRIPTION
                setSound(null, null) // Disable default sound
                enableVibration(false) // We'll handle vibration manually
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                setBypassDnd(true) // Bypass Do Not Disturb

                // Android 16 specific channel settings for Live Updates
                if (Build.VERSION.SDK_INT >= 35) {
                    // Critical: Enable live updates on this channel
                    setAllowBubbles(false) // Don't conflict with live updates
                    setLightColor(android.graphics.Color.TRANSPARENT) // No LED
                    // Ensure channel allows ongoing notifications for Live Updates
                    setShowBadge(false) // Live updates don't need badges
                }
            }

            val notificationManager = context!!.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showLiveNotification(alarmName: String, distance: Double, progress: Int) {
        val context = this.context ?: return

        val distanceText = formatDistance(distance)
        val notification = buildNotification(context, alarmName, distanceText, progress, false)

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }

    fun updateLiveNotification(distance: Double, progress: Int, isArrived: Boolean = false) {
        val context = this.context ?: return

        val distanceText = if (isArrived) "已抵達目的地" else formatDistance(distance)
        val notification = buildNotification(context, "", distanceText, progress, isArrived)

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }

    private fun buildNotification(
        context: Context,
        alarmName: String,
        distanceText: String,
        progress: Int,
        isArrived: Boolean
    ): android.app.Notification {
        val title = if (alarmName.isNotEmpty()) alarmName else "定位鬧鐘"
        val contentText = if (isArrived) "$distanceText" else "$distanceText"

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.ic_dialog_alert) // Use system icon
            .setOngoing(true) // Makes notification non-removable
            .setAutoCancel(false) // Prevent auto cancel
            .setPriority(NotificationCompat.PRIORITY_MAX) // Highest priority
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC) // Show on lock screen
            .setShowWhen(true)
            .setWhen(System.currentTimeMillis())
            .setContentIntent(createOpenAppIntent(context))

        // Add progress bar
        builder.setProgress(100, progress, false)

        // For Android 10+ (API 29+), use foreground service type
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            builder.setCategory(NotificationCompat.CATEGORY_SERVICE)
        }

        return builder.build()
    }

    private fun createOpenAppIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun createDeleteIntent(context: Context): PendingIntent {
        val intent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = "NOTIFICATION_DISMISSED"
        }
        return PendingIntent.getBroadcast(
            context,
            1,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun createCloseAction(context: Context): NotificationCompat.Action {
        val intent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = "CLOSE_ALARM"
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Action.Builder(
            0,
            "關閉鬧鐘",
            pendingIntent
        ).build()
    }

    fun createForegroundNotification(title: String, distanceText: String, progress: Int, showCloseButton: Boolean = false): android.app.Notification {
        val context = this.context ?: throw IllegalStateException("Context not initialized")

        // Extract remaining distance for Status Chips
        val remainingDistance = extractDistanceFromText(distanceText)

        // Debug logging
        android.util.Log.d("LiveNotification", "Creating notification - API Level: ${Build.VERSION.SDK_INT}, Title: $title, Distance: $remainingDistance")

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(title) // Required: Must have contentTitle
            .setContentText(distanceText)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setOngoing(true) // Required: Must be ongoing (FLAG_ONGOING_EVENT)
            .setAutoCancel(false)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_NAVIGATION) // Use NAVIGATION for location-based updates
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setShowWhen(false) // Live updates typically don't show timestamp
            .setContentIntent(createOpenAppIntent(context)) // Click to open app
            .setDeleteIntent(createDeleteIntent(context)) // Detect when notification is dismissed
            .setStyle(null) // Use no style or ProgressStyle for live updates
            .setColorized(false) // Required: Must NOT be colorized
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE) // Critical for Live Updates

        // Add progress bar (ProgressStyle is supported for live updates) - MUST be added before Android 16 extras
        builder.setProgress(100, progress, false)

        // Required: Request promotion for Live Update according to official documentation
        if (Build.VERSION.SDK_INT >= 35) { // Android 16 (API 35)
            try {
                // According to official docs, use the correct Bundle extras key
                val extras = Bundle().apply {
                    // Use the official EXTRA_REQUEST_PROMOTED_ONGOING constant
                    putBoolean("android.app.Notification.EXTRA_REQUEST_PROMOTED_ONGOING", true)
                    // Status chip text - maximum 96dp width according to docs
                    putString("android.app.Notification.EXTRA_SHORT_CRITICAL_TEXT", remainingDistance)
                }
                builder.setExtras(extras)

                android.util.Log.d("LiveNotification", "Live Update (official extras) - distance: $remainingDistance, progress: $progress")
                android.util.Log.d("LiveNotification", "Extras applied: EXTRA_REQUEST_PROMOTED_ONGOING=true, EXTRA_SHORT_CRITICAL_TEXT=$remainingDistance")
            } catch (e: Exception) {
                android.util.Log.e("LiveNotification", "Failed to setup Live Update: ${e.message}")
            }
        }

        // Only add close button when arrived
        if (showCloseButton) {
            builder.addAction(createCloseAction(context))
        }

        return builder.build()
    }

    fun hideLiveNotification() {
        val context = this.context ?: return
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
    }

    private fun formatDistance(distance: Double): String {
        return if (distance >= 1000) {
            "距離目的地 ${String.format("%.2f", distance / 1000)} 公里"
        } else {
            "距離目的地 ${distance.toInt()} 公尺"
        }
    }

    private fun extractDistanceFromText(distanceText: String): String {
        // Extract short distance for Status Chips
        return when {
            distanceText.contains("已抵達") -> "已抵達"
            distanceText.contains("公里") -> {
                val regex = Regex("""(\d+\.?\d*) 公里""")
                val match = regex.find(distanceText)
                "${match?.groupValues?.get(1) ?: "0"}km"
            }
            distanceText.contains("公尺") -> {
                val regex = Regex("""(\d+) 公尺""")
                val match = regex.find(distanceText)
                "${match?.groupValues?.get(1) ?: "0"}m"
            }
            else -> "監控中"
        }
    }
}