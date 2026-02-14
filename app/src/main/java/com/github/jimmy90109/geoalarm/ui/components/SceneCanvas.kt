package com.github.jimmy90109.geoalarm.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.github.jimmy90109.geoalarm.R
import kotlin.math.atan
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.tan

// Grid visible vertical range (ratio of canvas height).
internal const val GRID_TOP_RATIO = 0.22f
internal const val GRID_BOTTOM_RATIO = 0.90f

// Horizontal perspective lines count (more lines = denser depth cues).
private const val HORIZONTAL_LINE_COUNT = 16

// Vertical line tilt curvature.
private const val VERTICAL_TILT_CURVE = 4.8f

// Vertical line density controls.
private const val VERTICAL_LINE_SPACING_DP = 22f
private const val MIN_VERTICAL_LINE_COUNT = 24

// Marker animation and geometry.
private val MARKER_ICON_SIZE = 250.dp
private const val MARKER_FLOAT_AMPLITUDE_DP = 8f
private const val MARKER_ENTRY_ROTATION_START_DEG = -45f
private const val MARKER_INNER_ICON_RATIO = 0.62f
private const val MARKER_INNER_ICON_BOTTOM_BIAS = 0.18f
private const val MARKER_FLOOR_Y_RATIO = 0.55f
private const val MARKER_PEDESTAL_RADIUS_RATIO = 0.1f
private const val MARKER_PEDESTAL_Y_BIAS = 2f
private const val MARKER_PEDESTAL_FLOAT_SCALE = 0.3f
private const val MARKER_END_CENTER_X_RATIO = 0.5f

// Geofence animation and geometry.
private const val GEOFENCE_HEIGHT_RATIO = 0.26f
private const val GEOFENCE_FLOOR_Y_RATIO = 0.50f
private const val GEOFENCE_START_OFFSET_WIDTH_RATIO = 0.75f
private const val GEOFENCE_END_CENTER_X_RATIO = 0.99f

// Ripple setup.
private val RIPPLE_OFFSETS = floatArrayOf(0f, 0.4f, 0.8f)
private const val RIPPLE_BASE_RADIUS_RATIO = 0.16f
private const val RIPPLE_EXPAND_RATIO = 1.8f
private const val RIPPLE_TOP_CENTER_Y_RATIO = 0.08f

@androidx.compose.runtime.Composable
internal fun SceneCanvas(
    modifier: Modifier = Modifier,
    sceneSize: IntSize,
    onSceneSizeChanged: (IntSize) -> Unit,
    gridShift: Float,
    gridAlpha: Float,
    gridColor: Color,
    stroke: Stroke,
    cameraScaleY: Float,
    markerEnterProgress: Float,
    markerTiltProgress: Float,
    markerPagerTiltDeg: Float,
    markerFloatPhase: Float,
    geofenceEnterProgress: Float,
    enteredFence: Boolean,
    rippleProgress: Float,
    markerTintColor: Color,
    geofenceColor: Color,
    rippleColor: Color,
    pedestalColor: Color
) {
    val density = LocalDensity.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged(onSceneSizeChanged)
    ) {
        val widthPx = sceneSize.width.toFloat().coerceAtLeast(1f)
        val heightPx = sceneSize.height.toFloat().coerceAtLeast(1f)

        // Static horizontal-line path is cached by size.
        val gridPath = androidx.compose.runtime.remember(widthPx.roundToInt(), heightPx.roundToInt()) {
            buildPerspectiveGridPath(width = widthPx, height = heightPx)
        }

        val cycleWidth = widthPx.coerceAtLeast(1f)
        val shiftX = gridShift * cycleWidth

        // Marker geometry.
        val markerIconSizePx = with(density) { MARKER_ICON_SIZE.toPx() }
        val markerFloorY = heightPx * MARKER_FLOOR_Y_RATIO
        val markerStartX = -markerIconSizePx * 1.5f
        // Keep the marker's visual center locked to screen center at the end of enter animation.
        val markerEndX = widthPx * MARKER_END_CENTER_X_RATIO
        val markerCenterX = lerp(markerStartX, markerEndX, markerEnterProgress)
        val markerBadgeLeftPx = markerCenterX - markerIconSizePx / 2f

        val floatAmplitudePx = with(density) { MARKER_FLOAT_AMPLITUDE_DP.dp.toPx() }
        val shapedFloatPhase = shapeMarkerFloatPhase(markerFloatPhase)
        val floatOffsetY = floatAmplitudePx * shapedFloatPhase * markerEnterProgress

        val markerBadgeTopPx = markerFloorY - markerIconSizePx + floatOffsetY
        val markerInnerIconSizePx = markerIconSizePx * MARKER_INNER_ICON_RATIO
        val markerInnerIconSizeDp = with(density) { markerInnerIconSizePx.toDp() }
        val markerInnerIconLeftPx = markerBadgeLeftPx + (markerIconSizePx - markerInnerIconSizePx) / 2f
        val markerInnerIconTopPx = markerBadgeTopPx +
            (markerIconSizePx - markerInnerIconSizePx) / 2f +
            markerInnerIconSizePx * MARKER_INNER_ICON_BOTTOM_BIAS

        val markerTiltDeg = lerp(
            MARKER_ENTRY_ROTATION_START_DEG,
            0f,
            markerTiltProgress.coerceIn(0f, 1f)
        ) + markerPagerTiltDeg

        val markerPedestalScale = 1f +
            (MARKER_PEDESTAL_FLOAT_SCALE * shapedFloatPhase * markerEnterProgress)

        // Geofence geometry.
        val geofenceWidth = widthPx
        val geofenceHeight = heightPx * GEOFENCE_HEIGHT_RATIO
        val geofenceFloorY = heightPx * GEOFENCE_FLOOR_Y_RATIO
        val geofenceStartCenterX = widthPx + geofenceWidth * GEOFENCE_START_OFFSET_WIDTH_RATIO
        val geofenceEndCenterX = widthPx * GEOFENCE_END_CENTER_X_RATIO
        val geofenceCenterX = lerp(geofenceStartCenterX, geofenceEndCenterX, geofenceEnterProgress)

        val geofenceTopLeft = Offset(
            x = geofenceCenterX - geofenceWidth / 2f,
            y = geofenceFloorY - geofenceHeight / 2f
        )

        val markerPedestalRadius = markerInnerIconSizePx * MARKER_PEDESTAL_RADIUS_RATIO * markerPedestalScale
        val markerPedestalCenter = Offset(
            x = markerInnerIconLeftPx + markerInnerIconSizePx / 2f,
            y = (markerInnerIconTopPx + markerInnerIconSizePx) - markerPedestalRadius * MARKER_PEDESTAL_Y_BIAS
        )

        val rippleBaseRadius = max(widthPx, heightPx) * RIPPLE_BASE_RADIUS_RATIO
        val rippleCenter = Offset(x = widthPx * 0.5f, y = heightPx * RIPPLE_TOP_CENTER_Y_RATIO)

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .drawWithCache {
                    val gridTop = size.height * GRID_TOP_RATIO
                    val gridBottom = size.height * GRID_BOTTOM_RATIO
                    val gridHeight = (gridBottom - gridTop).coerceAtLeast(1f)

                    val gridFadeBrush = Brush.verticalGradient(
                        colorStops = arrayOf(
                            0f to gridColor.copy(alpha = 0f),
                            0.2f to gridColor.copy(alpha = gridColor.alpha * gridAlpha),
                            0.7f to gridColor.copy(alpha = gridColor.alpha * gridAlpha),
                            1f to gridColor.copy(alpha = 0f)
                        ),
                        startY = gridTop,
                        endY = gridBottom
                    )

                    val targetSpacingPx = with(density) { VERTICAL_LINE_SPACING_DP.dp.toPx() }
                    val verticalLineCount = max(
                        MIN_VERTICAL_LINE_COUNT,
                        ceil(size.width / targetSpacingPx).toInt()
                    )
                    val verticalSpacing = size.width / verticalLineCount.toFloat()

                    onDrawBehind {
                        if (gridAlpha > 0f) {
                            translate(left = -shiftX) {
                                drawPath(path = gridPath, brush = gridFadeBrush, style = stroke)
                            }
                            translate(left = cycleWidth - shiftX) {
                                drawPath(path = gridPath, brush = gridFadeBrush, style = stroke)
                            }

                            for (cycle in -1..2) {
                                for (index in 0 until verticalLineCount) {
                                    val baseX = (index * verticalSpacing) + (cycle * cycleWidth) - shiftX
                                    val offsetFromCenter = (baseX / cycleWidth) - 0.5f
                                    val angleRad = atan(offsetFromCenter * VERTICAL_TILT_CURVE)

                                    val endOffsetX = tan(angleRad) * gridHeight
                                    val start = Offset(baseX, gridTop)
                                    val end = Offset(baseX + endOffsetX, gridBottom)

                                    val minX = min(start.x, end.x)
                                    val maxX = max(start.x, end.x)
                                    if (maxX < 0f || minX > size.width) continue

                                    drawLine(
                                        brush = gridFadeBrush,
                                        start = start,
                                        end = end,
                                        strokeWidth = stroke.width,
                                        cap = StrokeCap.Round
                                    )
                                }
                            }
                        }

                        if (geofenceEnterProgress > 0f) {
                            drawOval(
                                color = geofenceColor.copy(alpha = 0.2f * geofenceEnterProgress),
                                topLeft = geofenceTopLeft,
                                size = Size(geofenceWidth, geofenceHeight)
                            )
                        }

                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    pedestalColor.copy(alpha = 0.30f),
                                    pedestalColor.copy(alpha = 0.20f),
                                    pedestalColor.copy(alpha = 0f)
                                ),
                                center = markerPedestalCenter,
                                radius = markerPedestalRadius
                            ),
                            radius = markerPedestalRadius,
                            center = markerPedestalCenter
                        )

                        if (enteredFence) {
                            for (index in RIPPLE_OFFSETS.indices) {
                                val offset = RIPPLE_OFFSETS[index]
                                val p = ((rippleProgress - offset) / (1f - offset)).coerceIn(0f, 1f)
                                if (p <= 0f) continue

                                val radius = rippleBaseRadius * (1f + p * RIPPLE_EXPAND_RATIO)
                                val compensatedRadiusY = radius / cameraScaleY.coerceAtLeast(0.001f)
                                val alpha = ((1f - p) * (1f - index * 0.12f)).coerceIn(0f, 1f)
                                drawOval(
                                    color = rippleColor.copy(alpha = 0.5f * alpha),
                                    topLeft = Offset(
                                        x = rippleCenter.x - radius,
                                        y = rippleCenter.y - compensatedRadiusY
                                    ),
                                    size = Size(
                                        width = radius * 2f,
                                        height = compensatedRadiusY * 2f
                                    )
                                )
                            }
                        }
                    }
                }
        ) {}

        Image(
            painter = painterResource(id = R.mipmap.ic_launcher_foreground),
            contentDescription = null,
            modifier = Modifier
                .size(markerInnerIconSizeDp)
                .offset {
                    IntOffset(
                        x = markerInnerIconLeftPx.roundToInt(),
                        y = markerInnerIconTopPx.roundToInt()
                    )
                }
                .graphicsLayer {
                    scaleY = if (cameraScaleY != 0f) 1f / cameraScaleY else 1f
                    rotationZ = markerTiltDeg
                    transformOrigin = TransformOrigin(0.5f, 1f)
                },
            colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(markerTintColor)
        )
    }
}

internal fun buildPerspectiveGridPath(
    width: Float,
    height: Float,
    horizontalLines: Int = HORIZONTAL_LINE_COUNT
): Path {
    val safeWidth = width.coerceAtLeast(1f)
    val safeHeight = height.coerceAtLeast(1f)

    val gridTop = safeHeight * GRID_TOP_RATIO
    val gridBottom = safeHeight * GRID_BOTTOM_RATIO

    val path = Path()
    repeat(horizontalLines + 1) { index ->
        val t = index / horizontalLines.toFloat()
        val perspectiveT = t * t
        val y = lerp(gridTop, gridBottom, perspectiveT)
        path.moveTo(0f, y)
        path.lineTo(safeWidth, y)
    }

    return path
}

internal fun shapeMarkerFloatPhase(phase: Float): Float {
    return if (phase < 0f) {
        -1f + (phase + 1f).pow(1.8f)
    } else {
        phase
    }
}

internal fun lerp(start: Float, stop: Float, amount: Float): Float {
    return start + (stop - start) * amount
}

internal fun wrap01(value: Float): Float {
    var wrapped = value % 1f
    if (wrapped < 0f) wrapped += 1f
    return wrapped
}
