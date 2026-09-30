# F-Droid Merge Request — COPY-PASTE READY — OptiCast v2.6.104

**Use this exact text when creating MR at https://gitlab.com/fdroid/fdroiddata**

---

**Fork:** https://gitlab.com/fdroid/fdroiddata → Fork → Your username

**File to add:** `metadata/com.opticast.player.yml` — copy content from `fdroid-com.opticast.player.yml` in this repo root (lint passes)

**Branch:** `com.opticast.player` or `add-opticast`

---

**MR Title (copy-paste):**
```
OptiCast Video Player — Local video player, offline-first
```

**MR Description (copy-paste):**

```
OptiCast Video Player — Local video player, offline-first, no ads, open source

**Package:** `com.opticast.player`
**Version:** 2.6.104 (153) — Latest stable, mobile only, offline-first
**License:** GPL-3.0-only
**Source:** https://github.com/opticastplayer-dev/opticast
**Website:** https://opticastplayer-dev.github.io/opticast/
**Issues:** https://github.com/opticastplayer-dev/opticast/issues
**Changelog:** https://github.com/opticastplayer-dev/opticast/releases

**Description:**
Beautiful, fast local video player. Your own movies and TV shows, organized beautifully, playing perfectly offline. No ads, no tracking. 38M APK.

- LIBRARY: Finds videos automatically via MediaStore, adaptive poster grid, Continue Watching, Movies & TV Shows, Favorites, Recently Added, Collections, search. Fast scrolling.
- MOVIE INFO: Recognizes movies/TV from filenames via TMDB, posters cached offline, blurred fallback never black.
- SUBTITLES: OpenSubtitles + SubDL, always saved offline during scan, custom fonts, dual subs.
- PLAYER: mpv + Media3, plays all formats mkv/mp4/avi, edge-to-edge, gestures, PiP auto-resume, speed, audio/subs picker.
- UPDATES: Checks once when internet detected, minimal data, in-app FileProvider with progress, full changelog visible, Up To Date only when update available, What's New real new.
- OFFLINE: Works fully offline after posters/subtitles cached.
- PRIVATE: No ads, no tracking, GPL-3.0, media stays on device, no accounts.
- WHAT'S NEW in 2.6.104: Audio Only removed fixes blank video, library scrolling fast, RAM 8/12/16/20 + 32/48/64/96 LRU 30 pool 200 + Coil clear, offline-first, install over existing same JKS.

**Permissions (7 minimal):**
Internet, Network State, Video Library, Notifications, Background Playback, Install Updates, Wake Lock — No location/contacts/mic/camera

**Fastlane metadata:**
- Ready: title.txt, short_description.txt (Local video player, offline-first), full_description.txt, changelogs/153.txt, images/phoneScreenshots/ 5 real PNGs 1080p (Library, Detail, TV Show, Player, Settings)
- `fdroid lint com.opticast.player` passes (only apksigner warning)

**Builds:**
- versionName: 2.6.104, versionCode: 153, commit: v2.6.104, subdir: project, gradle: yes
- AutoUpdateMode: Version v%v, UpdateCheckMode: Tags, CurrentVersion 2.6.104 CurrentVersionCode 153
- Complete source with full mpv 27MB zip + SHA256SUMS in GitHub release https://github.com/opticastplayer-dev/opticast/releases/tag/v2.6.104
- Same JKS for install over existing, higher versionCode, APK versionName matches asset version

**Tested:**
- `fdroid readmeta` passes
- `fdroid lint com.opticast.player` passes
- Build: mpv + Media3, native libs prebuilt for stability

**Links:**
- GitHub: https://github.com/opticastplayer-dev/opticast
- Release: https://github.com/opticastplayer-dev/opticast/releases/tag/v2.6.104
- APK: https://github.com/opticastplayer-dev/opticast/releases/download/v2.6.104/OptiCast-v2.6.104.apk
- Website: https://opticastplayer-dev.github.io/opticast/
- FAQ: https://github.com/opticastplayer-dev/opticast/blob/main/docs/FAQ.md
- Privacy: https://github.com/opticastplayer-dev/opticast/blob/main/docs/PRIVACY.md
- Screenshots: docs/showcase/ 5 real device screenshots v2.6.104

**Checklist:**
- [x] Package never changes, signing consistent, versionCode increases
- [x] GPL-3.0-only, open source, no ads, no tracking, offline-first
- [x] Fastlane ready, 5 screenshots real, minimal permissions
- [x] Source includes full mpv, builds with gradle
- [x] AutoUpdateMode Version v%v, UpdateCheckMode Tags

Contact: opticastproject@gmail.com

Thank you for reviewing!
```

---

**After MR created, paste link here and I will track review.**

**File to add in your fork:** `metadata/com.opticast.player.yml` — content below (copy from root file):

```yaml
Categories:
  - Multimedia
License: GPL-3.0-only
AuthorName: OptiCast Project
AuthorEmail: opticastproject@gmail.com
WebSite: https://opticastplayer-dev.github.io/opticast/
SourceCode: https://github.com/opticastplayer-dev/opticast
IssueTracker: https://github.com/opticastplayer-dev/opticast/issues
Changelog: https://github.com/opticastplayer-dev/opticast/releases

Name: OptiCast Video Player
AutoName: OptiCast Video Player
Summary: Local video player, offline-first
Description: |
  OptiCast — Your Local Cinema, Offline-First

  Beautiful, fast local video player. Your own movies and TV shows, organized beautifully, playing perfectly offline.

  No ads, no tracking. 38M APK.

  LIBRARY: Finds your videos automatically, adaptive poster grid, Continue Watching, Movies & TV Shows, Favorites, Recently Added, Collections, search. Fast scrolling.

  MOVIE INFO: Recognizes movies/TV from filenames via TMDB, posters cached offline, blurred fallback never black.

  SUBTITLES: OpenSubtitles + SubDL, always saved offline during scan, custom fonts, dual subs.

  PLAYER: mpv + Media3, plays all formats mkv/mp4/avi, edge-to-edge, gestures, PiP auto-resume, speed, audio/subs picker.

  UPDATES: Checks once when internet detected, minimal data, in-app FileProvider with progress, full changelog visible, Up To Date only when update available, What's New real new.

  OFFLINE: Works fully offline after posters/subtitles cached, minimal data.

  PRIVATE: No ads, no tracking, GPL-3.0, media stays on device, no accounts.

  WHAT'S NEW in 2.6.104: Audio Only removed fixes blank video, library scrolling fast, RAM 8/12/16/20 + 32/48/64/96 LRU 30 pool 200 + Coil clear, offline-first, install over existing same JKS.

  WORKS ON: Android 8+, all phones, offline-first, private

RepoType: git
Repo: https://github.com/opticastplayer-dev/opticast.git

Builds:
  - versionName: 2.6.104
    versionCode: 153
    commit: v2.6.104
    subdir: project
    gradle:
      - yes

AutoUpdateMode: Version v%v
UpdateCheckMode: Tags
CurrentVersion: 2.6.104
CurrentVersionCode: 153
```
