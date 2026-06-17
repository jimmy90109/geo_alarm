package com.github.jimmy90109.geoalarm.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.jimmy90109.geoalarm.data.Alarm
import com.github.jimmy90109.geoalarm.data.AlarmDataRepository
import com.github.jimmy90109.geoalarm.data.DEFAULT_ALARM_ICON_KEY
import com.github.jimmy90109.geoalarm.data.location.CurrentLocationRepository
import com.github.jimmy90109.geoalarm.data.places.PlaceAutocompleteService
import com.github.jimmy90109.geoalarm.data.places.PlaceCandidate
import com.github.jimmy90109.geoalarm.data.places.PlaceSearchService
import com.github.jimmy90109.geoalarm.data.places.PlaceSuggestion
import com.github.jimmy90109.geoalarm.share.SharedPlaceSource
import com.github.jimmy90109.geoalarm.widget.WidgetUpdater
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.lifecycle.HiltViewModel
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
import java.util.UUID

enum class AlarmEditStep {
    MapSelection,
    DetailsForm
}

enum class AlarmEditControlMode {
    Radius,
    SearchInput,
    SearchLoading,
    Candidates
}

enum class PlaceCandidateSource {
    Shared,
    InAppTextSearch,
    InAppAutocomplete
}

data class AlarmEditUiState(
    val selectedPosition: LatLng? = null,
    val radius: Float = 1000f,
    val name: String = "",
    val searchText: String = "",
    val selectedIconKey: String = DEFAULT_ALARM_ICON_KEY,
    val step: AlarmEditStep = AlarmEditStep.MapSelection,
    val isLoading: Boolean = true,
    val existingAlarm: Alarm? = null,
    val isSaved: Boolean = false,
    val savedAlarmId: String? = null, // ID of the alarm that was just saved (for highlight animation)
    val showDeleteErrorDialog: Boolean = false,
    val showDeleteConfirmDialog: Boolean = false,
    val currentLocation: LatLng? = null,
    val hasUserInteractedWithMap: Boolean = false,
    val isSearchingSharedPlace: Boolean = false,
    val placeCandidates: List<PlaceCandidate> = emptyList(),
    val currentCandidateIndex: Int = 0,
    val showSharedPlaceSearchError: Boolean = false,
    val controlMode: AlarmEditControlMode = AlarmEditControlMode.Radius,
    val inAppSearchQuery: String = "",
    val inAppSearchError: Boolean = false,
    val placeSuggestions: List<PlaceSuggestion> = emptyList(),
    val isLoadingSuggestions: Boolean = false,
    val candidateSource: PlaceCandidateSource? = null
) {
    val isSelectingCandidate: Boolean
        get() = controlMode == AlarmEditControlMode.Candidates && placeCandidates.isNotEmpty()

    val isInAppSearchActive: Boolean
        get() = controlMode != AlarmEditControlMode.Radius &&
            candidateSource != PlaceCandidateSource.Shared

    val currentCandidate: PlaceCandidate?
        get() = placeCandidates.getOrNull(currentCandidateIndex)
}

sealed interface AlarmEditAction {
    data class LoadAlarm(val alarmId: String?) : AlarmEditAction
    data object MapLoaded : AlarmEditAction
    data object MapInteracted : AlarmEditAction
    data class PositionSelected(val latLng: LatLng) : AlarmEditAction
    data class SearchPositionSelected(val latLng: LatLng, val placeName: String) : AlarmEditAction
    data class SearchSharedPlace(
        val query: String,
        val source: SharedPlaceSource = SharedPlaceSource.GoogleMapsPlace
    ) : AlarmEditAction
    data class StartInAppSearch(val mapCenter: LatLng) : AlarmEditAction
    data class InAppSearchQueryChanged(val query: String) : AlarmEditAction
    data class PlaceSuggestionSelected(val index: Int) : AlarmEditAction
    data object SubmitInAppSearch : AlarmEditAction
    data object CancelInAppSearch : AlarmEditAction
    data class CandidateChanged(val index: Int) : AlarmEditAction
    data object CandidateConfirmed : AlarmEditAction
    data object CandidateSelectionCancelled : AlarmEditAction
    data object SharedPlaceSearchErrorShown : AlarmEditAction
    data class RadiusChanged(val radius: Float) : AlarmEditAction
    data class NameChanged(val name: String) : AlarmEditAction
    data class IconSelected(val iconKey: String) : AlarmEditAction
    data object NextClicked : AlarmEditAction
    data object BackToMapClicked : AlarmEditAction
    data object SaveClicked : AlarmEditAction
    data object DeleteRequested : AlarmEditAction
    data object DeleteConfirmed : AlarmEditAction
    data object DeleteDialogDismissed : AlarmEditAction
    data object DeleteErrorDismissed : AlarmEditAction
}

sealed interface AlarmEditEffect {
    data class NavigateBack(val savedAlarmId: String?) : AlarmEditEffect
}

@HiltViewModel
class AlarmEditViewModel @Inject constructor(
    private val repository: AlarmDataRepository,
    private val widgetUpdater: WidgetUpdater,
    private val currentLocationRepository: CurrentLocationRepository,
    private val placeSearchService: PlaceSearchService,
    private val placeAutocompleteService: PlaceAutocompleteService
) : ViewModel() {

    private val _uiState = MutableStateFlow(AlarmEditUiState())
    val uiState: StateFlow<AlarmEditUiState> = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<AlarmEditEffect>()
    val effects: SharedFlow<AlarmEditEffect> = _effects.asSharedFlow()
    private var lastSharedPlaceRequest: Pair<String, SharedPlaceSource>? = null
    private var inAppSearchSnapshot: InAppSearchSnapshot? = null
    private var inAppSearchBiasCenter: LatLng? = null
    private var searchRequestVersion = 0
    private var autocompleteRequestVersion = 0
    private var autocompleteJob: Job? = null
    private var autocompleteSessionId: String? = null

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

    fun onAction(action: AlarmEditAction) {
        when (action) {
            is AlarmEditAction.LoadAlarm -> loadAlarm(action.alarmId)
            AlarmEditAction.MapLoaded -> setMapLoaded()
            AlarmEditAction.MapInteracted -> markMapInteracted()
            is AlarmEditAction.PositionSelected -> updatePosition(action.latLng)
            is AlarmEditAction.SearchPositionSelected -> updatePositionFromSearch(
                action.latLng,
                action.placeName
            )
            is AlarmEditAction.SearchSharedPlace -> searchSharedPlace(action.query, action.source)
            is AlarmEditAction.StartInAppSearch -> startInAppSearch(action.mapCenter)
            is AlarmEditAction.InAppSearchQueryChanged -> updateInAppSearchQuery(action.query)
            is AlarmEditAction.PlaceSuggestionSelected -> selectPlaceSuggestion(action.index)
            AlarmEditAction.SubmitInAppSearch -> submitInAppSearch()
            AlarmEditAction.CancelInAppSearch -> cancelInAppSearch()
            is AlarmEditAction.CandidateChanged -> updateCandidate(action.index)
            AlarmEditAction.CandidateConfirmed -> confirmCandidate()
            AlarmEditAction.CandidateSelectionCancelled -> cancelCandidateSelection()
            AlarmEditAction.SharedPlaceSearchErrorShown -> dismissSharedPlaceSearchError()
            is AlarmEditAction.RadiusChanged -> updateRadius(action.radius)
            is AlarmEditAction.NameChanged -> updateName(action.name)
            is AlarmEditAction.IconSelected -> selectIcon(action.iconKey)
            AlarmEditAction.NextClicked -> goToDetailsStep()
            AlarmEditAction.BackToMapClicked -> goToMapStep()
            AlarmEditAction.SaveClicked -> saveAlarm()
            AlarmEditAction.DeleteRequested -> requestDeleteAlarm()
            AlarmEditAction.DeleteConfirmed -> confirmDeleteAlarm()
            AlarmEditAction.DeleteDialogDismissed -> dismissDeleteConfirmDialog()
            AlarmEditAction.DeleteErrorDismissed -> dismissDeleteErrorDialog()
        }
    }

    private fun loadAlarm(alarmId: String?) {
        viewModelScope.launch {
            if (alarmId != null) {
                val alarm = repository.getAlarm(alarmId)
                if (alarm != null) {
                    _uiState.value = _uiState.value.copy(
                        existingAlarm = alarm,
                        selectedPosition = LatLng(alarm.latitude, alarm.longitude),
                        radius = alarm.radius.toFloat(),
                        name = alarm.name,
                        selectedIconKey = alarm.iconKey,
                        step = AlarmEditStep.MapSelection,
                        hasUserInteractedWithMap = true,
                        isLoading = false
                    )
                    return@launch
                }
            }
            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }

    private fun setMapLoaded() {
        _uiState.value = _uiState.value.copy(isLoading = false)
    }

    private fun markMapInteracted() {
        _uiState.value = _uiState.value.copy(hasUserInteractedWithMap = true)
    }

    private fun updatePosition(latLng: LatLng) {
        _uiState.value = _uiState.value.copy(
            selectedPosition = latLng,
            searchText = "",
            placeCandidates = emptyList(),
            currentCandidateIndex = 0,
            controlMode = AlarmEditControlMode.Radius,
            candidateSource = null,
            hasUserInteractedWithMap = true
        )
    }

    private fun updatePositionFromSearch(latLng: LatLng, placeName: String) {
        _uiState.value = _uiState.value.copy(
            selectedPosition = latLng,
            searchText = placeName,
            placeCandidates = emptyList(),
            currentCandidateIndex = 0,
            controlMode = AlarmEditControlMode.Radius,
            candidateSource = null,
            hasUserInteractedWithMap = true
        )
    }

    private fun startInAppSearch(mapCenter: LatLng) {
        if (_uiState.value.controlMode != AlarmEditControlMode.Radius) return
        val state = _uiState.value
        inAppSearchSnapshot = InAppSearchSnapshot(
            selectedPosition = state.selectedPosition,
            name = state.name,
            radius = state.radius,
            searchText = state.searchText,
            hasUserInteractedWithMap = state.hasUserInteractedWithMap
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
            candidateSource = null
        )
    }

    private fun updateInAppSearchQuery(query: String) {
        if (_uiState.value.controlMode == AlarmEditControlMode.SearchInput) {
            _uiState.value = _uiState.value.copy(
                inAppSearchQuery = query,
                inAppSearchError = false
            )
            scheduleAutocomplete(query)
        }
    }

    private fun scheduleAutocomplete(query: String) {
        autocompleteJob?.cancel()
        val trimmedQuery = query.trim()
        if (trimmedQuery.length < MIN_AUTOCOMPLETE_QUERY_LENGTH) {
            autocompleteRequestVersion += 1
            _uiState.value = _uiState.value.copy(
                placeSuggestions = emptyList(),
                isLoadingSuggestions = false
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
                        isLoadingSuggestions = false
                    )
                }
            }.onFailure {
                if (requestVersion == autocompleteRequestVersion) {
                    _uiState.value = _uiState.value.copy(
                        placeSuggestions = emptyList(),
                        isLoadingSuggestions = false
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
            isLoadingSuggestions = false
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
                        hasUserInteractedWithMap = true
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
        val locationBiasCenter = inAppSearchBiasCenter
        _uiState.value = _uiState.value.copy(
            controlMode = AlarmEditControlMode.SearchLoading,
            inAppSearchError = false
        )
        viewModelScope.launch {
            runCatching { placeSearchService.search(query, locationBiasCenter) }
                .onSuccess { candidates ->
                    if (requestVersion != searchRequestVersion) return@onSuccess
                    when (candidates.size) {
                        0 -> showInAppSearchError()
                        1 -> {
                            selectSharedPlace(candidates.single())
                            inAppSearchSnapshot = null
                            inAppSearchBiasCenter = null
                        }
                        else -> {
                            _uiState.value = _uiState.value.copy(
                                selectedPosition = null,
                                placeCandidates = candidates.take(MAX_PLACE_CANDIDATES),
                                currentCandidateIndex = 0,
                                controlMode = AlarmEditControlMode.Candidates,
                                candidateSource = PlaceCandidateSource.InAppTextSearch,
                                hasUserInteractedWithMap = true
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
            isLoadingSuggestions = false
        )
    }

    private fun cancelInAppSearch() {
        searchRequestVersion += 1
        autocompleteRequestVersion += 1
        autocompleteJob?.cancel()
        autocompleteSessionId?.let(placeAutocompleteService::endSession)
        autocompleteSessionId = null
        val snapshot = inAppSearchSnapshot
        _uiState.value = _uiState.value.copy(
            selectedPosition = snapshot?.selectedPosition,
            name = snapshot?.name ?: _uiState.value.name,
            radius = snapshot?.radius ?: _uiState.value.radius,
            searchText = snapshot?.searchText.orEmpty(),
            hasUserInteractedWithMap = snapshot?.hasUserInteractedWithMap ?: true,
            controlMode = AlarmEditControlMode.Radius,
            inAppSearchQuery = "",
            inAppSearchError = false,
            placeSuggestions = emptyList(),
            isLoadingSuggestions = false,
            placeCandidates = emptyList(),
            currentCandidateIndex = 0,
            candidateSource = null
        )
        inAppSearchSnapshot = null
        inAppSearchBiasCenter = null
    }

    private fun searchSharedPlace(query: String, source: SharedPlaceSource) {
        val trimmedQuery = query.trim()
        val request = trimmedQuery to source
        if (trimmedQuery.isEmpty() || request == lastSharedPlaceRequest) return
        lastSharedPlaceRequest = request
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isSearchingSharedPlace = true,
                showSharedPlaceSearchError = false
            )
            runCatching { placeSearchService.search(trimmedQuery) }
                .onSuccess { candidates ->
                    if (source == SharedPlaceSource.PlainTextAddress) {
                        val candidate = candidates.firstOrNull()
                        if (candidate == null) {
                            showSharedPlaceSearchError()
                        } else {
                            selectSharedPlace(candidate)
                        }
                        return@onSuccess
                    }
                    when (candidates.size) {
                        0 -> showSharedPlaceSearchError()
                        1 -> selectSharedPlace(candidates.single())
                        else -> {
                            _uiState.value = _uiState.value.copy(
                                selectedPosition = null,
                                placeCandidates = candidates.take(MAX_PLACE_CANDIDATES),
                                currentCandidateIndex = 0,
                                isSearchingSharedPlace = false,
                                controlMode = AlarmEditControlMode.Candidates,
                                candidateSource = PlaceCandidateSource.Shared,
                                hasUserInteractedWithMap = true
                            )
                        }
                    }
                }
                .onFailure { showSharedPlaceSearchError() }
        }
    }

    private fun selectSharedPlace(candidate: PlaceCandidate) {
        _uiState.value = _uiState.value.copy(
            selectedPosition = candidate.location,
            searchText = candidate.name,
            name = candidate.name,
            placeCandidates = emptyList(),
            currentCandidateIndex = 0,
            isSearchingSharedPlace = false,
            controlMode = AlarmEditControlMode.Radius,
            candidateSource = null,
            hasUserInteractedWithMap = true
        )
    }

    private fun updateCandidate(index: Int) {
        if (index in _uiState.value.placeCandidates.indices) {
            _uiState.value = _uiState.value.copy(currentCandidateIndex = index)
        }
    }

    private fun confirmCandidate() {
        val candidate = _uiState.value.currentCandidate ?: return
        _uiState.value = _uiState.value.copy(
            selectedPosition = candidate.location,
            searchText = candidate.name,
            name = candidate.name,
            placeCandidates = emptyList(),
            currentCandidateIndex = 0,
            controlMode = AlarmEditControlMode.Radius,
            candidateSource = null,
            hasUserInteractedWithMap = true
        )
        inAppSearchSnapshot = null
        inAppSearchBiasCenter = null
        autocompleteSessionId?.let(placeAutocompleteService::endSession)
        autocompleteSessionId = null
    }

    private fun cancelCandidateSelection() {
        if (_uiState.value.candidateSource == PlaceCandidateSource.InAppAutocomplete) {
            autocompleteSessionId = placeAutocompleteService.startSession()
            _uiState.value = _uiState.value.copy(
                placeCandidates = emptyList(),
                currentCandidateIndex = 0,
                controlMode = AlarmEditControlMode.SearchInput,
                candidateSource = null
            )
            scheduleAutocomplete(_uiState.value.inAppSearchQuery)
            return
        }
        if (_uiState.value.candidateSource == PlaceCandidateSource.InAppTextSearch) {
            cancelInAppSearch()
            return
        }
        _uiState.value = _uiState.value.copy(
            placeCandidates = emptyList(),
            currentCandidateIndex = 0,
            controlMode = AlarmEditControlMode.Radius,
            candidateSource = null
        )
    }

    private fun showSharedPlaceSearchError() {
        _uiState.value = _uiState.value.copy(
            isSearchingSharedPlace = false,
            placeCandidates = emptyList(),
            currentCandidateIndex = 0,
            controlMode = AlarmEditControlMode.Radius,
            candidateSource = null,
            showSharedPlaceSearchError = true
        )
    }

    private fun dismissSharedPlaceSearchError() {
        _uiState.value = _uiState.value.copy(showSharedPlaceSearchError = false)
    }

    private fun updateRadius(radius: Float) {
        _uiState.value = _uiState.value.copy(radius = radius)
    }

    private fun updateName(name: String) {
        _uiState.value = _uiState.value.copy(name = name)
    }

    private fun selectIcon(iconKey: String) {
        _uiState.value = _uiState.value.copy(selectedIconKey = iconKey)
    }

    private fun goToDetailsStep() {
        if (_uiState.value.selectedPosition != null) {
            _uiState.value = _uiState.value.copy(step = AlarmEditStep.DetailsForm)
        }
    }

    private fun goToMapStep() {
        _uiState.value = _uiState.value.copy(step = AlarmEditStep.MapSelection)
    }

    private fun dismissDeleteErrorDialog() {
        _uiState.value = _uiState.value.copy(showDeleteErrorDialog = false)
    }

    private fun dismissDeleteConfirmDialog() {
        _uiState.value = _uiState.value.copy(showDeleteConfirmDialog = false)
    }

    private fun saveAlarm() {
        val state = _uiState.value
        val position = state.selectedPosition ?: return
        val name = state.name.trim()
        if (name.isBlank()) return
        val existing = state.existingAlarm

        viewModelScope.launch {
            val alarmId: String
            if (existing != null) {
                // Update existing alarm
                alarmId = existing.id
                val updatedAlarm = existing.copy(
                    name = name,
                    latitude = position.latitude,
                    longitude = position.longitude,
                    radius = state.radius.toDouble(),
                    iconKey = state.selectedIconKey
                )
                repository.update(updatedAlarm)
            } else {
                // Create new alarm
                alarmId = UUID.randomUUID().toString()
                val newAlarm = Alarm(
                    id = alarmId,
                    name = name,
                    latitude = position.latitude,
                    longitude = position.longitude,
                    radius = state.radius.toDouble(),
                    isEnabled = false,
                    iconKey = state.selectedIconKey
                )
                repository.insert(newAlarm)
            }
            widgetUpdater.refreshAll()
            _uiState.value = _uiState.value.copy(
                isSaved = true,
                savedAlarmId = alarmId,
            )
            _effects.emit(AlarmEditEffect.NavigateBack(alarmId))
        }
    }

    /**
     * Request to delete the alarm. Shows confirmation or error dialog.
     */
    private fun requestDeleteAlarm() {
        val existing = _uiState.value.existingAlarm ?: return
        viewModelScope.launch {
            // Check if alarm is used in any schedule
            val isUsedInSchedule = repository.isAlarmUsedInSchedule(existing.id)
            if (isUsedInSchedule) {
                _uiState.value = _uiState.value.copy(showDeleteErrorDialog = true)
            } else {
                _uiState.value = _uiState.value.copy(showDeleteConfirmDialog = true)
            }
        }
    }

    /**
     * Confirm and execute the deletion.
     */
    private fun confirmDeleteAlarm() {
        val existing = _uiState.value.existingAlarm ?: return
        viewModelScope.launch {
            repository.delete(existing)
            widgetUpdater.refreshAll()
            _uiState.value = _uiState.value.copy(
                isSaved = true,
                showDeleteConfirmDialog = false
            )
            _effects.emit(AlarmEditEffect.NavigateBack(null))
        }
    }

    private companion object {
        const val MAX_PLACE_CANDIDATES = 5
        const val MIN_AUTOCOMPLETE_QUERY_LENGTH = 2
        const val AUTOCOMPLETE_DEBOUNCE_MS = 300L
    }

    private data class InAppSearchSnapshot(
        val selectedPosition: LatLng?,
        val name: String,
        val radius: Float,
        val searchText: String,
        val hasUserInteractedWithMap: Boolean
    )
}
