package com.github.jimmy90109.geoalarm.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SamsungNowBarGuideTest {
    @Test
    fun `supports Samsung devices on Android 16`() {
        assertTrue(SamsungNowBarGuide.isSupportedDevice("samsung", 36))
        assertTrue(SamsungNowBarGuide.isSupportedDevice("Samsung", 36))
    }

    @Test
    fun `supports later Android versions on Samsung devices`() {
        assertTrue(SamsungNowBarGuide.isSupportedDevice("SAMSUNG", 37))
    }

    @Test
    fun `does not support Samsung devices before Android 16`() {
        assertFalse(SamsungNowBarGuide.isSupportedDevice("samsung", 35))
    }

    @Test
    fun `does not support other manufacturers`() {
        assertFalse(SamsungNowBarGuide.isSupportedDevice("Google", 36))
    }

    @Test
    fun `uses Traditional Chinese anchor for Chinese locales`() {
        assertEquals(
            "https://jimmy90109.github.io/geo_alarm/samsung-now-bar.html#zh-tw",
            SamsungNowBarGuide.urlForLanguageTag("zh-TW"),
        )
        assertEquals(
            "https://jimmy90109.github.io/geo_alarm/samsung-now-bar.html#zh-tw",
            SamsungNowBarGuide.urlForLanguageTag("zh-Hant-TW"),
        )
    }

    @Test
    fun `uses English page for non-Chinese locales`() {
        assertEquals(
            "https://jimmy90109.github.io/geo_alarm/samsung-now-bar.html",
            SamsungNowBarGuide.urlForLanguageTag("en-US"),
        )
    }
}
