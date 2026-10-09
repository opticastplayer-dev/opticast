package com.opticast.player.data.remote

import com.opticast.player.data.SettingsRepository
import com.opticast.player.data.local.MetadataStore
import com.opticast.player.data.model.Metadata
import com.opticast.player.data.model.SavedSubtitle
import java.io.ByteArrayInputStream
import java.util.concurrent.TimeUnit
import java.util.zip.GZIPInputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class SubtitleSearchException(message: String) : Exception(message)

/** One downloadable subtitle from a provider search. */
data class SubtitleResult(
    val subtitleId: String,
    val fileId: Int,
    val language: String,
    val releaseName: String,
    val downloads: Int,
    val source: String = "opensubtitles",
    val subdlZipUrl: String? = null,
    val subdlFileUrl: String? = null,
) {
    /** Stable identity for tracking download progress in the UI. */
    val key: String get() = "$source-$subtitleId-$fileId"
}

@Serializable
private data class OsFileDto(
    @SerialName("file_id") val fileId: Int = 0,
    @SerialName("file_name") val fileName: String? = null,
)

@Serializable
private data class OsAttributesDto(
    val language: String = "",
    @SerialName("release_name") val releaseName: String = "",
    @SerialName("download_count") val downloadCount: Int = 0,
    @SerialName("subtitle_id") val subtitleId: String = "",
    val files: List<OsFileDto> = emptyList(),
)

@Serializable
private data class OsSubtitleDto(
    val id: String = "",
    val attributes: OsAttributesDto = OsAttributesDto(),
)

@Serializable
private data class OsSearchResponseDto(val data: List<OsSubtitleDto> = emptyList())

@Serializable
private data class OsDownloadResponseDto(val link: String = "")

@Serializable
private data class DownloadRequestDto(@SerialName("file_id") val fileId: Int)

/**
 * Client for the OpenSubtitles.com REST API (v1). Requires a free API key,
 * created at opensubtitles.com under Settings -> API keys.
 */
class OpenSubtitlesApi(
    private val settings: SettingsRepository,
    private val store: MetadataStore,
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()
    private val json = Json { ignoreUnknownKeys = true }

    private fun userAgent() = "OptiCast 1.0"

    suspend fun search(
        metadata: Metadata?,
        fallbackQuery: String,
        languages: List<String>,
        season: Int?,
        episode: Int?,
    ): List<SubtitleResult> {
        val key = settings.current().openSubtitlesApiKey
        if (key.isBlank()) {
            throw SubtitleSearchException("Add your OpenSubtitles API key in Settings first.")
        }
        val query = when {
            metadata?.type == "tv" && !metadata.showTitle.isNullOrBlank() -> metadata.showTitle
            metadata != null -> metadata.displayTitle
            else -> fallbackQuery
        }

        val url = "https://api.opensubtitles.com/api/v1/subtitles".toHttpUrl().newBuilder()
            .addQueryParameter("query", query)
            .addQueryParameter("languages", languages.joinToString(","))
            .addQueryParameter("order_by", "download_count")
            .addQueryParameter("order_direction", "desc")
            .apply {
                season?.let { addQueryParameter("season_number", it.toString()) }
                episode?.let { addQueryParameter("episode_number", it.toString()) }
                if (metadata != null && metadata.source == "tmdb") addQueryParameter("tmdb_id", metadata.tmdbId.toString())
            }
            .build()

        val request = Request.Builder()
            .url(url)
            .header("Api-Key", key)
            .header("User-Agent", userAgent())
            .build()

        val body = withContext(Dispatchers.IO) {
            client.newCall(request).execute().use { response ->
                when {
                    response.code == 403 ->
                        throw SubtitleSearchException("OpenSubtitles rejected the API key (403).")
                    response.code == 429 ->
                        throw SubtitleSearchException("OpenSubtitles rate limit reached - try again shortly.")
                    !response.isSuccessful ->
                        throw SubtitleSearchException("OpenSubtitles error: HTTP ${response.code}")
                    else -> response.body?.string().orEmpty()
                }
            }
        }

        val parsed = runCatching { json.decodeFromString<OsSearchResponseDto>(body) }
            .getOrElse { throw SubtitleSearchException("Could not read the OpenSubtitles response.") }

        return parsed.data
            .flatMap { subtitle ->
                val attrs = subtitle.attributes
                attrs.files.mapNotNull { file ->
                    if (file.fileId == 0) return@mapNotNull null
                    SubtitleResult(
                        subtitleId = attrs.subtitleId.ifBlank { subtitle.id },
                        fileId = file.fileId,
                        language = attrs.language,
                        releaseName = attrs.releaseName.ifBlank { file.fileName ?: "Subtitle" },
                        downloads = attrs.downloadCount,
                    )
                }
            }
            .distinctBy { it.fileId }
            .take(40)
    }

    /** Requests a temporary download link, fetches the file and stores it locally. */
    suspend fun download(result: SubtitleResult, videoId: Long): SavedSubtitle {
        val key = settings.current().openSubtitlesApiKey
        if (key.isBlank()) {
            throw SubtitleSearchException("Add your OpenSubtitles API key in Settings first.")
        }

        val payload = json.encodeToString(DownloadRequestDto.serializer(), DownloadRequestDto(result.fileId))
        val request = Request.Builder()
            .url("https://api.opensubtitles.com/api/v1/download")
            .header("Api-Key", key)
            .header("User-Agent", userAgent())
            .header("Content-Type", "application/json")
            .post(payload.toRequestBody("application/json".toMediaType()))
            .build()

        val link = withContext(Dispatchers.IO) {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw SubtitleSearchException("Could not get a download link (HTTP ${response.code}).")
                }
                val body = response.body?.string().orEmpty()
                runCatching { json.decodeFromString<OsDownloadResponseDto>(body).link }
                    .getOrNull()
                    ?: throw SubtitleSearchException("No download link in the response.")
            }
        }

        val bytes = withContext(Dispatchers.IO) {
            val fileRequest = Request.Builder()
                .url(link)
                .header("User-Agent", userAgent())
                .build()
            client.newCall(fileRequest).execute().use { response ->
                if (!response.isSuccessful) {
                    throw SubtitleSearchException("Subtitle download failed (HTTP ${response.code}).")
                }
                response.body?.bytes() ?: ByteArray(0)
            }
        }

        // OpenSubtitles usually serves gzip-compressed files.
        val content = if (bytes.size > 2 && bytes[0] == 0x1F.toByte() && bytes[1] == 0x8B.toByte()) {
            GZIPInputStream(ByteArrayInputStream(bytes)).readBytes()
        } else {
            bytes
        }
        if (content.isEmpty()) throw SubtitleSearchException("Downloaded subtitle was empty.")

        return store.saveSubtitle(videoId, result.language, result.releaseName, content)
    }
}
