# Reddit Draft — r/fossdroid + r/Android + r/Plex + r/htpc

**Title:** OptiCast 2.6.92 — Stable 9.2/10 — Infuse for Android, offline-first, 38M APK, fast-scroll thumb + shared element hero, open source

**Body:**

Hey all! I built OptiCast — your local cinema, offline-first, mobile only.

Think Infuse but for Android, open source, no ads, no tracking, GPL-3.0.

**What's New in 2.6.92 (141) — Stable 9.2/10:**
- Fast-scroll thumb like Infuse — appears only when scrolling >20 items, derivedStateOf + graphicsLayer GPU, 0 recomposition, low-RAM safe
- Shared element transition Library→Detail — poster hero spring 0.96f, SharedTransitionLayout
- Library smoothness fixed without removing features — grid changeable now works, Adaptive grid, 60fps matches Settings
- Baseline 99→110 locked — startup <300ms, 110 entries, R8 fullMode
- Predictive back PlayerScreen Android 14+ swipe preview, custom fonts picker .ttf/.otf offline
- Mobile only — removed TV bloat saves 18KB, touchscreen required
- Offline-first #1 — checks updates once when internet detected (7 days, 24h min), minimal data, subtitles always cached during scan
- Fixes: X dismiss 48dp, loading animation persisting, What's New real new, versionName matches asset

**Features:**
- Auto-discovers videos, auto-identifies movies/TV from filenames via TMDB, posters cached offline, beautiful blurred fallback never black
- mpv default local + Media3 fallback — plays everything (mkv, mp4, avi, etc.), gestures, scrub preview, PiP auto-resume fixed for 32-bit
- Subtitles: OpenSubtitles+SubDL, multi-lang, auto-download best during scan cached offline, sync ±250ms
- Extras: OMDb, Fanart.tv, frame artwork, thumbnail cache, confetti, heart burst
- Updates: in-app download FileProvider with progress, no browser, What's New card, Up To Date handling correct
- 38M APK ARM32+ARM64, Android 8+, 159 files 25.4k lines, budget 111 MiB/128 MiB

**Links:**
- GitHub: https://github.com/opticastplayer-dev/opticast
- Release: https://github.com/opticastplayer-dev/opticast/releases/tag/v2.6.92-optimized
- APK: https://github.com/opticastplayer-dev/opticast/releases/download/v2.6.92-optimized/OptiCast-v2.6.92-optimized.apk (38M)
- Source ZIP: complete source with full mpv
- Website: https://opticastplayer-dev.github.io/opticast/
- F-Droid: pending inclusion, metadata ready

**Rating vs others:** Infuse 10/10 iOS closed, OptiCast 9.2/10 Android open-source #1, Plex 8.0, Kodi 8.0, Nova 7.5, VLC 7.0

No ads, no tracking, private, your media stays on device. Installs over existing same JKS higher versionCode.

Would love feedback! Especially from low-RAM 32-bit 3GB device users — goal is buttery smooth on those.

Contact: opticastproject@gmail.com

#FOSS #Android #VideoPlayer #OfflineFirst #OpenSource #Infuse #mpv #F-Droid
