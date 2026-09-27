package com.opticast.player.ui.screens

/** Shared carousel geometry: a wide resume tile occupies two poster columns. */
internal const val DiscoveryGutterDp = 20
internal const val DiscoveryPosterDp = 118
internal const val DiscoveryGapDp = 10
internal const val DiscoveryResumeDp = DiscoveryPosterDp * 2 + DiscoveryGapDp
internal const val DiscoveryCollectionDp = DiscoveryPosterDp + 24
internal const val FeaturedPickLimit = 6

/** Opacity changes only: outer card bounds never shrink during a transition. */
internal fun featuredPageOpacity(offset: Float): Float =
    1f - 0.25f * (if (offset.isFinite()) kotlin.math.abs(offset).coerceIn(0f, 1f) else 0f)

internal fun discoveryHeading(title: String): String = title.uppercase(java.util.Locale.ROOT)
