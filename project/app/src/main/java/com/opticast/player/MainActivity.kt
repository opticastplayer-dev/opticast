package com.opticast.player

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.opticast.player.data.AppContainer
import com.opticast.player.data.AppSettings
import com.opticast.player.player.PlayerActivity
import com.opticast.player.ui.screens.DetailScreen
import com.opticast.player.ui.screens.LibraryScreen
import com.opticast.player.ui.screens.MatchScreen
import com.opticast.player.ui.screens.NetworkScreen
import com.opticast.player.ui.screens.SettingsScreen
import com.opticast.player.ui.screens.StorageScreen
import com.opticast.player.ui.screens.ShowScreen
import com.opticast.player.ui.theme.OptiCastTheme

class MainActivity : ComponentActivity() {
    private val libraryReturnRevision = androidx.compose.runtime.mutableIntStateOf(0)
    private val returnShowTitle = mutableStateOf<String?>(null)
    private val returnDetailId = androidx.compose.runtime.mutableLongStateOf(0L)
    companion object {
        const val EXTRA_SHOW_TITLE = "com.opticast.player.SHOW_TITLE"
        const val EXTRA_SHOW_LIBRARY = "com.opticast.player.SHOW_LIBRARY"
        const val EXTRA_SHOW_VIDEO_DETAILS = "com.opticast.player.SHOW_VIDEO_DETAILS"
    }
    private fun consumePlayerReturn(incoming: Intent) {
        val showTitle = incoming.getStringExtra(EXTRA_SHOW_TITLE)?.takeIf { it.isNotBlank() }?.take(500)
        val detailId = incoming.getLongExtra(EXTRA_SHOW_VIDEO_DETAILS, 0L)
        if (showTitle != null || detailId > 0L || incoming.getBooleanExtra(EXTRA_SHOW_LIBRARY, false)) {
            returnShowTitle.value = showTitle
            incoming.removeExtra(EXTRA_SHOW_TITLE)
            returnDetailId.longValue = detailId.coerceAtLeast(0L)
            libraryReturnRevision.intValue++
            incoming.removeExtra(EXTRA_SHOW_VIDEO_DETAILS)
            incoming.removeExtra(EXTRA_SHOW_LIBRARY)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        consumePlayerReturn(intent)
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        consumePlayerReturn(intent)
        enableEdgeToEdge()
        // Performance mode (Settings > Appearance) keeps the standard refresh
        // rate on devices where max refresh costs more than it gives.
        if (!AppContainer.initialSettings.performanceMode) {
            preferHighestRefreshRate()
        }
        // OptiCast is always dark — keep the status/nav bar icons light so they
        // stay readable while content scrolls under them.
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }
        // Match the very first window frame to the device palette when the
        // adaptive-colors toggle is on (Android 12+).
        if (AppContainer.initialSettings.useDeviceColors &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
        ) {
            window.setBackgroundDrawableResource(android.R.color.system_neutral1_900)
        }
        // Clear the old marker silently; active playback failure/recovery reporting is unchanged.
        com.opticast.player.player.mpv.NativeSessionJournal.consumeInterrupted(this)
        setContent {
            val settings by AppContainer.settings.settings
                .collectAsStateWithLifecycle(initialValue = AppContainer.initialSettings)
            androidx.compose.runtime.SideEffect { AppContainer.updateSettingsSnapshot(settings) }
            // Keep the artwork data-saver flag in step with the setting.
            LaunchedEffect(settings.dataSaverArtwork) {
                AppContainer.setDataSaver(settings.dataSaverArtwork)
            }
            OptiCastTheme(
                theme = settings.appTheme,
                useDeviceColors = settings.useDeviceColors,
            ) {
                OptiCastApp(libraryReturnRevision.intValue, returnDetailId.longValue, returnShowTitle.value)
                com.opticast.player.ui.screens.WhatsNewDialog()
                com.opticast.player.ui.screens.AutoUpdateDialog()
            }
        }
    }

    /**
     * Asks the display for its highest refresh rate at the current
     * resolution (90/120/144 Hz on supported devices) so scrolling and
     * animations run at full fluidity instead of being capped at 60 Hz.
     */
    private fun preferHighestRefreshRate() {
        runCatching {
            val display = if (Build.VERSION.SDK_INT >= 30) {
                display
            } else {
                @Suppress("DEPRECATION")
                windowManager.defaultDisplay
            } ?: return
            val current = display.mode ?: return
            val sameResolution = display.supportedModes.filter {
                it.physicalWidth == current.physicalWidth &&
                    it.physicalHeight == current.physicalHeight
            }
            val best = (sameResolution.ifEmpty { display.supportedModes.toList() })
                .maxByOrNull { it.refreshRate } ?: return
            if (best.refreshRate > current.refreshRate) {
                window.attributes = window.attributes.apply {
                    preferredDisplayModeId = best.modeId
                }
            }
        }
    }

}

/**
 * Local videos start with mpv, with a one-time Media3 fallback.
 * Every title opens in PlayerActivity (Media3 ExoPlayer) directly.
 */
private fun playerIntent(context: android.content.Context, videoId: Long): Intent {
    return PlayerActivity.intent(context, videoId)
}

@Composable
private fun OptiCastApp(libraryReturnRevision: Int = 0, returnDetailId: Long = 0L, returnShowTitle: String? = null) {
    val navController = rememberNavController()
    LaunchedEffect(libraryReturnRevision) {
        if (libraryReturnRevision > 0) {
            if (returnShowTitle != null) {
                navController.navigate("show/${Uri.encode(returnShowTitle)}") {
                    launchSingleTop = true
                    popUpTo(ROUTE_LIBRARY) { inclusive = false }
                }
            } else if (returnDetailId > 0L) {
                val current = navController.currentBackStackEntry
                val currentDetailId = if (current?.destination?.route == "detail/{videoId}")
                    current.arguments?.getString("videoId")?.toLongOrNull() else null
                if (com.opticast.player.player.shouldOpenReturnedDetails(returnDetailId, currentDetailId)) {
                    navController.navigate("detail/$returnDetailId") {
                        launchSingleTop = true
                        popUpTo(ROUTE_LIBRARY) { inclusive = false }
                    }
                }
            } else navController.popBackStack(ROUTE_LIBRARY, false)
        }
    }
    val context = LocalContext.current

    // Plain, immediate navigation: an earlier "queue while a transition is
    // running" guard could get stuck and then swallow every later tap.
    // Remote playback: same player activity, but pointed at a network link.
    // Remote playback stays on Media3.
    val playRemote: (String, String) -> Unit = { uri, title ->
        val intent = Intent(context, PlayerActivity::class.java)
            .putExtra(PlayerActivity.EXTRA_REMOTE_URI, uri)
            .putExtra(PlayerActivity.EXTRA_REMOTE_TITLE, title)
        PlayerActivity.launch(context, intent)
    }
    val navigate: (String) -> Unit = remember {
        { route -> navController.navigate(route) }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        NavHost(
            navController = navController,
            startDestination = ROUTE_LIBRARY,
            // Keep the underlying library stationary; only the covering page fades.
            enterTransition = { fadeIn(animationSpec = tween(140)) },
            exitTransition = { androidx.compose.animation.ExitTransition.None },
            popEnterTransition = { androidx.compose.animation.EnterTransition.None },
            popExitTransition = { fadeOut(animationSpec = tween(140)) },
        ) {
            composable(ROUTE_LIBRARY) {
                val launchPlayer = rememberPlayerLauncher()
                // Mobile only: no TV support - always show phone LibraryScreen
                LibraryScreen(
                    onOpenDetail = { id -> navigate("detail/$id") },
                    onOpenPlayer = { id -> launchPlayer(id) },
                    onOpenMatch = { id -> navController.navigate("match/$id") },
                    onOpenShow = { name -> navigate("show/${Uri.encode(name)}") },
                    onOpenSettings = { navigate(ROUTE_SETTINGS) },
                )
            }
            composable(ROUTE_SETTINGS) {
                SettingsScreen(
                    onBack = { navController.popBackStack() },
                    onOpenStorage = { navController.navigate(ROUTE_STORAGE) },
                    onOpenNetwork = { navController.navigate(ROUTE_NETWORK) },
                )
            }

            composable(ROUTE_STORAGE) {
                StorageScreen(onBack = { navController.popBackStack() },
                    onOpenMatch = { id -> navigate("match/$id") })
            }
            composable(ROUTE_NETWORK) {
                NetworkScreen(
                    onBack = { navController.popBackStack() },
                    onPlayRemote = playRemote,
                )
            }
            composable("show/{showName}") { entry ->
                val name = entry.arguments?.getString("showName")
                    ?.let { Uri.decode(it) }
                    ?: ""
                val launchPlayer = rememberPlayerLauncher()
                ShowScreen(
                    showName = name,
                    onBack = { navController.popBackStack() },
                    onPlayEpisode = { id -> launchPlayer(id) },
                    onOpenDetail = { id -> navigate("detail/$id") },
                )
            }
            composable("detail/{videoId}") { entry ->
                val id = entry.arguments?.getString("videoId")?.toLongOrNull() ?: 0L
                val launchPlayer = rememberPlayerLauncher()
                DetailScreen(
                    videoId = id,
                    onBack = { navController.popBackStack() },
                    onPlay = { launchPlayer(id) },
                    onOpenMatch = { navController.navigate("match/$id") },
                )
            }
            composable("match/{videoId}") { entry ->
                val id = entry.arguments?.getString("videoId")?.toLongOrNull() ?: 0L
                MatchScreen(
                    videoId = id,
                    onDone = { navController.popBackStack() },
                )
            }
        }
    }
}

/**
 * Reuses an existing PiP player without foregrounding it; otherwise opens the player.
 * Back never enters PiP automatically.
 */
@Composable
private fun rememberPlayerLauncher(): (Long) -> Unit {
    val context = LocalContext.current
    return remember {
        { id: Long -> PlayerActivity.launch(context, playerIntent(context, id)) }
    }
}

private const val ROUTE_LIBRARY = "library"
private const val ROUTE_NETWORK = "network"
private const val ROUTE_STORAGE = "storage"
private const val ROUTE_SETTINGS = "settings"
