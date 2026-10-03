package com.opticast.player.util

import org.junit.Test
import org.junit.Assert.*

class InstallerVerifierTest {

    @Test
    fun nullInstaller_isOfficial() {
        val info = InstallerVerifier.verify(null)
        assertTrue(info.isOfficial)
        assertFalse(info.isThirdParty)
        assertEquals("Direct installation (Official)", info.displayName)
    }

    @Test
    fun fdroid_isOfficial() {
        val fdroid = InstallerVerifier.verify("org.fdroid.fdroid")
        assertTrue(fdroid.isOfficial)
        assertEquals("F-Droid (Official)", fdroid.displayName)

        val privileged = InstallerVerifier.verify("org.fdroid.fdroid.privileged")
        assertTrue(privileged.isOfficial)
    }

    @Test
    fun izzy_isOfficial() {
        val izzy = InstallerVerifier.verify("org.izzyondroid.izzyondroid")
        assertTrue(izzy.isOfficial)
        assertEquals("IzzyOnDroid (Official)", izzy.displayName)
    }

    @Test
    fun packageInstaller_isOfficial() {
        val pi = InstallerVerifier.verify("com.android.packageinstaller")
        assertTrue(pi.isOfficial)
        assertEquals("System Installer (Official)", pi.displayName)

        val googlePi = InstallerVerifier.verify("com.google.android.packageinstaller")
        assertTrue(googlePi.isOfficial)
    }

    @Test
    fun shell_isOfficial() {
        val shell = InstallerVerifier.verify("com.android.shell")
        assertTrue(shell.isOfficial)
    }

    @Test
    fun thirdParty_isNotOfficial() {
        val thirdParty = InstallerVerifier.verify("com.example.thirdparty.store")
        assertFalse(thirdParty.isOfficial)
        assertTrue(thirdParty.isThirdParty)
        assertEquals("Third-party source", thirdParty.displayName)
    }

    @Test
    fun randomInstaller_isThirdParty() {
        val random = InstallerVerifier.verify("com.some.random.appstore")
        assertTrue(random.isThirdParty)
        assertFalse(random.isOfficial)
    }

    @Test
    fun fdroidSubstring_isOfficial() {
        // Contains fdroid substring should be considered official
        val customFdroid = InstallerVerifier.verify("com.my.fdroid.client")
        assertTrue(customFdroid.isOfficial)
    }

    @Test
    fun professional_displayNames() {
        // Ensure display names are professional, no explicit unofficial store names
        val thirdParty = InstallerVerifier.verify("com.example.store")
        assertFalse(thirdParty.displayName.contains("APKPure"))
        assertFalse(thirdParty.displayName.contains("Aptoide"))
        assertEquals("Third-party source", thirdParty.displayName)
    }
}
