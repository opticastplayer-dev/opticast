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
internal fun GestureSettingsCard(settings: AppSettings, scope: CoroutineScope) {
    SettingsCard(icon = Icons.Filled.TouchApp, title = "Gestures") {

        Text(
            "Double-tap seek distance",
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(top = 8.dp),
        )
        Spacer(Modifier.height(6.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            listOf(5, 10, 15, 30).forEach { seconds ->
                FilterChip(
                    selected = settings.doubleTapSeekSec == seconds,
                    onClick = {
                        scope.launch { AppContainer.settings.setDoubleTapSeekSec(seconds) }
                    },
                    label = { Text("${seconds}s") },
                )
            }
        }

        PrefToggle(
            label = "Hold to fast-forward",
            description = "Press & hold the video to temporarily speed up, release to return.",
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

        PrefToggle(
            label = "Swipe to seek",
            description = "Horizontal swipe scrubs through the video.",
            checked = settings.gestureSeek,
        ) { enabled -> scope.launch { AppContainer.settings.setGestureSeek(enabled) } }
        PrefToggle(
            label = "Swipe for volume & brightness",
            description = "Vertical swipe: volume on the right side, screen brightness on the left.",
            checked = settings.gestureVolumeBrightness,
        ) { enabled -> scope.launch { AppContainer.settings.setGestureVolumeBrightness(enabled) } }

        Spacer(Modifier.height(10.dp))
    }
}

@Composable
internal fun BehaviourSettingsCard(settings: AppSettings, scope: CoroutineScope) {
    SettingsCard(icon = Icons.Filled.PlayArrow, title = "Playback behaviour") {
        PrefToggle(
            label = "Start in audio-only mode",
            description = "Start with sound only. Turn the picture back on in the player.",
            checked = settings.audioOnlyByDefault,
        ) { enabled -> scope.launch { AppContainer.settings.setAudioOnlyByDefault(enabled) } }


        PrefToggle(
            label = "Start in landscape",
            description = "Pressing play rotates straight to landscape fullscreen — no waiting, no taps.",
            checked = settings.autoLandscape,
        ) { enabled -> scope.launch { AppContainer.settings.setAutoLandscape(enabled) } }
        PrefToggle(
            label = "Keep screen on",
            description = "Prevents the display from sleeping while a video is open.",
            checked = settings.keepScreenOn,
        ) { enabled -> scope.launch { AppContainer.settings.setKeepScreenOn(enabled) } }
        PrefToggle(
            label = "Preserve voice pitch at speed",
            description = "Keeps voices natural at 1.25×+ instead of sounding chipmunk-like.",
            checked = settings.preservePitch,
        ) { enabled -> scope.launch { AppContainer.settings.setPreservePitch(enabled) } }
        Text(
            "Resume playback is always on. To start over, use Restart from beginning on the video's information page.",
            style = MaterialTheme.typography.bodySmall,
        )
        PrefToggle(
            label = "Auto-play next episode",
            description = "Counts down 5 seconds after an episode ends, then plays the next one.",
            checked = settings.autoNextEpisode,
        ) { enabled -> scope.launch { AppContainer.settings.setAutoNextEpisode(enabled) } }

    }
}

@Composable
internal fun SoundSettingsCard(settings: AppSettings, scope: CoroutineScope) {
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

        Text(
            "Audio preset",
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(top = 14.dp),
        )
        Spacer(Modifier.height(6.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AudioPreset.entries.forEach { preset ->
                FilterChip(
                    selected = settings.audioPreset == preset.id,
                    onClick = { scope.launch { AppContainer.settings.setAudioPreset(preset.id) } },
                    label = { Text(preset.label) },
                )
            }
        }
        Text(
            AudioPreset.forId(settings.audioPreset).blurb +
                " Applied to playback immediately; needs no restart.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(6.dp))
        PrefToggle(
            label = "Dialogue boost",
            description = "Adds presence to speech so quiet dialogue stays clear at low " +
                "volume. Works with any preset, and stacks with the audio boost above.",
            checked = settings.dialogueBoost,
        ) { enabled -> scope.launch { AppContainer.settings.setDialogueBoost(enabled) } }
    }
}

@Composable
internal fun TrackLanguagePicker(label: String, selected: String, options: List<Pair<String, String>>, onSelect: (String) -> Unit) {
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
internal fun LanguageDropdownCard(settings: AppSettings, scope: CoroutineScope) {
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

// -------------------------------------------------------------- naming guide --

@Composable
internal fun NamingGuideCard() {
    val query = LocalSettingsQuery.current
    if (!settingsSearchMatches("File naming help", query)) return

    var expanded by rememberSaveable { mutableStateOf(false) }
    var searchCollapsed by remember(query) { mutableStateOf(false) }
    val showContent = if (query.isNotBlank()) !searchCollapsed else expanded
    val chevron by animateFloatAsState(
        targetValue = if (showContent) 180f else 0f,
        animationSpec = tween(260, easing = FastOutSlowInEasing),
        label = "namingChevron",
    )
    Box(modifier = settingsCategoryModifier("File naming help")) {
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
                    Icon(Icons.Filled.Description, null, Modifier.size(22.dp), tint = MaterialTheme.colorScheme.primary)
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(settingsHeaderTitle("File naming help"), style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Movie and episode filename examples",
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
                    NamingSection(
                        "Movies",
                        "Title plus year works best — spaces, dots or underscores all work:",
                        listOf(
                            "Dune Part Two (2024).mkv",
                            "Dune.Part.Two.2024.2160p.WEB.mkv",
                        ),
                    )
                    NamingSection(
                        "TV shows",
                        "Use the season & episode code S01E01 — the most reliable pattern there is:",
                        listOf(
                            "The Bear S02E05.mkv",
                            "The.Bear.S02E05.1080p.WEB.mkv",
                        ),
                        "Folders are optional but tidy:  TV Shows/The Bear/Season 2/The Bear S02E05.mkv",
                    )
                    NamingSection(
                        "Anime",
                        "Fansub-style names are recognised too (matched via AniList):",
                        listOf("[SubGroup] Frieren - 01 [1080p].mkv"),
                    )
                    Text(
                        "Tips",
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                    listOf(
                        "·  Add the year to movie titles — it removes ambiguity.",
                        "·  Keep one movie per file; split parts are treated separately.",
                        "·  Avoid cryptic names like \"movie_final2.mp4\" — they can't be matched.",
                        "·  Wrong match? Long-press the poster → Find metadata to fix it.",
                    ).forEach { tip ->
                        Text(
                            tip,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 1.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun NamingSection(heading: String, blurb: String, examples: List<String>, extra: String? = null) {
    Text(settingsHeaderTitle(heading), style = MaterialTheme.typography.labelLarge)
    Text(
        blurb,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(4.dp))
    examples.forEach { example ->
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp),
        ) {
            Text(
                example,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            )
        }
    }
    if (extra != null) {
        Text(
            extra,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 3.dp),
        )
    }
    Spacer(Modifier.height(10.dp))
}

@Composable
internal fun themePreviewColor(theme: String): Color = when (theme) {
    "ocean" -> Color(0xFF6FD3FF)
    "midnight" -> Color(0xFFEEC177)
    else -> com.opticast.player.ui.theme.OptiCastBrandColorScheme.primary
}

internal fun settingsTypography(base: androidx.compose.material3.Typography): androidx.compose.material3.Typography {
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
internal fun SettingsGroup(title: String, content: @Composable () -> Unit) {
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
internal fun settingsCategoryModifier(title: String): Modifier {
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
