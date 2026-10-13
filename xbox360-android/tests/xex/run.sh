#!/usr/bin/env bash
# Prueba del lector de XEX en el host: genera XEX sintéticos (sin/cifrado x sin/basic/LZX),
# compila con AddressSanitizer y comprueba título, ID e icono. Requiere g++, gcc y python3 + cryptography.
set -euo pipefail
here="$(cd "$(dirname "$0")" && pwd)"; root="$here/../.."
X="$root/third_party/xenia"; SRC="$root/app/src/main/cpp"
tmp="$(mktemp -d)"; cd "$tmp"
python3 "$here/gen_xex.py"
gcc -c -w -g -fsanitize=address -DHAVE_CONFIG_H -I"$X/third_party/mspack" "$X/third_party/mspack/lzxd.c" -o lzxd.o
gcc -c -w -g -fsanitize=address "$X/third_party/crypto/rijndael-alg-fst.c" -o rij.o
g++ -std=c++17 -Wall -g -fsanitize=address -I"$SRC" -I"$X" -I"$X/third_party/mspack" \
  "$here/main.cpp" "$SRC/xex_info.cpp" lzxd.o rij.o -o t
./t *.xex
