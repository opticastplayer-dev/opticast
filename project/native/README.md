# OptiCast controlled mpv build — unchanged engine supplied with 2.6.54

This release uses **our own source-built native libraries and JNI adapter**, not the unaudited Maven `mpv-android:1.0.0` binary. mpv, FFmpeg and libplacebo are pinned to the source revisions identified in that wrapper's version constants; other dependencies and all used submodules are explicitly pinned in `sources.lock.json`. The configuration is not byte-identical to mpvEx: OpenGL ES, no Vulkan/shaderc, no Lua/libcurl, NDK r28c, API 26, ARM64 and ARMv7 (32-bit ARM/NEON). Hardware-first is a preference, not proof of which decoder a given phone uses.

## What accompanies the APK

The locked baseline supplies `OptiCast-2.6.54-complete-source.zip` with all four dependency source bundles directly included. No older ZIP or shared-source restore is needed. Native library bytes and dependency sources remain unchanged. It contains:
- Complete OptiCast app/adapter source and this build directory.
- `project/native/corresponding-sources/native-sources.tar.xz`: the native source trees actually used, including submodule code, generated configure files and scripted mbedTLS configuration changes.
- `project/native/corresponding-sources/native-source-inventory.json`: hashes of included source files and an explicit list of omitted **unbuilt** font/image test fixtures. No compiled runtime source/header/generator is excluded. Objects/caches are omitted.
- `project/native/corresponding-sources/maven-sources.zip`: exact resolved Android/JVM runtime source JARs, POMs and notices. The empty Guava conflict-avoidance placeholder has no code/source requirement.
- `project/native/corresponding-sources/androidx-native-sources.zip`: the existing AndroidX native components’ full release source modules, including C++ and build scripts.
- Runtime, source and dependency inventories; GPL text, upstream notices and the build-script upstream MIT license.

The native binaries use the standard, unmodified Android NDK r28c compiler/runtime. NDK download: https://dl.google.com/android/repository/android-ndk-r28c-linux.zip . LLVM revision reported by that toolchain: `97a699bf4812a18fb657c2779f5296a4ab2694d2`; compiler source is available at https://android.googlesource.com/toolchain/llvm-project/+/97a699bf4812a18fb657c2779f5296a4ab2694d2 . The compiler/runtime is treated as the standard toolchain/System Library, not an OptiCast-specific library. Its applicable notices are included in the APK. No modified proprietary runtime is bundled.

## Rebuild the native engine

From the unpacked source ZIP's root on Linux:
1. Run `bash tools/setup.sh` to install Android 36, build tools and JDK 17. Build scripts never replace an existing signing key.
2. Install `build-essential pkg-config cmake ninja-build autoconf automake libtool-bin nasm gperf gettext autopoint bison flex` through your package manager.
3. Install the Python build tools: `python3 -m pip install --target .cache/native-python meson==1.11.0 jinja2 jsonschema`. The original build used Meson 1.9.2 for early dependencies and 1.11.0 for fontconfig/mpv; 1.11.0 meets all declared minimums. Byte-for-byte reproduction is not asserted.
4. Download/unzip the NDK archive into `.cache/native-build/sdk/`, giving `.cache/native-build/sdk/android-ndk-r28c/`.
5. For the source-bundled/offline path, extract `native-sources.tar.xz` under `.cache/`, then copy its `deps` directory to `.cache/native-build/deps`. Verify the source files against `native-source-inventory.json` first. Use `NATIVE_BUNDLED_SOURCES=1 bash project/native/build-native.sh arm64`. This bypasses downloads only after the bundled tree's file hashes have been verified.
6. Alternatively run `bash project/native/build-native.sh arm64` to download the pinned upstream archives and verify their hashes. Moving HEADs are not inputs. Build-script changes regenerate the same mbedTLS configuration from those inputs.
7. Run `bash project/native/package-runtime.sh arm64` to build the JNI layer, strip/package ARM64 libraries and generate a runtime hash manifest. Repeat build and packaging with `armv7l` for ARM32. The packager retains both staged ABI directories. All `.so` files must pass hash, architecture and dependency checks; ARM64 requires 16 KB ELF LOAD alignment, ARM32 at least 4 KB.

The compiled AAR is a generated artifact, not preferred-form source. In the maintained workspace it can also be recovered, hash-checked, from the verified release APK using `tools/restore-native-runtime.py`; app-only rebuilds need not compile FFmpeg again. Changes to native sources/configuration require a real native rebuild and new provenance records.

## Build and install a modified app

The official signing key is intentionally **not** published. On a separate copy of this source, create your own `signing/opticast-release.jks` and `signing/signing.properties` containing `storePassword`, `keyPassword` and `keyAlias` for that key. Never replace the existing official key in the maintained workspace. Android's `keytool` can generate your private development key.

Change only `applicationId` to a unique development ID (for example `com.opticast.player.dev`) if you want to install alongside the official app; keep the Kotlin/Android namespace unchanged. Then run `cd project && ./gradlew :app:assembleDebug`. The guarded official-release script verifies the private official certificate/baseline and is intended for the maintained release workspace, not third-party development builds. A differently signed package cannot update the official installation; a separate development ID avoids uninstalling it or losing its history.

Use `adb install -r app/build/outputs/apk/debug/app-debug.apk` or Android's package installer. No server approval, special unlocking secret or private native key is required to run a modified development package.

## Licensing audit / distribution

OptiCast's code in this release is GPL-3.0-or-later, as authorized by the owner; third-party code retains its compatible license. Native FFmpeg has GPL/version3 enabled. FreeType uses its FTL option with the required credit. Fribidi's LGPL terms and all other upstream notices/sources accompany the distribution.

The runtime inventory found an unused proprietary Google Play Services Cronet dependency. It was **removed**, along with the unused Media3 Cronet extension, before packaging this GPL build. Network playback already uses the existing RemoteDataSource/OkHttp path. The remaining resolved runtime artifacts have included sources and permissive Apache/MIT/BSD-style terms; BC's license was verified in its source because its POM omits it. This is a technical compliance record, not a claim of formal legal certification.

Ship the APK together with its complete source ZIP (or equivalent GPL-compliant source access), not just this directory or the small application-only sources. Preserve the notices and redistribution rights.

## Scope and verification limits

mpv is the default local-file engine. Reported mpv failures automatically try Media3 once per video request, without an engine loop. Network sources use Media3 directly. Failure cards retain explicit recovery actions. Existing controls, MediaSession, notification/PiP plumbing and identity-keyed resume store are reused. Android EQ/boost and network authentication/SMB remain Media3 features. mpv subtitle rendering/styles may differ.

Host compile, policy tests, ELF/dependency alignment, source/hash and signing checks are not device playback tests. Native decoder/Surface/audio-focus/PiP behaviour and comparisons using the two reported files still require phone testing. In particular, the adapter uses mpv playback-restart readiness to release PlayerView's shutter; it is **not measured first-rendered-frame telemetry**. No universal format/HDR/DRM, faster-startup or memory claim is made.

### Existing AndroidX native libraries

`androidx-native-sources.zip` supplements the Maven source JARs with the complete official graphics-path 1.0.1 and datastore-core 1.1.3 release source modules, including their C++ sources and CMake/Gradle files. These two existing libraries are **not** the newly controlled mpv build; their Maven artifact hashes, native payload hashes, pinned release commits and official release-note evidence are recorded in `androidx-native.lock.json`. Both ARM64 ELF payloads also have 16KB LOAD alignment. ARM32 payload hashes are also recorded, extracted from the same hash-verified official AARs.

To rebuild a module's native target, extract its tarball and invoke CMake with the NDK `build/cmake/android.toolchain.cmake`, `ANDROID_ABI=arm64-v8a` (or `armeabi-v7a` for ARM32), `ANDROID_PLATFORM=android-26`, `CMAKE_BUILD_TYPE=Release`, and `ANDROID_STL=c++_static`. The source directory is `src/main/cpp` for graphics-path and `src/androidMain/cpp` for DataStore; retain the module's additional flags in its included `build.gradle`. No bit-for-bit reproduction is promised.

The APK compresses native libraries for a smaller download. Android extracts them at installation; the audited native ELF bytes are preserved without a second Gradle stripping pass. To modify bundled native sources, verify/extract them first, make your changes, then update your own inventory/lock or adjust the verification script for your build. These transparent hash checks are release provenance checks, not a restriction on GPL modification rights.

Caption size and Boxed/White/Yellow styles now also configure mpv/libass text subtitles. Bitmap captions cannot be restyled as text. Aggressive Stretch+ scales the native video image while leaving its subtitle canvas at viewport size. Downloaded, pre-shifted subtitles retain the shared subtitle configuration. These changes still require phone visual/lifecycle acceptance testing.

## 2.6.51 ABI provenance

The ARM64 controlled runtime is reused byte-for-byte from locked 2.6.50 (prior manifest in `provenance/`). ARM32 is built from the same supplied source bundles with NDK r28c/API26 and Meson 1.11.0. No x86 runtime is shipped. No physical Android 8 installation or playback result is asserted.

## Baseline acceptance

On 2026-09-27 the owner reported installation and operation working as expected on the reported Android8/32-bit test device and requested this baseline lock. This is owner-reported acceptance, not independent instrumented testing or certification of every feature/device.
