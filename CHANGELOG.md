# Changelog — OptiCast

**Current:** v2.6.116 (165) — 2026-10-02

## v2.6.116 (165) — 2026-10-02

- Gold Architecture Step 3: Split LibraryScreen 1199→1081 lines — safe incremental 9.2→9.4/10
- New library components: LibraryPermissionHandler.kt, LibraryDeleteHandler.kt, LibraryDialogsHost.kt, LibraryMenuHost.kt, LibraryUiState.kt, LibraryDiscoverySections.kt — single responsibility, thin composable
- LibraryScreen now uses deleteHandler for performDelete, permissionState for permission, dialogsHost for all dialogs, menuHost for entry menu
- Keeps FileActions, SelectionState, SelectionBar, BottomSheets, FastScrollThumb from v2.6.115, domain UseCases, TmdbRepository, EmptyState premium, fail-fast, baseline 110 locked

## v2.6.115 (164) — 2026-10-02

- Gold Architecture Step 2: Split LibraryScreen 1336→1199 lines — safe incremental 9.0→9.2/10
- Extracted library components: LibraryFileActions.kt, LibrarySelectionState.kt, LibrarySelectionBar.kt, LibraryBottomSheets.kt, LibraryFastScrollThumb.kt, LibraryGridSection.kt, LibraryScreenStateHolder.kt, LibraryContentGrid.kt — single responsibility, thin composable
- LibraryScreen now uses LibraryFileActions for share/mime/uri, LibrarySelectionState for selection mode, LibrarySelectionBar for floating bar, LibraryGenrePickerSheet for genre picker
- FastScrollThumb delegated to library/LibraryFastScrollThumb.kt — low-RAM safe derivedStateOf + graphicsLayer
- Keeps domain layer UseCases, TmdbRepository interface, EmptyState premium, fail-fast, secret rotation, metadata fix, baseline 110 locked

## v2.6.114 (163) — 2026-10-02

- Gold Architecture Step 1: Domain layer + UseCases — safe incremental, 8.5→9.0/10
- New domain/usecase: SearchLibraryUseCase, GetContinueWatchingUseCase, RefreshLibraryUseCase, RecheckFilesUseCase, MatchMetadataUseCase, GetLibraryEntriesUseCase, ClearMetadataUseCase, SetWatchedUseCase — pure business logic, no Android, testable, 0KB
- LibraryViewModel now uses UseCases: thin ViewModel, business logic out of composable, uses repository interface
- Keeps repository interfaces, EmptyState premium, fail-fast, secret rotation, metadata fix, download fix, baseline 110 locked for v2.6.109

## v2.6.113 (162) — 2026-10-02

- Gold Step 2B: Architecture — Repository interfaces + split LibraryScreen (Option B)
- TmdbRepository: new interface + impl + fake — separates TMDB data from UI, easy to test, no God object, mirrors LibraryRepository pattern
- LibraryViewModel: split from LibraryScreen.kt (was 226 lines inside screen, total 1562 → now 1335 screen + 200 ViewModel separate file) — single source of truth, uses repository interface, business logic out of composable, offline-first, survives rotation, easy to test with FakeLibraryRepository
- LibraryRepository interface already existed with impl + fake — now properly used
- Keeps EmptyState premium (spring animation), fail-fast, secret rotation, metadata fix, download fix, baseline 110 locked for v2.6.109
- No size increase, still 38M, manual DI 0KB kept (no Hilt 100-200KB rejected)

## v2.6.112 (161) — 2026-10-02

- Fix: library messed up — MOVIES section showed 4 empty outlined boxes instead of posters (Screenshot_20261002_055504_OptiCast.jpg). Root cause: PosterCard sharedElement + animateItem in LazyVerticalGrid caused empty boxes. Fixed by removing sharedElement from grid cards, keeping ultra-fast baseModifier, SharedElement only for detail header (safe no-op). Grid now shows all 27 movies correctly like v2.6.110
- Keeps Gold Step 2 EmptyState premium: spring animation, 96dp Surface primaryContainer, bold title — for library empty, search no results, offline
- Keeps fail-fast, secret rotation, metadata fix, download fix, baseline 110 locked

## v2.6.111 (160) — 2026-10-02

- Gold Step 2: Empty states + SharedElement — premium UX like Infuse
- EmptyState: new component ui/components/EmptyState.kt with spring animation (scale + alpha, bouncy), large 96dp Surface primaryContainer, 48dp icon, bold title, clear actions — used for library empty, search no results, offline, no internet
- EmptyLibrary now uses EmptyLibraryPremium — simple professional, no technical bullets
- SharedElement: LibraryGrid accepts sharedTransitionScope + animatedVisibilityScope, PosterCard applies sharedElement key poster-{id} via rememberSharedContentState, DetailScreen already had sharedElement poster-{id} — poster morphs grid → detail header 300ms spring
- All 4 PosterCard calls in LibraryScreen now pass sharedTransitionScope + animatedVisibilityScope
- Keeps fail-fast, secret rotation, metadata fix, download fix, baseline 110 locked

## v2.6.110 (159) — 2026-10-01

- Gold Step 1: fail-fast check — prevents silent metadata failure like v2.6.106-107. If TMDB proxy secret blank, official release build now throws FATAL immediately instead of building APK that returns 403. Ensures metadata always works, never ships broken build. Build checks isOfficialRelease && proxySecret.isBlank() → throw GradleException
- Keeps secret rotation, metadata fix, download fix, secured proxy 9/10 safe, simple professional docs

## v2.6.109 (158) — 2026-10-01

- Security: rotate proxy secret — old secret was public in repo history (build.gradle.kts fallback) and APK, now rotated to new secret (stored in GitHub secrets TMDB_PROXY_SECRET + APP_SECRET via API 204 and Render env), old secret revoked
- Remove hardcoded public secret from build.gradle.kts — now no fallback in public repo, secret comes only from local.properties or env, prevents secret being public in repo history, workflow injection ensures metadata works
- Keeps metadata fix + download fix + secured proxy 9/10 safe: 403 without secret, 200 with secret

## v2.6.108 (157) — 2026-09-30

- Fix: metadata fetching failed in v2.6.106-107 vs v2.6.105 — root cause: secured proxy requires X-App-Secret, but CI builds had blank BuildConfig (no env in workflow), so proxy returned 403 and metadata fetch failed. v2.6.105 worked because proxy was open. Fix: workflow now injects secret from GitHub secrets into project/local.properties and env, BuildConfig gets secret, proxy returns 200, metadata works like v2.6.105
- Workflow: added Inject TMDB proxy secret step before Build APK, also fixed version safeguard to allow rebuild of same tag (equal VC OK)

## v2.6.107 (156) — 2026-09-30

- Fix: in-app download no longer cancels when scrolling settings or going to library — root cause: rememberCoroutineScope tied to composable lifecycle, cancelled on navigation. Fixed with global downloadScope SupervisorJob IO that survives navigation + NonCancellable file IO, progress via StateFlow that survives
- UI: UpdateCheckOption now observes globalDownloading + globalProgress StateFlow, shows "Downloading: X% — continues even if you scroll or go to Library"
- Both download buttons now use startDownloadInBackground instead of scope.launch downloadAndInstall directly

## v2.6.106 (155) — 2026-09-30

- Secured proxy: requires X-App-Secret header, rate limit 30/10s, CORS blocked, /health 200 — 9/10 safe, tested live: without secret 403, with secret 200, rate limit headers present
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
