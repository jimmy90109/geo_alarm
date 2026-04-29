package com.github.jimmy90109.geoalarm.analytics

import com.github.jimmy90109.geoalarm.data.AnalyticsPreferencesStore
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TelemetryTracker @Inject constructor(
    private val analytics: AppAnalytics,
    private val preferencesStore: AnalyticsPreferencesStore
) {

    suspend fun trackAppFirstOpenIfNeeded(isNewOnboardingUser: Boolean) {
        if (!isNewOnboardingUser) return
        if (preferencesStore.hasSentAppFirstOpen()) return

        val sent = analytics.signal(AnalyticsEvents.APP_FIRST_OPEN)
        if (sent) {
            preferencesStore.setAppFirstOpenSent(true)
        }
    }

    suspend fun trackAnalyticsOptInIfNeeded() {
        if (!preferencesStore.isAnalyticsEnabled()) return
        if (preferencesStore.hasSentAnalyticsOptIn()) return

        val sent = analytics.signal(AnalyticsEvents.ANALYTICS_OPT_IN)
        if (sent) {
            preferencesStore.setAnalyticsOptInSent(true)
        }
    }

    suspend fun trackArrivedTurnOff() {
        if (!preferencesStore.isAnalyticsEnabled()) return
        analytics.signal(AnalyticsEvents.ALARM_TURN_OFF_COMPLETED)
    }
}
