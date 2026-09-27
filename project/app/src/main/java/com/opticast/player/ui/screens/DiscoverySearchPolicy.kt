package com.opticast.player.ui.screens

internal fun showDiscoveryExtras(searchOpen: Boolean): Boolean = !searchOpen
internal fun includeInMainResults(watched: Boolean, searchOpen: Boolean): Boolean = searchOpen || !watched
