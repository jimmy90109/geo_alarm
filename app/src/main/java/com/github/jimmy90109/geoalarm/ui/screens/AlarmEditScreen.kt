package com.github.jimmy90109.geoalarm.ui.screens

import android.app.Activity
import android.os.Build
import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.jimmy90109.geoalarm.R
import com.github.jimmy90109.geoalarm.ui.components.AlarmIconBadge
import com.github.jimmy90109.geoalarm.ui.components.AlarmIconOptions
import com.github.jimmy90109.geoalarm.ui.components.DeleteAlarmDialog
import com.github.jimmy90109.geoalarm.ui.components.DeleteErrorDialog
import com.github.jimmy90109.geoalarm.ui.viewmodel.AlarmEditAction
import com.github.jimmy90109.geoalarm.ui.viewmodel.AlarmEditEffect
import com.github.jimmy90109.geoalarm.ui.viewmodel.AlarmEditStep
import com.github.jimmy90109.geoalarm.ui.viewmodel.AlarmEditUiState
import com.github.jimmy90109.geoalarm.ui.viewmodel.AlarmEditViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AlarmEditScreen(
    viewModel: AlarmEditViewModel,
    alarmId: String? = null,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val haptic = LocalHapticFeedback.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val isDetailsStep = uiState.step == AlarmEditStep.DetailsForm

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(25.034, 121.564), 13f)
    }

    val autocompleteLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val place = Autocomplete.getPlaceFromIntent(result.data!!)
            place.location?.let { latLng ->
                viewModel.onAction(
                    AlarmEditAction.SearchPositionSelected(
                        latLng,
                        place.displayName ?: place.formattedAddress ?: ""
                    )
                )
                scope.launch {
                    cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
                }
            }
        }
    }

    fun launchAutocomplete() {
        val fields = listOf(
            Place.Field.ID,
            Place.Field.DISPLAY_NAME,
            Place.Field.FORMATTED_ADDRESS,
            Place.Field.LOCATION
        )
        val intent = Autocomplete.IntentBuilder(AutocompleteActivityMode.OVERLAY, fields)
            .build(context)
        autocompleteLauncher.launch(intent)
    }

    LaunchedEffect(alarmId) {
        viewModel.onAction(AlarmEditAction.LoadAlarm(alarmId))
        delay(1000)
        viewModel.onAction(AlarmEditAction.MapLoaded)
    }

    LaunchedEffect(uiState.selectedPosition) {
        uiState.selectedPosition?.let { latLng ->
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

    Box(modifier = Modifier.fillMaxSize()) {
        if (isLandscape) {
            AlarmEditLandscapeLayout(
                uiState = uiState,
                cameraPositionState = cameraPositionState,
                isDetailsStep = isDetailsStep,
                onBack = onNavigateBack,
                onSearch = ::launchAutocomplete,
                onMapClick = { viewModel.onAction(AlarmEditAction.PositionSelected(it)) },
                onRadiusChange = { viewModel.onAction(AlarmEditAction.RadiusChanged(it)) },
                onNext = { viewModel.onAction(AlarmEditAction.NextClicked) },
                onDelete = { viewModel.onAction(AlarmEditAction.DeleteRequested) },
                onNameChange = { viewModel.onAction(AlarmEditAction.NameChanged(it)) },
                onIconSelected = { viewModel.onAction(AlarmEditAction.IconSelected(it)) },
                onBackToMap = { viewModel.onAction(AlarmEditAction.BackToMapClicked) },
                onSave = { viewModel.onAction(AlarmEditAction.SaveClicked) }
            )
        } else {
            AlarmEditPortraitLayout(
                uiState = uiState,
                cameraPositionState = cameraPositionState,
                isDetailsStep = isDetailsStep,
                onBack = onNavigateBack,
                onSearch = ::launchAutocomplete,
                onMapClick = { viewModel.onAction(AlarmEditAction.PositionSelected(it)) },
                onRadiusChange = { viewModel.onAction(AlarmEditAction.RadiusChanged(it)) },
                onNext = { viewModel.onAction(AlarmEditAction.NextClicked) },
                onDelete = { viewModel.onAction(AlarmEditAction.DeleteRequested) },
                onNameChange = { viewModel.onAction(AlarmEditAction.NameChanged(it)) },
                onIconSelected = { viewModel.onAction(AlarmEditAction.IconSelected(it)) },
                onBackToMap = { viewModel.onAction(AlarmEditAction.BackToMapClicked) },
                onSave = { viewModel.onAction(AlarmEditAction.SaveClicked) }
            )
        }
    }

    AnimatedVisibility(
        visible = uiState.isLoading,
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
private fun AlarmEditPortraitLayout(
    uiState: AlarmEditUiState,
    cameraPositionState: CameraPositionState,
    isDetailsStep: Boolean,
    onBack: () -> Unit,
    onSearch: () -> Unit,
    onMapClick: (LatLng) -> Unit,
    onRadiusChange: (Float) -> Unit,
    onNext: () -> Unit,
    onDelete: () -> Unit,
    onNameChange: (String) -> Unit,
    onIconSelected: (String) -> Unit,
    onBackToMap: () -> Unit,
    onSave: () -> Unit
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
                onBack = onBack,
                onSearch = onSearch,
                onMapClick = onMapClick,
                onRadiusChange = onRadiusChange,
                onNext = {
                    haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                    onNext()
                },
                onDelete = {
                    haptic.performHapticFeedback(HapticFeedbackType.Reject)
                    onDelete()
                },
                dimAlpha = 0.38f * firstProgress,
                onDimmedAreaClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                    onBackToMap()
                },
                modifier = Modifier.offset { IntOffset(x = 0, y = firstOffsetYPx) }
            )
        }
    }
}

@Composable
private fun AlarmEditLandscapeLayout(
    uiState: AlarmEditUiState,
    cameraPositionState: CameraPositionState,
    isDetailsStep: Boolean,
    onBack: () -> Unit,
    onSearch: () -> Unit,
    onMapClick: (LatLng) -> Unit,
    onRadiusChange: (Float) -> Unit,
    onNext: () -> Unit,
    onDelete: () -> Unit,
    onNameChange: (String) -> Unit,
    onIconSelected: (String) -> Unit,
    onBackToMap: () -> Unit,
    onSave: () -> Unit
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
                onBack = onBack,
                onSearch = onSearch,
                onMapClick = onMapClick,
                onRadiusChange = onRadiusChange,
                onNext = {
                    haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                    onNext()
                },
                onDelete = {
                    haptic.performHapticFeedback(HapticFeedbackType.Reject)
                    onDelete()
                },
                dimAlpha = 0.38f * firstProgress,
                onDimmedAreaClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                    onBackToMap()
                },
                modifier = Modifier.offset { IntOffset(x = firstOffsetXPx, y = 0) }
            )
        }
    }
}

@Composable
private fun AlarmEditPortraitStepOnePage(
    uiState: AlarmEditUiState,
    cameraPositionState: CameraPositionState,
    onBack: () -> Unit,
    onSearch: () -> Unit,
    onMapClick: (LatLng) -> Unit,
    onRadiusChange: (Float) -> Unit,
    onNext: () -> Unit,
    onDelete: () -> Unit,
    dimAlpha: Float,
    onDimmedAreaClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val navigationBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val bottomPadding = maxOf(navigationBottom, 24.dp)
    val deviceCorner = rememberSystemDisplayCornerRadiusDp()
    val deviceShape = RoundedCornerShape(deviceCorner)
    Box(modifier = modifier.fillMaxSize()) {
        AlarmEditMapContent(
            cameraPositionState = cameraPositionState,
            uiState = uiState,
            onMapClick = onMapClick,
            contentPadding = PaddingValues(bottom = 220.dp),
            modifier = Modifier.fillMaxSize()
        )

        com.github.jimmy90109.geoalarm.ui.components.TopAppBar(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(start = 24.dp, end = 24.dp),
            title = {
                Text(
                    if (uiState.existingAlarm != null) stringResource(R.string.edit_alarm)
                    else stringResource(R.string.add_alarm)
                )
            },
            navigationIcon = {
                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                        onBack()
                    }
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.cancel)
                    )
                }
            },
            actions = {
                IconButton(
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
        )

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(start = 24.dp, end = 24.dp, bottom = bottomPadding)
        ) {
            AlarmEditRadiusControl(
                radius = uiState.radius,
                onRadiusChange = onRadiusChange,
                onPrimaryClick = onNext,
                primaryButtonLabel = stringResource(R.string.next_step),
                primaryEnabled = uiState.selectedPosition != null,
                isEditMode = uiState.existingAlarm != null,
                onDeleteClick = onDelete,
                elevation = 10.dp,
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
    onBack: () -> Unit,
    onSearch: () -> Unit,
    onMapClick: (LatLng) -> Unit,
    onRadiusChange: (Float) -> Unit,
    onNext: () -> Unit,
    onDelete: () -> Unit,
    dimAlpha: Float,
    onDimmedAreaClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
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
            onMapClick = onMapClick,
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
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                com.github.jimmy90109.geoalarm.ui.components.TopAppBar(
                    title = {
                        Text(
                            if (uiState.existingAlarm != null) stringResource(R.string.edit_alarm)
                            else stringResource(R.string.add_alarm)
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                                onBack()
                            }
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.cancel)
                            )
                        }
                    },
                    actions = {
                        IconButton(
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
                )

                AlarmEditRadiusControl(
                    radius = uiState.radius,
                    onRadiusChange = onRadiusChange,
                    onPrimaryClick = onNext,
                    primaryButtonLabel = stringResource(R.string.next_step),
                    primaryEnabled = uiState.selectedPosition != null,
                    isEditMode = uiState.existingAlarm != null,
                    onDeleteClick = onDelete,
                    elevation = 10.dp,
                )
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
                            label = { Text(stringResource(R.string.alarm_name)) },
                            placeholder = { Text(stringResource(R.string.enter_alarm_name)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Text(
                            text = stringResource(R.string.select_alarm_icon),
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
                    label = { Text(stringResource(R.string.alarm_name)) },
                    placeholder = { Text(stringResource(R.string.enter_alarm_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = stringResource(R.string.select_alarm_icon),
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
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    val context = LocalContext.current
    val darkTheme = isSystemInDarkTheme()
    var mapProperties by remember { mutableStateOf(MapProperties()) }

    LaunchedEffect(darkTheme) {
        mapProperties = if (darkTheme) {
            mapProperties.copy(
                mapStyleOptions = MapStyleOptions.loadRawResourceStyle(
                    context,
                    R.raw.map_style_dark
                )
            )
        } else {
            mapProperties.copy(mapStyleOptions = null)
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
            onMapClick = onMapClick,
            onMapLoaded = { isMapLoaded = true }
        ) {
            uiState.selectedPosition?.let { pos ->
                Marker(
                    state = MarkerState(position = pos),
                    title = "Destination"
                )
                Circle(
                    center = pos,
                    radius = uiState.radius.toDouble(),
                    fillColor = Color(0xFF607D8B).copy(alpha = 0.1f),
                    strokeColor = Color(0xFF607D8B).copy(alpha = 0.8f),
                    strokeWidth = 2f
                )
            }
        }

        AnimatedVisibility(visible = !isMapLoaded, exit = fadeOut()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(backgroundColor)
            )
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
) {
    val view = LocalView.current
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
                text = stringResource(R.string.radius_label, radius.toInt()),
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
                valueRange = 500f..5000f,
                steps = 45
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
