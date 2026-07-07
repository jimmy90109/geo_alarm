package com.github.jimmy90109.geoalarm.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.jimmy90109.geoalarm.R
import com.github.jimmy90109.geoalarm.ui.components.BackgroundLocationPermissionDialog
import com.github.jimmy90109.geoalarm.ui.screens.place_reminders.components.PlaceReminderInfoSheet
import com.github.jimmy90109.geoalarm.ui.screens.place_reminders.components.PlaceReminderListContent
import com.github.jimmy90109.geoalarm.ui.viewmodel.PlaceReminderListViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PlaceReminderListScreen(
    viewModel: PlaceReminderListViewModel,
    onAddReminder: () -> Unit,
    onReminderClick: (String) -> Unit,
) {
    val listState by viewModel.listState.collectAsStateWithLifecycle()
    var permissionState by remember { mutableStateOf(viewModel.permissionState()) }
    var pendingEnableReminderId by remember { mutableStateOf<String?>(null) }
    var showBackgroundLocationDialog by remember { mutableStateOf(false) }
    var showReminderInfoSheet by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    lateinit var requestEnableReminder: (String) -> Unit
    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        permissionState = viewModel.permissionState()
        if (permissionState.hasNotifications) {
            pendingEnableReminderId?.let { requestEnableReminder(it) }
        } else {
            pendingEnableReminderId = null
        }
    }
    val locationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        permissionState = viewModel.permissionState()
        if (permissionState.hasPreciseLocation) {
            pendingEnableReminderId?.let { requestEnableReminder(it) }
        } else {
            pendingEnableReminderId = null
        }
    }

    fun refreshPermissions() {
        permissionState = viewModel.permissionState()
    }

    requestEnableReminder = { reminderId ->
        refreshPermissions()
        when {
            !permissionState.hasPreciseLocation -> {
                pendingEnableReminderId = reminderId
                locationLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                    )
                )
            }
            !permissionState.hasNotifications -> {
                pendingEnableReminderId = reminderId
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
            !permissionState.hasBackgroundLocation -> {
                pendingEnableReminderId = reminderId
                showBackgroundLocationDialog = true
            }
            !permissionState.isLocationServiceEnabled -> {
                pendingEnableReminderId = reminderId
                context.openLocationSettings()
            }
            else -> {
                pendingEnableReminderId = null
                viewModel.setEnabled(reminderId, true)
            }
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshPermissions()
                pendingEnableReminderId?.let { reminderId ->
                    if (permissionState.canEnableReminder) {
                        pendingEnableReminderId = null
                        viewModel.setEnabled(reminderId, true)
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text(stringResource(R.string.place_reminders_title)) },
                actions = {
                    IconButton(onClick = { showReminderInfoSheet = true }) {
                        Icon(
                            Icons.AutoMirrored.Outlined.HelpOutline,
                            contentDescription = stringResource(R.string.place_reminder_help_title),
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->
        PlaceReminderListContent(
            state = listState,
            permissionState = permissionState,
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = innerPadding.calculateTopPadding() + 16.dp,
                bottom = innerPadding.calculateBottomPadding() + 112.dp,
            ),
            loadingTopPadding = PaddingValues(
                top = innerPadding.calculateTopPadding() + 24.dp,
            ),
            emptyTopPadding = PaddingValues(
                top = innerPadding.calculateTopPadding(),
            ),
            onPermissionPrimaryAction = {
                listState.reminders.firstOrNull { it.reminder.enabled }?.reminder?.id
                    ?.let(requestEnableReminder)
            },
            onReminderClick = onReminderClick,
            onReminderEnabledChange = { reminderId, enabled ->
                if (enabled) {
                    requestEnableReminder(reminderId)
                } else {
                    pendingEnableReminderId = null
                    viewModel.setEnabled(reminderId, false)
                }
            },
            modifier = Modifier.fillMaxSize(),
        )
    }

    if (showBackgroundLocationDialog) {
        BackgroundLocationPermissionDialog(
            context = context,
            onDismiss = {
                pendingEnableReminderId = null
                showBackgroundLocationDialog = false
            },
            onOpenSettings = { showBackgroundLocationDialog = false },
        )
    }

    if (showReminderInfoSheet) {
        PlaceReminderInfoSheet(onDismissRequest = { showReminderInfoSheet = false })
    }
}
