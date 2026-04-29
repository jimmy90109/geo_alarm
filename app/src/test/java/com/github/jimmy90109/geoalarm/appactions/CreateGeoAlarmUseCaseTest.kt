package com.github.jimmy90109.geoalarm.appactions

import com.github.jimmy90109.geoalarm.data.Alarm
import com.github.jimmy90109.geoalarm.data.AlarmDataRepository
import com.github.jimmy90109.geoalarm.data.AlarmSchedule
import com.github.jimmy90109.geoalarm.data.ScheduleWithAlarm
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CreateGeoAlarmUseCaseTest {

    @Test
    fun `create geo alarm success when location can be geocoded`() = runBlocking {
        val repository = InMemoryAlarmDataRepository()
        val geocoder = FakeGeocodingService(GeoCoordinate(25.0330, 121.5654))
        val useCase = CreateGeoAlarmUseCase(repository, geocoder)

        val result = useCase(
            CreateGeoAlarmUseCase.Request(
                name = "Office",
                locationQuery = "Taipei 101",
                radiusMeters = 500.0
            )
        )

        assertTrue(result is AppActionResult.Success)
        assertEquals(1, repository.insertedAlarms.size)
        val created = repository.insertedAlarms.single()
        assertEquals("Office", created.name)
        assertEquals(500.0, created.radius, 0.0)
    }

    @Test
    fun `create geo alarm fails when geocoder cannot resolve location`() = runBlocking {
        val repository = InMemoryAlarmDataRepository()
        val geocoder = FakeGeocodingService(null)
        val useCase = CreateGeoAlarmUseCase(repository, geocoder)

        val result = useCase(
            CreateGeoAlarmUseCase.Request(
                name = "Office",
                locationQuery = "Unknown Place",
                radiusMeters = 500.0
            )
        )

        assertTrue(result is AppActionResult.Error)
        result as AppActionResult.Error
        assertEquals("ERR_GEOCODE_FAILED", result.code)
        assertTrue(repository.insertedAlarms.isEmpty())
    }

    @Test
    fun `create geo alarm fails when duplicate exists`() = runBlocking {
        val repository = InMemoryAlarmDataRepository()
        repository.insert(
            Alarm(
                id = "a1",
                name = "Office",
                latitude = 25.0330,
                longitude = 121.5654,
                radius = 500.0,
                isEnabled = false
            )
        )
        val geocoder = FakeGeocodingService(GeoCoordinate(25.0330, 121.5654))
        val useCase = CreateGeoAlarmUseCase(repository, geocoder)

        val result = useCase(
            CreateGeoAlarmUseCase.Request(
                name = "Office",
                locationQuery = "Taipei 101",
                radiusMeters = 500.0
            )
        )

        assertTrue(result is AppActionResult.Error)
        result as AppActionResult.Error
        assertEquals("ERR_DUPLICATE_ALARM", result.code)
    }

    @Test
    fun `create geo alarm uses default radius when radius is missing`() = runBlocking {
        val repository = InMemoryAlarmDataRepository()
        val geocoder = FakeGeocodingService(GeoCoordinate(25.0330, 121.5654))
        val useCase = CreateGeoAlarmUseCase(repository, geocoder)

        val result = useCase(
            CreateGeoAlarmUseCase.Request(
                name = "Office",
                locationQuery = "Taipei 101",
                radiusMeters = null
            )
        )

        assertTrue(result is AppActionResult.Success)
        val created = (result as AppActionResult.Success).value
        assertEquals(AppActionContract.DEFAULT_RADIUS_METERS, created.radius, 0.0)
    }
}

private class FakeGeocodingService(
    private val coordinate: GeoCoordinate?
) : GeocodingService {
    override suspend fun geocode(locationQuery: String): GeoCoordinate? = coordinate
}

internal class InMemoryAlarmDataRepository : AlarmDataRepository {
    private val alarmsStorage = linkedMapOf<String, Alarm>()
    private val schedulesStorage = linkedMapOf<String, AlarmSchedule>()

    private val alarmsFlow = MutableStateFlow<List<Alarm>>(emptyList())
    private val schedulesFlow = MutableStateFlow<List<AlarmSchedule>>(emptyList())

    val insertedAlarms = mutableListOf<Alarm>()
    val insertedSchedules = mutableListOf<AlarmSchedule>()

    override val allAlarms: Flow<List<Alarm>> = alarmsFlow
    override val allSchedules: Flow<List<AlarmSchedule>> = schedulesFlow
    override val allSchedulesWithAlarm: Flow<List<ScheduleWithAlarm>> = MutableStateFlow(emptyList())

    override suspend fun getAlarm(id: String): Alarm? = alarmsStorage[id]

    override suspend fun getAllAlarmsOneShot(): List<Alarm> = alarmsStorage.values.toList()

    override suspend fun findAlarmsByName(name: String): List<Alarm> =
        alarmsStorage.values.filter { it.name.trim() == name.trim() }

    override suspend fun insert(alarm: Alarm) {
        insertedAlarms += alarm
        alarmsStorage[alarm.id] = alarm
        alarmsFlow.value = alarmsStorage.values.toList()
    }

    override suspend fun delete(alarm: Alarm) {
        alarmsStorage.remove(alarm.id)
        alarmsFlow.value = alarmsStorage.values.toList()
    }

    override suspend fun update(alarm: Alarm) {
        alarmsStorage[alarm.id] = alarm
        alarmsFlow.value = alarmsStorage.values.toList()
    }

    override fun getSchedulesForAlarm(alarmId: String): Flow<List<AlarmSchedule>> {
        return MutableStateFlow(schedulesStorage.values.filter { it.alarmId == alarmId })
    }

    override suspend fun getSchedule(id: String): AlarmSchedule? = schedulesStorage[id]

    override suspend fun insertSchedule(schedule: AlarmSchedule) {
        insertedSchedules += schedule
        schedulesStorage[schedule.id] = schedule
        schedulesFlow.value = schedulesStorage.values.toList()
    }

    override suspend fun deleteSchedule(schedule: AlarmSchedule) {
        schedulesStorage.remove(schedule.id)
        schedulesFlow.value = schedulesStorage.values.toList()
    }

    override suspend fun updateSchedule(schedule: AlarmSchedule) {
        schedulesStorage[schedule.id] = schedule
        schedulesFlow.value = schedulesStorage.values.toList()
    }

    override suspend fun existsDuplicateSchedule(
        alarmId: String,
        days: Set<Int>,
        hour: Int,
        minute: Int
    ): Boolean {
        return schedulesStorage.values.any {
            it.alarmId == alarmId && it.daysOfWeek == days && it.hour == hour && it.minute == minute
        }
    }

    override suspend fun isAlarmUsedInSchedule(alarmId: String): Boolean {
        return schedulesStorage.values.any { it.alarmId == alarmId }
    }
}
