# Version Safeguard — Prevents Misleading Version Issue

**Issue fixed:** v2.6.105 first build had versionCode 153 (same as v2.6.104) because tag pointed to old commit 1981bf1 before bump. User installed APK, Settings showed older version, no new tweaks — misleading.

**Root causes:**
1. Duplicate `versionCode =` and `versionName =` lines in build.gradle.kts (2 each) — second overrides first, but if both same value, hard to spot, if different, last wins
2. Tag v2.6.105 pushed before versionCode bump committed to main — tag pointed to old commit with old versionCode
3. Workflow only checked versionCode <124, not check if matches tag or > previous release

**Fixes implemented (never happens again):**

1. **Single source of truth in build.gradle.kts:**
   - Only ONE `versionCode =` and ONE `versionName =` line (removed duplicate)
   - Comment: "SINGLE SOURCE OF TRUTH: versionCode/versionName defined once — prevents mismatch"

2. **Release workflow safeguard (.github/workflows/release.yml):**
   - Checks duplicate lines: `VC_COUNT` must be 1, `VN_COUNT` must be 1 — fails if duplicate
   - Checks versionName matches tag: tag v2.6.105 must have versionName 2.6.105 — fails if mismatch
   - Checks versionCode > previous release versionCode — fails if not increasing, would not install over
   - Example log:
     ```
     versionCode lines: 1, versionName lines: 1
     versionCode=154 versionName=2.6.105
     Tag: v2.6.105 -> 2.6.105, versionName: 2.6.105
     Previous versionCode 153, new 154 — OK
     versionCode OK: 154 versionName OK: 2.6.105
     ```

3. **Process (must follow):**
   - Bump versionCode + versionName together in build.gradle.kts (single place)
   - Commit to main, push main
   - Then create tag `vX.Y.Z` from main HEAD, push tag
   - Workflow verifies tag matches build.gradle.kts, fails otherwise
   - Never push tag before pushing main with bumped version

4. **Local check:**
   - Before push, run: `grep -c 'versionCode' project/app/build.gradle.kts` must be 1
   - `grep -oP 'versionCode\s*=\s*\K\d+'` must be > previous release

**Current:** v2.6.105 (154) — correct, single source, matches tag, increasing, verified by workflow 36734496075 SUCCESS.

**Future:** Any mismatch will FAIL build early with FATAL message, never produces misleading APK.
