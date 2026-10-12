package com.github.jimmy90109.geoalarm.ui.screens.place_reminders.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.github.jimmy90109.geoalarm.R
import com.github.jimmy90109.geoalarm.ui.components.DashedAddListItem
import com.github.jimmy90109.geoalarm.ui.theme.GeoAlarmTheme
import com.github.jimmy90109.geoalarm.ui.viewmodel.PlaceReminderListUiState
import com.github.jimmy90109.geoalarm.ui.viewmodel.PlaceReminderPermissionState

private enum class PlaceReminderListContentState {
    Loading,
    Empty,
    Content,
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PlaceReminderListContent(
    state: PlaceReminderListUiState,
    permissionState: PlaceReminderPermissionState,
    contentPadding: PaddingValues,
    loadingTopPadding: PaddingValues,
    emptyTopPadding: PaddingValues,
    onPermissionPrimaryAction: () -> Unit,
    onAddReminder: () -> Unit,
    onReminderClick: (String) -> Unit,
    onReminderEnabledChange: (String, Boolean) -> Unit,
    highlightedReminderId: String? = null,
    onHighlightFinished: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val enabledReminders = state.reminders.filter { it.reminder.enabled }
    val savedReminders = state.reminders.filterNot { it.reminder.enabled }
    val targetState = when {
        state.isLoading -> PlaceReminderListContentState.Loading
        state.reminders.isEmpty() -> PlaceReminderListContentState.Empty
        else -> PlaceReminderListContentState.Content
    }

    AnimatedContent(
        targetState = targetState,
        transitionSpec = {
            (fadeIn() + slideInVertically { it / 12 })
                .togetherWith(fadeOut() + slideOutVertically { -it / 12 })
        },
        label = "PlaceReminderListContentTransition",
        modifier = modifier,
    ) { contentState ->
        when (contentState) {
            PlaceReminderListContentState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(loadingTopPadding),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    LoadingIndicator(modifier = Modifier.size(64.dp))
                }
            }

            PlaceReminderListContentState.Empty -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(emptyTopPadding)
                        .padding(horizontal = 36.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Spacer(modifier = Modifier.weight(1f))
                    PlaceReminderEmptyState(
                        onAddReminder = onAddReminder,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(modifier = Modifier.weight(2f))
                }
            }

            PlaceReminderListContentState.Content -> {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(300.dp),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = contentPadding,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (enabledReminders.isNotEmpty() && !permissionState.canEnableReminder) {
                        item(
                            key = "place_reminder_permission_banner",
                            span = { GridItemSpan(maxLineSpan) },
                        ) {
                            PlaceReminderPermissionBanner(
                                permissionState = permissionState,
                                onPrimaryAction = onPermissionPrimaryAction,
                            )
                        }
                    }
                    if (enabledReminders.isNotEmpty()) {
                        item(
                            key = "place_reminder_section_enabled",
                            span = { GridItemSpan(maxLineSpan) },
                        ) {
                            PlaceReminderSectionHeader(text = stringResource(R.string.place_reminder_section_enabled))
                        }
                        items(enabledReminders, key = { it.reminder.id }) { reminder ->
                            PlaceReminderCard(
                                reminderWithItems = reminder,
                                onClick = { onReminderClick(reminder.reminder.id) },
                                onEnabledChange = { enabled ->
                                    onReminderEnabledChange(reminder.reminder.id, enabled)
                                },
                                isHighlighted = reminder.reminder.id == highlightedReminderId,
                                onHighlightFinished = onHighlightFinished,
                                modifier = Modifier.animateItem(),
                            )
                        }
                    }
                    if (state.reminders.isNotEmpty()) {
                        item(
                            key = "place_reminder_section_saved",
                            span = { GridItemSpan(maxLineSpan) },
                        ) {
                            PlaceReminderSectionHeader(text = stringResource(R.string.place_reminder_section_saved))
                        }
                    }
                    if (savedReminders.isNotEmpty()) {
                        items(savedReminders, key = { it.reminder.id }) { reminder ->
                            PlaceReminderCard(
                                reminderWithItems = reminder,
                                onClick = { onReminderClick(reminder.reminder.id) },
                                onEnabledChange = { enabled ->
                                    onReminderEnabledChange(reminder.reminder.id, enabled)
                                },
                                isHighlighted = reminder.reminder.id == highlightedReminderId,
                                onHighlightFinished = onHighlightFinished,
                                modifier = Modifier.animateItem(),
                            )
                        }
                    }
                    if (state.reminders.isNotEmpty()) {
                        item(
                            key = "place_reminder_add",
                            span = {
                                if (savedReminders.isEmpty()) {
                                    GridItemSpan(maxLineSpan)
                                } else {
                                    GridItemSpan(1)
                                }
                            },
                        ) {
                            DashedAddListItem(
                                text = stringResource(R.string.place_reminder_add),
                                onClick = onAddReminder,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(name = "Place reminder list empty", widthDp = 360, heightDp = 720)
@Composable
private fun PlaceReminderListContentEmptyPreview() {
    GeoAlarmTheme {
        Surface {
            PlaceReminderListContent(
                state = PlaceReminderListUiState(isLoading = false),
                permissionState = PlaceReminderPermissionState(),
                contentPadding = PaddingValues(16.dp),
                loadingTopPadding = PaddingValues(top = 24.dp),
                emptyTopPadding = PaddingValues(),
                onPermissionPrimaryAction = {},
                onAddReminder = {},
                onReminderClick = {},
                onReminderEnabledChange = { _, _ -> },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
