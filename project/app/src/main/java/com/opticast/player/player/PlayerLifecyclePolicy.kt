package com.opticast.player.player

internal enum class BackgroundAction { KEEP, PAUSE, STOP }
internal fun playerBackgroundAction(finishing: Boolean, changingConfiguration: Boolean, pip: Boolean, subtitlePicker: Boolean): BackgroundAction =
    when {
        changingConfiguration || pip || subtitlePicker -> BackgroundAction.KEEP
        finishing -> BackgroundAction.STOP
        else -> BackgroundAction.PAUSE
    }
internal fun liveSeekDue(now: Long, last: Long): Boolean = now - last >= 120L

/** MediaController can omit localConfiguration; null does NOT mean subtitles changed. */
internal fun subtitleReloadNeeded(knownCount: Int?, desiredCount: Int, oldKey: String?, newKey: String): Boolean =
    (knownCount != null && knownCount != desiredCount) || (oldKey != null && oldKey != newKey)

internal fun unlockAutoHideAllowed(locked: Boolean, visible: Boolean, dragging: Boolean): Boolean = locked && visible && !dragging
