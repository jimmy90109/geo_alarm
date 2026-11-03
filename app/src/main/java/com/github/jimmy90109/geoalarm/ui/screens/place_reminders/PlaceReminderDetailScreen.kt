@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.github.jimmy90109.geoalarm.ui.screens.place_reminders

import android.Manifest
import android.content.res.Configuration
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsIgnoringVisibility
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsIgnoringVisibility
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.zIndex
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle

import com.github.jimmy90109.geoalarm.R
import com.github.jimmy90109.geoalarm.data.PlaceReminderAttachment
import com.github.jimmy90109.geoalarm.data.PlaceReminderAttachmentType
import com.github.jimmy90109.geoalarm.data.PlaceReminderItem
import com.github.jimmy90109.geoalarm.data.PlaceReminderType
import com.github.jimmy90109.geoalarm.data.PlaceReminderWithItems
import com.github.jimmy90109.geoalarm.ui.components.AlarmIconBadge
import com.github.jimmy90109.geoalarm.ui.components.BackgroundLocationPermissionDialog
import com.github.jimmy90109.geoalarm.ui.components.rememberMediaPreviewAspectRatios
import com.github.jimmy90109.geoalarm.ui.components.MediaPreviewMotion
import com.github.jimmy90109.geoalarm.ui.components.MediaPreviewStackCard
import com.github.jimmy90109.geoalarm.ui.components.MediaPreviewItem
import com.github.jimmy90109.geoalarm.ui.components.MediaPreviewOverlay
import com.github.jimmy90109.geoalarm.ui.components.MediaPreviewPreloader
import com.github.jimmy90109.geoalarm.ui.components.MediaPreviewSelection
import com.github.jimmy90109.geoalarm.ui.screens.place_reminders.components.PlaceReminderMediaStack
import com.github.jimmy90109.geoalarm.ui.screens.place_reminders.components.PlaceReminderMediaLayout
import com.github.jimmy90109.geoalarm.ui.components.MediaPreviewType
import com.github.jimmy90109.geoalarm.ui.screens.place_reminders.components.PlaceReminderTextFieldShape
import com.github.jimmy90109.geoalarm.ui.components.TopAppBar as GeoTopAppBar
import com.github.jimmy90109.geoalarm.ui.viewmodel.PlaceReminderDetailEffect
import com.github.jimmy90109.geoalarm.ui.viewmodel.PlaceReminderDetailViewModel
import com.github.jimmy90109.geoalarm.ui.viewmodel.PlaceReminderListViewModel

private val PlaceReminderDetailMaxWidth = 640.dp
private val PlaceReminderDetailActionMaxWidth = 520.dp
private val PlaceReminderDetailLandscapeControlWidth = 360.dp
private val PlaceReminderDetailLandscapePaneGap = 24.dp
private val PlaceReminderDetailCardShape = RoundedCornerShape(24.dp)
private val PlaceReminderDetailActionShape = RoundedCornerShape(44.dp)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PlaceReminderDetailScreen(
    viewModel: PlaceReminderDetailViewModel,
    reminderId: String,
    onBack: () -> Unit,
    onEdit: (String) -> Unit,
) {
    val reminderWithItems by viewModel.reminder.collectAsStateWithLifecycle()
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showResetChecklistDialog by remember { mutableStateOf(false) }
    var newItemText by remember { mutableStateOf("") }
    val context = LocalContext.current
    val density = LocalDensity.current
    var topBarHeightPx by remember { mutableStateOf(0) }
    var actionBarHeightPx by remember { mutableStateOf(0) }
    val haptic = LocalHapticFeedback.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val lifecycleOwner = LocalLifecycleOwner.current
    var selectedPreview by remember { mutableStateOf<MediaPreviewSelection?>(null) }
    val previewTransitionProgress = remember(reminderId) { Animatable(0f) }
    var activePreviewItemId by remember { mutableStateOf<String?>(null) }
    var previewStackCards by remember { mutableStateOf<List<MediaPreviewStackCard>>(emptyList()) }
    val previewSourceBounds = remember { mutableStateMapOf<String, Rect>() }
    var permissionState by remember {
        mutableStateOf(PlaceReminderListViewModel.permissionState(context))
    }
    var pendingEnableReminderId by remember { mutableStateOf<String?>(null) }
    var showBackgroundLocationDialog by remember { mutableStateOf(false) }
    lateinit var requestEnableReminder: (String) -> Unit

    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        permissionState = PlaceReminderListViewModel.permissionState(context)
        if (permissionState.hasNotifications) {
            pendingEnableReminderId?.let { requestEnableReminder(it) }
        } else {
            pendingEnableReminderId = null
        }
    }
    val locationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        permissionState = PlaceReminderListViewModel.permissionState(context)
        if (permissionState.hasPreciseLocation) {
            pendingEnableReminderId?.let { requestEnableReminder(it) }
        } else {
            pendingEnableReminderId = null
        }
    }

    fun refreshPermissions() {
        permissionState = PlaceReminderListViewModel.permissionState(context)
    }

    requestEnableReminder = { id ->
        refreshPermissions()
        when {
            !permissionState.hasPreciseLocation -> {
                pendingEnableReminderId = id
                locationLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                    )
                )
            }
            !permissionState.hasNotifications -> {
                pendingEnableReminderId = id
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
            !permissionState.hasBackgroundLocation -> {
                pendingEnableReminderId = id
                showBackgroundLocationDialog = true
            }
            !permissionState.isLocationServiceEnabled -> {
                pendingEnableReminderId = id
                context.openLocationSettings()
            }
            else -> {
                pendingEnableReminderId = null
                viewModel.setEnabled(id, true)
            }
        }
    }

    LaunchedEffect(reminderId) {
        viewModel.load(reminderId)
    }
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                PlaceReminderDetailEffect.NavigateBack -> onBack()
            }
        }
    }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshPermissions()
                pendingEnableReminderId?.let { id ->
                    if (permissionState.canEnableReminder) {
                        pendingEnableReminderId = null
                        viewModel.setEnabled(id, true)
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val current = reminderWithItems
    val previewItems = remember(current?.sortedAttachments) {
        current?.sortedAttachments?.map { attachment ->
            attachment.toMediaPreviewItem()
        } ?: emptyList()
    }
    val previewAspectRatios = rememberMediaPreviewAspectRatios(previewItems)
    val previewWindowSize = androidx.compose.ui.platform.LocalWindowInfo.current.containerSize
    LaunchedEffect(previewItems, previewWindowSize) {
        selectedPreview = null
        activePreviewItemId = null
        previewTransitionProgress.snapTo(0f)
    }
    val handleBack = {
        haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
        onBack()
    }
    val handleEdit = {
        haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
        onEdit(reminderId)
    }
    val handleDelete = {
        haptic.performHapticFeedback(HapticFeedbackType.Reject)
        showDeleteDialog = true
    }
    Box(modifier = Modifier.fillMaxSize()) {
        MediaPreviewPreloader(items = previewItems)
        if (current == null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(stringResource(R.string.place_reminder_not_found))
            }
        } else {
            val reminder = current.reminder
            PlaceReminderDetailContent(
                reminderWithItems = current,
                newItemText = newItemText,
                onNewItemTextChange = { newItemText = it },
                onResetChecklist = {
                    haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                    showResetChecklistDialog = true
                },
                onItemCheckedChange = { item, checked ->
                    haptic.performHapticFeedback(
                        if (checked) HapticFeedbackType.ToggleOn else HapticFeedbackType.ToggleOff
                    )
                    viewModel.setItemChecked(item, checked)
                },
                onAddItem = {
                    haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                    viewModel.addItem(reminder.id, newItemText)
                    newItemText = ""
                },
                attachmentAspectRatios = previewAspectRatios,
                onStackCardsChanged = { previewStackCards = it },
                hiddenAttachmentId = activePreviewItemId ?: selectedPreview?.item?.id,
                onAttachmentBoundsChanged = { id, bounds ->
                    previewSourceBounds[id] = bounds
                },
                onAttachmentClick = { selection ->
                    haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                    selectedPreview = selection
                    activePreviewItemId = selection.item.id
                },
                contentPadding = if (isLandscape) {
                    PaddingValues()
                } else {
                    PaddingValues(
                        top = PlaceReminderDetailTopOverlaySpace(),
                        bottom = with(density) { actionBarHeightPx.toDp() },
                    )
                },
                isLandscape = isLandscape,
                modifier = Modifier.fillMaxSize(),
            )
        }

        if (isLandscape) {
            PlaceReminderDetailLandscapeControls(
                title = current?.reminder?.title ?: stringResource(R.string.place_reminder_detail_title),
                lastTriggeredAt = current?.reminder?.lastTriggeredAt,
                onBack = handleBack,
                onEdit = handleEdit,
                onDelete = handleDelete,
                actionEnabled = current != null,
                modifier = Modifier.align(Alignment.CenterEnd),
            )
        } else {
            val shadowClearancePx = with(density) { 24.dp.toPx() }
            // Keep the original occlusion order while the media expands beneath the moving chrome.
            val topChromeMotion = Modifier
                .zIndex(1f)
                .graphicsLayer {
                    translationY = -(topBarHeightPx + shadowClearancePx) * previewTransitionProgress.value
                }
            val bottomChromeMotion = Modifier
                .zIndex(1f)
                .graphicsLayer {
                    translationY = (actionBarHeightPx + shadowClearancePx) * previewTransitionProgress.value
                }
            if (current != null) {
                val topOverlayHeight = with(density) { topBarHeightPx.toDp() }
                    .takeIf { it > 0.dp } ?: PlaceReminderDetailTopOverlaySpace()
                val topMargin = maxOf(
                    24.dp,
                    WindowInsets.statusBarsIgnoringVisibility.asPaddingValues().calculateTopPadding(),
                )
                val cutoutPadding = WindowInsets.displayCutout.asPaddingValues()
                val bottomMargin = maxOf(
                    24.dp,
                    WindowInsets.navigationBarsIgnoringVisibility.asPaddingValues().calculateBottomPadding(),
                ) + cutoutPadding.calculateBottomPadding()
                val bottomOverlayHeight = with(density) { actionBarHeightPx.toDp() }
                val topFadeHeight = topMargin + (topOverlayHeight - topMargin) / 2
                val bottomFadeHeight = if (actionBarHeightPx > 0) {
                    (bottomOverlayHeight + bottomMargin) / 2
                } else {
                    0.dp
                }
                PlaceReminderDetailEdgeFade(
                    isTop = true,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .then(topChromeMotion)
                        .height(topFadeHeight.coerceAtLeast(0.dp)),
                )
                PlaceReminderDetailEdgeFade(
                    isTop = false,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .then(bottomChromeMotion)
                        .height(bottomFadeHeight.coerceAtLeast(0.dp)),
                )
            }
            PlaceReminderDetailTopBar(
                title = current?.reminder?.title ?: stringResource(R.string.place_reminder_detail_title),
                onBack = handleBack,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .then(topChromeMotion)
                    .onSizeChanged { topBarHeightPx = it.height },
            )

            if (current != null) {
                PlaceReminderDetailActionBar(
                    lastTriggeredAt = current.reminder.lastTriggeredAt,
                    onEdit = handleEdit,
                    onDelete = handleDelete,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .then(bottomChromeMotion)
                        .onSizeChanged { actionBarHeightPx = it.height },
                )
            }
        }

        selectedPreview?.let {
            MediaPreviewOverlay(
                selection = it,
                items = previewItems.ifEmpty { listOf(it.item) },
                sourceBoundsById = previewSourceBounds.toMap(),
                sourceCornerRadius = 16.dp,
                aspectRatiosById = previewAspectRatios,
                transitionProgress = previewTransitionProgress,
                hideSystemBars = true,
                stackCards = previewStackCards,
                peekAdjacentMedia = true,
                zoomEnabled = false,
                dismissOnBackgroundTap = true,
                onActiveItemChanged = { item ->
                    activePreviewItemId = item.id
                },
                onDismiss = {
                    selectedPreview = null
                    activePreviewItemId = null
                },
            )
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.place_reminder_delete_title)) },
            text = { Text(stringResource(R.string.place_reminder_delete_message)) },
            confirmButton = {
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.Reject)
                        viewModel.delete()
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                    showDeleteDialog = false
                }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    if (showResetChecklistDialog) {
        val reminder = current?.reminder
        AlertDialog(
            onDismissRequest = { showResetChecklistDialog = false },
            title = { Text(stringResource(R.string.place_reminder_reset_checklist_title)) },
            text = { Text(stringResource(R.string.place_reminder_reset_checklist_message)) },
            confirmButton = {
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                        reminder?.let { viewModel.resetChecklist(it.id) }
                        showResetChecklistDialog = false
                    },
                ) {
                    Text(stringResource(R.string.place_reminder_reset_checklist))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                    showResetChecklistDialog = false
                }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    if (showBackgroundLocationDialog) {
        BackgroundLocationPermissionDialog(
            context = context,
            onDismiss = {
                pendingEnableReminderId = null
                showBackgroundLocationDialog = false
            },
            onOpenSettings = {
                showBackgroundLocationDialog = false
            },
        )
    }

}

@Composable
private fun PlaceReminderDetailEdgeFade(
    isTop: Boolean,
    modifier: Modifier = Modifier,
) {
    val surface = MaterialTheme.colorScheme.surface
    val colors = listOf(
        surface.copy(alpha = 0.96f),
        surface.copy(alpha = 0.72f),
        surface.copy(alpha = 0f),
    )
    Spacer(
        modifier = modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(if (isTop) colors else colors.reversed())),
    )
}

@Composable
private fun PlaceReminderDetailTopBar(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    isLandscape: Boolean = false,
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
                    text = title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back),
                    )
                }
            },
            statusBarInsets = WindowInsets.statusBarsIgnoringVisibility,
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier
                .widthIn(max = if (isLandscape) PlaceReminderDetailLandscapeControlWidth else PlaceReminderDetailActionMaxWidth)
                .fillMaxWidth(),
        )
    }
}

@Composable
private fun PlaceReminderDetailLandscapeControls(
    title: String,
    lastTriggeredAt: Long?,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    actionEnabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val navInsets = WindowInsets.navigationBarsIgnoringVisibility.asPaddingValues()
    val layoutDirection = LocalLayoutDirection.current
    val bottomPadding = maxOf(24.dp, navInsets.calculateBottomPadding())
    val endPadding = maxOf(24.dp, navInsets.calculateEndPadding(layoutDirection))
    Box(
        modifier = modifier
            .padding(top = 24.dp, end = endPadding, bottom = bottomPadding)
            .windowInsetsPadding(WindowInsets.displayCutout)
            .widthIn(max = PlaceReminderDetailLandscapeControlWidth)
            .fillMaxHeight(),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            PlaceReminderDetailTopBar(
                title = title,
                onBack = onBack,
                isLandscape = true,
                applyHorizontalPadding = false,
            )
            Spacer(modifier = Modifier.weight(1f))
            if (actionEnabled) {
                PlaceReminderDetailActionBar(
                    lastTriggeredAt = lastTriggeredAt,
                    onEdit = onEdit,
                    onDelete = onDelete,
                    applyScreenPadding = false,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun PlaceReminderDetailTopOverlaySpace() =
    maxOf(
        WindowInsets.statusBarsIgnoringVisibility.asPaddingValues().calculateTopPadding(),
        24.dp,
    ) + 72.dp

@Composable
internal fun PlaceReminderDetailContent(
    reminderWithItems: PlaceReminderWithItems,
    newItemText: String,
    onNewItemTextChange: (String) -> Unit,
    onResetChecklist: () -> Unit,
    onItemCheckedChange: (PlaceReminderItem, Boolean) -> Unit,
    onAddItem: () -> Unit,
    attachmentAspectRatios: Map<String, Float>,
    onStackCardsChanged: (List<MediaPreviewStackCard>) -> Unit,
    hiddenAttachmentId: String?,
    onAttachmentBoundsChanged: (String, Rect) -> Unit,
    onAttachmentClick: (MediaPreviewSelection) -> Unit,
    contentPadding: PaddingValues,
    isLandscape: Boolean,
    modifier: Modifier = Modifier,
) {
    val navPadding = WindowInsets.navigationBarsIgnoringVisibility.asPaddingValues()
    val cutoutPadding = WindowInsets.displayCutout.asPaddingValues()
    val layoutDirection = LocalLayoutDirection.current
    val startPadding = maxOf(
        24.dp,
        navPadding.calculateStartPadding(layoutDirection),
        if (isLandscape) cutoutPadding.calculateStartPadding(layoutDirection) else 0.dp,
    )
    val endPadding = maxOf(
        24.dp,
        navPadding.calculateEndPadding(layoutDirection),
        if (isLandscape) cutoutPadding.calculateEndPadding(layoutDirection) else 0.dp,
    )
    val bottomPadding = if (isLandscape) {
        maxOf(24.dp, navPadding.calculateBottomPadding())
    } else {
        // The measured action bar already includes the card's distance from the screen bottom.
        contentPadding.calculateBottomPadding()
    }
    val density = LocalDensity.current
    var summaryHeightPx by remember { mutableStateOf(0) }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    var contentBounds by remember { mutableStateOf<Rect?>(null) }
    var checklistAddInputBounds by remember { mutableStateOf<Rect?>(null) }
    val mediaDrawingLayer = rememberGraphicsLayer()
    var mediaDrawingOrigin by remember { mutableStateOf<Offset?>(null) }
    var viewportOrigin by remember { mutableStateOf(Offset.Zero) }
    BoxWithConstraints(
        modifier = modifier
            .onGloballyPositioned { viewportOrigin = it.positionInRoot() }
            .drawWithContent {
                drawContent()
                // Draw outside the scroll container so narrow attachment layouts do not crop a swipe.
                val origin = mediaDrawingOrigin
                if (origin != null && reminderWithItems.sortedAttachments.isNotEmpty()) {
                    val offset = origin - viewportOrigin
                    clipRect {
                        translate(offset.x, offset.y) { drawLayer(mediaDrawingLayer) }
                    }
                }
            },
        contentAlignment = if (isLandscape) Alignment.CenterStart else Alignment.Center,
    ) {
        val viewportHeight = maxHeight
        val landscapeContentWidth = (
            maxWidth -
                startPadding -
                endPadding -
                PlaceReminderDetailLandscapeControlWidth -
                PlaceReminderDetailLandscapePaneGap
            ).coerceAtLeast(280.dp)
        val contentColumnModifier = if (isLandscape) {
            Modifier
                .align(Alignment.CenterStart)
                .padding(start = startPadding)
                .width(landscapeContentWidth)
                .fillMaxHeight()
        } else {
            Modifier.fillMaxSize()
        }
        val horizontalContentPadding = if (isLandscape) 0.dp else startPadding
        Column(
            modifier = contentColumnModifier
                .onGloballyPositioned { contentBounds = it.boundsInRoot() }
                .clearFocusOnTapOutsideFocusedInput(
                    inputBounds = listOfNotNull(checklistAddInputBounds),
                    containerBounds = contentBounds,
                ) {
                    focusManager.clearFocus(force = true)
                    keyboardController?.hide()
                }
                .imePadding()
                .testTag("place_reminder_detail_scroll")
                .verticalScroll(rememberScrollState())
                .padding(
                    start = horizontalContentPadding,
                    top = contentPadding.calculateTopPadding() + 16.dp,
                    end = if (isLandscape) 0.dp else endPadding,
                    bottom = bottomPadding,
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            val contentMaxWidth = if (isLandscape) {
                PlaceReminderDetailLandscapeControlWidth
            } else {
                PlaceReminderDetailMaxWidth
            }
            Column(
                modifier = Modifier
                    .widthIn(max = contentMaxWidth)
                    .fillMaxWidth(),
            ) {
                val hasText = reminderWithItems.reminder.content.isNotBlank()
                val hasChecklist = reminderWithItems.reminder.type == PlaceReminderType.CHECKLIST
                val hasAttachments = reminderWithItems.sortedAttachments.isNotEmpty()
                val compactMedia = !isLandscape && hasAttachments && (hasText || hasChecklist)
                val mediaHeight = (viewportHeight - contentPadding.calculateTopPadding() - 16.dp -
                    bottomPadding - with(density) { summaryHeightPx.toDp() } - 16.dp)
                    .coerceIn(160.dp, 360.dp)
                val media: @Composable () -> Unit = {
                    PlaceReminderMediaStack(
                        items = reminderWithItems.sortedAttachments.map { it.toMediaPreviewItem() },
                        aspectRatiosById = attachmentAspectRatios,
                        onStackCardsChanged = onStackCardsChanged,
                        hiddenAttachmentId = hiddenAttachmentId,
                        onAttachmentBoundsChanged = onAttachmentBoundsChanged,
                        onAttachmentClick = onAttachmentClick,
                        drawingLayer = mediaDrawingLayer,
                        onDrawingOriginChanged = { mediaDrawingOrigin = it },
                        layout = when {
                            isLandscape -> PlaceReminderMediaLayout.Original
                            compactMedia -> PlaceReminderMediaLayout.Compact
                            else -> PlaceReminderMediaLayout.FixedHeight(mediaHeight)
                        },
                    )
                }
                PlaceReminderSummaryCard(
                    reminderWithItems = reminderWithItems,
                    modifier = Modifier.onSizeChanged { summaryHeightPx = it.height },
                    isLandscape = isLandscape,
                    compactBottomSpacing = compactMedia,
                )
                if (hasText || hasChecklist || hasAttachments) {
                    Spacer(Modifier.height(if (compactMedia) 8.dp else 16.dp))
                }
                if (hasText) {
                    PlaceReminderContentWithMedia(compactMedia, media) {
                        PlaceReminderTextContentCard(
                            content = reminderWithItems.reminder.content,
                            headerEndPadding = if (compactMedia) 128.dp else 0.dp,
                        )
                    }
                }
                if (hasChecklist) {
                    if (hasText) Spacer(Modifier.height(16.dp))
                    PlaceReminderContentWithMedia(compactMedia && !hasText, media) {
                        PlaceReminderChecklistCard(
                            reminderWithItems = reminderWithItems,
                            newItemText = newItemText,
                            onNewItemTextChange = onNewItemTextChange,
                            onResetChecklist = onResetChecklist,
                            onItemCheckedChange = onItemCheckedChange,
                            onAddItem = onAddItem,
                            onAddInputBoundsChanged = { checklistAddInputBounds = it },
                            headerEndPadding = if (compactMedia && !hasText) 128.dp else 0.dp,
                        )
                    }
                }
                if (hasAttachments && !compactMedia) {
                    if (hasText || hasChecklist) Spacer(Modifier.height(16.dp))
                    media()
                }
            }
        }
    }
}

@Composable
private fun PlaceReminderContentWithMedia(
    showMedia: Boolean,
    media: @Composable () -> Unit,
    content: @Composable () -> Unit,
) {
    if (!showMedia) {
        content()
        return
    }
    Box(Modifier.fillMaxWidth()) {
        Box(Modifier.padding(top = 96.dp)) {
            content()
        }
        Box(Modifier.align(Alignment.TopEnd)) {
            media()
        }
    }
}

@Composable
private fun PlaceReminderSummaryCard(
    reminderWithItems: PlaceReminderWithItems,
    modifier: Modifier = Modifier,
    isLandscape: Boolean = false,
    compactBottomSpacing: Boolean = false,
) {
    val reminder = reminderWithItems.reminder
    val summaryModifier = modifier
        .fillMaxWidth()
        .padding(start = 16.dp, top = 20.dp, end = 16.dp, bottom = if (compactBottomSpacing) 0.dp else 20.dp)
    val icon: @Composable () -> Unit = {
        CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.primary) {
            AlarmIconBadge(iconKey = reminder.iconKey)
        }
    }
    if (isLandscape) {
        Column(
            modifier = summaryModifier,
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            icon()
            PlaceReminderSummaryText(reminderWithItems, TextAlign.Center)
        }
    } else {
        Row(
            modifier = summaryModifier.testTag("place_reminder_summary"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            icon()
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PlaceReminderSummaryText(reminderWithItems, TextAlign.Start)
            }
        }
    }
}

@Composable
private fun PlaceReminderSummaryText(reminderWithItems: PlaceReminderWithItems, textAlign: TextAlign) {
    val reminder = reminderWithItems.reminder
    Text(
        text = reminder.placeName,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = textAlign,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
    )
    Text(
        text = triggerText(reminder.triggerType, reminder.dwellMinutes),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = textAlign,
    )
}

@Composable
private fun PlaceReminderTextContentCard(content: String, headerEndPadding: Dp = 0.dp) {
    DetailGroupCard {
        Box(Modifier.fillMaxWidth().padding(end = headerEndPadding)) {
            DetailGroupTitle(text = stringResource(R.string.place_reminder_content))
        }
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = PlaceReminderTextFieldShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            Text(
                text = content,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun PlaceReminderChecklistCard(
    reminderWithItems: PlaceReminderWithItems,
    newItemText: String,
    onNewItemTextChange: (String) -> Unit,
    onResetChecklist: () -> Unit,
    onItemCheckedChange: (PlaceReminderItem, Boolean) -> Unit,
    onAddItem: () -> Unit,
    onAddInputBoundsChanged: (Rect?) -> Unit,
    headerEndPadding: Dp = 0.dp,
) {
    val checkedCount = reminderWithItems.sortedItems.count { it.checked }
    DetailGroupCard {
        Column(
            modifier = Modifier.fillMaxWidth().padding(end = headerEndPadding),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            DetailGroupTitle(text = stringResource(R.string.place_reminder_type_checklist))
            Text(
                text = stringResource(
                    R.string.place_reminder_checklist_progress,
                    checkedCount,
                    reminderWithItems.sortedItems.size,
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            reminderWithItems.sortedItems.forEach { item ->
                ChecklistDetailRow(
                    item = item,
                    onCheckedChange = { checked -> onItemCheckedChange(item, checked) },
                )
            }
            ChecklistDetailAddRow(
                value = newItemText,
                onValueChange = onNewItemTextChange,
                onAdd = onAddItem,
                onInputBoundsChanged = onAddInputBoundsChanged,
            )
        }
        FilledTonalButton(
            onClick = onResetChecklist,
            modifier = Modifier.fillMaxWidth().testTag("place_reminder_reset_checklist"),
        ) {
            Icon(Icons.Filled.Refresh, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.place_reminder_reset_checklist))
        }
    }
}

@Composable
private fun ChecklistDetailRow(
    item: PlaceReminderItem,
    onCheckedChange: (Boolean) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = PlaceReminderTextFieldShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Row(
            modifier = Modifier.padding(start = 4.dp, top = 4.dp, end = 16.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(
                checked = item.checked,
                onCheckedChange = onCheckedChange,
            )
            Text(
                text = item.text,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
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
private fun ChecklistDetailAddRow(
    value: String,
    onValueChange: (String) -> Unit,
    onAdd: () -> Unit,
    onInputBoundsChanged: (Rect?) -> Unit,
) {
    val canAdd = value.trim().isNotEmpty()
    DisposableEffect(Unit) {
        onDispose { onInputBoundsChanged(null) }
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .weight(1f)
                .onGloballyPositioned { onInputBoundsChanged(it.boundsInRoot()) },
            singleLine = true,
            placeholder = { Text(stringResource(R.string.place_reminder_new_item)) },
            shape = PlaceReminderTextFieldShape,
        )
        AnimatedVisibility(
            visible = canAdd,
            enter = expandHorizontally(
                animationSpec = MediaPreviewMotion.spatial(),
                expandFrom = Alignment.Start,
            ) + fadeIn(animationSpec = MediaPreviewMotion.effects()),
            exit = shrinkHorizontally(
                animationSpec = MediaPreviewMotion.spatial(),
                shrinkTowards = Alignment.Start,
            ) + fadeOut(animationSpec = MediaPreviewMotion.effects()),
        ) {
            IconButton(
                onClick = onAdd,
                modifier = Modifier.padding(start = 8.dp),
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                ) {
                    Box(
                        modifier = Modifier
                            .width(40.dp)
                            .height(40.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Filled.Add,
                            contentDescription = stringResource(R.string.place_reminder_add_item),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailGroupCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().testTag("place_reminder_detail_group"),
        shape = PlaceReminderDetailCardShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content,
        )
    }
}

@Composable
private fun DetailGroupTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
    )
}

@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
internal fun PlaceReminderDetailActionBar(
    lastTriggeredAt: Long?,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    applyScreenPadding: Boolean = true,
) {
    val navInsets = WindowInsets.navigationBarsIgnoringVisibility.asPaddingValues()
    val bottomPadding = maxOf(24.dp, navInsets.calculateBottomPadding())
    val screenPaddingModifier = if (applyScreenPadding) {
        Modifier
            .windowInsetsPadding(
                WindowInsets.displayCutout.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom),
            )
            .padding(start = 24.dp, end = 24.dp, bottom = bottomPadding)
    } else {
        Modifier
    }
    val editLabel = stringResource(R.string.place_reminder_edit_action)
    val deleteLabel = stringResource(R.string.delete)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .then(screenPaddingModifier),
        contentAlignment = Alignment.Center,
    ) {
        Card(
            modifier = Modifier
                .widthIn(max = PlaceReminderDetailActionMaxWidth)
                .fillMaxWidth(),
            shape = PlaceReminderDetailActionShape,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = lastTriggeredAt?.let {
                        stringResource(R.string.place_reminder_last_triggered, formatTime(it))
                    } ?: stringResource(R.string.place_reminder_no_recent_trigger),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                ButtonGroup(
                    overflowIndicator = { menuState ->
                        ButtonGroupDefaults.OverflowIndicator(menuState = menuState)
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    val buttonGroupScope = this
                    customItem(
                        buttonGroupContent = {
                            FilledIconButton(
                                onClick = onDelete,
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer,
                                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                                ),
                            ) {
                                Icon(Icons.Filled.Delete, contentDescription = deleteLabel)
                            }
                        },
                        menuContent = { menuState ->
                            DropdownMenuItem(
                                text = { Text(deleteLabel) },
                                onClick = {
                                    onDelete()
                                    menuState.dismiss()
                                },
                                leadingIcon = {
                                    Icon(Icons.Filled.Delete, contentDescription = deleteLabel)
                                },
                            )
                        },
                    )
                    customItem(
                        buttonGroupContent = {
                            Button(
                                onClick = onEdit,
                                modifier = with(buttonGroupScope) {
                                    Modifier.weight(1f)
                                },
                            ) {
                                Text(editLabel)
                            }
                        },
                        menuContent = { menuState ->
                            DropdownMenuItem(
                                text = { Text(editLabel) },
                                onClick = {
                                    onEdit()
                                    menuState.dismiss()
                                },
                                leadingIcon = {
                                    Icon(Icons.Filled.Edit, contentDescription = null)
                                },
                            )
                        },
                    )
                }
            }
        }
    }
}

private fun PlaceReminderAttachment.toMediaPreviewItem(): MediaPreviewItem =
    MediaPreviewItem(
        id = id,
        localPath = localPath,
        displayName = displayName,
        type = when (type) {
            PlaceReminderAttachmentType.IMAGE -> MediaPreviewType.IMAGE
            PlaceReminderAttachmentType.VIDEO -> MediaPreviewType.VIDEO
        },
        width = width,
        height = height,
        durationMillis = durationMillis,
    )
