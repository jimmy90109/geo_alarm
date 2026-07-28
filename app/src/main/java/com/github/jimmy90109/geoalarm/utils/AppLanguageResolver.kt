package com.github.jimmy90109.geoalarm.utils

object AppLanguageResolver {
    const val ENGLISH = "en"
    const val TRADITIONAL_CHINESE = "zh"

    fun resolve(
        applicationLanguageTags: String?,
        effectiveLanguage: String?,
    ): String {
        val language = applicationLanguageTags
            ?.takeIf(String::isNotBlank)
            ?.substringBefore(",")
            ?.substringBefore("-")
            ?: effectiveLanguage

        return if (language.equals(TRADITIONAL_CHINESE, ignoreCase = true)) {
            TRADITIONAL_CHINESE
        } else {
            ENGLISH
        }
    }
}
