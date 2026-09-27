package com.opticast.player.data.model

import android.content.Context
import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * A network location OptiCast can browse and stream from.
 *
 * Credentials are stored on the device's private app storage only - the same
 * place the API keys already live. They are never sent anywhere except the
 * server the user entered.
 */
@Serializable
data class NetworkSource(
    val id: String,
    val label: String,
    /** smb | webdav | http */
    val type: String,
    val host: String,
    val port: Int = 0,
    /** SMB share name, or the starting path for webdav/http. */
    val path: String = "",
    val username: String = "",
    val password: String = "",
    val domain: String = "",
    /** For SMB: the share to mount. Kept separately from the browse path. */
    val share: String = "",
    val useTls: Boolean = false,
) {
    val typeLabel: String
        get() = when (type) {
            "smb" -> "SMB / Windows share"
            "webdav" -> "WebDAV"
            else -> "HTTP folder"
        }

    /** The URL the player and downloader use for [filePath] inside this source. */
    fun uriFor(filePath: String): String = when (type) {
        "smb" -> "smb://$id/${filePath.trimStart('/')}"
        else -> {
            val scheme = if (useTls || port == 443) "https" else "http"
            val cleanHost = host.trim().removePrefix("[").removeSuffix("]")
            val urlHost = if (cleanHost.contains(':')) "[$cleanHost]" else cleanHost
            val defaultPort = if (scheme == "https") 443 else 80
            val base = "${scheme}://$urlHost" + if (port > 0 && port != defaultPort) ":$port" else ""
            val root = path.trim('/').let { if (it.isEmpty()) "" else "/$it" }
            "$base$root/${filePath.trimStart('/')}"
        }
    }
}

/**
 * File-backed list of network sources, mirroring the other small stores in the
 * app so the UI can observe changes the same way.
 */
class NetworkSourceStore(context: Context) {

    private val file = File(context.filesDir, "network_sources.json")
    private val json = Json { ignoreUnknownKeys = true }

    private val _sources = MutableStateFlow<List<NetworkSource>>(emptyList())
    val sources: StateFlow<List<NetworkSource>> = _sources

    init {
        runCatching {
            if (file.exists()) {
                _sources.value = json.decodeFromString<List<NetworkSource>>(file.readText())
            }
        }
    }

    fun find(id: String): NetworkSource? = _sources.value.firstOrNull { it.id == id }

    fun save(source: NetworkSource) {
        val next = _sources.value.toMutableList()
        val index = next.indexOfFirst { it.id == source.id }
        if (index >= 0) next[index] = source else next += source
        persist(next)
    }

    fun remove(id: String) = persist(_sources.value.filterNot { it.id == id })

    private fun persist(list: List<NetworkSource>) {
        _sources.value = list
        runCatching { file.writeText(json.encodeToString(list)) }
    }
}
