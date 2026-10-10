package com.opticast.player.data

import android.os.StatFs
import com.opticast.player.data.model.LibraryEntry
import java.io.File

data class StorageStats(
    val totalMoviesSize: Long,
    val totalShowsSize: Long,
    val totalSize: Long,
    val availableSpace: Long,
    val totalSpace: Long,
    val biggestFiles: List<LibraryEntry>,
    val moviesCount: Int,
    val showsCount: Int
)

object StorageAnalyzer {

    fun analyze(entries: List<LibraryEntry>): StorageStats {
        val movies = entries.filter { !it.video.isEpisode }
        val shows = entries.filter { it.video.isEpisode }

        val moviesSize = movies.sumOf { it.video.sizeBytes }
        val showsSize = shows.sumOf { it.video.sizeBytes }
        val totalSize = moviesSize + showsSize

        val biggestFiles = entries.sortedByDescending { it.video.sizeBytes }.take(10)

        val stat = try {
            val statFs = StatFs("/storage/emulated/0")
            Pair(statFs.availableBytes, statFs.totalBytes)
        } catch (_: Exception) {
            try {
                val statFs = StatFs("/storage/emulated/0/Movies")
                Pair(statFs.availableBytes, statFs.totalBytes)
            } catch (_: Exception) {
                Pair(0L, 0L)
            }
        }

        return StorageStats(
            totalMoviesSize = moviesSize,
            totalShowsSize = showsSize,
            totalSize = totalSize,
            availableSpace = stat.first,
            totalSpace = stat.second,
            biggestFiles = biggestFiles,
            moviesCount = movies.size,
            showsCount = shows.size
        )
    }

    fun formatSize(bytes: Long): String {
        return when {
            bytes >= 1024L * 1024 * 1024 -> String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024))
            bytes >= 1024L * 1024 -> String.format("%.1f MB", bytes / (1024.0 * 1024))
            bytes >= 1024L -> String.format("%.1f KB", bytes / 1024.0)
            else -> "$bytes B"
        }
    }
}
