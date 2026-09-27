package com.opticast.player.data.local

import java.util.concurrent.Executor

/**
 * Serial, bounded persistence for COMPLETE immutable snapshots (never per-item deltas).
 * At most one snapshot is being written and one newer snapshot is pending. The executor
 * must dispatch off the caller thread; its lifetime must not depend on a player/activity.
 * A submission updates pending state without waiting for storage. It is not a durability
 * acknowledgement: only a completed write is committed, including during process death.
 */
internal class LatestSnapshotWriter<T : Any>(
    private val executor: Executor,
    private val write: (T) -> Unit,
    private val onResult: (Boolean) -> Unit = {},
) {
    private val lock = Any()
    private var pending: T? = null
    private var running = false

    fun submit(snapshot: T) {
        val start = synchronized(lock) {
            pending = snapshot
            if (running) false else { running = true; true }
        }
        if (start) {
            try {
                executor.execute { drain() }
            } catch (_: java.util.concurrent.RejectedExecutionException) {
                // Keep the newest pending snapshot for the next submission; never spin/retry.
                synchronized(lock) { running = false }
                report(false)
            }
        }
    }

    private fun drain() {
        while (true) {
            val next = synchronized(lock) {
                val value = pending
                pending = null
                if (value == null) running = false
                value
            } ?: return
            val success = try { write(next); true } catch (_: Exception) { false }
            report(success)
            // On failure, a queued newer COMPLETE snapshot can recover all current state.
            // With no newer snapshot, wait for a future submission instead of retrying forever.
        }
    }

    private fun report(success: Boolean) {
        // A diagnostic callback must not strand the queue or stop resume persistence.
        try { onResult(success) } catch (_: Exception) { }
    }
}
