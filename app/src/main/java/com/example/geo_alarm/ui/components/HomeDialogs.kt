package com.example.geo_alarm.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.geo_alarm.R

/**
 * Dialog shown when trying to edit an enabled alarm.
 */
@Composable
fun EditDisabledDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.edit_alarm)) },
        text = { Text(stringResource(R.string.edit_disabled_error)) },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text(stringResource(R.string.ok))
            }
        },
    )
}

/**
 * Dialog shown when trying to enable a second alarm (when one is already active).
 */
@Composable
fun SingleAlarmDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.single_alarm_title)) },
        text = { Text(stringResource(R.string.only_one_alarm_error)) },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text(stringResource(R.string.ok))
            }
        },
    )
}

/**
 * Dialog explaining why background location permission is needed.
 * Guides the user to system settings.
 */
@Composable
fun BackgroundLocationPermissionDialog(
    context: Context, onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.background_location_title)) },
        text = { Text(stringResource(R.string.background_location_message)) },
        confirmButton = {
            Button(
                onClick = {
                    onDismiss()
                    // Navigate to app settings
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                    context.startActivity(intent)
                },
            ) {
                Text(stringResource(R.string.go_to_settings))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

/**
 * Dialog explaining why notification permission is needed.
 * Guides the user to system settings if permanently denied.
 */
@Composable
fun NotificationPermissionDialog(
    context: Context, onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.notification_permission_title)) },
        text = { Text(stringResource(R.string.notification_permission_message)) },
        confirmButton = {
            Button(
                onClick = {
                    onDismiss()
                    // Navigate to app settings
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                    context.startActivity(intent)
                },
            ) {
                Text(stringResource(R.string.go_to_settings))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

/**
 * Rationale dialog for notification permission retry.
 * Shown when the user denied permission but hasn't selected "Don't ask again".
 */
@Composable
fun NotificationRationaleDialog(
    onDismiss: () -> Unit, onRetry: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.notification_retry_title)) },
        text = { Text(stringResource(R.string.notification_retry_message)) },
        confirmButton = {
            Button(
                onClick = {
                    onDismiss()
                    onRetry()
                },
            ) {
                Text(stringResource(R.string.retry))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

/**
 * Dialog to confirm deletion of an alarm.
 */
@Composable
fun DeleteAlarmDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.delete_alarm_title)) },
        text = { Text(stringResource(R.string.delete_alarm_message)) },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm()
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text(stringResource(R.string.delete))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}


/**
 * Dialog shown when enabling an alarm while already within the target radius.
 */
@Composable
fun AlreadyAtDestinationDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.already_at_destination_title)) },
        text = { Text(stringResource(R.string.already_at_destination_message)) },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text(stringResource(R.string.ok))
            }
        },
    )
}
