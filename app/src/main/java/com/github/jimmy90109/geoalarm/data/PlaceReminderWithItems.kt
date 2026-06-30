package com.github.jimmy90109.geoalarm.data

import androidx.room.Embedded
import androidx.room.Relation

data class PlaceReminderWithItems(
    @Embedded val reminder: PlaceReminder,
    @Relation(
        parentColumn = "id",
        entityColumn = "reminderId"
    )
    val items: List<PlaceReminderItem>,
    @Relation(
        parentColumn = "id",
        entityColumn = "reminderId"
    )
    val attachments: List<PlaceReminderAttachment>
) {
    val sortedItems: List<PlaceReminderItem>
        get() = items.sortedBy { it.sortOrder }

    val sortedAttachments: List<PlaceReminderAttachment>
        get() = attachments.sortedBy { it.sortOrder }
}
