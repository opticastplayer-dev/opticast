package com.opticast.player.domain.usecase

import com.opticast.player.data.AppContainer
import com.opticast.player.data.model.LibraryEntry
import com.opticast.player.data.model.Metadata
import com.opticast.player.data.repository.TmdbRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

/**
 * Gold Standard UseCase — Match metadata + cache subtitles offline-first
 * - Business logic out of ViewModel (was in LibraryViewModel autoMatch)
 * - Pure, testable, uses repositories
 * - Offline-first: subtitles cached during scan when online
 */
class MatchMetadataUseCase(
    private val tmdbRepository: TmdbRepository? = null
) {
    data class MatchResult(
        val matched: Int,
        val subtitleFailures: Int
    )

    suspend operator fun invoke(
        entries: List<LibraryEntry>,
        manual: Boolean = false,
        onProgress: (done: Int, total: Int) -> Unit = { _, _ -> }
    ): MatchResult = withContext(Dispatchers.IO) {
        val settings = AppContainer.settings.current()
        if (!AppContainer.isOnline()) return@withContext MatchResult(0, 0)

        val hasSubtitleKeys = settings.openSubtitlesApiKey.isNotBlank() || settings.subdlApiKey.isNotBlank()
        val autoSubs = hasSubtitleKeys
        var subtitleFailures = 0
        var matchedCount = 0

        val unmatched = entries.filter {
            AppContainer.offlineLibrary.needsMatch(it.video, it.metadata, settings.tmdbApiKey, manual)
        }

        if (unmatched.isEmpty() && !autoSubs) return@withContext MatchResult(0, 0)

        if (unmatched.isNotEmpty()) unmatched.chunked(3).forEach { chunk ->
            coroutineScope {
                chunk.map { entry ->
                    async {
                        runCatching {
                            val metadata = AppContainer.offlineLibrary.match(entry.video, entry.metadata, manual)
                            if (metadata != null && metadata != entry.metadata) {
                                AppContainer.detailCache.clear(entry.video.id)
                                AppContainer.metadataStore.save(entry.video.id, metadata)
                            }
                            if (autoSubs && subtitleFailures < 2 &&
                                AppContainer.metadataStore.subtitlesFor(entry.video.id).isEmpty()
                            ) {
                                runCatching {
                                    val results = AppContainer.subtitles.search(
                                        metadata = metadata ?: entry.metadata ?: return@runCatching,
                                        fallbackQuery = metadata?.displayTitle ?: entry.video.parsed.title,
                                        languages = settings.subtitleLanguages,
                                        season = metadata?.seasonNumber,
                                        episode = metadata?.episodeNumber
                                    )
                                    results.firstOrNull()?.let {
                                        AppContainer.subtitles.download(it, entry.video.id)
                                    }
                                }.onFailure { subtitleFailures++ }
                            }
                        }
                        matchedCount++
                        onProgress(matchedCount, unmatched.size)
                    }
                }.awaitAll()
            }
        }

        MatchResult(matchedCount, subtitleFailures)
    }
}
