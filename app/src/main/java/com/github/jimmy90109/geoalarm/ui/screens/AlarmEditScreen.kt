package com.github.jimmy90109.geoalarm.ui.screens

import android.Manifest
import android.content.res.Configuration
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.carousel.HorizontalCenteredHeroCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.jimmy90109.geoalarm.R
import com.github.jimmy90109.geoalarm.data.places.PlaceCandidate
import com.github.jimmy90109.geoalarm.share.SharedPlaceSource
import com.github.jimmy90109.geoalarm.ui.components.AlarmIconBadge
import com.github.jimmy90109.geoalarm.ui.components.AlarmIconOptions
import com.github.jimmy90109.geoalarm.ui.components.DeleteAlarmDialog
import com.github.jimmy90109.geoalarm.ui.components.DeleteErrorDialog
import com.github.jimmy90109.geoalarm.ui.viewmodel.AlarmEditAction
import com.github.jimmy90109.geoalarm.ui.viewmodel.AlarmEditControlMode
import com.github.jimmy90109.geoalarm.ui.viewmodel.AlarmEditEffect
import com.github.jimmy90109.geoalarm.ui.viewmodel.AlarmEditStep
import com.github.jimmy90109.geoalarm.ui.viewmodel.AlarmEditUiState
import com.github.jimmy90109.geoalarm.ui.viewmodel.AlarmEditViewModel
import com.github.jimmy90109.geoalarm.ui.theme.GeoAlarmTheme
import com.github.jimmy90109.geoalarm.utils.DistanceFormatter
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

private val DefaultMapPosition = LatLng(25.034, 121.564)
private const val INITIAL_LOCATION_FALLBACK_DELAY_MS = 1200L

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AlarmEditScreen(
    viewModel: AlarmEditViewModel,
    alarmId: String? = null,
    sharedPlaceQuery: String? = null,
    sharedPlaceSource: SharedPlaceSource? = null,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val haptic = LocalHapticFeedback.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val isDetailsStep = uiState.step == AlarmEditStep.DetailsForm
    var canShowInitialLocationFallback by remember(alarmId) { mutableStateOf(alarmId != null) }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            uiState.currentLocation ?: DefaultMapPosition,
            if (uiState.currentLocation != null) 15f else 13f
        )
    }

    LaunchedEffect(alarmId) {
        viewModel.onAction(AlarmEditAction.LoadAlarm(alarmId))
        delay(1000)
        viewModel.onAction(AlarmEditAction.MapLoaded)
    }

    LaunchedEffect(sharedPlaceQuery, sharedPlaceSource) {
        sharedPlaceQuery?.let {
            viewModel.onAction(
                AlarmEditAction.SearchSharedPlace(
                    query = it,
                    source = sharedPlaceSource ?: SharedPlaceSource.GoogleMapsPlace
                )
            )
        }
    }

    LaunchedEffect(uiState.showSharedPlaceSearchError) {
        if (uiState.showSharedPlaceSearchError) {
            Toast.makeText(context, R.string.shared_place_search_failed, Toast.LENGTH_LONG).show()
            viewModel.onAction(AlarmEditAction.SharedPlaceSearchErrorShown)
        }
    }

    LaunchedEffect(alarmId, uiState.currentLocation, uiState.selectedPosition) {
        if (alarmId == null && uiState.currentLocation == null && uiState.selectedPosition == null) {
            canShowInitialLocationFallback = false
            delay(INITIAL_LOCATION_FALLBACK_DELAY_MS)
            canShowInitialLocationFallback = true
        } else {
            canShowInitialLocationFallback = true
        }
    }

    LaunchedEffect(
        alarmId,
        uiState.currentLocation,
        uiState.selectedPosition,
        uiState.hasUserInteractedWithMap
    ) {
        val currentLocation = uiState.currentLocation
        if (
            alarmId == null &&
            currentLocation != null &&
            uiState.selectedPosition == null &&
            !uiState.hasUserInteractedWithMap
        ) {
            cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(currentLocation, 15f))
        }
    }

    LaunchedEffect(uiState.selectedPosition) {
        uiState.selectedPosition?.let { latLng ->
            cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
        }
    }

    LaunchedEffect(uiState.currentCandidate?.location) {
        uiState.currentCandidate?.location?.let { latLng ->
            cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is AlarmEditEffect.NavigateBack -> onNavigateBack()
            }
        }
    }

    BackHandler(enabled = isDetailsStep) {
        viewModel.onAction(AlarmEditAction.BackToMapClicked)
    }

    val isSearchFlowActive = uiState.controlMode != AlarmEditControlMode.Radius
    fun handleMapStepBack() {
        when {
            uiState.isSelectingCandidate -> viewModel.onAction(AlarmEditAction.CandidateSelectionCancelled)
            uiState.isInAppSearchActive -> viewModel.onAction(AlarmEditAction.CancelInAppSearch)
            else -> onNavigateBack()
        }
    }

    BackHandler(enabled = !isDetailsStep && isSearchFlowActive) {
        handleMapStepBack()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (isLandscape) {
            AlarmEditLandscapeLayout(
                uiState = uiState,
                cameraPositionState = cameraPositionState,
                isDetailsStep = isDetailsStep,
                screenTitle = if (uiState.existingAlarm != null) {
                    stringResource(R.string.edit_alarm)
                } else {
                    stringResource(R.string.add_alarm)
                },
                nameLabel = stringResource(R.string.alarm_name),
                namePlaceholder = stringResource(R.string.enter_alarm_name),
                iconPickerTitle = stringResource(R.string.select_alarm_icon),
                radiusRange = 500f..5000f,
                radiusSteps = 45,
                disabledNextLabel = stringResource(R.string.tap_map_to_select_place),
                onBack = ::handleMapStepBack,
                onSearch = {
                    viewModel.onAction(AlarmEditAction.StartInAppSearch(cameraPositionState.position.target))
                },
                onSearchQueryChange = { viewModel.onAction(AlarmEditAction.InAppSearchQueryChanged(it)) },
                onSearchSubmit = { viewModel.onAction(AlarmEditAction.SubmitInAppSearch) },
                onSearchCancel = { viewModel.onAction(AlarmEditAction.CancelInAppSearch) },
                onSuggestionSelected = { viewModel.onAction(AlarmEditAction.PlaceSuggestionSelected(it)) },
                onMapClick = { viewModel.onAction(AlarmEditAction.PositionSelected(it)) },
                onMapInteracted = { viewModel.onAction(AlarmEditAction.MapInteracted) },
                onRadiusChange = { viewModel.onAction(AlarmEditAction.RadiusChanged(it)) },
                onNext = { viewModel.onAction(AlarmEditAction.NextClicked) },
                onDelete = { viewModel.onAction(AlarmEditAction.DeleteRequested) },
                onNameChange = { viewModel.onAction(AlarmEditAction.NameChanged(it)) },
                onIconSelected = { viewModel.onAction(AlarmEditAction.IconSelected(it)) },
                onBackToMap = { viewModel.onAction(AlarmEditAction.BackToMapClicked) },
                onSave = { viewModel.onAction(AlarmEditAction.SaveClicked) },
                onCandidateChanged = { viewModel.onAction(AlarmEditAction.CandidateChanged(it)) },
                onCandidateConfirmed = { viewModel.onAction(AlarmEditAction.CandidateConfirmed) },
                onCandidateCancelled = { viewModel.onAction(AlarmEditAction.CandidateSelectionCancelled) },
                hideMapForInitialLocation = !canShowInitialLocationFallback &&
                    uiState.currentLocation == null &&
                    uiState.selectedPosition == null
            )
        } else {
            AlarmEditPortraitLayout(
                uiState = uiState,
                cameraPositionState = cameraPositionState,
                isDetailsStep = isDetailsStep,
                screenTitle = if (uiState.existingAlarm != null) {
                    stringResource(R.string.edit_alarm)
                } else {
                    stringResource(R.string.add_alarm)
                },
                nameLabel = stringResource(R.string.alarm_name),
                namePlaceholder = stringResource(R.string.enter_alarm_name),
                iconPickerTitle = stringResource(R.string.select_alarm_icon),
                radiusRange = 500f..5000f,
                radiusSteps = 45,
                disabledNextLabel = stringResource(R.string.tap_map_to_select_place),
                onBack = ::handleMapStepBack,
                onSearch = {
                    viewModel.onAction(AlarmEditAction.StartInAppSearch(cameraPositionState.position.target))
                },
                onSearchQueryChange = { viewModel.onAction(AlarmEditAction.InAppSearchQueryChanged(it)) },
                onSearchSubmit = { viewModel.onAction(AlarmEditAction.SubmitInAppSearch) },
                onSearchCancel = { viewModel.onAction(AlarmEditAction.CancelInAppSearch) },
                onSuggestionSelected = { viewModel.onAction(AlarmEditAction.PlaceSuggestionSelected(it)) },
                onMapClick = { viewModel.onAction(AlarmEditAction.PositionSelected(it)) },
                onMapInteracted = { viewModel.onAction(AlarmEditAction.MapInteracted) },
                onRadiusChange = { viewModel.onAction(AlarmEditAction.RadiusChanged(it)) },
                onNext = { viewModel.onAction(AlarmEditAction.NextClicked) },
                onDelete = { viewModel.onAction(AlarmEditAction.DeleteRequested) },
                onNameChange = { viewModel.onAction(AlarmEditAction.NameChanged(it)) },
                onIconSelected = { viewModel.onAction(AlarmEditAction.IconSelected(it)) },
                onBackToMap = { viewModel.onAction(AlarmEditAction.BackToMapClicked) },
                onSave = { viewModel.onAction(AlarmEditAction.SaveClicked) },
                onCandidateChanged = { viewModel.onAction(AlarmEditAction.CandidateChanged(it)) },
                onCandidateConfirmed = { viewModel.onAction(AlarmEditAction.CandidateConfirmed) },
                onCandidateCancelled = { viewModel.onAction(AlarmEditAction.CandidateSelectionCancelled) },
                hideMapForInitialLocation = !canShowInitialLocationFallback &&
                    uiState.currentLocation == null &&
                    uiState.selectedPosition == null
            )
        }
    }

    AnimatedVisibility(
        visible = uiState.isLoading || uiState.isSearchingSharedPlace,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            LoadingIndicator(modifier = Modifier.size(100.dp))
        }
    }

    if (uiState.showDeleteConfirmDialog) {
        DeleteAlarmDialog(
            onConfirm = { viewModel.onAction(AlarmEditAction.DeleteConfirmed) },
            onDismiss = { viewModel.onAction(AlarmEditAction.DeleteDialogDismissed) }
        )
    }

    if (uiState.showDeleteErrorDialog) {
        DeleteErrorDialog(onDismiss = { viewModel.onAction(AlarmEditAction.DeleteErrorDismissed) })
    }
}

@Composable
fun AlarmEditPortraitLayout(
    uiState: AlarmEditUiState,
    cameraPositionState: CameraPositionState,
    isDetailsStep: Boolean,
    screenTitle: String,
    nameLabel: String,
    namePlaceholder: String,
    iconPickerTitle: String,
    radiusRange: ClosedFloatingPointRange<Float>,
    radiusSteps: Int,
    disabledNextLabel: String,
    onBack: () -> Unit,
    onSearch: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onSearchSubmit: () -> Unit,
    onSearchCancel: () -> Unit,
    onSuggestionSelected: (Int) -> Unit,
    onMapClick: (LatLng) -> Unit,
    onMapInteracted: () -> Unit,
    onRadiusChange: (Float) -> Unit,
    onNext: () -> Unit,
    onDelete: () -> Unit,
    onNameChange: (String) -> Unit,
    onIconSelected: (String) -> Unit,
    onBackToMap: () -> Unit,
    onSave: () -> Unit,
    onCandidateChanged: (Int) -> Unit,
    onCandidateConfirmed: () -> Unit,
    onCandidateCancelled: () -> Unit,
    hideMapForInitialLocation: Boolean
) {
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val sharedDurationMs = 420
        val sharedEasing = FastOutSlowInEasing
        var detailsContentHeightPx by remember { mutableStateOf(0) }
        val fallbackDetailsHeightPx = with(density) { 420.dp.roundToPx() }
        val resolvedDetailsHeightPx = detailsContentHeightPx.takeIf { it > 0 } ?: fallbackDetailsHeightPx
        val firstProgress by animateFloatAsState(
            targetValue = if (isDetailsStep) 1f else 0f,
            animationSpec = tween(durationMillis = sharedDurationMs, easing = sharedEasing),
            label = "PortraitFirstStageProgress"
        )
        val secondProgress by animateFloatAsState(
            targetValue = if (isDetailsStep) 1f else 0f,
            animationSpec = tween(durationMillis = sharedDurationMs, easing = sharedEasing),
            label = "PortraitSecondStageProgress"
        )
        val firstOffsetYPx = (-resolvedDetailsHeightPx * firstProgress).roundToInt()
        val screenHeightPx = with(density) { maxHeight.toPx() }
        val secondFinalOffsetY = screenHeightPx - resolvedDetailsHeightPx
        val secondStartOffsetY = screenHeightPx - (resolvedDetailsHeightPx / 2f)
        val secondOffsetY = secondStartOffsetY + (secondFinalOffsetY - secondStartOffsetY) * secondProgress
        val secondOffsetYPx = secondOffsetY.roundToInt()

        Box(
            modifier = Modifier
                .fillMaxSize()
                .clipToBounds()
        ) {
            AlarmDetailsForm(
                name = uiState.name,
                selectedIconKey = uiState.selectedIconKey,
                saveEnabled = uiState.name.isNotBlank() && uiState.selectedPosition != null,
                isLandscape = false,
                nameLabel = nameLabel,
                namePlaceholder = namePlaceholder,
                iconPickerTitle = iconPickerTitle,
                onNameChange = onNameChange,
                onIconSelected = onIconSelected,
                onBackToMap = {
                    haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                    onBackToMap()
                },
                onSave = {
                    haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                    onSave()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .onSizeChanged { detailsContentHeightPx = it.height }
                    .offset { IntOffset(x = 0, y = secondOffsetYPx) }
            )

            AlarmEditPortraitStepOnePage(
                uiState = uiState,
                cameraPositionState = cameraPositionState,
                screenTitle = screenTitle,
                radiusRange = radiusRange,
                radiusSteps = radiusSteps,
                disabledNextLabel = disabledNextLabel,
                onBack = onBack,
                onSearch = onSearch,
                onSearchQueryChange = onSearchQueryChange,
                onSearchSubmit = onSearchSubmit,
                onSearchCancel = onSearchCancel,
                onSuggestionSelected = onSuggestionSelected,
                onMapClick = onMapClick,
                onMapInteracted = onMapInteracted,
                onRadiusChange = onRadiusChange,
                onNext = {
                    haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                    onNext()
                },
                onDelete = {
                    haptic.performHapticFeedback(HapticFeedbackType.Reject)
                    onDelete()
                },
                onCandidateChanged = onCandidateChanged,
                onCandidateConfirmed = onCandidateConfirmed,
                onCandidateCancelled = onCandidateCancelled,
                dimAlpha = 0.38f * firstProgress,
                onDimmedAreaClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                    onBackToMap()
                },
                modifier = Modifier.offset { IntOffset(x = 0, y = firstOffsetYPx) },
                hideMapForInitialLocation = hideMapForInitialLocation
            )
        }
    }
}

@Composable
fun AlarmEditLandscapeLayout(
    uiState: AlarmEditUiState,
    cameraPositionState: CameraPositionState,
    isDetailsStep: Boolean,
    screenTitle: String,
    nameLabel: String,
    namePlaceholder: String,
    iconPickerTitle: String,
    radiusRange: ClosedFloatingPointRange<Float>,
    radiusSteps: Int,
    disabledNextLabel: String,
    onBack: () -> Unit,
    onSearch: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onSearchSubmit: () -> Unit,
    onSearchCancel: () -> Unit,
    onSuggestionSelected: (Int) -> Unit,
    onMapClick: (LatLng) -> Unit,
    onMapInteracted: () -> Unit,
    onRadiusChange: (Float) -> Unit,
    onNext: () -> Unit,
    onDelete: () -> Unit,
    onNameChange: (String) -> Unit,
    onIconSelected: (String) -> Unit,
    onBackToMap: () -> Unit,
    onSave: () -> Unit,
    onCandidateChanged: (Int) -> Unit,
    onCandidateConfirmed: () -> Unit,
    onCandidateCancelled: () -> Unit,
    hideMapForInitialLocation: Boolean
) {
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val sharedDurationMs = 420
        val sharedEasing = FastOutSlowInEasing
        val panelWidthPx = with(density) {
            minOf(
                360.dp.roundToPx(),
                (maxWidth.toPx() - 120.dp.toPx()).coerceAtLeast(0f).roundToInt()
            )
        }
        val panelWidthDp = with(density) { panelWidthPx.toDp() }
        val firstProgress by animateFloatAsState(
            targetValue = if (isDetailsStep) 1f else 0f,
            animationSpec = tween(durationMillis = sharedDurationMs, easing = sharedEasing),
            label = "LandscapeFirstStageProgress"
        )
        val secondProgress by animateFloatAsState(
            targetValue = if (isDetailsStep) 1f else 0f,
            animationSpec = tween(durationMillis = sharedDurationMs, easing = sharedEasing),
            label = "LandscapeSecondStageProgress"
        )
        val firstOffsetXPx = (-panelWidthPx * firstProgress).roundToInt()
        val screenWidthPx = with(density) { maxWidth.toPx() }
        val secondFinalOffsetX = screenWidthPx - panelWidthPx
        val secondStartOffsetX = screenWidthPx - (panelWidthPx / 2f)
        val secondOffsetX = secondStartOffsetX + (secondFinalOffsetX - secondStartOffsetX) * secondProgress
        val secondOffsetXPx = secondOffsetX.roundToInt()

        Box(
            modifier = Modifier
                .fillMaxSize()
                .clipToBounds()
        ) {
            AlarmDetailsForm(
                name = uiState.name,
                selectedIconKey = uiState.selectedIconKey,
                saveEnabled = uiState.name.isNotBlank() && uiState.selectedPosition != null,
                isLandscape = true,
                nameLabel = nameLabel,
                namePlaceholder = namePlaceholder,
                iconPickerTitle = iconPickerTitle,
                onNameChange = onNameChange,
                onIconSelected = onIconSelected,
                onBackToMap = {
                    haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                    onBackToMap()
                },
                onSave = {
                    haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                    onSave()
                },
                modifier = Modifier
                    .fillMaxHeight()
                    .width(panelWidthDp)
                    .offset { IntOffset(x = secondOffsetXPx, y = 0) }
            )

            AlarmEditLandscapeStepOnePage(
                uiState = uiState,
                cameraPositionState = cameraPositionState,
                screenTitle = screenTitle,
                radiusRange = radiusRange,
                radiusSteps = radiusSteps,
                disabledNextLabel = disabledNextLabel,
                onBack = onBack,
                onSearch = onSearch,
                onSearchQueryChange = onSearchQueryChange,
                onSearchSubmit = onSearchSubmit,
                onSearchCancel = onSearchCancel,
                onSuggestionSelected = onSuggestionSelected,
                onMapClick = onMapClick,
                onMapInteracted = onMapInteracted,
                onRadiusChange = onRadiusChange,
                onNext = {
                    haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                    onNext()
                },
                onDelete = {
                    haptic.performHapticFeedback(HapticFeedbackType.Reject)
                    onDelete()
                },
                onCandidateChanged = onCandidateChanged,
                onCandidateConfirmed = onCandidateConfirmed,
                onCandidateCancelled = onCandidateCancelled,
                dimAlpha = 0.38f * firstProgress,
                onDimmedAreaClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                    onBackToMap()
                },
                modifier = Modifier.offset { IntOffset(x = firstOffsetXPx, y = 0) },
                hideMapForInitialLocation = hideMapForInitialLocation
            )
        }
    }
}

@Composable
private fun AlarmEditPortraitStepOnePage(
    uiState: AlarmEditUiState,
    cameraPositionState: CameraPositionState,
    screenTitle: String,
    radiusRange: ClosedFloatingPointRange<Float>,
    radiusSteps: Int,
    disabledNextLabel: String,
    onBack: () -> Unit,
    onSearch: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onSearchSubmit: () -> Unit,
    onSearchCancel: () -> Unit,
    onSuggestionSelected: (Int) -> Unit,
    onMapClick: (LatLng) -> Unit,
    onMapInteracted: () -> Unit,
    onRadiusChange: (Float) -> Unit,
    onNext: () -> Unit,
    onDelete: () -> Unit,
    onCandidateChanged: (Int) -> Unit,
    onCandidateConfirmed: () -> Unit,
    onCandidateCancelled: () -> Unit,
    dimAlpha: Float,
    onDimmedAreaClick: () -> Unit,
    modifier: Modifier = Modifier,
    hideMapForInitialLocation: Boolean
) {
    val navigationBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val bottomPadding = maxOf(navigationBottom, 24.dp)
    val deviceCorner = rememberSystemDisplayCornerRadiusDp()
    val deviceShape = RoundedCornerShape(deviceCorner)
    Box(modifier = modifier.fillMaxSize()) {
        AlarmEditMapContent(
            cameraPositionState = cameraPositionState,
            uiState = uiState,
            onMapClick = { position ->
                if (uiState.controlMode == AlarmEditControlMode.Radius) onMapClick(position)
            },
            onMapInteracted = onMapInteracted,
            hideForInitialLocation = hideMapForInitialLocation,
            contentPadding = PaddingValues(
                bottom = when {
                    uiState.isSelectingCandidate -> 300.dp
                    uiState.controlMode == AlarmEditControlMode.SearchInput -> 24.dp
                    else -> 220.dp
                }
            ),
            modifier = Modifier.fillMaxSize()
        )

        AlarmEditTopControl(
            uiState = uiState,
            title = screenTitle,
            onBack = onBack,
            onSearch = onSearch,
            onSearchQueryChange = onSearchQueryChange,
            onSearchSubmit = onSearchSubmit,
            onSearchCancel = onSearchCancel,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(start = 24.dp, end = 24.dp)
        )

        AlarmEditSuggestionList(
            uiState = uiState,
            onSuggestionSelected = onSuggestionSelected,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(
                    start = 24.dp,
                    end = 24.dp,
                    top = maxOf(
                        WindowInsets.statusBars.asPaddingValues().calculateTopPadding(),
                        24.dp
                    ) + 72.dp
                )
        )

        AnimatedVisibility(
            visible = uiState.controlMode != AlarmEditControlMode.SearchInput,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(start = 24.dp, end = 24.dp, bottom = bottomPadding)
        ) {
            AlarmEditControlSwitcher(
                uiState = uiState,
                radiusRange = radiusRange,
                radiusSteps = radiusSteps,
                disabledNextLabel = disabledNextLabel,
                onRadiusChange = onRadiusChange,
                onNext = onNext,
                onDelete = onDelete,
                onCandidateChanged = onCandidateChanged,
                onCandidateConfirmed = onCandidateConfirmed,
                onCandidateCancelled = onCandidateCancelled,
                elevation = 10.dp
            )
        }

        if (dimAlpha > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(deviceShape)
                    .background(Color.Black.copy(alpha = dimAlpha))
                    .clickable(onClick = onDimmedAreaClick)
            )
        }
    }
}

@Composable
private fun AlarmEditLandscapeStepOnePage(
    uiState: AlarmEditUiState,
    cameraPositionState: CameraPositionState,
    screenTitle: String,
    radiusRange: ClosedFloatingPointRange<Float>,
    radiusSteps: Int,
    disabledNextLabel: String,
    onBack: () -> Unit,
    onSearch: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onSearchSubmit: () -> Unit,
    onSearchCancel: () -> Unit,
    onSuggestionSelected: (Int) -> Unit,
    onMapClick: (LatLng) -> Unit,
    onMapInteracted: () -> Unit,
    onRadiusChange: (Float) -> Unit,
    onNext: () -> Unit,
    onDelete: () -> Unit,
    onCandidateChanged: (Int) -> Unit,
    onCandidateConfirmed: () -> Unit,
    onCandidateCancelled: () -> Unit,
    dimAlpha: Float,
    onDimmedAreaClick: () -> Unit,
    modifier: Modifier = Modifier,
    hideMapForInitialLocation: Boolean
) {
    val deviceCorner = rememberSystemDisplayCornerRadiusDp()
    val deviceShape = RoundedCornerShape(deviceCorner)
    val navInsets = WindowInsets.navigationBars.asPaddingValues()
    val layoutDirection = LocalLayoutDirection.current
    val bottomPadding = maxOf(24.dp, navInsets.calculateBottomPadding())
    val endPadding = maxOf(24.dp, navInsets.calculateEndPadding(layoutDirection))
    Box(modifier = modifier.fillMaxSize()) {
        AlarmEditMapContent(
            cameraPositionState = cameraPositionState,
            uiState = uiState,
            onMapClick = { position ->
                if (uiState.controlMode == AlarmEditControlMode.Radius) onMapClick(position)
            },
            onMapInteracted = onMapInteracted,
            hideForInitialLocation = hideMapForInitialLocation,
            contentPadding = PaddingValues(end = 400.dp),
            modifier = Modifier.fillMaxSize()
        )

        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = endPadding, bottom = bottomPadding, top = 24.dp)
                .windowInsetsPadding(WindowInsets.displayCutout)
                .widthIn(max = 360.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Top
            ) {
                AlarmEditTopControl(
                    uiState = uiState,
                    title = screenTitle,
                    onBack = onBack,
                    onSearch = onSearch,
                    onSearchQueryChange = onSearchQueryChange,
                    onSearchSubmit = onSearchSubmit,
                    onSearchCancel = onSearchCancel
                )

                AlarmEditSuggestionList(
                    uiState = uiState,
                    onSuggestionSelected = onSuggestionSelected,
                    modifier = Modifier.padding(top = 8.dp)
                )

                Spacer(modifier = Modifier.weight(1f))

                AnimatedVisibility(visible = uiState.controlMode != AlarmEditControlMode.SearchInput) {
                    AlarmEditControlSwitcher(
                        uiState = uiState,
                        radiusRange = radiusRange,
                        radiusSteps = radiusSteps,
                        disabledNextLabel = disabledNextLabel,
                        onRadiusChange = onRadiusChange,
                        onNext = onNext,
                        onDelete = onDelete,
                        onCandidateChanged = onCandidateChanged,
                        onCandidateConfirmed = onCandidateConfirmed,
                        onCandidateCancelled = onCandidateCancelled,
                        elevation = 10.dp
                    )
                }
            }
        }

        if (dimAlpha > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(deviceShape)
                    .background(Color.Black.copy(alpha = dimAlpha))
                    .clickable(onClick = onDimmedAreaClick)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun AlarmDetailsForm(
    name: String,
    selectedIconKey: String,
    saveEnabled: Boolean,
    isLandscape: Boolean,
    nameLabel: String,
    namePlaceholder: String,
    iconPickerTitle: String,
    onNameChange: (String) -> Unit,
    onIconSelected: (String) -> Unit,
    onBackToMap: () -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier
) {
    val statusBarPlaceholder = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navInsets = WindowInsets.navigationBars.asPaddingValues()
    val layoutDirection = LocalLayoutDirection.current
    val bottomPadding = maxOf(24.dp, navInsets.calculateBottomPadding())
    val startPadding = maxOf(24.dp, navInsets.calculateStartPadding(layoutDirection))
    val endPadding = maxOf(24.dp, navInsets.calculateEndPadding(layoutDirection))
    BoxWithConstraints(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface)
            .padding(start = startPadding, end = endPadding, bottom = bottomPadding)
    ) {
        val iconAreaMaxHeight = (maxHeight - 220.dp).coerceAtLeast(120.dp)
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = if (isLandscape) 0.dp else 12.dp)
        ) {
            if (isLandscape) {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 72.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Spacer(modifier = Modifier.height(statusBarPlaceholder))
                    }
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        OutlinedTextField(
                            value = name,
                            onValueChange = onNameChange,
                            label = { Text(nameLabel) },
                            placeholder = { Text(namePlaceholder) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Text(
                            text = iconPickerTitle,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    items(AlarmIconOptions, key = { it.key }) { option ->
                        val selected = option.key == selectedIconKey
                        ToggleButton(
                            checked = selected,
                            onCheckedChange = { _ ->
                                onIconSelected(option.key)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shapes = ToggleButtonDefaults.shapes(
                                shape = RoundedCornerShape(14.dp),
                                pressedShape = RoundedCornerShape(18.dp),
                                checkedShape = ToggleButtonDefaults.roundShape
                            ),
                            colors = ToggleButtonDefaults.toggleButtonColors().copy(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                checkedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                checkedContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        ) {
                            AlarmIconBadge(iconKey = option.key)
                        }
                    }
                }
            } else {
                OutlinedTextField(
                    value = name,
                    onValueChange = onNameChange,
                    label = { Text(nameLabel) },
                    placeholder = { Text(namePlaceholder) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = iconPickerTitle,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 72.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 120.dp, max = iconAreaMaxHeight),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(AlarmIconOptions, key = { it.key }) { option ->
                        val selected = option.key == selectedIconKey
                        ToggleButton(
                            checked = selected,
                            onCheckedChange = { _ ->
                                onIconSelected(option.key)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shapes = ToggleButtonDefaults.shapes(
                                shape = RoundedCornerShape(14.dp),
                                pressedShape = RoundedCornerShape(18.dp),
                                checkedShape = ToggleButtonDefaults.roundShape
                            ),
                            colors = ToggleButtonDefaults.toggleButtonColors().copy(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                checkedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                checkedContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        ) {
                            AlarmIconBadge(iconKey = option.key)
                        }
                    }
                }
            }

            val backLabel = stringResource(R.string.back_to_map_step)
            val saveLabel = stringResource(R.string.save)
            val backIcon = if (isLandscape) {
                Icons.AutoMirrored.Filled.ArrowBack
            } else {
                Icons.Default.KeyboardArrowUp
            }
            ButtonGroup(
                overflowIndicator = { menuState ->
                    ButtonGroupDefaults.OverflowIndicator(menuState = menuState)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                val buttonGroupScope = this
                customItem(
                    buttonGroupContent = {
                        FilledIconButton(
                            onClick = onBackToMap,
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        ) {
                            Icon(
                                imageVector = backIcon,
                                contentDescription = backLabel
                            )
                        }
                    },
                    menuContent = { menuState ->
                        DropdownMenuItem(
                            text = { Text(backLabel) },
                            onClick = {
                                onBackToMap()
                                menuState.dismiss()
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = backIcon,
                                    contentDescription = backLabel
                                )
                            }
                        )
                    }
                )
                customItem(
                    buttonGroupContent = {
                        Button(
                            onClick = onSave,
                            enabled = saveEnabled,
                            modifier = with(buttonGroupScope) {
                                Modifier.weight(1f)
                            }
                        ) {
                            Text(saveLabel)
                        }
                    },
                    menuContent = { menuState ->
                        DropdownMenuItem(
                            text = { Text(saveLabel) },
                            onClick = {
                                if (saveEnabled) {
                                    onSave()
                                }
                                menuState.dismiss()
                            },
                            enabled = saveEnabled
                        )
                    }
                )
            }
        }
    }
}

@Composable
fun AlarmEditMapContent(
    cameraPositionState: CameraPositionState,
    uiState: AlarmEditUiState,
    onMapClick: (LatLng) -> Unit,
    onMapInteracted: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    hideForInitialLocation: Boolean = false
) {
    val context = LocalContext.current
    val darkTheme = isSystemInDarkTheme()
    var mapProperties by remember { mutableStateOf(MapProperties()) }
    val hasLocationPermission = remember {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
    }

    LaunchedEffect(darkTheme, hasLocationPermission) {
        mapProperties = if (darkTheme) {
            mapProperties.copy(
                mapStyleOptions = MapStyleOptions.loadRawResourceStyle(
                    context,
                    R.raw.map_style_dark
                ),
                isMyLocationEnabled = hasLocationPermission
            )
        } else {
            mapProperties.copy(
                mapStyleOptions = null,
                isMyLocationEnabled = hasLocationPermission
            )
        }
    }

    val backgroundColor =
        if (darkTheme) Color(0xFF1d2c4d) else MaterialTheme.colorScheme.surfaceVariant
    var isMapLoaded by remember { mutableStateOf(false) }

    val haptic = LocalHapticFeedback.current
    val systemCornerRadius = rememberSystemDisplayCornerRadiusDp()
    val mapClipShape = RoundedCornerShape(systemCornerRadius)
    Box(
        modifier = modifier
            .clip(mapClipShape)
            .background(backgroundColor)
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                    onMapInteracted()
                }
            }
    ) {
        GoogleMap(
            modifier = Modifier.matchParentSize(),
            cameraPositionState = cameraPositionState,
            properties = mapProperties,
            uiSettings = MapUiSettings(
                zoomControlsEnabled = false,
                myLocationButtonEnabled = false
            ),
            contentPadding = contentPadding,
            onMapClick = { position ->
                if (uiState.controlMode == AlarmEditControlMode.Radius) onMapClick(position)
            },
            onMapLoaded = { isMapLoaded = true }
        ) {
            val previewPosition = uiState.currentCandidate?.location ?: uiState.selectedPosition
            previewPosition?.let { pos ->
                Marker(
                    state = MarkerState(position = pos),
                    title = uiState.currentCandidate?.name ?: "Destination"
                )
                if (uiState.controlMode == AlarmEditControlMode.Radius) {
                    Circle(
                        center = pos,
                        radius = uiState.radius.toDouble(),
                        fillColor = Color(0xFF607D8B).copy(alpha = 0.1f),
                        strokeColor = Color(0xFF607D8B).copy(alpha = 0.8f),
                        strokeWidth = 2f
                    )
                }
            }
        }

        InitialMapLocationCover(
            visible = !isMapLoaded || hideForInitialLocation,
            backgroundColor = backgroundColor,
            showLocationProgress = hideForInitialLocation
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun InitialMapLocationCover(
    visible: Boolean,
    backgroundColor: Color,
    showLocationProgress: Boolean,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        exit = fadeOut(),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor),
            contentAlignment = Alignment.Center
        ) {
            if (showLocationProgress) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    LoadingIndicator(modifier = Modifier.size(64.dp))
                    Text(
                        text = stringResource(R.string.getting_current_location),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun rememberSystemDisplayCornerRadiusDp(fallback: androidx.compose.ui.unit.Dp = 28.dp): androidx.compose.ui.unit.Dp {
    val view = LocalView.current
    val density = LocalDensity.current
    return remember(view, density) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return@remember fallback
        }
        val insets = view.rootWindowInsets ?: return@remember fallback
        val radiiPx = listOf(
            android.view.RoundedCorner.POSITION_TOP_LEFT,
            android.view.RoundedCorner.POSITION_TOP_RIGHT,
            android.view.RoundedCorner.POSITION_BOTTOM_LEFT,
            android.view.RoundedCorner.POSITION_BOTTOM_RIGHT
        ).mapNotNull { position ->
            insets.getRoundedCorner(position)?.radius?.takeIf { it > 0 }
        }
        val radiusPx = radiiPx.minOrNull() ?: return@remember fallback
        with(density) { radiusPx.toDp() }
    }
}

@Composable
private fun AlarmEditSuggestionList(
    uiState: AlarmEditUiState,
    onSuggestionSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = uiState.controlMode == AlarmEditControlMode.SearchInput &&
            (uiState.isLoadingSuggestions || uiState.placeSuggestions.isNotEmpty()),
        modifier = modifier
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .heightIn(max = 320.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                if (uiState.isLoadingSuggestions) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
                uiState.placeSuggestions.forEachIndexed { index, suggestion ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSuggestionSelected(index) }
                            .padding(horizontal = 20.dp, vertical = 12.dp)
                    ) {
                        Text(
                            text = suggestion.primaryText,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        if (suggestion.secondaryText.isNotBlank()) {
                            Text(
                                text = suggestion.secondaryText,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    if (index != uiState.placeSuggestions.lastIndex) {
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun AlarmEditTopControl(
    uiState: AlarmEditUiState,
    title: String,
    onBack: () -> Unit,
    onSearch: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onSearchSubmit: () -> Unit,
    onSearchCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val showSearchField = uiState.controlMode == AlarmEditControlMode.SearchInput ||
        uiState.controlMode == AlarmEditControlMode.SearchLoading
    val searchEditable = uiState.controlMode == AlarmEditControlMode.SearchInput
    val haptic = LocalHapticFeedback.current
    val keyboardController = LocalSoftwareKeyboardController.current

    com.github.jimmy90109.geoalarm.ui.components.TopAppBar(
        modifier = modifier,
        title = {
            Text(title)
        },
        showAlternateTitle = showSearchField,
        alternateTitle = {
            AlarmEditTopSearchField(
                query = uiState.inAppSearchQuery,
                showError = uiState.inAppSearchError,
                readOnly = !searchEditable,
                onQueryChange = onSearchQueryChange,
                onSubmit = onSearchSubmit
            )
        },
        navigationIcon = {
            IconButton(
                onClick = {
                    keyboardController?.hide()
                    haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                    if (showSearchField) onSearchCancel() else onBack()
                }
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.cancel)
                )
            }
        },
        actions = {
            when {
                showSearchField -> IconButton(
                    onClick = {
                        keyboardController?.hide()
                        haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                        onSearchSubmit()
                    },
                    enabled = searchEditable && uiState.inAppSearchQuery.isNotBlank()
                ) {
                    Icon(
                        Icons.Filled.Search,
                        contentDescription = stringResource(R.string.search_location)
                    )
                }
                uiState.controlMode == AlarmEditControlMode.Radius -> IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                        onSearch()
                    }
                ) {
                    Icon(
                        Icons.Filled.Search,
                        contentDescription = stringResource(R.string.search_location)
                    )
                }
            }
        }
    )
}

@Composable
private fun AlarmEditTopSearchField(
    query: String,
    showError: Boolean,
    readOnly: Boolean,
    onQueryChange: (String) -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(readOnly, showError) {
        if (readOnly) {
            focusManager.clearFocus(force = true)
            keyboardController?.hide()
        } else {
            focusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    TextField(
        value = query,
        onValueChange = onQueryChange,
        readOnly = readOnly,
        singleLine = true,
        isError = showError,
        placeholder = { Text(stringResource(R.string.search_location)) },
        supportingText = if (showError) {
            { Text(stringResource(R.string.location_search_failed_inline)) }
        } else {
            null
        },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(
            onSearch = {
                if (!readOnly && query.isNotBlank()) {
                    keyboardController?.hide()
                    onSubmit()
                }
            }
        ),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent,
            errorContainerColor = Color.Transparent
        ),
        modifier = modifier
            .fillMaxWidth()
            .focusRequester(focusRequester)
    )
}

@Composable
private fun AlarmEditControlSwitcher(
    uiState: AlarmEditUiState,
    radiusRange: ClosedFloatingPointRange<Float>,
    radiusSteps: Int,
    disabledNextLabel: String,
    onRadiusChange: (Float) -> Unit,
    onNext: () -> Unit,
    onDelete: () -> Unit,
    onCandidateChanged: (Int) -> Unit,
    onCandidateConfirmed: () -> Unit,
    onCandidateCancelled: () -> Unit,
    elevation: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier
) {
    AnimatedContent(
        targetState = uiState.controlMode,
        transitionSpec = {
            (fadeIn(tween(220)) togetherWith fadeOut(tween(160)))
                .using(SizeTransform(clip = false))
        },
        label = "AlarmEditControlTransition",
        modifier = modifier.fillMaxWidth()
    ) { mode ->
        when (mode) {
            AlarmEditControlMode.Radius -> AlarmEditRadiusControl(
                radius = uiState.radius,
                onRadiusChange = onRadiusChange,
                onPrimaryClick = onNext,
                primaryEnabled = uiState.selectedPosition != null,
                primaryButtonLabel = if (uiState.selectedPosition == null) {
                    disabledNextLabel
                } else {
                    stringResource(R.string.next_step)
                },
                isEditMode = uiState.existingAlarm != null,
                onDeleteClick = onDelete,
                elevation = elevation,
                valueRange = radiusRange,
                steps = radiusSteps,
            )

            AlarmEditControlMode.SearchInput -> Unit

            AlarmEditControlMode.SearchLoading -> AlarmEditSearchLoadingControl(elevation = elevation)

            AlarmEditControlMode.Candidates -> SharedPlaceCandidateControl(
                candidates = uiState.placeCandidates,
                currentIndex = uiState.currentCandidateIndex,
                onCandidateChanged = onCandidateChanged,
                onConfirm = onCandidateConfirmed,
                onCancel = onCandidateCancelled,
                elevation = elevation
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun AlarmEditSearchLoadingControl(
    elevation: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(44.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            LoadingIndicator(modifier = Modifier.size(64.dp))
            Text(
                text = stringResource(R.string.searching_location),
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AlarmEditRadiusControl(
    modifier: Modifier = Modifier,
    radius: Float,
    onRadiusChange: (Float) -> Unit,
    onPrimaryClick: () -> Unit,
    primaryButtonLabel: String,
    primaryEnabled: Boolean,
    isEditMode: Boolean = false,
    onDeleteClick: () -> Unit = {},
    elevation: androidx.compose.ui.unit.Dp = 0.dp,
    valueRange: ClosedFloatingPointRange<Float> = 500f..5000f,
    steps: Int = 45,
) {
    val view = LocalView.current
    val distanceLocale = LocalConfiguration.current.locales[0]
    Card(
        shape = RoundedCornerShape(44.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(
                    R.string.radius_label,
                    DistanceFormatter.formatMeters(radius.toInt(), distanceLocale),
                ),
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.height(8.dp))
            Slider(
                value = radius,
                onValueChange = {
                    if (it != radius) {
                        view.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK)
                        onRadiusChange(it)
                    }
                },
                valueRange = valueRange,
                steps = steps
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (isEditMode) {
                val deleteLabel = stringResource(R.string.delete)
                ButtonGroup(
                    overflowIndicator = { menuState ->
                        ButtonGroupDefaults.OverflowIndicator(menuState = menuState)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val buttonGroupScope = this
                    customItem(
                        buttonGroupContent = {
                            FilledIconButton(
                                onClick = onDeleteClick,
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer,
                                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = deleteLabel
                                )
                            }
                        },
                        menuContent = { menuState ->
                            DropdownMenuItem(
                                text = { Text(deleteLabel) },
                                onClick = {
                                    onDeleteClick()
                                    menuState.dismiss()
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = deleteLabel
                                    )
                                }
                            )
                        }
                    )
                    customItem(
                        buttonGroupContent = {
                            Button(
                                onClick = onPrimaryClick,
                                enabled = primaryEnabled,
                                modifier = with(buttonGroupScope) {
                                    Modifier.weight(1f)
                                }
                            ) {
                                Text(primaryButtonLabel)
                            }
                        },
                        menuContent = { menuState ->
                            DropdownMenuItem(
                                text = { Text(primaryButtonLabel) },
                                onClick = {
                                    if (primaryEnabled) {
                                        onPrimaryClick()
                                    }
                                    menuState.dismiss()
                                },
                                enabled = primaryEnabled
                            )
                        }
                    )
                }
            } else {
                Button(
                    onClick = onPrimaryClick,
                    enabled = primaryEnabled,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(primaryButtonLabel)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SharedPlaceCandidateControl(
    candidates: List<PlaceCandidate>,
    currentIndex: Int,
    onCandidateChanged: (Int) -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    elevation: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier
) {
    val carouselState = rememberCarouselState(
        initialItem = currentIndex.coerceIn(0, (candidates.size - 1).coerceAtLeast(0)),
        itemCount = { candidates.size }
    )
    LaunchedEffect(carouselState.currentItem) {
        onCandidateChanged(carouselState.currentItem)
    }

    Card(
        shape = RoundedCornerShape(44.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            HorizontalCenteredHeroCarousel(
                state = carouselState,
                itemSpacing = 12.dp,
                contentPadding = PaddingValues( horizontal = 16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            ) { index ->
                val candidate = candidates[index]
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxSize()
                        .maskClip(RoundedCornerShape(28.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        candidate.photo?.let { bitmap ->
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = candidate.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(0.7f)
                                .align(Alignment.BottomCenter)
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.82f))
                                    )
                                )
                        )
                        if (candidate.photoAttribution.isNotBlank()) {
                            Text(
                                text = candidate.photoAttribution,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(16.dp)
                            )
                        }
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = candidate.name,
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                            if (candidate.address.isNotBlank() && candidate.address != candidate.name) {
                                Text(
                                    text = candidate.address,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.85f),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.cancel))
                }
                Button(
                    onClick = onConfirm,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.confirm))
                }
            }
        }
    }
}

@Preview(
    name = "Shared place candidates",
    showBackground = true,
    backgroundColor = 0xFFE5E5E5,
    widthDp = 412
)
@Composable
private fun SharedPlaceCandidateControlPreview() {
    GeoAlarmTheme {
        SharedPlaceCandidateControl(
            candidates = listOf(
                PlaceCandidate(
                    id = "cks-memorial-hall",
                    name = "國立中正紀念堂",
                    address = "100 台北市中正區中山南路 21 號",
                    location = LatLng(25.0346, 121.5219)
                ),
                PlaceCandidate(
                    id = "liberty-square",
                    name = "自由廣場",
                    address = "100 台北市中正區",
                    location = LatLng(25.0361, 121.5198)
                ),
                PlaceCandidate(
                    id = "national-theater",
                    name = "國家戲劇院",
                    address = "100 台北市中正區中山南路 21-1 號",
                    location = LatLng(25.0354, 121.5186)
                )
            ),
            currentIndex = 0,
            onCandidateChanged = {},
            onConfirm = {},
            onCancel = {},
            elevation = 10.dp
        )
    }
}

@Preview(
    name = "Waiting for initial location",
    showBackground = true,
    widthDp = 412,
    heightDp = 915
)
@Composable
private fun InitialMapLocationCoverPreview() {
    GeoAlarmTheme {
        val backgroundColor = MaterialTheme.colorScheme.surfaceVariant
        val navigationBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        val bottomPadding = maxOf(navigationBottom, 24.dp)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor)
        ) {
            InitialMapLocationCover(
                visible = true,
                backgroundColor = backgroundColor,
                showLocationProgress = true,
                modifier = Modifier.fillMaxSize()
            )

            com.github.jimmy90109.geoalarm.ui.components.TopAppBar(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(start = 24.dp, end = 24.dp),
                title = { Text(stringResource(R.string.add_alarm)) },
                navigationIcon = {
                    IconButton(onClick = {}) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cancel)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {}) {
                        Icon(
                            Icons.Filled.Search,
                            contentDescription = stringResource(R.string.search_location)
                        )
                    }
                }
            )

            AlarmEditRadiusControl(
                radius = 1000f,
                onRadiusChange = {},
                onPrimaryClick = {},
                primaryButtonLabel = stringResource(R.string.next_step),
                primaryEnabled = false,
                elevation = 10.dp,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(start = 24.dp, end = 24.dp, bottom = bottomPadding)
            )
        }
    }
}
