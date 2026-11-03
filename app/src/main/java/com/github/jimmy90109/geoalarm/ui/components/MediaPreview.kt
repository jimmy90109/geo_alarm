@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.github.jimmy90109.geoalarm.ui.components

import android.content.Context
import android.graphics.BitmapFactory
import android.media.ExifInterface
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.annotation.OptIn as AndroidXOptIn
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.gestures.TargetedFlingBehavior
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.systemBarsIgnoringVisibility
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import coil.decode.VideoFrameDecoder
import coil.imageLoader
import coil.request.ImageRequest
import com.github.jimmy90109.geoalarm.data.MaxPlaceReminderAttachments
import com.github.jimmy90109.geoalarm.R
import java.io.File
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.coroutineScope
import me.saket.telephoto.zoomable.coil.ZoomableAsyncImage
import me.saket.telephoto.zoomable.rememberZoomableImageState
import me.saket.telephoto.zoomable.rememberZoomableState
import kotlin.math.sign
import kotlin.math.abs

private val MediaPreviewControlEdgePadding = 16.dp

enum class MediaPreviewType {
    IMAGE,
    VIDEO,
}

data class MediaPreviewItem(
    val id: String,
    val localPath: String,
    val displayName: String,
    val type: MediaPreviewType,
    val width: Int? = null,
    val height: Int? = null,
    val durationMillis: Long? = null,
)

data class MediaPreviewStackCard(
    val item: MediaPreviewItem,
    val bounds: Rect,
    val rotation: Float,
    val sourceAlpha: Float = 1f,
    val scrimAlpha: Float = 0f,
)

data class MediaPreviewSelection(
    val item: MediaPreviewItem,
    val sourceBounds: Rect,
)

@Composable
fun MediaPreviewPreloader(
    items: List<MediaPreviewItem>,
    preloadLimit: Int = MaxPlaceReminderAttachments,
) {
    val context = LocalContext.current
    val preloadItems = remember(items, preloadLimit) {
        items.distinctBy { it.id }.take(preloadLimit)
    }

    DisposableEffect(context, preloadItems) {
        val disposables = preloadItems.map { item ->
            context.imageLoader.enqueue(
                mediaPreviewImageRequest(
                    context = context,
                    localPath = item.localPath,
                    isVideo = item.type == MediaPreviewType.VIDEO,
                )
            )
        }
        onDispose {
            disposables.forEach { it.dispose() }
        }
    }
}

@Composable
fun MediaPreviewThumbnail(
    item: MediaPreviewItem,
    hidden: Boolean,
    modifier: Modifier = Modifier,
    onBoundsChanged: (String, Rect) -> Unit = { _, _ -> },
    onClick: (MediaPreviewSelection) -> Unit,
    aspectRatio: Float = 1f,
    cornerRadius: Dp = 12.dp,
    interactive: Boolean = true,
    showPlayIndicator: Boolean = false,
    playIndicatorAlignment: Alignment = Alignment.Center,
    playIndicatorSize: Dp = 40.dp,
    showVideoDuration: Boolean = true,
    contentScale: ContentScale = ContentScale.Crop,
    scrimAlpha: Float = 0f,
) {
    val context = LocalContext.current
    val isVideo = item.type == MediaPreviewType.VIDEO
    val imageRequest = remember(item.localPath, isVideo) {
        mediaPreviewImageRequest(
            context = context,
            localPath = item.localPath,
            isVideo = isVideo,
        )
    }
    var failed by remember(item.localPath) { mutableStateOf(false) }
    var itemBounds by remember(item.id) { mutableStateOf<Rect?>(null) }

    Card(
        modifier = modifier
            .aspectRatio(aspectRatio)
            .onGloballyPositioned { coordinates ->
                val bounds = coordinates.boundsInRoot()
                itemBounds = bounds
                onBoundsChanged(item.id, bounds)
            }
            .clip(RoundedCornerShape(cornerRadius))
            .clickable(enabled = !hidden && interactive) {
                itemBounds?.let { onClick(MediaPreviewSelection(item, it)) }
            }
            .graphicsLayer {
                alpha = if (hidden) 0f else 1f
            },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        shape = RoundedCornerShape(cornerRadius),
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            AsyncImage(
                model = imageRequest,
                contentDescription = item.displayName,
                modifier = Modifier.fillMaxSize(),
                contentScale = contentScale,
                onError = { failed = true },
                onSuccess = { failed = false },
            )
            if (failed) {
                Icon(Icons.Filled.BrokenImage, stringResource(R.string.place_reminder_media_unavailable))
            }
            if (isVideo && showPlayIndicator) {
                Icon(
                    Icons.Filled.PlayArrow,
                    contentDescription = stringResource(R.string.place_reminder_media_video),
                    tint = MaterialTheme.colorScheme.inverseOnSurface,
                    modifier = Modifier
                        .align(playIndicatorAlignment)
                        .padding(8.dp)
                        .size(playIndicatorSize)
                        .background(MaterialTheme.colorScheme.inverseSurface.copy(alpha = 0.92f), RoundedCornerShape(24.dp))
                        .padding(playIndicatorSize / 5),
                )
            }
            if (isVideo && showVideoDuration) {
                val durationMillis = item.durationMillis ?: rememberMediaPreviewVideoDurationMillis(item.localPath)
                MediaPreviewVideoDurationBadge(
                    durationMillis = durationMillis,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(8.dp),
                )
            }
            if (scrimAlpha > 0f) {
                Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = scrimAlpha.coerceIn(0f, 1f))))
            }
        }
    }
}

@Composable
private fun MediaPreviewVideoDurationBadge(
    durationMillis: Long?,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.inverseSurface.copy(alpha = 0.92f)),
    ) {
        Text(
            text = formatMediaPreviewVideoDuration(durationMillis),
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.inverseOnSurface,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MediaPreviewOverlay(
    selection: MediaPreviewSelection,
    items: List<MediaPreviewItem> = listOf(selection.item),
    sourceBoundsById: Map<String, Rect> = mapOf(selection.item.id to selection.sourceBounds),
    onActiveItemChanged: (MediaPreviewItem) -> Unit = {},
    onDismiss: () -> Unit,
    sourceCornerRadius: Dp = 12.dp,
    aspectRatiosById: Map<String, Float>? = null,
    hideSystemBars: Boolean = false,
    stackCards: List<MediaPreviewStackCard> = emptyList(),
    peekAdjacentMedia: Boolean = false,
    zoomEnabled: Boolean = true,
    dismissOnBackgroundTap: Boolean = false,
    transitionProgress: Animatable<Float, AnimationVector1D> = remember(selection.item.id) {
        Animatable(0f)
    },
) {
    if (hideSystemBars) {
        MediaPreviewSystemBars { transitionProgress.value }
    }
    val previewItems = remember(items, selection.item) {
        val distinctItems = items.distinctBy { it.id }
        distinctItems.takeIf { list -> list.any { it.id == selection.item.id } }
            ?: listOf(selection.item)
    }
    val resolvedAspectRatios = aspectRatiosById ?: rememberMediaPreviewAspectRatios(previewItems)
    val initialPage = remember(previewItems, selection.item.id) {
        previewItems.indexOfFirst { it.id == selection.item.id }.coerceAtLeast(0)
    }
    val pagerState = rememberPagerState(initialPage = initialPage) {
        previewItems.size
    }
    var swipeStartPage by remember(selection.item.id) { mutableIntStateOf(initialPage) }
    val scope = rememberCoroutineScope()
    val dragOffsetY = remember(selection.item.id) { Animatable(0f) }
    var isDragging by remember(selection.item.id) { mutableStateOf(false) }
    var hasDismissed by remember(selection.item.id) { mutableStateOf(false) }
    var hasMultiplePointers by remember(selection.item.id) { mutableStateOf(false) }
    var hasOpened by remember(selection.item.id) { mutableStateOf(false) }
    val pageMediaBounds = remember(selection.item.id) { mutableStateMapOf<Int, Rect>() }
    var activeMediaBounds by remember { mutableStateOf<Rect?>(null) }
    var overlayOrigin by remember { mutableStateOf(Offset.Zero) }
    var counterBounds by remember { mutableStateOf<Rect?>(null) }
    var closeBounds by remember { mutableStateOf<Rect?>(null) }
    var videoControlsBounds by remember { mutableStateOf<Rect?>(null) }
    val activeItem = if (hasOpened) {
        previewItems.getOrElse(pagerState.settledPage) { selection.item }
    } else {
        selection.item
    }
    val openingCards = remember(selection.item.id) { stackCards.toList() }
    val companionOpeningProgress = remember(selection.item.id) {
        openingCards.filter { it.item.id != selection.item.id }.associate { it.item.id to Animatable(0f) }
    }
    var companionDismissProgress by remember(selection.item.id) { mutableStateOf(emptyMap<String, Float>()) }
    var dismissStartProgress by remember(selection.item.id) { mutableStateOf(1f) }
    val transitionCards = if (hasOpened) stackCards else openingCards
    val isActiveVideo = activeItem.type == MediaPreviewType.VIDEO
    var playbackPosition by remember { mutableStateOf(0L) }
    var playbackDuration by remember { mutableStateOf(0L) }
    var playbackPlaying by remember { mutableStateOf(false) }
    var playbackReady by remember(activeItem.id) { mutableStateOf(false) }
    var activePlayer by remember(activeItem.id) { mutableStateOf<Player?>(null) }
    val onPlayerChanged: (Player?, Boolean) -> Unit = remember(activeItem.id) {
        { player, ready ->
            activePlayer = player
            playbackReady = ready
        }
    }
    LaunchedEffect(activePlayer) {
        val player = activePlayer ?: return@LaunchedEffect
        while (true) {
            withFrameMillis {
                playbackPosition = player.currentPosition.coerceAtLeast(0L)
                playbackDuration = player.duration.takeUnless { it == C.TIME_UNSET }
                    ?.coerceAtLeast(0L) ?: 0L
                playbackPlaying = player.playWhenReady
            }
        }
    }
    fun requestDismiss() {
        if (hasDismissed) return
        dismissStartProgress = transitionProgress.value.coerceAtLeast(0.001f)
        companionDismissProgress = companionOpeningProgress.mapValues { it.value.value }
        hasDismissed = true
        scope.launch {
            transitionProgress.animateTo(
                targetValue = 0f,
                animationSpec = MediaPreviewMotion.spatial(visibilityThreshold = 0.001f),
            )
            onDismiss()
        }
    }

    BackHandler(onBack = ::requestDismiss)

    LaunchedEffect(selection.item.id) {
        transitionProgress.snapTo(0f)
        dragOffsetY.snapTo(0f)
        coroutineScope {
            if (peekAdjacentMedia && previewItems.size > 1) {
                companionOpeningProgress.values.forEachIndexed { index, progress ->
                    launch {
                        progress.snapTo(0f)
                        progress.animateTo(1f, MediaPreviewMotion.carouselExpansion(
                            delayMillis = (index + 1).coerceAtMost(3) * 40,
                        ))
                    }
                }
            }
            transitionProgress.animateTo(
                targetValue = 1f,
                animationSpec = if (peekAdjacentMedia && previewItems.size > 1) {
                    MediaPreviewMotion.carouselExpansion()
                } else {
                    MediaPreviewMotion.spatial(visibilityThreshold = 0.001f)
                },
            )
        }
        hasOpened = true
    }

    LaunchedEffect(hasOpened, activeItem.id) {
        if (hasOpened) {
            onActiveItemChanged(activeItem)
        }
    }

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize(),
    ) {
        val density = LocalDensity.current
        val maxHeightPx = with(density) { maxHeight.toPx() }.coerceAtLeast(1f)
        val maxWidthPx = with(density) { maxWidth.toPx() }.coerceAtLeast(1f)
        val showPeeks = peekAdjacentMedia && previewItems.size > 1
        val pageInset = if (showPeeks) 32.dp else 0.dp
        val pageGap = if (showPeeks) 12.dp else 0.dp
        val insetPx = with(density) { pageInset.toPx() }
        val pageWidthPx = (maxWidthPx - insetPx * 2f).coerceAtLeast(1f)
        val pageStridePx = pageWidthPx + with(density) { pageGap.toPx() }
        val defaultPagerFling = PagerDefaults.flingBehavior(state = pagerState)
        val pagerFling = remember(defaultPagerFling, showPeeks) {
            if (!showPeeks) defaultPagerFling else object : TargetedFlingBehavior {
                override suspend fun ScrollScope.performFling(
                    initialVelocity: Float,
                    onRemainingDistanceUpdated: (Float) -> Unit,
                ): Float {
                    val advanced = pagerState.currentPage - swipeStartPage
                    // A drag may already cross a narrower page before its release fling begins.
                    val velocity = if (advanced * initialVelocity > 0f) 0f else initialVelocity
                    return with(defaultPagerFling) {
                        this@performFling.performFling(velocity, onRemainingDistanceUpdated)
                    }
                }
            }
        }
        val dismissThresholdPx = maxHeightPx * 0.16f
        val dismissProgress by remember(maxHeightPx) {
            derivedStateOf {
                (abs(dragOffsetY.value) / (maxHeightPx * 0.34f)).coerceIn(0f, 1f)
            }
        }
        val viewerScale by remember {
            derivedStateOf {
                1f - (dismissProgress * 0.16f)
            }
        }
        val mediaAspectRatio = resolvedAspectRatios[activeItem.id]
        val targetBounds = remember(pageWidthPx, maxHeightPx, insetPx, mediaAspectRatio) {
            mediaAspectRatio?.let { aspectRatio ->
                mediaFitBounds(
                    maxWidthPx = pageWidthPx,
                    maxHeightPx = maxHeightPx,
                    mediaAspectRatio = aspectRatio,
                ).translate(x = insetPx)
            } ?: Rect(left = insetPx, top = 0f, right = maxWidthPx - insetPx, bottom = maxHeightPx)
        }
        val draggedTargetBounds = remember(targetBounds, viewerScale, dragOffsetY.value) {
            targetBounds
                .scaleAroundCenter(viewerScale)
                .translate(y = dragOffsetY.value)
        }
        val sourceBounds = sourceBoundsById[activeItem.id] ?: selection.sourceBounds
        val animatedBounds = remember(sourceBounds, draggedTargetBounds, transitionProgress.value) {
            lerp(sourceBounds, draggedTargetBounds, transitionProgress.value)
        }
        val fullScreenBounds = remember(maxWidthPx, maxHeightPx) {
            Rect(left = 0f, top = 0f, right = maxWidthPx, bottom = maxHeightPx)
        }
        val useFullScreenCanvas = transitionProgress.value > 0.98f &&
            (!showPeeks || hasOpened) &&
            (showPeeks || !isDragging) && !hasDismissed &&
            (!peekAdjacentMedia || previewItems.size > 1)
        val displayBounds = if (useFullScreenCanvas) {
            fullScreenBounds
        } else {
            animatedBounds
        }
        val animatedCornerRadius = if (peekAdjacentMedia) {
            sourceCornerRadius * lerpFloat(1f, viewerScale, transitionProgress.value)
        } else with(density) {
            lerpFloat(
                start = sourceCornerRadius.toPx(),
                end = if (isActiveVideo) {
                    0f
                } else {
                    (8.dp + (24.dp * dismissProgress)).toPx()
                },
                fraction = transitionProgress.value,
            ).toDp()
        }
        val displayCornerRadius = if (useFullScreenCanvas) 0.dp else animatedCornerRadius
        val displayWidth = with(density) { displayBounds.width.toDp() }
        val displayHeight = with(density) { displayBounds.height.toDp() }
        val controlsAlpha by animateFloatAsState(
            targetValue = if (transitionProgress.value > 0.98f && !isDragging && !hasDismissed) {
                1f - dismissProgress
            } else {
                0f
            },
            animationSpec = MediaPreviewMotion.effects(),
            label = "mediaPreviewControlsAlpha",
        )
        val mediaContentScale = if (useFullScreenCanvas) {
            ContentScale.Fit
        } else {
            ContentScale.Crop
        }
        val backgroundTap by rememberUpdatedState<(Offset) -> Unit> { localPosition ->
            if (dismissOnBackgroundTap && hasOpened && !hasDismissed && !isDragging &&
                !pagerState.isScrollInProgress && transitionProgress.value == 1f
            ) {
                val position = localPosition + overlayOrigin
                val insideMedia = if (showPeeks && useFullScreenCanvas) {
                    pageMediaBounds.any { (page, bounds) ->
                        abs(page - pagerState.currentPage) <= 1 && bounds.contains(position)
                    }
                } else activeMediaBounds?.contains(position) == true
                val insideControl = counterBounds?.contains(position) == true ||
                    closeBounds?.contains(position) == true || videoControlsBounds?.contains(position) == true
                if (!insideMedia && !insideControl) requestDismiss()
            }
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Color.Black.copy(
                        alpha = 0.94f * transitionProgress.value * (1f - dismissProgress),
                    ),
                )
                .onGloballyPositioned { overlayOrigin = it.boundsInRoot().topLeft }
                .pointerInput(activeItem.id) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                        swipeStartPage = pagerState.currentPage
                        var isTap = true
                        do {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            hasMultiplePointers = event.changes.count { it.pressed } > 1
                            if (event.changes.size > 1 || event.changes.any {
                                    it.isConsumed || (it.position - down.position).getDistance() > viewConfiguration.touchSlop ||
                                        it.uptimeMillis - down.uptimeMillis > viewConfiguration.longPressTimeoutMillis
                                }
                            ) isTap = false
                        } while (event.changes.any { it.pressed })
                        hasMultiplePointers = false
                        // Observe the completed tap without consuming carousel or player gestures.
                        if (isTap) backgroundTap(down.position)
                    }
                }
                .pointerInput(activeItem.id) {
                    detectVerticalDragGestures(
                        onDragStart = {
                            if (hasMultiplePointers || hasDismissed || transitionProgress.value < 1f) {
                                return@detectVerticalDragGestures
                            }
                            isDragging = true
                        },
                        onVerticalDrag = { change, dragAmount ->
                            if (hasMultiplePointers || !isDragging || transitionProgress.value < 1f) {
                                return@detectVerticalDragGestures
                            }
                            change.consume()
                            scope.launch {
                                dragOffsetY.snapTo(dragOffsetY.value + dragAmount)
                            }
                        },
                        onDragCancel = {
                            scope.launch {
                                try {
                                    dragOffsetY.animateTo(
                                        targetValue = 0f,
                                        animationSpec = MediaPreviewMotion.rebound(),
                                    )
                                } finally {
                                    isDragging = false
                                }
                            }
                        },
                        onDragEnd = {
                            if (!isDragging) return@detectVerticalDragGestures
                            if (abs(dragOffsetY.value) > dismissThresholdPx) {
                                requestDismiss()
                            } else {
                                scope.launch {
                                    try {
                                        dragOffsetY.animateTo(
                                            targetValue = 0f,
                                            animationSpec = MediaPreviewMotion.rebound(),
                                        )
                                    } finally {
                                        isDragging = false
                                    }
                                }
                            }
                        },
                    )
                },
            contentAlignment = Alignment.TopStart,
        ) {
            if (!hasOpened || transitionProgress.value < 1f || hasDismissed) {
                val activeIndex = previewItems.indexOfFirst { it.id == activeItem.id }
                transitionCards.asReversed().filter { it.item.id != activeItem.id }.forEach { card ->
                    androidx.compose.runtime.key(card.item.id) {
                        val index = previewItems.indexOfFirst { it.id == card.item.id }
                        if (index >= 0) {
                            val ratio = resolvedAspectRatios[card.item.id]
                                ?: (card.bounds.width / card.bounds.height)
                            val target = mediaFitBounds(pageWidthPx, maxHeightPx, ratio)
                            val edgeOffset = if (showPeeks) {
                                -sign((index - activeIndex).toFloat()) * (pageWidthPx - target.width) / 2f
                            } else 0f
                            val shiftedTarget = target.translate(
                                x = insetPx + (index - activeIndex) * pageStridePx + edgeOffset,
                            )
                            val progress = if (!hasOpened && showPeeks) {
                                if (hasDismissed) {
                                    (companionDismissProgress[card.item.id] ?: 0f) *
                                        (transitionProgress.value / dismissStartProgress).coerceIn(0f, 1f)
                                } else companionOpeningProgress[card.item.id]?.value ?: transitionProgress.value
                            } else transitionProgress.value.coerceIn(0f, 1f)
                            val bounds = lerp(card.bounds, shiftedTarget, progress)
                            MediaPreviewThumbnail(
                                item = card.item,
                                hidden = false,
                                interactive = false,
                                showPlayIndicator = true,
                                aspectRatio = ratio,
                                contentScale = ContentScale.Fit,
                                scrimAlpha = lerpFloat(card.scrimAlpha, 0.22f, progress),
                                cornerRadius = if (showPeeks) sourceCornerRadius else sourceCornerRadius * (1f - progress),
                                modifier = Modifier
                                    .testTag("media_preview_companion_${card.item.id}")
                                    .graphicsLayer {
                                        translationX = bounds.left
                                        translationY = bounds.top
                                        rotationZ = card.rotation * (1f - progress)
                                        alpha = card.sourceAlpha +
                                            (1f - card.sourceAlpha) * (progress * 4f).coerceAtMost(1f)
                                    }
                                    .size(
                                        with(density) { bounds.width.toDp() },
                                        with(density) { bounds.height.toDp() },
                                    )
                                    .shadow(
                                        6.dp * (1f - progress),
                                        RoundedCornerShape(
                                            if (peekAdjacentMedia) sourceCornerRadius else sourceCornerRadius * (1f - progress),
                                        ),
                                    ),
                                onClick = {},
                            )
                        }
                    }
                }
            }
            Box(
                modifier = Modifier
                    .graphicsLayer {
                        translationX = displayBounds.left
                        translationY = displayBounds.top
                    }
                    .size(width = displayWidth, height = displayHeight)
                    .testTag("media_preview_transition_media")
                    .onGloballyPositioned { activeMediaBounds = it.boundsInRoot() }
                    .clip(RoundedCornerShape(displayCornerRadius)),
            ) {
                if (transitionProgress.value > 0.98f && previewItems.size > 1 && (!showPeeks || useFullScreenCanvas)) {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize().testTag("media_preview_pager"),
                        contentPadding = PaddingValues(horizontal = pageInset),
                        pageSpacing = pageGap,
                        beyondViewportPageCount = if (showPeeks) 1 else 0,
                        flingBehavior = pagerFling,
                        userScrollEnabled = hasOpened && !isDragging && !hasDismissed && !hasMultiplePointers,
                    ) { page ->
                        val ratio = resolvedAspectRatios[previewItems[page].id] ?: 1f
                        val fitted = mediaFitBounds(pageWidthPx, maxHeightPx, ratio)
                        Box(
                            Modifier.fillMaxSize().testTag("media_preview_carousel_page_$page"),
                            contentAlignment = Alignment.Center,
                        ) {
                            val mediaModifier = if (showPeeks) {
                                Modifier.size(
                                    with(density) { fitted.width.toDp() },
                                    with(density) { fitted.height.toDp() },
                                ).graphicsLayer {
                                    val position = page - pagerState.currentPage - pagerState.currentPageOffsetFraction
                                    translationX = -(pageWidthPx - fitted.width) / 2f * position.coerceIn(-1f, 1f)
                                    if (page == pagerState.settledPage) {
                                        translationY = dragOffsetY.value
                                        scaleX = viewerScale
                                        scaleY = viewerScale
                                    }
                                }
                            } else Modifier.fillMaxSize()
                            Box(
                                mediaModifier
                                    .testTag("media_preview_carousel_media_$page")
                                    .onGloballyPositioned { pageMediaBounds[page] = it.boundsInRoot() }
                                    .clip(RoundedCornerShape(if (showPeeks) sourceCornerRadius else 0.dp)),
                            ) {
                                MediaPreviewPage(
                                    item = previewItems[page],
                                    active = page == pagerState.settledPage,
                                    complete = page == pagerState.settledPage && hasOpened,
                                    onPlayerChanged = onPlayerChanged,
                                    imageContentScale = mediaContentScale,
                                    zoomEnabled = zoomEnabled,
                                )
                                if (showPeeks) {
                                    Box(Modifier.fillMaxSize().graphicsLayer {
                                        val distance = abs(page - pagerState.currentPage - pagerState.currentPageOffsetFraction)
                                            .coerceIn(0f, 1f)
                                        alpha = 0.22f * distance
                                    }.background(Color.Black))
                                }
                            }
                        }
                    }
                } else {
                    MediaPreviewPage(
                        item = activeItem,
                        active = true,
                        complete = transitionProgress.value > 0.98f,
                        onPlayerChanged = onPlayerChanged,
                        imageContentScale = mediaContentScale,
                        zoomEnabled = zoomEnabled,
                    )
                }
            }
            AnimatedVisibility(
                visible = isActiveVideo && playbackReady && hasOpened && transitionProgress.value == 1f &&
                    !isDragging && !hasDismissed,
                enter = slideInVertically(animationSpec = MediaPreviewMotion.spatial()) { it } +
                    fadeIn(animationSpec = MediaPreviewMotion.effects()),
                exit = slideOutVertically(animationSpec = MediaPreviewMotion.spatial()) { it } +
                    fadeOut(animationSpec = MediaPreviewMotion.effects()),
                modifier = Modifier
                    .align(Alignment.BottomCenter),
            ) {
                DisposableEffect(Unit) {
                    onDispose { videoControlsBounds = null }
                }
                androidx.compose.runtime.key(activeItem.id) {
                    MediaPreviewVideoControls(
                        positionMillis = playbackPosition,
                        durationMillis = playbackDuration,
                        playing = playbackPlaying,
                        canSeek = activePlayer?.isCurrentMediaItemSeekable == true,
                        onTogglePlayback = {
                            activePlayer?.let { player ->
                                if (player.playWhenReady) player.pause() else player.play()
                                playbackPlaying = player.playWhenReady
                            }
                        },
                        onSeek = { position ->
                            activePlayer?.seekTo(position)
                            playbackPosition = position
                        },
                        modifier = Modifier
                            .windowInsetsPadding(WindowInsets.safeDrawing.union(WindowInsets.systemBarsIgnoringVisibility))
                            .padding(MediaPreviewControlEdgePadding)
                            .testTag("media_preview_video_controls")
                            .onGloballyPositioned { videoControlsBounds = it.boundsInRoot() },
                    )
                }
            }
            val currentPage = previewItems.indexOfFirst { it.id == activeItem.id }.coerceAtLeast(0) + 1
            val pageDescription = stringResource(
                R.string.media_preview_page_description, currentPage, previewItems.size,
            )
            val pageLabels = previewItems.indices.map { index ->
                stringResource(R.string.media_preview_page_count, index + 1, previewItems.size)
            }
            val labelStyle = MaterialTheme.typography.labelLarge
            val textMeasurer = rememberTextMeasurer()
            val counterWidth = with(density) {
                pageLabels.maxOf { textMeasurer.measure(it, labelStyle).size.width }.toDp()
            } + 32.dp
            val counterWidthPx = with(density) { counterWidth.toPx() }
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .windowInsetsPadding(
                        WindowInsets.safeDrawing.union(WindowInsets.systemBarsIgnoringVisibility)
                            .only(WindowInsetsSides.Top),
                    )
                    .padding(top = 12.dp)
                    .height(48.dp)
                    .width(counterWidth)
                    .graphicsLayer { alpha = controlsAlpha }
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.Black.copy(alpha = 0.42f))
                    .testTag("media_preview_page_count")
                    .onGloballyPositioned { counterBounds = it.boundsInRoot() }
                    .semantics(mergeDescendants = true) { contentDescription = pageDescription },
                contentAlignment = Alignment.Center,
            ) {
                pageLabels.forEachIndexed { index, label ->
                    Text(
                        text = label,
                        color = Color.White,
                        style = labelStyle,
                        modifier = Modifier
                            .graphicsLayer {
                                val position = index - pagerState.currentPage - pagerState.currentPageOffsetFraction
                                translationX = position * counterWidthPx
                                alpha = (1f - abs(position)).coerceIn(0f, 1f)
                            }
                            .testTag("media_preview_page_label_${index + 1}")
                            .clearAndSetSemantics {},
                    )
                }
            }
            IconButton(
                onClick = ::requestDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .windowInsetsPadding(WindowInsets.safeDrawing.union(WindowInsets.systemBarsIgnoringVisibility))
                    .padding(12.dp)
                    .onGloballyPositioned { closeBounds = it.boundsInRoot() }
                    .graphicsLayer {
                        alpha = controlsAlpha
                    }
                    .background(
                        color = Color.Black.copy(alpha = 0.42f),
                        shape = RoundedCornerShape(24.dp),
                    ),
            ) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = stringResource(R.string.close),
                    tint = Color.White,
                )
            }
        }
    }
}

@Composable
private fun BoxScope.MediaPreviewPage(
    item: MediaPreviewItem,
    active: Boolean,
    complete: Boolean,
    onPlayerChanged: (Player?, Boolean) -> Unit,
    imageContentScale: ContentScale,
    zoomEnabled: Boolean = true,
) {
    val context = LocalContext.current
    val isVideo = item.type == MediaPreviewType.VIDEO
    val imageRequest = remember(item.localPath, isVideo) {
        mediaPreviewImageRequest(
            context = context,
            localPath = item.localPath,
            isVideo = isVideo,
        )
    }

    if (isVideo && active && complete) {
        MediaPreviewVideoPlayer(
            localPath = item.localPath,
            onPlayerChanged = onPlayerChanged,
            modifier = Modifier.fillMaxSize(),
        )
    } else if (!isVideo) {
        MediaPreviewImagePage(
            imageRequest = imageRequest,
            contentDescription = item.displayName,
            complete = complete,
            contentScale = imageContentScale,
            zoomEnabled = zoomEnabled,
        )
    } else {
        AsyncImage(
            model = imageRequest,
            contentDescription = item.displayName,
            modifier = Modifier.fillMaxSize(),
            contentScale = imageContentScale,
        )
    }


}

@Composable
private fun MediaPreviewImagePage(
    imageRequest: ImageRequest,
    contentDescription: String,
    complete: Boolean,
    contentScale: ContentScale,
    zoomEnabled: Boolean,
) {
    if (!zoomEnabled) {
        AsyncImage(
            model = imageRequest,
            contentDescription = contentDescription,
            modifier = Modifier.fillMaxSize(),
            contentScale = contentScale,
        )
        return
    }
    val zoomableImageState = rememberZoomableImageState(rememberZoomableState())
    var hasShownZoomable by remember(imageRequest) { mutableStateOf(false) }
    LaunchedEffect(complete) {
        if (complete) {
            hasShownZoomable = true
        }
    }
    val zoomableImageDisplayed = hasShownZoomable && zoomableImageState.isImageDisplayed
    val bottomImageAlpha by animateFloatAsState(
        targetValue = if (zoomableImageDisplayed) 0f else 1f,
        animationSpec = MediaPreviewMotion.effects(),
        label = "mediaPreviewImageFallbackAlpha",
    )
    val zoomableAlpha by animateFloatAsState(
        targetValue = if (zoomableImageDisplayed) 1f else 0f,
        animationSpec = MediaPreviewMotion.effects(),
        label = "mediaPreviewImageZoomableAlpha",
    )
    Box(modifier = Modifier.fillMaxSize()) {
        AsyncImage(
            model = imageRequest,
            contentDescription = contentDescription,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    alpha = bottomImageAlpha
                },
            contentScale = contentScale,
        )
        if (hasShownZoomable) {
            ZoomableAsyncImage(
                model = imageRequest,
                contentDescription = contentDescription,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        alpha = zoomableAlpha
                    },
                state = zoomableImageState,
                contentScale = contentScale,
            )
        }
    }
}

@AndroidXOptIn(UnstableApi::class)
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun MediaPreviewVideoPlayer(
    localPath: String,
    onPlayerChanged: (Player?, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val posterRequest = remember(context, localPath) {
        mediaPreviewImageRequest(
            context = context,
            localPath = localPath,
            isVideo = true,
        )
    }
    val currentOnPlayerChanged by rememberUpdatedState(onPlayerChanged)
    var isBuffering by remember(localPath) { mutableStateOf(true) }
    var hasRenderedFirstFrame by remember(localPath) { mutableStateOf(false) }
    val playerAlpha by animateFloatAsState(
        targetValue = if (hasRenderedFirstFrame) 1f else 0f,
        animationSpec = MediaPreviewMotion.effects(),
        label = "mediaPreviewVideoPlayerAlpha",
    )
    val posterAlpha by animateFloatAsState(
        targetValue = if (hasRenderedFirstFrame) 0f else 1f,
        animationSpec = MediaPreviewMotion.effects(),
        label = "mediaPreviewVideoPosterAlpha",
    )
    val loadingAlpha by animateFloatAsState(
        targetValue = if (isBuffering) 1f else 0f,
        animationSpec = MediaPreviewMotion.effects(),
        label = "mediaPreviewVideoLoadingAlpha",
    )
    val player = remember(localPath) {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_ONE
            playWhenReady = true
            setMediaItem(MediaItem.fromUri(Uri.fromFile(File(localPath))))
            prepare()
        }
    }

    DisposableEffect(player, lifecycleOwner, onPlayerChanged) {
        currentOnPlayerChanged(player, hasRenderedFirstFrame)
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                isBuffering = playbackState == Player.STATE_BUFFERING
            }

            override fun onRenderedFirstFrame() {
                hasRenderedFirstFrame = true
                currentOnPlayerChanged(player, true)
            }
        }
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> player.play()
                Lifecycle.Event.ON_PAUSE,
                Lifecycle.Event.ON_STOP -> player.pause()
                else -> Unit
            }
        }
        isBuffering = player.playbackState == Player.STATE_BUFFERING
        player.addListener(listener)
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            player.removeListener(listener)
            currentOnPlayerChanged(null, false)
            player.release()
        }
    }

    Box(
        modifier = modifier
            .background(Color.Black)
            .clickable {
                if (player.isPlaying) {
                    player.pause()
                } else {
                    player.play()
                }
            },
    ) {
        AsyncImage(
            model = posterRequest,
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    alpha = posterAlpha
                },
            contentScale = ContentScale.Fit,
        )
        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    alpha = playerAlpha
                },
            factory = { viewContext ->
                PlayerView(viewContext).apply {
                    this.player = player
                    useController = false
                    controllerAutoShow = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    setKeepContentOnPlayerReset(true)
                    setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)
                }
            },
            update = { playerView ->
                playerView.player = player
                playerView.useController = false
            },
        )
        if (loadingAlpha > 0f) {
            LoadingIndicator(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(84.dp)
                    .graphicsLayer {
                        alpha = loadingAlpha
                    },
            )
        }
    }
}

@Composable
internal fun MediaPreviewVideoControls(
    positionMillis: Long,
    durationMillis: Long,
    playing: Boolean,
    canSeek: Boolean,
    onTogglePlayback: () -> Unit,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    var scrubProgress by remember { mutableStateOf<Float?>(null) }
    val progress = if (durationMillis > 0L) {
        (positionMillis.toFloat() / durationMillis.toFloat()).coerceIn(0f, 1f)
    } else 0f
    val displayedPosition = scrubProgress?.let { (it * durationMillis).toLong() } ?: positionMillis
    val seekDescription = stringResource(R.string.media_preview_seek)
    Box(modifier = modifier.fillMaxWidth()) {
        IconButton(
            onClick = onTogglePlayback,
            modifier = Modifier.align(Alignment.TopStart),
        ) {
            Icon(
                imageVector = if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = stringResource(
                    if (playing) R.string.media_preview_pause else R.string.media_preview_play,
                ),
                tint = Color.White,
            )
        }
        Text(
            text = "${formatPlaybackTime(displayedPosition)} / ${formatPlaybackTime(durationMillis)}",
            modifier = Modifier.align(Alignment.TopEnd).padding(vertical = 16.dp),
            color = Color.White,
            style = MaterialTheme.typography.labelMedium,
        )
        Slider(
            value = scrubProgress ?: progress,
            onValueChange = { scrubProgress = it },
            onValueChangeFinished = {
                scrubProgress?.let { onSeek((it * durationMillis).toLong().coerceIn(0L, durationMillis)) }
                scrubProgress = null
            },
            enabled = canSeek && durationMillis > 0L,
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = Color.White,
                inactiveTrackColor = Color.White.copy(alpha = 0.3f),
                disabledThumbColor = Color.White.copy(alpha = 0.5f),
                disabledActiveTrackColor = Color.White.copy(alpha = 0.5f),
                disabledInactiveTrackColor = Color.White.copy(alpha = 0.3f),
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 48.dp)
                .testTag("media_preview_seek")
                .semantics { contentDescription = seekDescription },
        )
    }
}

private fun lerp(start: Rect, end: Rect, fraction: Float): Rect {
    val coercedFraction = fraction.coerceIn(0f, 1f)
    return Rect(
        left = lerpFloat(start.left, end.left, coercedFraction),
        top = lerpFloat(start.top, end.top, coercedFraction),
        right = lerpFloat(start.right, end.right, coercedFraction),
        bottom = lerpFloat(start.bottom, end.bottom, coercedFraction),
    )
}

private fun lerpFloat(start: Float, end: Float, fraction: Float): Float =
    start + ((end - start) * fraction.coerceIn(0f, 1f))

private fun Rect.scaleAroundCenter(scale: Float): Rect {
    val center = center
    val scaledWidth = width * scale
    val scaledHeight = height * scale
    return Rect(
        left = center.x - (scaledWidth / 2f),
        top = center.y - (scaledHeight / 2f),
        right = center.x + (scaledWidth / 2f),
        bottom = center.y + (scaledHeight / 2f),
    )
}

private fun Rect.translate(x: Float = 0f, y: Float = 0f): Rect =
    Rect(left = left + x, top = top + y, right = right + x, bottom = bottom + y)

private fun mediaFitBounds(
    maxWidthPx: Float,
    maxHeightPx: Float,
    mediaAspectRatio: Float,
): Rect {
    val screenAspectRatio = maxWidthPx / maxHeightPx
    val targetWidth: Float
    val targetHeight: Float
    if (mediaAspectRatio > screenAspectRatio) {
        targetWidth = maxWidthPx
        targetHeight = maxWidthPx / mediaAspectRatio
    } else {
        targetHeight = maxHeightPx
        targetWidth = maxHeightPx * mediaAspectRatio
    }
    val left = (maxWidthPx - targetWidth) / 2f
    val top = (maxHeightPx - targetHeight) / 2f
    return Rect(
        left = left,
        top = top,
        right = left + targetWidth,
        bottom = top + targetHeight,
    )
}

private data class MediaAspectRatioKey(
    val localPath: String,
    val type: MediaPreviewType,
    val width: Int?,
    val height: Int?,
)

@Composable
internal fun rememberMediaPreviewAspectRatios(items: List<MediaPreviewItem>): Map<String, Float> {
    val keys = items.map { MediaAspectRatioKey(it.localPath, it.type, it.width, it.height) }
    val ids = items.map { it.id }
    val cache = remember { mutableMapOf<MediaAspectRatioKey, Float>() }
    val initial = items.mapIndexed { index, item ->
        item.id to (cache[keys[index]] ?: aspectRatioFromSize(item.width, item.height) ?: 1f)
    }.toMap()
    val ratios by produceState(initialValue = initial, keys, ids) {
        value = initial
        val uncached = keys.zip(items).distinctBy { it.first }.filter { it.first !in cache }
        val loaded = withContext(Dispatchers.IO) {
            uncached.associate { (key, item) -> key to (item.resolveAspectRatio() ?: 1f) }
        }
        cache.putAll(loaded)
        value = items.mapIndexed { index, item -> item.id to cache.getValue(keys[index]) }.toMap()
    }
    return ratios
}

internal fun MediaPreviewItem.resolveAspectRatio(): Float? {
    val storedAspectRatio = aspectRatioFromSize(width = width, height = height)
    if (storedAspectRatio != null) return storedAspectRatio

    return when (type) {
        MediaPreviewType.IMAGE -> imageAspectRatio(localPath)
        MediaPreviewType.VIDEO -> videoAspectRatio(localPath)
    }
}

private fun aspectRatioFromSize(width: Int?, height: Int?): Float? {
    val safeWidth = width?.takeIf { it > 0 } ?: return null
    val safeHeight = height?.takeIf { it > 0 } ?: return null
    return safeWidth.toFloat() / safeHeight.toFloat()
}

private fun imageAspectRatio(localPath: String): Float? {
    val options = BitmapFactory.Options().apply {
        inJustDecodeBounds = true
    }
    BitmapFactory.decodeFile(localPath, options)
    val width = options.outWidth.takeIf { it > 0 } ?: return null
    val height = options.outHeight.takeIf { it > 0 } ?: return null
    val rotated = try {
        val orientation = ExifInterface(localPath)
            .getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL,
            )
        orientation == ExifInterface.ORIENTATION_ROTATE_90 ||
            orientation == ExifInterface.ORIENTATION_ROTATE_270 ||
            orientation == ExifInterface.ORIENTATION_TRANSPOSE ||
            orientation == ExifInterface.ORIENTATION_TRANSVERSE
    } catch (_: IOException) {
        false
    } catch (_: RuntimeException) {
        false
    }
    return if (rotated) {
        height.toFloat() / width.toFloat()
    } else {
        width.toFloat() / height.toFloat()
    }
}

private fun videoAspectRatio(localPath: String): Float? {
    val retriever = MediaMetadataRetriever()
    return try {
        retriever.setDataSource(localPath)
        val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
            ?.toIntOrNull()
            ?.takeIf { it > 0 }
            ?: return null
        val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
            ?.toIntOrNull()
            ?.takeIf { it > 0 }
            ?: return null
        val rotation = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)
            ?.toIntOrNull()
            ?: 0
        if (rotation == 90 || rotation == 270) {
            height.toFloat() / width.toFloat()
        } else {
            width.toFloat() / height.toFloat()
        }
    } catch (_: RuntimeException) {
        null
    } finally {
        retriever.release()
    }
}

private fun mediaPreviewImageRequest(
    context: Context,
    localPath: String,
    isVideo: Boolean,
): ImageRequest =
    ImageRequest.Builder(context)
        .data(localPath)
        .crossfade(true)
        .apply {
            if (isVideo) {
                decoderFactory(VideoFrameDecoder.Factory())
            }
        }
        .build()

@Composable
private fun rememberMediaPreviewVideoDurationMillis(localPath: String): Long? {
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

private fun formatMediaPreviewVideoDuration(durationMillis: Long?): String {
    val totalSeconds = durationMillis
        ?.coerceAtLeast(0L)
        ?.let { (it + 999L) / 1_000L }
        ?: return "--:--"
    val seconds = totalSeconds % 60L
    val totalMinutes = totalSeconds / 60L
    val minutes = totalMinutes % 60L
    val hours = totalMinutes / 60L
    return if (hours > 0L) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%d:%02d".format(minutes, seconds)
    }
}

private fun formatPlaybackTime(millis: Long): String {
    val totalSeconds = millis.coerceAtLeast(0L) / 1_000L
    val hours = totalSeconds / 3_600L
    val minutes = (totalSeconds % 3_600L) / 60L
    val seconds = totalSeconds % 60L
    val secondText = seconds.toString().padStart(2, '0')
    return if (hours > 0L) {
        val minuteText = minutes.toString().padStart(2, '0')
        "$hours:$minuteText:$secondText"
    } else {
        "$minutes:$secondText"
    }
}
