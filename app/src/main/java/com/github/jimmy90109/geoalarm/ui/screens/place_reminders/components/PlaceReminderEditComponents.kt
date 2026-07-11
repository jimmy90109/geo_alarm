package com.github.jimmy90109.geoalarm.ui.screens.place_reminders.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
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
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.github.jimmy90109.geoalarm.R
import com.github.jimmy90109.geoalarm.data.PlaceReminderType
import com.github.jimmy90109.geoalarm.ui.components.MediaPreviewSelection
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
private const val PlaceReminderPreviewButtonAnimationMillis = 260

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
    onPreviewNotification: () -> Unit,
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
                onPreviewNotification = onPreviewNotification,
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
    hiddenAttachmentId: String?,
    onAttachmentBoundsChanged: (String, Rect) -> Unit,
    onAttachmentClick: (MediaPreviewSelection) -> Unit,
    pagerState: PagerState,
    isLandscape: Boolean,
    modifier: Modifier = Modifier,
) {
    var newChecklistText by remember { mutableStateOf("") }
    val newChecklistFocusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val inputBoundsById = remember { mutableStateMapOf<String, Rect>() }
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
            var pageContainerBounds by remember(page) { mutableStateOf<Rect?>(null) }
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        start = pageStartPadding,
                        end = pageEndPadding,
                    )
                    .onGloballyPositioned { coordinates ->
                        pageContainerBounds = coordinates.boundsInRoot()
                    }
                    .clearFocusOnTapOutsideFocusedInput(
                        inputBounds = inputBoundsById.values.toList(),
                        containerBounds = pageContainerBounds,
                    ) {
                        focusManager.clearFocus(force = true)
                        keyboardController?.hide()
                    },
                contentAlignment = Alignment.Center,
            ) {
                val pageViewportHeight = maxHeight
                val contentWidth = minOf(maxWidth, pageMaxWidth)
                val contentPageExtraVerticalPadding = if (!isLandscape && page == PlaceReminderContentPageIndex) {
                    maxHeight * 0.2f
                } else {
                    0.dp
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    contentAlignment = Alignment.Center,
                ) {
                        Box(
                            modifier = Modifier
                                .width(contentWidth)
                                .heightIn(min = pageViewportHeight)
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
                                    hiddenAttachmentId = hiddenAttachmentId,
                                    onAttachmentBoundsChanged = onAttachmentBoundsChanged,
                                    onAttachmentClick = onAttachmentClick,
                                    onInputBoundsChanged = { id, bounds ->
                                        if (bounds == null) {
                                            inputBoundsById.remove(id)
                                        } else {
                                            inputBoundsById[id] = bounds
                                        }
                                    },
                                )
                                else -> PlaceReminderTriggerFormPage(state = state, onAction = onAction)
                            }
                        }
                    }
            }
        }
    }
}

private fun Modifier.clearFocusOnTapOutsideFocusedInput(
    inputBounds: List<Rect>,
    containerBounds: Rect?,
    onClearFocus: () -> Unit,
): Modifier = pointerInput(inputBounds, containerBounds, onClearFocus) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        if (inputBounds.isEmpty()) return@awaitEachGesture
        val container = containerBounds ?: return@awaitEachGesture
        val rootPosition = Offset(
            x = container.left + down.position.x,
            y = container.top + down.position.y,
        )
        if (inputBounds.none { it.contains(rootPosition) }) {
            onClearFocus()
        }
    }
}

@Composable
internal fun PlaceReminderEditBottomBar(
    pagerState: PagerState,
    state: PlaceReminderEditUiState,
    onNext: () -> Unit,
    onPreviewNotification: () -> Unit,
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
            onPreviewNotification = onPreviewNotification,
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
    onPreviewNotification: () -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isLastPage = pagerState.currentPage == PlaceReminderEditPageCount - 1
    val showPreviewNotification = canProceedFromPlaceReminderPage(PlaceReminderContentPageIndex, state)
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
            PlaceReminderEditPrimaryActions(
                showPreviewNotification = showPreviewNotification,
                actionEnabled = actionEnabled,
                isLastPage = isLastPage,
                isEditMode = state.isEditMode,
                onPreviewNotification = onPreviewNotification,
                onPrimaryAction = {
                    if (isLastPage) onSave() else onNext()
                },
            )
        }
    }
}

@Composable
private fun PlaceReminderEditPrimaryActions(
    showPreviewNotification: Boolean,
    actionEnabled: Boolean,
    isLastPage: Boolean,
    isEditMode: Boolean,
    onPreviewNotification: () -> Unit,
    onPrimaryAction: () -> Unit,
) {
    val previewLabel = stringResource(R.string.preview)
    val primaryLabel = if (!isLastPage) {
        stringResource(R.string.next_step)
    } else if (isEditMode) {
        stringResource(R.string.save)
    } else {
        stringResource(R.string.place_reminder_create_button)
    }
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val previewButtonWidth = 48.dp
        val buttonGap = 8.dp
        val animatedPreviewSlotWidth by animateDpAsState(
            targetValue = if (showPreviewNotification) previewButtonWidth + buttonGap else 0.dp,
            animationSpec = tween(
                durationMillis = PlaceReminderPreviewButtonAnimationMillis,
                easing = FastOutSlowInEasing,
            ),
            label = "placeReminderPreviewSlotWidth",
        )
        val animatedPrimaryWidth by animateDpAsState(
            targetValue = (maxWidth - animatedPreviewSlotWidth).coerceAtLeast(0.dp),
            animationSpec = tween(
                durationMillis = PlaceReminderPreviewButtonAnimationMillis,
                easing = FastOutSlowInEasing,
            ),
            label = "placeReminderPrimaryActionWidth",
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .width(animatedPreviewSlotWidth)
                    .height(56.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                val previewAlphaEnter = fadeIn(
                    animationSpec = tween(
                        durationMillis = PlaceReminderPreviewButtonAnimationMillis,
                        easing = FastOutSlowInEasing,
                    ),
                ) + scaleIn(
                    animationSpec = tween(
                        durationMillis = PlaceReminderPreviewButtonAnimationMillis,
                        easing = FastOutSlowInEasing,
                    ),
                )
                val previewAlphaExit = fadeOut(
                    animationSpec = tween(
                        durationMillis = PlaceReminderPreviewButtonAnimationMillis / 2,
                        easing = FastOutSlowInEasing,
                    ),
                ) + scaleOut(
                    animationSpec = tween(
                        durationMillis = PlaceReminderPreviewButtonAnimationMillis / 2,
                        easing = FastOutSlowInEasing,
                    ),
                )
                androidx.compose.animation.AnimatedVisibility(
                    visible = showPreviewNotification,
                    enter = previewAlphaEnter,
                    exit = previewAlphaExit,
                ) {
                    FilledIconButton(
                        onClick = onPreviewNotification,
                        modifier = Modifier
                            .width(previewButtonWidth)
                            .height(56.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        ),
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = previewLabel)
                    }
                }
            }
            Button(
                onClick = onPrimaryAction,
                enabled = actionEnabled,
                modifier = Modifier
                    .width(animatedPrimaryWidth)
                    .height(56.dp),
                shape = CircleShape,
            ) {
                Text(primaryLabel)
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
