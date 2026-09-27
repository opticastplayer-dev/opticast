package com.opticast.player.player

import android.content.Context
import android.net.Uri
import androidx.media3.common.C
import androidx.media3.datasource.BaseDataSource
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import com.opticast.player.data.model.NetworkSourceStore
import com.opticast.player.data.remote.RemoteAccessException
import com.opticast.player.data.remote.RemoteFileSystem
import com.opticast.player.data.remote.SmbStream
import java.io.File
import java.io.IOException
import java.io.OutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Credentials
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * Media3 data source that teaches the player about network libraries.
 *
 *  - `smb://<sourceId>/<path>` is read straight off the share with random access,
 *    so seeking works exactly like a local file.
 *  - `http(s)://` URLs belonging to a saved WebDAV/HTTP source get their stored
 *    credentials attached automatically, so streams play without embedding
 *    passwords in the URL.
 *  - everything else (`content://`, `file://`, …) is handed to ExoPlayer's own
 *    DefaultDataSource untouched, so local playback is byte-for-byte the same
 *    code path as before this feature existed.
 *
 * Fixed 2026-09-23: local `content://` / `file://` URIs now get a **fresh**
 * DefaultDataSource per open (was a single lazy instance that could stay in
 * a failed state after one FileNotFound and then fail every later file with
 * "Source error"). Also fixes isNetwork handling and proper transfer lifecycle.
 */
class RemoteDataSource(
    private val context: Context,
    private val store: NetworkSourceStore,
) : BaseDataSource(/* isNetwork = */ true) {

    // Local playback: create a fresh DefaultDataSource per open so a failed
    // open never poisons the next file. The previous lazy singleton is the
    // root cause of "every local file shows Source error" after the mpv strip.
    private val resources = PlaybackResources()
    private var localDelegate: DataSource? = null
    private var httpSource: DefaultHttpDataSource? = null
    private var smb: SmbStream? = null
    private var currentUri: Uri? = null

    private var readPosition = 0L
    private var bytesRemaining = 0L
    private var usingDelegate = false
    private var usingHttp = false
    private var transferStarted = false

    override fun open(dataSpec: DataSpec): Long {
        close()
        transferInitializing(dataSpec)
        currentUri=dataSpec.uri
        readPosition=dataSpec.position
        bytesRemaining=dataSpec.length
        try {
            val openedLength: Long
            when(dataSpec.uri.scheme?.lowercase()) {
                "smb" -> {
                    val source=store.find(dataSpec.uri.authority ?: throw IOException("Malformed network link"))
                        ?: throw IOException("That network library was removed")
                    val stream=resources.own(RemoteFileSystem.openSmbStream(source,dataSpec.uri.path?.trimStart('/').orEmpty())) { it.close() }
                    smb=stream
                    bytesRemaining=smbReadLength(stream.length,dataSpec.position,dataSpec.length)
                    openedLength=bytesRemaining
                }
                "http", "https" -> {
                    val source=playbackCredentialSource(store.sources.value,dataSpec.uri.scheme.orEmpty(),dataSpec.uri.host,dataSpec.uri.port,dataSpec.uri.path.orEmpty())
                    val http=resources.own(DefaultHttpDataSource.Factory()
                        .setUserAgent("OptiCast").setConnectTimeoutMs(20_000).setReadTimeoutMs(30_000)
                        .setAllowCrossProtocolRedirects(source==null)
                        .apply { if(source!=null) setDefaultRequestProperties(mapOf("Authorization" to Credentials.basic(source.username,source.password))) }
                        .createDataSource()) { it.close() }
                    httpSource=http;usingHttp=true
                    openedLength=http.open(dataSpec)
                    bytesRemaining=openedLength
                }
                else -> {
                    val delegate=resources.own(DefaultDataSource.Factory(context.applicationContext).createDataSource()) { it.close() }
                    localDelegate=delegate;usingDelegate=true
                    openedLength=delegate.open(dataSpec)
                    bytesRemaining=openedLength
                }
            }
            // Only successful opens have a start/end pair. Failed opens still release handles.
            transferStarted(dataSpec)
            transferStarted=true
            return openedLength
        } catch(error: Exception) {
            close()
            if(error is IOException) throw error
            throw IOException("Could not open playback source",error)
        }
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        if (length == 0) return 0
        val read = when {
            usingDelegate -> localDelegate?.read(buffer, offset, length) ?: C.RESULT_END_OF_INPUT
            usingHttp -> httpSource?.read(buffer, offset, length) ?: C.RESULT_END_OF_INPUT
            else -> {
                val stream = smb ?: return C.RESULT_END_OF_INPUT
                val allowed = if (bytesRemaining == C.LENGTH_UNSET.toLong()) {
                    length
                } else {
                    minOf(length.toLong(), bytesRemaining).toInt()
                }
                if (allowed <= 0) return C.RESULT_END_OF_INPUT
                stream.read(readPosition, buffer, offset, allowed).also { checkSmbRead(it,bytesRemaining) }
            }
        }
        if (read > 0) {
            readPosition += read
            if (bytesRemaining != C.LENGTH_UNSET.toLong()) bytesRemaining -= read
            bytesTransferred(read)
        }
        return read
    }

    override fun getUri(): Uri? = currentUri

    override fun getResponseHeaders(): Map<String, List<String>> =
        if (usingHttp) httpSource?.responseHeaders ?: emptyMap() else emptyMap()

    override fun close() {
        resources.close()
        smb=null;localDelegate=null;httpSource=null;currentUri=null
        usingDelegate=false;usingHttp=false;bytesRemaining=0L;readPosition=0L
        if(transferStarted) { transferStarted=false;transferEnded() }
    }

    class Factory(
        private val context: Context,
        private val store: NetworkSourceStore,
    ) : DataSource.Factory {
        override fun createDataSource(): DataSource = StartupTracingDataSource(RemoteDataSource(context.applicationContext, store),ResumeProbes.current)
    }
}

/**
 * "Download for offline": pulls a file from a network library into app storage
 * so it can be watched with no network at all - the whole point of the feature
 * in a place where mobile data is expensive.
 */
object RemoteDownloader {

    private val http: OkHttpClient by lazy { OkHttpClient() }

    suspend fun download(
        context: Context,
        source: com.opticast.player.data.model.NetworkSource,
        remotePath: String,
        fileName: String,
        onProgress: (Float) -> Unit,
    ): File = withContext(Dispatchers.IO) {
        val dir = File(context.filesDir, "network").apply { mkdirs() }
        val target = File(dir, fileName.replace(Regex("[/\\\\:*?\"<>|]"), "_"))
        val temp = File(dir, "${target.name}.part")

        try {
            when (source.type) {
                "smb" -> {
                    val stream = RemoteFileSystem.openSmbStream(source, remotePath)
                    val total = stream.length.coerceAtLeast(1L)
                    var written = 0L
                    temp.outputStream().use { out ->
                        val buffer = ByteArray(256 * 1024)
                        while (true) {
                            val read = stream.read(written, buffer, 0, buffer.size)
                            if (read <= 0) break
                            out.write(buffer, 0, read)
                            written += read
                            onProgress((written.toFloat() / total).coerceIn(0f, 1f))
                        }
                    }
                    stream.close()
                }

                else -> {
                    val request = Request.Builder().url(source.uriFor(remotePath))
                        .apply {
                            if (source.username.isNotBlank()) {
                                header(
                                    "Authorization",
                                    Credentials.basic(source.username, source.password),
                                )
                            }
                        }
                        .build()
                    http.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) {
                            throw RemoteAccessException("Server replied ${response.code}")
                        }
                        val body = response.body
                            ?: throw RemoteAccessException("Empty response")
                        val total = body.contentLength().coerceAtLeast(1L)
                        var written = 0L
                        body.byteStream().use { input ->
                            temp.outputStream().use { out: OutputStream ->
                                val buffer = ByteArray(256 * 1024)
                                while (true) {
                                    val read = input.read(buffer)
                                    if (read <= 0) break
                                    out.write(buffer, 0, read)
                                    written += read
                                    onProgress((written.toFloat() / total).coerceIn(0f, 1f))
                                }
                            }
                        }
                    }
                }
            }
        } catch (error: Throwable) {
            temp.delete()
            throw error
        }

        temp.renameTo(target)
        onProgress(1f)
        target
    }

    /** Files downloaded so far, newest first. */
    fun downloaded(context: Context): List<File> =
        File(context.filesDir, "network")
            .listFiles { f -> f.isFile && !f.name.endsWith(".part") }
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList()

    fun delete(file: File): Boolean = runCatching { file.delete() }.getOrDefault(false)

    fun totalSizeBytes(context: Context): Long =
        downloaded(context).sumOf { it.length() }
}
