# Changelog - OptiCast

**Current:** v2.6.120 (169) - 2026-10-04

## v2.6.120 (169) - 2026-10-04

**Website and distribution**
- New official website with direct APK download, QR code for easy phone install, and verified badges - https://opticastplayer-dev.github.io/opticast/
- Added official distribution guide with verified sources and installation verification

**Design and readability**
- Improved readability across light and dark themes, including Samsung Browser
- Added light/dark theme toggle on website
- Simplified feature descriptions for clarity - focused on local playback, offline-first, and privacy

**Security and privacy**
- In-app detection of installation source in Settings → About, with guidance for staying on official builds
- Removed dependency metadata from APK to meet store security checks

**Quality**
- Added additional automated tests for update handling and version checks
- Improved overall stability and maintainability

## v2.6.119 (168) - 2026-10-02

**Updates**
- Simplified update check UI - clear status and actions
- Automatic update check on startup with prompt when new version is available
- Download continues in background and allows install from any screen when ready
- Removed redundant update popups in library

**Visual**
- Slightly larger backdrop in media details for better presentation

**Repository**
- Cleaned repository history, keeping only latest release for clarity

## v2.6.118 (167) - 2026-10-02

- Improved update handling and install flow
- Refined library UI for smoother browsing

## v2.6.114 (163) - 2026-10-02

- Improved library organization and search
- Better handling of file availability and metadata matching
- Enhanced empty states and loading indicators

## v2.6.110 (159) - 2026-10-01

- Added safeguards to prevent builds with missing configuration that would break metadata fetching

## v2.6.109 (158) - 2026-10-01

- Security improvement: rotated service credentials and removed hardcoded secrets from repository

## v2.6.108 (157) - 2026-09-30

- Fixed metadata fetching issue that affected v2.6.106-107 - restored reliable poster and info loading

## v2.6.107 (156) - 2026-09-30

- Fixed in-app download canceling when navigating away - downloads now continue in background

## v2.6.106 (155) - 2026-09-30

- Improved API security with request validation and rate limiting
- Added version safeguards to ensure consistent version reporting

## v2.6.105 (154) - 2026-09-30

- No API key required - metadata fetching works out of the box
- Simplified onboarding and first-run experience
- Updated documentation and store listings for clarity

## v2.6.104 (153) - 2026-09-30

- Fixed video playback showing blank until seek
- Improved library scrolling performance
- Optimized memory usage for smoother experience on low-end devices
- Offline-first improvements and reliable installation over existing versions

## Links

- GitHub: https://github.com/opticastplayer-dev/opticast
- Latest release: https://github.com/opticastplayer-dev/opticast/releases/tag/v2.6.120
- Website: https://opticastplayer-dev.github.io/opticast/
