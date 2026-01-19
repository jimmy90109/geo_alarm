package com.github.jimmy90109.geoalarm.service

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.github.jimmy90109.geoalarm.GeoAlarmApplication
import com.github.jimmy90109.geoalarm.R
import com.github.jimmy90109.geoalarm.data.AlarmRepository
import com.github.jimmy90109.geoalarm.data.AlarmSchedule
import com.github.jimmy90109.geoalarm.receiver.ScheduleReceiver
import com.github.jimmy90109.geoalarm.MainActivity
import java.util.Calendar

class ScheduleManager(private val context: Context) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val repository: AlarmRepository
        get() = (context.applicationContext as GeoAlarmApplication).repository

    companion object {
        const val ACTION_SCHEDULE_TRIGGER = "com.github.jimmy90109.geoalarm.ACTION_SCHEDULE_TRIGGER"
        const val EXTRA_SCHEDULE_ID = "schedule_id"
        const val CHANNEL_ID_SCHEDULE = "schedule_channel"
        const val NOTIFICATION_ID_BASE = 2000
    }

    suspend fun rescheduleAll() {
        // Fetch all enabled schedules and set them
        // Note: In real implementation we need to get list from DB.
        // For now, assuming we can get them. 
        // Since repository is flow, we might need a direct get or collect once.
        // Adding getEnabledSchedules() to Request? Or just collecting flow once.
        // Better: Dao method `getEnabledSchedules()`.
    }

    suspend fun setSchedule(schedule: AlarmSchedule) {
        if (!schedule.isEnabled) {
            cancelSchedule(schedule)
            return
        }

        // Calculate next occurrence
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, schedule.hour)
            set(Calendar.MINUTE, schedule.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // Find the next day in daysOfWeek
        // ... Logic to find next trigger time ...
        // Simplification for now: trigger next occurrence if today is in set, otherwise next.
        
        // Check if no days selected
        if (schedule.daysOfWeek.isEmpty()) return

        val now = Calendar.getInstance()
        var nextTriggerTimeMillis: Long = -1

        // Sort days: 1 (Sun) to 7 (Sat)
        // We iterate through sorted days.
        // For each day, we create a candidate trigger time.
        // If the day is today, we check if time has passed.
        // If multiple candidates, pick nearest positive delta.
        
        // Simpler approach: Check next 8 days (covering full week cycle)
        for (i in 0..7) {
            val candidate = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, i)
                set(Calendar.HOUR_OF_DAY, schedule.hour)
                set(Calendar.MINUTE, schedule.minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            
            // Check if this candidate day is in the selected days
            // Calendar.DAY_OF_WEEK: Sun=1 ... Sat=7
            val dayOfWeek = candidate.get(Calendar.DAY_OF_WEEK)
            
            if (schedule.daysOfWeek.contains(dayOfWeek)) {
                if (candidate.timeInMillis > now.timeInMillis) {
                    nextTriggerTimeMillis = candidate.timeInMillis
                    break // Found the nearest future occurrence
                }
            }
        }

        if (nextTriggerTimeMillis != -1L) {
            val intent = Intent(context, ScheduleReceiver::class.java).apply {
                action = ACTION_SCHEDULE_TRIGGER
                putExtra(EXTRA_SCHEDULE_ID, schedule.id)
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                schedule.id.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Check for exact alarm permission on Android 12+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (!alarmManager.canScheduleExactAlarms()) {
                    Log.w("ScheduleManager", "Cannot schedule exact alarm: permission denied")
                    // Fallback to inexact alarm or just return? 
                    // For now, let's use setWindow as a fallback which doesn't require permission but is less precise
                    // or just return to avoid crash. 
                    // A better approach is to use setExact if possible, else setWindow.
                    // But setWindow also requires permission for exact-like behavior in some contexts? 
                    // Actually setWindow doesn't throw SecurityException.
                     alarmManager.setWindow(
                        AlarmManager.RTC_WAKEUP,
                        nextTriggerTimeMillis,
                        10 * 60 * 1000, // 10 minutes window
                        pendingIntent
                    )
                    Log.d("ScheduleManager", "Scheduled inexact (window) ${schedule.id} for $nextTriggerTimeMillis")
                    return
                }
            }

            // Use setExactAndAllowWhileIdle for reliable timing
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                nextTriggerTimeMillis,
                pendingIntent
            )
            Log.d("ScheduleManager", "Scheduled ${schedule.id} for $nextTriggerTimeMillis")
        }
    }

    fun cancelSchedule(schedule: AlarmSchedule) {
        val intent = Intent(context, ScheduleReceiver::class.java).apply {
            action = ACTION_SCHEDULE_TRIGGER
            putExtra(EXTRA_SCHEDULE_ID, schedule.id)
        }
        val pendingIntent = PendingIntent.getBroadcast(
             context,
             schedule.id.hashCode(),
             intent,
             PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    suspend fun handleScheduleTrigger(scheduleId: String) {
        val schedule = repository.getSchedule(scheduleId) ?: return
        if (!schedule.isEnabled) return

        // 1. Post Notification (Confirm to Enable)
        showNotification(schedule)

        // 2. Schedule Next Occurrence (Repeating logic or just one-time and reschedule?)
        // AlarmManager setExactAndAllowWhileIdle is essentially one-time, so we reschedule next.
        setSchedule(schedule)
    }

    private suspend fun showNotification(schedule: AlarmSchedule) {
        val alarm = repository.getAlarm(schedule.alarmId) ?: return

        createNotificationChannel()

        val intent = Intent(context, MainActivity::class.java).apply {
             flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
             action = "ENABLE_ALARM_FROM_SCHEDULE" // TODO: Handle this in MainActivity
             putExtra("ALARM_ID", alarm.id)
        }
        
        val pendingIntent = PendingIntent.getActivity(
             context, 
             0,
             intent,
             PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_SCHEDULE)
            .setSmallIcon(R.drawable.ic_notification) // Replace with proper icon
            .setContentTitle(context.getString(R.string.notification_schedule_title, alarm.name))
            .setContentText(context.getString(R.string.notification_schedule_message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID_BASE + schedule.id.hashCode(), notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Scheduled Alarms"
            val descriptionText = "Notifications for scheduled alarms"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID_SCHEDULE, name, importance).apply {
                description = descriptionText
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
