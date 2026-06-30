package com.github.jimmy90109.geoalarm.data

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

interface PlaceReminderDataRepository {
    val allReminders: Flow<List<PlaceReminderWithItems>>
    fun getReminderFlow(id: String): Flow<PlaceReminderWithItems?>
    suspend fun getReminder(id: String): PlaceReminderWithItems?
    suspend fun getEnabledRemindersOneShot(): List<PlaceReminderWithItems>
    suspend fun save(
        reminder: PlaceReminder,
        items: List<PlaceReminderItem>,
        attachments: List<PlaceReminderAttachment>? = null,
    )
    suspend fun delete(reminder: PlaceReminder)
    suspend fun addAttachments(reminderId: String, attachments: List<PlaceReminderAttachment>)
    suspend fun deleteAttachment(attachmentId: String)
    suspend fun reorderAttachments(reminderId: String, orderedIds: List<String>)
    suspend fun setEnabled(id: String, enabled: Boolean)
    suspend fun updateItem(item: PlaceReminderItem)
    suspend fun markTriggered(id: String, triggeredAt: Long)
}

@Singleton
class PlaceReminderRepository @Inject constructor(
    private val dao: PlaceReminderDao,
    private val attachmentStore: PlaceReminderAttachmentStore,
) : PlaceReminderDataRepository {
    override val allReminders: Flow<List<PlaceReminderWithItems>> = dao.getAllReminders()

    override fun getReminderFlow(id: String): Flow<PlaceReminderWithItems?> =
        dao.getReminderFlow(id)

    override suspend fun getReminder(id: String): PlaceReminderWithItems? =
        dao.getReminder(id)

    override suspend fun getEnabledRemindersOneShot(): List<PlaceReminderWithItems> =
        dao.getEnabledRemindersOneShot()

    override suspend fun save(
        reminder: PlaceReminder,
        items: List<PlaceReminderItem>,
        attachments: List<PlaceReminderAttachment>?,
    ) {
        dao.upsertReminder(reminder)
        dao.deleteItemsForReminder(reminder.id)
        dao.upsertItems(items)
        if (attachments != null) {
            val previous = dao.getAttachmentsForReminder(reminder.id)
            val nextPaths = attachments.map { it.localPath }.toSet()
            previous.filterNot { it.localPath in nextPaths }.forEach {
                attachmentStore.delete(it.localPath)
            }
            dao.deleteAttachmentsForReminder(reminder.id)
            dao.upsertAttachments(attachments)
        }
    }

    override suspend fun delete(reminder: PlaceReminder) {
        dao.getAttachmentsForReminder(reminder.id).forEach {
            attachmentStore.delete(it.localPath)
        }
        dao.deleteReminder(reminder)
    }

    override suspend fun addAttachments(reminderId: String, attachments: List<PlaceReminderAttachment>) {
        if (attachments.isEmpty()) return
        dao.upsertAttachments(attachments)
    }

    override suspend fun deleteAttachment(attachmentId: String) {
        val attachment = dao.getAttachment(attachmentId) ?: return
        attachmentStore.delete(attachment.localPath)
        dao.deleteAttachment(attachment)
    }

    override suspend fun reorderAttachments(reminderId: String, orderedIds: List<String>) {
        val current = dao.getAttachmentsForReminder(reminderId).associateBy { it.id }
        orderedIds.forEachIndexed { index, id ->
            current[id]?.let { dao.updateAttachment(it.copy(sortOrder = index)) }
        }
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
