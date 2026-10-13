#!/usr/bin/env bash
# Prepara el submódulo de Xenia (con sus dependencias) y genera los makefiles de Android.
set -euo pipefail
cd "$(dirname "$0")/../third_party/xenia"
git submodule update --init --recursive --depth 1
# Requiere Python 3, ANDROID_NDK_HOME y el NDK 25.x que usa el proyecto de Xenia.
./xb premake --target_os=android
echo "Listo. Abre third_party/xenia/android/android_studio_project en Android Studio."
