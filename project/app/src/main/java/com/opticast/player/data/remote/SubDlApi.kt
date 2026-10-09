package com.opticast.player.data.remote

import com.opticast.player.data.SettingsRepository
import com.opticast.player.data.local.MetadataStore
import com.opticast.player.data.model.Metadata
import com.opticast.player.data.model.SavedSubtitle
import java.io.ByteArrayInputStream
import java.util.concurrent.TimeUnit
import java.util.zip.ZipInputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request

/** One raw file inside a SubDL subtitle pack (requested via unpack=1). */
@Serializable
private data class SdUnpackFile(
    @SerialName("file_n_id") val fileNId: String = "",
    val name: String? = null,
    @SerialName("release_name") val releaseName: String? = null,
    val season: Int? = null,
    val episode: Int? = null,
    val language: String? = null,
    val format: String? = null,
    val url: String = "",
)

@Serializable
private data class SdSubtitle(
    @SerialName("release_name") val releaseName: String? = null,
    val name: String? = null,
    val lang: String? = null,
    val language: String? = null,
    val url: String = "",
    val season: Int? = null,
    val episode: Int? = null,
    @SerialName("unpack_files") val unpackFiles: List<SdUnpackFile> = emptyList(),
)

@Serializable
private data class SdResponse(
    val status: Boolean = false,
    val subtitles: List<SdSubtitle> = emptyList(),
)

/**
 * Client for SubDL - the backup subtitle provider. Free key from the SubDL
 * account panel. Search returns entries that map onto [SubtitleResult] with
 * source = "subdl"; downloads are raw files (unpack=1) or extracted zips.
 */
class SubDlApi(
    private val settings: SettingsRepository,
    private val store: MetadataStore,
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun search(
        metadata: Metadata?,
        fallbackQuery: String,
        languages: List<String>,
        season: Int?,
        episode: Int?,
    ): List<SubtitleResult> {
        val key = settings.current().subdlApiKey.takeIf { it.isNotBlank() }
            ?: return emptyList()

        val builder = "https://api.subdl.com/api/v1/subtitles".toHttpUrl().newBuilder()
            .addQueryParameter("api_key", key)
            .addQueryParameter("languages", languages.joinToString(",") { it.uppercase() })
            .addQueryParameter("unpack", "1")

        if (metadata != null && metadata.source == "tmdb" && metadata.tmdbId > 0) {
            builder.addQueryParameter("tmdb_id", metadata.tmdbId.toString())
        } else {
            val query = when {
                metadata?.type == "tv" && !metadata.showTitle.isNullOrBlank() -> metadata.showTitle
                metadata != null -> metadata.displayTitle
                else -> fallbackQuery
            }
            builder.addQueryParameter("film_name", query)
        }
        builder.addQueryParameter(
            "type",
            if (metadata?.type == "movie") "movie" else "tv",
        )
        season?.let { builder.addQueryParameter("season_number", it.toString()) }
        episode?.let { builder.addQueryParameter("episode_number", it.toString()) }

        val body = withContext(Dispatchers.IO) {
            runCatching {
                client.newCall(Request.Builder().url(builder.build()).build()).execute().use { response ->
                    if (response.isSuccessful) response.body?.string() else null
                }
            }.getOrNull()
        } ?: return emptyList()

        val parsed = runCatching { json.decodeFromString<SdResponse>(body) }.getOrNull()
            ?: return emptyList()
        if (!parsed.status) return emptyList()

        return parsed.subtitles.flatMapIndexed { index, subtitle ->
            val language = subtitle.language ?: subtitle.lang ?: "unknown"
            val release = subtitle.releaseName ?: subtitle.name ?: "SubDL subtitle"
            if (subtitle.unpackFiles.isNotEmpty()) {
                // Prefer the exact episode, then any single file.
                val file = subtitle.unpackFiles.firstOrNull {
                    it.episode == episode && episode != null
                } ?: subtitle.unpackFiles.first()
                listOf(
                    SubtitleResult(
                        subtitleId = "sd$index-${file.fileNId}",
                        fileId = 0,
                        language = file.language ?: language,
                        releaseName = file.releaseName ?: release,
                        downloads = 0,
                        source = "subdl",
                        subdlZipUrl = null,
                        subdlFileUrl = file.url.takeIf { it.isNotBlank() },
                    )
                )
            } else if (subtitle.url.isNotBlank()) {
                listOf(
                    SubtitleResult(
                        subtitleId = "sd$index",
                        fileId = 0,
                        language = language,
                        releaseName = release,
                        downloads = 0,
                        source = "subdl",
                        subdlZipUrl = subtitle.url,
                        subdlFileUrl = null,
                    )
                )
            } else {
                emptyList()
            }
        }.take(40)
    }

    /** Downloads a SubDL subtitle (raw file or zip) and stores it locally. */
    suspend fun download(result: SubtitleResult, videoId: Long): SavedSubtitle {
        val key = settings.current().subdlApiKey
        if (key.isBlank()) {
            throw SubtitleSearchException("Add your SubDL API key in Settings first.")
        }

        val bytes: ByteArray
        val extension: String

        if (!result.subdlFileUrl.isNullOrBlank()) {
            bytes = fetch("https://dl.subdl.com${result.subdlFileUrl}", withKey = true)
            extension = result.subdlFileUrl.substringAfterLast('.', "srt")
                .lowercase().takeIf { it in listOf("srt", "ass", "vtt", "ssa") } ?: "srt"
        } else if (!result.subdlZipUrl.isNullOrBlank()) {
            val zip = fetch("https://dl.subdl.com${result.subdlZipUrl}", withKey = false)
            val extracted = extractFirstSubtitle(zip)
                ?: throw SubtitleSearchException("The SubDL archive had no subtitle file.")
            bytes = extracted.first
            extension = extracted.second
        } else {
            throw SubtitleSearchException("SubDL returned no download link.")
        }

        if (bytes.isEmpty()) throw SubtitleSearchException("Downloaded subtitle was empty.")
        return store.saveSubtitle(videoId, result.language, result.releaseName, bytes, "subdl", extension)
    }

    private suspend fun fetch(url: String, withKey: Boolean): ByteArray =
        withContext(Dispatchers.IO) {
            val builder = Request.Builder().url(url)
            if (withKey) {
                builder.header("x-api-key", settings.current().subdlApiKey)
            }
            client.newCall(builder.build()).execute().use { response ->
                if (!response.isSuccessful) {
                    throw SubtitleSearchException("SubDL download failed (HTTP ${response.code}).")
                }
                response.body?.bytes() ?: ByteArray(0)
            }
        }

    private fun extractFirstSubtitle(zip: ByteArray): Pair<ByteArray, String>? =
        runCatching {
            ZipInputStream(ByteArrayInputStream(zip)).use { stream ->
                var entry = stream.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory) {
                        val ext = entry.name.substringAfterLast('.', "").lowercase()
                        if (ext in listOf("srt", "ass", "vtt", "ssa")) {
                            return@runCatching stream.readBytes() to ext
                        }
                    }
                    entry = stream.nextEntry
                }
                null
            }
        }.getOrNull()
}
