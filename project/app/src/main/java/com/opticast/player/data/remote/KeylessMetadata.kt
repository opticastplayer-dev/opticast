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

/** Public APIs only; no embedded credentials or unofficial scraping endpoints. */
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
                        .header("User-Agent", "LumaPlayer/2.6.1 (Android; personal media metadata)")
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
        if (video.isEpisode) tvmaze(video) else wikipedia(video) ?: wikidata(video)

    /** Conservative matching: exact normalized title and supplied release year. */
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
        // Do not silently choose between remakes when no release year was supplied.
        return candidates
    }

    /** Structured film fallback from Wikidata's CC0 public entity API. */
    private suspend fun wikidata(video: LocalVideo): Metadata? {
        val root = get("https://www.wikidata.org/w/api.php", mapOf(
            "action" to "wbsearchentities", "format" to "json", "language" to "en",
            "search" to video.parsed.title, "limit" to "10", "maxlag" to "5"
        )) as? JsonObject ?: return null
        val hits = (root["search"] as? JsonArray)?.mapNotNull { it as? JsonObject }.orEmpty()
        val hit = hits.filter {
            normalized(it.text("label").orEmpty()) == normalized(video.parsed.title) &&
                Regex("\\bfilm\\b", RegexOption.IGNORE_CASE).containsMatchIn(it.text("description").orEmpty()) &&
                (video.parsed.year == null || Regex("\\b${video.parsed.year}\\b")
                    .containsMatchIn(it.text("description").orEmpty()))
        }.singleOrNull() ?: return null
        val id = hit.text("id")?.takeIf { it.matches(Regex("Q[0-9]+")) } ?: return null
        val entityRoot = get("https://www.wikidata.org/w/api.php", mapOf(
            "action" to "wbgetentities", "format" to "json", "ids" to id,
            "props" to "claims", "maxlag" to "5"
        )) as? JsonObject ?: return null
        val claims = entityRoot.obj("entities")?.obj(id)?.obj("claims") ?: return null
        fun values(property: String): List<JsonObject> = (claims[property] as? JsonArray)?.mapNotNull {
            (it as? JsonObject)?.obj("mainsnak")?.obj("datavalue")?.obj("value")
        }.orEmpty()
        val year = values("P577").mapNotNull { it.text("time")?.removePrefix("+")?.take(4)?.toIntOrNull() }.minOrNull()
            ?: Regex("\\b(?:18|19|20)\\d{2}\\b").find(hit.text("description").orEmpty())?.value?.toIntOrNull()
        if (video.parsed.year != null && year != video.parsed.year) return null
        val quantity = values("P2047").firstOrNull()
        val amount = quantity?.text("amount")?.toDoubleOrNull()
        val minutes = when (quantity?.text("unit")?.substringAfterLast('/')) {
            "Q7727" -> amount
            "Q11574" -> amount?.div(60)
            "Q25235" -> amount?.times(60)
            else -> null
        }?.toInt()?.takeIf { it > 0 }
        val genreIds = values("P136").mapNotNull { it.text("id") }.distinct().take(10)
        val genres = if (genreIds.isEmpty()) emptyList() else {
            val labels = get("https://www.wikidata.org/w/api.php", mapOf(
                "action" to "wbgetentities", "format" to "json", "ids" to genreIds.joinToString("|"),
                "props" to "labels", "languages" to "en", "maxlag" to "5"
            )) as? JsonObject
            genreIds.mapNotNull { labels?.obj("entities")?.obj(it)?.obj("labels")?.obj("en")?.text("value") }
        }
        return Metadata(tmdbId = id.drop(1).toIntOrNull() ?: return null, type = "movie",
            title = hit.text("label").orEmpty(), overview = hit.text("description").orEmpty(),
            year = year, runtimeMinutes = minutes, genres = genres, source = "wikidata",
            sourceUrl = "https://www.wikidata.org/wiki/$id", attribution = "Wikidata contributors · CC0 public-domain structured data.")
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
