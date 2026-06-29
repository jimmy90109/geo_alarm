package com.github.jimmy90109.geoalarm.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.github.jimmy90109.geoalarm.R
import com.github.jimmy90109.geoalarm.data.PlaceReminderWithItems
import com.github.jimmy90109.geoalarm.ui.components.AlarmIconBadge
import com.github.jimmy90109.geoalarm.ui.components.BackgroundLocationPermissionDialog
import com.github.jimmy90109.geoalarm.ui.viewmodel.PlaceReminderListViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class, ExperimentalLayoutApi::class)
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
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text(stringResource(R.string.place_reminders_title)) },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->
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
            if (reminders.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    PlaceReminderEmptyState(onAddReminder = onAddReminder)
                }
            } else {
                items(reminders, key = { it.reminder.id }) { reminder ->
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

    if (showBackgroundLocationDialog) {
        BackgroundLocationPermissionDialog(
            context = context,
            onDismiss = {
                pendingEnableReminderId = null
                showBackgroundLocationDialog = false
            },
            onOpenSettings = {
                showBackgroundLocationDialog = false
            },
        )
    }
}

@Composable
private fun PlaceReminderEmptyState(onAddReminder: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                Icons.Filled.LocationOn,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = stringResource(R.string.place_reminder_empty_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = stringResource(R.string.place_reminder_empty_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(onClick = onAddReminder) {
                Text(stringResource(R.string.place_reminder_empty_cta))
            }
        }
    }
}

@Composable
private fun PlaceReminderCard(
    reminderWithItems: PlaceReminderWithItems,
    onClick: () -> Unit,
    onEnabledChange: (Boolean) -> Unit,
) {
    val reminder = reminderWithItems.reminder
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AlarmIconBadge(iconKey = reminder.iconKey)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = reminder.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = reminder.placeName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = triggerText(reminder.triggerType, reminder.dwellMinutes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = reminderSummary(reminderWithItems),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                reminder.lastTriggeredAt?.let {
                    Text(
                        text = stringResource(R.string.place_reminder_last_triggered, formatTime(it)),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Switch(
                checked = reminder.enabled,
                onCheckedChange = onEnabledChange,
            )
        }
    }
}

