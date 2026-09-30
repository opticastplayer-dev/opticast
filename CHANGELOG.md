# Changelog — OptiCast

**Current:** v2.6.104 (153) — Stable 9.7/10 Ultra Fast — 2026-09-30

All notable changes, full changelog visible in app — not truncated link, offline-first #1.

## v2.6.104 (153) — 2026-09-30 — Stable Ultra Fast Restored + Audio Only Removed

- **Audio Only removed completely** — removed audioOnlyByDefault from SettingsRepository, var audioOnly + LaunchedEffect + AndroidView visibility + Audio only UI + PlayerActionButton audioonly from PlayerScreen (val audioOnly=false keeps structure balanced fixing align at 1840/2094 and top-level 2163), removed audioonly from playerControls and Start in audio-only mode from SettingsScreen, fixes blank video until seek by removing feature entirely
- **Ultra fast library scrolling** like episodes/settings — removed PosterCard press scale animateFloatAsState tween 100 + graphicsLayer scale + interactionSource, LibraryScreen nestedScroll chromeScroll + bottom bar AnimatedVisibility slide/fade 180/140 + haptic TextHandleMove/LongPress + snapFlingBehavior + FastScrollThumb overlay + chromeScroll NestedScrollConnection policy + snapshotFlow atTop + bottomChromePx/layoutDensity state, LibraryComponents favoriteBounce spring 0.38/420 + HeartBurst, Common shimmer infiniteTransition NEW badge, multi-select fadeIn/slideIn 200/260 → Box 100dp fixed — keeps Adaptive grid, border/clip 12dp, badges, discovery, progress, low-RAM 32-bit 3GB fast
- **Reverted v2.6.102 fling pause** that hid posters during scroll — posters always visible now, clean Infuse polish
- **RAM** 8/12/16/20 image + 32/48/64/96 disk LRU 30 pool 200 + Coil clear on playback, blank bar fixed, heart removed, collapsible no black bar, poster animation fixed, grid changeable fixed, startup library responsiveness matches settings
- **Offline-first #1:** check once when internet detected not every 6h, minimal data, subtitles cached during scan, Up To Date only when real update available not every startup, dismiss 48dp, What's New real new not old when fully updated
- **Install:** same JKS higher versionCode, in-app FileProvider progress, full changelog visible not truncated link, APK versionName matches asset version
- **Mobile only:** no TV, manual ServiceLocator DI 0KB, baseline 110 locked

## v2.6.103 (152) — 2026-09-30 — Revert Fling Pause

- Reverted v2.6.102 fling pause that hid posters during scroll — looked bad — restored v2.6.101 ultra fast without isScrolling, posters always visible

## v2.6.102 (151) — 2026-09-30 — Ultra Fast Fling Pause (Reverted)

- Attempted pause poster decoding during fling via derivedStateOf isScrollInProgress — showed FallbackPoster during scroll — looked bad, reverted

## v2.6.101 (150) — 2026-09-30 — Remove Audio Only + Ultra Fast Scroll

- Remove Audio Only completely, ultra fast scroll via removing press animation, nestedScroll, bottom bar animation, favoriteBounce, shimmer

## v2.6.100 (149) — 2026-09-30 — Stable RAM + Audio Instant

- StartupLoadControl simple, RAM via Coil clear + 8/12/16/20 + LRU 30/200, blank bar fixed

## Earlier — v2.6.93 and before

- See fastlane/metadata/android/en-US/changelogs/ for full history 125-153
- Features: Fast-scroll thumb, shared element hero, predictive back, custom fonts picker .ttf/.otf, baseline 110 locked, PiP auto-resume fixed 32-bit, grid changeable, in-app updates FileProvider, What's New real new, offline-first checks once when internet detected, subtitles cached, no Up To Date spam, X dismiss 48dp, loading animation fix, WhatsApp removed, TV removed saves 18KB, manual DI 0KB, mobile only

## Links

- GitHub: https://github.com/opticastplayer-dev/opticast
- Latest Release: https://github.com/opticastplayer-dev/opticast/releases/tag/v2.6.104
- Website: https://opticastplayer-dev.github.io/opticast/
- F-Droid: https://f-droid.org/packages/com.opticast.player
- FAQ: docs/FAQ.md, Privacy: docs/PRIVACY.md

---

**OptiCast v2.6.104 — Stable 9.7/10 Ultra Fast — Your local cinema, offline-first, no ads, open source**
