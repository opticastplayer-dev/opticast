# Changelog — OptiCast

**Current:** v2.6.105 (154) — 2026-09-30 — Secure proxy + honest simplicity

## v2.6.105 (154) — 2026-09-30 — Secure proxy + honest simplicity

**Focus: automatic metadata, honest simplicity tone, no comparisons**

- **Secure proxy**: No API key needed — metadata via https://tmdb-proxy-xstu.onrender.com/api/ with retry for Render waking (35s connect, 60s read, 3 retries exponential 2s/4s/8s). Fixes Cannot GET /3/search/movie — now uses /api/search/movie, /api/movie/550 etc. Confirmed working without key.
- **AppContainer**: tmdb now uses TmdbApiProxyAdapter (no settings), drop-in replacement, fetches automatically
- **Honest simplicity tone**: Landing page rewritten — header "Your videos, beautifully organized", hero "OptiCast is a local video player for Android...", Why it's different (focused local playback, offline-first, private & simple), Getting Started 30s, captioned screenshots, FAQ & Privacy links, stars/downloads badges, no self-scoring 9.5/10 removed, no beats VLC comparisons
- **README**: rewritten to match tone — "Your videos, beautifully organized", Why different, Getting Started with Where are my videos tooltip, captioned screenshots, what people say (early tester quotes), no 9.5/10
- **F-Droid**: full_description updated to honest simplicity, short_description "Your videos, beautifully organized. Offline, private, no ads."
- **First-run audited**: PermissionGate — "Your videos, beautifully organized" + private/offline/open source; EmptyLibrary — "Where are my videos?" tooltip with excluded folders, hidden files, pull to refresh, quick tour swipe/search/long-press
- **Landing page**: added Why it's different, Getting Started, captioned screenshots, FAQ/Privacy links, stars/downloads, testimonials, AlternativeTo/ProductHunt presence, first-run audit section

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
