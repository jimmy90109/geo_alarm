package com.github.jimmy90109.geoalarm.utils

import android.app.PendingIntent
import android.content.Context
import android.graphics.drawable.Icon
import android.os.Bundle
import androidx.core.app.NotificationCompat
import com.github.jimmy90109.geoalarm.R
import com.github.jimmy90109.geoalarm.service.GeoAlarmService.MonitoringZone
import io.github.d4viddf.hyperisland_kit.HyperAction
import io.github.d4viddf.hyperisland_kit.HyperIslandNotification
import io.github.d4viddf.hyperisland_kit.HyperPicture
import io.github.d4viddf.hyperisland_kit.models.ImageTextInfoLeft
import io.github.d4viddf.hyperisland_kit.models.ImageTextInfoRight
import io.github.d4viddf.hyperisland_kit.models.PicInfo
import io.github.d4viddf.hyperisland_kit.models.TextInfo

/**
 * Helper object for applying Xiaomi HyperOS Dynamic Island notification extras.
 * Falls back gracefully on non-Xiaomi devices.
 */
object HyperIslandHelper {

    private const val BUSINESS_ID = "geo_alarm"
    private const val ICON_KEY = "app_icon"
    private const val LOCATION_ICON_KEY = "location_icon"
    private const val FLAG_ICON_KEY = "flag_icon"
    
    private const val PROGRESS_COLOR = "#FFFFFF" // White

    /**
     * Check if HyperIsland notifications are supported on this device
     */
    fun isSupported(context: Context): Boolean {
        return try {
            HyperIslandNotification.isSupported(context)
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Apply HyperIsland extras to a notification builder for progress tracking
     * 
     * SmallIslandArea: Circular progress ring with AppIcon inside
     * BigIslandArea: Left side = AppIcon + destination name, Right side = percentage
     * Actions: Cancel button
     * 
     * @param context Application context
     * @param builder NotificationCompat.Builder to modify
     * @param alarmName Alarm name to display
     * @param progress Current progress (0-100), 0 for FAR zone
     * @param remainingDistance Remaining distance in meters
     * @param zone Current monitoring zone (FAR/MID/NEAR)
     * @param cancelPendingIntent PendingIntent to cancel the alarm
     * @return Modified NotificationCompat.Builder
     */
    fun applyProgressExtras(
        context: Context,
        builder: NotificationCompat.Builder,
        alarmName: String,
        progress: Int,
        remainingDistance: Int,
        zone: MonitoringZone,
        cancelPendingIntent: PendingIntent
    ): NotificationCompat.Builder {
        if (!isSupported(context)) {
            return builder
        }

        return try {
            // Determine progress value (0% for FAR zone)
            val displayProgress = when (zone) {
                MonitoringZone.FAR -> 0
                else -> progress
            }
            
            // Right side text - percentage or power saving message
            val rightText = when (zone) {
                MonitoringZone.FAR -> context.getString(R.string.notification_power_saving)
                else -> "$progress%"
            }

            val hyperBuilder = HyperIslandNotification.Builder(
                context,
                BUSINESS_ID,
                context.getString(R.string.notification_title, alarmName)
            )
                .setSmallWindowTarget("${context.packageName}.MainActivity")
                // Register icon resources
                .addPicture(HyperPicture(ICON_KEY, context, R.drawable.ic_notification))
                .addPicture(HyperPicture(LOCATION_ICON_KEY, context, R.drawable.ic_notification))
                .addPicture(HyperPicture(FLAG_ICON_KEY, context, R.drawable.ic_notification))
                // Set base info content (fallback for card expansion)
                .setBaseInfo(
                    title = context.getString(R.string.notification_title, alarmName),
                    content = when (zone) {
                        MonitoringZone.FAR -> context.getString(R.string.notification_power_saving)
                        MonitoringZone.MID, MonitoringZone.NEAR -> 
                            context.getString(R.string.notification_distance, remainingDistance, progress)
                    },
                    pictureKey = ICON_KEY
                )
                // Configure island behavior (priority 1 = normal)
                .setIslandConfig(priority = 1)
                // Prevent auto-expansion on updates
                .setIslandFirstFloat(false)
                // Small Island: Circular progress ring with icon inside
                .setSmallIslandCircularProgress(
                    ICON_KEY,        // pictureKey
                    displayProgress, // progress
                    PROGRESS_COLOR,  // colorProgress
                    null,            // colorProgressEnd (optional)
                    false            // isCCW (counter-clockwise)
                )
                // Big Island: Left (icon + text), Right (percentage text)
                .setBigIslandInfo(
                    left = ImageTextInfoLeft(
                        type = 1,
                        picInfo = PicInfo(type = 1, pic = ICON_KEY),
                        textInfo = TextInfo(title = alarmName, content = null)
                    ),
                    centerText = TextInfo(title = rightText),// TextInfo (center)
                )
                // Add Cancel Action as TextButton
                .setTextButtons(
                    HyperAction(
                        key = "cancel",
                        title = context.getString(R.string.notification_cancel),
                        icon = Icon.createWithResource(context, android.R.drawable.ic_menu_close_clear_cancel),
                        pendingIntent = cancelPendingIntent,
                        actionIntentType = 1 // Activity
                    )
                )

            // Add simple linear progress bar (no icons) for MID and NEAR zones
            if (zone != MonitoringZone.FAR) {
                hyperBuilder.setProgressBar(
                    progress,   // progress
                    color = PROGRESS_COLOR, // color
                )
            }

            // Build payloads
            val jsonPayload = hyperBuilder.buildJsonParam()
            val resBundle = hyperBuilder.buildResourceBundle()

            // Attach HyperOS extras
            val extras = Bundle()
            extras.putString("miui.focus.param", jsonPayload)
            extras.putAll(resBundle)
            builder.addExtras(extras)

            builder
        } catch (e: Exception) {
            // Fallback to original builder on any error
            builder
        }
    }

    /**
     * Apply HyperIsland extras for arrival notification
     * 
     * Actions: Close (Turn Off) button
     * 
     * @param context Application context
     * @param builder NotificationCompat.Builder to modify
     * @param alarmName Alarm name to display
     * @param turnOffPendingIntent PendingIntent to turn off the alarm
     * @return Modified NotificationCompat.Builder
     */
    fun applyArrivalExtras(
        context: Context,
        builder: NotificationCompat.Builder,
        alarmName: String,
        turnOffPendingIntent: PendingIntent
    ): NotificationCompat.Builder {
        if (!isSupported(context)) {
            return builder
        }

        return try {
            val hyperBuilder = HyperIslandNotification.Builder(
                context,
                BUSINESS_ID,
                context.getString(R.string.notification_arrived_title, alarmName)
            )
                .setSmallWindowTarget("${context.packageName}.MainActivity")
                // Register icon resources
                .addPicture(HyperPicture(ICON_KEY, context, R.drawable.ic_notification))
                // Set base info content for arrival
                .setBaseInfo(
                    title = context.getString(R.string.notification_arrived_title, alarmName),
                    content = context.getString(R.string.notification_arrived_text),
                    pictureKey = ICON_KEY
                )
                // Configure Island (priority 2 = popup)
                .setIslandConfig(priority = 2)
                // Small Island: Full progress ring (100%) with icon
                .setSmallIslandCircularProgress(
                    ICON_KEY,       // pictureKey
                    100,            // progress
                    PROGRESS_COLOR, // colorProgress
                    null,           // colorProgressEnd
                    false           // isCCW
                )
                // Big Island: Left (icon + title), Right (100%)
                .setBigIslandInfo(
                    left = ImageTextInfoLeft(
                        type = 1,
                        picInfo = PicInfo(type = 1, pic = ICON_KEY),
                        textInfo = TextInfo(title = alarmName, content = null)
                    ),
                    centerText = TextInfo(title = "100%"),
                )
                // Add Turn Off Action as TextButton
                .setTextButtons(
                    HyperAction(
                        key = "turn_off",
                        title = context.getString(R.string.notification_turn_off),
                        icon = Icon.createWithResource(context, android.R.drawable.ic_lock_power_off),
                        pendingIntent = turnOffPendingIntent,
                        actionIntentType = 1 // Activity
                    )
                )

            // Build payloads
            val jsonPayload = hyperBuilder.buildJsonParam()
            val resBundle = hyperBuilder.buildResourceBundle()

            // Attach HyperOS extras
            val extras = Bundle()
            extras.putString("miui.focus.param", jsonPayload)
            extras.putAll(resBundle)
            builder.addExtras(extras)

            builder
        } catch (e: Exception) {
            // Fallback to original builder on any error
            builder
        }
    }
}
