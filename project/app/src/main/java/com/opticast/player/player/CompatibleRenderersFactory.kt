package com.opticast.player.player

import android.content.Context
import android.media.MediaFormat
import android.os.Build
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.mediacodec.MediaCodecAdapter

/** Same Media3 renderers and adapter defaults; no extra engine or decoder preference changes. */
internal class CompatibleRenderersFactory(context: Context) : DefaultRenderersFactory(context) {
    override fun getCodecAdapterFactory(): MediaCodecAdapter.Factory {
        val delegate = super.getCodecAdapterFactory()
        return MediaCodecAdapter.Factory { configuration ->
            val mediaFormat = configuration.mediaFormat
            val profile = if(mediaFormat.containsKey(MediaFormat.KEY_PROFILE)) mediaFormat.getInteger(MediaFormat.KEY_PROFILE) else null
            // Media3 1.7.1 can copy a DV profile enum into a fallback HEVC MediaFormat.
            // Let the HEVC decoder infer its profile from unchanged codec initialization data.
            if(Build.VERSION.SDK_INT >= 29 && shouldClearDolbyProfileForHevc(
                    Build.VERSION.SDK_INT, configuration.format.sampleMimeType, configuration.format.codecs,
                    mediaFormat.getString(MediaFormat.KEY_MIME), profile,
                    configuration.crypto != null || configuration.format.drmInitData != null || configuration.codecInfo.secure,
                )) {
                mediaFormat.removeKey(MediaFormat.KEY_PROFILE)
            }
            delegate.createAdapter(configuration)
        }
    }
}
