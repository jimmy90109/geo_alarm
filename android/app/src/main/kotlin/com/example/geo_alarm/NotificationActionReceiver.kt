package com.example.geo_alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat

class NotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            "CLOSE_ALARM" -> {
                handleCloseAlarm(context)
            }
            "NOTIFICATION_DISMISSED" -> {
                handleNotificationDismissed(context)
            }
        }
    }

    private fun handleCloseAlarm(context: Context) {
        // Hide the live notification
        NotificationManagerCompat.from(context).cancel(LiveNotificationHelper.NOTIFICATION_ID)

        // Stop vibration if active
        try {
            val vibrationHelper = VibrationHelper()
            vibrationHelper.stopVibration(context)
        } catch (e: Exception) {
            // Ignore vibration stop errors
        }

        // Notify Flutter about alarm closure through MainActivity's MethodChannel
        try {
            MainActivity.notifyFlutterAlarmClosed()
        } catch (e: Exception) {
            // MainActivity might not be active, ignore error
        }
    }

    private fun handleNotificationDismissed(context: Context) {
        // Log that the Live Update was dismissed - as recommended by official docs
        android.util.Log.i("LiveNotification", "Live Update notification was dismissed by user")

        // According to official docs, avoid reposting dismissed Live Updates
        // We should stop the alarm monitoring when user dismisses
        try {
            MainActivity.notifyFlutterAlarmClosed()
        } catch (e: Exception) {
            // MainActivity might not be active, ignore error
        }
    }
}