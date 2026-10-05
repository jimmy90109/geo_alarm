package com.github.jimmy90109.geoalarm.utils

import android.content.Context
import android.icu.util.LocaleData
import android.icu.util.ULocale
import android.os.Build
import android.os.LocaleList
import android.app.LocaleManager
import android.content.res.Resources
import com.github.jimmy90109.geoalarm.data.DistanceUnitPreference
import com.github.jimmy90109.geoalarm.data.DistanceUnitSystem
import java.util.Locale

object DistanceUnitResolver {
    fun resolve(
        context: Context,
        preference: DistanceUnitPreference,
    ): DistanceUnitSystem {
        return resolve(preference, systemLocale(context))
    }

    fun resolve(
        preference: DistanceUnitPreference,
        systemLocale: Locale,
    ): DistanceUnitSystem {
        return when (preference) {
            DistanceUnitPreference.METRIC -> DistanceUnitSystem.METRIC
            DistanceUnitPreference.IMPERIAL -> DistanceUnitSystem.IMPERIAL
            DistanceUnitPreference.AUTO -> resolveAutomatic(systemLocale)
        }
    }

    internal fun resolveAutomatic(
        systemLocale: Locale,
        fallback: (Locale) -> DistanceUnitSystem = ::measurementSystemForLocale,
    ): DistanceUnitSystem {
        return when (systemLocale.getUnicodeLocaleType("ms")) {
            "metric" -> DistanceUnitSystem.METRIC
            "ussystem", "uksystem" -> DistanceUnitSystem.IMPERIAL
            else -> fallback(systemLocale)
        }
    }

    private fun systemLocale(context: Context): Locale {
        val locales = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.getSystemService(LocaleManager::class.java)?.systemLocales
        } else {
            @Suppress("DEPRECATION")
            Resources.getSystem().configuration.locales
        }
        return locales.firstOrNull() ?: Locale.getDefault(Locale.Category.FORMAT)
    }

    private fun measurementSystemForLocale(locale: Locale): DistanceUnitSystem {
        return when (
            LocaleData.getMeasurementSystem(ULocale.forLocale(locale))
        ) {
            LocaleData.MeasurementSystem.US,
            LocaleData.MeasurementSystem.UK -> DistanceUnitSystem.IMPERIAL
            else -> DistanceUnitSystem.METRIC
        }
    }

    private fun LocaleList?.firstOrNull(): Locale? {
        return if (this == null || isEmpty) null else get(0)
    }
}
