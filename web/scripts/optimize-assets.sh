#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SRC_DIR="$ROOT_DIR/public/assets"
OUT_DIR="$SRC_DIR/optimized"
WEBP_QUALITY="${WEBP_QUALITY:-82}"
AVIF_QUALITY="${AVIF_QUALITY:-45}"

if ! command -v cwebp >/dev/null 2>&1; then
  cat >&2 <<'EOF'
cwebp is required, but it is not installed.

This is a system image tool, not a project npm dependency. On Ubuntu/WSL install it with:

  sudo apt update
  sudo apt install -y webp

Optional AVIF output:

  sudo apt install -y libavif-bin

Then rerun:

  cd web
  bash scripts/optimize-assets.sh
EOF
  exit 1
fi

mkdir -p "$OUT_DIR/backgrounds" "$OUT_DIR/roles" "$OUT_DIR/avatars"

write_avif() {
  local input="$1"
  local output="$2"
  if command -v avifenc >/dev/null 2>&1; then
    avifenc --min 24 --max "$AVIF_QUALITY" --speed 6 "$input" "$output" >/dev/null
    return
  fi
  if command -v magick >/dev/null 2>&1; then
    magick "$input" -quality "$AVIF_QUALITY" "$output"
    return
  fi
  return 1
}

optimize_png() {
  local input="$1"
  local output_base="$2"
  local resize_args=("${@:3}")
  cwebp -quiet "${resize_args[@]}" -q "$WEBP_QUALITY" "$input" -o "$output_base.webp"
  if write_avif "$input" "$output_base.avif"; then
    echo "Generated $(basename "$output_base").avif"
  fi
}

for file in "$SRC_DIR"/backgrounds/*.png; do
  name="$(basename "${file%.png}")"
  optimize_png "$file" "$OUT_DIR/backgrounds/$name"
done

for file in "$SRC_DIR"/roles/*.png; do
  name="$(basename "${file%.png}")"
  optimize_png "$file" "$OUT_DIR/roles/$name" -resize 256 256
done

for file in "$SRC_DIR"/avatars/*.png; do
  name="$(basename "${file%.png}")"
  optimize_png "$file" "$OUT_DIR/avatars/$name" -resize 192 192
done

echo "Optimized assets written to $OUT_DIR"
echo "Use VITE_ASSET_VARIANT=optimized-webp or optimized-avif to enable compressed assets."
