# F-Droid Inclusion — OptiCast Video Player

**Package:** `com.opticast.player` | **Current:** v2.6.84 | **Status:** Ready for F-Droid

## Simple Description (For Users)

**OptiCast — Your Local Cinema, Offline-First**

Beautiful, fast local video player for Android. Your own movies and TV shows, organized beautifully, playing perfectly offline.

No ads, no tracking, completely free, open source. Works on Android 8 and newer, all phones, mobile only.

- Finds your videos automatically, beautiful poster grid you can customize
- Automatically recognizes movies and TV shows, fetches posters, saves offline
- Finds and saves subtitles for offline viewing, supports two languages
- Plays all video formats (mkv, mp4, avi, etc.), beautiful player with gestures
- Works fully offline, uses minimal data, easy updates inside app
- Fast and smooth, lightweight, private, no accounts needed

## Technical Details (For F-Droid Reviewers)

**License:** GPL-3.0-only
**Author:** OptiCast Project — opticastproject@gmail.com
**Website:** https://opticastplayer-dev.github.io/opticast/
**Source:** https://github.com/opticastplayer-dev/opticast
**Issues:** https://github.com/opticastplayer-dev/opticast/issues
**Changelog:** https://github.com/opticastplayer-dev/opticast/releases

**Current Version:** v2.6.84 — Mobile only, removed TV support, fast and smooth

**Build Details:**
- Uses powerful video engine that plays all formats
- Includes native libraries for best performance (prebuilt for stability and install-over support)
- Package name never changes, signing consistent, version increases
- Fastlane metadata ready: title, short description, full description, changelogs 125-133
- 7 permissions minimal, no location/contacts/mic/camera, offline-first, no ads/tracking
- Open source, source in releases contains everything needed

## F-Droid Metadata YAML

File `metadata/com.opticast.player.yml` for https://gitlab.com/fdroid/fdroiddata :

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
Summary: Best local video player for Android — offline, no ads, open source
Description: |
  OptiCast — Your Local Cinema, Offline-First, Mobile Only

  Beautiful, fast local video player for Android. Your own movies and TV shows, organized beautifully, playing perfectly offline.

  No ads, no tracking, completely free, open source.

  LIBRARY: Finds your videos automatically, beautiful poster grid you can customize, Continue Watching, Movies & TV Shows organized, Favorites, Recently Added, Collections, search, easy sharing. Fast and smooth on all Android phones.

  MOVIE INFO: Automatically recognizes movies and TV shows from filenames, fetches posters and information, saves for offline viewing, beautiful fallback never shows black background. Ratings, cast, genres, runtime, story.

  SUBTITLES: Finds and saves subtitles for offline viewing, supports two languages at once, easy sync adjustment, all formats supported. Always saved offline.

  VIDEO PLAYER — PLAYS EVERYTHING: Powerful engine plays all video formats (mkv, mp4, avi, etc.). Beautiful player: easy controls, speed control, aspect ratio, subtitle/audio picker. Gestures: double-tap to seek, swipe for volume/brightness, pinch to zoom, hold for fast forward, scrub preview. Sleep timer, auto-play next episode, chapters, notification controls, Picture-in-Picture that auto-resumes.

  UPDATES: Works offline-first: checks for updates only when internet is available, uses minimal data. Easy updates inside app with progress bar, no browser needed. Clearly shows what's new.

  WORKS OFFLINE: Works fully offline after saving posters and subtitles, uses minimal data.

  FAST & SMOOTH: Quick startup, smooth scrolling, lightweight, reliable.

  PRIVATE: No ads, no tracking, open source, your media stays on your device, no accounts needed.

  WHAT'S NEW in 2.6.84: Mobile only — focused on phones for best experience, removed TV support to keep app small and fast. Improved: faster, smoother, grid layout changeable, better file handling.

  WORKS ON: Android 8 and newer, all phones, offline-first, private
  LICENSE: Open source

RepoType: git
Repo: https://github.com/opticastplayer-dev/opticast.git

Builds:
  - versionName: 2.6.84
    versionCode: 133
    commit: v2.6.84-optimized
    subdir: project
    gradle:
      - yes

  - versionName: 2.6.83
    versionCode: 132
    commit: v2.6.83-optimized
    subdir: project
    gradle:
      - yes

AutoUpdateMode: Version v%v-optimized
UpdateCheckMode: Tags
CurrentVersion: 2.6.84
CurrentVersionCode: 133
```

## Steps to Submit

1. Fork https://gitlab.com/fdroid/fdroiddata
2. Create branch `com.opticast.player`
3. Add `metadata/com.opticast.player.yml`
4. Run `fdroid readmeta` and `fdroid lint com.opticast.player`
5. Test build: `fdroid build -v -l com.opticast.player`
6. Submit MR with description:

```
OptiCast Video Player — Best local video player for Android, offline, no ads, open source, mobile only

- Package: com.opticast.player
- Version: 2.6.84 — mobile only, fast, smooth
- License: GPL-3.0-only
- Source: https://github.com/opticastplayer-dev/opticast
- Description: Beautiful local video player, finds videos automatically, movie info, subtitles saved offline, plays all formats, works offline, fast and smooth, no ads, open source
- Permissions minimal, offline-first, private
- Fastlane metadata ready, changelogs 125-133
- Builds: gradle yes, subdir project, commit v2.6.84-optimized
- AutoUpdateMode: Version v%v-optimized, UpdateCheckMode: Tags
```

7. Wait for review

## Current Status

- F-Droid API: Not yet included, needs PR
- Badge in README points to https://f-droid.org/packages/com.opticast.player (will work after inclusion)
- Fastlane: Ready
- Version: v2.6.84 mobile only

## Contact

`opticastproject@gmail.com`

## Links

- GitHub: https://github.com/opticastplayer-dev/opticast
- Release: https://github.com/opticastplayer-dev/opticast/releases/tag/v2.6.84-optimized
- Website: https://opticastplayer-dev.github.io/opticast/
- FAQ: https://github.com/opticastplayer-dev/opticast/blob/main/docs/FAQ.md
- Privacy: https://github.com/opticastplayer-dev/opticast/blob/main/docs/PRIVACY.md
