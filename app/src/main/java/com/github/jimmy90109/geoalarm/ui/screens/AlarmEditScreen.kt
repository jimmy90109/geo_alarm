package com.github.jimmy90109.geoalarm.ui.screens

import android.app.Activity
import android.content.res.Configuration
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.jimmy90109.geoalarm.R
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AlarmEditScreen(
    viewModel: AlarmEditViewModel, alarmId: String? = null, onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            LatLng(
                25.034,
                121.564,
            ),
            13f,
        )
    }

    // Places Autocomplete launcher
    val autocompleteLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val place = Autocomplete.getPlaceFromIntent(result.data!!)
            place.latLng?.let { latLng ->
                viewModel.updatePositionFromSearch(
                    latLng, place.displayName ?: place.formattedAddress ?: ""
                )
                scope.launch {
                    cameraPositionState.animate(
                        CameraUpdateFactory.newLatLngZoom(latLng, 15f)
                    )
                }
            }
        }
    }

    // Launch autocomplete
    fun launchAutocomplete() {
        val fields = listOf(
            Place.Field.ID,
            Place.Field.DISPLAY_NAME,
            Place.Field.FORMATTED_ADDRESS,
            Place.Field.LOCATION,
        )
        val intent =
            Autocomplete.IntentBuilder(AutocompleteActivityMode.OVERLAY, fields).build(context)
        autocompleteLauncher.launch(intent)
    }

    // Load Data
    LaunchedEffect(alarmId) {
        viewModel.loadAlarm(alarmId)
        delay(1000)
        viewModel.setMapLoaded()
    }

    // Update camera when position changes (e.g. from existing alarm or search)
    LaunchedEffect(uiState.selectedPosition) {
        uiState.selectedPosition?.let { latLng ->
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngZoom(latLng, 15f)
            )
        }
    }

    // Navigation effect
    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            onNavigateBack()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Content
        if (isLandscape) {
            // LANDSCAPE LAYOUT
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                // Map - Full Screen
                AlarmEditMapContent(
                    cameraPositionState = cameraPositionState,
                    uiState = uiState,
                    onMapClick = { viewModel.updatePosition(it) },
                    contentPadding = PaddingValues(end = 400.dp),
                    modifier = Modifier.fillMaxSize()
                )

                // Controls Overlay
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 24.dp)
                        .windowInsetsPadding(WindowInsets.displayCutout)
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .widthIn(max = 360.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Overlay Top App Bar
                        com.github.jimmy90109.geoalarm.ui.components.TopAppBar(
                            title = {
                                Text(
                                    if (uiState.existingAlarm != null) stringResource(R.string.edit_alarm)
                                    else stringResource(
                                        R.string.add_alarm
                                    )
                                )
                            },
                            navigationIcon = {
                                IconButton(onClick = onNavigateBack) {
                                    Icon(
                                        Icons.Default.ArrowBack, // Updated to non-deprecated
                                        contentDescription = stringResource(R.string.cancel),
                                    )
                                }
                            },
                            actions = {
                                IconButton(onClick = { launchAutocomplete() }) {
                                    Icon(
                                        Icons.Filled.Search,
                                        contentDescription = stringResource(R.string.search_location)
                                    )
                                }
                            },
                        )

                        AlarmEditRadiusControl(
                            radius = uiState.radius,
                            onRadiusChange = { viewModel.updateRadius(it) },
                            onSaveClick = { viewModel.showNameDialog() },
                            saveEnabled = uiState.selectedPosition != null,
                            elevation = 10.dp,
                            shape = RoundedCornerShape(24.dp)
                        )

                    }
                }
            }
        } else {
            // PORTRAIT LAYOUT
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                // Map
                AlarmEditMapContent(
                    cameraPositionState = cameraPositionState,
                    uiState = uiState,
                    onMapClick = { viewModel.updatePosition(it) },
                    contentPadding = PaddingValues(bottom = 200.dp),
                    modifier = Modifier.fillMaxSize()
                )

                com.github.jimmy90109.geoalarm.ui.components.TopAppBar(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(start = 24.dp, end = 24.dp),
                    title = {
                        Text(
                            if (uiState.existingAlarm != null) stringResource(R.string.edit_alarm) else stringResource(
                                R.string.add_alarm
                            )
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                Icons.Default.ArrowBack, // Updated to non-deprecated
                                contentDescription = stringResource(R.string.cancel),
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { launchAutocomplete() }) {
                            Icon(
                                Icons.Filled.Search,
                                contentDescription = stringResource(R.string.search_location)
                            )
                        }
                    },
                )

                // Bottom Slider Widget
                val navigationBottom =
                    WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                val bottomPadding = maxOf(navigationBottom, 24.dp)

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(start = 24.dp, end = 24.dp, bottom = bottomPadding)
                ) {
                    AlarmEditRadiusControl(
                        radius = uiState.radius,
                        onRadiusChange = { viewModel.updateRadius(it) },
                        onSaveClick = { viewModel.showNameDialog() },
                        saveEnabled = uiState.selectedPosition != null,
                        elevation = 10.dp,
                        shape = RoundedCornerShape(44.dp)
                    )
                }
            }
        }
    }


    // Loading Overlay
    AnimatedVisibility(
        visible = uiState.isLoading, enter = fadeIn(), exit = fadeOut(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            CircularWavyProgressIndicator()
        }
    }


    // Name Dialog
    if (uiState.showNameDialog) {
        var name by remember { mutableStateOf(uiState.name) }
        AlertDialog(
            onDismissRequest = { viewModel.dismissNameDialog() },
            title = { Text(stringResource(R.string.alarm_name)) },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.enter_alarm_name)) },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (name.isNotBlank() && uiState.selectedPosition != null) {
                            viewModel.saveAlarm(name.trim())
                        }
                    }) {
                    Text(stringResource(R.string.save))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissNameDialog() }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

// ---- Sub-components ----

@Composable
fun AlarmEditMapContent(
    cameraPositionState: CameraPositionState,
    uiState: AlarmEditUiState,
    onMapClick: (LatLng) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {

    val context = LocalContext.current
    val isSystemInDarkTheme = isSystemInDarkTheme()
    var mapProperties by remember {
        mutableStateOf(MapProperties())
    }

    // Update map style based on theme
    LaunchedEffect(isSystemInDarkTheme) {
        if (isSystemInDarkTheme) {
            mapProperties = mapProperties.copy(
                mapStyleOptions = MapStyleOptions.loadRawResourceStyle(
                    context, R.raw.map_style_dark
                )
            )
        } else {
            mapProperties = mapProperties.copy(
                mapStyleOptions = null
            )
        }
    }

    val backgroundColor =
        if (isSystemInDarkTheme) Color(0xFF1d2c4d) else MaterialTheme.colorScheme.surfaceVariant
    var isMapLoaded by remember { mutableStateOf(false) }

    Box(modifier = modifier.background(backgroundColor)) {
        GoogleMap(
            modifier = Modifier.matchParentSize(),
            cameraPositionState = cameraPositionState,
            properties = mapProperties,
            uiSettings = MapUiSettings(
                zoomControlsEnabled = false, myLocationButtonEnabled = false
            ),
            contentPadding = contentPadding,
            onMapClick = onMapClick,
            onMapLoaded = { isMapLoaded = true }) {
            uiState.selectedPosition?.let { pos ->
                Marker(
                    state = MarkerState(position = pos), title = "Destination"
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

        // Cover white flash
        androidx.compose.animation.AnimatedVisibility(
            visible = !isMapLoaded, exit = androidx.compose.animation.fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(backgroundColor)
            )
        }
    }
}


@Composable
fun AlarmEditRadiusControl(
    radius: Float,
    onRadiusChange: (Float) -> Unit,
    onSaveClick: () -> Unit,
    saveEnabled: Boolean,
    modifier: Modifier = Modifier,
    elevation: androidx.compose.ui.unit.Dp = 0.dp,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(12.dp)
) {
    Card(
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.radius_label, radius.toInt()),
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.height(8.dp))
            Slider(
                value = radius, onValueChange = onRadiusChange, valueRange = 500f..5000f, steps = 45
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onSaveClick, enabled = saveEnabled, modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.save))
            }
        }
    }
}



