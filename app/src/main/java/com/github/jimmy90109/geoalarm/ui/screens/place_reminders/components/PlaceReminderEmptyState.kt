package com.github.jimmy90109.geoalarm.ui.screens.place_reminders.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.jimmy90109.geoalarm.R
import com.github.jimmy90109.geoalarm.ui.theme.GeoAlarmTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch

@Composable
fun PlaceReminderEmptyState(
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 36.dp),
    ) {
        val isWide = maxWidth >= 560.dp
        val textContent: @Composable () -> Unit = {
            Column(
                verticalArrangement = Arrangement.spacedBy(18.dp),
                horizontalAlignment = Alignment.Start,
            ) {
                Text(
                    text = stringResource(R.string.place_reminder_empty_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = stringResource(R.string.place_reminder_empty_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (isWide) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PlaceReminderEmptyAnimation(
                    modifier = Modifier
                        .weight(1f)
                        .widthIn(max = 420.dp),
                )
                Box(modifier = Modifier.weight(1f)) {
                    textContent()
                }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                PlaceReminderEmptyAnimation(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 420.dp),
                )
                textContent()
            }
        }
    }
}

@OptIn(ExperimentalTextApi::class)
@Composable
private fun PlaceReminderEmptyAnimation(modifier: Modifier = Modifier) {
    val density = LocalDensity.current
    val attraction = remember { Animatable(0f) }
    val badgeAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        while (true) {
            attraction.snapTo(0f)
            badgeAlpha.snapTo(0f)

            val badgeIn = launch {
                badgeAlpha.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 420, easing = FastOutSlowInEasing),
                )
            }
            val approach = launch {
                attraction.animateTo(
                    targetValue = 1f,
                    animationSpec = spring(
                        dampingRatio = 0.82f,
                        stiffness = Spring.StiffnessLow,
                    ),
                )
            }
            joinAll(badgeIn, approach)
            delay(1000)

            val badgeOut = launch {
                badgeAlpha.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(durationMillis = 140, easing = FastOutSlowInEasing),
                )
            }
            val returnHome = launch {
                attraction.animateTo(
                    targetValue = 0f,
                    animationSpec = spring(
                        dampingRatio = 0.9f,
                        stiffness = Spring.StiffnessMediumLow,
                    ),
                )
            }
            joinAll(badgeOut, returnHome)
            delay(420)
        }
    }

    val phoneWidth = 48.dp
    val phoneHeight = 84.dp
    val phoneStartPadding = 30.dp
    val phoneBorderWidth = 4.dp
    val phoneDotSize = 8.dp
    val roundedFontFamily = remember {
        FontFamily(
            Font(
                resId = R.font.nunito,
                weight = FontWeight.Black,
                variationSettings = FontVariation.Settings(
                    FontVariation.weight(1000),
                ),
            )
        )
    }
    val watchLabelStyle = MaterialTheme.typography.headlineMedium.copy(
        fontFamily = roundedFontFamily,
        fontSize = 29.sp,
        fontWeight = FontWeight.Black,
    )

    BoxWithConstraints(
        modifier = modifier
            .heightIn(max = 220.dp)
            .height(220.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        val progress = attraction.value
        val phoneTranslationX = with(density) {
            ((maxWidth - phoneWidth) / 2 - phoneStartPadding).toPx() * progress
        }
        val geofenceTranslationX = with(density) { -116.dp.toPx() * progress }

        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 14.dp, end = 22.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_schedule_rounded_bold),
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = "24 / 7",
                style = watchLabelStyle,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 172.dp, y = (-96).dp)
                .graphicsLayer {
                    translationX = geofenceTranslationX
                }
                .requiredSize(420.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f))
                .border(0.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.42f), CircleShape),
        )

        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = phoneStartPadding, bottom = 24.dp)
                .graphicsLayer {
                    translationX = phoneTranslationX
                }
                .size(width = phoneWidth + 16.dp, height = phoneHeight + 16.dp),
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .size(width = phoneWidth, height = phoneHeight)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(
                        width = phoneBorderWidth,
                        color = MaterialTheme.colorScheme.onSurface,
                        shape = RoundedCornerShape(10.dp),
                    ),
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 10.dp)
                        .size(phoneDotSize)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onSurface),
                )
            }
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-10).dp, y = 8.dp)
                    .graphicsLayer {
                        alpha = badgeAlpha.value
                        scaleX = 0.8f + badgeAlpha.value * 0.2f
                        scaleY = 0.8f + badgeAlpha.value * 0.2f
                    }
                    .requiredSize(22.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFD32F2F)),
            )
        }
    }
}

@Preview(name = "Place reminder empty - phone", widthDp = 360, heightDp = 720)
@Composable
private fun PlaceReminderEmptyStatePhonePreview() {
    GeoAlarmTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center,
        ) {
            PlaceReminderEmptyState(modifier = Modifier.fillMaxWidth())
        }
    }
}

@Preview(name = "Place reminder empty - wide", widthDp = 720, heightDp = 360)
@Composable
private fun PlaceReminderEmptyStateWidePreview() {
    GeoAlarmTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center,
        ) {
            PlaceReminderEmptyState(modifier = Modifier.fillMaxWidth())
        }
    }
}
