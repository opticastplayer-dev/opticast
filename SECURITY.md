# Security Policy — OptiCast

**Current:** v2.6.109 (158)

## Supported Versions

| Version | Supported          |
| ------- | ------------------ |
| 2.6.109 | :white_check_mark: |
| < 2.6.109 | :x: (please update) |

We support only latest stable release. Previous releases deleted from GitHub — only v2.6.109 kept.

## Reporting a Vulnerability

**Do not open public issue for security vulnerabilities.**

Email: **opticastproject@gmail.com** with subject `[SECURITY]`

Include:
- Device, Android version, OptiCast version (e.g. v2.6.104 build 153)
- Description, steps to reproduce, impact
- Screenshots/logs if applicable
- Whether offline-first affected, install-over existing, FileProvider, mpv, subtitles, TMDB

We will:
- Acknowledge within 48h
- Investigate, fix, release patch with full changelog visible
- Credit reporter if desired

## Privacy & Permissions

OptiCast is 100% private:
- No accounts, no tracking, no analytics, no ads
- Permissions only: Internet, Network State, Video Library, Notifications, Background Playback, Install Updates, Wake Lock
- No location, contacts, microphone, camera
- Library stays on device, works 100% offline after posters/subtitles cached
- Update check only when internet available, minimal data, version number only
- See `docs/PRIVACY.md`

## Build Integrity

- Same JKS for install over existing, higher versionCode
- APK versionName matches asset version
- Complete source with full mpv uploaded (27MB zip) + SHA256SUMS
- Baseline 110 locked, R8 fullMode
- Manual ServiceLocator DI 0KB — no Hilt reflection risk

## No Sketchy Behavior

- Open source GPL-3.0, full source on GitHub
- No hidden code, no WhatsApp, no TV bloat, mobile only
- F-Droid available, GitHub Pages live, AlternativeTo listing, Reddit draft in docs/
- Contact email only, no hidden tracking

Thank you for keeping OptiCast safe!
