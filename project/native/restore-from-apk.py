#!/usr/bin/env python3
"""
Restore native runtime AAR from existing APK for CI full release with mpv.
Downloads 2.6.59 APK if needed, extracts .so files, repacks as AAR.
"""
import sys
import zipfile
import hashlib
import json
from pathlib import Path
import urllib.request
import os

ROOT = Path(__file__).resolve().parents[2]
CACHE = ROOT / ".cache" / "native-runtime"
# Try multiple URLs - latest first, fallback to older - fixed after deleting old releases
APK_URLS = [
    "https://github.com/opticastplayer-dev/opticast/releases/download/v2.6.73-optimized/OptiCast-v2.6.73-optimized.apk",
    "https://github.com/opticastplayer-dev/opticast/releases/download/v2.6.73-optimized/OptiCast-v2.6.60.apk",  # Misnamed but contains mpv
    "https://github.com/opticastplayer-dev/opticast/releases/download/v2.6.72-optimized/OptiCast-v2.6.72-optimized.apk",
    "https://github.com/opticastplayer-dev/opticast/releases/download/v2.6.72-optimized/OptiCast-v2.6.60.apk",
    "https://github.com/opticastplayer-dev/opticast/releases/download/v2.6.59/OptiCast-2.6.59.apk",
    "https://github.com/opticastplayer-dev/opticast/releases/download/v2.6.59/OptiCast-v2.6.59.apk",
]
APK_PATH = ROOT / ".cache" / "OptiCast-restore.apk"

def download_apk():
    if APK_PATH.exists() and APK_PATH.stat().st_size > 10*1024*1024:
        print(f"APK already exists: {APK_PATH} {APK_PATH.stat().st_size}")
        return APK_PATH
    CACHE.parent.mkdir(parents=True, exist_ok=True)
    APK_PATH.parent.mkdir(parents=True, exist_ok=True)
    for apk_url in APK_URLS:
        try:
            print(f"Trying download {apk_url} -> {APK_PATH}")
            req = urllib.request.Request(apk_url, headers={'User-Agent': 'OptiCast-restore'})
            with urllib.request.urlopen(req, timeout=120) as r, open(APK_PATH, 'wb') as f:
                while True:
                    chunk = r.read(8192)
                    if not chunk:
                        break
                    f.write(chunk)
            if APK_PATH.stat().st_size > 10*1024*1024:
                print(f"Downloaded {APK_PATH.stat().st_size} bytes from {apk_url}")
                return APK_PATH
            else:
                print(f"Downloaded file too small from {apk_url}, trying next")
                APK_PATH.unlink(missing_ok=True)
        except Exception as e:
            print(f"Failed to download {apk_url}: {e}, trying next")
            APK_PATH.unlink(missing_ok=True)
            continue
    print(f"All download attempts failed, trying to find existing APK in workspace")
    # Try to find any existing APK with mpv in workspace or cache
    for search_path in [ROOT / ".cache", ROOT, Path("/tmp")]:
        if search_path.exists():
            for apk_file in search_path.rglob("*.apk"):
                try:
                    if apk_file.stat().st_size > 10*1024*1024:
                        with zipfile.ZipFile(apk_file, 'r') as z:
                            if any('libmpv' in n for n in z.namelist()):
                                print(f"Found existing APK with mpv: {apk_file}")
                                return apk_file
                except:
                    continue
    raise Exception("Failed to download or find APK with mpv for restore")

def extract_so_to_aar(apk_path: Path):
    CACHE.mkdir(parents=True, exist_ok=True)
    aar_path = CACHE / "opticast-mpv-runtime.aar"
    
    # Extract .so files from APK
    with zipfile.ZipFile(apk_path, 'r') as apk_zip:
        so_files = [n for n in apk_zip.namelist() if n.startswith('lib/') and n.endswith('.so')]
        print(f"Found {len(so_files)} .so files in APK: {so_files[:10]}")
        if not so_files:
            print("No .so files found in APK, cannot restore")
            return None
        
        # Create AAR (which is just a zip with jni/ folder)
        with zipfile.ZipFile(aar_path, 'w', zipfile.ZIP_DEFLATED, compresslevel=9) as aar_zip:
            # AndroidManifest.xml required for AAR
            aar_zip.writestr('AndroidManifest.xml', '<manifest xmlns:android="http://schemas.android.com/apk/res/android" package="com.opticast.nativeengine"><uses-sdk android:minSdkVersion="26"/></manifest>')
            # Copy .so files to jni/ structure
            for so_name in so_files:
                # so_name like lib/arm64-v8a/libmpv.so -> jni/arm64-v8a/libmpv.so
                jni_name = so_name.replace('lib/', 'jni/')
                data = apk_zip.read(so_name)
                aar_zip.writestr(jni_name, data)
                print(f"  {so_name} -> {jni_name} {len(data)} bytes")
    
    print(f"Created AAR: {aar_path} {aar_path.stat().st_size} bytes")
    
    # Create minimal runtime-manifest if missing
    manifest_path = ROOT / "project" / "native" / "runtime-manifest.json"
    if not manifest_path.exists():
        manifest = {
            "controlled_source_build": False,
            "restored_from_apk": True,
            "source_apk": str(apk_path),
            "runtime_sha256": hashlib.sha256(aar_path.read_bytes()).hexdigest(),
            "libraries": {n: "restored" for n in so_files},
            "toolchain": {"note": "restored from latest APK for CI - fixed after deleting old releases"},
            "features": {"renderer": "OpenGL ES"}
        }
        manifest_path.write_text(json.dumps(manifest, indent=2) + "\n")
        print(f"Created manifest: {manifest_path}")
    
    return aar_path

if __name__ == "__main__":
    try:
        apk = download_apk()
        aar = extract_so_to_aar(apk)
        if aar and aar.exists():
            print(f"SUCCESS: Restored {aar}")
            sys.exit(0)
        else:
            print("FAILED to restore AAR")
            sys.exit(1)
    except Exception as e:
        print(f"ERROR: {e}", file=sys.stderr)
        import traceback
        traceback.print_exc()
        sys.exit(1)
