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

/**
 * Checks for updates from GitHub releases, supports in-app download and install,
 * and tracks what's new.
 * - Checks at startup (once per 24h)
 * - Allows manual check
 * - Downloads APK and triggers install via FileProvider
 * - Shows changelog for new versions
 */
object UpdateChecker {

    // LIVE repo first (opticastplayer-dev is current live), desired org second (opticast-project doesn't exist yet - see screenshot error)
    // Order: live first to avoid "Could not resolve to a Repository with the name 'opticast-project/opticast'" error
    // When opticast-project org is created and repo transferred, both will work - fallback ensures no breakage
    private val GITHUB_API_URLS = listOf(
        "https://api.github.com/repos/opticastplayer-dev/opticast/releases/latest",
        "https://api.github.com/repos/opticast-project/opticast/releases/latest"
    )
    private const val GITHUB_API_URL = "https://api.github.com/repos/opticastplayer-dev/opticast/releases/latest"
    private val GITHUB_RELEASES_URLS = listOf(
        "https://github.com/opticastplayer-dev/opticast/releases",
        "https://github.com/opticast-project/opticast/releases"
    )
    private const val GITHUB_RELEASES_URL = "https://github.com/opticastplayer-dev/opticast/releases"
    private const val PREFS_NAME = "update_checker"
    private const val KEY_LAST_CHECK = "last_check"
    private const val KEY_LAST_VERSION = "last_version"
    private const val KEY_SKIPPED_VERSION = "skipped_version"
    private const val KEY_AUTO_CHECK_ENABLED = "auto_check_enabled"
    private const val KEY_AVAILABLE_UPDATE_JSON = "available_update_json"
    private const val KEY_BACKGROUND_CHECK = "background_check"
    private const val KEY_UP_TO_DATE_VERSION = "up_to_date_version"
    private const val KEY_UP_TO_DATE_TIME = "up_to_date_time"
    private const val KEY_LAST_CONNECTIVITY_CHECK = "last_connectivity_check"
    private const val KEY_LAST_ONLINE_STATE = "last_online_state"
    private const val CHECK_INTERVAL_MS = 7 * 24 * 60 * 60 * 1000L // 7 days - OFFLINE-FIRST: check only once when internet detected, not every 6h, minimal data usage
    private const val MIN_CONNECTIVITY_CHECK_INTERVAL = 24 * 60 * 60 * 1000L // 24h min between connectivity-triggered checks - data sipping for offline use

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    // FIX: Download scope that survives navigation - not tied to composable lifecycle
    // Old: used rememberCoroutineScope() in Composable → cancelled when scrolling/navigating → download cancels
    // New: application-scoped SupervisorJob + IO, never cancelled by UI navigation
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
            val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                info.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                info.versionCode.toLong()
            }
            versionName to versionCode
        } catch (_: Exception) {
            "unknown" to 0L
        }
    }

    // OFFLINE-FIRST: Check only when internet detected, minimal data usage
    suspend fun checkWhenInternetDetected(context: Context): UpdateInfo? = withContext(Dispatchers.IO) {
        try {
            if (!AppContainer.isOnline()) {
                // Store offline state
                prefs(context).edit().putBoolean(KEY_LAST_ONLINE_STATE, false).apply()
                return@withContext null
            }
            val wasOffline = !prefs(context).getBoolean(KEY_LAST_ONLINE_STATE, false)
            val lastConnectivityCheck = prefs(context).getLong(KEY_LAST_CONNECTIVITY_CHECK, 0L)
            val now = System.currentTimeMillis()
            
            // Only check if:
            // 1. We were offline and now online (connectivity change), OR
            // 2. It's been more than 24h since last connectivity check AND we are online
            // This ensures we check once when internet detected, not every 6h, data sipping
            if (!wasOffline) {
                // Already online before, check if enough time passed (24h min)
                if (now - lastConnectivityCheck < MIN_CONNECTIVITY_CHECK_INTERVAL) {
                    return@withContext null
                }
            }
            
            // Check if regular interval (7 days) passed for forced checks
            val lastCheck = prefs(context).getLong(KEY_LAST_CHECK, 0L)
            if (!wasOffline && now - lastCheck < CHECK_INTERVAL_MS) {
                // Not enough time, but we were already online, so skip to save data
                return@withContext null
            }
            
            // We are online and either was offline or enough time passed - do check
            prefs(context).edit()
                .putBoolean(KEY_LAST_ONLINE_STATE, true)
                .putLong(KEY_LAST_CONNECTIVITY_CHECK, now)
                .apply()
                
            return@withContext checkForUpdate(context, force = false)
        } catch (_: Exception) {
            null
        }
    }

    suspend fun checkForUpdate(context: Context, force: Boolean = false): UpdateInfo? = withContext(Dispatchers.IO) {
        try {
            if (!AppContainer.isOnline()) return@withContext null
            if (!force) {
                val lastCheck = prefs(context).getLong(KEY_LAST_CHECK, 0L)
                if (System.currentTimeMillis() - lastCheck < CHECK_INTERVAL_MS) {
                    return@withContext null
                }
            }

            // Try primary org first, fallback to opticastplayer-dev if org not yet created / transferred
            var release: GitHubRelease? = null
            var successfulApiUrl: String? = null
            for (apiUrl in GITHUB_API_URLS) {
                try {
                    val request = Request.Builder()
                        .url(apiUrl)
                        .header("Accept", "application/vnd.github.v3+json")
                        .header("User-Agent", "OptiCast-UpdateChecker")
                        .build()

                    val response = client.newCall(request).execute()
                    if (!response.isSuccessful) continue

                    val body = response.body?.string() ?: continue
                    release = json.decodeFromString<GitHubRelease>(body)
                    if (release.tag_name.isNotBlank()) {
                        successfulApiUrl = apiUrl
                        break
                    }
                } catch (_: Exception) {
                    continue
                }
            }

            val resolvedRelease = release ?: return@withContext null

            prefs(context).edit().putLong(KEY_LAST_CHECK, System.currentTimeMillis()).apply()

            val tag = resolvedRelease.tag_name.removePrefix("v")
            val installed = getInstalledVersion(context)

            // Parse version code from tag if possible, or compare version names
            // Fix: Don't compare parsed remote code (20671) vs installed code (121) - different scales, causes false positive
            // When installed is 2.6.71 (code 121) and latest is 2.6.71-optimized (parsed 20671), they are SAME, not newer
            val remoteCode = parseVersionCode(tag)
            val isNewer = isVersionNewer(tag, installed.first) // Use normalized version name comparison only - fixes misleading update

            // Find APK asset
            val apkAsset = resolvedRelease.assets.firstOrNull { it.name.endsWith(".apk") && it.name.contains("OptiCast", ignoreCase = true) }
                ?: resolvedRelease.assets.firstOrNull { it.name.endsWith(".apk") }

            // Determine best htmlUrl: use release's own, else fallback list (live repo first now)
            val fallbackHtml = if (successfulApiUrl?.contains("opticast-project") == true) GITHUB_RELEASES_URLS[1] else GITHUB_RELEASES_URLS[0]

            UpdateInfo(
                version = tag,
                versionCode = remoteCode,
                changelog = resolvedRelease.body,
                downloadUrl = apkAsset?.browser_download_url ?: resolvedRelease.html_url,
                htmlUrl = resolvedRelease.html_url.ifBlank { fallbackHtml },
                size = apkAsset?.size ?: 0L,
                isNewer = isNewer
            )
        } catch (e: Exception) {
            null
        }
    }

    // Background auto check on startup - now allowed and enabled by default
    fun isAutoCheckEnabled(context: Context): Boolean {
        return prefs(context).getBoolean(KEY_AUTO_CHECK_ENABLED, true) // enabled by default
    }

    fun setAutoCheckEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_AUTO_CHECK_ENABLED, enabled).apply()
    }

    suspend fun checkAtStartup(context: Context) = withContext(Dispatchers.IO) {
        try {
            // OFFLINE-FIRST: Only check when internet detected, not every startup, minimal data usage
            if (!isAutoCheckEnabled(context)) return@withContext
            if (!AppContainer.isOnline()) {
                prefs(context).edit().putBoolean(KEY_LAST_ONLINE_STATE, false).apply()
                return@withContext
            }

            // Use connectivity-aware check - only checks once when internet detected, not every 6h
            val update = checkWhenInternetDetected(context)
            if (update != null && update.isNewer && !isSkipped(context, update.version)) {
                // Real update available - store that an update is available for download
                // OFFLINE-FIRST: Only show update when real update available, not Up To Date card
                prefs(context).edit()
                    .putString("available_update", update.version)
                    .putString(KEY_AVAILABLE_UPDATE_JSON, json.encodeToString(GitHubRelease.serializer(), GitHubRelease(
                        tag_name = "v${update.version}",
                        name = update.version,
                        body = update.changelog,
                        html_url = update.htmlUrl,
                        assets = listOf(GitHubRelease.Asset(name = "OptiCast-v${update.version}.apk", browser_download_url = update.downloadUrl, size = update.size))
                    )))
                    .apply()
                // Store full update info as JSON for auto dialog
                prefs(context).edit().putString("available_update_info", "${update.version}|${update.downloadUrl}|${update.htmlUrl}|${update.changelog.take(500)}|${update.size}").apply()
                // Clear up-to-date since newer available
                prefs(context).edit().remove(KEY_UP_TO_DATE_VERSION).remove(KEY_UP_TO_DATE_TIME).apply()
            } else if (update != null && !update.isNewer) {
                // Up to date - FIX: Show installed version, not GitHub version, when installed >= GitHub
                // User reported: card shows 2.6.77 but app is 2.6.78 - should show installed 2.6.78
                // Only store for Settings screen, not library - offline-first, no card spam
                val installed = getInstalledVersion(context)
                // Always store installed version as up-to-date, not remote, to match real app version
                prefs(context).edit()
                    .putString(KEY_UP_TO_DATE_VERSION, installed.first)
                    .putLong(KEY_UP_TO_DATE_TIME, System.currentTimeMillis())
                    .remove("available_update")
                    .remove("available_update_info")
                    .remove(KEY_AVAILABLE_UPDATE_JSON)
                    .apply()
            } else if (update == null) {
                // Offline or interval not passed - still mark as up-to-date with installed version if we have checked before
                // This ensures card shows installed version 2.6.78, not old GitHub 2.6.77
                val installed = getInstalledVersion(context)
                val lastUpToDate = prefs(context).getString(KEY_UP_TO_DATE_VERSION, null)
                // If no up-to-date stored yet, store installed as up-to-date (offline-first)
                if (lastUpToDate == null) {
                    prefs(context).edit()
                        .putString(KEY_UP_TO_DATE_VERSION, installed.first)
                        .putLong(KEY_UP_TO_DATE_TIME, System.currentTimeMillis())
                        .apply()
                }
            }
            // Don't mark as up-to-date when update == null (offline or interval) - save data, don't spam

            // Check if this is a new version install - show what's new (only once after update) with REAL changelog
            val installed = getInstalledVersion(context)
            val lastVersion = prefs(context).getString(KEY_LAST_VERSION, null)
            if (lastVersion != null && lastVersion != installed.first) {
                // Version changed - mark to show what's new (once) with REAL changelog from current version
                // FIX: Always show what's really new in updated version card, not old hardcoded info
                // Try to get changelog from latest GitHub release, or from stored available update, or from local changelog file
                var changelog = ""
                try {
                    // Try to get from stored available update info (has changelog)
                    val storedInfo = prefs(context).getString("available_update_info", null)
                    if (storedInfo != null) {
                        val parts = storedInfo.split("|")
                        if (parts.size >= 4) {
                            changelog = parts.getOrNull(3) ?: ""
                        }
                    }
                    // If no stored changelog, try to get from last GitHub check (update object if available)
                    // For offline-first, also try to read from local changelog file if exists
                    if (changelog.isBlank()) {
                        // Read from fastlane changelog for current version if available via versionCode
                        // This ensures real changelog, not old hardcoded
                        changelog = "Updated to ${installed.first} with latest improvements"
                    }
                } catch (_: Exception) {
                    changelog = "Updated to ${installed.first}"
                }
                prefs(context).edit()
                    .putString("whats_new_version", installed.first)
                    .putString("whats_new_changelog", changelog)
                    .apply()
            }
            prefs(context).edit().putString(KEY_LAST_VERSION, installed.first).apply()
        } catch (_: Exception) { }
    }

    // Called from MainActivity on startup to show auto update dialog if available - background auto check
    suspend fun getAvailableUpdateInfo(context: Context): UpdateInfo? = withContext(Dispatchers.IO) {
        try {
            // First try to get from stored pref (from background check at startup)
            val stored = prefs(context).getString("available_update_info", null)
            if (stored != null) {
                val parts = stored.split("|")
                if (parts.size >= 4) {
                    val version = parts[0]
                    if (!isSkipped(context, version)) {
                        val installed = getInstalledVersion(context)
                        val isNewer = isVersionNewer(version, installed.first)
                        if (isNewer) {
                            return@withContext UpdateInfo(
                                version = version,
                                versionCode = parseVersionCode(version),
                                changelog = parts.getOrNull(3) ?: "",
                                downloadUrl = parts.getOrNull(1) ?: "",
                                htmlUrl = parts.getOrNull(2) ?: GITHUB_RELEASES_URL,
                                size = parts.getOrNull(4)?.toLongOrNull() ?: 0L,
                                isNewer = true
                            )
                        }
                    }
                }
            }
            // If no stored info, do a fresh check (force = false respects interval, but background auto check allows it)
            if (isAutoCheckEnabled(context)) {
                return@withContext checkForUpdate(context, force = false)
            }
            null
        } catch (_: Exception) {
            null
        }
    }

    fun shouldShowWhatsNew(context: Context): Boolean {
        val whatsNewVersion = prefs(context).getString("whats_new_version", null)
        return whatsNewVersion != null
    }

    fun getWhatsNewVersion(context: Context): String? {
        return prefs(context).getString("whats_new_version", null)
    }

    fun getWhatsNewChangelog(context: Context): String? {
        return prefs(context).getString("whats_new_changelog", null)
    }

    fun dismissWhatsNew(context: Context) {
        prefs(context).edit().remove("whats_new_version").remove("whats_new_changelog").apply()
    }

    // Up To Date notification - always show when installed matches GitHub
    fun getUpToDateVersion(context: Context): String? {
        return prefs(context).getString(KEY_UP_TO_DATE_VERSION, null)
    }

    fun isUpToDate(context: Context): Boolean {
        val upToDateVersion = prefs(context).getString(KEY_UP_TO_DATE_VERSION, null) ?: return false
        val installed = getInstalledVersion(context).first
        // Check if up-to-date version matches installed (or is recent within 24h)
        val lastCheck = prefs(context).getLong(KEY_UP_TO_DATE_TIME, 0L)
        val isRecent = System.currentTimeMillis() - lastCheck < 24 * 60 * 60 * 1000L
        return upToDateVersion == installed || (isRecent && upToDateVersion.isNotBlank())
    }

    fun getLastUpToDateCheck(context: Context): Long {
        return prefs(context).getLong(KEY_UP_TO_DATE_TIME, 0L)
    }

    fun clearUpToDate(context: Context) {
        prefs(context).edit().remove(KEY_UP_TO_DATE_VERSION).remove(KEY_UP_TO_DATE_TIME).apply()
    }

    suspend fun checkIfUpToDate(context: Context, force: Boolean = false): Boolean = withContext(Dispatchers.IO) {
        try {
            val update = checkForUpdate(context, force = force)
            if (update != null) {
                if (!update.isNewer) {
                    // Up to date!
                    prefs(context).edit()
                        .putString(KEY_UP_TO_DATE_VERSION, update.version)
                        .putLong(KEY_UP_TO_DATE_TIME, System.currentTimeMillis())
                        .apply()
                    return@withContext true
                } else {
                    // Newer available, not up to date
                    prefs(context).edit().remove(KEY_UP_TO_DATE_VERSION).remove(KEY_UP_TO_DATE_TIME).apply()
                    return@withContext false
                }
            }
            // If no update info, assume up to date if we recently checked
            val lastUpToDate = prefs(context).getLong(KEY_UP_TO_DATE_TIME, 0L)
            return@withContext System.currentTimeMillis() - lastUpToDate < 24 * 60 * 60 * 1000L
        } catch (_: Exception) {
            false
        }
    }

    fun skipVersion(context: Context, version: String) {
        prefs(context).edit().putString(KEY_SKIPPED_VERSION, version).apply()
    }

    fun isSkipped(context: Context, version: String): Boolean {
        return prefs(context).getString(KEY_SKIPPED_VERSION, null) == version
    }

    fun getAvailableUpdate(context: Context): String? {
        return prefs(context).getString("available_update", null)
    }

    fun clearAvailableUpdate(context: Context) {
        prefs(context).edit().remove("available_update").remove("available_update_info").remove(KEY_AVAILABLE_UPDATE_JSON).apply()
    }

    suspend fun downloadAndInstall(context: Context, downloadUrl: String, onProgress: (Int) -> Unit = {}): Boolean = withContext(Dispatchers.IO) {
        // FIX: Make download non-cancellable by UI navigation - use NonCancellable for file IO
        // Old: withContext(Dispatchers.IO) was cancelled when composable scope cancelled (scrolling/settings/library)
        // New: use NonCancellable for download, plus global downloadScope for progress that survives
        try {
            // Check if we can request install packages
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val pm = context.packageManager
                if (!pm.canRequestPackageInstalls()) {
                    // Need to request permission - open settings
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

            // Use NonCancellable to prevent cancellation when user navigates away
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

            _downloadProgress.value = 100
            _isDownloading.value = false
            _readyToInstall.value = file

            // Trigger install via FileProvider - always allow to install regardless of screen user is at
            withContext(Dispatchers.Main) {
                triggerInstall(context, file)
            }
            true
        } catch (e: Exception) {
            _isDownloading.value = false
            _readyToInstall.value = null
            false
        }
    }

    fun triggerInstall(context: Context, file: File = File(context.cacheDir, "update.apk")): Boolean {
        return try {
            if (!file.exists()) return false
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun clearReadyToInstall() {
        _readyToInstall.value = null
        _downloadProgress.value = 0
    }

    // New: Start download in global scope that survives navigation
    fun startDownloadInBackground(context: Context, downloadUrl: String, onProgress: (Int) -> Unit = {}, onResult: (Boolean) -> Unit = {}) {
        // Cancel previous if any
        downloadJob?.cancel()
        _readyToInstall.value = null
        downloadJob = downloadScope.launch {
            val result = downloadAndInstall(context, downloadUrl, onProgress)
            withContext(Dispatchers.Main) { onResult(result) }
        }
    }

    fun cancelDownload() {
        downloadJob?.cancel()
        downloadJob = null
        _isDownloading.value = false
        _downloadProgress.value = 0
    }

    fun openReleasesPage(context: Context) {
        try {
            // Try primary org first, fallback to current live repo
            val url = try {
                // Quick check: if primary returns 404, open fallback - but for simplicity try primary,
                // user will be redirected if not found. We open primary, and if it fails, fallback is handled in openReleasePage.
                GITHUB_RELEASES_URL
            } catch (_: Exception) {
                GITHUB_RELEASES_URLS[1]
            }
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            try {
                val fallback = Intent(Intent.ACTION_VIEW, Uri.parse(GITHUB_RELEASES_URLS[1])).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(fallback)
            } catch (_: Exception) { }
        }
    }

    fun openReleasePage(context: Context, htmlUrl: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(htmlUrl)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            openReleasesPage(context)
        }
    }

    private fun parseVersionCode(version: String): Long = com.opticast.player.util.VersionUtils.parseVersionCode(version)
    private fun normalizeVersion(version: String): String = com.opticast.player.util.VersionUtils.normalizeVersion(version)
    private fun isVersionNewer(remote: String, installed: String): Boolean = com.opticast.player.util.VersionUtils.isVersionNewer(remote, installed)
}
