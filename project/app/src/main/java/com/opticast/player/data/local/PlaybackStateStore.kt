package com.opticast.player.data.local

import android.content.Context
import java.io.File
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** Saved playback position for a video. */
@Serializable
data class PlaybackState(
    val positionMs: Long = 0,
    val durationMs: Long = 0,
    val updatedAt: Long = 0,
) {
    val progress: Float
        get() = if (durationMs > 0) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f

    /** Completion, not an arbitrary 92% threshold, is the end of a resume cue. */
    val isWatched: Boolean
        get() = durationMs > 0 && positionMs >= durationMs

    val isResumable: Boolean
        get() = positionMs > 0 && (durationMs <= 0 || positionMs < durationMs)

    val remainingMs: Long
        get() = (durationMs - positionMs).coerceAtLeast(0)
}

/** In-memory resume state with serial off-main AtomicFile checkpoints. */
class PlaybackStateStore(context: Context) {

    private val json = Json { ignoreUnknownKeys = true }
    private val file = File(context.filesDir, "playback_state.json")
    private val atomicFile = android.util.AtomicFile(file)
    private val cache = mutableMapOf<String, PlaybackState>()
    @Volatile internal var persistenceFailed = false
        private set
    private val persistence = LatestSnapshotWriter<Map<String, PlaybackState>>(
        executor = persistenceExecutor,
        write = { snapshot -> persistSnapshot(snapshot) },
        onResult = { success ->
            if (!success && !persistenceFailed) {
                android.util.Log.w("OptiCastResume", "Playback progress could not be committed; a later checkpoint will retry")
            }
            persistenceFailed = !success
        },
    )

    private val _version = MutableStateFlow(0)

    /** Bumps whenever a position changes so the UI can refresh. */
    val version: StateFlow<Int> = _version

    /**
     * Compose-observable progress, updated per video id.
     *
     * Cards read this instead of watching a global version counter: writing one
     * key only invalidates the card showing that video, so the periodic 2 s
     * position saves during playback no longer recompose the whole library.
     */
    private val progressStates = androidx.compose.runtime.mutableStateMapOf<Long, PlaybackState>()

    /** Increments on every save - a cheap key for list derivations. */
    var progressTick by androidx.compose.runtime.mutableIntStateOf(0)
        private set

    /** Snapshot-aware read for composables. */
    fun progressOf(videoId: Long): PlaybackState? = progressStates[videoId]

    init {
        runCatching {
            if (file.exists()) {
                json.decodeFromString<Map<String, PlaybackState>>(atomicFile.openRead().bufferedReader().use { it.readText() })
                    .forEach { (key, value) ->
                        cache[key] = value
                        key.toLongOrNull()?.let { id -> progressStates[id] = value }
                    }
            }
        }
    }

    fun state(videoId: Long): PlaybackState? =
        synchronized(cache) { cache[videoId.toString()] }

    fun save(videoId: Long, positionMs: Long, durationMs: Long,
             capturedAtMs: Long = System.currentTimeMillis(), allowReset: Boolean = false) {
        if (videoId == 0L) return
        synchronized(cache) {
            val old = cache[videoId.toString()]
            val state = mergePlaybackSnapshot(old, positionMs, durationMs, capturedAtMs, allowReset)
            if (state == old) return
            cache[videoId.toString()] = state
            enqueueCheckpointLocked()
            progressStates[videoId] = state
            progressTick++
            _version.value++
        }
    }

    fun restart(videoId: Long, durationMs: Long = 0L) {
        save(videoId, 0L, durationMs, allowReset = true)
    }

    /** Caller holds cache monitor; copying state is bounded by history, never by write backlog. */
    private fun enqueueCheckpointLocked() {
        persistence.submit(cache.toMap())
    }

    /** Only the process-owned serial worker calls this; it never holds the cache monitor. */
    private fun persistSnapshot(snapshot: Map<String, PlaybackState>) {
        var output: java.io.FileOutputStream? = null
        try {
            // Serialization is off main too. Preserve the old file if encoding fails.
            val bytes = json.encodeToString(snapshot).toByteArray(Charsets.UTF_8)
            output = atomicFile.startWrite()
            output.write(bytes)
            atomicFile.finishWrite(output)
        } catch (error: Exception) {
            atomicFile.failWrite(output)
            throw error
        }
    }

    /** Everything saved, for the backup file. */
    fun snapshot(): Map<String, List<Long>> = synchronized(cache) {
        cache.mapValues { (_, state) ->
            listOf(state.positionMs, state.durationMs, state.updatedAt)
        }
    }

    /** Bulk restore from a backup file. */
    fun restore(states: Map<String, PlaybackState>) {
        synchronized(cache) {
            states.forEach { (key, state) -> cache[key] = state }
            enqueueCheckpointLocked()
        }
        states.forEach { (key, state) ->
            key.toLongOrNull()?.let { id -> progressStates[id] = state }
        }
        progressTick++
        _version.value++
    }

    /** Manually marks a video watched / unwatched. */
    fun setWatched(videoId: Long, watched: Boolean, durationMs: Long) {
        if (durationMs <= 0) return
        save(videoId, if (watched) durationMs else 0L, durationMs, allowReset = true)
    }
    companion object {
        // Process-owned: service/activity destruction must not cancel pending close checkpoints.
        // This executor is deliberately never shut down by a playback lifecycle callback.
        private val persistenceExecutor = java.util.concurrent.Executors.newSingleThreadExecutor { task ->
            Thread(task, "OptiCast-resume-io").apply { isDaemon = true }
        }
    }
}


/** Zero reported during teardown is not a user request to discard a resume cue. */
internal fun mergePlaybackSnapshot(old: PlaybackState?, positionMs: Long, durationMs: Long,
    capturedAtMs: Long, allowReset: Boolean = false): PlaybackState {
    if (old != null && capturedAtMs < old.updatedAt) return old
    if (positionMs <= 0L && !allowReset && old != null) return old
    val duration = durationMs.takeIf { it > 0L } ?: old?.durationMs ?: 0L
    val position = positionMs.coerceAtLeast(0L).let { if (duration > 0L) it.coerceAtMost(duration) else it }
    return PlaybackState(position, duration, capturedAtMs)
}
