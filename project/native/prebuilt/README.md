# Prebuilt Native Runtime - CRITICAL - DO NOT DELETE

This file `opticast-mpv-runtime.aar` (25MB) contains full mpv runtime with libmpv.so

## Why this exists (bulletproof fix for install-over issue)

**Problem that happened in v2.6.72 -> v2.6.73:**
- Old GitHub releases (v2.6.59) were deleted to keep only latest
- `restore-from-apk.py` hardcoded URL to v2.6.59 APK -> 404 Not Found
- Restore failed silently -> CI built Media3-only APK 5.6M without libmpv
- Media3-only APK cannot install over full mpv APK (same signature, higher versionCode but missing native libs)
- Users saw "new version discoverable but cannot install over 2.6.72"

**Solution - This file:**
- This AAR is COMMITTED to git (not in .cache which is gitignored)
- Contains 24 .so files: libmpv.so 5.9M, libavcodec 11.7M, etc.
- Restore script checks this file FIRST before trying to download
- Even if ALL GitHub releases deleted, build still produces full mpv 31M APK
- Build FAILS explicitly if full mpv not found instead of silent Media3-only

## Rules to prevent future breakage:

1. **NEVER delete this file** - it's the permanent source of truth for CI
2. **NEVER gitignore `project/native/prebuilt/`** - it must be committed
3. **When updating mpv version**, rebuild AAR and commit new version here
4. **CI verification**: release.yml now has step "Verify APK is full mpv" that fails if APK <20MB or missing libmpv
5. **Version handling**: release.yml uses `github.ref_name` fallback when `inputs.version` empty, so tag pushes correctly name assets

## Verification:

```bash
unzip -l project/native/prebuilt/opticast-mpv-runtime.aar | grep libmpv
# Should show: jni/arm64-v8a/libmpv.so, jni/armeabi-v7a/libmpv.so

ls -lh project/native/prebuilt/opticast-mpv-runtime.aar
# Should be ~25MB, not 14KB
```

If this file is missing or small, build will FAIL intentionally to prevent broken Media3-only releases.
