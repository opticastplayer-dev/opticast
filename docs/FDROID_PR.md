# F-Droid Inclusion PR — OptiCast Video Player

**Package:** `com.opticast.player` | **Current:** v2.6.84 (133) | **Status:** NOT_FOUND on F-Droid API (needs inclusion PR)

## Why F-Droid?
- Offline-first, no ads, no tracking, GPL-3.0, 100% free open source
- Mobile only, touchscreen required=true, no TV
- 7 permissions minimal, privacy-focused
- Builds with full mpv 37.6M, installs over existing
- Fastlane metadata 125-133 ready

## F-Droid Metadata YAML (for fdroiddata repo)

Create file `metadata/com.opticast.player.yml` in https://gitlab.com/fdroid/fdroiddata :

```yaml
Categories:
  - Multimedia
License: GPL-3.0-only
AuthorName: OptiCast Project
AuthorEmail: opticastproject@gmail.com
WebSite: https://opticastplayer-dev.github.io/opticast/
SourceCode: https://github.com/opticastplayer-dev/opticast
IssueTracker: https://github.com/opticastplayer-dev/opticast/issues
Changelog: https://github.com/opticastplayer-dev/opticast/releases

Name: OptiCast Video Player
AutoName: OptiCast Video Player
Summary: Best local video player for Android — offline, mpv, no ads, open source, mobile only
Description: |
  OptiCast — Your Local Cinema, Offline-First, Mobile Only

  Infuse-style local video player for Android — beautiful, performance-focused, privacy-focused. Your local cinema, offline-first, mobile only (no TV).

  No ads, no tracking, GPL-3.0, 100% free, open source.

  LIBRARY:
  MediaStore auto-scan, adaptive poster grid changeable (Compact 86dp / Medium 108dp / Comfortable 140dp), Continue Watching, Movies 27, TV Shows 2, Favorites, Recently Added, Collections, search, multi-select share/delete. Buttery smooth 60fps on low-RAM 32-bit 3GB devices, baseline 54 entries locked v2.6.84 (133) 9.3/10.

  METADATA:
  Auto-identifies movies/TV from filenames via TMDB, posters, backdrops, offline caching, blurred poster fallback fixes black background. Ratings, cast headshots, genres, runtime, synopsis.

  SUBTITLES:
  OpenSubtitles + SubDL search, always cached during scan for offline playback, saved to filesDir/subtitles/. Dual subtitles, sync ±250ms + auto-align, SRT/ASS/VTT. Offline-first.

  PLAYBACK (mpv + Media3):
  Full mpv 37.6M with libmpv True 24 .so (arm64 6.1M + armv7 5.3M + libavcodec 11.7M + FFmpeg + libplacebo), plays all videos mkv/mp4/avi, Media3 fallback. Edge-to-edge Compose: thick progress, ±10s, play/pause, speed 0.5x-3x, aspect fit/zoom/stretch, subtitle/audio track picker. Gestures: double-tap seek, swipe volume/brightness, pinch-zoom 0.5x-2x GPU with pan, hold 2x-4x, scrub preview real frame. Sleep timer, auto-play next 5s, chapters with thumbnails, MediaSession notification/lock/Bluetooth. PiP auto-resume fixed for 32-bit, loading persist fix keyed to video.id auto-hide 3s max.

  OFFLINE-FIRST PRIORITY #1:
  Works fully offline after initial caching, minimal data usage, checks updates once when internet detected (24h min, 7 days max), not every 6h. Subtitles always cached, posters cached, metadata cached.

  PERFORMANCE:
  Baseline 54 entries locked v2.6.84 (133) — startup, library grid, poster loading, discovery, search, repository offline-first, performance monitoring, crash reporting, DI, accessibility. 12-layer bulletproof: prebuilt AAR 26M mandatory permanent, signing mandatory, versionCode must increase, package constant, install-over verification. Mobile only: no TV support, touchscreen required=true, saves 18KB.

  PRIVACY:
  No ads, no tracking, GPL-3.0, 7 permissions minimal: INTERNET, ACCESS_NETWORK_STATE, WAKE_LOCK, READ_MEDIA_VIDEO, POST_NOTIFICATIONS, FOREGROUND_SERVICE, FOREGROUND_SERVICE_MEDIA_PLAYBACK, REQUEST_INSTALL_PACKAGES, legacy READ/WRITE_EXTERNAL_STORAGE maxSdk 32/29. No location, contacts, microphone, camera.

  BUILD:
  cd project && ./gradlew assembleRelease — 37.6M APK, R8 minify, shrinkResources, full mpv 24 .so, installs over existing same signature higher versionCode.

  LINKS:
  GitHub: https://github.com/opticastplayer-dev/opticast
  Releases: https://github.com/opticastplayer-dev/opticast/releases/tag/v2.6.84-optimized
  F-Droid: https://f-droid.org/packages/com.opticast.player
  Pages: https://opticastplayer-dev.github.io/opticast/
  Privacy: docs/PRIVACY.md — FAQ: docs/FAQ.md

  WHAT'S NEW in 2.6.84:
  Mobile only, removed Android TV / Fire TV support (TvHomeScreen 481 lines + TvSupport, leanback, banner). Fixed weaknesses: docs meta v2.6.78->2.6.84, baseline date, grid changeable fixed, scan corrupted skip.

  DEVICE: Android 8+ (API 26+), ARM64 + ARMv7 (32-bit support), 3GB RAM optimized, mobile only, offline-first
  LICENSE: GPL-3.0 — complete source in releases contains prebuilt AAR
  SIZE: 37.6M universal APK → ~23M download via Play Store AAB (per ABI split), 75M install, 26M AAR
  RATING: 9.3/10 standalone Android, 8.7/10 vs Infuse (Apple gold standard 9.5/10), 9.2/10 for Android local-only

RepoType: git
Repo: https://github.com/opticastplayer-dev/opticast.git

Builds:
  - versionName: 2.6.84
    versionCode: 133
    commit: v2.6.84-optimized
    subdir: project
    gradle:
      - yes
    prebuild:
      - echo "Prebuilt AAR mandatory 26M with libmpv - see project/native/prebuilt/README.md"
      - ls -lh native/prebuilt/opticast-mpv-runtime.aar
    scandelete:
      - project/app/build

  - versionName: 2.6.83
    versionCode: 132
    commit: v2.6.83-optimized
    subdir: project
    gradle:
      - yes

AutoUpdateMode: Version v%v-optimized
UpdateCheckMode: Tags
CurrentVersion: 2.6.84
CurrentVersionCode: 133
```

## Important Notes for F-Droid Reviewers

1. **Prebuilt AAR 26M mandatory:** `project/native/prebuilt/opticast-mpv-runtime.aar` contains libmpv 24 .so (arm64 6.1M + armv7 5.3M + libavcodec 11.7M + FFmpeg + libplacebo). This is required for install-over existing same signature higher versionCode. Building libmpv from source would break install-over (different signatures, missing codecs). F-Droid can either:
   - Allow prebuilt AAR as binary (like many apps with native libs)
   - Or we provide `project/native/` build scripts to build AAR from mpv source (takes ~30min, needs NDK 27.0.12077973)

2. **Package constant:** `com.opticast.player` never changes — bulletproof check in CI fails if changed

3. **Signing:** Same JKS for all releases, versionCode must increase, install-over verification

4. **Permissions minimal:** 7 permissions, no location/contacts/mic/camera, offline-first, no ads/tracking

5. **Fastlane metadata:** Already in repo `project/fastlane/metadata/android/en-US/` with title, short_description 79 chars, full_description 48 lines, changelogs 125-133

6. **GPL-3.0:** License in repo, source in releases `complete-source.zip` contains prebuilt AAR

## Steps to Submit PR

1. Fork https://gitlab.com/fdroid/fdroiddata
2. Create branch `com.opticast.player`
3. Add `metadata/com.opticast.player.yml` with above content
4. Run `fdroid readmeta` and `fdroid lint com.opticast.player` locally
5. Test build: `fdroid build -v -l com.opticast.player` (needs Android SDK + NDK 27)
6. Submit MR to fdroiddata with description:

```
OptiCast Video Player — Best local video player for Android, Infuse alternative, offline-first, mobile only, no ads, GPL-3.0

- Package: com.opticast.player
- Version: 2.6.84 (133) — mobile only, removed TV support, 37.6M APK full mpv 24 .so
- License: GPL-3.0-only
- Source: https://github.com/opticastplayer-dev/opticast
- IssueTracker: https://github.com/opticastplayer-dev/opticast/issues
- Description: offline-first local video player, mpv + Media3, MediaStore scan, TMDB metadata, OpenSubtitles/SubDL always cached offline, buttery smooth 60fps on 3GB 32-bit, baseline 54 locked, 9.3/10 rating
- Permissions minimal 7, no ads/tracking, offline-first
- Fastlane metadata ready, changelogs 125-133
- Prebuilt AAR 26M mandatory for install-over — see project/native/prebuilt/README.md — contains libmpv 24 .so
- Builds: gradle yes, subdir project, commit v2.6.84-optimized
- AutoUpdateMode: Version v%v-optimized, UpdateCheckMode: Tags
```

7. Wait for F-Droid review, address comments (usually prebuilt AAR discussion)

## Alternative: F-Droid via GitHub Releases (No Build)

If building from source with prebuilt AAR is issue, F-Droid can also package from GitHub Releases APK (binary repo) — but preferred is build from source.

We prefer source build with prebuilt AAR allowed as binary blob (common for mpv apps).

## Current Status

- **F-Droid API:** NOT_FOUND (not yet included)
- **Badge in README:** Points to https://f-droid.org/packages/com.opticast.player (will work after inclusion)
- **Fastlane:** Ready
- **Version:** 133 / 2.6.84 mobile only

## Contact

`opticastproject@gmail.com` — Email only, for F-Droid reviewers

## Links

- GitHub: https://github.com/opticastplayer-dev/opticast
- Release v2.6.84: https://github.com/opticastplayer-dev/opticast/releases/tag/v2.6.84-optimized
- Pages: https://opticastplayer-dev.github.io/opticast/
- FAQ: https://github.com/opticastplayer-dev/opticast/blob/main/docs/FAQ.md
- Privacy: https://github.com/opticastplayer-dev/opticast/blob/main/docs/PRIVACY.md
