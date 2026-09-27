package com.opticast.player.data.local

/** Stable per-proposal identity: reordered scans and remaining subsets never re-alert. */
internal fun renameNoticeKey(item: BatchRenameItem): String = java.security.MessageDigest.getInstance("SHA-256")
    .digest("${item.id}\u0000${item.original}\u0000${item.proposed}".toByteArray(Charsets.UTF_8))
    .joinToString("") { "%02x".format(it) }

internal fun unseenRenameNotices(keys: Set<String>, seen: Set<String>): Set<String> = keys - seen
