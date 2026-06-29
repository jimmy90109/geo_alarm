package com.github.jimmy90109.geoalarm.data

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

interface PlaceReminderDataRepository {
    val allReminders: Flow<List<PlaceReminderWithItems>>
    fun getReminderFlow(id: String): Flow<PlaceReminderWithItems?>
    suspend fun getReminder(id: String): PlaceReminderWithItems?
    suspend fun getEnabledRemindersOneShot(): List<PlaceReminderWithItems>
    suspend fun save(reminder: PlaceReminder, items: List<PlaceReminderItem>)
    suspend fun delete(reminder: PlaceReminder)
    suspend fun setEnabled(id: String, enabled: Boolean)
    suspend fun updateItem(item: PlaceReminderItem)
    suspend fun markTriggered(id: String, triggeredAt: Long)
}

@Singleton
class PlaceReminderRepository @Inject constructor(
    private val dao: PlaceReminderDao
) : PlaceReminderDataRepository {
    override val allReminders: Flow<List<PlaceReminderWithItems>> = dao.getAllReminders()

    override fun getReminderFlow(id: String): Flow<PlaceReminderWithItems?> =
        dao.getReminderFlow(id)

    override suspend fun getReminder(id: String): PlaceReminderWithItems? =
        dao.getReminder(id)

    override suspend fun getEnabledRemindersOneShot(): List<PlaceReminderWithItems> =
        dao.getEnabledRemindersOneShot()

    override suspend fun save(reminder: PlaceReminder, items: List<PlaceReminderItem>) {
        dao.upsertReminder(reminder)
        dao.deleteItemsForReminder(reminder.id)
        dao.upsertItems(items)
    }

    override suspend fun delete(reminder: PlaceReminder) {
        dao.deleteReminder(reminder)
    }

    override suspend fun setEnabled(id: String, enabled: Boolean) {
        dao.setEnabled(id, enabled, System.currentTimeMillis())
    }

    override suspend fun updateItem(item: PlaceReminderItem) {
        dao.updateItem(item)
    }

    override suspend fun markTriggered(id: String, triggeredAt: Long) {
        dao.markTriggered(id, triggeredAt, triggeredAt)
    }
}
