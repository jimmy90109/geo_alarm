package com.github.jimmy90109.geoalarm.ui.components

import android.content.Context
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
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
import kotlinx.coroutines.launch
import me.saket.telephoto.zoomable.coil.ZoomableAsyncImage
import kotlin.math.abs

private const val MediaPreviewTransitionMillis = 360
private const val MediaPreviewReturnMillis = 260
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
    var itemBounds by remember(item.id) { mutableStateOf<Rect?>(null) }

    Card(
        modifier = modifier
            .aspectRatio(1f)
            .onGloballyPositioned { coordinates ->
                val bounds = coordinates.boundsInRoot()
                itemBounds = bounds
                onBoundsChanged(item.id, bounds)
            }
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = !hidden) {
                itemBounds?.let { onClick(MediaPreviewSelection(item, it)) }
            }
            .graphicsLayer {
                alpha = if (hidden) 0f else 1f
            },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        shape = RoundedCornerShape(12.dp),
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            AsyncImage(
                model = imageRequest,
                contentDescription = item.displayName,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
            if (isVideo) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(4.dp),
                    contentAlignment = Alignment.BottomEnd,
                ) {
                    Icon(
                        Icons.Filled.Videocam,
                        contentDescription = null,
                        tint = Color.White,
                    )
                }
            }
        }
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
) {
    val previewItems = remember(items, selection.item) {
        val distinctItems = items.distinctBy { it.id }
        distinctItems.takeIf { list -> list.any { it.id == selection.item.id } }
            ?: listOf(selection.item)
    }
    val initialPage = remember(previewItems, selection.item.id) {
        previewItems.indexOfFirst { it.id == selection.item.id }.coerceAtLeast(0)
    }
    val pagerState = rememberPagerState(initialPage = initialPage) {
        previewItems.size
    }
    val scope = rememberCoroutineScope()
    val transitionProgress = remember(selection.item.id) { Animatable(0f) }
    val dragOffsetY = remember(selection.item.id) { Animatable(0f) }
    var isDragging by remember(selection.item.id) { mutableStateOf(false) }
    var hasDismissed by remember(selection.item.id) { mutableStateOf(false) }
    var hasMultiplePointers by remember(selection.item.id) { mutableStateOf(false) }
    var hasOpened by remember(selection.item.id) { mutableStateOf(false) }
    val activeItem = if (hasOpened) {
        previewItems.getOrElse(pagerState.settledPage) { selection.item }
    } else {
        selection.item
    }
    val isActiveVideo = activeItem.type == MediaPreviewType.VIDEO
    fun requestDismiss() {
        if (hasDismissed) return
        hasDismissed = true
        scope.launch {
            transitionProgress.animateTo(
                targetValue = 0f,
                animationSpec = tween(MediaPreviewReturnMillis),
            )
            onDismiss()
        }
    }

    BackHandler(onBack = ::requestDismiss)

    LaunchedEffect(selection.item.id) {
        transitionProgress.snapTo(0f)
        dragOffsetY.snapTo(0f)
        transitionProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(MediaPreviewTransitionMillis),
        )
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
        val targetBounds = remember(maxWidthPx, maxHeightPx) {
            Rect(left = 0f, top = 0f, right = maxWidthPx, bottom = maxHeightPx)
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
        val animatedCornerRadius = with(density) {
            lerpFloat(
                start = 12.dp.toPx(),
                end = if (isActiveVideo) {
                    0f
                } else {
                    (8.dp + (24.dp * dismissProgress)).toPx()
                },
                fraction = transitionProgress.value,
            ).toDp()
        }
        val animatedWidth = with(density) { animatedBounds.width.toDp() }
        val animatedHeight = with(density) { animatedBounds.height.toDp() }
        val controlsAlpha by animateFloatAsState(
            targetValue = if (transitionProgress.value > 0.98f && !isDragging && !hasDismissed) {
                1f - dismissProgress
            } else {
                0f
            },
            animationSpec = tween(120),
            label = "mediaPreviewControlsAlpha",
        )
        val imageContentScale = when {
            isActiveVideo && transitionProgress.value > 0.18f -> ContentScale.Fit
            isActiveVideo -> ContentScale.Crop
            transitionProgress.value > 0.18f -> ContentScale.Fit
            else -> ContentScale.Crop
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Color.Black.copy(
                        alpha = 0.94f * transitionProgress.value * (1f - dismissProgress),
                    ),
                )
                .pointerInput(activeItem.id) {
                    awaitEachGesture {
                        do {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            hasMultiplePointers = event.changes.count { it.pressed } > 1
                        } while (event.changes.any { it.pressed })
                        hasMultiplePointers = false
                    }
                }
                .pointerInput(activeItem.id) {
                    detectVerticalDragGestures(
                        onDragStart = {
                            if (transitionProgress.value < 1f) return@detectVerticalDragGestures
                            isDragging = true
                        },
                        onVerticalDrag = { change, dragAmount ->
                            if (transitionProgress.value < 1f) return@detectVerticalDragGestures
                            change.consume()
                            scope.launch {
                                dragOffsetY.snapTo(dragOffsetY.value + dragAmount)
                            }
                        },
                        onDragCancel = {
                            isDragging = false
                            scope.launch {
                                dragOffsetY.animateTo(
                                    targetValue = 0f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioNoBouncy,
                                        stiffness = Spring.StiffnessMediumLow,
                                    ),
                                )
                            }
                        },
                        onDragEnd = {
                            isDragging = false
                            if (abs(dragOffsetY.value) > dismissThresholdPx) {
                                requestDismiss()
                            } else {
                                scope.launch {
                                    dragOffsetY.animateTo(
                                        targetValue = 0f,
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioMediumBouncy,
                                            stiffness = Spring.StiffnessMediumLow,
                                        ),
                                    )
                                }
                            }
                        },
                    )
                },
            contentAlignment = Alignment.TopStart,
        ) {
            Box(
                modifier = Modifier
                    .graphicsLayer {
                        translationX = animatedBounds.left
                        translationY = animatedBounds.top
                    }
                    .size(width = animatedWidth, height = animatedHeight)
                    .clip(RoundedCornerShape(animatedCornerRadius)),
            ) {
                if (transitionProgress.value > 0.98f && previewItems.size > 1) {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize(),
                        userScrollEnabled = !isDragging && !hasDismissed && !hasMultiplePointers,
                    ) { page ->
                        MediaPreviewPage(
                            item = previewItems[page],
                            active = page == pagerState.settledPage,
                            complete = true,
                            imageContentScale = ContentScale.Fit,
                        )
                    }
                } else {
                    MediaPreviewPage(
                        item = activeItem,
                        active = true,
                        complete = transitionProgress.value > 0.98f,
                        imageContentScale = imageContentScale,
                    )
                }
            }
            IconButton(
                onClick = ::requestDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(12.dp)
                    .background(
                        color = Color.Black.copy(alpha = 0.42f),
                        shape = RoundedCornerShape(24.dp),
                    )
                    .graphicsLayer {
                        alpha = controlsAlpha
                    },
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
    imageContentScale: ContentScale,
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
            modifier = Modifier.fillMaxSize(),
        )
    } else if (!isVideo) {
        MediaPreviewImagePage(
            imageRequest = imageRequest,
            contentDescription = item.displayName,
            complete = complete,
            contentScale = imageContentScale,
        )
    } else {
        AsyncImage(
            model = imageRequest,
            contentDescription = item.displayName,
            modifier = Modifier.fillMaxSize(),
            contentScale = imageContentScale,
        )
    }

    if (isVideo) {
        Icon(
            Icons.Filled.Videocam,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(12.dp)
                .graphicsLayer {
                    alpha = if (complete) 1f else 0f
                },
        )
    }
}

@Composable
private fun MediaPreviewImagePage(
    imageRequest: ImageRequest,
    contentDescription: String,
    complete: Boolean,
    contentScale: ContentScale,
) {
    var hasShownZoomable by remember(imageRequest) { mutableStateOf(false) }
    LaunchedEffect(complete) {
        if (complete) {
            hasShownZoomable = true
        }
    }
    val zoomableAlpha by animateFloatAsState(
        targetValue = if (hasShownZoomable) 1f else 0f,
        animationSpec = tween(140),
        label = "mediaPreviewImageZoomableAlpha",
    )

    Box(modifier = Modifier.fillMaxSize()) {
        AsyncImage(
            model = imageRequest,
            contentDescription = contentDescription,
            modifier = Modifier.fillMaxSize(),
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
                contentScale = contentScale,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun MediaPreviewVideoPlayer(
    localPath: String,
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
    var playbackPosition by remember(localPath) { mutableStateOf(0L) }
    var playbackDuration by remember(localPath) { mutableStateOf(0L) }
    var isBuffering by remember(localPath) { mutableStateOf(true) }
    var hasRenderedFirstFrame by remember(localPath) { mutableStateOf(false) }
    val playerAlpha by animateFloatAsState(
        targetValue = if (hasRenderedFirstFrame) 1f else 0f,
        animationSpec = tween(180),
        label = "mediaPreviewVideoPlayerAlpha",
    )
    val posterAlpha by animateFloatAsState(
        targetValue = if (hasRenderedFirstFrame) 0f else 1f,
        animationSpec = tween(180),
        label = "mediaPreviewVideoPosterAlpha",
    )
    val loadingAlpha by animateFloatAsState(
        targetValue = if (isBuffering) 1f else 0f,
        animationSpec = tween(160),
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

    LaunchedEffect(player) {
        while (true) {
            withFrameMillis {
                playbackPosition = player.currentPosition.coerceAtLeast(0L)
                playbackDuration = player.duration
                    .takeUnless { it == C.TIME_UNSET }
                    ?.coerceAtLeast(0L)
                    ?: 0L
            }
        }
    }

    DisposableEffect(player, lifecycleOwner) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                isBuffering = playbackState == Player.STATE_BUFFERING
            }

            override fun onRenderedFirstFrame() {
                hasRenderedFirstFrame = true
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
        MediaPreviewVideoControls(
            positionMillis = playbackPosition,
            durationMillis = playbackDuration,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(
                    start = MediaPreviewControlEdgePadding,
                    top = MediaPreviewControlEdgePadding,
                    end = MediaPreviewControlEdgePadding,
                    bottom = MediaPreviewControlEdgePadding,
                ),
        )
    }
}

@Composable
private fun MediaPreviewVideoControls(
    positionMillis: Long,
    durationMillis: Long,
    modifier: Modifier = Modifier,
) {
    val progress = remember(positionMillis, durationMillis) {
        if (durationMillis > 0L) {
            (positionMillis.toFloat() / durationMillis.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(42.dp),
    ) {
        Text(
            text = formatPlaybackTime(positionMillis),
            modifier = Modifier
                .align(Alignment.TopStart)
                .background(
                    color = Color.Black.copy(alpha = 0.52f),
                    shape = RoundedCornerShape(24.dp),
                )
                .padding(horizontal = 10.dp, vertical = 6.dp),
            color = Color.White,
            style = MaterialTheme.typography.labelMedium,
        )
        Text(
            text = formatPlaybackTime(durationMillis),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .background(
                    color = Color.Black.copy(alpha = 0.52f),
                    shape = RoundedCornerShape(24.dp),
                )
                .padding(horizontal = 10.dp, vertical = 6.dp),
            color = Color.White,
            style = MaterialTheme.typography.labelMedium,
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(5.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color.White.copy(alpha = 0.24f)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .height(5.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.primary),
            )
        }
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
