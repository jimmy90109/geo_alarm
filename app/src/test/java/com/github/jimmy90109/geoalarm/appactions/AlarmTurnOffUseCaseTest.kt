package com.github.jimmy90109.geoalarm.appactions

import com.github.jimmy90109.geoalarm.data.Alarm
import com.github.jimmy90109.geoalarm.service.GeoAlarmContract
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class AlarmTurnOffUseCaseTest {
    @Test
    fun `regular alarm is disabled and arrival effects run`() = runBlocking {
        val repository = InMemoryAlarmDataRepository()
        repository.insert(alarm(id = "regular", isEnabled = true))
        val effects = FakeAlarmTurnOffEffects()

        AlarmTurnOffUseCase(repository, effects)("regular", trackArrivedTurnOff = true)

        assertFalse(repository.getAlarm("regular")!!.isEnabled)
        assertEquals(1, effects.arrivedTurnOffCount)
        assertEquals(1, effects.stopCount)
        assertEquals(1, effects.refreshCount)
    }

    @Test
    fun `test alarm skips repository and still stops`() = runBlocking {
        val repository = InMemoryAlarmDataRepository()
        val effects = FakeAlarmTurnOffEffects()

        AlarmTurnOffUseCase(repository, effects)(
            GeoAlarmContract.TEST_ALARM_ID,
            trackArrivedTurnOff = true,
        )

        assertEquals(0, repository.getAlarmCalls)
        assertEquals(1, effects.arrivedTurnOffCount)
        assertEquals(1, effects.stopCount)
        assertEquals(1, effects.refreshCount)
    }

    @Test
    fun `non-arrival cancellation does not run arrival effects`() = runBlocking {
        val repository = InMemoryAlarmDataRepository()
        repository.insert(alarm(id = "regular", isEnabled = true))
        val effects = FakeAlarmTurnOffEffects()

        AlarmTurnOffUseCase(repository, effects)("regular", trackArrivedTurnOff = false)

        assertEquals(0, effects.arrivedTurnOffCount)
        assertEquals(1, effects.stopCount)
        assertEquals(1, effects.refreshCount)
    }

    private fun alarm(id: String, isEnabled: Boolean) = Alarm(
        id = id,
        name = id,
        latitude = 1.0,
        longitude = 2.0,
        radius = 100.0,
        isEnabled = isEnabled,
    )
}

private class FakeAlarmTurnOffEffects : AlarmTurnOffEffects {
    var arrivedTurnOffCount = 0
    var stopCount = 0
    var refreshCount = 0

    override suspend fun onArrivedTurnOff() {
        arrivedTurnOffCount += 1
    }

    override fun stopCurrentAlarm(alarmId: String) {
        stopCount += 1
    }

    override suspend fun refreshWidgets() {
        refreshCount += 1
    }
}
