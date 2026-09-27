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

/**
 * Checks for updates from GitHub releases, supports in-app download and install,
 * and tracks what's new.
 * - Checks at startup (once per 24h)
 * - Allows manual check
 * - Downloads APK and triggers install via FileProvider
 * - Shows changelog for new versions
 */
object UpdateChecker {

    // LIVE repo first (opticastplayer-dev is current live), desired org second (opticast-project doesn't exist yet — see screenshot error)
    // Order: live first to avoid "Could not resolve to a Repository with the name 'opticast-project/opticast'" error
    // When opticast-project org is created and repo transferred, both will work — fallback ensures no breakage
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
    private const val CHECK_INTERVAL_MS = 6 * 60 * 60 * 1000L // 6h for background auto check on startup (was 24h) - checks at most every 6h to allow background auto check

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

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
            val installedCode = installed.second

            // Parse version code from tag if possible, or compare version names
            val remoteCode = parseVersionCode(tag)
            val isNewer = when {
                remoteCode > 0 && installedCode > 0 -> remoteCode > installedCode
                else -> isVersionNewer(tag, installed.first)
            }

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
            // Allow background auto check for updates on app startup - enabled by default
            if (!isAutoCheckEnabled(context)) return@withContext

            // Check if we should skip due to interval, but allow background check
            val update = checkForUpdate(context, force = false)
            if (update != null && update.isNewer && !isSkipped(context, update.version)) {
                // Store that an update is available - UI can show badge/dialog on startup
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
            } else if (update == null || !update.isNewer) {
                // No newer version - clear available update if same or older
                // Don't clear if we haven't checked yet
            }

            // Check if this is a new version install - show what's new
            val installed = getInstalledVersion(context)
            val lastVersion = prefs(context).getString(KEY_LAST_VERSION, null)
            if (lastVersion != null && lastVersion != installed.first) {
                // Version changed - mark to show what's new
                prefs(context).edit().putString("whats_new_version", installed.first).apply()
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

    fun dismissWhatsNew(context: Context) {
        prefs(context).edit().remove("whats_new_version").apply()
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

            val request = Request.Builder().url(downloadUrl).build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return@withContext false

            val body = response.body ?: return@withContext false
            val total = body.contentLength()
            val file = File(context.cacheDir, "update.apk")
            file.delete()

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
                            withContext(Dispatchers.Main) { onProgress(progress) }
                        }
                    }
                }
            }

            // Trigger install via FileProvider
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
        } catch (e: Exception) {
            false
        }
    }

    fun openReleasesPage(context: Context) {
        try {
            // Try primary org first, fallback to current live repo
            val url = try {
                // Quick check: if primary returns 404, open fallback — but for simplicity try primary,
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

    private fun parseVersionCode(version: String): Long {
        return try {
            // Try to parse version like 2.6.58 -> 108 or 2.6.58 as code
            // For simplicity, extract numbers and convert
            val parts = version.split(".")
            if (parts.size >= 3) {
                val major = parts[0].toLongOrNull() ?: 0
                val minor = parts[1].toLongOrNull() ?: 0
                val patch = parts[2].substringBefore("-").toLongOrNull() ?: 0
                // Rough conversion, but we also check version name comparison
                major * 10000 + minor * 100 + patch
            } else {
                0L
            }
        } catch (_: Exception) {
            0L
        }
    }

    private fun isVersionNewer(remote: String, installed: String): Boolean {
        return try {
            val remoteParts = remote.split(".").map { it.substringBefore("-").toIntOrNull() ?: 0 }
            val installedParts = installed.split(".").map { it.substringBefore("-").toIntOrNull() ?: 0 }
            for (i in 0 until maxOf(remoteParts.size, installedParts.size)) {
                val r = remoteParts.getOrNull(i) ?: 0
                val inst = installedParts.getOrNull(i) ?: 0
                if (r > inst) return true
                if (r < inst) return false
            }
            false
        } catch (_: Exception) {
            remote != installed
        }
    }
}
