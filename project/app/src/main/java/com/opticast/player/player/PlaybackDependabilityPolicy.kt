package com.opticast.player.player

internal const val PLAYBACK_NO_PROGRESS_MS = 30_000L
internal fun currentPlaybackRequest(expected: Any, current: Any): Boolean = expected === current

/** Null preserves the engine's default/live start rather than forcing a zero seek. */
internal fun playbackStartPosition(sameItem: Boolean, currentPosition: Long, savedPosition: Long?): Long? =
    if(sameItem) currentPosition.coerceAtLeast(0) else savedPosition?.takeIf { it>0 }

/** UI warning only. Never stops playback or retries; caller scopes one instance per request/retry. */
internal class PlaybackProgressWatch(private val timeoutMs: Long = PLAYBACK_NO_PROGRESS_MS) {
    private var lastProgress: Long? = null
    private var lastPosition = 0L
    private var lastBuffered = 0L
    private var warned = false
    fun observe(nowMs: Long, waiting: Boolean, positionMs: Long, bufferedMs: Long): Boolean {
        if(!waiting) { lastProgress=null; warned=false; return false }
        val previous=lastProgress
        if(previous==null || nowMs<previous || positionMs!=lastPosition || bufferedMs>lastBuffered) {
            lastProgress=nowMs
            warned=false
        }
        lastPosition=positionMs
        lastBuffered=bufferedMs
        if(!warned && nowMs-(lastProgress ?: nowMs)>=timeoutMs) { warned=true; return true }
        return false
    }
}

internal data class BufferingHealth(val episodes: Int = 0, val elapsedMs: Long = 0, val sinceMs: Long? = null) {
    fun observe(buffering: Boolean, nowMs: Long): BufferingHealth = when {
        buffering && sinceMs==null -> copy(episodes=episodes+1,sinceMs=nowMs)
        !buffering && sinceMs!=null -> copy(elapsedMs=totalMs(nowMs),sinceMs=null)
        else -> this
    }
    fun totalMs(nowMs: Long): Long = elapsedMs + (sinceMs?.let { (nowMs-it).coerceAtLeast(0) } ?: 0)
}
