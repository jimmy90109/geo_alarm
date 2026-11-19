package com.github.jimmy90109.geoalarm.ui.viewmodel

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaceReminderEditUiStateTest {
    @Test
    fun `opening place picker from reminder form does not show place details`() {
        val state = PlaceReminderEditUiState(
            reminderId = "saved-reminder",
            isLoading = false,
            step = AlarmEditStep.DetailsForm,
            isSelectingPlace = false,
        )

        assertFalse(state.isShowingPlaceDetails)
        assertFalse(state.copy(isSelectingPlace = true, step = AlarmEditStep.MapSelection).isShowingPlaceDetails)
    }

    @Test
    fun `place details are visible after next and hidden after confirmation`() {
        val state = PlaceReminderEditUiState(
            isLoading = false,
            isSelectingPlace = true,
            step = AlarmEditStep.DetailsForm,
        )

        assertTrue(state.isShowingPlaceDetails)
        assertFalse(state.copy(step = AlarmEditStep.MapSelection).isShowingPlaceDetails)
        assertFalse(state.copy(isSelectingPlace = false).isShowingPlaceDetails)
    }
}
