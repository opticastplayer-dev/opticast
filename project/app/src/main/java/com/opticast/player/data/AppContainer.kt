package com.opticast.player.data

import android.app.Application
import com.opticast.player.data.local.FolderExclusions
import com.opticast.player.data.local.FavoritesStore
import com.opticast.player.data.local.DetailCache
import com.opticast.player.data.local.MediaScanner
import com.opticast.player.data.local.MetadataStore
import com.opticast.player.data.local.PlaybackStateStore
import com.opticast.player.data.local.PosterCache
import com.opticast.player.data.remote.AniListApi
import com.opticast.player.data.local.ChapterIndexer
import com.opticast.player.data.local.FrameArtwork
import com.opticast.player.data.model.NetworkSourceStore
import com.opticast.player.data.local.ThumbnailCache
import com.opticast.player.data.remote.OpenSubtitlesApi
import com.opticast.player.data.remote.SubDlApi
import com.opticast.player.data.remote.SubtitleSources
import com.opticast.player.data.remote.TmdbApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

/** Manual dependency container, optimised for fast startup */
object AppContainer {

    private lateinit var application: Application

    @Volatile
    var lowRamMode: Boolean = false
        private set

    @Volatile
    var memoryClassMb: Int = 256
        private set

    @Volatile
    var dataSaver: Boolean = true
        private set

    fun setDataSaver(enabled: Boolean) { dataSaver = enabled }

    fun isMeteredNetwork(): Boolean = runCatching {
        val cm = application.getSystemService(android.content.Context.CONNECTIVITY_SERVICE)
            as? android.net.ConnectivityManager
        cm?.isActiveNetworkMetered ?: false
    }.getOrDefault(false)

    fun setLowRamMode(enabled: Boolean) { lowRamMode = enabled }
    fun setMemoryClass(mb: Int) { memoryClassMb = mb.coerceIn(64, 1024) }

    // Adaptive budgets using memoryClass
    fun adaptiveImageMemoryBudget(): Int = com.opticast.player.data.imageMemoryBudgetBytes(lowRamMode, memoryClassMb)
    fun adaptiveImageDiskBudget(): Long = com.opticast.player.data.imageDiskBudgetBytes(lowRamMode, memoryClassMb)
    fun adaptivePosterConcurrency(): Int = com.opticast.player.data.posterDownloadConcurrency(lowRamMode, memoryClassMb)

    // Default settings to avoid blocking main thread - will be replaced async
    @Volatile
    var initialSettings: AppSettings = AppSettings()
        private set

    internal fun updateSettingsSnapshot(value: AppSettings) { initialSettings = value }

    private val initScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun init(app: Application) {
        application = app
        // Performance: avoid runBlocking on main thread - load settings async with fast default
        initScope.launch {
            try {
                val loaded = settings.current()
                initialSettings = loaded
                FolderExclusions.hydrate(loaded.excludedFolders)
                dataSaver = loaded.dataSaverArtwork
            } catch (_: Exception) {
            }
        }
        // For first frame, try fast non-blocking read if DataStore file already exists
        runCatching {
            val prefsFile = app.filesDir.resolve("datastore/settings.preferences_pb")
            if (prefsFile.exists() && prefsFile.length() > 0) {
                runBlocking(Dispatchers.IO) {
                    initialSettings = settings.current()
                    FolderExclusions.hydrate(initialSettings.excludedFolders)
                    dataSaver = initialSettings.dataSaverArtwork
                }
            }
        }
    }

    val settings: SettingsRepository by lazy { SettingsRepository(application) }
    val metadataStore: MetadataStore by lazy { MetadataStore(application) }
    val playbackState: PlaybackStateStore by lazy { PlaybackStateStore(application) }
    val mediaScanner: MediaScanner by lazy { MediaScanner(application) }
    val renameSuggestions by lazy { com.opticast.player.data.local.RenameSuggestions(application) }
    val posterCache: PosterCache by lazy { PosterCache(application) }
    val detailCache: DetailCache by lazy { DetailCache(application) }

    private val primedArtwork = java.util.Collections.newSetFromMap(
        java.util.concurrent.ConcurrentHashMap<Long, Boolean>(),
    )

    fun prefetchArtworkOnce(videoId: Long, urls: List<String?>) {
        if (primedArtwork.add(videoId)) prefetchArtwork(urls)
    }

    private val artworkScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun prefetchArtwork(urls: List<String?>) {
        artworkScope.launch { if (isOnline()) offlineArtwork.prefetch(urls) }
    }

    fun isOnline(): Boolean = runCatching {
        val cm = application.getSystemService(android.content.Context.CONNECTIVITY_SERVICE)
            as android.net.ConnectivityManager
        val caps = cm.getNetworkCapabilities(cm.activeNetwork)
        caps?.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET) == true &&
            caps.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }.getOrDefault(false)

    val offlineArtwork by lazy { com.opticast.player.data.local.OfflineArtwork(application) }
    val keyless by lazy { com.opticast.player.data.remote.KeylessMetadata() }
    val offlineLibrary by lazy { OfflineLibrary(application) }
    val favorites: FavoritesStore by lazy { FavoritesStore(application) }
    val chapters: ChapterIndexer by lazy { ChapterIndexer(application) }
    val networkSources: NetworkSourceStore by lazy { NetworkSourceStore(application) }
    val frameArtwork: FrameArtwork by lazy { FrameArtwork(application) }
    val thumbnails: ThumbnailCache by lazy { ThumbnailCache(application) }
    // SECURE PROXY: No API key in APK — key stays server-side at https://tmdb-proxy-xstu.onrender.com/
    // Old direct TMDB client kept for fallback if needed, but proxy is primary
    val tmdbProxy: com.opticast.player.data.remote.TmdbProxyService by lazy { com.opticast.player.data.remote.TmdbProxyService() }
    val tmdbAdapter: com.opticast.player.data.remote.TmdbApiProxyAdapter by lazy { com.opticast.player.data.remote.TmdbApiProxyAdapter() }
    val tmdb: com.opticast.player.data.remote.TmdbApiProxyAdapter by lazy { tmdbAdapter } // drop-in replacement, no key required
    val openSubtitles: OpenSubtitlesApi by lazy { OpenSubtitlesApi(settings, metadataStore) }
    val anilist: AniListApi by lazy { AniListApi() }
    val subDl: SubDlApi by lazy { SubDlApi(settings, metadataStore) }
    val subtitles: SubtitleSources by lazy { SubtitleSources(openSubtitles, subDl, settings) }

    // Clean - removed placeholder cloud/cast that didn't benefit end user (0 benefit)
    // Cloud Drive/SMB/WebDAV + Cast will be added later when real implementation ready, offline-first #1
    val breadcrumb: com.opticast.player.data.local.BreadcrumbTracker by lazy { com.opticast.player.data.local.BreadcrumbTracker(application) }
    val subtitleFonts: com.opticast.player.data.local.SubtitleFontManager by lazy { com.opticast.player.data.local.SubtitleFontManager(application) }
    val gaplessQueue: com.opticast.player.player.GaplessQueue by lazy { com.opticast.player.player.GaplessQueue() }
}
