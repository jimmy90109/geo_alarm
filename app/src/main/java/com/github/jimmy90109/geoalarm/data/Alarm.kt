package com.github.jimmy90109.geoalarm.data

import androidx.room.Entity
import androidx.room.ColumnInfo
import androidx.room.PrimaryKey
import java.util.UUID

const val DEFAULT_ALARM_ICON_KEY = "location"

@Entity(tableName = "alarms")
data class Alarm(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val radius: Double,
    val isEnabled: Boolean,
    @ColumnInfo(defaultValue = "'$DEFAULT_ALARM_ICON_KEY'")
    val iconKey: String = DEFAULT_ALARM_ICON_KEY
)
