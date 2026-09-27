package com.opticast.player.external

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.opticast.player.data.AppContainer
import com.opticast.player.player.PlayerActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Only the video intent boundary is exported; arbitrary internal playback extras are ignored. */
class ExternalVideoActivity : ComponentActivity() {
    private var request: Job? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val frame = android.widget.FrameLayout(this)
        frame.addView(android.widget.ProgressBar(this), android.widget.FrameLayout.LayoutParams(
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
            android.view.Gravity.CENTER))
        setContentView(frame)
        open(intent)
    }
    override fun onNewIntent(intent: Intent) { super.onNewIntent(intent); setIntent(intent); open(intent) }
    @Suppress("DEPRECATION")
    private fun open(incoming: Intent) {
        request?.cancel()
        val uri = runCatching {
            when (incoming.action) {
                Intent.ACTION_VIEW -> incoming.data
                Intent.ACTION_SEND -> incoming.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
                    ?: incoming.clipData?.takeIf { it.itemCount == 1 }?.getItemAt(0)?.uri
                else -> null
            }
        }.getOrNull()
        if (uri == null || uri.toString().length > 16384) { reject(); return }
        request = lifecycleScope.launch {
            val resolved = withContext(Dispatchers.IO) {
                runCatching {
                    val type = incoming.type ?: if (uri.scheme == "content") contentResolver.getType(uri) else null
                    var title = uri.lastPathSegment?.take(200) ?: "Video"
                    if (uri.scheme == "content") {
                        runCatching {
                            contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
                                if (it.moveToFirst()) title = it.getString(0)?.take(200) ?: title
                            }
                        }
                    }
                    require(supportedExternalVideo(uri.scheme, type, title))
                    if (uri.scheme in listOf("content", "file")) {
                        contentResolver.openFileDescriptor(uri, "r")?.use { } ?: error("No read access")
                    }
                    val local = runCatching { AppContainer.mediaScanner.inventory.available().firstOrNull { it.uri == uri.toString() } }.getOrNull()
                    title to local?.id
                }.getOrNull()
            }
            if (resolved == null) { reject(); return@launch }
            if (uri.scheme == "content") ExternalVideoGrants.retain(this@ExternalVideoActivity, uri, incoming.flags)
            val next = resolved.second?.let { PlayerActivity.intent(this@ExternalVideoActivity, it) }
                ?: Intent(this@ExternalVideoActivity, PlayerActivity::class.java)
                    .putExtra(PlayerActivity.EXTRA_REMOTE_URI, uri.toString())
                    .putExtra(PlayerActivity.EXTRA_REMOTE_TITLE, resolved.first)
            next.data = uri
            next.clipData = ClipData.newRawUri("Video", uri)
            next.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            try { PlayerActivity.launch(this@ExternalVideoActivity, next) }
            catch (_: Exception) { Toast.makeText(this@ExternalVideoActivity, "Unable to open this video", Toast.LENGTH_LONG).show() }
            finish()
        }
    }
    private fun reject() {
        Toast.makeText(this, "Cannot open this video. Select a readable video file and allow access.", Toast.LENGTH_LONG).show()
        finish()
    }
}

/** Keep temporary content grants alive for PiP reuse; revoke only self-grants after playback ends. */
internal object ExternalVideoGrants {
    private val temporary = linkedSetOf<Uri>()
    @Synchronized fun retain(context: Context, uri: Uri, flags: Int) {
        if (flags and Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION != 0) {
            runCatching { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
        }
        if (context.contentResolver.persistedUriPermissions.any { it.uri == uri && it.isReadPermission }) return
        runCatching {
            context.grantUriPermission(context.packageName, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            temporary += uri
        }
    }
    @Synchronized fun release(context: Context) {
        temporary.forEach { uri -> runCatching { context.revokeUriPermission(context.packageName, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) } }
        temporary.clear()
    }
}
