package com.github.jimmy90109.geoalarm.ui.components

import androidx.compose.animation.Animatable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.github.jimmy90109.geoalarm.R
import com.github.jimmy90109.geoalarm.data.Alarm
import com.github.jimmy90109.geoalarm.data.AlarmSchedule
import com.github.jimmy90109.geoalarm.data.ScheduleWithAlarm
import com.github.jimmy90109.geoalarm.utils.TimeUtils
import com.google.android.gms.ads.nativead.NativeAd

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
    onAddSchedule: () -> Unit,
    onOpenWidgetPicker: () -> Unit,
    contentPadding: PaddingValues = PaddingValues(bottom = 80.dp),
    highlightedAlarmId: String? = null,
    highlightedScheduleId: String? = null,
    homeNativeAd: NativeAd? = null,
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
        if (schedules.isNotEmpty() || alarms.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    text = stringResource(R.string.section_schedules),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 4.dp)
                )
            }
        }

        if (schedules.isNotEmpty()) {
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
        } else if (alarms.isNotEmpty()) {
             // Show Guide Item if no schedules but alarms exist
            item(span = { GridItemSpan(maxLineSpan) }) {
                ScheduleGuideItem(onClick = onAddSchedule)
            }
        }

        if (alarms.isNotEmpty() && homeNativeAd != null) {
            item(key = "home_native_ad", span = { GridItemSpan(maxLineSpan) }) {
                AnimatedHomeNativeAdCard(nativeAd = homeNativeAd)
            }
        }

        // All Alarms Section
        if (alarms.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 4.dp, top = 16.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.section_all_alarm),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                            onOpenWidgetPicker()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Widgets,
                            contentDescription = stringResource(R.string.open_widget_picker)
                        )
                    }
                }
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

@Composable
private fun AnimatedHomeNativeAdCard(nativeAd: NativeAd) {
    var visible by remember(nativeAd) { mutableStateOf(false) }

    LaunchedEffect(nativeAd) {
        visible = true
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(durationMillis = 520, delayMillis = 140)) +
            expandVertically(
                animationSpec = tween(durationMillis = 820, easing = FastOutSlowInEasing),
                expandFrom = Alignment.Top,
            ) +
            slideInVertically(
                animationSpec = tween(durationMillis = 720, easing = FastOutSlowInEasing)
            ) { it / 10 },
        exit = fadeOut(animationSpec = tween(160)) +
            shrinkVertically(animationSpec = tween(180)) +
            slideOutVertically(animationSpec = tween(180)) { it / 5 },
    ) {
        HomeNativeAdCard(nativeAd = nativeAd)
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
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AlarmIconBadge(iconKey = alarm.iconKey)
                Text(
                    modifier = Modifier.padding(start = 12.dp),
                    text = alarm.name,
                    style = MaterialTheme.typography.titleLarge,
                )
            }

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


/**
 * A guide item encouraging users to add a schedule.
 */
@Composable
fun ScheduleGuideItem(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val stroke = StrokeCap.Round
    val pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
    val borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)

    androidx.compose.foundation.layout.Box(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                onClick()
            })
            .drawBehind {
                drawRoundRect(
                    color = borderColor,
                    style = Stroke(
                        width = 2.dp.toPx(),
                        pathEffect = pathEffect,
                        cap = stroke
                    ),
                    cornerRadius = CornerRadius(12.dp.toPx())
                )
            }
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            androidx.compose.material3.Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(end = 8.dp)
            )
            Text(
                text = stringResource(R.string.schedule_guide_message),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
