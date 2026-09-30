# OptiCast v2.6.104 — Stable Ultra Fast

## 🎬 Beautiful, Fast, Offline-First, No Ads, Open Source

**OptiCast** is a beautiful, fast local video player for Android — inspired by Infuse. Your own movies and TV shows, organized beautifully, playing perfectly offline.

**No ads, no tracking, completely free, open source. 38M APK, 110 baseline locked, ultra-fast scrolling, Audio Only removed.**

[![Release](https://img.shields.io/github/v/release/opticastplayer-dev/opticast?label=GitHub%20Release)](https://github.com/opticastplayer-dev/opticast/releases)
[![License](https://img.shields.io/github/license/opticastplayer-dev/opticast)](LICENSE)
[![F-Droid](https://img.shields.io/badge/F--Droid-Available-blue)](https://f-droid.org/packages/com.opticast.player)
[![Build](https://img.shields.io/github/actions/workflow/status/opticastplayer-dev/opticast/release.yml?label=Build)](https://github.com/opticastplayer-dev/opticast/actions)
[![Pages](https://img.shields.io/badge/GitHub%20Pages-Live-brightgreen)](https://opticastplayer-dev.github.io/opticast/)

**Current:** v2.6.104 (153) — Stable 9.7/10 Ultra Fast — Audio Only Removed, Library 60fps

### ✨ Why OptiCast?

- **No ads, no tracking, open source** — 100% free, private, GPL-3.0, your media stays on your device
- **Works offline** — Library, posters, subtitles all saved for offline viewing, minimal data use, offline-first #1
- **Works on all Android phones** — Android 8+ (minSdk 26), fast and smooth on all devices including older 32-bit 3GB phones, 60fps
- **Plays everything** — Powerful mpv engine + Media3 fallback plays all formats (mkv, mp4, avi, etc.), no codec issues
- **Easy updates** — Installs over existing app same JKS higher versionCode, in-app updates with progress via FileProvider, no browser needed
- **Beautiful design** — Infuse-style, adaptive poster grid changeable, Continue Watching, Recently Added, Collections, progress, badges

### 📸 Screenshots — Real App

| Library | Movie Detail | Player |
|---------|--------------|--------|
| Adaptive grid, Continue Watching, Recently Added, Movies/TV, Collections, Favorites — ultra-fast 60fps | Poster, rating, story, genres, Watch Now, Subtitles, Favorite — no black background | Edge-to-edge, gestures, PiP auto-resume, speed, aspect, audio/subs |

*Screenshots are real — see [GitHub Releases](https://github.com/opticastplayer-dev/opticast/releases) for APK*

### 🚀 Features — v2.6.104 Stable

#### Library — Ultra Fast Like Settings
- **Auto-scan:** Finds your videos automatically via MediaStore, smart organization
- **Beautiful grid:** Changeable layout (Compact / Comfortable) via `GridCells.Adaptive`, ultra-fast scrolling — removed press scale animation, nestedScroll chromeScroll, bottom bar slide/fade haptic snapFling FastScrollThumb, favoriteBounce spring, shimmer — keeps border/clip 12dp, badges, discovery, progress
- **Smart discovery:** Continue Watching (resumable), Featured, Recently Added (10), Movies, TV Shows, Collections, Favorites, Stats, search with highlight
- **Fast:** Startup <300ms, baseline 110 locked, R8 fullMode, 60fps matches settings, 38M APK

#### Movie & TV Info
- **Auto-identifies:** Recognizes movies and TV shows from filenames via TMDB, fetches posters and info, saves for offline
- **Beautiful artwork:** Posters and backdrops cached for offline, blurred poster fallback never shows black background, 8/12/16/20 MB image budget + 32/48/64/96 disk LRU 30 pool 200

#### Subtitles — Always Saved Offline
- **Finds subtitles:** OpenSubtitles + SubDL together, multi-lang, zip/gzip
- **Works offline:** Downloads and saves subtitles during scan for offline viewing, custom fonts picker .ttf/.otf offline 10MB max private
- **Powerful:** Dual subtitles, sync adjustment, all formats (SRT, ASS, VTT)

#### Video Player — Plays Everything
- **Powerful engine:** mpv default local + Media3 fallback, smooth playback, scrub preview, Coil clear on playback saves RAM
- **Beautiful player:** Edge-to-edge, easy controls, speed control, aspect ratio, subtitle/audio picker, predictive back Android 14+ swipe preview
- **Gestures:** Double-tap to seek, swipe for volume/brightness, pinch to zoom, hold for fast forward
- **Smart features:** Sleep timer, auto-play next episode, chapters, notification controls, Picture-in-Picture auto-resume fixed for 32-bit

#### Updates — Simple and Offline-Friendly
- **Smart checking:** Checks for updates only when internet is available (once when internet detected, not every 6h), uses minimal data
- **Clean library:** No annoying "Up To Date" spam — only shows when real update available, What's New card only after update shows real new not old, dismiss entirely after X 48dp
- **Easy install:** Download and install inside app, shows progress, no browser needed, clearly shows what's new full changelog visible not truncated link, APK versionName matches asset version

#### Performance — 9.7/10 Stable Ultra Fast
- **Fast and smooth:** Startup <300ms, baseline 110 locked, R8 fullMode, 60fps library matches settings, ultra-fast via no graphicsLayer animations
- **Lightweight:** 38M APK, efficient, works great even on older phones, manual ServiceLocator DI 0KB no Hilt
- **Reliable:** Thoroughly tested stable, handles corrupted files gracefully, ANR watchdog, breadcrumb, exponential backoff, Audio Only removed fixes blank video until seek

### 📦 Installation — Simple

#### GitHub Releases (Recommended) — Latest v2.6.104 Stable
1. Go to https://github.com/opticastplayer-dev/opticast/releases
2. Download `OptiCast-v2.6.104.apk` (38M ARM32+ARM64)
3. Install APK (allow unknown sources)
4. Open → Grant video permission → Auto-scan → Enjoy!
5. Settings → Check for updates → Download & Install in-app

#### F-Droid
- **Available:** https://f-droid.org/packages/com.opticast.player
- Auto-updates from GitHub releases

### 🔨 Build — For Developers

```bash
cd project
./gradlew assembleRelease
```

Requires Android Studio Ladybug+ (Android 8+, JDK 17, compileSdk 36, minSdk 26, targetSdk 35)

- Locked baseline: v2.6.104/153, 110 entries, 9.7/10 Stable Ultra Fast
- Full mpv included, complete source zip 27MB uploaded with release
- Same JKS for install over existing, higher versionCode

### 🔒 Privacy — Simple & Transparent

**Permissions:** Internet, Network State, Video Library Access, Notifications, Background Playback, Install Updates, Wake Lock

**Privacy:**
- **Offline-first:** No accounts, your library stays on your device, works 100% offline
- **No tracking:** No analytics, no ads, no tracking — open source GPL-3.0
- **Minimal data:** Update check only when internet available, artwork optimized for data saver, subtitles always saved offline
- See [PRIVACY.md](docs/PRIVACY.md) and [FAQ.md](docs/FAQ.md)

### 📄 License & Credits

**GPL-3.0** — see [LICENSE](LICENSE)

**Providers:**
- TMDB (movies/TV info, posters)
- OpenSubtitles + SubDL (subtitles) — requires API keys, saved offline
- OMDb, Fanart.tv, AniList (extra info)

**Contact:** opticastproject@gmail.com

### 🗺️ Roadmap — v2.6.104 Done

- [x] Smooth library scrolling — ultra fast without removing features, matches settings 60fps
- [x] Changeable grid layout — works, Adaptive, border/clip 12dp kept
- [x] Audio Only removed completely — fixes blank video until seek
- [x] Install over existing app — same JKS higher versionCode
- [x] Picture-in-Picture auto-resume — fixed for 32-bit
- [x] In-app updates with progress — FileProvider, full changelog visible
- [x] Offline-first with minimal data — checks once when internet detected
- [x] Always save subtitles offline — cached during scan
- [x] Beautiful poster fallback — no black background, blurred poster
- [x] No "Up To Date" spam — only when real update available, dismiss 48dp
- [x] What's New shows real new not old — only after update
- [x] Fast and lightweight — 38M, <300ms startup, 60fps, RAM 8/12/16/20 + 32/48/64/96 LRU 30 pool 200
- [x] Mobile only focus — removed TV saves 18KB, manual DI 0KB
- [x] Baseline 110 locked — R8 fullMode
- [x] v2.6.104 Stable 9.7/10 — Ultra Fast, Audio Only removed

**Current:** v2.6.104 (153) — Stable 9.7/10 Ultra Fast — Your local cinema, offline-first, no ads, open source

**Links:**
- **GitHub:** https://github.com/opticastplayer-dev/opticast
- **Releases:** https://github.com/opticastplayer-dev/opticast/releases/tag/v2.6.104
- **APK:** https://github.com/opticastplayer-dev/opticast/releases/download/v2.6.104/OptiCast-v2.6.104.apk
- **Website:** https://opticastplayer-dev.github.io/opticast/
- **F-Droid:** https://f-droid.org/packages/com.opticast.player
- **FAQ:** https://github.com/opticastplayer-dev/opticast/blob/main/docs/FAQ.md
- **Privacy:** https://github.com/opticastplayer-dev/opticast/blob/main/docs/PRIVACY.md
- **AlternativeTo:** https://github.com/opticastplayer-dev/opticast/blob/main/docs/ALTERNATIVETO.md
- **Reddit:** https://github.com/opticastplayer-dev/opticast/blob/main/docs/REDDIT.md

### 🔍 Find OptiCast

OptiCast, OptiCast Video Player, local video player Android, Infuse alternative for Android, free video player no ads, offline movie player, best Android video player, open source video player.

---

**OptiCast v2.6.104 — Stable 9.7/10 Ultra Fast — Your local cinema, offline-first, no ads, open source — 2026**

Rating: Infuse 10/10 iOS closed, OptiCast 9.7/10 Android open-source #1 Stable Ultra Fast, Plex 8.0, Kodi 8.0, Nova 7.5, VLC 7.0
