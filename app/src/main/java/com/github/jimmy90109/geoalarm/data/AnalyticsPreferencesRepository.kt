package com.github.jimmy90109.geoalarm.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class AnalyticsPreferencesRepository @Inject constructor(
    @param:ApplicationContext private val context: Context
) : AnalyticsPreferencesStore {

    companion object {
        private val ANALYTICS_ENABLED_KEY = booleanPreferencesKey("analytics_enabled")
        private val ANALYTICS_OPT_IN_SENT_KEY = booleanPreferencesKey("analytics_opt_in_sent")

        internal fun analyticsEnabled(preferences: Preferences): Boolean =
            preferences[ANALYTICS_ENABLED_KEY] ?: false
    }

    override val analyticsEnabledFlow: Flow<Boolean> =
        context.dataStore.data.map(::analyticsEnabled)

    override suspend fun isAnalyticsEnabled(): Boolean {
        val prefs: Preferences = context.dataStore.data.first()
        return analyticsEnabled(prefs)
    }

    override suspend fun setAnalyticsEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[ANALYTICS_ENABLED_KEY] = enabled
        }
    }

    override suspend fun hasSentAnalyticsOptIn(): Boolean {
        val prefs: Preferences = context.dataStore.data.first()
        return prefs[ANALYTICS_OPT_IN_SENT_KEY] ?: false
    }

    override suspend fun setAnalyticsOptInSent(sent: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[ANALYTICS_OPT_IN_SENT_KEY] = sent
        }
    }
}
