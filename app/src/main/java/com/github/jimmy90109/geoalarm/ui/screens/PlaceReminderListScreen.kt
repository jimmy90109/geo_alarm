package com.github.jimmy90109.geoalarm.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.Alignment
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
import com.github.jimmy90109.geoalarm.ui.screens.place_reminders.components.PlaceReminderCard
import com.github.jimmy90109.geoalarm.ui.screens.place_reminders.components.PlaceReminderEmptyState
import com.github.jimmy90109.geoalarm.ui.screens.place_reminders.components.PlaceReminderInfoSheet
import com.github.jimmy90109.geoalarm.ui.screens.place_reminders.components.PlaceReminderPermissionBanner
import com.github.jimmy90109.geoalarm.ui.screens.place_reminders.components.PlaceReminderSectionHeader
import com.github.jimmy90109.geoalarm.ui.viewmodel.PlaceReminderListViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PlaceReminderListScreen(
    viewModel: PlaceReminderListViewModel,
    onAddReminder: () -> Unit,
    onReminderClick: (String) -> Unit,
) {
    val reminders by viewModel.reminders.collectAsStateWithLifecycle()
    var permissionState by remember { mutableStateOf(viewModel.permissionState()) }
    var pendingEnableReminderId by remember { mutableStateOf<String?>(null) }
    var showBackgroundLocationDialog by remember { mutableStateOf(false) }
    var showReminderInfoSheet by remember { mutableStateOf(false) }
    val enabledReminders = reminders.filter { it.reminder.enabled }
    val savedReminders = reminders.filterNot { it.reminder.enabled }
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
        if (reminders.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = innerPadding.calculateTopPadding())
                    .padding(horizontal = 36.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(modifier = Modifier.weight(1f))
                PlaceReminderEmptyState(modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.weight(2f))
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(300.dp),
                modifier = Modifier
                    .padding(top = innerPadding.calculateTopPadding())
                    .fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 16.dp,
                    bottom = innerPadding.calculateBottomPadding() + 112.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (enabledReminders.isNotEmpty() && !permissionState.canEnableReminder) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        PlaceReminderPermissionBanner(
                            permissionState = permissionState,
                            onPrimaryAction = {
                                enabledReminders.firstOrNull()?.reminder?.id?.let(requestEnableReminder)
                            },
                        )
                    }
                }
                if (enabledReminders.isNotEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        PlaceReminderSectionHeader(text = stringResource(R.string.place_reminder_section_enabled))
                    }
                    items(enabledReminders, key = { it.reminder.id }) { reminder ->
                        PlaceReminderCard(
                            reminderWithItems = reminder,
                            onClick = { onReminderClick(reminder.reminder.id) },
                            onEnabledChange = { enabled ->
                                if (enabled) {
                                    requestEnableReminder(reminder.reminder.id)
                                } else {
                                    pendingEnableReminderId = null
                                    viewModel.setEnabled(reminder.reminder.id, false)
                                }
                            },
                        )
                    }
                }
                if (savedReminders.isNotEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        PlaceReminderSectionHeader(text = stringResource(R.string.place_reminder_section_saved))
                    }
                    items(savedReminders, key = { it.reminder.id }) { reminder ->
                        PlaceReminderCard(
                            reminderWithItems = reminder,
                            onClick = { onReminderClick(reminder.reminder.id) },
                            onEnabledChange = { enabled ->
                                if (enabled) {
                                    requestEnableReminder(reminder.reminder.id)
                                } else {
                                    pendingEnableReminderId = null
                                    viewModel.setEnabled(reminder.reminder.id, false)
                                }
                            },
                        )
                    }
                }
            }
        }
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
