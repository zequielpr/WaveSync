#!/usr/bin/env bash
set -euo pipefail

# =========================
# CONFIG (edit these only)
# =========================
ANDROID_NDK_HOME_WIN="C:/Users/Zequi/AppData/Local/Android/Sdk/ndk/29.0.14206865"
ANDROID_API_LEVEL=21

# ✅ Only the ABIs you actually need on real devices:
ABIS=("arm64-v8a" "armeabi-v7a")
# If you ONLY care about modern devices, use:
# ABIS=("arm64-v8a")

# =========================

to_posix_abs() {
  local p="$1"
  if command -v cygpath >/dev/null 2>&1; then
    cygpath -u "$p"
  else
    echo "$p" | sed -E 's#^([A-Za-z]):#/\L\1#'
  fi
}

ANDROID_NDK_HOME="$(to_posix_abs "$ANDROID_NDK_HOME_WIN")"

if [ ! -d "$ANDROID_NDK_HOME/toolchains/llvm" ]; then
  echo "ERROR: NDK root not found at: $ANDROID_NDK_HOME"
  exit 1
fi

HOST_TAG="windows-x86_64"
TOOLCHAIN="$ANDROID_NDK_HOME/toolchains/llvm/prebuilt/$HOST_TAG"
SYSROOT="$TOOLCHAIN/sysroot"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
OUT_DIR="$SCRIPT_DIR/jniLibs"

export PATH="$TOOLCHAIN/bin:$PATH"
NPROC="${NUMBER_OF_PROCESSORS:-4}"

rm -rf "$OUT_DIR"
mkdir -p "$OUT_DIR"

echo "NDK:    $ANDROID_NDK_HOME"
echo "OUT:    $OUT_DIR"
echo "API:    $ANDROID_API_LEVEL"
echo "ABIS:   ${ABIS[*]}"
echo "========================================"

for ABI in "${ABIS[@]}"; do
  echo ">>> Building ABI: $ABI"

  TARGET_TRIPLE=""
  HOST_TRIPLE=""

  case "$ABI" in
    "arm64-v8a")
      TARGET_TRIPLE="aarch64-linux-android"
      HOST_TRIPLE="aarch64-linux-android"
      ;;
    "armeabi-v7a")
      TARGET_TRIPLE="armv7a-linux-androideabi"
      HOST_TRIPLE="arm-linux-androideabi"
      ;;
    *)
      echo "Unsupported ABI: $ABI"
      exit 1
      ;;
  esac

  CC="$TOOLCHAIN/bin/${TARGET_TRIPLE}${ANDROID_API_LEVEL}-clang"
  CXX="$TOOLCHAIN/bin/${TARGET_TRIPLE}${ANDROID_API_LEVEL}-clang++"
  AR="$TOOLCHAIN/bin/llvm-ar"
  RANLIB="$TOOLCHAIN/bin/llvm-ranlib"
  STRIP="$TOOLCHAIN/bin/llvm-strip"

  BUILD_DIR="$SCRIPT_DIR/android-build-$ABI"
  INSTALL_DIR="$BUILD_DIR/install"

  rm -rf "$BUILD_DIR"
  mkdir -p "$BUILD_DIR"

  pushd "$BUILD_DIR" >/dev/null

  export CC CXX AR RANLIB STRIP
  export CPPFLAGS="--sysroot=$SYSROOT"
  export CFLAGS="--sysroot=$SYSROOT -fPIC"
  export CXXFLAGS="--sysroot=$SYSROOT -fPIC"
  export LDFLAGS="--sysroot=$SYSROOT"

  "$SCRIPT_DIR/configure" \
    --prefix="$INSTALL_DIR" \
    --host="$HOST_TRIPLE" \
    --with-sysroot="$SYSROOT" \
    --enable-shared=yes \
    --enable-static=no \
    --disable-doc \
    --disable-extra-programs

  make -j"$NPROC"
  make install

  popd >/dev/null

  ABI_OUT="$OUT_DIR/$ABI"
  mkdir -p "$ABI_OUT"

  if [ ! -f "$INSTALL_DIR/lib/libopus.so" ]; then
    echo "ERROR: libopus.so not produced for $ABI"
    echo "Look inside: $BUILD_DIR/config.log"
    exit 1
  fi

  cp -f "$INSTALL_DIR/lib/libopus.so" "$ABI_OUT/libopus.so"
  "$STRIP" --strip-unneeded "$ABI_OUT/libopus.so" || true

  echo "✓ Output: $ABI_OUT/libopus.so"
  echo "----------------------------------------"
done

echo "DONE. Libraries in: $OUT_DIR"
