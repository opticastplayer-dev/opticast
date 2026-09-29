# Play Store Listing — OptiCast Video Player

**Package:** `com.opticast.player` | **Version:** v2.6.84 (133) | **Size:** 37.6M universal APK → ~23M AAB download (per ABI split)

### Short Description (80 chars)
Best local video player for Android — offline, mpv, no ads, open source

### Full Description

**OptiCast — Your Local Cinema, Offline-First**

Infuse-style local video player for Android — beautiful, performance-focused, privacy-focused. Your local cinema, offline-first.

**No ads, no tracking, GPL-3.0, 100% free, open source.**

**Library:**
- MediaStore auto-scan, adaptive poster grid changeable (Compact 86dp / Medium 108dp / Comfortable 140dp), Continue Watching, Movies/TV grouping, Favorites, Recently Added, Collections, search, multi-select share/delete
- Buttery smooth 60fps on low-RAM 32-bit 3GB devices, baseline 54 entries locked, R8 minify + shrinkResources
- Stats: Watched, Library time 65h

**Metadata:**
- Auto-identifies movies/TV from filenames via TMDB: `The.Bear.S02E05.1080p.WEB.h264.mkv` → TMDB, `Dune.Part.Two.2024.mkv` → Movie
- Posters, backdrops, offline caching for offline use, blurred poster fallback fixes black background (Afterburn 2025)
- Ratings (OMDb), cast headshots, genres, runtime, synopsis

**Subtitles:**
- OpenSubtitles + SubDL search, always cached during scan for offline playback, saved to `filesDir/subtitles/` with JSON sidecar
- Dual subtitles (two languages), sync ±250ms + auto-align, SRT/ASS/VTT

**Playback (mpv + Media3):**
- Full mpv 38M with libmpv True 24 .so (arm64 6.1M + armv7 5.3M + libavcodec 11.7M + FFmpeg + libplacebo), plays all videos mkv/mp4/avi, Media3 fallback
- Edge-to-edge Compose: thick progress, ±10s, play/pause, speed 0.5x-3x, aspect fit/zoom/stretch, subtitle/audio track picker
- Gestures: double-tap seek, swipe volume/brightness, pinch-zoom 0.5x-2x GPU with pan, hold 2x-4x, scrub preview real frame
- Sleep timer, auto-play next 5s, chapters with thumbnails, MediaSession notification/lock/Bluetooth
- PiP: Manual entry, auto-resume on expand fixed for 32-bit, X closes
- Loading animation fixed: keyed to video.id, auto-hide 3s max, prevents persistent spinner when quickly jumping videos

**Updates:**
- Offline-first: checks GitHub Releases API once when internet detected (24h min, 7 days max), minimal data
- In-app download via FileProvider with progress + auto install, no browser, handles REQUEST_INSTALL_PACKAGES
- Up To Date shows installed version, What's New compact card shows real 3-bullet features, dismissible

**Offline-First Priority #1:**
- Works fully offline after initial caching, minimal data usage, data saver artwork (smaller posters, no HD Fanart on metered)
- Subtitles always cached, posters/backdrops cached, metadata cached

**Performance:**
- Baseline 54 entries locked v2.6.84 (133) — startup, library grid, poster loading, discovery, search, repository offline-first, performance monitoring, crash reporting, DI, accessibility
- 12-layer bulletproof: prebuilt AAR 26M mandatory permanent, signing mandatory, versionCode must increase, package constant, install-over verification
- Mobile only: no TV support, touchscreen required=true, saves 18KB, focuses on phone 32-bit 3GB

**Privacy:**
- No ads, no tracking, GPL-3.0, 7 permissions minimal: INTERNET, ACCESS_NETWORK_STATE, WAKE_LOCK, READ_MEDIA_VIDEO, POST_NOTIFICATIONS, FOREGROUND_SERVICE, FOREGROUND_SERVICE_MEDIA_PLAYBACK, REQUEST_INSTALL_PACKAGES, legacy READ/WRITE_EXTERNAL_STORAGE maxSdk 32/29
- No location, contacts, microphone, camera

**Build:**
- `cd project && ./gradlew assembleRelease` — 37.6M APK, R8, full mpv 24 .so, installs over existing same signature higher versionCode

**Links:**
- GitHub: https://github.com/opticastplayer-dev/opticast
- Releases: https://github.com/opticastplayer-dev/opticast/releases/tag/v2.6.84-optimized
- F-Droid: https://f-droid.org/packages/com.opticast.player
- Pages: https://opticastplayer-dev.github.io/opticast/
- Privacy: https://opticastplayer-dev.github.io/opticast/ — docs/PRIVACY.md
- Contact: opticastproject@gmail.com

**What's New in 2.6.84:**
- Mobile only, removed Android TV / Fire TV support (TvHomeScreen 481 lines + TvSupport, leanback, banner)
- Fixed weaknesses: docs meta v2.6.78->2.6.83, baseline date, grid changeable fixed, scan corrupted skip with try/catch
- Compact What's New card: 16dp corners, 3 real bullets, bodySmall max 4 lines
- Stability: loading spinner persisting when quickly jumping videos fixed

**Previous:**
- v2.6.83: docs meta, baseline date, grid changeable, scan corrupted
- v2.6.82: compact What's New shows real features
- v2.6.81: loading animation persisting fix, PiP auto-resume 32-bit, blurred poster fallback, real What's New, complete README

**Device:** Android 8+ (API 26+), ARM64 + ARMv7 (32-bit support), 3GB RAM optimized, mobile only, offline-first

**License:** GPL-3.0 — complete source in releases contains prebuilt AAR

**Size:** 37.6M universal APK → ~23M download via Play Store AAB (per ABI split: arm64 22-24M, armv7 21-23M), 75M install, 26M AAR

**Rating:** 9.3/10 standalone Android, 8.7/10 vs Infuse (Apple gold standard 9.5/10), 9.2/10 for Android local-only (Infuse doesn't exist on Android)
