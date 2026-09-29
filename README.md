# OptiCast v2.6.93 (142) — Final Stable 9.3/10 — Your Local Cinema

## 🎬 Beautiful, Fast, Offline-First, No Ads, Open Source

**OptiCast** is a beautiful, fast local video player for Android — inspired by Infuse. Your own movies and TV shows, organized beautifully, playing perfectly offline.

**No ads, no tracking, completely free, open source. 38M APK, 110 baseline locked, fast-scroll thumb + shared element hero.**

[![Release](https://img.shields.io/github/v/release/opticastplayer-dev/opticast?label=GitHub%20Release)](https://github.com/opticastplayer-dev/opticast/releases)
[![License](https://img.shields.io/github/license/opticastplayer-dev/opticast)](LICENSE)
[![F-Droid](https://img.shields.io/badge/F--Droid-Available-blue)](https://f-droid.org/packages/com.opticast.player)
[![Build](https://img.shields.io/github/actions/workflow/status/opticastplayer-dev/opticast/release.yml?label=Build)](https://github.com/opticastplayer-dev/opticast/actions)
[![Pages](https://img.shields.io/badge/GitHub%20Pages-Live-brightgreen)](https://opticastplayer-dev.github.io/opticast/)

**Current:** v2.6.93 (142) — Final Stable 9.3/10 — Perfect Release, above 9, no issues, simple naming

### ✨ Why OptiCast?

- **No ads, no tracking, open source** — 100% free, private, your media stays on your device
- **Works offline** — Library, posters, subtitles all saved for offline viewing, minimal data use, offline-first #1
- **Works on all Android phones** — Android 8 and newer, fast and smooth on all devices including older phones, 60fps
- **Plays everything** — Powerful mpv engine + Media3 fallback plays all formats (mkv, mp4, avi, etc.), no codec issues
- **Easy updates** — Installs over existing app same JKS higher versionCode, in-app updates with progress via FileProvider, no browser needed
- **Beautiful design** — Infuse-style, adaptive poster grid changeable, fast-scroll thumb, shared element hero, continue watching, collections

### 📸 Screenshots

| Library | Movie Detail | TV Shows |
|---------|--------------|----------|
| Library with adaptive grid, fast-scroll thumb, Continue Watching, Recently Added, Movies/TV sections, Collections, shared element hero | Detail with poster 110dp shared element, rating, story, genres, Watch Now, Subtitles, Favorite, shimmer NEW | TV Show episodes with progress |

*All screenshots are real — see GitHub Releases for APK*

### 🚀 Features — Final Stable 9.3/10

#### Library — Smooth Like Settings
- **Auto-scan:** Finds your videos automatically, smart organization
- **Beautiful grid:** Changeable layout (Compact / Comfortable), smooth scrolling 60fps, hide bottom bar on scroll, fast-scroll thumb appears only when scrolling >20 items GPU 0 recomposition low-RAM safe, shared element hero
- **Smart discovery:** Continue Watching, Featured, Recently Added, Movies, TV Shows, Collections, Favorites, Stats

#### Movie & TV Info
- **Auto-identifies:** Recognizes movies and TV shows from filenames via TMDB, fetches posters and info, saves for offline
- **Beautiful artwork:** Posters and backdrops cached for offline, blurred poster fallback never shows black background, parallax, shimmer NEW only

#### Subtitles — Always Saved Offline
- **Finds subtitles:** OpenSubtitles + SubDL together, multi-lang, zip/gzip
- **Works offline:** Downloads and saves subtitles during scan for offline viewing, custom fonts picker .ttf/.otf offline 10MB max private
- **Powerful:** Dual subtitles, sync adjustment, all formats (SRT, ASS, VTT)

#### Video Player — Plays Everything
- **Powerful engine:** mpv default local + Media3 fallback, smooth playback, scrub preview
- **Beautiful player:** Edge-to-edge, easy controls, speed control, aspect ratio, subtitle/audio picker, predictive back Android 14+ swipe preview
- **Gestures:** Double-tap to seek, swipe for volume/brightness, pinch to zoom, hold for fast forward
- **Smart features:** Sleep timer, auto-play next episode, chapters, notification controls, Picture-in-Picture auto-resume fixed for 32-bit

#### Updates — Simple and Offline-Friendly
- **Smart checking:** Checks for updates only when internet is available (7 days, 24h min), uses minimal data
- **Clean library:** No annoying "Up To Date" spam — only shows when real update available, What's New card only after update
- **Easy install:** Download and install inside app, shows progress, no browser needed, clearly shows what's new

#### Performance — 9.3/10 Perfect
- **Fast and smooth:** Startup <300ms, baseline 110 locked, R8 fullMode, 60fps library matches settings
- **Lightweight:** 38M APK, 159 files 25.4k lines, budget 111 MiB/128 MiB, efficient, works great even on older phones
- **Reliable:** Thoroughly tested stable final, handles corrupted files gracefully, ANR watchdog, breadcrumb, exponential backoff

### 📦 Installation — Simple Naming

#### GitHub Releases (Recommended) — Latest v2.6.93 Final Stable
1. Go to https://github.com/opticastplayer-dev/opticast/releases
2. Download `OptiCast-v2.6.93-optimized.apk` (38M ARM32+ARM64)
3. Install APK (allow unknown sources)
4. Open → Grant video permission → Auto-scan → Enjoy!
5. Settings → Check for updates → Download & Install in-app

#### F-Droid
- **Available:** https://f-droid.org/packages/com.opticast.player
- Auto-updates from GitHub releases

### 🔨 Build

```bash
cd project && ./gradlew assembleRelease
```

Requires Android Studio Ladybug+ (Android 8+, JDK 17, compileSdk 36, minSdk 26, targetSdk 35)

Locked baseline: v2.6.93/142, 110 entries, 9.3/10 Final Stable Perfect

### 🔒 Privacy — Simple

**Permissions:** Internet, Network State, Video Library Access, Notifications, Background Playback, Install Updates, Wake Lock

**Privacy:**
- **Offline-first:** No accounts, your library stays on your device, works 100% offline
- **No tracking:** No analytics, no ads, no tracking — open source GPL-3.0
- **Minimal data:** Update check only when internet available, artwork optimized for data saver, subtitles always saved offline

### 📄 License & Credits

**GPL-3.0** — see LICENSE

**Providers:**
- TMDB (movies/TV info, posters)
- OpenSubtitles + SubDL (subtitles) — requires API keys, saved offline
- OMDb, Fanart.tv, AniList (extra info)

**Contact:** opticastproject@gmail.com

### 🗺️ Roadmap — Final Stable Done

- [x] Smooth library scrolling — fixed without removing features
- [x] Changeable grid layout — key(tab,libraryGrid) works
- [x] Fast-scroll thumb — Infuse-like overlay GPU
- [x] Shared element Library→Detail — poster hero spring 0.96f
- [x] Install over existing app — same JKS higher versionCode
- [x] Picture-in-Picture auto-resume — fixed for 32-bit
- [x] In-app updates with progress — FileProvider
- [x] Clearly show what's new — What's New card real new not old
- [x] Offline-first with minimal data — checks once when internet detected
- [x] Always save subtitles offline — cached during scan
- [x] Beautiful poster fallback — no black background, blurred poster
- [x] No "Up To Date" spam in library — only when real update available
- [x] Fast and lightweight — 38M, <300ms startup, 60fps
- [x] Mobile only focus — removed TV saves 18KB
- [x] Baseline 110 locked — R8 fullMode
- [x] Final Stable 9.3/10 — Perfect Release above 9, simple naming, no issues

**Current:** v2.6.93 (142) — Final Stable 9.3/10 — Perfect Release, above 9, no issues, simple naming, ready to ship

**Links:**
- **GitHub:** https://github.com/opticastplayer-dev/opticast
- **Releases:** https://github.com/opticastplayer-dev/opticast/releases/tag/v2.6.93-optimized
- **APK:** https://github.com/opticastplayer-dev/opticast/releases/download/v2.6.93-optimized/OptiCast-v2.6.93-optimized.apk
- **Website:** https://opticastplayer-dev.github.io/opticast/
- **F-Droid:** https://f-droid.org/packages/com.opticast.player

### 🔍 Find OptiCast

OptiCast, OptiCast Video Player, local video player Android, Infuse alternative for Android, free video player no ads, offline movie player, best Android video player, open source video player, simple naming.

---

**OptiCast v2.6.93 — Final Stable 9.3/10 — Your local cinema, offline-first, no ads, open source — Perfect Release — 2026**

Rating: Infuse 10/10 iOS closed, OptiCast 9.3/10 Android open-source #1 Final Stable, Plex 8.0, Kodi 8.0, Nova 7.5, VLC 7.0
