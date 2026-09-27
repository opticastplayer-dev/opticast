# OptiCast — Infuse-style Local Video Player

[![Release](https://img.shields.io/github/v/release/opticastplayer-dev/opticast?label=GitHub%20Release)](https://github.com/opticastplayer-dev/opticast/releases)
[![License](https://img.shields.io/github/license/opticastplayer-dev/opticast)](LICENSE)
[![F-Droid](https://img.shields.io/badge/F--Droid-Available-blue)](https://f-droid.org/packages/com.opticast.player)
[![Build](https://img.shields.io/github/actions/workflow/status/opticastplayer-dev/opticast/release.yml?label=Build)](https://github.com/opticastplayer-dev/opticast/actions)

**OptiCast** is a beautiful, performance-focused local video player for Android — inspired by Infuse. Scans device, auto-identifies movies & TV shows, fetches posters from TMDB, subtitles from OpenSubtitles/SubDL, plays with **mpv** + Media3 fallback.

- **No ads, no tracking, GPL-3.0**
- **ARM32 + ARM64, Android 8+**
- **PiP fix, in-app updates, What's New**

## ✨ Showcase

| Library Grid | Detail Page | Player |
|--------------|-------------|--------|
| ![Library](docs/showcase/01-library-grid.png) | ![Detail](docs/showcase/02-detail-page.png) | ![Player](docs/showcase/03-player.png) |

| PiP Auto-Resume | Settings & Updates |
|-----------------|-------------------|
| ![PiP](docs/showcase/04-pip.png) | ![Settings](docs/showcase/05-settings.png) |

*Accurate mockups based on actual code (Color.kt #000000 pitch-black, amber #EEC177 / violet #C08AFF). Real device captures TODO — see `docs/showcase/README.md`.*

## Features

**Library:** MediaStore auto-scan, adaptive poster grid, Continue Watching, Movies/TV grouping, Favorites, Recently Added, sort Recent/Title/Rating/Year + genre filter, search, multi-select share/delete, excluded folders.

**Auto-matching:** `The.Bear.S02E05.1080p.WEB.h264.mkv` → TMDB, `Dune.Part.Two.2024.mkv` → Movie, `[Group] Title - 01 [1080p].mkv` → AniList fallback.

**Detail/Show:** Backdrop hero, poster, rating, genres, runtime, synopsis, cast headshots, episode list, Resume/Mark Watched/Refresh artwork/Share.

**Player (mpv + Media3):** mpv default local, Media3 fallback, edge-to-edge Compose: thick progress, ±10s, speed 0.5-3x, aspect fit/zoom/stretch, subtitle/audio picker, sync ±250ms, audio boost, EQ, gestures double-tap seek swipe volume/brightness hold 2x-4x, sleep timer, auto-play next 5s, chapters, MediaSession.

**PiP:** Manual entry, auto-resume on expand fixed for 32-bit, X closes. Fix: wasPlayingBeforePip + onReturnFromPip + 1000ms delay + RESUMED check.

**Subtitles:** OpenSubtitles+SubDL together, multi-lang, zip/gzip, auto-download best, in-player search & hot-swap.

**Extras:** OMDb IMDb/RT/Metacritic, Fanart.tv clearlogos, frame artwork, thumbnail cache scrub previews, offline poster cache w185/w342.

**Performance 2.6.60:** No runBlocking main, async settings, 6/16 MiB image cache, 64/192 MiB disk, no HW bitmaps lowRam RGB_565, ConcurrentHashMap, baseline profiles + R8 fullMode + dex-startup-opt, PlaybackWorkBudget gates downloads during playback.

**Updates:** GitHub API dual-repo fallback opticast-project + opticastplayer-dev, 24h interval, manual check, Download & Install in-app via FileProvider, startup check, What's New dialog.

## Installation

**GitHub Releases:** https://github.com/opticastplayer-dev/opticast/releases → download APK 31.4MB → Install → Settings → Check for updates → Download & Install

**F-Droid:** Pending inclusion — metadata in `fastlane/` + `project/.fdroid/` → https://f-droid.org/packages/com.opticast.player

**Direct APK:** SHA256 `74084d45f74c166ea09302274d27aa0da8c0d2ca8feb494fa4a2b3c23dac689d` on Release.

## Building

Requires Android Studio Ladybug+ (AGP 8.7, Kotlin 2.0, compileSdk 36, minSdk 26)

```bash
bash tools/setup.sh
python3 tools/restore-native-runtime.py
bash tools/build-apk.sh
python3 tools/package-release.py
```

## PiP Fix + Update Checker Fallback

**PiP 2.6.59:** 32-bit expanding PiP paused video. Fix: track wasPlayingBeforePip, onReturnFromPip → play(), 1000ms delay + onResume 150ms, RESUMED check. Files: PiPController.kt, PlayerActivity.kt, PlayerScreen.kt

**Update Checker 2.6.60:** Dual-repo fallback — tries opticast-project/opticast primary, falls back to opticastplayer-dev/opticast live if org not yet created. Loop GITHUB_API_URLS with try/catch. Files: UpdateChecker.kt

## Privacy & Permissions

INTERNET, ACCESS_NETWORK_STATE, READ_MEDIA_VIDEO, READ_EXTERNAL_STORAGE, POST_NOTIFICATIONS, FOREGROUND_SERVICE, FOREGROUND_SERVICE_MEDIA_PLAYBACK, REQUEST_INSTALL_PACKAGES, WAKE_LOCK. No analytics/ads/tracking.

## License & Credits

GPL-3.0-or-later — see LICENSE + app/src/main/assets/legal/. TMDB uses but not endorsed. Providers: TMDB, OpenSubtitles, SubDL, OMDb, Fanart.tv, AniList. mpv GPL-compatible controlled build pinned source.

Contact: opticastproject@gmail.com (WhatsApp removed 2.6.59)

## Roadmap

- [x] PiP auto-resume fix 32-bit
- [x] Remove WhatsApp Email only
- [x] In-app updates + startup check + dual-repo fallback
- [x] What's New dialog
- [x] GitHub Actions release workflow
- [x] F-Droid metadata + fastlane
- [x] Accurate showcase mockups (real device captures TODO)
- [ ] F-Droid inclusion MR
- [ ] Build 2.6.60 APK
- [ ] Create org opticast-project and transfer

**Locked Baseline:** 2.6.60 / 110 — 746 tests, budget 18 MiB / 128 MiB cleaned
**Live:** https://github.com/opticastplayer-dev/opticast — Releases v2.6.59 (APK+source) + v2.6.60 (fallback fix)
