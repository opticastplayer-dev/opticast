# OptiCast 2.6.60 (110) - Update Checker Dual-Repo Fallback

## What's New

### Update Checker Fallback (Fix for GitHub Org Transfer)
- **Problem:** 2.6.59 checked only `opticast-project/opticast` which doesn't exist yet (org not created). Update check failed silently.
- **Fix:** Now checks both:
  - Primary: `https://api.github.com/repos/opticast-project/opticast/releases/latest` (desired org, after transfer)
  - Fallback: `https://api.github.com/repos/opticastplayer-dev/opticast/releases/latest` (current live)
- Tries primary first, if fails (404/network) tries fallback automatically
- Determines correct releases page URL based on successful API
- `openReleasesPage()` also fallback to current live repo

### Files Changed
- `data/remote/UpdateChecker.kt`: Added `GITHUB_API_URLS` and `GITHUB_RELEASES_URLS` lists, loop with try/catch, fallbackHtml logic

### Why 2.6.60?
- 2.6.59 was PiP fix + in-app updates + showcase, pushed to `opticastplayer-dev/opticast`
- 2.6.60 adds dual-repo fallback so in-app updates work both before and after org transfer to `opticast-project`
- VersionCode 110, VersionName 2.6.60

### Distribution
- GitHub: `opticastplayer-dev/opticast` (current) → will be transferred to `opticast-project/opticast`
- F-Droid: metadata unchanged (v109 changelog still valid, v110 will be added)
- Showcase: same 5 screenshots, README updated

### Upgrade Path
- 2.6.59 users: Settings → Check for updates will fail (primary org 404). Workaround: open https://github.com/opticastplayer-dev/opticast/releases directly, or update to 2.6.60 which fixes checker.
- 2.6.60 users: Update checker works with both repos automatically.

### Locked Baseline
- Previous: 2.6.59/109 SHA 74084d45f74c166ea09302274d27aa0da8c0d2ca8feb494fa4a2b3c23dac689d
- New: 2.6.60/110 pending build, 746 tests expected, budget 111 MiB

### Next
- Build APK 2.6.60 (requires signing key)
- Push to GitHub with same showcase
- Create org opticast-project and transfer
- Add signing secrets for Actions auto-build

Contact: opticastproject@gmail.com
