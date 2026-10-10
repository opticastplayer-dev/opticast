package com.opticast.player.data

data class ProviderNotice(val name: String, val description: String, val notice: String, val links: List<Pair<String, String>>)

/** Provider credits moved to website https://opticastplayer-dev.github.io/opticast/ */
object ProviderNotices {
    val entries = emptyList<ProviderNotice>()
}
