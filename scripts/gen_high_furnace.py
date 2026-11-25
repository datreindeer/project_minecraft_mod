"""The High Furnace (docs/steel-furnace-plan.md, Steel Age; code HighFurnace.java and the classes it names), phases 0-2:
Silica Bricks, Banded Furnace Casing (with four heat glows), Firebox, Taphole, High Furnace Front (sight hatch, glowing
by heat), Great Bellows (static frame + the part models client/GreatBellowsRenderer works), the Clay Tuyere Mold, the items
(Crushed Quartzite, Unfired Silica Brick, Silica Brick, Steel Hoop, Bronze Tuyere Nozzle), recipes, loot, tags, lang.
The kiln firing (Unfired Silica Brick 1250°, the tuyere mold), the Stone Anvil (Quartzite -> 2 Crushed Quartzite) and the
anvil's Hoop pattern are in Java. Textures: gen_high_furnace_textures.py.
Generator order: right after gen_wildgrain.py (anything after it that rewrites block items / held models runs later anyway).
Run: python scripts/gen_high_furnace.py"""
import json
from pathlib import Path

from gen_metals import CLAY_METALS  # the Clay Mold "metal" states

ROOT = Path(__file__).resolve().parent.parent / "src/main/resources"
A, D = ROOT / "assets/mechanical", ROOT / "data"
M, B = "mechanical:", "mechanical:block/"
ROT = {"north": 0, "east": 90, "south": 180, "west": 270}
FACES = ("down", "up", "north", "south", "east", "west")
SIDES = ("north", "south", "east", "west")


def w(path, obj):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, indent=2) + "\n", encoding="utf-8")


def merge(path, obj):
    old = json.loads(path.read_text(encoding="utf-8")) if path.exists() else {}
    old.update(obj)
    path.write_text(json.dumps(old, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")


def tag_add(path, *values):
    old = json.loads(path.read_text(encoding="utf-8")) if path.exists() else {"values": []}
    old["values"] += [v for v in values if v not in old["values"]]
    w(path, old)


def face_uv(face, frm, to):
    (x0, y0, z0), (x1, y1, z1) = frm, to
    u, v = {"north": ((16 - x1, 16 - x0), (16 - y1, 16 - y0)), "south": ((x0, x1), (16 - y1, 16 - y0)),
            "east": ((16 - z1, 16 - z0), (16 - y1, 16 - y0)), "west": ((z0, z1), (16 - y1, 16 - y0)),
            "up": ((x0, x1), (z0, z1)), "down": ((x0, x1), (16 - z1, 16 - z0))}[face]

    def fix(a, b):
        lo, hi = min(a, b), max(a, b)
        d = 16 if hi <= 0 else -16 if lo >= 16 else -lo if lo < 0 else 16 - hi if hi > 16 else 0
        return a + d, b + d
    (u0, u1), (v0, v1) = fix(*u), fix(*v)
    return [u0, v0, u1, v1]


def box(frm, to, tex, faces=FACES, **extra):
    texs = tex if isinstance(tex, dict) else {f: tex for f in faces}
    return {"from": frm, "to": to, **extra, "faces": {f: {"uv": face_uv(f, frm, to), "texture": t} for f, t in texs.items()}}


def cube(tex, cull=True):
    """A full cube; tex maps face -> texture ref."""
    return {"from": [0, 0, 0], "to": [16, 16, 16], "faces": {f: {"texture": tex[f], **({"cullface": f} if cull else {})} for f in FACES}}


def glow_planes(faces, tex, light, inset=0.01):
    """Fullbright planes a hair outside the cube's faces (the glowing joints / grate), culled with the face."""
    planes = {"north": ([0, 0, -inset], [16, 16, -inset]), "south": ([0, 0, 16 + inset], [16, 16, 16 + inset]),
              "west": ([-inset, 0, 0], [-inset, 16, 16]), "east": ([16 + inset, 0, 0], [16 + inset, 16, 16])}
    return [{"from": planes[f][0], "to": planes[f][1], "light_emission": light, "shade": False,
             "faces": {f: {"uv": [0, 0, 16, 16], "texture": tex, "cullface": f}}} for f in faces]


def turn180(el):
    """The same element turned half round the block's vertical axis (for the item models)."""
    (x0, y0, z0), (x1, y1, z1) = el["from"], el["to"]
    swap = {"north": "south", "south": "north", "east": "west", "west": "east", "up": "up", "down": "down"}
    frm, to = [16 - x1, y0, 16 - z1], [16 - x0, y1, 16 - z0]
    return {**el, "from": frm, "to": to, "faces": {swap[f]: {**v, "uv": face_uv(swap[f], frm, to)} for f, v in el["faces"].items()}}


def model(name, elements, textures, parent="minecraft:block/block"):
    w(A / f"models/block/{name}.json", {"parent": parent, "textures": textures, "elements": elements})


def recipe(name, obj):
    w(D / f"mechanical/recipe/{name}.json", obj)


def shapeless(name, ingredients, count=1):
    recipe(name, {"type": "minecraft:crafting_shapeless", "category": "building", "ingredients": ingredients,
                  "result": {"id": M + name, "count": count}})


def item_def(name, mdl):
    w(A / f"items/{name}.json", {"model": {"type": "minecraft:model", "model": mdl}})


def loot(name):
    w(D / f"mechanical/loot_table/blocks/{name}.json", {"type": "minecraft:block", "pools": [{"rolls": 1,
        "condition": {"type": "minecraft:survives_explosion"}, "entries": [{"type": "minecraft:item", "name": M + name}]}],
        "random_sequence": f"mechanical:blocks/{name}"})


LIGHT = {1: 9, 2: 12, 3: 15}

# ---------------- Silica Bricks: the floor
w(A / "models/block/silica_bricks.json", {"parent": "minecraft:block/cube_all", "textures": {"all": B + "silica_bricks"}})
w(A / "blockstates/silica_bricks.json", {"variants": {"": {"model": B + "silica_bricks"}}})
item_def("silica_bricks", B + "silica_bricks")
loot("silica_bricks")

# ---------------- Banded Furnace Casing. Loose: a plain brick cube. In a formed furnace (HighFurnaceWallBlock.Casing) the
# Front sets part (by layer), facing (out) and end_cw / end_ccw (next to an empty corner). Models face north (out = -z):
#   base: rusticated stone with a plinth stepping 2 px out at the bottom;  hot: heat-stained brick, joints glow (heat 1-3);
#   plain / strap: sooty brick, a thin raised iron tie strap round strap courses;  upper / upper_strap: set back 1 px
#   (the shaft narrows) with a little sill closing the step;  top: set back, a corbelled iron rim 2.5 px out at the throat
#   whose inner face glows when hot.  Ends: a vertical iron corner strap on the side's end (wrapping round the corner),
#   with a bolt plate where a tie strap crosses it.
CT = {"particle": B + "banded_furnace_casing", "side": B + "banded_furnace_casing", "end": B + "banded_furnace_casing_top",
      "hot": B + "banded_furnace_casing_hot", "upper": B + "banded_furnace_casing_upper", "base": B + "furnace_base",
      "base_top": B + "furnace_base_top", "strap": B + "furnace_strap", "rim": B + "furnace_rim", "rim_glow": B + "furnace_rim_glow"}
CASING = CT  # the old name, used below
CASE_FACES = {"north": "#side", "south": "#side", "east": "#side", "west": "#side", "up": "#end", "down": "#end"}


def walls(side, z0=0):
    """A wall body from z0 (its outer face) to 16 with `side` on the four sides, brick tops."""
    el = box([0, 0, z0], [16, 16, 16], {"north": side, "south": side, "east": side, "west": side, "up": "#end", "down": "#end"})
    for f in ("south", "east", "west", "up", "down"):
        el["faces"][f]["cullface"] = f
    if z0 == 0:
        el["faces"]["north"]["cullface"] = "north"
    return el


def strap(z, y0=7, y1=9):
    """A thin iron tie strap across the outer face at depth z (its face 0.6 px proud)."""
    return box([0, y0, z - 0.6], [16, y1, z], {"north": "#strap", "up": "#strap", "down": "#strap"})


def sill():
    """Closes the 1 px step where the set-back upper shaft sits on the full-width wall below."""
    return box([0, 0, 0], [16, 0.5, 1], {"north": "#end", "up": "#end", "east": "#end", "west": "#end"})


PLINTH = box([0, 0, -2], [16, 4, 0], {"north": "#base", "up": "#base_top", "east": "#base", "west": "#base", "down": "#base"})
CORBEL = box([0, 11, -1.5], [16, 16, 1], {"north": "#rim", "up": "#rim", "down": "#rim", "east": "#rim", "west": "#rim"})
model("banded_furnace_casing", [cube(CASE_FACES)], CT)
model("casing_base", [walls("#base"), PLINTH], {**CT, "end": B + "furnace_base_top"})
model("casing_hot", [walls("#hot")], CT)
model("casing_hot_arch", [walls("#hot")], CT)
model("casing_plain", [walls("#side")], CT)
model("casing_strap", [walls("#side"), strap(0)], CT)
model("casing_upper", [walls("#upper", 1), sill()], CT)
model("casing_upper_strap", [walls("#upper", 1), sill(), strap(1)], CT)
TOP_ELS = [walls("#upper", 1), sill(), CORBEL]
model("casing_top", TOP_ELS, CT)
for h in (1, 2, 3):
    # the hot course: only the blocks over an arch glow, a few joints dull red (heat 2) to orange-red (heat 3)
    model(f"casing_hot_heat_{h}", [walls("#hot")], CT)
    model(f"casing_hot_arch_heat_{h}", [walls("#hot")] + (glow_planes(["north"], "#glow", {1: 3, 2: 5, 3: 8}[h])), {**CT, "glow": B + f"casing_glow_sparse_{h}"})
    # the throat: the rim's inner face (toward the inside, +z) glows, and its top
    model(f"casing_top_heat_{h}", TOP_ELS + [
        {"from": [0, 10, 16.01], "to": [16, 16, 16.01], "light_emission": LIGHT[h], "shade": False,
         "faces": {"south": {"uv": [0, 0, 16, 6], "texture": "#rim_glow", "cullface": "south"}}},
        {"from": [0, 16.01, 13], "to": [16, 16.01, 16], "light_emission": LIGHT[h], "shade": False,
         "faces": {"up": {"uv": [0, 0, 16, 3], "texture": "#rim_glow"}}}], CT)


def end_strap(z, ccw, top=False, plate=False):
    """The vertical corner strap at a side's end: x 0..1.5 on the counter-clockwise end (west when facing north), wrapping
    1.5 px onto the side face, and a bolt plate where a tie strap crosses."""
    y1 = 11 if top else 16
    x0, x1, sx = (0, 1.5, -0.6) if ccw else (14.5, 16, 16)
    els = [box([x0, 0, z - 0.6], [x1, y1, z], {"north": "#strap", "east": "#strap", "west": "#strap", "up": "#strap"}),
           box([sx, 0, z - 0.6], [sx + 0.6, y1, z + 1.5], {"west" if ccw else "east": "#strap", "south": "#strap", "up": "#strap"})]
    if plate:
        px0, px1 = (0, 3.5) if ccw else (12.5, 16)
        els.append({"from": [px0, 5.5, z - 1.1], "to": [px1, 10.5, z - 0.6],
                    "faces": {"north": {"uv": [4, 4, 12, 12], "texture": "#strap"}, "up": {"uv": [4, 4, 12, 5], "texture": "#strap"},
                              "down": {"uv": [4, 11, 12, 12], "texture": "#strap"}, "east": {"uv": [4, 4, 5, 12], "texture": "#strap"},
                              "west": {"uv": [11, 4, 12, 12], "texture": "#strap"}}})
    return els


for kind, z, top, plate in (("full", 0, False, False), ("full_plate", 0, False, True), ("inset", 1, False, False),
                            ("inset_plate", 1, False, True), ("top", 1, True, False)):
    for ccw in (True, False):
        model(f"casing_end_{kind}_{'ccw' if ccw else 'cw'}", end_strap(z, ccw, top, plate), CT)

PARTS = ["none", "base", "hot", "hot_arch", "plain", "strap", "upper", "upper_strap", "top"]
END_KIND = {"hot": "full", "hot_arch": "full", "plain": "full", "strap": "full_plate", "upper": "inset", "upper_strap": "inset_plate", "top": "top"}
mp = []
for f, y in ROT.items():
    rot = {"y": y} if y else {}
    for p in PARTS:
        for h in range(4):
            if p == "none":
                mdl = "banded_furnace_casing"
            else:
                mdl = f"casing_{p}" + (f"_heat_{h}" if h and p in ("hot", "hot_arch", "top") else "")
            mp.append({"when": {"part": p, "facing": f, "heat": str(h)}, "apply": {"model": B + mdl, **rot}})
    for p, kind in END_KIND.items():
        for side in ("ccw", "cw"):
            mp.append({"when": {"part": p, "facing": f, f"end_{side}": "true"}, "apply": {"model": B + f"casing_end_{kind}_{side}", **rot}})
w(A / "blockstates/banded_furnace_casing.json", {"multipart": mp})
item_def("banded_furnace_casing", B + "banded_furnace_casing")
loot("banded_furnace_casing")

# ---------------- the hearth openings: Front, Firebox and Taphole stand in the stone base under a recessed arch. Model
# facing north: stone piers x 0..3 and 13..16 (with the plinth under them), a stepped arch ring over x 3..13 from y 11,
# a stone threshold, and the door set 3 px back (z 3) in the opening. Hot: the arch's inner faces glow at the joints.
AT = {"particle": B + "furnace_base", "base": B + "furnace_base", "base_top": B + "furnace_base_top", "arch": B + "hearth_arch",
      "arch_glow": B + "hearth_arch_glow", "strap": B + "furnace_strap", "end": B + "furnace_base_top"}


def arch_body(front_tex):
    """The stone around a hearth opening: piers x 0..2 and 14..16, a stepped round arch over x 2..14 rising to 14, 4 px deep;
    the door plane at z 4; the plinth under the piers; an iron band round the arch."""
    els = [box([0, 0, 4], [16, 16, 16], {"north": front_tex, "south": "#base", "east": "#base", "west": "#base", "up": "#base_top", "down": "#base_top"})]
    for f in ("south", "east", "west", "up", "down"):
        els[0]["faces"][f]["cullface"] = f
    els += [box([0, 0, 0], [2, 16, 4], {"north": "#base", "east": "#arch", "west": "#base", "up": "#base_top", "down": "#base"}),
            box([14, 0, 0], [16, 16, 4], {"north": "#base", "west": "#arch", "east": "#base", "up": "#base_top", "down": "#base"}),
            box([2, 14, 0], [14, 16, 4], {"north": "#arch", "down": "#arch", "up": "#base_top"}),            # the crown
            box([2, 11, 0], [3.5, 14, 4], {"north": "#arch", "down": "#arch", "east": "#arch"}),             # haunches, stepped
            box([12.5, 11, 0], [14, 14, 4], {"north": "#arch", "down": "#arch", "west": "#arch"}),
            box([3.5, 12.5, 0], [5, 14, 4], {"north": "#arch", "down": "#arch", "east": "#arch"}),
            box([11, 12.5, 0], [12.5, 14, 4], {"north": "#arch", "down": "#arch", "west": "#arch"}),
            box([5, 13.4, 0], [6.5, 14, 4], {"north": "#arch", "down": "#arch", "east": "#arch"}),
            box([9.5, 13.4, 0], [11, 14, 4], {"north": "#arch", "down": "#arch", "west": "#arch"}),
            box([2, 0, 0], [14, 0.75, 4], {"north": "#base", "up": "#base_top"}),                         # threshold
            box([0, 0, -2], [2, 4, 0], {"north": "#base", "up": "#base_top", "east": "#base", "west": "#base"}),
            box([14, 0, -2], [16, 4, 0], {"north": "#base", "up": "#base_top", "east": "#base", "west": "#base"}),
            box([1.4, 14.3, -0.4], [14.6, 15.1, 0], {"north": "#strap", "down": "#strap", "up": "#strap"}),  # iron band over the arch
            box([1.4, 0, -0.4], [2.2, 14.3, 0], {"north": "#strap", "east": "#strap", "west": "#strap"}),
            box([13.8, 0, -0.4], [14.6, 14.3, 0], {"north": "#strap", "east": "#strap", "west": "#strap"})]
    return els


def arch_glow(light):
    """The arch's inner faces glowing (joints and hot edge): inner sides of the piers and the underside of the ring."""
    g = {"light_emission": light, "shade": False}
    return [{"from": [2.01, 0.75, 0], "to": [2.01, 11, 4], **g, "faces": {"east": {"uv": [12, 5, 16, 15], "texture": "#arch_glow"}}},
            {"from": [13.99, 0.75, 0], "to": [13.99, 11, 4], **g, "faces": {"west": {"uv": [0, 5, 4, 15], "texture": "#arch_glow"}}},
            {"from": [6.5, 13.99, 0], "to": [9.5, 13.99, 4], **g, "faces": {"down": {"uv": [6, 0, 10, 4], "texture": "#arch_glow"}}},
            {"from": [2, 10.99, 0], "to": [3.5, 10.99, 4], **g, "faces": {"down": {"uv": [2, 0, 4, 4], "texture": "#arch_glow"}}},
            {"from": [12.5, 10.99, 0], "to": [14, 10.99, 4], **g, "faces": {"down": {"uv": [12, 0, 14, 4], "texture": "#arch_glow"}}}]


def door_glow(tex, light):
    return {"from": [0, 0, 3.98], "to": [16, 16, 3.98], "light_emission": light, "shade": False,
            "faces": {"north": {"uv": [0, 0, 16, 16], "texture": tex}}}


def arched_block(name, front, front_glow, extra=(), hot_extra=lambda h: [], open_front=None, open_glow=None, has_open=True):
    """Models and blockstates of an arched hearth block; with open_front also open variants (the taphole while pouring)."""
    tex = {**AT, "front": B + front}
    opens = (False, True) if open_front else (False,)
    for op in opens:
        t0 = {**tex, "front": B + open_front} if op else dict(tex)
        suffix = "_open" if op else ""
        base_els = arch_body("#front") + list(extra)
        if op:
            t0["oglow"] = B + open_glow
            base_els = base_els + [door_glow("#oglow", 13)]
        model(name + suffix, base_els, t0)
        for h in (1, 2, 3):
            t = dict(t0)
            els = base_els + arch_glow(LIGHT[h])
            if front_glow:
                t["fglow"] = B + (f"{front_glow}_{h}" if front_glow.endswith("glow") and front_glow.startswith("firebox") else front_glow)
                els.append(door_glow("#fglow", LIGHT[h]))
            model(f"{name}{suffix}_heat_{h}", els + hot_extra(h), t)
    if open_front:
        variants = {f"facing={f},heat={h},open={str(op).lower()}": {"model": B + name + ("_open" if op else "") + (f"_heat_{h}" if h else ""), **({"y": y} if y else {})}
                    for f, y in ROT.items() for h in range(4) for op in (False, True)}
    elif has_open:
        variants = {f"facing={f},heat={h},open={o}": {"model": B + name + (f"_heat_{h}" if h else ""), **({"y": y} if y else {})}
                    for f, y in ROT.items() for h in range(4) for o in ("false", "true")}
    else:
        variants = {f"facing={f},heat={h}": {"model": B + name + (f"_heat_{h}" if h else ""), **({"y": y} if y else {})}
                    for f, y in ROT.items() for h in range(4)}
    w(A / f"blockstates/{name}.json", {"variants": variants})
    item_def(name, B + name)
    loot(name)


arched_block("firebox", "firebox_front", "firebox_front_glow")
# the taphole's lip: a short iron tongue under the hole from the door out to the face (a trough joined straight to the
# taphole takes the metal running off it; client/TroughRenderer draws the run), on a little prop off the threshold
TAP_LIP = [box([5.5, 4.4, 0], [10.5, 5.4, 4], "#strap"), box([5.5, 5.4, 0], [6.5, 6.3, 4], "#strap"), box([9.5, 5.4, 0], [10.5, 6.3, 4], "#strap"),
           box([7.5, 0.75, 0.6], [8.5, 4.4, 1.6], "#strap")]
arched_block("taphole", "taphole_front", None, TAP_LIP, open_front="taphole_front_open", open_glow="taphole_glow")
# the Front: the hatch cover hangs open on its hinge in the recess, the handle bar stands off the door
FRONT_EXTRA = [
    {"from": [11, 7, 3.4], "to": [17, 13, 3.9], "rotation": {"angle": -45, "axis": "y", "origin": [11, 10, 3.65]},
     "faces": {"north": {"uv": [5, 3, 11, 9], "texture": "#hatch"}, "south": {"uv": [5, 3, 11, 9], "texture": "#hatch"},
               "east": {"uv": [5, 3, 6, 9], "texture": "#hatch"}, "west": {"uv": [10, 3, 11, 9], "texture": "#hatch"},
               "up": {"uv": [5, 3, 11, 4], "texture": "#hatch"}, "down": {"uv": [5, 8, 11, 9], "texture": "#hatch"}}},
    box([10.6, 9, 3.1], [11.4, 11, 4], "#strap"),                                             # hinge knuckle
    box([3.6, 4, 2.4], [4.6, 10, 3.2], "#strap"), box([3.6, 4.5, 3.2], [4.6, 5.5, 4], "#strap"), box([3.6, 8.5, 3.2], [4.6, 9.5, 4], "#strap")]
AT_FRONT = {"hatch": B + "high_furnace_hatch", "hglow": B + "high_furnace_front_glow"}
AT.update(AT_FRONT)
arched_block("high_furnace_front", "high_furnace_front", None, FRONT_EXTRA, has_open=False, hot_extra=lambda h: [
    {"from": [5, 7, 3.98], "to": [11, 13, 3.98], "light_emission": LIGHT[h], "shade": False,
     "faces": {"north": {"uv": [5, 3, 11, 9], "texture": "#hglow"}}}])

# ---------------- Great Bellows (nozzle to the north): a GREAT wedge bellows two blocks long, lying on a timber trestle
# that reaches back into the block behind (keep it clear), its iron-shod bronze nozzle going straight into the Firebox's
# tuyere. The wheel's axle runs across the block's middle (y 8) under the bellows; a crank on its free end works a long
# pitman rod up to a rocker beam on a tall post at the side, whose back arm lifts the top board by a hanger: each turn of
# the wheel the bellows opens to about a block high and is pressed shut. Static (this model): trestle, axle bearings,
# bottom board, nozzle. Moving (client/GreatBellowsRenderer): axle, crank, pitman, rocker post and beam, hanger, the top
# board on its hinge at the nozzle end and the pleated leather between the boards.
BT = {"particle": B + "great_bellows_board", "board": B + "great_bellows_board", "leather": B + "great_bellows_leather",
      "frame": B + "great_bellows_frame", "end": B + "great_bellows_frame_end", "bronze": B + "great_bellows_nozzle", "cam": B + "great_bellows_cam"}
POST = {"north": "#frame", "south": "#frame", "east": "#frame", "west": "#frame", "up": "#end", "down": "#end"}
RAIL = {"north": "#end", "south": "#end", "east": "#frame", "west": "#frame", "up": "#frame", "down": "#frame"}
static = []
for z0 in (1, 14.5, 28.5):                                                               # three pairs of legs along it
    static += [box([1, 0, z0], [3, 10, z0 + 2.5], POST), box([13, 0, z0], [15, 10, z0 + 2.5], POST)]
static += [box([0.5, 8, 0.5], [3, 10, 31.5], RAIL), box([13, 8, 0.5], [15.5, 10, 31.5], RAIL),   # the side rails
           box([3, 1.5, 15], [13, 3, 16.5], "#frame"),                                     # a stretcher
           box([0, 0, 6.5], [1.2, 10, 9.5], POST), box([14.8, 0, 6.5], [16, 10, 9.5], POST),   # axle bearing posts
           box([-0.2, 6, 6.1], [1.4, 10, 6.6], "#cam"), box([14.6, 6, 6.1], [16.2, 10, 6.6], "#cam"),
           box([1.5, 10, 2], [14.5, 11.5, 30], "#board"),                                  # the bottom board, thick
           box([2, 11.5, 0.4], [14, 12, 2.2], "#cam"),                                     # iron shoe at the mouth
           box([5, 10.2, 0.2], [11, 14.8, 2.2], "#bronze"),                                # the mouth
           box([6, 10.8, -4.5], [10, 14.2, 0.2], "#bronze"),                               # the nozzle into the tuyere
           box([5.6, 10.4, -1.8], [10.4, 14.6, -1.2], "#cam"), box([5.6, 10.4, -4.9], [10.4, 14.6, -4.5], "#cam")]  # iron bands
model("great_bellows", static, BT)
w(A / "blockstates/great_bellows.json", {"variants": {
    f"facing={f}": {"model": B + "great_bellows", **({"y": y} if y else {})} for f, y in ROT.items()}})
loot("great_bellows")
# moving parts. Lid and pleats: hinge at model (2, 8, -4), 28 long (the renderer shifts them so the hinge is at the pose).
model("great_bellows_lid", [box([1.5, 8, -4], [14.5, 9.5, 24], "#board"),
                            box([1.3, 9.5, 1], [14.7, 10, 2.3], "#cam"), box([1.3, 9.5, 11], [14.7, 10, 12.3], "#cam"),
                            box([1.3, 9.5, 21], [14.7, 10, 22.3], "#cam"),                    # iron bands across
                            box([14.5, 8.5, 21], [17.5, 9.5, 23], "#cam"), box([-1.5, 8.5, 21], [1.5, 9.5, 23], "#cam")], BT)  # hanger brackets
model("great_bellows_pleat", [box([1.0, 8, -3.6], [15.0, 9.2, 23.4], "#leather")], BT)     # a fold standing out past the boards
model("great_bellows_pleat_in", [box([2.0, 8, -3.8], [14.0, 9.2, 23.0], "#leather")], BT)   # a fold tucked in
ROD = {"north": "#end", "south": "#end", "east": "#frame", "west": "#frame", "up": "#frame", "down": "#frame"}
model("great_bellows_shaft", [box([6.7, 6.7, 0], [9.3, 9.3, 16], ROD)], BT)                 # the axle (along z here)
model("great_bellows_crank", [box([5.6, 5.6, 7.4], [10.4, 10.4, 8.6], "#cam"),             # crank web, its pin 3 px out (+y)
                              box([7.2, 8, 7.4], [8.8, 11.6, 8.6], "#cam")], BT)
model("great_bellows_pin", [box([7, 7, 6.6], [9, 9, 9.4], "#cam"), box([7.4, 7.4, 6.2], [8.6, 8.6, 9.8], "#bronze")], BT)  # a pin through an eye
model("great_bellows_pitman", [box([7.4, 8, 7.4], [8.6, 24, 8.6], ROD),                     # a rod 16 long up from its foot (scaled)
                               box([7, 7.3, 7], [9, 8.7, 9], "#cam"), box([7, 23.3, 7], [9, 24.7, 9], "#cam")], BT)
model("great_bellows_beam", [box([7.2, 7, -4], [8.8, 9, 22], ROD),                         # the rocker beam, pivot at its centre
                             box([6.8, 6.6, 7], [9.2, 9.4, 9], "#cam")], BT)
for side, x0, x1 in (("r", 15, 17.4), ("l", -1.4, 1)):                                     # the tall rocker post at the side
    model(f"great_bellows_post_{side}", [box([x0, 0, 14.6], [x1, 24, 17.4], POST),
                                         box([x0 - 0.3, 22, 14.2], [x1 + 0.3, 24.5, 17.8], "#cam")], BT)
for p in ("great_bellows_lid", "great_bellows_pleat", "great_bellows_pleat_in", "great_bellows_shaft", "great_bellows_crank", "great_bellows_pin",
          "great_bellows_pitman", "great_bellows_beam", "great_bellows_post_r", "great_bellows_post_l"):
    item_def(p, B + p)
item_def("great_bellows", B + "great_bellows")

# ---------------- the Clay Tuyere Mold (as gen_bloomery.py's molds, bronze only)
s = "tuyere"
base = {"particle": B + "kiln_clay_fired", "side": B + "kiln_clay_fired", "top": B + f"clay_{s}_mold_top"}
slab = {"from": [1, 0, 1], "to": [15, 3, 15], "faces": {
    "up": {"texture": "#top", "uv": [1, 1, 15, 15]}, "down": {"texture": "#side", "cullface": "down"},
    **{d: {"texture": "#side", "uv": [1, 13, 15, 16]} for d in SIDES}}}
w(A / f"models/block/clay_{s}_mold.json", {"parent": "minecraft:block/block", "textures": base, "elements": [slab]})
for fill in ("molten", "solid"):
    metal = {"from": [1, 3.01, 1], "to": [15, 3.01, 15], "shade": fill == "solid", **({"light_emission": 12} if fill == "molten" else {}),
             "faces": {"up": {"texture": "#metal", "uv": [1, 1, 15, 15]}}}
    w(A / f"models/block/clay_{s}_mold_{fill}_bronze.json", {"parent": "minecraft:block/block",
        "textures": {**base, "metal": B + f"clay_{s}_mold_bronze_{fill}"}, "elements": [slab, metal]})
w(A / f"blockstates/clay_{s}_mold.json", {"variants": {
    f"fill={fill},metal={m},uses={u}": {"model": B + f"clay_{s}_mold" + ("" if fill == "empty" else f"_{fill}_bronze")}
    for fill in ("empty", "molten", "solid") for m in CLAY_METALS for u in range(5)}})
item_def(f"clay_{s}_mold", B + f"clay_{s}_mold")
w(D / f"mechanical/loot_table/blocks/clay_{s}_mold.json", {"type": "minecraft:block", "pools": [{"rolls": 1,
    "condition": {"type": "minecraft:survives_explosion"},
    "entries": [{"type": "minecraft:item", "name": M + f"clay_{s}_mold",
                 "modifier": [{"type": "minecraft:copy_state", "block": M + f"clay_{s}_mold", "properties": ["uses"]}]}]}],
    "random_sequence": f"mechanical:blocks/clay_{s}_mold"})

# ---------------- Pouring Spout (pours north; its back flange against the Taphole at z 16): a short riveted steel trough
# with raised sides, its floor ending at z 9 and its lip turned down just past the middle of the block below (the
# stream curls over it and lands 1 px past the middle); a lever on the right, up when shut, thrown forward when open;
# pouring, the metal in the trough glows (the falling stream is client/PouringSpoutRenderer).
ST = {"particle": B + "pouring_spout_steel", "steel": B + "pouring_spout_steel", "lever": B + "pouring_spout_lever", "glow": B + "taphole_glow"}
SPOUT = [box([3, 3, 19], [13, 11, 20], "#steel"),                                    # flange bolted to the taphole frame in its recess
         box([5, 4.5, 9], [11, 6, 19], "#steel"),                                    # trough floor
         box([4, 4.5, 9], [5, 8.5, 19], "#steel"), box([11, 4.5, 9], [12, 8.5, 19], "#steel"),   # sides
         box([5, 4.2, 8.6], [11, 5.4, 9.4], "#steel"),                                # the lip, turned down
         box([4.5, 3.5, 10.5], [11.5, 4.5, 12], "#steel"), box([4.5, 3.5, 13.5], [11.5, 4.5, 15], "#steel"),  # straps under it
         box([12, 6, 11.5], [13, 7, 12.5], "#lever")]                                 # lever pivot
LEVER_SHUT = [box([12.2, 6.5, 11.6], [12.9, 13, 12.4], "#lever")]
LEVER_OPEN = [{"from": [12.2, 6.5, 11.6], "to": [12.9, 13, 12.4], "rotation": {"angle": -45, "axis": "x", "origin": [12.5, 6.5, 12]},
               "faces": {f: {"uv": [7, 2, 8, 9], "texture": "#lever"} for f in FACES}}]
GLOW = [{"from": [5, 6.05, 9], "to": [11, 6.05, 19], "light_emission": 15, "shade": False,
         "faces": {"up": {"uv": [5, 4, 11, 12], "texture": "#glow"}}}]
def shorten(els):
    """The spout for a trough's end: nothing reaches past the block's back face."""
    out = []
    for e in els:
        e = {**e, "from": list(e["from"]), "to": list(e["to"])}
        if e["from"][2] >= 16:
            e["from"][2], e["to"][2] = 15, 16
        e["to"][2] = min(e["to"][2], 16)
        out.append(e)
    return out


for short in (False, True):
    suf = "_short" if short else ""
    fix = shorten if short else (lambda e: e)
    model("pouring_spout" + suf, fix(SPOUT + LEVER_SHUT), ST)
    model("pouring_spout_open" + suf, fix(SPOUT + LEVER_OPEN), ST)
    model("pouring_spout_pouring" + suf, fix(SPOUT + LEVER_OPEN + GLOW), ST)
w(A / "blockstates/pouring_spout.json", {"variants": {
    f"facing={f},open={o},pouring={po},short={sh}": {"model": B + "pouring_spout" + ("_pouring" if po == "true" and o == "true" else "_open" if o == "true" else "")
                                                     + ("_short" if sh == "true" else ""), **({"y": y} if y else {})}
    for f, y in ROT.items() for o in ("false", "true") for po in ("false", "true") for sh in ("false", "true")}})
item_def("pouring_spout", B + "pouring_spout")
loot("pouring_spout")
recipe("pouring_spout", {"type": "minecraft:crafting_shaped", "category": "misc", "pattern": ["P P", " P "],
                         "key": {"P": M + "steel_plate"}, "result": {"id": M + "pouring_spout"}})
tag_add(D / "minecraft/tags/block/mineable/pickaxe.json", M + "pouring_spout")
merge(A / "lang/en_us.json", {"block.mechanical.pouring_spout": "Pouring Spout",
    "message.mechanical.pouring_spout.open": "Spout open: it pours the chosen layer into what stands under its lip",
    "message.mechanical.pouring_spout.closed": "Spout shut",
    "message.mechanical.taphole.open": "Plug knocked out: the chosen layer runs into the trough at the taphole's lip",
    "message.mechanical.taphole.plugged": "Taphole plugged with clay",
    "message.mechanical.taphole.spout": "The Pouring Spout's lever opens and shuts this taphole",
    "message.mechanical.pouring_spout.too_hot": "Too hot for clay: a Clay Mold takes nothing hotter than 1100°"})

def gate_parts(els, gates, floor):
    """Splits a cast's rim at its side gates (side -> x0 x1 z0 z1, cut down to {floor}): the elements that always stand,
    and per side the pieces that fill that gate when no trough joins there (ChannelFlow.connect opens it)."""
    cuts = sorted({v for g in gates.values() for v in g})
    base, shut = [], {side: [] for side in gates}
    for e in els:
        (x0, y0, z0), (x1, y1, z1) = e["from"], e["to"]
        if y1 <= floor:
            base.append(e)
            continue
        tex = {f: v["texture"] for f, v in e["faces"].items()}
        xs = [x0] + [c for c in cuts if x0 < c < x1] + [x1]
        zs = [z0] + [c for c in cuts if z0 < c < z1] + [z1]
        ys = [y0] + ([floor] if y0 < floor < y1 else []) + [y1]
        for i in range(len(xs) - 1):
            for k in range(len(zs) - 1):
                for j in range(len(ys) - 1):
                    a, b = [xs[i], ys[j], zs[k]], [xs[i + 1], ys[j + 1], zs[k + 1]]
                    side = next((sd for sd, (gx0, gx1, gz0, gz1) in gates.items()
                                 if a[1] >= floor and gx0 <= a[0] and b[0] <= gx1 and gz0 <= a[2] and b[2] <= gz1), None)
                    (shut[side] if side else base).append(box(a, b, tex))
    return base, shut


def gated_cast(name, els, tex, gates, floor, lip=None, item=True):
    """A cast's models and multipart: the rim with its gates cut, a shut piece per side, an open side's lip (north, turned)."""
    base, shut = gate_parts(els, gates, floor)
    model(name, base, tex)
    parts = [{"apply": {"model": B + name}}]
    for side in gates:
        model(f"{name}_shut_{side}", shut[side], tex)
        parts.append({"when": {side: "false"}, "apply": {"model": B + f"{name}_shut_{side}"}})
        if lip:
            parts.append({"when": {side: "true"}, "apply": {"model": B + f"{name}_lip", **({"y": ROT[side]} if ROT[side] else {})}})
    if lip:
        model(f"{name}_lip", lip, tex)
    w(A / f"blockstates/{name}.json", {"multipart": parts})
    if item:
        model(f"{name}_inventory", base + [e for side in gates for e in shut[side]], tex)
        item_def(name, B + f"{name}_inventory")


# the iron molds' gates: a 6 px notch through the 1.5 px rim (x 5..11) down to the plate, over a 1 px iron lip that
# bridges the gap to the block edge, its cheeks in line with a trough's walls
IRON_GATES = {"north": (5, 11, 1, 2.5), "south": (5, 11, 13.5, 15), "west": (1, 2.5, 5, 11), "east": (13.5, 15, 5, 11)}
IRON_LIP = [box([5, 0, 0], [11, 1.6, 1], "#side"), box([3, 0, 0], [5, 4, 1], "#side"), box([11, 0, 0], [13, 4, 1], "#side")]

# ---------------- Iron Ingot Mold (phase 4b/5): a cast-iron plate with four ingot pockets that never cracks; its melt
# is drawn by client/IngotMoldRenderer. Cast from pig iron in a Molding Flask with the Ingot Mold pattern.
IM = {"particle": B + "iron_mold_side", "top": B + "iron_ingot_mold_top", "side": B + "iron_mold_side"}
# real pockets (x/z 2.5..7.5 and 8.5..13.5, floor 1.6, rim 4) so the metal can be seen rising in them (IngotMoldBlock.POCKETS)
SIDES4 = {"north": "#side", "south": "#side", "east": "#side", "west": "#side"}
MOLD = [box([1, 0, 1], [15, 1.6, 15], {"up": "#top", "down": "#side", **SIDES4})]
for x0, x1 in ((1, 2.5), (7.5, 8.5), (13.5, 15)):                                     # rims and the middle bar along z
    MOLD.append(box([x0, 1.6, 1], [x1, 4, 15], {"up": "#top", **SIDES4}))
for z0, z1 in ((1, 2.5), (7.5, 8.5), (13.5, 15)):                                     # and across x, between them
    for xa, xb in ((2.5, 7.5), (8.5, 13.5)):
        MOLD.append(box([xa, 1.6, z0], [xb, 4, z1], {"up": "#top", **SIDES4}))
gated_cast("iron_ingot_mold", MOLD, IM, IRON_GATES, 1.6, IRON_LIP)
loot("iron_ingot_mold")
(D / "mechanical/recipe/iron_ingot_mold.json").unlink(missing_ok=True)  # cast in a Molding Flask now (phase 4b)
tag_add(D / "minecraft/tags/block/mineable/pickaxe.json", M + "iron_ingot_mold")
merge(A / "lang/en_us.json", {"block.mechanical.iron_ingot_mold": "Iron Ingot Mold", "item.mechanical.pig_iron_ingot": "Pig Iron Ingot",
    "message.mechanical.ingot_mold.molten": "Still molten: wait until it has set"})

# ---------------- Refractory Trough and Trough Gate (phase 4): the Clay Channel's shape (floor 0..2, walls to 5, the
# channel 6 wide) in an iron trough lined with silica brick: the same multipart (middle, arm, end wall, slope) so it joins
# and slopes the same way. The gate: a straight trough with an iron frame over its middle and a sluice plate that is
# down (shut) or lifted (open).
TT = {"particle": B + "refractory_trough_outside", "out": B + "refractory_trough_outside", "lin": B + "refractory_trough_lining",
      "gate": B + "trough_gate", "lever": B + "pouring_spout_lever", "plate": B + "furnace_strap"}
OUTF = {f: "#out" for f in FACES}
RIM = {**OUTF, "up": "#lin"}
T_MIDDLE = [box([3, 0, 3], [13, 2, 13], {**OUTF, "up": "#lin"}),
            box([3, 2, 3], [5, 5, 5], RIM), box([11, 2, 3], [13, 5, 5], RIM),
            box([3, 2, 11], [5, 5, 13], RIM), box([11, 2, 11], [13, 5, 13], RIM)]
T_ARM = [box([3, 0, 0], [13, 2, 3], {**OUTF, "up": "#lin"}),
         box([3, 2, 0], [5, 5, 3], {**RIM, "east": "#lin"}),
         box([11, 2, 0], [13, 5, 3], {**RIM, "west": "#lin"}),
         box([2.6, 0.5, 0.6], [13.4, 1.5, 1.4], "#out")]                          # a rivet band at the joint
T_END = [box([5, 2, 3], [11, 5, 5], {**RIM, "south": "#lin"})]
T_TILT = {"origin": [8, 8, 8], "axis": "x", "angle": 45, "rescale": True}
T_SLOPE = [box([3, 6, 0], [13, 8, 16], {**OUTF, "up": "#lin"}, rotation=T_TILT),
           box([3, 8, 0], [5, 11, 16], {**OUTF, "east": "#lin"}, rotation=T_TILT),
           box([11, 8, 0], [13, 11, 16], {**OUTF, "west": "#lin"}, rotation=T_TILT)]
for name, els in (("refractory_trough_middle", T_MIDDLE), ("refractory_trough_arm", T_ARM), ("refractory_trough_end", T_END),
                  ("refractory_trough_slope", T_SLOPE)):
    model(name, els, TT)
tparts = [{"when": {"slope": "none"}, "apply": {"model": B + "refractory_trough_middle"}}]
for side, y in ROT.items():
    rot = {"y": y, "uvlock": True} if y else {}
    tparts += [{"when": {"slope": "none", side: "true"}, "apply": {"model": B + "refractory_trough_arm", **rot}},
               {"when": {"slope": "none", side: "false"}, "apply": {"model": B + "refractory_trough_end", **rot}},
               {"when": {"slope": side}, "apply": {"model": B + "refractory_trough_slope", **({"y": y} if y else {})}}]
w(A / "blockstates/refractory_trough.json", {"multipart": tparts})
model("refractory_trough_inventory", T_MIDDLE + T_ARM + [turn180(e) for e in T_ARM], TT)
item_def("refractory_trough", B + "refractory_trough_inventory")
loot("refractory_trough")
# the gate (axis z in the models; x turns them): frame posts at the middle's sides, a cross beam, the plate in its slots
GATE_FRAME = [box([3, 2, 7], [5, 10, 9], "#out"), box([11, 2, 7], [13, 10, 9], "#out"), box([3, 10, 6.5], [13, 11.5, 9.5], "#out"),
              box([7.5, 11.5, 7.6], [8.5, 13, 8.4], "#lever")]
GATE_SHUT = [box([5, 2, 7.4], [11, 6.5, 8.6], {**{f: "#plate" for f in FACES}}), box([7.4, 6.5, 7.6], [8.6, 11.5, 8.4], "#lever")]
GATE_OPEN = [box([5, 6, 7.4], [11, 10.4, 8.6], {**{f: "#plate" for f in FACES}}), box([7.4, 10.4, 7.6], [8.6, 13, 8.4], "#lever")]
model("trough_gate_frame", GATE_FRAME, TT)
model("trough_gate_shut", GATE_SHUT, TT)
model("trough_gate_open", GATE_OPEN, TT)
gparts = [{"when": {"slope": "none"}, "apply": {"model": B + "refractory_trough_middle"}}]
for side, y in ROT.items():
    rot = {"y": y, "uvlock": True} if y else {}
    gparts += [{"when": {side: "true"}, "apply": {"model": B + "refractory_trough_arm", **rot}},
               {"when": {side: "false"}, "apply": {"model": B + "refractory_trough_end", **rot}}]
for axis, y in (("z", 0), ("x", 90)):
    rot = {"y": y} if y else {}
    gparts += [{"when": {"axis": axis}, "apply": {"model": B + "trough_gate_frame", **rot}},
               {"when": {"axis": axis, "open": "false"}, "apply": {"model": B + "trough_gate_shut", **rot}},
               {"when": {"axis": axis, "open": "true"}, "apply": {"model": B + "trough_gate_open", **rot}}]
w(A / "blockstates/trough_gate.json", {"multipart": gparts})
model("trough_gate_inventory", T_MIDDLE + T_ARM + [turn180(e) for e in T_ARM] + GATE_FRAME + GATE_SHUT, TT)
item_def("trough_gate", B + "trough_gate_inventory")
loot("trough_gate")
recipe("refractory_trough", {"type": "minecraft:crafting_shaped", "category": "misc", "pattern": ["B B", "PBP"],
                             "key": {"B": M + "silica_brick", "P": M + "iron_plate"}, "result": {"id": M + "refractory_trough", "count": 4}})
recipe("trough_gate", {"type": "minecraft:crafting_shapeless", "category": "misc",
                       "ingredients": [M + "refractory_trough", M + "iron_plate", M + "iron_tool_rod"], "result": {"id": M + "trough_gate"}})
tag_add(D / "minecraft/tags/block/mineable/pickaxe.json", M + "refractory_trough", M + "trough_gate")
merge(A / "lang/en_us.json", {"block.mechanical.refractory_trough": "Refractory Trough", "block.mechanical.trough_gate": "Trough Gate",
    "message.mechanical.trough.empty": "The trough is empty", "message.mechanical.trough.molten": "Still molten: let it set before you knock it out"})

# ---------------- the casting tier (phase 4b)
def pocket_mold(name, xs, zs, top):
    """A cast-iron mold plate with real pockets: the plate (floor at 1.6) and bars (to 4) between the pocket intervals."""
    tex = {"particle": B + "iron_mold_side", "top": B + top, "side": B + "iron_mold_side"}
    side4 = {"north": "#side", "south": "#side", "east": "#side", "west": "#side"}
    els = [box([1, 0, 1], [15, 1.6, 15], {"up": "#top", "down": "#side", **side4})]
    def gaps(iv):
        edges = [1] + [v for a, b in iv for v in (a, b)] + [15]
        return [(edges[i], edges[i + 1]) for i in range(0, len(edges), 2)]
    for x0, x1 in gaps(xs):
        els.append(box([x0, 1.6, 1], [x1, 4, 15], {"up": "#top", **side4}))
    for z0, z1 in gaps(zs):
        for xa, xb in xs:
            els.append(box([xa, 1.6, z0], [xb, 4, z1], {"up": "#top", **side4}))
    gated_cast(name, els, tex, IRON_GATES, 1.6, IRON_LIP)
    loot(name)


pocket_mold("iron_plate_mold", [(2.5, 13.5)], [(2.5, 13.5)], "iron_plate_mold_top")
pocket_mold("iron_nugget_mold", [(2.5, 5.5), (6.5, 9.5), (10.5, 13.5)], [(2.5, 5.5), (6.5, 9.5), (10.5, 13.5)], "iron_nugget_mold_top")

for name in ("casting_sand", "burnt_casting_sand", "bronze_block", "steel_block", "lead_block", "slag_stone", "cast_basalt"):
    w(A / f"models/block/{name}.json", {"parent": "minecraft:block/cube_all", "textures": {"all": B + name}})
    w(A / f"blockstates/{name}.json", {"variants": {"": {"model": B + name}}})
    item_def(name, B + name)
    loot(name)

# Sand Runner: Casting Sand with a groove dug in its top (floor 13, the groove 6 wide like a trough's channel); arms
# open the groove out to the joined sides
SR = {"particle": B + "casting_sand", "sand": B + "casting_sand", "burnt": B + "burnt_casting_sand"}
SANDF = {f: "#sand" for f in FACES}
SR_MIDDLE = [box([0, 0, 0], [16, 13, 16], {**SANDF, "up": "#burnt"})] + [box([x0, 13, z0], [x0 + 5, 16, z0 + 5], SANDF) for x0 in (0, 11) for z0 in (0, 11)]
SR_END = [box([5, 13, 0], [11, 16, 5], SANDF)]                  # the north side closed
model("sand_runner_middle", SR_MIDDLE, SR)
model("sand_runner_end", SR_END, SR)
sparts = [{"apply": {"model": B + "sand_runner_middle"}}]
for side, y in ROT.items():
    sparts.append({"when": {side: "false"}, "apply": {"model": B + "sand_runner_end", **({"y": y} if y else {})}})
w(A / "blockstates/sand_runner.json", {"multipart": sparts})
w(D / "mechanical/loot_table/blocks/sand_runner.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": M + "burnt_casting_sand"}]}],
   "random_sequence": "mechanical:blocks/sand_runner"})
# Pig Mold: a trench pressed in the top of Casting Sand (x 3..13, z 5.5..10.5, floor 13)
PIG = [box([0, 0, 0], [16, 13, 16], {**SANDF, "up": "#burnt"}), box([0, 13, 0], [16, 16, 5.5], SANDF), box([0, 13, 10.5], [16, 16, 16], SANDF),
       box([0, 13, 5.5], [3, 16, 10.5], SANDF), box([13, 13, 5.5], [16, 16, 10.5], SANDF)]
# a 3 px gate cut down to 14 from each side a runner or trough joins, into the trench (its ends, or across a long wall)
gated_cast("pig_mold", PIG, SR, {"north": (6.5, 9.5, 0, 5.5), "south": (6.5, 9.5, 10.5, 16), "west": (0, 3, 6.5, 9.5), "east": (13, 16, 6.5, 9.5)},
           14, item=False)
w(D / "mechanical/loot_table/blocks/pig_mold.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": M + "burnt_casting_sand"}]}],
   "random_sequence": "mechanical:blocks/pig_mold"})

# Foundry Table: steel legs and apron, a silica-lined top with a water channel round it
FT = {"particle": B + "foundry_table_side", "top": B + "foundry_table_top", "side": B + "foundry_table_side", "steel": B + "pouring_spout_steel"}
TABLE = [box([0, 12, 0], [16, 16, 16], {"up": "#top", "down": "#steel", "north": "#side", "south": "#side", "east": "#side", "west": "#side"})]
TABLE += [box([x0, 0, z0], [x0 + 3, 12, z0 + 3], "#steel") for x0 in (1, 12) for z0 in (1, 12)]
TABLE += [box([1, 5, 2], [15, 6.5, 3], "#steel"), box([1, 5, 13], [15, 6.5, 14], "#steel")]       # stretchers
model("foundry_table", TABLE, FT)
w(A / "blockstates/foundry_table.json", {"variants": {"": {"model": B + "foundry_table"}}})
item_def("foundry_table", B + "foundry_table")
loot("foundry_table")
# Foundry Basin: a deep riveted iron tub lined with the furnace's brick (floor 3, rim 14), a lifting lug each side
FB = {"particle": B + "foundry_basin_side", "side": B + "foundry_basin_side", "in": B + "banded_furnace_casing_top", "lug": B + "finery_hearth_rim"}
BASIN = [box([0, 0, 0], [16, 3, 16], {"up": "#in", "down": "#side", "north": "#side", "south": "#side", "east": "#side", "west": "#side"}),
         box([0, 3, 0], [16, 14, 2], {"north": "#side", "south": "#in", "east": "#side", "west": "#side", "up": "#side"}),
         box([0, 3, 14], [16, 14, 16], {"south": "#side", "north": "#in", "east": "#side", "west": "#side", "up": "#side"}),
         box([0, 3, 2], [2, 14, 14], {"west": "#side", "east": "#in", "up": "#side"}),
         box([14, 3, 2], [16, 14, 14], {"east": "#side", "west": "#in", "up": "#side"}),
         box([-1, 8, 6], [0, 10, 10], "#lug"), box([16, 8, 6], [17, 10, 10], "#lug")]
model("foundry_basin", BASIN, FB)


def chute(length, prop):
    """A trough's end-spout hung on a tall cast (north side; the trough stands a block up beside it): the trough itself
    carried on {length} px over the rim, the same iron outside, silica-lined floor flush with the trough's (y 18) and
    walls as high (21), ending in an iron lip turned down; the metal runs along it and falls off the lip (client/TroughRenderer)."""
    out = {f: "#tout" for f in FACES}
    return [box([5, 16, 0], [11, 18, length], {**out, "up": "#lin"}),
            box([3, 16, 0], [5, 21, length - 1], {**out, "up": "#lin", "east": "#lin"}),
            box([11, 16, 0], [13, 21, length - 1], {**out, "up": "#lin", "west": "#lin"}),
            box([3.5, 16, length - 1], [5, 19, length], out), box([11, 16, length - 1], [12.5, 19, length], out),   # cheeks stepping down to the lip
            box([5, 15.6, length - 0.4], [11, 17.2, length + 0.6], "#steel"),            # the lip, turned down
            box([2.6, 16.5, 0.6], [13.4, 17.5, 1.4], "#tout")] + prop                     # a rivet band at the joint


FB.update({"steel": B + "pouring_spout_steel", "lin": B + "refractory_trough_lining", "tout": B + "refractory_trough_outside"})
# on the basin it hooks over the 2 px wall (a hanger on the rim and a strap down the outside)
model("foundry_basin_chute", chute(4.5, [box([6, 14, 0], [10, 16, 2.4], "#tout"), box([6, 11, -0.4], [10, 16, 0], "#tout")]), FB)
w(A / "blockstates/foundry_basin.json", {"multipart": [{"apply": {"model": B + "foundry_basin"}}] + [
    {"when": {side: "true"}, "apply": {"model": B + "foundry_basin_chute", **({"y": y} if y else {})}} for side, y in ROT.items()]})
item_def("foundry_basin", B + "foundry_basin")
loot("foundry_basin")
# Molding Flask: an iron frame of two halves round rammed sand; the top shows the stage (empty frame, sand, the pouring
# hole once a pattern is in, glowing while molten)
MF = {"particle": B + "molding_flask_side", "side": B + "molding_flask_side", "empty": B + "molding_flask_top_empty",
      "sand": B + "molding_flask_top_sand", "glow": B + "taphole_glow", "inside": B + "casting_sand"}
def flask(top, glow=False):
    side4 = {"north": "#side", "south": "#side", "east": "#side", "west": "#side"}
    els = [box([1, 0, 1], [15, 12, 15], {"up": top, "down": "#side", **side4}),
           box([0.5, 5.5, 0.5], [15.5, 6.5, 15.5], side4 | {"up": "#side", "down": "#side"})]   # where the halves meet
    els += [box([x0, 4, z0], [x0 + 1.5, 8, z0 + 1.5], "#side") for x0 in (0, 14.5) for z0 in (0, 14.5)]  # the pins at the corners
    if top != "#empty" and top != "#inside":  # the pouring cup over the sprue: a raised sand ring round a hole
        ring = {"north": "#inside", "south": "#inside", "east": "#inside", "west": "#inside", "up": "#inside"}
        # wide enough (4.8 px) that the melt shows round the 1.8 px stream while it fills (client/FlaskRenderer)
        els += [box([4.5, 12, 4.5], [11.5, 13.2, 5.6], ring), box([4.5, 12, 10.4], [11.5, 13.2, 11.5], ring),
                box([4.5, 12, 5.6], [5.6, 13.2, 10.4], ring), box([10.4, 12, 5.6], [11.5, 13.2, 10.4], ring)]
    if glow:  # the cup's melt is drawn by client/FlaskRenderer; the block just gives off light
        pass
    return els
model("molding_flask_empty", flask("#empty"), MF)
model("molding_flask_sand", flask("#inside"), MF)
model("molding_flask_ready", flask("#sand"), MF)
model("molding_flask_molten", flask("#sand", True), MF)
model("molding_flask_set", flask("#sand"), MF)
# on the flask it reaches 7 px in, over the pouring cup, on a post standing on the frame
MF.update({"steel": B + "pouring_spout_steel", "lin": B + "refractory_trough_lining", "tout": B + "refractory_trough_outside"})
model("molding_flask_chute", chute(7, [box([6.5, 12, 1], [9.5, 16, 2.5], "#tout"), box([6, 12, 0.5], [10, 12.6, 3], "#tout")]), MF)
w(A / "blockstates/molding_flask.json", {"multipart": [{"when": {"stage": st}, "apply": {"model": B + f"molding_flask_{st}"}} for st in ("empty", "sand", "ready", "molten", "set")] + [
    {"when": {side: "true"}, "apply": {"model": B + "molding_flask_chute", **({"y": y} if y else {})}} for side, y in ROT.items()]})
item_def("molding_flask", B + "molding_flask_empty")
loot("molding_flask")
# Pattern Bench: a joiner's bench (legs, top, a vice at the front)
PB = {"particle": B + "pattern_bench_side", "top": B + "pattern_bench_top", "side": B + "pattern_bench_side", "front": B + "pattern_bench_front"}
BENCH = [box([0, 10, 0], [16, 13, 16], {"up": "#top", "down": "#side", "north": "#front", "south": "#side", "east": "#side", "west": "#side"})]
BENCH += [box([x0, 0, z0], [x0 + 2.5, 10, z0 + 2.5], "#side") for x0 in (0.5, 13) for z0 in (0.5, 13)]
BENCH += [box([1, 2, 1], [15, 3.5, 15], "#side"), box([2, 8, -1], [6, 12, 0], "#front")]
model("pattern_bench", BENCH, PB)
w(A / "blockstates/pattern_bench.json", {"variants": {f"facing={f}": {"model": B + "pattern_bench", **({"y": y} if y else {})} for f, y in ROT.items()}})
item_def("pattern_bench", B + "pattern_bench")
loot("pattern_bench")

# items
FLASK_SHAPES = ["pickaxe_head", "axe_head", "shovel_head", "hoe_head", "knife_blade", "saw_blade", "spear_head", "sword_blade", "hammer_head",
                "plate", "ingot_mold", "plate_mold", "nugget_mold", "gear", "pot"]
OWN = {"ingot_mold", "plate_mold", "nugget_mold", "gear", "pot"}
for shape in FLASK_SHAPES:
    tex = f"wooden_pattern_{shape}" if shape in OWN else ("wooden_pattern_plate_mold" if shape == "plate" else "wooden_pattern")
    w(A / f"models/item/wooden_pattern_{shape}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": M + f"item/{tex}"}})
    item_def(f"wooden_pattern_{shape}", M + f"item/wooden_pattern_{shape}")
for name in ("coal_dust", "cast_iron_gear", "cast_iron_pot", "pig_pattern"):
    w(A / f"models/item/{name}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": M + f"item/{name}"}})
    item_def(name, M + f"item/{name}")

shapeless("casting_sand", ["minecraft:sand"] * 4 + ["minecraft:clay_ball", M + "coal_dust"], 4)
recipe("casting_sand_from_burnt", {"type": "minecraft:crafting_shapeless", "category": "building",
       "ingredients": [M + "burnt_casting_sand"] * 4 + ["minecraft:clay_ball", M + "coal_dust"], "result": {"id": M + "casting_sand", "count": 4}})
recipe("molding_flask", {"type": "minecraft:crafting_shaped", "category": "misc", "pattern": ["P P", "R R", "P P"],
       "key": {"P": M + "iron_plate", "R": M + "iron_tool_rod"}, "result": {"id": M + "molding_flask"}})
recipe("foundry_table", {"type": "minecraft:crafting_shaped", "category": "misc", "pattern": ["PPP", "BUB"],
       "key": {"P": M + "steel_plate", "B": M + "silica_bricks", "U": M + "bronze_bucket"}, "result": {"id": M + "foundry_table"}})
recipe("foundry_basin", {"type": "minecraft:crafting_shaped", "category": "misc", "pattern": ["P P", "PBP", "BPB"],
       "key": {"P": M + "steel_plate", "B": M + "silica_bricks"}, "result": {"id": M + "foundry_basin"}})
recipe("pattern_bench", {"type": "minecraft:crafting_shaped", "category": "misc", "pattern": ["PCP", "L L"],
       "key": {"P": "#minecraft:planks", "L": "#minecraft:logs", "C": M + "bronze_chisel"}, "result": {"id": M + "pattern_bench"}})
tag_add(D / "minecraft/tags/block/mineable/shovel.json", M + "casting_sand", M + "burnt_casting_sand", M + "sand_runner", M + "pig_mold")
tag_add(D / "minecraft/tags/block/mineable/pickaxe.json", M + "iron_plate_mold", M + "iron_nugget_mold", M + "foundry_table", M + "foundry_basin",
        M + "molding_flask", M + "bronze_block", M + "steel_block", M + "lead_block", M + "slag_stone", M + "cast_basalt")
tag_add(D / "minecraft/tags/block/mineable/axe.json", M + "pattern_bench")
LANG4B = {"item.mechanical.coal_dust": "Coal Dust", "block.mechanical.casting_sand": "Casting Sand", "block.mechanical.burnt_casting_sand": "Burnt Casting Sand",
          "block.mechanical.sand_runner": "Sand Runner", "block.mechanical.pig_mold": "Pig Mold", "item.mechanical.pig_pattern": "Pig Pattern",
          "block.mechanical.molding_flask": "Molding Flask", "block.mechanical.foundry_table": "Foundry Table", "block.mechanical.foundry_basin": "Foundry Basin",
          "block.mechanical.pattern_bench": "Pattern Bench", "container.mechanical.pattern_bench": "Pattern Bench",
          "block.mechanical.iron_plate_mold": "Iron Plate Mold", "block.mechanical.iron_nugget_mold": "Iron Nugget Mold",
          "item.mechanical.cast_iron_gear": "Cast Iron Gear", "item.mechanical.cast_iron_pot": "Cast Iron Pot",
          "block.mechanical.bronze_block": "Block of Bronze", "block.mechanical.steel_block": "Block of Steel", "block.mechanical.lead_block": "Block of Lead",
          "block.mechanical.slag_stone": "Slag Stone", "block.mechanical.cast_basalt": "Cast Basalt",
          "message.mechanical.molding_flask.sand": "Ram it with 4 Casting Sand",
          "message.mechanical.molding_flask.empty": "Empty frame: ram it with 4 Casting Sand",
          "message.mechanical.molding_flask.sand": "Rammed: press a Wooden Pattern in",
          "message.mechanical.molding_flask.ready": "Ready for metal: %s",
          "message.mechanical.molding_flask.molten": "Setting: %s",
          "message.mechanical.molding_flask.set": "Set: knock it out with a pickaxe or hammer (%s)"}
for shape in FLASK_SHAPES:
    nice = shape.replace("_", " ").title()
    LANG4B[f"item.mechanical.wooden_pattern_{shape}"] = f"Wooden Pattern ({nice})"
    LANG4B[f"pattern.mechanical.flask.{shape}"] = nice
merge(A / "lang/en_us.json", LANG4B)

# ---------------- phases 5-7: Finery Hearth, Slag Stone Bricks, Sight Glass, loam, the Furnace Salamander, coke, Hot Blast
FH = {"particle": B + "finery_hearth_side", "side": B + "finery_hearth_side", "top": B + "finery_hearth_top", "lit": B + "finery_hearth_top_lit",
      "rim": B + "finery_hearth_rim"}
def hearth(top):
    side4 = {"north": "#side", "south": "#side", "east": "#side", "west": "#side"}
    return [box([0, 0, 0], [16, 6, 16], {"up": top, "down": "#side", **side4}),             # the charcoal bed
            box([0, 6, 0], [16, 10, 2], {**side4, "up": "#rim"}), box([0, 6, 14], [16, 10, 16], {**side4, "up": "#rim"}),  # walls, an iron rim on top
            box([0, 6, 2], [2, 10, 14], {**side4, "up": "#rim"}), box([14, 6, 2], [16, 10, 14], {**side4, "up": "#rim"})]
model("finery_hearth", hearth("#top"), FH)
model("finery_hearth_lit", hearth("#lit") + [{"from": [2, 6.01, 2], "to": [14, 6.01, 14], "light_emission": 15, "shade": False,
      "faces": {"up": {"uv": [2, 2, 14, 14], "texture": "#lit"}}}], FH)
w(A / "blockstates/finery_hearth.json", {"variants": {"lit=false": {"model": B + "finery_hearth"}, "lit=true": {"model": B + "finery_hearth_lit"}}})
item_def("finery_hearth", B + "finery_hearth")
loot("finery_hearth")
w(A / "models/block/slag_stone_bricks.json", {"parent": "minecraft:block/cube_all", "textures": {"all": B + "slag_stone_bricks"}})
# Hot Blast Stove: a round riveted iron tower (two crossed boxes, so it reads round, not a plain cube), a bolted hatch
# on the north face; stacked, the stoves make one tall column
HB = {"particle": B + "hot_blast_stove_side", "side": B + "hot_blast_stove_side", "hatch": B + "hot_blast_stove_hatch", "top": B + "hot_blast_stove_top"}
model("hot_blast_stove", [box([2, 0, 1], [14, 16, 15], {"north": "#hatch", "south": "#side", "east": "#side", "west": "#side", "up": "#top", "down": "#top"}),
                          box([1, 0, 2], [15, 16, 14], {"north": "#side", "south": "#side", "east": "#side", "west": "#side", "up": "#top", "down": "#top"}),
                          box([5, 3, 0.5], [11, 13, 1], {"north": "#hatch", "east": "#side", "west": "#side", "up": "#side", "down": "#side"})], HB)
for name in ("slag_stone_bricks", "hot_blast_stove"):
    w(A / f"blockstates/{name}.json", {"variants": {"": {"model": B + name}}})
    item_def(name, B + name)
    loot(name)
# Sight Glass: the furnace's brick with an iron-framed window; the glass shows the inside's heat (dark, dull red,
# orange, yellow-white) and glows once it is hot
def window_glow(light):
    """Fullbright planes over the window (4..12) on the four sides."""
    return [
        {"from": [4, 4, -0.01], "to": [12, 12, -0.01], "light_emission": light, "shade": False, "faces": {"north": {"uv": [4, 4, 12, 12], "texture": "#all", "cullface": "north"}}},
        {"from": [4, 4, 16.01], "to": [12, 12, 16.01], "light_emission": light, "shade": False, "faces": {"south": {"uv": [4, 4, 12, 12], "texture": "#all", "cullface": "south"}}},
        {"from": [-0.01, 4, 4], "to": [-0.01, 12, 12], "light_emission": light, "shade": False, "faces": {"west": {"uv": [4, 4, 12, 12], "texture": "#all", "cullface": "west"}}},
        {"from": [16.01, 4, 4], "to": [16.01, 12, 12], "light_emission": light, "shade": False, "faces": {"east": {"uv": [4, 4, 12, 12], "texture": "#all", "cullface": "east"}}}]
for h in range(4):
    model(f"sight_glass_{h}", [cube({f: "#all" for f in FACES})] + (window_glow(4 + 3 * h) if h else []),
          {"particle": B + f"sight_glass_{h}", "all": B + f"sight_glass_{h}"})
w(A / "blockstates/sight_glass.json", {"variants": {f"heat={h}": {"model": B + f"sight_glass_{h}"} for h in range(4)}})
item_def("sight_glass", B + "sight_glass_0")
loot("sight_glass")
# Coke Oven: a beehive of the furnace's brick in three steps, its iron door (two slits that glow while it cokes) to
# the north, the charging hole on the crown
CO = {"particle": B + "banded_furnace_casing", "dome": B + "banded_furnace_casing", "upper": B + "banded_furnace_casing_upper",
      "front": B + "coke_oven_front", "front_lit": B + "coke_oven_front_lit", "glow": B + "coke_oven_glow",
      "top": B + "coke_oven_top", "top_lit": B + "coke_oven_top_lit", "ring": B + "banded_furnace_casing_top"}
for lit in (False, True):
    els = [box([0, 0, 0], [16, 8, 16], {"north": "#front_lit" if lit else "#front", "south": "#dome", "east": "#dome", "west": "#dome", "up": "#ring", "down": "#dome"}),
           box([1, 8, 1], [15, 11, 15], {"north": "#upper", "south": "#upper", "east": "#upper", "west": "#upper", "up": "#ring"}),
           box([3, 11, 3], [13, 14, 13], {"north": "#upper", "south": "#upper", "east": "#upper", "west": "#upper", "up": "#ring"}),
           box([5, 14, 5], [11, 15, 11], {"north": "#upper", "south": "#upper", "east": "#upper", "west": "#upper", "up": "#top_lit" if lit else "#top"})]
    if lit:
        els.append({"from": [5, 0, -0.01], "to": [11, 8, -0.01], "light_emission": 12, "shade": False,
                    "faces": {"north": {"uv": [5, 8, 11, 16], "texture": "#glow"}}})
    model("coke_oven" + ("_lit" if lit else ""), els, CO)
w(A / "blockstates/coke_oven.json", {"variants": {f"facing={f},lit={str(l).lower()}": {"model": B + "coke_oven" + ("_lit" if l else ""), **({"y": y} if y else {})}
                                                  for f, y in ROT.items() for l in (False, True)}})
item_def("coke_oven", B + "coke_oven")
loot("coke_oven")
for name in ("glass_batch", "loam", "strickle_board_bell", "strickle_board_cauldron", "unfired_loam_mold_bell", "unfired_loam_mold_cauldron",
             "loam_mold_bell", "loam_mold_cauldron", "furnace_salamander", "coke", "steel_ladle"):
    w(A / f"models/item/{name}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": M + f"item/{name}"}})
    item_def(name, M + f"item/{name}")
# a Steel Ladle holding melt (custom data set) shows it glowing in the bowl
w(A / "models/item/steel_ladle_full.json", {"parent": "minecraft:item/generated", "textures": {"layer0": M + "item/steel_ladle_full"}})
w(A / "items/steel_ladle.json", {"model": {"type": "minecraft:condition", "property": "minecraft:has_component", "component": "minecraft:custom_data",
                                           "on_true": {"type": "minecraft:model", "model": M + "item/steel_ladle_full"},
                                           "on_false": {"type": "minecraft:model", "model": M + "item/steel_ladle"}}})
recipe("finery_hearth", {"type": "minecraft:crafting_shaped", "category": "misc", "pattern": ["F F", "FPF"],
       "key": {"F": M + "fire_brick", "P": M + "iron_plate"}, "result": {"id": M + "finery_hearth"}})
recipe("slag_stone_bricks", {"type": "minecraft:crafting_shaped", "category": "building", "pattern": ["SS", "SS"],
       "key": {"S": M + "slag_stone"}, "result": {"id": M + "slag_stone_bricks", "count": 4}})
shapeless("glass_batch", ["minecraft:sand"] * 4 + [M + "wood_ash"], 4)
shapeless("sight_glass", [M + "banded_furnace_casing", "minecraft:glass_pane", "minecraft:glass_pane"])
shapeless("loam", ["minecraft:clay_ball", "minecraft:clay_ball", "minecraft:sand", "minecraft:wheat"], 4)
shapeless("unfired_loam_mold_bell", [M + "loam"] * 4 + [M + "strickle_board_bell"])
shapeless("unfired_loam_mold_cauldron", [M + "loam"] * 4 + [M + "strickle_board_cauldron"])
recipe("strickle_board_bell", {"type": "minecraft:crafting_shaped", "category": "misc", "pattern": ["P ", "PS"],
       "key": {"P": "#minecraft:planks", "S": "minecraft:stick"}, "result": {"id": M + "strickle_board_bell"}})
recipe("strickle_board_cauldron", {"type": "minecraft:crafting_shaped", "category": "misc", "pattern": ["PP", " S"],
       "key": {"P": "#minecraft:planks", "S": "minecraft:stick"}, "result": {"id": M + "strickle_board_cauldron"}})
recipe("coke_oven", {"type": "minecraft:crafting_shaped", "category": "misc", "pattern": ["BBB", "B B", "BPB"],
       "key": {"B": M + "silica_bricks", "P": M + "iron_plate"}, "result": {"id": M + "coke_oven"}})
recipe("hot_blast_stove", {"type": "minecraft:crafting_shaped", "category": "misc", "pattern": ["PBP", "B B", "PBP"],
       "key": {"P": M + "steel_plate", "B": M + "silica_bricks"}, "result": {"id": M + "hot_blast_stove"}})
recipe("steel_ladle", {"type": "minecraft:crafting_shaped", "category": "misc", "pattern": ["P P", " B ", "  R"],
       "key": {"P": M + "steel_plate", "B": M + "silica_brick", "R": M + "iron_tool_rod"}, "result": {"id": M + "steel_ladle"}})
tag_add(D / "minecraft/tags/block/mineable/pickaxe.json", *[M + b for b in ("finery_hearth", "slag_stone_bricks", "sight_glass", "coke_oven", "hot_blast_stove")])
merge(A / "lang/en_us.json", {
    "block.mechanical.finery_hearth": "Finery Hearth", "block.mechanical.slag_stone_bricks": "Slag Stone Bricks", "block.mechanical.sight_glass": "Sight Glass",
    "block.mechanical.coke_oven": "Coke Oven", "block.mechanical.hot_blast_stove": "Hot Blast Stove",
    "item.mechanical.glass_batch": "Glass Batch", "item.mechanical.loam": "Loam", "item.mechanical.strickle_board_bell": "Strickle Board (Bell)",
    "item.mechanical.strickle_board_cauldron": "Strickle Board (Cauldron)", "item.mechanical.unfired_loam_mold_bell": "Unfired Loam Mold (Bell)",
    "item.mechanical.unfired_loam_mold_cauldron": "Unfired Loam Mold (Cauldron)", "item.mechanical.loam_mold_bell": "Loam Mold (Bell)",
    "item.mechanical.loam_mold_cauldron": "Loam Mold (Cauldron)", "item.mechanical.furnace_salamander": "Furnace Salamander",
    "item.mechanical.coke": "Coke", "item.mechanical.steel_ladle": "Steel Ladle",
    "item.mechanical.steel_ladle.empty": "Empty: scoop a trough's metal", "item.mechanical.steel_ladle.holds": "%s / %s mB %s",
    "message.mechanical.finery_hearth": "%s°, %s pig iron, %s charcoal, blast %s (fines at 1300°)",
    "message.mechanical.coke_oven.coal": "%s / %s Coal: it closes and bakes when full",
    "message.mechanical.coke_oven.baking": "Baking: %s s left",
})

# ---------------- flat items
for name in ("crushed_quartzite", "unfired_silica_brick", "silica_brick", "steel_hoop", "bronze_tuyere_nozzle", "unfired_clay_tuyere_mold", "pig_iron_ingot"):
    w(A / f"models/item/{name}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": M + f"item/{name}"}})
    item_def(name, M + f"item/{name}")

# ---------------- recipes


shapeless("unfired_silica_brick", [M + "crushed_quartzite", M + "crushed_quartzite", M + "fire_clay", M + "quicklime"], 4)
recipe("silica_bricks", {"type": "minecraft:crafting_shaped", "category": "building", "pattern": ["BB", "BB"],
                         "key": {"B": M + "silica_brick"}, "result": {"id": M + "silica_bricks"}})
shapeless("banded_furnace_casing", [M + "silica_brick"] * 4 + [M + "steel_hoop"])
shapeless("firebox", [M + "banded_furnace_casing", M + "bronze_tuyere_nozzle", M + "iron_plate"])
shapeless("high_furnace_front", [M + "banded_furnace_casing", M + "steel_plate", M + "steel_plate", M + "iron_plate"])
shapeless("taphole", [M + "banded_furnace_casing", M + "iron_tool_rod", M + "fire_clay"])
recipe("great_bellows", {"type": "minecraft:crafting_shaped", "category": "misc", "pattern": ["PLP", "LGL", "PTP"],
                         "key": {"P": "#minecraft:planks", "L": "minecraft:leather", "G": M + "gearbox", "T": M + "bronze_tuyere_nozzle"},
                         "result": {"id": M + "great_bellows"}})

# ---------------- tags
STONE = ["silica_bricks", "banded_furnace_casing", "firebox", "taphole", "high_furnace_front"]
tag_add(D / "minecraft/tags/block/mineable/pickaxe.json", *[M + b for b in STONE])
tag_add(D / "minecraft/tags/block/mineable/axe.json", M + "great_bellows")

# ---------------- lang
merge(A / "lang/en_us.json", {
    "item.mechanical.crushed_quartzite": "Crushed Quartzite", "item.mechanical.unfired_silica_brick": "Unfired Silica Brick",
    "item.mechanical.silica_brick": "Silica Brick", "item.mechanical.steel_hoop": "Steel Hoop",
    "item.mechanical.bronze_tuyere_nozzle": "Bronze Tuyere Nozzle", "item.mechanical.unfired_clay_tuyere_mold": "Unfired Clay Tuyere Mold",
    "block.mechanical.clay_tuyere_mold": "Clay Tuyere Mold",
    "block.mechanical.silica_bricks": "Silica Bricks", "block.mechanical.banded_furnace_casing": "Banded Furnace Casing",
    "block.mechanical.firebox": "Firebox", "block.mechanical.taphole": "Taphole", "block.mechanical.high_furnace_front": "High Furnace Front",
    "block.mechanical.great_bellows": "Great Bellows", "container.mechanical.high_furnace": "High Furnace",
    "pattern.mechanical.hoop": "Hoops (8)",
    "message.mechanical.high_furnace.checking": "Checking the furnace...",
    "message.mechanical.high_furnace.blocked": "The inside must be empty: clear %s %s %s",
    "message.mechanical.high_furnace.too_narrow": "Too narrow: the hearth needs at least 2 blocks inside, both ways",
    "message.mechanical.high_furnace.too_wide": "Too wide: a real furnace's bosh is at most 3 blocks inside",
    "message.mechanical.high_furnace.floor": "The floor under the inside must be Silica Bricks: %s %s %s",
    "message.mechanical.high_furnace.missing": "Not a High Furnace yet: %s wall blocks missing (one at %s %s %s)",
    "message.mechanical.high_furnace.too_low": "Too low: at least %s wall layers (6 m with the floor)",
    "message.mechanical.high_furnace.too_tall": "Too tall: at most %s wall layers",
    "message.mechanical.high_furnace.fireboxes": "The furnace needs %s Fireboxes for its size (has %s)",
    "message.mechanical.high_furnace.too_many_fireboxes": "At most %s Fireboxes",
    "message.mechanical.high_furnace.too_many_tapholes": "At most %s Tapholes",
    "message.mechanical.high_furnace.no_furnace": "This Firebox is not part of a High Furnace",
    "message.mechanical.high_furnace.needs_fuel": "No fuel: give the Fireboxes Charcoal or Coal",
    "message.mechanical.high_furnace.firebox_full": "The Firebox is full (%s)",
    "message.mechanical.high_furnace.firebox": "Firebox: %s / %s fuel, blast %s / %s (not lit)",
    "message.mechanical.high_furnace.firebox_lit": "Firebox burning: %s / %s fuel, blast %s / %s",
    "message.mechanical.great_bellows.blowing": "Blast %s / %s",
    "message.mechanical.great_bellows.still": "Still: turn its cam shaft with a Water Wheel's axle from the side",
    "gui.mechanical.high_furnace.heat": "%s° → %s°",
    "gui.mechanical.high_furnace.blast": "Blast %s / %s per volume",
    "gui.mechanical.high_furnace.drying": "Drying out: %s:%s (max 600°)",
    "gui.mechanical.high_furnace.lit": "Burning", "gui.mechanical.high_furnace.out": "Out",
    "gui.mechanical.high_furnace.firebox": "#%s  fuel %s  blast %s",
    "gui.mechanical.high_furnace.pool": "Pool %s / %s mB",
    "gui.mechanical.high_furnace.pours": "Pours: %s", "gui.mechanical.high_furnace.pours_nothing": "Pours: nothing yet",
    "gui.mechanical.high_furnace.layer": "%s mB (%s ingots), melts at %s°",
    "gui.mechanical.high_furnace.layer_click": "Click: the tapholes pour this layer (again: the bottom layer)",
    "gui.mechanical.high_furnace.slot": "Melts into %s mB %s", "gui.mechanical.high_furnace.needs": "Needs %s°",
    "gui.mechanical.high_furnace.mark.950": "%s°: bronze", "gui.mechanical.high_furnace.mark.1450": "%s°: iron ore",
    "gui.mechanical.high_furnace.mark.1500": "%s°: steel", "gui.mechanical.high_furnace.mark.1600": "%s°: charcoal's ceiling",
})
print("high furnace: ok")
