package com.github.jimmy90109.geoalarm.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "alarm_schedules",
    foreignKeys = [
        ForeignKey(
            entity = Alarm::class,
            parentColumns = ["id"],
            childColumns = ["alarmId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["alarmId"])]
)
data class AlarmSchedule(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val alarmId: String,
    val daysOfWeek: Set<Int>, // 1 = Sunday, 2 = Monday, ..., 7 = Saturday (Calendar constants)
    val hour: Int,
    val minute: Int,
    val isEnabled: Boolean = true
)
