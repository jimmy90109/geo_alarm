package com.github.jimmy90109.geoalarm.ui.screens.place_reminders.components

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitHorizontalTouchSlopOrCancellation
import androidx.compose.foundation.gestures.horizontalDrag
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.github.jimmy90109.geoalarm.R
import com.github.jimmy90109.geoalarm.ui.components.rememberMediaPreviewAspectRatios
import com.github.jimmy90109.geoalarm.ui.components.MediaPreviewItem
import com.github.jimmy90109.geoalarm.ui.components.MediaPreviewMotion
import com.github.jimmy90109.geoalarm.ui.components.MediaPreviewSelection
import com.github.jimmy90109.geoalarm.ui.components.MediaPreviewThumbnail
import com.github.jimmy90109.geoalarm.ui.components.MediaPreviewType
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sign

@Composable
fun PlaceReminderMediaStack(
    items: List<MediaPreviewItem>,
    hiddenAttachmentId: String?,
    onAttachmentBoundsChanged: (String, Rect) -> Unit,
    onAttachmentClick: (MediaPreviewSelection) -> Unit,
    modifier: Modifier = Modifier,
    aspectRatiosById: Map<String, Float>? = null,
) {
    if (items.isEmpty()) return
    val resolvedRatios = aspectRatiosById ?: rememberMediaPreviewAspectRatios(items)
    var appliedRatios by remember { mutableStateOf(resolvedRatios) }
    val ids = items.map { it.id }
    var frontId by rememberSaveable { mutableStateOf<String?>(null) }
    val frontIndex = items.indexOfFirst { it.id == frontId }.coerceAtLeast(0)
    val front = items[frontIndex]
    val ordered = items.drop(frontIndex) + items.take(frontIndex)
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    var dragX by remember { mutableFloatStateOf(0f) }
    var busy by remember { mutableStateOf(false) }
    var returning by remember { mutableStateOf(false) }
    var outgoingSize by remember { mutableStateOf<Pair<Dp, Dp>?>(null) }
    var outgoing by remember { mutableStateOf<MediaPreviewItem?>(null) }
    var animationJob by remember { mutableStateOf<Job?>(null) }
    val motion = remember { Animatable(0f) }
    var frontBounds by remember { mutableStateOf<Rect?>(null) }
    val latestBoundsCallback by rememberUpdatedState(onAttachmentBoundsChanged)
    val latestClick by rememberUpdatedState(onAttachmentClick)

    LaunchedEffect(ids, hiddenAttachmentId) {
        animationJob?.cancelAndJoin()
        if (hiddenAttachmentId in ids) frontId = hiddenAttachmentId
        else if (frontId !in ids) frontId = ids.first()
    }
    LaunchedEffect(resolvedRatios, busy, dragX == 0f) {
        if (!busy && dragX == 0f) appliedRatios = resolvedRatios
    }
    val windowHeight = with(density) {
        (LocalWindowInfo.current.containerSize.height - WindowInsets.safeDrawing.getTop(density) -
            WindowInsets.safeDrawing.getBottom(density)).coerceAtLeast(1).toDp()
    }
    val cardMaxHeight = minOf(420.dp, windowHeight * 0.55f)
    val nextLabel = stringResource(R.string.place_reminder_media_next)
    val openLabel = stringResource(R.string.place_reminder_media_open)
    val typeLabel = stringResource(if (front.type == MediaPreviewType.VIDEO) R.string.place_reminder_media_video else R.string.place_reminder_media_photo)
    val description = stringResource(R.string.place_reminder_media_description, frontIndex + 1, items.size, typeLabel, front.displayName)

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            stringResource(R.string.place_reminder_attachments),
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.titleMedium,
        )
        BoxWithConstraints(Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
            val widthPx = with(density) { maxWidth.toPx() }
            val cardSizes = items.associate { item ->
                val ratio = appliedRatios[item.id]?.takeIf { it.isFinite() && it > 0f } ?: 1f
                val height = minOf(maxWidth / ratio, cardMaxHeight)
                item.id to (height * ratio to height)
            }
            val frontSize = cardSizes.getValue(front.id)
            val frontWidthPx = with(density) { frontSize.first.toPx() }
            val outgoingWidthPx = with(density) { (outgoingSize?.first ?: frontSize.first).toPx() }
            val regionHeight = cardSizes.values.maxOf { it.second } + 32.dp
            val offsetStep = with(density) { 8.dp.toPx() }
            val velocityThreshold = with(density) { 800.dp.toPx() }
            val motionThresholdPx = with(density) { 0.5.dp.toPx() }
            fun settle(advance: Boolean, direction: Float) {
                if (busy || hiddenAttachmentId != null) return
                busy = true
                val current = front
                val next = ordered.getOrNull(1)
                animationJob = scope.launch {
                    try {
                        if (!advance || next == null) {
                            motion.snapTo(dragX)
                            motion.animateTo(0f, MediaPreviewMotion.rebound(visibilityThreshold = motionThresholdPx)) { dragX = value }
                        } else if (coroutineContext[MotionDurationScale]?.scaleFactor == 0f) {
                            frontId = next.id
                        } else {
                            outgoingSize = cardSizes.getValue(current.id)
                            outgoing = current
                            motion.snapTo(dragX)
                            val exitDistance = widthPx * 1.35f
                            if (abs(dragX) < exitDistance) {
                                // Reach the layer-swap boundary at speed instead of waiting for a spring's tail.
                                motion.updateBounds(lowerBound = -exitDistance, upperBound = exitDistance)
                                motion.animateTo(direction * exitDistance * 1.2f, MediaPreviewMotion.spatial()) {
                                    dragX = value
                                }
                                motion.updateBounds(lowerBound = null, upperBound = null)
                            }
                            returning = true
                            frontId = next.id
                            motion.animateTo(0f, MediaPreviewMotion.rebound(visibilityThreshold = motionThresholdPx)) { dragX = value }
                        }
                    } finally {
                        motion.updateBounds(lowerBound = null, upperBound = null)
                        dragX = 0f
                        outgoing = null
                        outgoingSize = null
                        returning = false
                        busy = false
                    }
                }
            }
            val progress = if (outgoing != null && !returning) (abs(dragX) / (widthPx * 1.35f)).coerceIn(0f, 1f) else 0f
            Box(Modifier.fillMaxWidth().height(regionHeight).testTag("place_reminder_media_region")) {
                // The outgoing card only changes drawing order after leaving the front card's bounds.
                val visible = ordered.take(3).filterNot { it.id == outgoing?.id }
                visible.reversed().forEach { item ->
                    val depth = ordered.indexOf(item)
                    val animatedDepth = (depth - progress).coerceAtLeast(0f)
                    val rotation = when (depth) {
                        1 -> -4f * (1f - progress)
                        2 -> 4f - 8f * progress
                        else -> 0f
                    }
                    val isFront = depth == 0
                    MediaPreviewThumbnail(
                        item = item,
                        hidden = isFront && item.id == hiddenAttachmentId,
                        interactive = false,
                        showPlayIndicator = true,
                        cornerRadius = 16.dp,
                        aspectRatio = appliedRatios[item.id] ?: 1f,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.align(Alignment.Center).size(cardSizes.getValue(item.id).first, cardSizes.getValue(item.id).second)
                            .zIndex(-depth.toFloat())
                            .graphicsLayer {
                                scaleX = 1f - 0.04f * animatedDepth
                                scaleY = scaleX
                                translationY = offsetStep * animatedDepth
                                rotationZ = if (isFront && outgoing == null) (dragX / frontWidthPx * 8f).coerceIn(-8f, 8f) else rotation
                                translationX = if (isFront && outgoing == null) dragX else 0f
                            }
                            .shadow(6.dp, RoundedCornerShape(16.dp))
                            .clearAndSetSemantics {},
                        onClick = {},
                    )
                }
                outgoing?.let { item ->
                    val size = outgoingSize ?: frontSize
                    val returnProgress = if (returning) (1f - abs(dragX) / (widthPx * 1.35f)).coerceIn(0f, 1f) else 0f
                    val bottomDepth = minOf(items.size - 1, 2).toFloat()
                    MediaPreviewThumbnail(
                        item = item, hidden = false, interactive = false, showPlayIndicator = true, cornerRadius = 16.dp,
                        aspectRatio = size.first.value / size.second.value, contentScale = ContentScale.Fit,
                        modifier = Modifier.align(Alignment.Center).size(size.first, size.second).zIndex(if (returning) -3f else 1f)
                            .graphicsLayer {
                                translationX = dragX
                                translationY = offsetStep * bottomDepth * returnProgress
                                scaleX = 1f - 0.04f * bottomDepth * returnProgress
                                scaleY = scaleX
                                rotationZ = if (returning) (if (bottomDepth == 1f) -4f else 4f) * returnProgress else (dragX / outgoingWidthPx * 8f).coerceIn(-8f, 8f)
                                alpha = if (returning && items.size > 3) 1f - returnProgress else 1f
                            }.shadow(6.dp, RoundedCornerShape(16.dp)).clearAndSetSemantics {},
                        onClick = {},
                    )
                }
                // A stationary hit area keeps tap bounds and vertical scrolling independent of card rotation.
                val tracker = remember { VelocityTracker() }
                key(front.id) {
                Box(Modifier.align(Alignment.Center).size(frontSize.first, frontSize.second)
                    .onGloballyPositioned {
                        val bounds = it.boundsInRoot()
                        frontBounds = bounds
                        latestBoundsCallback(front.id, bounds)
                    }.testTag("place_reminder_media_front")
                    .semantics(mergeDescendants = true) {
                        contentDescription = description
                        if (busy || hiddenAttachmentId != null) disabled()
                        onClick(openLabel) {
                            if (!busy && hiddenAttachmentId == null) frontBounds?.let { latestClick(MediaPreviewSelection(front, it)) }
                            !busy && hiddenAttachmentId == null
                        }
                        if (items.size > 1) customActions = listOf(CustomAccessibilityAction(nextLabel) {
                            if (busy || hiddenAttachmentId != null) false else { settle(true, -1f); true }
                        })
                    }
                    .pointerInput(front.id, busy, hiddenAttachmentId, ids, frontSize, widthPx) {
                        if (busy || hiddenAttachmentId != null || items.size < 2) return@pointerInput
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            tracker.resetTracking()
                            tracker.addPosition(down.uptimeMillis, down.position)
                            val start = awaitHorizontalTouchSlopOrCancellation(down.id) { change, _ ->
                                change.consume()
                                // Keep the full movement from touch-down when evaluating the card-width threshold.
                                dragX = change.position.x - down.position.x
                                tracker.addPosition(change.uptimeMillis, change.position)
                            }
                            if (start != null) {
                                val completed = horizontalDrag(start.id) { change ->
                                    dragX += change.positionChange().x
                                    tracker.addPosition(change.uptimeMillis, change.position)
                                    change.consume()
                                }
                                val velocity = tracker.calculateVelocity().x
                                val advance = completed && (abs(dragX) > frontWidthPx * 0.25f || abs(velocity) > velocityThreshold)
                                val direction = if (abs(velocity) > velocityThreshold) sign(velocity) else if (dragX >= 0f) 1f else -1f
                                settle(advance, direction)
                            }
                        }
                    }
                    .pointerInput(front.id, busy, hiddenAttachmentId) {
                        detectTapGestures {
                            if (!busy && hiddenAttachmentId == null) frontBounds?.let { latestClick(MediaPreviewSelection(front, it)) }
                        }
                    })
                }
            }
        }
    }
}
