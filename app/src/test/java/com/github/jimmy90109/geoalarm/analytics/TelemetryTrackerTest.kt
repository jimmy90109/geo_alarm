package com.github.jimmy90109.geoalarm.analytics

import com.github.jimmy90109.geoalarm.data.AnalyticsPreferencesStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TelemetryTrackerTest {

    @Test
    fun `trackAnalyticsOptInIfNeeded sends once only when enabled`() = runTest {
        val analytics = FakeAnalytics()
        val store = FakeAnalyticsPreferencesStore()
        val tracker = TelemetryTracker(analytics, store)

        store.setAnalyticsEnabled(true)
        val first = launch { tracker.trackAnalyticsOptInIfNeeded() }
        val second = launch { tracker.trackAnalyticsOptInIfNeeded() }
        first.join()
        second.join()

        assertEquals(listOf(AnalyticsEvents.ANALYTICS_OPT_IN), analytics.events)
        assertTrue(store.hasSentAnalyticsOptIn())

        store.setAnalyticsEnabled(false)
        tracker.trackAnalyticsOptInIfNeeded()
        assertEquals(listOf(AnalyticsEvents.ANALYTICS_OPT_IN), analytics.events)
    }

    @Test
    fun `trackArrivedTurnOff sends only when analytics enabled`() = runTest {
        val analytics = FakeAnalytics()
        val store = FakeAnalyticsPreferencesStore()
        val tracker = TelemetryTracker(analytics, store)

        store.setAnalyticsEnabled(true)
        tracker.trackArrivedTurnOff()
        store.setAnalyticsEnabled(false)
        tracker.trackArrivedTurnOff()

        assertEquals(listOf(AnalyticsEvents.ALARM_TURN_OFF_COMPLETED), analytics.events)
    }

    @Test
    fun `disabled analytics never signals`() = runTest {
        val analytics = FakeAnalytics()
        val store = FakeAnalyticsPreferencesStore()
        val tracker = TelemetryTracker(analytics, store)

        tracker.trackAnalyticsOptInIfNeeded()
        tracker.trackArrivedTurnOff()

        assertTrue(analytics.events.isEmpty())
    }
}

private class FakeAnalytics : AppAnalytics {
    val events = mutableListOf<String>()

    override suspend fun signal(eventName: String): Boolean {
        events += eventName
        return true
    }
}

private class FakeAnalyticsPreferencesStore : AnalyticsPreferencesStore {
    private val analyticsEnabledState = MutableStateFlow(false)
    private var analyticsOptInSent = false

    override val analyticsEnabledFlow: Flow<Boolean> = analyticsEnabledState

    override suspend fun isAnalyticsEnabled(): Boolean = analyticsEnabledState.value

    override suspend fun setAnalyticsEnabled(enabled: Boolean) {
        analyticsEnabledState.value = enabled
    }

    override suspend fun hasSentAnalyticsOptIn(): Boolean = analyticsOptInSent

    override suspend fun setAnalyticsOptInSent(sent: Boolean) {
        analyticsOptInSent = sent
    }
}
