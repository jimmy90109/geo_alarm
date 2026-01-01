package com.example.geo_alarm.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.geo_alarm.R
import com.example.geo_alarm.data.Alarm
import com.example.geo_alarm.data.AlarmRepository
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmEditScreen(
    repository: AlarmRepository, alarmId: String? = null, onNavigateBack: () -> Unit
) {
    // State
    var selectedPosition by remember { mutableStateOf<LatLng?>(null) }
    var radius by remember { mutableStateOf(1000f) }
    var initialName by remember { mutableStateOf("") }
    var isMapLoading by remember { mutableStateOf(true) }
    var showNameDialog by remember { mutableStateOf(false) }
    var existingAlarm by remember { mutableStateOf<Alarm?>(null) }

    val scope = rememberCoroutineScope()
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(25.034, 121.564), 13f)
    }

    // Load Data
    LaunchedEffect(alarmId) {
        if (alarmId != null) {
            val alarm = repository.getAlarm(alarmId)
            if (alarm != null) {
                existingAlarm = alarm
                selectedPosition = LatLng(alarm.latitude, alarm.longitude)
                radius = alarm.radius.toFloat()
                initialName = alarm.name
                cameraPositionState.position = CameraPosition.fromLatLngZoom(
                    LatLng(alarm.latitude, alarm.longitude), 15f
                )
            }
        }
        delay(1000)
        isMapLoading = false
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(title = {
                Text(
                    if (existingAlarm != null) stringResource(R.string.edit_alarm) else stringResource(
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
                    selectedPosition = latLng
                }) {
                selectedPosition?.let { pos ->
                    Marker(
                        state = MarkerState(position = pos), title = "Destination"
                    )
                    Circle(
                        center = pos,
                        radius = radius.toDouble(),
                        fillColor = Color(0xFF607D8B).copy(alpha = 0.1f), // BlueGrey with 0.1 alpha
                        strokeColor = Color(0xFF607D8B).copy(alpha = 0.8f), // BlueGrey with 0.8 alpha
                        strokeWidth = 2f
                    )
                }
            }

// Bottom Slider Widget
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(start = 24.dp, end = 24.dp, bottom = 24.dp)
            ) {
                Card(
                    shape = RoundedCornerShape(36.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
                    elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
                    modifier = Modifier.fillMaxWidth()
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
                            onValueChange = { radius = it },
                            valueRange = 100f..5000f,
                            steps = 49
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { showNameDialog = true },
                            enabled = selectedPosition != null,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.save))
                        }
                    }
                }
            }

            // Loading Overlay
            AnimatedVisibility(
                visible = isMapLoading, enter = fadeIn(), exit = fadeOut()
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
    if (showNameDialog) {
        var name by remember { mutableStateOf(initialName) }
        AlertDialog(
            onDismissRequest = { showNameDialog = false },
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
                        if (name.isNotBlank() && selectedPosition != null) {
                            val newAlarm = Alarm(
                                id = existingAlarm?.id ?: UUID.randomUUID().toString(),
                                name = name.trim(),
                                latitude = selectedPosition!!.latitude,
                                longitude = selectedPosition!!.longitude,
                                radius = radius.toDouble(),
                                isEnabled = existingAlarm?.isEnabled ?: false
                            )
                            scope.launch {
                                if (existingAlarm == null) {
                                    repository.insert(newAlarm)
                                } else {
                                    repository.update(newAlarm)
                                }
                                showNameDialog = false
                                onNavigateBack()
                            }
                        }
                    }) {
                    Text(stringResource(R.string.save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showNameDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            })
    }
}
