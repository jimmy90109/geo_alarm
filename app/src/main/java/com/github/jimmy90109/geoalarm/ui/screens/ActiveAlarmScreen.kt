package com.github.jimmy90109.geoalarm.ui.screens

import android.content.res.Configuration.ORIENTATION_LANDSCAPE
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.jimmy90109.geoalarm.R
import com.github.jimmy90109.geoalarm.data.Alarm
import kotlinx.coroutines.delay
import kotlin.math.sqrt

@Composable
fun ActiveAlarmScreen(
    modifier: Modifier = Modifier,
    alarm: Alarm,
    progress: Int,
    distanceMeters: Int?,
    onStopAlarm: () -> Unit,
) {
    // Colors
    // Use a low saturation, elegant primary color.
    val primaryColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
    val surfaceColor = MaterialTheme.colorScheme.surface

    // Breathing Animation
    val infiniteTransition = rememberInfiniteTransition(label = "breathing")
    val alphaAnim by infiniteTransition.animateFloat(
        initialValue = 0.2f, // User preferred lower alpha
        targetValue = 0.5f,  // Breathe up slightly
        animationSpec = infiniteRepeatable(
            animation = tween(
                1500,
                easing = FastOutSlowInEasing,
            ),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "alpha",
    )

    val radiusOffset by infiniteTransition.animateFloat(
        initialValue = -20f, // Breathe out range (pixels)
        targetValue = 20f,   // Breathe in range
        animationSpec = infiniteRepeatable(
            animation = tween(
                1500,
                easing = FastOutSlowInEasing,
            ),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "radius",
    )

    // Check for states
    val isFarState = distanceMeters == -1
    val isArrived = distanceMeters == 0

    // Animated Progress with Easing
    val targetProgress = when {
        isArrived -> 100f
        isFarState -> 5f
        else -> progress.toFloat()
    }

    val animatedProgress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = tween(
            durationMillis = 1000, easing = FastOutSlowInEasing
        ),
        label = "progress",
    )

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == ORIENTATION_LANDSCAPE

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(surfaceColor)
    ) {
        // Full screen progress background
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Circular Progress Logic
            // Origin: LeftCenter (0, size.height / 2)
            val origin = Offset(0f, size.height / 2)

            val currentRadius = when {
                isArrived -> {
                    // Arrived: Expand to fill screen (Max radius + padding)
                    sqrt(size.width * size.width + size.height * size.height)
                }

                isFarState -> {
                    // FAR State: Small pulsating circle, no expansion
                    // Max size ~15% of width + breathing
                    (size.width * 0.15f) + radiusOffset
                }

                else -> {
                    // Normal State: Expand to cover screen
                    val maxRadius =
                        sqrt(size.width * size.width + (size.height / 2) * (size.height / 2))
                    maxRadius * (animatedProgress / 100f) + radiusOffset
                }
            }

            // Draw Expanding Circle with Breathing Opacity
            drawCircle(
                color = primaryColor.copy(alpha = alphaAnim),
                radius = currentRadius.coerceAtLeast(0f), // Ensure non-negative radius
                center = origin,
            )
        }

        // Info Overlay
        if (isLandscape) {
            // Landscape Layout: Row (Info Left, Button Right)
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Left Column: Distance & Name
                Column(
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.weight(1f)
                ) {
                    if (isArrived) {
                        Text(
                            text = stringResource(R.string.arrived_status),
                            style = MaterialTheme.typography.displaySmall.copy(
                                fontWeight = FontWeight.Bold,
                            )
                        )
                    } else if (isFarState) {
                        Text(
                            text = stringResource(R.string.notification_power_saving),
                            style = MaterialTheme.typography.displaySmall.copy(
                                // Smaller for long text
                                fontWeight = FontWeight.Bold,
                            )
                        )
                    } else {
                        Text(
                            text = if (distanceMeters != null) stringResource(
                                R.string.distance_meters,
                                distanceMeters
                            ) else "--", style = MaterialTheme.typography.displayLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 64.sp, // Slightly smaller for landscape
                            )
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = alarm.name,
                        style = MaterialTheme.typography.headlineSmall.copy(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
                        ),
                    )
                }

                // Right Column: Button
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    Button(
                        onClick = onStopAlarm,
                    ) {
                        Text(
                            modifier = Modifier.padding(16.dp),
                            text = stringResource(if (isArrived) R.string.notification_turn_off else R.string.cancel_alarm),
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                            )
                        )
                    }
                }
            }
        } else {
            // Portrait Layout: Centered Column
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Distance or Status
                if (isArrived) {
                    Text(
                        text = stringResource(R.string.arrived_status),
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontWeight = FontWeight.Bold,
                        )
                    )
                } else if (isFarState) {
                    Text(
                        text = stringResource(R.string.notification_power_saving),
                        style = MaterialTheme.typography.displayMedium.copy(
                            // Smaller than displayLarge
                            fontWeight = FontWeight.Bold,
                        ),
                        textAlign = TextAlign.Center
                    )
                } else {
                    Text(
                        text = if (distanceMeters != null) stringResource(
                            R.string.distance_meters, distanceMeters
                        ) else "--", style = MaterialTheme.typography.displayMedium.copy(
                            fontWeight = FontWeight.Bold,
                        )
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Destination Name
                Text(
                    text = alarm.name, style = MaterialTheme.typography.headlineMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
                    )
                )

                Spacer(modifier = Modifier.height(64.dp))

                // Stop Button
                Button(
                    onClick = onStopAlarm,
                ) {
                    Text(
                        modifier = Modifier.padding(16.dp),
                        text = stringResource(if (isArrived) R.string.notification_turn_off else R.string.cancel_alarm),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                        )
                    )
                }
            }
        }
    }
}

@Preview
@Composable
fun ActiveAlarmScreenPreview() {
    val mockAlarm = Alarm(
        name = "Taipei Main Station",
        latitude = 0.0,
        longitude = 0.0,
        radius = 100.0,
        isEnabled = true
    )

    // Simulation state
    val progressState = remember { mutableIntStateOf(10) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(2000)
            progressState.intValue = if (progressState.intValue == 10) 95 else 10
        }
    }

    MaterialTheme {
        ActiveAlarmScreen(
            alarm = mockAlarm,
            progress = progressState.intValue,
            distanceMeters = if (progressState.intValue == 10) 5000 else 250,
            onStopAlarm = {},
        )
    }
}
