package com.example.geo_alarm.ui.screens

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.view.RoundedCorner
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.geo_alarm.R
import com.example.geo_alarm.data.Alarm
import com.example.geo_alarm.data.AlarmRepository
import com.example.geo_alarm.ui.viewmodel.AlarmEditViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import com.google.maps.android.compose.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmEditScreen(
    viewModel: AlarmEditViewModel, alarmId: String? = null, onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    // Local state for camera
    var isMapLoading by remember { mutableStateOf(true) }

    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(25.034, 121.564), 13f)
    }

    // Places Autocomplete launcher
    val autocompleteLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val place = Autocomplete.getPlaceFromIntent(result.data!!)
            place.latLng?.let { latLng ->
                viewModel.updatePositionFromSearch(latLng, place.displayName ?: place.formattedAddress ?: "")
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
        val fields = listOf(Place.Field.ID, Place.Field.DISPLAY_NAME, Place.Field.FORMATTED_ADDRESS, Place.Field.LOCATION)
        val intent = Autocomplete.IntentBuilder(AutocompleteActivityMode.OVERLAY, fields)
            .build(context)
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

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(title = {
                Text(
                    if (uiState.existingAlarm != null) stringResource(R.string.edit_alarm) else stringResource(
                        R.string.add_alarm
                    )
                )
            }, navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        Icons.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.cancel)
                    )
                }
            })
        }) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(top = innerPadding.calculateTopPadding())
                .fillMaxSize()
        ) {
            // Map
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                uiSettings = MapUiSettings(zoomControlsEnabled = false),
                onMapClick = { latLng ->
                    viewModel.updatePosition(latLng)
                }) {
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

            // Search Bar
            Card(
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                onClick = { launchAutocomplete() }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = uiState.searchText.ifEmpty { stringResource(R.string.search_location) },
                        color = if (uiState.searchText.isEmpty()) 
                            MaterialTheme.colorScheme.onSurfaceVariant 
                        else 
                            MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }

            // Bottom Slider Widget
            val deviceCornerRadius = rememberDeviceCornerRadius(fallback = 36.dp)
            val navigationBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
            val bottomPadding = maxOf(navigationBottom, 24.dp)
            
            // Calculate card radius: DeviceRadius - Padding (24.dp) for concentric corners
            // Enforce minimum of 24.dp
            val cardCornerRadius = maxOf(deviceCornerRadius - 24.dp, 24.dp)
            
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(start = 24.dp, end = 24.dp, bottom = bottomPadding)
            ) {
                Card(
                    shape = RoundedCornerShape(cardCornerRadius),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
                    elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stringResource(R.string.radius_label, uiState.radius.toInt()),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Slider(
                            value = uiState.radius,
                            onValueChange = { viewModel.updateRadius(it) },
                            valueRange = 100f..5000f,
                            steps = 49
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.showNameDialog() },
                            enabled = uiState.selectedPosition != null,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.save))
                        }
                    }
                }
            }

            // Loading Overlay
            AnimatedVisibility(
                visible = uiState.isLoading, enter = fadeIn(), exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
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
            })
    }
}

/**
 * Gets the device's display corner radius using Android 12+ RoundedCorner API.
 * Falls back to the provided default value on older devices or if corner radius cannot be determined.
 */
@Composable
private fun rememberDeviceCornerRadius(fallback: Dp): Dp {
    val view = LocalView.current
    val density = LocalDensity.current
    
    return remember(view, density) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val windowInsets = view.rootWindowInsets
            // Get corner from bottom-left or bottom-right (for bottom widget)
            val bottomLeftCorner = windowInsets?.getRoundedCorner(RoundedCorner.POSITION_BOTTOM_LEFT)
            val bottomRightCorner = windowInsets?.getRoundedCorner(RoundedCorner.POSITION_BOTTOM_RIGHT)
            
            // Use the larger of the two corners, or fallback
            val radiusPx = maxOf(
                bottomLeftCorner?.radius ?: 0,
                bottomRightCorner?.radius ?: 0
            )
            
            if (radiusPx > 0) {
                with(density) { radiusPx.toDp() }
            } else {
                fallback
            }
        } else {
            fallback
        }
    }
}

