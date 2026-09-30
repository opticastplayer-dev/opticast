package com.opticast.player.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

data class AppSettings(
    val tmdbApiKey: String = "",
    val openSubtitlesApiKey: String = "",
    val subtitleLanguages: List<String> = listOf("en"),
    val captionScale: Float = 1f,
    val captionStyle: Int = 1,
    val autoSubtitles: Boolean = false,
    val excludedFolders: List<String> = emptyList(),
    val omdbApiKey: String = "",
    val fanartApiKey: String = "",
    val subdlApiKey: String = "",
    val useDeviceColors: Boolean = false,
    val useExternalPlayer: Boolean = false,
    // Playback & control
    val doubleTapSeekSec: Int = 10,
    val holdToSpeed: Boolean = true,
    val holdSpeedFactor: Float = 2f,
    val gestureSeek: Boolean = true,
    val gestureVolumeBrightness: Boolean = true,
    val keepScreenOn: Boolean = true,
    val autoLandscape: Boolean = true,
    val preservePitch: Boolean = true,
    val audioBoostPct: Int = 100,
    val playbackEngine: String = "mpv", // mpv first with Media3 fallback | media3 only
    val engineMemoryEnabled: Boolean = true,
    val localBufferTrial: Boolean = true,
    val preferredAudioLanguage: String = "",
    val embeddedSubtitleLanguage: String = "",
    val embeddedSubtitleMode: String = "default",
    val avoidCommentary: Boolean = true,
    val audioPreset: String = "flat",
    val dialogueBoost: Boolean = false,
    val autoNextEpisode: Boolean = true,
    val resumePlayback: Boolean = true,
    // Progress bar look, and which buttons the playing screen shows.
    val showChapterStamps: Boolean = false,
    val progressBarStyle: String = "thick", // thick | gradient | hidden
    val playerControls: List<String> = listOf(
        "back", "speed", "subtitles", "library", "chapters", "info", "lock", "aspect",
        "sleep", "audioonly",
    ),
    /** Start every video with the picture off: saves data and battery. */
    // Appearance / library
    val appTheme: String = "cast", // cast | midnight | ocean
    val libraryGrid: String = DEFAULT_LIBRARY_GRID, // compact | medium | comfortable
    /**
     * Budget-device mode: locks the display to its standard refresh rate
     * (usually 60 Hz) instead of the highest available one. Off by default so
     * the fluidity behaviour is unchanged unless the user asks for it.
     */
    val performanceMode: Boolean = false,
    /**
     * Keeps mobile data usage down: smaller posters/backdrops, no HD Fanart.tv
     * artwork, and bulk artwork prefetching only on unmetered connections.
     * On by default, because artwork should never quietly burn someone's data.
     */
    val dataSaverArtwork: Boolean = true,
)

private val Context.settingsDataStore: DataStore<Preferences> by
    preferencesDataStore(name = "opticast_settings")

class SettingsRepository(private val context: Context) {

    private val tmdbKey = stringPreferencesKey("tmdb_api_key")
    private val osKey = stringPreferencesKey("opensubtitles_api_key")
    private val langsKey = stringPreferencesKey("subtitle_languages")
    private val captionScaleKey = floatPreferencesKey("caption_scale")
    private val captionStyleKey = intPreferencesKey("caption_style")
    private val autoSubsKey = booleanPreferencesKey("auto_subtitles")
    private val excludedKey = stringPreferencesKey("excluded_folders")
    private val omdbKey = stringPreferencesKey("omdb_api_key")
    private val fanartKey = stringPreferencesKey("fanart_api_key")
    private val subdlKey = stringPreferencesKey("subdl_api_key")
    private val deviceColorsKey = booleanPreferencesKey("use_device_colors")
    private val performanceModeKey = booleanPreferencesKey("performance_mode")
    private val dataSaverKey = booleanPreferencesKey("data_saver_artwork")
    private val externalPlayerKey = booleanPreferencesKey("use_external_player")
    private val doubleTapSeekKey = intPreferencesKey("double_tap_seek_sec")
    private val playbackEngineKey = stringPreferencesKey("playback_engine_v2")
    private val audioLanguageKey = stringPreferencesKey("preferred_audio_language")
    private val embeddedLanguageKey = stringPreferencesKey("embedded_subtitle_language")
    private val embeddedModeKey = stringPreferencesKey("embedded_subtitle_mode")
    private val avoidCommentaryKey = booleanPreferencesKey("avoid_commentary")
    private val localBufferTrialKey = booleanPreferencesKey("local_buffer_trial")
    private val engineMemoryKey = booleanPreferencesKey("engine_memory")
    private val audioPresetKey = stringPreferencesKey("audio_preset")
    private val dialogueBoostKey = booleanPreferencesKey("dialogue_boost")
    private val holdToSpeedKey = booleanPreferencesKey("hold_to_speed")
    private val holdSpeedFactorKey = floatPreferencesKey("hold_speed_factor")
    private val gestureSeekKey = booleanPreferencesKey("gesture_seek")
    private val gestureVolKey = booleanPreferencesKey("gesture_volume_brightness")
    private val keepScreenOnKey = booleanPreferencesKey("keep_screen_on")
    private val autoLandscapeKey = booleanPreferencesKey("auto_landscape")
    private val preservePitchKey = booleanPreferencesKey("preserve_pitch")
    private val audioBoostKey = intPreferencesKey("audio_boost_pct")
    private val autoNextKey = booleanPreferencesKey("auto_next_episode")
    private val resumeKey = booleanPreferencesKey("resume_playback")
    private val chapterStampsKey = booleanPreferencesKey("show_chapter_stamps")
    private val progressBarStyleKey = stringPreferencesKey("progress_bar_style")
    private val playerControlsKey = stringSetPreferencesKey("player_controls")
    private val themeKey = stringPreferencesKey("app_theme")
    private val gridKey = stringPreferencesKey("library_grid")

    val settings: Flow<AppSettings> = context.settingsDataStore.data.map { prefs ->
        AppSettings(
            tmdbApiKey = prefs[tmdbKey].orEmpty(),
            openSubtitlesApiKey = prefs[osKey].orEmpty(),
            subtitleLanguages = prefs[langsKey]
                ?.split(",")
                ?.filter { it.isNotBlank() }
                ?.takeIf { it.isNotEmpty() }
                ?: listOf("en"),
            captionScale = prefs[captionScaleKey] ?: 1f,
            captionStyle = com.opticast.player.player.resolvedCaptionStyle(prefs[captionStyleKey]),
            autoSubtitles = prefs[autoSubsKey] ?: false,
            excludedFolders = prefs[excludedKey]
                ?.split("|")
                ?.filter { it.isNotBlank() }
                ?: emptyList(),
            omdbApiKey = prefs[omdbKey] ?: "",
            fanartApiKey = prefs[fanartKey] ?: "",
            subdlApiKey = prefs[subdlKey] ?: "",
            useDeviceColors = prefs[deviceColorsKey] ?: false,
            performanceMode = prefs[performanceModeKey] ?: false,
            dataSaverArtwork = prefs[dataSaverKey] ?: true,
            useExternalPlayer = prefs[externalPlayerKey] ?: false,
            doubleTapSeekSec = prefs[doubleTapSeekKey] ?: 10,
            holdToSpeed = prefs[holdToSpeedKey] ?: true,
            holdSpeedFactor = prefs[holdSpeedFactorKey] ?: 2f,
            gestureSeek = prefs[gestureSeekKey] ?: true,
            gestureVolumeBrightness = prefs[gestureVolKey] ?: true,
            keepScreenOn = prefs[keepScreenOnKey] ?: true,
            autoLandscape = prefs[autoLandscapeKey] ?: true,
            preservePitch = prefs[preservePitchKey] ?: true,
            audioBoostPct = prefs[audioBoostKey] ?: 100,
            // New preference generation intentionally adopts the requested mpv-first default
            // for upgrades too. New explicit Media3 choices remain persistent.
            playbackEngine = if (prefs[playbackEngineKey] == "media3") "media3" else "mpv",
            engineMemoryEnabled = prefs[engineMemoryKey] ?: true,
            localBufferTrial = prefs[localBufferTrialKey] ?: true,
            preferredAudioLanguage = prefs[audioLanguageKey].orEmpty(),
            embeddedSubtitleLanguage = prefs[embeddedLanguageKey].orEmpty(),
            embeddedSubtitleMode = prefs[embeddedModeKey]?.takeIf { it in listOf("default", "forced", "full", "off") } ?: "default",
            avoidCommentary = prefs[avoidCommentaryKey] ?: true,
            audioPreset = prefs[audioPresetKey] ?: "flat",
            dialogueBoost = prefs[dialogueBoostKey] ?: false,
            autoNextEpisode = prefs[autoNextKey] ?: true,
            resumePlayback = prefs[resumeKey] ?: true,
            showChapterStamps = prefs[chapterStampsKey] ?: false,
            progressBarStyle = resolvedProgressStyle(prefs[progressBarStyleKey]),
            playerControls = resolvedPlayerControls(
                prefs[playerControlsKey]
                    ?: setOf(
                        "back", "speed", "subtitles", "library", "chapters", "info",
                        "lock", "aspect", "sleep", "audioonly",
                    )
                ),
            appTheme = resolvedAppTheme(prefs[themeKey]),
            libraryGrid = resolvedLibraryGrid(prefs[gridKey]),
        )
    }

    suspend fun setPreferredAudioLanguage(value: String) { context.settingsDataStore.edit { it[audioLanguageKey] = value } }
    suspend fun setEmbeddedSubtitleLanguage(value: String) { context.settingsDataStore.edit { it[embeddedLanguageKey] = value } }
    suspend fun setEmbeddedSubtitleMode(value: String) { require(value in listOf("default", "forced", "full", "off")); context.settingsDataStore.edit { it[embeddedModeKey] = value } }
    suspend fun setAvoidCommentary(value: Boolean) { context.settingsDataStore.edit { it[avoidCommentaryKey] = value } }

    suspend fun current(): AppSettings = settings.first()

    suspend fun setTmdbApiKey(value: String) {
        context.settingsDataStore.edit { it[tmdbKey] = value.trim() }
    }

    suspend fun setOpenSubtitlesApiKey(value: String) {
        context.settingsDataStore.edit { it[osKey] = value.trim() }
    }

    suspend fun setSubtitleLanguages(languages: List<String>) {
        context.settingsDataStore.edit { it[langsKey] = languages.joinToString(",") }
    }

    suspend fun setCaptionScale(scale: Float) {
        context.settingsDataStore.edit { it[captionScaleKey] = scale }
    }

    suspend fun setCaptionStyle(style: Int) {
        context.settingsDataStore.edit { it[captionStyleKey] = style }
    }

    suspend fun setAutoSubtitles(enabled: Boolean) {
        context.settingsDataStore.edit { it[autoSubsKey] = enabled }
    }

    suspend fun setExcludedFolders(folders: List<String>) {
        context.settingsDataStore.edit { it[excludedKey] = folders.joinToString("|") }
    }

    suspend fun setOmdbApiKey(key: String) {
        context.settingsDataStore.edit { it[omdbKey] = key.trim() }
    }

    suspend fun setFanartApiKey(key: String) {
        context.settingsDataStore.edit { it[fanartKey] = key.trim() }
    }

    suspend fun setSubdlApiKey(key: String) {
        context.settingsDataStore.edit { it[subdlKey] = key.trim() }
    }

    /**
     * Every stored preference as plain strings, for the backup file. Read
     * straight from DataStore rather than from the typed model, so a future
     * setting is backed up without anyone remembering to add it here.
     */
    fun exportSettings(): Map<String, String> = runCatching {
        kotlinx.coroutines.runBlocking {
            context.settingsDataStore.data.first().asMap().entries.associate { (key, value) ->
                key.name to when (value) {
                    is Set<*> -> value.joinToString(SEPARATOR) { it.toString() }
                    else -> value.toString()
                }
            }
        }
    }.getOrDefault(emptyMap())

    /**
     * Writes a backup's preferences back. Each value is written through the key
     * that is already present, so its stored type is preserved - a string never
     * lands in an integer slot.
     */
    fun importSettings(values: Map<String, String>) {
        if (values.isEmpty()) return
        runCatching {
            kotlinx.coroutines.runBlocking {
                context.settingsDataStore.edit { prefs ->
                    // New playback preferences also restore on a clean installation with no existing key.
                    listOf(audioLanguageKey, embeddedLanguageKey).forEach { key -> values[key.name]?.let { prefs[key] = it } }
                    values[embeddedModeKey.name]?.takeIf { it in listOf("default", "forced", "full", "off") }?.let { prefs[embeddedModeKey] = it }
                    listOf(engineMemoryKey, avoidCommentaryKey, localBufferTrialKey).forEach { key -> values[key.name]?.toBooleanStrictOrNull()?.let { prefs[key] = it } }
                    prefs.asMap().forEach { (key, current) ->
                        val raw = values[key.name] ?: return@forEach
                        when (current) {
                            is Boolean -> (key as? Preferences.Key<Boolean>)
                                ?.let { prefs[it] = raw.toBoolean() }
                            is Int -> (key as? Preferences.Key<Int>)
                                ?.let { prefs[it] = raw.toIntOrNull() ?: current }
                            is Long -> (key as? Preferences.Key<Long>)
                                ?.let { prefs[it] = raw.toLongOrNull() ?: current }
                            is Float -> (key as? Preferences.Key<Float>)
                                ?.let { prefs[it] = raw.toFloatOrNull() ?: current }
                            is String -> (key as? Preferences.Key<String>)
                                ?.let { prefs[it] = raw }
                            is Set<*> -> (key as? Preferences.Key<Set<String>>)
                                ?.let { prefs[it] = raw.split(SEPARATOR).toSet() }
                        }
                    }
                }
            }
        }
    }

    private companion object {
        const val SEPARATOR = "\u0001"
    }

    suspend fun setDataSaverArtwork(enabled: Boolean) {
        context.settingsDataStore.edit { it[dataSaverKey] = enabled }
    }

    suspend fun setPerformanceMode(enabled: Boolean) {
        context.settingsDataStore.edit { it[performanceModeKey] = enabled }
    }

    suspend fun setUseDeviceColors(enabled: Boolean) {
        context.settingsDataStore.edit { it[deviceColorsKey] = enabled }
    }

    suspend fun setUseExternalPlayer(enabled: Boolean) {
        context.settingsDataStore.edit { it[externalPlayerKey] = enabled }
    }

    suspend fun setLocalBufferTrial(enabled: Boolean) { context.settingsDataStore.edit { it[localBufferTrialKey] = enabled } }

    suspend fun setEngineMemoryEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[engineMemoryKey] = enabled }
    }

    suspend fun setPlaybackEngine(engine: String) {
        context.settingsDataStore.edit { it[playbackEngineKey] = engine }
    }

    suspend fun setAudioPreset(id: String) {
        context.settingsDataStore.edit { it[audioPresetKey] = id }
    }

    suspend fun setDialogueBoost(enabled: Boolean) {
        context.settingsDataStore.edit { it[dialogueBoostKey] = enabled }
    }

    suspend fun setDoubleTapSeekSec(seconds: Int) {
        context.settingsDataStore.edit { it[doubleTapSeekKey] = seconds }
    }

    suspend fun setHoldToSpeed(enabled: Boolean) {
        context.settingsDataStore.edit { it[holdToSpeedKey] = enabled }
    }

    suspend fun setHoldSpeedFactor(factor: Float) {
        context.settingsDataStore.edit { it[holdSpeedFactorKey] = factor }
    }

    suspend fun setGestureSeek(enabled: Boolean) {
        context.settingsDataStore.edit { it[gestureSeekKey] = enabled }
    }

    suspend fun setGestureVolumeBrightness(enabled: Boolean) {
        context.settingsDataStore.edit { it[gestureVolKey] = enabled }
    }

    suspend fun setKeepScreenOn(enabled: Boolean) {
        context.settingsDataStore.edit { it[keepScreenOnKey] = enabled }
    }

    suspend fun setAutoLandscape(enabled: Boolean) {
        context.settingsDataStore.edit { it[autoLandscapeKey] = enabled }
    }

    suspend fun setPreservePitch(enabled: Boolean) {
        context.settingsDataStore.edit { it[preservePitchKey] = enabled }
    }

    suspend fun setAudioBoostPct(pct: Int) {
        context.settingsDataStore.edit { it[audioBoostKey] = pct }
    }

    suspend fun setAutoNextEpisode(enabled: Boolean) {
        context.settingsDataStore.edit { it[autoNextKey] = enabled }
    }

    suspend fun setShowChapterStamps(show: Boolean) {
        context.settingsDataStore.edit { it[chapterStampsKey] = show }
    }

    suspend fun setProgressBarStyle(style: String) {
        context.settingsDataStore.edit { it[progressBarStyleKey] = style }
    }

    /** Adds or removes one button from the playing screen. */
    suspend fun setPlayerControl(id: String, enabled: Boolean) {
        context.settingsDataStore.edit { prefs ->
            val current: MutableSet<String> = prefs[playerControlsKey]?.let { resolvedPlayerControls(it).toMutableSet() }
                ?: mutableSetOf(
                    "back", "speed", "subtitles", "library", "chapters", "info",
                    "lock", "aspect", "sleep", "audioonly",
                )
            val resolvedId = if (id == "audio") "library" else id
            if (enabled) current.add(resolvedId) else current.remove(resolvedId)
            prefs[playerControlsKey] = current
        }
    }

    suspend fun setResumePlayback(enabled: Boolean) {
        context.settingsDataStore.edit { it[resumeKey] = enabled }
    }

    suspend fun setAppTheme(theme: String) {
        context.settingsDataStore.edit { it[themeKey] = theme }
    }

    suspend fun setLibraryGrid(grid: String) {
        context.settingsDataStore.edit { it[gridKey] = grid }
    }
}
