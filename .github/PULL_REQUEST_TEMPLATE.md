# Pull Request — OptiCast v2.6.104

**Checklist — Required for not sketchy, professional PR**

- [ ] **Mobile only** — No TV support, phone/tablet only, touchscreen required
- [ ] **Offline-first #1** — Check internet once when detected not every 6h, minimal data, subtitles cached during scan, posters cached offline
- [ ] **No features removed to fix smoothness** — Keep Adaptive grid, border/clip 12dp, badges, discovery, progress, low-RAM 32-bit 3GB fast. Previous builds before GitHub were buttery smooth with no features removed.
- [ ] **Version files correctly updated** — `project/app/build.gradle.kts` versionCode/versionName matches dispatch version, ran `project/sync-versions.sh`, updated README.md, docs/index.html, fastlane full_description, fdroid yml, changelog 153.txt full visible not truncated link
- [ ] **Install over existing** — Same JKS higher versionCode, FileProvider in-app download progress auto install, APK versionName matches asset version
- [ ] **Stability** — No `!!`, no star imports, files <1000 lines, baseline 110 locked, ANR watchdog, breadcrumb, exponential backoff, Coil clear on playback, RAM 8/12/16/20 + 32/48/64/96 LRU 30 pool 200
- [ ] **UI** — No black background, blurred poster fallback, X dismiss 48dp, loading animation not persisting when quickly jumping videos, What's New real new not old when fully updated, Up To Date only when real update available dismiss entirely after X
- [ ] **Manual DI** — ServiceLocator 0KB, no Hilt (100-200KB reflection cost rejected)
- [ ] **Complete source** — Full mpv included, 27MB zip uploaded with release + SHA256SUMS
- [ ] **Tested** — On 32-bit 3GB device if possible, grid changeable works, PiP auto-resume, startup library responsiveness matches settings

## Description

What does this PR do? Why? How to test?

## What's New

Full changelog visible for user — not truncated link.

## Screenshots

Library, Detail, Player — real app, no mockups.

## Related Issues

Fixes #...

## Contact

opticastproject@gmail.com

Thank you for making OptiCast better — offline-first, no ads, open source!
