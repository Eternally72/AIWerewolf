#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SRC_DIR="$ROOT_DIR/public/assets"
OUT_DIR="$SRC_DIR/optimized"

if ! command -v cwebp >/dev/null 2>&1; then
  echo "cwebp is required. Install webp tools first, then rerun this script." >&2
  exit 1
fi

mkdir -p "$OUT_DIR/backgrounds" "$OUT_DIR/roles" "$OUT_DIR/avatars"

for file in "$SRC_DIR"/backgrounds/*.png; do
  name="$(basename "${file%.png}")"
  cwebp -quiet -q 82 "$file" -o "$OUT_DIR/backgrounds/$name.webp"
done

for file in "$SRC_DIR"/roles/*.png; do
  name="$(basename "${file%.png}")"
  cwebp -quiet -resize 256 256 -q 82 "$file" -o "$OUT_DIR/roles/$name.webp"
done

for file in "$SRC_DIR"/avatars/*.png; do
  name="$(basename "${file%.png}")"
  cwebp -quiet -resize 192 192 -q 82 "$file" -o "$OUT_DIR/avatars/$name.webp"
done

echo "Optimized assets written to $OUT_DIR"
