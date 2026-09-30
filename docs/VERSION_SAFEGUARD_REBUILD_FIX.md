# Version Safeguard Rebuild Fix — Pending Push

**Issue:** Release workflow `release.yml` fails when rebuilding same tag (e.g., v2.6.107) because it compares new versionCode 156 <= previous versionCode 156 (same tag) and exits FATAL.

**Root cause:** `releases/latest` returns the same tag being built if a release already exists (even if assets missing), so equal versionCode triggers failure. Should allow equal for same-tag rebuild.

**Fix (in local commit bf26e76 / e138e4a):**

```bash
# Old:
PREV_VC=$(curl ... releases/latest ...)
if [ -n "$PREV_VC" ]; then
  PREV_BUILD=...
  if [ "$PREV_BUILD" -ge "$VC" ]; then FATAL; exit 1; fi
fi

# New:
PREV_VC=$(curl ... releases/latest ...)
if [ -n "$PREV_VC" ]; then
  TAG_VER_CURRENT=${TAG_VER:-${TAG#v}}
  if [ "$PREV_VC" = "$TAG_VER_CURRENT" ]; then
    echo "Previous tag is same as current ($PREV_VC) — rebuild, skipping increasing check"
  else
    PREV_BUILD=...
    if [ "$PREV_BUILD" -ge "$VC" ]; then FATAL; exit 1; fi
  fi
fi
```

**Why not pushed yet:** PAT `ghp_aBd6...` lacks `workflow` scope, GitHub rejects push of `.github/workflows/release.yml`:
`refusing to allow a Personal Access Token to create or update workflow without workflow scope`

**Workaround applied for v2.6.107:** Deleted failed release v2.6.107 (id 400292959) via API, so latest became v2.6.106 (155). Then re-pushed tag v2.6.107 — old safeguard saw 156 > 155 and passed. Build 36755323847 succeeded, APK 39458482.

**Next steps:**
- For v2.6.108+, old safeguard will pass because 157 > 156, so not blocking.
- To permanently fix rebuild case, push the fix with a PAT that has `workflow` scope, or via GitHub web UI edit, or via `gh` CLI with workflow scope.

**File to apply:** `.github/workflows/release.yml` lines 124-135.

**Status:** v2.6.107 LIVE — https://github.com/opticastplayer-dev/opticast/releases/tag/v2.6.107
