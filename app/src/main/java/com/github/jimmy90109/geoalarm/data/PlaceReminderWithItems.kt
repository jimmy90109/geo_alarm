package com.github.jimmy90109.geoalarm.data

import androidx.room.Embedded
import androidx.room.Relation

data class PlaceReminderWithItems(
    @Embedded val reminder: PlaceReminder,
    @Relation(
        parentColumn = "id",
        entityColumn = "reminderId"
    )
    val items: List<PlaceReminderItem>
) {
    val sortedItems: List<PlaceReminderItem>
        get() = items.sortedBy { it.sortOrder }
}
