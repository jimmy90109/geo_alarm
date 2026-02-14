package com.github.jimmy90109.geoalarm.ui.components

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.ContextWrapper
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.github.jimmy90109.geoalarm.R
import com.github.jimmy90109.geoalarm.ui.theme.GeoAlarmTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.atan
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.tan

// Camera intro animation.
private const val CAMERA_TARGET_SCALE_Y = 0.75f
private const val CAMERA_TARGET_ROTATION_X = 11f

// Notification overlay setup.
private const val NOTIFICATION_MAX_WIDTH_DP = 420

data class OnboardingMotionConfig(
    val cameraDurationMs: Int = 2000,
    val markerEnterDurationMs: Int = 1200,
    val markerTiltDurationMs: Int = 1300,
    val markerFloatDurationMs: Int = 2000,
    val geofenceDurationMs: Int = 2000,
    val gridLoopDurationMs: Float = 6000f,
    val rippleDurationMs: Int = 1400,
    val sceneExitDurationMs: Int = 520,
    val introCardFadeDurationMs: Int = 620,
    val introPageSwitchDurationMs: Int = 700,
    val notificationSlideInMs: Int = 520,
    val notificationFadeInMs: Int = 420,
    val notificationShakeDelayMs: Long = 560L,
    val notificationShakeDurationMs: Int = 420,
    val markerPagerTiltMaxDeg: Float = 14f,
    val markerPagerVelocityThreshold: Float = 0.0010f,
    val markerPagerAccelThreshold: Float = 0.0040f,
    val markerPagerTiltReturnMs: Int = 260,
    val permissionGrantedDelayMs: Long = 180L,
    val permissionExpandDurationMs: Int = 360,
    val permissionFadeDurationMs: Int = 240,
    val permissionSpacingDurationMs: Int = 300
)

private sealed class AnimationState {
    // Initial marker-only intro.
    data object Initial : AnimationState()

    // User marker enters from the left.
    data object MarkerEntering : AnimationState()

    // Marker has settled; show title + start card.
    data object WaitingForStart : AnimationState()

    // User pressed start; camera/grid transition runs before location card appears.
    data object CameraTransitioning : AnimationState()

    // Scene is running; show location permission card.
    data object LocationPermission : AnimationState()

    // Geofence entering from the right.
    data object GeofenceEntering : AnimationState()

    // Show notification permission card after notification preview.
    data object NotificationPermission : AnimationState()

    // Scene completed.
    data object Completed : AnimationState()
}

/**
 * Reusable onboarding animation for location alarm concept.
 *
 * High-level sequence:
 * 1) Camera tilts into perspective.
 * 2) Marker enters and settles near center.
 * 3) User taps to continue.
 * 4) Geofence enters while grid decelerates to stop.
 * 5) Ripple + haptic + top notification appear.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LocationOnboardingScene(
    modifier: Modifier = Modifier,
    isDarkMode: Boolean,
    motionConfig: OnboardingMotionConfig = OnboardingMotionConfig(),
    onAnimationFinished: () -> Unit = {}
) {
    val appContext = androidx.compose.ui.platform.LocalContext.current
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val colorScheme = MaterialTheme.colorScheme
    val navigationBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val bottomCardPadding = maxOf(navigationBottom, 24.dp)

    var animationState by remember { mutableStateOf<AnimationState>(AnimationState.Initial) }
    var enteredFence by remember { mutableStateOf(false) }
    var sceneSize by remember { mutableStateOf(IntSize.Zero) }
    var locationGranted by remember { mutableStateOf(false) }
    var notificationGranted by remember { mutableStateOf(false) }
    var showNotificationRetryDialog by remember { mutableStateOf(false) }
    var showNotificationSettingsDialog by remember { mutableStateOf(false) }
    val introPages = listOf(
        IntroPage(
            title = stringResource(R.string.onboarding_intro_core_title),
            bullets = listOf(
                stringResource(R.string.onboarding_intro_core_item_1),
                stringResource(R.string.onboarding_intro_core_item_2),
                stringResource(R.string.onboarding_intro_core_item_3)
            )
        ),
        IntroPage(
            title = stringResource(R.string.onboarding_intro_logic_title),
            bullets = listOf(
                stringResource(R.string.onboarding_intro_logic_item_1),
                stringResource(R.string.onboarding_intro_logic_item_2),
                stringResource(R.string.onboarding_intro_logic_item_3),
                stringResource(R.string.onboarding_intro_logic_item_4)
            )
        ),
        IntroPage(
            title = stringResource(R.string.onboarding_intro_privacy_title),
            bullets = listOf(
                stringResource(R.string.onboarding_intro_privacy_item_1),
                stringResource(R.string.onboarding_intro_privacy_item_2),
                stringResource(R.string.onboarding_intro_privacy_item_3),
                stringResource(R.string.onboarding_intro_privacy_item_4)
            )
        )
    )
    val introPagerState = rememberPagerState(pageCount = { introPages.size })

    // Horizontal grid phase in [0, 1).
    var gridShift by remember { mutableFloatStateOf(0f) }

    // Velocity multiplier used to smoothly decelerate the moving grid.
    var gridVelocityFactor by remember { mutableFloatStateOf(1f) }

    // Start immediately when composable first appears.
    LaunchedEffect(Unit) {
        animationState = AnimationState.MarkerEntering
    }

    val notificationPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                notificationGranted = true
            } else {
                val activity = appContext.findActivity()
                val shouldShowRationale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && activity != null) {
                    activity.shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    false
                }
                if (shouldShowRationale) {
                    showNotificationRetryDialog = true
                } else {
                    showNotificationSettingsDialog = true
                }
            }
        }

    // Camera perspective transform.
    val cameraScaleY by animateFloatAsState(
        targetValue = if (animationState is AnimationState.CameraTransitioning ||
            animationState is AnimationState.LocationPermission ||
            animationState is AnimationState.GeofenceEntering ||
            animationState is AnimationState.NotificationPermission ||
            animationState is AnimationState.Completed
        ) {
            CAMERA_TARGET_SCALE_Y
        } else {
            1f
        },
        animationSpec = tween(durationMillis = motionConfig.cameraDurationMs, easing = FastOutSlowInEasing),
        label = "CameraScaleY"
    )
    val cameraRotationX by animateFloatAsState(
        targetValue = if (animationState is AnimationState.CameraTransitioning ||
            animationState is AnimationState.LocationPermission ||
            animationState is AnimationState.GeofenceEntering ||
            animationState is AnimationState.NotificationPermission ||
            animationState is AnimationState.Completed
        ) {
            CAMERA_TARGET_ROTATION_X
        } else {
            0f
        },
        animationSpec = tween(durationMillis = motionConfig.cameraDurationMs, easing = FastOutSlowInEasing),
        label = "CameraRotationX"
    )

    // Marker translation uses tween for deterministic speed (no spring overshoot).
    val markerEnterProgress by animateFloatAsState(
        targetValue = when (animationState) {
            is AnimationState.MarkerEntering,
            is AnimationState.WaitingForStart,
            is AnimationState.CameraTransitioning,
            is AnimationState.LocationPermission,
            is AnimationState.GeofenceEntering,
            is AnimationState.NotificationPermission,
            is AnimationState.Completed -> 1f

            else -> 0f
        },
        animationSpec = tween(durationMillis = motionConfig.markerEnterDurationMs, easing = FastOutSlowInEasing),
        finishedListener = {
            if (animationState is AnimationState.MarkerEntering) {
                animationState = AnimationState.WaitingForStart
            }
        },
        label = "MarkerEnterProgress"
    )

    // Keep tilt longer than translation so the "swing-in" feeling stays visible.
    val markerTiltProgress by animateFloatAsState(
        targetValue = when (animationState) {
            is AnimationState.MarkerEntering,
            is AnimationState.WaitingForStart,
            is AnimationState.CameraTransitioning,
            is AnimationState.LocationPermission,
            is AnimationState.GeofenceEntering,
            is AnimationState.NotificationPermission,
            is AnimationState.Completed -> 1f

            else -> 0f
        },
        animationSpec = tween(durationMillis = motionConfig.markerTiltDurationMs, easing = FastOutSlowInEasing),
        label = "MarkerTiltProgress"
    )

    // Geofence enters only after user tap.
    val geofenceEnterProgress by animateFloatAsState(
        targetValue = if (
            animationState is AnimationState.GeofenceEntering ||
            animationState is AnimationState.NotificationPermission ||
            animationState is AnimationState.Completed
        ) {
            1f
        } else {
            0f
        },
        animationSpec = tween(durationMillis = motionConfig.geofenceDurationMs, easing = EaseOutCubic),
        finishedListener = {
            if (animationState is AnimationState.GeofenceEntering) {
                enteredFence = true
            }
        },
        label = "GeofenceEnterProgress"
    )

    val introCardAlpha by animateFloatAsState(
        targetValue = if (animationState is AnimationState.WaitingForStart && markerEnterProgress >= 0.98f) 1f else 0f,
        animationSpec = tween(durationMillis = motionConfig.introCardFadeDurationMs, easing = FastOutSlowInEasing),
        label = "IntroCardAlpha"
    )
    var markerPagerTiltTarget by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(animationState, introPagerState) {
        if (animationState !is AnimationState.WaitingForStart) {
            markerPagerTiltTarget = 0f
            return@LaunchedEffect
        }

        var lastPos = introPagerState.currentPage + introPagerState.currentPageOffsetFraction
        var lastVelocity = 0f
        var lastFrameNanos = 0L

        while (true) {
            withFrameNanos { frameNanos ->
                val pos = introPagerState.currentPage + introPagerState.currentPageOffsetFraction
                if (lastFrameNanos != 0L) {
                    val dtMs = (frameNanos - lastFrameNanos) / 1_000_000f
                    if (dtMs > 0f) {
                        val velocity = (pos - lastPos) / dtMs // page / ms
                        val acceleration = (velocity - lastVelocity) / dtMs // page / ms^2
                        val direction = when {
                            velocity > 0f -> -1f // next page (swipe left)
                            velocity < 0f -> 1f  // previous page (swipe right)
                            else -> 0f
                        }

                        val shouldTilt = introPagerState.isScrollInProgress && (
                            abs(velocity) >= motionConfig.markerPagerVelocityThreshold ||
                                abs(acceleration) >= motionConfig.markerPagerAccelThreshold
                            )

                        markerPagerTiltTarget = if (shouldTilt) {
                            direction * motionConfig.markerPagerTiltMaxDeg
                        } else {
                            0f
                        }

                        lastVelocity = velocity
                    }
                }

                lastPos = pos
                lastFrameNanos = frameNanos
            }
        }
    }
    val markerPagerTiltDeg by animateFloatAsState(
        targetValue = markerPagerTiltTarget,
        animationSpec = tween(
            durationMillis = motionConfig.markerPagerTiltReturnMs,
            easing = FastOutSlowInEasing
        ),
        label = "MarkerPagerTiltDeg"
    )
    val gridAlpha by animateFloatAsState(
        targetValue = if (
            animationState is AnimationState.CameraTransitioning ||
            animationState is AnimationState.LocationPermission ||
            animationState is AnimationState.GeofenceEntering ||
            animationState is AnimationState.NotificationPermission ||
            animationState is AnimationState.Completed
        ) {
            1f
        } else {
            0f
        },
        animationSpec = tween(durationMillis = 420, easing = FastOutSlowInEasing),
        label = "GridAlpha"
    )
    val sceneExitProgress by animateFloatAsState(
        targetValue = if (animationState is AnimationState.Completed) 1f else 0f,
        animationSpec = tween(durationMillis = motionConfig.sceneExitDurationMs, easing = FastOutSlowInEasing),
        label = "SceneExitProgress"
    )
    val sceneExitAlpha = (1f - sceneExitProgress).coerceIn(0f, 1f)

    // Ripple plays once geofence flow is completed.
    val rippleProgress by animateFloatAsState(
        targetValue = if (enteredFence) 1f else 0f,
        animationSpec = tween(durationMillis = motionConfig.rippleDurationMs, easing = FastOutSlowInEasing),
        label = "RippleProgress"
    )

    // Marker idle floating after it appears.
    val markerFloatTransition = rememberInfiniteTransition(label = "MarkerFloat")
    val markerFloatPhase by markerFloatTransition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = motionConfig.markerFloatDurationMs, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "MarkerFloatPhase"
    )

    // Trigger haptic pulses when entering fence is confirmed.
    LaunchedEffect(enteredFence) {
        if (!enteredFence) return@LaunchedEffect

        val pulseOffsetsMs = listOf(0L, 260L, 520L)
        pulseOffsetsMs.forEachIndexed { index, offsetMs ->
            if (index > 0) {
                delay(offsetMs - pulseOffsetsMs[index - 1])
            }
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    // Grid deceleration is synchronized with geofence entrance progress.
    LaunchedEffect(animationState, geofenceEnterProgress) {
        gridVelocityFactor = if (
            animationState is AnimationState.GeofenceEntering
        ) {
            (1f - geofenceEnterProgress).coerceIn(0f, 1f)
        } else if (
            animationState is AnimationState.CameraTransitioning ||
            animationState is AnimationState.LocationPermission
        ) {
            1f
        } else {
            0f
        }
    }

    // Permission re-check when returning from Settings.
    DisposableEffect(lifecycleOwner, animationState) {
        val observer = LifecycleEventObserver { _, event ->
            if (event != Lifecycle.Event.ON_RESUME) return@LifecycleEventObserver
            if (animationState is AnimationState.LocationPermission && hasRequiredLocationPermission(appContext)) {
                locationGranted = true
            }
            if (animationState is AnimationState.NotificationPermission && hasNotificationPermission(appContext)) {
                notificationGranted = true
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // When entering each permission stage, sync current permission state immediately.
    LaunchedEffect(animationState) {
        if (animationState is AnimationState.CameraTransitioning) {
            delay(motionConfig.cameraDurationMs.toLong())
            if (animationState is AnimationState.CameraTransitioning) {
                animationState = AnimationState.LocationPermission
            }
        }
        if (animationState is AnimationState.LocationPermission) {
            locationGranted = hasRequiredLocationPermission(appContext)
        }
        if (animationState is AnimationState.NotificationPermission) {
            notificationGranted = hasNotificationPermission(appContext)
        }
    }

    // After top notification animation is done, then show notification permission card.
    LaunchedEffect(enteredFence, animationState) {
        if (enteredFence && animationState is AnimationState.GeofenceEntering) {
            delay(motionConfig.notificationShakeDelayMs + motionConfig.notificationShakeDurationMs)
            if (animationState is AnimationState.GeofenceEntering) {
                animationState = AnimationState.NotificationPermission
            }
        }
    }

    // Continuous frame loop for deterministic linear horizontal grid motion.
    LaunchedEffect(Unit) {
        val baseSpeedPerMs = 1f / motionConfig.gridLoopDurationMs
        var lastFrameNanos = 0L

        while (true) {
            withFrameNanos { frameNanos ->
                if (lastFrameNanos == 0L) {
                    lastFrameNanos = frameNanos
                    return@withFrameNanos
                }

                val deltaMs = (frameNanos - lastFrameNanos) / 1_000_000f
                lastFrameNanos = frameNanos

                gridShift = wrap01(gridShift + (baseSpeedPerMs * gridVelocityFactor * deltaMs))
            }
        }
    }

    // Stroke object is stable and reused by canvas drawing.
    val stroke = remember(density) {
        Stroke(width = with(density) { 4.dp.toPx() })
    }

    val gridColor = colorScheme.outlineVariant.copy(alpha = if (isDarkMode) 0.34f else 0.28f)

    Box(modifier = modifier.clipToBounds()) {
        // 3D scene layer (camera-transformed).
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationX = -sceneSize.width * sceneExitProgress
                    alpha = sceneExitAlpha
                }
                .pointerInput(animationState) {
                    detectTapGestures { }
                }
                .graphicsLayer {
                    scaleY = cameraScaleY
                    rotationX = cameraRotationX
                    // Higher distance => weaker perspective distortion from camera rotation.
                    cameraDistance = 12f * density.density * 72f
                }
        ) {
            SceneCanvas(
                sceneSize = sceneSize,
                onSceneSizeChanged = { sceneSize = it },
                gridShift = gridShift,
                gridAlpha = gridAlpha,
                gridColor = gridColor,
                stroke = stroke,
                cameraScaleY = cameraScaleY,
                markerEnterProgress = markerEnterProgress,
                markerTiltProgress = markerTiltProgress,
                markerPagerTiltDeg = markerPagerTiltDeg,
                markerFloatPhase = markerFloatPhase,
                geofenceEnterProgress = geofenceEnterProgress,
                enteredFence = enteredFence,
                rippleProgress = rippleProgress,
                markerTintColor = colorScheme.primary,
                geofenceColor = colorScheme.secondary,
                rippleColor = colorScheme.primary,
                pedestalColor = colorScheme.primary,
                modifier = Modifier.fillMaxSize()
            )

        }

        AnimatedVisibility(
            visible = animationState is AnimationState.WaitingForStart,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = bottomCardPadding)
                .fillMaxWidth()
                .graphicsLayer {
                    alpha = introCardAlpha * sceneExitAlpha
                    translationX = -sceneSize.width * sceneExitProgress
                },
            enter = fadeIn(animationSpec = tween(260, easing = FastOutSlowInEasing)),
            exit = fadeOut(animationSpec = tween(220, easing = FastOutSlowInEasing))
        ) {
            IntroStartStage(
                pages = introPages,
                pagerState = introPagerState,
                motionConfig = motionConfig,
                onStart = { animationState = AnimationState.CameraTransitioning }
            )
        }

        AnimatedVisibility(
            visible = animationState is AnimationState.LocationPermission,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(start = 24.dp, end = 24.dp, bottom = bottomCardPadding + 36.dp)
                .widthIn(max = 520.dp)
                .fillMaxWidth()
                .graphicsLayer {
                    translationX = -sceneSize.width * sceneExitProgress
                    alpha = sceneExitAlpha
                },
            enter = slideInHorizontally(
                initialOffsetX = { full -> full / 2 },
                animationSpec = tween(280, easing = FastOutSlowInEasing)
            ) + fadeIn(),
            exit = slideOutHorizontally(
                targetOffsetX = { full -> -full / 2 },
                animationSpec = tween(280, easing = FastOutSlowInEasing)
            ) + fadeOut()
        ) {
                PermissionCard(
                    title = stringResource(R.string.onboarding_location_permission_title),
                    body = stringResource(R.string.onboarding_location_permission_body),
                    isGranted = locationGranted,
                    motionConfig = motionConfig,
                    primaryActionLabel = stringResource(R.string.go_to_settings),
                    onPrimary = { appContext.openAppSettings() },
                    onSecondary = {
                        animationState = AnimationState.GeofenceEntering
                    },
                    onGrantedNext = {
                        animationState = AnimationState.GeofenceEntering
                    }
                )
            }

        AnimatedVisibility(
            visible = animationState is AnimationState.NotificationPermission,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(start = 24.dp, end = 24.dp, bottom = bottomCardPadding + 36.dp)
                .widthIn(max = 520.dp)
                .fillMaxWidth()
                .graphicsLayer {
                    translationX = -sceneSize.width * sceneExitProgress
                    alpha = sceneExitAlpha
                },
            enter = fadeIn(animationSpec = tween(280, easing = FastOutSlowInEasing)),
            exit = fadeOut(animationSpec = tween(240, easing = FastOutSlowInEasing))
        ) {
                PermissionCard(
                    title = stringResource(R.string.onboarding_notification_permission_title),
                    body = stringResource(R.string.onboarding_notification_permission_body),
                    isGranted = notificationGranted,
                    motionConfig = motionConfig,
                    primaryActionLabel = stringResource(R.string.onboarding_allow),
                    onPrimary = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            notificationGranted = true
                        }
                    },
                    onSecondary = {
                        animationState = AnimationState.Completed
                    },
                    onGrantedNext = {
                        animationState = AnimationState.Completed
                    }
                )
            }

        // Notification skeleton appears from top when fence is entered.
        AnimatedVisibility(
            visible = enteredFence,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 12.dp, start = 16.dp, end = 16.dp)
                .widthIn(max = NOTIFICATION_MAX_WIDTH_DP.dp)
                .fillMaxWidth()
                .graphicsLayer {
                    translationX = -sceneSize.width * sceneExitProgress
                    alpha = sceneExitAlpha
                }
                .zIndex(2f),
            enter = slideInVertically(
                initialOffsetY = { fullHeight -> -fullHeight },
                animationSpec = tween(durationMillis = motionConfig.notificationSlideInMs, easing = FastOutSlowInEasing)
            ) + fadeIn(animationSpec = tween(durationMillis = motionConfig.notificationFadeInMs))
        ) {
            NotificationSkeletonCard(motionConfig = motionConfig)
        }

        AnimatedVisibility(
            visible = animationState is AnimationState.Completed,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 24.dp),
            enter = fadeIn(animationSpec = tween(durationMillis = 280, delayMillis = 320))
        ) {
            FinalSetupCard(onStartNow = onAnimationFinished)
        }

        if (showNotificationRetryDialog) {
            AlertDialog(
                onDismissRequest = { showNotificationRetryDialog = false },
                title = {
                    Text(
                        text = stringResource(R.string.onboarding_notification_permission_title),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                text = {
                    Text(
                        text = stringResource(R.string.onboarding_notification_retry_message),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showNotificationRetryDialog = false
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                notificationGranted = true
                            }
                        }
                    ) {
                        Text(
                            text = stringResource(R.string.retry),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showNotificationRetryDialog = false }) {
                        Text(
                            text = stringResource(R.string.onboarding_later),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )
        }

        if (showNotificationSettingsDialog) {
            AlertDialog(
                onDismissRequest = { showNotificationSettingsDialog = false },
                title = {
                    Text(
                        text = stringResource(R.string.onboarding_notification_permission_title),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                text = {
                    Text(
                        text = stringResource(R.string.onboarding_notification_settings_message),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showNotificationSettingsDialog = false
                            appContext.openAppSettings()
                        }
                    ) {
                        Text(
                            text = stringResource(R.string.go_to_settings),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showNotificationSettingsDialog = false }) {
                        Text(
                            text = stringResource(R.string.onboarding_later),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )
        }
    }
}

private fun hasRequiredLocationPermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_BACKGROUND_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    } else {
        val fineGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        fineGranted && coarseGranted
    }
}

private fun hasNotificationPermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    } else {
        true
    }
}

private fun Context.openAppSettings() {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", packageName, null)
    }
    startActivity(intent)
}

private fun Context.findActivity(): Activity? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun LocationOnboardingSceneLightPreview() {
    GeoAlarmTheme(darkTheme = false, dynamicColor = false) {
        LocationOnboardingScene(
            modifier = Modifier.fillMaxSize(),
            isDarkMode = false
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun LocationOnboardingSceneDarkPreview() {
    GeoAlarmTheme(darkTheme = true, dynamicColor = false) {
        LocationOnboardingScene(
            modifier = Modifier.fillMaxSize(),
            isDarkMode = true
        )
    }
}
