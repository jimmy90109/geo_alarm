package com.github.jimmy90109.geoalarm.service

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.github.jimmy90109.geoalarm.MainActivity
import com.github.jimmy90109.geoalarm.R
import com.github.jimmy90109.geoalarm.data.PlaceReminderType
import com.github.jimmy90109.geoalarm.data.PlaceReminderWithItems
import kotlin.math.absoluteValue

class PlaceReminderNotifier(private val context: Context) {
    fun notify(reminderWithItems: PlaceReminderWithItems) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        createChannel()
        val reminder = reminderWithItems.reminder
        val pendingIntent = PendingIntent.getActivity(
            context,
            reminder.id.hashCode().absoluteValue,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                action = MainActivity.ACTION_OPEN_PLACE_REMINDER
                putExtra(MainActivity.EXTRA_PLACE_REMINDER_ID, reminder.id)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val pendingCount = reminderWithItems.sortedItems.count { !it.checked }
        val body = when (reminder.type) {
            PlaceReminderType.TEXT -> reminder.content.ifBlank { reminder.title }
            PlaceReminderType.CHECKLIST -> context.resources.getQuantityString(
                R.plurals.place_reminder_notification_checklist_body,
                pendingCount,
                pendingCount,
            )
        }
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.place_reminder_notification_title, reminder.placeName))
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        context.getSystemService(NotificationManager::class.java)
            .notify(reminder.id.hashCode().absoluteValue, notification)
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.place_reminder_channel_name),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = context.getString(R.string.place_reminder_channel_description)
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    companion object {
        private const val CHANNEL_ID = "place_reminder_channel"
    }
}
