# Bulletproof Build System - Prevent Install-Over Failures

## Issue History

**v2.6.73 broke install-over v2.6.72:**
- Root cause: Deleting old releases broke restore script (404)
- Result: Media3-only APK 5.6M without libmpv
- Impact: Cannot install over full mpv APK 31M with libmpv

## Permanent Fixes Applied (v2.6.75+)

### 1. Prebuilt AAR Committed (Primary Fix)

**File:** `project/native/prebuilt/opticast-mpv-runtime.aar` (25MB)

- **Not gitignored** - permanent in repo
- Contains full mpv: libmpv.so, libavcodec.so, etc. (24 .so files)
- Restore script checks this FIRST
- Even if GitHub releases deleted, build works

**Before:**
```python
APK_URLS = ["https://.../v2.6.59/OptiCast-2.6.59.apk"] # Deleted -> 404 -> Media3-only
```

**After:**
```python
PREBUILT_AAR = "project/native/prebuilt/opticast-mpv-runtime.aar" # Always exists
if is_full_mpv_aar(PREBUILT_AAR):
    copy to cache and use
    return None # Skip download
```

### 2. Fail Fast Instead of Silent Media3-Only

**Before:**
```yaml
python3 restore-from-apk.py || echo "Restore failed" # Continues to build Media3-only
```

**After:**
```yaml
python3 restore-from-apk.py
if [ $? -ne 0 ]; then
  echo "FATAL: Refusing Media3-only that breaks install-over"
  exit 1
fi
```

### 3. APK Verification Step

```yaml
- name: Verify APK is full mpv
  run: |
    SIZE=$(stat -c%s "$APK")
    if [ "$SIZE" -lt 20000000 ]; then
      echo "FATAL: APK too small, Media3-only"
      exit 1
    fi
    if ! unzip -l "$APK" | grep -q libmpv; then
      echo "FATAL: Missing libmpv"
      exit 1
    fi
```

### 4. Version Handling Fixed

**Before:**
```yaml
VERSION="${{ inputs.version }}"
if [ -z "$VERSION" ]; then
  VERSION="2.6.60" # Bug: always 2.6.60 on tag push
fi
```

**After:**
```yaml
VERSION="${{ inputs.version }}"
if [ -z "$VERSION" ]; then
  VERSION="${{ github.ref_name }}" # Use tag name like v2.6.75-optimized
fi
if [ -z "$VERSION" ]; then
  VERSION="2.6.75-optimized"
fi
VERSION=${VERSION#v} # Strip leading v
```

### 5. Workflow File Protection

**Problem:** PAT without `workflow` scope cannot push `.github/workflows/` files

**Solution:**
- Prebuilt AAR fix doesn't require workflow file push (already committed)
- For workflow fix, manually update via GitHub Web UI:
  1. Go to https://github.com/opticastplayer-dev/opticast/edit/main/.github/workflows/release.yml
  2. Paste content from `/tmp/final-release.yml` (or current local fixed version)
  3. Commit directly to main

**Alternative:** Use `workflow_dispatch` with version input (works even with buggy release.yml):
```bash
gh workflow run release.yml -f version=2.6.75-optimized -f full_mpv=true
```

## How to Verify Future Builds

1. **Check artifact size:** Should be ~63-89MB (APK 31M + source 59M), not 13.5M
2. **Check APK has libmpv:** `unzip -l OptiCast-*.apk | grep libmpv`
3. **Check install-over:** New APK versionCode > old, same signature, size ~31M
4. **Check prebuilt exists:** `ls -lh project/native/prebuilt/*.aar` ~25MB

## Emergency Recovery

If build produces Media3-only again:

1. Check `project/native/prebuilt/opticast-mpv-runtime.aar` exists and is 25MB with libmpv
2. If missing, restore from latest working APK:
   ```bash
   python3 -c "
   import zipfile
   apk = '/tmp/v2.6.75/OptiCast-v2.6.75-optimized.apk'
   # Extract .so to AAR
   "
   ```
3. Commit AAR and trigger build with version input

## Version History

- v2.6.72: 30.8M full mpv, libmpv True (last good before cleanup)
- v2.6.73: 5.6M Media3-only, libmpv False (broken due to deleted releases)
- v2.6.74: 30.8M full mpv, libmpv True (fixed with prebuilt AAR from v2.6.72)
- v2.6.75: 30.8M full mpv, libmpv True (bulletproof with permanent prebuilt)
