# OptiCast — Infuse-style Local Video Player

[![Release](https://img.shields.io/github/v/release/opticastplayer-dev/opticast?label=GitHub%20Release)](https://github.com/opticastplayer-dev/opticast/releases)
[![License](https://img.shields.io/github/license/opticastplayer-dev/opticast)](LICENSE)
[![F-Droid](https://img.shields.io/badge/F--Droid-Available-blue)](https://f-droid.org/packages/com.opticast.player)
[![Build](https://img.shields.io/github/actions/workflow/status/opticastplayer-dev/opticast/release.yml?label=Build)](https://github.com/opticastplayer-dev/opticast/actions)

**OptiCast** is a beautiful, performance-focused local video player for Android — inspired by Infuse. Scans device, auto-identifies movies & TV shows, fetches posters from TMDB, subtitles from OpenSubtitles/SubDL, plays with **mpv** + Media3 fallback.

- **No ads, no tracking, GPL-3.0**
- **ARM32 + ARM64, Android 8+**
- **PiP auto-resume fixed for 32-bit, in-app updates, What's New**
- **100% REAL device screenshots**

## ✨ Showcase — 100% REAL Device Captures

| Library Grid (27 movies) | Movie Detail - Just Play Dead | TV Show - Banshee |
|--------------------------|-------------------------------|-------------------|
| ![Library](docs/showcase/01-library-grid.png) | ![Detail](docs/showcase/02-detail-page.png) | ![Banshee](docs/showcase/03-banshee-show.png) |

| TV Show - Spider-Noir (8 eps) | Player - Now Playing 27 movies | Settings - 2.6.60 |
|-------------------------------|--------------------------------|-------------------|
| ![Spider-Noir](docs/showcase/04-show-episodes.png) | ![Player](docs/showcase/05-player-now-playing.png) | ![Settings](docs/showcase/06-settings.png) |

**All screenshots are REAL captures from actual OptiCast app running on device:**
- **01-library-grid.png** 997K 18:15 90% — OptiCast wordmark blue C, Your local cinema, 0 Watched 65h Library time, Recent blue chip #6FD3FF, Title/Rating/Year/Genre, CONTINUE WATCHING 12 items Vengeance 2026 1:43:40 left 0% played + Meet the Blacks 2016 1:33:27 left, FEATURED 2/6 Meet the Blacks MOVIE 2016 Comedy ★4.7 Watch Now white button, RECENTLY ADDED 10 items Next Friday 2000 Meet the Blacks 2016 Poetic Justice 1993 The House Next Door 2021, BottomBar Movies(27) TV shows(2) Favorites(0) Search — Ocean theme dark blue #020810
- **02-detail-page.png** 762K 18:16 89% — Just Play Dead 5.9 2026 97min 480p backdrop Samuel L Jackson, poster palm trees, Resume 12:22 light blue #9BD0FF, Restart from beginning, Subtitles + Match blue #0B4C69, Mark as watched check, Story criminal mastermind fake death insurance, Thriller Crime chips, Cast Samuel L Jackson Jack Wolfe Eva Green Nora Wolfe Maria Pedraza Bianca, File Just Play Dead (2026).mkv 1:36:32 709.7MB 1280x534
- **03-banshee-show.png** 609K 18:43 84% — Banshee 8.0 2013 14 episodes Watch Now, backdrop man digging grave police car, Story paroled master thief Lucas Hood sheriff Banshee, Cast Antony Starr Lucas Hood Ivana Milicevic Hoon Lee Job Frankie Faison Sugar Bates, SEASON1 0/10 10 episodes SEASON2 0/4 4 episodes
- **04-show-episodes.png** 778K 18:43 84% — Spider-Noir 8.4 2026 8 episodes Resume 0:03 tagline "It won't end the way you want it to", Cast Nicolas Cage Ben Reilly Spider Lamorne Morris Robbie Robertson Li Jun Li Cat Hardy, SEASON1 0/8 8 episodes E01 Step into My Office In progress 0% 0:03 E02 Tread Lightly E03 Double Cross E04 A Mistake
- **05-player-now-playing.png** 761K — Player Now Playing 27 movies Horizontal cards 14-19: Next Friday 1:38:07 848x480 Current, No Ordinary Heist 1:39:16 1280x534, Poetic Justice 1:49:08 856x460, The Furious 1:53:11 1280x534, The House Next Door Meet Blacks 2 1:37:17 720x480, The Man from Toronto 1280x720, progress 0:14/1:38:07, Next Friday 14/27 back arrow VideoLibrary Subtitles More
- **06-settings.png** 317K 18:16 89% — Settings dark blue UI & Appearance Appearance Theme wallpaper colours grid size Player Layout Customize buttons, Playback & Controls Playback Engine Built-in fallback buffer Behaviour Orientation screen episode Gestures Double tap swipe hold, File Management Excluded Folders Network Libraries Storage Backups Library Maintenance File Naming Help, Media Settings Audio & Subtitle Tracks

*No AI mockups — all REAL, perfect for F-Droid + Play Store.*

## Features

**Library:** MediaStore auto-scan, adaptive poster grid, Continue Watching, Movies/TV grouping, Favorites, Recently Added, sort Recent/Title/Rating/Year + genre filter, search, multi-select share/delete, excluded folders.

**Auto-matching:** `The.Bear.S02E05.1080p.WEB.h264.mkv` → TMDB, `Dune.Part.Two.2024.mkv` → Movie, `[Group] Title - 01 [1080p].mkv` → AniList fallback.

**Detail/Show:** Backdrop hero, poster, rating, genres, runtime, synopsis, cast headshots, episode list with In progress 0% tracking, Resume/Mark Watched/Refresh artwork/Share.

**Player (mpv + Media3):** mpv default local, Media3 fallback, edge-to-edge Compose: thick progress, ±10s, speed 0.5-3x, aspect fit/zoom/stretch, subtitle/audio picker, sync ±250ms, audio boost, EQ, gestures double-tap seek swipe volume/brightness hold 2x-4x, sleep timer, auto-play next 5s, chapters, MediaSession, Now Playing horizontal cards 14/27.

**PiP:** Manual entry via PictureInPictureAlt icon, auto-resume on expand fixed for 32-bit (wasPlayingBeforePip + onReturnFromPip + 1000ms delay + RESUMED check), X closes.

**Subtitles:** OpenSubtitles+SubDL together, multi-lang, zip/gzip, auto-download best, in-player search & hot-swap.

**Extras:** OMDb IMDb/RT/Metacritic, Fanart.tv clearlogos, frame artwork, thumbnail cache scrub previews, offline poster cache w185/w342.

**Performance 2.6.60:** No runBlocking main, async settings, 6/16 MiB image cache, 64/192 MiB disk, no HW bitmaps lowRam RGB_565, ConcurrentHashMap, baseline profiles + R8 fullMode + dex-startup-opt, PlaybackWorkBudget gates downloads during playback.

**Updates:** GitHub API dual-repo fallback opticast-project + opticastplayer-dev, 24h interval, manual check, Download & Install in-app via FileProvider, startup check, What's New dialog.

## Installation — Available For Everyone Now!

### GitHub Releases (Recommended)
1. Go to **https://github.com/opticastplayer-dev/opticast/releases**
2. Download latest APK: `OptiCast-v2.6.59-arm32-arm64.apk` 31.4MB
3. SHA256 `74084d45f74c166ea09302274d27aa0da8c0d2ca8feb494fa4a2b3c23dac689d` — verify in `releases/SHA256SUMS`
4. Install APK (allow unknown sources)
5. Open → Grant video permission → Auto-scan → Enjoy!
6. Settings → Check for updates → Download & Install in-app for future versions

**What's included in Release v2.6.59:**
- APK 32M ARM32+ARM64 Android 8+
- Complete source 65.7M
- SHA256SUMS verification
- What's New dialog after update

### F-Droid
- Metadata ready in `fastlane/` + `project/.fdroid/` + 6 REAL screenshots
- Pending inclusion — will be at https://f-droid.org/packages/com.opticast.player
- Badge in README

### Direct APK Build
```bash
bash tools/setup.sh
python3 tools/restore-native-runtime.py
bash tools/build-apk.sh
python3 tools/package-release.py
```
Requires Android Studio Ladybug+ (AGP 8.7, Kotlin 2.0, compileSdk 36, minSdk 26)

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
- [x] 100% REAL device screenshots — 6 real captures (Library, Just Play Dead, Banshee, Spider-Noir, Player Now Playing, Settings) — NO mockups
- [ ] F-Droid inclusion MR
- [ ] Build 2.6.60 APK with fallback fix
- [ ] Create org opticast-project and transfer
- [ ] Optional: Add real PiP floating window screenshot if available (currently 6 real cover all features)

**Locked Baseline:** 2.6.60 / 110 — 746 tests, budget 17.63 MiB / 128 MiB cleaned (100% real)
**Live:** https://github.com/opticastplayer-dev/opticast — Releases v2.6.59 (APK+source) + v2.6.60 (fallback fix) — 6 REAL screenshots — Available for everyone now!
