package com.github.jimmy90109.geoalarm.ui.screens.place_reminders.components

import android.media.MediaMetadataRetriever
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
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
import com.github.jimmy90109.geoalarm.ui.components.MediaPreviewItem
import com.github.jimmy90109.geoalarm.ui.components.MediaPreviewSelection
import com.github.jimmy90109.geoalarm.ui.components.MediaPreviewType
import com.github.jimmy90109.geoalarm.ui.viewmodel.PlaceReminderEditUiState

private const val PlaceReminderAttachmentGridColumns = 3
private val PlaceReminderAttachmentGridGap = 10.dp
private val PlaceReminderAttachmentTileShape = RoundedCornerShape(24.dp)

@Composable
internal fun PlaceReminderAttachmentEditor(
    state: PlaceReminderEditUiState,
    onPickAttachments: () -> Unit,
    onRemoveAttachment: (String) -> Unit,
    hiddenAttachmentId: String?,
    onAttachmentBoundsChanged: (String, Rect) -> Unit,
    onAttachmentClick: (MediaPreviewSelection) -> Unit,
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
        AttachmentGrid(
            attachments = state.attachments.sortedBy { it.sortOrder },
            addTileEnabled = !state.isAddingAttachments && !state.attachmentLimitReached,
            hiddenAttachmentId = hiddenAttachmentId,
            onPickAttachments = onPickAttachments,
            onRemoveAttachment = onRemoveAttachment,
            onAttachmentBoundsChanged = onAttachmentBoundsChanged,
            onAttachmentClick = onAttachmentClick,
        )
    }
}

@Composable
private fun AttachmentGrid(
    attachments: List<PlaceReminderAttachment>,
    addTileEnabled: Boolean,
    hiddenAttachmentId: String?,
    onPickAttachments: () -> Unit,
    onRemoveAttachment: (String) -> Unit,
    onAttachmentBoundsChanged: (String, Rect) -> Unit,
    onAttachmentClick: (MediaPreviewSelection) -> Unit,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val tileSize = (
            maxWidth - PlaceReminderAttachmentGridGap * (PlaceReminderAttachmentGridColumns - 1)
            ) / PlaceReminderAttachmentGridColumns
        val tiles = attachments.map { AttachmentGridTile.Preview(it) } + AttachmentGridTile.Add

        Column(verticalArrangement = Arrangement.spacedBy(PlaceReminderAttachmentGridGap)) {
            tiles.chunked(PlaceReminderAttachmentGridColumns).forEach { rowTiles ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(PlaceReminderAttachmentGridGap),
                ) {
                    rowTiles.forEach { tile ->
                        when (tile) {
                            AttachmentGridTile.Add -> AttachmentAddTile(
                                enabled = addTileEnabled,
                                onClick = onPickAttachments,
                                modifier = Modifier.size(tileSize),
                            )
                            is AttachmentGridTile.Preview -> AttachmentPreviewTile(
                                attachment = tile.attachment,
                                onRemove = { onRemoveAttachment(tile.attachment.id) },
                                hidden = tile.attachment.id == hiddenAttachmentId,
                                onBoundsChanged = onAttachmentBoundsChanged,
                                onClick = onAttachmentClick,
                                modifier = Modifier.size(tileSize),
                            )
                        }
                    }
                }
            }
        }
    }
}

private sealed interface AttachmentGridTile {
    data object Add : AttachmentGridTile
    data class Preview(val attachment: PlaceReminderAttachment) : AttachmentGridTile
}

@Composable
private fun AttachmentPreviewTile(
    attachment: PlaceReminderAttachment,
    onRemove: () -> Unit,
    hidden: Boolean,
    onBoundsChanged: (String, Rect) -> Unit,
    onClick: (MediaPreviewSelection) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isVideo = attachment.type == PlaceReminderAttachmentType.VIDEO
    val previewItem = remember(attachment) { attachment.toMediaPreviewItem() }
    var itemBounds by remember(attachment.id) { mutableStateOf<Rect?>(null) }
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
        modifier = modifier
            .onGloballyPositioned { coordinates ->
                val bounds = coordinates.boundsInRoot()
                itemBounds = bounds
                onBoundsChanged(attachment.id, bounds)
            }
            .clip(PlaceReminderAttachmentTileShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(enabled = !hidden) {
                itemBounds?.let { bounds ->
                    onClick(MediaPreviewSelection(previewItem, bounds))
                }
            }
            .graphicsLayer {
                alpha = if (hidden) 0f else 1f
            },
    ) {
        AsyncImage(
            model = imageRequest,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
        if (isVideo) {
            val durationMillis = attachment.durationMillis ?: rememberVideoDurationMillis(attachment.localPath)
            VideoDurationBadge(
                durationMillis = durationMillis,
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

internal fun PlaceReminderAttachment.toMediaPreviewItem(): MediaPreviewItem =
    MediaPreviewItem(
        id = id,
        localPath = localPath,
        displayName = displayName,
        type = when (type) {
            PlaceReminderAttachmentType.IMAGE -> MediaPreviewType.IMAGE
            PlaceReminderAttachmentType.VIDEO -> MediaPreviewType.VIDEO
        },
        width = width,
        height = height,
    )

@Composable
private fun VideoDurationBadge(
    durationMillis: Long?,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = Color.Black.copy(alpha = 0.68f),
    ) {
        Text(
            text = formatVideoDuration(durationMillis),
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun rememberVideoDurationMillis(localPath: String): Long? {
    return remember(localPath) {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(localPath)
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull()
                ?.takeIf { it > 0L }
        } catch (_: RuntimeException) {
            null
        } finally {
            retriever.release()
        }
    }
}

private fun formatVideoDuration(durationMillis: Long?): String {
    val totalSeconds = durationMillis
        ?.coerceAtLeast(0L)
        ?.let { (it + 999L) / 1000L }
        ?: return "--:--"
    val seconds = totalSeconds % 60
    val totalMinutes = totalSeconds / 60
    val minutes = totalMinutes % 60
    val hours = totalMinutes / 60
    return if (hours > 0L) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%d:%02d".format(minutes, seconds)
    }
}

@Composable
private fun AttachmentAddTile(
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
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
        modifier = modifier
            .clip(PlaceReminderAttachmentTileShape)
            .clickable(enabled = enabled, onClick = onClick)
            .drawDottedOutline(outlineColor),
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
