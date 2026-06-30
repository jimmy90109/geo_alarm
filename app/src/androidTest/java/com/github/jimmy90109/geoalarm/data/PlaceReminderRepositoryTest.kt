package com.github.jimmy90109.geoalarm.data

import android.content.Context
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PlaceReminderRepositoryTest {
    private lateinit var database: AppDatabase
    private lateinit var repository: PlaceReminderRepository

    @Before
    fun setUp() {
        val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = PlaceReminderRepository(database.placeReminderDao(), FakeAttachmentStore())
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun saveToggleAndUpdateChecklistItem() = runBlocking {
        val now = 1_000L
        val reminder = PlaceReminder(
            id = UUID.randomUUID().toString(),
            title = "Supermarket list",
            type = PlaceReminderType.CHECKLIST,
            placeName = "PX Mart",
            address = "Taipei",
            iconKey = DEFAULT_ALARM_ICON_KEY,
            latitude = 25.0,
            longitude = 121.0,
            radiusMeters = 150,
            triggerType = PlaceTriggerType.ENTER,
            dwellMinutes = null,
            cooldownMinutes = 360,
            enabled = false,
            lastTriggeredAt = null,
            createdAt = now,
            updatedAt = now,
        )
        val items = listOf(
            PlaceReminderItem(reminderId = reminder.id, text = "Milk", checked = false, sortOrder = 0),
            PlaceReminderItem(reminderId = reminder.id, text = "Eggs", checked = false, sortOrder = 1),
        )

        val attachments = listOf(
            PlaceReminderAttachment(
                reminderId = reminder.id,
                type = PlaceReminderAttachmentType.IMAGE,
                localPath = "/tmp/photo.jpg",
                mimeType = "image/jpeg",
                displayName = "photo.jpg",
                sizeBytes = 100,
                durationMillis = null,
                width = null,
                height = null,
                sortOrder = 0,
                createdAt = now,
            )
        )

        repository.save(reminder, items, attachments)
        repository.setEnabled(reminder.id, true)

        val saved = repository.getReminder(reminder.id)
        assertNotNull(saved)
        assertTrue(saved!!.reminder.enabled)
        assertEquals(listOf("Milk", "Eggs"), saved.sortedItems.map { it.text })
        assertEquals(listOf("photo.jpg"), saved.sortedAttachments.map { it.displayName })

        repository.updateItem(saved.sortedItems.first().copy(checked = true))
        val updated = repository.getReminder(reminder.id)!!
        assertTrue(updated.sortedItems.first().checked)
        assertFalse(updated.sortedItems.last().checked)

        repository.delete(updated.reminder)
        assertTrue(repository.allReminders.first().isEmpty())
    }
}

private class FakeAttachmentStore : PlaceReminderAttachmentStore {
    override suspend fun copy(
        reminderId: String,
        uri: android.net.Uri,
        sortOrder: Int
    ): PlaceReminderAttachment? = null

    override fun delete(localPath: String) = Unit
}
