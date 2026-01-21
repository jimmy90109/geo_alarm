package com.github.jimmy90109.geoalarm.ui.screens

import android.content.res.Configuration
import android.view.HapticFeedbackConstants
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.view.HapticFeedbackConstantsCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.jimmy90109.geoalarm.R
import com.github.jimmy90109.geoalarm.ui.components.DeleteScheduleDialog
import com.github.jimmy90109.geoalarm.ui.viewmodel.ScheduleEditViewModel
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ScheduleEditScreen(
    viewModel: ScheduleEditViewModel, scheduleId: String?, onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val alarms by viewModel.alarms.collectAsStateWithLifecycle()

    LaunchedEffect(scheduleId) {
        viewModel.loadSchedule(scheduleId)
    }

    // Use scheduleId as key to recreate the picker when schedule data is loaded
    val timePickerState = key(uiState.scheduleId) {
        rememberTimePickerState(
            initialHour = uiState.hour,
            initialMinute = uiState.minute,
        )
    }


    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val view = LocalView.current
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(timePickerState) {
        snapshotFlow { timePickerState.hour to timePickerState.minute }.drop(1).collect {
            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (isLandscape) {
            // LANDSCAPE LAYOUT
            Box(modifier = Modifier.fillMaxSize()) {
                // Main Content - Left Side (TimePicker + DaySelector)
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(end = 400.dp)
                        .windowInsetsPadding(WindowInsets.displayCutout)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // title
                    Text(
                        text = stringResource(R.string.select_time),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    TimePicker(state = timePickerState)
                    Spacer(modifier = Modifier.height(24.dp))
                    DayOfWeekSelector(
                        selectedDays = uiState.daysOfWeek,
                        onDayToggle = {
                            haptic.performHapticFeedback(HapticFeedbackType.ToggleOn)
                            viewModel.toggleDay(it)
                        },
                    )
                }

                // Controls Overlay - Right Side
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 24.dp)
                        .windowInsetsPadding(WindowInsets.displayCutout)
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .widthIn(max = 360.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        // TopAppBar
                        com.github.jimmy90109.geoalarm.ui.components.TopAppBar(
                            title = {
                                Text(
                                    stringResource(
                                        if (scheduleId == null) R.string.new_schedule
                                        else R.string.edit_schedule
                                    )
                                )
                            },
                            navigationIcon = {
                                IconButton(onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                                    onBack()
                                }) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = stringResource(R.string.back)
                                    )
                                }
                            },
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        )

                        // Bottom Control Card
                        Card(
                            shape = RoundedCornerShape(44.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                AlarmSelector(
                                    alarms = alarms,
                                    selectedAlarmId = uiState.selectedAlarmId,
                                    onAlarmSelected = {
                                        haptic.performHapticFeedback(HapticFeedbackType.ToggleOn)
                                        viewModel.selectAlarm(it)
                                    },
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                ButtonGroup(
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    if (scheduleId != null) {
                                        FilledIconButton(
                                            onClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.Reject)
                                                viewModel.requestDeleteSchedule()
                                            }, shape = RoundedCornerShape(
                                                topStart = 28.dp,
                                                bottomStart = 28.dp,
                                                topEnd = 4.dp,
                                                bottomEnd = 4.dp
                                            ), colors = IconButtonDefaults.filledIconButtonColors(
                                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                                contentColor = MaterialTheme.colorScheme.onErrorContainer
                                            )
                                        ) {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = stringResource(R.string.delete)
                                            )
                                        }
                                    }
                                    Button(
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                            viewModel.setTime(
                                                timePickerState.hour,
                                                timePickerState.minute,
                                            )
                                            viewModel.saveSchedule { _ -> onBack() }
                                        },
                                        shape = if (scheduleId != null) RoundedCornerShape(
                                            topStart = 4.dp,
                                            bottomStart = 4.dp,
                                            topEnd = 28.dp,
                                            bottomEnd = 28.dp
                                        ) else CircleShape,
                                        modifier = Modifier.weight(1f),
                                        enabled = uiState.selectedAlarmId != null && uiState.daysOfWeek.isNotEmpty(),
                                    ) {
                                        Text(stringResource(R.string.save))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // PORTRAIT LAYOUT

            val navigationBottom =
                WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
            val bottomPadding = maxOf(navigationBottom, 24.dp)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = bottomPadding),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                com.github.jimmy90109.geoalarm.ui.components.TopAppBar(
                    title = {
                    Text(
                        stringResource(
                            if (scheduleId == null) R.string.new_schedule
                            else R.string.edit_schedule
                        )
                    )
                },
                    navigationIcon = {
                        IconButton(onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                            onBack()
                        }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.back)
                            )
                        }
                    },
                    modifier = Modifier.padding(horizontal = 24.dp),
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )

                // Content Container (Centered)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 24.dp, end = 24.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // title
                    Text(
                        text = stringResource(R.string.select_time),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    TimePicker(state = timePickerState)
                    Spacer(modifier = Modifier.height(24.dp))
                    DayOfWeekSelector(
                        selectedDays = uiState.daysOfWeek,
                        onDayToggle = {
                            haptic.performHapticFeedback(HapticFeedbackType.ToggleOn)
                            viewModel.toggleDay(it)
                        },
                    )
                }

                // Bottom Control Card
                Card(
                    shape = RoundedCornerShape(44.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 24.dp, end = 24.dp, top = 16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        AlarmSelector(
                            alarms = alarms,
                            selectedAlarmId = uiState.selectedAlarmId,
                            onAlarmSelected = {
                                haptic.performHapticFeedback(HapticFeedbackType.ToggleOn)
                                viewModel.selectAlarm(it)
                            },
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        ButtonGroup(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (scheduleId != null) {
                                FilledIconButton(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.Reject)
                                        viewModel.requestDeleteSchedule()
                                    }, shape = RoundedCornerShape(
                                        topStart = 28.dp,
                                        bottomStart = 28.dp,
                                        topEnd = 4.dp,
                                        bottomEnd = 4.dp
                                    ), colors = IconButtonDefaults.filledIconButtonColors(
                                        containerColor = MaterialTheme.colorScheme.errorContainer,
                                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = stringResource(R.string.delete)
                                    )
                                }
                            }
                            Button(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                    viewModel.setTime(
                                        timePickerState.hour, timePickerState.minute
                                    )
                                    viewModel.saveSchedule { _ -> onBack() }
                                },
                                shape = if (scheduleId != null) RoundedCornerShape(
                                    topStart = 4.dp,
                                    bottomStart = 4.dp,
                                    topEnd = 28.dp,
                                    bottomEnd = 28.dp
                                ) else CircleShape,
                                modifier = Modifier.weight(1f),
                                enabled = uiState.selectedAlarmId != null && uiState.daysOfWeek.isNotEmpty()
                            ) {
                                Text(stringResource(R.string.save))
                            }
                        }
                    }
                }
            }
        }
    }

    // Delete Confirmation Dialog
    if (uiState.showDeleteConfirmDialog) {
        DeleteScheduleDialog(
            onConfirm = { viewModel.confirmDeleteSchedule(onBack) },
            onDismiss = { viewModel.dismissDeleteConfirmDialog() },
        )
    }
}

@Composable
fun DayOfWeekSelector(
    selectedDays: Set<Int>, onDayToggle: (Int) -> Unit
) {
    val dayLabels = androidx.compose.ui.res.stringArrayResource(R.array.weekdays_short)
    // Map Calendar constant to index in array (0=Sun, 6=Sat)
    // Calendar.SUNDAY = 1
    val days = listOf(
        Calendar.SUNDAY to dayLabels[0],
        Calendar.MONDAY to dayLabels[1],
        Calendar.TUESDAY to dayLabels[2],
        Calendar.WEDNESDAY to dayLabels[3],
        Calendar.THURSDAY to dayLabels[4],
        Calendar.FRIDAY to dayLabels[5],
        Calendar.SATURDAY to dayLabels[6]
    )

    Row(
        modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween
    ) {
        days.forEach { (dayValues, label) ->
            val isSelected = selectedDays.contains(dayValues)
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceContainerHighest
                    )
                    .toggleable(
                        value = isSelected, onValueChange = { onDayToggle(dayValues) }),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmSelector(
    alarms: List<com.github.jimmy90109.geoalarm.data.Alarm>,
    selectedAlarmId: String?,
    onAlarmSelected: (String) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    var showSheet by remember { mutableStateOf(false) }
    val selectedAlarm = alarms.find { it.id == selectedAlarmId }
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()

    OutlinedCard(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.ToggleOn)
            showSheet = true
        },
        shape = CircleShape,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = selectedAlarm?.name ?: stringResource(R.string.select_alarm),
                style = MaterialTheme.typography.bodyLarge,
                color = if (selectedAlarm != null) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
        }
    }

    if (showSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSheet = false },
            sheetState = sheetState,
        ) {
            Text(
                text = stringResource(R.string.select_alarm),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(start = 24.dp, bottom = 16.dp)
            )
            LazyColumn(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 16.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainer)
            ) {
                items(alarms.size) { index ->
                    val alarm = alarms[index]
                    val isSelected = alarm.id == selectedAlarmId

                    SelectionItem(
                        text = alarm.name, selected = isSelected,
                        onClick = {
                            onAlarmSelected(alarm.id)
                            scope.launch {
                                sheetState.hide()
                                showSheet = false
                            }
                        },
                    )

                    if (index < alarms.size - 1) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectionItem(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(
                onClick = onClick, enabled = enabled
            ) // Clickable is fine, selectable is for semantics
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
                color = if (enabled) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.onSurface.copy(
                    alpha = 0.38f
                )
            )
        }

        if (selected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 16.dp)
            )
        }
    }
}
