package com.github.jimmy90109.geoalarm.widget

import android.content.Context

data class GeoAlarmWidgetRuntimeState(
    val progress: Int,
    val remainingDistance: Int
)

object GeoAlarmWidgetRuntimeStore {
    private const val PREF_NAME = "geo_alarm_widget_runtime"
    private const val KEY_PROGRESS = "runtime_progress"
    private const val KEY_REMAINING_DISTANCE = "runtime_remaining_distance"

    fun saveProgress(context: Context, progress: Int, remainingDistance: Int) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_PROGRESS, progress)
            .putInt(KEY_REMAINING_DISTANCE, remainingDistance)
            .apply()
    }

    fun getState(context: Context): GeoAlarmWidgetRuntimeState {
        val pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return GeoAlarmWidgetRuntimeState(
            progress = pref.getInt(KEY_PROGRESS, 0),
            remainingDistance = pref.getInt(KEY_REMAINING_DISTANCE, -1)
        )
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_PROGRESS)
            .remove(KEY_REMAINING_DISTANCE)
            .apply()
    }
}
