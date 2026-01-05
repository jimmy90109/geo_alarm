package com.github.jimmy90109.geoalarm.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GithubRelease(
    @SerialName("tag_name") val tagName: String,
    @SerialName("html_url") val htmlUrl: String,
    @SerialName("assets") val assets: List<Asset> = emptyList(),
    @SerialName("body") val body: String? = null
)

@Serializable
data class Asset(
    @SerialName("name") val name: String,
    @SerialName("browser_download_url") val downloadUrl: String
)
