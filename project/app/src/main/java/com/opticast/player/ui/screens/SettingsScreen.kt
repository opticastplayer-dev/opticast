package com.opticast.player.ui.screens

import androidx.compose.material.icons.filled.SystemUpdate

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import kotlinx.coroutines.CoroutineScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.draw.rotate
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.size
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.opticast.player.data.AppContainer
import com.opticast.player.ui.components.progressBarStyleLabel
import com.opticast.player.data.AppSettings
import com.opticast.player.data.local.FolderExclusions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.content.Context
import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOff
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.vector.ImageVector
import com.opticast.player.R

private val LANGUAGE_OPTIONS = listOf(
    "en" to "English",
    "fr" to "French",
    "es" to "Spanish",
    "zh" to "Chinese",
)

private val THEME_OPTIONS = listOf(
    Triple("cast", "Cast · default", "Pitch black with wordmark violet, purple and magenta accents"),
    Triple("midnight", "Midnight", "Pitch-black cinema with warm amber accents"),
    Triple("ocean", "Ocean", "Deep navy with a luminous cyan accent"),
)

private data class ApiProvider(
    val title: String,
    val initials: String,
    val color: Long,
    val url: String,
)

private val API_PROVIDERS = listOf(
    ApiProvider("TMDB", "TM", 0xFF01B4E4, "https://www.themoviedb.org/settings/api"),
    ApiProvider("OpenSubtitles", "OS", 0xFF8AC926, "https://www.opensubtitles.com/en/consumers"),
    ApiProvider("SubDL", "SD", 0xFFFF595E, "https://subdl.com/settings"),
)

private val GRID_OPTIONS = listOf(
    Triple("compact", "Compact", "Smaller posters, more per row"),
    Triple("medium", "Medium", "The default balance"),
    Triple("comfortable", "Comfortable", "Bigger posters, fewer per row"),
)

private val PRESET_FOLDERS = listOf(
    "DCIM/Camera" to "Camera",
    "DCIM/Screen recordings" to "Screen recordings",
    "Pictures/Screenshots" to "Screenshots",
    "Download" to "Downloads",
    "Movies/WhatsApp" to "WhatsApp",
    "Movies/Telegram" to "Telegram",
)

/** Settings renders its text slightly larger than the rest of the app. */
private const val SETTINGS_FONT_SCALE = 1.10f
private val LocalSettingsQuery = staticCompositionLocalOf { "" }

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenStorage: () -> Unit = {},
    onOpenNetwork: () -> Unit = {},
    onOpenStorageAnalyzer: () -> Unit = {},
    onOpenTrash: () -> Unit = {},
    onOpenQueue: () -> Unit = {},
    onOpenOrganizeAssistant: () -> Unit = {},
    onOpenGestureCustomization: () -> Unit = {},
) {
    val settings by AppContainer.settings.settings
        .collectAsStateWithLifecycle(initialValue = AppSettings())
    val scope = rememberCoroutineScope()
    var confirmClear by remember { mutableStateOf(false) }
    var appearanceExpanded by rememberSaveable { mutableStateOf(false) }
    var tweaksExpanded by rememberSaveable { mutableStateOf(false) }
    var developerExpanded by rememberSaveable { mutableStateOf(false) }
    var customFolder by remember { mutableStateOf("") }

    // Preserve the settings' modest type emphasis without replacing Android's
    // native density/font-scaling implementation (including nonlinear scaling).
    MaterialTheme(typography = settingsTypography(MaterialTheme.typography)) {
    var searchOpen by rememberSaveable { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    val activeQuery = if (searchOpen) searchQuery.trim() else ""
    val searchFocus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val listState = rememberLazyListState()
    BackHandler(searchOpen) { searchOpen = false; searchQuery = ""; keyboard?.hide() }
    LaunchedEffect(searchOpen) { if (searchOpen) searchFocus.requestFocus() }
    LaunchedEffect(activeQuery) { listState.scrollToItem(0) }
    CompositionLocalProvider(LocalSettingsQuery provides activeQuery) {
    Scaffold(topBar = {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(Modifier.statusBarsPadding()) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { if (searchOpen) { searchOpen = false; searchQuery = ""; keyboard?.hide() } else onBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                    Text(settingsHeaderTitle("Settings"), style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
                    IconButton(onClick = {
                        searchOpen = !searchOpen
                        if (!searchOpen) { searchQuery = ""; keyboard?.hide() }
                    }) { Icon(if (searchOpen) Icons.Filled.Close else Icons.Filled.Search, if (searchOpen) "Close settings search" else "Search settings") }
                }
                if (searchOpen) {
                    OutlinedTextField(value = searchQuery, onValueChange = { searchQuery = it },
                        label = { Text("Search settings") }, placeholder = { Text("Try subtitles, theme or backup") },
                        singleLine = true, modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp).focusRequester(searchFocus),
                        trailingIcon = { if (searchQuery.isNotEmpty()) IconButton(onClick = { searchQuery = "" }) { Icon(Icons.Filled.Close, "Clear search") } })
                    Text(if (activeQuery.isEmpty()) "Find a setting by name or keyword" else {
                        val count = matchingSettingsSections(activeQuery).size
                        if (count == 0) "No matching settings. Try a different word." else "$count matching categor${if (count == 1) "y" else "ies"}"
                    }, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
                }
            }
        }
    }) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().padding(padding).imePadding(),
            contentPadding = PaddingValues(bottom = 32.dp),
        ) {
            item(key = "UI & Appearance") {
                SettingsGroup("UI & Appearance") {
                CollapsibleSettingsCard(
                    icon = Icons.Filled.Palette,
                    title = "Appearance",
                    subtitle = "Theme \u00b7 wallpaper colours \u00b7 grid size",
                    expanded = appearanceExpanded,
                    onToggle = { appearanceExpanded = !appearanceExpanded },
                ) {
                    Text("Theme", style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.height(8.dp))
                    THEME_OPTIONS.forEach { (id, name, blurb) ->
                        val selected = settings.appTheme == id
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = if (selected) {
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                            } else {
                                MaterialTheme.colorScheme.surfaceContainer
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .clickable {
                                    scope.launch { AppContainer.settings.setAppTheme(id) }
                                },
                        ) {
                            Row(
                                Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(
                                    Modifier
                                        .size(22.dp)
                                        .clip(RoundedCornerShape(50))
                                        .background(themePreviewColor(id)),
                                )
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(name, style = MaterialTheme.typography.labelLarge)
                                    Text(
                                        blurb,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                if (selected) {
                                    Icon(
                                        Icons.Filled.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                    )
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    Text("Library grid", style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        GRID_OPTIONS.forEach { (id, name, _) ->
                            FilterChip(
                                selected = settings.libraryGrid == id,
                                onClick = {
                                    scope.launch { AppContainer.settings.setLibraryGrid(id) }
                                },
                                label = { Text(name) },
                            )
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                "Adapt colors to your wallpaper",
                                style = MaterialTheme.typography.labelLarge,
                            )
                            Text(
                                "Use wallpaper colours on Android 12+. Keeps the dark background.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Switch(
                            checked = settings.useDeviceColors,
                            onCheckedChange = { enabled ->
                                scope.launch { AppContainer.settings.setUseDeviceColors(enabled) }
                            },
                        )
                    }
                }
                PlayerLayoutSettingsCard(settings, scope)
                }
            }
            item(key = "Playback & Controls") {
                SettingsGroup("Playback & Controls") {
                EngineSettingsCard(settings, scope)
                BehaviourSettingsCard(settings, scope, onOpenNetwork)
                GestureSettingsCard(settings, scope, onOpenGestureCustomization)
                PlayerAdvancedSettingsCard(settings, scope)
                }
            }
            item(key = "File Management") {
                SettingsGroup("File Management") {
                SettingsCard(icon = Icons.Filled.FolderOff, title = "Excluded folders") {
                    Text(
                        "Videos inside these folders are skipped when scanning - handy for camera clips, screen recordings and messenger videos.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(12.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        PRESET_FOLDERS.forEach { (path, label) ->
                            val selected = settings.excludedFolders.contains(path)
                            FilterChip(
                                selected = selected,
                                onClick = {
                                    val next = if (selected) {
                                        settings.excludedFolders - path
                                    } else {
                                        settings.excludedFolders + path
                                    }
                                    scope.launch {
                                        AppContainer.settings.setExcludedFolders(next)
                                        FolderExclusions.hydrate(next)
                                        AppContainer.metadataStore.touch()
                                    }
                                },
                                label = { Text(label) },
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = customFolder,
                            onValueChange = { customFolder = it },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(18.dp),
                            placeholder = { Text("Custom path, e.g. Movies/Clips") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                            ),
                        )
                        IconButton(
                            onClick = {
                                val path = customFolder.trim().trim('/')
                                if (path.isNotBlank() && !settings.excludedFolders.contains(path)) {
                                    val next = settings.excludedFolders + path
                                    scope.launch {
                                        AppContainer.settings.setExcludedFolders(next)
                                        FolderExclusions.hydrate(next)
                                        AppContainer.metadataStore.touch()
                                    }
                                    customFolder = ""
                                }
                            },
                        ) {
                            Icon(
                                Icons.Filled.Add,
                                contentDescription = "Add folder",
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                    val customFolders = settings.excludedFolders.filter { folder ->
                        PRESET_FOLDERS.none { it.first == folder }
                    }
                    customFolders.forEach { folder ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                folder,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f),
                            )
                            IconButton(onClick = {
                                val next = settings.excludedFolders - folder
                                scope.launch {
                                    AppContainer.settings.setExcludedFolders(next)
                                    FolderExclusions.hydrate(next)
                                    AppContainer.metadataStore.touch()
                                }
                            }) {
                                Icon(
                                    Icons.Filled.Close,
                                    contentDescription = "Remove",
                                    tint = MaterialTheme.colorScheme.error,
                                )
                            }
                        }
                    }
                }

                SettingsCard(icon = Icons.Filled.Storage, title = "Storage") {
                    Text(
                        "Manage space, cached files and backups.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = onOpenStorage,
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        Text("Open storage dashboard")
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = onOpenTrash,
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        Text("Trash / Recently deleted")
                    }
                }

                SettingsCard(icon = Icons.Filled.Movie, title = "Library maintenance") {
                    OutlinedButton(
                        onClick = { confirmClear = true },
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        Text("Clear all saved metadata")
                    }
                }
                }
            }
            item(key = "Media Settings") {
                SettingsGroup("Media Settings") {
                TrackSettingsCard(settings, scope)
                LanguageDropdownCard(settings = settings, scope = scope)
                SettingsCard(icon = Icons.Filled.Subtitles, title = "Auto subtitles") {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                "Download subtitles during scan",
                                style = MaterialTheme.typography.labelLarge,
                            )
                            Text(
                                "When a file is matched, OptiCast fetches the most-downloaded subtitle in your languages. Uses your OpenSubtitles quota.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Switch(
                            checked = settings.autoSubtitles,
                            onCheckedChange = { enabled ->
                                scope.launch { AppContainer.settings.setAutoSubtitles(enabled) }
                            },
                        )
                    }
                }
                SoundSettingsCard(settings, scope)
                }
            }
            item(key = "Advanced & About") {
                SettingsGroup("Advanced & About") {
                CollapsibleSettingsCard(
                    icon = Icons.Filled.Tune,
                    title = "Data usage",
                    subtitle = "Artwork quality",
                    expanded = tweaksExpanded,
                    onToggle = { tweaksExpanded = !tweaksExpanded },
                ) {
                    Spacer(Modifier.height(14.dp))
                    PrefToggle(
                        label = "Save mobile data",
                        description = "Use smaller artwork and download HD images only on Wi-Fi.",
                        checked = settings.dataSaverArtwork,
                    ) { enabled ->
                        scope.launch { AppContainer.settings.setDataSaverArtwork(enabled) }
                    }
                }
                ApiKeysCard(settings = settings, scope = scope)
                SettingsCard(icon = Icons.Filled.SystemUpdate, title = "Check for updates") {
                    UpdateCheckOption(installedVersionLabel(LocalContext.current))
                }
                SettingsCard(icon = Icons.Filled.Description, title = "About") {
                    com.opticast.player.ui.components.LegalNoticesButton()
                    Spacer(Modifier.height(14.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(R.drawable.ic_opticast_mark),
                            contentDescription = null,
                            modifier = Modifier.size(56.dp),
                        )
                        Spacer(Modifier.width(14.dp))
                        Column {
                            Text(settingsHeaderTitle("OptiCast"), style = MaterialTheme.typography.headlineSmall)
                            Text(
                                // Read from the installed package, never a
                                // literal: the old hardcoded string claimed
                                // "2.0.0 (build 34)" no matter which build was
                                // actually running.
                                installedVersionLabel(LocalContext.current),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    Text(
                        "OptiCast plays your own media. No movies or streaming accounts are supplied. Only access content you have permission to use.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(16.dp))
                    Spacer(Modifier.height(16.dp))
                    Image(painterResource(R.drawable.tmdb_attribution), contentDescription = "TMDB",
                        modifier = Modifier.width(88.dp))
                    Text("This product uses the TMDB API but is not endorsed or certified by TMDB.",
                        style = MaterialTheme.typography.bodySmall)
                    ProviderCredits()
                    Spacer(Modifier.height(16.dp))
                    DeviceInfo()
                }
                CollapsibleSettingsCard(
                    icon = Icons.Filled.Code,
                    title = "Contact & support",
                    subtitle = "Contact and support",
                    expanded = developerExpanded,
                    onToggle = { developerExpanded = !developerExpanded },
                ) {
                    DeveloperRow("Email", "opticastproject@gmail.com", "mailto:opticastproject@gmail.com")
                }
                }
            }
        }
    }
    }
    }

    if (confirmClear) {
        AlertDialog(containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.92f), 
            onDismissRequest = { confirmClear = false },
            title = { Text("Clear all metadata?") },
            text = { Text("Removes all saved provider matches on this device. Your video files are not touched.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmClear = false
                    scope.launch {
                        withContext(Dispatchers.IO) { AppContainer.metadataStore.clearAll() }
                    }
                }) { Text("Clear") }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) { Text("Cancel") }
            },
        )
    }
}

/** Device + build facts, shown in About. */
@Composable
/**
 * The version actually installed, straight from the package manager - so the
 * About screen can never drift from the build that is running again.
 */
private fun installedVersionLabel(context: Context): String = runCatching {
    val info = context.packageManager.getPackageInfo(context.packageName, 0)
    val code = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        info.longVersionCode
    } else {
        @Suppress("DEPRECATION")
        info.versionCode.toLong()
    }
    "Version ${info.versionName} (build $code)"
}.getOrDefault("Version unknown")

@Composable
private fun DeviceInfo() {
    val context = LocalContext.current
    val dm = context.resources.displayMetrics
    val installer = getInstallerInfo(context)
    val rows = listOf(
        "Device" to "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL}",
        "Android" to "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
        "Screen" to "${dm.widthPixels} x ${dm.heightPixels} px \u00b7 ${dm.densityDpi} dpi",
        "CPU" to (Build.SUPPORTED_ABIS.firstOrNull() ?: "unknown"),
        "Package" to context.packageName,
        "Build" to installedVersionLabel(context).removePrefix("Version "),
        "Installer" to installer.first,
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(18.dp)),
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            rows.forEach { (label, value) ->
                com.opticast.player.ui.components.AlignedLabelValue(label, value)
            }
            if (installer.second) {
                Spacer(Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            androidx.compose.ui.graphics.Color(0xFFB3261E).copy(alpha = 0.12f),
                            RoundedCornerShape(12.dp)
                        )
                        .padding(12.dp)
                ) {
                    Column {
                        Text(
                            "Notice: Third-party source",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                            color = androidx.compose.ui.graphics.Color(0xFFB3261E)
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "This build was installed from a source outside the official distribution channels. For security and to ensure you receive verified updates, we recommend obtaining OptiCast from official sources listed in the project documentation.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            } else {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Official distribution channel verified",
                    style = MaterialTheme.typography.bodySmall,
                    color = androidx.compose.ui.graphics.Color(0xFF2E7D32),
                    fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                )
            }
        }
    }
}

private fun getInstallerInfo(context: Context): Pair<String, Boolean> {
    return try {
        val pm = context.packageManager
        val installer = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            pm.getInstallSourceInfo(context.packageName).installingPackageName
        } else {
            @Suppress("DEPRECATION")
            pm.getInstallerPackageName(context.packageName)
        }
        val info = com.opticast.player.util.InstallerVerifier.verify(installer)
        info.displayName to info.isThirdParty
    } catch (_: Exception) {
        "Unknown" to false
    }
}

/** One labelled row in the Developer card; [link] makes it tappable. */
@Composable
private fun DeveloperRow(label: String, value: String, link: String?) {
    val context = LocalContext.current
    com.opticast.player.ui.components.AlignedLabelValue(label, value,
        modifier = if (link != null) Modifier.heightIn(min = 48.dp).clickable {
            runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link))) }
        } else Modifier,
        valueColor = if (link != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)

}

/** All provider tokens grouped in one expandable card for a clean settings list. */
@Composable
private fun ApiKeysCard(settings: AppSettings, scope: CoroutineScope) {
    val query = LocalSettingsQuery.current
    if (!settingsSearchMatches("API keys", query)) return

    var expanded by rememberSaveable { mutableStateOf(false) }
    var searchCollapsed by remember(query) { mutableStateOf(false) }
    val showContent = if (query.isNotBlank()) !searchCollapsed else expanded
    val chevron by animateFloatAsState(
        targetValue = if (showContent) 180f else 0f,
        animationSpec = tween(260, easing = FastOutSlowInEasing),
        label = "apiChevron",
    )
    Box(modifier = settingsCategoryModifier("API keys")) {
        Column(
            // Deliberately asymmetric, generous inset: the first glyph of every
            // title sits well clear of the card edge.
            Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { if (query.isNotBlank()) searchCollapsed = !searchCollapsed else expanded = !expanded }
                    .heightIn(min = 48.dp)
                    .padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.VpnKey, null, Modifier.size(22.dp), tint = MaterialTheme.colorScheme.primary)
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(settingsHeaderTitle("API keys"), style = MaterialTheme.typography.titleMedium)
                    Text(
                        "TMDB, OpenSubtitles and SubDL",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Icon(
                    Icons.Filled.ExpandMore,
                    contentDescription = if (showContent) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 8.dp).size(24.dp).rotate(chevron),
                )
            }
            AnimatedVisibility(
                visible = showContent,
                enter = fadeIn(tween(220)) + expandVertically(tween(260, easing = FastOutSlowInEasing)),
                exit = fadeOut(tween(180)) + shrinkVertically(tween(220, easing = FastOutSlowInEasing)),
            ) {
                Column {
                    Spacer(Modifier.height(12.dp))
                    ApiKeySection(
                        provider = API_PROVIDERS[0],
                        description = "Movie & TV artwork and info from themoviedb.org. Free v3 key: sign up, then open Settings → API.",
                    ) {
                        ApiKeyField(
                            value = settings.tmdbApiKey,
                            placeholder = "TMDB API key",
                            onSave = { key ->
                                scope.launch { AppContainer.settings.setTmdbApiKey(key) }
                            },
                        )
                    }
                    ApiKeySection(
                        provider = API_PROVIDERS[1],
                        description = "Subtitles from opensubtitles.com. Free key: create an account, then add a consumer app under Settings → API keys.",
                    ) {
                        ApiKeyField(
                            value = settings.openSubtitlesApiKey,
                            placeholder = "OpenSubtitles API key",
                            onSave = { key ->
                                scope.launch { AppContainer.settings.setOpenSubtitlesApiKey(key) }
                            },
                        )
                    }
                    ApiKeySection(
                        provider = API_PROVIDERS[2],
                        description = "Backup subtitle provider - great for anime. Free key from your SubDL account panel.",
                    ) {
                        ApiKeyField(
                            value = settings.subdlApiKey,
                            placeholder = "SubDL API key",
                            onSave = { key ->
                                scope.launch { AppContainer.settings.setSubdlApiKey(key) }
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ApiKeySection(
    provider: ApiProvider,
    description: String,
    field: @Composable () -> Unit,
) {
    val context = LocalContext.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        // Brand monogram badge.
        Box(
            Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(provider.color).copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                provider.initials,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = Color(provider.color),
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(provider.title, style = MaterialTheme.typography.labelLarge)
            if (description.isNotBlank()) Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(
            onClick = {
                runCatching {
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse(provider.url))
                    )
                }
            },
        ) {
            Text("Get API key", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.width(6.dp))
            Icon(
                Icons.Filled.OpenInNew,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
            )
        }
    }
    Spacer(Modifier.height(4.dp))
    field()
    Spacer(Modifier.height(14.dp))
}

@Composable
private fun CollapsibleSettingsCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable () -> Unit,
) {
    val query = LocalSettingsQuery.current
    if (!settingsSearchMatches(title, query)) return
    var searchCollapsed by remember(query) { mutableStateOf(false) }
    val showContent = if (query.isNotBlank()) !searchCollapsed else expanded

    val chevron by animateFloatAsState(
        targetValue = if (showContent) 180f else 0f,
        animationSpec = tween(260, easing = FastOutSlowInEasing),
        label = "collapseChevron",
    )
    Box(modifier = settingsCategoryModifier(title)) {
        Column(
            // Deliberately asymmetric, generous inset: the first glyph of every
            // title sits well clear of the card edge.
            Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { if (query.isNotBlank()) searchCollapsed = !searchCollapsed else onToggle() }
                    .heightIn(min = 48.dp).padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Leading icon so each header reads at a glance. Painted in a
                // plain Box: nothing here clips, so a title can never be shaved.
                Box(
                    modifier = Modifier.size(40.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp),
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(settingsHeaderTitle(title), style = MaterialTheme.typography.titleMedium)
                    if (subtitle.isNotBlank()) Text(
                        subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Icon(
                    Icons.Filled.ExpandMore,
                    contentDescription = if (showContent) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 8.dp).size(24.dp).rotate(chevron),
                )
            }
            AnimatedVisibility(
                visible = showContent,
                enter = fadeIn(tween(220)) +
                    expandVertically(tween(260, easing = FastOutSlowInEasing)),
                exit = fadeOut(tween(180)) +
                    shrinkVertically(tween(220, easing = FastOutSlowInEasing)),
            ) {
                Column {
                    Spacer(Modifier.height(10.dp))
                    content()
                }
            }
        }
    }
}

@Composable
private fun SettingsCard(
    icon: ImageVector,
    title: String,
    content: @Composable () -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    CollapsibleSettingsCard(icon, title, settingsCategorySubtitle(title), expanded, { expanded = !expanded }, content)

}

@Composable
private fun ApiKeyField(
    value: String,
    placeholder: String,
    onSave: (String) -> Unit,
) {
    var draft by remember(value) { mutableStateOf(value) }
    OutlinedTextField(
        value = draft,
        onValueChange = { draft = it },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        placeholder = { Text(placeholder) },
        singleLine = true,
        trailingIcon = {
            if (draft.trim() != value) {
                IconButton(onClick = { onSave(draft) }) {
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = "Save",
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        },
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    )
}


// ------------------------------------------------------------ playback card --

/** Toggle row used inside the Playback & control card. */
@Composable
private fun PrefToggle(
    label: String,
    description: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp).padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.labelLarge)
            if (description.isNotBlank()) Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

/** Playback settings applied immediately. */
@Composable
private fun EngineSettingsCard(settings: AppSettings, scope: CoroutineScope) {
    val playbackContext = LocalContext.current
    SettingsCard(icon = Icons.Filled.PlayArrow, title = "Playback") {
        Text("To set as default, open a video from your file manager, select OptiCast and choose Always. You can also share videos to OptiCast.", style = MaterialTheme.typography.bodySmall)
        TextButton(onClick = {
            runCatching { playbackContext.startActivity(android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                android.net.Uri.parse("package:${playbackContext.packageName}"))) }
        }) { Text("System default settings") }
        PrefToggle(
            label = "Use external player",
            description = "Open videos in another installed player.",
            checked = settings.useExternalPlayer,
        ) { enabled -> scope.launch { AppContainer.settings.setUseExternalPlayer(enabled) } }

        Spacer(Modifier.height(10.dp))
        Text(settingsHeaderTitle("Engine selection"), style = MaterialTheme.typography.titleMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("mpv" to "MPV (Default)", "media3" to "Media3").forEach { (id, name) ->
                FilterChip(selected = if (id == "media3") settings.playbackEngine != "mpv" else settings.playbackEngine == id,
                    onClick = { scope.launch { AppContainer.settings.setPlaybackEngine(id) } },
                    label = { Text(name) }, enabled = !settings.useExternalPlayer)
            }
        }
        Text("MPV handles local files with automatic fallback. Network sources and audio enhancements use Media3. Changes apply to the next video.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun TrackSettingsCard(settings: AppSettings, scope: CoroutineScope) {
    SettingsCard(icon = Icons.Filled.Subtitles, title = "Audio & subtitle tracks") {
        Text("For tracks already in your video. Manual choices take priority.", style = MaterialTheme.typography.bodySmall)
        val trackLanguages = listOf("" to "Any / default", "en" to "English", "fr" to "French", "es" to "Spanish", "zh" to "Chinese")
        TrackLanguagePicker("Preferred audio language", settings.preferredAudioLanguage, trackLanguages) { code ->
            scope.launch { AppContainer.settings.setPreferredAudioLanguage(code) }
        }
        PrefToggle("Avoid commentary", "Prefer main audio when available in your chosen language.", settings.avoidCommentary) {
            scope.launch { AppContainer.settings.setAvoidCommentary(it) }
        }
        Text("Subtitle rule", style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("default" to "Container default", "forced" to "Forced only", "full" to "Full subtitles", "off" to "Off").forEach { (mode, label) ->
                FilterChip(selected = settings.embeddedSubtitleMode == mode,
                    onClick = { scope.launch { AppContainer.settings.setEmbeddedSubtitleMode(mode) } }, label = { Text(label) })
            }
        }
        TrackLanguagePicker("Embedded subtitle language", settings.embeddedSubtitleLanguage, trackLanguages) { code ->
            scope.launch { AppContainer.settings.setEmbeddedSubtitleLanguage(code) }
        }
        Text("No matching forced/full track? Subtitles stay off. Download languages are set separately.", style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(10.dp))
    }
}

@Composable
private fun PlayerLayoutSettingsCard(settings: AppSettings, scope: CoroutineScope) {
    SettingsCard(icon = Icons.Filled.Tune, title = "Player layout") {
        Text(
            settingsHeaderTitle("Progress bar"),
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(top = 12.dp),
        )
        Spacer(Modifier.height(6.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            listOf(
                "thick" to "Thick · default",
                "gradient" to "Gradient",
                "hidden" to "Hidden",
            ).forEach { (id, label) ->
                FilterChip(
                    selected = settings.progressBarStyle == id,
                    onClick = { scope.launch { AppContainer.settings.setProgressBarStyle(id) } },
                    label = { Text(label) },
                )
            }
        }
        if (settings.progressBarStyle == "hidden") {
            Text(
                "The bar is hidden; the elapsed and total times stay on screen.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        }

        Text(
            settingsHeaderTitle("Player controls"),
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(top = 16.dp, bottom = 6.dp),
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            listOf(
                "back" to "Back",
                "speed" to "Speed",
                "subtitles" to "Audio & Subtitles",
                "library" to "Library",
                "chapters" to "Chapters",
                "info" to "Info",
                "lock" to "Lock",
                "aspect" to "Aspect",
                "sleep" to "Sleep",
            ).forEach { (id, label) ->
                val selected = settings.playerControls.contains(id)
                FilterChip(
                    selected = selected,
                    onClick = { scope.launch { AppContainer.settings.setPlayerControl(id, !selected) } },
                    label = { Text(label) },
                    leadingIcon = if (selected) {
                        { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    } else null
                )
            }
        }
    }
}

@Composable
private fun GestureSettingsCard(settings: AppSettings, scope: CoroutineScope, onOpenGestureCustomization: () -> Unit = {}) {
    SettingsCard(icon = Icons.Filled.TouchApp, title = "Gestures") {
        Text(
            "Configure swipe and double-tap actions for the player.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(10.dp))
        OutlinedButton(onClick = onOpenGestureCustomization, shape = RoundedCornerShape(16.dp)) {
            Text("Customize gestures")
        }
        Spacer(Modifier.height(12.dp))
        PrefToggle(
            label = "Hold to fast-forward",
            description = "Press and hold to speed up temporarily.",
            checked = settings.holdToSpeed,
        ) { enabled -> scope.launch { AppContainer.settings.setHoldToSpeed(enabled) } }
        if (settings.holdToSpeed) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf(2f, 3f, 4f).forEach { factor ->
                    FilterChip(
                        selected = settings.holdSpeedFactor == factor,
                        onClick = {
                            scope.launch { AppContainer.settings.setHoldSpeedFactor(factor) }
                        },
                        label = { Text("${factor.toInt()}× while held") },
                    )
                }
            }
        }
    }
}

@Composable
private fun BehaviourSettingsCard(settings: AppSettings, scope: CoroutineScope, onOpenNetwork: () -> Unit = {}) {
    SettingsCard(icon = Icons.Filled.PlayArrow, title = "Playback behaviour") {
        PrefToggle(
            label = "Start in landscape",
            description = "Automatically switch to landscape when playback starts.",
            checked = settings.autoLandscape,
        ) { enabled -> scope.launch { AppContainer.settings.setAutoLandscape(enabled) } }
        PrefToggle(
            label = "Keep screen on",
            description = "Prevent display sleep during playback.",
            checked = settings.keepScreenOn,
        ) { enabled -> scope.launch { AppContainer.settings.setKeepScreenOn(enabled) } }
        PrefToggle(
            label = "Preserve voice pitch at speed",
            description = "Maintain natural voice pitch at higher speeds.",
            checked = settings.preservePitch,
        ) { enabled -> scope.launch { AppContainer.settings.setPreservePitch(enabled) } }
        Text(
            "Resume is always enabled. To restart, use Restart from beginning on the detail page.",
            style = MaterialTheme.typography.bodySmall,
        )
        PrefToggle(
            label = "Auto-play next episode",
            description = "Automatically play the next episode when current ends.",
            checked = settings.autoNextEpisode,
        ) { enabled -> scope.launch { AppContainer.settings.setAutoNextEpisode(enabled) } }

        PrefToggle(
            label = "Enable network browsing",
            description = "Show network libraries.",
            checked = settings.enableNetworkBrowsing,
        ) { enabled -> scope.launch { AppContainer.settings.setEnableNetworkBrowsing(enabled) } }
        if (settings.enableNetworkBrowsing) {
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = onOpenNetwork, shape = RoundedCornerShape(16.dp)) {
                Text("Open network libraries")
            }
        }
    }
}

@Composable
private fun PlayerAdvancedSettingsCard(settings: AppSettings, scope: CoroutineScope) {
    SettingsCard(icon = Icons.Filled.Tune, title = "Advanced player") {
        Text("Orientation mode", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 12.dp))
        Spacer(Modifier.height(6.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("auto" to "Auto", "portrait" to "Portrait").forEach { (id, label) ->
                val isSelected = when (id) {
                    "auto" -> settings.orientationMode == "auto" || settings.orientationMode == "sensor"
                    else -> settings.orientationMode == id
                }
                FilterChip(selected = isSelected, onClick = { scope.launch { AppContainer.settings.setOrientationMode(id) } }, label = { Text(label) })
            }
        }

        PrefToggle(
            label = "Auto crop black bars",
            description = "Automatically remove black bars from videos.",
            checked = settings.autoCrop,
        ) { enabled -> scope.launch { AppContainer.settings.setAutoCrop(enabled) } }

        PrefToggle(
            label = "Volume normalization",
            description = "Normalize volume across videos.",
            checked = settings.volumeNormalization,
        ) { enabled -> scope.launch { AppContainer.settings.setVolumeNormalization(enabled) } }
    }
}

@Composable
private fun SoundSettingsCard(settings: AppSettings, scope: CoroutineScope) {
    SettingsCard(icon = Icons.Filled.Tune, title = "Sound") {
        Text(
            "Audio boost",
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(top = 8.dp),
        )
        Spacer(Modifier.height(6.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            listOf(100 to "Off", 125 to "125%", 150 to "150%", 200 to "200%").forEach { (pct, label) ->
                FilterChip(
                    selected = settings.audioBoostPct == pct,
                    onClick = {
                        scope.launch { AppContainer.settings.setAudioBoostPct(pct) }
                    },
                    label = { Text(label) },
                )
            }
        }
        Text(
            "Amplifies quiet recordings above 100%. May clip very loud sources.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(6.dp))
        PrefToggle(
            label = "Dialogue boost",
            description = "Adds presence to speech so quiet dialogue stays clear at low volume. Stacks with audio boost.",
            checked = settings.dialogueBoost,
        ) { enabled -> scope.launch { AppContainer.settings.setDialogueBoost(enabled) } }
    }
}

@Composable
private fun TrackLanguagePicker(label: String, selected: String, options: List<Pair<String, String>>, onSelect: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
        TextButton(onClick = { open = true }, modifier = Modifier.weight(1f)) {
            Text(options.firstOrNull { it.first == selected }?.second ?: selected)
        }
    }
    if (open) AlertDialog(
        onDismissRequest = { open = false },
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.96f),
        title = { Text(label) },
        text = {
            LazyColumn(Modifier.heightIn(max = 400.dp)) {
                options.forEach { (code, name) -> item(key = code) {
                    TextButton(onClick = { onSelect(code); open = false }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                        Text(name, modifier = Modifier.weight(1f))
                        if (code == selected) Icon(Icons.Filled.Check, "Selected")
                    }
                } }
            }
        },
        confirmButton = { TextButton(onClick = { open = false }) { Text("Cancel") } },
    )
}

// -------------------------------------------------------- languages dropdown --

/** Compact dropdown listing every supported subtitle language. */
@Composable
private fun LanguageDropdownCard(settings: AppSettings, scope: CoroutineScope) {
    val query = LocalSettingsQuery.current
    if (!settingsSearchMatches("Subtitle languages", query)) return

    val selected = settings.subtitleLanguages
    val summary = if (selected.isEmpty()) {
        "Choose languages\u2026"
    } else {
        val names = LANGUAGE_OPTIONS
            .filter { it.first in selected }
            .map { it.second }
        when {
            names.isEmpty() -> "${selected.size} selected"
            names.size <= 2 -> names.joinToString(", ")
            else -> "${names.take(2).joinToString(", ")} +${names.size - 2} more"
        }
    }
    var expanded by rememberSaveable { mutableStateOf(false) }
    var searchCollapsed by remember(query) { mutableStateOf(false) }
    val showContent = if (query.isNotBlank()) !searchCollapsed else expanded
    val chevron by animateFloatAsState(
        targetValue = if (showContent) 180f else 0f,
        animationSpec = tween(260, easing = FastOutSlowInEasing),
        label = "languagesChevron",
    )

    Box(modifier = settingsCategoryModifier("Subtitle languages")) {
        Column(
            // Deliberately asymmetric, generous inset: the first glyph of every
            // title sits well clear of the card edge.
            Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { if (query.isNotBlank()) searchCollapsed = !searchCollapsed else expanded = !expanded }
                    .heightIn(min = 48.dp)
                    .padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Subtitles, null, Modifier.size(22.dp), tint = MaterialTheme.colorScheme.primary)
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(settingsHeaderTitle("Subtitle languages"), style = MaterialTheme.typography.titleMedium)
                    Text(
                        summary,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Icon(
                    Icons.Filled.ExpandMore,
                    contentDescription = if (showContent) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 8.dp).size(24.dp).rotate(chevron),
                )
            }
            AnimatedVisibility(
                visible = showContent,
                enter = fadeIn(tween(220)) + expandVertically(tween(260, easing = FastOutSlowInEasing)),
                exit = fadeOut(tween(180)) + shrinkVertically(tween(220, easing = FastOutSlowInEasing)),
            ) {
                Column {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "Subtitle searches (detail page & in-player) look for these languages.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))
                    LANGUAGE_OPTIONS.forEach { (code, name) ->
                        val checked = code in selected
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (checked) {
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                            } else {
                                Color.Transparent
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 1.dp)
                                .clickable {
                                    val next =
                                        if (checked) selected - code else selected + code
                                    if (next.isNotEmpty()) {
                                        scope.launch {
                                            AppContainer.settings.setSubtitleLanguages(next)
                                        }
                                    }
                                },
                        ) {
                            Row(
                                Modifier.heightIn(min = 48.dp).padding(horizontal = 14.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Checkbox(
                                    checked = checked,
                                    onCheckedChange = null,
                                    modifier = Modifier.size(32.dp),
                                )
                                Spacer(Modifier.width(10.dp))
                                Text(name, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun themePreviewColor(theme: String): Color = when (theme) {
    "ocean" -> Color(0xFF6FD3FF)
    "midnight" -> Color(0xFFEEC177)
    else -> com.opticast.player.ui.theme.OptiCastBrandColorScheme.primary
}

private fun settingsTypography(base: androidx.compose.material3.Typography): androidx.compose.material3.Typography {
    fun emphasis(style: androidx.compose.ui.text.TextStyle) = style.copy(
        fontSize = style.fontSize * SETTINGS_FONT_SCALE, lineHeight = style.lineHeight * SETTINGS_FONT_SCALE)
    return base.copy(
        displayLarge = emphasis(base.displayLarge),
        displayMedium = emphasis(base.displayMedium),
        displaySmall = emphasis(base.displaySmall),
        headlineLarge = emphasis(base.headlineLarge),
        headlineMedium = emphasis(base.headlineMedium),
        headlineSmall = emphasis(base.headlineSmall),
        titleLarge = emphasis(base.titleLarge),
        titleMedium = emphasis(base.titleMedium),
        titleSmall = emphasis(base.titleSmall),
        bodyLarge = emphasis(base.bodyLarge),
        bodyMedium = emphasis(base.bodyMedium),
        bodySmall = emphasis(base.bodySmall),
        labelLarge = emphasis(base.labelLarge),
        labelMedium = emphasis(base.labelMedium),
        labelSmall = emphasis(base.labelSmall),
    )
}

private val LocalSettingsGroupLast = staticCompositionLocalOf<String?> { null }

@Composable
private fun SettingsGroup(title: String, content: @Composable () -> Unit) {
    val query = LocalSettingsQuery.current
    val matches = settingsGroups.getValue(title).filter { settingsSearchMatches(it, query) }
    if (matches.isEmpty()) return
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Text(title, Modifier.padding(start = 16.dp, top = 24.dp, bottom = 12.dp),
            style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainerLow, RoundedCornerShape(28.dp))
            .padding(vertical = 4.dp)) {
            CompositionLocalProvider(LocalSettingsGroupLast provides matches.last()) { content() }
        }
    }
}

@Composable
private fun settingsCategoryModifier(title: String): Modifier {
    val last = LocalSettingsGroupLast.current
    val lineColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    return Modifier.fillMaxWidth()
        .padding(horizontal = if (last == null) 16.dp else 0.dp, vertical = if (last == null) 4.dp else 0.dp)
        .background(if (last == null) MaterialTheme.colorScheme.surfaceContainerLow else Color.Transparent, RoundedCornerShape(24.dp))
        .drawBehind {
            if (last != null && last != title) drawLine(lineColor,
                androidx.compose.ui.geometry.Offset(16.dp.toPx(), size.height),
                androidx.compose.ui.geometry.Offset(size.width - 16.dp.toPx(), size.height), 1.dp.toPx())
        }
}
