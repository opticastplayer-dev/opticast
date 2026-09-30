package com.opticast.player.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit

/**
 * Secure TMDB client via your Express proxy at https://tmdb-proxy-xstu.onrender.com/
 * 
 * Benefits:
 * - No API key in APK (key stays server-side in Render env var)
 * - Single base URL, easy to rotate key server-side
 * - Handles Render free-tier cold start (server waking ~30-50s)
 *
 * Proxy expected routes (your Express server should forward to api.themoviedb.org):
 * GET /3/search/movie?query=...
 * GET /3/search/tv?query=...
 * GET /3/movie/{id}
 * GET /3/tv/{id}
 * GET /3/tv/{id}/season/{season}
 * GET /3/movie/{id}/credits
 * GET /3/tv/{id}/credits
 * GET /3/movie/{id}/external_ids  (for imdb_id)
 * GET /3/tv/{id}/external_ids
 */

// ---------------------------------------------------------------------------
// 1. BASE CLIENT + CONFIG
// ---------------------------------------------------------------------------

object TmdbProxyConfig {
    // CONFIRMED WORKING: proxy uses /api/* not /3/*
    // Tested: /api/search/movie, /api/movie/550, /api/tv/1396 etc work
    // /3/search/movie returns Cannot GET
    const val BASE_URL = "https://tmdb-proxy-xstu.onrender.com/api/"
    // Render free tier sleeps after 15m, wake takes 30-50s -> need long timeouts
    const val CONNECT_TIMEOUT_SEC = 35L
    const val READ_TIMEOUT_SEC = 60L
    const val WRITE_TIMEOUT_SEC = 30L
    const val MAX_RETRIES = 3
    const val RETRY_DELAY_BASE_MS = 2000L // 2s, 4s, 8s
}

sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class Error(
        val message: String,
        val code: Int? = null,
        val isNetworkError: Boolean = false,
        val isServerWaking: Boolean = false,
    ) : ApiResult<Nothing>()

    object Loading : ApiResult<Nothing>()
}

class TmdbProxyClient {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    // OkHttp tuned for Render cold start
    private val client = OkHttpClient.Builder()
        .connectTimeout(TmdbProxyConfig.CONNECT_TIMEOUT_SEC, TimeUnit.SECONDS)
        .readTimeout(TmdbProxyConfig.READ_TIMEOUT_SEC, TimeUnit.SECONDS)
        .writeTimeout(TmdbProxyConfig.WRITE_TIMEOUT_SEC, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private fun buildUrl(path: String, queryParams: Map<String, String?> = emptyMap()): String {
        val urlBuilder = (TmdbProxyConfig.BASE_URL + path.trimStart('/')).toHttpUrl().newBuilder()
        for ((k, v) in queryParams) {
            if (v != null) urlBuilder.addQueryParameter(k, v)
        }
        return urlBuilder.build().toString()
    }

    /**
     * Core GET with retry for Render waking up (502/503/504 + SocketTimeout)
     * Returns raw JSON string or throws mapped ApiResult.Error
     */
    private suspend fun getWithRetry(url: String): ApiResult<String> = withContext(Dispatchers.IO) {
        var lastError: ApiResult.Error? = null

        repeat(TmdbProxyConfig.MAX_RETRIES) { attempt ->
            try {
                val request = Request.Builder()
                    .url(url)
                    .header("Accept", "application/json")
                    .header("Content-Type", "application/json")
                    // Optional: helps your Express log that it's from Android
                    .header("X-Client", "OptiCast-Android")
                    .get()
                    .build()

                client.newCall(request).execute().use { response ->
                    val body = response.body?.string()
                    when {
                        response.isSuccessful && body != null -> {
                            return@withContext ApiResult.Success(body)
                        }
                        response.code in listOf(502, 503, 504) -> {
                            // Render is waking up - common on free tier
                            lastError = ApiResult.Error(
                                message = "Server is waking up, retrying... (${attempt + 1}/${TmdbProxyConfig.MAX_RETRIES})",
                                code = response.code,
                                isServerWaking = true,
                                isNetworkError = true
                            )
                            // Exponential backoff: 2s, 4s, 8s
                            delay(TmdbProxyConfig.RETRY_DELAY_BASE_MS * (1 shl attempt))
                        }
                        response.code == 429 -> {
                            val retryAfter = response.header("Retry-After")?.toLongOrNull() ?: 5L
                            lastError = ApiResult.Error(
                                message = "Rate limited, retry after ${retryAfter}s",
                                code = 429
                            )
                            delay(retryAfter * 1000)
                        }
                        else -> {
                            return@withContext ApiResult.Error(
                                message = "Server error: ${response.code} ${response.message} ${body?.take(200) ?: ""}",
                                code = response.code
                            )
                        }
                    }
                }
            } catch (e: SocketTimeoutException) {
                // Render cold start often manifests as timeout
                lastError = ApiResult.Error(
                    message = "Server waking up (timeout), retrying... ${e.message}",
                    isServerWaking = true,
                    isNetworkError = true
                )
                delay(TmdbProxyConfig.RETRY_DELAY_BASE_MS * (1 shl attempt))
            } catch (e: IOException) {
                // No internet, DNS, etc.
                lastError = ApiResult.Error(
                    message = "Network error: ${e.message}",
                    isNetworkError = true
                )
                // Don't retry immediately on no internet, but retry for Render wake
                if (e.message?.contains("Unable to resolve host") == true) {
                    return@withContext lastError!!
                }
                delay(TmdbProxyConfig.RETRY_DELAY_BASE_MS * (1 shl attempt))
            } catch (e: Exception) {
                return@withContext ApiResult.Error(message = "Unexpected: ${e.message}")
            }
        }
        return@withContext lastError ?: ApiResult.Error("Failed after ${TmdbProxyConfig.MAX_RETRIES} retries")
    }

    suspend inline fun <reified T> get(
        path: String,
        query: Map<String, String?> = emptyMap(),
        noinline parser: (String) -> T = { jsonStr -> json.decodeFromString(jsonStr) }
    ): ApiResult<T> {
        val url = buildUrl(path, query)
        return when (val result = getWithRetry(url)) {
            is ApiResult.Success -> {
                try {
                    ApiResult.Success(parser(result.data))
                } catch (e: Exception) {
                    ApiResult.Error("Parsing failed: ${e.message}, raw: ${result.data.take(300)}")
                }
            }
            is ApiResult.Error -> result
            is ApiResult.Loading -> result
        }
    }

    // For raw access if you need custom parsing
    suspend fun getRaw(path: String, query: Map<String, String?> = emptyMap()): ApiResult<String> {
        return getWithRetry(buildUrl(path, query))
    }
}

// ---------------------------------------------------------------------------
// 2. DATA MODELS — Matching TMDB JSON exactly
// ---------------------------------------------------------------------------

@Serializable
data class TmdbSearchResponse(
    val page: Int = 1,
    val results: List<TmdbSearchResultDto> = emptyList(),
    @SerialName("total_pages") val totalPages: Int = 0,
    @SerialName("total_results") val totalResults: Int = 0
)

@Serializable
data class TmdbSearchResultDto(
    val id: Int,
    val title: String? = null, // movies
    val name: String? = null,  // tv
    val overview: String = "",
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("backdrop_path") val backdropPath: String? = null,
    @SerialName("vote_average") val voteAverage: Double = 0.0,
    @SerialName("vote_count") val voteCount: Int = 0,
    @SerialName("release_date") val releaseDate: String? = null,
    @SerialName("first_air_date") val firstAirDate: String? = null,
    @SerialName("original_title") val originalTitle: String? = null,
    @SerialName("original_name") val originalName: String? = null,
    @SerialName("original_language") val originalLanguage: String? = null,
    @SerialName("genre_ids") val genreIds: List<Int> = emptyList(),
    val popularity: Double = 0.0,
    @SerialName("media_type") val mediaType: String? = null
) {
    val displayTitle: String get() = title ?: name ?: ""
    val year: Int? get() = (releaseDate ?: firstAirDate)?.take(4)?.toIntOrNull()
}

@Serializable
data class TmdbGenreDto(
    val id: Int = 0,
    val name: String = ""
)

@Serializable
data class TmdbMovieDetailsDto(
    val id: Int,
    val title: String? = null,
    val overview: String = "",
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("backdrop_path") val backdropPath: String? = null,
    @SerialName("vote_average") val voteAverage: Double = 0.0,
    @SerialName("vote_count") val voteCount: Int = 0,
    @SerialName("release_date") val releaseDate: String? = null,
    @SerialName("original_title") val originalTitle: String? = null,
    val runtime: Int? = null,
    val genres: List<TmdbGenreDto> = emptyList(),
    val status: String? = null,
    val tagline: String? = null,
    @SerialName("imdb_id") val imdbId: String? = null
)

@Serializable
data class TmdbTvDetailsDto(
    val id: Int,
    val name: String? = null,
    val overview: String = "",
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("backdrop_path") val backdropPath: String? = null,
    @SerialName("vote_average") val voteAverage: Double = 0.0,
    @SerialName("vote_count") val voteCount: Int = 0,
    @SerialName("first_air_date") val firstAirDate: String? = null,
    @SerialName("last_air_date") val lastAirDate: String? = null,
    @SerialName("original_name") val originalName: String? = null,
    @SerialName("episode_run_time") val episodeRunTime: List<Int> = emptyList(),
    val genres: List<TmdbGenreDto> = emptyList(),
    val status: String? = null,
    val tagline: String? = null,
    @SerialName("number_of_seasons") val numberOfSeasons: Int? = null,
    @SerialName("number_of_episodes") val numberOfEpisodes: Int? = null
)

@Serializable
data class TmdbEpisodeDto(
    val id: Int = 0,
    @SerialName("episode_number") val episodeNumber: Int = 0,
    @SerialName("season_number") val seasonNumber: Int = 0,
    val name: String? = null,
    val overview: String = "",
    @SerialName("still_path") val stillPath: String? = null,
    @SerialName("vote_average") val voteAverage: Double = 0.0,
    @SerialName("air_date") val airDate: String? = null,
    val runtime: Int? = null
)

@Serializable
data class TmdbSeasonDto(
    val id: Int = 0,
    @SerialName("season_number") val seasonNumber: Int = 0,
    val name: String? = null,
    val overview: String = "",
    @SerialName("poster_path") val posterPath: String? = null,
    val episodes: List<TmdbEpisodeDto> = emptyList()
)

@Serializable
data class TmdbCastMemberDto(
    val id: Int = 0,
    val name: String = "",
    @SerialName("original_name") val originalName: String? = null,
    val character: String = "",
    @SerialName("profile_path") val profilePath: String? = null,
    val order: Int = 0
)

@Serializable
data class TmdbCreditsResponse(
    val id: Int = 0,
    val cast: List<TmdbCastMemberDto> = emptyList(),
    val crew: List<TmdbCrewMemberDto> = emptyList()
)

@Serializable
data class TmdbCrewMemberDto(
    val id: Int = 0,
    val name: String = "",
    val job: String? = null,
    val department: String? = null
)

@Serializable
data class TmdbExternalIdsDto(
    @SerialName("imdb_id") val imdbId: String? = null,
    @SerialName("tvdb_id") val tvdbId: Int? = null
)

// ---------------------------------------------------------------------------
// 3. HIGH-LEVEL SERVICE — Drop-in replacement for your old TmdbApi
// ---------------------------------------------------------------------------

/**
 * New secure service using proxy. No API key needed client-side.
 * Usage:
 * val tmdb = TmdbProxyService()
 * when(val res = tmdb.searchMovies("Inception")) {
 *   is ApiResult.Success -> show(res.data)
 *   is ApiResult.Error -> if(res.isServerWaking) showWakingUI() else showError(res.message)
 * }
 */
class TmdbProxyService {

    private val client = TmdbProxyClient()

    // ---------------- Search ----------------

    suspend fun searchMovies(query: String, year: Int? = null): ApiResult<List<TmdbSearchResultDto>> {
        if (query.isBlank()) return ApiResult.Success(emptyList())
        val result: ApiResult<TmdbSearchResponse> = client.get(
            path = "search/movie",
            query = mapOf(
                "query" to query,
                "include_adult" to "false",
                "year" to year?.toString()
            )
        )
        return when (result) {
            is ApiResult.Success -> ApiResult.Success(result.data.results)
            is ApiResult.Error -> result
            is ApiResult.Loading -> result
        }
    }

    suspend fun searchTv(query: String, year: Int? = null): ApiResult<List<TmdbSearchResultDto>> {
        if (query.isBlank()) return ApiResult.Success(emptyList())
        val result: ApiResult<TmdbSearchResponse> = client.get(
            path = "search/tv",
            query = mapOf(
                "query" to query,
                "include_adult" to "false",
                "first_air_date_year" to year?.toString()
            )
        )
        return when (result) {
            is ApiResult.Success -> ApiResult.Success(result.data.results)
            is ApiResult.Error -> result
            is ApiResult.Loading -> result
        }
    }

    // ---------------- Details ----------------

    suspend fun getMovieDetails(tmdbId: Int): ApiResult<TmdbMovieDetailsDto> {
        return client.get(path = "movie/$tmdbId")
    }

    suspend fun getTvDetails(tmdbId: Int): ApiResult<TmdbTvDetailsDto> {
        return client.get(path = "tv/$tmdbId")
    }

    suspend fun getSeasonDetails(tmdbId: Int, seasonNumber: Int): ApiResult<TmdbSeasonDto> {
        return client.get(path = "tv/$tmdbId/season/$seasonNumber")
    }

    suspend fun getEpisodeDetails(tmdbId: Int, seasonNumber: Int, episodeNumber: Int): ApiResult<TmdbEpisodeDto> {
        return client.get(path = "tv/$tmdbId/season/$seasonNumber/episode/$episodeNumber")
    }

    // ---------------- Credits & External IDs ----------------

    suspend fun getMovieCredits(tmdbId: Int): ApiResult<TmdbCreditsResponse> {
        return client.get(path = "movie/$tmdbId/credits")
    }

    suspend fun getTvCredits(tmdbId: Int): ApiResult<TmdbCreditsResponse> {
        return client.get(path = "tv/$tmdbId/credits")
    }

    suspend fun getMovieExternalIds(tmdbId: Int): ApiResult<TmdbExternalIdsDto> {
        return client.get(path = "movie/$tmdbId/external_ids")
    }

    suspend fun getTvExternalIds(tmdbId: Int): ApiResult<TmdbExternalIdsDto> {
        return client.get(path = "tv/$tmdbId/external_ids")
    }

    // Convenience: get imdb_id for OMDb enrichment (like old imdbIdFor)
    suspend fun getImdbId(tmdbId: Int, isTv: Boolean): String? {
        val result = if (isTv) getTvExternalIds(tmdbId) else getMovieExternalIds(tmdbId)
        return when (result) {
            is ApiResult.Success -> result.data.imdbId
            else -> null
        }
    }
}

// ---------------------------------------------------------------------------
// 4. UI HELPER — For your Compose UI to handle Render waking state
// ---------------------------------------------------------------------------

/**
 * Example Compose helper (put in your ViewModel):
 *
 * fun handleResult(result: ApiResult<*>) {
 *   when(result) {
 *     is ApiResult.Success -> _uiState.value = UiState.Success(result.data)
 *     is ApiResult.Error -> {
 *       _uiState.value = when {
 *         result.isServerWaking -> UiState.ServerWaking("Server waking up, please wait 30s...")
 *         result.isNetworkError -> UiState.NoInternet("Check connection")
 *         else -> UiState.Error(result.message)
 *       }
 *     }
 *   }
 * }
 *
 * In Composable:
 * if(uiState is ServerWaking) {
 *   Box { CircularProgressIndicator(); Text("Waking up server, this happens once after idle...") }
 * }
 */
