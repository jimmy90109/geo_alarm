package com.github.jimmy90109.geoalarm.data

import android.app.DownloadManager
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import com.github.jimmy90109.geoalarm.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.Locale

sealed interface UpdateStatus {
    object Idle : UpdateStatus
    object Checking : UpdateStatus
    data class Available(
        val version: String,
        val downloadUrl: String,
        val sha256: String
    ) : UpdateStatus
    object Downloading : UpdateStatus
    data class ReadyToInstall(val apkUri: Uri) : UpdateStatus
    data class Error(val message: String) : UpdateStatus
}

class UpdateManager(private val context: Context) {

    private val _status = MutableStateFlow<UpdateStatus>(UpdateStatus.Idle)
    val status: StateFlow<UpdateStatus> = _status.asStateFlow()

    private val json = Json { ignoreUnknownKeys = true }
    private var downloadId: Long? = null

    suspend fun checkForUpdates() {
        _status.value = UpdateStatus.Checking
        try {
            val release = fetchLatestRelease()
            val currentVersion = BuildConfig.VERSION_NAME

            val remoteVersion = sanitizeVersion(release.tagName)
            val localVersion = sanitizeVersion(currentVersion)

            if (isRemoteNewer(remoteVersion, localVersion)) {
                val apkAsset = release.assets.find { it.name.endsWith(".apk") }
                if (apkAsset != null) {
                    val sha256 = fetchApkSha256(release, apkAsset)
                    if (sha256 != null) {
                        _status.value = UpdateStatus.Available(
                            version = release.tagName,
                            downloadUrl = apkAsset.downloadUrl,
                            sha256 = sha256
                        )
                    } else {
                        _status.value = UpdateStatus.Error("Release checksum missing")
                    }
                } else {
                    _status.value = UpdateStatus.Idle // No APK found
                }
            } else {
                _status.value = UpdateStatus.Idle // Up to date
            }
        } catch (e: java.net.UnknownHostException) {
            e.printStackTrace()
            _status.value = UpdateStatus.Error("No internet connection")
            withContext(Dispatchers.Default) {
                kotlinx.coroutines.delay(3000)
                _status.value = UpdateStatus.Idle
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // If checking fails because of 404 (No releases), it's not really an error for the user
            if (e.message == "No releases found") {
                _status.value = UpdateStatus.Idle
                withContext(Dispatchers.Main) {
                   _status.value = UpdateStatus.Error("No update available")
                }
                withContext(Dispatchers.Default) {
                    kotlinx.coroutines.delay(2000)
                    _status.value = UpdateStatus.Idle
                }
            } else {
                _status.value = UpdateStatus.Error(e.message ?: "Unknown error")
                withContext(Dispatchers.Default) {
                    kotlinx.coroutines.delay(3000)
                    _status.value = UpdateStatus.Idle
                }
            }
        }
    }

    private suspend fun fetchLatestRelease(): GithubRelease = withContext(Dispatchers.IO) {
        // Use /releases list to get the most recent release (including pre-releases/betas)
        // because /latest only returns stable releases.
        val url = URL("https://api.github.com/repos/jimmy90109/geo_alarm/releases?per_page=1")
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 10000
        connection.readTimeout = 10000

        if (connection.responseCode == 200) {
            val reader = BufferedReader(InputStreamReader(connection.inputStream))
            val response = reader.readText()
            reader.close()
            // The API returns a List<GithubRelease> now
            val releases = json.decodeFromString<List<GithubRelease>>(response)
            releases.firstOrNull() ?: throw Exception("No releases found")
        } else if (connection.responseCode == 404) {
             throw Exception("No releases found")
        } else {
            throw Exception("GitHub API Error: ${connection.responseCode}")
        }
    }

    suspend fun downloadUpdate(url: String, expectedSha256: String) {
        _status.value = UpdateStatus.Downloading
        val request = DownloadManager.Request(Uri.parse(url)).setTitle("GeoAlarm Update")
            .setDescription("Downloading update...")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalFilesDir(
                context, Environment.DIRECTORY_DOWNLOADS, "update.apk"
            ).setAllowedOverMetered(true).setAllowedOverRoaming(true)

        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        downloadId = downloadManager.enqueue(request)

        // Start monitoring download
        monitorDownload(downloadManager, expectedSha256)
    }

    // We'll need a way to run this monitoring loop, passed from ViewModel ideally, 
    // but for now let's expose a suspect function or launch in a scope if we injected one.
    // For simplicity, I'll make this suspend and let VM call it.
    suspend fun monitorDownload(
        downloadManager: DownloadManager,
        expectedSha256: String
    ) = withContext(Dispatchers.IO) {
        var downloading = true
        while (downloading) {
            val query = DownloadManager.Query().setFilterById(downloadId ?: return@withContext)
            downloadManager.query(query).use { cursor ->
                if (cursor.moveToFirst()) {
                    val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))

                    when (status) {
                        DownloadManager.STATUS_SUCCESSFUL -> {
                            downloading = false
                            val uriString = cursor.getString(
                                cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_URI)
                            )
                            val apkUri = uriString?.let(Uri::parse)
                            if (apkUri != null) {
                                if (verifySha256(apkUri, expectedSha256)) {
                                    _status.value = UpdateStatus.ReadyToInstall(apkUri)
                                } else {
                                    _status.value = UpdateStatus.Error("Checksum verification failed")
                                }
                            } else {
                                _status.value = UpdateStatus.Error("Invalid download URI")
                            }
                        }

                        DownloadManager.STATUS_FAILED -> {
                            downloading = false
                            _status.value = UpdateStatus.Error("Download failed")
                        }
                    }
                }
            }
            if (downloading) kotlinx.coroutines.delay(1000)
        }
    }

    fun getInstallIntent(apkUri: Uri): Intent {
        val installUri = when (apkUri.scheme) {
            ContentResolver.SCHEME_CONTENT -> apkUri
            ContentResolver.SCHEME_FILE, null -> {
                val path = apkUri.path
                require(!path.isNullOrBlank()) { "Invalid file URI for install" }
                val file = File(path)
                FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
            }
            else -> apkUri
        }
        return Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(installUri, "application/vnd.android.package-archive")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
    }

    private fun sanitizeVersion(version: String): String =
        version.removePrefix("v").substringBefore("+")

    private fun isRemoteNewer(remoteVersion: String, localVersion: String): Boolean {
        val remote = parseVersion(remoteVersion)
        val local = parseVersion(localVersion)

        val maxCoreSize = maxOf(remote.core.size, local.core.size)
        for (i in 0 until maxCoreSize) {
            val remotePart = remote.core.getOrElse(i) { 0 }
            val localPart = local.core.getOrElse(i) { 0 }
            if (remotePart != localPart) return remotePart > localPart
        }

        if (remote.preRelease == null && local.preRelease != null) return true
        if (remote.preRelease != null && local.preRelease == null) return false
        if (remote.preRelease == null) return false

        return comparePreRelease(remote.preRelease, local.preRelease!!) > 0
    }

    private fun comparePreRelease(remote: List<String>, local: List<String>): Int {
        val maxSize = maxOf(remote.size, local.size)
        for (i in 0 until maxSize) {
            val remoteId = remote.getOrNull(i) ?: return -1
            val localId = local.getOrNull(i) ?: return 1
            val remoteNum = remoteId.toIntOrNull()
            val localNum = localId.toIntOrNull()

            val comparison = when {
                remoteNum != null && localNum != null -> remoteNum.compareTo(localNum)
                remoteNum != null -> -1
                localNum != null -> 1
                else -> remoteId.compareTo(localId)
            }
            if (comparison != 0) return comparison
        }
        return 0
    }

    private fun parseVersion(version: String): ParsedVersion {
        val parts = version.split("-", limit = 2)
        val core = parts[0].split(".").map { segment ->
            segment.takeWhile { it.isDigit() }.toIntOrNull() ?: 0
        }
        val preRelease = parts.getOrNull(1)?.split(".")?.filter { it.isNotBlank() }
        return ParsedVersion(core = core, preRelease = preRelease)
    }

    private data class ParsedVersion(
        val core: List<Int>,
        val preRelease: List<String>?
    )

    private suspend fun fetchApkSha256(release: GithubRelease, apkAsset: Asset): String? =
        withContext(Dispatchers.IO) {
            val checksumAsset = release.assets.firstOrNull { asset ->
                val name = asset.name.lowercase(Locale.US)
                name == "${apkAsset.name.lowercase(Locale.US)}.sha256" ||
                    (name.endsWith(".sha256") && name.contains(apkAsset.name.lowercase(Locale.US)))
            } ?: return@withContext null

            val content = downloadText(checksumAsset.downloadUrl)
            extractSha256(content)
        }

    private fun verifySha256(apkUri: Uri, expectedSha256: String): Boolean {
        val normalizedExpected = expectedSha256.lowercase(Locale.US)
        val stream = context.contentResolver.openInputStream(apkUri) ?: return false
        stream.use { input ->
            val digest = MessageDigest.getInstance("SHA-256")
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val read = input.read(buffer)
                if (read <= 0) break
                digest.update(buffer, 0, read)
            }
            val actual = digest.digest().joinToString("") { "%02x".format(it) }
            return actual == normalizedExpected
        }
    }

    private fun downloadText(urlString: String): String {
        val connection = URL(urlString).openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 10000
        connection.readTimeout = 10000

        if (connection.responseCode != 200) {
            throw Exception("Failed to fetch checksum: HTTP ${connection.responseCode}")
        }

        return connection.inputStream.bufferedReader().use { it.readText() }
    }

    private fun extractSha256(raw: String): String? {
        val match = Regex("""\b[a-fA-F0-9]{64}\b""").find(raw) ?: return null
        return match.value.lowercase(Locale.US)
    }

    fun resetState() {
        _status.value = UpdateStatus.Idle
    }
}
