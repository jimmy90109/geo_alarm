package com.github.jimmy90109.geoalarm.ui.screens.place_reminders.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.github.jimmy90109.geoalarm.R
import com.github.jimmy90109.geoalarm.data.PlaceReminderType
import com.github.jimmy90109.geoalarm.ui.viewmodel.PlaceReminderEditAction
import com.github.jimmy90109.geoalarm.ui.viewmodel.PlaceReminderEditUiState
import com.github.jimmy90109.geoalarm.ui.components.TopAppBar as GeoTopAppBar

private val PlaceReminderLandscapeControlWidth = 360.dp
private val PlaceReminderLandscapePaneGap = 24.dp
private val PlaceReminderOverlayMaxWidth = 720.dp
private val PlaceReminderFormMaxWidth = 360.dp
private const val PlaceReminderContentPageIndex = 1
private const val PlaceReminderEditPageCount = 3
private const val PlaceReminderRetainedOffscreenPageCount = PlaceReminderEditPageCount - 1
private const val PlaceReminderPageIndicatorAnimationMillis = 300

@Composable
internal fun PlaceReminderEditTopBar(
    isEditMode: Boolean,
    isLandscape: Boolean,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    applyHorizontalPadding: Boolean = true,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .then(if (applyHorizontalPadding) Modifier.padding(horizontal = 24.dp) else Modifier),
        contentAlignment = if (isLandscape) Alignment.TopEnd else Alignment.TopCenter,
    ) {
        GeoTopAppBar(
            title = {
                Text(
                    if (isEditMode) {
                        stringResource(R.string.place_reminder_edit_title)
                    } else {
                        stringResource(R.string.place_reminder_create_title)
                    }
                )
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                }
            },
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier
                .widthIn(max = if (isLandscape) PlaceReminderLandscapeControlWidth else PlaceReminderOverlayMaxWidth)
                .fillMaxWidth(),
        )
    }
}

@Composable
internal fun PlaceReminderEditLandscapeControls(
    pagerState: PagerState,
    state: PlaceReminderEditUiState,
    onBack: () -> Unit,
    onNext: () -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val navInsets = WindowInsets.navigationBars.asPaddingValues()
    val layoutDirection = LocalLayoutDirection.current
    val bottomPadding = maxOf(24.dp, navInsets.calculateBottomPadding())
    val endPadding = maxOf(24.dp, navInsets.calculateEndPadding(layoutDirection))
    Box(
        modifier = modifier
            .padding(top = 24.dp, end = endPadding, bottom = bottomPadding)
            .windowInsetsPadding(WindowInsets.displayCutout)
            .widthIn(max = PlaceReminderLandscapeControlWidth)
            .fillMaxHeight(),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            PlaceReminderEditTopBar(
                isEditMode = state.isEditMode,
                isLandscape = true,
                onBack = onBack,
                applyHorizontalPadding = false,
            )
            Spacer(modifier = Modifier.weight(1f))
            PlaceReminderEditActionCard(
                pagerState = pagerState,
                state = state,
                onNext = onNext,
                onSave = onSave,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
internal fun PlaceReminderEditContent(
    state: PlaceReminderEditUiState,
    onAction: (PlaceReminderEditAction) -> Unit,
    onSelectPlace: () -> Unit,
    onPickAttachments: () -> Unit,
    pagerState: PagerState,
    isLandscape: Boolean,
    modifier: Modifier = Modifier,
) {
    var newChecklistText by remember { mutableStateOf("") }
    val newChecklistFocusRequester = remember { FocusRequester() }
    val navInsets = WindowInsets.navigationBars.asPaddingValues()
    val cutoutInsets = WindowInsets.displayCutout.asPaddingValues()
    val layoutDirection = LocalLayoutDirection.current
    val startPadding = maxOf(
        24.dp,
        navInsets.calculateStartPadding(layoutDirection),
        if (isLandscape) cutoutInsets.calculateStartPadding(layoutDirection) else 0.dp,
    )
    val endPadding = maxOf(
        24.dp,
        navInsets.calculateEndPadding(layoutDirection),
        if (isLandscape) cutoutInsets.calculateEndPadding(layoutDirection) else 0.dp,
    )
    val contentTopPadding = maxOf(24.dp, WindowInsets.statusBars.asPaddingValues().calculateTopPadding())
    val contentBottomPadding = maxOf(24.dp, navInsets.calculateBottomPadding())
    BoxWithConstraints(
        modifier = modifier,
        contentAlignment = if (isLandscape) Alignment.CenterStart else Alignment.Center,
    ) {
        val landscapePagerWidth = (
            maxWidth -
                startPadding -
                endPadding -
                PlaceReminderLandscapeControlWidth -
                PlaceReminderLandscapePaneGap
            ).coerceAtLeast(280.dp)
        val pagerModifier = if (isLandscape) {
            Modifier
                .align(Alignment.CenterStart)
                .padding(start = startPadding)
                .width(landscapePagerWidth)
                .fillMaxHeight()
        } else {
            Modifier.fillMaxSize()
        }
        val pageStartPadding = if (isLandscape) 0.dp else startPadding
        val pageEndPadding = if (isLandscape) 0.dp else endPadding
        val pageMaxWidth = if (isLandscape) {
            minOf(landscapePagerWidth, PlaceReminderFormMaxWidth)
        } else {
            PlaceReminderFormMaxWidth
        }

        HorizontalPager(
            state = pagerState,
            modifier = pagerModifier,
            beyondViewportPageCount = PlaceReminderRetainedOffscreenPageCount,
            userScrollEnabled = false,
        ) { page ->
            BoxWithConstraints(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                val contentPageExtraVerticalPadding = if (!isLandscape && page == PlaceReminderContentPageIndex) {
                    maxHeight * 0.2f
                } else {
                    0.dp
                }
                Box(
                    modifier = Modifier
                        .padding(
                            start = pageStartPadding,
                            end = pageEndPadding,
                        )
                        .widthIn(max = pageMaxWidth)
                        .fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    top = contentTopPadding + contentPageExtraVerticalPadding,
                                    bottom = contentBottomPadding + contentPageExtraVerticalPadding,
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            when (page) {
                                0 -> PlaceReminderPlaceFormPage(state = state, onSelectPlace = onSelectPlace)
                                PlaceReminderContentPageIndex -> PlaceReminderContentFormPage(
                                    state = state,
                                    onAction = onAction,
                                    newChecklistText = newChecklistText,
                                    onNewChecklistTextChange = { newChecklistText = it },
                                    newChecklistFocusRequester = newChecklistFocusRequester,
                                    onPickAttachments = onPickAttachments,
                                )
                                else -> PlaceReminderTriggerFormPage(state = state, onAction = onAction)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun PlaceReminderEditBottomBar(
    pagerState: PagerState,
    state: PlaceReminderEditUiState,
    onNext: () -> Unit,
    onSave: () -> Unit,
) {
    val navInsets = WindowInsets.navigationBars.asPaddingValues()
    val bottomPadding = maxOf(24.dp, navInsets.calculateBottomPadding())
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 24.dp, bottom = bottomPadding),
        contentAlignment = Alignment.Center,
    ) {
        PlaceReminderEditActionCard(
            pagerState = pagerState,
            state = state,
            onNext = onNext,
            onSave = onSave,
            modifier = Modifier
                .widthIn(max = PlaceReminderOverlayMaxWidth)
                .fillMaxWidth(),
        )
    }
}

@Composable
private fun PlaceReminderEditActionCard(
    pagerState: PagerState,
    state: PlaceReminderEditUiState,
    onNext: () -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isLastPage = pagerState.currentPage == PlaceReminderEditPageCount - 1
    val actionEnabled = if (isLastPage) {
        state.canSave
    } else {
        canProceedFromPlaceReminderPage(pagerState.currentPage, state)
    }
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(44.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            PlaceReminderPageIndicator(
                pageCount = PlaceReminderEditPageCount,
                currentPage = pagerState.currentPage,
            )
            Button(
                onClick = {
                    if (isLastPage) onSave() else onNext()
                },
                enabled = actionEnabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = CircleShape,
            ) {
                Text(
                    if (!isLastPage) {
                        stringResource(R.string.next_step)
                    } else if (state.isEditMode) {
                        stringResource(R.string.save)
                    } else {
                        stringResource(R.string.place_reminder_create_button)
                    }
                )
            }
        }
    }
}

internal fun canProceedFromPlaceReminderPage(
    page: Int,
    state: PlaceReminderEditUiState,
): Boolean = when (page) {
    0 -> state.selectedPosition != null &&
        state.placeName.trim().isNotEmpty()
    1 -> state.title.trim().isNotEmpty() &&
        (when (state.type) {
            PlaceReminderType.TEXT -> state.content.trim().isNotEmpty()
            PlaceReminderType.CHECKLIST -> state.checklistItems.any { it.text.trim().isNotEmpty() }
        } || state.attachments.isNotEmpty())
    else -> state.canSave
}

@Composable
private fun PlaceReminderPageIndicator(
    pageCount: Int,
    currentPage: Int,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(pageCount) { index ->
            val selected = currentPage == index
            val animatedWidth by animateDpAsState(
                targetValue = if (selected) 24.dp else 6.dp,
                animationSpec = tween(
                    durationMillis = PlaceReminderPageIndicatorAnimationMillis,
                    easing = FastOutSlowInEasing,
                ),
                label = "placeReminderPageIndicatorWidth",
            )
            val animatedColor by animateColorAsState(
                targetValue = if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                },
                animationSpec = tween(durationMillis = PlaceReminderPageIndicatorAnimationMillis),
                label = "placeReminderPageIndicatorColor",
            )

            Box(
                modifier = Modifier
                    .height(6.dp)
                    .width(animatedWidth)
                    .clip(RoundedCornerShape(50))
                    .background(animatedColor),
            )
        }
    }
}
