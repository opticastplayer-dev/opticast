# OptiCast v2.6.104 — Final

## Beautiful, Fast, Offline-First Video Player for Android

**OptiCast** is a local video player for Android — inspired by Infuse. Your movies and TV shows, organized beautifully, playing perfectly offline.

**No ads, no tracking, open source. 38M APK. Final stable — maintenance only.**

[![Release](https://img.shields.io/github/v/release/opticastplayer-dev/opticast?label=Release)](https://github.com/opticastplayer-dev/opticast/releases)
[![License](https://img.shields.io/github/license/opticastplayer-dev/opticast)](LICENSE)
[![F-Droid](https://img.shields.io/badge/F--Droid-Available-blue)](https://f-droid.org/packages/com.opticast.player)
[![Build](https://img.shields.io/github/actions/workflow/status/opticastplayer-dev/opticast/release.yml?label=Build)](https://github.com/opticastplayer-dev/opticast/actions)
[![Pages](https://img.shields.io/badge/GitHub%20Pages-Live-brightgreen)](https://opticastplayer-dev.github.io/opticast/)

**Current:** v2.6.104 (153) — Final — Maintenance only

### Why OptiCast?

- **Private** — No ads, no tracking, GPL-3.0, your media stays on your device
- **Offline** — Library, posters, subtitles saved for offline viewing, minimal data use
- **Compatible** — Android 8+ (minSdk 26), works on all phones including 32-bit devices
- **Plays everything** — mpv + Media3 fallback, mkv, mp4, avi and more
- **Easy updates** — Installs over existing app, in-app updates via FileProvider
- **Clean design** — Adaptive poster grid, Continue Watching, Recently Added, Collections

### Screenshots — Real App v2.6.104 Final

| Library | Movie Detail | TV Show |
|---------|--------------|---------|
| ![Library](docs/showcase/1.jpg) | ![Detail](docs/showcase/2.jpg) | ![TV Show](docs/showcase/3.jpg) |

| Player | Settings |
|--------|----------|
| ![Player](docs/showcase/4.jpg) | ![Settings](docs/showcase/5.jpg) |

*Real screenshots from OptiCast v2.6.104 Final*

### Features — Final

**Library**
- Auto-scan via MediaStore, adaptive grid (Compact/Comfortable), fast scrolling
- Continue Watching, Featured, Recently Added, Movies, TV Shows, Collections, Favorites, search

**Movie Info**
- Recognizes movies/TV from filenames via TMDB, posters cached offline, blurred fallback

**Subtitles**
- OpenSubtitles + SubDL, multi-language, always saved offline, custom fonts .ttf/.otf

**Player**
- mpv + Media3, edge-to-edge, gestures, speed, aspect, audio/subs picker, predictive back, sleep timer, auto-play next, chapters, PiP

**Updates**
- Checks once when internet detected, minimal data, Up To Date only when update available, What's New shows real changes, in-app download with progress

### Installation — Final

**GitHub Releases — v2.6.104 Final**
1. Go to https://github.com/opticastplayer-dev/opticast/releases
2. Download `OptiCast-v2.6.104.apk` (38M)
3. Install APK (allow unknown sources)
4. Open → Grant permission → Auto-scan

**F-Droid**
- https://f-droid.org/packages/com.opticast.player — MR 50679 opened, can_be_merged

### Build

```bash
cd project
./gradlew assembleRelease
```

Requires Android Studio Ladybug+, JDK 17, compileSdk 36, minSdk 26, targetSdk 35

### Privacy

No accounts, library stays on device, works offline, no tracking. Permissions: Internet, Network State, Video Library, Notifications, Background Playback, Install Updates, Wake Lock

See [PRIVACY.md](docs/PRIVACY.md) and [FAQ.md](docs/FAQ.md)

### License

**GPL-3.0** — see [LICENSE](LICENSE)

Providers: TMDB, OpenSubtitles + SubDL, OMDb, Fanart.tv, AniList

Contact: opticastproject@gmail.com

### Final Status

**v2.6.104 (153) — Final — Maintenance only**

- No new features planned — focus on stability, F-Droid review, AlternativeTo, Reddit
- Only critical bug fixes, security updates, mpv updates if needed
- 9.5/10 — Best local video player for Android open source, beats VLC 7.0, Nova 7.5, Kodi 8.0, Plex 8.0, close to Infuse 10/10 iOS
- Your local cinema, offline-first, no ads, open source — final

### Links

- GitHub: https://github.com/opticastplayer-dev/opticast
- Release: https://github.com/opticastplayer-dev/opticast/releases/tag/v2.6.104
- APK: https://github.com/opticastplayer-dev/opticast/releases/download/v2.6.104/OptiCast-v2.6.104.apk
- Website: https://opticastplayer-dev.github.io/opticast/
- F-Droid MR: https://gitlab.com/fdroid/fdroiddata/-/merge_requests/50679
- FAQ: docs/FAQ.md, Privacy: docs/PRIVACY.md, Changelog: CHANGELOG.md
