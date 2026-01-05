package com.github.jimmy90109.geoalarm.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.jimmy90109.geoalarm.R
import com.github.jimmy90109.geoalarm.data.MonitoringMethod
import com.github.jimmy90109.geoalarm.ui.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val monitoringMethod by viewModel.monitoringMethod.collectAsStateWithLifecycle()

    // Current Language is pulled directly from the VM helper to ensure recomposition when changed? 
    // Actually AppCompatDelegate triggers recreation, but let's grab it for display consistency 
    // or arguably just keep using the VM's logic if it was a Flow. 
    // The previous implementation used LocalContext/AppCompatDelegate inside Composable. 
    // The VM implementation provides a getter. Since Activity recreation happens on locale change, 
    // reading it in composition is fine, but using VM helper might be cleaner for logic separation.
    // However, the VM property isn't a state, so it won't trigger updates if we just access it. 
    // But Activity recreation does the job.
    val currentLanguage = viewModel.currentLanguage

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.settings),
                        modifier = Modifier.fillMaxWidth(),
                    )
                },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(top = innerPadding.calculateTopPadding())
                .fillMaxSize(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(androidx.compose.foundation.rememberScrollState())
                    .padding(16.dp)
                    // Add padding at bottom for floating bar
                    .padding(bottom = 100.dp),
            ) {
                // General Section
                SettingsSectionHeader(title = stringResource(R.string.settings_section_general))

                SettingsCard(
                    title = stringResource(R.string.language),
                    value = if (currentLanguage == "zh") stringResource(R.string.locale_zh) else stringResource(
                        R.string.locale_en
                    ),
                    onClick = { viewModel.showLanguageSheet() },
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Alarm Section
                SettingsSectionHeader(title = stringResource(R.string.settings_section_alarm))

                val monitoringValue =
                    if (monitoringMethod == MonitoringMethod.GEOFENCE) stringResource(R.string.method_geofencing)
                    else stringResource(R.string.method_gps)

                SettingsCard(
                    title = stringResource(R.string.monitoring_method),
                    value = monitoringValue,
                    onClick = { viewModel.showMonitoringSheet() },
                    enabled = !uiState.anyAlarmEnabled,
                    subtitle = if (uiState.anyAlarmEnabled) stringResource(R.string.monitoring_method_locked) else null
                )
            }
        }
    }

    // Language Bottom Sheet
    InlineBottomSheet(
        visible = uiState.showLanguageSheet,
        onDismissRequest = { viewModel.dismissLanguageSheet() }) {
        Column(
            modifier = Modifier.padding(
                bottom = WindowInsets.navigationBars.asPaddingValues()
                    .calculateBottomPadding() + 72.dp
            )
        ) { // Padding for Navbar
            Text(
                text = stringResource(R.string.language),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
            )
            SettingsSelectionItem(
                text = stringResource(R.string.locale_zh),
                selected = currentLanguage == "zh",
                enabled = true,
                onClick = { viewModel.setAppLocale("zh-TW") },
            )
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 24.dp)
            )
            SettingsSelectionItem(
                text = stringResource(R.string.locale_en),
                selected = currentLanguage == "en",
                enabled = true,
                onClick = { viewModel.setAppLocale("en") },
            )
        }
    }

    // Monitoring Method Bottom Sheet
    InlineBottomSheet(
        visible = uiState.showMonitoringSheet,
        onDismissRequest = { viewModel.dismissMonitoringSheet() }) {
        Column(
            modifier = Modifier.padding(
                bottom = WindowInsets.navigationBars.asPaddingValues()
                    .calculateBottomPadding() + 72.dp
            )
        ) {
            Text(
                text = stringResource(R.string.monitoring_method),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
            )
            SettingsSelectionItem(
                text = stringResource(R.string.method_geofencing),
                description = stringResource(R.string.method_geofencing_desc),
                selected = monitoringMethod == MonitoringMethod.GEOFENCE,
                enabled = true,
                onClick = {
                    viewModel.setMonitoringMethod(MonitoringMethod.GEOFENCE)
                },
            )
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 24.dp)
            )
            SettingsSelectionItem(
                text = stringResource(R.string.method_gps),
                description = stringResource(R.string.method_gps_desc),
                selected = monitoringMethod == MonitoringMethod.GPS,
                enabled = true,
                onClick = {
                    viewModel.setMonitoringMethod(MonitoringMethod.GPS)
                },
            )
        }
    }
}

@Composable
fun InlineBottomSheet(
    visible: Boolean, onDismissRequest: () -> Unit, content: @Composable () -> Unit,
) {
    if (visible) {
        androidx.activity.compose.BackHandler(onBack = onDismissRequest)
    }

    AnimatedVisibility(
        visible = visible, enter = fadeIn(), exit = fadeOut()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismissRequest,
                )
        )
    }

    // Swipe logic state
    val offsetY = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    // Reset offset when visibility changes to true
    LaunchedEffect(visible) {
        if (visible) offsetY.snapTo(0f)
    }

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically { it } + fadeIn(),
        exit = slideOutVertically { it } + fadeOut()) {
        Box(
            modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter
        ) {
            Surface(
                shape = RoundedCornerShape(
                topStart = 28.dp, topEnd = 28.dp,
            ),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                tonalElevation = 6.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .offset {
                        IntOffset(0, offsetY.value.roundToInt())
                    }
                    .pointerInput(Unit) {
                        detectVerticalDragGestures(
                            onDragEnd = {
                                val dismissThreshold = 100.dp.toPx()
                                if (offsetY.value > dismissThreshold) {
                                    onDismissRequest()
                                } else {
                                    scope.launch { offsetY.animateTo(0f) }
                                }
                            },
                            onVerticalDrag = { change, dragAmount ->
                                change.consume()
                                scope.launch {
                                    val newOffset = (offsetY.value + dragAmount).coerceAtLeast(0f)
                                    offsetY.snapTo(newOffset)
                                }
                            },
                        )
                    }
                    .clickable(
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = null, // Disable ripple for the sheet background itself
                        onClick = {}, // Capture clicks so they don't dismiss
                    )
            ) {
                Column(
                    modifier = Modifier.restoreEdgeToEdgeProps(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    // Handle
                    Box(
                        modifier = Modifier
                            .padding(top = 16.dp, bottom = 8.dp)
                            .width(32.dp)
                            .height(4.dp)
                            .background(
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                shape = CircleShape,
                            )
                    )
                    content()
                }
            }
        }
    }
}

// Helper to remove any implicit padding from Surface if needed, 
// though Column usually handles content. Added for safety or future extension.
@Composable
private fun Modifier.restoreEdgeToEdgeProps() = this


@Composable
fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
    )
}

@Composable
fun SettingsCard(
    title: String,
    value: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    subtitle: String? = null
) {
    Card(
        onClick = onClick, enabled = enabled,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.6f)
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )

        }
    }
}

@Composable
fun SettingsSelectionItem(
    text: String,
    description: String? = null,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .selectable(
                selected = selected, role = Role.RadioButton, onClick = onClick, enabled = enabled
            )
            .padding(
                vertical = 16.dp, horizontal = 24.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(
                    alpha = 0.38f
                )
            )
            if (description != null) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface.copy(
                        alpha = 0.38f
                    )
                )
            }
        }

        if (selected) {
            Icon(
                imageVector = Icons.Default.Check, contentDescription = null, // decorative
                tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 16.dp)
            )
        }
    }
}
