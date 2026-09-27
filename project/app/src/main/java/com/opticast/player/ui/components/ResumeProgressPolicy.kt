package com.opticast.player.ui.components

import com.opticast.player.data.local.PlaybackState

/** Never invent a completion percentage for a stream with unknown duration. */
internal fun visibleResumeFraction(state: PlaybackState): Float? =
    if (state.durationMs > 0) state.progress.coerceIn(0f, 1f) else null

/** A show's bar follows the latest resumable episode, not a misleading series average. */
internal fun latestResumeProgress(states: List<PlaybackState>): PlaybackState? =
    states.filter { it.isResumable }.maxByOrNull { it.updatedAt }

internal fun toggledDiscoverySections(collapsed: Set<String>, id: String): Set<String> =
    if (id in collapsed) collapsed - id else collapsed + id
