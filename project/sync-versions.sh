#!/bin/bash
# Auto-sync versions across all files to prevent drift
set -e
VC=$(grep -oP 'versionCode\s*=\s*\K\d+' project/app/build.gradle.kts | head -1)
VN=$(grep -oP 'versionName\s*=\s*"\K[^"]+' project/app/build.gradle.kts | head -1)
echo "Syncing to versionCode=$VC versionName=$VN"

# README
sed -i "s/Version [0-9.]* —/Version $VN —/" README.md
sed -i "s/v[0-9.]*-optimized/v$VN-optimized/g" README.md
sed -i "s/build [0-9]*/build $VC/" README.md

# docs/index.html
sed -i "s/Version [0-9.]* —/Version $VN —/" docs/index.html
sed -i "s/Version [0-9.]*</Version $VN</" docs/index.html
sed -i "s/v[0-9.]*-optimized/v$VN-optimized/g" docs/index.html

# baseline header
sed -i "1s/v[0-9.]* (versionCode [0-9]*)/v$VN (versionCode $VC)/" project/app/src/main/baseline-prof.txt
sed -i "1s/versionCode [0-9]*/versionCode $VC/" project/app/src/main/baseline-prof.txt

echo "Sync done: $VN ($VC)"
