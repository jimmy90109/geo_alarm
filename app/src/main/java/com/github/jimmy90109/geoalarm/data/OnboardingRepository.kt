package com.github.jimmy90109.geoalarm.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class OnboardingRepository(
    private val context: Context
) {
    companion object {
        private val KEY_SEEN_LOCATION_ONBOARDING =
            booleanPreferencesKey("seen_location_onboarding")

        // Legacy SharedPreferences key used before DataStore migration.
        private const val LEGACY_PREF_NAME = "geo_alarm_prefs"
        private const val LEGACY_KEY_SEEN_LOCATION_ONBOARDING = "seen_location_onboarding"
    }

    val hasSeenLocationOnboardingFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_SEEN_LOCATION_ONBOARDING] ?: false
    }

    suspend fun hasSeenLocationOnboarding(): Boolean {
        val prefs = context.dataStore.data.first()
        val currentValue = prefs[KEY_SEEN_LOCATION_ONBOARDING]
        if (currentValue != null) return currentValue

        // One-time migration from legacy SharedPreferences.
        val legacyValue = context
            .getSharedPreferences(LEGACY_PREF_NAME, Context.MODE_PRIVATE)
            .getBoolean(LEGACY_KEY_SEEN_LOCATION_ONBOARDING, false)
        if (legacyValue) {
            context.dataStore.edit { it[KEY_SEEN_LOCATION_ONBOARDING] = true }
        }
        return legacyValue
    }

    suspend fun setSeenLocationOnboarding(seen: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_SEEN_LOCATION_ONBOARDING] = seen
        }
    }
}
