package com.github.jimmy90109.geoalarm.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

class SharedPreferenceManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREF_NAME = "geo_alarm_prefs"
        private const val KEY_SEEN_SCHEDULE_ONBOARDING = "seen_schedule_onboarding"
    }

    var hasSeenScheduleOnboarding: Boolean
        get() = prefs.getBoolean(KEY_SEEN_SCHEDULE_ONBOARDING, false)
        set(value) = prefs.edit { putBoolean(KEY_SEEN_SCHEDULE_ONBOARDING, value) }
}
