#!/bin/sh
# Mixes the pack's sounds from the CC0 files in src/sounds-src (see CREDITS.md there) into
# src/sounds/<group>/<name>.ogg (mono, so Minecraft plays them positionally). The results are
# committed, so build.sh doesn't need ffmpeg.
#
#   FFMPEG=/path/to/ffmpeg ./sounds.sh
#
# Each mix is trimmed of leading silence, then brought to its loudness (BIG -12 LUFS for strikes,
# rounds and drops, MID -15 for kill announcements, SOFT -18 for UI and feedback) with a limiter
# holding peaks near -1 dBTP, re-measured (up to 6 passes) until the limiting stops pulling it
# more than 0.3 LU off target.
set -e
cd "$(dirname "$0")"
FFMPEG="${FFMPEG:-ffmpeg}"
S=src/sounds-src
F=$S/fs
mkdir -p src/sounds/strike src/sounds/drop src/sounds/gadget src/sounds/kill src/sounds/round src/sounds/ui
T=$(mktemp -d)
trap 'rm -rf "$T"' EXIT
# Vorbis quality per tier, to keep the pack small: loud events q4 at 44.1 kHz, soft UI and feedback
# blips q3 at 32 kHz (they have little high end worth keeping)
OUT="-map_metadata -1 -fflags +bitexact -ac 1 -c:a libvorbis"
BIG=-12 MID=-15 SOFT=-18
ff() { "$FFMPEG" -hide_banner -loglevel error -y "$@"; }
# "<integrated LUFS> <true peak>" of $1, padded to one 400 ms gating block so blips measure too
measure() {
  "$FFMPEG" -hide_banner -nostats -i "$1" -af apad=whole_dur=0.4,ebur128=peak=true:framelog=quiet -f null - 2>&1 |
    awk '$1 == "I:" { i = $2 } $1 == "Peak:" { p = $2 } END { print i, p }'
}
# mix <LUFS> <out.ogg> <ffmpeg inputs and filter graph...>
mix() {
  lufs=$1 out=$2
  shift 2
  ff "$@" -ac 1 -ar 44100 -c:a pcm_f32le "$T/a.wav"
  ff -i "$T/a.wav" -af "silenceremove=start_periods=1:start_threshold=-60dB,afade=t=in:d=0.003" -c:a pcm_f32le "$T/b.wav"
  for pass in 1 2 3 4 5 6; do
    g=$(measure "$T/b.wav" | awk -v t="$lufs" '{ print t - $1 }')
    [ "$pass" -gt 1 ] && awk -v g="$g" 'BEGIN { exit !(g < 0.3 && g > -0.3) }' && break
    ff -i "$T/b.wav" -af "volume=${g}dB,alimiter=limit=0.8:level=0:attack=1:release=60:latency=1" -c:a pcm_f32le "$T/c.wav"
    mv "$T/c.wav" "$T/b.wav"
  done
  d=$("${FFMPEG%ffmpeg}ffprobe" -v error -show_entries format=duration -of csv=p=0 "$T/b.wav")
  if [ "$lufs" = "$SOFT" ]; then q="-q:a 3 -ar 32000"; else q="-q:a 4 -ar 44100"; fi
  ff -i "$T/b.wav" -af "afade=t=out:st=$(awk -v d="$d" 'BEGIN { print d - 0.02 }'):d=0.02" $OUT $q "$out"
}

# strikes
mix $BIG src/sounds/strike/charge.ogg -i $F/610562-BrickDeveloper171.mp3 -i $S/spaceEngineLarge_000.ogg -filter_complex \
  "[0:a]volume='0.45+t/2.4':eval=frame,afade=t=out:st=2.25:d=0.15[c];[1:a]atrim=0:2.4,asetpts=N/SR/TB,highpass=f=40,lowpass=f=400,afade=t=in:d=2.1:curve=exp,volume=0.4[e];[c][e]amix=inputs=2:normalize=0:duration=first"
mix $BIG src/sounds/strike/siren.ogg -i $F/351512-nsstudios.mp3 -filter_complex \
  "[0:a]atrim=4.2:7.6,asetpts=N/SR/TB,asetrate=32000*1.2,aresample=44100,highpass=f=200,afade=t=in:d=0.3,afade=t=out:st=2.45:d=0.38"
mix $BIG src/sounds/strike/impact.ogg -i $F/822677-AudioPapkin.mp3 -i $F/366091-bevibeldesign.mp3 -i $S/lowFrequency_explosion_000.ogg -filter_complex \
  "[0:a]atrim=start=0.1,asetpts=N/SR/TB[h];[1:a]areverse,atrim=start=0.15,asetpts=N/SR/TB,adelay=150|150,volume=0.6[w];[2:a]adelay=100|100,volume=0.8[b];[h][w][b]amix=inputs=3:normalize=0:duration=longest,apad=whole_dur=3,atrim=0:3,afade=t=out:st=2.2:d=0.8"
mix $BIG src/sounds/strike/wither.ogg -i $F/812709-AudioPapkin.mp3 -i $S/explosionCrunch_000.ogg -filter_complex \
  "[1:a]volume=0.4[x];[0:a][x]amix=inputs=2:normalize=0:duration=first,atrim=0:1.5,afade=t=out:st=1.1:d=0.4"
mix $BIG src/sounds/strike/mob.ogg -i $F/345736-noahpardo.mp3 -filter_complex \
  "[0:a]atrim=1.35:4.6,asetpts=N/SR/TB,afade=t=out:st=2.45:d=0.8"

# supply drops
mix $BIG src/sounds/drop/incoming.ogg -i $S/freesound-506313-daleonfire-falling-bomb.ogg -filter_complex "[0:a]afade=t=out:st=2.6:d=0.3"
mix $BIG src/sounds/drop/land.ogg -i $F/813053-AudioPapkin.mp3 -i $S/impactMetal_heavy_001.ogg -i $S/lowFrequency_explosion_000.ogg -filter_complex \
  "[0:a]atrim=start=0.2,asetpts=N/SR/TB[r];[1:a]adelay=100|100,volume=0.6[m];[2:a]adelay=100|100,lowpass=f=250,volume=0.6[b];[r][m][b]amix=inputs=3:normalize=0:duration=longest,atrim=0:1.2,afade=t=out:st=0.85:d=0.35"
mix $SOFT src/sounds/drop/pickup.ogg -i $F/735168-Irolan.mp3 -filter_complex "[0:a]atrim=0:1.5,afade=t=out:st=1.1:d=0.4"

# gadgets
mix $SOFT src/sounds/gadget/ready.ogg -i $F/683185-NearTheAtmoshphere.mp3 -filter_complex "[0:a]atrim=0:1.6,afade=t=out:st=1.3:d=0.3"
mix $SOFT src/sounds/gadget/use.ogg -i $F/222493-qubodup.mp3 -filter_complex \
  "[0:a]atrim=3.0:3.75,asetpts=N/SR/TB,afade=t=in:d=0.08,afade=t=out:st=0.55:d=0.2,apad=whole_dur=0.9"

# kills
mix $SOFT src/sounds/kill/confirm.ogg -i $F/570335-User391915396.mp3 -filter_complex "[0:a]anull"
mix $MID src/sounds/kill/streak.ogg -i $F/566203-colorsCrimsonTears.mp3 -filter_complex "[0:a]atrim=0:1.0,apad=whole_dur=1.2"
mix $MID src/sounds/kill/shutdown.ogg -i $F/848312-dwswords.mp3 -i $S/lowFrequency_explosion_000.ogg -filter_complex \
  "[0:a]atrim=start=0.38,asetpts=N/SR/TB[g];[1:a]lowpass=f=200,volume=0.8[b];[g][b]amix=inputs=2:normalize=0:duration=longest,atrim=0:1.5,afade=t=out:st=1.1:d=0.4"

# rounds
mix $BIG src/sounds/round/start.ogg -i $F/473601-CZghost.mp3 -filter_complex "[0:a]atrim=0.1:3.7,asetpts=N/SR/TB,afade=t=out:st=2.8:d=0.8"
mix $BIG src/sounds/round/border.ogg -i $F/827562-AudioPapkin.mp3 -i $F/366091-bevibeldesign.mp3 -filter_complex \
  "[0:a]asetrate=48000*0.8,aresample=44100[i];[1:a]areverse,atrim=start=0.15,asetpts=N/SR/TB,lowpass=f=300,volume=0.6[r];[i][r]amix=inputs=2:normalize=0:duration=longest,apad=whole_dur=3,atrim=0:3,afade=t=out:st=2.3:d=0.7"
mix $BIG src/sounds/round/sudden.ogg -i $F/232481-bluejello13.mp3 -i $S/lowFrequency_explosion_000.ogg -filter_complex \
  "[0:a]atrim=0:3.45[h];[1:a]lowpass=f=200,volume=0.7[b];[h][b]amix=inputs=2:normalize=0:duration=first,afade=t=out:st=3.1:d=0.35"
mix $BIG src/sounds/round/win.ogg -i $F/677859-el_boss.mp3 -filter_complex "[0:a]atrim=0.08:3.1,asetpts=N/SR/TB,afade=t=out:st=2.6:d=0.42"

# UI
mix $SOFT src/sounds/ui/tick.ogg -i $F/515317-basarunder.mp3 -filter_complex "[0:a]atrim=0:0.62"
mix $SOFT src/sounds/ui/click.ogg -i $F/839832-TommyListens.mp3 -filter_complex "[0:a]anull"
mix $SOFT src/sounds/ui/confirm.ogg -i $F/836200-MATUSTRM.mp3 -filter_complex "[0:a]atrim=0:1.0,afade=t=out:st=0.6:d=0.4"
ls -l src/sounds/*/

# lobby music: two synthwave loops, streamed; mono 32 kHz q2 keeps them small (they're background)
mkdir -p src/sounds/music
for t in "699241-LulSayer.mp3 lobby1" "697437-Johnnie_Holiday.mp3 lobby2"; do
  set -- $t
  ff -i "$F/$1" -af "loudnorm=I=-18:TP=-1.5:LRA=11,afade=t=in:d=0.4" -map_metadata -1 -fflags +bitexact -ac 1 -ar 32000 -c:a libvorbis -q:a 2 "src/sounds/music/$2.ogg"
done
