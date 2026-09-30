# Changelog — OptiCast

**Current:** v2.6.108 (157) — 2026-09-30

## v2.6.108 (157) — 2026-09-30

- Fix: metadata fetching failed in v2.6.106-107 vs v2.6.105 — root cause: secured proxy requires X-App-Secret, but CI builds had blank BuildConfig.TMDB_PROXY_SECRET (no TMDB_PROXY_SECRET env in workflow), so proxy returned 403 and metadata fetch failed. v2.6.105 worked because proxy was open (no secret required). Fix: workflow now injects secret from GitHub secrets TMDB_PROXY_SECRET / APP_SECRET into project/local.properties and env TMDB_PROXY_SECRET, build.gradle.kts now checks both TMDB_PROXY_SECRET and APP_SECRET env, BuildConfig gets secret AsGfhVhE0NxilwqMapsqLpE3bE7exg1n, proxy returns 200, metadata works like v2.6.105
- Workflow: added Inject TMDB proxy secret step before Build APK, also fixed version safeguard to allow rebuild of same tag (equal VC OK)

## v2.6.107 (156) — 2026-09-30

- Fix: in-app download no longer cancels when scrolling settings or going to library — root cause: rememberCoroutineScope tied to composable lifecycle, cancelled on navigation. Fixed with global downloadScope SupervisorJob IO that survives navigation + NonCancellable file IO, progress via StateFlow that survives
- UI: UpdateCheckOption now observes globalDownloading + globalProgress StateFlow, shows "Downloading: X% — continues even if you scroll or go to Library"
- Both download buttons now use startDownloadInBackground instead of scope.launch downloadAndInstall directly

## v2.6.106 (155) — 2026-09-30

- Secured proxy: requires X-App-Secret header AsGfhVhE0NxilwqMapsqLpE3bE7exg1n, rate limit 30/10s, CORS blocked, /health 200 — 9/10 safe, tested live: without secret 403, with secret 200, rate limit headers present
- Client TmdbProxyClient sends X-App-Secret from BuildConfig (injected from local.properties, not GitHub), @PublishedApi internal fix for compilation
- Server SECURE_PROXY_SERVER.js deployed to Render, env TMDB_API_KEY + APP_SECRET set, deploys Live green (was Failed red due to missing env)
- Version safeguard: single versionCode/versionName, workflow checks duplicate, tag match, increasing — prevents misleading version issue where 2.6.105 showed 2.6.104
- Build 36734496075 SUCCESS with versionCode 154, now 155

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
