package com.opticast.player.data.remote

import com.opticast.player.data.model.LocalVideo
import com.opticast.player.data.model.Metadata
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import com.opticast.player.data.AppContainer

/** One AniList search hit, for the manual match screen. */
data class AniResult(
    val id: Int,
    val title: String,
    val originalTitle: String,
    val coverUrl: String?,
    val bannerUrl: String?,
    val description: String,
    val year: Int?,
    val averageScore: Int?,
    val episodes: Int?,
    val format: String?,
)

@Serializable
private data class AniTitleDto(val romaji: String? = null, val english: String? = null)

@Serializable
private data class AniCoverDto(val large: String? = null, val extraLarge: String? = null)

@Serializable
private data class AniMediaDto(
    val id: Int = 0,
    val title: AniTitleDto = AniTitleDto(),
    val coverImage: AniCoverDto = AniCoverDto(),
    val bannerImage: String? = null,
    val description: String? = null,
    val seasonYear: Int? = null,
    val averageScore: Int? = null,
    val episodes: Int? = null,
    val format: String? = null,
)

@Serializable
private data class AniPageDto(val media: List<AniMediaDto> = emptyList())

@Serializable
private data class AniDataDto(val Page: AniPageDto? = null, val Media: AniMediaDto? = null)

@Serializable
private data class AniErrorDto(val message: String? = null)

@Serializable
private data class AniResponseDto(val data: AniDataDto? = null, val errors: List<AniErrorDto>? = null)

@Serializable
private data class GraphQLRequest(
    val query: String,
    val variables: Map<String, String> = emptyMap(),
)

/**
 * Client for AniList's free GraphQL API - no API key required. Used for
 * anime recognition (release-group style filenames) and manual anime matching.
 */
class AniListApi {

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()
    private val json = Json { ignoreUnknownKeys = true }

    private val mediaFields = """
        id
        title { romaji english }
        coverImage { large extraLarge }
        bannerImage
        description
        seasonYear
        averageScore
        episodes
        format
    """.trimIndent()

    private fun stripHtml(text: String?): String =
        text?.replace(Regex("<[^>]+>"), " ")
            ?.replace(Regex("\\s+"), " ")
            ?.trim()
            .orEmpty()

    private suspend fun post(query: String, variables: Map<String, String>): String? =
        withContext(Dispatchers.IO) {
            runCatching {
                val payload = json.encodeToString(
                    GraphQLRequest.serializer(),
                    GraphQLRequest(query, variables),
                )
                val request = Request.Builder()
                    .url("https://graphql.anilist.co")
                    .header("Content-Type", "application/json")
                    .post(payload.toRequestBody("application/json".toMediaType()))
                    .build()
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) response.body?.string() else null
                }
            }.getOrNull()
        }

    private fun toResult(dto: AniMediaDto): AniResult = AniResult(
        id = dto.id,
        title = dto.title.english ?: dto.title.romaji ?: "",
        originalTitle = dto.title.romaji ?: "",
        coverUrl = if (AppContainer.dataSaver) {
            dto.coverImage.large ?: dto.coverImage.extraLarge
        } else {
            dto.coverImage.extraLarge ?: dto.coverImage.large
        },
        bannerUrl = dto.bannerImage,
        description = stripHtml(dto.description),
        year = dto.seasonYear,
        averageScore = dto.averageScore,
        episodes = dto.episodes,
        format = dto.format,
    )

    /** Searches AniList for anime matching [query]. */
    suspend fun search(query: String): List<AniResult> {
        if (query.isBlank()) return emptyList()
        val body = post(
            "query(\$search: String) { Page(perPage: 10) { media(search: \$search, type: ANIME) { $mediaFields } } }",
            mapOf("search" to query),
        ) ?: return emptyList()
        val dto = runCatching { json.decodeFromString<AniResponseDto>(body) }.getOrNull()
            ?: return emptyList()
        return dto.data?.Page?.media.orEmpty()
            .map(::toResult)
            .filter { it.title.isNotBlank() }
    }

    /** Full metadata for one AniList id. */
    suspend fun mediaMetadata(
        id: Int,
        season: Int? = null,
        episode: Int? = null,
    ): Metadata? {
        val body = post(
            "query(\$id: Int) { Media(id: \$id, type: ANIME) { $mediaFields } }",
            mapOf("id" to id.toString()),
        ) ?: return null
        val dto = runCatching { json.decodeFromString<AniResponseDto>(body) }.getOrNull()
        val media = dto?.data?.Media ?: return null
        return toMetadata(media, season, episode)
    }

    private fun toMetadata(media: AniMediaDto, season: Int?, episode: Int?): Metadata {
        val title = media.title.english ?: media.title.romaji ?: ""
        return Metadata(
            tmdbId = media.id,
            type = "tv",
            title = title,
            originalTitle = media.title.romaji ?: title,
            overview = stripHtml(media.description),
            posterPath = media.coverImage.extraLarge ?: media.coverImage.large,
            backdropPath = media.bannerImage,
            voteAverage = (media.averageScore ?: 0) / 10.0,
            year = media.seasonYear,
            showTitle = title,
            seasonNumber = season ?: 1,
            episodeNumber = episode,
            episodeName = episode?.let { "Episode $it" },
            source = "anilist",
        )
    }

    // ----------------------------------------------------------- auto matching

    /**
     * Best-effort match for release-group style filenames such as
     * "[SubGroup] Show Name - 01 [1080p].mkv". Returns null for files that do
     * not look like scene/anime releases, so home videos are never matched.
     */
    suspend fun autoMatch(video: LocalVideo): Metadata? {
        val name = video.name
        val hasReleaseGroup = name.startsWith("[") ||
            name.contains(Regex("""\[[^\]]+\]"""))
        if (!hasReleaseGroup) return null

        val (title, parsedEpisode) = cleanAnimeTitle(name)
        if (title.isBlank() || title.length < 3) return null

        val candidates = search(title)
        if (candidates.isEmpty()) return null
        val episode = parsedEpisode ?: video.parsed.episode
        return mediaMetadata(candidates.first().id, season = 1, episode = episode)
    }

    /** Strips group tags and pulls out the " - NN " episode number. */
    private fun cleanAnimeTitle(fileName: String): Pair<String, Int?> {
        var t = fileName.substringBeforeLast('.').replace('_', ' ')
        t = t.replace(Regex("""^\s*(\[[^\]]*]\s*)+"""), "")
        t = t.replace(Regex("""(\s*\[[^\]]*])+\s*$"""), "")
        t = t.replace(Regex("""\b(1080p|720p|2160p|480p|HEVC|H264|H265|x264|x265|AAC|10bit|AV1)\b"""), "")
        val match = Regex("""^(.*?)[\s]*-[\s]*(\d{1,4})(v\d+)?\s*$""").find(t)
        return if (match != null) {
            match.groupValues[1].trim() to match.groupValues[2].toIntOrNull()
        } else {
            t.trim() to null
        }
    }
}
