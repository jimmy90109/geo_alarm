package com.github.jimmy90109.geoalarm.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import sh.calvin.reorderable.*
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.decode.VideoFrameDecoder
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner

import com.github.jimmy90109.geoalarm.R
import com.github.jimmy90109.geoalarm.data.MaxPlaceReminderAttachments
import com.github.jimmy90109.geoalarm.data.PlaceReminderType
import com.github.jimmy90109.geoalarm.data.PlaceReminderAttachmentType
import com.github.jimmy90109.geoalarm.data.PlaceReminderWithItems
import com.github.jimmy90109.geoalarm.data.PlaceTriggerType
import com.github.jimmy90109.geoalarm.ui.components.AlarmIconBadge
import com.github.jimmy90109.geoalarm.ui.components.BackgroundLocationPermissionDialog
import com.github.jimmy90109.geoalarm.ui.components.TopAppBar as GeoTopAppBar
import com.github.jimmy90109.geoalarm.ui.viewmodel.AlarmEditControlMode
import com.github.jimmy90109.geoalarm.ui.viewmodel.AlarmEditStep
import com.github.jimmy90109.geoalarm.ui.viewmodel.PlaceReminderDetailEffect
import com.github.jimmy90109.geoalarm.ui.viewmodel.PlaceReminderDetailViewModel
import com.github.jimmy90109.geoalarm.ui.viewmodel.PlaceReminderEditAction
import com.github.jimmy90109.geoalarm.ui.viewmodel.PlaceReminderEditEffect
import com.github.jimmy90109.geoalarm.ui.viewmodel.PlaceReminderEditUiState
import com.github.jimmy90109.geoalarm.ui.viewmodel.PlaceReminderEditViewModel
import com.github.jimmy90109.geoalarm.ui.viewmodel.PlaceReminderListViewModel
import com.github.jimmy90109.geoalarm.ui.viewmodel.PlaceReminderPermissionState
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import java.text.DateFormat
import java.util.Date
import kotlin.math.cos
import kotlin.math.log2
import kotlinx.coroutines.launch

private val DefaultPlaceReminderMapPosition = LatLng(25.034, 121.564)
private val RadiusOptions = listOf(100, 150, 200, 300)
private val DwellOptions = listOf(1, 3, 5, 10)
private val CooldownOptions = listOf(60, 180, 360, 1440)
private val PlaceReminderPreviewMapHeight = 180.dp
private val PlaceReminderLandscapeControlWidth = 360.dp
private val PlaceReminderLandscapePaneGap = 24.dp
private val PlaceReminderOverlayMaxWidth = 720.dp
private val PlaceReminderFormMaxWidth = 360.dp
private const val PlaceReminderContentPageIndex = 1
private const val PlaceReminderEditPageCount = 3

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

    val formPagerState = rememberPagerState(pageCount = { PlaceReminderEditPageCount })
    val formScope = rememberCoroutineScope()
    fun handleFormBack() {
        if (formPagerState.currentPage > 0) {
            formScope.launch {
                formPagerState.animateScrollToPage(formPagerState.currentPage - 1)
            }
        } else {
            onBack(null)
        }
    }

    BackHandler(enabled = formPagerState.currentPage > 0) {
        handleFormBack()
    }

    Box(modifier = Modifier.fillMaxSize()) {
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
            pagerState = formPagerState,
            isLandscape = isLandscape,
            modifier = Modifier.fillMaxSize(),
        )
        val onNextPage = {
            if (canProceedFromPlaceReminderPage(formPagerState.currentPage, uiState)) {
                formScope.launch {
                    formPagerState.animateScrollToPage(formPagerState.currentPage + 1)
                }
            }
        }
        if (isLandscape) {
            PlaceReminderEditLandscapeControls(
                pagerState = formPagerState,
                state = uiState,
                onBack = ::handleFormBack,
                onNext = onNextPage,
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
                    onSave = { viewModel.onAction(PlaceReminderEditAction.SaveClicked) },
                )
            }
        }
    }
}

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
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            uiState.selectedPosition ?: uiState.currentLocation ?: DefaultPlaceReminderMapPosition,
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
            onSearchQueryChange = { viewModel.onAction(PlaceReminderEditAction.InAppSearchQueryChanged(it)) },
            onSearchSubmit = { viewModel.onAction(PlaceReminderEditAction.SubmitInAppSearch) },
            onSearchCancel = { viewModel.onAction(PlaceReminderEditAction.CancelInAppSearch) },
            onSuggestionSelected = { viewModel.onAction(PlaceReminderEditAction.PlaceSuggestionSelected(it)) },
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
            onSearchQueryChange = { viewModel.onAction(PlaceReminderEditAction.InAppSearchQueryChanged(it)) },
            onSearchSubmit = { viewModel.onAction(PlaceReminderEditAction.SubmitInAppSearch) },
            onSearchCancel = { viewModel.onAction(PlaceReminderEditAction.CancelInAppSearch) },
            onSuggestionSelected = { viewModel.onAction(PlaceReminderEditAction.PlaceSuggestionSelected(it)) },
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

@Composable
private fun PlaceReminderEditTopBar(
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
private fun PlaceReminderEditLandscapeControls(
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
@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalLayoutApi::class)
private fun PlaceReminderEditContent(
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
private fun PlaceReminderPlaceFormPage(
    state: PlaceReminderEditUiState,
    onSelectPlace: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = stringResource(R.string.place_reminder_step_place),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
        PlaceReminderSelectedPlaceSection(
            state = state,
            onSelectPlace = onSelectPlace,
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private fun PlaceReminderContentFormPage(
    state: PlaceReminderEditUiState,
    onAction: (PlaceReminderEditAction) -> Unit,
    newChecklistText: String,
    onNewChecklistTextChange: (String) -> Unit,
    newChecklistFocusRequester: FocusRequester,
    onPickAttachments: () -> Unit,
) {
    FormSection(title = stringResource(R.string.place_reminder_step_content)) {
        OutlinedTextField(
            value = state.title,
            onValueChange = { onAction(PlaceReminderEditAction.TitleChanged(it)) },
            label = { Text(stringResource(R.string.place_reminder_name)) },
            placeholder = { Text(stringResource(R.string.place_reminder_name_placeholder)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = PlaceReminderTextFieldShape,
        )
        ExpressiveOptionGroup(
            title = stringResource(R.string.place_reminder_type),
            options = listOf(
                PlaceReminderType.TEXT to stringResource(R.string.place_reminder_type_text),
                PlaceReminderType.CHECKLIST to stringResource(R.string.place_reminder_type_checklist),
            ),
            selected = state.type,
            onSelected = { onAction(PlaceReminderEditAction.TypeChanged(it)) },
        )
        if (state.type == PlaceReminderType.TEXT) {
            OutlinedTextField(
                value = state.content,
                onValueChange = { onAction(PlaceReminderEditAction.ContentChanged(it)) },
                label = { Text(stringResource(R.string.place_reminder_content)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(136.dp),
                minLines = 4,
                shape = PlaceReminderTextFieldShape,
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = stringResource(R.string.place_reminder_content),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                val visibleItems = state.checklistItems.filter { it.text.isNotBlank() }
                ReorderableColumn(
                    list = visibleItems,
                    onSettle = { fromIndex, toIndex ->
                        val fromId = visibleItems.getOrNull(fromIndex)?.id
                        val toId = visibleItems.getOrNull(toIndex)?.id
                        val fullFrom = state.checklistItems.indexOfFirst { it.id == fromId }
                        val fullTo = state.checklistItems.indexOfFirst { it.id == toId }
                        if (fullFrom >= 0 && fullTo >= 0) {
                            onAction(PlaceReminderEditAction.MoveChecklistItem(fullFrom, fullTo))
                        }
                    },
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) { _, item, _ ->
                    val index = state.checklistItems.indexOfFirst { it.id == item.id }
                    ChecklistEditRow(
                        value = item.text,
                        onValueChange = {
                            if (index >= 0) onAction(PlaceReminderEditAction.ChecklistItemChanged(index, it))
                        },
                        onRemove = {
                            if (index >= 0) onAction(PlaceReminderEditAction.RemoveChecklistItem(index))
                        },
                        dragHandleModifier = Modifier.draggableHandle(),
                    )
                }
                ChecklistAddRow(
                    value = newChecklistText,
                    onValueChange = onNewChecklistTextChange,
                    onAdd = {
                        val text = newChecklistText.trim()
                        if (text.isNotEmpty()) {
                            onAction(PlaceReminderEditAction.AddChecklistItemWithText(text))
                            onNewChecklistTextChange("")
                            newChecklistFocusRequester.requestFocus()
                        }
                    },
                    focusRequester = newChecklistFocusRequester,
                )
            }
        }
        PlaceReminderAttachmentEditor(
            state = state,
            onPickAttachments = onPickAttachments,
            onRemoveAttachment = { onAction(PlaceReminderEditAction.RemoveAttachment(it)) },
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private fun PlaceReminderTriggerFormPage(
    state: PlaceReminderEditUiState,
    onAction: (PlaceReminderEditAction) -> Unit,
) {
    FormSection(title = stringResource(R.string.place_reminder_step_trigger)) {
        ExpressiveOptionGroup(
            title = stringResource(R.string.place_reminder_trigger_type),
            options = listOf(
                PlaceTriggerType.ENTER to stringResource(R.string.place_reminder_trigger_enter),
                PlaceTriggerType.DWELL to stringResource(R.string.place_reminder_trigger_dwell),
            ),
            selected = state.triggerType,
            onSelected = { onAction(PlaceReminderEditAction.TriggerTypeChanged(it)) },
        )
        if (state.triggerType == PlaceTriggerType.DWELL) {
            ExpressiveIntOptionGroup(
                title = stringResource(R.string.place_reminder_dwell_time),
                options = DwellOptions,
                selected = state.dwellMinutes,
                label = { it.toString() },
                onSelected = { onAction(PlaceReminderEditAction.DwellMinutesChanged(it)) },
            )
        }
        CooldownOptionGroup(
            title = stringResource(R.string.place_reminder_cooldown),
            numericOptions = CooldownOptions.filterNot { it == 1440 },
            selected = state.cooldownMinutes,
            oneDayLabel = stringResource(R.string.place_reminder_cooldown_today),
            onSelected = { onAction(PlaceReminderEditAction.CooldownMinutesChanged(it)) },
        )
    }
}

@Composable
private fun PlaceReminderEditBottomBar(
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

@Composable
private fun Modifier.landscapeSafeAreaPadding(isLandscape: Boolean): Modifier {
    return if (isLandscape) {
        windowInsetsPadding(WindowInsets.displayCutout)
    } else {
        this
    }
}

private fun canProceedFromPlaceReminderPage(
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
            Surface(
                modifier = Modifier.size(
                    width = if (index == currentPage) 22.dp else 8.dp,
                    height = 8.dp,
                ),
                shape = CircleShape,
                color = if (index == currentPage) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.outlineVariant
                },
                content = {},
            )
        }
    }
}

@Composable
private fun FormSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                content()
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private fun <T> ExpressiveOptionGroup(
    title: String,
    options: List<Pair<T, String>>,
    selected: T,
    onSelected: (T) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        ExpressiveOptionButtons(options = options, selected = selected, onSelected = onSelected)
    }
}

@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private fun <T> ExpressiveOptionButtons(
    options: List<Pair<T, String>>,
    selected: T,
    onSelected: (T) -> Unit,
) {
    ButtonGroup(
        overflowIndicator = { menuState ->
            ButtonGroupDefaults.OverflowIndicator(menuState = menuState)
        },
        modifier = Modifier.fillMaxWidth(),
    ) {
        val buttonGroupScope = this
        options.forEach { (value, label) ->
            val isSelected = selected == value
            customItem(
                buttonGroupContent = {
                    ToggleButton(
                        checked = isSelected,
                        onCheckedChange = { onSelected(value) },
                        modifier = with(buttonGroupScope) { Modifier.weight(1f) },
                        shapes = ToggleButtonDefaults.shapes(
                            shape = RoundedCornerShape(14.dp),
                            pressedShape = RoundedCornerShape(20.dp),
                            checkedShape = ToggleButtonDefaults.roundShape,
                        ),
                        colors = ToggleButtonDefaults.toggleButtonColors().copy(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.42f),
                            checkedContainerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            checkedContentColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                    ) {
                        if (isSelected) {
                            Icon(Icons.Filled.Check, contentDescription = null)
                        }
                        Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                },
                menuContent = { menuState ->
                    DropdownMenuItem(
                        text = { Text(label) },
                        onClick = {
                            onSelected(value)
                            menuState.dismiss()
                        },
                        leadingIcon = {
                            if (isSelected) Icon(Icons.Filled.Check, contentDescription = null)
                        },
                    )
                }
            )
        }
    }
}

@Composable
private fun ExpressiveIntOptionGroup(
    title: String,
    options: List<Int>,
    selected: Int,
    label: @Composable (Int) -> String,
    onSelected: (Int) -> Unit,
) {
    ExpressiveOptionGroup(
        title = title,
        options = options.map { it to label(it) },
        selected = selected,
        onSelected = onSelected,
    )
}

@Composable
private fun CooldownOptionGroup(
    title: String,
    numericOptions: List<Int>,
    selected: Int,
    oneDayLabel: String,
    onSelected: (Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        ExpressiveOptionButtons(
            options = numericOptions.map { it to (it / 60).toString() },
            selected = selected,
            onSelected = onSelected,
        )
        ExpressiveSingleOptionButton(
            label = oneDayLabel,
            selected = selected == 1440,
            onClick = { onSelected(1440) },
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private fun ExpressiveSingleOptionButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    ButtonGroup(
        overflowIndicator = { menuState ->
            ButtonGroupDefaults.OverflowIndicator(menuState = menuState)
        },
        modifier = Modifier.fillMaxWidth(),
    ) {
        val buttonGroupScope = this
        customItem(
            buttonGroupContent = {
                ToggleButton(
                    checked = selected,
                    onCheckedChange = { onClick() },
                    modifier = with(buttonGroupScope) {
                        Modifier
                            .weight(1f)
                            .height(48.dp)
                    },
                    shapes = ToggleButtonDefaults.shapes(
                        shape = RoundedCornerShape(14.dp),
                        pressedShape = RoundedCornerShape(20.dp),
                        checkedShape = ToggleButtonDefaults.roundShape,
                    ),
                    colors = ToggleButtonDefaults.toggleButtonColors().copy(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.42f),
                        checkedContainerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        checkedContentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                ) {
                    if (selected) {
                        Icon(Icons.Filled.Check, contentDescription = null)
                    }
                    Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            },
            menuContent = { menuState ->
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = {
                        onClick()
                        menuState.dismiss()
                    },
                    leadingIcon = {
                        if (selected) Icon(Icons.Filled.Check, contentDescription = null)
                    },
                )
            },
        )
    }
}

@Composable
private fun PlaceReminderSelectedPlaceSection(
    state: PlaceReminderEditUiState,
    onSelectPlace: () -> Unit,
) {
    if (state.selectedPosition == null) {
        Button(
            onClick = onSelectPlace,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = CircleShape,
        ) {
            Text(stringResource(R.string.select_shared_place))
        }
        return
    }
    val position = state.selectedPosition
    val context = LocalContext.current
    val darkTheme = isSystemInDarkTheme()
    val density = LocalDensity.current
    val mapHeightPx = with(density) { PlaceReminderPreviewMapHeight.toPx() }
    val previewZoom = remember(position, state.radiusMeters, mapHeightPx) {
        previewZoomForRadius(
            latitude = position.latitude,
            radiusMeters = state.radiusMeters,
            mapHeightPx = mapHeightPx,
        )
    }
    var mapProperties by remember { mutableStateOf(MapProperties()) }
    LaunchedEffect(darkTheme) {
        mapProperties = if (darkTheme) {
            MapProperties(
                mapStyleOptions = MapStyleOptions.loadRawResourceStyle(
                    context,
                    R.raw.map_style_dark,
                ),
            )
        } else {
            MapProperties(mapStyleOptions = null)
        }
    }
    val previewMapUiSettings = remember {
        MapUiSettings(
            zoomControlsEnabled = false,
            compassEnabled = false,
            indoorLevelPickerEnabled = false,
            mapToolbarEnabled = false,
            myLocationButtonEnabled = false,
            rotationGesturesEnabled = false,
            scrollGesturesEnabled = false,
            scrollGesturesEnabledDuringRotateOrZoom = false,
            tiltGesturesEnabled = false,
            zoomGesturesEnabled = false,
        )
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelectPlace),
        shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AlarmIconBadge(iconKey = state.selectedIconKey)
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = state.placeName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (state.address?.isNotBlank() == true && state.address != state.placeName) {
                        Text(
                            text = state.address,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                Icon(Icons.Filled.ChevronRight, contentDescription = stringResource(R.string.place_reminder_edit_place))
            }

            HorizontalDivider()
            val cameraPositionState = rememberCameraPositionState {
                this.position = CameraPosition.fromLatLngZoom(position, previewZoom)
            }
            LaunchedEffect(position, previewZoom) {
                cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(position, previewZoom))
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(PlaceReminderPreviewMapHeight),
            ) {
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    properties = mapProperties,
                    uiSettings = previewMapUiSettings,
                ) {
                    Marker(state = MarkerState(position = position), title = state.placeName)
                    Circle(
                        center = position,
                        radius = state.radiusMeters.toDouble(),
                        strokeColor = Color(0xFF607D8B).copy(alpha = 0.8f),
                        strokeWidth = 2f,
                        fillColor = Color(0xFF607D8B).copy(alpha = 0.14f),
                    )
                }
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable(onClick = onSelectPlace),
                )
            }
        }
    }
}

private fun previewZoomForRadius(
    latitude: Double,
    radiusMeters: Int,
    mapHeightPx: Float,
): Float {
    val radius = radiusMeters.coerceAtLeast(50)
    val targetDiameterPx = mapHeightPx * 0.72f
    val metersPerPixel = (radius * 2f) / targetDiameterPx.coerceAtLeast(1f)
    val latitudeScale = cos(Math.toRadians(latitude)).coerceAtLeast(0.2)
    val zoom = log2((156543.03392 * latitudeScale) / metersPerPixel)
    return zoom.toFloat().coerceIn(10f, 18f)
}

@Composable
private fun PlaceReminderAttachmentEditor(
    state: PlaceReminderEditUiState,
    onPickAttachments: () -> Unit,
    onRemoveAttachment: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = stringResource(R.string.place_reminder_attachments),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
        if (state.isAddingAttachments) {
            Text(
                text = stringResource(R.string.place_reminder_adding_attachments),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = if (state.attachmentLimitReached) {
                stringResource(R.string.place_reminder_attachments_limit_reached, MaxPlaceReminderAttachments)
            } else {
                stringResource(R.string.place_reminder_attachments_limit, MaxPlaceReminderAttachments)
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            state.attachments.sortedBy { it.sortOrder }.forEach { attachment ->
                AttachmentPreviewTile(
                    attachment = attachment,
                    onRemove = { onRemoveAttachment(attachment.id) },
                )
            }
            AttachmentAddTile(
                enabled = !state.isAddingAttachments && !state.attachmentLimitReached,
                onClick = onPickAttachments,
            )
        }
    }
}

@Composable
private fun AttachmentPreviewTile(
    attachment: com.github.jimmy90109.geoalarm.data.PlaceReminderAttachment,
    onRemove: () -> Unit,
) {
    val isVideo = attachment.type == PlaceReminderAttachmentType.VIDEO
    val context = LocalContext.current
    val imageRequest = remember(attachment.localPath, isVideo) {
        ImageRequest.Builder(context)
            .data(attachment.localPath)
            .crossfade(true)
            .apply {
                if (isVideo) decoderFactory(VideoFrameDecoder.Factory())
            }
            .build()
    }
    Box(
        modifier = Modifier
            .size(104.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        AsyncImage(
            model = imageRequest,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
        if (isVideo) {
            Icon(
                Icons.Filled.Videocam,
                contentDescription = null,
                tint = androidx.compose.ui.graphics.Color.White,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp),
            )
        }
        Surface(
            onClick = onRemove,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(6.dp)
                .size(28.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = stringResource(R.string.delete),
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Composable
private fun AttachmentAddTile(
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val outlineColor = if (enabled) {
        MaterialTheme.colorScheme.outline
    } else {
        MaterialTheme.colorScheme.outline.copy(alpha = 0.38f)
    }
    val iconColor = if (enabled) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
    }
    Box(
        modifier = Modifier
            .size(104.dp)
            .clip(RoundedCornerShape(24.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .drawDottedOutline(outlineColor, RoundedCornerShape(24.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Filled.Add,
            contentDescription = stringResource(R.string.place_reminder_add_attachment),
            tint = iconColor,
            modifier = Modifier.size(32.dp),
        )
    }
}

private fun Modifier.drawDottedOutline(
    color: androidx.compose.ui.graphics.Color,
    shape: RoundedCornerShape,
): Modifier = drawBehind {
    val strokeWidth = 1.5.dp.toPx()
    drawRoundRect(
        color = color,
        cornerRadius = CornerRadius(24.dp.toPx(), 24.dp.toPx()),
        style = Stroke(
            width = strokeWidth,
            cap = StrokeCap.Round,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(1.dp.toPx(), 7.dp.toPx())),
        ),
    )
}

@Composable
private fun PlacePickerSection(
    state: PlaceReminderEditUiState,
    onAction: (PlaceReminderEditAction) -> Unit,
) {
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            state.selectedPosition ?: state.currentLocation ?: DefaultPlaceReminderMapPosition,
            15f,
        )
    }
    LaunchedEffect(state.selectedPosition, state.currentLocation) {
        val target = state.selectedPosition ?: state.currentLocation
        if (target != null) {
            cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(target, 15f))
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.place_reminder_place),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = { onAction(PlaceReminderEditAction.SearchQueryChanged(it)) },
                label = { Text(stringResource(R.string.search_location)) },
                modifier = Modifier.weight(1f),
                singleLine = true,
                shape = PlaceReminderTextFieldShape,
            )
            IconButton(onClick = { onAction(PlaceReminderEditAction.SearchSubmitted) }) {
                Icon(Icons.Filled.Search, contentDescription = stringResource(R.string.search_location))
            }
        }
        if (state.isSearching) {
            Text(
                text = stringResource(R.string.searching_location),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (state.searchFailed) {
            Text(
                text = stringResource(R.string.location_search_failed_inline),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
        state.searchResults.forEachIndexed { index, candidate ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onAction(PlaceReminderEditAction.SearchResultSelected(index)) },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(candidate.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    candidate.address?.let {
                        Text(
                            it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp),
        ) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(isMyLocationEnabled = state.currentLocation != null),
                onMapClick = { onAction(PlaceReminderEditAction.MapPositionSelected(it)) },
            ) {
                state.selectedPosition?.let { position ->
                    Marker(state = MarkerState(position = position), title = state.placeName)
                    Circle(
                        center = position,
                        radius = state.radiusMeters.toDouble(),
                        strokeColor = MaterialTheme.colorScheme.primary,
                        fillColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                    )
                }
            }
        }
        if (state.selectedPosition == null) {
            Text(
                text = stringResource(R.string.place_reminder_place_required),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        } else {
            Text(
                text = listOf(state.placeName, state.address)
                    .filter { !it.isNullOrBlank() }
                    .joinToString("\n"),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
