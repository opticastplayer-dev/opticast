package com.opticast.player.data.remote

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import com.opticast.player.data.model.NetworkSource
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import okhttp3.Credentials
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume

/** Discovery is opt-in, local mDNS only; never a subnet/port sweep. */
class NetworkLibraryTools(context: Context) {
    private val nsd = context.applicationContext.getSystemService(Context.NSD_SERVICE) as NsdManager
    private val client = OkHttpClient.Builder().connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS).callTimeout(8, TimeUnit.SECONDS)
        .followRedirects(false).followSslRedirects(false).build()

    suspend fun check(source: NetworkSource): String = withContext(Dispatchers.IO) {
        try {
            if (source.type == "smb") {
                Socket().use { it.connect(InetSocketAddress(source.host, source.port.takeIf { p -> p > 0 } ?: 445), 5000) }
                "Server reachable. Open the library to verify the share and login."
            } else {
                val request = Request.Builder().url(source.uriFor(""))
                    .apply {
                        if (source.type == "webdav") method("PROPFIND", ByteArray(0).toRequestBody()).header("Depth", "0")
                        else head()
                        if (source.username.isNotBlank()) header("Authorization", Credentials.basic(source.username, source.password))
                    }.build()
                client.newCall(request).execute().use {
                    when {
                        it.isSuccessful -> "Server responded successfully. Open to browse."
                        it.code == 401 || it.code == 403 -> "Server reachable; access denied. Check credentials."
                        it.code in 300..399 -> "Server redirects this address. Review the configured URL."
                        it.code == 405 -> "Server reachable; quick check unsupported. Try opening the library."
                        else -> "Server replied HTTP ${it.code}. Check the configured path."
                    }
                }
            }
        } catch (e: CancellationException) { throw e }
        catch (_: Exception) { "Could not connect. Check Wi-Fi, address, port and server availability." }
    }

    @Suppress("DEPRECATION")
    suspend fun discover(onFound: (NetworkSource) -> Unit, onStatus: (String) -> Unit) = withContext(Dispatchers.Main) {
        coroutineScope {
            val queue = Channel<NsdServiceInfo>(32)
            val listeners = mutableListOf<NsdManager.DiscoveryListener>()
            val seen = mutableSetOf<String>()
            val resolver = launch {
                for (info in queue) {
                    val resolved = withTimeoutOrNull(2500) {
                        suspendCancellableCoroutine<NsdServiceInfo?> { continuation ->
                            val listener = object : NsdManager.ResolveListener {
                                override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                                    if (continuation.isActive) continuation.resume(null)
                                }
                                override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                                    if (continuation.isActive) continuation.resume(serviceInfo)
                                }
                            }
                            try { nsd.resolveService(info, listener) }
                            catch (_: Exception) { if (continuation.isActive) continuation.resume(null) }
                        }
                    } ?: continue
                    val host = resolved.host?.hostAddress ?: continue
                    val type = discoverySourceType(resolved.serviceType) ?: continue
                    if (resolved.port !in 1..65535) continue
                    val key = "$type|$host|${resolved.port}"
                    if (!seen.add(key)) continue
                    onFound(NetworkSource(id = java.util.UUID.randomUUID().toString(),
                        label = resolved.serviceName, type = type, host = host, port = resolved.port,
                        useTls = resolved.serviceType.contains("_webdavs") || resolved.serviceType.contains("_https")))
                }
            }
            try {
                for (type in DISCOVERY_TYPES) {
                    val listener = object : NsdManager.DiscoveryListener {
                        override fun onDiscoveryStarted(serviceType: String) {}
                        override fun onDiscoveryStopped(serviceType: String) {}
                        override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                            onStatus("Some services could not be scanned. You can still add a library manually.")
                            runCatching { nsd.stopServiceDiscovery(this) }
                        }
                        override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {}
                        override fun onServiceFound(serviceInfo: NsdServiceInfo) { queue.trySend(serviceInfo) }
                        override fun onServiceLost(serviceInfo: NsdServiceInfo) {}
                    }
                    listeners += listener
                    try { nsd.discoverServices(type, NsdManager.PROTOCOL_DNS_SD, listener) }
                    catch (_: Exception) { onStatus("Discovery unavailable for some services. Check network permissions or add manually.") }
                }
                delay(12_000)
            } finally {
                listeners.forEach { runCatching { nsd.stopServiceDiscovery(it) } }
                queue.close()
                resolver.cancelAndJoin()
            }
        }
    }

    companion object {
        val DISCOVERY_TYPES = listOf("_smb._tcp.", "_webdav._tcp.", "_webdavs._tcp.", "_http._tcp.", "_https._tcp.")
    }
}

internal fun discoverySourceType(serviceType: String): String? = when {
    serviceType.startsWith("_smb._tcp") -> "smb"
    serviceType.startsWith("_webdav._tcp") || serviceType.startsWith("_webdavs._tcp") -> "webdav"
    serviceType.startsWith("_http._tcp") || serviceType.startsWith("_https._tcp") -> "http"
    else -> null
}
