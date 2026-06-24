package com.github.jimmy90109.geoalarm.data.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

interface AlarmActivationPermissionChecker {
    fun hasPreciseForegroundLocation(): Boolean
    fun hasBackgroundLocation(): Boolean
}

class AndroidAlarmActivationPermissionChecker @Inject constructor(
    @param:ApplicationContext private val context: Context
) : AlarmActivationPermissionChecker {
    override fun hasPreciseForegroundLocation(): Boolean =
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

    override fun hasBackgroundLocation(): Boolean =
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_BACKGROUND_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
}
