package com.opticast.player.player

import java.io.EOFException
import java.io.File
import java.io.IOException

/** Own resources before open(): even partially opened handles must be closed on failure. */
internal class PlaybackResources {
    private val closers = mutableListOf<() -> Unit>()
    fun <T> own(resource: T, close: (T) -> Unit): T {
        closers += { close(resource) }
        return resource
    }
    fun close() {
        val pending=closers.toList();closers.clear()
        pending.asReversed().forEach { runCatching { it() } }
    }
}
internal fun smbReadLength(total: Long, position: Long, requested: Long): Long {
    if(total<0 || position<0 || position>total || requested < -1) throw EOFException("Invalid network file range; check whether the file changed")
    val available=total-position
    return if(requested == -1L) available else minOf(available,requested)
}
internal fun checkSmbRead(read: Int, remaining: Long) {
    if(read==0 && remaining>0) throw IOException("Network file returned no data; retry after checking the connection")
    if(read<0 && remaining>0) throw EOFException("Network file ended early; the file or connection may have changed")
}
internal fun usableSubtitleFile(file: File): Boolean = file.isFile && file.canRead() && file.length()>0L
internal const val PLAYER_CONNECTION_TIMEOUT_MS = 15_000L

/** Match saved HTTP credentials to scheme, effective port and the most specific source root. */
internal fun playbackCredentialSource(sources: List<com.opticast.player.data.model.NetworkSource>, scheme: String, host: String?, port: Int, path: String): com.opticast.player.data.model.NetworkSource? {
    val protocol=scheme.lowercase()
    if(protocol !in setOf("http","https") || host==null) return null
    val effectivePort=port.takeIf { it>0 } ?: if(protocol=="https") 443 else 80
    return sources.filter { source ->
        val sourceProtocol=if(source.useTls || source.port==443) "https" else "http"
        val sourcePort=source.port.takeIf { it>0 } ?: if(sourceProtocol=="https") 443 else 80
        val rawRoot="/"+source.path.trim('/')
        val root=runCatching { java.net.URI("https://placeholder$rawRoot").normalize().path }.getOrDefault(rawRoot)
        source.type in setOf("http","webdav") && source.username.isNotBlank() &&
            source.host.trim().trim('[',']').equals(host.trim('[',']'),true) && sourceProtocol==protocol && sourcePort==effectivePort &&
            (root=="/" || path==root || path.startsWith("$root/"))
    }.maxByOrNull { it.path.trim('/').length }
}

/** Recovery UI must not compete with full-screen playback gesture consumers. */
internal fun playerGestureInputEnabled(locked: Boolean, hasError: Boolean, diagnosticsOpen: Boolean): Boolean =
    !locked && !hasError && !diagnosticsOpen
