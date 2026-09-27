package com.opticast.player.data.local

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * In-memory list of folder prefixes to skip while scanning
 * (e.g. "DCIM/Camera", "Download"). Hydrated from settings at app start.
 */
object FolderExclusions {

    private val _exclusions = MutableStateFlow<List<String>>(emptyList())
    val exclusions: StateFlow<List<String>> = _exclusions

    fun hydrate(list: List<String>) {
        _exclusions.value = list.map { normalize(it) }.filter { it.isNotBlank() }
    }

    fun isExcluded(relativePath: String?): Boolean {
        if (relativePath.isNullOrBlank()) return false
        val path = normalize(relativePath)
        return _exclusions.value.any { exclusion ->
            exclusion.isNotBlank() && path.startsWith(exclusion, ignoreCase = true)
        }
    }

    private fun normalize(s: String): String =
        s.trim().trim('/').replace('\\', '/')
}
