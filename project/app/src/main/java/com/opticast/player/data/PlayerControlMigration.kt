package com.opticast.player.data

/** Preserve the old audio button's visibility when replacing its slot with Library. */
internal fun resolvedPlayerControls(controls: Collection<String>): List<String> =
    controls.map { if (it == "audio") "library" else it }.distinct()
