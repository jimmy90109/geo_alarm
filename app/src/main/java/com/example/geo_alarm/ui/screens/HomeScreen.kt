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
import com.example.geo_alarm.ui.viewmodel.HomeViewModel
import com.example.geo_alarm.service.GeoAlarmService
import com.google.accompanist.permissions.*
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel, onAddAlarm: () -> Unit, onAlarmClick: (Alarm) -> Unit
) {
    val alarms by viewModel.alarms.collectAsStateWithLifecycle(initialValue = emptyList())
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

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
            IconButton(onClick = { viewModel.showLanguageSheet() }) {
                Icon(
                    Icons.Filled.Language,
                    contentDescription = stringResource(R.string.language)
                )
            }
        }, scrollBehavior = scrollBehavior
        )
    }, floatingActionButton = {
        FloatingActionButton(onClick = {
            // explicit background permission check requested by user
            val hasLocationPermission = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                backgroundLocationPermissionState?.status?.isGranted == true
            } else {
                locationPermissionState.allPermissionsGranted
            }

            if (hasLocationPermission) {
                onAddAlarm()
            } else {
                viewModel.showBackgroundPermissionDialog()
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
                                viewModel.deleteAlarm(alarm)
                                val result = snackbarHostState.showSnackbar(
                                    message = msgAlarmDeleted,
                                    actionLabel = labelUndo,
                                    duration = SnackbarDuration.Short
                                )
                                if (result == SnackbarResult.ActionPerformed) {
                                    viewModel.restoreAlarm(alarm) // Undo delete
                                }
                            }
                        }) {
                        AlarmItem(alarm = alarm, onClick = {
                            if (alarm.isEnabled) {
                                viewModel.showEditDisabledDialog()
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
                                
                                // Check location permission
                                val hasLocationPermission = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                                    backgroundLocationPermissionState?.status?.isGranted == true
                                } else {
                                    locationPermissionState.allPermissionsGranted
                                }

                                if (hasLocationPermission) {
                                    viewModel.enableAlarm(alarm, alarms, context)
                                } else {
                                    viewModel.showBackgroundPermissionDialog()
                                }
                            } else {
                                viewModel.disableAlarm(alarm, context)
                            }
                        })
                    }

                }
            }
        }
    }

    if (uiState.showLanguageSheet) {
        val currentLocales = AppCompatDelegate.getApplicationLocales()
        val currentLanguage = if (!currentLocales.isEmpty) currentLocales.toLanguageTags().split("-")[0] else "en"

        ModalBottomSheet(onDismissRequest = { viewModel.dismissLanguageSheet() }) {
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
                        viewModel.dismissLanguageSheet()
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
                        viewModel.dismissLanguageSheet()
                    })
            }
        }
    }

    // Dialogs
    if (uiState.showEditDisabledDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissEditDisabledDialog() },
            title = { Text(stringResource(R.string.edit_alarm)) },
            text = { Text(stringResource(R.string.edit_disabled_error)) },
            confirmButton = {
                TextButton(onClick = { viewModel.dismissEditDisabledDialog() }) {
                    Text(stringResource(R.string.ok))
                }
            })
    }

    if (uiState.showSingleAlarmDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissSingleAlarmDialog() },
            title = { Text(stringResource(R.string.alarm_name)) }, // Using generic title or could specific 'Error'
            text = { Text(stringResource(R.string.only_one_alarm_error)) },
            confirmButton = {
                TextButton(onClick = { viewModel.dismissSingleAlarmDialog() }) {
                    Text(stringResource(R.string.ok))
                }
            })
    }

    // Background Location Permission Dialog
    if (uiState.showBackgroundPermissionDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissBackgroundPermissionDialog() },
            title = { Text(stringResource(R.string.background_location_title)) },
            text = { Text(stringResource(R.string.background_location_message)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.dismissBackgroundPermissionDialog()
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
                TextButton(onClick = { viewModel.dismissBackgroundPermissionDialog() }) {
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
                viewModel.showNotificationPermissionDialog()
                notificationPermissionRequested = false // Reset flag
            } else if (status != null && status.isGranted) {
                // Permission granted, reset flag
                notificationPermissionRequested = false
            }
        }
    }

    // Notification Permission Dialog
    if (uiState.showNotificationPermissionDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissNotificationPermissionDialog() },
            title = { Text(stringResource(R.string.notification_permission_title)) },
            text = { Text(stringResource(R.string.notification_permission_message)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.dismissNotificationPermissionDialog()
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
                TextButton(onClick = { viewModel.dismissNotificationPermissionDialog() }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
    
    // Already at Destination Dialog
    if (uiState.showAlreadyAtDestinationDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissAlreadyAtDestinationDialog() },
            title = { Text(stringResource(R.string.already_at_destination_title)) },
            text = { Text(stringResource(R.string.already_at_destination_message)) },
            confirmButton = {
                TextButton(onClick = { viewModel.dismissAlreadyAtDestinationDialog() }) {
                    Text(stringResource(R.string.ok))
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
