"""Willow, ash, hazel, pine and yew (Trees.java): their tree features and where they grow, plus the tree extras:
Beechnuts from beech leaves, Hazelnuts from hazel leaves, Pine Resin (knife on a pine log, Trees.java) and the Resin Torch recipe.
The wood sets themselves (blockstates, models, loot, tags, recipes, stripping) come from gen_data.py's NEW_WOODS.
Every species also gets `mechanical:<tree>_checked` (the feature with only the sapling-survival check, vanilla's
`*_checked` style) for biomes to put in their own tree lists; `mechanical:<tree>` is the tree feature (saplings grow it).
Textures: scripts/gen_wood_textures.py (then gen_firewood.py, gen_rough_log_textures.py, gen_leaf_litter_textures.py).
Generator order: ... gen_soot.py, gen_phantom.py, gen_trees.py, gen_block_items.py, gen_registry.py.
Run: python scripts/gen_trees.py"""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent / "src/main/resources"
A, D = ROOT / "assets/mechanical", ROOT / "data"
ALL_TREES = ["aspen", "alder", "beech", "linden", "willow", "ash", "hazel", "pine", "yew"]


def w(path, obj):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, indent=2) + "\n")


def merge(path, obj):
    old = json.loads(path.read_text(encoding="utf-8")) if path.exists() else {}
    old.update(obj)
    path.write_text(json.dumps(old, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")


def tag_add(path, *values):
    old = json.loads(path.read_text(encoding="utf-8")) if path.exists() else {"values": []}
    old["values"] += [v for v in values if v not in old["values"]]
    w(path, old)


# ---- tree features (same layout as gen_data.py's tree_feature) ----
def tree_feature(x, trunk, foliage, size=None):
    w(D / f"mechanical/worldgen/feature/{x}.json", {"type": "minecraft:tree", "below_trunk_provider": "minecraft:soil_beneath_tree",
        "decorators": [], "foliage_placer": foliage, "ignore_vines": True,
        "foliage_provider": {"id": f"mechanical:{x}_leaves", "properties": {"distance": "7", "persistent": "false", "waterlogged": "false"}},
        "minimum_size": size or {"type": "minecraft:two_layers_feature_size"}, "trunk_placer": trunk,
        "trunk_provider": {"id": f"mechanical:{x}_log", "properties": {"axis": "y"}}})


trunk = lambda kind, base, a, b=0: {"type": kind, "base_height": base, "height_rand_a": a, "height_rand_b": b}
FANCY_SIZE = {"type": "minecraft:two_layers_feature_size", "limit": 0, "min_clipped_height": 5, "upper_size": 0}
# willow: branched trunk, every branch end a flat leaf umbrella shedding curtains up to 3 blocks long (TreeShapes)
tree_feature("willow", trunk("minecraft:fancy_trunk_placer", 7, 3),
             {"type": "mechanical:weeping_foliage_placer", "radius": 3, "offset": 1, "height": 1, "hang_length": 3}, FANCY_SIZE)
# ash: tall straight trunk 10-15, high oval open crown
tree_feature("ash", trunk("minecraft:straight_trunk_placer", 10, 3, 2),
             {"type": "minecraft:fancy_foliage_placer", "radius": 2, "offset": 2, "height": 4})
# hazel: 3-5 thin stems from one stool, 4-6 tall, each with a small bushy crown (TreeShapes)
tree_feature("hazel", trunk("mechanical:multi_stem_trunk_placer", 3, 2, 1),
             {"type": "minecraft:blob_foliage_placer", "radius": 2, "offset": 0, "height": 2})
# pine: tall bare trunk 11-15 that may fork near the top, flat umbrella crowns (Scots pine)
tree_feature("pine", trunk("minecraft:forking_trunk_placer", 10, 3, 2),
             {"type": "minecraft:acacia_foliage_placer", "radius": 2, "offset": 0}, {"type": "minecraft:two_layers_feature_size", "upper_size": 2})
# yew: short thick (2x2) trunk with side limbs, low, broad and dense
tree_feature("yew", trunk("minecraft:dark_oak_trunk_placer", 4, 2, 1),
             {"type": "minecraft:dark_oak_foliage_placer", "radius": 0, "offset": 0},
             {"type": "minecraft:three_layers_feature_size", "upper_size": 2})

# ---- placed features ----
survives = lambda x: {"type": "minecraft:block_predicate_filter", "predicate": {"type": "minecraft:would_survive", "state": f"mechanical:{x}_sapling"}}
for x in ALL_TREES:  # for biomes' own tree lists (random_selector etc.), like vanilla's oak_checked
    w(D / f"mechanical/worldgen/placed_feature/{x}_checked.json", {"feature": f"mechanical:{x}", "placement": [survives(x)]})

weighted = lambda *counts: {"type": "minecraft:count", "count": {"type": "minecraft:weighted_list",
                            "distribution": [{"data": n, "weight": wt} for n, wt in counts]}}
rarity = lambda chance: {"type": "minecraft:rarity_filter", "chance": chance}
# water within 3 blocks of the trunk, one block down (a lake or stream shore)
NEAR_WATER = {"type": "minecraft:block_predicate_filter", "predicate": {"type": "minecraft:any_of", "predicates": [
    {"type": "minecraft:matching_fluids", "fluids": "minecraft:water", "offset": [dx, -1, dz]}
    for dx in range(-3, 4) for dz in range(-3, 4) if 0 < abs(dx) + abs(dz) <= 4]}}
FOREST = ["minecraft:forest", "minecraft:flower_forest"]
TAIGA = ["minecraft:taiga", "minecraft:snowy_taiga", "minecraft:old_growth_pine_taiga"]
# placed feature -> (tree, frequency per chunk, biomes[, extra filters]); placed on the ground, under any canopy (trees grow through leaves)
TREE_PLACEMENTS = {
    "willow_waterside": ("willow", weighted((0, 1), (1, 1), (2, 1)), ["minecraft:river", "minecraft:swamp"], []),
    "willow_lakeside": ("willow", weighted((0, 1), (2, 1)), FOREST + ["minecraft:plains", "minecraft:meadow", "minecraft:birch_forest"], [NEAR_WATER]),
    "ash_forest": ("ash", weighted((0, 2), (1, 1)), FOREST),
    "ash_plains": ("ash", rarity(14), ["minecraft:plains", "minecraft:meadow"]),
    "hazel_forest": ("hazel", weighted((0, 1), (1, 1)), FOREST + ["minecraft:birch_forest"]),
    "hazel_plains": ("hazel", rarity(10), ["minecraft:plains", "minecraft:sunflower_plains"]),
    "pine_taiga": ("pine", weighted((0, 1), (1, 1)), TAIGA),
    "pine_windswept": ("pine", weighted((0, 1), (1, 1)), ["minecraft:windswept_forest", "minecraft:windswept_hills"]),
    "yew_old_growth": ("yew", rarity(4), ["minecraft:old_growth_spruce_taiga", "minecraft:old_growth_pine_taiga", "minecraft:windswept_forest"]),
    "yew_forest": ("yew", rarity(16), ["minecraft:taiga", "minecraft:dark_forest"]),
}
for name, (x, freq, biomes, *extra) in TREE_PLACEMENTS.items():
    extra = extra[0] if extra else []
    w(D / f"mechanical/worldgen/placed_feature/{name}.json", {"feature": f"mechanical:{x}", "placement": [
        freq, {"type": "minecraft:in_square"}, {"type": "minecraft:surface_water_depth_filter", "max_water_depth": 0},
        {"type": "minecraft:heightmap", "heightmap": "MOTION_BLOCKING_NO_LEAVES"}, {"type": "minecraft:biome"}, *extra, survives(x)]})
    w(D / f"mechanical/neoforge/biome_modifier/add_{name}.json", {"type": "neoforge:add_features", "biomes": biomes,
        "features": f"mechanical:{name}", "step": "vegetal_decoration"})

# ---- nuts from leaves: a pool on top of the leaves' sapling/stick loot (not when sheared or silk-touched) ----
NOT_SHEARED = {"type": "minecraft:inverted", "term": {"type": "minecraft:any_of", "terms": ["minecraft:tool/can_shear", "minecraft:tool/can_silk_touch"]}}
for tree, nut, chance in (("beech", "mechanical:beechnuts", 0.06), ("hazel", "mechanical:hazelnuts", 0.06)):
    path = D / f"mechanical/loot_table/blocks/{tree}_leaves.json"
    loot = json.loads(path.read_text(encoding="utf-8"))
    loot["pools"] = [p for p in loot["pools"] if p.get("entries", [{}])[0].get("name") != nut]  # rerunnable
    loot["pools"].append({"rolls": 1, "condition": {"type": "minecraft:all_of", "terms": [NOT_SHEARED, {"type": "minecraft:survives_explosion"},
        {"type": "minecraft:random_chance", "chance": chance}]}, "entries": [{"type": "minecraft:item", "name": nut}]})
    w(path, loot)

# ---- items ----
ITEMS = {"beechnuts": "Beechnuts", "roasted_beechnuts": "Roasted Beechnuts", "pine_resin": "Pine Resin"}
for item in ITEMS:
    w(A / f"models/item/{item}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"mechanical:item/{item}"}})
    w(A / f"items/{item}.json", {"model": {"type": "minecraft:model", "model": f"mechanical:item/{item}"}})
merge(A / "lang/en_us.json", {f"item.mechanical.{k}": v for k, v in ITEMS.items()})
w(D / "mechanical/recipe/roasted_beechnuts.json", {"type": "minecraft:campfire_cooking", "category": "food", "cookingtime": 300,
    "experience": 0.1, "ingredient": "mechanical:beechnuts", "result": {"id": "mechanical:roasted_beechnuts"}})
# resin torch: a resin-soaked stick burns like a coal torch, before there is charcoal
w(D / "mechanical/recipe/torch_from_pine_resin.json", {"type": "minecraft:crafting_shaped", "category": "misc",
    "key": {"R": "mechanical:pine_resin", "#": "minecraft:stick"}, "pattern": ["R", "#"], "result": {"id": "minecraft:torch", "count": 2}})
w(D / "mechanical/advancement/recipes/pine_resin.json", {"parent": "minecraft:recipes/root",
    "criteria": {"has_item": {"trigger": "minecraft:inventory_changed", "conditions": {"items": [{"items": "mechanical:pine_resin"}]}}},
    "requirements": [["has_item"]], "rewards": {"recipes": ["mechanical:torch_from_pine_resin"]}})
tag_add(D / "mechanical/tags/item/basket_storable.json", "mechanical:beechnuts", "mechanical:roasted_beechnuts", "mechanical:pine_resin")
print("trees ok")
