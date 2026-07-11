package com.github.jimmy90109.geoalarm.ads

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdsEligibilityTest {
    @Test
    fun shouldShowHomeNativeAd_whenConsentAllowedContentExistsAndAdsNotRemoved() {
        assertTrue(
            AdsEligibility.shouldShowHomeNativeAd(
                canRequestAds = true,
                hasAdsRemoved = false,
                hasHomeContent = true,
            )
        )
    }

    @Test
    fun shouldNotShowHomeNativeAd_withoutConsent() {
        assertFalse(
            AdsEligibility.shouldShowHomeNativeAd(
                canRequestAds = false,
                hasAdsRemoved = false,
                hasHomeContent = true,
            )
        )
    }

    @Test
    fun shouldNotShowHomeNativeAd_whenAdsRemoved() {
        assertFalse(
            AdsEligibility.shouldShowHomeNativeAd(
                canRequestAds = true,
                hasAdsRemoved = true,
                hasHomeContent = true,
            )
        )
    }

    @Test
    fun shouldNotShowHomeNativeAd_withoutHomeContent() {
        assertFalse(
            AdsEligibility.shouldShowHomeNativeAd(
                canRequestAds = true,
                hasAdsRemoved = false,
                hasHomeContent = false,
            )
        )
    }
}
