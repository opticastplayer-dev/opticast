package com.opticast.player.data

import kotlinx.serialization.Serializable

@Serializable
data class GestureConfig(
    val leftSwipe: String = "brightness", // brightness, volume, none
    val rightSwipe: String = "volume", // brightness, volume, none
    val doubleTapLeft: String = "rewind", // rewind, none
    val doubleTapRight: String = "forward", // forward, none
    val doubleTapCenter: String = "play_pause", // play_pause, none
    val doubleTapSeekSec: Int = 10,
    val swipeSeekEnabled: Boolean = true,
    val pinchZoomEnabled: Boolean = true
) {
    companion object {
        fun default() = GestureConfig()
        
        fun fromString(value: String): GestureConfig {
            return try {
                kotlinx.serialization.json.Json.decodeFromString<GestureConfig>(value)
            } catch (_: Exception) {
                default()
            }
        }
    }

    fun toJson(): String {
        return try {
            kotlinx.serialization.json.Json.encodeToString(serializer(), this)
        } catch (_: Exception) {
            "{}"
        }
    }
}

object GestureDefaults {
    const val SWIPE_BRIGHTNESS = "brightness"
    const val SWIPE_VOLUME = "volume"
    const val SWIPE_NONE = "none"
    const val DOUBLE_TAP_REWIND = "rewind"
    const val DOUBLE_TAP_FORWARD = "forward"
    const val DOUBLE_TAP_PLAY_PAUSE = "play_pause"
    const val DOUBLE_TAP_NONE = "none"
}
