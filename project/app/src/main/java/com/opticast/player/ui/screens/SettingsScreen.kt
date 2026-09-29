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
import com.opticast.player.player.AudioPreset

private val LANGUAGE_OPTIONS = listOf(
    "en" to "English",
    "fr" to "French",
    "es" to "Spanish",
    "de" to "German",
    "it" to "Italian",
    "pt" to "Portuguese",
    "pt-BR" to "Portuguese (Brazil)",
    "nl" to "Dutch",
    "ru" to "Russian",
    "uk" to "Ukrainian",
    "pl" to "Polish",
    "cs" to "Czech",
    "sk" to "Slovak",
    "hu" to "Hungarian",
    "ro" to "Romanian",
    "bg" to "Bulgarian",
    "sr" to "Serbian",
    "hr" to "Croatian",
    "sl" to "Slovenian",
    "el" to "Greek",
    "tr" to "Turkish",
    "ar" to "Arabic",
    "he" to "Hebrew",
    "fa" to "Persian",
    "hi" to "Hindi",
    "bn" to "Bengali",
    "ta" to "Tamil",
    "te" to "Telugu",
    "ml" to "Malayalam",
    "ur" to "Urdu",
    "zh" to "Chinese",
    "zh-TW" to "Chinese (Traditional)",
    "ja" to "Japanese",
    "ko" to "Korean",
    "vi" to "Vietnamese",
    "th" to "Thai",
    "id" to "Indonesian",
    "ms" to "Malay",
    "sv" to "Swedish",
    "no" to "Norwegian",
    "da" to "Danish",
    "fi" to "Finnish",
    "is" to "Icelandic",
    "et" to "Estonian",
    "lv" to "Latvian",
    "lt" to "Lithuanian",
    "af" to "Afrikaans",
    "sw" to "Swahili",
    "ny" to "Chichewa",
    "zu" to "Zulu",
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
    ApiProvider("OMDb", "OM", 0xFFFFC300, "https://www.omdbapi.com/apikey.aspx"),
    ApiProvider("Fanart.tv", "FT", 0xFF9B5DE5, "https://fanart.tv/get-an-api-key/"),
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
                BehaviourSettingsCard(settings, scope)
                GestureSettingsCard(settings, scope)
                }
            }
            item(key = "File Management") {
                SettingsGroup("File Management") {
                SettingsCard(icon = Icons.Filled.FolderOff, title = "Excluded folders") {
                    Text(
                        "Videos inside these folders are skipped when scanning — handy for camera clips, screen recordings and messenger videos.",
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
                SettingsCard(icon = Icons.Filled.Folder, title = "Network libraries") {
                    NetworkToolsPanel(onOpenNetwork)
                }

                SettingsCard(icon = Icons.Filled.Storage, title = "Storage & backups") {
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
                }

                SettingsCard(icon = Icons.Filled.Movie, title = "Library maintenance") {
                    OutlinedButton(
                        onClick = { confirmClear = true },
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        Text("Clear all saved metadata")
                    }
                }
                NamingGuideCard()
                }
            }
            item(key = "Media Settings") {
                SettingsGroup("Media Settings") {
                TrackSettingsCard(settings, scope)
                SubtitleFontSettingsCard(scope)
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
                    title = "Data & performance",
                    subtitle = "Save mobile data \u00b7 Performance mode",
                    expanded = tweaksExpanded,
                    onToggle = { tweaksExpanded = !tweaksExpanded },
                ) {
                    Spacer(Modifier.height(14.dp))
                    PrefToggle(
                        label = "Save mobile data",
                        description = "Smaller artwork; bulk artwork downloads only on Wi-Fi.",
                        checked = settings.dataSaverArtwork,
                    ) { enabled ->
                        scope.launch { AppContainer.settings.setDataSaverArtwork(enabled) }
                    }
                    Spacer(Modifier.height(14.dp))
                    PrefToggle(
                        label = "Performance mode",
                        description = "Uses the standard refresh rate, usually 60 Hz. Restart to apply.",
                        checked = settings.performanceMode,
                    ) { enabled ->
                        scope.launch { AppContainer.settings.setPerformanceMode(enabled) }
                    }
                    Spacer(Modifier.height(8.dp))
                    com.opticast.player.player.MemorySnapshotPanel()
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
    val rows = listOf(
        "Device" to "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL}",
        "Android" to "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
        "Screen" to "${dm.widthPixels} x ${dm.heightPixels} px \u00b7 ${dm.densityDpi} dpi",
        "CPU" to (Build.SUPPORTED_ABIS.firstOrNull() ?: "unknown"),
        "Package" to context.packageName,
        "Build" to installedVersionLabel(context).removePrefix("Version "),
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
        }
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
                        "TMDB · OpenSubtitles · SubDL · OMDb · Fanart.tv",
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
                        description = "Backup subtitle provider — great for anime. Free key from your SubDL account panel.",
                    ) {
                        ApiKeyField(
                            value = settings.subdlApiKey,
                            placeholder = "SubDL API key",
                            onSave = { key ->
                                scope.launch { AppContainer.settings.setSubdlApiKey(key) }
                            },
                        )
                    }
                    ApiKeySection(
                        provider = API_PROVIDERS[3],
                        description = "IMDb votes, Rotten Tomatoes and Metacritic scores. Free key (1,000 calls/day).",
                    ) {
                        ApiKeyField(
                            value = settings.omdbApiKey,
                            placeholder = "OMDb API key",
                            onSave = { key ->
                                scope.launch { AppContainer.settings.setOmdbApiKey(key) }
                            },
                        )
                    }
                    ApiKeySection(
                        provider = API_PROVIDERS[4],
                        description = "Clearlogos and richer backgrounds — the Infuse look. Anime (AniList) needs no key.",
                    ) {
                        ApiKeyField(
                            value = settings.fanartApiKey,
                            placeholder = "Fanart.tv API key",
                            onSave = { key ->
                                scope.launch { AppContainer.settings.setFanartApiKey(key) }
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

/** MX-style playback & gesture settings, all applied live by the built-in player. */
@Composable
private fun EngineSettingsCard(settings: AppSettings, scope: CoroutineScope) {
    val playbackContext = LocalContext.current
    SettingsCard(icon = Icons.Filled.PlayArrow, title = "Playback engine") {
        Text("To make OptiCast your default, open a video from a file manager, choose OptiCast and select Always if Android offers it. You can also share a video to OptiCast.", style = MaterialTheme.typography.bodySmall)
        TextButton(onClick = {
            runCatching { playbackContext.startActivity(android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                android.net.Uri.parse("package:${playbackContext.packageName}"))) }
        }) { Text("Android app / default settings") }
        PrefToggle(
            label = "Use external player",
            description = "Open videos in another installed player.",
            checked = settings.useExternalPlayer,
        ) { enabled -> scope.launch { AppContainer.settings.setUseExternalPlayer(enabled) } }

        Spacer(Modifier.height(10.dp))
        Text(settingsHeaderTitle("Built-in playback engine"), style = MaterialTheme.typography.titleMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("mpv" to "mpv · default", "media3" to "Media3").forEach { (id, name) ->
                FilterChip(selected = if (id == "media3") settings.playbackEngine != "mpv" else settings.playbackEngine == id,
                    onClick = { scope.launch { AppContainer.settings.setPlaybackEngine(id) } },
                    label = { Text(name) }, enabled = !settings.useExternalPlayer)
            }
        }
        Text("mpv plays local files with one Media3 fallback. Network playback and EQ/boost use Media3. Engine changes apply to the next video opened.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

        Spacer(Modifier.height(10.dp))
        PrefToggle("Remember successful engine", "Reuses a video’s working engine after 10 seconds of playback. Explicit Media3 takes priority.", settings.engineMemoryEnabled) {
            scope.launch { AppContainer.settings.setEngineMemoryEnabled(it) }
        }
        PrefToggle("Smaller local playback buffer", "Uses less memory for local files. Automatically tries a larger buffer if sustained buffer starvation occurs. Reopen the video after changing this.", settings.localBufferTrial) {
            scope.launch { AppContainer.settings.setLocalBufferTrial(it) }
        }
        var enginesCleared by remember { mutableStateOf(false) }
        TextButton(onClick = {
            com.opticast.player.player.VideoPlaybackPreferences(playbackContext).clearEngines()
            enginesCleared = true
        }) { Text(if (enginesCleared) "Remembered engines cleared" else "Clear remembered engines") }
    }
}

@Composable
private fun TrackSettingsCard(settings: AppSettings, scope: CoroutineScope) {
    SettingsCard(icon = Icons.Filled.Subtitles, title = "Audio & subtitle tracks") {
        Text("For tracks already in your video. Manual choices take priority.", style = MaterialTheme.typography.bodySmall)
        val trackLanguages = listOf("" to "Any / default", "en" to "English", "ny" to "Chichewa", "fr" to "French", "es" to "Spanish", "pt" to "Portuguese", "de" to "German", "it" to "Italian", "ja" to "Japanese", "ko" to "Korean", "zh" to "Chinese", "ar" to "Arabic", "hi" to "Hindi", "ru" to "Russian")
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
private fun SubtitleFontSettingsCard(scope: CoroutineScope) {
    val context = LocalContext.current
    val fontManager = remember { AppContainer.subtitleFonts }
    var fonts by remember { mutableStateOf(fontManager.fontNames()) }
    var selectedFont by remember { mutableStateOf("System Default") }
    val fontPicker = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            try {
                val input = context.contentResolver.openInputStream(it)
                val fileName = it.lastPathSegment?.substringAfterLast("/") ?: "custom.ttf"
                val tempFile = java.io.File(context.cacheDir, fileName)
                input?.use { inp -> tempFile.outputStream().use { out -> inp.copyTo(out) } }
                if (fontManager.importFont(tempFile)) {
                    fonts = fontManager.fontNames()
                }
                tempFile.delete()
            } catch (_: Exception) {}
        }
    }
    
    SettingsCard(icon = Icons.Filled.Subtitles, title = "Subtitle fonts — custom") {
        Text("Add your own subtitle fonts — offline, no internet needed. Supports .ttf and .otf. Falls back to system font.", style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(8.dp))
        Text("Available fonts: ${fonts.size}", style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            fonts.forEach { fontName ->
                FilterChip(
                    selected = selectedFont == fontName,
                    onClick = { selectedFont = fontName },
                    label = { Text(fontName, maxLines = 1) }
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { fontPicker.launch(arrayOf("font/*", "application/octet-stream")) }) {
                Text("Import font")
            }
            if (selectedFont != "System Default") {
                OutlinedButton(onClick = {
                    if (fontManager.deleteFont(selectedFont)) {
                        fonts = fontManager.fontNames()
                        selectedFont = "System Default"
                    }
                }) {
                    Text("Delete")
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        Text("Fonts saved to: ${fontManager.fontDirPath()} — 10MB max per font, offline-first, private, stays on device", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
            settingsHeaderTitle("Buttons on the playing screen"),
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(top = 16.dp, bottom = 2.dp),
        )
        listOf(
            "back" to "Back",
            "speed" to "Playback speed",
            "subtitles" to "Subtitles and Audio",
            "library" to "Library",
            "chapters" to "Chapters",
            "info" to "Playback info",
            "lock" to "Lock controls",
            "aspect" to "Aspect ratio",
            "sleep" to "Sleep timer",
            "audioonly" to "Audio-only mode",
        ).forEach { (id, label) ->
            PrefToggle(
                label = label,
                description = "",
                checked = settings.playerControls.contains(id),
            ) { enabled ->
                scope.launch { AppContainer.settings.setPlayerControl(id, enabled) }
            }
        }

    }
}

@Composable
