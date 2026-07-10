package com.github.jimmy90109.geoalarm.ui.screens.place_reminders

import android.Manifest
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.jimmy90109.geoalarm.R
import com.github.jimmy90109.geoalarm.ui.components.MediaPreviewOverlay
import com.github.jimmy90109.geoalarm.ui.components.MediaPreviewPreloader
import com.github.jimmy90109.geoalarm.ui.components.MediaPreviewSelection
import com.github.jimmy90109.geoalarm.ui.components.NotificationPermissionDialog
import com.github.jimmy90109.geoalarm.ui.screens.AlarmEditLandscapeLayout
import com.github.jimmy90109.geoalarm.ui.screens.AlarmEditPortraitLayout
import com.github.jimmy90109.geoalarm.ui.screens.place_reminders.components.PlaceReminderEditBottomBar
import com.github.jimmy90109.geoalarm.ui.screens.place_reminders.components.PlaceReminderEditContent
import com.github.jimmy90109.geoalarm.ui.screens.place_reminders.components.PlaceReminderEditLandscapeControls
import com.github.jimmy90109.geoalarm.ui.screens.place_reminders.components.PlaceReminderEditTopBar
import com.github.jimmy90109.geoalarm.ui.screens.place_reminders.components.canProceedFromPlaceReminderPage
import com.github.jimmy90109.geoalarm.ui.screens.place_reminders.components.toMediaPreviewItem
import com.github.jimmy90109.geoalarm.ui.viewmodel.AlarmEditControlMode
import com.github.jimmy90109.geoalarm.ui.viewmodel.AlarmEditStep
import com.github.jimmy90109.geoalarm.ui.viewmodel.PlaceReminderEditAction
import com.github.jimmy90109.geoalarm.ui.viewmodel.PlaceReminderEditEffect
import com.github.jimmy90109.geoalarm.ui.viewmodel.PlaceReminderEditViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.launch

private val DefaultPlaceReminderMapPosition = LatLng(25.034, 121.564)
private const val PlaceReminderEditPageCount = 3
private const val PlaceReminderPageAnimationDurationMillis = 460
private val PlaceReminderPageEasing = CubicBezierEasing(0.2f, 0f, 0f, 1f)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaceReminderEditScreen(
    viewModel: PlaceReminderEditViewModel,
    reminderId: String?,
    onSelectPlace: () -> Unit,
    onBack: (String?) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val remainingAttachmentSlots = uiState.remainingAttachmentSlots
    val context = LocalContext.current
    var showNotificationPermissionDialog by remember { mutableStateOf(false) }
    val singleAttachmentPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let {
            viewModel.onAction(PlaceReminderEditAction.AttachmentsSelected(listOf(it)))
        }
    }
    val attachmentPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(
            maxItems = remainingAttachmentSlots.coerceAtLeast(2),
        )
    ) { uris ->
        viewModel.onAction(PlaceReminderEditAction.AttachmentsSelected(uris))
    }
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    var selectedPreview by remember { mutableStateOf<MediaPreviewSelection?>(null) }
    var activePreviewItemId by remember { mutableStateOf<String?>(null) }
    val previewSourceBounds = remember { mutableStateMapOf<String, Rect>() }
    val previewItems = remember(uiState.attachments) {
        uiState.attachments
            .sortedBy { it.sortOrder }
            .map { it.toMediaPreviewItem() }
    }
    LaunchedEffect(reminderId) {
        viewModel.onAction(PlaceReminderEditAction.Load(reminderId))
    }
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is PlaceReminderEditEffect.NavigateBack -> onBack(effect.reminderId)
            }
        }
    }

    val isInitialLoading = reminderId != null && uiState.isLoading
    if (isInitialLoading) {
        PlaceReminderInitialLoadPlaceholder(modifier = Modifier.fillMaxSize())
        return
    }

    val formPagerState = rememberPagerState(pageCount = { PlaceReminderEditPageCount })
    val formScope = rememberCoroutineScope()
    fun handleFormBack() {
        if (formPagerState.currentPage > 0) {
            formScope.launch {
                formPagerState.animateToPlaceReminderPage(formPagerState.currentPage - 1)
            }
        } else {
            onBack(null)
        }
    }

    BackHandler(enabled = formPagerState.currentPage > 0) {
        handleFormBack()
    }
    fun previewNotification() {
        if (!context.hasNotificationPermission()) {
            showNotificationPermissionDialog = true
            return
        }
        viewModel.onAction(PlaceReminderEditAction.PreviewNotificationClicked)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        MediaPreviewPreloader(items = previewItems)
        PlaceReminderEditContent(
            state = uiState,
            onAction = viewModel::onAction,
            onSelectPlace = onSelectPlace,
            onPickAttachments = {
                when (remainingAttachmentSlots) {
                    0 -> Unit
                    1 -> singleAttachmentPicker.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                    )
                    else -> attachmentPicker.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                    )
                }
            },
            hiddenAttachmentId = activePreviewItemId ?: selectedPreview?.item?.id,
            onAttachmentBoundsChanged = { id, bounds ->
                previewSourceBounds[id] = bounds
            },
            onAttachmentClick = { selection ->
                selectedPreview = selection
                activePreviewItemId = selection.item.id
            },
            pagerState = formPagerState,
            isLandscape = isLandscape,
            modifier = Modifier.fillMaxSize(),
        )
        val onNextPage = {
            if (canProceedFromPlaceReminderPage(formPagerState.currentPage, uiState)) {
                formScope.launch {
                    formPagerState.animateToPlaceReminderPage(formPagerState.currentPage + 1)
                }
            }
        }
        if (isLandscape) {
            PlaceReminderEditLandscapeControls(
                pagerState = formPagerState,
                state = uiState,
                onBack = ::handleFormBack,
                onNext = onNextPage,
                onPreviewNotification = ::previewNotification,
                onSave = { viewModel.onAction(PlaceReminderEditAction.SaveClicked) },
                modifier = Modifier.align(Alignment.CenterEnd),
            )
        } else {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth(),
            ) {
                PlaceReminderEditTopBar(
                    isEditMode = uiState.isEditMode,
                    isLandscape = false,
                    onBack = ::handleFormBack,
                )
            }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(),
            ) {
                PlaceReminderEditBottomBar(
                    pagerState = formPagerState,
                    state = uiState,
                    onNext = onNextPage,
                    onPreviewNotification = ::previewNotification,
                    onSave = { viewModel.onAction(PlaceReminderEditAction.SaveClicked) },
                )
            }
        }
        selectedPreview?.let { selection ->
            MediaPreviewOverlay(
                selection = selection,
                items = previewItems.ifEmpty { listOf(selection.item) },
                sourceBoundsById = previewSourceBounds.toMap(),
                onActiveItemChanged = { item ->
                    activePreviewItemId = item.id
                },
                onDismiss = {
                    selectedPreview = null
                    activePreviewItemId = null
                },
            )
        }
        if (showNotificationPermissionDialog) {
            NotificationPermissionDialog(
                context = context,
                onDismiss = { showNotificationPermissionDialog = false },
            )
        }
    }
}

@Composable
private fun PlaceReminderInitialLoadPlaceholder(modifier: Modifier = Modifier) {
    Box(modifier = modifier)
}

private suspend fun PagerState.animateToPlaceReminderPage(page: Int) {
    animateScrollToPage(
        page = page.coerceIn(0, pageCount - 1),
        animationSpec = tween(
            durationMillis = PlaceReminderPageAnimationDurationMillis,
            easing = PlaceReminderPageEasing,
        ),
    )
}

private fun android.content.Context.hasNotificationPermission(): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED

@Composable
fun PlaceReminderPlacePickerScreen(
    viewModel: PlaceReminderEditViewModel,
    reminderId: String?,
    onPlaceSelected: () -> Unit,
    onCancel: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val locationUiState = uiState.alarmEditUiState
    LaunchedEffect(reminderId) {
        if (reminderId != null) {
            viewModel.onAction(PlaceReminderEditAction.Load(reminderId))
        }
    }

    val isInitialLoading = reminderId != null && uiState.isLoading
    if (isInitialLoading) {
        PlaceReminderInitialLoadPlaceholder(modifier = Modifier.fillMaxSize())
        return
    }

    val cameraPositionState = rememberCameraPositionState {
        val initialPosition = uiState.selectedPosition
            ?: uiState.currentLocation
            ?: DefaultPlaceReminderMapPosition
        position = CameraPosition.fromLatLngZoom(
            initialPosition,
            if (uiState.selectedPosition != null || uiState.currentLocation != null) 15f else 13f,
        )
    }

    LaunchedEffect(Unit) {
        if (!uiState.isSelectingPlace) {
            viewModel.onAction(PlaceReminderEditAction.StartPlaceSelection)
        }
    }
    LaunchedEffect(
        uiState.isSelectingPlace,
        uiState.currentLocation,
        uiState.selectedPosition,
        uiState.hasUserInteractedWithMap,
    ) {
        if (!uiState.isSelectingPlace) return@LaunchedEffect
        val currentLocation = uiState.currentLocation
        if (
            reminderId == null &&
            currentLocation != null &&
            uiState.selectedPosition == null &&
            !uiState.hasUserInteractedWithMap
        ) {
            cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(currentLocation, 15f))
        }
    }
    LaunchedEffect(uiState.isSelectingPlace, uiState.selectedPosition) {
        if (!uiState.isSelectingPlace) return@LaunchedEffect
        uiState.selectedPosition?.let { latLng ->
            cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
        }
    }
    LaunchedEffect(uiState.isSelectingPlace, locationUiState.currentCandidate?.location) {
        if (!uiState.isSelectingPlace) return@LaunchedEffect
        locationUiState.currentCandidate?.location?.let { latLng ->
            cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
        }
    }

    fun cancelAndPop() {
        viewModel.onAction(PlaceReminderEditAction.PlaceSelectionCancelled)
        onCancel()
    }

    fun handleLocationBack() {
        when {
            uiState.controlMode == AlarmEditControlMode.Candidates && uiState.placeCandidates.isNotEmpty() ->
                viewModel.onAction(PlaceReminderEditAction.CandidateSelectionCancelled)
            uiState.controlMode != AlarmEditControlMode.Radius ->
                viewModel.onAction(PlaceReminderEditAction.CancelInAppSearch)
            else -> cancelAndPop()
        }
    }

    val interceptBack = uiState.step == AlarmEditStep.DetailsForm ||
        uiState.controlMode != AlarmEditControlMode.Radius ||
        uiState.placeCandidates.isNotEmpty()
    BackHandler(enabled = interceptBack) {
        if (uiState.step == AlarmEditStep.DetailsForm) {
            viewModel.onAction(PlaceReminderEditAction.BackToMapClicked)
        } else {
            handleLocationBack()
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            if (viewModel.uiState.value.isSelectingPlace) {
                viewModel.onAction(PlaceReminderEditAction.PlaceSelectionCancelled)
            }
        }
    }

    val savePlace = {
        if (uiState.selectedPosition != null && uiState.placeName.isNotBlank()) {
            viewModel.onAction(PlaceReminderEditAction.PlaceDetailsConfirmed)
            onPlaceSelected()
        }
    }

    if (isLandscape) {
        AlarmEditLandscapeLayout(
            uiState = locationUiState,
            cameraPositionState = cameraPositionState,
            isDetailsStep = uiState.step == AlarmEditStep.DetailsForm,
            screenTitle = stringResource(R.string.place_reminder_place),
            nameLabel = stringResource(R.string.place_reminder_place_name),
            namePlaceholder = stringResource(R.string.place_reminder_place_name_placeholder),
            iconPickerTitle = stringResource(R.string.place_reminder_select_place_icon),
            radiusRange = 100f..1000f,
            radiusSteps = 8,
            disabledNextLabel = stringResource(R.string.tap_map_to_select_place),
            onBack = ::handleLocationBack,
            onSearch = {
                viewModel.onAction(PlaceReminderEditAction.StartInAppSearch(cameraPositionState.position.target))
            },
            onSearchQueryChange = {
                viewModel.onAction(
                    PlaceReminderEditAction.InAppSearchQueryChanged(
                        it
                    )
                )
            },
            onSearchSubmit = { viewModel.onAction(PlaceReminderEditAction.SubmitInAppSearch) },
            onSearchCancel = { viewModel.onAction(PlaceReminderEditAction.CancelInAppSearch) },
            onSuggestionSelected = {
                viewModel.onAction(
                    PlaceReminderEditAction.PlaceSuggestionSelected(
                        it
                    )
                )
            },
            onMapClick = { viewModel.onAction(PlaceReminderEditAction.MapPositionSelected(it)) },
            onMapInteracted = { viewModel.onAction(PlaceReminderEditAction.MapInteracted) },
            onRadiusChange = { viewModel.onAction(PlaceReminderEditAction.RadiusChanged(it.toInt())) },
            onNext = { viewModel.onAction(PlaceReminderEditAction.NextClicked) },
            onDelete = {},
            onNameChange = { viewModel.onAction(PlaceReminderEditAction.PlaceNameChanged(it)) },
            onIconSelected = { viewModel.onAction(PlaceReminderEditAction.IconSelected(it)) },
            onBackToMap = { viewModel.onAction(PlaceReminderEditAction.BackToMapClicked) },
            onSave = savePlace,
            onCandidateChanged = { viewModel.onAction(PlaceReminderEditAction.CandidateChanged(it)) },
            onCandidateConfirmed = { viewModel.onAction(PlaceReminderEditAction.CandidateConfirmed) },
            onCandidateCancelled = { viewModel.onAction(PlaceReminderEditAction.CandidateSelectionCancelled) },
            hideMapForInitialLocation = false,
        )
    } else {
        AlarmEditPortraitLayout(
            uiState = locationUiState,
            cameraPositionState = cameraPositionState,
            isDetailsStep = uiState.step == AlarmEditStep.DetailsForm,
            screenTitle = stringResource(R.string.place_reminder_place),
            nameLabel = stringResource(R.string.place_reminder_place_name),
            namePlaceholder = stringResource(R.string.place_reminder_place_name_placeholder),
            iconPickerTitle = stringResource(R.string.place_reminder_select_place_icon),
            radiusRange = 100f..1000f,
            radiusSteps = 8,
            disabledNextLabel = stringResource(R.string.tap_map_to_select_place),
            onBack = ::handleLocationBack,
            onSearch = {
                viewModel.onAction(PlaceReminderEditAction.StartInAppSearch(cameraPositionState.position.target))
            },
            onSearchQueryChange = {
                viewModel.onAction(
                    PlaceReminderEditAction.InAppSearchQueryChanged(
                        it
                    )
                )
            },
            onSearchSubmit = { viewModel.onAction(PlaceReminderEditAction.SubmitInAppSearch) },
            onSearchCancel = { viewModel.onAction(PlaceReminderEditAction.CancelInAppSearch) },
            onSuggestionSelected = {
                viewModel.onAction(
                    PlaceReminderEditAction.PlaceSuggestionSelected(
                        it
                    )
                )
            },
            onMapClick = { viewModel.onAction(PlaceReminderEditAction.MapPositionSelected(it)) },
            onMapInteracted = { viewModel.onAction(PlaceReminderEditAction.MapInteracted) },
            onRadiusChange = { viewModel.onAction(PlaceReminderEditAction.RadiusChanged(it.toInt())) },
            onNext = { viewModel.onAction(PlaceReminderEditAction.NextClicked) },
            onDelete = {},
            onNameChange = { viewModel.onAction(PlaceReminderEditAction.PlaceNameChanged(it)) },
            onIconSelected = { viewModel.onAction(PlaceReminderEditAction.IconSelected(it)) },
            onBackToMap = { viewModel.onAction(PlaceReminderEditAction.BackToMapClicked) },
            onSave = savePlace,
            onCandidateChanged = { viewModel.onAction(PlaceReminderEditAction.CandidateChanged(it)) },
            onCandidateConfirmed = { viewModel.onAction(PlaceReminderEditAction.CandidateConfirmed) },
            onCandidateCancelled = { viewModel.onAction(PlaceReminderEditAction.CandidateSelectionCancelled) },
            hideMapForInitialLocation = false,
        )
    }
}
