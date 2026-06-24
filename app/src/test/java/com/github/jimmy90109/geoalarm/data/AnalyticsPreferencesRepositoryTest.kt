package com.github.jimmy90109.geoalarm.data

import androidx.datastore.preferences.core.emptyPreferences
import org.junit.Assert.assertFalse
import org.junit.Test

class AnalyticsPreferencesRepositoryTest {

    @Test
    fun `missing analytics preference defaults to disabled`() {
        assertFalse(AnalyticsPreferencesRepository.analyticsEnabled(emptyPreferences()))
    }
}
