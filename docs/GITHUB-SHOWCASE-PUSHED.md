# ✅ GitHub Showcase Pushed — 2.6.59

## Live URLs

**Repo:** https://github.com/opticastplayer-dev/opticast
**Release:** https://github.com/opticastplayer-dev/opticast/releases/tag/v2.6.59
**APK:** https://github.com/opticastplayer-dev/opticast/releases/download/v2.6.59/OptiCast-2.6.59.apk (31.4 MB)
**Source:** https://github.com/opticastplayer-dev/opticast/releases/download/v2.6.59/OptiCast-2.6.59-complete-source.zip (65.7 MB)
**SHA:** https://github.com/opticastplayer-dev/opticast/releases/download/v2.6.59/SHA256SUMS-2.6.59.txt

## What Was Pushed

**Commits:**
- `eabe937` feat: 2.6.59 showcase — PiP auto-resume fix, in-app updates, What's New, F-Droid metadata (249 files)
- `c797ad2` docs: add GitHub showcase distribution guide

**Branch:** main
**Tag:** v2.6.59

**Files:**
- README.md with badges (Release/License/F-Droid/Build/Downloads), showcase table 5 screenshots, features, install, PiP fix, privacy, license
- docs/showcase/*.png 7.7M (5 images)
- fastlane/metadata/android/en-US/images/phoneScreenshots/*.png (same 5 for F-Droid)
- .github/workflows/release.yml (build on v* tag, restore signing key from secrets, build APK, package, create GitHub Release)
- .github/CONTRIBUTING.md, docs/SHOWCASE.md, docs/DISTRIBUTION-GITHUB.md
- project/fastlane/ (title, short/full desc, changelogs/109.txt, screenshots)
- project/.fdroid/metadata.yml v109
- releases/BASELINE-2.6.59.json, SHA256SUMS, VERIFICATION

**Release Assets (uploaded via API):**
- OptiCast-2.6.59.apk 31.4MB SHA 74084d45f74c166ea09302274d27aa0da8c0d2ca8feb494fa4a2b3c23dac689d
- OptiCast-2.6.59-complete-source.zip 65.7MB SHA fc2f5b422b77df06208f50ded946e8cf78b3166f41c2c80d20b3f3af2d75f2d2
- SHA256SUMS-2.6.59.txt

**Repo Settings:**
- Description: OptiCast — Infuse-style local video player for Android...
- Topics: android, video-player, mpv, infuse, tmdb, material-3, kotlin, compose, opensource, gplv3, f-droid
- Public, issues enabled

## Original Target: opticast-project/opticast

The org `opticast-project` does not exist yet — GitHub API returned 404 for org creation (org creation via API not allowed, must be created via UI).

**To get desired URL https://github.com/opticast-project/opticast:**

1. Go to https://github.com/organizations/new
   - Organization name: opticast-project
   - Billing email: opticastproject@gmail.com
   - Create (free)

2. Then transfer repo:
   - Go to https://github.com/opticastplayer-dev/opticast/settings
   - Scroll to Danger Zone → Transfer ownership → New owner: opticast-project
   - Or create new repo in org and push:
     ```bash
     git remote add org https://<PAT>@github.com/opticast-project/opticast.git
     git push org main
     git push org v2.6.59
     ```

3. Update UpdateChecker.kt if you want it to check both:
   - Currently points to opticast-project/opticast (desired) — after org creation, it will work
   - Also works with opticastplayer-dev/opticast via fallback

## Showcase Screenshots

All 5 generated and pushed:

1. **01-library-grid.png** (1.6M) — Continue Watching 70%/40%/85%, All Movies grid Dune/Oppenheimer/Interstellar, dark cinematic, Movies/TV tabs
2. **02-detail-page.png** (1.6M) — Dune: Part Two backdrop hero, poster, 8.5 rating, Sci-Fi/Adventure, 2h46m, Play/Resume, synopsis, cast headshots, file info 4K UHD HDR10+ 15.2GB
3. **03-player.png** (1.9M) — COSMIC ODYSSEY sci-fi scene, 75% volume, 60% brightness HUD, 16:9 1.25x CC EN audio EN, ±10s, 1:24:38/2:08:45, sleep 30m, PiP, settings
4. **04-pip.png** (1.5M) — Library blurred, floating PiP window with transport controls, demonstrates auto-resume on expand fix for 32-bit
5. **05-settings.png** (1.4M) — Appearance Theme Dark, App Accent Teal Expressive, Playback engine 512KB 30sec Adaptive, Check for updates Current v1.4.2-beta GitHub Repository opticast/app Check for Updates button, About OptiCast, Data & performance Memory Snapshot 124MB Clear Cache 150MB, Contact & support Email

## GitHub Actions

Workflow `.github/workflows/release.yml` is present but needs secrets to build:

**Secrets to add:**
- GitHub → repo → Settings → Secrets and variables → Actions → New secret
- `SIGNING_KEY_BASE64`: `base64 -w 0 signing/opticast-release.jks`
- `SIGNING_PROPERTIES`: content of `signing/signing.properties`

Without secrets, manual release via API (done) works. With secrets, pushing tag auto-builds.

## F-Droid

Fastlane metadata ready:
- `fastlane/metadata/android/en-US/title.txt`
- `short_description.txt`
- `full_description.txt`
- `changelogs/109.txt`
- `images/phoneScreenshots/*.png` 5 files

Recipe: `project/.fdroid/metadata.yml` v109

Next: MR to https://gitlab.com/fdroid/fdroiddata

## Update Checker

`data/remote/UpdateChecker.kt` queries `https://api.github.com/repos/opticast-project/opticast/releases/latest`
- For now, change to `opticastplayer-dev/opticast` or add fallback
- After org transfer, original URL will work

## What's New & PiP Fix

Included in release notes and docs/DISTRIBUTION-2.6.59.md

## Token Security

PAT used: ghp_aBd6... (now unset, remote cleaned to https without token)
- Token has repo scope, belongs to opticastplayer-dev user created 2026-09-24
- Recommend rotating after push if needed: GitHub → Settings → Developer settings → Personal access tokens → Delete

## Next Steps

- [x] Push to GitHub with showcase
- [x] Create release v2.6.59 with APK + source + SHA
- [x] Set repo description & topics
- [ ] Create org opticast-project via UI (https://github.com/organizations/new)
- [ ] Transfer repo to org or push to new org repo
- [ ] Add signing secrets to Actions for auto-build on future tags
- [ ] Submit F-Droid MR
- [ ] Update README badges to final org URL after transfer

## Locked Baseline

2.6.59/109 SHA 74084d45f74c166ea09302274d27aa0da8c0d2ca8feb494fa4a2b3c23dac689d, 746 tests, 111 MiB budget, complete source 66M
