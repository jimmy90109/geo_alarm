package com.github.jimmy90109.geoalarm.data

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromIntSet(value: Set<Int>): String {
        return value.joinToString(",")
    }

    @TypeConverter
    fun toIntSet(value: String): Set<Int> {
        if (value.isEmpty()) return emptySet()
        return value.split(",").map { it.toInt() }.toSet()
    }

    @TypeConverter
    fun fromPlaceReminderType(value: PlaceReminderType): String = value.name

    @TypeConverter
    fun toPlaceReminderType(value: String): PlaceReminderType =
        PlaceReminderType.valueOf(value)

    @TypeConverter
    fun fromPlaceTriggerType(value: PlaceTriggerType): String = value.name

    @TypeConverter
    fun toPlaceTriggerType(value: String): PlaceTriggerType =
        PlaceTriggerType.valueOf(value)

    @TypeConverter
    fun fromPlaceReminderAttachmentType(value: PlaceReminderAttachmentType): String = value.name

    @TypeConverter
    fun toPlaceReminderAttachmentType(value: String): PlaceReminderAttachmentType =
        PlaceReminderAttachmentType.valueOf(value)
}
