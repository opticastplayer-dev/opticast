package com.opticast.player.external

internal fun supportedExternalVideo(scheme: String?, mime: String?, name: String?): Boolean {
    if (scheme?.lowercase() !in setOf("content", "file", "http", "https")) return false
    val type = mime?.substringBefore(';')?.trim()?.lowercase()
    if (type?.startsWith("video/") == true) return true
    if (type != null && type !in setOf("application/octet-stream", "application/x-matroska", "*/*")) return false
    return name?.substringBefore('?')?.substringBefore('#')?.substringAfterLast('.')?.lowercase() in
        setOf("mp4", "mkv", "webm", "avi", "mov", "m4v", "3gp", "3g2", "ts", "m2ts", "mpeg", "mpg", "wmv", "flv", "ogv")
}
