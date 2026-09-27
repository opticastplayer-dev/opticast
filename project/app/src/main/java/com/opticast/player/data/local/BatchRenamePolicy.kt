package com.opticast.player.data.local

import kotlinx.serialization.Serializable
import java.util.Locale

@Serializable
internal data class BatchRenameItem(val id: Long, val original: String, val proposed: String, val folder: String, val uri: String = "")

internal fun batchRenameItems(suggestions: List<RenameSuggestion>): List<BatchRenameItem> = suggestions.mapNotNull { s ->
    val name = s.proposedName ?: return@mapNotNull null
    if (name == s.entry.video.name || renameValidationError(s.entry.video.name, name) != null) return@mapNotNull null
    BatchRenameItem(s.entry.video.id, s.entry.video.name, name, s.entry.video.relativePath)
}.distinctBy { it.id }

/** Reject every member of a conflicting destination group, not just the last one. */
internal fun batchRenameConflicts(items: List<BatchRenameItem>): Set<Long> = items
    .groupBy { it.folder.trimEnd('/').lowercase(Locale.ROOT) + "/" + it.proposed.lowercase(Locale.ROOT) }
    .values.filter { it.size > 1 }.flatten().map { it.id }.toSet()
