package com.github.jimmy90109.geoalarm.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.github.jimmy90109.geoalarm.GeoAlarmApplication
import com.github.jimmy90109.geoalarm.service.PlaceReminderGeofenceManager

class PlaceReminderBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val app = context.applicationContext as GeoAlarmApplication
        PlaceReminderGeofenceManager(context, app.placeReminderRepository).syncEnabledReminders()
    }
}
