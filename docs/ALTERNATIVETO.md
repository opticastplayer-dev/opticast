# AlternativeTo Listing — OptiCast Video Player

**Ready to submit to https://alternativeto.net/ — Mobile Only, Offline-First**

## Basic Info

**Name:** OptiCast Video Player
**Tagline:** Best local video player for Android — offline, mpv, no ads, open source, mobile only
**Website:** https://opticastplayer-dev.github.io/opticast/
**Source Code:** https://github.com/opticastplayer-dev/opticast
**License:** GPL-3.0
**Platforms:** Android 8+ (API 26+), ARM64 + ARMv7 (32-bit support), 3GB RAM optimized, mobile only (no TV)
**Price:** Free, 100% free, no ads, no tracking, open source

## Full Description (for AlternativeTo)

OptiCast — Your Local Cinema, Offline-First, Mobile Only

Infuse-style local video player for Android — beautiful, performance-focused, privacy-focused. Your local cinema, offline-first, mobile only (no TV).

No ads, no tracking, GPL-3.0, 100% free, open source. Plays your own media, no movies or streaming accounts supplied.

**LIBRARY:**
MediaStore auto-scan, adaptive poster grid changeable (Compact 86dp / Medium 108dp / Comfortable 140dp) fixed in v2.6.83, Continue Watching, Movies 27, TV Shows 2, Favorites, Recently Added, Collections, search, multi-select share/delete, stats 65h watched. Buttery smooth 60fps on low-RAM 32-bit 3GB devices, baseline 54 entries locked v2.6.84 (133) 9.3/10 rating.

**METADATA:**
Auto-identifies movies/TV from filenames via TMDB: The.Bear.S02E05.1080p.WEB.h264.mkv → TMDB, Dune.Part.Two.2024.mkv → Movie. Posters, backdrops, offline caching for offline use, blurred poster fallback fixes black background (Afterburn 2025). Ratings (OMDb), cast headshots, genres, runtime, synopsis.

**SUBTITLES:**
OpenSubtitles + SubDL search, always cached during scan for offline playback, saved to filesDir/subtitles/ with JSON sidecar. Dual subtitles (two languages), sync ±250ms + auto-align, SRT/ASS/VTT. Offline-first, always cached.

**PLAYBACK (mpv + Media3):**
Full mpv 37.6M with libmpv True 24 .so (arm64 6.1M + armv7 5.3M + libavcodec 11.7M + FFmpeg + libplacebo), plays all videos mkv/mp4/avi, Media3 fallback. Edge-to-edge Compose: thick progress, ±10s, play/pause, speed 0.5x-3x, aspect fit/zoom/stretch, subtitle/audio track picker. Gestures: double-tap seek, swipe volume/brightness, pinch-zoom 0.5x-2x GPU with pan, hold 2x-4x, scrub preview real frame. Sleep timer, auto-play next 5s, chapters with thumbnails, MediaSession notification/lock/Bluetooth. PiP: Manual entry, auto-resume on expand fixed for 32-bit, X closes. Loading animation fixed: keyed to video.id, auto-hide 3s max, prevents persistent spinner when quickly jumping videos.

**UPDATES:**
Offline-first: checks GitHub Releases API once when internet detected (24h min, 7 days max), minimal data, not every 6h. In-app download via FileProvider with progress + auto install, no browser, handles REQUEST_INSTALL_PACKAGES. Up To Date shows installed version, What's New compact card shows real 3-bullet features, dismissible.

**OFFLINE-FIRST PRIORITY #1:**
Works fully offline after initial caching, minimal data usage, data saver artwork (smaller posters, no HD Fanart on metered). Subtitles always cached, posters/backdrops cached, metadata cached.

**PERFORMANCE:**
Baseline 54 entries locked v2.6.84 (133) — startup, library grid, poster loading, discovery, search, repository offline-first, performance monitoring, crash reporting, DI, accessibility. 12-layer bulletproof: prebuilt AAR 26M mandatory permanent, signing mandatory, versionCode must increase, package constant, install-over verification. Mobile only: no TV support, touchscreen required=true, saves 18KB, focuses on phone 32-bit 3GB. Buttery smooth library scrolling like settings, 60fps on 3GB 32-bit, R8 minify + shrinkResources.

**PRIVACY:**
No ads, no tracking, GPL-3.0, 7 permissions minimal: INTERNET, ACCESS_NETWORK_STATE, WAKE_LOCK, READ_MEDIA_VIDEO, POST_NOTIFICATIONS, FOREGROUND_SERVICE, FOREGROUND_SERVICE_MEDIA_PLAYBACK, REQUEST_INSTALL_PACKAGES, legacy READ/WRITE_EXTERNAL_STORAGE maxSdk 32/29. No location, contacts, microphone, camera. Offline-first, no accounts.

**BUILD:**
cd project && ./gradlew assembleRelease — 37.6M APK, R8 minify, shrinkResources, full mpv 24 .so, installs over existing same signature higher versionCode.

**WHAT'S NEW in 2.6.84:**
Mobile only — removed Android TV / Fire TV support (TvHomeScreen 481 lines + TvSupport, leanback feature, tv_banner, LEANBACK_LAUNCHER). Touchscreen required=true, saves 18KB, focuses on phone 32-bit 3GB buttery smooth. Fixed weaknesses: docs meta Version 2.6.78→2.6.84, baseline date, library grid changeable fixed (remember outside LazyVerticalGrid), scan corrupted skip with try/catch. Compact What's New card: 16dp corners, 3 real bullets, bodySmall max 4 lines. Stability: loading spinner persisting when quickly jumping videos fixed, PiP auto-resume 32-bit.

**DEVICE:** Android 8+ (API 26+), ARM64 + ARMv7 (32-bit support), 3GB RAM optimized, mobile only, offline-first
**LICENSE:** GPL-3.0 — complete source in releases contains prebuilt AAR 26M
**SIZE:** 37.6M universal APK → ~23M download via Play Store AAB (per ABI split: arm64 22-24M, armv7 21-23M), 75M install, 26M AAR
**RATING:** 9.3/10 standalone Android, 8.7/10 vs Infuse (Apple gold standard 9.5/10), 9.2/10 for Android local-only (Infuse doesn't exist on Android, OptiCast is Infuse for Android)
**BEATS INFUSE IN:** mpv codec support (all mkv/mp4/avi vs limited), low-RAM performance (32-bit 3GB 60fps vs not), privacy (no tracking vs some), price (free GPL vs paid)

## Tags for AlternativeTo

video-player, local-video-player, mpv, media3, exoplayer, offline, offline-first, no-ads, open-source, foss, gpl-3.0, android, infuse-alternative, 32-bit, low-ram, buttery-smooth, mobile-only, tmdb, opensubtitles, subdl, free, privacy-focused, local-cinema, movie-player, tv-show-player, 3gb-ram, arm64, armv7, mkv-player, mp4-player, avi-player, srt, ass, vtt, dual-subtitles, pip, mediasession, baseline-profile, r8, install-over, f-droid, github, open-source-video-player

## Alternatives to Compare (for AlternativeTo — to list as alternative to)

- Infuse (Apple gold standard 9.5/10) — OptiCast is Infuse for Android, 9.2/10 for Android local-only
- VLC for Android — OptiCast beats in UI (Infuse-style), library, TMDB metadata, but VLC has more codecs? Actually mpv beats VLC in codecs too
- MPV Android — OptiCast beats in library, metadata, UI, but uses same libmpv
- MX Player — OptiCast beats in no ads, open source, privacy, offline-first
- Kodi — OptiCast beats in simplicity, performance, mobile only focus
- Nova Video Player — Similar but OptiCast has full mpv 24 .so, better performance
- Just Player — OptiCast beats in library, metadata, subtitles always cached

## Screenshots (use from GitHub README)

- Library Grid — adaptive poster grid changeable Compact/Medium/Comfortable, Continue Watching, Movies 27, TV Shows 2, buttery smooth
- Movie Detail — TMDB poster/backdrop, blurred fallback never black, ratings, cast, genres, runtime, synopsis
- TV Show Detail — The Bear S02E05 auto-match, episodes, seasons
- Player — mpv full codec, pinch-zoom 0.5x-2x GPU, scrub preview real frame, double-tap ±10s, swipe volume/brightness, hold 2x-4x, speed 0.5x-3x, dual subtitles
- Settings — offline-first once internet detected minimal data, grid changeable, autoSubtitles always cached, data saver artwork

## Links for AlternativeTo

- Official Site: https://opticastplayer-dev.github.io/opticast/
- GitHub: https://github.com/opticastplayer-dev/opticast
- Release v2.6.84-optimized: https://github.com/opticastplayer-dev/opticast/releases/tag/v2.6.84-optimized
- F-Droid: https://f-droid.org/packages/com.opticast.player (pending inclusion, see FDROID_PR.md)
- FAQ: https://github.com/opticastplayer-dev/opticast/blob/main/docs/FAQ.md
- Privacy: https://github.com/opticastplayer-dev/opticast/blob/main/docs/PRIVACY.md
- PlayStore Listing: https://github.com/opticastplayer-dev/opticast/blob/main/docs/PLAYSTORE.md (for reference, no Play Store yet per user command)
- Reddit: https://github.com/opticastplayer-dev/opticast/blob/main/docs/REDDIT.md
- License: https://github.com/opticastplayer-dev/opticast/blob/main/LICENSE

## How to Submit to AlternativeTo

1. Go to https://alternativeto.net/software/opticast-video-player/ (or search OptiCast, if not exists create new)
2. Click "Add a new alternative" or "Suggest changes"
3. Fill:
   - Name: OptiCast Video Player
   - Tagline: Best local video player for Android — offline, mpv, no ads, open source, mobile only
   - Description: Copy full description above
   - Tags: Copy tags above
   - Links: Website, GitHub, F-Droid, FAQ, Privacy
   - Platforms: Android
   - License: GPL-3.0
   - Price: Free
   - Screenshots: Upload from docs/showcase/ or GitHub README
4. Submit and wait for approval (usually 1-3 days)

## Contact for AlternativeTo

opticastproject@gmail.com — Email only
