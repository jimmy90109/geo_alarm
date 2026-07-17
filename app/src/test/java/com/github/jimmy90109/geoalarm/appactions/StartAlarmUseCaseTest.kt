package com.github.jimmy90109.geoalarm.appactions

import com.github.jimmy90109.geoalarm.data.Alarm
import com.github.jimmy90109.geoalarm.data.location.AlarmActivationPermissionChecker
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StartAlarmUseCaseTest {

    @Test
    fun `start alarm success when target exists uniquely`() = runBlocking {
        val repository = InMemoryAlarmDataRepository()
        repository.insert(
            Alarm(
                id = "alarm-1",
                name = "Office",
                latitude = 1.0,
                longitude = 2.0,
                radius = 500.0,
                isEnabled = false
            )
        )
        val serviceStarter = FakeAlarmServiceStarter()
        val useCase = StartAlarmUseCase(repository, serviceStarter, FakeAlarmActivationPermissionChecker())

        val result = useCase(StartAlarmUseCase.Request(alarmName = "Office"))

        assertTrue(result is AppActionResult.Success)
        assertEquals(1, serviceStarter.started.size)
        assertEquals("alarm-1", serviceStarter.started.single().id)
    }

    @Test
    fun `start alarm fails when target not found`() = runBlocking {
        val repository = InMemoryAlarmDataRepository()
        val serviceStarter = FakeAlarmServiceStarter()
        val useCase = StartAlarmUseCase(repository, serviceStarter, FakeAlarmActivationPermissionChecker())

        val result = useCase(StartAlarmUseCase.Request(alarmName = "Missing"))

        assertTrue(result is AppActionResult.Error)
        result as AppActionResult.Error
        assertEquals("ERR_ALARM_NOT_FOUND", result.code)
    }

    @Test
    fun `start alarm fails when name is ambiguous`() = runBlocking {
        val repository = InMemoryAlarmDataRepository()
        repository.insert(
            Alarm(
                id = "alarm-1",
                name = "Office",
                latitude = 1.0,
                longitude = 2.0,
                radius = 500.0,
                isEnabled = false
            )
        )
        repository.insert(
            Alarm(
                id = "alarm-2",
                name = "Office",
                latitude = 3.0,
                longitude = 4.0,
                radius = 700.0,
                isEnabled = false
            )
        )
        val serviceStarter = FakeAlarmServiceStarter()
        val useCase = StartAlarmUseCase(repository, serviceStarter, FakeAlarmActivationPermissionChecker())

        val result = useCase(StartAlarmUseCase.Request(alarmName = "Office"))

        assertTrue(result is AppActionResult.Error)
        result as AppActionResult.Error
        assertEquals("ERR_ALARM_AMBIGUOUS", result.code)
    }

    @Test
    fun `start alarm stops existing running alarm before starting new one`() = runBlocking {
        val repository = InMemoryAlarmDataRepository()
        repository.insert(
            Alarm(
                id = "alarm-running",
                name = "Home",
                latitude = 1.0,
                longitude = 2.0,
                radius = 500.0,
                isEnabled = true
            )
        )
        repository.insert(
            Alarm(
                id = "alarm-target",
                name = "Office",
                latitude = 3.0,
                longitude = 4.0,
                radius = 700.0,
                isEnabled = false
            )
        )
        val serviceStarter = FakeAlarmServiceStarter()
        val useCase = StartAlarmUseCase(repository, serviceStarter, FakeAlarmActivationPermissionChecker())

        val result = useCase(StartAlarmUseCase.Request(alarmName = "Office"))

        assertTrue(result is AppActionResult.Success)
        assertEquals(1, serviceStarter.stopCount)
        assertEquals("alarm-target", serviceStarter.started.single().id)
    }

    @Test
    fun `start alarm fails without precise location permission`() = runBlocking {
        val repository = InMemoryAlarmDataRepository()
        repository.insert(testAlarm())
        val serviceStarter = FakeAlarmServiceStarter()
        val useCase = StartAlarmUseCase(
            repository,
            serviceStarter,
            FakeAlarmActivationPermissionChecker(preciseGranted = false)
        )

        val result = useCase(StartAlarmUseCase.Request(alarmName = "Office"))

        assertTrue(result is AppActionResult.Error)
        assertEquals("ERR_PRECISE_LOCATION_PERMISSION_REQUIRED", (result as AppActionResult.Error).code)
        assertTrue(serviceStarter.started.isEmpty())
    }

    @Test
    fun `start alarm fails without background location permission`() = runBlocking {
        val repository = InMemoryAlarmDataRepository()
        repository.insert(testAlarm())
        val serviceStarter = FakeAlarmServiceStarter()
        val useCase = StartAlarmUseCase(
            repository,
            serviceStarter,
            FakeAlarmActivationPermissionChecker(backgroundGranted = false)
        )

        val result = useCase(StartAlarmUseCase.Request(alarmName = "Office"))

        assertTrue(result is AppActionResult.Error)
        assertEquals("ERR_BACKGROUND_LOCATION_PERMISSION_REQUIRED", (result as AppActionResult.Error).code)
        assertTrue(serviceStarter.started.isEmpty())
    }

    private fun testAlarm() = Alarm(
        id = "alarm-permission",
        name = "Office",
        latitude = 1.0,
        longitude = 2.0,
        radius = 500.0,
        isEnabled = false
    )
}

private class FakeAlarmActivationPermissionChecker(
    private val preciseGranted: Boolean = true,
    private val backgroundGranted: Boolean = true,
) : AlarmActivationPermissionChecker {
    override fun hasPreciseForegroundLocation(): Boolean = preciseGranted
    override fun hasBackgroundLocation(): Boolean = backgroundGranted
}

private class FakeAlarmServiceStarter : AlarmServiceStarter {
    val started = mutableListOf<Alarm>()
    var stopCount = 0

    override suspend fun stopCurrentAlarm(alarmId: String): Boolean {
        stopCount += 1
        return true
    }

    override fun startAlarm(alarm: Alarm) {
        started += alarm
    }
}
