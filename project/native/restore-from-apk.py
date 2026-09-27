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
APK_URL = "https://github.com/opticastplayer-dev/opticast/releases/download/v2.6.59/OptiCast-2.6.59.apk"
APK_PATH = ROOT / ".cache" / "OptiCast-2.6.59.apk"

def download_apk():
    if APK_PATH.exists() and APK_PATH.stat().st_size > 10*1024*1024:
        print(f"APK already exists: {APK_PATH} {APK_PATH.stat().st_size}")
        return APK_PATH
    print(f"Downloading {APK_URL} -> {APK_PATH}")
    CACHE.parent.mkdir(parents=True, exist_ok=True)
    APK_PATH.parent.mkdir(parents=True, exist_ok=True)
    req = urllib.request.Request(APK_URL, headers={'User-Agent': 'OptiCast-restore'})
    with urllib.request.urlopen(req, timeout=120) as r, open(APK_PATH, 'wb') as f:
        while True:
            chunk = r.read(8192)
            if not chunk:
                break
            f.write(chunk)
    print(f"Downloaded {APK_PATH.stat().st_size} bytes")
    return APK_PATH

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
            "toolchain": {"note": "restored from 2.6.59 APK for CI"},
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
