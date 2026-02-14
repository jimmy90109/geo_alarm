package com.github.jimmy90109.geoalarm.ui.components

import androidx.annotation.StringRes
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
    HOME(R.string.tab_alarms, Icons.Filled.Alarm), SETTINGS(
        R.string.settings, Icons.Filled.Settings
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun BottomNavBar(
    modifier: Modifier = Modifier,
    currentTab: NavTab,
    onHomeClick: () -> Unit,
    onSettingsClick: () -> Unit,
    showSettingsUpdateDot: Boolean = false,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 3.dp,
        shadowElevation = 4.dp,
        shape = CircleShape,
        modifier = modifier,
    ) {
        ButtonGroup(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 8.dp),
        ) {
            val tabs = NavTab.entries.toTypedArray()
            val haptic = LocalHapticFeedback.current
            tabs.forEachIndexed { index, tab ->
                val selected = currentTab == tab
                val onClick = when (tab) {
                    NavTab.HOME -> onHomeClick
                    NavTab.SETTINGS -> onSettingsClick
                }

                val shape = when (index) {
                    0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                    tabs.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                    else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                }

                Box {
                    ToggleButton(
                        checked = selected, onCheckedChange = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onClick()
                        }, shapes = shape
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(vertical = 4.dp, horizontal = 8.dp)
                                .animateContentSize()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(24.dp)) {
                                    Icon(
                                        tab.iconVec,
                                        contentDescription = stringResource(tab.labelRes),
                                        modifier = Modifier.fillMaxSize(),
                                    )
                                }
                                if (selected) {
                                    Text(
                                        stringResource(tab.labelRes),
                                        modifier = Modifier.padding(start = 8.dp),
                                        style = MaterialTheme.typography.titleMedium,
                                    )
                                }
                            }
                        }
                    }
                    if (tab == NavTab.SETTINGS && showSettingsUpdateDot) {
                        Surface(
                            color = MaterialTheme.colorScheme.error,
                            shape = CircleShape,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                                .size(8.dp)
                        ) {}
                    }
                }
            }
        }
    }
}

@Composable
fun AppNavigationRail(
    currentTab: NavTab,
    onHomeClick: () -> Unit,
    onSettingsClick: () -> Unit,
    showSettingsUpdateDot: Boolean = false,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 3.dp,
        shadowElevation = 4.dp,
        shape = CircleShape,
        modifier = modifier.padding(16.dp),
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            RailItem(
                selected = currentTab == NavTab.HOME,
                onClick = onHomeClick,
                icon = NavTab.HOME.iconVec,
                label = stringResource(NavTab.HOME.labelRes)
            )

            RailItem(
                selected = currentTab == NavTab.SETTINGS,
                onClick = onSettingsClick,
                icon = NavTab.SETTINGS.iconVec,
                label = stringResource(NavTab.SETTINGS.labelRes),
                showUpdateDot = showSettingsUpdateDot
            )
        }
    }
}

@Composable
private fun RailItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: ImageVector,
    label: String,
    showUpdateDot: Boolean = false,
) {
    val colors = IconButtonDefaults.iconButtonColors(
        containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
        contentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    )
    val haptic = LocalHapticFeedback.current

    Surface(
        color = colors.containerColor,
        shape = CircleShape,
        modifier = Modifier
            .clip(CircleShape)
            .clickable(onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            })
    ) {
        Box {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 20.dp)
            ) {
                Box(modifier = Modifier.size(24.dp)) {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = colors.contentColor,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.contentColor
                )
            }
            if (showUpdateDot) {
                Surface(
                    color = MaterialTheme.colorScheme.error,
                    shape = CircleShape,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .size(8.dp)
                ) {}
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
            onSettingsClick = {},
            showSettingsUpdateDot = true
        )
    }
}
