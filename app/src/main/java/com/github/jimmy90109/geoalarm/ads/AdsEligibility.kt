package com.github.jimmy90109.geoalarm.ads

object AdsEligibility {
    fun shouldShowHomeNativeAd(
        canRequestAds: Boolean,
        hasAdsRemoved: Boolean,
        hasHomeContent: Boolean,
    ): Boolean {
        return canRequestAds && !hasAdsRemoved && hasHomeContent
    }
}
