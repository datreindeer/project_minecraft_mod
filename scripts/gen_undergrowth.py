"""Forest undergrowth data: hazel shrub, juniper, bramble, undergrowth, sapling thicket (blockstates, models, loot,
worldgen patches) and hazelnuts / roasted hazelnuts / juniper berries. Java: ForestUndergrowth.java.
Textures: gen_undergrowth_textures.py. usage: python scripts/gen_undergrowth.py"""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent / "src/main/resources"
A = ROOT / "assets/mechanical"
D = ROOT / "data/mechanical"


def w(path, obj):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, indent=2) + "\n", encoding="utf-8")


def title(s):
    return " ".join(p.capitalize() for p in s.split("_"))


def count(lo, hi):
    return [{"type": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": lo, "max": hi}}]


def item(name, lo, hi, chance=None):
    e = {"type": "minecraft:item", "name": name, "modifier": count(lo, hi)}
    if chance:
        e["condition"] = {"type": "minecraft:random_chance", "chance": chance}
    return e


def loot(block, pools, tall=False):
    """tall: drop only once per plant, from whichever half is broken while its partner still stands (like vanilla tall grass)."""
    if tall:
        both = {"type": "minecraft:any_of", "terms": [
            {"type": "minecraft:all_of", "terms": [
                {"type": "minecraft:match_block", "blocks": f"mechanical:{block}", "state": {"half": h}},
                {"type": "minecraft:location_check", "offsetY": dy, "predicate": {"block": {"blocks": f"mechanical:{block}", "state": {"half": o}}}}]}
            for h, o, dy in (("lower", "upper", 1), ("upper", "lower", -1))]}
        for p in pools:
            p["condition"] = both
    w(D / f"loot_table/blocks/{block}.json", {"type": "minecraft:block", "pools": pools, "random_sequence": f"mechanical:blocks/{block}"})


def pool(*entries):
    return {"rolls": 1, "entries": list(entries)}


lang_path = A / "lang/en_us.json"
lang = json.loads(lang_path.read_text(encoding="utf-8"))

# ---- models: a denser "shrub" model (cross + four upright planes), so bushes read as a leafy clump, not grass ----
def plane(frm, to, faces, rot=False):
    e = {"from": frm, "to": to, "shade_direction_override": "up",
         "faces": {f: {"uv": [16, 0, 0, 16] if flip else [0, 0, 16, 16], "texture": "#cross"} for f, flip in faces}}
    if rot:
        e["rotation"] = {"origin": [8, 8, 8], "axis": "y", "angle": 45, "rescale": True}
    return e


w(A / "models/block/shrub.json", {"ambientocclusion": False, "textures": {"particle": "#cross"}, "elements": [
    plane([0.8, 0, 8], [15.2, 16, 8], [("north", False), ("south", False)], True),
    plane([8, 0, 0.8], [8, 16, 15.2], [("west", False), ("east", False)], True),
    plane([3.5, 0, 0], [3.5, 16, 16], [("west", False), ("east", True)]),
    plane([12.5, 0, 0], [12.5, 16, 16], [("west", True), ("east", False)]),
    plane([0, 0, 3.5], [16, 16, 3.5], [("north", True), ("south", False)]),
    plane([0, 0, 12.5], [16, 16, 12.5], [("north", False), ("south", True)])]})


def model(name, tex, parent="mechanical:block/shrub"):
    w(A / f"models/block/{name}.json", {"parent": parent, "textures": {"cross": f"mechanical:block/{tex}"}})


# hazel shrub: two halves, nuts show on both when ripe
for half, part in (("lower", "bottom"), ("upper", "top")):
    model(f"hazel_shrub_{part}", f"hazel_shrub_{part}")
    model(f"hazel_shrub_{part}_nuts", f"hazel_shrub_{part}_nuts")
w(A / "blockstates/hazel_shrub.json", {"variants": {
    f"half={h},nuts={n}": {"model": f"mechanical:block/hazel_shrub_{p}{'_nuts' if n == 'true' else ''}"}
    for h, p in (("lower", "bottom"), ("upper", "top")) for n in ("false", "true")}})
# juniper
model("juniper", "juniper")
model("juniper_berries", "juniper_berries")
w(A / "blockstates/juniper.json", {"variants": {"berries=false": {"model": "mechanical:block/juniper"},
                                                "berries=true": {"model": "mechanical:block/juniper_berries"}}})
# bramble, undergrowth (fern-like, plain cross), sapling thicket
model("bramble", "bramble")
w(A / "blockstates/bramble.json", {"variants": {"": {"model": "mechanical:block/bramble"}}})
model("undergrowth", "undergrowth", "minecraft:block/cross")
w(A / "blockstates/undergrowth.json", {"variants": {"": {"model": "mechanical:block/undergrowth"}}})
for part in ("bottom", "top"):
    model(f"sapling_thicket_{part}", f"sapling_thicket_{part}")
w(A / "blockstates/sapling_thicket.json", {"variants": {"half=lower": {"model": "mechanical:block/sapling_thicket_bottom"},
                                                        "half=upper": {"model": "mechanical:block/sapling_thicket_top"}}})

# ---- loot ----
loot("hazel_shrub", [pool(item("minecraft:stick", 1, 3)), pool(item("mechanical:hazelnuts", 1, 1, 0.15))], tall=True)
loot("juniper", [pool(item("minecraft:stick", 1, 2)), {"rolls": 1, "entries": [item("mechanical:juniper_berries", 1, 2)],
     "condition": {"type": "minecraft:match_block", "blocks": "mechanical:juniper", "state": {"berries": "true"}}}])
loot("bramble", [pool(item("mechanical:plant_fibre", 1, 2)), pool(item("minecraft:stick", 1, 1, 0.5))])
loot("undergrowth", [pool(item("mechanical:plant_fibre", 1, 1, 0.5))])
loot("sapling_thicket", [pool(item("minecraft:stick", 2, 3))], tall=True)

for b in ("hazel_shrub", "juniper", "bramble", "undergrowth", "sapling_thicket"):
    lang[f"block.mechanical.{b}"] = title(b)

# ---- items ----
for it in ("hazelnuts", "roasted_hazelnuts", "juniper_berries"):
    w(A / f"models/item/{it}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"mechanical:item/{it}"}})
    w(A / f"items/{it}.json", {"model": {"type": "minecraft:model", "model": f"mechanical:item/{it}"}})
    lang[f"item.mechanical.{it}"] = title(it)
w(D / "recipe/roasted_hazelnuts.json", {"type": "minecraft:campfire_cooking", "category": "food", "cookingtime": 300,
    "experience": 0.1, "ingredient": "mechanical:hazelnuts", "result": {"id": "mechanical:roasted_hazelnuts"}})

# ---- worldgen: patches, same placement as gen_data.py's forage_feature (MOTION_BLOCKING_NO_LEAVES + replaceable + dirt below) ----
tri = lambda m: {"type": "minecraft:trapezoid", "min": -m, "max": m, "plateau": 0}


def patch(name, state, biomes, rarity, n, spread):
    feature = name.split("_in_")[0]
    w(D / f"worldgen/feature/{feature}.json", {"type": "minecraft:simple_block", "to_place": state})
    w(D / f"worldgen/placed_feature/{name}.json", {"feature": f"mechanical:{feature}", "placement": [
        {"type": "minecraft:rarity_filter", "chance": rarity}, {"type": "minecraft:in_square"},
        {"type": "minecraft:heightmap", "heightmap": "MOTION_BLOCKING_NO_LEAVES"}, {"type": "minecraft:biome"},
        {"type": "minecraft:count", "count": n},
        {"type": "minecraft:offset", "x": tri(spread), "y": tri(2), "z": tri(spread)},
        {"type": "minecraft:block_predicate_filter", "predicate": {"type": "minecraft:all_of", "predicates": [
            {"type": "minecraft:replaceable"},
            {"type": "minecraft:matching_block_tag", "tag": "minecraft:dirt", "offset": [0, -1, 0]}]}}]})
    w(D / f"neoforge/biome_modifier/add_{name}.json", {"type": "neoforge:add_features", "biomes": biomes,
        "features": f"mechanical:{name}", "step": "vegetal_decoration"})


M = lambda *b: [f"minecraft:{x}" for x in b]
FORESTS = M("forest", "flower_forest", "birch_forest", "old_growth_birch_forest", "dappled_forest", "dark_forest")
TAIGA = M("taiga", "snowy_taiga", "old_growth_pine_taiga", "old_growth_spruce_taiga")
# name, state, biomes, 1-in-N chunks, attempts, spread
patch("undergrowth", {"id": "mechanical:undergrowth"}, FORESTS, 1, 24, 7)
patch("hazel_shrub", {"id": "mechanical:hazel_shrub", "properties": {"half": "lower", "nuts": "true"}},
      M("forest", "birch_forest", "flower_forest", "old_growth_birch_forest", "dappled_forest"), 5, 8, 4)
patch("juniper", {"id": "mechanical:juniper", "properties": {"berries": "true"}}, TAIGA, 4, 10, 5)
patch("juniper_in_hills", {"id": "mechanical:juniper", "properties": {"berries": "true"}},
      M("windswept_hills", "windswept_forest", "windswept_gravelly_hills"), 6, 8, 4)
patch("bramble", {"id": "mechanical:bramble"}, M("forest", "dark_forest", "flower_forest", "dappled_forest"), 6, 12, 3)
patch("bramble_in_plains", {"id": "mechanical:bramble"}, M("plains", "sunflower_plains"), 24, 10, 3)
patch("sapling_thicket", {"id": "mechanical:sapling_thicket", "properties": {"half": "lower"}},
      M("forest", "birch_forest", "old_growth_birch_forest"), 5, 6, 3)

w(lang_path, lang)
print("undergrowth ok")
