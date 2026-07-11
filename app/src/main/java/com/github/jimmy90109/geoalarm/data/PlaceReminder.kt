package com.github.jimmy90109.geoalarm.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

enum class PlaceReminderType {
    TEXT,
    CHECKLIST
}

enum class PlaceTriggerType {
    ENTER,
    DWELL
}

enum class PlaceReminderAttachmentType {
    IMAGE,
    VIDEO
}

@Entity(tableName = "place_reminders")
data class PlaceReminder(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val type: PlaceReminderType,
    @ColumnInfo(defaultValue = "''")
    val content: String = "",
    val placeName: String,
    val address: String?,
    @ColumnInfo(defaultValue = "'$DEFAULT_ALARM_ICON_KEY'")
    val iconKey: String = DEFAULT_ALARM_ICON_KEY,
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Int,
    val triggerType: PlaceTriggerType,
    val dwellMinutes: Int?,
    val cooldownMinutes: Int,
    val enabled: Boolean,
    val lastTriggeredAt: Long?,
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(
    tableName = "place_reminder_items",
    foreignKeys = [
        ForeignKey(
            entity = PlaceReminder::class,
            parentColumns = ["id"],
            childColumns = ["reminderId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["reminderId"]),
        Index(value = ["reminderId", "sortOrder"])
    ]
)
data class PlaceReminderItem(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val reminderId: String,
    val text: String,
    val checked: Boolean,
    val sortOrder: Int
)

@Entity(
    tableName = "place_reminder_attachments",
    foreignKeys = [
        ForeignKey(
            entity = PlaceReminder::class,
            parentColumns = ["id"],
            childColumns = ["reminderId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["reminderId"]),
        Index(value = ["reminderId", "sortOrder"])
    ]
)
data class PlaceReminderAttachment(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val reminderId: String,
    val type: PlaceReminderAttachmentType,
    val localPath: String,
    val mimeType: String,
    val displayName: String,
    val sizeBytes: Long,
    val durationMillis: Long?,
    val width: Int?,
    val height: Int?,
    val sortOrder: Int,
    val createdAt: Long
)
