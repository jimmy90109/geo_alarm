package com.github.jimmy90109.geoalarm.ui.screens

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.jimmy90109.geoalarm.R
import com.github.jimmy90109.geoalarm.data.UpdateStatus
import com.github.jimmy90109.geoalarm.ui.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()


    // Current Language is pulled directly from the VM helper to ensure recomposition when changed? 
    // Actually AppCompatDelegate triggers recreation, but let's grab it for display consistency 
    // or arguably just keep using the VM's logic if it was a Flow. 
    // The previous implementation used LocalContext/AppCompatDelegate inside Composable. 
    // The VM implementation provides a getter. Since Activity recreation happens on locale change, 
    // reading it in composition is fine, but using VM helper might be cleaner for logic separation.
    // However, the VM property isn't a state, so it won't trigger updates if we just access it. 
    // But Activity recreation does the job.
    val currentLanguage = viewModel.currentLanguage

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
            val context = LocalContext.current
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
                    viewModel.resetUpdateState()
                }
            }

            if (showUpdateDialog) {
                val status = updateStatus
                if (status is UpdateStatus.Available) {
                    AlertDialog(
                        onDismissRequest = {
                            showUpdateDialog = false
                            viewModel.resetUpdateState()
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
                                    viewModel.downloadUpdate(status.downloadUrl)
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
                                    viewModel.resetUpdateState()
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
                            onLanguageClick = { viewModel.showLanguageSheet() })
                    }

                    // Right Column: About
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(androidx.compose.foundation.rememberScrollState())
                            .padding(bottom = 16.dp)
                    ) {
                        SettingsAboutSection(
                            updateStatus = updateStatus,
                            currentVersion = viewModel.currentVersion,
                            onUpdateClick = { status ->
                                when (status) {
                                    is UpdateStatus.Idle, is UpdateStatus.Error -> viewModel.checkForUpdates()
                                    is UpdateStatus.Available -> showUpdateDialog = true
                                    is UpdateStatus.ReadyToInstall -> viewModel.installUpdate(
                                        status.file, context
                                    )

                                    is UpdateStatus.Downloading -> {
                                        android.widget.Toast.makeText(
                                            context,
                                            R.string.update_downloading,
                                            android.widget.Toast.LENGTH_SHORT
                                        ).show()
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
                        onLanguageClick = { viewModel.showLanguageSheet() },
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    SettingsAboutSection(
                        updateStatus = updateStatus,
                        currentVersion = viewModel.currentVersion,
                        onUpdateClick = { status ->
                            when (status) {
                                is UpdateStatus.Idle, is UpdateStatus.Error -> viewModel.checkForUpdates()
                                is UpdateStatus.Available -> showUpdateDialog = true
                                is UpdateStatus.ReadyToInstall -> viewModel.installUpdate(
                                    status.file, context
                                )

                                is UpdateStatus.Downloading -> {
                                    android.widget.Toast.makeText(
                                        context,
                                        R.string.update_downloading,
                                        android.widget.Toast.LENGTH_SHORT
                                    ).show()
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
        androidx.compose.material3.ModalBottomSheet(
            onDismissRequest = { viewModel.dismissLanguageSheet() },
            sheetState = androidx.compose.material3.rememberModalBottomSheetState(),
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
                    onClick = { viewModel.setAppLocale("zh-TW") },
                )
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                SettingsSelectionItem(
                    text = stringResource(R.string.locale_en),
                    selected = currentLanguage == "en",
                    enabled = true,
                    onClick = { viewModel.setAppLocale("en") },
                )
            }
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
        value = if (currentLanguage == "zh") stringResource(R.string.locale_zh) else stringResource(
            R.string.locale_en
        ),
        onClick = onLanguageClick,
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
    Card(
        onClick = onClick, enabled = enabled,
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
    Row(
        Modifier
            .fillMaxWidth()
            .selectable(
                selected = selected, role = Role.RadioButton, onClick = onClick, enabled = enabled
            )
            .padding(
                vertical = 16.dp, horizontal = 24.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(
                    alpha = 0.38f
                )
            )
            if (description != null) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface.copy(
                        alpha = 0.38f
                    )
                )
            }
        }

        if (selected) {
            Icon(
                imageVector = Icons.Default.Check, contentDescription = null, // decorative
                tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 16.dp)
            )
        }
    }
}
