#!/usr/bin/env bash
# Controlled source rebuild; never consumes the published binary AAR.
set -euo pipefail
ROOT=$(cd "$(dirname "$0")/../.." && pwd)
WORK="$ROOT/.cache/native-build"
ARCH=${1:-arm64}
if [ "${NATIVE_BUNDLED_SOURCES:-0}" = 1 ]; then
    python3 "$ROOT/project/native/verify-bundled-sources.py"
else
    python3 "$ROOT/project/native/fetch-sources.py"
fi
cp -r "$ROOT/project/native/buildscripts/include" "$ROOT/project/native/buildscripts/scripts" "$ROOT/project/native/buildscripts/buildall.sh" "$WORK/"
chmod +x "$WORK"/*.sh "$WORK"/scripts/*.sh
mkdir -p "$WORK/bin"
cat > "$WORK/bin/meson" <<MESON
#!/bin/sh
PYTHONPATH="$ROOT/.cache/native-python" exec python3 -m mesonbuild.mesonmain "\$@"
MESON
chmod +x "$WORK/bin/meson"
export PATH="$WORK/bin:$PATH"
export PYTHONPATH="$ROOT/.cache/native-python${PYTHONPATH:+:$PYTHONPATH}"
export cores=${NATIVE_JOBS:-2}
cd "$WORK"
bash -e buildall.sh --arch "$ARCH" mpv
