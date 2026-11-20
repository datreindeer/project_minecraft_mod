"""Bark (Mechanical.BARKS) and Bark Roof, one per wood (BarkRoofBlock, Mechanical.BARK_ROOFS): models, blockstates, loot tables, recipes, tags, lang.

- A slope is a real 45-degree plate (high edge north in the models, turned by the blockstate). Corners follow vanilla
  stairs: the inner corner (high north and east) is two crossing plates, of which the upper one shows from above; the outer
  corner (peaking north-west) is cut along its diagonal into strips, the south-west half from the north slope and the
  north-east half from the west slope. Flat: a 2 px layer on the floor of the block.
- Top: the wood's bark shingles (gen_bark_roof_textures.py), underside: the wood's stripped log.
- Bark: <wood>_bark items (texture from gen_rough_log_textures.py), item tag mechanical:barks that every bark use takes.
  The old single mechanical:bark is aliased to oak_bark in Java, so its lang key and model are dropped here.
- Recipe (building): 3 of that wood's Bark in a slope (B. / BB) -> 4.
- Tags: minecraft:mineable/axe; item tag mechanical:bark_roofs (advancement stone_age/bark_roof in gen_advancements.py).
Generator order: ... gen_tribes2.py, gen_bark_roof.py, gen_wildtweaks.py ... (before gen_block_items, gen_unlocks).
Run: python scripts/gen_bark_roof.py"""
import json
import math
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent / "src/main/resources"
A, D = ROOT / "assets/mechanical", ROOT / "data"
M = "mechanical:"
WOODS = ["oak", "spruce", "birch", "jungle", "acacia", "dark_oak", "mangrove", "cherry", "pale_oak", "poplar",
         "aspen", "alder", "beech", "linden", "willow", "ash", "hazel", "pine", "yew"]
NEW_WOODS = ["aspen", "alder", "beech", "linden", "willow", "ash", "hazel", "pine", "yew"]
ROT = {"north": 0, "east": 90, "south": 180, "west": 270}
CW = {"north": "east", "east": "south", "south": "west", "west": "north"}
CCW = {v: k for k, v in CW.items()}
STRIPS = 8  # strips per half of an outer corner
R2 = math.sqrt(2)


def w(path, obj):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, indent=2) + "\n", encoding="utf-8")


def merge(path, obj, drop=()):
    old = json.loads(path.read_text(encoding="utf-8")) if path.exists() else {}
    old.update(obj)
    for k in drop:
        old.pop(k, None)
    path.write_text(json.dumps(old, indent=2) + "\n", encoding="utf-8")


def ns(wood):
    return "mechanical" if wood in NEW_WOODS else "minecraft"


def r(v):
    return round(v, 4)


EDGE = {"texture": "#top", "uv": [0, 0, 16, 1]}


# ---------------------------------------------------------------- geometry

def north_strip(x0, x1, za, zb):
    """Part of the slope y = 16 - z (high north) over x0..x1, z za..zb: a 1 px plate tipped 45 degrees about x."""
    zc = (za + zb) / 2
    yc, half = 16 - zc, (zb - za) * R2 / 2
    return {"from": [r(x0), r(yc - 0.5), r(zc - half)], "to": [r(x1), r(yc + 0.5), r(zc + half)],
            "rotation": {"origin": [8, r(yc), r(zc)], "axis": "x", "angle": 45},
            "faces": {"up": {"texture": "#top", "uv": [r(x0), r(za), r(x1), r(zb)]},
                      "down": {"texture": "#under", "uv": [r(x0), r(za), r(x1), r(zb)]},
                      "east": EDGE, "west": EDGE, "north": EDGE, "south": EDGE}}


def west_strip(z0, z1, xa, xb):
    """Part of the slope y = 16 - x (high west) over z0..z1, x xa..xb: tipped -45 degrees about z, shingle rows along z."""
    xc = (xa + xb) / 2
    yc, half = 16 - xc, (xb - xa) * R2 / 2
    uv = [r(16 - z1), r(xa), r(16 - z0), r(xb)]
    return {"from": [r(xc - half), r(yc - 0.5), r(z0)], "to": [r(xc + half), r(yc + 0.5), r(z1)],
            "rotation": {"origin": [r(xc), r(yc), 8], "axis": "z", "angle": -45},
            "faces": {"up": {"texture": "#top", "uv": uv, "rotation": 270}, "down": {"texture": "#under", "uv": uv, "rotation": 270},
                      "east": EDGE, "west": EDGE, "north": EDGE, "south": EDGE}}


def full_north():
    return {"from": [0, 7.5, 0], "to": [16, 8.5, 16], "rotation": {"origin": [8, 8, 8], "axis": "x", "angle": 45, "rescale": True},
            "faces": {"up": {"texture": "#top", "uv": [0, 0, 16, 16]}, "down": {"texture": "#under", "uv": [0, 0, 16, 16]},
                      "east": EDGE, "west": EDGE}}


def full_east():
    """The slope y = x (high east), shingle rows along z, their lower edges toward the west."""
    return {"from": [0, 7.5, 0], "to": [16, 8.5, 16], "rotation": {"origin": [8, 8, 8], "axis": "z", "angle": 45, "rescale": True},
            "faces": {"up": {"texture": "#top", "uv": [0, 0, 16, 16], "rotation": 90}, "down": {"texture": "#under", "uv": [0, 0, 16, 16], "rotation": 90},
                      "north": EDGE, "south": EDGE}}


def outer_elements():
    """min(north slope, west slope): z > x is the north slope's half, x > z the west slope's, each in strips."""
    s, out = 16 / STRIPS, []
    for i in range(STRIPS):
        out.append(north_strip(i * s, (i + 1) * s, i * s, 16))
        out.append(west_strip(i * s, (i + 1) * s, i * s, 16))
    return out


def rotate(p, origin, axis, angle, rescale=False):
    """Minecraft's element rotation (as FaceBakery: a right-handed rotation about the axis through origin)."""
    a = math.radians(angle)
    x, y, z = (p[i] - origin[i] for i in range(3))
    c, s = math.cos(a), math.sin(a)
    if axis == "x":
        y, z = y * c - z * s, y * s + z * c
        if rescale:
            y, z = y * R2, z * R2
    elif axis == "z":
        x, y = x * c - y * s, x * s + y * c
        if rescale:
            x, y = x * R2, y * R2
    return (x + origin[0], y + origin[1], z + origin[2])


def top_corners(e):
    """The four corners of an element's up face, after rotation."""
    f, t = e["from"], e["to"]
    rot = e["rotation"]
    return [rotate((x, t[1], z), rot["origin"], rot["axis"], rot["angle"], rot.get("rescale", False))
            for x in (f[0], t[0]) for z in (f[2], t[2])]


def check():
    """Self-check: every plate's top lies its half-thickness (0.71 px up, 1 px rescaled) above the surface it stands for, within the block."""
    def near(e, h, tol):
        for x, y, z in top_corners(e):
            assert abs(y - h(x, z)) < tol, (e["from"], e["to"], x, y, z, h(x, z))
    near(full_north(), lambda x, z: 16 - z + 1.0, 0.01)  # rescaled: the 0.5 px half-thickness grows to 0.71 across, 1 px up
    near(full_east(), lambda x, z: x + 1.0, 0.01)
    for e in outer_elements():
        h = (lambda x, z: 16 - z + 0.707) if e["rotation"]["axis"] == "x" else (lambda x, z: 16 - x + 0.707)
        near(e, h, 0.01)
        for x, y, z in top_corners(e):
            assert -0.8 < x < 16.8 and -0.8 < z < 16.8, (x, z)
    # the strips together cover the corner's surface: each point of the block lies under a strip of its own half
    s = 16 / STRIPS
    for gx in range(16):
        for gz in range(16):
            x, z = gx + 0.5, gz + 0.5
            i = int((x if z > x else z) // s)
            assert (i * s <= x < (i + 1) * s and z >= i * s) if z > x else (i * s <= z < (i + 1) * s and x >= i * s)


# ---------------------------------------------------------------- models, blockstates

def template(elements):
    return {"parent": "minecraft:block/block", "ambientocclusion": False,
            "textures": {"particle": "#top"}, "elements": elements}


def models():
    b = A / "models/block"
    w(b / "bark_roof_straight.json", template([full_north()]))
    w(b / "bark_roof_inner.json", template([full_north(), full_east()]))
    w(b / "bark_roof_outer.json", template(outer_elements()))
    w(b / "bark_roof_flat.json", template([{"from": [0, 0, 0], "to": [16, 2, 16], "faces": {
        "up": {"texture": "#top", "uv": [0, 0, 16, 16]}, "down": {"texture": "#under", "uv": [0, 0, 16, 16]},
        **{f: {"texture": "#top", "uv": [0, 14, 16, 16]} for f in ("north", "south", "east", "west")}}}]))
    for wood in WOODS:
        tex = {"top": f"{M}block/{wood}_bark_roof", "under": f"{ns(wood)}:block/stripped_{wood}_log"}
        for shape in ("straight", "inner", "outer", "flat"):
            w(b / f"{wood}_bark_roof_{shape}.json", {"parent": f"{M}block/bark_roof_{shape}", "textures": tex})
        variants = {}
        for flat in ("false", "true"):
            for facing in ROT:
                for shape in ("straight", "inner_left", "inner_right", "outer_left", "outer_right"):
                    if flat == "true":
                        m = {"model": f"{M}block/{wood}_bark_roof_flat"}
                    else:
                        # as vanilla stairs: outer_left / inner_right use the facing, outer_right turns cw, inner_left ccw
                        turn = CW[facing] if shape == "outer_right" else CCW[facing] if shape == "inner_left" else facing
                        m = {"model": f"{M}block/{wood}_bark_roof_{shape.split('_')[0]}"}
                        if ROT[turn]:
                            m["y"] = ROT[turn]
                    variants[f"facing={facing},flat={flat},shape={shape}"] = m
        w(A / f"blockstates/{wood}_bark_roof.json", {"variants": variants})


# ---------------------------------------------------------------- recipes, loot, tags, lang

def name(wood):
    return " ".join(p.capitalize() for p in wood.split("_"))


def data():
    for wood in WOODS:
        b = f"{wood}_bark_roof"
        w(A / f"models/item/{wood}_bark.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"{M}item/{wood}_bark"}})
        w(A / f"items/{wood}_bark.json", {"model": {"type": "minecraft:model", "model": f"{M}item/{wood}_bark"}})
        w(D / f"mechanical/recipe/{b}.json", {"type": "minecraft:crafting_shaped", "category": "building", "pattern": ["B ", "BB"],
                                               "key": {"B": f"{M}{wood}_bark"}, "result": {"id": M + b, "count": 4}})
        w(D / f"mechanical/loot_table/blocks/{b}.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [
            {"type": "minecraft:item", "name": M + b}], "conditions": [{"condition": "minecraft:survives_explosion"}]}],
            "random_sequence": f"mechanical:blocks/{b}"})
    w(D / "mechanical/tags/item/barks.json", {"values": [f"{M}{x}_bark" for x in WOODS]})
    for old in (A / "models/item/bark.json", A / "items/bark.json"):
        old.unlink(missing_ok=True)
    ids = [f"{M}{x}_bark_roof" for x in WOODS]
    w(D / "mechanical/tags/item/bark_roofs.json", {"values": ids})
    w(D / "mechanical/tags/block/bark_roofs.json", {"values": ids})
    axe = D / "minecraft/tags/block/mineable/axe.json"
    tag = json.loads(axe.read_text(encoding="utf-8"))
    tag["values"] = [v for v in tag["values"] if v not in ids] + ids
    w(axe, tag)
    merge(A / "lang/en_us.json", {f"block.mechanical.{x}_bark_roof": f"{name(x)} Bark Roof" for x in WOODS}
          | {f"item.mechanical.{x}_bark": f"{name(x)} Bark" for x in WOODS}, drop=["item.mechanical.bark"])


if __name__ == "__main__":
    check()
    models()
    data()
    print(f"bark roof: {len(WOODS)} blocks")
