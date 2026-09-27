# Final Showcase Status — OptiCast 2.6.60

## ✅ Live GitHub

**Primary (current live):** https://github.com/opticastplayer-dev/opticast
- Main branch: 4 commits, latest c56adf6
- Tags: v2.6.59, v2.6.60
- Releases:
  - v2.6.59: https://github.com/opticastplayer-dev/opticast/releases/tag/v2.6.59 — 3 assets (APK 31.4MB, Source 65.7MB, SHA)
  - v2.6.60: https://github.com/opticastplayer-dev/opticast/releases/tag/v2.6.60 — code fix, no APK yet (build via Actions or local)

**Desired org (not yet created):** https://github.com/opticast-project/opticast
- To create: https://github.com/organizations/new → name opticast-project → free → create
- Then transfer: https://github.com/opticastplayer-dev/opticast/settings → Transfer ownership → opticast-project

## Update Checker Dual-Repo Fallback (2.6.60)

**Code:** `project/app/src/main/java/com/opticast/player/data/remote/UpdateChecker.kt`

```kotlin
private val GITHUB_API_URLS = listOf(
    "https://api.github.com/repos/opticast-project/opticast/releases/latest", // primary desired
    "https://api.github.com/repos/opticastplayer-dev/opticast/releases/latest" // fallback live
)
```

- Loops URLs, tries primary first, fallback if 404/exception
- Stores successful URL, uses its releases page as fallbackHtml
- `openReleasesPage()` also falls back to live repo

**Result:**
- Before org transfer: primary 404 → fallback succeeds → update check works
- After org transfer: primary succeeds → update check works
- No code change needed after transfer

## Showcase Assets

**5 screenshots (7.7M total):**
- docs/showcase/01-library-grid.png 1.6M
- docs/showcase/02-detail-page.png 1.6M
- docs/showcase/03-player.png 1.9M
- docs/showcase/04-pip.png 1.5M
- docs/showcase/05-settings.png 1.4M

**Fastlane (F-Droid + Play):**
- fastlane/metadata/android/en-US/images/phoneScreenshots/*.png (same 5)
- project/fastlane/... same
- title.txt, short_description.txt, full_description.txt, changelogs/109.txt, 110.txt

**README.md:**
- Badges Release/License/F-Droid/Build/Downloads
- Showcase table with 5 images
- Features, Installation, Building, PiP fix, Privacy, License, Roadmap
- Locked baseline 2.6.59/109, now 2.6.60/110

## PiP Fix (2.6.59 retained)

- Track wasPlayingBeforePip
- onReturnFromPip callback → controller.play()
- Delay 400→1000ms, onResume 150ms auto-resume
- Check RESUMED state: if RESUMED → expand → resume else dismiss → pause
- Files: PiPController.kt, PlayerActivity.kt, PlayerScreen.kt

## In-App Updates + What's New

- UpdateChecker: GitHub API, 24h interval, manual check, download to cache, FileProvider install, handles REQUEST_INSTALL_PACKAGES
- Startup check once per day if online
- WhatsNewDialog: shows changelog after version change

## Version Bumps

- 2.6.59/109: PiP fix, in-app updates, What's New, showcase, F-Droid metadata, remove WhatsApp
- 2.6.60/110: Dual-repo fallback for update checker

## GitHub Actions

- .github/workflows/release.yml present
- Needs secrets:
  - SIGNING_KEY_BASE64 = base64 -w 0 signing/opticast-release.jks
  - SIGNING_PROPERTIES = content of signing.properties
- Without secrets, releases created manually via API (done for 2.6.59 + 2.6.60)
- With secrets, pushing tag auto-builds APK + source

## F-Droid

- Metadata: project/fastlane/ + project/.fdroid/metadata.yml v109 (needs update to 110)
- Next: MR to https://gitlab.com/fdroid/fdroiddata

## Budget

- Workspace: 111 MiB /128 MiB (before) → now with 2.6.60 docs ~112 MiB
- Releases not in git (only BASELINE/SHA/VERIFICATION), APK/source via Release assets

## Token

- PAT ghp_aBd6... used, unset, remote cleaned
- User: opticastplayer-dev (id 333497962, created 2026-09-24)
- Recommend delete after: GitHub → Settings → Developer settings → PAT

## Commands to Build 2.6.60 APK Locally

```bash
bash tools/setup.sh
python3 tools/restore-native-runtime.py
bash tools/build-apk.sh
python3 tools/package-release.py
```

Then upload to release v2.6.60 via:

```bash
export GH_TOKEN=...
RELEASE_ID=$(curl -s -H "Authorization: token $GH_TOKEN" https://api.github.com/repos/opticastplayer-dev/opticast/releases/tags/v2.6.60 | python3 -c "import json,sys; print(json.load(sys.stdin)['id'])")
curl -X POST -H "Authorization: token $GH_TOKEN" -H "Content-Type: application/vnd.android.package-archive" --data-binary @releases/OptiCast-2.6.60.apk "https://uploads.github.com/repos/opticastplayer-dev/opticast/releases/$RELEASE_ID/assets?name=OptiCast-2.6.60.apk"
```

## Final Checklist

- [x] Showcase screenshots generated and pushed
- [x] README with showcase table and badges
- [x] Fastlane metadata + screenshots for F-Droid
- [x] GitHub release workflow
- [x] Pushed to GitHub (opticastplayer-dev/opticast) main + tags v2.6.59 + v2.6.60
- [x] Created releases with assets (2.6.59) and notes (2.6.60)
- [x] Update checker dual-repo fallback (2.6.60)
- [x] PiP fix retained
- [x] Remove WhatsApp, Email only
- [x] In-app updates + startup check + What's New
- [ ] Create org opticast-project via UI and transfer
- [ ] Build 2.6.60 APK (requires signing key) and upload to release
- [ ] Add signing secrets to Actions
- [ ] F-Droid MR
- [ ] Update README badges to final org URL after transfer
