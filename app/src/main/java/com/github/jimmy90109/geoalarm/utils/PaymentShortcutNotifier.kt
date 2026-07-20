package com.github.jimmy90109.geoalarm.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import com.github.jimmy90109.geoalarm.R
import com.github.jimmy90109.geoalarm.data.PaymentShortcut
import com.github.jimmy90109.geoalarm.util.WebPageLauncher

object PaymentShortcutNotifier {
    private const val CHANNEL_ID = "payment_shortcut_channel"
    const val NOTIFICATION_ID = 2001
    const val EXTRA_SHORTCUT_ID = "com.github.jimmy90109.geoalarm.extra.PAYMENT_SHORTCUT_ID"
    private const val REQUEST_CODE = 2001

    internal enum class OpenTarget {
        LaunchApp,
        Market,
        Web,
    }

    fun show(context: Context, shortcut: PaymentShortcut) {
        if (!PaymentShortcutAvailability.isInstalled(context, shortcut)) return

        createNotificationChannel(context)

        val pendingIntent = createNotificationClickPendingIntent(context, shortcut)

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.payment_shortcut_notification_title))
            .setContentText(
                context.getString(
                    R.string.payment_shortcut_notification_text,
                    shortcut.displayName,
                )
            )
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    context.getString(
                        R.string.payment_shortcut_notification_big_text,
                        shortcut.displayName,
                    )
                )
            )
            .setContentIntent(pendingIntent)
            .addAction(
                android.R.drawable.ic_menu_send,
                context.getString(R.string.payment_shortcut_notification_action, shortcut.displayName),
                pendingIntent,
            )
            .setAutoCancel(false)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_NAVIGATION)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setRequestPromotedOngoing(true)
            .setShortCriticalText(shortcut.displayName)

        HyperIslandHelper.applyPaymentShortcutExtras(
            context,
            builder,
            shortcut.displayName,
            pendingIntent,
        )

        val notification = builder.build()

        context.getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, notification)
    }

    fun cancel(context: Context) {
        context.getSystemService(NotificationManager::class.java).cancel(NOTIFICATION_ID)
    }

    fun openPaymentTarget(context: Context, shortcut: PaymentShortcut) {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(shortcut.packageName)
        val marketIntent = Intent(Intent.ACTION_VIEW, Uri.parse(shortcut.playStoreUri)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        when (
            planOpenPaymentTarget(
                hasLaunchIntent = launchIntent != null,
                canHandleMarketIntent = marketIntent.resolveActivity(context.packageManager) != null,
            )
        ) {
            OpenTarget.LaunchApp -> context.startActivity(
                launchIntent!!.apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                }
            )
            OpenTarget.Market -> context.startActivity(marketIntent)
            OpenTarget.Web -> WebPageLauncher.open(context, shortcut.playStoreWebUri)
        }
    }

    private fun createNotificationClickPendingIntent(
        context: Context,
        shortcut: PaymentShortcut,
    ): PendingIntent {
        val intent = Intent(context, PaymentShortcutLaunchActivity::class.java).apply {
            putExtra(EXTRA_SHORTCUT_ID, shortcut.id)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        return PendingIntent.getActivity(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    internal fun planOpenPaymentTarget(
        hasLaunchIntent: Boolean,
        canHandleMarketIntent: Boolean,
    ): OpenTarget = when {
        hasLaunchIntent -> OpenTarget.LaunchApp
        canHandleMarketIntent -> OpenTarget.Market
        else -> OpenTarget.Web
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.payment_shortcut_channel_name),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = context.getString(R.string.payment_shortcut_channel_description)
            lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }
}
