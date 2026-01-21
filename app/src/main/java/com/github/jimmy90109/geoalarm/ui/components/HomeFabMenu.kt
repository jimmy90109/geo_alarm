package com.github.jimmy90109.geoalarm.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButtonMenu
import androidx.compose.material3.FloatingActionButtonMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeFloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.github.jimmy90109.geoalarm.R
import com.github.jimmy90109.geoalarm.data.Alarm
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HomeFabMenu(
    expanded: Boolean,
    onToggle: () -> Unit,
    alarms: List<Alarm>,
    onAddSchedule: () -> Unit,
    onAddAlarm: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    FloatingActionButtonMenu(
        modifier = modifier,
        expanded = expanded,
        horizontalAlignment = Alignment.End,
        button = {
            val fabFocusRequester = remember { FocusRequester() }
            LargeFloatingActionButton(
                modifier = Modifier.focusRequester(fabFocusRequester),
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                    onToggle()
                },
                containerColor = if (expanded) MaterialTheme.colorScheme.secondaryContainer
                else MaterialTheme.colorScheme.primaryContainer,
                contentColor = if (expanded) MaterialTheme.colorScheme.onSecondaryContainer
                else MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                val imageVector by remember(expanded) {
                    derivedStateOf {
                        if (expanded) Icons.Filled.Close else Icons.Filled.Add
                    }
                }
                Icon(
                    imageVector = imageVector,
                    contentDescription = stringResource(R.string.add_alarm),
                    modifier = Modifier.size(36.dp)
                )
            }
        },
    ) {
        val fabFocusRequester = remember { FocusRequester() }

        if (alarms.isNotEmpty()) {
            FloatingActionButtonMenuItem(
                modifier = Modifier
                    .semantics {
                        isTraversalGroup = true
                    }
                    .onKeyEvent {
                        if (it.type == KeyEventType.KeyDown && (it.key == Key.DirectionUp || (it.isShiftPressed && it.key == Key.Tab))) {
                            fabFocusRequester.requestFocus()
                            return@onKeyEvent true
                        }
                        return@onKeyEvent false
                    },
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                    onToggle() // Close menu
                    onAddSchedule()
                },
                icon = { Icon(Icons.Filled.DateRange, contentDescription = null) },
                text = { Text(text = stringResource(R.string.new_schedule)) })
        }

        FloatingActionButtonMenuItem(
            modifier = Modifier.semantics {
            isTraversalGroup = true
            customActions = listOf(
                CustomAccessibilityAction(
                    label = "Close menu",
                    action = {
                        onToggle()
                        true
                    },
                )
            )
        },
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onToggle() // Close menu
                onAddAlarm()
            },
            icon = { Icon(Icons.Filled.Alarm, contentDescription = null) },
            text = { Text(text = stringResource(R.string.new_alarm)) })
    }
}
