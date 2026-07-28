package com.github.jimmy90109.geoalarm.util

import android.content.Context
import android.os.Build

object SamsungNowBarGuide {
    private const val BASE_URL =
        "https://jimmy90109.github.io/geo_alarm/samsung-now-bar.html"

    fun isSupportedDevice(
        manufacturer: String = Build.MANUFACTURER,
        sdkInt: Int = Build.VERSION.SDK_INT,
    ): Boolean {
        return manufacturer.equals("samsung", ignoreCase = true) &&
            sdkInt >= Build.VERSION_CODES.BAKLAVA
    }

    fun urlForLanguageTag(languageTag: String): String {
        return if (languageTag.substringBefore('-').equals("zh", ignoreCase = true)) {
            "$BASE_URL#zh-tw"
        } else {
            BASE_URL
        }
    }

    fun url(context: Context): String {
        val languageTag = context.resources.configuration.locales[0].toLanguageTag()
        return urlForLanguageTag(languageTag)
    }
}
