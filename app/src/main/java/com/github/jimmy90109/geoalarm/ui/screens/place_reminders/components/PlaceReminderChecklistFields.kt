package com.github.jimmy90109.geoalarm.ui.screens.place_reminders.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.github.jimmy90109.geoalarm.R

internal val PlaceReminderTextFieldShape = RoundedCornerShape(24.dp)

@Composable
internal fun ChecklistEditRow(
    value: String,
    onValueChange: (String) -> Unit,
    onRemove: () -> Unit,
    dragHandleModifier: Modifier,
    onInputBoundsChanged: (Rect?) -> Unit = {},
) {
    var isFocused by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current
    DisposableEffect(Unit) {
        onDispose { onInputBoundsChanged(null) }
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = Icons.Filled.DragHandle,
            contentDescription = null,
            modifier = dragHandleModifier.padding(end = 8.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        TextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .weight(1f)
                .onGloballyPositioned { onInputBoundsChanged(it.boundsInRoot()) }
                .onFocusChanged {
                    isFocused = it.isFocused
                },
            singleLine = true,
            shape = PlaceReminderTextFieldShape,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                errorContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
                errorIndicatorColor = Color.Transparent,
            ),
        )
        AnimatedVisibility(
            visible = isFocused,
            enter = expandHorizontally(
                animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
                expandFrom = Alignment.Start,
            ) + fadeIn(animationSpec = tween(durationMillis = 120)),
            exit = shrinkHorizontally(
                animationSpec = tween(durationMillis = 160, easing = FastOutSlowInEasing),
                shrinkTowards = Alignment.Start,
            ) + fadeOut(animationSpec = tween(durationMillis = 100)),
        ) {
            IconButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.Reject)
                    onRemove()
                },
                modifier = Modifier.padding(start = 8.dp),
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.errorContainer,
                ) {
                    Box(
                        modifier = Modifier.size(40.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Filled.Delete,
                            contentDescription = stringResource(R.string.delete),
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun ChecklistAddRow(
    value: String,
    onValueChange: (String) -> Unit,
    onAdd: () -> Unit,
    focusRequester: FocusRequester,
    onInputBoundsChanged: (Rect?) -> Unit = {},
    onKeepFocusBoundsChanged: (Rect?) -> Unit = {},
) {
    val canAdd = value.trim().isNotEmpty()
    val haptic = LocalHapticFeedback.current
    DisposableEffect(Unit) {
        onDispose { onInputBoundsChanged(null) }
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .weight(1f)
                .focusRequester(focusRequester)
                .onGloballyPositioned { onInputBoundsChanged(it.boundsInRoot()) },
            singleLine = true,
            placeholder = { Text(stringResource(R.string.place_reminder_new_item)) },
            shape = PlaceReminderTextFieldShape,
        )
        AnimatedVisibility(
            visible = canAdd,
            enter = expandHorizontally(
                animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
                expandFrom = Alignment.Start,
            ) + fadeIn(animationSpec = tween(durationMillis = 120)),
            exit = shrinkHorizontally(
                animationSpec = tween(durationMillis = 160, easing = FastOutSlowInEasing),
                shrinkTowards = Alignment.Start,
            ) + fadeOut(animationSpec = tween(durationMillis = 100)),
        ) {
            DisposableEffect(Unit) {
                onDispose { onKeepFocusBoundsChanged(null) }
            }
            IconButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                    onAdd()
                },
                modifier = Modifier
                    .padding(start = 8.dp)
                    .onGloballyPositioned {
                        onKeepFocusBoundsChanged(it.boundsInRoot())
                    },
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                ) {
                    Box(
                        modifier = Modifier.size(40.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Filled.Add,
                            contentDescription = stringResource(R.string.place_reminder_add_item),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
            }
        }
    }
}
