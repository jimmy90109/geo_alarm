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
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.github.jimmy90109.geoalarm.R
import com.github.jimmy90109.geoalarm.data.Alarm
import com.github.jimmy90109.geoalarm.data.AlarmSchedule
import com.github.jimmy90109.geoalarm.data.ScheduleWithAlarm
import com.github.jimmy90109.geoalarm.ui.components.AlarmList
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
                        Text(
                            stringResource(R.string.no_alarms),
                            style = MaterialTheme.typography.bodyLarge,
                        )
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
