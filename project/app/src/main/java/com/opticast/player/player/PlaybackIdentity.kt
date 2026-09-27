package com.opticast.player.player

/** Route identity alone is never evidence that a controller is showing that item. */
internal fun samePlaybackItem(expectedId: Long, actualMediaId: String?): Boolean =
    expectedId != 0L && actualMediaId?.toLongOrNull() == expectedId

/** Save the outgoing PositionInfo only under its embedded identity. */
internal fun shouldSaveOutgoing(oldId: Long?, newId: Long?, positionMs: Long): Boolean =
    oldId != null && oldId != 0L && oldId != newId && positionMs > 0L
