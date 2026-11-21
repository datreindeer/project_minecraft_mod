"""3D models in hand, on the ground and in item frames (the GUI keeps the sprite) - only where they beat the extruded
sprite: rough logs and firewood (their wood's real log / stripped log / log top block textures, more wood the bigger the
stack) and the Bark Basket (a woven-bark texture painted here, textures/item/bark_basket_weave.png).
Every other held item stays the extruded sprite, which keeps every pixel of its hand-made texture.

Models are "<model>_held" ("<wood>_rough_log_held_2/_3" for bigger stacks). Item definitions are wrapped in a
display_context select: "gui" shows the old tree, everything else the same tree with each model swapped for its _held one
(firewood's own count variants keep working; rough logs get a count dispatch here). Re-running is safe: a wrapped
definition is unwrapped first, and definitions / _held files of items no longer listed are restored / deleted.
In hand the logs and the basket are placed by client/HoldPoses / FirstPersonHolds (ItemDisplayContext.NONE), so only
ground / fixed / on_shelf displays matter here.
Generator order: after gen_staffs.py / gen_block_items.py (every flat model and definition exists), before
gen_hold_displays.py. Run: python scripts/gen_held_models.py
"""
import json
import math
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
A = ROOT / "src/main/resources/assets/mechanical"
MODELS, ITEMS = A / "models/item", A / "items"
M = "mechanical:"
DIRS = {"n": "north", "s": "south", "e": "east", "w": "west", "u": "up", "d": "down"}


def cube(frm, to, uv, tex="#t", rot=None):
    """A cuboid. uv: one rect for every face, {"side": .., "end": ..} (end: up/down), or per face name."""
    faces = {}
    for name in DIRS.values():
        r = uv.get(name) or (uv.get("end") if name in ("up", "down") else None) or uv.get("side") if isinstance(uv, dict) else uv
        if r is None:
            continue
        face = {"uv": list(r[:4]), "texture": tex}
        if len(r) > 4:
            face["rotation"] = r[4]
        faces[name] = face
    e = {"from": [round(v, 4) for v in frm], "to": [round(v, 4) for v in to], "faces": faces}
    if rot:
        e["rotation"] = rot
    return e


def stick(p0, p1, w, d, uv, z=8.0):
    """A bar in the front plane from 2D point p0 to p1 (model x, y), w wide, d deep."""
    cx, cy = (p0[0] + p1[0]) / 2, (p0[1] + p1[1]) / 2
    length = math.dist(p0, p1)
    angle = math.degrees(math.atan2(p1[1] - p0[1], p1[0] - p0[0])) - 90
    return cube([cx - w / 2, cy - length / 2, z - d / 2], [cx + w / 2, cy + length / 2, z + d / 2], uv,
                rot={"origin": [round(cx, 4), round(cy, 4), z], "axis": "z", "angle": round(angle, 3)})


# ------------------------------------------------------------------ the Bark Basket and its woven texture

def rgb(h):
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4)) + (255,)


# bark_basket.png's own colours, dark -> light
DARK, DARK2, SHADE, MID, MID2, BARK, LIGHT, LIGHT2, HI = map(rgb, ["241b12", "2e2419", "3d2b1e", "4c3d26", "5c4319", "665844",
                                                                    "87755b", "987849", "be9d5f"])


def weave_texture():
    """16x16: rows 0-7 the outside wall (over-under bark strips, 2 px weft x 4 px warp), 8-9 the braided rim, 10-11 the
    bark-wrapped handle, 12-15 the inside (the same weave in shadow)."""
    img = Image.new("RGBA", (16, 16))
    px = img.putpixel

    def weave(y0, rows, light, mid, dark, deep):
        for y in range(rows):
            for x in range(16):
                row, col = y // 2, x // 4
                weft_over = (row + col) % 2 == 0
                if weft_over:  # horizontal strip on top: lit upper edge, shaded lower edge, darker where it dives under
                    c = light if y % 2 == 0 else mid
                    if x % 4 in (0, 3):
                        c = mid if y % 2 == 0 else dark
                else:  # vertical stake on top: lit left side, shaded right side
                    c = (mid, light, light, dark)[x % 4]
                    if y % 2 == 1 and x % 4 == 3:
                        c = deep
                px((x, y0 + y), c)

    weave(0, 8, LIGHT2, BARK, MID, SHADE)
    for x in range(16):  # rim: two twisted bark ropes
        px((x, 8), (HI, LIGHT2, BARK, MID)[x % 4])
        px((x, 9), (BARK, MID, SHADE, LIGHT)[x % 4])
    for x in range(16):  # handle: bark wrapped round in a spiral
        px((x, 10), (LIGHT2, LIGHT, BARK, MID2)[x % 4])
        px((x, 11), (BARK, MID2, LIGHT2, LIGHT)[x % 4])
    weave(12, 4, MID, SHADE, DARK2, DARK)
    for x in range(16):  # the bottom edge of the wall
        px((x, 7), SHADE if x % 4 == 3 else MID)
    return img


WALL, RIM, HANDLE, INSIDE = [0, 0, 16, 8], [0, 8, 16, 9.5], [0, 10, 4, 12], [0, 12, 16, 16]


def hollow(x0, x1, z0, z1, y0, y1, t=1.0):
    """Four woven walls (t thick) of an open basket: outside the weave, inside the shaded weave, rim on top."""
    def uv(width):  # outside weave, one texel per model px across, the full 8 rows down the 7 px wall
        return [0, 0, min(16, width), 8]
    walls = [([x0, y0, z0], [x1, y1, z0 + t], "south"), ([x0, y0, z1 - t], [x1, y1, z1], "north"),
             ([x0, y0, z0 + t], [x0 + t, y1, z1 - t], "east"), ([x1 - t, y0, z0 + t], [x1, y1, z1 - t], "west")]
    out = []
    for frm, to, inward in walls:
        width = max(to[0] - frm[0], to[2] - frm[2])
        out.append(cube(frm, to, {"side": uv(width), "up": [0, 8, width, 9], "down": [0, 7, width, 8],
                                  inward: [0, 12, min(16, width), 16]}))
    return out


def basket():
    """Woven bark basket, 12 x 8, 7 tall, with a bark-wrapped handle across."""
    els = [cube([2.5, 0, 4.5], [13.5, 1, 11.5], {"side": [0, 7, 11, 8], "up": [0, 12, 11, 16], "down": [0, 12, 11, 16]})]
    els += hollow(2, 14, 4, 12, 1, 8)
    els += [cube([1.75, 7.25, 3.75], [14.25, 8.5, 4.75], {"side": RIM, "end": RIM}),  # braided rim, proud of the walls
            cube([1.75, 7.25, 11.25], [14.25, 8.5, 12.25], {"side": RIM, "end": RIM}),
            cube([1.75, 7.25, 4.75], [2.75, 8.5, 11.25], {"side": RIM, "end": RIM}),
            cube([13.25, 7.25, 4.75], [14.25, 8.5, 11.25], {"side": RIM, "end": RIM})]
    h = {"side": [0, 10, 16, 12, 90], "end": [0, 10, 2, 12]}
    els += [stick((3.2, 7.5), (3.2, 12.0), 1, 1, h), stick((12.8, 7.5), (12.8, 12.0), 1, 1, h),
            stick((3.2, 11.6), (5.4, 14.9), 1, 1, h), stick((12.8, 11.6), (10.6, 14.9), 1, 1, h),
            cube([5, 14.4, 7.5], [11, 15.4, 8.5], {"side": [0, 10, 16, 12], "end": [0, 10, 6, 11]})]
    return els


# ------------------------------------------------------------------ logs (block textures), lying along x

WOODS = sorted(p.name[:-len("_rough_log.json")] for p in MODELS.glob("*_rough_log.json"))


def wood_textures(w):
    def ns(name):
        return (M if (A / f"textures/block/{name}.png").exists() else "minecraft:") + "block/" + name
    return {"side": ns(f"{w}_log"), "end": ns(f"{w}_log_top"), "inner": ns(f"stripped_{w}_log"), "particle": ns(f"{w}_log")}


def log_piece(x0, x1, cy, cz, size, bark_faces, end_uv, tilt=0.0):
    """A log or split piece along x; bark faces carry the grain along it. tilt: degrees round x (a piece lying askew)."""
    h = size / 2
    long_ = round(x1 - x0, 3)
    faces = {}
    for name in ("north", "south", "up", "down"):
        faces[name] = {"uv": [8 - h, 8 - long_ / 2, 8 + h, 8 + long_ / 2], "rotation": 90,
                       "texture": "#side" if name in bark_faces else "#inner"}
    for name in ("east", "west"):
        faces[name] = {"uv": end_uv, "texture": "#end"}
    e = {"from": [x0, cy - h, cz - h], "to": [x1, cy + h, cz + h], "faces": faces}
    if tilt:
        e["rotation"] = {"origin": [8, cy, cz], "axis": "x", "angle": tilt}
    return e


BARK_ALL = ("north", "south", "up", "down")
# rough logs: 1, 2 side by side with one on top... (centre y, z, length offset)
ROUGH = {1: [(8, 8, 0)], 2: [(5.5, 5.4, -0.5), (5.5, 10.6, 0.5)], 3: [(5.2, 5.2, -0.5), (5.2, 10.8, 0.6), (9.8, 8, 0)]}
# firewood: a split piece, three, an armful of six (y, z, x offset, tilt)
SPLIT = {1: [(8, 8, 0, 0)],
         2: [(6.5, 6.4, -0.8, 0), (6.5, 9.6, 0.6, 0), (9.4, 8, 0, 45)],
         3: [(5.0, 4.4, -0.6, 0), (5.0, 7.6, 0.8, 0), (5.0, 10.8, -0.2, 0), (8.0, 6.0, 0.4, 45), (8.0, 9.2, -0.8, 0), (11.0, 7.6, 0.2, 45)]}


def rough_log(n):
    return [log_piece(1.5 + dx, 14.5 + dx, y, z, 5.2 if n > 1 else 6, BARK_ALL, [5, 5, 11, 11]) for y, z, dx in ROUGH[n]]


def firewood(n):
    return [log_piece(2.5 + dx, 13.5 + dx, y, z, 3, ("up", "south"), [8, 8, 11, 11], tilt) for y, z, dx, tilt in SPLIT[n]]


GROUND = {"ground": {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [0.5, 0.5, 0.5]},
          "fixed": {"rotation": [0, 180, 0], "scale": [1, 1, 1]},
          "head": {"rotation": [0, 180, 0], "translation": [0, 13, 7], "scale": [1, 1, 1]}}
LOG_DISPLAY = {"ground": {"rotation": [0, 30, 0], "translation": [0, 2, 0], "scale": [0.5, 0.5, 0.5]},
               "fixed": {"rotation": [0, 180, 0], "scale": [0.9, 0.9, 0.9]},
               "on_shelf": {"rotation": [0, 0, 0], "translation": [0, 0, 0], "scale": [0.9, 0.9, 0.9]},
               "head": {"rotation": [0, 180, 0], "translation": [0, 13, 7], "scale": [1, 1, 1]}}

written = {}
tex = A / "textures/item"
weave_texture().save(tex / "bark_basket_weave.png")
flat_basket = json.loads((MODELS / "bark_basket.json").read_text(encoding="utf-8"))
written["bark_basket"] = {"textures": {"t": M + "item/bark_basket_weave", "particle": M + "item/bark_basket"}, "elements": basket(),
                          "display": {**GROUND, **{k: v for k, v in flat_basket.get("display", {}).items() if k.startswith("thirdperson")}}}
for w in WOODS:
    t = wood_textures(w)
    written[f"{w}_rough_log"] = {"textures": t, "elements": rough_log(1), "display": LOG_DISPLAY}
    for n in (2, 3):
        written[f"{w}_rough_log/{n}"] = {"textures": t, "elements": rough_log(n), "display": LOG_DISPLAY}
    for n in (1, 2, 3):
        written[f"{w}_firewood_{n}"] = {"textures": t, "elements": firewood(n), "display": LOG_DISPLAY}


def held_name(key):
    base, _, n = key.partition("/")
    return f"{base}_held" + (f"_{n}" if n else "")


def write(path, obj):
    path.write_text(json.dumps(obj, indent=2) + "\n", encoding="utf-8")


keep = set()
for key, model in written.items():
    for e in model["elements"]:
        assert all(-16 <= v <= 32 for v in e["from"] + e["to"]), (key, e)
    write(MODELS / f"{held_name(key)}.json", model)
    keep.add(f"{held_name(key)}.json")
stale = [p for p in MODELS.glob("*_held*.json") if p.name not in keep]
for p in stale:
    p.unlink()


def held_tree(node):
    """The definition's model tree with every model that has a _held version swapped for it (rough logs: by count)."""
    if isinstance(node, dict):
        out = {k: held_tree(v) for k, v in node.items()}
        ref = node.get("model")
        if node.get("type") == "minecraft:model" and isinstance(ref, str) and ref.startswith(M + "item/") and ref[len(M + "item/"):] in written:
            name = ref[len(M + "item/"):]
            out["model"] = ref + "_held"
            if f"{name}/2" in written:
                return {"type": "minecraft:range_dispatch", "property": "minecraft:count", "normalize": False,
                        "entries": [{"threshold": 2, "model": {"type": "minecraft:model", "model": ref + "_held_2"}},
                                    {"threshold": 5, "model": {"type": "minecraft:model", "model": ref + "_held_3"}}],
                        "fallback": out}
        return out
    if isinstance(node, list):
        return [held_tree(v) for v in node]
    return node


def ours(node):
    return (node.get("property") == "minecraft:display_context" and len(node.get("cases", [])) == 1
            and node["cases"][0].get("when") == ["gui"])


wrapped = restored = 0
for path in sorted(ITEMS.glob("*.json")):
    item = json.loads(path.read_text(encoding="utf-8"))
    tree = item["model"]
    was = ours(tree)
    if was:
        tree = tree["cases"][0]["model"]
    held = held_tree(tree)
    if held == tree:
        if was:  # no 3D model any more: back to the plain definition
            item["model"] = tree
            write(path, item)
            restored += 1
        continue
    item["model"] = {"type": "minecraft:select", "property": "minecraft:display_context",
                     "cases": [{"when": ["gui"], "model": tree}], "fallback": held}
    write(path, item)
    wrapped += 1
assert wrapped == len(WOODS) * 2 + 1, wrapped
print(f"{len(written)} held models, {wrapped} item definitions wrapped, {restored} restored, {len(stale)} stale models removed")
