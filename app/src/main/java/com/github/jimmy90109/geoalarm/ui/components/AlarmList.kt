package com.github.jimmy90109.geoalarm.ui.components

import androidx.compose.animation.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.github.jimmy90109.geoalarm.R
import com.github.jimmy90109.geoalarm.data.Alarm
import com.github.jimmy90109.geoalarm.data.AlarmSchedule
import com.github.jimmy90109.geoalarm.data.ScheduleWithAlarm
import com.github.jimmy90109.geoalarm.utils.TimeUtils

/**
 * Displays the list of alarms and schedules with sections.
 */
@Composable
fun AlarmList(
    alarms: List<Alarm>,
    schedules: List<ScheduleWithAlarm>,
    onAlarmClick: (Alarm) -> Unit,
    onToggleAlarm: (Alarm, Boolean) -> Unit,
    onScheduleClick: (ScheduleWithAlarm) -> Unit,
    onToggleSchedule: (AlarmSchedule, Boolean) -> Unit,
    contentPadding: PaddingValues = PaddingValues(bottom = 80.dp),
    highlightedAlarmId: String? = null,
    highlightedScheduleId: String? = null,
    onHighlightFinished: () -> Unit = {},
) {
    val haptic = LocalHapticFeedback.current

    LazyVerticalGrid(
        columns = GridCells.Adaptive(300.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Schedules Section
        if (schedules.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    text = stringResource(R.string.section_schedules),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 4.dp)
                )
            }
            items(
                items = schedules, key = { "schedule_${it.schedule.id}" }) { item ->
                ScheduleItem(
                    scheduleWithAlarm = item,
                    onClick = { onScheduleClick(item) },
                    onToggle = { isChecked ->
                        val type = if (isChecked) HapticFeedbackType.ToggleOff
                        else HapticFeedbackType.ToggleOn
                        haptic.performHapticFeedback(type)
                        onToggleSchedule(item.schedule, isChecked)
                    },
                    isHighlighted = item.schedule.id == highlightedScheduleId,
                    onHighlightFinished = onHighlightFinished
                )
            }
        }

        // All Alarms Section
        if (alarms.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    text = stringResource(R.string.section_all_alarm),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 4.dp, top = 16.dp, bottom = 4.dp)
                )
            }
            items(
                items = alarms, key = { "alarm_${it.id}" }) { alarm ->
                AlarmItem(
                    alarm = alarm,
                    onClick = { onAlarmClick(alarm) },
                    onToggle = { isChecked -> onToggleAlarm(alarm, isChecked) },
                    isHighlighted = alarm.id == highlightedAlarmId,
                    onHighlightFinished = onHighlightFinished
                )
            }
        }
    }
}

/**
 * A list item representing a single alarm.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AlarmItem(
    alarm: Alarm,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onToggle: (Boolean) -> Unit,
    isHighlighted: Boolean = false,
    onHighlightFinished: () -> Unit = {},
) {
    val containerColor = MaterialTheme.colorScheme.surfaceContainer
    val highlightColor = MaterialTheme.colorScheme.primaryContainer
    val haptic = LocalHapticFeedback.current
    val animatedColor = remember { Animatable(containerColor) }

    LaunchedEffect(isHighlighted) {
        if (isHighlighted) {
            // Blink twice
            repeat(2) {
                animatedColor.animateTo(highlightColor, animationSpec = tween(200))
                animatedColor.animateTo(containerColor, animationSpec = tween(200))
            }
            onHighlightFinished()
        }
    }

    Card(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
            onClick()
        },
        colors = CardDefaults.cardColors(
            containerColor = animatedColor.value
        ),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                modifier = Modifier.weight(1f),
                text = alarm.name,
                style = MaterialTheme.typography.titleLarge,
            )

            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.Reject)
                    onToggle(true)
                },
            ) {
                Text(stringResource(R.string.button_start))
            }
        }
    }
}

/**
 * A list item representing a single schedule.
 */
@Composable
fun ScheduleItem(
    scheduleWithAlarm: ScheduleWithAlarm,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onToggle: (Boolean) -> Unit,
    isHighlighted: Boolean = false,
    onHighlightFinished: () -> Unit = {},
) {
    val haptic = LocalHapticFeedback.current

    val schedule = scheduleWithAlarm.schedule
    val alarm = scheduleWithAlarm.alarm

    val containerColor = MaterialTheme.colorScheme.surfaceContainer
    val highlightColor = MaterialTheme.colorScheme.primaryContainer
    val animatedColor = remember { Animatable(containerColor) }

    LaunchedEffect(isHighlighted) {
        if (isHighlighted) {
            // Blink twice
            repeat(2) {
                animatedColor.animateTo(highlightColor, animationSpec = tween(200))
                animatedColor.animateTo(containerColor, animationSpec = tween(200))
            }
            onHighlightFinished()
        }
    }

    Card(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
            onClick()
        },
        colors = CardDefaults.cardColors(
            containerColor = animatedColor.value
        ),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = TimeUtils.formatScheduleTitle(
                        androidx.compose.ui.platform.LocalContext.current,
                        schedule.hour,
                        schedule.minute,
                        schedule.daysOfWeek
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = alarm.name,
                    style = MaterialTheme.typography.titleLarge,
                )
            }

            Switch(
                checked = schedule.isEnabled, onCheckedChange = onToggle,
            )
        }
    }
}
