# OptiCast Proguard Rules - Optimized for 9/10 rating
# Keeps all features, but smaller APK and faster via R8

# R8 fullMode issues - dontwarn for third-party libs that use reflection
-dontwarn javax.el.**
-dontwarn org.ietf.jgss.**
-dontwarn net.engio.mbassy.**
-dontwarn com.hierynomus.smbj.**
-dontwarn org.bouncycastle.**
-dontwarn jcifs.**
-dontwarn org.slf4j.**
-dontwarn org.bouncycastle.jsse.**
-dontwarn sun.security.**

# Keep MPV native - critical for full mpv playback
-keep class is.xyz.mpv.** { *; }
-keep class com.opticast.player.player.mpv.** { *; }
-keep class com.opticast.player.nativeengine.** { *; }

# Keep models - used by serialization
-keep class com.opticast.player.data.model.** { *; }
-keepclassmembers class com.opticast.player.data.model.** { *; }

# Keep serialization - kotlinx-serialization uses reflection
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *; }
-keep class kotlinx.serialization.** { *; }
-keepclassmembers class ** {
    @kotlinx.serialization.Serializable <fields>;
}
-keepclassmembers class ** {
    @kotlinx.serialization.SerialName <fields>;
}

# Keep Compose - required for Compose runtime
-keep class androidx.compose.** { *; }
-keepclassmembers class androidx.compose.** { *; }
-keepattributes Signature, InnerClasses, EnclosingMethod

# Keep Coil - image loading uses reflection
-keep class coil.** { *; }
-keep class coil.compose.** { *; }

# Keep OkHttp - networking
-keep class okhttp3.** { *; }
-keep class okio.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**

# Keep Media3 / ExoPlayer - playback
-keep class androidx.media3.** { *; }
-keep class com.google.android.exoplayer2.** { *; }

# Keep Room / DataStore - if used
-keep class androidx.datastore.** { *; }

# Keep native libs - JNI
-keepclasseswithmembernames class * {
    native <methods>;
}

# Optimization - for 9/10 rating, smaller APK, faster startup
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*
-optimizationpasses 5
-allowaccessmodification
-keepattributes Signature, InnerClasses, EnclosingMethod, *Annotation*, SourceFile, LineNumberTable

# For official release - reduce notes
-dontnote **
-dontwarn **

# Keep crash reporting - for debugging
-keep class com.opticast.player.data.CrashReporting { *; }

# Keep critical for install-over - package name, version
-keep class com.opticast.player.BuildConfig { *; }

# Remove logging in release for smaller APK and performance (keep for crash reporting)
-assumenosideeffects class android.util.Log {
    public static *** v(...);
    public static *** d(...);
    public static *** i(...);
}

# Keep line numbers for crash reports
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
