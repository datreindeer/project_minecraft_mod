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
ALL_TREES = ["aspen", "alder", "beech", "linden", "willow", "ash", "hazel", "pine", "yew", "rowan", "bird_cherry", "wild_apple", "plum", "olive",
             "date_palm"]


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

# ---- fruit trees (Trees.FRUIT_KINDS, leaves FruitLeavesBlock; docs/flora-and-food-plan.md section 4) ----
uniform = lambda lo, hi: {"type": "minecraft:uniform", "min_inclusive": lo, "max_inclusive": hi}
wl = lambda *vals: {"type": "minecraft:weighted_list", "distribution": [{"data": v, "weight": 1} for v in vals]}


def cherry_trunk(base, rand, branches, length, start, end=(-1, 0)):
    """vanilla's cherry trunk: a short bole forking into crooked side limbs that each carry a crown"""
    return {"type": "minecraft:cherry_trunk_placer", "base_height": base, "height_rand_a": rand, "height_rand_b": 0,
            "branch_count": wl(*branches), "branch_horizontal_length": uniform(*length),
            "branch_start_offset_from_top": {"min_inclusive": start[0], "max_inclusive": start[1]},
            "branch_end_offset_from_top": uniform(*end)}


def cherry_crown(radius, height, holes, hanging=0.1):
    """ragged crowns: corners and the wide bottom layer broken up, a few hanging leaves"""
    return {"type": "minecraft:cherry_foliage_placer", "radius": radius, "offset": 0, "height": height, "corner_hole_chance": holes,
            "wide_bottom_layer_hole_chance": holes, "hanging_leaves_chance": hanging, "hanging_leaves_extension_chance": 0.3}


# rowan: slim trunk with a few short limbs (fancy oak's branching), an upright airy oval crown
tree_feature("rowan", trunk("minecraft:fancy_trunk_placer", 6, 4), {"type": "minecraft:fancy_foliage_placer", "radius": 2, "offset": 3, "height": 4},
             {"type": "minecraft:two_layers_feature_size", "limit": 0, "min_clipped_height": 5, "upper_size": 0})
# bird cherry: a few leaning stems from one stool, each a loose umbrella shedding short drooping curtains (the willow's placer)
tree_feature("bird_cherry", trunk("mechanical:multi_stem_trunk_placer", 4, 2, 1),
             {"type": "mechanical:weeping_foliage_placer", "radius": 2, "offset": 1, "height": 1, "hang_length": 2})
# wild apple: a low crooked bole forking into 2-3 wide limbs, a low broad ragged crown
tree_feature("wild_apple", cherry_trunk(3, 1, (2, 3), (2, 3), (-2, -1)), cherry_crown(3, 4, 0.45, 0.12),
             {"type": "minecraft:two_layers_feature_size", "upper_size": 2})
# plum: small tree, short limbs, small rounded crown
tree_feature("plum", trunk("minecraft:fancy_trunk_placer", 4, 2), {"type": "minecraft:fancy_foliage_placer", "radius": 2, "offset": 3, "height": 3},
             {"type": "minecraft:two_layers_feature_size", "limit": 0, "min_clipped_height": 3, "upper_size": 0})
# olive: a short gnarled bole splitting into leaning stems, each with a small open crown full of holes
tree_feature("olive", trunk("mechanical:multi_stem_trunk_placer", 3, 1, 1),
             {"type": "minecraft:random_spread_foliage_placer", "radius": 2, "offset": 0, "foliage_height": 2, "leaf_placement_attempts": 22})
# date palm: a tall, slightly curving trunk (TreeShapes.CurvedTrunkPlacer) and a star of long drooping fronds (PalmFoliagePlacer)
tree_feature("date_palm", trunk("mechanical:curved_trunk_placer", 7, 3, 1), {"type": "mechanical:palm_foliage_placer", "radius": 4, "offset": 0})

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
    # fruit trees: scattered, never forests of them
    "rowan_forest": ("rowan", rarity(3), ["mechanical:deciduous_forest", "minecraft:birch_forest", "minecraft:old_growth_birch_forest", "minecraft:forest"]),
    "rowan_upland": ("rowan", rarity(5), ["mechanical:slate_highlands", "minecraft:taiga", "minecraft:windswept_forest", "minecraft:meadow"]),
    "bird_cherry_waterside": ("bird_cherry", weighted((0, 2), (1, 1)), ["mechanical:willow_fen", "mechanical:reed_delta", "minecraft:river"]),
    "bird_cherry_lakeside": ("bird_cherry", weighted((0, 3), (1, 1)), ["mechanical:deciduous_forest", "mechanical:hazel_meadow", "minecraft:forest",
                                                                     "minecraft:meadow", "minecraft:plains"], [NEAR_WATER]),
    "wild_apple_meadow": ("wild_apple", rarity(4), ["mechanical:hazel_meadow"]),
    "wild_apple_forest": ("wild_apple", rarity(12), ["mechanical:deciduous_forest", "minecraft:forest", "minecraft:flower_forest"]),
    "plum_meadow": ("plum", rarity(4), ["mechanical:hazel_meadow", "mechanical:chalk_downs"]),
    "plum_plains": ("plum", rarity(14), ["minecraft:plains", "mechanical:limestone_karst"]),
    "olive_gorge": ("olive", weighted((0, 1), (1, 1)), ["mechanical:marble_gorge"]),
    "olive_plateau": ("olive", rarity(8), ["minecraft:savanna_plateau"]),
    "date_palm_oasis": ("date_palm", weighted((0, 1), (1, 1)), ["minecraft:desert", "mechanical:gypsum_flats"], [NEAR_WATER]),
    "date_palm_desert": ("date_palm", rarity(40), ["minecraft:desert", "mechanical:gypsum_flats"]),
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
for tree, nut, chance in (("beech", "mechanical:beechnuts", 0.06), ("hazel", "mechanical:hazelnuts", 0.06), ("yew", "mechanical:yew_arils", 0.08)):
    path = D / f"mechanical/loot_table/blocks/{tree}_leaves.json"
    loot = json.loads(path.read_text(encoding="utf-8"))
    loot["pools"] = [p for p in loot["pools"] if p.get("entries", [{}])[0].get("name") != nut]  # rerunnable
    loot["pools"].append({"rolls": 1, "condition": {"type": "minecraft:all_of", "terms": [NOT_SHEARED, {"type": "minecraft:survives_explosion"},
        {"type": "minecraft:random_chance", "chance": chance}]}, "entries": [{"type": "minecraft:item", "name": nut}]})
    w(path, loot)

# ---- fruit leaves: a model per stage (leaves, blossom, unripe, ripe), fruit when broken ripe ----
FRUIT = {"rowan": "rowan_berries", "bird_cherry": "bird_cherries", "wild_apple": "crab_apple", "plum": "wild_plums", "olive": "olives",
         "date_palm": "dates"}  # Trees.FRUIT_KINDS
LOOKS = ["", "_blossom", "_unripe", "_ripe"]
TEX = ROOT / "assets/mechanical/textures/block"


def frond(name, tex):
    """A date palm frond block: the frond seen from above (#top, a flat plane high in the block) and its leaflet tips,
    flower strands or date bunches hanging under it (#hang, two crossed planes), so a crown of these reads as thin
    drooping fronds rather than leaf cubes."""
    cross = [{"from": [0.8, 0, 8], "to": [15.2, 11, 8], "shade": False, "rotation": {"origin": [8, 8, 8], "axis": "y", "angle": ang, "rescale": True},
              "faces": {d: {"uv": [0, 5, 16, 16], "texture": "#hang"} for d in ("north", "south")}} for ang in (45, -45)]
    top = [{"from": [-2, 11, -2], "to": [18, 11, 18], "shade": False, "faces": {d: {"uv": [0, 0, 16, 16], "texture": "#top"} for d in ("up", "down")}}]
    w(A / f"models/block/{name}.json", {"ambientocclusion": False, "textures": {"particle": "mechanical:block/date_palm_leaves_top",
        "top": "mechanical:block/date_palm_leaves_top", "hang": f"mechanical:block/{tex}"}, "elements": top + cross})


for tree, fruit in FRUIT.items():
    variants = {}
    for i, look in enumerate(LOOKS):
        # 2-3 painted variants of each stage (where they exist), each turned four ways, so a crown shows no repeating pattern
        texs = [f"{tree}_leaves{look}"] + [f"{tree}_leaves{look}_{k}" for k in (2, 3) if (TEX / f"{tree}_leaves{look}_{k}.png").exists()]
        models = []
        for t in texs:
            if tree == "date_palm":
                frond(t, t if look else "date_palm_leaves_hang")
            elif t != f"{tree}_leaves":
                w(A / f"models/block/{t}.json", {"parent": "minecraft:block/leaves", "textures": {"all": f"mechanical:block/{t}"}})
            models += [{"model": f"mechanical:block/{t}", **({"y": r} if r else {})} for r in (0, 90, 180, 270)]
        if tree == "date_palm" and look:  # only every fourth frond carries a flower strand or date bunch; the rest plain leaflets
            models = [dict(m, weight=1) for m in models] + [{"model": "mechanical:block/date_palm_leaves", **({"y": r} if r else {}), "weight": 3}
                                                            for r in (0, 90, 180, 270)]
        variants[f"age={i}"] = models
    w(A / f"blockstates/{tree}_leaves.json", {"variants": variants})
    path = D / f"mechanical/loot_table/blocks/{tree}_leaves.json"
    loot = json.loads(path.read_text(encoding="utf-8"))
    loot["pools"] = [p for p in loot["pools"] if p.get("entries", [{}])[0].get("name") != f"mechanical:{fruit}"]  # rerunnable
    loot["pools"].append({"rolls": 1, "condition": {"type": "minecraft:all_of", "terms": [NOT_SHEARED, {"type": "minecraft:survives_explosion"},
        {"type": "minecraft:match_block", "blocks": f"mechanical:{tree}_leaves", "state": {"age": "3"}}]},
        "entries": [{"type": "minecraft:item", "name": f"mechanical:{fruit}",
                     "modifier": [{"type": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": 1, "max": 2}}]}]})
    w(path, loot)

# ---- items ----
ITEMS = {"beechnuts": "Beechnuts", "roasted_beechnuts": "Roasted Beechnuts", "pine_resin": "Pine Resin", "yew_arils": "Yew Arils",
         "rowan_berries": "Rowan Berries", "bird_cherries": "Bird Cherries", "crab_apple": "Crab Apple", "wild_plums": "Wild Plums",
         "olives": "Olives", "dates": "Dates"}
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
tag_add(D / "mechanical/tags/item/basket_storable.json", "mechanical:beechnuts", "mechanical:roasted_beechnuts", "mechanical:pine_resin",
        *[f"mechanical:{f}" for f in FRUIT.values()], "mechanical:yew_arils")
tag_add(D / "mechanical/tags/item/perishes_2_days.json", "mechanical:bird_cherries", "mechanical:wild_plums", "mechanical:yew_arils")
tag_add(D / "mechanical/tags/item/perishes_3_days.json", "mechanical:rowan_berries", "mechanical:crab_apple", "mechanical:olives")
tag_add(D / "mechanical/tags/item/dryable_berries.json", "mechanical:rowan_berries", "mechanical:bird_cherries", "mechanical:wild_plums", "mechanical:dates")
print("trees ok")
