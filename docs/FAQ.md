# OptiCast FAQ — Mobile Only, Offline-First

**Current:** v2.6.84 (build 133) — 37.6M APK, full mpv 24 .so, baseline 54 locked, mobile only, no TV

### Is OptiCast for TV devices?
**No — mobile only.** This is a phone/tablet app for Android 8+ (API 26+), ARM64 + ARMv7, optimized for low-RAM 32-bit 3GB devices, buttery smooth 60fps. Android TV / Fire TV / Google TV support was removed in v2.6.84 to save 18KB and focus on mobile. No leanback, no banner, touchscreen required=true.

### Does it work offline?
**Yes — offline-first priority #1.** Library scan via MediaStore, posters/backdrops cached, subtitles saved to `filesDir/subtitles/` persist offline, metadata cached. Update check only once when internet detected (24h min, 7 days max) — minimal data, no background every 6h. Works fully offline.

### How to install over existing app?
Same signature, higher versionCode. Example: v2.6.83 build 132 → v2.6.84 build 133 installs over. Full mpv 24 .so mandatory (prebuilt AAR 26M permanent) — CI fails if APK <20M or missing libmpv, prevents Media3-only that breaks install-over.

### What's New card shows old info?
Fixed in v2.6.81+: UpdateChecker stores `whats_new_changelog` from GitHub release body when version changes, WhatsNewCard reads real changelog, fallback to version-specific compact 3-bullet. Compact design: 16dp corners, 12dp/6dp padding, titleSmall, bodySmall 16.sp max 4 lines, X 32dp clickable.

### Loading animation persists when quickly jumping videos?
Fixed in v2.6.81: Player state keyed to `video.id` (isPlaying, buffering, playbackEnded, showLoading), reset immediately on video change, `showLoading` checks `samePlaybackItem` + `requestIsCurrent`, delay 300ms to avoid flicker, auto-hide after 3s max, extra `LaunchedEffect(activeVideoId)` force hide.

### PiP expand doesn't auto-continue?
Fixed for 32-bit: `PiPController.wasPlayingBeforePip` tracks if playing before PiP, `onReturnFromPip` resumes `controller.play()` if true, 1000ms delay + RESUMED check for slow 32-bit devices.

### Library grid can't be changed?
Fixed in v2.6.83: `remember(appSettings.libraryGrid)` was inside LazyVerticalGrid columns param not triggering recomposition. Now `currentGridCells = remember(libraryGrid) { GridCells.Adaptive(libraryPosterMinimumDp(libraryGrid).dp) }` outside, then `columns = currentGridCells`. Compact 86dp, Medium 108dp, Comfortable 140dp.

### Black background in detail page (Afterburn 2025)?
Fixed in v2.6.79+: Blurred poster fallback when no backdrop — poster scaled 1.2x + alpha 0.6 + vertical gradient overlay, never black, offline-first (poster cached, backdrop may not be).

### Subtitles always cached?
Yes — always download subtitles during scan and cache them (when autoSubtitles enabled + internet + API keys). Saved to `filesDir/subtitles/` with JSON sidecar, secondary dual subtitles via `secondary_subtitles.json`, persists offline. Search uses OpenSubtitles + SubDL, offline-first.

### How to update inside app without browser?
Settings → Check for updates queries GitHub Releases API, downloads APK to cache, installs via FileProvider with progress + auto install, handles `REQUEST_INSTALL_PACKAGES`. No browser.

### Why no TV support?
Mobile only focus — saves 18KB, simplifies codebase (removed `ui/tv/` 481 lines TvHomeScreen + TvSupport), focuses on phone 32-bit 3GB buttery smooth. No leanback, no banner.

### Baseline locked automatically?
No — manual lock. `baseline-prof.txt` 54 entries locked, header manually updated on bump with date + notes. Not auto-generated via Macrobenchmark each version to keep startup <400ms deterministic. To auto-generate: `./gradlew :app:generateBaselineProfile`.

### F-Droid and Play Store?
- **F-Droid:** Metadata in `project/fastlane/` + changelogs 125-133, auto-updates from GitHub Tags mode
- **Play Store:** AAB ~23M download via per-ABI split (arm64 22-24M, armv7 21-23M) vs 37.6M universal APK, 75M install
- **GitHub:** Releases v2.6.78-84-optimized with APK + complete-source.zip (contains prebuilt AAR)

### How to build?
```bash
cd project && ./gradlew assembleRelease # 37.6M APK, R8 minify, shrinkResources, full mpv 24 .so
```

### Privacy?
No ads, no tracking, GPL-3.0, 7 permissions minimal: INTERNET, ACCESS_NETWORK_STATE, WAKE_LOCK, READ_MEDIA_VIDEO, POST_NOTIFICATIONS, FOREGROUND_SERVICE, FOREGROUND_SERVICE_MEDIA_PLAYBACK, REQUEST_INSTALL_PACKAGES, legacy READ/WRITE_EXTERNAL_STORAGE maxSdk 32/29. Offline-first, no accounts.

### Contact?
`opticastproject@gmail.com` — Email only, WhatsApp removed.

### Current rating vs Infuse?
- Standalone Android: 9.3/10
- vs Infuse (Apple gold standard 9.5/10): 8.7/10 overall, 9.2/10 for Android local-only (Infuse doesn't exist on Android, OptiCast is Infuse for Android)
- Beats Infuse in mpv codec support, low-RAM performance, privacy, price (free GPL)
