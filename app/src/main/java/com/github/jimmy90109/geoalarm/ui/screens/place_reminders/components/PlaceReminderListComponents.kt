package com.github.jimmy90109.geoalarm.ui.screens.place_reminders.components

import androidx.compose.animation.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.github.jimmy90109.geoalarm.R
import com.github.jimmy90109.geoalarm.data.PlaceReminderWithItems
import com.github.jimmy90109.geoalarm.ui.components.ActionBanner
import com.github.jimmy90109.geoalarm.ui.components.ActionBannerButton
import com.github.jimmy90109.geoalarm.ui.components.AlarmIconBadge
import com.github.jimmy90109.geoalarm.ui.viewmodel.PlaceReminderPermissionState
import kotlinx.coroutines.launch

@Composable
fun PlaceReminderSectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 4.dp),
    )
}

@Composable
fun PlaceReminderPermissionBanner(
    permissionState: PlaceReminderPermissionState,
    onPrimaryAction: () -> Unit,
) {
    val missing = listOfNotNull(
        stringResource(R.string.place_reminder_missing_precise).takeIf { !permissionState.hasPreciseLocation },
        stringResource(R.string.place_reminder_missing_background).takeIf { !permissionState.hasBackgroundLocation },
        stringResource(R.string.place_reminder_missing_notifications).takeIf { !permissionState.hasNotifications },
        stringResource(R.string.place_reminder_missing_location_service).takeIf { !permissionState.isLocationServiceEnabled },
    )

    ActionBanner(
        icon = Icons.Default.ErrorOutline,
        message = missing.joinToString(" · "),
        containerColor = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
    ) {
        ActionBannerButton(
            text = stringResource(R.string.place_reminder_fix_permissions),
            onClick = onPrimaryAction,
            modifier = Modifier
                .weight(1f)
                .height(48.dp),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaceReminderInfoSheet(onDismissRequest: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    fun dismissWithAnimation() {
        coroutineScope.launch {
            sheetState.hide()
            onDismissRequest()
        }
    }

    ModalBottomSheet(
        onDismissRequest = ::dismissWithAnimation,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Text(
                text = stringResource(R.string.place_reminder_help_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = stringResource(R.string.place_reminder_help_body),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            ReminderInfoRow(
                title = stringResource(R.string.place_reminder_help_alarm_title),
                body = stringResource(R.string.place_reminder_help_alarm_body),
            )
            HorizontalDivider()
            ReminderInfoRow(
                title = stringResource(R.string.place_reminder_help_reminder_title),
                body = stringResource(R.string.place_reminder_help_reminder_body),
            )
            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                    dismissWithAnimation()
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.place_reminder_help_done))
            }
        }
    }
}

@Composable
private fun ReminderInfoRow(
    title: String,
    body: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun PlaceReminderCard(
    reminderWithItems: PlaceReminderWithItems,
    onClick: () -> Unit,
    onEnabledChange: (Boolean) -> Unit,
    isHighlighted: Boolean = false,
    onHighlightFinished: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val reminder = reminderWithItems.reminder
    val containerColor = MaterialTheme.colorScheme.surfaceContainer
    val highlightColor = MaterialTheme.colorScheme.primaryContainer
    val animatedColor = remember { Animatable(containerColor) }

    LaunchedEffect(isHighlighted) {
        if (isHighlighted) {
            repeat(2) {
                animatedColor.animateTo(highlightColor, animationSpec = tween(200))
                animatedColor.animateTo(containerColor, animationSpec = tween(200))
            }
            onHighlightFinished()
        }
    }

    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = animatedColor.value),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AlarmIconBadge(iconKey = reminder.iconKey)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = reminder.placeName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = reminder.title,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Switch(checked = reminder.enabled, onCheckedChange = onEnabledChange)
        }
    }
}
