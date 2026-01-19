package com.github.jimmy90109.geoalarm.data

import androidx.room.Embedded
import androidx.room.Relation

data class ScheduleWithAlarm(
    @Embedded val schedule: AlarmSchedule,
    @Relation(
        parentColumn = "alarmId",
        entityColumn = "id"
    )
    val alarm: Alarm
)
