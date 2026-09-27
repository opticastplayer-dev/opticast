package com.opticast.player.ui.screens

internal fun needsAutomaticLibraryScan(previousKey: String?, currentKey: String): Boolean = previousKey != currentKey
