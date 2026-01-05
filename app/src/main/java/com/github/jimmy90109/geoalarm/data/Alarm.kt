package com.github.jimmy90109.geoalarm.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "alarms")
data class Alarm(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val radius: Double,
    val isEnabled: Boolean
)
