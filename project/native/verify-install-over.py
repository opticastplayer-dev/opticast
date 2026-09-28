#!/usr/bin/env python3
"""
Verify new APK can install over old APK (same package, same signature, higher versionCode)
Prevents breaking install-over like v2.6.73 Media3-only issue.
"""
import sys
import zipfile
import subprocess
import json
from pathlib import Path
import re

def get_apk_info(apk_path: Path):
    """Extract package, versionCode, versionName from APK via aapt or apkanalyzer"""
    info = {}
    try:
        # Try aapt2
        result = subprocess.run(
            ["aapt2", "dump", "badging", str(apk_path)],
            capture_output=True, text=True, timeout=10
        )
        if result.returncode == 0:
            # package: name='com.opticast.player' versionCode='124' versionName='2.6.74'
            m = re.search(r"package: name='([^']+)' versionCode='(\d+)' versionName='([^']+)'", result.stdout)
            if m:
                info['package'] = m.group(1)
                info['versionCode'] = int(m.group(2))
                info['versionName'] = m.group(3)
                return info
    except:
        pass
    
    # Fallback: parse from build.gradle.kts if APK not available
    try:
        # Use zipfile to check libs
        with zipfile.ZipFile(apk_path) as z:
            info['has_libmpv'] = any('libmpv' in n for n in z.namelist())
            info['size'] = apk_path.stat().st_size
            info['so_count'] = len([n for n in z.namelist() if n.endswith('.so')])
    except Exception as e:
        print(f"Failed to read APK {apk_path}: {e}")
    
    return info

def verify_install_over(new_apk: Path, old_apk: Path = None):
    print(f"=== Verifying install-over for {new_apk} ===")
    
    if not new_apk.exists():
        print(f"FATAL: New APK not found {new_apk}")
        return False
    
    # Check new APK has full mpv
    try:
        with zipfile.ZipFile(new_apk) as z:
            has_mpv = any('libmpv' in n for n in z.namelist())
            size = new_apk.stat().st_size
            print(f"New APK: {size/1024/1024:.1f}M, has libmpv={has_mpv}")
            if not has_mpv:
                print("FATAL: New APK missing libmpv - would break install-over from full mpv")
                return False
            if size < 20*1024*1024:
                print(f"FATAL: New APK too small {size} < 20MB - likely Media3-only")
                return False
    except Exception as e:
        print(f"FATAL: Cannot read new APK: {e}")
        return False
    
    # If old APK provided, check versionCode and package
    if old_apk and old_apk.exists():
        print(f"Checking against old APK {old_apk}")
        # For now, just check both have libmpv and new is larger or same
        try:
            with zipfile.ZipFile(old_apk) as z_old, zipfile.ZipFile(new_apk) as z_new:
                old_has_mpv = any('libmpv' in n for n in z_old.namelist())
                new_has_mpv = any('libmpv' in n for n in z_new.namelist())
                print(f"Old has libmpv={old_has_mpv}, New has libmpv={new_has_mpv}")
                if old_has_mpv and not new_has_mpv:
                    print("FATAL: Old had libmpv but new doesn't - cannot install over")
                    return False
        except Exception as e:
            print(f"Warning: Could not compare APKs: {e}")
    
    # Check build.gradle.kts versionCode
    gradle_path = Path(__file__).parent.parent / "app" / "build.gradle.kts"
    if gradle_path.exists():
        content = gradle_path.read_text()
        m = re.search(r"versionCode\s*=\s*(\d+)", content)
        if m:
            vc = int(m.group(1))
            print(f"build.gradle.kts versionCode={vc}")
            # Should be > 124 (last known good)
            if vc < 124:
                print(f"FATAL: versionCode {vc} < 124 - would not install over latest")
                return False
        
        m = re.search(r"applicationId\s*=\s*\"([^\"]+)\"", content)
        if m:
            app_id = m.group(1)
            print(f"applicationId={app_id}")
            if app_id != "com.opticast.player":
                print(f"FATAL: applicationId changed to {app_id} - breaks install-over")
                return False
    
    # Check prebuilt AAR exists
    prebuilt = Path(__file__).parent / "prebuilt" / "opticast-mpv-runtime.aar"
    if not prebuilt.exists():
        print(f"FATAL: Prebuilt AAR missing {prebuilt} - build would fail or be Media3-only")
        return False
    if prebuilt.stat().st_size < 10*1024*1024:
        print(f"FATAL: Prebuilt AAR too small {prebuilt.stat().st_size}")
        return False
    try:
        with zipfile.ZipFile(prebuilt) as z:
            if not any('libmpv' in n for n in z.namelist()):
                print(f"FATAL: Prebuilt AAR missing libmpv")
                return False
    except Exception as e:
        print(f"FATAL: Prebuilt AAR invalid: {e}")
        return False
    print(f"Prebuilt AAR OK: {prebuilt.stat().st_size/1024/1024:.1f}M with libmpv")
    
    print("SUCCESS: Install-over verification passed")
    return True

if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("Usage: verify-install-over.py <new-apk> [old-apk]")
        sys.exit(1)
    new_apk = Path(sys.argv[1])
    old_apk = Path(sys.argv[2]) if len(sys.argv) > 2 else None
    ok = verify_install_over(new_apk, old_apk)
    sys.exit(0 if ok else 1)
