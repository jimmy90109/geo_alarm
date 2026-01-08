package com.github.jimmy90109.geoalarm.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences

val Context.dataStore: DataStore<Preferences> by androidx.datastore.preferences.preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context)
