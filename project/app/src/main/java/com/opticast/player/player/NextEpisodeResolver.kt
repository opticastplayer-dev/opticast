package com.opticast.player.player

import com.opticast.player.data.AppContainer
import com.opticast.player.data.model.LocalVideo
import com.opticast.player.data.model.Metadata

/** The next episode in a series, ready to be auto-played. */
data class NextEpisode(
    val video: LocalVideo,
    val metadata: Metadata?,
    val tag: String,
)

/** Finds the next episode of the current show among the device's videos. */
object NextEpisodeResolver {

    private fun normalize(s: String): String =
        s.lowercase().replace(Regex("[^a-z0-9 ]"), " ").replace(Regex("\\s+"), " ").trim()

    fun find(
        current: LocalVideo,
        currentMetadata: Metadata?,
        allVideos: List<LocalVideo>,
    ): NextEpisode? {
        val season = current.parsed.season ?: currentMetadata?.seasonNumber ?: return null
        val episode = current.parsed.episode ?: currentMetadata?.episodeNumber ?: return null
        val show = normalize(currentMetadata?.showTitle ?: current.parsed.title)
        if (show.isBlank()) return null

        val pool = allVideos.filter { candidate ->
            candidate.id != current.id &&
                candidate.isEpisode &&
                normalize(candidate.parsed.title) == show
        }

        // Next episode in the same season, else first episode of the next season.
        val next = pool.firstOrNull {
            it.parsed.season == season && it.parsed.episode == episode + 1
        } ?: pool.firstOrNull {
            it.parsed.season == season + 1 && it.parsed.episode == 1
        } ?: return null

        return NextEpisode(
            video = next,
            metadata = AppContainer.metadataStore.get(next.id),
            tag = "S%02dE%02d".format(next.parsed.season, next.parsed.episode),
        )
    }
}
