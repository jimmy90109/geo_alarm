package com.github.jimmy90109.geoalarm.appactions

import com.github.jimmy90109.geoalarm.data.AlarmSchedule

interface ScheduleGateway {
    suspend fun setSchedule(schedule: AlarmSchedule)
}
