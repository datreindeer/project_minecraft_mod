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


def model(name, elements, textures, parent="minecraft:block/block"):
    w(A / f"models/block/{name}.json", {"parent": parent, "textures": textures, "elements": elements})


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
model("casing_plain", [walls("#side")], CT)
model("casing_strap", [walls("#side"), strap(0)], CT)
model("casing_upper", [walls("#upper", 1), sill()], CT)
model("casing_upper_strap", [walls("#upper", 1), sill(), strap(1)], CT)
TOP_ELS = [walls("#upper", 1), sill(), CORBEL]
model("casing_top", TOP_ELS, CT)
for h in (1, 2, 3):
    # the joints glow one step behind the heat and dimmer (a full white-hot grid shouts): red at heat 2, orange at 3
    if h > 1:
        model(f"casing_hot_heat_{h}", [walls("#hot")] + glow_planes(["north"], "#glow", LIGHT[h - 1] - 3),
              {**CT, "glow": B + f"banded_furnace_casing_glow_{h - 1}"})
    else:
        model(f"casing_hot_heat_{h}", [walls("#hot")], CT)
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

PARTS = ["none", "base", "hot", "plain", "strap", "upper", "upper_strap", "top"]
END_KIND = {"hot": "full", "plain": "full", "strap": "full_plate", "upper": "inset", "upper_strap": "inset_plate", "top": "top"}
mp = []
for f, y in ROT.items():
    rot = {"y": y} if y else {}
    for p in PARTS:
        for h in range(4):
            if p == "none":
                mdl = "banded_furnace_casing"
            else:
                mdl = f"casing_{p}" + (f"_heat_{h}" if h and p in ("hot", "top") else "")
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
    els = [box([0, 0, 3], [16, 16, 16], {"north": front_tex, "south": "#base", "east": "#base", "west": "#base", "up": "#base_top", "down": "#base_top"})]
    for f in ("south", "east", "west", "up", "down"):
        els[0]["faces"][f]["cullface"] = f
    els += [box([0, 0, 0], [3, 16, 3], {"north": "#base", "east": "#arch", "west": "#base", "up": "#base_top", "down": "#base"}),
            box([13, 0, 0], [16, 16, 3], {"north": "#base", "west": "#arch", "east": "#base", "up": "#base_top", "down": "#base"}),
            box([3, 13, 0], [13, 16, 3], {"north": "#arch", "down": "#arch", "up": "#base_top"}),           # the arch ring's crown
            box([3, 11, 0], [5, 13, 3], {"north": "#arch", "down": "#arch", "east": "#arch"}),             # its stepped haunches
            box([11, 11, 0], [13, 13, 3], {"north": "#arch", "down": "#arch", "west": "#arch"}),
            box([5, 12, 0], [6.5, 13, 3], {"north": "#arch", "down": "#arch", "east": "#arch"}),
            box([9.5, 12, 0], [11, 13, 3], {"north": "#arch", "down": "#arch", "west": "#arch"}),
            box([3, 0, 0], [13, 0.75, 3], {"north": "#base", "up": "#base_top"}),                         # threshold
            box([0, 0, -2], [3, 4, 0], {"north": "#base", "up": "#base_top", "east": "#base", "west": "#base"}),
            box([13, 0, -2], [16, 4, 0], {"north": "#base", "up": "#base_top", "east": "#base", "west": "#base"}),
            box([2.6, 11.5, -0.4], [13.4, 12.2, 0], {"north": "#strap", "down": "#strap", "up": "#strap"})]  # an iron arch band
    return els


def arch_glow(light):
    """The arch's inner faces glowing (joints and hot edge): inner sides of the piers and the underside of the ring."""
    g = {"light_emission": light, "shade": False}
    return [{"from": [3.01, 0.75, 0], "to": [3.01, 11, 3], **g, "faces": {"east": {"uv": [13, 5, 16, 15], "texture": "#arch_glow"}}},
            {"from": [12.99, 0.75, 0], "to": [12.99, 11, 3], **g, "faces": {"west": {"uv": [0, 5, 3, 15], "texture": "#arch_glow"}}},
            {"from": [6.5, 12.99, 0], "to": [9.5, 12.99, 3], **g, "faces": {"down": {"uv": [6, 0, 10, 3], "texture": "#arch_glow"}}},
            {"from": [3, 10.99, 0], "to": [5, 10.99, 3], **g, "faces": {"down": {"uv": [3, 0, 5, 3], "texture": "#arch_glow"}}},
            {"from": [11, 10.99, 0], "to": [13, 10.99, 3], **g, "faces": {"down": {"uv": [11, 0, 13, 3], "texture": "#arch_glow"}}}]


def door_glow(tex, light):
    return {"from": [0, 0, 2.98], "to": [16, 16, 2.98], "light_emission": light, "shade": False,
            "faces": {"north": {"uv": [0, 0, 16, 16], "texture": tex}}}


def arched_block(name, front, front_glow, extra=(), hot_extra=lambda h: []):
    tex = {**AT, "front": B + front}
    model(name, arch_body("#front") + list(extra), tex)
    for h in (1, 2, 3):
        t = dict(tex)
        els = arch_body("#front") + list(extra) + arch_glow(LIGHT[h])
        if front_glow:
            t["fglow"] = B + (f"{front_glow}_{h}" if front_glow.endswith("glow") and front_glow.startswith("firebox") else front_glow)
            els.append(door_glow("#fglow", LIGHT[h]))
        model(f"{name}_heat_{h}", els + hot_extra(h), t)
    w(A / f"blockstates/{name}.json", {"variants": {
        f"facing={f},heat={h}": {"model": B + name + (f"_heat_{h}" if h else ""), **({"y": y} if y else {})}
        for f, y in ROT.items() for h in range(4)}})
    item_def(name, B + name)
    loot(name)


arched_block("firebox", "firebox_front", "firebox_front_glow")
arched_block("taphole", "taphole_front", None)
# the Front: the hatch cover hangs open on its hinge in the recess, the handle bar stands off the door
FRONT_EXTRA = [
    {"from": [11, 7, 2.4], "to": [17, 13, 2.9], "rotation": {"angle": -45, "axis": "y", "origin": [11, 10, 2.65]},
     "faces": {"north": {"uv": [5, 3, 11, 9], "texture": "#hatch"}, "south": {"uv": [5, 3, 11, 9], "texture": "#hatch"},
               "east": {"uv": [5, 3, 6, 9], "texture": "#hatch"}, "west": {"uv": [10, 3, 11, 9], "texture": "#hatch"},
               "up": {"uv": [5, 3, 11, 4], "texture": "#hatch"}, "down": {"uv": [5, 8, 11, 9], "texture": "#hatch"}}},
    box([10.6, 9, 2.1], [11.4, 11, 3], "#strap"),                                             # hinge knuckle
    box([3.6, 4, 1.4], [4.6, 10, 2.2], "#strap"), box([3.6, 4.5, 2.2], [4.6, 5.5, 3], "#strap"), box([3.6, 8.5, 2.2], [4.6, 9.5, 3], "#strap")]
AT_FRONT = {"hatch": B + "high_furnace_hatch", "hglow": B + "high_furnace_front_glow"}
AT.update(AT_FRONT)
arched_block("high_furnace_front", "high_furnace_front", None, FRONT_EXTRA, lambda h: [
    {"from": [5, 7, 2.98], "to": [11, 13, 2.98], "light_emission": LIGHT[h], "shade": False,
     "faces": {"north": {"uv": [5, 3, 11, 9], "texture": "#hglow"}}}])

# ---------------- Great Bellows (nozzle to the north): one big wedge bellows on a plank bed. Static: the bed, the bottom
# board, two posts carrying the shaft across at the axle's height (y 8, z 8), and the bronze blowpipe rising from the
# bellows' mouth and bending forward into the Firebox's tuyere (it reaches 3.5 px into the firebox's arched recess).
# Moving (client/GreatBellowsRenderer): the top board hinged at the mouth, the folded leather, the shaft with its cam
# that presses the board down once a turn.
BT = {"particle": B + "great_bellows_board", "board": B + "great_bellows_board", "leather": B + "great_bellows_leather",
      "frame": B + "great_bellows_frame", "end": B + "great_bellows_frame_end", "bronze": B + "great_bellows_nozzle", "cam": B + "great_bellows_cam"}
POST = {"north": "#frame", "south": "#frame", "east": "#frame", "west": "#frame", "up": "#end", "down": "#end"}
static = [
    box([1, 0, 1], [15, 1.5, 16], "#board"),                                                     # the plank bed
    box([2, 1.5, 2.5], [14, 2.5, 17], "#board"),                                                 # the bottom board (its tail over the bed's end)
    box([0, 0, 6], [2, 10.5, 10], POST), box([14, 0, 6], [16, 10.5, 10], POST),                   # posts with the shaft bearings
    box([-0.2, 6, 5.6], [2.2, 10, 6.4], "#cam"), box([13.8, 6, 5.6], [16.2, 10, 6.4], "#cam"),    # iron bearing straps
    box([5.5, 1.5, 0.8], [10.5, 4.5, 2.8], "#bronze"),                                           # the bellows' mouth
    box([6.5, 4.5, 0.6], [9.5, 13.5, 3], "#bronze"),                                             # the blowpipe rising
    box([6.5, 11, -3.5], [9.5, 14, 0.6], "#bronze"),                                             # and bending into the tuyere
    box([6, 10.6, -3.9], [10, 14.4, -3.5], "#cam"),                                               # its flange
]
model("great_bellows", static, BT)
w(A / "blockstates/great_bellows.json", {"variants": {
    f"facing={f}": {"model": B + "great_bellows", **({"y": y} if y else {})} for f, y in ROT.items()}})
loot("great_bellows")
# part models, their hinge / axle at the model's centre (8, 8, 8)
model("great_bellows_lid", [box([8, 8, 8], [20, 9, 22.5], "#board"), box([8.5, 9, 15.5], [19.5, 9.6, 17], "#cam"),
                            box([8.5, 9, 9], [19.5, 9.6, 10.5], "#cam")], BT)               # the top board and its iron bands
model("great_bellows_pleat", [box([7.4, 8, 8.2], [20.6, 8.9, 22.2], "#leather")], BT)       # a fold standing out past the boards
model("great_bellows_pleat_in", [box([8.6, 8, 8.6], [19.4, 8.9, 21.8], "#leather")], BT)    # a fold tucked in
model("great_bellows_shaft", [box([6.6, 6.6, 0], [9.4, 9.4, 16], {"north": "#end", "south": "#end", "east": "#frame", "west": "#frame", "up": "#frame", "down": "#frame"}),
                              box([6.1, 6.1, 5.5], [9.9, 12.4, 10.5], "#cam")], BT)          # the shaft and its cam
for p in ("great_bellows_lid", "great_bellows_pleat", "great_bellows_pleat_in", "great_bellows_shaft"):
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
    for fill in ("empty", "molten", "solid") for m in ("copper", "bronze", "poor_bronze") for u in range(5)}})
item_def(f"clay_{s}_mold", B + f"clay_{s}_mold")
w(D / f"mechanical/loot_table/blocks/clay_{s}_mold.json", {"type": "minecraft:block", "pools": [{"rolls": 1,
    "condition": {"type": "minecraft:survives_explosion"},
    "entries": [{"type": "minecraft:item", "name": M + f"clay_{s}_mold",
                 "modifier": [{"type": "minecraft:copy_state", "block": M + f"clay_{s}_mold", "properties": ["uses"]}]}]}],
    "random_sequence": f"mechanical:blocks/clay_{s}_mold"})

# ---------------- flat items
for name in ("crushed_quartzite", "unfired_silica_brick", "silica_brick", "steel_hoop", "bronze_tuyere_nozzle", "unfired_clay_tuyere_mold"):
    w(A / f"models/item/{name}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": M + f"item/{name}"}})
    item_def(name, M + f"item/{name}")

# ---------------- recipes
def recipe(name, obj):
    w(D / f"mechanical/recipe/{name}.json", obj)


def shapeless(name, ingredients, count=1):
    recipe(name, {"type": "minecraft:crafting_shapeless", "category": "building", "ingredients": ingredients,
                  "result": {"id": M + name, "count": count}})


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
    **{f"molten.mechanical.{k}": v for k, v in {"gold": "Molten Gold", "lead": "Molten Lead", "silver": "Molten Silver",
       "copper": "Molten Copper", "bronze": "Molten Bronze", "poor_bronze": "Molten Poor Bronze", "pig_iron": "Molten Pig Iron",
       "steel": "Molten Steel", "wrought_iron": "Molten Wrought Iron", "tin": "Molten Tin", "glass": "Molten Glass",
       "basalt": "Molten Basalt", "obsidian": "Molten Obsidian", "slag": "Molten Slag"}.items()},
    "gui.mechanical.high_furnace.mark.950": "%s°: bronze", "gui.mechanical.high_furnace.mark.1450": "%s°: iron ore",
    "gui.mechanical.high_furnace.mark.1500": "%s°: steel", "gui.mechanical.high_furnace.mark.1600": "%s°: charcoal's ceiling",
})
print("high furnace: ok")
