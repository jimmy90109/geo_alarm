package com.github.jimmy90109.geoalarm.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.github.jimmy90109.geoalarm.R
import com.github.jimmy90109.geoalarm.data.Alarm
import com.github.jimmy90109.geoalarm.data.AlarmSchedule
import com.github.jimmy90109.geoalarm.data.ScheduleWithAlarm
import com.github.jimmy90109.geoalarm.ui.components.AlarmList
import com.github.jimmy90109.geoalarm.ui.theme.GeoAlarmTheme
import com.github.jimmy90109.geoalarm.ui.viewmodel.HomeListUiState
import com.google.android.gms.ads.nativead.NativeAd

private enum class HomeListContentState {
    Loading,
    Empty,
    Content,
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HomeListContent(
    state: HomeListUiState,
    contentPadding: PaddingValues,
    loadingTopPadding: PaddingValues,
    onAlarmClick: (Alarm) -> Unit,
    onToggleAlarm: (Alarm, Boolean) -> Unit,
    onScheduleClick: (ScheduleWithAlarm) -> Unit,
    onToggleSchedule: (AlarmSchedule, Boolean) -> Unit,
    onAddAlarm: () -> Unit,
    onAddSchedule: () -> Unit,
    onOpenWidgetPicker: () -> Unit,
    highlightedAlarmId: String?,
    highlightedScheduleId: String?,
    homeNativeAd: NativeAd?,
    onHighlightFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val targetState = when {
        state.isLoading -> HomeListContentState.Loading
        state.alarms.isEmpty() && state.schedules.isEmpty() -> HomeListContentState.Empty
        else -> HomeListContentState.Content
    }

    Box(modifier = modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = targetState,
            transitionSpec = {
                (fadeIn() + slideInVertically { it / 12 })
                    .togetherWith(fadeOut() + slideOutVertically { -it / 12 })
            },
            label = "HomeListContentTransition",
        ) { contentState ->
            when (contentState) {
                HomeListContentState.Loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(loadingTopPadding),
                        contentAlignment = Alignment.TopCenter,
                    ) {
                        LoadingIndicator(modifier = Modifier.size(64.dp))
                    }
                }

                HomeListContentState.Empty -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Button(
                            onClick = onAddAlarm,
                            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = null,
                            )
                            Text(
                                text = stringResource(R.string.add_alarm),
                                modifier = Modifier.padding(start = 8.dp),
                                style = MaterialTheme.typography.titleMedium,
                            )
                        }
                    }
                }

                HomeListContentState.Content -> {
                    AlarmList(
                        alarms = state.alarms,
                        schedules = state.schedules,
                        contentPadding = contentPadding,
                        onAlarmClick = onAlarmClick,
                        onToggleAlarm = onToggleAlarm,
                        onScheduleClick = onScheduleClick,
                        onToggleSchedule = onToggleSchedule,
                        onAddAlarm = onAddAlarm,
                        onAddSchedule = onAddSchedule,
                        onOpenWidgetPicker = onOpenWidgetPicker,
                        highlightedAlarmId = highlightedAlarmId,
                        highlightedScheduleId = highlightedScheduleId,
                        homeNativeAd = homeNativeAd,
                        onHighlightFinished = onHighlightFinished,
                    )
                }
            }
        }
    }
}

@Preview(name = "Home list empty", widthDp = 360, heightDp = 720)
@Composable
private fun HomeListContentEmptyPreview() {
    GeoAlarmTheme {
        Surface {
            HomeListContent(
                state = HomeListUiState(isLoading = false),
                contentPadding = PaddingValues(16.dp),
                loadingTopPadding = PaddingValues(top = 24.dp),
                onAlarmClick = {},
                onToggleAlarm = { _, _ -> },
                onScheduleClick = {},
                onToggleSchedule = { _, _ -> },
                onAddAlarm = {},
                onAddSchedule = {},
                onOpenWidgetPicker = {},
                highlightedAlarmId = null,
                highlightedScheduleId = null,
                homeNativeAd = null,
                onHighlightFinished = {},
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
