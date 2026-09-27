package com.opticast.player.ui.tv

import android.app.UiModeManager
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

/**
 * True on Android TV / Fire TV / Google TV boxes.
 *
 * Three signals, because no single one is reliable across all launchers: the UI
 * mode (what the launcher reports), the Leanback feature flag and the legacy
 * television hardware flag. A phone or tablet matches none of them.
 */
fun isTelevision(context: Context): Boolean {
    val uiMode = runCatching {
        (context.getSystemService(Context.UI_MODE_SERVICE) as? UiModeManager)?.currentModeType
    }.getOrNull()
    if (uiMode == Configuration.UI_MODE_TYPE_TELEVISION) return true

    val pm = context.packageManager
    val leanback = runCatching { pm.hasSystemFeature(PackageManager.FEATURE_LEANBACK) }
        .getOrDefault(false)
    val television = runCatching { pm.hasSystemFeature("android.hardware.type.television") }
        .getOrDefault(false)
    return leanback || television
}

/**
 * A focusable TV card body: the shared focus treatment for anything a remote
 * can land on.
 *
 * A remote has no pointer, so the only way to show where the user is, is to
 * react to focus - the card lifts slightly and takes a themed outline. This is
 * applied only from the TV screens, so the phone layout is untouched.
 */
@Composable
fun TvFocusableCard(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(18.dp),
    focusedScale: Float = 1.08f,
    onFocusChanged: (Boolean) -> Unit = {},
    content: @Composable BoxScope.() -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (focused) focusedScale else 1f,
        animationSpec = tween(160),
        label = "tvCardScale",
    )
    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(shape)
            .border(
                width = if (focused) 2.dp else 1.dp,
                color = if (focused) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.30f)
                },
                shape = shape,
            )
            // hasFocus (not isFocused): the card wraps the caller's focusable
            // node, so it must also light up when a CHILD holds the focus.
            .onFocusChanged { state ->
                if (state.hasFocus != focused) {
                    focused = state.hasFocus
                    onFocusChanged(state.hasFocus)
                }
            },
        content = content,
    )
}
