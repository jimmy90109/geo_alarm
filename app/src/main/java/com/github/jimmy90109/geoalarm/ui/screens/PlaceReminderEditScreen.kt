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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
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
import com.github.jimmy90109.geoalarm.data.PlaceReminderType
import com.github.jimmy90109.geoalarm.data.PlaceReminderWithItems
import com.github.jimmy90109.geoalarm.data.PlaceTriggerType
import com.github.jimmy90109.geoalarm.ui.components.AlarmIconBadge
import com.github.jimmy90109.geoalarm.ui.components.BackgroundLocationPermissionDialog
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
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import java.text.DateFormat
import java.util.Date

private val DefaultPlaceReminderMapPosition = LatLng(25.034, 121.564)
private val RadiusOptions = listOf(100, 150, 200, 300)
private val DwellOptions = listOf(1, 3, 5, 10)
private val CooldownOptions = listOf(60, 180, 360, 1440)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaceReminderEditScreen(
    viewModel: PlaceReminderEditViewModel,
    reminderId: String?,
    onBack: (String?) -> Unit,
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
    LaunchedEffect(reminderId) {
        viewModel.onAction(PlaceReminderEditAction.Load(reminderId))
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
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is PlaceReminderEditEffect.NavigateBack -> onBack(effect.reminderId)
            }
        }
    }

    fun handleLocationBack() {
        when {
            uiState.controlMode == AlarmEditControlMode.Candidates && uiState.placeCandidates.isNotEmpty() ->
                viewModel.onAction(PlaceReminderEditAction.CandidateSelectionCancelled)
            uiState.controlMode != AlarmEditControlMode.Radius ->
                viewModel.onAction(PlaceReminderEditAction.CancelInAppSearch)
            else -> viewModel.onAction(PlaceReminderEditAction.PlaceSelectionCancelled)
        }
    }

    BackHandler(enabled = uiState.isSelectingPlace) {
        if (uiState.step == AlarmEditStep.DetailsForm) {
            viewModel.onAction(PlaceReminderEditAction.BackToMapClicked)
        } else {
            handleLocationBack()
        }
    }

    if (uiState.isSelectingPlace) {
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
                onSave = { viewModel.onAction(PlaceReminderEditAction.PlaceDetailsConfirmed) },
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
                onSave = { viewModel.onAction(PlaceReminderEditAction.PlaceDetailsConfirmed) },
                onCandidateChanged = { viewModel.onAction(PlaceReminderEditAction.CandidateChanged(it)) },
                onCandidateConfirmed = { viewModel.onAction(PlaceReminderEditAction.CandidateConfirmed) },
                onCandidateCancelled = { viewModel.onAction(PlaceReminderEditAction.CandidateSelectionCancelled) },
                hideMapForInitialLocation = false,
            )
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (uiState.isEditMode) {
                            stringResource(R.string.place_reminder_edit_title)
                        } else {
                            stringResource(R.string.place_reminder_create_title)
                        }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { onBack(null) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
            )
        },
        bottomBar = {
            Button(
                onClick = { viewModel.onAction(PlaceReminderEditAction.SaveClicked) },
                enabled = uiState.canSave,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            ) {
                Text(
                    if (uiState.isEditMode) {
                        stringResource(R.string.save)
                    } else {
                        stringResource(R.string.place_reminder_create_button)
                    }
                )
            }
        },
    ) { innerPadding ->
        PlaceReminderEditContent(
            state = uiState,
            onAction = viewModel::onAction,
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
        )
    }
}

@Composable
private fun PlaceReminderEditContent(
    state: PlaceReminderEditUiState,
    onAction: (PlaceReminderEditAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()
    Column(
        modifier = modifier
            .verticalScroll(scrollState)
            .padding(16.dp)
            .padding(bottom = 96.dp)
            .widthIn(max = 720.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        OutlinedTextField(
            value = state.title,
            onValueChange = { onAction(PlaceReminderEditAction.TitleChanged(it)) },
            label = { Text(stringResource(R.string.place_reminder_name)) },
            placeholder = { Text(stringResource(R.string.place_reminder_name_placeholder)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        OptionGroup(
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
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.place_reminder_content),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                state.checklistItems.forEachIndexed { index, item ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = item,
                            onValueChange = {
                                onAction(PlaceReminderEditAction.ChecklistItemChanged(index, it))
                            },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                        )
                        IconButton(onClick = { onAction(PlaceReminderEditAction.RemoveChecklistItem(index)) }) {
                            Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.delete))
                        }
                    }
                }
                OutlinedButton(onClick = { onAction(PlaceReminderEditAction.AddChecklistItem) }) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Text(stringResource(R.string.place_reminder_add_item))
                }
            }
        }
        PlaceReminderSelectedPlaceSection(
            state = state,
            onSelectPlace = { onAction(PlaceReminderEditAction.StartPlaceSelection) },
        )
        Text(
            text = stringResource(R.string.place_reminder_radius_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OptionGroup(
            title = stringResource(R.string.place_reminder_trigger_type),
            options = listOf(
                PlaceTriggerType.ENTER to stringResource(R.string.place_reminder_trigger_enter),
                PlaceTriggerType.DWELL to stringResource(R.string.place_reminder_trigger_dwell),
            ),
            selected = state.triggerType,
            onSelected = { onAction(PlaceReminderEditAction.TriggerTypeChanged(it)) },
        )
        if (state.triggerType == PlaceTriggerType.DWELL) {
            IntOptionGroup(
                title = stringResource(R.string.place_reminder_dwell_time),
                options = DwellOptions,
                selected = state.dwellMinutes,
                label = { stringResource(R.string.place_reminder_minutes, it) },
                onSelected = { onAction(PlaceReminderEditAction.DwellMinutesChanged(it)) },
            )
        }
        IntOptionGroup(
            title = stringResource(R.string.place_reminder_cooldown),
            options = CooldownOptions,
            selected = state.cooldownMinutes,
            label = {
                if (it == 1440) {
                    stringResource(R.string.place_reminder_cooldown_today)
                } else {
                    stringResource(R.string.place_reminder_hours, it / 60)
                }
            },
            onSelected = { onAction(PlaceReminderEditAction.CooldownMinutesChanged(it)) },
        )
    }
}

@Composable
private fun PlaceReminderSelectedPlaceSection(
    state: PlaceReminderEditUiState,
    onSelectPlace: () -> Unit,
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AlarmIconBadge(iconKey = state.selectedIconKey)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = stringResource(R.string.place_reminder_place),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (state.selectedPosition == null) {
                    Text(
                        text = stringResource(R.string.place_reminder_place_required),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
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
                    Text(
                        text = stringResource(R.string.radius_label, "${state.radiusMeters}"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (state.selectedPosition == null) {
                Button(onClick = onSelectPlace) {
                    Text(stringResource(R.string.select_shared_place))
                }
            } else {
                IconButton(onClick = onSelectPlace) {
                    Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.place_reminder_edit_place))
                }
            }
        }
    }
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

