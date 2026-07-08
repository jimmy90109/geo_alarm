package com.github.jimmy90109.geoalarm.ui.screens.place_reminders.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.github.jimmy90109.geoalarm.R
import com.github.jimmy90109.geoalarm.data.PlaceReminderType
import com.github.jimmy90109.geoalarm.data.PlaceTriggerType
import com.github.jimmy90109.geoalarm.ui.components.MediaPreviewSelection
import com.github.jimmy90109.geoalarm.ui.viewmodel.PlaceReminderEditAction
import com.github.jimmy90109.geoalarm.ui.viewmodel.PlaceReminderEditUiState
import kotlinx.coroutines.android.awaitFrame
import kotlinx.coroutines.launch
import sh.calvin.reorderable.ReorderableColumn

private val DwellOptions = listOf(1, 3, 5, 10)
private val CooldownOptions = listOf(60, 180, 360, 1440)

@Composable
internal fun PlaceReminderPlaceFormPage(
    state: PlaceReminderEditUiState,
    onSelectPlace: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = stringResource(R.string.place_reminder_step_place),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
        PlaceReminderSelectedPlaceSection(
            state = state,
            onSelectPlace = onSelectPlace,
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
internal fun PlaceReminderContentFormPage(
    state: PlaceReminderEditUiState,
    onAction: (PlaceReminderEditAction) -> Unit,
    newChecklistText: String,
    onNewChecklistTextChange: (String) -> Unit,
    newChecklistFocusRequester: FocusRequester,
    onPickAttachments: () -> Unit,
    hiddenAttachmentId: String?,
    onAttachmentBoundsChanged: (String, Rect) -> Unit,
    onAttachmentClick: (MediaPreviewSelection) -> Unit,
    onInputBoundsChanged: (String, Rect?) -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    FormSection(title = stringResource(R.string.place_reminder_step_content)) {
        DisposableEffect(Unit) {
            onDispose { onInputBoundsChanged("title", null) }
        }
        OutlinedTextField(
            value = state.title,
            onValueChange = { onAction(PlaceReminderEditAction.TitleChanged(it)) },
            label = { Text(stringResource(R.string.place_reminder_name)) },
            placeholder = { Text(stringResource(R.string.place_reminder_name_placeholder)) },
            modifier = Modifier
                .fillMaxWidth()
                .onGloballyPositioned { onInputBoundsChanged("title", it.boundsInRoot()) },
            singleLine = true,
            shape = PlaceReminderTextFieldShape,
        )
        ExpressiveOptionGroup(
            title = stringResource(R.string.place_reminder_type),
            options = listOf(
                PlaceReminderType.TEXT to stringResource(R.string.place_reminder_type_text),
                PlaceReminderType.CHECKLIST to stringResource(R.string.place_reminder_type_checklist),
            ),
            selected = state.type,
            onSelected = { onAction(PlaceReminderEditAction.TypeChanged(it)) },
        )
        if (state.type == PlaceReminderType.TEXT) {
            DisposableEffect(Unit) {
                onDispose { onInputBoundsChanged("content", null) }
            }
            OutlinedTextField(
                value = state.content,
                onValueChange = { onAction(PlaceReminderEditAction.ContentChanged(it)) },
                label = { Text(stringResource(R.string.place_reminder_content)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(136.dp)
                    .onGloballyPositioned { onInputBoundsChanged("content", it.boundsInRoot()) },
                minLines = 4,
                shape = PlaceReminderTextFieldShape,
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = stringResource(R.string.place_reminder_content),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                val visibleItems = state.checklistItems.filter { it.text.isNotBlank() }
                ReorderableColumn(
                    list = visibleItems,
                    onSettle = { fromIndex, toIndex ->
                        val fromId = visibleItems.getOrNull(fromIndex)?.id
                        val toId = visibleItems.getOrNull(toIndex)?.id
                        val fullFrom = state.checklistItems.indexOfFirst { it.id == fromId }
                        val fullTo = state.checklistItems.indexOfFirst { it.id == toId }
                        if (fullFrom >= 0 && fullTo >= 0) {
                            onAction(PlaceReminderEditAction.MoveChecklistItem(fullFrom, fullTo))
                        }
                    },
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) { _, item, _ ->
                    val index = state.checklistItems.indexOfFirst { it.id == item.id }
                    ChecklistEditRow(
                        value = item.text,
                        onValueChange = {
                            if (index >= 0) onAction(PlaceReminderEditAction.ChecklistItemChanged(index, it))
                        },
                        onRemove = {
                            if (index >= 0) onAction(PlaceReminderEditAction.RemoveChecklistItem(index))
                        },
                        dragHandleModifier = Modifier.draggableHandle(),
                        onInputBoundsChanged = { bounds ->
                            onInputBoundsChanged("checklist:${item.id}", bounds)
                        },
                    )
                }
                ChecklistAddRow(
                    value = newChecklistText,
                    onValueChange = onNewChecklistTextChange,
                    onAdd = {
                        val text = newChecklistText.trim()
                        if (text.isNotEmpty()) {
                            onAction(PlaceReminderEditAction.AddChecklistItemWithText(text))
                            onNewChecklistTextChange("")
                            coroutineScope.launch {
                                awaitFrame()
                                newChecklistFocusRequester.requestFocus()
                            }
                        }
                    },
                    focusRequester = newChecklistFocusRequester,
                    onInputBoundsChanged = { bounds ->
                        onInputBoundsChanged("checklist:add", bounds)
                    },
                    onKeepFocusBoundsChanged = { bounds ->
                        onInputBoundsChanged("checklist:addButton", bounds)
                    },
                )
            }
        }
        PlaceReminderAttachmentEditor(
            state = state,
            onPickAttachments = onPickAttachments,
            onRemoveAttachment = { onAction(PlaceReminderEditAction.RemoveAttachment(it)) },
            hiddenAttachmentId = hiddenAttachmentId,
            onAttachmentBoundsChanged = onAttachmentBoundsChanged,
            onAttachmentClick = onAttachmentClick,
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
internal fun PlaceReminderTriggerFormPage(
    state: PlaceReminderEditUiState,
    onAction: (PlaceReminderEditAction) -> Unit,
) {
    FormSection(title = stringResource(R.string.place_reminder_step_trigger)) {
        ExpressiveOptionGroup(
            title = stringResource(R.string.place_reminder_trigger_type),
            options = listOf(
                PlaceTriggerType.ENTER to stringResource(R.string.place_reminder_trigger_enter),
                PlaceTriggerType.DWELL to stringResource(R.string.place_reminder_trigger_dwell),
            ),
            selected = state.triggerType,
            onSelected = { onAction(PlaceReminderEditAction.TriggerTypeChanged(it)) },
        )
        if (state.triggerType == PlaceTriggerType.DWELL) {
            ExpressiveIntOptionGroup(
                title = stringResource(R.string.place_reminder_dwell_time),
                options = DwellOptions,
                selected = state.dwellMinutes,
                label = { it.toString() },
                onSelected = { onAction(PlaceReminderEditAction.DwellMinutesChanged(it)) },
            )
        }
        CooldownOptionGroup(
            title = stringResource(R.string.place_reminder_cooldown),
            numericOptions = CooldownOptions.filterNot { it == 1440 },
            selected = state.cooldownMinutes,
            oneDayLabel = stringResource(R.string.place_reminder_cooldown_today),
            onSelected = { onAction(PlaceReminderEditAction.CooldownMinutesChanged(it)) },
        )
    }
}

@Composable
private fun FormSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                content()
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private fun <T> ExpressiveOptionGroup(
    title: String,
    options: List<Pair<T, String>>,
    selected: T,
    onSelected: (T) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        ExpressiveOptionButtons(options = options, selected = selected, onSelected = onSelected)
    }
}

@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private fun <T> ExpressiveOptionButtons(
    options: List<Pair<T, String>>,
    selected: T,
    onSelected: (T) -> Unit,
) {
    ButtonGroup(
        overflowIndicator = { menuState ->
            ButtonGroupDefaults.OverflowIndicator(menuState = menuState)
        },
        modifier = Modifier.fillMaxWidth(),
    ) {
        val buttonGroupScope = this
        options.forEach { (value, label) ->
            val isSelected = selected == value
            customItem(
                buttonGroupContent = {
                    ToggleButton(
                        checked = isSelected,
                        onCheckedChange = { onSelected(value) },
                        modifier = with(buttonGroupScope) { Modifier.weight(1f) },
                        shapes = ToggleButtonDefaults.shapes(
                            shape = RoundedCornerShape(14.dp),
                            pressedShape = RoundedCornerShape(20.dp),
                            checkedShape = ToggleButtonDefaults.roundShape,
                        ),
                        colors = ToggleButtonDefaults.toggleButtonColors().copy(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.42f),
                            checkedContainerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            checkedContentColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                    ) {
                        if (isSelected) {
                            Icon(Icons.Filled.Check, contentDescription = null)
                        }
                        Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                },
                menuContent = { menuState ->
                    DropdownMenuItem(
                        text = { Text(label) },
                        onClick = {
                            onSelected(value)
                            menuState.dismiss()
                        },
                        leadingIcon = {
                            if (isSelected) Icon(Icons.Filled.Check, contentDescription = null)
                        },
                    )
                }
            )
        }
    }
}

@Composable
private fun ExpressiveIntOptionGroup(
    title: String,
    options: List<Int>,
    selected: Int,
    label: @Composable (Int) -> String,
    onSelected: (Int) -> Unit,
) {
    ExpressiveOptionGroup(
        title = title,
        options = options.map { it to label(it) },
        selected = selected,
        onSelected = onSelected,
    )
}

@Composable
private fun CooldownOptionGroup(
    title: String,
    numericOptions: List<Int>,
    selected: Int,
    oneDayLabel: String,
    onSelected: (Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        ExpressiveOptionButtons(
            options = numericOptions.map { it to (it / 60).toString() },
            selected = selected,
            onSelected = onSelected,
        )
        ExpressiveSingleOptionButton(
            label = oneDayLabel,
            selected = selected == 1440,
            onClick = { onSelected(1440) },
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private fun ExpressiveSingleOptionButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    ButtonGroup(
        overflowIndicator = { menuState ->
            ButtonGroupDefaults.OverflowIndicator(menuState = menuState)
        },
        modifier = Modifier.fillMaxWidth(),
    ) {
        val buttonGroupScope = this
        customItem(
            buttonGroupContent = {
                ToggleButton(
                    checked = selected,
                    onCheckedChange = { onClick() },
                    modifier = with(buttonGroupScope) {
                        Modifier
                            .weight(1f)
                            .height(48.dp)
                    },
                    shapes = ToggleButtonDefaults.shapes(
                        shape = RoundedCornerShape(14.dp),
                        pressedShape = RoundedCornerShape(20.dp),
                        checkedShape = ToggleButtonDefaults.roundShape,
                    ),
                    colors = ToggleButtonDefaults.toggleButtonColors().copy(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.42f),
                        checkedContainerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        checkedContentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                ) {
                    if (selected) {
                        Icon(Icons.Filled.Check, contentDescription = null)
                    }
                    Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            },
            menuContent = { menuState ->
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = {
                        onClick()
                        menuState.dismiss()
                    },
                    leadingIcon = {
                        if (selected) Icon(Icons.Filled.Check, contentDescription = null)
                    },
                )
            },
        )
    }
}
