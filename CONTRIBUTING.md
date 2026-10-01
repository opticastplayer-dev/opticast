# Contributing to OptiCast

Thank you for considering contributing to OptiCast — beautiful, offline-first, no ads, open source local video player for Android.

**Current:** v2.6.109 (158)

## How to Contribute

### Reporting Bugs
- Use GitHub Issues with template: Device, Version, Mobile only, Steps, Expected, Screenshots, Logs
- Check FAQ.md first, check latest release v2.6.104
- Include: library grid choppy? settings smooth? loading animation persists? PiP auto-resume? grid changeable? low-RAM 32-bit?
- No TV support — phone/tablet only

### Suggesting Features
- Focus: stability, reliability, performance, offline-first
- Do not suggest: TV support, Hilt (manual ServiceLocator 0KB preferred), features that remove existing functionality
- Open issue with [FEATURE] prefix

### Pull Requests
1. Fork repo, create branch `feat/your-feature`
2. Follow existing style: Kotlin, Compose, Material 3, manual DI, no Hilt, low-RAM safe
3. Keep Adaptive grid, border/clip 12dp, badges, discovery, progress — do not remove features to fix smoothness
4. Test on 32-bit 3GB device if possible — goal is fast on low-RAM
5. Update version files correctly: `project/app/build.gradle.kts` versionCode/versionName, run `project/sync-versions.sh`
6. Ensure baseline 110 locked, build passes, install over existing same JKS higher versionCode
7. Submit PR with clear description, link issue, show what's new full changelog not truncated link

### Code Style
- Kotlin, Compose, MVVM, ServiceLocator
- No `!!` — use safe calls
- No star imports
- Keep files under 1000 lines, split god files
- Offline-first: check internet once when detected not every 6h, minimal data, subtitles cached during scan

### Build
```bash
cd project
./gradlew assembleRelease
```
Requires Android Studio Ladybug+, JDK 17, compileSdk 36, minSdk 26, targetSdk 35

### License
GPL-3.0 — by contributing you agree to license your contributions under GPL-3.0

### Contact
opticastproject@gmail.com — Email only

Thank you for making OptiCast better!
