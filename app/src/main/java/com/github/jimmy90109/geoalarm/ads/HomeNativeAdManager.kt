package com.github.jimmy90109.geoalarm.ads

import android.content.Context
import android.os.SystemClock
import android.util.Log
import com.github.jimmy90109.geoalarm.BuildConfig
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.NativeAd
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed interface HomeNativeAdState {
    data object Idle : HomeNativeAdState
    data object Loading : HomeNativeAdState
    data class Loaded(val nativeAd: NativeAd) : HomeNativeAdState
    data class Failed(val message: String) : HomeNativeAdState
}

@Singleton
class HomeNativeAdManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    private val _state = MutableStateFlow<HomeNativeAdState>(HomeNativeAdState.Idle)
    val state: StateFlow<HomeNativeAdState> = _state.asStateFlow()
    private var loadedAtElapsedRealtime: Long = 0L

    fun setEligible(eligible: Boolean) {
        if (!eligible || !BuildConfig.ADS_ENABLED || BuildConfig.HOME_NATIVE_AD_UNIT_ID.isBlank()) {
            clear()
            return
        }

        val current = _state.value
        if (current is HomeNativeAdState.Loading) return
        if (current is HomeNativeAdState.Loaded && !isExpired()) return
        load()
    }

    fun clear() {
        val current = _state.value
        if (current is HomeNativeAdState.Loaded) {
            current.nativeAd.destroy()
        }
        loadedAtElapsedRealtime = 0L
        _state.value = HomeNativeAdState.Idle
    }

    private fun load() {
        clear()
        _state.value = HomeNativeAdState.Loading
        AdLoader.Builder(context, BuildConfig.HOME_NATIVE_AD_UNIT_ID)
            .forNativeAd { nativeAd ->
                val previous = _state.value
                if (previous is HomeNativeAdState.Loaded) previous.nativeAd.destroy()
                loadedAtElapsedRealtime = SystemClock.elapsedRealtime()
                _state.value = HomeNativeAdState.Loaded(nativeAd)
            }
            .withAdListener(
                object : com.google.android.gms.ads.AdListener() {
                    override fun onAdFailedToLoad(error: LoadAdError) {
                        Log.w(TAG, "Home native ad failed: ${error.code} ${error.message}")
                        _state.value = HomeNativeAdState.Failed(error.message)
                    }
                }
            )
            .build()
            .loadAd(AdRequest.Builder().build())
    }

    private fun isExpired(): Boolean {
        return SystemClock.elapsedRealtime() - loadedAtElapsedRealtime >= AD_TTL_MILLIS
    }

    private companion object {
        private const val TAG = "HomeNativeAdManager"
        private const val AD_TTL_MILLIS = 60 * 60 * 1000L
    }
}
