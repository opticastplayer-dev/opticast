# Contributing to OptiCast

Thank you for considering contributing to OptiCast — a beautifully organized, offline-first, open source local video player for Android.

**Current:** v2.6.120 (169)

## How to Contribute

### Reporting Bugs

- Use GitHub Issues with a clear description: device, version, steps to reproduce, expected behavior, screenshots or logs
- Check `docs/FAQ.md` and the latest release before reporting
- Focus on stability, performance, and offline-first experience

### Suggesting Features

- Focus on stability, reliability, performance, and offline-first experience
- Open an issue with `[FEATURE]` prefix and describe the use case
- We prioritize features that benefit personal video collections and privacy

### Pull Requests

1. Fork the repository and create a branch `feat/your-feature`
2. Follow existing code style: Kotlin, Compose, Material 3
3. Keep the app lightweight and efficient — avoid adding unnecessary dependencies
4. Test your changes thoroughly
5. Update documentation if needed
6. Submit a PR with a clear description and link to related issues

### Commit Style — Short and brief like VLC

We follow VLC/mpv style — short, component prefix, little description only where needed:

```
<type>(<scope>): <short description>

- optional bullet, only if needed to avoid confusion
```

**Types:** `feat`, `fix`, `docs`, `refactor`, `chore`, `perf`
**Scopes:** `player`, `library`, `settings`, `metadata`, `subtitles`, `docs`, `website`, `build`

**Examples (VLC-style):**
- `player: show battery and clock`
- `fix: allow Media3-only build`
- `docs: update FAQ`
- `refactor: simplify metadata to TMDB only`
- `library: add trash and restore`
- `settings: hide network behind beta toggle`

**Rules:**
- Title ≤50 chars, lowercase, no period, no em dash
- No AI phrases: no "No version bump, safe for F-Droid", no "lightweight for solo", no "professional"
- Body only if needed to avoid confusion — 1-2 short bullets max
- Keep brief like VLC: https://github.com/videolan/vlc/commits/master/

### Code Style

- Kotlin and Jetpack Compose
- Material 3 design principles
- Avoid forced unwrapping — use safe calls
- Keep files focused and under 1000 lines where possible
- Offline-first design: minimize network usage and support offline viewing

### Build

```bash
cd project
./gradlew assembleRelease
```

Requires Android Studio and JDK 17.

### License

GPL-3.0-only — by contributing you agree to license your contributions under the same license.

### Contact

opticastproject@gmail.com

Thank you for making OptiCast better!
