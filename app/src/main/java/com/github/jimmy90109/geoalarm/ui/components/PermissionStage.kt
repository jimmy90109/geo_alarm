package com.github.jimmy90109.geoalarm.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.github.jimmy90109.geoalarm.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
internal fun PermissionCard(
    title: String,
    body: String,
    isGranted: Boolean,
    motionConfig: OnboardingMotionConfig,
    primaryActionLabel: String,
    onPrimary: () -> Unit,
    onSecondary: () -> Unit,
    onGrantedNext: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        PermissionActionButtons(
            isGranted = isGranted,
            motionConfig = motionConfig,
            primaryActionLabel = primaryActionLabel,
            onPrimary = onPrimary,
            onSecondary = onSecondary,
            onGrantedNext = onGrantedNext,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun PermissionActionButtons(
    isGranted: Boolean,
    motionConfig: OnboardingMotionConfig,
    primaryActionLabel: String,
    onPrimary: () -> Unit,
    onSecondary: () -> Unit,
    onGrantedNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    var startGrantedExpand by remember { mutableStateOf(false) }
    LaunchedEffect(isGranted) {
        if (isGranted) {
            startGrantedExpand = false
            delay(motionConfig.permissionGrantedDelayMs)
            startGrantedExpand = true
        } else {
            startGrantedExpand = false
        }
    }
    val leftWeight by animateFloatAsState(
        targetValue = if (isGranted && startGrantedExpand) 0.0001f else 1f,
        animationSpec = tween(durationMillis = motionConfig.permissionExpandDurationMs, easing = FastOutSlowInEasing),
        label = "PermissionLeftWeight"
    )
    val leftAlpha by animateFloatAsState(
        targetValue = if (isGranted && startGrantedExpand) 0f else 1f,
        animationSpec = tween(durationMillis = motionConfig.permissionFadeDurationMs, easing = FastOutSlowInEasing),
        label = "PermissionLeftAlpha"
    )
    val buttonSpacing by androidx.compose.animation.core.animateDpAsState(
        targetValue = if (isGranted && startGrantedExpand) 0.dp else 8.dp,
        animationSpec = tween(durationMillis = motionConfig.permissionSpacingDurationMs, easing = FastOutSlowInEasing),
        label = "PermissionButtonSpacing"
    )

    Row(
        modifier = modifier.height(48.dp),
        horizontalArrangement = Arrangement.spacedBy(buttonSpacing)
    ) {
        TextButton(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                onSecondary()
            },
            enabled = !isGranted,
            modifier = Modifier
                .fillMaxHeight()
                .weight(leftWeight)
                .graphicsLayer { alpha = leftAlpha }
        ) {
            Text(
                text = androidx.compose.ui.res.stringResource(R.string.onboarding_set_later),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Button(
            onClick = {
                haptic.performHapticFeedback(
                    if (isGranted) HapticFeedbackType.LongPress else HapticFeedbackType.ContextClick
                )
                if (isGranted) onGrantedNext() else onPrimary()
            },
            modifier = Modifier
                .fillMaxHeight()
                .weight(1f)
        ) {
            if (isGranted) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null
                )
                Text(
                    text = androidx.compose.ui.res.stringResource(R.string.onboarding_next_step),
                    modifier = Modifier.padding(start = 8.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text(
                    text = primaryActionLabel,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}

@Composable
internal fun FinalSetupCard(onStartNow: () -> Unit) {
    val primary = MaterialTheme.colorScheme.primary
    val haptic = LocalHapticFeedback.current
    var showIcon by remember { mutableStateOf(false) }
    var showTitle by remember { mutableStateOf(false) }
    var showDesc by remember { mutableStateOf(false) }
    var showButton by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        showIcon = false
        showTitle = false
        showDesc = false
        showButton = false
        delay(180)
        showIcon = true
        delay(240)
        showTitle = true
        delay(280)
        showDesc = true
        delay(340)
        showButton = true
    }

    val buttonHintTransition = rememberInfiniteTransition(label = "FinalButtonHint")
    val buttonPulse by buttonHintTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ButtonPulse"
    )
    val buttonScale by buttonHintTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ButtonScale"
    )

    val iconAlpha by animateFloatAsState(
        targetValue = if (showIcon) 1f else 0f,
        animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing),
        label = "FinalIconAlpha"
    )
    val iconOffsetY by animateFloatAsState(
        targetValue = if (showIcon) 0f else 16f,
        animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing),
        label = "FinalIconOffsetY"
    )
    val titleAlpha by animateFloatAsState(
        targetValue = if (showTitle) 1f else 0f,
        animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing),
        label = "FinalTitleAlpha"
    )
    val titleOffsetY by animateFloatAsState(
        targetValue = if (showTitle) 0f else 14f,
        animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing),
        label = "FinalTitleOffsetY"
    )
    val descAlpha by animateFloatAsState(
        targetValue = if (showDesc) 1f else 0f,
        animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing),
        label = "FinalDescAlpha"
    )
    val descOffsetY by animateFloatAsState(
        targetValue = if (showDesc) 0f else 12f,
        animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing),
        label = "FinalDescOffsetY"
    )
    val buttonAlpha by animateFloatAsState(
        targetValue = if (showButton) 1f else 0f,
        animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing),
        label = "FinalButtonAlpha"
    )
    val buttonOffsetY by animateFloatAsState(
        targetValue = if (showButton) 0f else 10f,
        animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing),
        label = "FinalButtonOffsetY"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .widthIn(max = 520.dp)
                .padding(horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier.heightIn(min = 96.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier.graphicsLayer {
                        alpha = iconAlpha
                        translationY = iconOffsetY
                    }
                ) {
                    CelebrationHeader()
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 36.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = androidx.compose.ui.res.stringResource(R.string.onboarding_completed_title),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            alpha = titleAlpha
                            translationY = titleOffsetY
                        }
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = androidx.compose.ui.res.stringResource(R.string.onboarding_completed_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            alpha = descAlpha
                            translationY = descOffsetY
                        }
                )
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .graphicsLayer {
                    alpha = buttonAlpha
                    translationY = buttonOffsetY
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(240.dp, 88.dp)) {
                val c = Offset(size.width / 2f, size.height / 2f)
                val r1 = 36f + (buttonPulse * 38f)
                val r2 = 22f + (buttonPulse * 30f)
                drawCircle(
                    color = primary.copy(alpha = 0.16f * (1f - buttonPulse)),
                    radius = r1,
                    center = c
                )
                drawCircle(
                    color = primary.copy(alpha = 0.1f * (1f - buttonPulse)),
                    radius = r2,
                    center = c
                )
            }
            Box(
                modifier = Modifier
                    .widthIn(max = 360.dp)
                    .fillMaxWidth()
            ) {
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onStartNow()
                    },
                    enabled = showButton,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .graphicsLayer {
                            scaleX = buttonScale
                            scaleY = buttonScale
                        }
                ) {
                    Text(
                        text = androidx.compose.ui.res.stringResource(R.string.onboarding_start_now),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }
}

@Composable
private fun CelebrationHeader(modifier: Modifier = Modifier) {
    val primary = MaterialTheme.colorScheme.primary
    Box(
        modifier = modifier.size(96.dp),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Filled.CheckCircle,
            contentDescription = null,
            tint = primary,
            modifier = Modifier.size(64.dp)
        )
    }
}

@Composable
internal fun NotificationSkeletonCard(
    motionConfig: OnboardingMotionConfig,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val shakeOffsetX = remember { Animatable(0f) }
    val shakeRotation = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        delay(motionConfig.notificationShakeDelayMs)

        launch {
            shakeOffsetX.animateTo(
                targetValue = 0f,
                animationSpec = keyframes {
                    durationMillis = motionConfig.notificationShakeDurationMs
                    0f at 0
                    -14f at 60
                    12f at 120
                    -10f at 180
                    8f at 240
                    -4f at 300
                    0f at motionConfig.notificationShakeDurationMs
                }
            )
        }

        launch {
            shakeRotation.animateTo(
                targetValue = 0f,
                animationSpec = keyframes {
                    durationMillis = motionConfig.notificationShakeDurationMs
                    0f at 0
                    -2.6f at 60
                    2.2f at 120
                    -1.8f at 180
                    1.2f at 240
                    -0.6f at 300
                    0f at motionConfig.notificationShakeDurationMs
                }
            )
        }
    }

    Box(
        modifier = modifier
            .graphicsLayer {
                translationX = shakeOffsetX.value
                rotationZ = shakeRotation.value
            }
            .clip(RoundedCornerShape(28.dp))
            .background(colorScheme.surfaceVariant)
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(colorScheme.primary)
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .size(width = 140.dp, height = 16.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                )
                Box(
                    modifier = Modifier
                        .size(width = 84.dp, height = 16.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
                )
            }
        }
    }
}
