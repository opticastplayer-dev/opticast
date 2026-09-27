package com.opticast.player.data.remote

import com.opticast.player.data.SettingsRepository
import com.opticast.player.data.model.Metadata
import com.opticast.player.data.model.SavedSubtitle

/**
 * Brokers subtitle search/download across providers: OpenSubtitles first,
 * SubDL as an extra source (and automatic fallback). Providers without a
 * configured key are skipped silently.
 */
class SubtitleSources(
    private val openSubtitles: OpenSubtitlesApi,
    private val subDl: SubDlApi,
    private val settings: SettingsRepository,
) {

    suspend fun search(
        metadata: Metadata?,
        fallbackQuery: String,
        languages: List<String>,
        season: Int?,
        episode: Int?,
    ): List<SubtitleResult> {
        val current = settings.current()
        val results = mutableListOf<SubtitleResult>()
        val errors = mutableListOf<String>()

        if (current.openSubtitlesApiKey.isNotBlank()) {
            runCatching {
                results += openSubtitles.search(metadata, fallbackQuery, languages, season, episode)
            }.onFailure { errors += it.message ?: "OpenSubtitles search failed." }
        }
        if (current.subdlApiKey.isNotBlank()) {
            runCatching {
                results += subDl.search(metadata, fallbackQuery, languages, season, episode)
            }.onFailure { errors += it.message ?: "SubDL search failed." }
        }

        if (results.isEmpty() && errors.isNotEmpty()) {
            throw SubtitleSearchException(errors.first())
        }
        if (results.isEmpty()) {
            throw SubtitleSearchException(
                "Add a subtitle API key (OpenSubtitles or SubDL) in Settings first.",
            )
        }
        return results
    }

    suspend fun download(result: SubtitleResult, videoId: Long): SavedSubtitle =
        when (result.source) {
            "subdl" -> subDl.download(result, videoId)
            else -> openSubtitles.download(result, videoId)
        }
}
