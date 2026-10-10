package com.opticast.player.data.remote

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import com.opticast.player.data.AppContainer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit

object UpdateChecker {

    private const val GITHUB_API_URL = "https://api.github.com/repos/opticastplayer-dev/opticast/releases/latest"
    private const val GITHUB_RELEASES_URL = "https://github.com/opticastplayer-dev/opticast/releases"
    private const val PREFS_NAME = "update_checker"
    private const val KEY_LAST_CHECK = "last_check"
    private const val KEY_LAST_VERSION = "last_version"
    private const val CHECK_INTERVAL_MS = 24 * 60 * 60 * 1000L

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private val downloadScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + Dispatchers.IO)
    private val _downloadProgress = kotlinx.coroutines.flow.MutableStateFlow(0)
    val downloadProgress: kotlinx.coroutines.flow.StateFlow<Int> = _downloadProgress
    private val _isDownloading = kotlinx.coroutines.flow.MutableStateFlow(false)
    val isDownloading: kotlinx.coroutines.flow.StateFlow<Boolean> = _isDownloading
    private val _readyToInstall = kotlinx.coroutines.flow.MutableStateFlow<File?>(null)
    val readyToInstall: kotlinx.coroutines.flow.StateFlow<File?> = _readyToInstall

    @Serializable
    data class GitHubRelease(
        val tag_name: String = "",
        val name: String = "",
        val body: String = "",
        val html_url: String = "",
        val assets: List<Asset> = emptyList()
    ) {
        @Serializable
        data class Asset(
            val name: String = "",
            val browser_download_url: String = "",
            val size: Long = 0L
        )
    }

    data class UpdateInfo(
        val version: String,
        val versionCode: Long,
        val changelog: String,
        val downloadUrl: String,
        val htmlUrl: String,
        val size: Long,
        val isNewer: Boolean
    )

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getInstalledVersion(context: Context): Pair<String, Long> {
        return try {
            val pm = context.packageManager
            val info = pm.getPackageInfo(context.packageName, 0)
            val versionName = info.versionName ?: "unknown"
            val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) info.longVersionCode else info.versionCode.toLong()
            versionName to versionCode
        } catch (_: Exception) {
            "unknown" to 0L
        }
    }

    suspend fun checkForUpdate(context: Context, force: Boolean = false): UpdateInfo? = withContext(Dispatchers.IO) {
        try {
            if (!AppContainer.isOnline()) return@withContext null
            if (!force) {
                val lastCheck = prefs(context).getLong(KEY_LAST_CHECK, 0L)
                if (System.currentTimeMillis() - lastCheck < CHECK_INTERVAL_MS) return@withContext null
            }
            val request = Request.Builder()
                .url(GITHUB_API_URL)
                .header("Accept", "application/vnd.github.v3+json")
                .header("User-Agent", "OptiCast-UpdateChecker")
                .build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return@withContext null
            val body = response.body?.string() ?: return@withContext null
            val release = json.decodeFromString<GitHubRelease>(body)
            if (release.tag_name.isBlank()) return@withContext null
            prefs(context).edit().putLong(KEY_LAST_CHECK, System.currentTimeMillis()).apply()
            val tag = release.tag_name.removePrefix("v")
            val installed = getInstalledVersion(context)
            val isNewer = isVersionNewer(tag, installed.first)
            val apkAsset = release.assets.firstOrNull { it.name.endsWith(".apk") && it.name.contains("OptiCast", ignoreCase = true) }
                ?: release.assets.firstOrNull { it.name.endsWith(".apk") }
            UpdateInfo(
                version = tag,
                versionCode = parseVersionCode(tag),
                changelog = release.body,
                downloadUrl = apkAsset?.browser_download_url ?: release.html_url,
                htmlUrl = release.html_url.ifBlank { GITHUB_RELEASES_URL },
                size = apkAsset?.size ?: 0L,
                isNewer = isNewer
            )
        } catch (_: Exception) {
            null
        }
    }

    fun isVersionNewer(remote: String, installed: String): Boolean {
        return try {
            val rParts = remote.split(".", "-").mapNotNull { it.toIntOrNull() }
            val iParts = installed.split(".", "-").mapNotNull { it.toIntOrNull() }
            for (idx in 0 until maxOf(rParts.size, iParts.size)) {
                val r = rParts.getOrNull(idx) ?: 0
                val i = iParts.getOrNull(idx) ?: 0
                if (r > i) return true
                if (r < i) return false
            }
            false
        } catch (_: Exception) {
            remote != installed
        }
    }

    fun parseVersionCode(version: String): Long {
        return try {
            val parts = version.split(".", "-").mapNotNull { it.toIntOrNull() }
            var code = 0L
            for (p in parts) code = code * 1000 + p
            code
        } catch (_: Exception) {
            0L
        }
    }

    suspend fun downloadAndInstall(context: Context, downloadUrl: String, onProgress: (Int) -> Unit = {}): Boolean = withContext(Dispatchers.IO) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val pm = context.packageManager
                if (!pm.canRequestPackageInstalls()) {
                    withContext(Dispatchers.Main) {
                        val intent = Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                            data = Uri.parse("package:${context.packageName}")
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(intent)
                    }
                    return@withContext false
                }
            }
            _isDownloading.value = true
            _downloadProgress.value = 0
            val request = Request.Builder().url(downloadUrl).build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                _isDownloading.value = false
                return@withContext false
            }
            val body = response.body ?: run {
                _isDownloading.value = false
                return@withContext false
            }
            val total = body.contentLength()
            val file = File(context.cacheDir, "update.apk")
            file.delete()
            kotlinx.coroutines.withContext(kotlinx.coroutines.NonCancellable) {
                body.byteStream().use { input ->
                    file.outputStream().use { output ->
                        val buffer = ByteArray(8192)
                        var downloaded = 0L
                        var read: Int
                        while (input.read(buffer).also { read = it } != -1) {
                            output.write(buffer, 0, read)
                            downloaded += read
                            if (total > 0) {
                                val progress = ((downloaded * 100) / total).toInt()
                                _downloadProgress.value = progress
                                withContext(Dispatchers.Main) { onProgress(progress) }
                            }
                        }
                    }
                }
            }
            _isDownloading.value = false
            _readyToInstall.value = file
            withContext(Dispatchers.Main) {
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "application/vnd.android.package-archive")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(intent)
            }
            true
        } catch (_: Exception) {
            _isDownloading.value = false
            false
        }
    }

    fun clearDownload() {
        _readyToInstall.value = null
        _downloadProgress.value = 0
        _isDownloading.value = false
    }
}
