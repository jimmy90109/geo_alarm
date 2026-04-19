package com.github.jimmy90109.geoalarm.widget

import android.appwidget.AppWidgetManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.nestedscroll.nestedScroll
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
import com.github.jimmy90109.geoalarm.ui.components.AlarmIconBadge
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
    val availableAlarmIds = remember(alarms) { alarms.map { it.id }.toSet() }
    var selectedIds by remember(appWidgetId) {
        mutableStateOf(
            initialSelection
                .distinct()
                .filter { it in availableAlarmIds }
                .take(2)
        )
    }
    LaunchedEffect(availableAlarmIds) {
        selectedIds = selectedIds.filter { it in availableAlarmIds }.take(2)
    }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeFlexibleTopAppBar(
                title = {
                    Column(modifier = Modifier.fillMaxWidth()) {
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
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(
                    WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)
                )
        ) {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(300.dp),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = innerPadding.calculateTopPadding(),
                    bottom = innerPadding.calculateBottomPadding() + 100.dp,
                    start = 16.dp, 
                    end = 16.dp
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    // Add some top padding before the first item
                    androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(4.dp))
                }
                items(alarms, key = { it.id }) { alarm ->
                    val selectedIndex = selectedIds.indexOf(alarm.id)
                    val isSelected = selectedIndex >= 0
                    val activeSelectionCount = selectedIds.count { it in availableAlarmIds }
                    val isEnabled = isSelected || activeSelectionCount < 2
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
                item(span = { GridItemSpan(maxLineSpan) }) {
                    // Add some bottom padding after the last item
                    androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(16.dp))
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(
                        start = 16.dp, 
                        end = 16.dp, 
                        bottom = innerPadding.calculateBottomPadding() + 16.dp
                    ),
                contentAlignment = Alignment.Center
            ) {
                ButtonGroup(
                    overflowIndicator = { menuState ->
                        ButtonGroupDefaults.OverflowIndicator(menuState = menuState)
                    },
                    modifier = Modifier.widthIn(max = 480.dp).fillMaxWidth()
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
                                onClick = { onSave(selectedIds.filter { it in availableAlarmIds }.take(2)) },
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
                                    onSave(selectedIds.filter { it in availableAlarmIds }.take(2))
                                    menuState.dismiss()
                                }
                            )
                        }
                    )
                }
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
                !enabled -> MaterialTheme.colorScheme.surfaceContainer
                selected -> MaterialTheme.colorScheme.primaryContainer
                else -> MaterialTheme.colorScheme.surfaceContainer
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
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AlarmIconBadge(iconKey = alarm.iconKey, modifier = Modifier.size(24.dp))
                Text(
                    text = alarm.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = when {
                        !enabled -> MaterialTheme.colorScheme.onSurfaceVariant
                        selected -> MaterialTheme.colorScheme.onPrimaryContainer
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.padding(start = 10.dp)
                )
            }
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
