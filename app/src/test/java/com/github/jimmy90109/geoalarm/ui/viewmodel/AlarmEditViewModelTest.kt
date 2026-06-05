package com.github.jimmy90109.geoalarm.ui.viewmodel

import com.github.jimmy90109.geoalarm.data.Alarm
import com.github.jimmy90109.geoalarm.data.AlarmDao
import com.github.jimmy90109.geoalarm.data.AlarmRepository
import com.github.jimmy90109.geoalarm.data.DEFAULT_ALARM_ICON_KEY
import com.github.jimmy90109.geoalarm.data.AlarmSchedule
import com.github.jimmy90109.geoalarm.data.ScheduleDao
import com.github.jimmy90109.geoalarm.data.ScheduleWithAlarm
import com.github.jimmy90109.geoalarm.widget.WidgetUpdater
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description

@OptIn(ExperimentalCoroutinesApi::class)
class AlarmEditViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `loadAlarm with existing id updates ui state`() = runTest {
        val existing = Alarm(
            id = "alarm-1",
            name = "Office",
            latitude = 25.1,
            longitude = 121.5,
            radius = 800.0,
            isEnabled = false
        )
        val repository = buildRepository(alarms = listOf(existing))
        val viewModel = createViewModel(repository)

        viewModel.onAction(AlarmEditAction.LoadAlarm(existing.id))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(existing, state.existingAlarm)
        assertEquals(LatLng(25.1, 121.5), state.selectedPosition)
        assertEquals(800f, state.radius)
        assertEquals("Office", state.name)
        assertEquals(DEFAULT_ALARM_ICON_KEY, state.selectedIconKey)
        assertFalse(state.isLoading)
    }

    @Test
    fun `loadAlarm with null id only clears loading`() = runTest {
        val viewModel = createViewModel(buildRepository())

        viewModel.onAction(AlarmEditAction.LoadAlarm(null))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.existingAlarm)
    }

    @Test
    fun `saveAlarm without position does nothing`() = runTest {
        val alarmDao = FakeAlarmDao()
        val viewModel = createViewModel(buildRepository(alarmDao = alarmDao))

        viewModel.onAction(AlarmEditAction.NameChanged("Home"))
        viewModel.onAction(AlarmEditAction.SaveClicked)
        advanceUntilIdle()

        assertTrue(alarmDao.inserted.isEmpty())
        assertFalse(viewModel.uiState.value.isSaved)
        assertNull(viewModel.uiState.value.savedAlarmId)
    }

    @Test
    fun `saveAlarm creates new alarm and marks saved`() = runTest {
        val alarmDao = FakeAlarmDao()
        val viewModel = createViewModel(buildRepository(alarmDao = alarmDao))
        val effects = mutableListOf<AlarmEditEffect>()
        val effectsJob = launch { viewModel.effects.collect { effects += it } }
        viewModel.onAction(AlarmEditAction.PositionSelected(LatLng(24.9, 121.1)))
        viewModel.onAction(AlarmEditAction.RadiusChanged(1200f))
        viewModel.onAction(AlarmEditAction.NameChanged("Gym"))

        viewModel.onAction(AlarmEditAction.SaveClicked)
        advanceUntilIdle()

        assertEquals(1, alarmDao.inserted.size)
        val created = alarmDao.inserted.single()
        assertEquals("Gym", created.name)
        assertEquals(24.9, created.latitude, 0.0)
        assertEquals(121.1, created.longitude, 0.0)
        assertEquals(1200.0, created.radius, 0.0)
        assertFalse(created.isEnabled)
        assertEquals(DEFAULT_ALARM_ICON_KEY, created.iconKey)

        val state = viewModel.uiState.value
        assertTrue(state.isSaved)
        assertNotNull(state.savedAlarmId)
        assertEquals(listOf(AlarmEditEffect.NavigateBack(state.savedAlarmId)), effects)
        effectsJob.cancel()
    }

    @Test
    fun `saveAlarm updates existing alarm`() = runTest {
        val existing = Alarm(
            id = "alarm-2",
            name = "Old name",
            latitude = 10.0,
            longitude = 10.0,
            radius = 500.0,
            isEnabled = true
        )
        val alarmDao = FakeAlarmDao(initialAlarms = listOf(existing))
        val repository = buildRepository(alarmDao = alarmDao)
        val viewModel = createViewModel(repository)
        viewModel.onAction(AlarmEditAction.LoadAlarm(existing.id))
        advanceUntilIdle()
        viewModel.onAction(AlarmEditAction.PositionSelected(LatLng(11.0, 12.0)))
        viewModel.onAction(AlarmEditAction.RadiusChanged(900f))
        viewModel.onAction(AlarmEditAction.NameChanged("New name"))

        viewModel.onAction(AlarmEditAction.SaveClicked)
        advanceUntilIdle()

        assertEquals(1, alarmDao.updated.size)
        val updated = alarmDao.updated.single()
        assertEquals(existing.id, updated.id)
        assertEquals("New name", updated.name)
        assertEquals(11.0, updated.latitude, 0.0)
        assertEquals(12.0, updated.longitude, 0.0)
        assertEquals(900.0, updated.radius, 0.0)
        assertTrue(updated.isEnabled)
        assertTrue(viewModel.uiState.value.isSaved)
        assertEquals(existing.id, viewModel.uiState.value.savedAlarmId)
    }

    @Test
    fun `goToDetailsStep changes step when position exists and goToMapStep restores`() = runTest {
        val viewModel = createViewModel(buildRepository())
        viewModel.onAction(AlarmEditAction.LoadAlarm(null))
        advanceUntilIdle()

        viewModel.onAction(AlarmEditAction.NextClicked)
        assertEquals(AlarmEditStep.MapSelection, viewModel.uiState.value.step)

        viewModel.onAction(AlarmEditAction.PositionSelected(LatLng(25.0, 121.0)))
        viewModel.onAction(AlarmEditAction.NextClicked)
        assertEquals(AlarmEditStep.DetailsForm, viewModel.uiState.value.step)

        viewModel.onAction(AlarmEditAction.BackToMapClicked)
        assertEquals(AlarmEditStep.MapSelection, viewModel.uiState.value.step)
    }

    @Test
    fun `saveAlarm stores selected icon`() = runTest {
        val alarmDao = FakeAlarmDao()
        val viewModel = createViewModel(buildRepository(alarmDao = alarmDao))
        viewModel.onAction(AlarmEditAction.PositionSelected(LatLng(24.9, 121.1)))
        viewModel.onAction(AlarmEditAction.NameChanged("Office"))
        viewModel.onAction(AlarmEditAction.IconSelected("train"))

        viewModel.onAction(AlarmEditAction.SaveClicked)
        advanceUntilIdle()

        val created = alarmDao.inserted.single()
        assertEquals("train", created.iconKey)
    }

    @Test
    fun `requestDeleteAlarm shows error dialog when alarm used by schedule`() = runTest {
        val existing = Alarm(
            id = "alarm-3",
            name = "Mall",
            latitude = 1.0,
            longitude = 2.0,
            radius = 300.0,
            isEnabled = false
        )
        val scheduleDao = FakeScheduleDao(isAlarmUsed = true)
        val viewModel = createViewModel(
            buildRepository(
                alarms = listOf(existing),
                scheduleDao = scheduleDao
            )
        )
        viewModel.onAction(AlarmEditAction.LoadAlarm(existing.id))
        advanceUntilIdle()

        viewModel.onAction(AlarmEditAction.DeleteRequested)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.showDeleteErrorDialog)
        assertFalse(viewModel.uiState.value.showDeleteConfirmDialog)
    }

    @Test
    fun `confirmDeleteAlarm deletes alarm and marks saved`() = runTest {
        val existing = Alarm(
            id = "alarm-4",
            name = "School",
            latitude = 3.0,
            longitude = 4.0,
            radius = 700.0,
            isEnabled = false
        )
        val alarmDao = FakeAlarmDao(initialAlarms = listOf(existing))
        val scheduleDao = FakeScheduleDao(isAlarmUsed = false)
        val viewModel = createViewModel(
            buildRepository(
                alarmDao = alarmDao,
                scheduleDao = scheduleDao
            )
        )
        viewModel.onAction(AlarmEditAction.LoadAlarm(existing.id))
        advanceUntilIdle()
        viewModel.onAction(AlarmEditAction.DeleteRequested)
        advanceUntilIdle()
        val effects = mutableListOf<AlarmEditEffect>()
        val effectsJob = launch { viewModel.effects.collect { effects += it } }

        viewModel.onAction(AlarmEditAction.DeleteConfirmed)
        advanceUntilIdle()

        assertEquals(listOf(existing), alarmDao.deleted)
        assertTrue(viewModel.uiState.value.isSaved)
        assertFalse(viewModel.uiState.value.showDeleteConfirmDialog)
        assertEquals(listOf(AlarmEditEffect.NavigateBack(null)), effects)
        effectsJob.cancel()
    }

    private fun buildRepository(
        alarms: List<Alarm> = emptyList(),
        alarmDao: FakeAlarmDao = FakeAlarmDao(initialAlarms = alarms),
        scheduleDao: FakeScheduleDao = FakeScheduleDao()
    ): AlarmRepository = AlarmRepository(alarmDao, scheduleDao)

    private fun createViewModel(
        repository: AlarmRepository,
        widgetUpdater: WidgetUpdater = FakeWidgetUpdater()
    ): AlarmEditViewModel = AlarmEditViewModel(repository, widgetUpdater)
}

@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    private val dispatcher: TestDispatcher = StandardTestDispatcher()
) : TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(dispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}

private class FakeWidgetUpdater : WidgetUpdater {
    override suspend fun refreshAll() = Unit
}

private class FakeAlarmDao(
    initialAlarms: List<Alarm> = emptyList()
) : AlarmDao {
    private val storage = LinkedHashMap<String, Alarm>().apply {
        initialAlarms.forEach { put(it.id, it) }
    }
    private val alarmsFlow = MutableStateFlow(storage.values.toList())

    val inserted = mutableListOf<Alarm>()
    val updated = mutableListOf<Alarm>()
    val deleted = mutableListOf<Alarm>()

    override fun getAllAlarms(): Flow<List<Alarm>> = alarmsFlow

    override suspend fun getAllAlarmsOneShot(): List<Alarm> = storage.values.toList()

    override suspend fun getAlarmById(id: String): Alarm? = storage[id]

    override suspend fun findAlarmsByName(name: String): List<Alarm> =
        storage.values.filter { it.name.trim() == name.trim() }

    override suspend fun insertAlarm(alarm: Alarm) {
        inserted += alarm
        storage[alarm.id] = alarm
        alarmsFlow.update { storage.values.toList() }
    }

    override suspend fun deleteAlarm(alarm: Alarm) {
        deleted += alarm
        storage.remove(alarm.id)
        alarmsFlow.update { storage.values.toList() }
    }

    override suspend fun updateAlarm(alarm: Alarm) {
        updated += alarm
        storage[alarm.id] = alarm
        alarmsFlow.update { storage.values.toList() }
    }
}

private class FakeScheduleDao(
    private val isAlarmUsed: Boolean = false
) : ScheduleDao {
    private val schedulesFlow = MutableStateFlow<List<AlarmSchedule>>(emptyList())
    private val schedulesWithAlarmFlow = MutableStateFlow<List<ScheduleWithAlarm>>(emptyList())

    override fun getAllSchedules(): Flow<List<AlarmSchedule>> = schedulesFlow

    override fun getAllSchedulesWithAlarm(): Flow<List<ScheduleWithAlarm>> = schedulesWithAlarmFlow

    override fun getSchedulesForAlarm(alarmId: String): Flow<List<AlarmSchedule>> =
        MutableStateFlow(emptyList())

    override suspend fun getSchedulesForAlarmOneShot(alarmId: String): List<AlarmSchedule> = emptyList()

    override suspend fun insertSchedule(schedule: AlarmSchedule) = Unit

    override suspend fun deleteSchedule(schedule: AlarmSchedule) = Unit

    override suspend fun updateSchedule(schedule: AlarmSchedule) = Unit

    override suspend fun getScheduleById(id: String): AlarmSchedule? = null

    override suspend fun isAlarmUsedInSchedule(alarmId: String): Boolean = isAlarmUsed
}
