package com.github.jimmy90109.geoalarm.appactions

import com.github.jimmy90109.geoalarm.data.Alarm
import com.github.jimmy90109.geoalarm.data.AlarmSchedule
import java.time.LocalTime
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CreateScheduleUseCaseTest {

    @Test
    fun `create schedule success when alarm name resolves uniquely`() = runBlocking {
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
        val gateway = FakeScheduleGateway()
        val useCase = CreateScheduleUseCase(repository, gateway)

        val result = useCase(
            CreateScheduleUseCase.Request(
                alarmName = "Office",
                daysOfWeek = setOf(2, 4, 6),
                time = LocalTime.of(8, 30)
            )
        )

        assertTrue(result is AppActionResult.Success)
        assertEquals(1, repository.insertedSchedules.size)
        assertEquals(1, gateway.scheduled.size)
    }

    @Test
    fun `create schedule fails when alarm is not found`() = runBlocking {
        val repository = InMemoryAlarmDataRepository()
        val gateway = FakeScheduleGateway()
        val useCase = CreateScheduleUseCase(repository, gateway)

        val result = useCase(
            CreateScheduleUseCase.Request(
                alarmName = "Office",
                daysOfWeek = setOf(2),
                time = LocalTime.of(9, 0)
            )
        )

        assertTrue(result is AppActionResult.Error)
        result as AppActionResult.Error
        assertEquals("ERR_ALARM_NOT_FOUND", result.code)
    }

    @Test
    fun `create schedule fails when alarm name is ambiguous`() = runBlocking {
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
        val gateway = FakeScheduleGateway()
        val useCase = CreateScheduleUseCase(repository, gateway)

        val result = useCase(
            CreateScheduleUseCase.Request(
                alarmName = "Office",
                daysOfWeek = setOf(2),
                time = LocalTime.of(9, 0)
            )
        )

        assertTrue(result is AppActionResult.Error)
        result as AppActionResult.Error
        assertEquals("ERR_ALARM_AMBIGUOUS", result.code)
    }

    @Test
    fun `create schedule fails when duplicate schedule exists`() = runBlocking {
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
        repository.insertSchedule(
            AlarmSchedule(
                id = "schedule-1",
                alarmId = "alarm-1",
                daysOfWeek = setOf(2, 4),
                hour = 8,
                minute = 30,
                isEnabled = true
            )
        )
        val gateway = FakeScheduleGateway()
        val useCase = CreateScheduleUseCase(repository, gateway)

        val result = useCase(
            CreateScheduleUseCase.Request(
                alarmName = "Office",
                daysOfWeek = setOf(2, 4),
                time = LocalTime.of(8, 30)
            )
        )

        assertTrue(result is AppActionResult.Error)
        result as AppActionResult.Error
        assertEquals("ERR_DUPLICATE_SCHEDULE", result.code)
    }
}

private class FakeScheduleGateway : ScheduleGateway {
    val scheduled = mutableListOf<AlarmSchedule>()

    override suspend fun setSchedule(schedule: AlarmSchedule) {
        scheduled += schedule
    }
}
