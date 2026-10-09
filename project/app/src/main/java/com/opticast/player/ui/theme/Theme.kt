package com.opticast.player.ui.theme

import android.os.Build
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

// Expressive shape scale - everything is generously rounded, Infuse-style.
val OptiCastShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp),
)

@Composable
fun OptiCastTheme(
    theme: String = "cast",
    useDeviceColors: Boolean = false,
    content: @Composable () -> Unit,
) {
    com.opticast.player.ui.layout.AdaptiveAppWindow {
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val compact = com.opticast.player.ui.layout.useCompactLayout(configuration.screenWidthDp, configuration.screenHeightDp)
    // A cinema app is always dark; with the toggle on (Android 12+) the
    // palette is derived from the device wallpaper instead of the chosen
    // OptiCast theme.
    val context = LocalContext.current
    val colorScheme = if (useDeviceColors && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        dynamicDarkColorScheme(context)
    } else {
        opticastColorSchemeFor(theme)
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = if (compact) CompactOptiCastTypography else OptiCastTypography,
        shapes = OptiCastShapes,
        content = {
            androidx.compose.runtime.CompositionLocalProvider(
                androidx.compose.material3.LocalMinimumInteractiveComponentSize provides 48.dp,
                content = content,
            )
        },
    )
}
}
