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
- **Fast and smooth:** Startup <300ms, baseline 110 locked, R8 fullMod