package com.opticast.player.player

internal fun reusePipPlayer(hasOwner: Boolean, inPip: Boolean, finishing: Boolean, destroyed: Boolean): Boolean =
    hasOwner && inPip && !finishing && !destroyed

internal fun validPlaybackRequest(id: Long, uri: String?): Boolean = id > 0L || !uri.isNullOrBlank()

internal fun samePlaybackRequest(oldId: Long, oldUri: String?, newId: Long, newUri: String?): Boolean = when {
    oldId > 0L || newId > 0L -> oldId > 0L && newId > 0L && oldId == newId
    else -> !oldUri.isNullOrBlank() && oldUri == newUri
}

/** null preserves ordinary Back; network-only items have no local details page. */
internal fun expandedPipBackRoute(usedPip: Boolean, videoId: Long): String? = when {
    !usedPip -> null
    videoId > 0L -> "detail/$videoId"
    else -> "library"
}

internal fun shouldOpenReturnedDetails(requestedId: Long, currentDetailId: Long?): Boolean =
    requestedId > 0L && requestedId != currentDetailId
