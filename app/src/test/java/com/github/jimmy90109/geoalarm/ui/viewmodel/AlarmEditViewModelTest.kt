package com.github.jimmy90109.geoalarm.ui.viewmodel

import com.github.jimmy90109.geoalarm.data.Alarm
import com.github.jimmy90109.geoalarm.data.AlarmDao
import com.github.jimmy90109.geoalarm.data.AlarmRepository
import com.github.jimmy90109.geoalarm.data.DEFAULT_ALARM_ICON_KEY
import com.github.jimmy90109.geoalarm.data.AlarmSchedule
import com.github.jimmy90109.geoalarm.data.ScheduleDao
import com.github.jimmy90109.geoalarm.data.ScheduleWithAlarm
import com.github.jimmy90109.geoalarm.data.location.CurrentLocationRepository
import com.github.jimmy90109.geoalarm.data.places.PlaceCandidate
import com.github.jimmy90109.geoalarm.data.places.PlaceAutocompleteService
import com.github.jimmy90109.geoalarm.data.places.PlaceSearchService
import com.github.jimmy90109.geoalarm.data.places.PlaceSuggestion
import com.github.jimmy90109.geoalarm.share.SharedPlaceSource
import com.github.jimmy90109.geoalarm.widget.WidgetUpdater
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description

@OptIn(ExperimentalCoroutinesApi::class)
class AlarmEditViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `loadAlarm with existing id updates ui state`() = runTest {
        val existing = Alarm(
            id = "alarm-1",
            name = "Office",
            latitude = 25.1,
            longitude = 121.5,
            radius = 800.0,
            isEnabled = false
        )
        val repository = buildRepository(alarms = listOf(existing))
        val viewModel = createViewModel(repository)

        viewModel.onAction(AlarmEditAction.LoadAlarm(existing.id))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(existing, state.existingAlarm)
        assertEquals(LatLng(25.1, 121.5), state.selectedPosition)
        assertEquals(800f, state.radius)
        assertEquals("Office", state.name)
        assertEquals(DEFAULT_ALARM_ICON_KEY, state.selectedIconKey)
        assertFalse(state.isLoading)
    }

    @Test
    fun `loadAlarm with null id only clears loading`() = runTest {
        val viewModel = createViewModel(buildRepository())

        viewModel.onAction(AlarmEditAction.LoadAlarm(null))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.existingAlarm)
    }

    @Test
    fun `saveAlarm without position does nothing`() = runTest {
        val alarmDao = FakeAlarmDao()
        val viewModel = createViewModel(buildRepository(alarmDao = alarmDao))

        viewModel.onAction(AlarmEditAction.NameChanged("Home"))
        viewModel.onAction(AlarmEditAction.SaveClicked)
        advanceUntilIdle()

        assertTrue(alarmDao.inserted.isEmpty())
        assertFalse(viewModel.uiState.value.isSaved)
        assertNull(viewModel.uiState.value.savedAlarmId)
    }

    @Test
    fun `saveAlarm creates new alarm and marks saved`() = runTest {
        val alarmDao = FakeAlarmDao()
        val viewModel = createViewModel(buildRepository(alarmDao = alarmDao))
        val effects = mutableListOf<AlarmEditEffect>()
        val effectsJob = launch { viewModel.effects.collect { effects += it } }
        viewModel.onAction(AlarmEditAction.PositionSelected(LatLng(24.9, 121.1)))
        viewModel.onAction(AlarmEditAction.RadiusChanged(1200f))
        viewModel.onAction(AlarmEditAction.NameChanged("Gym"))

        viewModel.onAction(AlarmEditAction.SaveClicked)
        advanceUntilIdle()

        assertEquals(1, alarmDao.inserted.size)
        val created = alarmDao.inserted.single()
        assertEquals("Gym", created.name)
        assertEquals(24.9, created.latitude, 0.0)
        assertEquals(121.1, created.longitude, 0.0)
        assertEquals(1200.0, created.radius, 0.0)
        assertFalse(created.isEnabled)
        assertEquals(DEFAULT_ALARM_ICON_KEY, created.iconKey)

        val state = viewModel.uiState.value
        assertTrue(state.isSaved)
        assertNotNull(state.savedAlarmId)
        assertEquals(listOf(AlarmEditEffect.NavigateBack(state.savedAlarmId)), effects)
        effectsJob.cancel()
    }

    @Test
    fun `saveAlarm updates existing alarm`() = runTest {
        val existing = Alarm(
            id = "alarm-2",
            name = "Old name",
            latitude = 10.0,
            longitude = 10.0,
            radius = 500.0,
            isEnabled = true
        )
        val alarmDao = FakeAlarmDao(initialAlarms = listOf(existing))
        val repository = buildRepository(alarmDao = alarmDao)
        val viewModel = createViewModel(repository)
        viewModel.onAction(AlarmEditAction.LoadAlarm(existing.id))
        advanceUntilIdle()
        viewModel.onAction(AlarmEditAction.PositionSelected(LatLng(11.0, 12.0)))
        viewModel.onAction(AlarmEditAction.RadiusChanged(900f))
        viewModel.onAction(AlarmEditAction.NameChanged("New name"))

        viewModel.onAction(AlarmEditAction.SaveClicked)
        advanceUntilIdle()

        assertEquals(1, alarmDao.updated.size)
        val updated = alarmDao.updated.single()
        assertEquals(existing.id, updated.id)
        assertEquals("New name", updated.name)
        assertEquals(11.0, updated.latitude, 0.0)
        assertEquals(12.0, updated.longitude, 0.0)
        assertEquals(900.0, updated.radius, 0.0)
        assertTrue(updated.isEnabled)
        assertTrue(viewModel.uiState.value.isSaved)
        assertEquals(existing.id, viewModel.uiState.value.savedAlarmId)
    }

    @Test
    fun `goToDetailsStep changes step when position exists and goToMapStep restores`() = runTest {
        val viewModel = createViewModel(buildRepository())
        viewModel.onAction(AlarmEditAction.LoadAlarm(null))
        advanceUntilIdle()

        viewModel.onAction(AlarmEditAction.NextClicked)
        assertEquals(AlarmEditStep.MapSelection, viewModel.uiState.value.step)

        viewModel.onAction(AlarmEditAction.PositionSelected(LatLng(25.0, 121.0)))
        viewModel.onAction(AlarmEditAction.NextClicked)
        assertEquals(AlarmEditStep.DetailsForm, viewModel.uiState.value.step)

        viewModel.onAction(AlarmEditAction.BackToMapClicked)
        assertEquals(AlarmEditStep.MapSelection, viewModel.uiState.value.step)
    }

    @Test
    fun `shared place search with one result selects and names place`() = runTest {
        val candidate = placeCandidate("one", "Memorial Hall", 25.0, 121.5)
        val viewModel = createViewModel(
            repository = buildRepository(),
            placeSearchService = FakePlaceSearchService(listOf(candidate))
        )

        viewModel.onAction(AlarmEditAction.SearchSharedPlace("Memorial Hall"))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(candidate.location, state.selectedPosition)
        assertEquals(candidate.name, state.name)
        assertTrue(state.placeCandidates.isEmpty())
    }

    @Test
    fun `shared place search limits candidates and confirm selects current candidate`() = runTest {
        val candidates = (0..6).map {
            placeCandidate("$it", "Place $it", 25.0 + it, 121.0 + it)
        }
        val viewModel = createViewModel(
            repository = buildRepository(),
            placeSearchService = FakePlaceSearchService(candidates)
        )

        viewModel.onAction(AlarmEditAction.SearchSharedPlace("Place"))
        advanceUntilIdle()
        assertEquals(5, viewModel.uiState.value.placeCandidates.size)
        assertNull(viewModel.uiState.value.selectedPosition)

        viewModel.onAction(AlarmEditAction.CandidateChanged(2))
        viewModel.onAction(AlarmEditAction.CandidateConfirmed)

        val state = viewModel.uiState.value
        assertEquals(candidates[2].location, state.selectedPosition)
        assertEquals(candidates[2].name, state.name)
        assertTrue(state.placeCandidates.isEmpty())
    }

    @Test
    fun `cancelling shared place candidates leaves position unselected`() = runTest {
        val viewModel = createViewModel(
            repository = buildRepository(),
            placeSearchService = FakePlaceSearchService(
                listOf(
                    placeCandidate("1", "First", 25.0, 121.0),
                    placeCandidate("2", "Second", 26.0, 122.0)
                )
            )
        )
        viewModel.onAction(AlarmEditAction.SearchSharedPlace("Place"))
        advanceUntilIdle()

        viewModel.onAction(AlarmEditAction.CandidateSelectionCancelled)

        assertNull(viewModel.uiState.value.selectedPosition)
        assertTrue(viewModel.uiState.value.placeCandidates.isEmpty())
    }

    @Test
    fun `empty shared place search result exposes error`() = runTest {
        val viewModel = createViewModel(
            repository = buildRepository(),
            placeSearchService = FakePlaceSearchService(emptyList())
        )

        viewModel.onAction(AlarmEditAction.SearchSharedPlace("Missing"))
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.showSharedPlaceSearchError)
        assertFalse(viewModel.uiState.value.isSearchingSharedPlace)
    }

    @Test
    fun `failed shared place search exposes error`() = runTest {
        val viewModel = createViewModel(
            repository = buildRepository(),
            placeSearchService = FakePlaceSearchService(error = IllegalStateException("network"))
        )

        viewModel.onAction(AlarmEditAction.SearchSharedPlace("Unavailable"))
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.showSharedPlaceSearchError)
        assertTrue(viewModel.uiState.value.placeCandidates.isEmpty())
    }

    @Test
    fun `plain text address search with multiple results selects first result`() = runTest {
        val candidates = listOf(
            placeCandidate("first", "First result", 25.0, 121.5),
            placeCandidate("second", "Second result", 25.1, 121.6)
        )
        val placeSearchService = FakePlaceSearchService(candidates)
        val viewModel = createViewModel(
            repository = buildRepository(),
            placeSearchService = placeSearchService
        )

        viewModel.onAction(
            AlarmEditAction.SearchSharedPlace(
                query = "台北市中正區中山南路21號",
                source = SharedPlaceSource.PlainTextAddress
            )
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(candidates.first().location, state.selectedPosition)
        assertEquals(candidates.first().name, state.name)
        assertEquals(AlarmEditControlMode.Radius, state.controlMode)
        assertTrue(state.placeCandidates.isEmpty())
        assertEquals(listOf(null), placeSearchService.locationBiasCenters)
    }

    @Test
    fun `empty plain text address search exposes error`() = runTest {
        val viewModel = createViewModel(
            repository = buildRepository(),
            placeSearchService = FakePlaceSearchService(emptyList())
        )

        viewModel.onAction(
            AlarmEditAction.SearchSharedPlace(
                query = "Missing address",
                source = SharedPlaceSource.PlainTextAddress
            )
        )
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.showSharedPlaceSearchError)
        assertNull(viewModel.uiState.value.selectedPosition)
    }

    @Test
    fun `in app search with one result selects place and passes map center bias`() = runTest {
        val candidate = placeCandidate("one", "Memorial Hall", 25.0, 121.5)
        val mapCenter = LatLng(25.04, 121.52)
        val placeSearchService = FakePlaceSearchService(listOf(candidate))
        val viewModel = createViewModel(buildRepository(), placeSearchService = placeSearchService)

        viewModel.onAction(AlarmEditAction.StartInAppSearch(mapCenter))
        viewModel.onAction(AlarmEditAction.InAppSearchQueryChanged("Memorial Hall"))
        viewModel.onAction(AlarmEditAction.SubmitInAppSearch)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(AlarmEditControlMode.Radius, state.controlMode)
        assertNull(state.candidateSource)
        assertTrue(state.placeCandidates.isEmpty())
        assertEquals(candidate.location, state.selectedPosition)
        assertEquals(candidate.name, state.name)
        assertEquals(listOf("Memorial Hall"), placeSearchService.queries)
        assertEquals(listOf(mapCenter), placeSearchService.locationBiasCenters)
    }

    @Test
    fun `in app search with multiple results shows candidates`() = runTest {
        val candidates = listOf(
            placeCandidate("one", "First", 25.0, 121.5),
            placeCandidate("two", "Second", 25.1, 121.6)
        )
        val viewModel = createViewModel(
            repository = buildRepository(),
            placeSearchService = FakePlaceSearchService(candidates)
        )

        viewModel.onAction(AlarmEditAction.StartInAppSearch(LatLng(25.04, 121.52)))
        viewModel.onAction(AlarmEditAction.InAppSearchQueryChanged("Place"))
        viewModel.onAction(AlarmEditAction.SubmitInAppSearch)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(AlarmEditControlMode.Candidates, state.controlMode)
        assertEquals(PlaceCandidateSource.InAppTextSearch, state.candidateSource)
        assertEquals(candidates, state.placeCandidates)
        assertNull(state.selectedPosition)
    }

    @Test
    fun `cancelling in app search restores full selection snapshot`() = runTest {
        val originalPosition = LatLng(24.9, 121.1)
        val viewModel = createViewModel(buildRepository())
        viewModel.onAction(AlarmEditAction.SearchPositionSelected(originalPosition, "Original"))
        viewModel.onAction(AlarmEditAction.NameChanged("Custom name"))
        viewModel.onAction(AlarmEditAction.RadiusChanged(1350f))

        viewModel.onAction(AlarmEditAction.StartInAppSearch(LatLng(25.0, 121.5)))
        viewModel.onAction(AlarmEditAction.InAppSearchQueryChanged("Replacement"))
        viewModel.onAction(AlarmEditAction.CancelInAppSearch)

        val state = viewModel.uiState.value
        assertEquals(AlarmEditControlMode.Radius, state.controlMode)
        assertEquals(originalPosition, state.selectedPosition)
        assertEquals("Original", state.searchText)
        assertEquals("Custom name", state.name)
        assertEquals(1350f, state.radius)
        assertTrue(state.placeCandidates.isEmpty())
    }

    @Test
    fun `failed in app search returns to input and retains query`() = runTest {
        val viewModel = createViewModel(
            repository = buildRepository(),
            placeSearchService = FakePlaceSearchService(error = IllegalStateException("network"))
        )
        viewModel.onAction(AlarmEditAction.StartInAppSearch(LatLng(25.0, 121.5)))
        viewModel.onAction(AlarmEditAction.InAppSearchQueryChanged("Unavailable"))

        viewModel.onAction(AlarmEditAction.SubmitInAppSearch)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(AlarmEditControlMode.SearchInput, state.controlMode)
        assertEquals("Unavailable", state.inAppSearchQuery)
        assertTrue(state.inAppSearchError)
    }

    @Test
    fun `blank in app search query is not submitted`() = runTest {
        val placeSearchService = FakePlaceSearchService()
        val viewModel = createViewModel(buildRepository(), placeSearchService = placeSearchService)
        viewModel.onAction(AlarmEditAction.StartInAppSearch(LatLng(25.0, 121.5)))
        viewModel.onAction(AlarmEditAction.InAppSearchQueryChanged("   "))

        viewModel.onAction(AlarmEditAction.SubmitInAppSearch)
        advanceUntilIdle()

        assertEquals(AlarmEditControlMode.SearchInput, viewModel.uiState.value.controlMode)
        assertTrue(placeSearchService.queries.isEmpty())
    }

    @Test
    fun `autocomplete starts after two characters and uses map center bias`() = runTest {
        val suggestion = PlaceSuggestion("one", "台北車站", "台北市", "台北車站 台北市")
        val autocompleteService = FakePlaceAutocompleteService(suggestions = listOf(suggestion))
        val mapCenter = LatLng(25.04, 121.52)
        val viewModel = createViewModel(
            buildRepository(),
            placeAutocompleteService = autocompleteService
        )
        viewModel.onAction(AlarmEditAction.StartInAppSearch(mapCenter))

        viewModel.onAction(AlarmEditAction.InAppSearchQueryChanged("台"))
        advanceUntilIdle()
        assertTrue(autocompleteService.queries.isEmpty())

        viewModel.onAction(AlarmEditAction.InAppSearchQueryChanged("台北"))
        advanceUntilIdle()

        assertEquals(listOf("台北"), autocompleteService.queries)
        assertEquals(listOf(mapCenter), autocompleteService.locationBiasCenters)
        assertEquals(listOf(suggestion), viewModel.uiState.value.placeSuggestions)
    }

    @Test
    fun `selecting second autocomplete suggestion opens carousel at second result`() = runTest {
        val suggestions = listOf(
            PlaceSuggestion("one", "First", "First address", "First full"),
            PlaceSuggestion("two", "Second", "Second address", "Second full"),
            PlaceSuggestion("three", "Third", "Third address", "Third full")
        )
        val candidates = listOf(
            placeCandidate("one", "First", 25.0, 121.5),
            placeCandidate("two", "Second", 25.1, 121.6),
            placeCandidate("three", "Third", 25.2, 121.7)
        )
        val autocompleteService = FakePlaceAutocompleteService(suggestions, candidates)
        val viewModel = createViewModel(
            buildRepository(),
            placeAutocompleteService = autocompleteService
        )
        viewModel.onAction(AlarmEditAction.StartInAppSearch(LatLng(25.04, 121.52)))
        viewModel.onAction(AlarmEditAction.InAppSearchQueryChanged("Place"))
        advanceUntilIdle()

        viewModel.onAction(AlarmEditAction.PlaceSuggestionSelected(1))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(AlarmEditControlMode.Candidates, state.controlMode)
        assertEquals(PlaceCandidateSource.InAppAutocomplete, state.candidateSource)
        assertEquals(candidates, state.placeCandidates)
        assertEquals(1, state.currentCandidateIndex)
        assertEquals(listOf("two"), autocompleteService.selectedPlaceIds)

        viewModel.onAction(AlarmEditAction.CandidateSelectionCancelled)
        assertEquals(AlarmEditControlMode.SearchInput, viewModel.uiState.value.controlMode)
        assertEquals(suggestions, viewModel.uiState.value.placeSuggestions)
        assertEquals("Place", viewModel.uiState.value.inAppSearchQuery)
    }

    @Test
    fun `cancelled in app search ignores delayed result`() = runTest {
        val deferredResult = CompletableDeferred<List<PlaceCandidate>>()
        val placeSearchService = object : PlaceSearchService {
            override suspend fun search(
                query: String,
                locationBiasCenter: LatLng?
            ): List<PlaceCandidate> = deferredResult.await()
        }
        val originalPosition = LatLng(24.9, 121.1)
        val viewModel = createViewModel(buildRepository(), placeSearchService = placeSearchService)
        viewModel.onAction(AlarmEditAction.PositionSelected(originalPosition))
        viewModel.onAction(AlarmEditAction.StartInAppSearch(LatLng(25.0, 121.5)))
        viewModel.onAction(AlarmEditAction.InAppSearchQueryChanged("Delayed"))
        viewModel.onAction(AlarmEditAction.SubmitInAppSearch)

        assertEquals(AlarmEditControlMode.SearchLoading, viewModel.uiState.value.controlMode)
        assertEquals("Delayed", viewModel.uiState.value.inAppSearchQuery)

        viewModel.onAction(AlarmEditAction.CancelInAppSearch)

        deferredResult.complete(listOf(placeCandidate("late", "Late result", 25.1, 121.6)))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(AlarmEditControlMode.Radius, state.controlMode)
        assertEquals(originalPosition, state.selectedPosition)
        assertTrue(state.placeCandidates.isEmpty())
    }

    @Test
    fun `new alarm receives cached current location`() = runTest {
        val currentLocationRepository = FakeCurrentLocationRepository(
            initialLocation = LatLng(25.2, 121.6)
        )
        val viewModel = createViewModel(
            repository = buildRepository(),
            currentLocationRepository = currentLocationRepository
        )

        viewModel.onAction(AlarmEditAction.LoadAlarm(null))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(LatLng(25.2, 121.6), state.currentLocation)
        assertNull(state.selectedPosition)
        assertFalse(state.hasUserInteractedWithMap)
        assertEquals(1, currentLocationRepository.warmUpCount)
    }

    @Test
    fun `existing alarm keeps selected alarm position when current location exists`() = runTest {
        val existing = Alarm(
            id = "alarm-current-location",
            name = "Station",
            latitude = 24.9,
            longitude = 121.1,
            radius = 800.0,
            isEnabled = false
        )
        val viewModel = createViewModel(
            repository = buildRepository(alarms = listOf(existing)),
            currentLocationRepository = FakeCurrentLocationRepository(
                initialLocation = LatLng(25.2, 121.6)
            )
        )

        viewModel.onAction(AlarmEditAction.LoadAlarm(existing.id))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(LatLng(24.9, 121.1), state.selectedPosition)
        assertEquals(LatLng(25.2, 121.6), state.currentLocation)
        assertTrue(state.hasUserInteractedWithMap)
    }

    @Test
    fun `map interaction is retained when current location arrives later`() = runTest {
        val currentLocationRepository = FakeCurrentLocationRepository()
        val viewModel = createViewModel(
            repository = buildRepository(),
            currentLocationRepository = currentLocationRepository
        )
        viewModel.onAction(AlarmEditAction.LoadAlarm(null))
        viewModel.onAction(AlarmEditAction.MapInteracted)

        currentLocationRepository.emit(LatLng(25.2, 121.6))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(LatLng(25.2, 121.6), state.currentLocation)
        assertNull(state.selectedPosition)
        assertTrue(state.hasUserInteractedWithMap)
    }

    @Test
    fun `saveAlarm stores selected icon`() = runTest {
        val alarmDao = FakeAlarmDao()
        val viewModel = createViewModel(buildRepository(alarmDao = alarmDao))
        viewModel.onAction(AlarmEditAction.PositionSelected(LatLng(24.9, 121.1)))
        viewModel.onAction(AlarmEditAction.NameChanged("Office"))
        viewModel.onAction(AlarmEditAction.IconSelected("train"))

        viewModel.onAction(AlarmEditAction.SaveClicked)
        advanceUntilIdle()

        val created = alarmDao.inserted.single()
        assertEquals("train", created.iconKey)
    }

    @Test
    fun `requestDeleteAlarm shows error dialog when alarm used by schedule`() = runTest {
        val existing = Alarm(
            id = "alarm-3",
            name = "Mall",
            latitude = 1.0,
            longitude = 2.0,
            radius = 300.0,
            isEnabled = false
        )
        val scheduleDao = FakeScheduleDao(isAlarmUsed = true)
        val viewModel = createViewModel(
            buildRepository(
                alarms = listOf(existing),
                scheduleDao = scheduleDao
            )
        )
        viewModel.onAction(AlarmEditAction.LoadAlarm(existing.id))
        advanceUntilIdle()

        viewModel.onAction(AlarmEditAction.DeleteRequested)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.showDeleteErrorDialog)
        assertFalse(viewModel.uiState.value.showDeleteConfirmDialog)
    }

    @Test
    fun `confirmDeleteAlarm deletes alarm and marks saved`() = runTest {
        val existing = Alarm(
            id = "alarm-4",
            name = "School",
            latitude = 3.0,
            longitude = 4.0,
            radius = 700.0,
            isEnabled = false
        )
        val alarmDao = FakeAlarmDao(initialAlarms = listOf(existing))
        val scheduleDao = FakeScheduleDao(isAlarmUsed = false)
        val viewModel = createViewModel(
            buildRepository(
                alarmDao = alarmDao,
                scheduleDao = scheduleDao
            )
        )
        viewModel.onAction(AlarmEditAction.LoadAlarm(existing.id))
        advanceUntilIdle()
        viewModel.onAction(AlarmEditAction.DeleteRequested)
        advanceUntilIdle()
        val effects = mutableListOf<AlarmEditEffect>()
        val effectsJob = launch { viewModel.effects.collect { effects += it } }

        viewModel.onAction(AlarmEditAction.DeleteConfirmed)
        advanceUntilIdle()

        assertEquals(listOf(existing), alarmDao.deleted)
        assertTrue(viewModel.uiState.value.isSaved)
        assertFalse(viewModel.uiState.value.showDeleteConfirmDialog)
        assertEquals(listOf(AlarmEditEffect.NavigateBack(null)), effects)
        effectsJob.cancel()
    }

    private fun buildRepository(
        alarms: List<Alarm> = emptyList(),
        alarmDao: FakeAlarmDao = FakeAlarmDao(initialAlarms = alarms),
        scheduleDao: FakeScheduleDao = FakeScheduleDao()
    ): AlarmRepository = AlarmRepository(alarmDao, scheduleDao)

    private fun createViewModel(
        repository: AlarmRepository,
        widgetUpdater: WidgetUpdater = FakeWidgetUpdater(),
        currentLocationRepository: CurrentLocationRepository = FakeCurrentLocationRepository(),
        placeSearchService: PlaceSearchService = FakePlaceSearchService(),
        placeAutocompleteService: PlaceAutocompleteService = FakePlaceAutocompleteService()
    ): AlarmEditViewModel = AlarmEditViewModel(
        repository,
        widgetUpdater,
        currentLocationRepository,
        placeSearchService,
        placeAutocompleteService
    )

    private fun placeCandidate(
        id: String,
        name: String,
        latitude: Double,
        longitude: Double
    ) = PlaceCandidate(id, name, "$name address", LatLng(latitude, longitude))
}

@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    private val dispatcher: TestDispatcher = StandardTestDispatcher()
) : TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(dispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}

private class FakeWidgetUpdater : WidgetUpdater {
    override suspend fun refreshAll() = Unit
}

private class FakeCurrentLocationRepository(
    initialLocation: LatLng? = null
) : CurrentLocationRepository {
    private val locations = MutableStateFlow(initialLocation)
    override val currentLocation: StateFlow<LatLng?> = locations
    var warmUpCount = 0
        private set

    override suspend fun warmUp() {
        warmUpCount += 1
    }

    fun emit(location: LatLng?) {
        locations.value = location
    }
}

private class FakePlaceSearchService(
    private val results: List<PlaceCandidate> = emptyList(),
    private val error: Throwable? = null
) : PlaceSearchService {
    val queries = mutableListOf<String>()
    val locationBiasCenters = mutableListOf<LatLng?>()

    override suspend fun search(query: String, locationBiasCenter: LatLng?): List<PlaceCandidate> {
        queries += query
        locationBiasCenters += locationBiasCenter
        error?.let { throw it }
        return results
    }
}

private class FakePlaceAutocompleteService(
    private val suggestions: List<PlaceSuggestion> = emptyList(),
    private val candidates: List<PlaceCandidate> = emptyList()
) : PlaceAutocompleteService {
    val queries = mutableListOf<String>()
    val locationBiasCenters = mutableListOf<LatLng?>()
    val selectedPlaceIds = mutableListOf<String>()
    private var sessionCount = 0

    override fun startSession(): String = "session-${++sessionCount}"
    override fun endSession(sessionId: String) = Unit

    override suspend fun suggestions(
        query: String,
        locationBiasCenter: LatLng?,
        sessionId: String
    ): List<PlaceSuggestion> {
        queries += query
        locationBiasCenters += locationBiasCenter
        return suggestions
    }

    override suspend fun resolveCandidates(
        suggestions: List<PlaceSuggestion>,
        selectedPlaceId: String,
        sessionId: String
    ): List<PlaceCandidate> {
        selectedPlaceIds += selectedPlaceId
        return candidates
    }
}

private class FakeAlarmDao(
    initialAlarms: List<Alarm> = emptyList()
) : AlarmDao {
    private val storage = LinkedHashMap<String, Alarm>().apply {
        initialAlarms.forEach { put(it.id, it) }
    }
    private val alarmsFlow = MutableStateFlow(storage.values.toList())

    val inserted = mutableListOf<Alarm>()
    val updated = mutableListOf<Alarm>()
    val deleted = mutableListOf<Alarm>()

    override fun getAllAlarms(): Flow<List<Alarm>> = alarmsFlow

    override suspend fun getAllAlarmsOneShot(): List<Alarm> = storage.values.toList()

    override suspend fun getAlarmById(id: String): Alarm? = storage[id]

    override suspend fun findAlarmsByName(name: String): List<Alarm> =
        storage.values.filter { it.name.trim() == name.trim() }

    override suspend fun insertAlarm(alarm: Alarm) {
        inserted += alarm
        storage[alarm.id] = alarm
        alarmsFlow.update { storage.values.toList() }
    }

    override suspend fun deleteAlarm(alarm: Alarm) {
        deleted += alarm
        storage.remove(alarm.id)
        alarmsFlow.update { storage.values.toList() }
    }

    override suspend fun updateAlarm(alarm: Alarm) {
        updated += alarm
        storage[alarm.id] = alarm
        alarmsFlow.update { storage.values.toList() }
    }
}

private class FakeScheduleDao(
    private val isAlarmUsed: Boolean = false
) : ScheduleDao {
    private val schedulesFlow = MutableStateFlow<List<AlarmSchedule>>(emptyList())
    private val schedulesWithAlarmFlow = MutableStateFlow<List<ScheduleWithAlarm>>(emptyList())

    override fun getAllSchedules(): Flow<List<AlarmSchedule>> = schedulesFlow

    override fun getAllSchedulesWithAlarm(): Flow<List<ScheduleWithAlarm>> = schedulesWithAlarmFlow

    override fun getSchedulesForAlarm(alarmId: String): Flow<List<AlarmSchedule>> =
        MutableStateFlow(emptyList())

    override suspend fun getSchedulesForAlarmOneShot(alarmId: String): List<AlarmSchedule> = emptyList()

    override suspend fun insertSchedule(schedule: AlarmSchedule) = Unit

    override suspend fun deleteSchedule(schedule: AlarmSchedule) = Unit

    override suspend fun updateSchedule(schedule: AlarmSchedule) = Unit

    override suspend fun getScheduleById(id: String): AlarmSchedule? = null

    override suspend fun isAlarmUsedInSchedule(alarmId: String): Boolean = isAlarmUsed
}
