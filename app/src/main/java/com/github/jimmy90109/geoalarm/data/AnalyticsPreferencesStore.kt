package com.github.jimmy90109.geoalarm.data

import kotlinx.coroutines.flow.Flow

interface AnalyticsPreferencesStore {
    val analyticsEnabledFlow: Flow<Boolean>

    suspend fun isAnalyticsEnabled(): Boolean
    suspend fun setAnalyticsEnabled(enabled: Boolean)

    suspend fun hasSentAnalyticsOptIn(): Boolean
    suspend fun setAnalyticsOptInSent(sent: Boolean)
}
