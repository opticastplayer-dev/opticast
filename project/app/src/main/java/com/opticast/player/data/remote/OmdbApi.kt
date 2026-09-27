package com.opticast.player.data.remote

import com.opticast.player.data.SettingsRepository
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request

/** Ratings bundle from the Open Movie Database (OMDb). */
data class OmdbRatings(
    val imdb: String?,
    val imdbVotes: String?,
    val rottenTomatoes: String?,
    val metacritic: String?,
    val rated: String?,
    val awards: String?,
    val boxOffice: String?,
    val director: String?,
    val writer: String?,
)

@Serializable
private data class OmdbRatingDto(
    @SerialName("Source") val source: String = "",
    @SerialName("Value") val value: String = "",
)

@Serializable
private data class OmdbResponseDto(
    @SerialName("Response") val response: String = "",
    @SerialName("imdbRating") val imdbRating: String? = null,
    @SerialName("imdbVotes") val imdbVotes: String? = null,
    @SerialName("Ratings") val ratings: List<OmdbRatingDto> = emptyList(),
    @SerialName("Rated") val rated: String? = null,
    @SerialName("Awards") val awards: String? = null,
    @SerialName("BoxOffice") val boxOffice: String? = null,
    @SerialName("Director") val director: String? = null,
    @SerialName("Writer") val writer: String? = null,
)

/**
 * Client for omdbapi.com — one small request gets IMDb, Rotten Tomatoes and
 * Metacritic ratings plus a few extra facts. Free key: 1,000 requests/day.
 */
class OmdbApi(private val settings: SettingsRepository) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()
    private val json = Json { ignoreUnknownKeys = true }
    private val cache = mutableMapOf<String, OmdbRatings>()

    suspend fun byImdbId(imdbId: String): OmdbRatings? {
        if (imdbId.isBlank()) return null
        cache[imdbId]?.let { return it }
        val key = settings.current().omdbApiKey.takeIf { it.isNotBlank() } ?: return null

        val url = "https://www.omdbapi.com/?apikey=$key&i=$imdbId&tomatoes=true"
        val body = withContext(Dispatchers.IO) {
            runCatching {
                client.newCall(Request.Builder().url(url).build()).execute().use { response ->
                    if (response.isSuccessful) response.body?.string() else null
                }
            }.getOrNull()
        } ?: return null

        val dto = runCatching { json.decodeFromString<OmdbResponseDto>(body) }.getOrNull()
            ?: return null
        if (dto.response != "True") return null

        fun ratingOf(source: String): String? =
            dto.ratings.firstOrNull { it.source.equals(source, ignoreCase = true) }
                ?.value?.takeIf { it.isNotBlank() && it != "N/A" }

        val result = OmdbRatings(
            imdb = dto.imdbRating?.takeIf { it.isNotBlank() && it != "N/A" },
            imdbVotes = dto.imdbVotes?.takeIf { it.isNotBlank() && it != "N/A" },
            rottenTomatoes = ratingOf("Rotten Tomatoes")?.removeSuffix("%"),
            metacritic = ratingOf("Metacritic")?.removeSuffix("/100"),
            rated = dto.rated?.takeIf { it.isNotBlank() && it != "N/A" },
            awards = dto.awards?.takeIf { it.isNotBlank() && it != "N/A" },
            boxOffice = dto.boxOffice?.takeIf { it.isNotBlank() && it != "N/A" },
            director = dto.director?.takeIf { it.isNotBlank() && it != "N/A" },
            writer = dto.writer?.takeIf { it.isNotBlank() && it != "N/A" },
        )
        cache[imdbId] = result
        return result
    }
}
