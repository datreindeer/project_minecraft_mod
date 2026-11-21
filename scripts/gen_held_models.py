"""3D models in hand, on the ground and in item frames for items that are flat sprites (the GUI keeps the sprite).

Each 3D model ("<model>_held") is built from cuboids that sample the item's own sprite (one texture, so one atlas) - the
logs use their wood's log / stripped log / log top block textures. Every model keeps the sprite's footprint and slant
(tools and logs run up the bottom-left to top-right diagonal, containers stand upright facing the front, the pan's dish
faces the front), so the display transforms written for the sprites (vanilla generated / handheld, and the third-person
hold poses of gen_hold_displays.py) still place them. First-person transforms of the items client/HoldPoses animates
itself (basket, buckets, carried logs and kettle) are set here, the rest stay vanilla's.

The item definitions are wrapped in a display_context select: "gui" shows the old tree, everything else the same tree with
each model swapped for its _held one (count stacks of firewood, the bronze crucible's condition... keep working).
Re-running is safe (an already wrapped definition is unwrapped first).
Generator order: after gen_staffs.py / gen_block_items.py (every flat model and definition exists), before
gen_hold_displays.py (which writes the third-person transforms into the _held models). Run: python scripts/gen_held_models.py
"""
import copy
import json
import math
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
A = ROOT / "src/main/resources/assets/mechanical"
MODELS, ITEMS = A / "models/item", A / "items"
M = "mechanical:"
DIRS = {"n": "north", "s": "south", "e": "east", "w": "west", "u": "up", "d": "down"}



def cube(frm, to, uv, tex="#t", rot=None, light=None, skip=""):
    """A cuboid. uv: one rect for every face, or {"side": .., "end": ..} (end: up/down), or a dict per face name."""
    faces = {}
    for f, name in DIRS.items():
        if f in skip:
            continue
        if isinstance(uv, dict):
            r = uv.get(name) or (uv.get("end") if name in ("up", "down") else None) or uv.get("side")
        else:
            r = uv
        if r is None:
            continue
        face = {"uv": list(r[:4]), "texture": tex}
        if len(r) > 4:
            face["rotation"] = r[4]
        faces[name] = face
    e = {"from": [round(v, 4) for v in frm], "to": [round(v, 4) for v in to], "faces": faces}
    if rot:
        e["rotation"] = rot
    if light:
        e["light_emission"] = light
    return e


def stick(p0, p1, w, d, uv, z=8.0, tex="#t", light=None):
    """A bar in the front plane from 2D point p0 to p1 (model x, y), w wide in that plane, d deep (z)."""
    cx, cy = (p0[0] + p1[0]) / 2, (p0[1] + p1[1]) / 2
    length = math.dist(p0, p1)
    angle = math.degrees(math.atan2(p1[1] - p0[1], p1[0] - p0[0])) - 90  # the cube runs along y
    return cube([cx - w / 2, cy - length / 2, z - d / 2], [cx + w / 2, cy + length / 2, z + d / 2], uv, tex,
                rot={"origin": [round(cx, 4), round(cy, 4), z], "axis": "z", "angle": round(angle, 3)}, light=light)


def hollow(x0, x1, z0, z1, y0, y1, outer, inner, rim, t=1.0):
    """Four walls (t thick) of an open box: outer faces outer, inner faces inner, tops rim."""
    walls = [([x0, y0, z0], [x1, y1, z0 + t], "north", "south"), ([x0, y0, z1 - t], [x1, y1, z1], "south", "north"),
             ([x0, y0, z0 + t], [x0 + t, y1, z1 - t], "west", "east"), ([x1 - t, y0, z0 + t], [x1, y1, z1 - t], "east", "west")]
    return [cube(frm, to, {"side": outer, "up": rim, "down": outer, facing_in: inner}) for frm, to, _, facing_in in walls]


# ------------------------------------------------------------------ containers (upright, facing the front like the sprite)

def bucket(body, inner, rim, bottom, fill=None, handle=None, light=None):
    """10 wide, 11 tall; a fill surface at y 9 (water, milk, lava); a bail handle over the top."""
    els = [cube([3.5, 0, 3.5], [12.5, 1, 12.5], {"side": body, "up": inner, "down": bottom})]
    els += hollow(3, 13, 3, 13, 1, 11, body, rim if fill else inner, rim)  # a full sprite's inside rows are the fill
    if fill:
        els.append(cube([4, 1, 4], [12, 9, 12], {"up": fill}, light=light))
    if handle:
        els += [stick((2.6, 8), (2.6, 14.4), 0.75, 1, handle), stick((13.4, 8), (13.4, 14.4), 0.75, 1, handle),
                cube([2.25, 14.25, 7.5], [13.75, 15, 8.5], handle)]
    return els


BRONZE_BUCKET = dict(body=[3, 8, 13, 15], inner=[4, 5, 12, 6], rim=[5, 2, 11, 3], bottom=[4, 15, 12, 16], handle=[5, 0, 11, 1])
CLAY_BUCKET = dict(body=[3, 7, 13, 13], inner=[4, 4, 12, 5], rim=[5, 1, 11, 2], bottom=[5, 14, 11, 15])


def basket():
    """Woven bark basket, 12 x 8, with a carrying handle across."""
    handle = [5, 1, 11, 2]
    els = [cube([2.5, 0, 4.5], [13.5, 1, 11.5], {"side": [1, 14, 15, 15], "up": [3, 7, 13, 8], "down": [3, 15, 13, 16]})]
    els += hollow(2, 14, 4, 12, 1, 8, [1, 10, 15, 14], [3, 7, 13, 8], [2, 9, 14, 10])
    els += [stick((3.5, 7.5), (3.5, 12.5), 1, 1, handle), stick((12.5, 7.5), (12.5, 12.5), 1, 1, handle),
            stick((3.5, 12.2), (5.3, 15.2), 1, 1, handle), stick((12.5, 12.2), (10.7, 15.2), 1, 1, handle),
            cube([5, 14.6, 7.5], [11, 15.6, 8.5], handle)]
    return els


def kettle():
    """A round-bellied bronze pot with a bail handle, open at the top."""
    side, rim = [2, 6, 14, 13], [3, 5, 13, 6]
    handle = [5, 1, 11, 2]
    return [cube([4, 0, 4], [12, 1, 12], {"side": [3, 12, 13, 13], "up": [3, 12, 13, 13]}),
            cube([2.5, 1, 2.5], [13.5, 8, 13.5], {"side": side, "up": rim, "down": [3, 13, 13, 14]}),
            *hollow(3.5, 12.5, 3.5, 12.5, 8, 10.5, rim, [4, 13, 12, 14], rim),
            cube([4.5, 7, 4.5], [11.5, 9.5, 11.5], {"up": [4, 13, 12, 14]}),
            stick((3.1, 9), (4.6, 14.5), 0.75, 1, handle), stick((12.9, 9), (11.4, 14.5), 0.75, 1, handle),
            cube([4.4, 14.1, 7.5], [11.6, 14.9, 8.5], handle)]


def pan(content):
    """A shallow dish facing the front (the sprite looks down into it): 12 x 12, rim 2 deep."""
    rim, outer = [3, 4, 13, 5], [1, 11, 15, 12]
    els = [cube([3, 3, 7], [13, 13, 8], {"south": content, "north": outer, "side": outer})]
    for frm, to in (([2, 2, 7], [14, 3, 10]), ([2, 13, 7], [14, 14, 10]), ([2, 3, 7], [3, 13, 10]), ([13, 3, 7], [14, 13, 10])):
        els.append(cube(frm, to, {"south": rim, "side": outer}))
    return els


def crucible(molten):
    """A clay crucible; molten: the glowing melt at the top (the animated texture's own pixels, full bright)."""
    body, inner, rim = [2, 7, 13, 12], [4, 4, 11, 5], [3, 2, 12, 3]
    els = [cube([4.5, 0, 4.5], [11.5, 2, 11.5], {"side": [3, 11, 12, 13], "up": inner, "down": [4, 12, 11, 13]})]
    els += hollow(3.5, 12.5, 3.5, 12.5, 2, 11, body, rim if molten else inner, rim)
    if molten:
        els.append(cube([4.5, 2, 4.5], [11.5, 10, 11.5], {"up": [3, 3, 11, 5]}, light=15))
    return els


# ------------------------------------------------------------------ tools (up the diagonal like the sprite)

def tongs():
    """Two bronze arms crossing at a rivet: handles at the bottom left, jaws at the top right."""
    a, b, pad = [11, 4, 12, 5], [10, 3, 11, 4], [11, 1, 12, 2]
    return [stick((1.5, 4), (14.5, 13), 1, 0.75, a, z=7.6),
            stick((4.5, 1), (11.5, 16), 1, 0.75, b, z=8.4),
            stick((13.8, 12.6), (13.0, 14.6), 1.25, 1.5, pad, z=7.6),  # jaws bent in towards each other
            stick((11.6, 15.6), (13.2, 14.4), 1.25, 1.5, pad, z=8.4),
            cube([7.25, 7.75, 6.9], [8.75, 9.25, 9.1], pad)]  # rivet


def broom():
    handle = {"north": [4, 11, 5, 12], "south": [4, 11, 5, 12], "side": [5, 11, 6, 12]}
    straw = {"side": [8, 2, 13, 7], "end": [9, 3, 11, 4]}
    return [stick((1, 1), (8, 8), 1, 1, handle),
            stick((7.2, 7.2), (8.9, 8.9), 2.2, 2.2, [5, 8, 7, 9]),
            stick((8.8, 8.8), (11.5, 11.5), 3.6, 2.2, straw),
            stick((11.4, 11.4), (15, 15), 5.4, 2.6, straw)]


def flail():
    return [stick((1.5, 4), (8.6, 11), 1, 1, [2, 10, 3, 11]),
            cube([7.9, 10.4, 7.25], [9.4, 11.9, 8.75], [8, 5, 10, 6]),  # leather joint
            stick((9.6, 11), (14.6, 5), 1.5, 1.5, [11, 8, 12, 9])]


def fire_drill():
    """Pump drill: spindle, cross bar on cords, flywheel low on the spindle."""
    return [cube([7.5, 0, 7.5], [8.5, 15.5, 8.5], {"north": [9, 2, 10, 13], "south": [9, 2, 10, 13], "side": [8, 2, 9, 13], "end": [8, 1, 10, 2]}),
            cube([2.5, 7.5, 7.25], [13.5, 10, 8.75], {"side": [3, 6, 14, 9], "east": [3, 7, 4, 8], "west": [3, 7, 4, 8], "end": [3, 7, 14, 8]}),
            cube([5, 1, 5], [11, 2.75, 11], {"side": [5, 14, 13, 16], "end": [5, 14, 11, 16]})]


def hammer(handle, head, end, w):
    """Handle up the diagonal, the head a real block across its top (w thick)."""
    c, half = (10.4, 10.4), 4.6
    return [stick((0.5, 0.5), (10.5, 10.5), 1, 1, handle),
            stick((c[0] - half / math.sqrt(2) * 1.0, c[1] + half / math.sqrt(2)), (c[0] + half / math.sqrt(2), c[1] - half / math.sqrt(2)),
                  w, w, {"side": head, "end": end})]


# ------------------------------------------------------------------ logs (block textures)

WOODS = sorted(p.name[:-len("_rough_log.json")] for p in MODELS.glob("*_rough_log.json"))


def wood_textures(w):
    def ns(name):
        return (M if (A / f"textures/block/{name}.png").exists() else "minecraft:") + "block/" + name
    return {"side": ns(f"{w}_log"), "end": ns(f"{w}_log_top"), "inner": ns(f"stripped_{w}_log"), "particle": ns(f"{w}_log")}


DIAG = {"origin": [8, 8, 8], "axis": "z", "angle": 45}


def log_piece(x0, x1, cy, cz, size, bark_faces, end_uv):
    """A log (or split piece) along x before the diagonal turn: bark faces carry the grain along the log."""
    h = size / 2
    faces = {}
    for name in ("north", "south", "up", "down"):
        long_ = round(x1 - x0, 3)
        faces[name] = {"uv": [8 - h, 8 - long_ / 2, 8 + h, 8 + long_ / 2], "rotation": 90,
                       "texture": "#side" if name in bark_faces else "#inner"}
    for name in ("east", "west"):
        faces[name] = {"uv": end_uv, "texture": "#end"}
    return {"from": [x0, cy - h, cz - h], "to": [x1, cy + h, cz + h], "rotation": DIAG, "faces": faces}


def rough_log():
    return [log_piece(1.5, 14.5, 8, 8, 5.5, ("north", "south", "up", "down"), [5.25, 5.25, 10.75, 10.75])]


FIREWOOD_SPOTS = {1: [(0, 0, 0)], 2: [(-1.6, 0, -0.6), (1.6, 0, 0.6)], 3: [(-1.6, -1.2, 0), (1.6, -1.2, 0.8), (0, 1.6, -0.8)]}


def firewood(n):
    """1-3 split quarters of a log: bark on two faces, split wood on the others."""
    return [log_piece(2.5 + dx, 13.5 + dx, 8 + dy, 8 + dz, 3, ("up", "south"), [8, 8, 11, 11]) for dy, dz, dx in FIREWOOD_SPOTS[n]]


# ------------------------------------------------------------------ the list

HELD = {
    "bark_basket": basket(),
    "bronze_bucket": bucket(**BRONZE_BUCKET),
    "bronze_water_bucket": bucket(**BRONZE_BUCKET, fill=[5, 3, 11, 6]),
    "bronze_milk_bucket": bucket(**BRONZE_BUCKET, fill=[5, 3, 11, 6]),
    "clay_bucket": bucket(**CLAY_BUCKET),
    "clay_water_bucket": bucket(**CLAY_BUCKET, fill=[5, 3, 11, 5]),
    "bronze_kettle": kettle(),
    "pan": pan([5, 6, 11, 10]),
    "pan_with_sand": pan([5, 6, 11, 10]),
    "pan_with_gravel": pan([5, 6, 11, 10]),
    "crucible": crucible(False),
    "molten_crucible": crucible(True),
    "molten_crucible_bronze": crucible(True),
    "bronze_tongs": tongs(),
    "broom": broom(),
    "flail": flail(),
    "fire_drill": fire_drill(),
    "bronze_smithing_hammer": hammer([1, 13, 3, 14], [7, 3, 12, 7], [10, 6, 13, 8], 3.4),
    "iron_smithing_hammer": hammer([1, 13, 3, 14], [7, 3, 12, 7], [10, 6, 13, 8], 3.4),
    "bronze_quarry_hammer": hammer([2, 12, 4, 13], [6, 2, 12, 7], [9, 4, 13, 6], 4.4),
    "steel_quarry_hammer": hammer([2, 12, 4, 13], [6, 2, 12, 7], [9, 4, 13, 6], 4.4),
}
LOGS = {}
for w in WOODS:
    LOGS[f"{w}_rough_log"] = (rough_log(), wood_textures(w))
    for n in (1, 2, 3):
        LOGS[f"{w}_firewood_{n}"] = (firewood(n), wood_textures(w))

# first person, for the items HoldPoses places itself (it applies the position, these the model's turn and size)
FIRST_PERSON = {
    "basket": ([0, 25, 0], [0, 0, 0], 0.6),
    "bucket": ([0, 20, 0], [0, 0, 0], 0.55),
    "carry": ([0, 0, -45], [0, 0, 0], 0.7),   # the log turned to lie across, in both arms
    "kettle": ([0, 30, 0], [0, 0, 0], 0.45),
}
FP_KIND = {"bark_basket": "basket", "bronze_kettle": "kettle", **{n: "bucket" for n in HELD if "bucket" in n},
           **{n: "carry" for n in LOGS}}

GENERATED = {"ground": {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [0.5, 0.5, 0.5]},
             "head": {"rotation": [0, 180, 0], "translation": [0, 13, 7], "scale": [1, 1, 1]},
             "thirdperson_righthand": {"rotation": [0, 0, 0], "translation": [0, 3, 1], "scale": [0.55, 0.55, 0.55]},
             "thirdperson_lefthand": {"rotation": [0, 0, 0], "translation": [0, 3, 1], "scale": [0.55, 0.55, 0.55]},
             "firstperson_righthand": {"rotation": [0, -90, 25], "translation": [1.13, 3.2, 1.13], "scale": [0.68, 0.68, 0.68]},
             "firstperson_lefthand": {"rotation": [0, 90, -25], "translation": [1.13, 3.2, 1.13], "scale": [0.68, 0.68, 0.68]},
             "fixed": {"rotation": [0, 180, 0], "scale": [1, 1, 1]}}
HANDHELD = {**GENERATED,
            "thirdperson_righthand": {"rotation": [0, -90, 55], "translation": [0, 4.0, 0.5], "scale": [0.85, 0.85, 0.85]},
            "thirdperson_lefthand": {"rotation": [0, 90, -55], "translation": [0, 4.0, 0.5], "scale": [0.85, 0.85, 0.85]}}


def display(name, flat):
    d = copy.deepcopy(HANDHELD if flat.get("parent", "").endswith("handheld") else GENERATED)
    for k in ("thirdperson_righthand", "thirdperson_lefthand"):  # the sprite's own hold transform (gen_hold_displays rewrites these)
        if k in flat.get("display", {}):
            d[k] = flat["display"][k]
    if name.startswith("pan"):
        d["ground"] = {"rotation": [-90, 0, 0], "translation": [0, 1, 0], "scale": [0.5, 0.5, 0.5]}  # lying flat
    if name in FP_KIND:
        rot, tr, s = FIRST_PERSON[FP_KIND[name]]
        d["firstperson_righthand"] = {"rotation": rot, "translation": tr, "scale": [s, s, s]}
        d["firstperson_lefthand"] = copy.deepcopy(d["firstperson_righthand"])  # ItemTransform.apply mirrors left-hand transforms itself
    return d


def write(path, obj):
    path.write_text(json.dumps(obj, indent=2) + "\n", encoding="utf-8")


written = {}
for name, elements in HELD.items():
    flat = json.loads((MODELS / f"{name}.json").read_text(encoding="utf-8"))
    tex = flat["textures"]["layer0"]
    written[name] = {"textures": {"t": tex, "particle": tex}, "elements": elements, "display": display(name, flat)}
for name, (elements, textures) in LOGS.items():
    flat = json.loads((MODELS / f"{name}.json").read_text(encoding="utf-8"))
    written[name] = {"textures": textures, "elements": elements, "display": display(name, flat)}
for name, model in written.items():
    for e in model["elements"]:
        assert all(-16 <= v <= 32 for v in e["from"] + e["to"]), (name, e)
    write(MODELS / f"{name}_held.json", model)


def held_tree(node):
    """The definition's model tree with every model that has a _held version swapped for it."""
    if isinstance(node, dict):
        out = {k: held_tree(v) for k, v in node.items()}
        ref = node.get("model")
        if node.get("type") == "minecraft:model" and isinstance(ref, str) and ref.startswith(M + "item/") and ref[len(M + "item/"):] in written:
            out["model"] = ref + "_held"
        return out
    if isinstance(node, list):
        return [held_tree(v) for v in node]
    return node


def ours(node):
    return (node.get("property") == "minecraft:display_context" and len(node.get("cases", [])) == 1
            and node["cases"][0].get("when") == ["gui"])


wrapped = 0
for path in sorted(ITEMS.glob("*.json")):
    item = json.loads(path.read_text(encoding="utf-8"))
    tree = item["model"]
    if ours(tree):
        tree = tree["cases"][0]["model"]
    held = held_tree(tree)
    if held == tree:
        continue
    item["model"] = {"type": "minecraft:select", "property": "minecraft:display_context",
                     "cases": [{"when": ["gui"], "model": tree}], "fallback": held}
    write(path, item)
    wrapped += 1
assert wrapped >= len(WOODS) * 2 + 15, wrapped
print(f"{len(written)} held models, {wrapped} item definitions")
