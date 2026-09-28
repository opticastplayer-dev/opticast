package com.opticast.player.ui.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import android.content.Context

/**
 * Accessibility improvements for better app experience
 * - TalkBack support for posters
 * - Content descriptions
 * - Keyboard navigation
 */
object AccessibilityHelper {
    
    fun posterContentDescription(
        title: String,
        year: String?,
        isWatched: Boolean,
        isNew: Boolean
    ): String {
        val parts = mutableListOf<String>()
        parts.add(title)
        year?.let { parts.add("Year $it") }
        if (isWatched) parts.add("Watched")
        if (isNew) parts.add("New")
        return parts.joinToString(", ")
    }
    
    fun gridContentDescription(
        count: Int,
        filter: String?
    ): String {
        return if (filter != null) {
            "$count items, filtered by $filter"
        } else {
            "$count items in library"
        }
    }
}

/**
 * Modifier for accessible poster cards
 */
fun Modifier.accessiblePoster(
    title: String,
    year: String? = null,
    isWatched: Boolean = false,
    isNew: Boolean = false
): Modifier {
    return this.semantics {
        contentDescription = AccessibilityHelper.posterContentDescription(
            title = title,
            year = year,
            isWatched = isWatched,
            isNew = isNew
        )
    }
}
