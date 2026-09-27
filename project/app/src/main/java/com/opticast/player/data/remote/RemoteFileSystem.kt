package com.opticast.player.data.remote

import com.hierynomus.msdtyp.AccessMask
import com.hierynomus.mssmb2.SMB2CreateDisposition
import com.hierynomus.mssmb2.SMB2ShareAccess
import com.hierynomus.smbj.SMBClient
import com.hierynomus.smbj.auth.AuthenticationContext
import com.hierynomus.smbj.connection.Connection
import com.hierynomus.smbj.session.Session
import com.hierynomus.smbj.share.DiskShare
import com.opticast.player.data.model.NetworkSource
import java.util.EnumSet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Credentials
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.w3c.dom.Element
import java.io.ByteArrayInputStream
import java.util.concurrent.TimeUnit
import javax.xml.parsers.DocumentBuilderFactory

/** One row in a network folder. */
data class RemoteEntry(
    val name: String,
    /** Path relative to the source root, without a leading slash. */
    val path: String,
    val isDirectory: Boolean,
    val sizeBytes: Long,
    val isVideo: Boolean,
)

private val VIDEO_EXTENSIONS = setOf(
    "mkv", "mp4", "avi", "mov", "m4v", "webm", "ts", "m2ts", "mpg", "mpeg",
    "wmv", "flv", "3gp", "ogv", "divx", "rmvb", "vob", "iso",
)

fun String.looksLikeVideo(): Boolean =
    substringAfterLast('.', "").lowercase() in VIDEO_EXTENSIONS

/** Thrown with a message the UI can show verbatim. */
class RemoteAccessException(message: String) : Exception(message)

/**
 * Browses a network source. Three protocols, one shape, so the UI does not care
 * which one it is talking to:
 *
 *  - **WebDAV** (Nextcloud, Synology, most routers, Windows over IIS/WebDAV)
 *  - **HTTP(S) folder listings** (Apache/nginx autoindex, router USB pages)
 *  - **SMB/CIFS** (Windows shares, NAS, most router USB drives) via smbj
 *
 * Everything is resilient: a source that is unreachable or misconfigured throws
 * [RemoteAccessException] with a readable reason instead of leaking transport
 * exceptions into the UI.
 */
object RemoteFileSystem {

    private val http: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    suspend fun list(source: NetworkSource, path: String): List<RemoteEntry> =
        withContext(Dispatchers.IO) {
            when (source.type) {
                "smb" -> listSmb(source, path)
                "webdav" -> listWebDav(source, path)
                else -> listHttp(source, path)
            }
        }.sortedWith(
            compareByDescending<RemoteEntry> { it.isDirectory }
                .thenBy(String.CASE_INSENSITIVE_ORDER) { it.name },
        )

    // ------------------------------------------------------------------ WebDAV

    private fun listWebDav(source: NetworkSource, path: String): List<RemoteEntry> {
        val url = source.uriFor(path)
        val request = Request.Builder()
            .url(url)
            .method(
                "PROPFIND",
                """<?xml version="1.0" encoding="utf-8"?>
                   <d:propfind xmlns:d="DAV:"><d:prop>
                     <d:displayname/><d:resourcetype/><d:getcontentlength/>
                   </d:prop></d:propfind>""".trimIndent().toRequestBody("application/xml".toMediaTypeOrNull()),
            )
            .header("Depth", "1")
            .apply { if (source.username.isNotBlank()) header("Authorization", basic(source)) }
            .build()

        val body = runCatching { http.newCall(request).execute() }.getOrElse {
            throw RemoteAccessException("Could not reach ${source.host}: ${it.message ?: "network error"}")
        }.use { response ->
            if (!response.isSuccessful) {
                throw RemoteAccessException(
                    if (response.code == 401) "Wrong username or password for ${source.host}."
                    else "Server replied ${response.code}.",
                )
            }
            response.body?.string().orEmpty()
        }

        return runCatching { parseWebDav(body, source, path) }.getOrElse { emptyList() }
    }

    private fun parseWebDav(xml: String, source: NetworkSource, path: String): List<RemoteEntry> {
        if (xml.isBlank()) return emptyList()
        val factory = DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = true
        }
        val doc = factory.newDocumentBuilder()
            .parse(ByteArrayInputStream(xml.toByteArray(Charsets.UTF_8)))
        val nodes = doc.getElementsByTagNameNS("DAV:", "response")
        val out = mutableListOf<RemoteEntry>()
        val rootPath = path.trim('/')
        for (i in 0 until nodes.length) {
            val element = nodes.item(i) as? Element ?: continue
            val href = textOf(element, "DAV:", "href") ?: continue
            val decoded = java.net.URLDecoder.decode(href, "UTF-8")
            // Skip the folder itself.
            if (decoded.trimEnd('/').endsWith(rootPath)) continue
            val isDir = element.getElementsByTagNameNS("DAV:", "collection").length > 0
            val name = decoded.trimEnd('/').substringAfterLast('/')
            if (name.isBlank()) continue
            val size = textOf(element, "DAV:", "getcontentlength")?.toLongOrNull() ?: 0L
            // Belt and braces: unless we know, treat "no extension" as a folder.
            val looksDir = isDir || (!name.contains('.') && size == 0L)
            out += RemoteEntry(
                name = name,
                path = if (rootPath.isEmpty()) name else "$rootPath/$name",
                isDirectory = looksDir,
                sizeBytes = size,
                isVideo = !looksDir && name.looksLikeVideo(),
            )
        }
        return out
    }

    // -------------------------------------------------------------- HTTP listing

    private val hrefRegex = Regex("""href\s*=\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE)

    private fun listHttp(source: NetworkSource, path: String): List<RemoteEntry> {
        val url = source.uriFor(path)
        val request = Request.Builder().url(url)
            .apply { if (source.username.isNotBlank()) header("Authorization", basic(source)) }
            .build()
        val body = runCatching { http.newCall(request).execute() }.getOrElse {
            throw RemoteAccessException("Could not reach ${source.host}: ${it.message ?: "network error"}")
        }.use { response ->
            if (!response.isSuccessful) {
                throw RemoteAccessException("Server replied ${response.code}.")
            }
            response.body?.string().orEmpty()
        }
        val out = mutableListOf<RemoteEntry>()
        val rootPath = path.trim('/')
        for (match in hrefRegex.findAll(body)) {
            val raw = match.groupValues[1]
            if (raw.startsWith("?") || raw.startsWith("#") || raw.startsWith("http")) continue
            if (raw == "../" || raw == "/") continue
            val decoded = runCatching { java.net.URLDecoder.decode(raw, "UTF-8") }.getOrDefault(raw)
            val isDir = decoded.endsWith("/")
            val name = decoded.trimEnd('/').substringAfterLast('/')
            if (name.isBlank() || name.startsWith(".")) continue
            out += RemoteEntry(
                name = name,
                path = if (rootPath.isEmpty()) name else "$rootPath/$name",
                isDirectory = isDir,
                sizeBytes = 0L,
                isVideo = !isDir && name.looksLikeVideo(),
            )
        }
        return out.distinctBy { it.path }
    }

    // --------------------------------------------------------------------- SMB

    /** Live SMB connections, keyed by source id. Reused so browsing is fast. */
    private val smbConnections = mutableMapOf<String, SmbHandle>()

    private class SmbHandle(
        val client: SMBClient,
        val connection: Connection,
        val session: Session,
        val share: DiskShare,
    )

    private fun smbHandle(source: NetworkSource): SmbHandle {
        synchronized(smbConnections) {
            smbConnections[source.id]?.let { handle ->
                if (handle.connection.isConnected) return handle
                smbConnections.remove(source.id)
            }
            val shareName = source.share.ifBlank { source.path.trim('/').substringBefore('/') }
            if (shareName.isBlank()) {
                throw RemoteAccessException("No share name set for ${source.label}.")
            }
            return runCatching {
                val client = SMBClient()
                val connection = client.connect(source.host, if (source.port > 0) source.port else 445)
                val auth = if (source.username.isBlank()) {
                    AuthenticationContext.anonymous()
                } else {
                    AuthenticationContext(
                        source.username,
                        source.password.toCharArray(),
                        source.domain.ifBlank { null },
                    )
                }
                val session = connection.authenticate(auth)
                val share = session.connectShare(shareName) as? DiskShare
                    ?: throw RemoteAccessException("“$shareName” is not a shared folder.")
                SmbHandle(client, connection, session, share).also { smbConnections[source.id] = it }
            }.getOrElse { error ->
                if (error is RemoteAccessException) throw error
                throw RemoteAccessException(
                    "SMB connection failed: ${error.message ?: error.javaClass.simpleName}",
                )
            }
        }
    }

    private fun listSmb(source: NetworkSource, path: String): List<RemoteEntry> {
        val handle = smbHandle(source)
        val root = shareRoot(source)
        val relative = path.trim('/')
        val target = if (relative.isEmpty()) root else "$root/$relative"
        val entries = runCatching { handle.share.list(target) }.getOrElse {
            throw RemoteAccessException("Could not list $target: ${it.message ?: "access denied"}")
        }
        return entries.mapNotNull { info ->
            val name = info.fileName
            if (name == "." || name == ".." || name.isBlank()) return@mapNotNull null
            // 0x10 = DIRECTORY in the Windows file attribute bitfield.
            val isDir = (info.fileAttributes and 0x10L) != 0L
            RemoteEntry(
                name = name,
                path = if (relative.isEmpty()) name else "$relative/$name",
                isDirectory = isDir,
                sizeBytes = if (isDir) 0L else info.endOfFile,
                isVideo = !isDir && name.looksLikeVideo(),
            )
        }
    }

    /** Path prefix inside the share (a source may point at a subfolder). */
    private fun shareRoot(source: NetworkSource): String {
        val share = source.share.ifBlank { source.path.trim('/').substringBefore('/') }
        val rest = source.path.trim('/').removePrefix(share).trim('/')
        return rest
    }

    // ------------------------------------------------------- streaming / reading

    /** Opens a random-access reader for [filePath] inside the source. */
    fun openSmbStream(source: NetworkSource, filePath: String): SmbStream {
        val handle = smbHandle(source)
        val root = shareRoot(source)
        val target = if (root.isEmpty()) filePath else "$root/$filePath"
        val file = runCatching {
            handle.share.openFile(
                target,
                EnumSet.of(AccessMask.GENERIC_READ),
                null,
                SMB2ShareAccess.ALL,
                SMB2CreateDisposition.FILE_OPEN,
                null,
            )
        }.getOrElse {
            throw RemoteAccessException("Could not open ${filePath.substringAfterLast('/')}: ${it.message}")
        }
        val length = try { file.fileInformation.standardInformation.endOfFile }
        catch(error: Exception) {
            runCatching { file.close() }
            throw RemoteAccessException("Could not read network file length; reconnect the source and retry")
        }
        return SmbStream(file, length)
    }

    fun closeAll() {
        synchronized(smbConnections) {
            smbConnections.values.forEach { handle ->
                runCatching { handle.share.close() }
                runCatching { handle.session.close() }
                runCatching { handle.connection.close() }
            }
            smbConnections.clear()
        }
    }

    private fun basic(source: NetworkSource): String =
        Credentials.basic(source.username, source.password)

    private fun textOf(element: Element, ns: String, tag: String): String? {
        val nodes = element.getElementsByTagNameNS(ns, tag)
        if (nodes.length == 0) return null
        return nodes.item(0)?.textContent?.trim()
    }
}

/** Random-access read handle over an SMB file. */
class SmbStream(private val file: com.hierynomus.smbj.share.File, val length: Long) {
    fun read(offset: Long, buffer: ByteArray, bufferOffset: Int, count: Int): Int =
        file.read(buffer, offset, bufferOffset, count)

    fun close() {
        runCatching { file.close() }
    }
}
