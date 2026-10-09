package com.opticast.player.data

import com.opticast.player.data.local.CachedDetails
import com.opticast.player.data.model.LocalVideo
import com.opticast.player.data.model.Metadata
import com.opticast.player.data.remote.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** One shared pipeline for startup and detail-page matching. */
class OfflineLibrary(context: android.content.Context) {
    private val attempts = context.getSharedPreferences("tmdb_upgrade_attempts", android.content.Context.MODE_PRIVATE)
    private fun fingerprint(key: String): String = java.security.MessageDigest.getInstance("SHA-256")
        .digest(key.trim().toByteArray()).joinToString("") { "%02x".format(it) }
    private fun attemptKey(video: LocalVideo, metadata: Metadata) =
        "${video.id}:${video.name.hashCode()}:${metadata.source}:${metadata.tmdbId}"

    private fun initialAttemptKey(video: LocalVideo): String = "initial:" + fingerprint(
        "${video.id}|${video.uri}|${video.name}|${video.sizeBytes}|${video.dateAddedSec}")

    fun preserveAttempt(old: LocalVideo, moved: LocalVideo) {
        val edit = attempts.edit()
        var changed = false
        if (attempts.getBoolean(initialAttemptKey(old), false)) {
            edit.putBoolean(initialAttemptKey(moved), true)
            changed = true
        }
        AppContainer.metadataStore.get(old.id)?.let { metadata ->
            attempts.getString(attemptKey(old, metadata), null)?.let { previousKey ->
                edit.putString(attemptKey(moved, metadata), previousKey)
                changed = true
            }
        }
        if (changed) check(edit.commit())
    }

    fun needsMatch(video: LocalVideo, metadata: Metadata?, key: String, manual: Boolean = false): Boolean {
        if (metadata == null) {
            if (com.opticast.player.data.local.requiresRenameClue(video.name) && AppContainer.renameSuggestions.clue(video).title.isBlank()) return false
            return shouldAttemptUnmatched(attempts.getBoolean(initialAttemptKey(video), false), manual)
        }
        return shouldUpgradeMetadata(metadata, true,
            !manual && attempts.getString(attemptKey(video, metadata), null) == fingerprint(key.ifBlank { "proxy" }))
    }

    private val matching = java.util.concurrent.ConcurrentHashMap<Long, Mutex>()
    private val gate = Mutex()
    private suspend fun <T> optional(block: suspend () -> T): T? = try { block() }
        catch (e: CancellationException) { throw e }
        catch (_: Exception) { null }

    suspend fun match(video: LocalVideo, existing: Metadata? = null, manual: Boolean = false): Metadata? {
        return matching.getOrPut(video.id) { Mutex() }.withLock {
        if (!AppContainer.isOnline()) return@withLock existing
        val key = AppContainer.settings.current().tmdbApiKey.trim().ifBlank { "proxy" }
        if (!needsMatch(video, existing, key, manual)) return@withLock existing
        val clue = AppContainer.renameSuggestions.clue(video)
        val lookup = if (clue.title.isNotBlank()) video.copy(parsed = clue.parsed()) else video
        val result = resolveMetadata(existing,
            tmdb = { AppContainer.tmdb.autoMatch(lookup) },
            keyless = { AppContainer.keyless.autoMatch(lookup) },
            anime = { AppContainer.anilist.autoMatch(lookup) })
        if (result != null && result.source != "tmdb") {
            attempts.edit().putString(attemptKey(video, result), fingerprint(key)).apply()
        }
        if (result == null) {
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                attempts.edit().putBoolean(initialAttemptKey(video), true).commit()
            }
        }
        result
        }
    }

    /** Only missing cast/images are downloaded. Stored data never expires. Simplified: TMDB only, no OMDb/Fanart. */
    suspend fun prepare(videoId: Long, metadata: Metadata) = gate.withLock {
        if (!AppContainer.isOnline()) return@withLock
        var details = AppContainer.detailCache.get(videoId)
        if (metadata.source == "tmdb" && details == null) {
            val cast = optional { AppContainer.tmdb.castFor(metadata.tmdbId, metadata.type == "tv") }
            val imdb = optional { AppContainer.tmdb.imdbIdFor(metadata.tmdbId, metadata.type == "tv") }
            if (cast != null || imdb != null) {
                details = CachedDetails.from(imdbId = imdb, cast = cast.orEmpty(), previous = null)
                details?.let { AppContainer.detailCache.save(videoId, it) }
            }
        }
        val urls = buildList {
            add(tmdbPosterUrl(metadata.posterPath))
            add(tmdbBackdropUrl(metadata.backdropPath))
            add(tmdbBackdropUrl(metadata.episodeStillPath))
            details?.imageUrls()?.forEach { add(tmdbImageUrl(it, "w185")) }
        }
        AppContainer.offlineArtwork.prefetch(urls, deferDuringPlayback = false)
    }
}

internal fun shouldUpgradeMetadata(metadata: Metadata, hasKey: Boolean, alreadyTriedKey: Boolean): Boolean =
    hasKey && metadata.source != "tmdb" && !metadata.manuallyMatched && !alreadyTriedKey

internal suspend fun resolveMetadata(
    existing: Metadata?,
    tmdb: suspend () -> Metadata?,
    keyless: suspend () -> Metadata?,
    anime: suspend () -> Metadata?,
): Metadata? {
    suspend fun attempt(provider: suspend () -> Metadata?): Metadata? = try { provider() }
        catch (e: CancellationException) { throw e }
        catch (_: Exception) { null }
    return attempt(tmdb) ?: existing ?: attempt(keyless) ?: attempt(anime)
}

internal fun shouldAttemptUnmatched(attempted: Boolean, manual: Boolean): Boolean = manual || !attempted
