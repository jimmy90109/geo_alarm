package com.github.jimmy90109.geoalarm.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.animation.animateContentSize
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.github.jimmy90109.geoalarm.R

import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.annotation.StringRes

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
    currentTab: NavTab,
    onHomeClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
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

                ToggleButton(
                    checked = selected, onCheckedChange = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onClick()
                    }, shapes = shape
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .padding(vertical = 4.dp, horizontal = 8.dp)
                            .animateContentSize()
                    ) {
                        Icon(
                            tab.iconVec,
                            contentDescription = stringResource(tab.labelRes),
                            modifier = Modifier.size(24.dp),
                        )
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
        }
    }
}

@Composable
fun AppNavigationRail(
    currentTab: NavTab,
    onHomeClick: () -> Unit,
    onSettingsClick: () -> Unit,
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
                label = stringResource(NavTab.SETTINGS.labelRes)
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
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 20.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = colors.contentColor,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = colors.contentColor
            )
        }
    }
}
