package com.opticast.player.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color

// A cinematic, Infuse-inspired dark palette: pitch-black AMOLED surfaces
// with a warm amber primary accent.
val OptiCastDarkColorScheme = darkColorScheme(
    primary = Color(0xFFEEC177),
    onPrimary = Color(0xFF2B1D00),
    primaryContainer = Color(0xFF4A3600),
    onPrimaryContainer = Color(0xFFFFE0A3),
    secondary = Color(0xFFD6C5A6),
    onSecondary = Color(0xFF231B0C),
    secondaryContainer = Color(0xFF453A24),
    onSecondaryContainer = Color(0xFFF1E4C8),
    tertiary = Color(0xFF9BD0FF),
    onTertiary = Color(0xFF00273F),
    tertiaryContainer = Color(0xFF12456B),
    onTertiaryContainer = Color(0xFFCFE8FF),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF000000),
    onBackground = Color(0xFFE6E2DB),
    surface = Color(0xFF000000),
    onSurface = Color(0xFFE6E2DB),
    surfaceVariant = Color(0xFF191C24),
    onSurfaceVariant = Color(0xFFC6C6D0),
    surfaceContainerLowest = Color(0xFF000000),
    surfaceContainerLow = Color(0xFF0D0F12),
    surfaceContainer = Color(0xFF12141A),
    surfaceContainerHigh = Color(0xFF171A21),
    surfaceContainerHighest = Color(0xFF1D212A),
    outline = Color(0xFF575A66),
    outlineVariant = Color(0xFF262933),
    scrim = Color(0xFF000000),
    inverseSurface = Color(0xFFE6E2DB),
    inverseOnSurface = Color(0xFF1A1C20),
    inversePrimary = Color(0xFF6B5300),
)


// ---------------------------------------------------------------- themes -----

/** Deep-ocean navy with a luminous cyan accent — modern, cool, cinematic. */
val OptiCastOceanColorScheme = darkColorScheme(
    primary = Color(0xFF6FD3FF),
    onPrimary = Color(0xFF003448),
    primaryContainer = Color(0xFF0B4C69),
    onPrimaryContainer = Color(0xFFC4E9FF),
    secondary = Color(0xFFA9CBE0),
    onSecondary = Color(0xFF0E3446),
    secondaryContainer = Color(0xFF24475B),
    onSecondaryContainer = Color(0xFFDCEEFB),
    tertiary = Color(0xFFB7C6FF),
    onTertiary = Color(0xFF1E2D60),
    tertiaryContainer = Color(0xFF374579),
    onTertiaryContainer = Color(0xFFDDE1FF),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF020810),
    onBackground = Color(0xFFDFE5EE),
    surface = Color(0xFF020810),
    onSurface = Color(0xFFDFE5EE),
    surfaceVariant = Color(0xFF16222F),
    onSurfaceVariant = Color(0xFFBFC8D4),
    surfaceContainerLowest = Color(0xFF020810),
    surfaceContainerLow = Color(0xFF07111E),
    surfaceContainer = Color(0xFF0B1725),
    surfaceContainerHigh = Color(0xFF101E2D),
    surfaceContainerHighest = Color(0xFF152435),
    outline = Color(0xFF4E5E6E),
    outlineVariant = Color(0xFF233140),
    scrim = Color(0xFF000000),
    inverseSurface = Color(0xFFDFE5EE),
    inverseOnSurface = Color(0xFF14202B),
    inversePrimary = Color(0xFF00668C),
)

/** Pitch-black surfaces, violet/purple/magenta from the wordmark's “st”. */
val OptiCastBrandColorScheme = OptiCastDarkColorScheme.copy(
    primary = Color(0xFFC08AFF), onPrimary = Color(0xFF32005F),
    primaryContainer = Color(0xFF60229A), onPrimaryContainer = Color(0xFFF0DDFF),
    secondary = Color(0xFFE58CFF), onSecondary = Color(0xFF4B005C),
    secondaryContainer = Color(0xFF742483), onSecondaryContainer = Color(0xFFFBD6FF),
    tertiary = Color(0xFFAD95FF), onTertiary = Color(0xFF2D126B),
    tertiaryContainer = Color(0xFF4F3591), onTertiaryContainer = Color(0xFFEADDFF),
    onBackground = Color(0xFFF0EAF5), onSurface = Color(0xFFF0EAF5),
    inversePrimary = Color(0xFF793AB1),
)

fun opticastColorSchemeFor(theme: String): androidx.compose.material3.ColorScheme = when (theme) {
    "ocean" -> OptiCastOceanColorScheme
    "midnight" -> OptiCastDarkColorScheme
    else -> OptiCastBrandColorScheme
}
