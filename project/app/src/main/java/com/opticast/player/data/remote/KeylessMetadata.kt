package com.opticast.player.data.remote

import android.text.Html
import com.opticast.player.data.model.LocalVideo
import com.opticast.player.data.model.Metadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/** Public APIs only; no embedded credentials. Simplified: Wikipedia + TVmaze only, no Wikidata for lightness. */
class KeylessMetadata {
    private val client = OkHttpClient.Builder().connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS).callTimeout(20, TimeUnit.SECONDS).build()
    private val gate = Mutex()
    private var lastRequest = 0L
    private val blockedUntil = mutableMapOf<String, Long>()

    private suspend fun get(base: String, params: Map<String, String> = emptyMap()): JsonElement? =
        withContext(Dispatchers.IO) {
            gate.withLock {
                val url = base.toHttpUrl().newBuilder().apply {
                    params.forEach { (k, v) -> addQueryParameter(k, v) }
                }.build()
                if ((blockedUntil[url.host] ?: 0L) > System.currentTimeMillis()) return@withLock null
                delay((550 - (android.os.SystemClock.elapsedRealtime() - lastRequest)).coerceAtLeast(0))
                lastRequest = android.os.SystemClock.elapsedRealtime()
                try {
                    client.newCall(Request.Builder().url(url)
                        .header("User-Agent", "OptiCast/2.6.120 (Android; personal media metadata)")
                        .build()).execute().use { response ->
                        if (response.code == 429 || response.code == 503) {
                            val seconds = response.header("Retry-After")?.toLongOrNull() ?: 60L
                            blockedUntil[url.host] = System.currentTimeMillis() + seconds.coerceIn(1, 3600) * 1000
                        }
                        if (!response.isSuccessful) null
                        else response.body?.string()?.let { Json.parseToJsonElement(it) }
                    }
                } catch (_: java.io.IOException) { null }
            }
        }

    private fun JsonObject.text(key: String): String? = (get(key) as? JsonPrimitive)?.contentOrNull
    private fun JsonObject.obj(key: String): JsonObject? = get(key) as? JsonObject
    private fun normalized(text: String) = java.text.Normalizer.normalize(text, java.text.Normalizer.Form.NFD)
        .replace(Regex("\\p{M}"), "").lowercase(java.util.Locale.ROOT).replace(Regex("[^\\p{L}\\p{N}]"), "")
    private fun clean(text: String?) = Html.fromHtml(text.orEmpty(), Html.FROM_HTML_MODE_LEGACY).toString().trim()

    suspend fun autoMatch(video: LocalVideo): Metadata? =
        if (video.isEpisode) tvmaze(video) else wikipedia(video)

    private suspend fun wikipedia(video: LocalVideo): Metadata? {
        val title = video.parsed.title
        if (title.isBlank()) return null
        val root = get("https://en.wikipedia.org/w/api.php", mapOf(
            "action" to "query", "format" to "json", "formatversion" to "2",
            "generator" to "search", "gsrsearch" to "$title ${video.parsed.year ?: ""} film",
            "gsrnamespace" to "0", "gsrlimit" to "5", "prop" to "extracts|pageterms",
            "exintro" to "1", "explaintext" to "1", "exlimit" to "5",
            "wbptterms" to "description", "maxlag" to "5"
        )) as? JsonObject ?: return null
        return parseWikipedia(video, root).singleOrNull()
    }

    internal fun parseWikipedia(video: LocalVideo, root: JsonObject): List<Metadata> {
        val title = video.parsed.title
        val pages = root.obj("query")?.get("pages") as? JsonArray ?: return emptyList()
        val candidates = pages.mapNotNull { element ->
            val page = element as? JsonObject ?: return@mapNotNull null
            val pageTitle = page.text("title") ?: return@mapNotNull null
            val plainTitle = pageTitle.replace(Regex("\\s*\\([^)]*film\\)$"), "")
            if (normalized(plainTitle) != normalized(title)) return@mapNotNull null
            val description = (page.obj("terms")?.get("description") as? JsonArray)
                ?.firstOrNull()?.jsonPrimitive?.contentOrNull.orEmpty()
            val extract = page.text("extract").orEmpty()
            val lead = description.ifBlank { extract.take(250) }
            if (!Regex("\\bfilm\\b", RegexOption.IGNORE_CASE).containsMatchIn(lead)) return@mapNotNull null
            val year = Regex("\\b(?:18|19|20)\\d{2}\\b").find(lead)?.value?.toIntOrNull()
            if (video.parsed.year != null && year != video.parsed.year) return@mapNotNull null
            if (extract.isBlank()) return@mapNotNull null
            val id = page.text("pageid")?.toIntOrNull() ?: return@mapNotNull null
            Metadata(tmdbId = id, type = "movie", title = plainTitle, overview = extract,
                year = year, source = "wikipedia",
                sourceUrl = "https://en.wikipedia.org/?curid=$id",
                attribution = "Wikipedia contributors · CC BY-SA 4.0 · Introductory text; see article history for authors.")
        }
        return candidates
    }

    private suspend fun tvmaze(video: LocalVideo): Metadata? {
        val results = get("https://api.tvmaze.com/search/shows", mapOf("q" to video.parsed.title)) as? JsonArray
            ?: return null
        val matches = results.mapNotNull { (it as? JsonObject)?.obj("show") }.filter {
            normalized(it.text("name").orEmpty()) == normalized(video.parsed.title) &&
                (video.parsed.year == null || it.text("premiered")?.take(4)?.toIntOrNull() == video.parsed.year)
        }
        val show = matches.singleOrNull() ?: return null
        val id = show.text("id")?.toIntOrNull() ?: return null
        val episode = get("https://api.tvmaze.com/shows/$id/episodebynumber", mapOf(
            "season" to video.seasonNumber.toString(), "number" to video.episodeNumber.toString()
        )) as? JsonObject ?: return null
        return Metadata(tmdbId = id, type = "tv", title = show.text("name").orEmpty(),
            showTitle = show.text("name"), overview = clean(show.text("summary")),
            posterPath = show.obj("image")?.text("medium"),
            voteAverage = show.obj("rating")?.text("average")?.toDoubleOrNull() ?: 0.0,
            year = show.text("premiered")?.take(4)?.toIntOrNull(),
            genres = (show["genres"] as? JsonArray)?.mapNotNull { it.jsonPrimitive.contentOrNull }.orEmpty(),
            runtimeMinutes = episode.text("runtime")?.toIntOrNull(),
            seasonNumber = video.seasonNumber, episodeNumber = video.episodeNumber,
            episodeName = episode.text("name"), episodeOverview = clean(episode.text("summary")),
            episodeStillPath = episode.obj("image")?.text("medium"), source = "tvmaze",
            sourceUrl = show.text("url"), attribution = "TVmaze · CC BY-SA 4.0 · Summaries converted from HTML to plain text.")
    }
}
