#!/usr/bin/env bash
# Run only after the controlled source build succeeds.
set -euo pipefail
ROOT=$(cd "$(dirname "$0")/../.." && pwd)
WORK="$ROOT/.cache/native-build"
ARCH=${1:-arm64}
case "$ARCH" in
 arm64) ABI=arm64-v8a; TRIPLE=aarch64-linux-android ;;
 armv7l) ABI=armeabi-v7a; TRIPLE=armv7a-linux-androideabi ;;
 *) echo 'Unsupported packaging ABI'; exit 1 ;;
esac
NDK="$WORK/sdk/android-ndk-r28c"
BIN="$NDK/toolchains/llvm/prebuilt/linux-x86_64/bin"
PREFIX="$WORK/prefix/$ARCH"
STAGE="$WORK/runtime/jni/$ABI"
mkdir -p "$STAGE"
test -f "$PREFIX/lib/libmpv.so"
cp "$PREFIX"/lib/*.so "$STAGE/"
cp "$NDK/toolchains/llvm/prebuilt/linux-x86_64/sysroot/usr/lib/$( [ "$ARCH" = arm64 ] && echo aarch64-linux-android || echo arm-linux-androideabi )/libc++_shared.so" "$STAGE/"
"$BIN/${TRIPLE}26-clang" -shared -fPIC -O2 -Wl,-z,max-page-size=16384 -Wl,--no-undefined \
 -I"$PREFIX/include" -L"$PREFIX/lib" "$ROOT/project/native/jni/opticast_mpv.c" \
 -Wl,-soname,libopticast_mpv.so -lmpv -lavcodec -o "$STAGE/libopticast_mpv.so"
for lib in "$STAGE"/*.so; do "$BIN/llvm-strip" --strip-unneeded "$lib"; done
mkdir -p "$ROOT/.cache/native-runtime"
python3 - "$ROOT" "$WORK" <<'PY'
from pathlib import Path
import sys,zipfile,hashlib,json
root,work=map(Path,sys.argv[1:])
artifact=root/'.cache/native-runtime/opticast-mpv-runtime.aar'
with zipfile.ZipFile(artifact,'w',zipfile.ZIP_DEFLATED,compresslevel=9) as z:
 z.writestr('AndroidManifest.xml','<manifest xmlns:android="http://schemas.android.com/apk/res/android" package="com.opticast.nativeengine"><uses-sdk android:minSdkVersion="26"/></manifest>')
 for p in sorted((work/'runtime').rglob('*.so')):z.write(p,str(p.relative_to(work/'runtime')))
manifest={'controlled_source_build':True,'source_lock_sha256':hashlib.sha256((root/'project/native/sources.lock.json').read_bytes()).hexdigest(),
 'runtime_sha256':hashlib.sha256(artifact.read_bytes()).hexdigest(),
 'jni_source_sha256':hashlib.sha256((root/'project/native/jni/opticast_mpv.c').read_bytes()).hexdigest(),
 'build_script_sha256':{str(p.relative_to(root/'project/native')):hashlib.sha256(p.read_bytes()).hexdigest() for p in sorted((root/'project/native/buildscripts').rglob('*.sh'))},
 'libraries':{str(p.relative_to(work/'runtime')):hashlib.sha256(p.read_bytes()).hexdigest() for p in sorted((work/'runtime').rglob('*.so'))},
 'toolchain':{'ndk':'r28c / 28.2.13676358','api':26,'meson':'1.9.2 for initial dependencies; 1.11.0 for fontconfig/mpv; rebuild with 1.11.0','native_jobs':2},
 'features':{'renderer':'OpenGL ES','vulkan':False,'lua':False,'hardware_preference':'mediacodec,mediacodec-copy,no'}}
(root/'project/native/runtime-manifest.json').write_text(json.dumps(manifest,indent=2)+'\n')
print('Packaged controlled runtime:',artifact,artifact.stat().st_size)
PY
