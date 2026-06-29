package com.github.jimmy90109.geoalarm.ui.components

import androidx.annotation.StringRes
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalFloatingToolbar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.github.jimmy90109.geoalarm.R
import com.github.jimmy90109.geoalarm.ui.theme.GeoAlarmTheme

enum class NavTab(
    @StringRes val labelRes: Int, val iconVec: ImageVector
) {
    HOME(R.string.tab_alarms, Icons.Filled.Alarm),
    REMINDERS(R.string.tab_reminders, Icons.Filled.Notifications),
    SETTINGS(R.string.settings, Icons.Filled.Settings)
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun BottomNavBar(
    modifier: Modifier = Modifier,
    currentTab: NavTab,
    onHomeClick: () -> Unit,
    onRemindersClick: () -> Unit,
    onSettingsClick: () -> Unit,
    showSettingsUpdateDot: Boolean = false,
) {
    HorizontalFloatingToolbar(
        expanded = true,
        modifier = modifier,
    ) {
        val tabs = NavTab.entries.toTypedArray()
        val haptic = LocalHapticFeedback.current
        tabs.forEach { tab ->
            val selected = currentTab == tab
            val onClick = when (tab) {
                NavTab.HOME -> onHomeClick
                NavTab.REMINDERS -> onRemindersClick
                NavTab.SETTINGS -> onSettingsClick
            }

            Box {
                if (selected) {
                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onClick()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.animateContentSize()
                        ) {
                            Icon(
                                tab.iconVec,
                                contentDescription = stringResource(tab.labelRes),
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = stringResource(tab.labelRes),
                                modifier = Modifier.padding(start = 8.dp),
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                } else {
                    TextButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onClick()
                        },
                    ) {
                        Text(
                            text = stringResource(tab.labelRes),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                if (tab == NavTab.SETTINGS && showSettingsUpdateDot) {
                    Surface(
                        color = MaterialTheme.colorScheme.error,
                        shape = CircleShape,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                            .size(8.dp)
                    ) {}
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AppNavigationRail(
    modifier: Modifier = Modifier,
    currentTab: NavTab,
    onHomeClick: () -> Unit,
    onRemindersClick: () -> Unit,
    onSettingsClick: () -> Unit,
    showSettingsUpdateDot: Boolean = false,
) {
    VerticalFloatingToolbar(
        expanded = true,
        modifier = modifier.padding(16.dp),
    ) {
        val haptic = LocalHapticFeedback.current
        val tabs = NavTab.entries.toTypedArray()

        tabs.forEach { tab ->
            val selected = currentTab == tab
            val onClick = when (tab) {
                NavTab.HOME -> onHomeClick
                NavTab.REMINDERS -> onRemindersClick
                NavTab.SETTINGS -> onSettingsClick
            }

            Box {
                if (selected) {
                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onClick()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                    ) {
                        Icon(
                            tab.iconVec,
                            contentDescription = stringResource(tab.labelRes),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                } else {
                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onClick()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        contentPadding = PaddingValues(
                            horizontal = 16.dp, vertical = 16.dp
                        ),
                    ) {
                        Icon(
                            tab.iconVec,
                            contentDescription = stringResource(tab.labelRes),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                if (tab == NavTab.SETTINGS && showSettingsUpdateDot) {
                    Surface(
                        color = MaterialTheme.colorScheme.error,
                        shape = CircleShape,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                            .size(8.dp)
                    ) {}
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun BottomNavBarPreview() {
    GeoAlarmTheme {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
            BottomNavBar(
                currentTab = NavTab.HOME,
                onHomeClick = {},
                onRemindersClick = {},
                onSettingsClick = {},
                showSettingsUpdateDot = true
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun NavigationRailPreview() {
    GeoAlarmTheme {
        AppNavigationRail(
            currentTab = NavTab.HOME,
            onHomeClick = {},
            onRemindersClick = {},
            onSettingsClick = {},
            showSettingsUpdateDot = true
        )
    }
}
