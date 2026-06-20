package com.github.jimmy90109.geoalarm.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

val Context.dataStore: DataStore<Preferences> by androidx.datastore.preferences.preferencesDataStore(name = "settings")

class SettingsRepository @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    companion object {
        private val RINGTONE_ENABLED_KEY = booleanPreferencesKey("ringtone_enabled")
        private val RINGTONE_URI_KEY = stringPreferencesKey("ringtone_uri")
        private val RINGTONE_NAME_KEY = stringPreferencesKey("ringtone_name")
        private val PAYMENT_SHORTCUT_KEY = stringPreferencesKey("payment_shortcut")
        private val FULLSCREEN_INTENT_PROMPT_HANDLED_KEY =
            booleanPreferencesKey("fullscreen_intent_prompt_handled")
    }

    val ringtoneSettingsFlow: Flow<RingtoneSettings> = context.dataStore.data.map { preferences ->
        RingtoneSettings(
            enabled = preferences[RINGTONE_ENABLED_KEY] ?: false,
            ringtoneUri = preferences[RINGTONE_URI_KEY],
            ringtoneName = preferences[RINGTONE_NAME_KEY]
        )
    }

    val paymentShortcutFlow: Flow<PaymentShortcut?> = context.dataStore.data.map { preferences ->
        PaymentShortcut.fromId(preferences[PAYMENT_SHORTCUT_KEY])
    }

    val fullscreenIntentPromptHandledFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[FULLSCREEN_INTENT_PROMPT_HANDLED_KEY] ?: false
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

    suspend fun setPaymentShortcut(shortcut: PaymentShortcut?) {
        context.dataStore.edit { preferences ->
            if (shortcut != null) {
                preferences[PAYMENT_SHORTCUT_KEY] = shortcut.id
            } else {
                preferences.remove(PAYMENT_SHORTCUT_KEY)
            }
        }
    }

    suspend fun setFullscreenIntentPromptHandled(handled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[FULLSCREEN_INTENT_PROMPT_HANDLED_KEY] = handled
        }
    }
}
