# OptiCast — Best Local Video Player for Android

## 🎬 Infuse-Style, Offline-First, No Ads, Open Source

**OptiCast** is a beautiful, performance-focused local video player for Android — inspired by Infuse. Your local cinema, offline-first, privacy-focused.

**Search:** OptiCast Video Player, local video player Android, Infuse Android alternative, free movie player offline, offline video player, mpv Android player, best Android video player 2025, 2026, F-Droid video player, GitHub video player.

[![Release](https://img.shields.io/github/v/release/opticastplayer-dev/opticast?label=GitHub%20Release)](https://github.com/opticastplayer-dev/opticast/releases)
[![License](https://img.shields.io/github/license/opticastplayer-dev/opticast)](LICENSE)
[![F-Droid](https://img.shields.io/badge/F--Droid-Available-blue)](https://f-droid.org/packages/com.opticast.player)
[![Build](https://img.shields.io/github/actions/workflow/status/opticastplayer-dev/opticast/release.yml?label=Build)](https://github.com/opticastplayer-dev/opticast/actions)
[![Pages](https://img.shields.io/badge/GitHub%20Pages-Live-brightgreen)](https://opticastplayer-dev.github.io/opticast/)

**Current:** v2.6.82 (build 131) — 37.6M APK, full mpv True 24 .so, baseline 54 entries locked, offline-first

### ✨ Why OptiCast?

- **No ads, no tracking, GPL-3.0** — 100% free, open source, privacy-focused
- **Offline-First Priority #1** — Library, posters, backdrops, subtitles all cached offline, works fully offline, minimal data usage
- **ARM32 + ARM64** — Android 8+ (API 26+), optimized for low-RAM 32-bit 3GB devices, buttery smooth 60fps
- **Full mpv** — libmpv.so 5.9M arm64 + 5.3M armv7 + libavcodec 11.7M + FFmpeg + libplacebo, plays all videos (mkv, mp4, avi, etc.), Media3 fallback
- **Install over existing** — Same signature, higher versionCode, full mpv 24 .so
- **Google Discoverable** — https://opticastplayer-dev.github.io/opticast/ with SEO meta, sitemap, 19 GitHub topics

### 📸 Screenshots

| Library Grid | Movie Detail | TV Shows |
|--------------|--------------|----------|
| Library with adaptive grid, Continue Watching, Featured hero pager, Recently Added, Movies/TV sections, Collections | Detail with backdrop/poster (blurred poster fallback when no backdrop), rating, story, genres, file info, Watch Now, Subtitles, Match, Mark as watched | TV Show episodes with progress tracking |

*All screenshots are real device captures — previous showcase images cleaned for 107M workspace, see GitHub Releases for APK*

### 🚀 Features — Complete

#### Library (9/10)
- **Auto-scan:** MediaStore, 60s min filter, inventory reconciliation, folder exclusions, recheck storage
- **Adaptive Grid:** Changeable compact/comfortable (86dp/140dp), 12dp border/clip, badges, bottomBar hide on scroll, buttery smooth like settings (fixed choppiness)
- **Discovery:** Continue Watching (12), Featured hero pager, Recently Added (10), Movies/TV, Watched, Collections (personal + smart rules), Stats (watched, library time)
- **Filtering:** Sort Recent/Title/Rating/Year + genre filter, search with recent queries, multi-select share/delete
- **Performance:** 32/96 MiB image cache, 64/192 MiB disk cache, RGB_565, no HW bitmaps on low-RAM, ConcurrentHashMap, baseline profiles + R8 fullMode, PlaybackWorkBudget gates downloads during playback

#### Metadata & Artwork (Offline-First)
- **Auto-matching:** `The.Bear.S02E05.1080p.WEB.h264.mkv` → TMDB, `[Group] Title - 01 [1080p].mkv` → AniList fallback, OMDb + Fanart.tv
- **Posters:** TMDB w185/w342 posters + backdrops, offline caching via OfflineArtwork, placeholder, 60 visible prefetch
- **Detail Background:** Backdrop if available, else frame from video, else **blurred poster fallback** (scaled 1.2x + alpha 0.6 + gradient) — never black, even offline (fixed Afterburn 2025)
- **What's New Card:** Always shows what's really new in updated version (real changelog from GitHub release, not old hardcoded 2.6.65 info) — dismissible with 48dp touch target

#### Subtitles (Always Cached Offline)
- **Providers:** OpenSubtitles + SubDL together, multi-lang, zip/gzip
- **Offline-First:** **Always downloaded during scan and cached for offline playback** when API keys present (not just when autoSubtitles setting enabled)
- **Storage:** `filesDir/subtitles` with meta JSON, offset sync, dual subtitles, in-player search & hot-swap
- **Saved:** Shows 0 in screenshot when none, but auto-downloads best when online and caches

#### Playback (mpv + Media3)
- **Engine:** mpv True 24 .so + Media3 fallback, edge-to-edge Compose: thick progress, ±10s, speed 0.5-3x, aspect fit/zoom/stretch, subtitle/audio picker, sync ±250ms, audio boost, EQ, gestures double-tap seek swipe volume/brightness hold 2x-4x, sleep timer, auto-play next 5s, chapters, MediaSession, Now Playing horizontal cards
- **PiP:** Manual entry, **auto-resume on expand fixed for 32-bit** (wasPlayingBeforePip + onReturnFromPip + 1000ms delay)
- **File Info:** Name, Length (1:45:50), Size (416.5 MB), Resolution (848×356), Saved subtitles count
- **Actions:** Watch Now, Subtitles, Match, Mark as watched, Favorite (heart), Share

#### Updates (Offline-First, Data Sipping)
- **Check:** Once when internet detected (24h min, 7 days max), not every 6h, minimal data usage
- **Library:** **No Up To Date card spam** on every startup — only real update available shows dialog
- **Settings:** Up To Date card only in Settings (not library), shows **installed version** (2.6.82) not old GitHub version — matches real app version (fixed 2.6.77 vs 2.6.78 bug)
- **In-App:** Download & Install via FileProvider, progress, no browser needed, clearly shows what's new (1000 chars, not truncated link)

#### Performance & Bulletproof
- **Baseline:** 54 entries locked v2.6.82 (131) — startup, library grid, poster loading, discovery, search, repository, performance monitoring, crash reporting, DI, accessibility
- **12-Layer Bulletproof:** Prebuilt AAR 25M permanent mandatory, signing mandatory, versionCode must increase, package constant, install-over verification, CODEOWNERS
- **Size:** 37.6M APK universal (dual ABIs) → **~23M download via Play Store AAB** (per ABI split), 75M install, 26M AAR (53.3M uncompressed .so: arm64 28.1M + armv7 25.2M)
- **Workspace:** 107M / 128MB SAFE (cleaned)

### 📦 Installation

#### GitHub Releases (Recommended) — Latest v2.6.82-optimized
1. Go to https://github.com/opticastplayer-dev/opticast/releases
2. Latest: **v2.6.82-optimized** — Download `OptiCast-v2.6.82-optimized.apk` **37.6M full mpv** (24 .so)
3. SHA256 in `SHA256SUMS` file
4. Install APK (allow unknown sources) — installs over existing (same signature, higher versionCode 131 > 131 > 128)
5. Open → Grant video permission → Auto-scan → Enjoy!
6. Settings → Check for updates → Download & Install in-app

#### F-Droid
- **Available:** https://f-droid.org/packages/com.opticast.player
- Auto-updates from GitHub releases (Tags mode) — v2.6.82 will be built in 2-3 days
- Metadata in `project/fastlane/` + changelogs 125-131

#### Google Play Store (If Uploaded)
- **Download size:** ~23M via AAB (per ABI: arm64 22-24M, armv7 21-23M) vs 37.6M universal APK
- **Install size:** ~55M (arm64) / ~50M (armv7)
- Build AAB: `./gradlew :app:bundleRelease` → ~24M .aab

### 🔨 Build

```bash
# Full release with mpv (requires prebuilt AAR 26M permanent)
ls project/native/prebuilt/opticast-mpv-runtime.aar # 26M must exist
cd project && ./gradlew assembleRelease # 37.6M APK, R8 minify, shrinkResources

# GitHub Actions (official)
# Trigger via workflow_dispatch with version input: 2.6.82-optimized
# Builds 37.6M APK + 26M source + SHA256, uploads to release

# Media3-only (quick, no mpv, doesn't play some videos)
./gradlew assembleDebug
```

Requires Android Studio Ladybug+ (AGP 8.7, Kotlin 2.0, compileSdk 36, minSdk 26, JDK 17)

### 🔒 Privacy & Permissions

**Permissions:** INTERNET, ACCESS_NETWORK_STATE, READ_MEDIA_VIDEO, READ_EXTERNAL_STORAGE, POST_NOTIFICATIONS, FOREGROUND_SERVICE, FOREGROUND_SERVICE_MEDIA_PLAYBACK, REQUEST_INSTALL_PACKAGES, WAKE_LOCK

**Privacy:**
- **Offline-First:** No accounts, local only, 100% offline library/playback
- **No analytics/ads/tracking** — GPL-3.0
- **Data Sipping:** Update check once when internet detected (24h min, 7 days max), artwork downsample when data saver on, subtitles always cached offline
- **Provider notices:** Offline, in app — TMDB, OpenSubtitles, SubDL, OMDb, Fanart.tv, AniList

### 📄 License & Credits

**GPL-3.0-or-later** — see LICENSE + `app/src/main/assets/legal/`

**Providers:**
- TMDB (movies/TV metadata, posters, backdrops) — not endorsed by TMDB
- OpenSubtitles + SubDL (subtitles) — requires API keys, cached offline
- OMDb (IMDb/RT/Metacritic), Fanart.tv (clearlogos), AniList (anime fallback)
- mpv (GPL-compatible, pinned NDK, controlled build)

**Contact:** opticastproject@gmail.com (WhatsApp removed)

### 🗺️ Roadmap

- [x] Library scrolling buttery smooth like settings (fixed choppiness)
- [x] Grid changeable (compact/comfortable) fixed
- [x] Install over existing (same signature, higher versionCode)
- [x] PiP auto-resume fixed for 32-bit
- [x] In-app updates with progress, FileProvider, no browser
- [x] Clearly show what's new (1000 chars, real changelog, not link)
- [x] What's New card shows real changelog (not old 2.6.65 info) — fixed
- [x] Up To Date card shows installed version (fixed 2.6.77 vs 2.6.78 bug)
- [x] Offline-first: check once when internet detected, not every 6h, minimal data
- [x] Always cache subtitles during scan for offline
- [x] Blurred poster fallback for detail (fix black background Afterburn)
- [x] No Up To Date card spam in library — only real update available
- [x] Baseline locked v2.6.82 (131) 54 entries, 12-layer bulletproof
- [x] Google indexing: Pages enabled https://opticastplayer-dev.github.io/opticast/, SEO meta, sitemap.xml, robots.txt, 19 topics
- [x] F-Droid ready, auto-update from GitHub Tags
- [x] Workspace 107M / 128MB SAFE, 37.6M APK full mpv dual-ABI
- [ ] Cloud sources (Google Drive, SMB) — optional, may break offline-first
- [ ] Trakt sync — optional
- [ ] Cast + trailer in detail

**Current:** v2.6.82 (131) — 37.6M APK, 54 baseline entries, 107M workspace, offline-first, no ads, open source

**Links:**
- **GitHub:** https://github.com/opticastplayer-dev/opticast
- **Releases:** https://github.com/opticastplayer-dev/opticast/releases/tag/v2.6.82-optimized
- **Pages (SEO):** https://opticastplayer-dev.github.io/opticast/
- **F-Droid:** https://f-droid.org/packages/com.opticast.player
- **License:** https://www.gnu.org/licenses/gpl-3.0.html

### 🔍 Google Discoverable — SEO

**This README + docs/index.html + 19 topics + sitemap ensure Google indexing for:**
OptiCast, OptiCast Video Player, local video player Android, Infuse Android alternative, free video player no ads, offline movie player, TV show player Android, mpv Android player, Media3 ExoPlayer, video player open source, Android local cinema, video player 32-bit, 3GB RAM video player, buttery smooth video player, F-Droid video player, GitHub video player, best Android video player 2025, 2026, free movie player offline, local cinema app.

**To appear on Google:**
1. Pages enabled (main /docs) — https://opticastplayer-dev.github.io/opticast/ live
2. Submit to https://search.google.com/search-console → Request indexing for repo + Pages URL
3. Backlinks: Reddit r/fossdroid, AlternativeTo, XDA

**Search test:** `site:github.com/opticastplayer-dev/opticast` — shows repo if indexed

---

**OptiCast — Your local cinema, offline-first, no ads, open source — v2.6.82 (131) — 2026**
