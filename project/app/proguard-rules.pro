# R8 fullMode issues - dontwarn
-dontwarn javax.el.**
-dontwarn org.ietf.jgss.**
-dontwarn net.engio.mbassy.**
-dontwarn com.hierynomus.smbj.**
-dontwarn org.bouncycastle.**
-dontwarn jcifs.**
-dontwarn org.slf4j.**

# Keep MPV native
-keep class is.xyz.mpv.** { *; }
-keep class com.opticast.player.player.mpv.** { *; }

# Keep models
-keep class com.opticast.player.data.model.** { *; }

# For official release - allow
-dontnote **
-dontwarn **
-keepattributes Signature, InnerClasses, EnclosingMethod, *Annotation*
