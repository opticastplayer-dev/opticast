# Showcase Screenshots — OptiCast

## Status: Accurate Mockups Based on Actual Code (Not Real Device Captures Yet)

These 5 screenshots are **AI-generated accurate mockups** based on the actual OptiCast codebase:

- **Color.kt**: Pitch-black #000000 background, primary amber #EEC177 (Midnight), ocean cyan #6FD3FF, brand violet #C08AFF, surfaceContainer #12141A, star yellow #FFC94D, favorite red #FF5470
- **LibraryScreen.kt**: Wordmark `opticast_wordmark`, subtitle "Your local cinema", top icons Tune/Customize, AutoFixHigh/Scan, Settings, filter chips Recent/Title/Rating/Year/Genre 48dp, StatsCard surfaceContainerLow 20dp, Continue Watching, Featured hero pager 16:9 280-460dp with gradient scrim, Recently Added, adaptive poster grid GridCells.Adaptive, PosterCard 2/3 aspect rounded 16dp shadow 16dp, BottomBar 52dp Movies/TV Shows/Favorites/Search
- **DetailScreen.kt**: Hero 16:9 with Brush.verticalGradient 0.25→transparent→0.72→background, back arrow, favorite heart, share, logo/title headlineMedium extraBold white, rating row star yellow, poster 110dp 2/3 rounded 16dp shadow 16dp, Watch Now Button 52dp rounded 18dp, FilledTonalButton Subtitles/Match 48dp, Mark as watched, RatingsRow IMDb/RT/MC, Story More/Less, FlowRow genres AssistChip, CastRow headshots, File info surfaceContainerLow 20dp
- **PlayerScreen.kt**: Black background, TopBar statusBarsPadding 12dp, PlayerActionButton 52dp circle black 0.50 alpha border white 0.25, center surface 28dp black 0.52 alpha, Bottom actions audioOnly MusicNote, Lock, ScreenRotation, Speed, ZoomOutMap, PictureInPictureAlt, AspectRatio, Center transport Replay10 52dp, BigPlayPauseButton 76dp circle black 0.55 alpha border white 0.3 Pause 40dp white, Forward10, PlayerProgressBar thick, ScrubPreview 148x83dp rounded 8dp border white 0.25, seek HUD Surface black 0.72 alpha rounded 16dp
- **PiP**: Floating window bottom right rounded 12dp with Replay10/Pause/Forward10, expand/close, library blurred behind, demonstrates auto-resume fix wasPlayingBeforePip + onReturnFromPip
- **SettingsScreen**: Search field rounded 22dp surfaceContainer, sections surfaceContainerLow 20dp: Appearance Theme Midnight, Playback engine 512KB Adaptive, Check for updates Current v2.6.60 Build 110 GitHub opticastplayer-dev/opticast, About, Data & performance Memory Snapshot 124MB, Contact Email

## Why Not Real Screenshots?

Real device screenshots require:
1. Running app on device/emulator with videos scanned
2. TMDB API key for posters
3. Manual capture via Android Studio

These mockups are **visually accurate to the code** but not pixel-perfect captures. They are suitable for:
- GitHub README showcase
- F-Droid fastlane (allowed, but real screenshots preferred)
- Play Store (requires real screenshots)

## How to Replace with Real Screenshots

1. Install OptiCast-2.6.59.apk or build 2.6.60 on device
2. Add videos, enter TMDB key in Settings, let it match
3. Capture:
   - Library: Continue Watching + poster grid
   - Detail: Dune or similar with backdrop, poster, ratings, cast
   - Player: Playing video with controls visible (tap screen)
   - PiP: Press PiP button, then show floating window over library
   - Settings: Scroll to show Check for updates + Data & performance
4. Save as:
   - `docs/showcase/01-library-grid.png` etc.
   - `fastlane/metadata/android/en-US/images/phoneScreenshots/` same
5. Push to GitHub

## Current Files

- 01-library-grid.png — Library with wordmark, stats, Continue Watching, Featured, poster grid, bottom bar
- 02-detail-page.png — Detail hero with gradient, poster 110dp, Watch Now 52dp, ratings, story, cast, file info
- 03-player.png — Player black background, top bar, bottom actions, center transport, seek bar, HUDs
- 04-pip.png — PiP floating window over blurred library, demonstrates auto-resume fix
- 05-settings.png — Settings searchable, appearance, playback engine, check for updates v2.6.60, data & performance

All 5 are 9:16 or 16:9, dark cinematic, amber/violet primary, Material 3 Expressive.

## Verification

Compare with actual code:
- `project/app/src/main/java/com/opticast/player/ui/theme/Color.kt` — colors
- `LibraryScreen.kt` — LibraryHeader, HeroPager, StatsCard, FilterChipsRow, BottomBar
- `DetailScreen.kt` — heroHeight (screenWidth*9/16 280-460dp), gradient, poster width 110dp, Button 52dp 18dp
- `PlayerScreen.kt` — PlayerTopBar, PlayerActionButton 52dp circle, BigPlayPauseButton 76dp, ScrubPreview 148x83dp 8dp, VerticalGestureHud

These mockups match the code's dimensions, colors, and component structure.

## TODO

- [ ] Replace with real device screenshots when device available
- [ ] Record video demo: library scan → detail → player gestures → PiP expand auto-resume → Settings update check
