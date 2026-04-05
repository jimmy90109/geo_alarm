package com.github.jimmy90109.geoalarm.appactions

import android.content.Context
import android.content.Intent
import com.github.jimmy90109.geoalarm.data.Alarm
import com.github.jimmy90109.geoalarm.service.GeoAlarmService
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class AndroidAlarmServiceStarter @Inject constructor(
    @ApplicationContext private val context: Context
) : AlarmServiceStarter {

    override fun stopCurrentAlarm() {
        context.startService(
            Intent(context, GeoAlarmService::class.java).apply {
                action = GeoAlarmService.ACTION_STOP
            }
        )
    }

    override fun startAlarm(alarm: Alarm) {
        val serviceIntent = Intent(context, GeoAlarmService::class.java).apply {
            action = GeoAlarmService.ACTION_START
            putExtra(GeoAlarmService.EXTRA_ALARM_ID, alarm.id)
            putExtra(GeoAlarmService.EXTRA_NAME, alarm.name)
            putExtra(GeoAlarmService.EXTRA_DEST_LAT, alarm.latitude)
            putExtra(GeoAlarmService.EXTRA_DEST_LNG, alarm.longitude)
            putExtra(GeoAlarmService.EXTRA_RADIUS, alarm.radius)
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
    }
}
