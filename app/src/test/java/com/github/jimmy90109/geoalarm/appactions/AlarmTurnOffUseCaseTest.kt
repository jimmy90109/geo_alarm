package com.github.jimmy90109.geoalarm.appactions

import com.github.jimmy90109.geoalarm.data.Alarm
import com.github.jimmy90109.geoalarm.data.ReviewPromptStore
import com.github.jimmy90109.geoalarm.service.GeoAlarmContract
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AlarmTurnOffUseCaseTest {
    @Test
    fun `regular alarm is disabled and arrival effects run`() = runBlocking {
        val repository = InMemoryAlarmDataRepository()
        repository.insert(alarm(id = "regular", isEnabled = true))
        val effects = FakeAlarmTurnOffEffects()
        val reviewPromptStore = FakeReviewPromptStore(shouldRequest = true)

        val result = AlarmTurnOffUseCase(repository, effects, reviewPromptStore)(
            "regular",
            trackArrivedTurnOff = true,
        )

        assertFalse(repository.getAlarm("regular")!!.isEnabled)
        assertEquals(true, result.shouldRequestInAppReview)
        assertEquals(1, effects.arrivedTurnOffCount)
        assertEquals(1, reviewPromptStore.recordCount)
        assertEquals(1, effects.stopCount)
        assertEquals(1, effects.refreshCount)
    }

    @Test
    fun `test alarm skips repository and still stops`() = runBlocking {
        val repository = InMemoryAlarmDataRepository()
        val effects = FakeAlarmTurnOffEffects()
        val reviewPromptStore = FakeReviewPromptStore(shouldRequest = true)

        val result = AlarmTurnOffUseCase(repository, effects, reviewPromptStore)(
            GeoAlarmContract.TEST_ALARM_ID,
            trackArrivedTurnOff = true,
        )

        assertEquals(0, repository.getAlarmCalls)
        assertEquals(1, effects.arrivedTurnOffCount)
        assertFalse(result.shouldRequestInAppReview)
        assertEquals(0, reviewPromptStore.recordCount)
        assertEquals(1, effects.stopCount)
        assertEquals(1, effects.refreshCount)
    }

    @Test
    fun `non-arrival cancellation does not run arrival effects`() = runBlocking {
        val repository = InMemoryAlarmDataRepository()
        repository.insert(alarm(id = "regular", isEnabled = true))
        val effects = FakeAlarmTurnOffEffects()
        val reviewPromptStore = FakeReviewPromptStore(shouldRequest = true)

        val result = AlarmTurnOffUseCase(repository, effects, reviewPromptStore)(
            "regular",
            trackArrivedTurnOff = false,
        )

        assertFalse(result.shouldRequestInAppReview)
        assertEquals(0, effects.arrivedTurnOffCount)
        assertEquals(0, reviewPromptStore.recordCount)
        assertEquals(1, effects.stopCount)
        assertEquals(1, effects.refreshCount)
    }

    @Test
    fun `duplicate arrival turn off is counted only once`() = runBlocking {
        val repository = InMemoryAlarmDataRepository()
        repository.insert(alarm(id = "regular", isEnabled = true))
        val effects = FakeAlarmTurnOffEffects()
        val reviewPromptStore = FakeReviewPromptStore(shouldRequest = true)
        val useCase = AlarmTurnOffUseCase(repository, effects, reviewPromptStore)

        val first = useCase("regular", trackArrivedTurnOff = true)
        val duplicate = useCase("regular", trackArrivedTurnOff = true)

        assertEquals(true, first.shouldRequestInAppReview)
        assertFalse(duplicate.shouldRequestInAppReview)
        assertEquals(1, effects.arrivedTurnOffCount)
        assertEquals(1, reviewPromptStore.recordCount)
        assertEquals(2, effects.stopCount)
        assertEquals(2, effects.refreshCount)
    }

    @Test
    fun `review storage failure does not prevent alarm cleanup`() = runBlocking {
        val repository = InMemoryAlarmDataRepository()
        repository.insert(alarm(id = "regular", isEnabled = true))
        val effects = FakeAlarmTurnOffEffects()
        val reviewPromptStore = FakeReviewPromptStore(throwOnRecord = true)

        val result = AlarmTurnOffUseCase(repository, effects, reviewPromptStore)(
            "regular",
            trackArrivedTurnOff = true,
        )

        assertFalse(result.shouldRequestInAppReview)
        assertFalse(repository.getAlarm("regular")!!.isEnabled)
        assertEquals(1, effects.stopCount)
        assertEquals(1, effects.refreshCount)
    }

    @Test
    fun `failed alarm cleanup does not record successful arrival`() = runBlocking {
        val repository = InMemoryAlarmDataRepository()
        repository.insert(alarm(id = "regular", isEnabled = true))
        val effects = FakeAlarmTurnOffEffects(throwOnStop = true)
        val reviewPromptStore = FakeReviewPromptStore(shouldRequest = true)

        var failed = false
        try {
            AlarmTurnOffUseCase(repository, effects, reviewPromptStore)(
                "regular",
                trackArrivedTurnOff = true,
            )
        } catch (_: IllegalStateException) {
            failed = true
        }

        assertTrue(failed)
        assertEquals(0, reviewPromptStore.recordCount)
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

private class FakeReviewPromptStore(
    private val shouldRequest: Boolean = false,
    private val throwOnRecord: Boolean = false,
) : ReviewPromptStore {
    var recordCount = 0

    override suspend fun recordSuccessfulArrivalAndReservePrompt(): Boolean {
        recordCount += 1
        if (throwOnRecord) error("review storage unavailable")
        return shouldRequest
    }
}

private class FakeAlarmTurnOffEffects(
    private val throwOnStop: Boolean = false,
) : AlarmTurnOffEffects {
    var arrivedTurnOffCount = 0
    var stopCount = 0
    var refreshCount = 0

    override suspend fun onArrivedTurnOff() {
        arrivedTurnOffCount += 1
    }

    override fun stopCurrentAlarm(alarmId: String) {
        stopCount += 1
        if (throwOnStop) error("alarm cleanup failed")
    }

    override suspend fun refreshWidgets() {
        refreshCount += 1
    }
}
