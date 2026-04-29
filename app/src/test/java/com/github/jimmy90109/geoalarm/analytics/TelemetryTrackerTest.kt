package com.github.jimmy90109.geoalarm.analytics

import com.github.jimmy90109.geoalarm.data.AnalyticsPreferencesStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TelemetryTrackerTest {

    @Test
    fun `trackAppFirstOpenIfNeeded sends once for new onboarding users`() = runTest {
        val analytics = FakeAnalytics()
        val store = FakeAnalyticsPreferencesStore()
        val tracker = TelemetryTracker(analytics, store)

        tracker.trackAppFirstOpenIfNeeded(isNewOnboardingUser = true)
        tracker.trackAppFirstOpenIfNeeded(isNewOnboardingUser = true)

        assertEquals(listOf(AnalyticsEvents.APP_FIRST_OPEN), analytics.events)
        assertTrue(store.hasSentAppFirstOpen())
    }

    @Test
    fun `trackAppFirstOpenIfNeeded skips non onboarding users`() = runTest {
        val analytics = FakeAnalytics()
        val store = FakeAnalyticsPreferencesStore()
        val tracker = TelemetryTracker(analytics, store)

        tracker.trackAppFirstOpenIfNeeded(isNewOnboardingUser = false)

        assertTrue(analytics.events.isEmpty())
        assertTrue(!store.hasSentAppFirstOpen())
    }

    @Test
    fun `trackAnalyticsOptInIfNeeded sends once only when enabled`() = runTest {
        val analytics = FakeAnalytics()
        val store = FakeAnalyticsPreferencesStore()
        val tracker = TelemetryTracker(analytics, store)

        tracker.trackAnalyticsOptInIfNeeded()
        tracker.trackAnalyticsOptInIfNeeded()

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

        tracker.trackArrivedTurnOff()
        store.setAnalyticsEnabled(false)
        tracker.trackArrivedTurnOff()

        assertEquals(listOf(AnalyticsEvents.ALARM_TURN_OFF_COMPLETED), analytics.events)
    }

    @Test
    fun `trackAppFirstOpenIfNeeded does not mark sent when signaling fails`() = runTest {
        val analytics = FailingAnalytics()
        val store = FakeAnalyticsPreferencesStore()
        val tracker = TelemetryTracker(analytics, store)

        tracker.trackAppFirstOpenIfNeeded(isNewOnboardingUser = true)

        assertTrue(!store.hasSentAppFirstOpen())
    }
}

private class FakeAnalytics : AppAnalytics {
    val events = mutableListOf<String>()

    override fun signal(eventName: String): Boolean {
        events += eventName
        return true
    }
}

private class FailingAnalytics : AppAnalytics {
    override fun signal(eventName: String): Boolean = false
}

private class FakeAnalyticsPreferencesStore : AnalyticsPreferencesStore {
    private val analyticsEnabledState = MutableStateFlow(true)
    private var appFirstOpenSent = false
    private var analyticsOptInSent = false

    override val analyticsEnabledFlow: Flow<Boolean> = analyticsEnabledState

    override suspend fun isAnalyticsEnabled(): Boolean = analyticsEnabledState.value

    override suspend fun setAnalyticsEnabled(enabled: Boolean) {
        analyticsEnabledState.value = enabled
    }

    override suspend fun hasSentAppFirstOpen(): Boolean = appFirstOpenSent

    override suspend fun setAppFirstOpenSent(sent: Boolean) {
        appFirstOpenSent = sent
    }

    override suspend fun hasSentAnalyticsOptIn(): Boolean = analyticsOptInSent

    override suspend fun setAnalyticsOptInSent(sent: Boolean) {
        analyticsOptInSent = sent
    }
}
