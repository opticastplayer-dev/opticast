# OptiCast v2.6.104

[![Release](https://img.shields.io/github/v/release/opticastplayer-dev/opticast?label=Release)](https://github.com/opticastplayer-dev/opticast/releases)
[![License](https://img.shields.io/github/license/opticastplayer-dev/opticast)](../LICENSE)
[![F-Droid](https://img.shields.io/badge/F--Droid-Available-blue)](https://f-droid.org/packages/com.opticast.player)

Local video player for Android - offline-first, no ads, open source.

**No ads, no tracking, GPL-3.0. 38M APK, Android 8+, mobile only**

## What's New in v2.6.104

- Audio Only removed - fixes blank video until seek
- Library scrolling fast - removed unnecessary animations, keeps Adaptive grid, border/clip 12dp, badges, discovery
- RAM 8/12/16/20 + 32/48/64/96 LRU 30 pool 200 + Coil clear
- Offline-first: check once when internet detected, subtitles cached, Up To Date only when update available
- Install over existing same JKS higher versionCode, in-app FileProvider, full changelog visible

## Build

```bash
cd project && ./gradlew assembleRelease
```

Requires Android Studio Ladybug+, JDK 17, compileSdk 36, minSdk 26, targetSdk 35
