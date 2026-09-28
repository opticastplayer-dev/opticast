#!/usr/bin/env python3
"""
Bulletproof restore - never depends on deleted releases.
Priority:
1. project/native/prebuilt/opticast-mpv-runtime.aar (permanent, committed)
2. .cache/native-runtime/opticast-mpv-runtime.aar
3. Download latest v2.6.74-optimized
Fail if full mpv not found instead of silent Media3-only.
"""
import sys, zipfile, hashlib, json, urllib.request, shutil
from pathlib import Path
ROOT = Path(__file__).resolve().parents[2]
CACHE = ROOT / ".cache" / "native-runtime"
PREBUILT_DIR = ROOT / "project" / "native" / "prebuilt"
PREBUILT_AAR = PREBUILT_DIR / "opticast-mpv-runtime.aar"
CACHE_AAR = CACHE / "opticast-mpv-runtime.aar"
APK_URLS = [
    "https://github.com/opticastplayer-dev/opticast/releases/download/v2.6.74-optimized/OptiCast-v2.6.74-optimized.apk",
    "https://github.com/opticastplayer-dev/opticast/releases/download/v2.6.73-optimized/OptiCast-v2.6.73-optimized.apk",
    "https://github.com/opticastplayer-dev/opticast/releases/download/v2.6.73-optimized/OptiCast-v2.6.60.apk",
    "https://github.com/opticastplayer-dev/opticast/releases/download/v2.6.72-optimized/OptiCast-v2.6.72-optimized.apk",
]
APK_PATH = ROOT / ".cache" / "OptiCast-restore.apk"

def is_full_mpv_aar(p: Path) -> bool:
    if not p.exists() or p.stat().st_size < 10*1024*1024:
        return False
    try:
        with zipfile.ZipFile(p) as z:
            return any('libmpv' in n for n in z.namelist())
    except:
        return False

def ensure_cache():
    if is_full_mpv_aar(PREBUILT_AAR):
        CACHE.mkdir(parents=True, exist_ok=True)
        if not is_full_mpv_aar(CACHE_AAR):
            print(f"Copy prebuilt -> cache")
            shutil.copy2(PREBUILT_AAR, CACHE_AAR)
        return True
    return False

def download_apk():
    print("=== Checking prebuilt ===")
    if ensure_cache() and is_full_mpv_aar(CACHE_AAR):
        print(f"Using prebuilt {CACHE_AAR}")
        return None
    print("=== Checking cache ===")
    if is_full_mpv_aar(CACHE_AAR):
        print(f"Cache exists {CACHE_AAR}")
        return None
    print("=== Downloading ===")
    CACHE.parent.mkdir(parents=True, exist_ok=True)
    for url in APK_URLS:
        try:
            print(f"Trying {url}")
            req = urllib.request.Request(url, headers={'User-Agent':'OptiCast'})
            with urllib.request.urlopen(req, timeout=120) as r, open(APK_PATH,'wb') as f:
                while True:
                    c=r.read(8192)
                    if not c: break
                    f.write(c)
            if APK_PATH.stat().st_size < 5*1024*1024:
                APK_PATH.unlink(missing_ok=True)
                continue
            with zipfile.ZipFile(APK_PATH) as z:
                if any('libmpv' in n for n in z.namelist()):
                    print(f"Downloaded valid {url}")
                    return APK_PATH
                else:
                    print(f"No libmpv in {url}")
                    APK_PATH.unlink(missing_ok=True)
        except Exception as e:
            print(f"Failed {url}: {e}")
            APK_PATH.unlink(missing_ok=True)
    for sp in [ROOT/".cache", ROOT, Path("/tmp")]:
        if sp.exists():
            for apk in sp.rglob("*.apk"):
                try:
                    if apk.stat().st_size>10*1024*1024:
                        with zipfile.ZipFile(apk) as z:
                            if any('libmpv' in n for n in z.namelist()):
                                print(f"Found {apk}")
                                return apk
                except: pass
    raise Exception("FATAL: No full mpv AAR/APK - refusing Media3-only")

def extract(apk: Path):
    CACHE.mkdir(parents=True, exist_ok=True)
    aar=CACHE_AAR
    with zipfile.ZipFile(apk) as az:
        sos=[n for n in az.namelist() if n.startswith('lib/') and n.endswith('.so')]
        if not any('libmpv' in n for n in sos):
            raise Exception("APK no libmpv")
        with zipfile.ZipFile(aar,'w',zipfile.ZIP_DEFLATED,compresslevel=9) as z:
            z.writestr('AndroidManifest.xml','<manifest xmlns:android="http://schemas.android.com/apk/res/android" package="com.opticast.nativeengine"><uses-sdk android:minSdkVersion="26"/></manifest>')
            for s in sos:
                z.writestr(s.replace('lib/','jni/'), az.read(s))
    print(f"Created {aar} {aar.stat().st_size}")
    try:
        PREBUILT_DIR.mkdir(parents=True, exist_ok=True)
        shutil.copy2(aar, PREBUILT_AAR)
    except: pass
    mp=ROOT/"project"/"native"/"runtime-manifest.json"
    import hashlib, json
    m={"restored_from_apk":True,"source_apk":str(apk),"version":"2.6.74","runtime_sha256":hashlib.sha256(aar.read_bytes()).hexdigest(),"features":{"mpv":True,"full_mpv":True}}
    mp.write_text(json.dumps(m,indent=2))
    return aar

if __name__=="__main__":
    try:
        apk=download_apk()
        if apk is None:
            assert is_full_mpv_aar(CACHE_AAR)
            print(f"SUCCESS {CACHE_AAR}")
            sys.exit(0)
        aar=extract(apk)
        assert is_full_mpv_aar(aar)
        print(f"SUCCESS {aar}")
    except Exception as e:
        print(f"ERROR {e}",file=sys.stderr)
        import traceback; traceback.print_exc()
        sys.exit(1)
