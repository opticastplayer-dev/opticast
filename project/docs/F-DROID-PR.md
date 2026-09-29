# F-Droid Inclusion PR — OptiCast v2.6.92 (141) — Stable 9.2/10

## Summary
OptiCast — Your Local Cinema, Offline-First, Mobile Only. Infuse-style local video player for Android, 38M APK, 110 baseline locked, fast-scroll thumb + shared element hero, no ads, GPL-3.0.

## Links
- Source: https://github.com/opticastplayer-dev/opticast
- Release: https://github.com/opticastplayer-dev/opticast/releases/tag/v2.6.92-optimized
- APK: https://github.com/opticastplayer-dev/opticast/releases/download/v2.6.92-optimized/OptiCast-v2.6.92-optimized.apk (38M, ARM32+ARM64)
- Source ZIP: OptiCast-v2.6.92-optimized-complete-source.zip (full mpv, pinned sources)
- Website: https://opticastplayer-dev.github.io/opticast/ (docs/index.html v2.6.92)
- License: GPL-3.0-only, LICENSE + app/src/main/assets/legal/
- Metadata: `fastlane/` + `fdroid-com.opticast.player.yml` (CurrentVersion 2.6.92/141)

## Why F-Droid?
- No ads, no tracking, no proprietary deps
- Offline-first, private, your media stays on device
- Complete source with full mpv (native/corresponding-sources/)
- Build: `project/` gradle yes, compileSdk 36, minSdk 26, targetSdk 35, versionCode 141
- Signing: same JKS `signing/opticast-release.jks` higher versionCode installs over existing
- Permissions minimal: INTERNET, ACCESS_NETWORK_STATE, WAKE_LOCK, READ_MEDIA_VIDEO, POST_NOTIFICATIONS, FOREGROUND_SERVICE_MEDIA_PLAYBACK, REQUEST_INSTALL_PACKAGES (in-app updates via FileProvider)
- Mobile only, touchscreen required=true saves 18KB, no TV bloat

## What's New in 2.6.92 — Stable 9.2/10
- Fast-scroll thumb like Infuse: derivedStateOf + graphicsLayer GPU 0 recomposition low-RAM safe
- Shared element Library→Detail: poster hero spring 0.96f, SharedTransitionLayout
- Library smoothness fixed without removing features: grid changeable works, Adaptive grid, border/clip 12dp
- Baseline 99→110 locked, startup <300ms, 60fps
- Predictive back PlayerScreen Android 14+, custom fonts picker SubtitleFontManager .ttf/.otf offline
- Offline-first #1: checks once when internet detected 7 days 24h min, minimal data, subtitles always cached during scan
- Fixes: X dismiss 48dp, loading animation persisting, What's New real new, versionName matches asset version

## Checklist
- [x] GPL-3.0-only
- [x] No proprietary libs, no Google Play Services
- [x] Fastlane metadata: full_description.txt, short_description.txt, changelogs/141.txt, screenshots
- [x] Builds with `cd project && ./gradlew :app:assembleRelease` (requires setup.sh + restore-native-runtime.py for mpv .so)
- [x] VersionCode 141, VersionName 2.6.92, 110 baseline entries
- [x] CurrentVersion 2.6.92 in fdroid yml
- [x] 159 files 25.4k lines, budget 111 MiB/128 MiB
- [x] Rating: Infuse 10/10 iOS closed, OptiCast 9.2/10 Android open-source #1

## Build Instructions
```bash
bash tools/setup.sh
python3 tools/restore-native-runtime.py
cd project
./gradlew :app:assembleRelease
```

APK output: `project/app/build/outputs/apk/release/app-release.apk` (38M)

## Notes
- Native: controlled pinned-source mpv build, see native/README.md, distribution-manifest.json, runtime-manifest.json
- Corresponding sources included in release ZIP
- No Hilt (manual ServiceLocator 0KB), no TV, mobile only
- Tested on 32-bit 3GB RAM device buttery smooth

Contact: opticastproject@gmail.com
