package com.github.jimmy90109.geoalarm.util

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings

object FullScreenIntentPermissionHelper {
    fun isRequired(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE

    fun canUseFullScreenIntent(context: Context): Boolean {
        if (!isRequired()) return true
        return context.getSystemService(NotificationManager::class.java).canUseFullScreenIntent()
    }

    fun createSettingsIntent(context: Context): Intent {
        val packageUri = Uri.parse("package:${context.packageName}")
        return if (isRequired()) {
            Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT).apply {
                data = packageUri
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            }
        } else {
            createAppDetailsIntent(context)
        }
    }

    fun createAppDetailsIntent(context: Context): Intent {
        return Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
        }
    }
}
