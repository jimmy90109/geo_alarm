package com.github.jimmy90109.geoalarm.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.github.jimmy90109.geoalarm.BuildConfig
import com.google.android.gms.ads.MobileAds
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class AdConsentState(
    val canRequestAds: Boolean = false,
    val isPrivacyOptionsRequired: Boolean = false,
    val isMobileAdsInitialized: Boolean = false,
)

@Singleton
class AdConsentManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    private val consentInformation: ConsentInformation =
        UserMessagingPlatform.getConsentInformation(context)
    private val _state = MutableStateFlow(readConsentState())
    val state: StateFlow<AdConsentState> = _state.asStateFlow()
    private var mobileAdsInitialized = false
    private var mobileAdsInitializationStarted = false
    private var consentUpdateInFlight = false

    fun requestConsentUpdate(activity: Activity) {
        if (!BuildConfig.ADS_ENABLED || consentUpdateInFlight) {
            refreshState()
            initializeMobileAdsIfAllowed()
            return
        }

        consentUpdateInFlight = true
        val params = ConsentRequestParameters.Builder().build()
        consentInformation.requestConsentInfoUpdate(
            activity,
            params,
            {
                consentUpdateInFlight = false
                refreshState()
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { formError ->
                    formError?.let {
                        Log.w(TAG, "Consent form error: ${it.errorCode} ${it.message}")
                    }
                    refreshState()
                    initializeMobileAdsIfAllowed()
                }
            },
            { requestError ->
                consentUpdateInFlight = false
                Log.w(TAG, "Consent update error: ${requestError.errorCode} ${requestError.message}")
                refreshState()
                initializeMobileAdsIfAllowed()
            },
        )
    }

    fun showPrivacyOptionsForm(activity: Activity) {
        if (!BuildConfig.ADS_ENABLED || !state.value.isPrivacyOptionsRequired) return
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { formError ->
            formError?.let {
                Log.w(TAG, "Privacy options form error: ${it.errorCode} ${it.message}")
            }
            refreshState()
            initializeMobileAdsIfAllowed()
        }
    }

    private fun initializeMobileAdsIfAllowed() {
        if (!BuildConfig.ADS_ENABLED ||
            mobileAdsInitializationStarted ||
            !consentInformation.canRequestAds()
        ) {
            return
        }
        mobileAdsInitializationStarted = true
        MobileAds.initialize(context) {
            mobileAdsInitialized = true
            Log.d(TAG, "Mobile Ads initialized")
            refreshState()
        }
    }

    private fun refreshState() {
        _state.update { readConsentState() }
    }

    private fun readConsentState(): AdConsentState {
        if (!BuildConfig.ADS_ENABLED) return AdConsentState()
        return AdConsentState(
            canRequestAds = consentInformation.canRequestAds() && mobileAdsInitialized,
            isPrivacyOptionsRequired =
                consentInformation.privacyOptionsRequirementStatus ==
                    ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED,
            isMobileAdsInitialized = mobileAdsInitialized,
        )
    }

    private companion object {
        private const val TAG = "AdConsentManager"
    }
}
