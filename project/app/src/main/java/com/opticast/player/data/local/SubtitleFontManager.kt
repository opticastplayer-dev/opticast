package com.opticast.player.data.local

import android.content.Context
import java.io.File

/**
 * Custom subtitle fonts - user can add fonts to filesDir/fonts
 * - Offline, no internet needed
 * - Supports .ttf, .otf
 * - Falls back to system font
 */
class SubtitleFontManager(private val context: Context) {
    private val fontDir = File(context.filesDir, "fonts").apply { mkdirs() }
    
    fun availableFonts(): List<File> {
        return try {
            fontDir.listFiles()?.filter { 
                it.isFile && (it.name.endsWith(".ttf", true) || it.name.endsWith(".otf", true))
            }?.sortedBy { it.name } ?: emptyList()
        } catch (_: Exception) { emptyList() }
    }
    
    fun fontNames(): List<String> {
        return listOf("System Default") + availableFonts().map { it.nameWithoutExtension }
    }
    
    fun getFontPath(name: String): String? {
        if (name == "System Default") return null
        return try {
            availableFonts().find { it.nameWithoutExtension == name }?.absolutePath
        } catch (_: Exception) { null }
    }
    
    fun importFont(source: File): Boolean {
        return try {
            if (!source.exists()) return false
            if (!source.name.endsWith(".ttf", true) && !source.name.endsWith(".otf", true)) return false
            if (source.length() > 10 * 1024 * 1024) return false // Max 10MB font
            val dest = File(fontDir, source.name)
            source.copyTo(dest, overwrite = true)
            true
        } catch (_: Exception) { false }
    }
    
    fun deleteFont(name: String): Boolean {
        return try {
            val file = availableFonts().find { it.nameWithoutExtension == name } ?: return false
            file.delete()
        } catch (_: Exception) { false }
    }
    
    fun fontDirPath(): String = fontDir.absolutePath
}
