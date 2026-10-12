package com.github.jimmy90109.geoalarm.utils

import android.content.Context
import com.github.jimmy90109.geoalarm.data.PaymentShortcut

object PaymentShortcutAvailability {
    fun installedShortcuts(context: Context): List<PaymentShortcut> =
        installedShortcuts { packageName ->
            context.packageManager.getLaunchIntentForPackage(packageName) != null
        }

    fun isInstalled(context: Context, shortcut: PaymentShortcut): Boolean =
        context.packageManager.getLaunchIntentForPackage(shortcut.packageName) != null

    internal fun installedShortcuts(
        hasLaunchIntent: (packageName: String) -> Boolean,
    ): List<PaymentShortcut> = PaymentShortcut.entries.filter { shortcut ->
        hasLaunchIntent(shortcut.packageName)
    }
}
