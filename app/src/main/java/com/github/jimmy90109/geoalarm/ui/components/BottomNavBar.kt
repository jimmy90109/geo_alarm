package com.github.jimmy90109.geoalarm.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.github.jimmy90109.geoalarm.R

enum class NavTab {
    HOME, SETTINGS
}

@Composable
fun BottomNavBar(
    currentTab: NavTab,
    onHomeClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
        shadowElevation = 4.dp,
        shape = CircleShape,
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CapsuleNavigationItem(
                selected = currentTab == NavTab.HOME,
                onClick = onHomeClick,
                icon = Icons.Filled.Alarm,
                label = stringResource(R.string.tab_alarms)
            )
            CapsuleNavigationItem(
                selected = currentTab == NavTab.SETTINGS,
                onClick = onSettingsClick,
                icon = Icons.Filled.Settings,
                label = stringResource(R.string.settings)
            )
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
        color = MaterialTheme.colorScheme.surface,
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
            CapsuleNavigationItem(
                selected = currentTab == NavTab.HOME,
                onClick = onHomeClick,
                icon = Icons.Filled.Alarm,
                label = stringResource(R.string.tab_alarms),
                vertical = true
            )

            CapsuleNavigationItem(
                selected = currentTab == NavTab.SETTINGS,
                onClick = onSettingsClick,
                icon = Icons.Filled.Settings,
                label = stringResource(R.string.settings),
                vertical = true
            )
        }
    }
}

@Composable
fun CapsuleNavigationItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    vertical: Boolean = false
) {
    val backgroundColor =
        if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
    val contentColor =
        if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant

    Surface(
        color = backgroundColor,
        shape = CircleShape,
        modifier = modifier
            .clip(CircleShape)
            .clickable(onClick = onClick)
    ) {
        if (vertical) {
            Column(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = contentColor,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = label, color = contentColor,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        } else {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = contentColor,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = label, color = contentColor,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}
