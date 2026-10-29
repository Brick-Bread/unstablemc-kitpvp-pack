#!/bin/sh
# Regenerates the pack's sounds: src/Sfx.java writes WAVs, ffmpeg (libvorbis) turns them into
# src/sounds/<group>/<name>.ogg, which are committed so build.sh doesn't need ffmpeg.
#
#   FFMPEG=/path/to/ffmpeg ./sounds.sh
set -e
cd "$(dirname "$0")"
JAVA="${JAVA:-$HOME/.local/jdks/jdk-25.0.5+8/bin/java}"
FFMPEG="${FFMPEG:-ffmpeg}"
tmp=$(mktemp -d)
"$JAVA" src/Sfx.java "$tmp"
for f in "$tmp"/*.wav; do
  n=$(basename "$f" .wav); group=${n%%_*}; name=${n#*_}
  mkdir -p "src/sounds/$group"
  "$FFMPEG" -hide_banner -loglevel error -y -i "$f" -map_metadata -1 -fflags +bitexact -c:a libvorbis -q:a 5 -ac 1 "src/sounds/$group/$name.ogg"
done
rm -rf "$tmp"
ls -l src/sounds/*/
