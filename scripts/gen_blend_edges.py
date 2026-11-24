# Edge blending (client/EdgeBlend.java): where two related blocks meet on their top faces, the stronger one frays over the
# weaker one instead of a straight seam. This script holds the one table of blend strengths and groups, works out every
# block's top texture from its model, and draws for each block that can fray a sheet of 16x16 overlay tiles: its own top
# texture cut out by an edge mask, one tile per edge direction and variant. The mask style follows the material:
#   noise  - ground (grass, sand, dirt, gravel...): a ragged, dithered edge a few pixels deep with stray specks
#   chunk  - rough stone (stone, andesite, mod stones): an irregular edge made of whole stone chunks; cobble-like textures
#            (cobblestone, fieldstone) cut along their own stones, found by segmenting the texture
#   brick  - bricks and tiles: toothed; the edge follows the texture's own mortar lines, every other course reaches half a
#            brick over the neighbour, along a course every third brick steps out
#   plank  - planks: boards interleave; along the grain every other board runs on to one of its joints, across the grain a
#            stretch of the first board swaps
# Writes textures/block/blend/<namespace>_<name>.png, blend/edges.json (read by EdgeBlend at model bake) and the
# client config option's en_us name.
# Tile order per sheet: 4 edges (N, E, S, W) x K x K x M, then (when the style has corners) 4 corners (NE, SE, SW, NW) x K.
# K: classes of the lattice corners at an edge's two ends (keeps a long edge continuous from block to block), M: variants.
# usage: python scripts/gen_blend_edges.py   (needs build/moddev/artifacts/minecraft-patched-*.jar)
import io
import json
import math
import random
import re
import zipfile
from pathlib import Path

import numpy as np
from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
RES = ROOT / "src/main/resources"
OUT_TEX = RES / "assets/mechanical/textures/block/blend"
OUT_JSON = RES / "assets/mechanical/blend/edges.json"
LANG = RES / "assets/mechanical/lang/en_us.json"
JAR = zipfile.ZipFile(next((ROOT / "build/moddev/artifacts").glob("minecraft-patched-*[0-9].jar")))

G, S, P = 1, 2, 4  # groups: ground, stone, planks. A block frays over a weaker one when they share a group.

# ---------------------------------------------------------------------------------------------------------------------
# THE TABLE: strongest first. Position is the blend strength (ties cannot happen). (block, groups, style); style None =
# takes overlays but never frays over anything (polished and cut stone, the mod's paths). The mod's stones come from
# StoneFamilies.java (the same rows as gen_stone_families.py), its planks from the minecraft:planks tag, its ground slabs'
# bases from EarthBlocks.java, so a new stone, wood or ground slab joins on the next run; check() fails on anything missed.
JAVA = ROOT / "src/main/java/com/mechanical"
WOOD_ORDER = ["dark_oak", "bog_oak", "spruce", "mangrove", "jungle", "acacia", "oak", "birch", "cherry", "pale_oak", "poplar",
              "crimson", "warped", "pine", "yew", "alder", "ash", "aspen", "beech", "hazel", "linden", "willow", "rowan", "bird_cherry", "wild_apple", "plum", "olive", "date_palm"]
VANILLA_RAW = ["stone", "andesite", "granite", "diorite", "tuff", "calcite", "deepslate"]
VANILLA_POLISHED = ["smooth_stone", "polished_andesite", "polished_granite", "polished_diorite", "polished_tuff", "polished_deepslate"]
# ground that is not a stone family: above the stones (living ground) and below them (bare ground)
GROUND_TOP = ["grass_block", "mechanical:turf", "moss_block", "snow_block", "mechanical:packed_snow", "mycelium", "podzol", "sand",
              "mechanical:packed_sand", "red_sand", "mechanical:packed_red_sand"]  # loose sand and snow fray over their packed kind
GROUND_LOW = ["mechanical:chalk_block", "mechanical:wood_ash", "mechanical:burnt_soil", "mechanical:loess", "coarse_dirt", "rooted_dirt", "mechanical:peat",
              "clay", "mechanical:kaolin", "mud", "packed_mud", "dirt"]
# functional blocks that only look like masonry
NOT_BLENDED = {"mechanical:furnace_bricks", "mechanical:furnace_bricks_tuyere", "mechanical:bloomery_bricks", "mechanical:bloomery_bricks_sooty",
               "minecraft:packed_ice"}  # the ice slabs (gen_dirt_slab.py): ice does not fray into ground


def ns_id(b):
    return b if ":" in b else "minecraft:" + b


def stone_families():
    """(name, base, template, patterns) of every StoneFamilies row, with the weathered bricks added like the Java does."""
    src = (JAVA / "StoneFamilies.java").read_text(encoding="utf-8")
    enum = src[src.index("public enum Template"):src.index("final boolean pillarBase")]
    templates = {m.group(1): re.findall(r'"([^"]+)"', m.group(2))
                 for m in re.finditer(r'\n\s+([A-Z]+)\((?:true|false),((?:\s*"[^"]*",?)+)\)', enum)}
    weathered = re.findall(r'"([^"]+)"', re.search(r"WEATHERED = List\.of\(([^)]*)\)", src).group(1))
    weathers = re.findall(r"Template\.([A-Z]+)", re.search(r"WEATHERS = [^;]*;", src).group(0))
    rows = re.findall(r'(?:new Stone|Stone\.of)\("([a-z_]+)", "([a-z_]+)", Template\.([A-Z]+)', src)
    assert rows and templates, "StoneFamilies.java no longer parses"
    out = []
    for name, base, tpl in rows:
        pats = list(templates[tpl]) + ([p for p in weathered if p not in templates[tpl]] if tpl in weathers else [])
        out.append((name, base, tpl, pats))
    return out


def full_blocks(name, base, pats):
    """The family's full blocks (slabs, stairs and walls share their tops), without chiseled and pillar ones."""
    ids = [base] + [p.replace("{}", name) for p in pats]
    return ["mechanical:" + i for i in ids if not re.search(r"(_slab|_stairs|_wall|_pillar)$|^chiseled_", i)]


def planks():
    vanilla = json.loads(JAR.read("data/minecraft/tags/block/planks.json"))["values"]
    mod = json.loads((RES / "data/minecraft/tags/block/planks.json").read_text())["values"]
    ids = list(dict.fromkeys(ns_id(v) for v in vanilla + mod if not v.startswith("#")))
    wood = lambda i: i.split(":")[1][:-len("_planks")]
    return sorted(ids, key=lambda i: (WOOD_ORDER.index(wood(i)) if wood(i) in WOOD_ORDER else len(WOOD_ORDER), i))


def ground_slab_bases():
    src = (JAVA / "EarthBlocks.java").read_text(encoding="utf-8")
    return ["minecraft:" + v.lower() if v else "mechanical:" + m.lower()
            for v, m in re.findall(r'slab\("[a-z_]+", (?:\(\) -> Blocks\.([A-Z_]+)|([A-Z_]+))\)', src)]


def table():
    t, seen = [], set()

    def add(block, groups, style):
        block = ns_id(block)
        if block not in seen:
            seen.add(block)
            t.append((block, groups, style))

    ground = {ns_id(b) for b in GROUND_TOP + GROUND_LOW}
    mossy, bricks, tiles, cracked, cobbled, raw, polished = [], [], [], [], [], [], []
    for name, base, tpl, pats in stone_families():
        for b in full_blocks(name, base, pats):
            short = b.split(":")[1]
            if b in NOT_BLENDED or b in ground:
                continue
            if short.startswith("mossy_"):
                mossy.append(b)
            elif short.startswith("cracked_"):
                cracked.append(b)
            elif short.startswith("cobbled_"):
                cobbled.append(b)
            elif short.startswith(("polished_", "smooth_", "cut_")):
                polished.append(b)
            elif short.endswith("_tiles"):
                tiles.append(b)
            elif short.endswith("_bricks") or tpl == "BRICKS":
                bricks.append(b)
            else:
                raw.append(b)
    # living ground on top: grass creeps over everything it shares a group with, moss nearly as much
    add("grass_block", G | S, "noise")
    add("mechanical:turf", G, "noise")
    add("moss_block", G | S, "noise")
    for b in GROUND_TOP[3:]:
        add(b, G, "noise")
    # masonry: mossy over clean, bricks interlock over rough stone and tiles, cracked last
    for b in ["mossy_stone_bricks"] + mossy:
        add(b, S, "brick")
    for b in ["stone_bricks", "deepslate_bricks", "tuff_bricks", "mud_bricks", "bricks"] + bricks:
        add(b, S, "brick")
    for b in ["deepslate_tiles"] + tiles:
        add(b, S, "brick")
    for b in ["cracked_stone_bricks", "cracked_deepslate_bricks", "cracked_deepslate_tiles"] + cracked:
        add(b, S, "brick")
    # cobblestone is also ground (like gravel): its stones sit in dirt, coarse dirt and paths, and grass grows over it
    add("mossy_cobblestone", G | S, "cobble")
    add("cobblestone", G | S, "cobble")
    for b in ["cobbled_deepslate"] + cobbled + ["mechanical:fieldstone"]:
        add(b, S, "cobble")
    # loose stone between masonry and bedrock: it spills over rock and over dirt
    add("gravel", G | S, "noise")
    add("mechanical:beach_pebbles", G | S, "noise")
    for b in VANILLA_RAW + raw:
        add(b, S, "chunk")
    for b in VANILLA_POLISHED + polished:
        add(b, S, None)
    # bare ground, from loose and light to the trodden path at the bottom
    for b in GROUND_LOW:
        add(b, G, "noise")
    for b in ["mechanical:worn_grass", "mechanical:trodden_path", "dirt_path"]:
        add(b, G, None)
    # planks only meet planks
    for b in planks():
        add(b, P, "plank")
    return t


def check():
    """Every mod block of a blending kind has a row: stone family blocks, planks, ground slab bases, and by name anything
    that looks like planks, bricks, tiles, cobbled, polished, smooth or cut stone."""
    have = {b for b, _, _ in table()}
    want = set(planks()) | set(ground_slab_bases())
    for name, base, tpl, pats in stone_families():
        want.update(full_blocks(name, base, pats))
    for f in (RES / "assets/mechanical/blockstates").glob("*.json"):
        if re.search(r"(_planks|_bricks|_tiles)$|^(cobbled|polished|smooth|cut)_", f.stem) and not re.search(r"(_slab|_stairs|_wall)$|^chiseled_", f.stem):
            want.add("mechanical:" + f.stem)
    missing = sorted(want - have - NOT_BLENDED)
    assert not missing, "blocks without an edge blend row (add them to the table or NOT_BLENDED): " + ", ".join(missing)


# ---------------------------------------------------------------------------------------------------------------------
# assets: the mod's resources first (they override vanilla), then the patched jar
def asset(ns, path):
    p = RES / "assets" / ns / path
    if p.exists():
        return p.read_bytes()
    try:
        return JAR.read(f"assets/{ns}/{path}")
    except KeyError:
        return None


def split(i):
    return tuple(i.split(":", 1)) if ":" in i else ("minecraft", i)


def texture(tid):
    ns, p = split(tid)
    data = asset(ns, f"textures/{p}.png")
    return None if data is None else Image.open(io.BytesIO(data)).convert("RGBA")


def model_chain(mid):
    chain = []
    while mid and not mid.startswith("builtin/") and not mid.startswith("minecraft:builtin/"):
        ns, p = split(mid)
        data = asset(ns, f"models/{p}.json")
        if data is None:
            break
        j = json.loads(data)
        chain.append(j)
        mid = j.get("parent")
    return chain


def up_textures(mid):
    """Up-face textures of a model's full-size elements, in element order."""
    chain = model_chain(mid)
    tex = {}
    for m in reversed(chain):
        tex.update(m.get("textures", {}))
    elements = next((m["elements"] for m in chain if "elements" in m), [])

    def resolve(ref, depth=0):
        while ref.startswith("#") and depth < 10:
            ref = tex.get(ref[1:], "")
            depth += 1
        return ref if ":" in ref else ("minecraft:" + ref if ref else ref)

    out = []
    for e in elements:
        f = e.get("faces", {}).get("up")
        if f and e["from"][0] == 0 and e["from"][2] == 0 and e["to"][0] == 16 and e["to"][2] == 16:
            out.append(resolve(f["texture"]))
    return out


def block_models(block):
    ns, p = split(block)
    data = asset(ns, f"blockstates/{p}.json")
    if data is None:
        return []
    j = json.loads(data)
    models = []
    for key, v in j.get("variants", {}).items():
        if "snowy=true" in key:
            continue
        for m in v if isinstance(v, list) else [v]:
            if m["model"] not in models:
                models.append(m["model"])
    for part in j.get("multipart", []):
        a = part["apply"]
        for m in a if isinstance(a, list) else [a]:
            if m["model"] not in models:
                models.append(m["model"])
    return models


def top_keys(block):
    """Texture ids EdgeBlend sees as this block's top: the last full up face of each of its models."""
    keys = []
    for m in block_models(block):
        ups = up_textures(m)
        if ups and ups[-1] not in keys:
            keys.append(ups[-1])
    return keys


# ---------------------------------------------------------------------------------------------------------------------
# shared geometry. Pixel (x, y) of a tile: x east, y south. Edge "along" runs from end a (west/north) to end b.
DIRS = ["n", "e", "s", "w"]
CORNERS = {"ne": (16, 0), "se": (16, 16), "sw": (0, 16), "nw": (0, 0)}
XS, YS = np.meshgrid(np.arange(16) + 0.5, np.arange(16) + 0.5)  # pixel centres, [y, x]


def along_depth(d, x, y):
    if d == "n":
        return x, y
    if d == "s":
        return x, 16 - y
    if d == "w":
        return y, x
    return y, 16 - x


def edge_profile(ea, eb, rng, wobble):
    """Depth of the edge along it: from ea at end a to eb at end b, with a few smooth bumps that vanish at the ends."""
    a1, a2, ph = rng.uniform(-wobble, wobble * 1.3), rng.uniform(-wobble * 0.6, wobble * 0.6), rng.uniform(0, 2 * math.pi)
    a3, ph3 = rng.uniform(-wobble * 0.35, wobble * 0.35), rng.uniform(0, 2 * math.pi)
    return lambda s: ea + (eb - ea) * s + np.sin(np.pi * s) * (a1 + a2 * np.sin(2 * np.pi * s + ph) + a3 * np.sin(5 * np.pi * s + ph3))


def lum(img):
    a = np.asarray(img, dtype=float)
    return 0.299 * a[..., 0] + 0.587 * a[..., 1] + 0.114 * a[..., 2]


# ---------------------------------------------------------------------------------------------------------------------
# noise: ground
NOISE_K, NOISE_M = 3, 2
NOISE_DEPTH = [2.4, 3.4, 4.6]  # depth at a lattice corner, by its class
FRINGE = 2.6                   # width of the dithered band


def dither(p, rng, depth_over):
    r = rng.random((16, 16))
    m = r < p
    # stray specks a little past the fringe, as in loose soil
    m |= (depth_over > 0.6) & (depth_over < 3.2) & (rng.random((16, 16)) < 0.06)
    return m


def noise_edge(d, ea, eb, rng):
    al, dp = along_depth(d, XS, YS)
    t = edge_profile(ea, eb, rng, 1.25)(al / 16)
    p = np.clip((t - dp) / FRINGE + 0.5, 0, 1)
    p[dp < t - 1.3] = 1
    return dither(p, rng, dp - t)


def noise_corner(c, e, rng):
    cx, cy = CORNERS[c]
    r = np.hypot(XS - cx, YS - cy)
    th = np.arctan2(YS - cy, XS - cx)
    t = e * (0.95 + 0.12 * np.sin(3 * th + rng.uniform(0, 6.3)))
    p = np.clip((t - r) / FRINGE + 0.5, 0, 1)
    p[r < t - 1.3] = 1
    return dither(p, rng, r - t)


# ---------------------------------------------------------------------------------------------------------------------
# chunk: rough stone, whole chunks (Voronoi cells over the tile, or the texture's own stones for cobble)
CHUNK_K, CHUNK_M = 2, 2
CHUNK_DEPTH = [3.2, 5.0]


def voronoi(rng, spacing=3.4):
    seeds = []
    n = int(24 / spacing) + 2
    for i in range(n):
        for j in range(n):
            seeds.append((-4 + (i + rng.uniform(0.15, 0.85)) * spacing, -4 + (j + rng.uniform(0.15, 0.85)) * spacing))
    seeds = np.array(seeds)
    d = (XS[..., None] - seeds[:, 0]) ** 2 + (YS[..., None] - seeds[:, 1]) ** 2
    return seeds, d.argmin(-1)


def stones(img):
    """Cobble-like textures: label each stone (bright, connected) over a 3x3 tiling; returns labels and centroids."""
    L = lum(img)
    thr = np.percentile(L, 42)
    big = np.tile(L > thr, (3, 3))
    lab = -np.ones(big.shape, int)
    cents = []
    for y0 in range(48):
        for x0 in range(48):
            if big[y0, x0] and lab[y0, x0] < 0:
                stack, pts = [(y0, x0)], []
                lab[y0, x0] = len(cents)
                while stack:
                    y, x = stack.pop()
                    pts.append((y, x))
                    for dy, dx in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                        yy, xx = y + dy, x + dx
                        if 0 <= yy < 48 and 0 <= xx < 48 and big[yy, xx] and lab[yy, xx] < 0:
                            lab[yy, xx] = len(cents)
                            stack.append((yy, xx))
                pts = np.array(pts)
                cents.append((pts[:, 1].mean() - 16 + 0.5, pts[:, 0].mean() - 16 + 0.5, len(pts)))
    return lab[16:32, 16:32], cents


def chunk_select(d, ea, eb, rng, cx, cy):
    al, dp = along_depth(d, cx, cy)
    t = edge_profile(ea, eb, rng, 1.0)(np.clip(al / 16, 0, 1))
    return dp < t


def chunk_edge(d, ea, eb, rng, cobble):
    if cobble is not None:
        lab, cents = cobble
        c = np.array(cents)
        inc = chunk_select(d, ea, eb, rng, c[:, 0], c[:, 1])
        m = np.zeros((16, 16), bool)
        ok = lab >= 0
        m[ok] = inc[lab[ok]]
        return grow_dark(m, lab)
    seeds, cell = voronoi(rng)
    inc = chunk_select(d, ea, eb, rng, seeds[:, 0], seeds[:, 1])
    return inc[cell]


def chunk_corner(c, e, rng, cobble):
    cx, cy = CORNERS[c]
    if cobble is not None:
        lab, cents = cobble
        cc = np.array(cents)
        inc = np.hypot(cc[:, 0] - cx, cc[:, 1] - cy) < e * 0.9
        m = np.zeros((16, 16), bool)
        ok = lab >= 0
        m[ok] = inc[lab[ok]]
        return grow_dark(m, lab)
    seeds, cell = voronoi(rng)
    inc = np.hypot(seeds[:, 0] - cx, seeds[:, 1] - cy) < e * 0.95
    return inc[cell]


def grow_dark(m, lab):
    """Add the dark gaps round the chosen stones (1 px), so each stone keeps its outline."""
    out = m.copy()
    for y in range(16):
        for x in range(16):
            if lab[y, x] < 0 and not m[y, x]:
                near = [m[yy, xx] for yy in range(max(0, y - 1), min(16, y + 2)) for xx in range(max(0, x - 1), min(16, x + 2))]
                n_edge = (y == 0 or y == 15 or x == 0 or x == 15)
                out[y, x] = sum(near) >= 2 or (n_edge and sum(near) >= 1)
    return out


# ---------------------------------------------------------------------------------------------------------------------
# brick: the texture's courses and joints. Along a course one brick in BRICK_EVERY steps out (the variant is the block's
# position along the edge mod BRICK_EVERY, so a brick that crosses into the next block agrees on both sides).
BRICK_EVERY = 3
def runs(flags):
    """Cyclic runs of False in a boolean ring -> list of (start, length)."""
    n = len(flags)
    if all(not f for f in flags):
        return [(0, n)]
    start = next(i for i in range(n) if flags[i])
    out, i = [], 0
    while i < n:
        j = (start + i) % n
        if not flags[j]:
            k = i
            while k < n and not flags[(start + k) % n]:
                k += 1
            out.append(((start + i) % n, k - i))
            i = k
        else:
            i += 1
    return out


def brick_grid(img):
    """Mortar rows, courses (rows) and per course the joint columns. None when the texture has no clear grid."""
    L = lum(img)
    rm = L.mean(1)
    med = np.median(rm)
    mortar_rows = rm < med - max(0.45 * (med - rm.min()), 4)
    courses = [(s, n) for s, n in runs(list(mortar_rows)) if n >= 2]
    if len(courses) < 2 and not mortar_rows.any():
        return None
    out = []
    for s, n in courses:
        rows = [(s + i) % 16 for i in range(n)]
        cm = L[rows].mean(0)
        cmed = np.median(cm)
        joints = cm < cmed - max(0.45 * (cmed - cm.min()), 5)
        bricks = [(bs, bn) for bs, bn in runs(list(joints)) if bn >= 2]
        out.append((rows, joints, bricks))
    return mortar_rows, out


def brick_labels(grid):
    """Extended 48x48 plane (tile at 16..32): brick id per pixel, -1 mortar; and per brick (course index, start x)."""
    mortar_rows, courses = grid
    lab = -np.ones((48, 48), int)
    info = []
    for ty in range(3):
        for tx in range(3):
            for ci, (rows, joints, bricks) in enumerate(courses):
                for bs, bn in bricks:
                    bid = len(info)
                    info.append((ci, bs + (tx - 1) * 16, ty - 1, bn))
                    for r in rows:
                        for i in range(bn):
                            x = (bs + i)
                            lab[ty * 16 + r, (tx * 16 + x) % 48 if tx * 16 + x < 48 else (tx * 16 + x) - 48] = bid
    return lab, info


def brick_mask(grid, include):
    """include(bid, info) -> bool for bricks in the extended plane. Tile pixels of included bricks + the mortar along them."""
    lab, info = brick_labels(grid)
    inc = np.array([include(i, info[i]) for i in range(len(info))])
    full = np.zeros((48, 48), bool)
    ok = lab >= 0
    full[ok] = inc[lab[ok]]
    m = full[16:32, 16:32].copy()
    for y in range(16):
        for x in range(16):
            if lab[16 + y, 16 + x] < 0:  # mortar: straight above, below or beside a chosen brick, never past its corner
                m[y, x] = full[16 + y - 2:16 + y + 3, 16 + x].any() or full[16 + y, 16 + x - 2:16 + x + 3].any()
    return m


def brick_edge(d, v, grid):
    lab, info = brick_labels(grid)
    ys, xs = np.nonzero(lab >= 0)
    # each brick's pixel extent in the extended plane
    ext = {}
    for y, x in zip(ys, xs):
        b = lab[y, x]
        x0, x1, y0, y1 = ext.get(b, (99, -99, 99, -99))
        ext[b] = (min(x0, x), max(x1, x), min(y0, y), max(y1, y))
    courses = grid[1]

    def edge_course(top):
        # the course nearest the edge inside the tile: smallest (top) or largest (bottom) first row in the tile
        best = None
        for ci, (rows, _, _) in enumerate(courses):
            key = min(rows) if top else -max(rows)
            if best is None or key < best[0]:
                best = (key, ci)
        return best[1]

    def parity_ok(bid):
        ci, start, ty, bn = info[bid]
        starts = sorted({s for s, _ in courses[ci][2]})
        n = len(starts)
        tx = math.floor(start / 16)
        i = starts.index(start - 16 * tx)
        return ((v + tx) * n + i) % BRICK_EVERY == 0

    if d in ("w", "e"):
        def include(bid, inf):
            x0, x1, y0, y1 = ext[bid]
            if not (16 <= y0 <= 31 or 16 <= y1 <= 31):
                return False
            return (x1 < 16 or x0 < 16 <= x1) if d == "w" else (x0 > 31 or x0 <= 31 < x1)
        return brick_mask(grid, include)
    top = d == "n"
    ci_edge = edge_course(top)

    def include(bid, inf):
        x0, x1, y0, y1 = ext[bid]
        if top and y1 < 16 or not top and y0 > 31:
            return True
        return inf[0] == ci_edge and inf[2] == 0 and parity_ok(bid)
    return brick_mask(grid, include)


# ---------------------------------------------------------------------------------------------------------------------
# plank: boards and their end joints
def plank_grid(img):
    """Boards along x: seam rows and per board up to two joint columns. Transposed = boards run along y."""
    L = lum(img)
    for transposed, A in ((False, L), (True, L.T)):
        rm = A.mean(1)
        med = np.median(rm)
        seams = rm < med - max(0.45 * (med - rm.min()), 6)
        boards = [(s, n) for s, n in runs(list(seams)) if n >= 2]
        if len(boards) >= 2:
            out = []
            for s, n in boards:
                rows = [(s + i) % 16 for i in range(n)]
                cm = A[rows].mean(0)
                cmed = np.median(cm)
                joints = []
                for x in np.argsort(cm):
                    if cm[x] < cmed - 5 and all(min(abs(x - j), 16 - abs(x - j)) >= 4 for j in joints):
                        joints.append(int(x))
                    if len(joints) == 2:
                        break
                seam_rows = []
                r = (s + n) % 16
                while seams[r] and len(seam_rows) < 3:
                    seam_rows.append(r)
                    r = (r + 1) % 16
                out.append((rows, seam_rows, sorted(joints)))
            dark = np.asarray(img)[..., :3][list(np.nonzero(seams)[0])] if not transposed else np.asarray(img)[..., :3][:, list(np.nonzero(seams)[0])]
            return transposed, out, dark.reshape(-1, 3).mean(0)
    return None


def plank_edge(d, a, b, v, grid, rng):
    """Mask (16x16, board frame) plus the pixels to paint as a cut board end, for an edge given in the board frame.
    Across the grain (w/e) every other board runs on (v picks which), to its first joint (a = 0) or to a cut a board
    length further (a = 1). Along the grain (n/s) the board at the edge is the neighbour's wood over the stretch next to
    each end whose lattice corner says so (a, b: shared with the block beside, so the board carries on across the line),
    to a cut; with both the whole board, with neither a straight seam. EdgeBlend sets a or b itself when the block
    beside along the edge is the same wood, so the swapped board never stops short of it."""
    transposed, boards, _ = grid
    m = np.zeros((16, 16), bool)
    paint = np.zeros((16, 16), bool)

    def cut(rows, c, joints):
        if 0 < c < 15 and c not in joints:
            paint[rows, c] = True

    if d in ("w", "e"):
        for i, (rows, seam, joints) in enumerate(boards):
            if (i + v) % 2:
                continue
            band = rows + seam
            cand = [j for j in joints if 3 <= j <= 11]
            if d == "w":
                end = (cand[0] if cand else rng.randint(4, 7)) if a == 0 else rng.randint(8, 11)
                m[band, :end + 1] = True
            else:
                end = (cand[-1] if cand else rng.randint(8, 11)) if a == 0 else rng.randint(4, 7)
                m[band, end:] = True
            cut(rows, end, joints)
        return m, paint
    rows, seam, joints = min(boards, key=lambda bd: min(bd[0])) if d == "n" else max(boards, key=lambda bd: max(bd[0] + bd[1]))
    band = rows + seam
    if a and b:  # the board carries on through the whole block
        m[band, :] = True
    elif a:
        e = rng.randint(7, 11)
        m[band, :e + 1] = True
        cut(rows, e, joints)
    elif b:
        s = rng.randint(5, 9)
        m[band, s:] = True
        cut(rows, s, joints)
    return m, paint


# ---------------------------------------------------------------------------------------------------------------------
# sheets
def hash3(x, z, salt):
    """Same as EdgeBlend.hash (32-bit int maths)."""
    h = (x * 0x27D4EB2D) ^ (z * 0x165667B1) ^ (salt * 0x61C88647)
    h &= 0xFFFFFFFF
    h ^= h >> 15
    h = (h * 0x2C1B3C6D) & 0xFFFFFFFF
    h ^= h >> 12
    h = (h * 0x297A2D39) & 0xFFFFFFFF
    h ^= h >> 15
    return h & 0x7FFFFFFF


def style_shape(style):
    """(K, M, corners, select) per style."""
    return {"noise": (NOISE_K, NOISE_M, True, "hash"), "chunk": (CHUNK_K, CHUNK_M, True, "hash"),
            "cobble": (CHUNK_K, CHUNK_M, True, "hash"), "brick": (1, BRICK_EVERY, False, "parity"), "plank": (2, 2, False, "hash")}[style]


def make_tiles(tex_id, img, style):
    """List of 16x16 RGBA tiles in sheet order, or None when the texture does not fit the style."""
    img = img.crop((0, 0, 16, 16))
    K, M, corners, _ = style_shape(style)
    rng = random.Random(tex_id)
    nrng = np.random.default_rng(abs(hash3(len(tex_id), sum(map(ord, tex_id)), 7)))
    masks, paints = [], []
    if style == "noise":
        for d in DIRS:
            for a in range(K):
                for b in range(K):
                    for v in range(M):
                        masks.append(noise_edge(d, NOISE_DEPTH[a], NOISE_DEPTH[b], _NR(nrng, rng)))
        for c in CORNERS:
            for k in range(K):
                masks.append(noise_corner(c, NOISE_DEPTH[k], _NR(nrng, rng)))
    elif style in ("chunk", "cobble"):
        cobble = stones(img) if style == "cobble" else None
        for d in DIRS:
            for a in range(K):
                for b in range(K):
                    for v in range(M):
                        masks.append(chunk_edge(d, CHUNK_DEPTH[a], CHUNK_DEPTH[b], rng, cobble))
        for c in CORNERS:
            for k in range(K):
                masks.append(chunk_corner(c, CHUNK_DEPTH[k], rng, cobble))
    elif style == "brick":
        grid = brick_grid(img)
        if grid is None:
            return None
        for d in DIRS:
            for v in range(M):
                masks.append(brick_edge(d, v, grid))
    elif style == "plank":
        grid = plank_grid(img)
        if grid is None:
            return None
        transposed, _, dark = grid
        # board frame: when boards run along y, an n/s edge of the face is a w/e edge of the boards
        frame = {"n": "w", "e": "s", "s": "e", "w": "n"} if transposed else {d: d for d in DIRS}
        for d in DIRS:
            for a in range(K):
                for b in range(K):
                    for v in range(M):
                        m, p = plank_edge(frame[d], a, b, v, grid, rng)
                        if transposed:
                            m, p = m.T, p.T
                        masks.append(m)
                        paints.append(p)
    tiles = []
    src = np.asarray(img).copy()
    for i, m in enumerate(masks):
        t = src.copy()
        if paints:
            p = paints[i]
            t[p, :3] = (t[p, :3] * 0.35 + dark * 0.65).astype(np.uint8)
        t[..., 3] = np.where(m & (src[..., 3] > 0), 255, 0)
        tiles.append(Image.fromarray(t, "RGBA"))
    return tiles


class _NR:
    """numpy random + python random in one, so the noise masks get both."""
    def __init__(self, n, r):
        self.n, self.r = n, r

    def random(self, shape):
        return self.n.random(shape)

    def uniform(self, a, b):
        return self.r.uniform(a, b)


def sheet(tiles):
    cols = 16 if len(tiles) > 16 else len(tiles)
    rows = math.ceil(len(tiles) / cols)
    # sprites in the atlas are best kept power-of-two-ish; pad rows
    im = Image.new("RGBA", (cols * 16, rows * 16), (0, 0, 0, 0))
    for i, t in enumerate(tiles):
        im.paste(t, ((i % cols) * 16, (i // cols) * 16))
    return im, cols, rows


def build():
    entries = []
    tab = table()
    # a top texture shared by several blocks (smooth chalk has chalk's) belongs to the first block that frays, else the first
    owner = {}
    for block, groups, style in sorted(tab, key=lambda r: r[2] is None):
        for k in top_keys(block):
            owner.setdefault(k, block)
    for i, (block, groups, style) in enumerate(tab):
        strength = len(tab) - i
        keys = top_keys(block)
        if not keys:
            if style:
                print("skip (no model):", block)
            continue
        entry = {"block": block, "keys": [k for k in keys if owner[k] == block], "strength": strength, "groups": groups}
        if not entry["keys"]:
            continue  # same top as a block before it (turf is grass on top): it blends as that block
        if style:
            src = entry["keys"][0]
            img = texture(src)
            tiles = make_tiles(src, img, style) if img is not None and img.size[0] == 16 else None
            if tiles is None:
                print("no sheet (texture does not fit the style):", block, style)
            else:
                K, M, corners, select = style_shape(style)
                im, cols, rows = sheet(tiles)
                name = src.replace(":", "_").replace("block/", "").replace("/", "_")
                entry.update({"style": style, "sheet": f"mechanical:block/blend/{name}", "cols": cols, "rows": rows, "k": K, "m": M,
                              "corners": corners, "select": select, "join": style == "plank", "_image": im})
        entries.append(entry)
    return entries


def write(entries):
    OUT_TEX.mkdir(parents=True, exist_ok=True)
    for f in OUT_TEX.glob("*.png"):
        f.unlink()
    for e in entries:
        if "_image" in e:
            e["_image"].save(OUT_TEX / (e["sheet"].split("/")[-1] + ".png"))
    OUT_JSON.parent.mkdir(parents=True, exist_ok=True)
    clean = [{k: v for k, v in e.items() if not k.startswith("_")} for e in entries]
    OUT_JSON.write_text(json.dumps({"_comment": "generated by scripts/gen_blend_edges.py; strongest first", "blocks": clean}, indent=1) + "\n")
    lang = json.loads(LANG.read_text(encoding="utf-8"))
    lang.update({"mechanical.configuration.blendEdges": "Blend Block Edges",
                 "mechanical.configuration.blendEdges.tooltip": "Related neighbouring blocks (ground, stone, planks) fray into each other on their top faces instead of meeting in a straight seam"})
    LANG.write_text(json.dumps(lang, indent=2) + "\n", encoding="utf-8")
    print(len(entries), "blocks,", sum("_image" in e for e in entries), "sheets")


if __name__ == "__main__":
    # EdgeBlend.hash must pick the same tiles as the preview here: values from the Java method
    assert (hash3(1, 2, 3), hash3(-17, 40, 1), hash3(123456, -98765, 5)) == (509940096, 1477669019, 231247597)
    check()
    es = build()
    write(es)
