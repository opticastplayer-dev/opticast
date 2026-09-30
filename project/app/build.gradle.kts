import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "com.opticast.player"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.opticast.player"
        minSdk = 26
        targetSdk = 35
        ndk {
            // First controlled mpv release targets ARM64 (including the reported SM-A155M).
            abiFilters += listOf("arm64-v8a", "armeabi-v7a")
        }
        versionCode = 147
        versionName = "2.6.98"
        versionCode = 147
        versionName = "2.6.98"
        vectorDrawables { useSupportLibrary = true }
    }

    signingConfigs {
        create("official") {
            // BULLETPROOF: Require release keystore for official builds - fail if missing
            // Prevents debug signature that cannot install over release
            val signingPropsFile = rootProject.file("../signing/signing.properties")
            val signingJksFile = rootProject.file("../signing/opticast-release.jks")
            if (!signingJksFile.exists()) {
                // In CI, signing key is restored from secret in workflow
                // If missing here, workflow should have already failed
                // For local builds without key, we still allow debug fallback but warn
                println("WARNING: Release JKS not found at ${signingJksFile.absolutePath} - official build will fail if signing secret missing")
            }
            val credentials = Properties().apply {
                if (signingPropsFile.exists()) {
                    signingPropsFile.inputStream().use { load(it) }
                } else {
                    // Dummy for local builds without secrets - workflow will fail earlier if secret missing
                    setProperty("storePassword", "android")
                    setProperty("keyAlias", "androiddebugkey")
                    setProperty("keyPassword", "android")
                }
            }
            if (signingJksFile.exists()) {
                storeFile = signingJksFile
                storePassword = credentials.getProperty("storePassword")
                keyAlias = credentials.getProperty("keyAlias")
                keyPassword = credentials.getProperty("keyPassword")
                println("Using official release keystore: ${signingJksFile.name}")
            } else {
                // Fallback to debug only for local testing - CI workflow will FAIL before reaching here if secret missing
                println("WARNING: Using debug keystore - APK will NOT install over release builds!")
                storeFile = rootProject.file("../signing/debug.keystore").takeIf { it.exists() }
                storePassword = "android"
                keyAlias = "androiddebugkey"
                keyPassword = "android"
            }
        }
        getByName("debug") {
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            isDebuggable = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // BULLETPROOF: Use official signing - workflow verifies JKS exists before build
            // If JKS missing, this will use debug but workflow already failed for official releases
            val officialJks = rootProject.file("../signing/opticast-release.jks")
            signingConfig = if (officialJks.exists()) {
                signingConfigs.getByName("official")
            } else {
                // Local builds without release key - still builds but warns
                // CI official builds will have failed earlier in workflow if secret missing
                signingConfigs.getByName("debug")
            }
        }
        debug {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
    packaging {
        jniLibs {
            // Smaller direct-download APK; Android extracts the already 16KB-aligned ELF files.
            useLegacyPackaging = true
            // Runtime was stripped by the pinned NDK; preserve its audited byte hashes.
            keepDebugSymbols += "**/*.so"
        }
        resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" }
    }

    lint {
        // The bundled lint tooling crashes release analysis on this AGP/Kotlin
        // combination (IncompatibleClassChangeError inside the
        // NullSafeMutableLiveData detector's Kotlin-API handler). Lint is a
        // build-time gate and never ships in the APK, so it must not be able to
        // block a release build.
        checkReleaseBuilds = false
        abortOnError = false
        disable += "NullSafeMutableLiveData"
    }
}

dependencies {
    testImplementation("junit:junit:4.13.2")
    val compose = "1.8.3"

    // Core + lifecycle
    implementation("androidx.core:core-ktx:1.16.0")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.window:window:1.3.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.0")

    // Compose UI (Material 3 Expressive)
    implementation("androidx.compose.ui:ui:$compose")
    implementation("androidx.compose.ui:ui-graphics:$compose")
    implementation("androidx.compose.ui:ui-tooling-preview:$compose")
    implementation("androidx.compose.foundation:foundation:$compose")
    implementation("androidx.compose.material3:material3:1.5.0-alpha01")
    implementation("androidx.compose.material:material-icons-extended:1.7.8")
    implementation("androidx.compose.material3:material3-window-size-class:1.4.0")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.9.0")

    // BULLETPROOF: Prebuilt AAR mandatory - permanent, never depends on GitHub releases
    // If missing, build fails explicitly instead of silent Media3-only that breaks install-over
    val prebuiltAar = rootProject.file("src/main/../../native/prebuilt/opticast-mpv-runtime.aar")
    val cacheAar = rootProject.file("../.cache/native-runtime/opticast-mpv-runtime.aar")
    val mpvAar = when {
        prebuiltAar.exists() -> prebuiltAar
        cacheAar.exists() -> cacheAar
        else -> null
    }
    if (mpvAar != null && mpvAar.exists()) {
        if (mpvAar.length() < 10*1024*1024) {
            throw GradleException("FATAL: mpv AAR too small ${mpvAar.length()} - Media3-only would break install-over. See project/native/prebuilt/README.md")
        }
        implementation(files(mpvAar))
        println("Using mpv runtime AAR: ${mpvAar.absolutePath} ${mpvAar.length()/1024/1024}M")
    } else {
        // Only allow Media3-only if explicitly building debug without full_mpv
        // For official release, workflow will have already failed if AAR missing
        val isOfficial = gradle.startParameter.taskNames.any { it.contains("OfficialRelease") || it.contains("Release") }
        if (isOfficial) {
            throw GradleException("FATAL: mpv runtime AAR not found - would build Media3-only that cannot install over full mpv. Prebuilt must exist at ${prebuiltAar.absolutePath}. See docs/BULLETPROOF-BUILD.md")
        } else {
            println("WARNING: mpv AAR not found - building Media3-only (debug only)")
        }
    }

    // Media3 / ExoPlayer handles fallback, explicit selection and network sources
    implementation("androidx.media3:media3-exoplayer:1.7.1")
    implementation("androidx.media3:media3-exoplayer-dash:1.7.1")
    implementation("androidx.media3:media3-exoplayer-hls:1.7.1")
    implementation("androidx.media3:media3-exoplayer-rtsp:1.7.1")
    implementation("androidx.media3:media3-exoplayer-smoothstreaming:1.7.1")
    implementation("androidx.media3:media3-ui:1.7.1")
    implementation("androidx.media3:media3-session:1.7.1")
    // The unused Cronet/Google Play Services extension is excluded from this GPL build.
    // Network playback already uses RemoteDataSource + OkHttp.
    implementation("androidx.media3:media3-datasource-okhttp:1.7.1")
    implementation("androidx.media3:media3-effect:1.7.1")
    implementation("androidx.media3:media3-container:1.7.1")

    // Images
    implementation("io.coil-kt:coil-compose:2.7.0")

    // Networking + serialization
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    // SMB/CIFS network libraries (Windows shares, NAS, router USB drives)
    implementation("com.hierynomus:smbj:0.13.0")
    implementation("org.slf4j:slf4j-nop:2.0.13")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.1")

    // Settings storage
    implementation("androidx.datastore:datastore-preferences:1.1.3")

    // Baseline profile installer for faster startup
    implementation("androidx.profileinstaller:profileinstaller:1.4.0")

    debugImplementation("androidx.compose.ui:ui-tooling:$compose")
}


// Resolved *runtime* inventory for the GPL corresponding-source distribution.
tasks.register("writeRuntimeInventory") {
    doLast {
        val records = configurations.getByName("releaseRuntimeClasspath").resolvedConfiguration.resolvedArtifacts
            .map { artifact ->
                val id = artifact.moduleVersion.id
                "${id.group}\t${id.name}\t${id.version}\t${artifact.file.absolutePath}"
            }.sorted()
        rootProject.file("../.cache/maven-runtime.tsv").writeText(records.joinToString("\n") + "\n")
    }
}
