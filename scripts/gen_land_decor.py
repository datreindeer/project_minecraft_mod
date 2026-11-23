"""Placement data for the small things in the mountains, on open ground, in dry and in cold land (docs/terrain-plan.md
phase 4; Java features MountainDecor, OpenDecor and DryColdDecor in com.mechanical.worldgen): features, placed features
and biome modifiers. Each placed feature is (feature, biomes, placement); the features bail out on unsuitable spots (no
rock wall, not flat, wrong ground, too low for lasting snow), so the real rate is well below the tries.
Generator order: anywhere after gen_world_biomes.py (it only writes its own files). Run: python scripts/gen_land_decor.py"""
import json
from pathlib import Path

D = Path(__file__).resolve().parent.parent / "src/main/resources/data/mechanical"


def w(path, obj):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, indent=2) + "\n")


def mc(*names):
    return ["minecraft:" + n for n in names]


def mod(*names):
    return ["mechanical:" + n for n in names]


PEAKS = mc("stony_peaks", "jagged_peaks", "frozen_peaks", "snowy_slopes")
HIGHLAND = mc("grove", "meadow", "windswept_hills", "windswept_gravelly_hills", "windswept_forest", "windswept_savanna", "savanna_plateau") + mod(
    "slate_highlands", "limestone_karst", "marble_gorge", "soapstone_valley")
# valleys between the ranges: forest and taiga floors under the walls
VALLEYS = mc("taiga", "snowy_taiga", "old_growth_spruce_taiga", "old_growth_pine_taiga", "forest", "birch_forest", "plains", "snowy_plains",
             "cherry_grove") + mod("pine_heath", "deciduous_forest")
MOUNTAINS = PEAKS + HIGHLAND
CLIFFS = MOUNTAINS + mc("stony_shore") + mod("basalt_coast")
OPEN = mc("plains", "sunflower_plains", "meadow", "snowy_plains", "savanna") + mod("hazel_meadow", "pine_heath", "chalk_downs")
OUTCROP = mc("plains", "sunflower_plains", "meadow", "taiga", "old_growth_pine_taiga", "windswept_hills") + mod(
    "hazel_meadow", "pine_heath", "glacial_moraine", "slate_highlands")
BONES = mc("plains", "sunflower_plains", "meadow", "savanna", "savanna_plateau", "snowy_plains") + mod("mammoth_steppe", "chalk_downs", "hazel_meadow")
ANTS = mc("taiga", "old_growth_pine_taiga", "old_growth_spruce_taiga", "forest", "birch_forest") + mod("pine_heath")
ANTS_OPEN = mc("plains", "meadow") + mod("hazel_meadow")
PLAINS = mc("plains", "sunflower_plains")
LONE = PLAINS + mc("meadow") + mod("hazel_meadow")
FIRE = mc("plains", "sunflower_plains", "meadow", "taiga", "snowy_plains", "savanna") + mod("hazel_meadow", "pine_heath")
DRY = mc("desert", "badlands", "eroded_badlands", "wooded_badlands") + mod("gypsum_flats", "ochre_badlands")
SAVANNA = mc("savanna", "savanna_plateau", "windswept_savanna")
SAND = mc("desert") + mod("gypsum_flats")
ROCKY_DRY = mc("badlands", "eroded_badlands", "wooded_badlands", "desert", "savanna_plateau", "windswept_savanna") + mod("ochre_badlands", "marble_gorge")
SNOWY = mc("snowy_plains", "ice_spikes", "snowy_beach")
FROST = mc("snowy_plains", "snowy_taiga", "ice_spikes", "grove", "snowy_slopes") + mod("glacial_moraine", "mammoth_steppe")


def rarity(n):
    return [{"type": "minecraft:rarity_filter", "chance": n}]


def count(n):
    return [{"type": "minecraft:count", "count": n}]


# placed feature: (feature type, biomes, tries per chunk). OFF: switched off until they look right in close-up shots
# (code kept, feature type registered, no configured or placed feature or biome modifier): see docs/biomes.md for why.
OFF = ["rock_overhang", "ice_blocks", "talus", "fallen_rock", "snow_patch", "farm_walls", "juniper_in_plains", "bird_cliff", "root_cellar"]
PLACED = {
    # mountains
    "talus": ("talus", MOUNTAINS + VALLEYS, count(3)),
    "fallen_rock": ("fallen_rock", MOUNTAINS + VALLEYS, count(1)),
    "rock_arch": ("rock_arch", mc("stony_peaks", "windswept_hills", "windswept_gravelly_hills", "savanna_plateau", "badlands", "eroded_badlands")
                  + mod("slate_highlands", "limestone_karst", "marble_gorge", "ochre_badlands"), rarity(40)),
    "bird_cliff": ("bird_cliff", CLIFFS, count(2)),
    "valley_waterfall": ("valley_waterfall", MOUNTAINS + VALLEYS, count(4)),
    "snow_patch": ("snow_patch", MOUNTAINS, count(1)),
    # open ground
    "rock_outcrop": ("rock_outcrop", OUTCROP, rarity(7)),
    "bone_remains": ("bone_remains", BONES, rarity(14)),
    "anthill": ("anthill", ANTS, rarity(5)),
    "anthill_open": ("anthill", ANTS_OPEN, rarity(10)),
    "lone_tree": ("lone_tree", LONE, rarity(8)),
    "farm_walls": ("farm_walls", PLAINS, rarity(70)),
    "fire_ring": ("fire_ring", FIRE, rarity(40)),
    "root_cellar": ("root_cellar", LONE, rarity(60)),
    # dry and warm
    "dead_tree": ("dead_tree", DRY + SAVANNA, rarity(5)),
    "sand_skeleton": ("sand_skeleton", SAND, rarity(14)),
    "dry_creek": ("dry_creek", DRY + SAVANNA, rarity(6)),
    "rock_overhang": ("rock_overhang", ROCKY_DRY, count(2)),
    # cold
    "frost_split": ("frost_split", FROST, rarity(20)),
    "ice_blocks": ("ice_blocks", SNOWY, rarity(16)),
    "mammoth_tusks": ("mammoth_tusks", mod("mammoth_steppe"), rarity(8)),
}

# dev only: DECOR_TEST=1 switches everything on and places it far more often, for test worlds (never commit its output)
import os
if os.environ.get("DECOR_TEST"):
    OFF = []
    PLACED = {n: (f, b, rarity(t[0]["chance"] if t[0]["chance"] <= 7 else max(2, t[0]["chance"] // 4)) if t[0]["type"] == "minecraft:rarity_filter" else count(max(2, t[0]["count"])))
              for n, (f, b, t) in PLACED.items()}
    PLACED["farm_walls"] = PLACED["farm_walls"][:2] + (rarity(2),)  # plains are few in the test areas
for name, (feature, biomes, tries) in PLACED.items():
    if name in OFF:
        continue
    w(D / f"worldgen/feature/{feature}.json", {"type": f"mechanical:{feature}"})
    w(D / f"worldgen/placed_feature/{name}.json", {"feature": f"mechanical:{feature}", "placement": tries + [
        {"type": "minecraft:in_square"}, {"type": "minecraft:heightmap", "heightmap": "WORLD_SURFACE_WG"}, {"type": "minecraft:biome"}]})
    w(D / f"neoforge/biome_modifier/add_{name}.json", {"type": "neoforge:add_features", "biomes": sorted(set(biomes)),
        "features": f"mechanical:{name}", "step": "vegetal_decoration"})

# lone junipers on the open plain (gen_earth.py's juniper feature; it has its own clumps in taiga and hills)
if "juniper_in_plains" not in OFF:
    w(D / "worldgen/placed_feature/juniper_in_plains.json", {"feature": "mechanical:juniper", "placement": (count(2) if os.environ.get("DECOR_TEST") else rarity(5)) + [
        {"type": "minecraft:in_square"}, {"type": "minecraft:heightmap", "heightmap": "MOTION_BLOCKING_NO_LEAVES"}, {"type": "minecraft:biome"},
        {"type": "minecraft:block_predicate_filter", "predicate": {"type": "minecraft:all_of", "predicates": [
            {"type": "minecraft:replaceable"}, {"type": "minecraft:matching_block_tag", "tag": "minecraft:substrate_overworld", "offset": [0, -1, 0]}]}}]})
    w(D / "neoforge/biome_modifier/add_juniper_in_plains.json", {"type": "neoforge:add_features", "biomes": sorted(mc("plains", "meadow") + mod("hazel_meadow", "chalk_downs")),
        "features": "mechanical:juniper_in_plains", "step": "vegetal_decoration"})
for name in OFF:  # anything an earlier run wrote for a feature now off
    feature = PLACED[name][0] if name in PLACED else name
    if any(PLACED[n][0] == feature for n in PLACED if n not in OFF):
        feature = None  # still used by a placement that is on (anthill_open and anthill share one)
    for f in ([D / f"worldgen/feature/{feature}.json"] if feature else []) + [D / f"worldgen/placed_feature/{name}.json", D / f"neoforge/biome_modifier/add_{name}.json"]:
        f.unlink(missing_ok=True)
print(f"{len([n for n in PLACED if n not in OFF])} land decorations on, {len(OFF)} off")

# ---------------------------------------------------------------- the thin old bones (worldgen/OldBonesBlock)
import math

A = D.parent.parent / "assets/mechanical"
BONE = "mechanical:block/bone_pile_bone"
SIGN = 1  # the model rotation's sense: +1 turns a vertical bar toward +x (about z) or -z (about x) for a positive angle


def bar(a, b, t, axis, plane0, depth=None):
    """One straight piece of bone from a to b (2D points in the plane across axis), t thick, as a model element.
    axis z: points are (x, y) at z = plane0; axis x: points are (z, y) at x = plane0. Angles snap to 22.5 degrees."""
    (u1, v1), (u2, v2) = a, b
    du, dv = u2 - u1, v2 - v1
    L = math.hypot(du, dv) + t * 0.6  # a little longer so the joints close
    ang = math.degrees(math.atan2(du, dv))  # from vertical toward +u
    cu, cv = (u1 + u2) / 2, (v1 + v2) / 2
    lying = abs(ang) > 45
    rot = (ang - 90 if ang > 0 else ang + 90) if lying else ang
    rot = max(-45, min(45, round(rot / 22.5) * 22.5))
    d = depth if depth is not None else t
    hu, hv = (L / 2, t / 2) if lying else (t / 2, L / 2)
    if axis == "z":
        frm, to = [cu - hu, cv - hv, plane0 - d / 2], [cu + hu, cv + hv, plane0 + d / 2]
        r = {"origin": [cu, cv, plane0], "axis": "z", "angle": -SIGN * rot}
    else:
        frm, to = [plane0 - d / 2, cv - hv, cu - hu], [plane0 + d / 2, cv + hv, cu + hu]
        r = {"origin": [plane0, cv, cu], "axis": "x", "angle": SIGN * rot}
    frm = [round(x, 3) for x in frm]
    to = [round(x, 3) for x in to]
    w = lambda n: round(min(16, max(0.5, n)), 3)
    faces = {f: {"uv": [0, 0, w(t), w(L)], "texture": "#bone"} for f in ("north", "south", "east", "west")}
    faces.update({f: {"uv": [0, 0, w(t), w(t)], "texture": "#bone"} for f in ("up", "down")})
    return {"from": frm, "to": to, "rotation": r, "faces": faces}


def chain(start, steps, axis, plane0):
    """Bars along a polyline: start point, then (angle from vertical toward +u, length, thickness) per step."""
    out, (u, v) = [], start
    for ang, length, t in steps:
        nu, nv = u + math.sin(math.radians(ang)) * length, v + math.cos(math.radians(ang)) * length
        out.append(bar((u, v), (nu, nv), t, axis, plane0))
        u, v = nu, nv
    return out


def rib_model(dy):
    els = []
    for z in (4, 11.5):
        for side in (1, -1):  # left rib curves in toward +x, the right one toward -x
            x0 = 8 - side * 6
            els += chain((x0, dy), [(side * 4, 4, 1.6), (side * 22.5, 4, 1.5), (side * 45, 3.5, 1.4), (side * 67.5, 1.6, 1.3)], "z", z)
    els.append({"from": [7, max(0, dy), 0], "to": [9, max(0, dy) + 1.5, 16], "faces": {f: {"uv": [0, 0, 2, 16 if f in ("east", "west", "up", "down") else 1.5], "texture": "#bone"} for f in ("north", "south", "east", "west", "up", "down")}})
    return {"parent": "minecraft:block/block", "ambientocclusion": False, "textures": {"particle": BONE, "bone": BONE}, "elements": els}


TUSK_CURVE = [(0, 5, 7.2), (0, 5, 7.0), (0, 4, 6.9), (22.5, 3, 6.7), (22.5, 3, 6.5), (22.5, 3, 6.3), (45, 3, 6.0), (45, 3, 5.6),
              (67.5, 3, 5.1), (67.5, 3, 4.6), (90, 3, 4.0), (90, 2.5, 3.3), (112.5, 2.5, 2.6), (112.5, 2, 1.9)]


def tusk_model(low):
    """One curve for both blocks, split where it passes y 16: up out of the ground, bending forward (-z, north) and
    tapering to a point. Each piece is two crossed bars, so the tusk looks round rather than square."""
    els = []
    u, v = 0, -3
    for ang, length, t in TUSK_CURVE:
        nu, nv = u + math.sin(math.radians(ang)) * length, v + math.cos(math.radians(ang)) * length
        if ((v + nv) / 2 < 16) == low:
            dv = 0 if low else -16
            for thick, wide in ((t * 0.72, t), (t, t * 0.72)):
                e = bar((8 - u, v + dv), (8 - nu, nv + dv), thick, "x", 8, depth=wide)  # z = 8 - u
                e["rotation"]["angle"] = -e["rotation"]["angle"]  # u runs toward -z
                els.append(e)
        u, v = nu, nv
    return {"parent": "minecraft:block/block", "ambientocclusion": False, "textures": {"particle": BONE, "bone": BONE}, "elements": els}


Y = {"north": 0, "east": 90, "south": 180, "west": 270}
for name, models in (("rib_cage", {"false": rib_model(0), "true": rib_model(-6)}), ("mammoth_tusk", {"true": tusk_model(True), "false": tusk_model(False)})):
    for low, model in models.items():
        w(A / f"models/block/{name}{'_low' if low == 'true' else ''}.json", model)
    w(A / f"blockstates/{name}.json", {"variants": {f"facing={f},low={low}": {"model": f"mechanical:block/{name}{'_low' if low == 'true' else ''}", "y": y}
                                                    for f, y in Y.items() for low in ("false", "true")}})
    w(A / f"items/{name}.json", {"model": {"type": "minecraft:model", "model": f"mechanical:block/{name}{'_low' if name == 'mammoth_tusk' else ''}"}})


def drop(item, lo, hi, cond=None):
    e = {"type": "minecraft:item", "name": item}
    if hi > 1:
        e["modifier"] = [{"type": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": lo, "max": hi}}]
    return e


w(D / "loot_table/blocks/rib_cage.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [drop("minecraft:bone", 1, 3)]}],
                                          "random_sequence": "mechanical:blocks/rib_cage"})
low = lambda v: {"type": "minecraft:match_block", "predicate": {"blocks": "mechanical:mammoth_tusk", "state": {"low": v}}}
w(D / "loot_table/blocks/mammoth_tusk.json", {"type": "minecraft:block", "pools": [
    {"rolls": 1, "entries": [{**drop("mechanical:tusk", 1, 1), "condition": low("true")}]},
    {"rolls": 1, "entries": [{**drop("minecraft:bone", 1, 2), "condition": low("false")}]}], "random_sequence": "mechanical:blocks/mammoth_tusk"})
LANG = A / "lang/en_us.json"
text = LANG.read_text(encoding="utf-8")
if '"block.mechanical.rib_cage"' not in text:  # appended as text, so the rest of the file keeps its escaping
    text = text.rstrip().rstrip("}").rstrip() + ',\n  "block.mechanical.rib_cage": "Rib Cage",\n  "block.mechanical.mammoth_tusk": "Mammoth Tusk"\n}\n'
    json.loads(text)
    LANG.write_text(text, encoding="utf-8")
print("old bones models ok")

# ---------------------------------------------------------------- the anthill (worldgen/AnthillBlock): a dome of needles in fine steps
import random as _random

from PIL import Image


def needles_tex(seed):
    """Spruce needles and bits of twig heaped by wood ants: warm browns, short strokes every way, dark holes, a few ants."""
    rnd = _random.Random(seed)
    img = Image.new("RGB", (16, 16), (96, 68, 42))
    px = img.load()
    for _ in range(170):  # needles: 2-3 px strokes, horizontal, vertical or diagonal
        x, y = rnd.randrange(16), rnd.randrange(16)
        dx, dy = rnd.choice([(1, 0), (0, 1), (1, 1), (1, -1)])
        col = rnd.choice([(128, 92, 54), (146, 106, 60), (112, 78, 46), (164, 124, 72), (84, 58, 36), (138, 100, 52)])
        for k in range(rnd.choice([2, 2, 3])):
            px[(x + dx * k) % 16, (y + dy * k) % 16] = col
    for _ in range(9):  # the ants' holes
        x, y = rnd.randrange(16), rnd.randrange(16)
        px[x, y] = (46, 31, 21)
    for _ in range(3):  # ants
        px[rnd.randrange(16), rnd.randrange(16)] = (24, 14, 10)
    return img


TEXB = A / "textures/block"
needles_tex(20261123).save(TEXB / "anthill.png")


def dome(layers):
    """Stacked boxes (y0, y1, half width) round the block's middle, each face cut from the texture by its size."""
    els = []
    rnd = _random.Random(len(layers))
    for i, (y0, y1, h) in enumerate(layers):
        jx, jz = rnd.uniform(-0.5, 0.5), rnd.uniform(-0.5, 0.5)  # each course a little off centre: lumpy, not turned
        x0, x1, z0, z1 = 8 - h + jx, 8 + h + jx, 8 - h + jz, 8 + h + jz
        v = (y0 * 5) % 14  # each course its own row of the texture, or every course shows the same stripe
        side = [rnd.uniform(0, max(0, 16 - min(16, 2 * h))), v, 0, v + min(2, y1 - y0)]
        side[2] = side[0] + min(16, 2 * h)
        faces = {f: {"uv": [round(a, 2) for a in side], "texture": "#n"} for f in ("north", "south", "east", "west")}
        faces["up"] = {"uv": [max(0, x0), max(0, z0), min(16, x1), min(16, z1)], "texture": "#n"}
        els.append({"from": [round(x0, 2), y0, round(z0, 2)], "to": [round(x1, 2), y1, round(z1, 2)], "faces": faces})
    return {"parent": "minecraft:block/block", "ambientocclusion": True, "textures": {"particle": "mechanical:block/anthill", "n": "mechanical:block/anthill"},
            "elements": els}


def profile(r0, height, y_from=0, y_to=None, step=1.0):
    """A rounded dome r0 wide at its foot and height high, cut in steps of step px: the half width at each step's middle."""
    out, y = [], 0.0
    while y < height - 0.01:
        y1 = min(height, y + step)
        h = r0 * math.sqrt(max(0.0, 1 - ((y + y1) / 2 / height) ** 2)) ** 0.8
        if h >= 0.6:
            out.append((round(y, 2), round(y1, 2), round(h, 2)))
        y = y1
    return out


BIG = profile(20, 28, step=1.0)  # one dome over two blocks: the lower 16 px are the base, the rest the top
ANTHILL = {"small": profile(10, 12, step=1.0),
           "base": [(a, b, h) for a, b, h in BIG if b <= 16],
           "top": [(a - 16, b - 16, h) for a, b, h in BIG if a >= 16]}
for part, layers in ANTHILL.items():
    w(A / f"models/block/anthill_{part}.json", dome(layers))
w(A / "blockstates/anthill.json", {"variants": {f"part={p}": [{"model": f"mechanical:block/anthill_{p}", **({"y": y} if y else {})} for y in (0, 90, 180, 270)]
                                                for p in ANTHILL}})
w(A / "items/anthill.json", {"model": {"type": "minecraft:model", "model": "mechanical:block/anthill_small"}})
w(D / "loot_table/blocks/anthill.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": "mechanical:anthill"}],
                                                                              "condition": {"type": "minecraft:survives_explosion"}}], "random_sequence": "mechanical:blocks/anthill"})
text = LANG.read_text(encoding="utf-8")
if '"block.mechanical.anthill"' not in text:
    text = text.rstrip().rstrip("}").rstrip() + ',\n  "block.mechanical.anthill": "Anthill"\n}\n'
    json.loads(text)
    LANG.write_text(text, encoding="utf-8")

# the root cellar's store (OpenDecor.rootCellar): what was put by for the winter, mostly gone to seed and dust
w(D / "loot_table/chests/root_cellar.json", {"type": "minecraft:chest", "pools": [{"rolls": {"type": "minecraft:uniform", "min": 2, "max": 4}, "entries": [
    {"type": "minecraft:item", "name": n, "weight": wt, "modifier": [{"type": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": 1, "max": mx}}]}
    for n, wt, mx in (("mechanical:peas", 6, 4), ("mechanical:wild_carrot", 6, 3), ("mechanical:barley_grain", 5, 5), ("mechanical:flax_seeds", 4, 3),
                      ("minecraft:beetroot_seeds", 3, 3), ("mechanical:cordage", 2, 2), ("minecraft:bone_meal", 2, 2))]}],
    "random_sequence": "mechanical:chests/root_cellar"})
print("anthill and root cellar ok")
