#!/bin/sh
# Rebuilds the pack, pushes it to GitHub and sends it live to everyone on the KitPvP server
# (/kitpvp pack set <url> <sha1>), no restart or rejoin.
#
#   ./deploy.sh <vanilla ascii.png>
#
# Panel key: $PTERO_KEY, or the file ~/.unstablemc/ptero-key.
set -e
cd "$(dirname "$0")"
key="${PTERO_KEY:-$(cat "$HOME/.unstablemc/ptero-key" 2>/dev/null || true)}"
[ -n "$key" ] || { echo "set PTERO_KEY or write the key to ~/.unstablemc/ptero-key" >&2; exit 2; }
api="https://panel.ultraservers.com/api/client/servers/a6c1b33a"

./build.sh "$1" >/dev/null
zip=$(ls dist/*.zip); name=$(basename "$zip"); sha=$(sha1sum "$zip" | cut -d' ' -f1)
git add -A
git commit -qm "Pack $name" || true
git push -q
url="https://raw.githubusercontent.com/Brick-Bread/unstablemc-kitpvp-pack/main/dist/$name"
for i in $(seq 1 30); do
  got=$(curl -sfL "$url" | sha1sum | cut -d' ' -f1 || true)
  [ "$got" = "$sha" ] && break
  sleep 2
done
[ "$got" = "$sha" ] || { echo "GitHub isn't serving $name yet; run again in a minute" >&2; exit 1; }
curl -sS -o /dev/null -H "Authorization: Bearer $key" -H "Accept: Application/vnd.pterodactyl.v1+json" \
  -H "Content-Type: application/json" -X POST "$api/command" --data-binary "{\"command\":\"kitpvp pack set $url $sha\"}"
echo "live: $url ($sha)"
