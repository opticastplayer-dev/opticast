# Changelog — OptiCast

**Current:** v2.6.105 (154) — 2026-09-30

## v2.6.105 (154) — 2026-09-30

- No API key needed — metadata via https://tmdb-proxy-xstu.onrender.com/api/ with retry for waking, confirmed working
- AppContainer tmdb now uses proxy adapter, drop-in replacement, fetches automatically
- Your videos, beautifully organized — header, Why it's different (focused local playback, offline-first, private & simple), Getting Started 30s, captioned screenshots, FAQ & Privacy links, stars/downloads
- README rewritten to match — simple, focused
- F-Droid full_description updated, short_description "Your videos, beautifully organized. Offline, private, no ads."
- First-run: PermissionGate private/offline/open source, EmptyLibrary Where are my videos? tooltip + quick tour
- Landing page: Why different, Getting Started, captioned screenshots, FAQ/Privacy links, stars/downloads, testimonials, presence

## v2.6.104 (153) — 2026-09-30 — Final

- Audio Only removed — fixes blank video until seek
- Library scrolling fast — removed unnecessary animations, keeps Adaptive grid, border/clip 12dp, badges, discovery
- RAM 8/12/16/20 + 32/48/64/96 LRU 30 pool 200 + Coil clear
- Offline-first: check once when internet detected, subtitles cached
- Install over existing same JKS, in-app FileProvider, full changelog visible
- Screenshots real 5, simple professional naming, trust files, F-Droid MR 50679

## Links

- GitHub: https://github.com/opticastplayer-dev/opticast
- Release: https://github.com/opticastplayer-dev/opticast/releases/tag/v2.6.105
- F-Droid MR: https://gitlab.com/fdroid/fdroiddata/-/merge_requests/50679
- Website: https://opticastplayer-dev.github.io/opticast/
- Proxy: https://tmdb-proxy-xstu.onrender.com/api/
