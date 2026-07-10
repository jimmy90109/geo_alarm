package com.github.jimmy90109.geoalarm.service

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.github.jimmy90109.geoalarm.MainActivity
import com.github.jimmy90109.geoalarm.R
import com.github.jimmy90109.geoalarm.data.PlaceReminderAttachment
import com.github.jimmy90109.geoalarm.data.PlaceReminderAttachmentType
import com.github.jimmy90109.geoalarm.data.PlaceReminderType
import com.github.jimmy90109.geoalarm.data.PlaceReminderWithItems
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import kotlin.math.absoluteValue

class PlaceReminderNotifier @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    fun notify(reminderWithItems: PlaceReminderWithItems) {
        notifyInternal(
            reminderWithItems = reminderWithItems,
            notificationId = reminderWithItems.reminder.id.hashCode().absoluteValue,
            pendingIntent = placeReminderPendingIntent(reminderWithItems.reminder.id),
        )
    }

    fun notifyPreview(
        reminderWithItems: PlaceReminderWithItems,
        openReminderId: String?,
    ) {
        notifyInternal(
            reminderWithItems = reminderWithItems,
            notificationId = PREVIEW_NOTIFICATION_ID,
            pendingIntent = previewPendingIntent(openReminderId),
        )
    }

    private fun notifyInternal(
        reminderWithItems: PlaceReminderWithItems,
        notificationId: Int,
        pendingIntent: PendingIntent,
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        createChannel()
        val reminder = reminderWithItems.reminder
        val body = notificationBody(context, reminderWithItems)
        val attachmentBitmap = reminderWithItems.sortedAttachments.firstOrNull()
            ?.notificationBitmap()
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.place_reminder_notification_title, reminder.placeName))
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        if (attachmentBitmap != null) {
            builder
                .setLargeIcon(attachmentBitmap)
                .setStyle(
                    NotificationCompat.BigPictureStyle()
                        .bigPicture(attachmentBitmap)
                        .setBigContentTitle(
                            context.getString(R.string.place_reminder_notification_title, reminder.placeName)
                        )
                        .setSummaryText(body)
                )
        } else {
            builder.setStyle(NotificationCompat.BigTextStyle().bigText(body))
        }

        context.getSystemService(NotificationManager::class.java)
            .notify(notificationId, builder.build())
    }

    private fun placeReminderPendingIntent(reminderId: String): PendingIntent =
        PendingIntent.getActivity(
            context,
            reminderId.hashCode().absoluteValue,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                action = MainActivity.ACTION_OPEN_PLACE_REMINDER
                putExtra(MainActivity.EXTRA_PLACE_REMINDER_ID, reminderId)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    private fun previewPendingIntent(openReminderId: String?): PendingIntent =
        PendingIntent.getActivity(
            context,
            PREVIEW_NOTIFICATION_REQUEST_CODE,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                action = MainActivity.ACTION_OPEN_PLACE_REMINDER_PREVIEW
                if (!openReminderId.isNullOrBlank()) {
                    putExtra(MainActivity.EXTRA_PLACE_REMINDER_ID, openReminderId)
                }
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    private fun createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.place_reminder_channel_name),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = context.getString(R.string.place_reminder_channel_description)
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    companion object {
        private const val CHANNEL_ID = "place_reminder_channel"
        private const val PREVIEW_NOTIFICATION_ID = 0x706C6163
        private const val PREVIEW_NOTIFICATION_REQUEST_CODE = 0x70726576

        fun notificationBody(context: Context, reminderWithItems: PlaceReminderWithItems): String {
            val reminder = reminderWithItems.reminder
            val pendingCount = reminderWithItems.sortedItems.count { !it.checked }
            return when {
                reminder.type == PlaceReminderType.CHECKLIST && reminderWithItems.items.isNotEmpty() ->
                    context.resources.getQuantityString(
                        R.plurals.place_reminder_notification_checklist_body,
                        pendingCount,
                        reminder.title,
                        pendingCount,
                    )
                reminder.content.isNotBlank() -> reminder.content
                reminderWithItems.attachments.isNotEmpty() -> {
                    val imageCount = reminderWithItems.attachments.count {
                        it.type == PlaceReminderAttachmentType.IMAGE
                    }
                    val videoCount = reminderWithItems.attachments.count {
                        it.type == PlaceReminderAttachmentType.VIDEO
                    }
                    context.getString(
                        R.string.place_reminder_notification_attachment_body,
                        reminder.title,
                        imageCount,
                        videoCount,
                    )
                }
                else -> reminder.title
            }
        }
    }
}

private fun PlaceReminderAttachment.notificationBitmap(): Bitmap? =
    when (type) {
        PlaceReminderAttachmentType.IMAGE -> decodeSampledBitmap(localPath, NotificationAttachmentMaxSizePx)
        PlaceReminderAttachmentType.VIDEO -> videoFrame(localPath)
    }

private fun decodeSampledBitmap(
    localPath: String,
    maxSizePx: Int,
): Bitmap? {
    if (!File(localPath).exists()) return null
    val boundsOptions = BitmapFactory.Options().apply {
        inJustDecodeBounds = true
    }
    BitmapFactory.decodeFile(localPath, boundsOptions)
    val width = boundsOptions.outWidth
    val height = boundsOptions.outHeight
    if (width <= 0 || height <= 0) return null
    val decodeOptions = BitmapFactory.Options().apply {
        inSampleSize = sampleSizeFor(width, height, maxSizePx)
    }
    return runCatching { BitmapFactory.decodeFile(localPath, decodeOptions) }.getOrNull()
}

private fun sampleSizeFor(
    width: Int,
    height: Int,
    maxSizePx: Int,
): Int {
    var sampleSize = 1
    while (width / sampleSize > maxSizePx || height / sampleSize > maxSizePx) {
        sampleSize *= 2
    }
    return sampleSize
}

private fun videoFrame(localPath: String): Bitmap? {
    if (!File(localPath).exists()) return null
    val retriever = MediaMetadataRetriever()
    return try {
        retriever.setDataSource(localPath)
        retriever.getFrameAtTime(0L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
    } catch (_: RuntimeException) {
        null
    } finally {
        retriever.release()
    }
}

private const val NotificationAttachmentMaxSizePx = 1024
