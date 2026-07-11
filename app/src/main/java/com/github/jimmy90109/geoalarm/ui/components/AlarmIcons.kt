package com.github.jimmy90109.geoalarm.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.LocalPostOffice
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Train
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.github.jimmy90109.geoalarm.data.DEFAULT_ALARM_ICON_KEY

data class AlarmIconOption(
    val key: String,
    val icon: ImageVector
)

val AlarmIconOptions = listOf(
    AlarmIconOption(key = DEFAULT_ALARM_ICON_KEY, icon = Icons.Default.LocationOn),
    AlarmIconOption(key = "home", icon = Icons.Default.Home),
    AlarmIconOption(key = "work", icon = Icons.Default.Work),
    AlarmIconOption(key = "school", icon = Icons.Default.School),
    AlarmIconOption(key = "bus", icon = Icons.Default.DirectionsBus),
    AlarmIconOption(key = "train", icon = Icons.Default.Train),
    AlarmIconOption(key = "walk", icon = Icons.AutoMirrored.Filled.DirectionsWalk),
    AlarmIconOption(key = "bike", icon = Icons.AutoMirrored.Filled.DirectionsBike),
    AlarmIconOption(key = "car", icon = Icons.Default.DirectionsCar),
    AlarmIconOption(key = "gym", icon = Icons.Default.FitnessCenter),
    AlarmIconOption(key = "supermarket", icon = Icons.Default.ShoppingCart),
    AlarmIconOption(key = "post_office", icon = Icons.Default.LocalPostOffice)
)

fun alarmIconForKey(key: String): AlarmIconOption {
    val normalizedKey = normalizeAlarmIconKey(key)
    return AlarmIconOptions.firstOrNull { it.key == normalizedKey } ?: AlarmIconOptions.first()
}

fun normalizeAlarmIconKey(rawKey: String): String {
    val key = rawKey.trim().lowercase()
    return AlarmIconOptions.firstOrNull { it.key == key }?.key ?: DEFAULT_ALARM_ICON_KEY
}

@Composable
fun AlarmIconBadge(
    iconKey: String,
    modifier: Modifier = Modifier
) {
    val option = alarmIconForKey(iconKey)
    Icon(
        imageVector = option.icon,
        contentDescription = option.key,
        modifier = modifier.size(28.dp),
        tint = LocalContentColor.current
    )
}
