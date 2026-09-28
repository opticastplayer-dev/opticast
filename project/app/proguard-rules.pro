# Keep OptiCast core
-keep class com.opticast.player.** { *; }
-keep class org.videolan.libvlc.** { *; }

# R8 fullMode issues with mbassy and smbj - dontwarn missing javax.el and GSS
-dontwarn javax.el.**
-dontwarn org.ietf.jgss.**
-dontwarn net.engio.mbassy.**
-dontwarn com.hierynomus.smbj.**
-dontwarn org.bouncycastle.**
-dontwarn jcifs.**

# Keep MPV
-keep class is.xyz.mpv.** { *; }
-keep class com.opticast.player.player.mpv.** { *; }

# Keep Media3
-keep class androidx.media3.** { *; }

# Keep Coil
-keep class coil.** { *; }

# Keep Compose
-keep class androidx.compose.** { *; }

# Dont obfuscate for stability on low-RAM
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes *Annotation*

# For official release - allow missing classes
-dontnote **
-dontwarn **
