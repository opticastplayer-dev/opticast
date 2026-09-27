package com.opticast.player.ui.components

/** Fixed palettes keep status colours recognisable across light/dark app themes. */
enum class PosterBadge(val label: String, val gradientStart: Long, val gradientEnd: Long) {
    NEW("NEW", 0xFFB0F8FF, 0xFF64B9FF),
    CONTINUE("CONTINUE\nWATCHING", 0xFFFFEEB0, 0xFFFFB648),
    WATCHED("WATCHED", 0xFFB5FFE0, 0xFF48D79D)
}

/** A single top ribbon: playback status takes priority over recency. */
internal fun primaryPosterBadge(
    newlyAdded: Boolean,
    completelyWatched: Boolean,
    resumable: Boolean = false,
): PosterBadge? = when {
    completelyWatched -> PosterBadge.WATCHED
    resumable -> PosterBadge.CONTINUE
    newlyAdded -> PosterBadge.NEW
    else -> null
}

internal fun badgeShouldAnimate(status: PosterBadge, motionAllowed: Boolean): Boolean =
    status == PosterBadge.NEW && motionAllowed

/** Insets the whole badge rectangle beyond the rounded corner's diagonal, plus 2dp clearance. */
internal fun posterBadgeInsetDp(cornerRadiusDp: Float): Float =
    kotlin.math.ceil(cornerRadiusDp.coerceAtLeast(0f) * (1f - 1f / kotlin.math.sqrt(2f)) + 2f).coerceAtLeast(4f)
