package com.opticast.player.data.local

import com.opticast.player.data.model.LocalVideo
import kotlinx.serialization.Serializable

/** Stable library id lives in video.id; MediaStore ids can change after a move. */
@Serializable
internal data class TrackedFile(
    val video: LocalVideo,
    val signature: String? = null,
    val available: Boolean = true,
    val dismissed: Boolean = false,
)

internal data class ObservedFile(val video: LocalVideo, val signature: String?)

internal fun unchangedFileRow(old: LocalVideo, fresh: LocalVideo): Boolean =
    old.uri == fresh.uri && old.sizeBytes == fresh.sizeBytes && old.durationMs == fresh.durationMs &&
        old.name == fresh.name && old.dateAddedSec == fresh.dateAddedSec && old.modifiedSec == fresh.modifiedSec

internal fun sameFileContent(old: TrackedFile, fresh: ObservedFile): Boolean =
    old.signature != null && old.signature == fresh.signature && old.video.sizeBytes > 0 &&
        old.video.sizeBytes == fresh.video.sizeBytes && old.video.durationMs > 0 &&
        old.video.durationMs == fresh.video.durationMs

/** Pure, one-to-one reconciliation. Names alone never join two playback histories. */
internal fun reconcileFiles(previous: List<TrackedFile>, observed: List<ObservedFile>): List<TrackedFile> {
    require(previous.map { it.video.id }.distinct().size == previous.size)
    require(observed.map { it.video.uri }.distinct().size == observed.size)
    val assigned = mutableMapOf<Int, TrackedFile>()
    val used = mutableSetOf<Long>()
    // Retain the stable identity when the MediaStore row is still the same content.
    observed.forEachIndexed { index, fresh ->
        val atUri = previous.filter { it.video.uri == fresh.video.uri }
        val old = atUri.singleOrNull { it.available } ?: atUri.singleOrNull()
        if (old != null && (sameFileContent(old, fresh) ||
            (old.signature == null && unchangedFileRow(old.video, fresh.video)))) {
            assigned[index] = old.copy(video = fresh.video.copy(id = old.video.id),
                signature = fresh.signature ?: old.signature, available = true, dismissed = false)
            used += old.video.id
        }
    }
    // Match only a unique source AND unique destination, including present copies.
    observed.forEachIndexed { index, fresh ->
        if (index in assigned) return@forEachIndexed
        val candidates = previous.filter { sameFileContent(it, fresh) }
        val destinations = observed.count { other -> candidates.any { sameFileContent(it, other) } }
        val old = candidates.singleOrNull()
        if (old != null && old.video.id !in used && destinations == 1) {
            assigned[index] = old.copy(video = fresh.video.copy(id = old.video.id),
                signature = fresh.signature, available = true, dismissed = false)
            used += old.video.id
        }
    }
    val reserved = previous.map { it.video.id }.toMutableSet()
    var synthetic = (1L shl 60)
    observed.forEachIndexed { index, fresh ->
        if (index !in assigned) {
            val id = if (fresh.video.id > 0 && fresh.video.id !in reserved) fresh.video.id else {
                while (synthetic in reserved || observed.any { it.video.id == synthetic }) synthetic++
                synthetic++
                synthetic - 1
            }
            reserved += id
            assigned[index] = TrackedFile(fresh.video.copy(id = id), fresh.signature)
        }
    }
    return assigned.toSortedMap().values.toList() + previous.filter { it.video.id !in used }
        .map { it.copy(available = false) }
}
