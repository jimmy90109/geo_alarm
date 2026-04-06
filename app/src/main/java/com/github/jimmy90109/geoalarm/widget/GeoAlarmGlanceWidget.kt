package com.github.jimmy90109.geoalarm.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.currentState
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.github.jimmy90109.geoalarm.GeoAlarmApplication
import com.github.jimmy90109.geoalarm.MainActivity
import com.github.jimmy90109.geoalarm.R
import com.github.jimmy90109.geoalarm.data.Alarm

class GeoAlarmGlanceWidget : GlanceAppWidget() {
    // Use exact sizing so padding/layout can react to every host resize step.
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val application = context.applicationContext as GeoAlarmApplication
        val repository = application.repository
        val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(id)

        val alarms = repository.getAllAlarmsOneShot()

        provideContent {
            GlanceTheme {
                val size = LocalSize.current
                val outerPadding = widgetOuterPadding(size)
                val titleBottomSpacing = if (outerPadding <= 8.dp) 8.dp else 16.dp
                val useHorizontalButtons = size.width > size.height
                val prefs = currentState<Preferences>()
                val selectedIds = prefs[SelectedAlarmIdsKey]?.toList().orEmpty()
                val selectedAlarms = resolveSelectedAlarms(alarms, selectedIds)
                val rootClickAction = if (selectedAlarms.isEmpty()) {
                    actionStartActivity(
                        Intent(context, GeoAlarmWidgetConfigActivity::class.java).apply {
                            action = AppWidgetManager.ACTION_APPWIDGET_CONFIGURE
                            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                        }
                    )
                } else {
                    actionStartActivity(Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    })
                }

                Column(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(GlanceTheme.colors.surface)
                        .cornerRadius(android.R.dimen.system_app_widget_background_radius)
                        .padding(outerPadding)
                        .clickable(rootClickAction)
                ) {
                    Text(
                        text = context.getString(R.string.app_name),
                        style = TextStyle(
                            color = GlanceTheme.colors.onSurface,
                            fontSize = androidx.compose.ui.unit.TextUnit(14f, androidx.compose.ui.unit.TextUnitType.Sp),
                            fontWeight = androidx.glance.text.FontWeight.Bold
                        )
                    )

                    Spacer(modifier = GlanceModifier.height(titleBottomSpacing))

                    if (selectedAlarms.isEmpty()) {
                        Text(
                            text = context.getString(R.string.widget_empty_message),
                            style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant),
                            modifier = GlanceModifier.clickable(
                                actionStartActivity(
                                    Intent(context, GeoAlarmWidgetConfigActivity::class.java).apply {
                                        action = AppWidgetManager.ACTION_APPWIDGET_CONFIGURE
                                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                                        flags =
                                            Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                                    }
                                )
                            )
                        )
                    } else {
                        if (useHorizontalButtons && selectedAlarms.size > 1) {
                            Row(
                                modifier = GlanceModifier
                                    .defaultWeight()
                                    .fillMaxWidth()
                            ) {
                                selectedAlarms.forEachIndexed { index, alarm ->
                                    AlarmButton(
                                        context = context,
                                        alarm = alarm,
                                        fillWidth = false,
                                        modifier = GlanceModifier
                                            .defaultWeight()
                                            .fillMaxHeight()
                                    )
                                    if (index < selectedAlarms.lastIndex) {
                                        Spacer(modifier = GlanceModifier.width(8.dp))
                                    }
                                }
                            }
                        } else {
                            Column(
                                modifier = GlanceModifier
                                    .defaultWeight()
                                    .fillMaxWidth()
                            ) {
                                selectedAlarms.forEachIndexed { index, alarm ->
                                    AlarmButton(
                                        context = context,
                                        alarm = alarm,
                                        fillWidth = true,
                                        modifier = GlanceModifier
                                            .defaultWeight()
                                            .fillMaxWidth()
                                    )
                                    if (index < selectedAlarms.lastIndex) {
                                        Spacer(modifier = GlanceModifier.height(8.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun AlarmButton(
        context: Context,
        alarm: Alarm,
        fillWidth: Boolean,
        modifier: GlanceModifier
    ) {
        val clickAction = actionStartActivity(Intent(context, MainActivity::class.java).apply {
            action = MainActivity.ACTION_ENABLE_ALARM_FROM_WIDGET
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(MainActivity.EXTRA_WIDGET_ALARM_ID, alarm.id)
        })

        val buttonModifier = modifier
            .background(GlanceTheme.colors.secondaryContainer)
            .cornerRadius(android.R.dimen.system_app_widget_inner_radius)
            .clickable(clickAction)
            .padding(12.dp)

        val contentModifier = if (fillWidth) {
            buttonModifier.fillMaxWidth()
        } else {
            buttonModifier.fillMaxHeight()
        }

        Column(
            modifier = contentModifier,
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                provider = ImageProvider(R.drawable.ic_widget_location),
                contentDescription = null,
                modifier = GlanceModifier.size(24.dp),
                colorFilter = androidx.glance.ColorFilter.tint(GlanceTheme.colors.onSecondaryContainer)
            )
            Spacer(modifier = GlanceModifier.height(4.dp))
            Text(
                text = alarm.name,
                style = TextStyle(
                    color = GlanceTheme.colors.onSecondaryContainer,
                    fontWeight = androidx.glance.text.FontWeight.Bold
                ),
                maxLines = 1
            )
        }
    }

    private fun resolveSelectedAlarms(allAlarms: List<Alarm>, selectedIds: List<String>): List<Alarm> {
        if (selectedIds.isEmpty()) return emptyList()
        val byId = allAlarms.associateBy { it.id }
        return selectedIds.mapNotNull { byId[it] }.take(2)
    }

    companion object {
        val SelectedAlarmIdsKey = stringSetPreferencesKey("selected_alarm_ids")
    }

    private fun widgetOuterPadding(size: DpSize): Dp {
        val width = size.width
        val height = size.height
        return when {
            width <= 140.dp || height <= 140.dp -> 8.dp
            width <= 220.dp || height <= 220.dp -> 12.dp
            else -> 16.dp
        }
    }
}
