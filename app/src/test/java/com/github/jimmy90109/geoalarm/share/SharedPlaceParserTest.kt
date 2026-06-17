package com.github.jimmy90109.geoalarm.share

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SharedPlaceParserTest {
    @Test
    fun `parses place name and maps short url`() {
        val result = SharedPlaceParser.parse(
            """
            國立中正紀念堂
            https://maps.app.goo.gl/gQZxzqspdve9R6B48?g_st=atm
            """.trimIndent()
        )

        assertEquals("國立中正紀念堂", result?.query)
        assertEquals(SharedPlaceSource.GoogleMapsPlace, result?.source)
        assertEquals(
            "https://maps.app.goo.gl/gQZxzqspdve9R6B48?g_st=atm",
            result?.mapsUrl
        )
    }

    @Test
    fun `ignores blank lines around shared content`() {
        val result = SharedPlaceParser.parse(
            "\n\nTaipei 101\nhttps://www.google.com/maps/place/Taipei101\n"
        )

        assertEquals("Taipei 101", result?.query)
    }

    @Test
    fun `rejects content without place name`() {
        assertNull(SharedPlaceParser.parse("https://maps.app.goo.gl/example"))
    }

    @Test
    fun `rejects non Google Maps url`() {
        assertNull(SharedPlaceParser.parse("Taipei 101\nhttps://example.com/place"))
    }

    @Test
    fun `combines place title and url from separate share fields`() {
        val result = SharedPlaceParser.parse(
            listOf(
                "https://maps.app.goo.gl/gQZxzqspdve9R6B48?g_st=atm",
                "國立中正紀念堂"
            )
        )

        assertEquals("國立中正紀念堂", result?.query)
    }

    @Test
    fun `parses plain text address`() {
        val result = SharedPlaceParser.parse("台北市中正區中山南路21號")

        assertEquals("台北市中正區中山南路21號", result?.query)
        assertEquals(SharedPlaceSource.PlainTextAddress, result?.source)
        assertNull(result?.mapsUrl)
    }

    @Test
    fun `parses plain place name`() {
        val result = SharedPlaceParser.parse("國立中正紀念堂")

        assertEquals("國立中正紀念堂", result?.query)
        assertEquals(SharedPlaceSource.PlainTextAddress, result?.source)
    }

    @Test
    fun `normalizes multiline plain text address`() {
        val result = SharedPlaceParser.parse(
            """
            100
            台北市中正區中山南路21號
            """.trimIndent()
        )

        assertEquals("100 台北市中正區中山南路21號", result?.query)
        assertEquals(SharedPlaceSource.PlainTextAddress, result?.source)
    }

    @Test
    fun `rejects blank plain text`() {
        assertNull(SharedPlaceParser.parse(" \n "))
    }

    @Test
    fun `rejects text containing non Google url`() {
        assertNull(SharedPlaceParser.parse("台北市中正區 https://example.com/place"))
    }
}
