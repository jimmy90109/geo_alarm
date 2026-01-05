package com.github.jimmy90109.geoalarm.data

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
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

sealed interface UpdateStatus {
    object Idle : UpdateStatus
    object Checking : UpdateStatus
    data class Available(val version: String, val downloadUrl: String) : UpdateStatus
    object Downloading : UpdateStatus
    data class ReadyToInstall(val file: File) : UpdateStatus
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

            // cleanup version strings for comparison
            val remoteVersion = release.tagName.removePrefix("v")

            if (remoteVersion != currentVersion) { // Simple string comparison for now, assuming semantic versioning
                val apkAsset = release.assets.find { it.name.endsWith(".apk") }
                if (apkAsset != null) {
                    _status.value = UpdateStatus.Available(release.tagName, apkAsset.downloadUrl)
                } else {
                    _status.value = UpdateStatus.Idle // No APK found
                }
            } else {
                _status.value = UpdateStatus.Idle // Up to date
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // If checking fails because of 404 (No releases), it's not really an error for the user
            if (e.message == "No releases found") {
                _status.value = UpdateStatus.Idle
                // Optionally show a "No update available" toast via a different state if we want explicit feedback
                // But for now, Idle is fine (the button just stops spinning)
                withContext(Dispatchers.Main) {
                    // We could add a "NoUpdateFound" state if we wanted to show a snackbar
                    _status.value =
                        UpdateStatus.Error("No update available") // Re-using Error to show toast
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
        val url = URL("https://api.github.com/repos/jimmy90109/geo_alarm/releases/latest")
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 10000
        connection.readTimeout = 10000

        if (connection.responseCode == 200) {
            val reader = BufferedReader(InputStreamReader(connection.inputStream))
            val response = reader.readText()
            reader.close()
            json.decodeFromString(response)
        } else if (connection.responseCode == 404) {
            // 404 means no release found. Treat as up-to-date.
            // We'll throw a specific exception to be caught cleanly
            throw Exception("No releases found")
        } else {
            throw Exception("GitHub API Error: ${connection.responseCode}")
        }
    }

    suspend fun downloadUpdate(url: String) {
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
        monitorDownload(downloadManager)
    }

    // We'll need a way to run this monitoring loop, passed from ViewModel ideally, 
    // but for now let's expose a suspect function or launch in a scope if we injected one.
    // For simplicity, I'll make this suspend and let VM call it.
    suspend fun monitorDownload(downloadManager: DownloadManager) = withContext(Dispatchers.IO) {
        var downloading = true
        while (downloading) {
            val query = DownloadManager.Query().setFilterById(downloadId ?: return@withContext)
            val cursor = downloadManager.query(query)
            if (cursor.moveToFirst()) {
                val statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                val status = cursor.getInt(statusIndex)

                when (status) {
                    DownloadManager.STATUS_SUCCESSFUL -> {
                        downloading = false
                        val uriIndex = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)
                        val uriString = cursor.getString(uriIndex)
                        val file = File(Uri.parse(uriString).path!!)
                        _status.value = UpdateStatus.ReadyToInstall(file)
                    }

                    DownloadManager.STATUS_FAILED -> {
                        downloading = false
                        _status.value = UpdateStatus.Error("Download failed")
                    }
                }
            }
            cursor.close()
            if (downloading) kotlinx.coroutines.delay(1000)
        }
    }

    fun getInstallIntent(file: File): Intent {
        val uri = FileProvider.getUriForFile(
            context, "${context.packageName}.provider", file
        )
        return Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
    }

    fun resetState() {
        _status.value = UpdateStatus.Idle
    }
}
