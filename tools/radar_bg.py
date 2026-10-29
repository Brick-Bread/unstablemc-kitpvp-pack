"""Height grid for the radar HUD's background map (bg.png), generated once from the arena's region files.

usage: python3 -I tools/radar_bg.py <unstablemc repo> <out radar-heights.txt>

Reuses the anvil/NBT parsing in <unstablemc repo>/tools/kitpvp/heightmap.py (not its main()). Each of the
88x88 pixels covers about 8.6 blocks square of the arena (x 140..900, z 220..980, north up) and gets the
tallest MOTION_BLOCKING_NO_LEAVES block in its footprint, the same footprint the plugin's radar used.
Writes 88 lines of 88 ints (block Y of the top block); src/Gen.java colours them.
"""
import os, sys

repo, out = sys.argv[1], sys.argv[2]
src = open(os.path.join(repo, "tools/kitpvp/heightmap.py")).read()
src = src[:src.rindex("\nmain()")]  # the module calls main() at the end; take only its functions
hm = {}
exec(compile(src, "heightmap.py", "exec"), hm)

X0, Z0, SPAN, SIZE = 140, 220, 760, 88
region = os.path.join(repo, "data/capitalcity-src/region")


def signed(v, bits):
    # the parser reads TAG_Byte unsigned
    return v - (1 << bits) if v >= 1 << (bits - 1) else v


def column_heights(longs, miny):
    # 256 values packed without spanning longs; the bit width follows from the array length
    for bits in range(9, 16):
        if len(longs) == -(-256 // (64 // bits)): break
    if bits == 9: return hm["heights"](longs, miny)
    per = 64 // bits; vals = []
    for v in longs:
        v &= (1 << 64) - 1
        for k in range(per):
            if len(vals) < 256: vals.append(((v >> (k * bits)) & ((1 << bits) - 1)) + miny)
    return vals


ground = {}  # (x, z) -> block Y of the top MOTION_BLOCKING_NO_LEAVES block
for f in sorted(os.listdir(region)):
    if not f.endswith(".mca"): continue
    rx, rz = (int(p) for p in f.split(".")[1:3])
    if rx * 512 > X0 + SPAN or rx * 512 + 511 < X0 or rz * 512 > Z0 + SPAN or rz * 512 + 511 < Z0: continue
    print("reading", f, flush=True)
    for c in hm["chunks"](os.path.join(region, f)):
        cx, cz = c.get("xPos"), c.get("zPos")
        if cx is None: continue
        y = c.get("yPos")
        if y is None:
            secs = [signed(s.get("Y", 0), 8) for s in (c.get("sections") or [])]
            y = min(secs) if secs else -4
        elif y > 127 and y < 256: y = signed(y, 8)
        maps = c.get("Heightmaps") or {}
        hmap = maps.get("MOTION_BLOCKING_NO_LEAVES") or maps.get("MOTION_BLOCKING") or maps.get("WORLD_SURFACE")
        if not hmap: continue
        for i, h in enumerate(column_heights(hmap, y * 16)):
            ground[(cx * 16 + (i & 15), cz * 16 + (i >> 4))] = h - 1

step = SPAN / SIZE
rows, missing = [], 0
for j in range(SIZE):
    row = []
    for i in range(SIZE):
        bx0, bz0 = int((X0 + i * step) // 1), int((Z0 + j * step) // 1)
        bx1 = max(bx0, int((X0 + (i + 1) * step) // 1) - 1)
        bz1 = max(bz0, int((Z0 + (j + 1) * step) // 1) - 1)
        best = None
        for x in range(bx0, bx1 + 1):
            for z in range(bz0, bz1 + 1):
                g = ground.get((x, z))
                if g is not None and (best is None or g > best): best = g
        if best is None: missing += 1; best = -64
        row.append(best)
    rows.append(row)
with open(out, "w") as f:
    for row in rows: f.write(" ".join(str(v) for v in row) + "\n")
flat = [v for r in rows for v in r]
print("wrote", out, "min", min(flat), "max", max(flat), "missing", missing)
