package com.github.jimmy90109.geoalarm.util

import org.junit.Assert.assertEquals
import org.junit.Test

class PlayStoreListingLauncherTest {
    @Test
    fun `uses market when Play Store can handle intent`() {
        assertEquals(
            PlayStoreListingLauncher.ListingTarget.Market,
            PlayStoreListingLauncher.planTarget(canHandleMarketIntent = true),
        )
    }

    @Test
    fun `falls back to web when market intent is unavailable`() {
        assertEquals(
            PlayStoreListingLauncher.ListingTarget.Web,
            PlayStoreListingLauncher.planTarget(canHandleMarketIntent = false),
        )
    }

    @Test
    fun `web url points to release package name`() {
        assertEquals(
            "https://play.google.com/store/apps/details?id=com.github.jimmy90109.geoalarm",
            PlayStoreListingLauncher.webUrl(),
        )
    }
}
