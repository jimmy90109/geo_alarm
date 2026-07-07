package com.github.jimmy90109.geoalarm.ui.screens.place_reminders.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.decode.VideoFrameDecoder
import coil.request.ImageRequest
import com.github.jimmy90109.geoalarm.R
import com.github.jimmy90109.geoalarm.data.MaxPlaceReminderAttachments
import com.github.jimmy90109.geoalarm.data.PlaceReminderAttachment
import com.github.jimmy90109.geoalarm.data.PlaceReminderAttachmentType
import com.github.jimmy90109.geoalarm.ui.viewmodel.PlaceReminderEditUiState

@Composable
internal fun PlaceReminderAttachmentEditor(
    state: PlaceReminderEditUiState,
    onPickAttachments: () -> Unit,
    onRemoveAttachment: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = stringResource(R.string.place_reminder_attachments),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
        if (state.isAddingAttachments) {
            Text(
                text = stringResource(R.string.place_reminder_adding_attachments),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = if (state.attachmentLimitReached) {
                stringResource(R.string.place_reminder_attachments_limit_reached, MaxPlaceReminderAttachments)
            } else {
                stringResource(R.string.place_reminder_attachments_limit, MaxPlaceReminderAttachments)
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            state.attachments.sortedBy { it.sortOrder }.forEach { attachment ->
                AttachmentPreviewTile(
                    attachment = attachment,
                    onRemove = { onRemoveAttachment(attachment.id) },
                )
            }
            AttachmentAddTile(
                enabled = !state.isAddingAttachments && !state.attachmentLimitReached,
                onClick = onPickAttachments,
            )
        }
    }
}

@Composable
private fun AttachmentPreviewTile(
    attachment: PlaceReminderAttachment,
    onRemove: () -> Unit,
) {
    val isVideo = attachment.type == PlaceReminderAttachmentType.VIDEO
    val context = LocalContext.current
    val imageRequest = remember(attachment.localPath, isVideo) {
        ImageRequest.Builder(context)
            .data(attachment.localPath)
            .crossfade(true)
            .apply {
                if (isVideo) decoderFactory(VideoFrameDecoder.Factory())
            }
            .build()
    }
    Box(
        modifier = Modifier
            .size(104.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        AsyncImage(
            model = imageRequest,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
        if (isVideo) {
            Icon(
                Icons.Filled.Videocam,
                contentDescription = null,
                tint = androidx.compose.ui.graphics.Color.White,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp),
            )
        }
        Surface(
            onClick = onRemove,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(6.dp)
                .size(28.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = stringResource(R.string.delete),
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Composable
private fun AttachmentAddTile(
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val outlineColor = if (enabled) {
        MaterialTheme.colorScheme.outline
    } else {
        MaterialTheme.colorScheme.outline.copy(alpha = 0.38f)
    }
    val iconColor = if (enabled) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
    }
    Box(
        modifier = Modifier
            .size(104.dp)
            .clip(RoundedCornerShape(24.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .drawDottedOutline(outlineColor, RoundedCornerShape(24.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Filled.Add,
            contentDescription = stringResource(R.string.place_reminder_add_attachment),
            tint = iconColor,
            modifier = Modifier.size(32.dp),
        )
    }
}

private fun Modifier.drawDottedOutline(
    color: androidx.compose.ui.graphics.Color,
    shape: RoundedCornerShape,
): Modifier = drawBehind {
    val strokeWidth = 1.5.dp.toPx()
    drawRoundRect(
        color = color,
        cornerRadius = CornerRadius(24.dp.toPx(), 24.dp.toPx()),
        style = Stroke(
            width = strokeWidth,
            cap = StrokeCap.Round,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(1.dp.toPx(), 7.dp.toPx())),
        ),
    )
}
