package com.github.jimmy90109.geoalarm.widget

import android.appwidget.AppWidgetManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.MutablePreferences
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.updateAll
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.jimmy90109.geoalarm.GeoAlarmApplication
import com.github.jimmy90109.geoalarm.R
import com.github.jimmy90109.geoalarm.data.Alarm
import com.github.jimmy90109.geoalarm.ui.theme.GeoAlarmTheme
import kotlinx.coroutines.launch

class GeoAlarmWidgetConfigActivity : ComponentActivity() {
    private val repository by lazy {
        (application as GeoAlarmApplication).repository
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setResult(RESULT_CANCELED)

        val appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }


        setContent {
            GeoAlarmTheme {
                WidgetConfigScreen(
                    appWidgetId = appWidgetId,
                    alarms = repository.allAlarms.collectAsStateWithLifecycle(initialValue = emptyList()).value,
                    initialSelection = GeoAlarmWidgetConfigStore.getSelectedAlarmIds(this, appWidgetId),
                    onCancel = { finish() },
                    onSave = { selectedIds ->
                        GeoAlarmWidgetConfigStore.saveSelectedAlarmIds(this, appWidgetId, selectedIds)
                        lifecycleScope.launch {
                            val widget = GeoAlarmGlanceWidget()
                            val glanceId = GlanceAppWidgetManager(this@GeoAlarmWidgetConfigActivity)
                                .getGlanceIdBy(appWidgetId)
                            updateAppWidgetState(
                                context = this@GeoAlarmWidgetConfigActivity,
                                glanceId = glanceId
                            ) { prefs: MutablePreferences ->
                                prefs[GeoAlarmGlanceWidget.SelectedAlarmIdsKey] = selectedIds.toSet()
                            }
                            widget.update(this@GeoAlarmWidgetConfigActivity, glanceId)
                            widget.updateAll(this@GeoAlarmWidgetConfigActivity)
                            val resultIntent = android.content.Intent().apply {
                                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                            }
                            setResult(RESULT_OK, resultIntent)
                            finish()
                        }
                    },
                    onSelectionLimitReached = {
                        Toast.makeText(this, R.string.widget_selection_limit, Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun WidgetConfigScreen(
    appWidgetId: Int,
    alarms: List<Alarm>,
    initialSelection: List<String>,
    onCancel: () -> Unit,
    onSave: (List<String>) -> Unit,
    onSelectionLimitReached: () -> Unit
) {
    var selectedIds by remember(appWidgetId) {
        mutableStateOf(initialSelection.distinct().take(2))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.widget_config_title),
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = stringResource(R.string.widget_config_subtitle),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
        },
        bottomBar = {
            val navigationBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = navigationBottom + 12.dp)
            ) {
                ButtonGroup(
                    overflowIndicator = { menuState ->
                        ButtonGroupDefaults.OverflowIndicator(menuState = menuState)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    val buttonGroupScope = this
                    customItem(
                        buttonGroupContent = {
                            FilledIconButton(
                                onClick = onCancel,
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = stringResource(R.string.cancel)
                                )
                            }
                        },
                        menuContent = { menuState ->
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.cancel)) },
                                onClick = {
                                    onCancel()
                                    menuState.dismiss()
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Filled.Close,
                                        contentDescription = stringResource(R.string.cancel)
                                    )
                                }
                            )
                        }
                    )
                    customItem(
                        buttonGroupContent = {
                            Button(
                                onClick = { onSave(selectedIds) },
                                modifier = with(buttonGroupScope) {
                                    Modifier.weight(1f)
                                }
                            ) {
                                Text(text = stringResource(R.string.save))
                            }
                        },
                        menuContent = { menuState ->
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.save)) },
                                onClick = {
                                    onSave(selectedIds)
                                    menuState.dismiss()
                                }
                            )
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                // Add some top padding before the first item
                androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(4.dp))
            }
            items(alarms, key = { it.id }) { alarm ->
                val selectedIndex = selectedIds.indexOf(alarm.id)
                val isSelected = selectedIndex >= 0
                val isEnabled = isSelected || selectedIds.size < 2
                WidgetAlarmToggleRow(
                    alarm = alarm,
                    selected = isSelected,
                    selectedOrder = if (isSelected) selectedIndex + 1 else null,
                    enabled = isEnabled,
                    onCheckedChange = { checked ->
                        selectedIds = when {
                            checked && selectedIds.contains(alarm.id) -> selectedIds
                            checked && selectedIds.size >= 2 -> {
                                onSelectionLimitReached()
                                selectedIds
                            }
                            checked -> selectedIds + alarm.id
                            else -> selectedIds.filterNot { it == alarm.id }
                        }
                    }
                )
            }
            item {
                // Add some bottom padding after the last item
                androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(16.dp))
            }
        }
    }
}

@Composable
private fun WidgetAlarmToggleRow(
    alarm: Alarm,
    selected: Boolean,
    selectedOrder: Int?,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val cardShape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
    Card(
        onClick = { onCheckedChange(!selected) },
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.6f),
        shape = cardShape,
        colors = CardDefaults.cardColors(
            containerColor = when {
                !enabled -> MaterialTheme.colorScheme.surfaceVariant
                selected -> MaterialTheme.colorScheme.primaryContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
            },
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = alarm.name,
                style = MaterialTheme.typography.titleMedium,
                color = when {
                    !enabled -> MaterialTheme.colorScheme.onSurfaceVariant
                    selected -> MaterialTheme.colorScheme.onPrimaryContainer
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.weight(1f)
            )
            // Keep a fixed right slot so card height/layout stays stable.
            Box(
                modifier = Modifier
                    .padding(start = 12.dp)
                    .size(30.dp),
                contentAlignment = Alignment.Center
            ) {
                if (selected && selectedOrder != null) {
                    Surface(
                        shape = androidx.compose.foundation.shape.CircleShape,
                        color = MaterialTheme.colorScheme.primary
                    ) {
                        Box(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = selectedOrder.toString(),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}
