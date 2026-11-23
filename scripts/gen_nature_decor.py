"""Placement data for the small things in forests and by the water (docs/terrain-plan.md phase 3; Java features
ForestDecor and WaterDecor in com.mechanical.worldgen): features, placed features and biome modifiers. Each placed
feature is (feature, biomes, placement); the features bail out on unsuitable spots (no tree nearby, no creek, no slope),
so the real rate is well below the tries. Old forests get their own, denser placed features.
Generator order: anywhere after gen_world_biomes.py (it only writes its own files). Run: python scripts/gen_nature_decor.py"""
import json
from pathlib import Path

D = Path(__file__).resolve().parent.parent / "src/main/resources/data/mechanical"


def w(path, obj):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, indent=2) + "\n")


def mc(*names):
    return [n if ":" in n else "minecraft:" + n for n in names]


def mod(*names):
    return ["mechanical:" + n for n in names]


FORESTS = mc("forest", "flower_forest", "birch_forest", "taiga", "snowy_taiga", "windswept_forest", "grove", "jungle", "sparse_jungle",
             "cherry_grove", "swamp") + mod("willow_fen", "pine_heath", "soapstone_valley", "peat_bog", "glacial_moraine", "hazel_meadow")
OLD = mc("old_growth_birch_forest", "old_growth_pine_taiga", "old_growth_spruce_taiga", "dark_forest", "pale_garden") + mod("deciduous_forest")
WOODED = FORESTS + OLD
HEATH = mc("meadow", "plains", "windswept_hills") + mod("pine_heath", "chalk_downs", "slate_highlands", "hazel_meadow")
RIVERS = mc("river")
SHORES = mc("beach", "snowy_beach", "stony_shore", "river") + mod("shingle_beach", "basalt_coast", "reed_delta")
# still water without reeds of its own (gen_earth.py puts reed in rivers, swamps and mangroves)
REEDLESS = [b for b in WOODED if b != "minecraft:swamp"] + mc("plains", "sunflower_plains", "meadow")
HILLS = mc("forest", "birch_forest", "taiga", "meadow", "windswept_hills", "windswept_forest", "grove") + mod(
    "deciduous_forest", "hazel_meadow", "pine_heath", "chalk_downs", "slate_highlands", "soapstone_valley", "limestone_karst")


def rarity(n):
    return [{"type": "minecraft:rarity_filter", "chance": n}]


def count(n):
    return [{"type": "minecraft:count", "count": n}]


# placed feature: (feature type, biomes, tries per chunk)
PLACED = {
    "fallen_log": ("fallen_log", FORESTS, count(1)),
    "fallen_log_old": ("fallen_log", OLD, count(2)),
    # snag switched off 2026-11-23 (user: barkless trunk read as planks with a bark block in the middle): redo before re-enabling
    "root_plate": ("root_plate", FORESTS, rarity(10)),
    "root_plate_old": ("root_plate", OLD, rarity(4)),
    "mossy_stones": ("mossy_stones", WOODED + RIVERS, rarity(4)),
    "mossy_stones_heath": ("mossy_stones", HEATH, rarity(8)),
    "log_bridge": ("log_bridge", WOODED + RIVERS, count(2)),
    "beaver_dam": ("beaver_dam", WOODED + RIVERS, rarity(2)),
    "driftwood": ("driftwood", SHORES, count(1)),
    "gravel_bar": ("gravel_bar", RIVERS, count(1)),
    "reed_belt": ("reed_belt", REEDLESS, count(1)),
    "hillside_spring": ("hillside_spring", HILLS, rarity(2)),
}

for name, (feature, biomes, tries) in PLACED.items():
    w(D / f"worldgen/feature/{feature}.json", {"type": f"mechanical:{feature}"})
    w(D / f"worldgen/placed_feature/{name}.json", {"feature": f"mechanical:{feature}", "placement": tries + [
        {"type": "minecraft:in_square"}, {"type": "minecraft:heightmap", "heightmap": "WORLD_SURFACE_WG"}, {"type": "minecraft:biome"}]})
    w(D / f"neoforge/biome_modifier/add_{name}.json", {"type": "neoforge:add_features", "biomes": sorted(set(biomes)),
        "features": f"mechanical:{name}", "step": "vegetal_decoration"})
print(f"{len(PLACED)} nature decorations ok")
