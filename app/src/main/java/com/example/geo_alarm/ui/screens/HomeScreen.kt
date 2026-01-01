package com.example.geo_alarm.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.geo_alarm.R
import com.example.geo_alarm.data.Alarm
import com.example.geo_alarm.data.AlarmRepository
import com.example.geo_alarm.service.GeoAlarmService
import com.google.accompanist.permissions.*
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun HomeScreen(
    repository: AlarmRepository, onAddAlarm: () -> Unit, onAlarmClick: (Alarm) -> Unit
) {
    val alarms by repository.allAlarms.collectAsStateWithLifecycle(initialValue = emptyList())
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    var showLanguageSheet by remember { mutableStateOf(false) }
    var showEditDisabledDialog by remember { mutableStateOf(false) }
    var showSingleAlarmDialog by remember { mutableStateOf(false) }
    var showBackgroundPermissionDialog by remember { mutableStateOf(false) }
    var showNotificationPermissionDialog by remember { mutableStateOf(false) }
    var notificationPermissionRequested by remember { mutableStateOf(false) }

    // Permission Handling - Fine/Coarse Location
    val locationPermissionState = rememberMultiplePermissionsState(
        listOf(
            Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )

    // Background Location Permission (Android 10+)
    val backgroundLocationPermissionState = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
        rememberPermissionState(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
    } else {
        null
    }

    // Notification Permission (Android 13+)
    val notificationPermissionState = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
        rememberPermissionState(Manifest.permission.POST_NOTIFICATIONS)
    } else {
        null
    }

    Scaffold(modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection), topBar = {
        LargeTopAppBar(
            title = { Text(stringResource(R.string.home_title)) }, actions = {
            // Debug: Test notification button
//            IconButton(onClick = {
//                val serviceIntent = Intent(context, GeoAlarmService::class.java).apply {
//                    action = GeoAlarmService.ACTION_START_TEST
//                }
//                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
//                    context.startForegroundService(serviceIntent)
//                } else {
//                    context.startService(serviceIntent)
//                }
//            }) {
//                Icon(
//                    Icons.Filled.PlayArrow,
//                    contentDescription = "Test Notification"
//                )
//            }
            IconButton(onClick = { showLanguageSheet = true }) {
                Icon(
                    Icons.Filled.Language,
                    contentDescription = stringResource(R.string.language)
                )
            }
        }, scrollBehavior = scrollBehavior
        )
    }, floatingActionButton = {
        FloatingActionButton(onClick = {
            // Check foreground location permission first
            if (!locationPermissionState.allPermissionsGranted) {
                locationPermissionState.launchMultiplePermissionRequest()
            } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q && 
                       backgroundLocationPermissionState?.status?.isGranted != true) {
                // Need background location permission
                showBackgroundPermissionDialog = true
            } else {
                // All permissions granted
                onAddAlarm()
            }
        }) {
            Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.add_alarm))
        }
    }, snackbarHost = { SnackbarHost(snackbarHostState) }) { innerPadding ->
        if (alarms.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(stringResource(R.string.no_alarms), style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(bottom = 80.dp) // Add bottom padding for FAB
            ) {
                items(
                    items = alarms, key = { it.id }) { alarm ->
                    val msgAlarmDeleted = stringResource(R.string.alarm_deleted)
                    val labelUndo = stringResource(R.string.undo)


                    SwipeToDeleteContainer(
                        item = alarm, onDelete = {
                            scope.launch {
                                // Delete logic
                                repository.delete(alarm)
                                val result = snackbarHostState.showSnackbar(
                                    message = msgAlarmDeleted,
                                    actionLabel = labelUndo,
                                    duration = SnackbarDuration.Short
                                )
                                if (result == SnackbarResult.ActionPerformed) {
                                    repository.insert(alarm) // Undo delete
                                }
                            }
                        }) {
                        AlarmItem(alarm = alarm, onClick = {
                            if (alarm.isEnabled) {
                                showEditDisabledDialog = true
                            } else {
                                onAlarmClick(alarm)
                            }
                        }, onToggle = { isChecked ->
                            if (isChecked) {
                                // Check notification permission first (Android 13+)
                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU &&
                                    notificationPermissionState?.status?.isGranted != true) {
                                    notificationPermissionRequested = true
                                    notificationPermissionState?.launchPermissionRequest()
                                    return@AlarmItem
                                }
                                
                                // Check if any other alarm is enabled
                                val anyEnabled = alarms.any { it.isEnabled && it.id != alarm.id }
                                if (anyEnabled) {
                                    showSingleAlarmDialog = true
                                } else {
                                    if (locationPermissionState.allPermissionsGranted) {
                                        scope.launch {
                                            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
                                            try {
                                                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                                                    if (location != null) {
                                                        // Update DB
                                                        scope.launch { repository.update(alarm.copy(isEnabled = true)) }
                                                        
                                                        // Start Service
                                                        val serviceIntent = Intent(context, GeoAlarmService::class.java).apply {
                                                            action = GeoAlarmService.ACTION_START
                                                            putExtra(GeoAlarmService.EXTRA_ALARM_ID, alarm.id)
                                                            putExtra(GeoAlarmService.EXTRA_NAME, alarm.name)
                                                            putExtra(GeoAlarmService.EXTRA_DEST_LAT, alarm.latitude)
                                                            putExtra(GeoAlarmService.EXTRA_DEST_LNG, alarm.longitude)
                                                            putExtra(GeoAlarmService.EXTRA_RADIUS, alarm.radius)
                                                            putExtra(GeoAlarmService.EXTRA_START_LAT, location.latitude)
                                                            putExtra(GeoAlarmService.EXTRA_START_LNG, location.longitude)
                                                        }
                                                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                                            context.startForegroundService(serviceIntent)
                                                        } else {
                                                            context.startService(serviceIntent)
                                                        }
                                                    } else {
                                                        // Handle null location (maybe show snackbar or request updates)
                                                        // For now just toggle on but service might be wonky without start location? 
                                                        // Actually if we can't get start location, the progress calculation will fail.
                                                        // Let's assume 0,0 or try to handle it.
                                                        // Better: Don't enable if no location
                                                    }
                                                }
                                            } catch (e: SecurityException) {
                                                // Handle permission exception
                                            }
                                        }
                                    } else {
                                       locationPermissionState.launchMultiplePermissionRequest()
                                    }
                                }
                            } else {
                                scope.launch { repository.update(alarm.copy(isEnabled = false)) }
                                // Stop Service
                                val serviceIntent = Intent(context, GeoAlarmService::class.java).apply {
                                    action = GeoAlarmService.ACTION_STOP
                                }
                                context.startService(serviceIntent)
                            }
                        })
                    }

                }
            }
        }
    }

    if (showLanguageSheet) {
        val currentLocales = AppCompatDelegate.getApplicationLocales()
        val currentLanguage = if (!currentLocales.isEmpty) currentLocales.toLanguageTags().split("-")[0] else "en"

        ModalBottomSheet(onDismissRequest = { showLanguageSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            ) {
                ListItem(
                    colors = ListItemDefaults.colors(
                        containerColor = Color.Transparent,
                    ),
                    headlineContent = { Text(stringResource(R.string.locale_zh)) },
                    trailingContent = {
                        if (currentLanguage == "zh") {
                            Icon(Icons.Filled.Check, contentDescription = null)
                        }
                    },
                    modifier = Modifier.clickable {
                        setAppLocale("zh-TW")
                        showLanguageSheet = false
                    })
                ListItem(
                    colors = ListItemDefaults.colors(
                        containerColor = Color.Transparent,
                    ),
                    headlineContent = { Text(stringResource(R.string.locale_en)) },
                    trailingContent = {
                        if (currentLanguage == "en") {
                            Icon(Icons.Filled.Check, contentDescription = null)
                        }
                    },
                    modifier = Modifier.clickable {
                        setAppLocale("en")
                        showLanguageSheet = false
                    })
            }
        }
    }

    // Dialogs
    if (showEditDisabledDialog) {
        AlertDialog(
            onDismissRequest = { showEditDisabledDialog = false },
            title = { Text(stringResource(R.string.edit_alarm)) },
            text = { Text(stringResource(R.string.edit_disabled_error)) },
            confirmButton = {
                TextButton(onClick = { showEditDisabledDialog = false }) {
                    Text(stringResource(R.string.ok))
                }
            })
    }

    if (showSingleAlarmDialog) {
        AlertDialog(
            onDismissRequest = { showSingleAlarmDialog = false },
            title = { Text(stringResource(R.string.alarm_name)) }, // Using generic title or could specific 'Error'
            text = { Text(stringResource(R.string.only_one_alarm_error)) },
            confirmButton = {
                TextButton(onClick = { showSingleAlarmDialog = false }) {
                    Text(stringResource(R.string.ok))
                }
            })
    }

    // Background Location Permission Dialog
    if (showBackgroundPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showBackgroundPermissionDialog = false },
            title = { Text(stringResource(R.string.background_location_title)) },
            text = { Text(stringResource(R.string.background_location_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showBackgroundPermissionDialog = false
                    // Navigate to app settings
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                    context.startActivity(intent)
                }) {
                    Text(stringResource(R.string.go_to_settings))
                }
            },
            dismissButton = {
                TextButton(onClick = { showBackgroundPermissionDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // Detect notification permission denial state (only after user-initiated request)
    LaunchedEffect(notificationPermissionState?.status) {
        if (notificationPermissionRequested && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            val status = notificationPermissionState?.status
            // If permission was denied after user requested it
            if (status != null && !status.isGranted) {
                // User denied - show dialog
                showNotificationPermissionDialog = true
                notificationPermissionRequested = false // Reset flag
            } else if (status != null && status.isGranted) {
                // Permission granted, reset flag
                notificationPermissionRequested = false
            }
        }
    }

    // Notification Permission Dialog
    if (showNotificationPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showNotificationPermissionDialog = false },
            title = { Text(stringResource(R.string.notification_permission_title)) },
            text = { Text(stringResource(R.string.notification_permission_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showNotificationPermissionDialog = false
                    // Navigate to app settings
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                    context.startActivity(intent)
                }) {
                    Text(stringResource(R.string.go_to_settings))
                }
            },
            dismissButton = {
                TextButton(onClick = { showNotificationPermissionDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
fun AlarmItem(
    alarm: Alarm, modifier: Modifier = Modifier, onClick: () -> Unit, onToggle: (Boolean) -> Unit
) {
    Box(
        modifier = modifier
            .background(MaterialTheme.colorScheme.background)
            .clickable {
                onClick()
            },
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                modifier = Modifier.weight(1f),
                text = alarm.name, style = MaterialTheme.typography.titleLarge,
            )

            Switch(
                checked = alarm.isEnabled, onCheckedChange = onToggle
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> SwipeToDeleteContainer(
    item: T, onDelete: (T) -> Unit, animationDuration: Int = 500, content: @Composable (T) -> Unit
) {
    var isRemoved by remember { mutableStateOf(false) }
    val state = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                isRemoved = true
                true
            } else {
                false
            }
        })

    LaunchedEffect(isRemoved) {
        if (isRemoved) {
            onDelete(item)
        }
    }

    AnimatedVisibility(
        visible = !isRemoved, exit = shrinkVertically(
            animationSpec = tween(durationMillis = animationDuration), shrinkTowards = Alignment.Top
        ) + fadeOut()
    ) {
        SwipeToDismissBox(
            state = state, backgroundContent = {
            val color = Color.Red
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color)
                    .padding(16.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = stringResource(R.string.delete),
                    tint = Color.White
                )
            }
        }, content = { content(item) }, enableDismissFromStartToEnd = false
        )
    }
}

private fun setAppLocale(languageTag: String) {
    val appLocale = LocaleListCompat.forLanguageTags(languageTag)
    AppCompatDelegate.setApplicationLocales(appLocale)
}
