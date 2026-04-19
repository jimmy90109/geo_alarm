package com.github.jimmy90109.geoalarm.widget

import android.content.Context

object GeoAlarmWidgetConfigStore {
    private const val PREF_NAME = "geo_alarm_widget_config"

    private fun keyForWidget(appWidgetId: Int): String = "widget_${appWidgetId}_alarm_ids"

    fun saveSelectedAlarmIds(context: Context, appWidgetId: Int, alarmIds: List<String>) {
        val encoded = GeoAlarmWidgetSelectionCodec.encode(alarmIds)
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(keyForWidget(appWidgetId), encoded)
            .apply()
    }

    fun getSelectedAlarmIds(context: Context, appWidgetId: Int): List<String> {
        val raw = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString(keyForWidget(appWidgetId), "")
            .orEmpty()
        return GeoAlarmWidgetSelectionCodec.decode(raw)
    }

    fun clear(context: Context, appWidgetId: Int) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(keyForWidget(appWidgetId))
            .apply()
    }
}
