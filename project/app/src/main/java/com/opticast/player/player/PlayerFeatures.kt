package com.opticast.player.player

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

// A-B Loop feature
data class ABLoop(
    val pointA: Long? = null,
    val pointB: Long? = null,
    val isActive: Boolean = false
) {
    val isSet: Boolean get() = pointA != null && pointB != null
}

class ABLoopManager {
    var abLoop by mutableStateOf(ABLoop())
        private set

    fun setPointA(position: Long) {
        abLoop = abLoop.copy(pointA = position, isActive = false)
    }

    fun setPointB(position: Long) {
        abLoop = abLoop.copy(pointB = position, isActive = true)
    }

    fun clear() {
        abLoop = ABLoop()
    }

    fun shouldLoop(position: Long): Long? {
        val loop = abLoop
        if (!loop.isActive || loop.pointA == null || loop.pointB == null) return null
        if (position >= loop.pointB) {
            return loop.pointA
        }
        return null
    }
}

// Auto Crop feature
class AutoCropManager {
    var isEnabled by mutableStateOf(false)
        private set

    fun toggle() {
        isEnabled = !isEnabled
    }

    fun enable() {
        isEnabled = true
    }

    fun disable() {
        isEnabled = false
    }
}

// Dialogue Boost
class DialogueBoostManager {
    var isEnabled by mutableStateOf(false)
        private set
    var level by mutableStateOf(1) // 1-3

    fun toggle() {
        isEnabled = !isEnabled
    }

    fun setLevel(newLevel: Int) {
        level = newLevel.coerceIn(1, 3)
        isEnabled = true
    }

    fun getFilter(): String? {
        return if (isEnabled) {
            when (level) {
                1 -> "dynaudnorm=f=50:g=5"
                2 -> "dynaudnorm=f=100:g=10"
                3 -> "dynaudnorm=f=150:g=15"
                else -> "dynaudnorm"
            }
        } else null
    }
}

// Volume Normalization
class VolumeNormManager {
    var isEnabled by mutableStateOf(false)
        private set
    private val perVideoVolume = mutableMapOf<Long, Float>()

    fun toggle() {
        isEnabled = !isEnabled
    }

    fun setVolumeForVideo(videoId: Long, volume: Float) {
        perVideoVolume[videoId] = volume.coerceIn(0f, 2f)
    }

    fun getVolumeForVideo(videoId: Long): Float {
        return perVideoVolume[videoId] ?: 1f
    }

    fun getFilter(): String? {
        return if (isEnabled) "loudnorm=I=-16:TP=-1.5:LRA=11" else null
    }
}

// Orientation modes
enum class OrientationMode {
    AUTO, PORTRAIT, LANDSCAPE, SENSOR, LOCKED;

    companion object {
        fun fromString(value: String): OrientationMode {
            return when (value.lowercase()) {
                "portrait" -> PORTRAIT
                "landscape" -> LANDSCAPE
                "sensor" -> SENSOR
                "locked" -> LOCKED
                else -> AUTO
            }
        }
    }
}
