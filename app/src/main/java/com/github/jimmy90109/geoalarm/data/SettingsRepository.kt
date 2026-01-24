package com.github.jimmy90109.geoalarm.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by androidx.datastore.preferences.preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {
    companion object {
        private val RINGTONE_ENABLED_KEY = booleanPreferencesKey("ringtone_enabled")
        private val RINGTONE_URI_KEY = stringPreferencesKey("ringtone_uri")
        private val RINGTONE_NAME_KEY = stringPreferencesKey("ringtone_name")
    }

    val ringtoneSettingsFlow: Flow<RingtoneSettings> = context.dataStore.data.map { preferences ->
        RingtoneSettings(
            enabled = preferences[RINGTONE_ENABLED_KEY] ?: false,
            ringtoneUri = preferences[RINGTONE_URI_KEY],
            ringtoneName = preferences[RINGTONE_NAME_KEY]
        )
    }

    suspend fun setRingtoneEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[RINGTONE_ENABLED_KEY] = enabled
        }
    }

    suspend fun setRingtone(uri: String?, name: String?) {
        context.dataStore.edit { preferences ->
            if (uri != null) {
                preferences[RINGTONE_URI_KEY] = uri
            } else {
                preferences.remove(RINGTONE_URI_KEY)
            }
            if (name != null) {
                preferences[RINGTONE_NAME_KEY] = name
            } else {
                preferences.remove(RINGTONE_NAME_KEY)
            }
        }
    }
}
