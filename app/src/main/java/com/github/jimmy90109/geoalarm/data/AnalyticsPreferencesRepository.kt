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
        private val APP_FIRST_OPEN_SENT_KEY = booleanPreferencesKey("analytics_app_first_open_sent")
        private val ANALYTICS_OPT_IN_SENT_KEY = booleanPreferencesKey("analytics_opt_in_sent")
    }

    override val analyticsEnabledFlow: Flow<Boolean> =
        context.dataStore.data.map { preferences ->
            preferences[ANALYTICS_ENABLED_KEY] ?: true
        }

    override suspend fun isAnalyticsEnabled(): Boolean {
        val prefs: Preferences = context.dataStore.data.first()
        return prefs[ANALYTICS_ENABLED_KEY] ?: true
    }

    override suspend fun setAnalyticsEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[ANALYTICS_ENABLED_KEY] = enabled
        }
    }

    override suspend fun hasSentAppFirstOpen(): Boolean {
        val prefs: Preferences = context.dataStore.data.first()
        return prefs[APP_FIRST_OPEN_SENT_KEY] ?: false
    }

    override suspend fun setAppFirstOpenSent(sent: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[APP_FIRST_OPEN_SENT_KEY] = sent
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
