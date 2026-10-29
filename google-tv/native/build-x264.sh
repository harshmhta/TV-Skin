#!/usr/bin/env bash
set -euo pipefail
TASK_ROOT=$(cd "$(dirname "$0")" && pwd)
TASK_SDK=${ANDROID_HOME:-"$HOME/Library/Android/sdk"}
TASK_NDK="$TASK_SDK/ndk/27.1.12297006"
TASK_TOOLCHAIN="$TASK_NDK/toolchains/llvm/prebuilt/darwin-x86_64"
TASK_REV=$(cat "$TASK_ROOT/x264-version.txt")
TASK_BUILD="$TASK_ROOT/build-x264"
mkdir -p "$TASK_BUILD" "$TASK_ROOT/armeabi-v7a"
if [[ ! -f "$TASK_BUILD/source/configure" ]]; then
    curl -fsSL "https://codeload.github.com/mirror/x264/tar.gz/$TASK_REV" -o "$TASK_BUILD/source.tar.gz"
    mkdir -p "$TASK_BUILD/source"
    tar -xzf "$TASK_BUILD/source.tar.gz" -C "$TASK_BUILD/source" --strip-components=1
fi
cd "$TASK_BUILD/source"
CC="$TASK_TOOLCHAIN/bin/armv7a-linux-androideabi26-clang" AR="$TASK_TOOLCHAIN/bin/llvm-ar" RANLIB="$TASK_TOOLCHAIN/bin/llvm-ranlib" STRIP="$TASK_TOOLCHAIN/bin/llvm-strip" CFLAGS="-O3 -mfpu=neon" ./configure --host=arm-linux --enable-static --enable-pic --disable-cli --disable-opencl --bit-depth=8 --chroma-format=420 --prefix="$TASK_ROOT/armeabi-v7a" > "$TASK_BUILD/configure.log"
make -j8 > "$TASK_BUILD/make.log" 2>&1
make install-lib-static >> "$TASK_BUILD/make.log" 2>&1
mkdir -p "$TASK_ROOT/../app/src/main/assets/licenses"
cp COPYING "$TASK_ROOT/../app/src/main/assets/licenses/x264-COPYING.txt"
printf 'Built x264 %s for armeabi-v7a\n' "$TASK_REV"
