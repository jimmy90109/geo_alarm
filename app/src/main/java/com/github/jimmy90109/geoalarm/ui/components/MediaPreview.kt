package com.github.jimmy90109.geoalarm.ui.components

import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
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
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import coil.decode.VideoFrameDecoder
import coil.request.ImageRequest
import com.github.jimmy90109.geoalarm.R
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.saket.telephoto.zoomable.coil.ZoomableAsyncImage
import kotlin.math.abs

private const val MediaPreviewTransitionMillis = 360
private const val MediaPreviewReturnMillis = 260

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
fun MediaPreviewThumbnail(
    item: MediaPreviewItem,
    hidden: Boolean,
    modifier: Modifier = Modifier,
    onClick: (MediaPreviewSelection) -> Unit,
) {
    val context = LocalContext.current
    val isVideo = item.type == MediaPreviewType.VIDEO
    val imageRequest = remember(item.localPath, isVideo) {
        ImageRequest.Builder(context)
            .data(item.localPath)
            .crossfade(true)
            .apply {
                if (isVideo) {
                    decoderFactory(VideoFrameDecoder.Factory())
                }
            }
            .build()
    }
    var itemBounds by remember(item.id) { mutableStateOf<Rect?>(null) }

    Card(
        modifier = modifier
            .aspectRatio(1f)
            .onGloballyPositioned { coordinates ->
                itemBounds = coordinates.boundsInRoot()
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

@Composable
fun MediaPreviewOverlay(
    selection: MediaPreviewSelection,
    onDismiss: () -> Unit,
) {
    val item = selection.item
    val context = LocalContext.current
    val isVideo = item.type == MediaPreviewType.VIDEO
    val imageRequest = remember(item.localPath, isVideo) {
        ImageRequest.Builder(context)
            .data(item.localPath)
            .crossfade(true)
            .apply {
                if (isVideo) {
                    decoderFactory(VideoFrameDecoder.Factory())
                }
            }
            .build()
    }
    val storedAspectRatio = remember(item.width, item.height) {
        val width = item.width?.takeIf { it > 0 }?.toFloat()
        val height = item.height?.takeIf { it > 0 }?.toFloat()
        if (width != null && height != null) width / height else null
    }
    var decodedAspectRatio by remember(item.id) {
        mutableStateOf(storedAspectRatio)
    }
    val mediaAspectRatio = decodedAspectRatio ?: 1f

    LaunchedEffect(item.id, item.localPath, isVideo, storedAspectRatio) {
        if (storedAspectRatio == null) {
            decodedAspectRatio = resolveMediaAspectRatio(
                localPath = item.localPath,
                isVideo = isVideo,
            ) ?: 1f
        }
    }

    val scope = rememberCoroutineScope()
    val transitionProgress = remember(item.id) { Animatable(0f) }
    val dragOffsetY = remember(item.id) { Animatable(0f) }
    var isDragging by remember(item.id) { mutableStateOf(false) }
    var hasDismissed by remember(item.id) { mutableStateOf(false) }

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

    LaunchedEffect(item.id) {
        transitionProgress.snapTo(0f)
        dragOffsetY.snapTo(0f)
        transitionProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(MediaPreviewTransitionMillis),
        )
    }

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize(),
    ) {
        val density = LocalDensity.current
        val maxHeightPx = with(density) { maxHeight.toPx() }.coerceAtLeast(1f)
        val maxWidthPx = with(density) { maxWidth.toPx() }.coerceAtLeast(1f)
        val horizontalPaddingPx = with(density) { 12.dp.toPx() }
        val verticalPaddingPx = with(density) { 24.dp.toPx() }
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
        val targetBounds = remember(
            maxWidthPx,
            maxHeightPx,
            horizontalPaddingPx,
            verticalPaddingPx,
            mediaAspectRatio,
            isVideo,
        ) {
            if (isVideo) {
                fitRectInContainer(
                    containerWidth = maxWidthPx,
                    containerHeight = maxHeightPx,
                    horizontalPadding = horizontalPaddingPx,
                    verticalPadding = verticalPaddingPx,
                    aspectRatio = mediaAspectRatio,
                )
            } else {
                Rect(left = 0f, top = 0f, right = maxWidthPx, bottom = maxHeightPx)
            }
        }
        val draggedTargetBounds = remember(targetBounds, viewerScale, dragOffsetY.value) {
            targetBounds
                .scaleAroundCenter(viewerScale)
                .translate(y = dragOffsetY.value)
        }
        val animatedBounds = remember(selection.sourceBounds, draggedTargetBounds, transitionProgress.value) {
            lerp(selection.sourceBounds, draggedTargetBounds, transitionProgress.value)
        }
        val animatedCornerRadius = with(density) {
            lerpFloat(
                start = 12.dp.toPx(),
                end = (8.dp + (24.dp * dismissProgress)).toPx(),
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
            isVideo -> {
                if (transitionProgress.value > 0.96f) ContentScale.Fit else ContentScale.Crop
            }
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
                .pointerInput(item.id) {
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
                if (isVideo && transitionProgress.value > 0.98f) {
                    MediaPreviewVideoPlayer(
                        localPath = item.localPath,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else if (transitionProgress.value > 0.98f) {
                    ZoomableAsyncImage(
                        model = imageRequest,
                        contentDescription = item.displayName,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit,
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
                                alpha = transitionProgress.value * (1f - dismissProgress)
                            },
                    )
                }
            }
            IconButton(
                onClick = ::requestDismiss,
                modifier = Modifier
                    .align(Alignment.TopStart)
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
private fun MediaPreviewVideoPlayer(
    localPath: String,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val player = remember(localPath) {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_OFF
            playWhenReady = true
            setMediaItem(MediaItem.fromUri(Uri.fromFile(File(localPath))))
            prepare()
        }
    }

    DisposableEffect(player, lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> player.play()
                Lifecycle.Event.ON_PAUSE,
                Lifecycle.Event.ON_STOP -> player.pause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            player.release()
        }
    }

    AndroidView(
        modifier = modifier.background(Color.Black),
        factory = { viewContext ->
            PlayerView(viewContext).apply {
                this.player = player
                useController = true
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
            }
        },
        update = { playerView ->
            playerView.player = player
        },
    )
}

private suspend fun resolveMediaAspectRatio(
    localPath: String,
    isVideo: Boolean,
): Float? = withContext(Dispatchers.IO) {
    if (isVideo) {
        resolveVideoAspectRatio(localPath)
    } else {
        resolveImageAspectRatio(localPath)
    }
}

private fun fitRectInContainer(
    containerWidth: Float,
    containerHeight: Float,
    horizontalPadding: Float,
    verticalPadding: Float,
    aspectRatio: Float,
): Rect {
    val availableWidth = (containerWidth - (horizontalPadding * 2f)).coerceAtLeast(1f)
    val availableHeight = (containerHeight - (verticalPadding * 2f)).coerceAtLeast(1f)
    val availableAspectRatio = availableWidth / availableHeight
    val width: Float
    val height: Float
    if (availableAspectRatio > aspectRatio) {
        height = availableHeight
        width = height * aspectRatio
    } else {
        width = availableWidth
        height = width / aspectRatio
    }
    val left = (containerWidth - width) / 2f
    val top = (containerHeight - height) / 2f
    return Rect(left = left, top = top, right = left + width, bottom = top + height)
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

private fun resolveImageAspectRatio(localPath: String): Float? {
    val options = BitmapFactory.Options().apply {
        inJustDecodeBounds = true
    }
    BitmapFactory.decodeFile(localPath, options)
    val width = options.outWidth.takeIf { it > 0 } ?: return null
    val height = options.outHeight.takeIf { it > 0 } ?: return null
    return width.toFloat() / height.toFloat()
}

private fun resolveVideoAspectRatio(localPath: String): Float? {
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
    } finally {
        runCatching { retriever.release() }
    }
}
