package com.example.geo_alarm.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by androidx.datastore.preferences.preferencesDataStore(name = "settings")

enum class MonitoringMethod {
    GPS,
    GEOFENCE
}

class SettingsRepository(private val context: Context) {

    private val MONITORING_METHOD_KEY = stringPreferencesKey("monitoring_method")
    
    val monitoringMethod: Flow<MonitoringMethod> = context.dataStore.data
        .map { preferences ->
            try {
                val value = preferences[MONITORING_METHOD_KEY] ?: MonitoringMethod.GEOFENCE.name
                MonitoringMethod.valueOf(value)
            } catch (e: IllegalArgumentException) {
                MonitoringMethod.GEOFENCE
            }
        }

    suspend fun setMonitoringMethod(method: MonitoringMethod) {
        context.dataStore.edit { preferences ->
            preferences[MONITORING_METHOD_KEY] = method.name
        }
    }
}
