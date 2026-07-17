package com.github.jimmy90109.geoalarm.appactions

import com.github.jimmy90109.geoalarm.data.Alarm

interface AlarmServiceStarter {
    suspend fun stopCurrentAlarm(alarmId: String): Boolean
    fun startAlarm(alarm: Alarm)
}
