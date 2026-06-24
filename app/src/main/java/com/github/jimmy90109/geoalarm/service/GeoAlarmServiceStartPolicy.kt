package com.github.jimmy90109.geoalarm.service

import com.github.jimmy90109.geoalarm.data.Alarm

internal object GeoAlarmServiceStartPolicy {
    fun requiresImmediateForeground(action: String?): Boolean =
        action == GeoAlarmService.ACTION_GEOFENCE_TRIGGERED ||
            action == GeoAlarmService.ACTION_WARNING_GEOFENCE_TRIGGERED

    fun selectActiveAlarm(alarms: List<Alarm>): Alarm? =
        alarms.filter { it.isEnabled }.singleOrNull()
}
