# F-Droid Inclusion — OptiCast v2.6.104

**Package:** `com.opticast.player` | **Current:** v2.6.104 (153)

## Description for Users

**OptiCast — Your Local Cinema, Offline-First**

Beautiful, fast local video player for Android. Your own movies and TV shows, organized beautifully, playing perfectly offline.

No ads, no tracking, open source. 38M APK. Works on Android 8+, all phones, mobile only.

- Finds videos automatically, adaptive poster grid, Continue Watching, Movies & TV Shows, Favorites, Recently Added, Collections, search
- Recognizes movies/TV from filenames via TMDB, posters cached offline, blurred fallback never black
- Finds and saves subtitles offline during scan, supports two languages
- Plays all formats mkv/mp4/avi, beautiful player with gestures, PiP auto-resume
- Works fully offline, minimal data, easy updates inside app
- Fast, lightweight, private, no accounts

## Technical Details for Reviewers

**License:** GPL-3.0-only
**Author:** OptiCast Project — opticastproject@gmail.com
**Website:** https://opticastplayer-dev.github.io/opticast/
**Source:** https://github.com/opticastplayer-dev/opticast
**Issues:** https://github.com/opticastplayer-dev/opticast/issues
**Changelog:** https://github.com/opticastplayer-dev/opticast/releases

**Current:** v2.6.104 (153) — Mobile only, offline-first, no ads, open source

**Build:**
- mpv + Media3, native libs prebuilt for stability and install-over support
- Package name never changes, signing consistent, versionCode increases
- Fastlane metadata ready, 7 permissions minimal, no location/contacts/mic/camera
- Complete source with full mpv 27MB zip + SHA256SUMS in release

## F-Droid YAML

See `fdroid-com.opticast.player.yml` at root — Categories Multimedia, GPL-3.0-only, Author OptiCast Project, Website, Source, IssueTracker, Changelog, Name OptiCast Video Player, Summary Local video player offline no ads open source, Description simple, Repo git, Build gradle yes subdir project, AutoUpdateMode Version v%v, UpdateCheckMode Tags, CurrentVersion 2.6.104 CurrentVersionCode 153

## Steps to Submit

1. Fork https://gitlab.com/fdroid/fdroiddata
2. Create branch `com.opticast.player`
3. Add `metadata/com.opticast.player.yml`
4. Run `fdroid readmeta` and `fdroid lint com.opticast.player`
5. Test build: `fdroid build -v -l com.opticast.player`
6. Submit MR

## Status

- F-Droid API: Ready for PR, badge in README points to https://f-droid.org/packages/com.opticast.player
- Fastlane: Ready, changelog 153.txt simple
- Version: v2.6.104 mobile only, offline-first, no ads

Contact: opticastproject@gmail.com

Links:
- GitHub: https://github.com/opticastplayer-dev/opticast
- Release: https://github.com/opticastplayer-dev/opticast/releases/tag/v2.6.104
- Website: https://opticastplayer-dev.github.io/opticast/
- FAQ: docs/FAQ.md, Privacy: docs/PRIVACY.md
