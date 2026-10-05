package com.github.jimmy90109.geoalarm.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class AppLanguageResolverTest {
    @Test
    fun `uses effective Chinese language when app follows the system`() {
        assertEquals(
            AppLanguageResolver.TRADITIONAL_CHINESE,
            AppLanguageResolver.resolve(
                applicationLanguageTags = "",
                effectiveLanguage = "zh",
            ),
        )
    }

    @Test
    fun `uses effective English language when app follows the system`() {
        assertEquals(
            AppLanguageResolver.ENGLISH,
            AppLanguageResolver.resolve(
                applicationLanguageTags = null,
                effectiveLanguage = "en",
            ),
        )
    }

    @Test
    fun `explicit app language overrides effective system language`() {
        assertEquals(
            AppLanguageResolver.ENGLISH,
            AppLanguageResolver.resolve(
                applicationLanguageTags = "en",
                effectiveLanguage = "zh",
            ),
        )
        assertEquals(
            AppLanguageResolver.TRADITIONAL_CHINESE,
            AppLanguageResolver.resolve(
                applicationLanguageTags = "zh-TW",
                effectiveLanguage = "en",
            ),
        )
    }

    @Test
    fun `unsupported effective language falls back to English`() {
        assertEquals(
            AppLanguageResolver.ENGLISH,
            AppLanguageResolver.resolve(
                applicationLanguageTags = "",
                effectiveLanguage = "ja",
            ),
        )
    }
}
