package com.opticast.player.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext

@Composable
internal fun LegalNoticesButton() {
    // Legal notices moved to website per user request.
    val ctx = LocalContext.current
    OutlinedButton(
        onClick = {
            runCatching {
                ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://opticastplayer-dev.github.io/opticast/")))
            }
        }
    ) {
        Text("Legal notices and credits")
    }
}
