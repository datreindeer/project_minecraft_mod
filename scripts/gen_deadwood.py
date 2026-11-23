"""The pieces of a snag, a dead standing tree (Deadwood.java; worldgen ForestDecor.snag): Deadwood, Broken Deadwood,
Deadwood Branch, Shelf Fungus and one Fallen Bark per wood. Blockstates, models, item definitions, loot, tags, lang, the
furnace fuel values, and their textures (painted here, seeded, so a rerun gives the same files). Also repaints the Grub
Log as Deadwood bored by beetles, with a woodpecker's hole, so it sits in a dead trunk without looking like bark.

Generator order: after gen_fauna_temperate.py (Grub Log) and before gen_block_items.py. Run: python scripts/gen_deadwood.py
(writes deadwood_preview.png in the repo root, not committed).
"""
import glob
import io
import json
import math
import zipfile
from pathlib import Path

import numpy as np
from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
RES = ROOT / "src/main/resources"
A, D = RES / "assets/mechanical", RES / "data"
TEX = A / "textures/block"
JAR = zipfile.ZipFile(sorted(glob.glob(str(ROOT / "build/moddev/artifacts/minecraft-patched-*-merged.jar")))[0])
JAVA = (ROOT / "src/main/java/com/mechanical/Mechanical.java").read_text(encoding="utf-8")
WOODS = __import__("re").search(r'WOODS = List\.of\(([^)]*)\)', JAVA).group(1).replace('"', "").replace(" ", "").replace("\n", "").split(",")
M = "mechanical:"
N = 16
Y, X = np.mgrid[0:N, 0:N].astype(float)


def w(path, obj):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, indent=2) + "\n", encoding="utf-8")


# ------------------------------------------------------------------------------------------------ textures
# weathered deadwood, dark crack -> silver
# weathered deadwood: silver-grey with a brown undertone (never birch-white), dark fissure -> sun-bleached ridge
GREY = [(50, 44, 38), (76, 69, 61), (98, 90, 80), (116, 108, 97), (132, 124, 112), (147, 139, 127), (161, 154, 142), (175, 169, 157),
        (190, 184, 172)]


def paint(light, ramp, rng, grain=0.25):
    idx = np.clip(np.rint(light * (len(ramp) - 1) + rng.normal(0, grain, light.shape)), 0, len(ramp) - 1).astype(int)
    img = Image.new("RGBA", (N, N))
    for y in range(N):
        for x in range(N):
            img.putpixel((x, y), ramp[idx[y, x]] + (255,))
    return img


def deadwood_side(seed, knot=False):
    """Bleached, barkless wood: grain in streaks running up (tiling top to bottom), deep checks with a lit lip on one side
    and shade on the other, now and then a knot."""
    rng = np.random.default_rng(seed)
    cols = np.cumsum(rng.normal(0, 0.11, N))
    cols = (cols - cols.mean()) * 0.8 + rng.normal(0, 0.08, N)  # streaks a pixel or three wide
    light = 0.56 + cols[None, :] + 0.05 * np.sin(2 * math.pi * (Y / N) + X * 0.9 + rng.uniform(0, 6))
    light += 0.06 * (rng.random((N, N)) - 0.5)
    for _ in range(int(rng.integers(2, 4))):  # checks along the grain, wrapping top to bottom
        cx, y0, length = int(rng.integers(0, N)), int(rng.integers(0, N)), int(rng.integers(8, 16))
        for i in range(length):
            y = (y0 + i) % N
            x = (cx + (1 if i > length // 2 and rng.random() < 0.3 else 0)) % N
            t = 1.0 if 1 <= i < length - 1 else 0.55
            light[y, x] = min(light[y, x], 0.62 - 0.55 * t)
            light[y, (x - 1) % N] += 0.12 * t  # lit lip
            light[y, (x + 1) % N] -= 0.07 * t
    if knot:
        kx, ky = int(rng.integers(4, 12)), int(rng.integers(3, 13))
        for y in range(N):
            for x in range(N):
                d = ((x - kx) / 1.3) ** 2 + ((y - ky) / 1.9) ** 2
                if d < 1.1:
                    light[y, x] = 0.18
                elif d < 2.6:
                    light[y, x] = 0.78 if x < kx else 0.5
    return paint(np.clip(light, 0, 1), GREY, rng, 0.2)


def deadwood_top(seed, rot=0.0):
    """End grain gone grey: rings, a darker, punky heart, checks split out from the middle, a weathered rim."""
    rng = np.random.default_rng(seed)
    r = np.maximum(np.abs(X - 7.5), np.abs(Y - 7.5)) * 0.65 + np.hypot(X - 7.5, Y - 7.5) * 0.35
    light = 0.62 + 0.08 * np.cos(r * 2.1) + 0.05 * (rng.random((N, N)) - 0.5)
    light -= 0.18 * np.clip(1 - r / 2.6, 0, 1)  # the heart rots first
    light = np.where((np.minimum(np.minimum(X, N - 1 - X), np.minimum(Y, N - 1 - Y)) == 0), 0.42, light)  # weathered rim
    for a in rng.uniform(0, 2 * math.pi, 3):  # radial checks
        for k in np.arange(1.0, 7.5, 0.5):
            x, y = int(round(7.5 + math.cos(a) * k)), int(round(7.5 + math.sin(a) * k))
            if 0 <= x < N and 0 <= y < N:
                light[y, x] = 0.1
    img = paint(np.clip(light, 0, 1), GREY, rng, 0.15)
    if rot:  # rot browns the heart a little
        px = img.load()
        for y in range(N):
            for x in range(N):
                f = rot * max(0.0, 1 - math.hypot(x - 7.5, y - 7.5) / 6)
                c = px[x, y]
                px[x, y] = (int(c[0] * (1 - f) + 96 * f), int(c[1] * (1 - f) + 74 * f), int(c[2] * (1 - f) + 52 * f), 255)
    return img


def branch_tex(seed):
    """Small dead branches: the same grey, finer and without a strong grain, so it reads along any axis."""
    rng = np.random.default_rng(seed)
    light = 0.62 + 0.10 * (rng.random((N, N)) - 0.5)
    for _ in range(10):  # short splits, either way
        x, y = int(rng.integers(0, N)), int(rng.integers(0, N))
        dx, dy = (1, 0) if rng.random() < 0.5 else (0, 1)
        for i in range(int(rng.integers(2, 4))):
            light[(y + i * dy) % N, (x + i * dx) % N] = 0.22
            light[(y + i * dy - dx) % N, (x + i * dx - dy) % N] += 0.1
    return paint(np.clip(light, 0, 1), GREY, rng, 0.15)


def grub_side(seed):
    """Deadwood gone soft and brown in patches, peppered with beetle exit holes, a woodpecker's hole chiselled in."""
    rng = np.random.default_rng(seed)
    img = deadwood_side(seed)
    px = img.load()
    rot = np.clip(np.sin(X * 0.7 + rng.uniform(0, 6)) * np.cos(Y * 0.5 + rng.uniform(0, 6)) + 0.3, 0, 1)
    for y in range(N):
        for x in range(N):
            f = 0.35 * rot[y, x]
            c = px[x, y]
            px[x, y] = (int(c[0] * (1 - f) + 110 * f), int(c[1] * (1 - f) + 86 * f), int(c[2] * (1 - f) + 60 * f), 255)
    for _ in range(9):  # beetle exit holes: a dark pit, a lighter chewed edge under it
        x, y = int(rng.integers(0, N)), int(rng.integers(0, N - 1))
        px[x, y] = (26, 20, 16, 255)
        px[x, y + 1] = (150, 134, 108, 255)
    hx, hy = int(rng.integers(5, 9)), int(rng.integers(4, 9))  # the woodpecker's hole: dark, oval, a fresh pale rim
    for y in range(hy - 1, hy + 4):
        for x in range(hx - 1, hx + 3):
            inner = hy <= y <= hy + 2 and hx <= x <= hx + 1
            corner = (y in (hy - 1, hy + 3)) and (x in (hx - 1, hx + 2))
            if inner:
                px[x, y] = (14, 11, 10, 255) if y > hy else (30, 24, 20, 255)
            elif not corner:
                px[x, y] = (196, 176, 138, 255) if y >= hy + 2 or x == hx + 2 else (176, 156, 120, 255)
    return img


# polypore brackets: rows 0-7 the top (rim at row 0 -> base at row 7), 8-11 the pale pored underside, 12-15 the edge
FUNGUS_TOP = [(216, 200, 160), (198, 176, 132), (158, 116, 70), (140, 100, 60), (112, 96, 80), (96, 82, 68), (78, 66, 54), (64, 54, 46)]


def fungus_tex(seed):
    rng = np.random.default_rng(seed)
    img = Image.new("RGBA", (N, N))
    for y in range(N):
        for x in range(N):
            if y < 8:  # growth bands, a little wavy
                yy = min(7, max(0, y + (1 if rng.random() < 0.15 else 0) - (1 if rng.random() < 0.1 else 0)))
                c = FUNGUS_TOP[yy]
            elif y < 12:  # the pores
                c = (206, 192, 156) if (x + y) % 3 else (178, 162, 126)
            else:  # the edge: dark top band down to a pale growing rim
                c = [(92, 78, 64), (138, 100, 62), (190, 168, 124), (220, 206, 168)][y - 12]
            k = 1 + rng.normal(0, 0.035)
            img.putpixel((x, y), tuple(min(255, int(v * k)) for v in c) + (255,))
    return img


def log_texture(wood):
    """That wood's bark texture: the side texture of its log model (mod or vanilla)."""
    for ns in ("mechanical", "minecraft"):
        p = A.parent / ns / "blockstates" / f"{wood}_log.json"
        text = p.read_text(encoding="utf-8") if p.exists() else None
        if text is None:
            try:
                text = JAR.read(f"assets/{ns}/blockstates/{wood}_log.json").decode()
            except KeyError:
                continue
        bs = json.loads(text)
        model = next(iter(bs["variants"].values()))
        model = (model[0] if isinstance(model, list) else model)["model"]
        mns, mpath = model.split(":") if ":" in model else ("minecraft", model)
        mp = A.parent / mns / "models" / f"{mpath}.json"
        m = json.loads(mp.read_text(encoding="utf-8") if mp.exists() else JAR.read(f"assets/{mns}/models/{mpath}.json"))
        tex = m["textures"]["side"]
        tns, tpath = tex.split(":") if ":" in tex else ("minecraft", tex)
        tp = A.parent / tns / "textures" / f"{tpath}.png"
        return Image.open(tp if tp.exists() else io.BytesIO(JAR.read(f"assets/{tns}/textures/{tpath}.png"))).convert("RGBA").crop((0, 0, N, N))
    raise SystemExit(f"no log for {wood}")


def fallen_bark_tex(wood, seed):
    """Three or four curled flakes cut out of the wood's own bark, each with a lit upper edge and a shadowed lower one."""
    rng = np.random.default_rng(seed)
    bark = log_texture(wood)
    img = Image.new("RGBA", (N, N), (0, 0, 0, 0))
    spots = [(3.5, 4.0), (11.5, 3.5), (5.0, 11.5), (12.0, 11.0)]
    for cx, cy in spots[:3 + int(rng.integers(0, 2))]:
        cx, cy = cx + rng.uniform(-1, 1), cy + rng.uniform(-1, 1)
        a, rx, ry = rng.uniform(0, math.pi), rng.uniform(2.2, 3.2), rng.uniform(1.0, 1.6)
        for y in range(N):
            for x in range(N):
                u = (x + 0.5 - cx) * math.cos(a) + (y + 0.5 - cy) * math.sin(a)
                v = -(x + 0.5 - cx) * math.sin(a) + (y + 0.5 - cy) * math.cos(a)
                d = (u / rx) ** 2 + (v / ry) ** 2
                if d > 1:
                    continue
                c = bark.getpixel((x, y))[:3]
                k = 1.18 if v < -ry * 0.45 else 0.7 if v > ry * 0.5 else 1.0  # curled: lit lip, shaded underside
                img.putpixel((x, y), tuple(min(255, int(ch * k)) for ch in c) + (255,))
    return img


textures = {"deadwood": deadwood_side(11), "deadwood_2": deadwood_side(12, knot=True), "deadwood_top": deadwood_top(13),
            "deadwood_branch": branch_tex(14), "shelf_fungus": fungus_tex(15), "grub_log": grub_side(16), "grub_log_top": deadwood_top(17, rot=0.55)}
for i, wood in enumerate(WOODS):
    textures[f"{wood}_fallen_bark"] = fallen_bark_tex(wood, 100 + i)
for name, img in textures.items():
    img.save(TEX / f"{name}.png")


# ------------------------------------------------------------------------------------------------ models


def face(tex, uv=None, rotation=0, cull=None):
    f = {"texture": tex}
    if uv:
        f["uv"] = [round(v, 3) for v in uv]
    if rotation:
        f["rotation"] = rotation
    if cull:
        f["cullface"] = cull
    return f


def model(name, elements, textures, parent="minecraft:block/block"):
    w(A / f"models/block/{name}.json", ({"parent": parent} if parent else {}) | {"textures": textures, "elements": elements})


def box(fr, to, faces):
    return {"from": fr, "to": to, "faces": faces}


def side_uv(fr, to, d):
    """The uv a face of a box would get by default (MC's auto uv), for faces that need it spelled out."""
    x1, y1, z1 = fr
    x2, y2, z2 = to
    return {"north": [16 - x2, 16 - y2, 16 - x1, 16 - y1], "south": [x1, 16 - y2, x2, 16 - y1], "west": [z1, 16 - y2, z2, 16 - y1],
            "east": [16 - z2, 16 - y2, 16 - z1, 16 - y1], "up": [x1, z1, x2, z2], "down": [x1, 16 - z2, x2, 16 - z1]}[d]


# Deadwood: a log like any other, two looks, turned so a tall trunk doesn't repeat
for name in ("deadwood", "deadwood_2"):
    w(A / f"models/block/{name}.json", {"parent": "minecraft:block/cube_column", "textures": {"side": M + "block/" + name, "end": M + "block/deadwood_top"}})
w(A / "models/block/deadwood_horizontal.json", {"parent": "minecraft:block/cube_column_horizontal",
                                                "textures": {"side": M + "block/deadwood", "end": M + "block/deadwood_top"}})
w(A / "blockstates/deadwood.json", {"variants": {
    "axis=y": [{"model": M + "block/deadwood"}, {"model": M + "block/deadwood_2"}, {"model": M + "block/deadwood", "y": 180},
               {"model": M + "block/deadwood_2", "y": 180}],
    "axis=z": [{"model": M + "block/deadwood_horizontal", "x": 90}, {"model": M + "block/deadwood_horizontal", "x": 90, "y": 180}],
    "axis=x": [{"model": M + "block/deadwood_horizontal", "x": 90, "y": 90}, {"model": M + "block/deadwood_horizontal", "x": 90, "y": 270}]}})


def shards(spec):
    """A broken top: the lower half of the trunk, shards of wood round its rim at different heights, a splinter standing
    above the block. spec: (x1, z1, x2, z2, height) per shard; the splinter's box is the last entry and may pass y 16."""
    els = [box([0, 0, 0], [16, 8, 16], {d: face("#side", cull=d if d != "up" else None) for d in ("north", "south", "east", "west", "down")}
               | {"up": face("#end")})]
    for x1, z1, x2, z2, h in spec:
        fr, to = [x1, 8, z1], [x2, h, z2]
        faces = {"up": face("#end", [x1, z1, x2, z2])}
        for d in ("north", "south", "east", "west"):
            u = side_uv(fr, to, d)
            if h > 16:  # spell the uv out inside the texture for the part above the block
                u = [u[0], 0, u[2], min(16, h - 8)]
            faces[d] = face("#side", u)
        els.append(box(fr, to, faces))
    return els


TOP_TEX = {"side": M + "block/deadwood", "end": M + "block/deadwood_top", "particle": M + "block/deadwood"}
model("broken_deadwood", shards([(0, 0, 7, 5, 15), (7, 0, 16, 4, 11), (11, 4, 16, 11, 13), (9, 11, 16, 16, 10), (3, 10, 9, 16, 14),
                                 (0, 5, 4, 10, 12), (4, 4, 11, 10, 9), (1, 1, 4, 3, 22)]), TOP_TEX)
model("broken_deadwood_2", shards([(0, 0, 16, 6, 12), (10, 6, 16, 16, 15), (0, 11, 10, 16, 10), (0, 6, 5, 11, 13), (5, 6, 10, 11, 9),
                                   (12, 12, 15, 15, 21)]), TOP_TEX)
w(A / "blockstates/broken_deadwood.json", {"variants": {"": [{"model": M + f"block/broken_deadwood{v}", **({"y": y} if y else {})}
                                                              for v in ("", "_2") for y in (0, 90, 180, 270)]}})

# Deadwood Branch (6 px) and Deadwood Twig (4 px, the thin ends of a dead tree's limbs): stubs joining like a chorus plant.
# The core's faces are separate parts: a free end (nothing that way, joined the other way) shows the grey end grain of a
# snapped twig, any other free face the branch's skin.
BR_TEX = {"side": M + "block/deadwood_branch", "end": M + "block/deadwood_top", "particle": M + "block/deadwood_branch"}
OPP = {"north": "south", "south": "north", "east": "west", "west": "east", "up": "down", "down": "up"}
ARM_ROT = {"north": {}, "east": {"y": 90}, "south": {"y": 180}, "west": {"y": 270}, "up": {"x": 270}, "down": {"x": 90}}
for name, B0, B1 in (("deadwood_branch", 5, 11), ("deadwood_twig", 6, 10)):
    for d in OPP:
        for kind, tex in (("side", "#side"), ("end", "#end")):
            model(f"{name}_{kind}_{d}", [box([B0] * 3, [B1] * 3, {d: face(tex, side_uv([B0] * 3, [B1] * 3, d))})], BR_TEX)
    model(f"{name}_arm", [box([B0, B0, 0], [B1, B1, B0], {d: face("#side", side_uv([B0, B0, 0], [B1, B1, B0], d))
                                                          for d in ("east", "west", "up", "down")} | {"north": face("#side", cull="north")})], BR_TEX)
    parts = [{"when": {d: "true"}, "apply": {"model": M + f"block/{name}_arm", **ARM_ROT[d]}} for d in OPP]
    for d, o in OPP.items():
        parts.append({"when": {d: "false", o: "true"}, "apply": {"model": M + f"block/{name}_end_{d}"}})
        parts.append({"when": {d: "false", o: "false"}, "apply": {"model": M + f"block/{name}_side_{d}"}})
    w(A / f"blockstates/{name}.json", {"multipart": parts})
    model(f"{name}_inventory", [box([B0] * 3, [B1] * 3, {d: face("#end" if d == "south" else "#side", side_uv([B0] * 3, [B1] * 3, d))
                                                         for d in OPP if d != "north"}),
                                box([B0, B0, 0], [B1, B1, B0], {d: face("#side", side_uv([B0, B0, 0], [B1, B1, B0], d))
                                                                for d in ("east", "west", "up", "down", "north")})], BR_TEX)


def bracket(tiers):
    """One polypore bracket on the trunk to the south (z 16), pointing north: stacked tiers (x1, x2, z_front, y1, y2),
    the top banded from the pale growing rim at the front to the dark base, the underside pored, the front edge banded."""
    rim = min(t[2] for t in tiers)
    els = []
    for x1, x2, z1, y1, y2 in tiers:
        fr, to = [x1, y1, z1], [x2, y2, 16]
        v = lambda z: (z - rim) / (16 - rim) * 8  # top rows by distance from the rim
        faces = {"up": face("#t", [x1, v(z1), x2, v(16)]), "down": face("#t", [x1, 8, x2, 12]),
                 "north": face("#t", [16 - x2, 12, 16 - x1, 12 + min(4, (y2 - y1) * 4 / 3)]),
                 "east": face("#t", [0, 12, min(16, 16 - z1), 12 + min(4, (y2 - y1) * 4 / 3)]),
                 "west": face("#t", [0, 12, min(16, 16 - z1), 12 + min(4, (y2 - y1) * 4 / 3)])}
        els.append(box(fr, to, faces))
    return els


FUN_TEX = {"t": M + "block/shelf_fungus", "particle": M + "block/shelf_fungus"}
model("shelf_fungus", bracket([(2, 14, 12, 4, 7), (3, 13, 10, 4.5, 6.5), (5, 11, 9, 5, 6)]) + bracket([(7, 14, 13.5, 10, 12), (8, 13, 12, 10.5, 11.5)])
      + bracket([(2, 6, 14.5, 12.5, 13.5)]), FUN_TEX)
model("shelf_fungus_single", bracket([(3, 13, 12, 6, 9), (4, 12, 10, 6.5, 8.5), (6, 10, 9, 7, 8)]), FUN_TEX)
w(A / "blockstates/shelf_fungus.json", {"variants": {f"facing={f}": [{"model": M + f"block/shelf_fungus{v}", **({"y": y} if y else {})}
                                                                     for v in ("", "_single")]
                                                     for f, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270))}})

# Fallen Bark: a flat litter like Twigs, turned four ways
for wood in WOODS:
    name = f"{wood}_fallen_bark"
    model(name, [box([0, 0.1, 0], [16, 0.1, 16], {"up": face("#bark", [0, 0, 16, 16]), "down": face("#bark", [0, 16, 16, 0])}) | {"shade": False}],
          {"bark": M + "block/" + name, "particle": M + "block/" + name}, parent=None)
    w(A / f"blockstates/{name}.json", {"variants": {"": [{"model": M + "block/" + name, **({"y": y} if y else {})} for y in (0, 90, 180, 270)]}})
    w(A / f"items/{name}.json", {"model": {"type": "minecraft:model", "model": M + f"item/{wood}_bark"}})

for block, mdl in (("deadwood", "deadwood"), ("broken_deadwood", "broken_deadwood"), ("deadwood_branch", "deadwood_branch_inventory"),
                   ("deadwood_twig", "deadwood_twig_inventory"), ("shelf_fungus", "shelf_fungus")):
    w(A / f"items/{block}.json", {"model": {"type": "minecraft:model", "model": M + "block/" + mdl}})

# ------------------------------------------------------------------------------------------------ loot, tags, fuel, lang


def loot(block, entry):
    w(D / f"mechanical/loot_table/blocks/{block}.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [entry],
      "condition": {"type": "minecraft:survives_explosion"}}], "random_sequence": f"mechanical:blocks/{block}"})


def sticks(lo, hi):
    return {"type": "minecraft:item", "name": "minecraft:stick", "modifier": [{"type": "minecraft:set_count",
            "count": {"type": "minecraft:uniform", "min": lo, "max": hi}}]}


loot("deadwood", {"type": "minecraft:item", "name": M + "deadwood"})
loot("shelf_fungus", {"type": "minecraft:item", "name": M + "shelf_fungus"})
loot("broken_deadwood", sticks(1, 2))
loot("deadwood_branch", sticks(1, 1))
loot("deadwood_twig", sticks(0, 1))
for wood in WOODS:
    loot(f"{wood}_fallen_bark", {"type": "minecraft:item", "name": M + f"{wood}_bark"})


def tag_add(rel, values):
    path = D / rel
    tag = json.loads(path.read_text(encoding="utf-8")) if path.exists() else {"values": []}
    tag["values"] += [v for v in values if v not in tag["values"]]
    w(path, tag)


tag_add("minecraft/tags/block/mineable/axe.json", [M + b for b in ("deadwood", "broken_deadwood", "deadwood_branch", "deadwood_twig", "shelf_fungus")])
fuel = D / "neoforge/data_maps/item/furnace_fuels.json"
fmap = json.loads(fuel.read_text(encoding="utf-8")) if fuel.exists() else {"values": {}}
fmap["values"].update({M + "deadwood": {"burn_time": 300}, M + "broken_deadwood": {"burn_time": 150}, M + "deadwood_branch": {"burn_time": 100}, M + "deadwood_twig": {"burn_time": 50}})
w(fuel, fmap)

lang_path = A / "lang/en_us.json"
lang = json.loads(lang_path.read_text(encoding="utf-8"))
lang.update({"block.mechanical.deadwood": "Deadwood", "block.mechanical.broken_deadwood": "Broken Deadwood",
             "block.mechanical.deadwood_branch": "Deadwood Branch", "block.mechanical.deadwood_twig": "Deadwood Twig",
             "block.mechanical.shelf_fungus": "Shelf Fungus"})
lang.update({f"block.mechanical.{wood}_fallen_bark": f"Fallen {wood.replace('_', ' ').title()} Bark" for wood in WOODS})
lang_path.write_text(json.dumps(lang, indent=2) + "\n", encoding="utf-8")

# preview (not committed): the textures 8x
names = [n for n in textures if not n.endswith("_fallen_bark")] + ["oak_fallen_bark", "birch_fallen_bark", "spruce_fallen_bark"]
sheet = Image.new("RGBA", (len(names) * 16, 16), (90, 120, 70, 255))
for i, n in enumerate(names):
    sheet.alpha_composite(textures[n], (i * 16, 0))
sheet.resize((sheet.width * 8, 128), Image.NEAREST).save(ROOT / "deadwood_preview.png")
print(f"deadwood: {len(textures)} textures, {len(WOODS)} fallen barks")
