#!/bin/sh
# Builds the KitPvP pack: draws the art with src/Gen.java, zips it deterministically into
# dist/kitpvp-pack-<sha1 prefix>.zip and prints the URL + SHA-1 for the server's server.properties.
# Needs Java 25 and the vanilla client's assets/minecraft/textures/font/ascii.png (passed as $1).
set -e
cd "$(dirname "$0")"
JAVA="${JAVA:-$HOME/.local/jdks/jdk-25.0.5+8/bin/java}"
rm -rf build && "$JAVA" src/Gen.java "$1" build
python3 -I - <<'P'
import hashlib, os, zipfile, io
buf = io.BytesIO()
with zipfile.ZipFile(buf, "w", zipfile.ZIP_DEFLATED) as z:
    for root, _, files in os.walk("build"):
        for f in sorted(files):
            p = os.path.join(root, f); arc = os.path.relpath(p, "build").replace(os.sep, "/")
            zi = zipfile.ZipInfo(arc, (2026, 1, 1, 0, 0, 0)); zi.compress_type = zipfile.ZIP_DEFLATED
            z.writestr(zi, open(p, "rb").read())
data = buf.getvalue(); sha = hashlib.sha1(data).hexdigest()
for old in os.listdir("dist"): os.remove(os.path.join("dist", old))
name = f"kitpvp-pack-{sha[:10]}.zip"; open(os.path.join("dist", name), "wb").write(data)
print("dist/" + name); print("sha1 " + sha)
P
