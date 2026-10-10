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
import kotlinx.coroutines.launch
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
    private const val KEY_SKIPPED_VERSION = "skipped_version"
    private const val KEY_AUTO_CHECK_ENABLED = "auto_check_enabled"
    private const val KEY_AVAILABLE_UPDATE_JSON = "available_update_json"
    private const val KEY_UP_TO_DATE_VERSION = "up_to_date_version"
    private const val KEY_UP_TO_DATE_TIME = "up_to_date_time"
    private const val KEY_LAST_VERSION = "last_version"
    private const val KEY_WHATS_NEW_SHOWN = "whats_new_shown"
    private const val KEY_WHATS_NEW_VERSION = "whats_new_version"
    private const val KEY_WHATS_NEW_CHANGELOG = "whats_new_changelog"
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
    private var downloadJob: kotlinx.coroutines.Job? = null

    @Serializable
    data class GitHubRelease(
        val tag_name: String = "",
        val name: String = "",
        val body: String = "",
        val html_url: String = "",
        val assets: List<Asset> = emptyList()
    ) {
        @Serializable
        data class Asset(val name: String = "", val browser_download_url: String = "", val size: Long = 0L)
    }

    @Serializable
    data class UpdateInfo(
        val version: String,
        val versionCode: Long,
        val changelog: String,
        val downloadUrl: String,
        val htmlUrl: String,
        val size: Long,
        val isNewer: Boolean
    )

    private fun prefs(context: Context): SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getInstalledVersion(context: Context): Pair<String, Long> = try {
        val info = context.packageManager.getPackageInfo(context.packageName, 0)
        val name = info.versionName ?: "unknown"
        val code = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) info.longVersionCode else info.versionCode.toLong()
        name to code
    } catch (_: Exception) { "unknown" to 0L }

    fun isAutoCheckEnabled(context: Context): Boolean = prefs(context).getBoolean(KEY_AUTO_CHECK_ENABLED, true)
    fun setAutoCheckEnabled(context: Context, enabled: Boolean) { prefs(context).edit().putBoolean(KEY_AUTO_CHECK_ENABLED, enabled).apply() }

    fun getUpToDateVersion(context: Context): String? = prefs(context).getString(KEY_UP_TO_DATE_VERSION, null)
    fun isUpToDate(context: Context): Boolean = getUpToDateVersion(context) != null
    fun getLastUpToDateCheck(context: Context): Long = prefs(context).getLong(KEY_UP_TO_DATE_TIME, 0L)
    fun clearUpToDate(context: Context) { prefs(context).edit().remove(KEY_UP_TO_DATE_VERSION).remove(KEY_UP_TO_DATE_TIME).apply() }

    fun getWhatsNewVersion(context: Context): String? = prefs(context).getString(KEY_WHATS_NEW_VERSION, null)
    fun getWhatsNewChangelog(context: Context): String = prefs(context).getString(KEY_WHATS_NEW_CHANGELOG, "") ?: ""
    fun shouldShowWhatsNew(context: Context): Boolean = prefs(context).getBoolean(KEY_WHATS_NEW_SHOWN, false)
    fun dismissWhatsNew(context: Context) { prefs(context).edit().putBoolean(KEY_WHATS_NEW_SHOWN, false).apply() }

    fun getAvailableUpdateInfo(context: Context): UpdateInfo? = try {
        prefs(context).getString(KEY_AVAILABLE_UPDATE_JSON, null)?.let { json.decodeFromString<UpdateInfo>(it) }
    } catch (_: Exception) { null }

    fun clearAvailableUpdate(context: Context) { prefs(context).edit().remove(KEY_AVAILABLE_UPDATE_JSON).apply() }
    fun skipVersion(context: Context, version: String) { prefs(context).edit().putString(KEY_SKIPPED_VERSION, version).apply() }
    fun isSkipped(context: Context, version: String): Boolean = prefs(context).getString(KEY_SKIPPED_VERSION, null) == version

    suspend fun checkForUpdate(context: Context, force: Boolean = false): UpdateInfo? = withContext(Dispatchers.IO) {
        try {
            if (!AppContainer.isOnline()) return@withContext null
            if (!force) {
                val last = prefs(context).getLong(KEY_LAST_CHECK, 0L)
                if (System.currentTimeMillis() - last < CHECK_INTERVAL_MS) {
                    return@withContext getAvailableUpdateInfo(context)
                }
            }
            val req = Request.Builder().url(GITHUB_API_URL).header("Accept", "application/vnd.github.v3+json").header("User-Agent", "OptiCast-UpdateChecker").build()
            val resp = client.newCall(req).execute()
            if (!resp.isSuccessful) return@withContext null
            val body = resp.body?.string() ?: return@withContext null
            val release = json.decodeFromString<GitHubRelease>(body)
            if (release.tag_name.isBlank()) return@withContext null
            prefs(context).edit().putLong(KEY_LAST_CHECK, System.currentTimeMillis()).apply()
            val tag = release.tag_name.removePrefix("v")
            val installed = getInstalledVersion(context)
            val isNewer = isVersionNewer(tag, installed.first)
            val apk = release.assets.firstOrNull { it.name.endsWith(".apk") && it.name.contains("OptiCast", true) } ?: release.assets.firstOrNull { it.name.endsWith(".apk") }
            val info = UpdateInfo(tag, parseVersionCode(tag), release.body, apk?.browser_download_url ?: release.html_url, release.html_url.ifBlank { GITHUB_RELEASES_URL }, apk?.size ?: 0L, isNewer)
            prefs(context).edit().putString(KEY_AVAILABLE_UPDATE_JSON, json.encodeToString(info)).apply()
            if (isNewer) {
                prefs(context).edit().putString(KEY_LAST_VERSION, tag).putString(KEY_WHATS_NEW_VERSION, tag).putString(KEY_WHATS_NEW_CHANGELOG, release.body).putBoolean(KEY_WHATS_NEW_SHOWN, true).apply()
            } else {
                prefs(context).edit().putString(KEY_UP_TO_DATE_VERSION, installed.first).putLong(KEY_UP_TO_DATE_TIME, System.currentTimeMillis()).apply()
            }
            info
        } catch (_: Exception) { null }
    }

    suspend fun checkAtStartup(context: Context) {
        if (!isAutoCheckEnabled(context)) return
        checkForUpdate(context, force = false)
    }

    suspend fun checkWhenInternetDetected(context: Context) {
        if (!isAutoCheckEnabled(context)) return
        if (!AppContainer.isOnline()) return
        checkForUpdate(context, force = false)
    }

    fun isVersionNewer(remote: String, installed: String): Boolean { return try {
        val r = remote.split(".", "-").mapNotNull { it.toIntOrNull() }
        val i = installed.split(".", "-").mapNotNull { it.toIntOrNull() }
        for (idx in 0 until maxOf(r.size, i.size)) {
            val rv = r.getOrNull(idx) ?: 0
            val iv = i.getOrNull(idx) ?: 0
            if (rv > iv) return true
            if (rv < iv) return false
        }
        false
    } catch (_: Exception) { remote != installed } }

    fun parseVersionCode(version: String): Long = try {
        var code = 0L
        for (p in version.split(".", "-").mapNotNull { it.toIntOrNull() }) code = code * 1000 + p
        code
    } catch (_: Exception) { 0L }

    fun openReleasesPage(context: Context) { runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(GITHUB_RELEASES_URL)).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }) } }
    fun openReleasePage(context: Context, url: String) { runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }) } }

    fun startDownloadInBackground(context: Context, downloadUrl: String) {
        downloadJob?.cancel()
        downloadJob = downloadScope.launch {
            _isDownloading.value = true
            _downloadProgress.value = 0
            try {
                val req = Request.Builder().url(downloadUrl).build()
                val resp = client.newCall(req).execute()
                if (!resp.isSuccessful) { _isDownloading.value = false; return@launch }
                val body = resp.body ?: run { _isDownloading.value = false; return@launch }
                val total = body.contentLength()
                val file = File(context.cacheDir, "update.apk").apply { delete() }
                body.byteStream().use { input ->
                    file.outputStream().use { output ->
                        val buf = ByteArray(8192)
                        var downloaded = 0L
                        var read: Int
                        while (input.read(buf).also { read = it } != -1) {
                            output.write(buf, 0, read)
                            downloaded += read
                            if (total > 0) _downloadProgress.value = ((downloaded * 100) / total).toInt()
                        }
                    }
                }
                _readyToInstall.value = file
            } catch (_: Exception) {
            } finally { _isDownloading.value = false }
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
            startDownloadInBackground(context, downloadUrl)
            downloadJob?.join()
            val file = _readyToInstall.value ?: return@withContext false
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
        } catch (_: Exception) { false }
    }

    fun triggerInstall(context: Context, file: File) {
        runCatching {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(intent)
        }
    }

    fun clearReadyToInstall() { _readyToInstall.value = null; _downloadProgress.value = 0; _isDownloading.value = false }
}
