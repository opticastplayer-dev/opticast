# OptiCast — Your videos, beautifully organized.

OptiCast is a local video player for Android. It finds your movies and TV shows, organizes them with posters and info, and plays them perfectly — even offline.

**No ads, no tracking, open source. Focused on local playback.**

[![Release](https://img.shields.io/github/v/release/opticastplayer-dev/opticast?label=Release)](https://github.com/opticastplayer-dev/opticast/releases)
[![License](https://img.shields.io/github/license/opticastplayer-dev/opticast)](LICENSE)
[![Stars](https://img.shields.io/github/stars/opticastplayer-dev/opticast?style=social)](https://github.com/opticastplayer-dev/opticast/stargazers)
[![Downloads](https://img.shields.io/github/downloads/opticastplayer-dev/opticast/total?color=63daff)](https://github.com/opticastplayer-dev/opticast/releases)
[![Build](https://img.shields.io/github/actions/workflow/status/opticastplayer-dev/opticast/release.yml?label=Build)](https://github.com/opticastplayer-dev/opticast/actions)
[![Pages](https://img.shields.io/badge/Website-Live-brightgreen)](https://opticastplayer-dev.github.io/opticast/)

**Current:** v2.6.119 (168)

### Why it's different

Most players try to do everything. OptiCast focuses on one thing: playing your own videos well.

- **Focused local playback** — Scans your device via MediaStore, shows Continue Watching, Recently Added, Collections. No streaming catalogs, no login walls.
- **Offline-first, by design** — Posters and subtitles are saved for offline during scan. Works fully offline after. Update check once when internet detected, minimal data.
- **Private & simple** — No ads, no tracking, GPL-3.0. Your media stays on device. No accounts. Small APK, manual DI, no bloat.

### Getting Started — 30 seconds

1. **Install** — Download APK from [Releases](https://github.com/opticastplayer-dev/opticast/releases/latest) and install. Grant video permission.
2. **Open** — Auto-scans your videos. If you don't see them, check `Settings → Folders` — maybe a folder is excluded.
3. **Watch** — Tap a poster. Posters and subtitles are fetched and cached for offline.

**Where are my videos?** OptiCast looks in Movies, DCIM, Download via MediaStore. If missing: `Settings → Excluded folders` → remove exclusion, then pull to refresh in Library. Files hidden (dot prefix) or in `Android/data` are hidden by system.

### Screenshots — real app

| Library | Movie detail | TV show |
|---------|--------------|---------|
| ![Library](docs/showcase/1.jpg) | Library: adaptive grid, Continue Watching, Recently Added. Fast scrolling. | ![Detail](docs/showcase/2.jpg) | Movie detail: poster, backdrop, genres, runtime, cast. Cached offline. | ![TV Show](docs/showcase/3.jpg) | TV shows: seasons & episodes, stills, auto-play next. |

| Player | Settings |
|--------|----------|
| ![Player](docs/showcase/4.jpg) | Player: mpv + Media3 fallback, gestures, speed, PiP auto-resumes, chapters, sleep timer. | ![Settings](docs/showcase/5.jpg) | Settings: grid size, data saver, subtitle fonts, in-app updates with progress. |

*Real screenshots from OptiCast v2.6.119*

### Features

**Library**
- Auto-scan via MediaStore, adaptive grid (Compact/Comfortable), fast scrolling, search
- Continue Watching, Featured, Recently Added, Movies, TV Shows, Collections, Favorites

**Movie Info**
- Recognizes movies/TV from filenames, posters cached offline, blurred fallback

**Subtitles**
- OpenSubtitles + SubDL, multi-language, always saved offline during scan, custom fonts .ttf/.otf, dual subs

**Player**
- mpv + Media3 fallback, edge-to-edge, gestures, speed, aspect, audio/subs picker, predictive back, sleep timer, auto-play next, chapters, PiP auto-resume

**Updates**
- Checks once when internet detected, minimal data, in-app download with progress via FileProvider, full changelog visible

### Installation

**GitHub Releases**
1. Go to https://github.com/opticastplayer-dev/opticast/releases
2. Download `OptiCast-v2.6.119.apk`
3. Install APK (allow unknown sources)
4. Open → Grant permission → Auto-scan

**F-Droid**
- https://f-droid.org/packages/com.opticast.player — MR 50679 open, can_be_merged

### What people say

> “Finally a player that just shows my movies nicely and works offline on my old phone. No ads, no clutter.” — Early tester, 32-bit device

> “I like that it doesn't try to sell me streaming. It's my library, organized like Infuse but for Android and open.” — GitHub feedback

Users give feedback, not us rating ourselves.

### Build

```bash
cd project
./gradlew assembleRelease
```

Requires Android Studio Ladybug+, JDK 17, compileSdk 36, minSdk 26, targetSdk 35

### Privacy

No accounts, library stays on device, works offline, no tracking. Permissions: Internet, Network State, Video Library, Notifications, Background Playback, Install Updates, Wake Lock

See [PRIVACY.md](docs/PRIVACY.md) and [FAQ.md](docs/FAQ.md)

### Links

- Website: https://opticastplayer-dev.github.io/opticast/
- GitHub: https://github.com/opticastplayer-dev/opticast
- Release: https://github.com/opticastplayer-dev/opticast/releases/latest
- F-Droid MR: https://gitlab.com/fdroid/fdroiddata/-/merge_requests/50679
- IzzyOnDroid: https://codeberg.org/IzzyOnDroid/repodata/issues/657

### License

**GPL-3.0** — see [LICENSE](LICENSE)

Contact: opticastproject@gmail.com
