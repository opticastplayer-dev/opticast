#!/usr/bin/env python3
"""
ULTRA BULLETPROOF restore - prebuilt MANDATORY, never depends on GitHub releases.

Priority:
1. project/native/prebuilt/opticast-mpv-runtime.aar (permanent, committed) - MANDATORY
2. .cache/native-runtime/opticast-mpv-runtime.aar (CI cache copy)

NO download fallback by default - if prebuilt missing, FAIL explicitly.
This prevents any future breakage from deleted releases or network issues.

If you need to update mpv, manually build new AAR and commit to prebuilt/.
"""
import sys, zipfile, hashlib, json, shutil
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
CACHE = ROOT / ".cache" / "native-runtime"
PREBUILT_DIR = ROOT / "project" / "native" / "prebuilt"
PREBUILT_AAR = PREBUILT_DIR / "opticast-mpv-runtime.aar"
CACHE_AAR = CACHE / "opticast-mpv-runtime.aar"

# NO download URLs by default - prebuilt is mandatory
# If you absolutely need emergency download, set ALLOW_DOWNLOAD=1 env var
ALLOW_DOWNLOAD = False

def is_full_mpv_aar(p: Path) -> bool:
    if not p.exists():
        return False
    if p.stat().st_size < 10*1024*1024:
        print(f"AAR too small {p}: {p.stat().st_size} < 10MB")
        return False
    try:
        with zipfile.ZipFile(p) as z:
            has_mpv = any('libmpv' in n for n in z.namelist())
            if has_mpv:
                # Count .so files
                so_count = len([n for n in z.namelist() if n.endswith('.so')])
                print(f"AAR verified: {p} {p.stat().st_size/1024/1024:.1f}M, {so_count} .so, has libmpv")
                return True
            else:
                print(f"AAR missing libmpv {p}: {z.namelist()[:5]}")
                return False
    except Exception as e:
        print(f"AAR invalid {p}: {e}")
        return False

def main():
    print("="*60)
    print("ULTRA BULLETPROOF RESTORE - Prebuilt MANDATORY")
    print("="*60)
    print(f"Checking prebuilt: {PREBUILT_AAR}")
    
    # Step 1: Prebuilt MUST exist
    if not PREBUILT_AAR.exists():
        print(f"\nFATAL: Prebuilt AAR missing {PREBUILT_AAR}")
        print("This file is MANDATORY and must be committed to git.")
        print("It prevents install-over breakage from deleted GitHub releases.")
        print("\nTo fix:")
        print("  1. Find latest working APK with libmpv (e.g., v2.6.75-optimized)")
        print("  2. Extract .so files and create AAR")
        print("  3. Commit to project/native/prebuilt/opticast-mpv-runtime.aar")
        print("\nSee project/native/prebuilt/README.md")
        print("="*60)
        sys.exit(1)
    
    if not is_full_mpv_aar(PREBUILT_AAR):
        print(f"\nFATAL: Prebuilt AAR invalid {PREBUILT_AAR}")
        print(f"Size: {PREBUILT_AAR.stat().st_size if PREBUILT_AAR.exists() else 'missing'}")
        print("Must be 25MB+ with libmpv.so")
        sys.exit(1)
    
    # Step 2: Copy to cache
    CACHE.mkdir(parents=True, exist_ok=True)
    if not is_full_mpv_aar(CACHE_AAR) or CACHE_AAR.stat().st_size != PREBUILT_AAR.stat().st_size:
        print(f"Copying prebuilt -> cache: {PREBUILT_AAR} -> {CACHE_AAR}")
        shutil.copy2(PREBUILT_AAR, CACHE_AAR)
    
    if not is_full_mpv_aar(CACHE_AAR):
        print(f"FATAL: Cache AAR invalid after copy {CACHE_AAR}")
        sys.exit(1)
    
    print(f"\nSUCCESS: Using prebuilt full mpv AAR {CACHE_AAR}")
    print(f"Size: {CACHE_AAR.stat().st_size/1024/1024:.1f}M with libmpv")
    
    # Update manifest
    manifest_path = ROOT / "project" / "native" / "runtime-manifest.json"
    try:
        with zipfile.ZipFile(CACHE_AAR) as z:
            so_files = [n for n in z.namelist() if n.endswith('.so')]
        manifest = {
            "controlled_source_build": False,
            "restored_from_apk": False,
            "from_prebuilt": True,
            "source_aar": str(PREBUILT_AAR),
            "version": "2.6.75-bulletproof",
            "runtime_sha256": hashlib.sha256(CACHE_AAR.read_bytes()).hexdigest(),
            "libraries": {n: "prebuilt" for n in so_files},
            "toolchain": {"note": "ultra bulletproof - prebuilt mandatory, never depends on releases"},
            "features": {"renderer": "OpenGL ES", "mpv": True, "full_mpv": True}
        }
        manifest_path.write_text(json.dumps(manifest, indent=2) + "\n")
        print(f"Updated manifest: {manifest_path}")
    except Exception as e:
        print(f"Warning: Could not update manifest: {e}")
    
    print("="*60)
    sys.exit(0)

if __name__ == "__main__":
    try:
        main()
    except SystemExit:
        raise
    except Exception as e:
        print(f"ERROR: {e}", file=sys.stderr)
        import traceback; traceback.print_exc()
        sys.exit(1)
