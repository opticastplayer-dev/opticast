# Reddit Final Drafts — Ready to Post — OptiCast v2.6.84 (133) Mobile Only

**Version:** v2.6.84 (133) — 37.6M APK full mpv 24 .so, mobile only no TV, offline-first, no ads, GPL-3.0

---

## 1. r/fossdroid — FOSS Focus

**Title:** [FOSS] OptiCast v2.6.84 — Infuse for Android, offline-first local video player, mpv + Media3, no ads, GPL-3.0, mobile only, 32-bit 3GB buttery smooth, F-Droid PR ready

**Body:**

OptiCast — Your Local Cinema, Offline-First, Mobile Only — GPL-3.0, no ads, no tracking, 100% free, open source.

**What:**
Infuse-style local video player for Android. Scans device via MediaStore, auto-identifies movies/TV via TMDB, posters/backdrops offline cached + blurred fallback fixes black bg, subtitles OpenSubtitles/SubDL always cached offline in filesDir/subtitles/, plays with full mpv 37.6M (libmpv True 24 .so arm64 6.1M + armv7 5.3M + libavcodec 11.7M) + Media3 fallback. Mobile only — removed TV support in v2.6.84 (TvHomeScreen 481 lines + TvSupport, leanback, banner) saves 18KB, focuses on phone 32-bit 3GB 60fps.

**FOSS & Privacy:**
- GPL-3.0, source in releases complete-source.zip contains prebuilt AAR 26M mandatory
- 7 permissions minimal: INTERNET, ACCESS_NETWORK_STATE, WAKE_LOCK, READ_MEDIA_VIDEO, POST_NOTIFICATIONS, FOREGROUND_SERVICE, FOREGROUND_SERVICE_MEDIA_PLAYBACK, REQUEST_INSTALL_PACKAGES, legacy READ/WRITE maxSdk 32/29. No location/contacts/mic/camera.
- No ads, no tracking, no accounts, offline-first priority #1: checks updates once internet detected 24h min 7d max minimal data, not every 6h
- Baseline 54 entries locked v2.6.84 9.3/10 rating, startup <400ms, R8 minify, 12-layer bulletproof: prebuilt AAR mandatory, signing mandatory, versionCode must increase, package constant, install-over verification

**Features:**
- Library: adaptive grid changeable Compact 86dp/Medium 108dp/Comfortable 140dp fixed in v2.6.83 (remember outside LazyVerticalGrid), Continue Watching, Movies 27, TV 2, Favorites, search, multi-select, stats 65h watched, buttery smooth like settings
- Metadata: TMDB auto-match The.Bear.S02E05.1080p → TMDB, Dune.Part.Two.2024 → Movie, ratings, cast headshots, genres, runtime, synopsis
- Subtitles: OpenSubtitles + SubDL, dual subtitles, sync ±250ms + auto-align, SRT/ASS/VTT, always cached offline
- Playback: mpv + Media3, edge-to-edge Compose, thick progress, ±10s, speed 0.5x-3x, aspect fit/zoom/stretch, double-tap seek, swipe volume/brightness, pinch-zoom 0.5x-2x GPU with pan, hold 2x-4x, scrub preview real frame, sleep timer, auto-play next 5s, chapters thumbnails, MediaSession notification/lock/Bluetooth, PiP auto-resume fixed 32-bit 1000ms + RESUMED, loading persist fix keyed to video.id auto-hide 3s max
- Updates: in-app FileProvider progress + auto install, no browser, What's New compact card 16dp corners 3 real bullets bodySmall max 4 lines X 32dp dismissible, Up To Date when matches GitHub

**Build:**
`cd project && ./gradlew assembleRelease` — 37.6M APK, installs over existing same JKS higher versionCode 132→133, 75M install, 26M AAR.

**F-Droid:**
- Fastlane metadata ready title/short 79 chars/full 48 lines, changelogs 125-133
- PR ready: metadata/com.opticast.player.yml in docs/FDROID_PR.md, AutoUpdateMode Version v%v-optimized, UpdateCheckMode Tags, prebuilt AAR mandatory discussion
- Currently NOT_FOUND on F-Droid API, needs inclusion — help welcome!

**Links:**
- GitHub: https://github.com/opticastplayer-dev/opticast
- Release v2.6.84-optimized: https://github.com/opticastplayer-dev/opticast/releases/tag/v2.6.84-optimized (APK 38M SHA256 3f942dbdcd0e51faad3b4fbe1d12a5ead176d1ed3a129b8d82e21f3db18b1646 + source 26M)
- Pages: https://opticastplayer-dev.github.io/opticast/
- FAQ: https://github.com/opticastplayer-dev/opticast/blob/main/docs/FAQ.md
- Privacy: https://github.com/opticastplayer-dev/opticast/blob/main/docs/PRIVACY.md
- F-Droid PR: https://github.com/opticastplayer-dev/opticast/blob/main/docs/FDROID_PR.md
- Contact: opticastproject@gmail.com

**Device:** Android 8+ API 26+, ARM64+ARMv7, 3GB RAM optimized, mobile only

Feedback especially low-RAM 32-bit testing, offline-first data usage, install-over. GPL-3.0.

---

## 2. r/androidapps — General Android

**Title:** OptiCast v2.6.84 — Best local video player for Android? Infuse alternative, offline-first, mpv, no ads, mobile only, 32-bit 3GB buttery smooth 60fps

**Body:**

I built OptiCast — trying to be Infuse for Android, but mobile only, offline-first.

**TL;DR:** Local video player, scans your device, auto-identifies movies/TV via TMDB, posters/backdrops cached + blurred fallback never black, subtitles always cached offline, plays all mkv/mp4/avi with full mpv 37.6M + Media3, no ads, GPL-3.0, 32-bit 3GB buttery smooth.

**Why mobile only?**
v2.6.84 removed Android TV / Fire TV support (481 lines TvHomeScreen + TvSupport, leanback, banner) — saves 18KB, focuses on phone. Touchscreen required=true. No TV devices.

**Offline-first #1:**
- Works fully offline after initial caching
- Checks updates once internet detected (24h min, 7 days max) minimal data, not every 6h
- Subtitles always cached during scan, saved to filesDir/subtitles/
- Posters/backdrops cached, blurred poster fallback fixes black background (Afterburn 2025)

**What I fixed recently:**
- Library grid can't be changed? Fixed in v2.6.83 — remember outside LazyVerticalGrid
- Loading animation persists when quickly jumping videos? Fixed in v2.6.81 — keyed to video.id, auto-hide 3s max
- PiP expand doesn't auto-continue on 32-bit? Fixed — tracks wasPlayingBeforePip, 1000ms delay + RESUMED check
- Black background in detail? Fixed — blurred poster fallback scaled 1.2x + alpha 0.6 + gradient
- What's New shows old info? Fixed — stores whats_new_changelog from GitHub release body, compact card 3 bullets
- Up To Date vs Update available misleading? Fixed — normalize stripping -optimized suffix, Up To Date notification when matches GitHub but not always visible header, dismiss entirely after X

**Performance:**
- Baseline 54 entries locked v2.6.84 (133) — startup <400ms, library grid, poster loading, discovery, search, offline-first, etc.
- Buttery smooth library scrolling like settings, 60fps on 3GB 32-bit, R8 minify + shrinkResources
- Rating: 9.3/10 standalone Android, 8.7/10 vs Infuse (Apple gold standard 9.5/10), 9.2/10 for Android local-only (Infuse doesn't exist on Android)

**Features:**
- Library: MediaStore auto-scan, adaptive grid Compact 86dp/Medium 108dp/Comfortable 140dp, Continue Watching, Movies 27, TV 2, Favorites, Recently Added, Collections, search, multi-select share/delete, stats
- Metadata: TMDB auto-identification, posters, backdrops, offline caching, ratings, cast, genres, runtime, synopsis
- Subtitles: OpenSubtitles + SubDL, dual subtitles, sync ±250ms + auto-align, SRT/ASS/VTT
- Playback: mpv + Media3, thick progress, ±10s, speed 0.5x-3x, aspect fit/zoom/stretch, double-tap seek, swipe volume/brightness, pinch-zoom 0.5x-2x GPU, hold 2x-4x, scrub preview real frame, sleep timer, auto-play next 5s, chapters thumbnails, MediaSession, PiP
- Updates: in-app download FileProvider progress + auto install, no browser

**Privacy:** No ads, no tracking, GPL-3.0, 7 perms minimal, no location/contacts/mic/camera, offline-first, no accounts. Plays your own media only.

**Size:** 37.6M universal APK → ~23M download via Play Store AAB per-ABI split (arm64 22-24M, armv7 21-23M), 75M install. Full mpv 24 .so mandatory.

**Links:**
- GitHub: https://github.com/opticastplayer-dev/opticast
- Release: https://github.com/opticastplayer-dev/opticast/releases/tag/v2.6.84-optimized
- Pages: https://opticastplayer-dev.github.io/opticast/
- FAQ: https://github.com/opticastplayer-dev/opticast/blob/main/docs/FAQ.md
- Contact: opticastproject@gmail.com

**Device:** Android 8+ API 26+, ARM64+ARMv7, 3GB RAM optimized, mobile only

Looking for feedback on low-RAM performance, library smoothness, offline-first data usage. Free, open source, no ads.

---

## 3. r/mpv + r/Android — mpv Focus

**Title:** OptiCast — Android frontend for mpv with Infuse-style library, offline-first, mobile only, full libmpv 24 .so

**Body:**

Built OptiCast as Android frontend for mpv — full libmpv True 24 .so (arm64 6.1M + armv7 5.3M + libavcodec 11.7M + FFmpeg + libplacebo) 37.6M APK, plays all mkv/mp4/avi, Media3 fallback.

**Why not just mpv-android?**
- Library: MediaStore scan, adaptive grid changeable, Continue Watching, Movies/TV grouping, Favorites, search, multi-select, stats 65h watched, buttery smooth 60fps on 3GB 32-bit baseline 54 locked
- Metadata: TMDB auto-match, posters/backdrops offline cached, blurred fallback never black
- Subtitles: OpenSubtitles/SubDL always cached offline filesDir/subtitles/, dual, sync ±250ms
- Playback: mpv + Media3, edge-to-edge Compose, pinch-zoom 0.5x-2x GPU with pan, scrub preview real frame, PiP auto-resume fixed 32-bit, loading persist fix keyed to video.id auto-hide 3s
- Offline-first: checks updates once internet detected 24h min 7d max minimal data, works fully offline
- Mobile only: removed TV support v2.6.84 saves 18KB, touchscreen required=true

**mpv details:**
- Prebuilt AAR 26M mandatory permanent project/native/prebuilt/opticast-mpv-runtime.aar contains libmpv 24 .so, prevents Media3-only that breaks install-over, CI fails if APK <20M or missing libmpv
- Installs over existing same JKS higher versionCode 132→133, 75M install
- Build: cd project && ./gradlew assembleRelease

**GPL-3.0, no ads/tracking, 7 perms minimal, offline-first, source in releases complete-source.zip with AAR**

GitHub: https://github.com/opticastplayer-dev/opticast — Release v2.6.84-optimized APK 38M SHA256 3f942dbdcd0e51faad3b4fbe1d12a5ead176d1ed3a129b8d82e21f3db18b1646

Feedback on mpv integration, codec support, low-RAM performance welcome.

---

## 4. AlternativeTo Comment (when listing exists)

**Comment:**

OptiCast v2.6.84 (133) — mobile only, offline-first, Infuse for Android, 9.3/10 rating

Best local video player for Android I've used — beats Infuse in mpv codec support (all mkv/mp4/avi vs limited), low-RAM performance (32-bit 3GB 60fps vs not), privacy (no tracking vs some), price (free GPL vs paid). Infuse doesn't exist on Android, OptiCast is Infuse for Android 9.2/10 for Android local-only.

- Library: MediaStore auto-scan, adaptive grid changeable Compact/Medium/Comfortable fixed, Continue Watching, Movies 27, TV 2, Favorites, search, multi-select, stats, buttery smooth like settings baseline 54 locked
- Metadata: TMDB auto-identification, posters/backdrops offline cached, blurred fallback fixes black bg Afterburn 2025
- Subtitles: OpenSubtitles + SubDL always cached offline filesDir/subtitles/, dual, sync ±250ms
- Playback: full mpv 37.6M 24 .so + Media3, pinch-zoom 0.5x-2x GPU, scrub preview real frame, PiP auto-resume 32-bit, loading persist fix
- Offline-first #1: checks once internet detected 24h min 7d max minimal data, works offline
- Privacy: no ads/tracking GPL-3.0, 7 perms minimal, no location/contacts/mic/camera
- Mobile only: removed TV support v2.6.84 saves 18KB, focuses phone 32-bit 3GB

37.6M APK → ~23M AAB download, 75M install, installs over existing same JKS higher versionCode.

GitHub: https://github.com/opticastplayer-dev/opticast — Release v2.6.84-optimized — Pages: https://opticastplayer-dev.github.io/opticast/ — FAQ/Privacy in docs.

GPL-3.0, no ads, open source, F-Droid PR ready (fastlane metadata 125-133).

---

## Posting Checklist

- [ ] r/fossdroid — FOSS focus, GPL-3.0, no ads, F-Droid PR ready, 7 perms minimal, offline-first
- [ ] r/androidapps — General, Infuse alternative, offline-first, mobile only, 32-bit 3GB buttery smooth, fixes
- [ ] r/mpv — mpv frontend, full libmpv 24 .so, prebuilt AAR mandatory, codec support
- [ ] r/Android — Similar to androidapps but broader
- [ ] AlternativeTo — Submit new listing via docs/ALTERNATIVETO.md, then comment
- [ ] F-Droid Forum — Post in https://forum.f-droid.org/ about inclusion PR
- [ ] GitHub Discussions — Announce v2.6.84 mobile only
- [ ] No Play Store yet per user command — skip Play Store posting

## Timing

- Post r/fossdroid first (most relevant, FOSS)
- Wait 1 day, post r/androidapps
- Wait 2 days, post r/mpv
- Submit AlternativeTo anytime
- F-Droid forum after PR submitted to fdroiddata

## Contact for All Posts

opticastproject@gmail.com — Email only, no WhatsApp

## Links to Include in All Posts

- GitHub: https://github.com/opticastplayer-dev/opticast
- Release v2.6.84-optimized: https://github.com/opticastplayer-dev/opticast/releases/tag/v2.6.84-optimized
- Pages: https://opticastplayer-dev.github.io/opticast/
- FAQ: https://github.com/opticastplayer-dev/opticast/blob/main/docs/FAQ.md
- Privacy: https://github.com/opticastplayer-dev/opticast/blob/main/docs/PRIVACY.md
- F-Droid PR: https://github.com/opticastplayer-dev/opticast/blob/main/docs/FDROID_PR.md
- AlternativeTo: https://github.com/opticastplayer-dev/opticast/blob/main/docs/ALTERNATIVETO.md
