"""Soot (SootBlock, grown by Smoke.java): textures for the three darkness stages and the item, block models (a thin
layer like Glow Lichen), the multipart blockstate, loot (the Soot item, more with a knife), Soot -> Black Dye, lang.
Generator order: ... gen_wind.py, gen_smoke.py, gen_soot.py, gen_block_items.py, gen_registry.py.
Run: python scripts/gen_soot.py"""
import json
import random
import zipfile
from io import BytesIO
from pathlib import Path

from PIL import Image

from gen_crude_ranged_textures import JAR

ROOT = Path(__file__).resolve().parent.parent / "src/main/resources"
A, D = ROOT / "assets/mechanical", ROOT / "data"
STAGES = (1, 2, 3)


def w(path, obj):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, indent=2) + "\n")


def merge(path, obj):
    old = json.loads(path.read_text(encoding="utf-8")) if path.exists() else {}
    old.update(obj)
    path.write_text(json.dumps(old, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")


# ---- textures ----
def value_noise(rnd, cells):
    """Tileable 16x16 value noise (smooth-interpolated random grid that wraps), 0..1."""
    grid = [[rnd.random() for _ in range(cells)] for _ in range(cells)]
    out = [[0.0] * 16 for _ in range(16)]
    for y in range(16):
        for x in range(16):
            fx, fy = x * cells / 16, y * cells / 16
            x0, y0 = int(fx), int(fy)
            tx, ty = fx - x0, fy - y0
            tx, ty = tx * tx * (3 - 2 * tx), ty * ty * (3 - 2 * ty)
            g = lambda i, j: grid[(y0 + j) % cells][(x0 + i) % cells]
            top = g(0, 0) * (1 - tx) + g(1, 0) * tx
            bottom = g(0, 1) * (1 - tx) + g(1, 1) * tx
            out[y][x] = top * (1 - ty) + bottom * ty
    return out


rnd = random.Random(4711)
# one shared field so a darker stage grows out of the lighter one's smudges; tileable so neighbouring blocks join up
octaves = [(value_noise(rnd, 2), 0.6), (value_noise(rnd, 4), 0.28), (value_noise(rnd, 8), 0.12)]
field = [[sum(n[y][x] * wt for n, wt in octaves) for x in range(16)] for y in range(16)]
lo, hi = min(map(min, field)), max(map(max, field))
field = [[(v - lo) / (hi - lo) for v in row] for row in field]
speck = [[rnd.random() for _ in range(16)] for _ in range(16)]

# stage: (field threshold, max alpha, darkest grey, lightest grey)
LOOK = {1: (0.42, 90, 36, 60), 2: (0.30, 180, 22, 44), 3: (0.04, 235, 10, 30)}
for stage, (cut, amax, dark, light) in LOOK.items():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y in range(16):
        for x in range(16):
            v = field[y][x] + (speck[y][x] - 0.5) * 0.1  # flecks break up the soft edges
            if v < cut:
                continue
            t = min(1.0, (v - cut) / (1 - cut) * 1.6)  # 0 at the smudge edge, 1 in its thickest part
            alpha = int(40 + (amax - 40) * t)
            grey = int(light - (light - dark) * t)
            img.putpixel((x, y), (grey, grey, max(0, grey - 3), alpha))
    assert img.getbbox() is not None
    (A / "textures/block").mkdir(parents=True, exist_ok=True)
    img.save(A / f"textures/block/soot_{stage}.png")

# item: vanilla gunpowder's heap of powder, recoloured to soot (dark greys, the lightest grains a dull ash grey)
gunpowder = Image.open(BytesIO(zipfile.ZipFile(JAR).read("assets/minecraft/textures/item/gunpowder.png"))).convert("RGBA")
item = Image.new("RGBA", gunpowder.size, (0, 0, 0, 0))
for y in range(gunpowder.height):
    for x in range(gunpowder.width):
        r, g, b, a = gunpowder.getpixel((x, y))
        if a:
            lum = (r * 299 + g * 587 + b * 114) / 1000 / 255
            grey = int(12 + lum ** 1.4 * 78)
            item.putpixel((x, y), (grey, grey, grey - 2 if grey > 2 else grey, a))
(A / "textures/item").mkdir(parents=True, exist_ok=True)
item.save(A / "textures/item/soot.png")

# ---- models, blockstate ----
for stage in STAGES:
    tex = f"mechanical:block/soot_{stage}"
    w(A / f"models/block/soot_{stage}.json", {"ambientocclusion": False, "textures": {"particle": tex, "soot": tex},
        "elements": [{"from": [0, 0, 0.1], "to": [16, 16, 0.1], "faces": {
            "north": {"uv": [16, 0, 0, 16], "texture": "#soot"}, "south": {"uv": [0, 0, 16, 16], "texture": "#soot"}}}]})
ROT = {"north": {}, "east": {"y": 90}, "south": {"y": 180}, "west": {"y": 270}, "up": {"x": 270}, "down": {"x": 90}}
w(A / "blockstates/soot.json", {"multipart": [
    {"apply": {"model": f"mechanical:block/soot_{stage}", **({"uvlock": True} if rot else {}), **rot}, "when": {face: "true", "stage": str(stage)}}
    for stage in STAGES for face, rot in ROT.items()]})
w(A / "items/soot.json", {"model": {"type": "minecraft:model", "model": "mechanical:item/soot"}})
w(A / "models/item/soot.json", {"parent": "minecraft:item/generated", "textures": {"layer0": "mechanical:item/soot"}})


# ---- loot: by hand a light smudge gives Soot half the time, darker 1 / 2; a knife scrapes off 1 more ----
def at(stage):
    return {"type": "minecraft:match_block", "blocks": "mechanical:soot", "state": {"stage": str(stage)}}


def pool(condition, count=1):
    entry = {"type": "minecraft:item", "name": "mechanical:soot"}
    if count > 1:
        entry["modifier"] = [{"type": "minecraft:set_count", "count": count}]
    return {"rolls": 1, "condition": condition, "entries": [entry]}


KNIFE = {"type": "minecraft:match_tool", "predicate": {"items": "#mechanical:knives"}}
w(D / "mechanical/loot_table/blocks/soot.json", {"type": "minecraft:block", "pools": [
    pool({"type": "minecraft:all_of", "terms": [at(1), {"type": "minecraft:random_chance", "chance": 0.5}]}),
    pool(at(2)),
    pool(at(3), 2),
    pool(KNIFE),
], "random_sequence": "mechanical:blocks/soot"})

# ---- recipe, lang ----
w(D / "mechanical/recipe/black_dye_from_soot.json", {"type": "minecraft:crafting_shapeless", "category": "misc", "group": "black_dye",
    "ingredients": ["mechanical:soot"], "result": {"id": "minecraft:black_dye", "count": 1}})
merge(A / "lang/en_us.json", {"block.mechanical.soot": "Soot", "item.mechanical.soot": "Soot"})
print("soot ok")
