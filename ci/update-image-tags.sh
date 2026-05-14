#!/usr/bin/env sh
set -eu

values_file="${1:-charts/edulearn/values.yaml}"
image_tag="${2:?image tag is required}"

if [ ! -f "$values_file" ]; then
  echo "Values file not found: $values_file" >&2
  exit 1
fi

tmp_file="$(mktemp)"
awk -v tag="$image_tag" '
  /^    tag: / && in_images == 1 {
    print "    tag: \"" tag "\""
    next
  }
  /^images:/ {
    in_images = 1
  }
  /^services:/ {
    in_images = 0
  }
  { print }
' "$values_file" > "$tmp_file"

mv "$tmp_file" "$values_file"
