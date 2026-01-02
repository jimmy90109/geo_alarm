package com.example.geo_alarm.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.PowerManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import com.example.geo_alarm.ui.components.AlreadyAtDestinationDialog
import com.example.geo_alarm.ui.components.BackgroundLocationPermissionDialog
import com.example.geo_alarm.ui.components.DeleteAlarmDialog
import com.example.geo_alarm.ui.components.EditDisabledDialog
import com.example.geo_alarm.ui.components.NotificationPermissionDialog
import com.example.geo_alarm.ui.components.NotificationRationaleDialog
import com.example.geo_alarm.ui.components.SingleAlarmDialog
import com.example.geo_alarm.ui.viewmodel.HomeViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
/**
 * The main screen of the application displaying the list of alarms.
 * Handles permission requests (background location, notification) and navigation.
 *
 * @param viewModel The ViewModel capable of managing home screen state.
 * @param onAddAlarm Callback to navigate to 'Add Alarm' screen.
 * @param onAlarmClick Callback to navigate to 'Edit Alarm' screen.
 * @param onNavigateToBatteryOptimization Callback to navigate to battery optimization warning screen.
 */
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onAddAlarm: () -> Unit,
    onAlarmClick: (Alarm) -> Unit,
    onNavigateToBatteryOptimization: () -> Unit,
) {
    val alarms by viewModel.alarms.collectAsStateWithLifecycle(initialValue = emptyList())
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    // Permissions
    val locationPermissionState = rememberMultiplePermissionsState(
        listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
    )
    val backgroundLocationPermissionState =
        rememberPermissionState(Manifest.permission.ACCESS_BACKGROUND_LOCATION)

    // Notification Logic
    var notificationPermissionLaunchTime by remember { mutableLongStateOf(0L) }
    var preRationale by remember { mutableStateOf(false) }
    var pendingAlarm by remember { mutableStateOf<Alarm?>(null) }
    var alarmToDelete by remember { mutableStateOf<Alarm?>(null) }

    // Helper: Check location permission -> Enable Alarm
    val checkLocationAndEnableAlarm = { alarm: Alarm ->
        val hasLocationPermission =
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                backgroundLocationPermissionState.status.isGranted
            } else {
                locationPermissionState.allPermissionsGranted
            }

        if (hasLocationPermission) {
            viewModel.enableAlarm(alarm, alarms, context)
        } else {
            viewModel.showBackgroundPermissionDialog()
        }
    }

    // Permission Launcher
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            pendingAlarm?.let {
                checkLocationAndEnableAlarm(it)
                pendingAlarm = null
            }
        } else {
            val activity = context.findActivity() ?: return@rememberLauncherForActivityResult
            val showRationale =
                activity.shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)
            if (showRationale) {
                viewModel.showNotificationRationaleDialog()
            } else {
                viewModel.showNotificationPermissionDialog()
            }
            pendingAlarm = null
        }
    }

    // Unified Toggle Logic
    val handleAlarmToggle = { alarm: Alarm, isChecked: Boolean ->
        if (isChecked) {
            // 1. Check Notification Permission (Android 13+)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                val permission = Manifest.permission.POST_NOTIFICATIONS
                val isGranted = androidx.core.content.ContextCompat.checkSelfPermission(
                    context, permission
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED

                if (!isGranted) {
                    val activity = context.findActivity()
                    if (activity != null) {
                        pendingAlarm = alarm
                        preRationale = activity.shouldShowRequestPermissionRationale(permission)
                        notificationPermissionLaunchTime = System.currentTimeMillis()
                        notificationPermissionLauncher.launch(permission)
                    }
                } else {
                    // 2. Check Location -> Enable
                    checkLocationAndEnableAlarm(alarm)
                }
            } else {
                // < Android 13: Directly Check Location -> Enable
                checkLocationAndEnableAlarm(alarm)
            }
        } else {
            viewModel.disableAlarm(alarm, context)
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text(stringResource(R.string.home_title)) },
                actions = {
//                    Debug: Test notification button
//                    IconButton(onClick = {
//                        val serviceIntent = Intent(context, GeoAlarmService::class.java).apply {
//                            action = GeoAlarmService.ACTION_START_TEST
//                        }
//                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
//                            context.startForegroundService(serviceIntent)
//                        } else {
//                            context.startService(serviceIntent)
//                        }
//                    }) {
//                        Icon(
//                            Icons.Filled.PlayArrow,
//                            contentDescription = "Test Notification"
//                        )
//                    }
                    IconButton(onClick = { viewModel.showLanguageSheet() }) {
                        Icon(
                            Icons.Filled.Language,
                            contentDescription = stringResource(R.string.language),
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    // Add Alarm Permission Check
                    val hasLocationPermission =
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                            backgroundLocationPermissionState.status.isGranted
                        } else {
                            locationPermissionState.allPermissionsGranted
                        }

                    if (hasLocationPermission) {
                        onAddAlarm()
                    } else {
                        viewModel.showBackgroundPermissionDialog()
                    }
                },
            ) {
                Icon(
                    Icons.Filled.Add,
                    contentDescription = stringResource(R.string.add_alarm),
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            if (alarms.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        stringResource(R.string.no_alarms),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            } else {
                AlarmList(
                    alarms = alarms,
                    onAlarmClick = { alarm ->
                        if (alarm.isEnabled) {
                            viewModel.showEditDisabledDialog()
                        } else {
                            onAlarmClick(alarm)
                        }
                    },
                    onAlarmLongClick = { alarm ->
                        if (alarm.isEnabled) {
                            viewModel.showEditDisabledDialog()
                        } else {
                            alarmToDelete = alarm
                        }
                    },
                    onToggle = handleAlarmToggle,
                )
            }
        }
    }

    // Language Sheet
    if (uiState.showLanguageSheet) {
        val currentLocales = AppCompatDelegate.getApplicationLocales()
        val currentLanguage =
            if (!currentLocales.isEmpty) currentLocales.toLanguageTags().split("-")[0] else "en"

        ModalBottomSheet(onDismissRequest = { viewModel.dismissLanguageSheet() }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
            ) {
                ListItem(
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    headlineContent = { Text(stringResource(R.string.locale_zh)) },
                    trailingContent = {
                        if (currentLanguage == "zh") Icon(
                            Icons.Filled.Check, null
                        )
                    },
                    modifier = Modifier.clickable {
                        setAppLocale("zh-TW")
                        viewModel.dismissLanguageSheet()
                    },
                )
                ListItem(
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    headlineContent = { Text(stringResource(R.string.locale_en)) },
                    trailingContent = {
                        if (currentLanguage == "en") Icon(
                            Icons.Filled.Check, null
                        )
                    },
                    modifier = Modifier.clickable {
                        setAppLocale("en")
                        viewModel.dismissLanguageSheet()
                    },
                )
            }
        }
    }

    // Dialogs
    HomeDialogsContainer(
        uiState = uiState, viewModel = viewModel,
        onRetryNotificationPermission = {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                val activity = context.findActivity()
                if (activity != null) {
                    preRationale =
                        activity.shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)
                    notificationPermissionLaunchTime = System.currentTimeMillis()
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        },
        showDeleteDialog = alarmToDelete != null,
        onConfirmDelete = {
            alarmToDelete?.let { alarm ->
                scope.launch {
                    val msgAlarmDeleted = context.getString(R.string.alarm_deleted)
                    val labelUndo = context.getString(R.string.undo)
                    viewModel.deleteAlarm(alarm)
                    val result = snackbarHostState.showSnackbar(
                        message = msgAlarmDeleted,
                        actionLabel = labelUndo,
                        duration = SnackbarDuration.Short
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        viewModel.restoreAlarm(alarm)
                    }
                }
            }
            alarmToDelete = null
        },
        onDismissDelete = { alarmToDelete = null },
    )

    // Battery Optimization Check
    LaunchedEffect(alarms) {
        val anyEnabled = alarms.any { it.isEnabled }
        if (anyEnabled) {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            if (!powerManager.isIgnoringBatteryOptimizations(context.packageName)) {
                onNavigateToBatteryOptimization()
            }
        }
    }
}


/**
 * A list item representing a single alarm.
 *
 * @param alarm The alarm data object.
 * @param modifier Modifier for layout adjustments.
 * @param onClick Callback when the item body is clicked.
 * @param onLongClick Callback when the item body is long-clicked (for delete).
 * @param onToggle Callback when the switch is toggled.
 */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun AlarmItem(
    alarm: Alarm,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onToggle: (Boolean) -> Unit,
) {
    Box(
        modifier = modifier
            .background(MaterialTheme.colorScheme.background)
            .combinedClickable(
                onClick = onClick, onLongClick = onLongClick,
            ),
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                modifier = Modifier.weight(1f),
                text = alarm.name,
                style = MaterialTheme.typography.titleLarge,
            )

            Switch(
                checked = alarm.isEnabled, onCheckedChange = onToggle,
            )
        }
    }
}


/**
 * Updates the application locale.
 *
 * @param languageTag The IETF BCP 47 language tag string (e.g., "en", "zh-TW").
 */
private fun setAppLocale(languageTag: String) {
    val appLocale = LocaleListCompat.forLanguageTags(languageTag)
    AppCompatDelegate.setApplicationLocales(appLocale)
}

/**
 * Helper extension to find the [Activity] from a [Context].
 * Useful when working within Composables that might be wrapped in ContextWrappers.
 */
private fun Context.findActivity(): Activity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return null
}

/**
 * Displays the list of alarms.
 */
@Composable
private fun AlarmList(
    alarms: List<Alarm>,
    onAlarmClick: (Alarm) -> Unit,
    onAlarmLongClick: (Alarm) -> Unit,
    onToggle: (Alarm, Boolean) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 80.dp),
    ) {
        items(
            items = alarms, key = { it.id }) { alarm ->
            AlarmItem(
                alarm = alarm,
                onClick = { onAlarmClick(alarm) },
                onLongClick = { onAlarmLongClick(alarm) },
                onToggle = { isChecked -> onToggle(alarm, isChecked) },
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
        }
    }
}

/**
 * Container for all Home Screen dialogs.
 */
@Composable
private fun HomeDialogsContainer(
    uiState: com.example.geo_alarm.ui.viewmodel.HomeUiState,
    viewModel: HomeViewModel,
    onRetryNotificationPermission: () -> Unit,
    onConfirmDelete: () -> Unit,
    onDismissDelete: () -> Unit,
    showDeleteDialog: Boolean,
) {
    val context = LocalContext.current

    if (uiState.showEditDisabledDialog) {
        EditDisabledDialog(onDismiss = { viewModel.dismissEditDisabledDialog() })
    }

    if (uiState.showSingleAlarmDialog) {
        SingleAlarmDialog(onDismiss = { viewModel.dismissSingleAlarmDialog() })
    }

    if (uiState.showBackgroundPermissionDialog) {
        BackgroundLocationPermissionDialog(
            context = context, onDismiss = { viewModel.dismissBackgroundPermissionDialog() })
    }

    if (uiState.showNotificationPermissionDialog) {
        NotificationPermissionDialog(
            context = context, onDismiss = { viewModel.dismissNotificationPermissionDialog() })
    }

    if (uiState.showNotificationRationaleDialog) {
        NotificationRationaleDialog(
            onDismiss = { viewModel.dismissNotificationRationaleDialog() },
            onRetry = onRetryNotificationPermission
        )
    }

    if (uiState.showAlreadyAtDestinationDialog) {
        AlreadyAtDestinationDialog(onDismiss = { viewModel.dismissAlreadyAtDestinationDialog() })
    }

    // Delete Alarm Dialog
    if (showDeleteDialog) {
        DeleteAlarmDialog(
            onConfirm = onConfirmDelete, onDismiss = onDismissDelete,
        )
    }
}
