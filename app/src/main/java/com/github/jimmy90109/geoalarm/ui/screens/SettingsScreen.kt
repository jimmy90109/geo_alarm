package com.github.jimmy90109.geoalarm.ui.screens

import android.app.Activity
import android.content.Intent
import android.content.res.Configuration
import android.media.RingtoneManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.core.graphics.drawable.toBitmap
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.github.jimmy90109.geoalarm.R
import com.github.jimmy90109.geoalarm.data.PaymentShortcut
import com.github.jimmy90109.geoalarm.data.RingtoneSettings
import com.github.jimmy90109.geoalarm.data.UpdateStatus
import com.github.jimmy90109.geoalarm.ui.viewmodel.SettingsAction
import com.github.jimmy90109.geoalarm.ui.viewmodel.SettingsEffect
import com.github.jimmy90109.geoalarm.ui.viewmodel.SettingsViewModel
import com.github.jimmy90109.geoalarm.utils.AudioUtils
import com.github.jimmy90109.geoalarm.utils.PaymentShortcutNotifier

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val ringtoneSettings by viewModel.ringtoneSettings.collectAsStateWithLifecycle()
    val paymentShortcut by viewModel.paymentShortcut.collectAsStateWithLifecycle()
    val analyticsEnabled by viewModel.analyticsEnabled.collectAsStateWithLifecycle()
    val currentLanguage = viewModel.currentLanguage
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val ringtonePickerTitle = stringResource(R.string.ringtone_select)

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is SettingsEffect.OpenIntent -> context.startActivity(effect.intent)
            }
        }
    }

    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.onAction(SettingsAction.PendingInstallRetryRequested(context))
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Ringtone picker launcher
    val ringtonePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.getParcelableExtra<android.net.Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            val uriString = uri?.toString()
            val name = if (uriString != null) AudioUtils.getRingtoneName(context, uriString) else null
            viewModel.onAction(SettingsAction.RingtoneSelected(uriString, name))
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeFlexibleTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.settings),
                        modifier = Modifier.fillMaxWidth(),
                    )
                },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(top = innerPadding.calculateTopPadding())
                .fillMaxSize(),
            contentAlignment = Alignment.TopCenter
        ) {
            val configuration = LocalConfiguration.current
            val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

            // Shared Logic for About Section
            val updateStatus by viewModel.updateStatus.collectAsStateWithLifecycle()
            var showUpdateDialog by remember { mutableStateOf(false) }

            // Handle status changes (e.g. show dialog when Available detected)
            LaunchedEffect(updateStatus) {
                if (updateStatus is UpdateStatus.Available) {
                    showUpdateDialog = true
                }
                if (updateStatus is UpdateStatus.Error) {
                    android.widget.Toast.makeText(
                        context,
                        (updateStatus as UpdateStatus.Error).message,
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                    viewModel.onAction(SettingsAction.UpdateStateReset)
                }
            }

            if (showUpdateDialog) {
                val status = updateStatus
                if (status is UpdateStatus.Available) {
                    AlertDialog(
                        onDismissRequest = {
                            showUpdateDialog = false
                            viewModel.onAction(SettingsAction.UpdateStateReset)
                        },
                        title = { Text(stringResource(R.string.update_available_title)) },
                        text = {
                            Text(
                                stringResource(
                                    R.string.update_available_message,
                                    status.version,
                                )
                            )
                        },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    viewModel.onAction(
                                        SettingsAction.UpdateDownloadRequested(
                                            status.downloadUrl,
                                            status.sha256
                                        )
                                    )
                                    showUpdateDialog = false
                                },
                            ) {
                                Text(stringResource(R.string.download))
                            }
                        },
                        dismissButton = {
                            TextButton(
                                onClick = {
                                    showUpdateDialog = false
                                    viewModel.onAction(SettingsAction.UpdateStateReset)
                                },
                            ) {
                                Text(stringResource(R.string.cancel))
                            }
                        },
                    )
                }
            }

            if (isLandscape) {
                // LANDSCAPE: Two Columns
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Left Column: General
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(androidx.compose.foundation.rememberScrollState())
                            .padding(bottom = 16.dp) // Less padding needed than portrait
                    ) {
                        SettingsGeneralSection(
                            currentLanguage = currentLanguage,
                            onLanguageClick = { viewModel.onAction(SettingsAction.LanguageSheetRequested) })
                        Spacer(modifier = Modifier.height(16.dp))
                        SettingsAlarmSection(
                            ringtoneSettings = ringtoneSettings,
                            paymentShortcut = paymentShortcut,
                            onRingtoneClick = { viewModel.onAction(SettingsAction.RingtoneSheetRequested) },
                            onPaymentShortcutClick = {
                                viewModel.onAction(SettingsAction.PaymentShortcutSheetRequested)
                            },
                        )
                    }

                    // Right Column: About
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(androidx.compose.foundation.rememberScrollState())
                            .padding(bottom = 16.dp)
                    ) {
                        SettingsPrivacySection(
                            analyticsEnabled = analyticsEnabled,
                            onPrivacyClick = { viewModel.onAction(SettingsAction.AnalyticsSheetRequested) }
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        SettingsAboutSection(
                            updateStatus = updateStatus,
                            currentVersion = viewModel.currentVersion,
                            onUpdateClick = { status ->
                                when (status) {
                                    is UpdateStatus.Idle, is UpdateStatus.Error -> viewModel.onAction(SettingsAction.UpdateCheckRequested)
                                    is UpdateStatus.Available -> showUpdateDialog = true
                                    is UpdateStatus.ReadyToInstall -> viewModel.onAction(
                                        SettingsAction.UpdateInstallRequested(status.apkUri, context)
                                    )
                                    is UpdateStatus.Downloading -> {
                                        android.widget.Toast.makeText(context, R.string.update_downloading, android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                    else -> {}
                                }
                            },
                        )
                    }
                }
            } else {
                // PORTRAIT: Single Column
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 600.dp)
                        .verticalScroll(androidx.compose.foundation.rememberScrollState())
                        .padding(16.dp)
                        .padding(bottom = 100.dp),
                ) {
                    SettingsGeneralSection(
                        currentLanguage = currentLanguage,
                        onLanguageClick = { viewModel.onAction(SettingsAction.LanguageSheetRequested) },
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    SettingsAlarmSection(
                        ringtoneSettings = ringtoneSettings,
                        paymentShortcut = paymentShortcut,
                        onRingtoneClick = { viewModel.onAction(SettingsAction.RingtoneSheetRequested) },
                        onPaymentShortcutClick = {
                            viewModel.onAction(SettingsAction.PaymentShortcutSheetRequested)
                        },
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    SettingsPrivacySection(
                        analyticsEnabled = analyticsEnabled,
                        onPrivacyClick = { viewModel.onAction(SettingsAction.AnalyticsSheetRequested) }
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    SettingsAboutSection(
                        updateStatus = updateStatus,
                        currentVersion = viewModel.currentVersion,
                        onUpdateClick = { status ->
                            when (status) {
                                is UpdateStatus.Idle, is UpdateStatus.Error -> viewModel.onAction(SettingsAction.UpdateCheckRequested)
                                is UpdateStatus.Available -> showUpdateDialog = true
                                is UpdateStatus.ReadyToInstall -> viewModel.onAction(
                                    SettingsAction.UpdateInstallRequested(status.apkUri, context)
                                )
                                is UpdateStatus.Downloading -> {
                                    android.widget.Toast.makeText(context, R.string.update_downloading, android.widget.Toast.LENGTH_SHORT).show()
                                }
                                else -> {}
                            }
                        },
                    )
                }
            }
        }
    }

    // Language Bottom Sheet
    if (uiState.showLanguageSheet) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.onAction(SettingsAction.LanguageSheetDismissed) },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        ) {
            Text(
                text = stringResource(R.string.language),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(start = 24.dp, bottom = 16.dp)
            )
            Column(
                modifier = Modifier
                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainer)
            ) {
                SettingsSelectionItem(
                    text = stringResource(R.string.locale_zh),
                    selected = currentLanguage == "zh",
                    enabled = true,
                    onClick = { viewModel.onAction(SettingsAction.LocaleSelected("zh-TW")) },
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                SettingsSelectionItem(
                    text = stringResource(R.string.locale_en),
                    selected = currentLanguage == "en",
                    enabled = true,
                    onClick = { viewModel.onAction(SettingsAction.LocaleSelected("en")) },
                )
            }
        }
    }

    // Ringtone Settings Bottom Sheet
    if (uiState.showRingtoneSheet) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.onAction(SettingsAction.RingtoneSheetDismissed) },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        ) {
            // Header with Switch
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 24.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.settings_ringtone),
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Text(
                        text = stringResource(R.string.ringtone_feature_description),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = ringtoneSettings.enabled,
                    onCheckedChange = { viewModel.onAction(SettingsAction.RingtoneEnabledChanged(it)) }
                )
            }

            // Ringtone selection
            Column(
                modifier = Modifier
                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainer)
            ) {
                // Default ringtone option with play button
                RingtoneSelectionItem(
                    text = stringResource(R.string.ringtone_default),
                    selected = ringtoneSettings.ringtoneUri == null,
                    enabled = ringtoneSettings.enabled,
                    isPlaying = uiState.isPreviewPlaying && uiState.isPreviewingDefault,
                    onPlayClick = {
                        if (uiState.isPreviewPlaying && uiState.isPreviewingDefault) {
                            viewModel.onAction(SettingsAction.PreviewStopRequested(context))
                        } else {
                            viewModel.onAction(
                                SettingsAction.PreviewPlayRequested(
                                    context,
                                    null,
                                    isDefault = true
                                )
                            )
                        }
                    },
                    onClick = { viewModel.onAction(SettingsAction.RingtoneSelected(null, null)) },
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                // Custom ringtone option with play button
                RingtoneSelectionItem(
                    text = ringtoneSettings.ringtoneName ?: stringResource(R.string.ringtone_select),
                    selected = ringtoneSettings.ringtoneUri != null,
                    enabled = ringtoneSettings.enabled,
                    isPlaying = uiState.isPreviewPlaying && !uiState.isPreviewingDefault,
                    onPlayClick = if (ringtoneSettings.ringtoneUri != null) {
                        {
                            if (uiState.isPreviewPlaying && !uiState.isPreviewingDefault) {
                                viewModel.onAction(SettingsAction.PreviewStopRequested(context))
                            } else {
                                viewModel.onAction(
                                    SettingsAction.PreviewPlayRequested(
                                        context,
                                        ringtoneSettings.ringtoneUri,
                                        isDefault = false
                                    )
                                )
                            }
                        }
                    } else null,
                    onClick = {
                        val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                            putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_RINGTONE)
                            putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, ringtonePickerTitle)
                            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                            if (ringtoneSettings.ringtoneUri != null) {
                                putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, android.net.Uri.parse(ringtoneSettings.ringtoneUri))
                            }
                        }
                        ringtonePickerLauncher.launch(intent)
                    },
                )
            }
        }
    }

    if (uiState.showPaymentShortcutSheet) {
        PaymentShortcutBottomSheet(
            selectedShortcut = paymentShortcut,
            onSelected = { viewModel.onAction(SettingsAction.PaymentShortcutSelected(it)) },
            onPreview = { PaymentShortcutNotifier.show(context, it) },
            onDismiss = { viewModel.onAction(SettingsAction.PaymentShortcutSheetDismissed) },
        )
    }

    // Analytics Settings Bottom Sheet
    if (uiState.showAnalyticsSheet) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.onAction(SettingsAction.AnalyticsSheetDismissed) },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 24.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    modifier = Modifier.weight(1f),
                    text = stringResource(R.string.analytics_help_improve_title),
                    style = MaterialTheme.typography.titleLarge,
                )

                Switch(
                    checked = analyticsEnabled,
                    onCheckedChange = { viewModel.onAction(SettingsAction.AnalyticsEnabledChanged(it)) }
                )
            }

            Text(
                modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 16.dp),
                text = stringResource(R.string.analytics_help_improve_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SettingsGeneralSection(
    currentLanguage: String, onLanguageClick: () -> Unit
) {
    SettingsSectionHeader(title = stringResource(R.string.settings_section_general))
    SettingsCard(
        title = stringResource(R.string.language),
        value = if (currentLanguage == "zh") stringResource(R.string.locale_zh) else stringResource(R.string.locale_en),
        onClick = onLanguageClick,
    )
}

@Composable
private fun SettingsAlarmSection(
    ringtoneSettings: RingtoneSettings,
    paymentShortcut: PaymentShortcut?,
    onRingtoneClick: () -> Unit,
    onPaymentShortcutClick: () -> Unit,
) {
    SettingsSectionHeader(title = stringResource(R.string.settings_section_alarm))
    
    // Ringtone card
    SettingsCard(
        title = stringResource(R.string.settings_ringtone),
        value = if (ringtoneSettings.enabled) {
            ringtoneSettings.ringtoneName ?: stringResource(R.string.ringtone_default)
        } else {
            stringResource(R.string.ringtone_mode_none)
        },
        onClick = onRingtoneClick,
    )
    Spacer(modifier = Modifier.height(8.dp))
    SettingsCard(
        title = stringResource(R.string.settings_payment_shortcut),
        value = paymentShortcut?.displayName ?: stringResource(R.string.payment_shortcut_off),
        onClick = onPaymentShortcutClick,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentShortcutBottomSheet(
    selectedShortcut: PaymentShortcut?,
    onSelected: (PaymentShortcut?) -> Unit,
    onPreview: (PaymentShortcut) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        val context = LocalContext.current
        val installedShortcuts = remember {
            PaymentShortcut.entries.filter {
                context.packageManager.getLaunchIntentForPackage(it.packageName) != null
            }
        }
        val effectiveShortcut = selectedShortcut?.takeIf { it in installedShortcuts }
        val enabled = effectiveShortcut != null

        LaunchedEffect(selectedShortcut, installedShortcuts) {
            if (selectedShortcut != null && selectedShortcut !in installedShortcuts) {
                onSelected(null)
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 16.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.settings_payment_shortcut),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f)
            )
            Switch(
                checked = enabled,
                enabled = installedShortcuts.isNotEmpty(),
                onCheckedChange = { checked ->
                    onSelected(if (checked) effectiveShortcut ?: installedShortcuts.first() else null)
                },
            )
        }
        Text(
            text = stringResource(R.string.payment_shortcut_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 16.dp)
        )
        if (installedShortcuts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(148.dp)
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.payment_shortcut_no_installed_apps),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            ) {
                val columns = when {
                    maxWidth >= 420.dp -> 5
                    maxWidth >= 340.dp -> 3
                    else -> 2
                }
                val rows = remember(installedShortcuts, columns) { installedShortcuts.chunked(columns) }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    rows.forEach { rowShortcuts ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(66.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            rowShortcuts.forEach { shortcut ->
                                Box(modifier = Modifier.weight(1f)) {
                                    PaymentShortcutGridCard(
                                        shortcut = shortcut,
                                        selected = effectiveShortcut == shortcut,
                                        enabled = true,
                                        onClick = { onSelected(shortcut) },
                                        loadIcon = {
                                            runCatching {
                                                context.packageManager
                                                    .getApplicationIcon(shortcut.packageName)
                                                    .toBitmap(width = 96, height = 96)
                                                    .asImageBitmap()
                                            }.getOrNull()
                                        },
                                    )
                                }
                            }
                            repeat(columns - rowShortcuts.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
        Button(
            enabled = effectiveShortcut != null,
            onClick = { effectiveShortcut?.let(onPreview) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 16.dp)
                .height(52.dp),
            shape = RoundedCornerShape(percent = 50),
        ) {
            Text(stringResource(R.string.preview))
        }
    }
}

@Composable
private fun PaymentShortcutGridCard(
    shortcut: PaymentShortcut,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    loadIcon: () -> ImageBitmap?,
) {
    val haptic = LocalHapticFeedback.current
    val icon = remember(shortcut.packageName) { loadIcon() }
    val shape = RoundedCornerShape(16.dp)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxSize()
            .clip(shape)
            .clickable(enabled = enabled) {
                haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                onClick()
            },
        colors = CardDefaults.cardColors(
            containerColor = when {
                !enabled -> MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.58f)
                selected -> MaterialTheme.colorScheme.primaryContainer
                else -> MaterialTheme.colorScheme.surfaceContainer
            }
        ),
        border = if (selected) {
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else {
            null
        },
        shape = shape,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
        ) {
            Row(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (icon != null) {
                    Image(
                        bitmap = icon,
                        contentDescription = null,
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Fit,
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.secondaryContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountBalanceWallet,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                    }
                }
                Text(
                    text = shortcut.displayName,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f),
                    color = if (enabled) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    },
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (selected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(18.dp),
                )
            }
        }
    }
}

@Composable
private fun SettingsPrivacySection(
    analyticsEnabled: Boolean,
    onPrivacyClick: () -> Unit
) {
    SettingsSectionHeader(title = stringResource(R.string.settings_section_privacy_improvement))
    SettingsCard(
        title = stringResource(R.string.analytics_help_improve_title),
        value = if (analyticsEnabled) {
            stringResource(R.string.analytics_enabled)
        } else {
            stringResource(R.string.analytics_disabled)
        },
        onClick = onPrivacyClick
    )
}

@Composable
private fun SettingsAboutSection(
    updateStatus: UpdateStatus, currentVersion: String, onUpdateClick: (UpdateStatus) -> Unit
) {
    SettingsSectionHeader(title = stringResource(R.string.section_about))

    val updateValue = when (updateStatus) {
        is UpdateStatus.Checking -> stringResource(R.string.checking_update)
        is UpdateStatus.Downloading -> stringResource(R.string.update_downloading)
        is UpdateStatus.ReadyToInstall -> stringResource(R.string.update_ready_to_install)
        else -> stringResource(R.string.settings_version_label, currentVersion)
    }

    SettingsCard(
        title = stringResource(R.string.check_for_updates),
        value = updateValue,
        onClick = { onUpdateClick(updateStatus) },
    )
}

@Composable
fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
    )
}

@Composable
fun SettingsCard(
    title: String,
    value: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    subtitle: String? = null
) {
    val haptic = LocalHapticFeedback.current
    Card(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
            onClick()
        },
        enabled = enabled,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.6f)
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun SettingsSelectionItem(
    text: String,
    description: String? = null,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    Row(
        Modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                role = Role.RadioButton,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                    onClick()
                },
                enabled = enabled
            )
            .padding(vertical = 16.dp, horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            )
            if (description != null) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                )
            }
        }
        if (selected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.38f),
                modifier = Modifier.padding(start = 16.dp)
            )
        }
    }
}

@Composable
fun RingtoneSelectionItem(
    text: String,
    selected: Boolean,
    enabled: Boolean,
    isPlaying: Boolean,
    onPlayClick: (() -> Unit)?,
    onClick: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    Row(
        Modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                role = Role.RadioButton,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                    onClick()
                },
                enabled = enabled
            )
            .padding(start = 8.dp, end = 24.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Play button on the left
        IconButton(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                onPlayClick?.invoke()
            },
            enabled = enabled && onPlayClick != null
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                contentDescription = null,
                tint = if (enabled && onPlayClick != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            )
        }
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
            modifier = Modifier.weight(1f)
        )
        if (selected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.38f),
                modifier = Modifier.padding(start = 16.dp)
            )
        }
    }
}
