package com.opticast.player.util

/**
 * Installer verification
 */
object InstallerVerifier {

    private val officialInstallers = setOf(
        "org.fdroid.fdroid", "org.fdroid.fdroid.privileged",
        "com.android.packageinstaller", "com.google.android.packageinstaller",
        "com.android.shell"
    )

    data class InstallerInfo(
        val displayName: String,
        val isOfficial: Boolean,
        val isThirdParty: Boolean
    )

    fun verify(installerPackageName: String?): InstallerInfo {
        val installer = installerPackageName
        val isOfficial = installer == null || installer in officialInstallers ||
                installer.contains("fdroid") ||
                installer.contains("packageinstaller") || installer.contains("shell")

        val displayName = when {
            installer == null -> "Direct installation (Official)"
            installer in officialInstallers -> when (installer) {
                "org.fdroid.fdroid", "org.fdroid.fdroid.privileged" -> "F-Droid (Official)"
                else -> "System Installer (Official)"
            }
            isOfficial -> "System Installer (Official)"
            else -> "Third-party source"
        }

        return InstallerInfo(
            displayName = displayName,
            isOfficial = isOfficial,
            isThirdParty = !isOfficial
        )
    }
}
