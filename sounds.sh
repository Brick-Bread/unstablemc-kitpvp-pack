#!/bin/sh
# Mixes the pack's sounds from the CC0 files in src/sounds-src (see CREDITS.md there) into
# src/sounds/<group>/<name>.ogg (mono, so Minecraft plays them positionally). The results are
# committed, so build.sh doesn't need ffmpeg.
#
#   FFMPEG=/path/to/ffmpeg ./sounds.sh
set -e
cd "$(dirname "$0")"
FFMPEG="${FFMPEG:-ffmpeg}"
S=src/sounds-src
mkdir -p src/sounds/strike src/sounds/drop
OUT="-map_metadata -1 -fflags +bitexact -ac 1 -ar 44100 -c:a libvorbis -q:a 6"
NORM="loudnorm=I=-12:TP=-1:LRA=11"
mix() { "$FFMPEG" -hide_banner -loglevel error -y "$@"; }

mix -i $S/spaceEngineLarge_000.ogg -i $S/forceField_003.ogg -filter_complex \
  "[0:a]atrim=0:2.4,asetpts=N/SR/TB,afade=t=in:d=2.1:curve=exp,afade=t=out:st=2.25:d=0.15[e];[1:a]adelay=1450|1450,volume=0.8[f];[e][f]amix=inputs=2:normalize=0:duration=first,$NORM" \
  $OUT src/sounds/strike/charge.ogg
mix -i $S/oga-aquinn-sirens.mp3 -filter_complex \
  "[0:a]atrim=26.2:28.8,asetpts=N/SR/TB,afade=t=in:d=0.05,afade=t=out:st=2.2:d=0.4,$NORM" $OUT src/sounds/strike/siren.ogg
mix -i $S/lowFrequency_explosion_000.ogg -i $S/oga-rubberduck-cannon_02.ogg -i $S/explosionCrunch_004.ogg -filter_complex \
  "[1:a]volume=0.9[c];[2:a]adelay=30|30,volume=0.7[x];[0:a][c][x]amix=inputs=3:normalize=0:duration=longest,apad=whole_dur=3,afade=t=out:st=2.4:d=0.6,$NORM" \
  $OUT src/sounds/strike/impact.ogg
mix -i $S/freesound-506313-daleonfire-falling-bomb.ogg -filter_complex "[0:a]afade=t=out:st=2.6:d=0.3,$NORM" $OUT src/sounds/drop/incoming.ogg
mix -i $S/impactMetal_heavy_001.ogg -i $S/explosionCrunch_000.ogg -filter_complex \
  "[1:a]volume=0.5[x];[0:a][x]amix=inputs=2:normalize=0:duration=longest,apad=whole_dur=1,afade=t=out:st=0.7:d=0.3,$NORM" \
  $OUT src/sounds/drop/land.ogg
ls -l src/sounds/*/
