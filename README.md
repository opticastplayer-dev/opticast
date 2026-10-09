# OptiCast — Your videos, beautifully organized.

Get a great local video player experience with OptiCast! It finds and organizes your Movies and TV shows with posters and info, and plays them flawlessly offline

**No ads, no tracking, open source. Focused on local playback.**

[![Release](https://img.shields.io/github/v/release/opticastplayer-dev/opticast?label=Release)](https://github.com/opticastplayer-dev/opticast/releases)
[![License](https://img.shields.io/github/license/opticastplayer-dev/opticast)](LICENSE)
[![Stars](https://img.shields.io/github/stars/opticastplayer-dev/opticast?style=social)](https://github.com/opticastplayer-dev/opticast/stargazers)
[![Downloads](https://img.shields.io/github/downloads/opticastplayer-dev/opticast/total?color=63daff)](https://github.com/opticastplayer-dev/opticast/releases)
[![Build](https://img.shields.io/github/actions/workflow/status/opticastplayer-dev/opticast/release.yml?label=Build)](https://github.com/opticastplayer-dev/opticast/actions)
[![Tests](https://img.shields.io/github/actions/workflow/status/opticastplayer-dev/opticast/test.yml?label=Tests)](https://github.com/opticastplayer-dev/opticast/actions/workflows/test.yml)
[![Pages](https://img.shields.io/badge/Website-Live-brightgreen)](https://opticastplayer-dev.github.io/opticast/)

**Current:** v2.6.120 (169)

### Official Distribution

**Official sources:**

| Source | URL |
|--------|-----|
| GitHub Releases | https://github.com/opticastplayer-dev/opticast/releases/latest |
| Website | https://opticastplayer-dev.github.io/opticast/ |
| F-Droid | https://f-droid.org/packages/com.opticast.player (MR !50919 — Pipeline Passed ✅) |
| IzzyOnDroid | https://apt.izzysoft.de/fdroid/index/apk/com.opticast.player (Issue #659) |

For security and to ensure you receive verified builds, please use only the official sources listed above. See [OFFICIAL_DISTRIBUTION.md](OFFICIAL_DISTRIBUTION.md) for full policy.

### Why it's different

Most players try to do everything. OptiCast focuses on one thing: playing your own videos well.

- **Focused local playback** — Automatically organizes your movies and TV shows with clear artwork. Continue where you left off and browse by category.
- **Offline-first, by design** — Works fully offline after initial setup. Posters and subtitles are saved to your device.
- **Private and simple** — No advertisements, no tracking, open source. Your videos stay on your device with no accounts required.

### Getting Started — 30 seconds

1. **Install** — Download APK from [Releases](https://github.com/opticastplayer-dev/opticast/releases/latest) and install. Allow access to videos.
2. **Open** — Your videos are organized automatically.
3. **Watch** — Tap any poster to start watching.

**Where are my videos?** OptiCast looks in your Movies, DCIM, and Download folders. If you don't see a video, check Settings → Excluded folders and ensure the folder is not excluded, then pull to refresh in your Library.

### Screenshots — real app

| Library | Movie detail | TV shows |
|---------|--------------|----------|
| Your collection at a glance with Continue Watching and Recently Added | Clear posters, cast, and details saved for offline viewing | Browse seasons and episodes with progress remembered |

| Player | Settings |
|--------|----------|
| Simple controls, adjustable speed, picture-in-picture, and sleep timer | Customize your experience and check for updates |

*Real screenshots available on the official website: https://opticastplayer-dev.github.io/opticast/*

### Features

**Library**
- Organized automatically with clear posters
- Continue Watching, Movies, TV Shows, Favorites, Recently Added, Collections
- Fast search

**Movie Info**
- Recognizes titles and shows posters, cast, and details
- Saved for offline viewing

**Subtitles**
- Multiple languages, saved offline
- Customizable fonts and dual subtitle support

**Player**
- Plays common video formats smoothly
- Intuitive controls, adjustable speed, picture-in-picture, sleep timer

**Updates**
- Check for updates inside the app
- View what's new in each version

### Installation

**GitHub Releases**
1. Go to https://github.com/opticastplayer-dev/opticast/releases
2. Download the latest APK
3. Install and allow access to videos
4. Open the app — your collection is organized automatically

**F-Droid and IzzyOnDroid**
- Available via official repositories when reviews are complete (F-Droid MR !50919, IzzyOnDroid #659)

### What people say

> “Finally a player that just shows my movies nicely and works offline on my old phone. No ads, no clutter.” — Early tester

> “I like that it doesn't try to sell me streaming. It's my library, organized beautifully.” — GitHub feedback

Users give feedback, not us rating ourselves.

### Build

```bash
cd project
./gradlew assembleRelease
```

Requires Android Studio, JDK 17

### Privacy

No accounts, your library stays on your device, works offline, no tracking. See [PRIVACY.md](docs/PRIVACY.md) and [FAQ.md](docs/FAQ.md)

### Links

- Website: https://opticast.app/ (https://opticastplayer-dev.github.io/opticast/)
- GitHub: https://github.com/opticastplayer-dev/opticast
- Release: https://github.com/opticastplayer-dev/opticast/releases/latest
- F-Droid MR: https://gitlab.com/fdroid/fdroiddata/-/merge_requests/50919 (new, template compliant, was !50679 closed)
- IzzyOnDroid: https://codeberg.org/IzzyOnDroid/repodata/issues/659 (new, proper template, was #657 closed)
- Official Distribution: [OFFICIAL_DISTRIBUTION.md](OFFICIAL_DISTRIBUTION.md)

### License

**GPL-3.0** — see [LICENSE](LICENSE)

Contact: opticastproject@gmail.com
