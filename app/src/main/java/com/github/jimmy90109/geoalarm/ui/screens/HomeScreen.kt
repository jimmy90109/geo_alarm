package com.github.jimmy90109.geoalarm.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.PowerManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.jimmy90109.geoalarm.R
import com.github.jimmy90109.geoalarm.data.Alarm
import com.github.jimmy90109.geoalarm.ui.components.AlreadyAtDestinationDialog
import com.github.jimmy90109.geoalarm.ui.components.BackgroundLocationPermissionDialog
import com.github.jimmy90109.geoalarm.ui.components.DeleteAlarmDialog
import com.github.jimmy90109.geoalarm.ui.components.EditDisabledDialog
import com.github.jimmy90109.geoalarm.ui.components.NotificationPermissionDialog
import com.github.jimmy90109.geoalarm.ui.components.NotificationRationaleDialog
import com.github.jimmy90109.geoalarm.ui.components.SingleAlarmDialog
import com.github.jimmy90109.geoalarm.ui.viewmodel.HomeUiState
import com.github.jimmy90109.geoalarm.ui.viewmodel.HomeViewModel
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
    onNavigateToBatteryOptimization: () -> Unit
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

    // Check for active alarm
    val activeAlarm = alarms.find { it.isEnabled }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.home_title),
                        modifier = Modifier.fillMaxWidth(),
                    )
                },
                scrollBehavior = scrollBehavior,
            )
        },
        floatingActionButton = {
            // Only show FAB if no alarm is active
            if (activeAlarm == null) {
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
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Box {
            AnimatedContent(
                targetState = activeAlarm,
                transitionSpec = {
                    if (targetState != null) {
                        // Entering Active Mode: Slide in from Left
                        (slideInHorizontally { -it } + fadeIn()).togetherWith(
                            slideOutHorizontally { it } + fadeOut())
                    } else {
                        // Exiting Active Mode: Slide out to Left
                        (slideInHorizontally { it } + fadeIn()).togetherWith(
                            slideOutHorizontally { -it } + fadeOut())
                    }
                },
                label = "ActiveAlarmTransition"
            ) { targetAlarm ->
                if (targetAlarm != null) {
                    ActiveAlarmScreen(
                        modifier = Modifier.padding(top = innerPadding.calculateTopPadding()),
                        alarm = targetAlarm,
                        progress = uiState.monitoringProgress,
                        distanceMeters = uiState.monitoringDistance,
                        onStopAlarm = { viewModel.disableAlarm(targetAlarm, context) }
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize()) {
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
                                // Add extra padding at bottom for the floating bar
                                contentPadding = PaddingValues(
                                    top = innerPadding.calculateTopPadding() + 16.dp,
                                    bottom = innerPadding.calculateBottomPadding() + 100.dp,
                                    start = 16.dp,
                                    end = 16.dp,
                                ),
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
    contentPadding: PaddingValues = PaddingValues(bottom = 80.dp),
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(300.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(
            items = alarms, key = { it.id }) { alarm ->
            AlarmItem(
                alarm = alarm,
                onClick = { onAlarmClick(alarm) },
                onLongClick = { onAlarmLongClick(alarm) },
                onToggle = { isChecked -> onToggle(alarm, isChecked) },
            )
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
    Card(
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        modifier = modifier
            .fillMaxWidth()
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
 * Container for all Home Screen dialogs.
 */
@Composable
private fun HomeDialogsContainer(
    uiState: HomeUiState,
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
