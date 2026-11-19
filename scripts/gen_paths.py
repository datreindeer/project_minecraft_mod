"""Data for trampled paths (TrampledPaths.java, TrampledPathBlock.java): Worn Grass and Trodden Path.

- blockstates: one model per connection mask (north 1, east 2, south 4, west 8), each offered in 4 random rotations
  like vanilla grass: rotation r shows the model drawn for the mask turned back r quarter turns, so the earth still runs
  toward the connected sides but every tile shape has 4 looks. RECENT / PERSISTENT don't change the look. Snowy: the
  vanilla snowy grass block model.
- models: vanilla grass_block (tinted grass_block_top, grass sides with the tinted overlay) plus the bare earth layer
  (textures from gen_paths_textures.py, untinted, cutout from its transparency) over the top face
- items with the grass tint, loot (silk touch: itself, else Dirt), lang
- tags: both blocks in #minecraft:dirt (so plants, saplings and crops' substrate tags accept them), mineable/shovel,
  turns_into_farmland (hoe), turns_into_dirt_path (shovel), animals_spawnable_on; mechanical:path_connects (the ground
  a path's earth runs out toward)
Generator order: gen_data.py src/main/resources, gen_tribe_village.py, gen_trails.py, gen_paths.py, gen_bronze_ores.py,
... gen_block_items.py, gen_unlocks.py, gen_advancements.py, gen_registry.py.
Run: python scripts/gen_paths.py
"""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent / "src/main/resources"
A, D = ROOT / "assets/mechanical", ROOT / "data"
BLOCKS = {"worn_grass": "Worn Grass", "trodden_path": "Trodden Path"}
ITEM_MASK = 5  # north + south: a straight piece of path
GRASS_TINT = {"type": "minecraft:grass", "temperature": 0.5, "downfall": 1.0}


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


def unrot(m):
    """The mask turned back a quarter turn (counter-clockwise seen from above): east -> north."""
    return ((m >> 1) | (m << 3)) & 15


def faces(texture, tint, sides=True, extra=None):
    out = {}
    for f in ("north", "south", "west", "east"):
        if sides:
            out[f] = {"uv": [0, 0, 16, 16], "texture": texture, "cullface": f, **({"tintindex": 0} if tint else {})}
    return {**out, **(extra or {})}


def model(block, m):
    full = [0, 0, 0], [16, 16, 16]
    return {"parent": "minecraft:block/block",
            "textures": {"particle": "minecraft:block/dirt", "bottom": "minecraft:block/dirt",
                         "top": "minecraft:block/grass_block_top", "side": "minecraft:block/grass_block_side",
                         "overlay": "minecraft:block/grass_block_side_overlay", "earth": f"mechanical:block/{block}_top_{m}"},
            "elements": [
                {"from": full[0], "to": full[1], "faces": faces("#side", False, extra={
                    "down": {"uv": [0, 0, 16, 16], "texture": "#bottom", "cullface": "down"},
                    "up": {"uv": [0, 0, 16, 16], "texture": "#top", "cullface": "up", "tintindex": 0}})},
                {"from": full[0], "to": full[1], "faces": faces("#overlay", True, extra={
                    "up": {"uv": [0, 0, 16, 16], "texture": "#earth", "cullface": "up"}})},
            ]}


def blockstate(block):
    variants = {}
    for m in range(16):
        key = f"east={str(bool(m & 2)).lower()},north={str(bool(m & 1)).lower()},south={str(bool(m & 4)).lower()},west={str(bool(m & 8)).lower()}"
        options, base = [], m
        for r in range(4):
            options.append({"model": f"mechanical:block/{block}_{base}", **({"y": 90 * r} if r else {})})
            base = unrot(base)
        variants[key + ",snowy=false"] = options
        variants[key + ",snowy=true"] = {"model": "minecraft:block/grass_block_snow"}
    return {"variants": variants}


lang = {}
for block, name in BLOCKS.items():
    for m in range(16):
        w(A / f"models/block/{block}_{m}.json", model(block, m))
    w(A / f"blockstates/{block}.json", blockstate(block))
    w(A / f"items/{block}.json", {"model": {"type": "minecraft:model", "model": f"mechanical:block/{block}_{ITEM_MASK}", "tints": [GRASS_TINT]}})
    w(D / f"mechanical/loot_table/blocks/{block}.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [
        {"type": "minecraft:alternatives", "children": [
            {"type": "minecraft:item", "condition": "minecraft:tool/can_silk_touch", "name": f"mechanical:{block}"},
            {"type": "minecraft:item", "condition": {"type": "minecraft:survives_explosion"}, "name": "minecraft:dirt"}]}]}],
        "random_sequence": f"mechanical:blocks/{block}"})
    lang[f"block.mechanical.{block}"] = name

T = D / "minecraft/tags/block"
ids = [f"mechanical:{b}" for b in BLOCKS]
for tag in ("dirt", "mineable/shovel", "turns_into_farmland", "turns_into_dirt_path", "animals_spawnable_on"):
    tag_add(T / f"{tag}.json", *ids)
w(D / "mechanical/tags/block/path_connects.json", {"values": [
    "#minecraft:dirt", "minecraft:dirt_path", "minecraft:farmland", "minecraft:gravel", "minecraft:packed_mud", "minecraft:mud"]})
merge(A / "lang/en_us.json", lang)
print("paths ok")
