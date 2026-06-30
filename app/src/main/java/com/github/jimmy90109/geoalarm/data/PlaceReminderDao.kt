package com.github.jimmy90109.geoalarm.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaceReminderDao {
    @Transaction
    @Query("SELECT * FROM place_reminders ORDER BY updatedAt DESC")
    fun getAllReminders(): Flow<List<PlaceReminderWithItems>>

    @Transaction
    @Query("SELECT * FROM place_reminders WHERE enabled = 1 ORDER BY updatedAt DESC")
    suspend fun getEnabledRemindersOneShot(): List<PlaceReminderWithItems>

    @Transaction
    @Query("SELECT * FROM place_reminders WHERE id = :id")
    fun getReminderFlow(id: String): Flow<PlaceReminderWithItems?>

    @Transaction
    @Query("SELECT * FROM place_reminders WHERE id = :id")
    suspend fun getReminder(id: String): PlaceReminderWithItems?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertReminder(reminder: PlaceReminder)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertItems(items: List<PlaceReminderItem>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAttachments(attachments: List<PlaceReminderAttachment>)

    @Update
    suspend fun updateReminder(reminder: PlaceReminder)

    @Update
    suspend fun updateItem(item: PlaceReminderItem)

    @Update
    suspend fun updateAttachment(attachment: PlaceReminderAttachment)

    @Delete
    suspend fun deleteReminder(reminder: PlaceReminder)

    @Delete
    suspend fun deleteAttachment(attachment: PlaceReminderAttachment)

    @Query("DELETE FROM place_reminder_items WHERE reminderId = :reminderId")
    suspend fun deleteItemsForReminder(reminderId: String)

    @Query("DELETE FROM place_reminder_attachments WHERE reminderId = :reminderId")
    suspend fun deleteAttachmentsForReminder(reminderId: String)

    @Query("SELECT * FROM place_reminder_attachments WHERE reminderId = :reminderId")
    suspend fun getAttachmentsForReminder(reminderId: String): List<PlaceReminderAttachment>

    @Query("SELECT * FROM place_reminder_attachments WHERE id = :attachmentId")
    suspend fun getAttachment(attachmentId: String): PlaceReminderAttachment?

    @Query("UPDATE place_reminders SET enabled = :enabled, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setEnabled(id: String, enabled: Boolean, updatedAt: Long)

    @Query("UPDATE place_reminders SET lastTriggeredAt = :triggeredAt, updatedAt = :updatedAt WHERE id = :id")
    suspend fun markTriggered(id: String, triggeredAt: Long, updatedAt: Long)
}
