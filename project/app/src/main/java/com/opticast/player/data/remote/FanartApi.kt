package com.opticast.player.data.remote

import com.opticast.player.data.SettingsRepository
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request

/** Artwork bundle from Fanart.tv (all images are absolute URLs). */
data class FanartArtwork(
    val logo: String?,
    val background: String?,
    val banner: String?,
    val discart: String?,
    val clearart: String?,
)

/**
 * Client for fanart.tv — one request per title returns high-quality clearlogos,
 * backgrounds, banners and disc art. Free API key required.
 */
class FanartApi(private val settings: SettingsRepository) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()
    private val json = Json { ignoreUnknownKeys = true }
    private val cache = mutableMapOf<String, FanartArtwork>()

    suspend fun artworkFor(tmdbId: Int, isTv: Boolean): FanartArtwork? {
        val cacheKey = "${if (isTv) "tv" else "movie"}-$tmdbId"
        cache[cacheKey]?.let { return it }
        val key = settings.current().fanartApiKey.takeIf { it.isNotBlank() } ?: return null

        val kind = if (isTv) "tv" else "movies"
        val url = "https://webservice.fanart.tv/v3/$kind/$tmdbId?api_key=$key"
        val body = withContext(Dispatchers.IO) {
            runCatching {
                client.newCall(Request.Builder().url(url).build()).execute().use { response ->
                    if (response.isSuccessful) response.body?.string() else null
                }
            }.getOrNull()
        } ?: return null

        val obj = runCatching { json.parseToJsonElement(body).jsonObject }.getOrNull()
            ?: return null

        fun firstUrl(vararg fields: String): String? {
            for (field in fields) {
                val url = (obj[field] as? JsonArray)
                    ?.firstOrNull()
                    ?.jsonObject
                    ?.get("url")
                    ?.jsonPrimitive
                    ?.contentOrNull
                if (!url.isNullOrBlank()) return url
            }
            return null
        }

        val artwork = if (isTv) {
            FanartArtwork(
                logo = firstUrl("hdtvlogo", "tvlogo", "clearlogo"),
                background = firstUrl("showbackground"),
                banner = firstUrl("tvbanner"),
                discart = null,
                clearart = firstUrl("hdclearart", "clearart"),
            )
        } else {
            FanartArtwork(
                logo = firstUrl("hdmovielogo", "movielogo"),
                background = firstUrl("moviebackground"),
                banner = firstUrl("moviebanner"),
                discart = firstUrl("moviedisc"),
                clearart = firstUrl("hdmovieclearart", "movieart"),
            )
        }
        cache[cacheKey] = artwork
        return artwork
    }
}
