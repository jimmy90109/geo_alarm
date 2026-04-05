package com.github.jimmy90109.geoalarm.appactions

import com.github.jimmy90109.geoalarm.data.Alarm
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
        val useCase = StartAlarmUseCase(repository, serviceStarter)

        val result = useCase(StartAlarmUseCase.Request(alarmName = "Office"))

        assertTrue(result is AppActionResult.Success)
        assertEquals(1, serviceStarter.started.size)
        assertEquals("alarm-1", serviceStarter.started.single().id)
    }

    @Test
    fun `start alarm fails when target not found`() = runBlocking {
        val repository = InMemoryAlarmDataRepository()
        val serviceStarter = FakeAlarmServiceStarter()
        val useCase = StartAlarmUseCase(repository, serviceStarter)

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
        val useCase = StartAlarmUseCase(repository, serviceStarter)

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
        val useCase = StartAlarmUseCase(repository, serviceStarter)

        val result = useCase(StartAlarmUseCase.Request(alarmName = "Office"))

        assertTrue(result is AppActionResult.Success)
        assertEquals(1, serviceStarter.stopCount)
        assertEquals("alarm-target", serviceStarter.started.single().id)
    }
}

private class FakeAlarmServiceStarter : AlarmServiceStarter {
    val started = mutableListOf<Alarm>()
    var stopCount = 0

    override fun stopCurrentAlarm() {
        stopCount += 1
    }

    override fun startAlarm(alarm: Alarm) {
        started += alarm
    }
}
