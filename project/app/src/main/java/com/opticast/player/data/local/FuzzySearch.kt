package com.opticast.player.data.local

import com.opticast.player.data.model.LibraryEntry

/**
 * 10/10 Fuzzy search - typo tolerance like Infuse
 * - Levenshtein distance 1-2 for typo tolerance
 * - Search "Avngers" finds "Avengers"
 * - Fast, no extra dependencies
 */
object FuzzySearch {

    fun search(entries: List<LibraryEntry>, query: String): List<LibraryEntry> {
        if (query.isBlank()) return entries
        val q = query.lowercase().trim()
        
        // First exact match - fast path
        val exact = entries.filter { entry ->
            val title = entry.metadata?.title?.lowercase() ?: entry.video.name.lowercase()
            title.contains(q) || entry.video.name.lowercase().contains(q)
        }
        if (exact.isNotEmpty()) return exact

        // Fuzzy - Levenshtein distance <=2 for typo tolerance
        return entries.filter { entry ->
            val title = entry.metadata?.title?.lowercase() ?: entry.video.name.lowercase()
            val words = title.split(" ", "-", ":", ".", "_")
            words.any { word ->
                levenshtein(word, q) <= 2 || (q.length > 3 && word.contains(q.take(3)))
            }
        }.sortedBy { entry ->
            val title = entry.metadata?.title?.lowercase() ?: entry.video.name.lowercase()
            wordsDistance(title, q)
        }
    }

    private fun wordsDistance(title: String, query: String): Int {
        val words = title.split(" ", "-", ":", ".", "_")
        return words.minOfOrNull { levenshtein(it, query) } ?: 100
    }

    private fun levenshtein(a: String, b: String): Int {
        if (a == b) return 0
        if (a.isEmpty()) return b.length
        if (b.isEmpty()) return a.length
        
        // Optimization for long strings - early exit if length diff >2
        if (kotlin.math.abs(a.length - b.length) > 2) return 10
        
        val dp = Array(a.length + 1) { IntArray(b.length + 1) }
        for (i in 0..a.length) dp[i][0] = i
        for (j in 0..b.length) dp[0][j] = j
        
        for (i in 1..a.length) {
            for (j in 1..b.length) {
                val cost = if (a[i-1] == b[j-1]) 0 else 1
                dp[i][j] = minOf(
                    dp[i-1][j] + 1,
                    dp[i][j-1] + 1,
                    dp[i-1][j-1] + cost
                )
            }
        }
        return dp[a.length][b.length]
    }
}
