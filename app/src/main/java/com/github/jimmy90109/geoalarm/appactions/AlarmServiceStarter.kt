package com.github.jimmy90109.geoalarm.appactions

import com.github.jimmy90109.geoalarm.data.Alarm

interface AlarmServiceStarter {
    fun stopCurrentAlarm()
    fun startAlarm(alarm: Alarm)
}
