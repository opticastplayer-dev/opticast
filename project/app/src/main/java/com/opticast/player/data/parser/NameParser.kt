package com.opticast.player.data.parser

import com.opticast.player.data.model.ParsedName

/**
 * Extracts a clean title, year and SxxExx info from typical scene-style file
 * names, e.g. "The.Bear.S02E05.1080p.WEB.h264-EDITH.mkv" or
 * "Dune.Part.Two.2024.2160p.WEB-DL.x265.mkv".
 */
object NameParser {

    private val junkTokens = setOf(
        "2160p", "1440p", "1080p", "1080i", "720p", "576p", "480p", "360p",
        "4k", "uhd", "sdr", "hdr", "hdr10", "hdr10plus", "dv", "dovi",
        "bluray", "blu", "ray", "brrip", "bdrip", "webrip", "webdl", "web",
        "hdtv", "dvdrip", "dvd", "hdcam", "cam",
        "x264", "x265", "h264", "h265", "hevc", "avc", "av1", "xvid",
        "aac", "aac2", "ac3", "eac3", "ddp", "dd", "dts", "truehd", "atmos", "opus", "mp3",
        "remux", "proper", "repack", "internal", "extended", "remastered", "unrated",
        "10bit", "8bit", "hi10p", "amzn", "nf", "dsnp", "hmax", "atvp",
    )

    private val episodePattern = Regex("(?i)S(\\d{1,2})[ ._\\-]*E(\\d{1,3})")
    private val episodePatternAlt = Regex("(?<![A-Za-z0-9])(\\d{1,2})x(\\d{2,3})(?![A-Za-z0-9])")
    private val yearPattern = Regex("(?<![0-9])(19\\d{2}|20\\d{2})(?![0-9])")

    fun parse(fileName: String): ParsedName {
        val base = fileName.substringBeforeLast('.').ifBlank { fileName }

        // TV episode: S01E02 style
        episodePattern.find(base)?.let { match ->
            return ParsedName(
                title = clean(base.substring(0, match.range.first)),
                season = match.groupValues[1].toIntOrNull(),
                episode = match.groupValues[2].toIntOrNull(),
            )
        }
        // TV episode: 1x02 style
        episodePatternAlt.find(base)?.let { match ->
            return ParsedName(
                title = clean(base.substring(0, match.range.first)),
                season = match.groupValues[1].toIntOrNull(),
                episode = match.groupValues[2].toIntOrNull(),
            )
        }

        // Movie: take the last plausible year as the release year.
        // ("2001.A.Space.Odyssey.1968.1080p" -> title "2001 A Space Odyssey", year 1968)
        val yearMatch = yearPattern.findAll(base)
            .lastOrNull { (it.value.toIntOrNull() ?: 0) in 1900..2099 }
        if (yearMatch != null) {
            val title = clean(base.substring(0, yearMatch.range.first))
            if (title.isNotBlank()) {
                return ParsedName(title = title, year = yearMatch.value.toIntOrNull())
            }
        }

        return ParsedName(title = clean(base))
    }

    private fun clean(raw: String): String {
        val noBrackets = raw.replace(Regex("\\[[^\\]]*]|\\([^)]*\\)"), " ")
        val spaced = noBrackets.replace('.', ' ').replace('_', ' ')
        val tokens = spaced.split(Regex("\\s+"))
            .filter { it.isNotBlank() }
            .filterNot { it.lowercase() in junkTokens }
            .filterNot { it.startsWith("-") }
        return tokens.joinToString(" ").trim()
    }
}
