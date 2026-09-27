package com.opticast.player.player

const val AGGRESSIVE_STRETCH = 100
fun surfaceResizeMode(mode: Int): Int = if (mode == AGGRESSIVE_STRETCH) 3 else mode
fun aggressiveVideoScale(mode: Int): Float = if (mode == AGGRESSIVE_STRETCH) 1.75f else 1f
fun resolvedCaptionStyle(stored: Int?): Int = when (stored) { 2, 3 -> stored; else -> 1 }

data class NativePresentation(val videoScale: Float = 1f, val captionStyle: Int = 1, val captionScale: Float = 1f)
fun nativePresentationOptions(p: NativePresentation): Map<String,String> = mapOf(
    // PlayerView owns fit/crop/stretch. Do not letterbox a second time inside mpv.
    "keepaspect" to "no",
    "video-scale-x" to p.videoScale.toString(), "video-scale-y" to p.videoScale.toString(),
    "sub-ass-override" to "force", "sub-scale" to p.captionScale.toString(),
    "sub-color" to if(p.captionStyle == 2) "#FFFFFF00" else "#FFFFFFFF",
    "sub-back-color" to if(p.captionStyle == 1) "#B3000000" else "#00000000",
    "sub-border-style" to if(p.captionStyle == 1) "background-box" else "outline-and-shadow",
    "sub-outline-color" to "#FF000000",
    "sub-outline-size" to if(p.captionStyle == 3) "0" else "2",
    "sub-shadow-offset" to "0",
)
