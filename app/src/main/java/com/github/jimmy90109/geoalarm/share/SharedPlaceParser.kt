package com.github.jimmy90109.geoalarm.share

import androidx.annotation.Keep
import kotlinx.serialization.Serializable

@Keep
@Serializable
enum class SharedPlaceSource {
    GoogleMapsPlace,
    PlainTextAddress
}

data class SharedPlace(
    val query: String,
    val source: SharedPlaceSource,
    val mapsUrl: String? = null
)

object SharedPlaceParser {
    private val urlRegex = Regex("""https?://\S+""", RegexOption.IGNORE_CASE)

    fun parse(text: String?): SharedPlace? {
        return parse(listOfNotNull(text))
    }

    fun parse(textCandidates: List<String>): SharedPlace? {
        val contents = textCandidates.map(String::trim).filter(String::isNotEmpty)
        if (contents.isEmpty()) return null

        val urls = contents.asSequence()
            .flatMap { urlRegex.findAll(it) }
            .map { it.value.trimEnd('.', ',', ';') }
            .toList()

        if (urls.isEmpty()) {
            val query = normalizePlainText(contents.first())
            return query.takeIf(String::isNotEmpty)?.let {
                SharedPlace(query = it, source = SharedPlaceSource.PlainTextAddress)
            }
        }

        if (urls.any { !isGoogleMapsUrl(it) }) return null
        val mapsUrl = urls.firstOrNull(::isGoogleMapsUrl) ?: return null

        val query = contents.asSequence()
            .flatMap { it.lineSequence() }
            .map { line -> urlRegex.replace(line, "").trim() }
            .firstOrNull { it.isNotEmpty() }
            ?: return null

        return SharedPlace(
            query = query,
            source = SharedPlaceSource.GoogleMapsPlace,
            mapsUrl = mapsUrl
        )
    }

    private fun normalizePlainText(text: String): String =
        text.lineSequence()
            .map(String::trim)
            .filter(String::isNotEmpty)
            .joinToString(" ")

    private fun isGoogleMapsUrl(url: String): Boolean {
        val uri = runCatching { java.net.URI(url) }.getOrNull() ?: return false
        val host = uri.host?.lowercase() ?: return false
        return host == "maps.app.goo.gl" ||
            host == "maps.google.com" ||
            ((host == "www.google.com" || host == "google.com") && uri.path.orEmpty().startsWith("/maps"))
    }
}
