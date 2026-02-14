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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import com.github.jimmy90109.geoalarm.R
import com.github.jimmy90109.geoalarm.data.Alarm
import com.github.jimmy90109.geoalarm.data.ScheduleWithAlarm
import com.github.jimmy90109.geoalarm.ui.components.AlarmList
import com.github.jimmy90109.geoalarm.ui.components.AlreadyAtDestinationDialog
import com.github.jimmy90109.geoalarm.ui.components.BackgroundLocationPermissionDialog
import com.github.jimmy90109.geoalarm.ui.components.DeleteErrorDialog
import com.github.jimmy90109.geoalarm.ui.components.EditDisabledDialog
import com.github.jimmy90109.geoalarm.ui.components.HomeFabMenu
import com.github.jimmy90109.geoalarm.ui.components.NotificationPermissionDialog
import com.github.jimmy90109.geoalarm.ui.components.NotificationRationaleDialog
import com.github.jimmy90109.geoalarm.ui.components.ScheduleConflictDialog
import com.github.jimmy90109.geoalarm.ui.components.SingleAlarmDialog
import com.github.jimmy90109.geoalarm.ui.viewmodel.HomeUiState
import com.github.jimmy90109.geoalarm.ui.viewmodel.HomeViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.accompanist.permissions.rememberPermissionState

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalPermissionsApi::class,
    ExperimentalMaterial3ExpressiveApi::class
)
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
    onAddSchedule: () -> Unit,
    onScheduleClick: (ScheduleWithAlarm) -> Unit,
    onNavigateToBatteryOptimization: () -> Unit,
    onOpenOnboarding: () -> Unit
) {
    val alarms by viewModel.alarms.collectAsStateWithLifecycle(initialValue = emptyList())
    val schedules by viewModel.schedules.collectAsStateWithLifecycle(initialValue = emptyList())
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
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

    // FAB Menu State
    var showFabMenu by remember { mutableStateOf(false) }

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
            LargeFlexibleTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.home_title),
                        modifier = Modifier.fillMaxWidth(),
                    )
                },
                scrollBehavior = scrollBehavior,
                actions = {
//                    // Debug test button
//                    androidx.compose.material3.TextButton(
//                        onClick = {
//                            haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
//                            viewModel.startTestAlarm(context)
//                        }
//                    ) {
//                        Text("🧪 Test Alarm (10s)")
//                    },
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                            onOpenOnboarding()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = stringResource(R.string.open_onboarding)
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            // Only show FAB if no alarm is active
            if (activeAlarm == null) {
                // FAB Menu (Expressive)
                HomeFabMenu(
                    modifier = Modifier.offset(y = 16.dp),
                    expanded = showFabMenu,
                    onToggle = { showFabMenu = !showFabMenu },
                    alarms = alarms,
                    onAddSchedule = {
                        showFabMenu = false
                        onAddSchedule()
                    },
                    onAddAlarm = {
                        showFabMenu = false
                        // Add Alarm Permission Check Logic
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
                    })
            }
        },

        ) { innerPadding ->
        Box {
            // Scrim for FAB Menu
            if (showFabMenu) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() },
                        ) {
                            showFabMenu = false
                        }
                        .zIndex(1f) // Ensure it sits above content but below FAB if FAB is in Scaffold (it is)
                )
            }

            AnimatedContent(
                targetState = activeAlarm,
                transitionSpec = {
                    if (targetState != null) {
                        // Entering Active Mode: Slide in from Left
                        (slideInHorizontally { -it } + fadeIn()).togetherWith(slideOutHorizontally { it } + fadeOut())
                    } else {
                        // Exiting Active Mode: Slide out to Left
                        (slideInHorizontally { it } + fadeIn()).togetherWith(slideOutHorizontally { -it } + fadeOut())
                    }
                },
                label = "ActiveAlarmTransition",
            ) { targetAlarm ->
                if (targetAlarm != null) {
                    ActiveAlarmScreen(
                        modifier = Modifier.padding(top = innerPadding.calculateTopPadding()),
                        alarm = targetAlarm,
                        progress = uiState.monitoringProgress,
                        distanceMeters = uiState.monitoringDistance,
                        onStopAlarm = {
                            haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                            viewModel.disableAlarm(targetAlarm, context)
                        },
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize()) {
                        if (alarms.isEmpty() && schedules.isEmpty()) {
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
                                schedules = schedules,
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
                                onToggleAlarm = handleAlarmToggle,
                                onScheduleClick = { schedule -> onScheduleClick(schedule) },
                                onToggleSchedule = { schedule, isEnabled ->
                                    viewModel.toggleSchedule(schedule, isEnabled)
                                },
                                onAddSchedule = onAddSchedule,
                                highlightedAlarmId = uiState.highlightedAlarmId,
                                highlightedScheduleId = uiState.highlightedScheduleId,
                                onHighlightFinished = { viewModel.clearHighlight() },
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
 * Container for all Home Screen dialogs.
 */
@Composable
private fun HomeDialogsContainer(
    uiState: HomeUiState,
    viewModel: HomeViewModel,
    onRetryNotificationPermission: () -> Unit,
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

    // Delete Error Dialog
    if (uiState.showDeleteErrorDialog) {
        DeleteErrorDialog(onDismiss = { viewModel.dismissDeleteErrorDialog() })
    }

    // Schedule Conflict Dialog
    if (uiState.showScheduleConflictDialog) {
        ScheduleConflictDialog(
            onConfirm = { viewModel.confirmScheduleConflict() },
            onDismiss = { viewModel.dismissScheduleConflictDialog() })
    }
}
