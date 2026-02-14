package com.github.jimmy90109.geoalarm.ui.components

import android.content.res.Configuration
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.jimmy90109.geoalarm.R
import kotlinx.coroutines.launch

private const val INTRO_TEXT_MAX_WIDTH_DP = 560

internal data class IntroPage(val title: String, val bullets: List<String>)

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun IntroStartStage(
    pages: List<IntroPage>,
    pagerState: androidx.compose.foundation.pager.PagerState,
    motionConfig: OnboardingMotionConfig,
    onStart: () -> Unit
) {
    val pageCount = pages.size
    val activePageIndex = pagerState.currentPage
    val isLastPage = activePageIndex == pageCount - 1
    val coroutineScope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val introBodyBottomPadding = if (isLandscape) 0.dp else 80.dp

    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .then(
                                if (isLandscape) {
                                    Modifier
                                } else {
                                    Modifier.statusBarsPadding().padding(top = 32.dp)
                                }
                            )
                            .widthIn(max = INTRO_TEXT_MAX_WIDTH_DP.dp)
                            .fillMaxWidth()
                    ) {
                        Text(
                            text = pages[page].title,
                            style = MaterialTheme.typography.displayLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                            textAlign = TextAlign.End,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .widthIn(max = INTRO_TEXT_MAX_WIDTH_DP.dp)
                            .fillMaxWidth()
                            .padding(bottom = introBodyBottomPadding)
                    ) {
                        IntroBulletList(
                            items = pages[page].bullets,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
        Column(
            modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(pageCount) { index ->
                    val selected = activePageIndex == index

                    val animatedWidth by animateDpAsState(
                        targetValue = if (selected) 24.dp else 6.dp,
                        animationSpec = tween(
                            durationMillis = 300,
                            easing = FastOutSlowInEasing
                        ),
                        label = "indicatorWidth"
                    )

                    val animatedColor by animateColorAsState(
                        targetValue = if (selected)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                        animationSpec = tween(300),
                        label = "indicatorColor"
                    )

                    Box(
                        modifier = Modifier
                            .height(6.dp)
                            .width(animatedWidth)
                            .clip(RoundedCornerShape(50))
                            .background(animatedColor)
                    )
                }
            }
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .widthIn(max = 360.dp)
                        .fillMaxWidth()
                ) {
                    Button(
                        onClick = {
                            if (isLastPage) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onStart()
                            } else {
                                haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(
                                        page = activePageIndex + 1,
                                        animationSpec = tween(
                                            durationMillis = motionConfig.introPageSwitchDurationMs,
                                            easing = EaseOutCubic
                                        )
                                    )
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Text(
                            text = if (isLastPage) {
                                stringResource(R.string.onboarding_start_using)
                            } else {
                                stringResource(R.string.onboarding_next_step)
                            },
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun IntroBulletList(
    items: List<String>,
    modifier: Modifier = Modifier
) {
    val textStyle = MaterialTheme.typography.bodyLarge.copy(
        fontWeight = FontWeight.Bold,
        lineHeight = 22.sp
    )
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items.forEach { item ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = "•",
                    style = textStyle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .width(16.dp)
                        .alignByBaseline()
                )
                Text(
                    text = item,
                    style = textStyle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Start,
                    modifier = Modifier
                        .weight(1f)
                        .alignByBaseline()
                )
            }
        }
    }
}
