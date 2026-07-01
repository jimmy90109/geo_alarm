package com.github.jimmy90109.geoalarm.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.jimmy90109.geoalarm.data.DEFAULT_ALARM_ICON_KEY
import com.github.jimmy90109.geoalarm.data.PlaceReminder
import com.github.jimmy90109.geoalarm.data.PlaceReminderAttachment
import com.github.jimmy90109.geoalarm.data.PlaceReminderAttachmentStore
import com.github.jimmy90109.geoalarm.data.PlaceReminderDataRepository
import com.github.jimmy90109.geoalarm.data.PlaceReminderItem
import com.github.jimmy90109.geoalarm.data.PlaceReminderType
import com.github.jimmy90109.geoalarm.data.PlaceTriggerType
import com.github.jimmy90109.geoalarm.data.location.CurrentLocationRepository
import com.github.jimmy90109.geoalarm.data.places.PlaceAutocompleteService
import com.github.jimmy90109.geoalarm.data.places.PlaceCandidate
import com.github.jimmy90109.geoalarm.data.places.PlaceSearchService
import com.github.jimmy90109.geoalarm.data.places.PlaceSuggestion
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class EditChecklistItem(val id: String = java.util.UUID.randomUUID().toString(), val text: String)

data class PlaceReminderEditUiState(
    val reminderId: String? = null,
    val isLoading: Boolean = true,
    val title: String = "",
    val type: PlaceReminderType = PlaceReminderType.TEXT,
    val content: String = "",
    val checklistItems: List<EditChecklistItem> = listOf(EditChecklistItem(text = "")),
    val placeName: String = "",
    val address: String? = null,
    val selectedIconKey: String = DEFAULT_ALARM_ICON_KEY,
    val selectedPosition: LatLng? = null,
    val currentLocation: LatLng? = null,
    val radiusMeters: Int = 1000,
    val triggerType: PlaceTriggerType = PlaceTriggerType.ENTER,
    val dwellMinutes: Int = 3,
    val cooldownMinutes: Int = 360,
    val searchQuery: String = "",
    val searchResults: List<PlaceCandidate> = emptyList(),
    val isSearching: Boolean = false,
    val searchFailed: Boolean = false,
    val step: AlarmEditStep = AlarmEditStep.MapSelection,
    val isSelectingPlace: Boolean = false,
    val hasUserInteractedWithMap: Boolean = false,
    val placeCandidates: List<PlaceCandidate> = emptyList(),
    val currentCandidateIndex: Int = 0,
    val controlMode: AlarmEditControlMode = AlarmEditControlMode.Radius,
    val inAppSearchQuery: String = "",
    val inAppSearchError: Boolean = false,
    val placeSuggestions: List<PlaceSuggestion> = emptyList(),
    val isLoadingSuggestions: Boolean = false,
    val candidateSource: PlaceCandidateSource? = null,
    val attachments: List<PlaceReminderAttachment> = emptyList(),
    val isAddingAttachments: Boolean = false,
    val savedReminderId: String? = null,
) {
    val isEditMode: Boolean get() = reminderId != null
    val canSave: Boolean
        get() {
            val hasContent = when (type) {
                PlaceReminderType.TEXT -> content.trim().isNotEmpty()
                PlaceReminderType.CHECKLIST -> checklistItems.any { it.text.trim().isNotEmpty() }
            }
            val hasAnyContent = hasContent || attachments.isNotEmpty()
            return title.trim().isNotEmpty() &&
                selectedPosition != null &&
                placeName.trim().isNotEmpty() &&
                hasAnyContent
        }

    val alarmEditUiState: AlarmEditUiState
        get() = AlarmEditUiState(
            selectedPosition = selectedPosition,
            radius = radiusMeters.toFloat(),
            name = placeName,
            selectedIconKey = selectedIconKey,
            step = step,
            isLoading = isLoading,
            currentLocation = currentLocation,
            hasUserInteractedWithMap = hasUserInteractedWithMap,
            placeCandidates = placeCandidates,
            currentCandidateIndex = currentCandidateIndex,
            controlMode = controlMode,
            inAppSearchQuery = inAppSearchQuery,
            inAppSearchError = inAppSearchError,
            placeSuggestions = placeSuggestions,
            isLoadingSuggestions = isLoadingSuggestions,
            candidateSource = candidateSource,
        )
}

sealed interface PlaceReminderEditAction {
    data class Load(val reminderId: String?) : PlaceReminderEditAction
    data class TitleChanged(val value: String) : PlaceReminderEditAction
    data class TypeChanged(val value: PlaceReminderType) : PlaceReminderEditAction
    data class ContentChanged(val value: String) : PlaceReminderEditAction
    data class ChecklistItemChanged(val index: Int, val value: String) : PlaceReminderEditAction
    data object AddChecklistItem : PlaceReminderEditAction
    data class AddChecklistItemWithText(val value: String) : PlaceReminderEditAction
    data class RemoveChecklistItem(val index: Int) : PlaceReminderEditAction
    data class MoveChecklistItem(val from: Int, val to: Int) : PlaceReminderEditAction
    data class SearchQueryChanged(val value: String) : PlaceReminderEditAction
    data object SearchSubmitted : PlaceReminderEditAction
    data class SearchResultSelected(val index: Int) : PlaceReminderEditAction
    data class MapPositionSelected(val latLng: LatLng) : PlaceReminderEditAction
    data class RadiusChanged(val meters: Int) : PlaceReminderEditAction
    data object MapInteracted : PlaceReminderEditAction
    data class StartInAppSearch(val mapCenter: LatLng) : PlaceReminderEditAction
    data class InAppSearchQueryChanged(val query: String) : PlaceReminderEditAction
    data class PlaceSuggestionSelected(val index: Int) : PlaceReminderEditAction
    data object SubmitInAppSearch : PlaceReminderEditAction
    data object CancelInAppSearch : PlaceReminderEditAction
    data class CandidateChanged(val index: Int) : PlaceReminderEditAction
    data object CandidateConfirmed : PlaceReminderEditAction
    data object CandidateSelectionCancelled : PlaceReminderEditAction
    data object StartPlaceSelection : PlaceReminderEditAction
    data object PlaceSelectionCancelled : PlaceReminderEditAction
    data object NextClicked : PlaceReminderEditAction
    data object BackToMapClicked : PlaceReminderEditAction
    data object PlaceDetailsConfirmed : PlaceReminderEditAction
    data class PlaceNameChanged(val name: String) : PlaceReminderEditAction
    data class IconSelected(val iconKey: String) : PlaceReminderEditAction
    data class TriggerTypeChanged(val value: PlaceTriggerType) : PlaceReminderEditAction
    data class DwellMinutesChanged(val minutes: Int) : PlaceReminderEditAction
    data class CooldownMinutesChanged(val minutes: Int) : PlaceReminderEditAction
    data class AttachmentsSelected(val uris: List<Uri>) : PlaceReminderEditAction
    data class RemoveAttachment(val attachmentId: String) : PlaceReminderEditAction
    data object SaveClicked : PlaceReminderEditAction
}

sealed interface PlaceReminderEditEffect {
    data class NavigateBack(val reminderId: String) : PlaceReminderEditEffect
}

@HiltViewModel
class PlaceReminderEditViewModel @Inject constructor(
    private val repository: PlaceReminderDataRepository,
    private val attachmentStore: PlaceReminderAttachmentStore,
    private val currentLocationRepository: CurrentLocationRepository,
    private val placeSearchService: PlaceSearchService,
    private val placeAutocompleteService: PlaceAutocompleteService,
) : ViewModel() {
    private val _uiState = MutableStateFlow(PlaceReminderEditUiState())
    val uiState: StateFlow<PlaceReminderEditUiState> = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<PlaceReminderEditEffect>()
    val effects: SharedFlow<PlaceReminderEditEffect> = _effects.asSharedFlow()
    private var placeSelectionSnapshot: PlaceSelectionSnapshot? = null
    private var inAppSearchSnapshot: PlaceSearchSnapshot? = null
    private var inAppSearchBiasCenter: LatLng? = null
    private var searchRequestVersion = 0
    private var autocompleteRequestVersion = 0
    private var autocompleteJob: Job? = null
    private var autocompleteSessionId: String? = null
    private val draftReminderId = UUID.randomUUID().toString()
    private var originalAttachmentIds: Set<String> = emptySet()

    init {
        viewModelScope.launch {
            currentLocationRepository.currentLocation.collect { location ->
                _uiState.value = _uiState.value.copy(currentLocation = location)
            }
        }
        viewModelScope.launch {
            currentLocationRepository.warmUp()
        }
    }

    fun onAction(action: PlaceReminderEditAction) {
        when (action) {
            is PlaceReminderEditAction.Load -> load(action.reminderId)
            is PlaceReminderEditAction.TitleChanged -> update { it.copy(title = action.value) }
            is PlaceReminderEditAction.TypeChanged -> update { it.copy(type = action.value) }
            is PlaceReminderEditAction.ContentChanged -> update { it.copy(content = action.value) }
            is PlaceReminderEditAction.ChecklistItemChanged -> updateChecklistItem(action.index, action.value)
            PlaceReminderEditAction.AddChecklistItem -> update { it.copy(checklistItems = it.checklistItems + EditChecklistItem(text = "")) }
            is PlaceReminderEditAction.AddChecklistItemWithText -> addChecklistItem(action.value)
            is PlaceReminderEditAction.RemoveChecklistItem -> removeChecklistItem(action.index)
            is PlaceReminderEditAction.MoveChecklistItem -> moveChecklistItem(action.from, action.to)
            is PlaceReminderEditAction.SearchQueryChanged -> update {
                it.copy(searchQuery = action.value, searchFailed = false)
            }
            PlaceReminderEditAction.SearchSubmitted -> search()
            is PlaceReminderEditAction.SearchResultSelected -> selectSearchResult(action.index)
            is PlaceReminderEditAction.MapPositionSelected -> selectMapPosition(action.latLng)
            is PlaceReminderEditAction.RadiusChanged -> update { it.copy(radiusMeters = action.meters) }
            PlaceReminderEditAction.MapInteracted -> update { it.copy(hasUserInteractedWithMap = true) }
            is PlaceReminderEditAction.StartInAppSearch -> startInAppSearch(action.mapCenter)
            is PlaceReminderEditAction.InAppSearchQueryChanged -> updateInAppSearchQuery(action.query)
            is PlaceReminderEditAction.PlaceSuggestionSelected -> selectPlaceSuggestion(action.index)
            PlaceReminderEditAction.SubmitInAppSearch -> submitInAppSearch()
            PlaceReminderEditAction.CancelInAppSearch -> cancelInAppSearch()
            is PlaceReminderEditAction.CandidateChanged -> updateCandidate(action.index)
            PlaceReminderEditAction.CandidateConfirmed -> confirmCandidate()
            PlaceReminderEditAction.CandidateSelectionCancelled -> cancelCandidateSelection()
            PlaceReminderEditAction.StartPlaceSelection -> startPlaceSelection()
            PlaceReminderEditAction.PlaceSelectionCancelled -> cancelPlaceSelection()
            PlaceReminderEditAction.NextClicked -> goToDetailsStep()
            PlaceReminderEditAction.BackToMapClicked -> update {
                it.copy(step = AlarmEditStep.MapSelection, isSelectingPlace = true)
            }
            PlaceReminderEditAction.PlaceDetailsConfirmed -> confirmPlaceDetails()
            is PlaceReminderEditAction.PlaceNameChanged -> update { it.copy(placeName = action.name) }
            is PlaceReminderEditAction.IconSelected -> update { it.copy(selectedIconKey = action.iconKey) }
            is PlaceReminderEditAction.TriggerTypeChanged -> update { it.copy(triggerType = action.value) }
            is PlaceReminderEditAction.DwellMinutesChanged -> update { it.copy(dwellMinutes = action.minutes) }
            is PlaceReminderEditAction.CooldownMinutesChanged -> update { it.copy(cooldownMinutes = action.minutes) }
            is PlaceReminderEditAction.AttachmentsSelected -> addAttachments(action.uris)
            is PlaceReminderEditAction.RemoveAttachment -> removeAttachment(action.attachmentId)
            PlaceReminderEditAction.SaveClicked -> save()
        }
    }

    private fun load(reminderId: String?) {
        viewModelScope.launch {
            if (reminderId == null) {
                _uiState.value = _uiState.value.copy(isLoading = false)
                return@launch
            }
            val reminderWithItems = repository.getReminder(reminderId)
            val reminder = reminderWithItems?.reminder
            if (reminder == null) {
                _uiState.value = _uiState.value.copy(isLoading = false)
                return@launch
            }
            originalAttachmentIds = reminderWithItems.sortedAttachments.map { it.id }.toSet()
            _uiState.value = _uiState.value.copy(
                reminderId = reminder.id,
                isLoading = false,
                title = reminder.title,
                type = reminder.type,
                content = reminder.content,
                checklistItems = reminderWithItems.sortedItems.map { EditChecklistItem(id = it.id, text = it.text) }.ifEmpty { listOf(EditChecklistItem(text = "")) },
                placeName = reminder.placeName,
                address = reminder.address,
                selectedIconKey = reminder.iconKey,
                selectedPosition = LatLng(reminder.latitude, reminder.longitude),
                radiusMeters = reminder.radiusMeters,
                step = AlarmEditStep.DetailsForm,
                isSelectingPlace = false,
                hasUserInteractedWithMap = true,
                triggerType = reminder.triggerType,
                dwellMinutes = reminder.dwellMinutes ?: 3,
                cooldownMinutes = reminder.cooldownMinutes,
                attachments = reminderWithItems.sortedAttachments,
            )
        }
    }

    private fun addAttachments(uris: List<Uri>) {
        if (uris.isEmpty() || _uiState.value.isAddingAttachments) return
        val reminderId = _uiState.value.reminderId ?: draftReminderId
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isAddingAttachments = true)
            val existing = _uiState.value.attachments
            val copied = uris.mapIndexedNotNull { index, uri ->
                attachmentStore.copy(reminderId, uri, existing.size + index)
            }
            _uiState.value = _uiState.value.copy(
                attachments = existing + copied,
                isAddingAttachments = false,
            )
        }
    }

    private fun removeAttachment(attachmentId: String) {
        val attachment = _uiState.value.attachments.firstOrNull { it.id == attachmentId } ?: return
        if (attachment.id !in originalAttachmentIds) {
            attachmentStore.delete(attachment.localPath)
        }
        _uiState.value = _uiState.value.copy(
            attachments = _uiState.value.attachments
                .filterNot { it.id == attachmentId }
                .mapIndexed { index, item -> item.copy(sortOrder = index) }
        )
    }

    private fun search() {
        val query = _uiState.value.searchQuery.trim()
        if (query.isEmpty()) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSearching = true, searchFailed = false)
            runCatching { placeSearchService.search(query, _uiState.value.currentLocation) }
                .onSuccess { results ->
                    _uiState.value = _uiState.value.copy(
                        isSearching = false,
                        searchResults = results,
                        searchFailed = results.isEmpty(),
                    )
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(
                        isSearching = false,
                        searchResults = emptyList(),
                        searchFailed = true,
                    )
                }
        }
    }

    private fun selectSearchResult(index: Int) {
        val candidate = _uiState.value.searchResults.getOrNull(index) ?: return
        _uiState.value = _uiState.value.copy(
            placeName = candidate.name,
            address = candidate.address,
            selectedPosition = candidate.location,
            searchQuery = candidate.name,
            searchResults = emptyList(),
            title = _uiState.value.title.ifBlank { candidate.name },
        )
    }

    private fun selectMapPosition(latLng: LatLng) {
        _uiState.value = _uiState.value.copy(
            selectedPosition = latLng,
            address = null,
            placeCandidates = emptyList(),
            currentCandidateIndex = 0,
            controlMode = AlarmEditControlMode.Radius,
            candidateSource = null,
            hasUserInteractedWithMap = true,
        )
    }

    private fun startInAppSearch(mapCenter: LatLng) {
        if (_uiState.value.controlMode != AlarmEditControlMode.Radius) return
        val state = _uiState.value
        inAppSearchSnapshot = PlaceSearchSnapshot(
            selectedPosition = state.selectedPosition,
            placeName = state.placeName,
            address = state.address,
            radiusMeters = state.radiusMeters,
            hasUserInteractedWithMap = state.hasUserInteractedWithMap,
        )
        inAppSearchBiasCenter = mapCenter
        autocompleteSessionId = placeAutocompleteService.startSession()
        _uiState.value = state.copy(
            controlMode = AlarmEditControlMode.SearchInput,
            inAppSearchQuery = "",
            inAppSearchError = false,
            placeSuggestions = emptyList(),
            isLoadingSuggestions = false,
            placeCandidates = emptyList(),
            currentCandidateIndex = 0,
            candidateSource = null,
        )
    }

    private fun updateInAppSearchQuery(query: String) {
        if (_uiState.value.controlMode != AlarmEditControlMode.SearchInput) return
        _uiState.value = _uiState.value.copy(inAppSearchQuery = query, inAppSearchError = false)
        scheduleAutocomplete(query)
    }

    private fun scheduleAutocomplete(query: String) {
        autocompleteJob?.cancel()
        val trimmedQuery = query.trim()
        if (trimmedQuery.length < MIN_AUTOCOMPLETE_QUERY_LENGTH) {
            autocompleteRequestVersion += 1
            _uiState.value = _uiState.value.copy(
                placeSuggestions = emptyList(),
                isLoadingSuggestions = false,
            )
            return
        }
        val sessionId = autocompleteSessionId ?: return
        val requestVersion = ++autocompleteRequestVersion
        autocompleteJob = viewModelScope.launch {
            delay(AUTOCOMPLETE_DEBOUNCE_MS)
            _uiState.value = _uiState.value.copy(isLoadingSuggestions = true)
            runCatching {
                placeAutocompleteService.suggestions(trimmedQuery, inAppSearchBiasCenter, sessionId)
            }.onSuccess { suggestions ->
                if (requestVersion == autocompleteRequestVersion &&
                    _uiState.value.controlMode == AlarmEditControlMode.SearchInput
                ) {
                    _uiState.value = _uiState.value.copy(
                        placeSuggestions = suggestions.take(MAX_PLACE_CANDIDATES),
                        isLoadingSuggestions = false,
                    )
                }
            }.onFailure {
                if (requestVersion == autocompleteRequestVersion) {
                    _uiState.value = _uiState.value.copy(
                        placeSuggestions = emptyList(),
                        isLoadingSuggestions = false,
                    )
                }
            }
        }
    }

    private fun selectPlaceSuggestion(index: Int) {
        if (_uiState.value.controlMode != AlarmEditControlMode.SearchInput) return
        val suggestions = _uiState.value.placeSuggestions
        val selected = suggestions.getOrNull(index) ?: return
        val sessionId = autocompleteSessionId ?: return
        autocompleteJob?.cancel()
        autocompleteRequestVersion += 1
        val requestVersion = ++searchRequestVersion
        _uiState.value = _uiState.value.copy(
            controlMode = AlarmEditControlMode.SearchLoading,
            inAppSearchError = false,
            isLoadingSuggestions = false,
        )
        viewModelScope.launch {
            runCatching {
                placeAutocompleteService.resolveCandidates(suggestions, selected.placeId, sessionId)
            }.onSuccess { candidates ->
                if (requestVersion != searchRequestVersion) return@onSuccess
                val selectedIndex = candidates.indexOfFirst { it.id == selected.placeId }
                if (selectedIndex < 0) {
                    showInAppSearchError()
                } else {
                    _uiState.value = _uiState.value.copy(
                        selectedPosition = null,
                        placeCandidates = candidates,
                        currentCandidateIndex = selectedIndex,
                        controlMode = AlarmEditControlMode.Candidates,
                        candidateSource = PlaceCandidateSource.InAppAutocomplete,
                        hasUserInteractedWithMap = true,
                    )
                }
            }.onFailure {
                if (requestVersion == searchRequestVersion) showInAppSearchError()
            }
        }
    }

    private fun submitInAppSearch() {
        val query = _uiState.value.inAppSearchQuery.trim()
        if (query.isEmpty() || _uiState.value.controlMode != AlarmEditControlMode.SearchInput) return
        val requestVersion = ++searchRequestVersion
        autocompleteJob?.cancel()
        autocompleteRequestVersion += 1
        autocompleteSessionId?.let(placeAutocompleteService::endSession)
        autocompleteSessionId = null
        _uiState.value = _uiState.value.copy(
            controlMode = AlarmEditControlMode.SearchLoading,
            inAppSearchError = false,
        )
        viewModelScope.launch {
            runCatching { placeSearchService.search(query, inAppSearchBiasCenter) }
                .onSuccess { candidates ->
                    if (requestVersion != searchRequestVersion) return@onSuccess
                    when (candidates.size) {
                        0 -> showInAppSearchError()
                        1 -> selectPlaceCandidate(candidates.single())
                        else -> {
                            _uiState.value = _uiState.value.copy(
                                selectedPosition = null,
                                placeCandidates = candidates.take(MAX_PLACE_CANDIDATES),
                                currentCandidateIndex = 0,
                                controlMode = AlarmEditControlMode.Candidates,
                                candidateSource = PlaceCandidateSource.InAppTextSearch,
                                hasUserInteractedWithMap = true,
                            )
                        }
                    }
                }
                .onFailure {
                    if (requestVersion == searchRequestVersion) showInAppSearchError()
                }
        }
    }

    private fun showInAppSearchError() {
        autocompleteSessionId?.let(placeAutocompleteService::endSession)
        autocompleteSessionId = placeAutocompleteService.startSession()
        _uiState.value = _uiState.value.copy(
            controlMode = AlarmEditControlMode.SearchInput,
            inAppSearchError = true,
            placeCandidates = emptyList(),
            currentCandidateIndex = 0,
            isLoadingSuggestions = false,
        )
    }

    private fun cancelInAppSearch() {
        val snapshot = inAppSearchSnapshot
        cancelInAppSearchState()
        _uiState.value = _uiState.value.copy(
            selectedPosition = snapshot?.selectedPosition,
            placeName = snapshot?.placeName.orEmpty(),
            address = snapshot?.address,
            radiusMeters = snapshot?.radiusMeters ?: _uiState.value.radiusMeters,
            hasUserInteractedWithMap = snapshot?.hasUserInteractedWithMap ?: true,
            controlMode = AlarmEditControlMode.Radius,
            inAppSearchQuery = "",
            inAppSearchError = false,
            placeSuggestions = emptyList(),
            isLoadingSuggestions = false,
            placeCandidates = emptyList(),
            currentCandidateIndex = 0,
            candidateSource = null,
        )
        inAppSearchSnapshot = null
        inAppSearchBiasCenter = null
    }

    private fun cancelInAppSearchState() {
        searchRequestVersion += 1
        autocompleteRequestVersion += 1
        autocompleteJob?.cancel()
        autocompleteSessionId?.let(placeAutocompleteService::endSession)
        autocompleteSessionId = null
        inAppSearchSnapshot = null
        inAppSearchBiasCenter = null
    }

    private fun updateCandidate(index: Int) {
        if (index in _uiState.value.placeCandidates.indices) {
            _uiState.value = _uiState.value.copy(currentCandidateIndex = index)
        }
    }

    private fun confirmCandidate() {
        val candidate = _uiState.value.placeCandidates.getOrNull(_uiState.value.currentCandidateIndex) ?: return
        selectPlaceCandidate(candidate)
    }

    private fun cancelCandidateSelection() {
        when (_uiState.value.candidateSource) {
            PlaceCandidateSource.InAppAutocomplete -> {
                autocompleteSessionId = placeAutocompleteService.startSession()
                _uiState.value = _uiState.value.copy(
                    placeCandidates = emptyList(),
                    currentCandidateIndex = 0,
                    controlMode = AlarmEditControlMode.SearchInput,
                    candidateSource = null,
                )
                scheduleAutocomplete(_uiState.value.inAppSearchQuery)
            }
            PlaceCandidateSource.InAppTextSearch -> cancelInAppSearch()
            else -> {
                _uiState.value = _uiState.value.copy(
                    placeCandidates = emptyList(),
                    currentCandidateIndex = 0,
                    controlMode = AlarmEditControlMode.Radius,
                    candidateSource = null,
                )
            }
        }
    }

    private fun selectPlaceCandidate(candidate: PlaceCandidate) {
        _uiState.value = _uiState.value.copy(
            selectedPosition = candidate.location,
            placeName = candidate.name,
            address = candidate.address,
            placeCandidates = emptyList(),
            currentCandidateIndex = 0,
            controlMode = AlarmEditControlMode.Radius,
            candidateSource = null,
            hasUserInteractedWithMap = true,
        )
        inAppSearchSnapshot = null
        inAppSearchBiasCenter = null
        autocompleteSessionId?.let(placeAutocompleteService::endSession)
        autocompleteSessionId = null
    }

    private fun goToDetailsStep() {
        if (_uiState.value.selectedPosition != null) {
            _uiState.value = _uiState.value.copy(step = AlarmEditStep.DetailsForm)
        }
    }

    private fun confirmPlaceDetails() {
        val state = _uiState.value
        if (state.selectedPosition != null && state.placeName.isNotBlank()) {
            placeSelectionSnapshot = null
            _uiState.value = state.copy(
                isSelectingPlace = false,
                step = AlarmEditStep.DetailsForm,
            )
        }
    }

    private fun startPlaceSelection() {
        val state = _uiState.value
        placeSelectionSnapshot = PlaceSelectionSnapshot(
            selectedPosition = state.selectedPosition,
            placeName = state.placeName,
            address = state.address,
            selectedIconKey = state.selectedIconKey,
            radiusMeters = state.radiusMeters,
            hasUserInteractedWithMap = state.hasUserInteractedWithMap,
        )
        _uiState.value = _uiState.value.copy(
            isSelectingPlace = true,
            step = AlarmEditStep.MapSelection,
            controlMode = AlarmEditControlMode.Radius,
            placeCandidates = emptyList(),
            currentCandidateIndex = 0,
            candidateSource = null,
            inAppSearchError = false,
            placeSuggestions = emptyList(),
            isLoadingSuggestions = false,
        )
    }

    private fun cancelPlaceSelection() {
        val snapshot = placeSelectionSnapshot
        placeSelectionSnapshot = null
        cancelInAppSearchState()
        _uiState.value = _uiState.value.copy(
            isSelectingPlace = false,
            step = AlarmEditStep.DetailsForm,
            selectedPosition = snapshot?.selectedPosition,
            placeName = snapshot?.placeName.orEmpty(),
            address = snapshot?.address,
            selectedIconKey = snapshot?.selectedIconKey ?: DEFAULT_ALARM_ICON_KEY,
            radiusMeters = snapshot?.radiusMeters ?: 1000,
            hasUserInteractedWithMap = snapshot?.hasUserInteractedWithMap ?: false,
            controlMode = AlarmEditControlMode.Radius,
            placeCandidates = emptyList(),
            currentCandidateIndex = 0,
            candidateSource = null,
        )
    }

    private fun updateChecklistItem(index: Int, value: String) {
        val items = _uiState.value.checklistItems.toMutableList()
        if (index !in items.indices) return
        items[index] = items[index].copy(text = value)
        _uiState.value = _uiState.value.copy(checklistItems = items)
    }

    private fun addChecklistItem(value: String) {
        val trimmed = value.trim()
        if (trimmed.isEmpty()) return
        val items = _uiState.value.checklistItems
            .filter { it.text.trim().isNotEmpty() } + EditChecklistItem(text = trimmed)
        _uiState.value = _uiState.value.copy(checklistItems = items)
    }

    private fun removeChecklistItem(index: Int) {
        val items = _uiState.value.checklistItems.toMutableList()
        if (index !in items.indices) return
        items.removeAt(index)
        _uiState.value = _uiState.value.copy(checklistItems = items.ifEmpty { mutableListOf(EditChecklistItem(text = "")) })
    }

    private fun moveChecklistItem(from: Int, to: Int) {
        val items = _uiState.value.checklistItems.toMutableList()
        if (from in items.indices && to in items.indices) {
            val item = items.removeAt(from)
            items.add(to, item)
            _uiState.value = _uiState.value.copy(checklistItems = items)
        }
    }

    private fun save() {
        val state = _uiState.value
        if (!state.canSave) return
        val position = state.selectedPosition ?: return
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val existing = state.reminderId?.let { repository.getReminder(it)?.reminder }
            val reminderId = existing?.id ?: draftReminderId
            val reminder = PlaceReminder(
                id = reminderId,
                title = state.title.trim(),
                type = state.type,
                content = if (state.type == PlaceReminderType.TEXT) state.content.trim() else "",
                placeName = state.placeName.ifBlank { state.title.trim() },
                address = state.address,
                iconKey = state.selectedIconKey,
                latitude = position.latitude,
                longitude = position.longitude,
                radiusMeters = state.radiusMeters,
                triggerType = state.triggerType,
                dwellMinutes = if (state.triggerType == PlaceTriggerType.DWELL) state.dwellMinutes else null,
                cooldownMinutes = state.cooldownMinutes,
                enabled = existing?.enabled ?: false,
                lastTriggeredAt = existing?.lastTriggeredAt,
                createdAt = existing?.createdAt ?: now,
                updatedAt = now,
            )
            val items = if (state.type == PlaceReminderType.CHECKLIST) {
                state.checklistItems.filter { it.text.trim().isNotEmpty() }
                    .mapIndexed { index, item ->
                        PlaceReminderItem(
                            id = item.id,
                            reminderId = reminderId,
                            text = item.text.trim(),
                            checked = false,
                            sortOrder = index,
                        )
                    }
            } else {
                emptyList()
            }
            val attachments = state.attachments.mapIndexed { index, attachment ->
                attachment.copy(reminderId = reminderId, sortOrder = index)
            }
            repository.save(reminder, items, attachments)
            _uiState.value = state.copy(savedReminderId = reminderId)
            _effects.emit(PlaceReminderEditEffect.NavigateBack(reminderId))
        }
    }

    private inline fun update(block: (PlaceReminderEditUiState) -> PlaceReminderEditUiState) {
        _uiState.value = block(_uiState.value)
    }

    private companion object {
        const val MAX_PLACE_CANDIDATES = 5
        const val MIN_AUTOCOMPLETE_QUERY_LENGTH = 2
        const val AUTOCOMPLETE_DEBOUNCE_MS = 300L
    }

    private data class PlaceSearchSnapshot(
        val selectedPosition: LatLng?,
        val placeName: String,
        val address: String?,
        val radiusMeters: Int,
        val hasUserInteractedWithMap: Boolean,
    )

    private data class PlaceSelectionSnapshot(
        val selectedPosition: LatLng?,
        val placeName: String,
        val address: String?,
        val selectedIconKey: String,
        val radiusMeters: Int,
        val hasUserInteractedWithMap: Boolean,
    )
}
