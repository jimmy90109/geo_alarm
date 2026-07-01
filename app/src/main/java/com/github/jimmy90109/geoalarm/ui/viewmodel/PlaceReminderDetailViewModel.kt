package com.github.jimmy90109.geoalarm.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.github.jimmy90109.geoalarm.data.PlaceReminderDataRepository
import com.github.jimmy90109.geoalarm.data.PlaceReminderItem
import com.github.jimmy90109.geoalarm.data.PlaceReminderWithItems
import com.github.jimmy90109.geoalarm.service.PlaceReminderGeofenceManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch

sealed interface PlaceReminderDetailEffect {
    data object NavigateBack : PlaceReminderDetailEffect
}

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class PlaceReminderDetailViewModel @Inject constructor(
    application: Application,
    private val repository: PlaceReminderDataRepository,
) : AndroidViewModel(application) {
    private val reminderId = MutableStateFlow<String?>(null)

    val reminder: StateFlow<PlaceReminderWithItems?> = reminderId.flatMapLatest { id ->
        if (id == null) flowOf(null) else repository.getReminderFlow(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _effects = MutableSharedFlow<PlaceReminderDetailEffect>()
    val effects: SharedFlow<PlaceReminderDetailEffect> = _effects.asSharedFlow()

    fun load(id: String) {
        reminderId.value = id
    }

    fun setEnabled(id: String, enabled: Boolean) {
        val context = getApplication<Application>()
        if (enabled && !PlaceReminderListViewModel.permissionState(context).canEnableReminder) return
        viewModelScope.launch {
            repository.setEnabled(id, enabled)
            PlaceReminderGeofenceManager(context, repository).syncReminder(id)
        }
    }

    fun setItemChecked(item: PlaceReminderItem, checked: Boolean) {
        viewModelScope.launch {
            repository.updateItem(item.copy(checked = checked))
        }
    }

    fun moveItem(reminderId: String, from: Int, to: Int) {
        viewModelScope.launch {
            val current = repository.getReminder(reminderId) ?: return@launch
            val items = current.sortedItems.toMutableList()
            if (from in items.indices && to in items.indices) {
                val item = items.removeAt(from)
                items.add(to, item)
                val updatedItems = items.mapIndexed { index, placeReminderItem ->
                    placeReminderItem.copy(sortOrder = index)
                }
                repository.save(current.reminder, updatedItems)
            }
        }
    }

    fun addItem(reminderId: String, text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            val current = repository.getReminder(reminderId) ?: return@launch
            val items = current.sortedItems + PlaceReminderItem(
                reminderId = reminderId,
                text = trimmed,
                checked = false,
                sortOrder = current.items.size,
            )
            repository.save(current.reminder, items)
        }
    }

    fun resetChecklist(reminderId: String) {
        viewModelScope.launch {
            val current = repository.getReminder(reminderId) ?: return@launch
            current.sortedItems.forEach { item ->
                if (item.checked) repository.updateItem(item.copy(checked = false))
            }
        }
    }

    fun deleteAttachment(attachmentId: String) {
        viewModelScope.launch {
            repository.deleteAttachment(attachmentId)
        }
    }

    fun delete() {
        val current = reminder.value ?: return
        val context = getApplication<Application>()
        viewModelScope.launch {
            repository.delete(current.reminder)
            PlaceReminderGeofenceManager(context, repository).removeReminder(current.reminder.id)
            _effects.emit(PlaceReminderDetailEffect.NavigateBack)
        }
    }
}
