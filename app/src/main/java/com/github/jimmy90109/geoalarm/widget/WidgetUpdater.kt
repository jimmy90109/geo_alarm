package com.github.jimmy90109.geoalarm.widget

import android.content.Context
import android.content.Intent
import android.util.Log
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import androidx.glance.appwidget.updateAll
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.datastore.preferences.core.MutablePreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface WidgetUpdater {
    suspend fun refreshAll()
}

@Singleton
class AppWidgetUpdater @Inject constructor(
    @ApplicationContext private val context: Context
) : WidgetUpdater {
    companion object {
        private const val TAG = "WidgetUpdater"
    }

    override suspend fun refreshAll() {
        withContext(Dispatchers.IO) {
            val widget = GeoAlarmGlanceWidget()
            Log.d(TAG, "refreshAll() start")
            widget.updateAll(context)

            // Also force per-widget-id refresh for hosts/devices where batched update can lag.
            val manager = GlanceAppWidgetManager(context)
            val glanceIds = manager.getGlanceIds(GeoAlarmGlanceWidget::class.java)
            val now = System.currentTimeMillis()
            Log.d(TAG, "Glance ids count=${glanceIds.size}")
            glanceIds.forEach { glanceId ->
                updateAppWidgetState(context, glanceId) { prefs: MutablePreferences ->
                    prefs[GeoAlarmGlanceWidget.LastRefreshEpochMsKey] = now
                }
                widget.update(context, glanceId)
                Log.d(TAG, "Updated glanceId=$glanceId")
            }

            // Fallback: force host launcher redraw for devices that don't repaint promptly on Glance updates.
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val receiver = ComponentName(context, GeoAlarmGlanceWidgetReceiver::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(receiver)
            if (appWidgetIds.isNotEmpty()) {
                context.sendBroadcast(
                    Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE).apply {
                        component = receiver
                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
                    }
                )
            }
            Log.d(TAG, "Fallback appWidgetIds=${appWidgetIds.joinToString(prefix = "[", postfix = "]")}")
            Log.d(TAG, "refreshAll() done")
        }
    }
}
