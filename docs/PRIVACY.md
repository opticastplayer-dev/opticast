# Privacy Policy — OptiCast Video Player

**Version:** v2.6.84 (build 133) — Mobile Only, Offline-First, No Ads, No Tracking

**Last updated:** 2026-09-29

### Summary
OptiCast is **100% offline-first, privacy-focused, no ads, no tracking, GPL-3.0 open source**. Your local cinema — plays your own media, no movies or streaming accounts supplied.

### Data Collection
**We collect nothing.** No analytics, no crash reporting to servers, no ads SDK, no tracking.

- **Local only:** Library scan via MediaStore, metadata from TMDB (only when you add API key), subtitles from OpenSubtitles/SubDL (only when you search with API keys)
- **No internet required:** Works fully offline after initial metadata/subtitle caching
- **Update check:** Once when internet detected (24h min, 7 days max), queries GitHub Releases API `api.github.com/repos/opticastplayer-dev/opticast/releases/latest` — minimal data, no personal info sent, only version check
- **No accounts:** No login, no Trakt, no Plex, no iCloud sync (mobile only, not TV)

### Permissions — Minimal 7

- `INTERNET` — TMDB metadata, OpenSubtitles/SubDL subtitles, GitHub update check (only when internet detected)
- `ACCESS_NETWORK_STATE` — Check if internet available before update check (offline-first data sipping)
- `WAKE_LOCK` — Keep screen awake while playing (if enabled in Settings)
- `READ_MEDIA_VIDEO` — Scan device for video files via MediaStore (Android 13+)
- `POST_NOTIFICATIONS` — MediaSession notification for playback controls (Android 13+)
- `FOREGROUND_SERVICE` + `FOREGROUND_SERVICE_MEDIA_PLAYBACK` — Playback service for background audio + PiP
- `REQUEST_INSTALL_PACKAGES` — In-app APK install via FileProvider for updates without browser
- Legacy `READ_EXTERNAL_STORAGE` maxSdk 32 / `WRITE_EXTERNAL_STORAGE` maxSdk 29 — For Android 8-12 compatibility

No location, no contacts, no microphone, no camera.

### Offline-First

- **Library:** Scanned via MediaStore, stored locally, no cloud
- **Posters/Backdrops:** Cached via Coil + `PosterCache`, `prefetchArtworkOnce` for offline use, blurred poster fallback when no backdrop (never black)
- **Subtitles:** Saved to `filesDir/subtitles/` with JSON sidecar, `secondary_subtitles.json` for dual subtitles, persists offline, always cached during scan when autoSubtitles enabled
- **Metadata:** Cached in `MetadataStore`, `CachedDetails` bundle, offline-first, no network while fresh
- **Thumbnails:** Frame previews cached for scrub, prepared on first scrub, offline

### Third-Party Services (Only When You Configure)

- **TMDB:** Optional, requires your API key, fetches movie/TV metadata, posters, backdrops. This product uses TMDB API but not endorsed by TMDB. Provider credits in app.
- **OpenSubtitles:** Optional subtitle search/download, requires API key, provider account rules/quotas apply. Subtitle authors retain rights.
- **SubDL:** Optional alternative subtitle search, requires API key.
- **OMDb / Fanart.tv:** Optional ratings + extra artwork, requires API keys.

All third-party use governed by their terms, quotas, copyright law. OptiCast does not claim redistribution licence.

### Updates

- Checks GitHub Releases API once when internet detected, 24h min, 7 days max — minimal data
- In-app download to cache, installs via FileProvider, no browser
- What's New card shows real changelog, compact design, dismissible

### Children's Privacy

No data collection, no ads, safe for all ages. Plays only your own media.

### Changes

Any changes to this policy will be in GitHub README + docs/PRIVACY.md + GitHub Pages.

### Contact

`opticastproject@gmail.com` — Email only.

### License

GPL-3.0 — Open source, source in GitHub releases `complete-source.zip` contains prebuilt AAR 26M with libmpv.

**Package:** `com.opticast.player` | **MinSdk 26** | **TargetSdk 35** | **Mobile only** (no TV, touchscreen required=true) | **37.6M APK** full mpv 24 .so, baseline 54 locked v2.6.84 (133)
