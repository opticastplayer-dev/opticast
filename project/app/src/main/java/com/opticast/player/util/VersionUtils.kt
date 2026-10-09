package com.opticast.player.util

/**
 * Version comparison utilities - extracted for testability
 * Covers bugs fixed: misleading update when 2.6.71-optimized vs 2.6.71
 */
object VersionUtils {

    fun parseVersionCode(version: String): Long {
        return try {
            val normalized = version.substringBefore("-optimized").substringBefore("-")
            val parts = normalized.split(".")
            if (parts.size >= 3) {
                val major = parts[0].toLongOrNull() ?: 0
                val minor = parts[1].toLongOrNull() ?: 0
                val patch = parts[2].substringBefore("-").toLongOrNull() ?: 0
                major * 10000 + minor * 100 + patch
            } else {
                0L
            }
        } catch (_: Exception) {
            0L
        }
    }

    fun normalizeVersion(version: String): String {
        return version.substringBefore("-optimized").substringBefore("-").trim()
    }

    fun isVersionNewer(remote: String, installed: String): Boolean {
        return try {
            val remoteNorm = normalizeVersion(remote)
            val installedNorm = normalizeVersion(installed)
            if (remoteNorm == installedNorm) return false

            val remoteParts = remoteNorm.split(".").map { it.substringBefore("-").toIntOrNull() ?: 0 }
            val installedParts = installedNorm.split(".").map { it.substringBefore("-").toIntOrNull() ?: 0 }
            for (i in 0 until maxOf(remoteParts.size, installedParts.size)) {
                val r = remoteParts.getOrNull(i) ?: 0
                val inst = installedParts.getOrNull(i) ?: 0
                if (r > inst) return true
                if (r < inst) return false
            }
            false
        } catch (_: Exception) {
            normalizeVersion(remote) != normalizeVersion(installed) && remote != installed
        }
    }
}
