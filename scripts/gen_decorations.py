"""Data for the small worldgen decorations (boulders, cairns, carvings, statues; Java features in com.mechanical.worldgen):
features, placed features, biome modifiers and the pot loot tables. Run: python scripts/gen_decorations.py"""
import json
from pathlib import Path

D = Path(__file__).resolve().parent.parent / "src/main/resources/data/mechanical"
LANG = Path(__file__).resolve().parent.parent / "src/main/resources/assets/mechanical/lang/en_us.json"


def w(path, obj):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, indent=2) + "\n")


def anyof(*sets):
    return {"type": "neoforge:or", "values": list(sets)}


OPEN = ["minecraft:plains", "minecraft:sunflower_plains", "minecraft:meadow", "minecraft:snowy_plains", "minecraft:savanna",
        "minecraft:savanna_plateau", "minecraft:windswept_hills", "minecraft:windswept_gravelly_hills", "minecraft:stony_shore"]
WOODS = ["minecraft:forest", "minecraft:flower_forest", "minecraft:birch_forest", "minecraft:taiga", "minecraft:snowy_taiga"]

# name: (biomes, 1-in-N chunks). The features bail out on bad terrain, so the real rate is lower than 1/N.
DECORATIONS = {
    "glacial_erratic": (OPEN, 24),
    "mossy_boulder": (anyof("#minecraft:is_forest", "#minecraft:is_taiga"), 24),
    "sandstone_boulder": (anyof("#minecraft:is_badlands", ["minecraft:desert"]), 24),
    "cairn": (["minecraft:windswept_hills", "minecraft:windswept_gravelly_hills", "minecraft:windswept_forest", "minecraft:stony_shore",
               "minecraft:meadow", "minecraft:grove", "minecraft:taiga", "minecraft:snowy_taiga", "minecraft:snowy_plains",
               "minecraft:plains"], 70),
    "balanced_rock": (anyof("#minecraft:is_badlands", ["minecraft:desert", "minecraft:savanna", "minecraft:savanna_plateau",
                                                       "minecraft:windswept_savanna", "minecraft:windswept_hills"]), 70),
    "rune_stone": (["minecraft:plains", "minecraft:sunflower_plains", "minecraft:meadow", "minecraft:snowy_plains"] + WOODS, 140),
    "stone_head": (["minecraft:plains", "minecraft:sunflower_plains", "minecraft:meadow", "minecraft:savanna",
                    "minecraft:windswept_hills", "minecraft:stony_shore", "minecraft:snowy_plains"], 200),
    "animal_carving": (["minecraft:plains", "minecraft:sunflower_plains", "minecraft:meadow", "minecraft:snowy_plains",
                        "minecraft:savanna"] + WOODS, 160),
    "shrine_statue": (["minecraft:plains", "minecraft:sunflower_plains", "minecraft:meadow", "minecraft:savanna"] + WOODS, 200),
}

for name, (biomes, rarity) in DECORATIONS.items():
    w(D / f"worldgen/feature/{name}.json", {"type": f"mechanical:{name}"})
    w(D / f"worldgen/placed_feature/{name}.json", {"feature": f"mechanical:{name}", "placement": [
        {"type": "minecraft:rarity_filter", "chance": rarity}, {"type": "minecraft:in_square"},
        {"type": "minecraft:heightmap", "heightmap": "WORLD_SURFACE_WG"}, {"type": "minecraft:biome"}]})
    w(D / f"neoforge/biome_modifier/add_{name}.json", {"type": "neoforge:add_features", "biomes": biomes,
        "features": f"mechanical:{name}", "step": "surface_structures"})

# Decorated pots hold a single stack, so each pot table rolls once from a weighted list: (item, weight, min, max).
LOOT = {
    "cairn": [("minecraft:bone", 10, 2, 5), ("minecraft:flint", 8, 2, 4), ("mechanical:antler", 4, 1, 1),
              ("mechanical:bone_arrow", 5, 2, 5), ("mechanical:stone_axe_head", 2, 1, 1), ("mechanical:tusk", 2, 1, 1)],
    "shrine_statue": [("mechanical:blueberries", 6, 2, 6), ("mechanical:hazelnuts", 6, 2, 6), ("mechanical:antler", 4, 1, 1),
                      ("mechanical:horn", 4, 1, 1), ("mechanical:tallow", 5, 1, 3), ("mechanical:juniper_berries", 4, 2, 5),
                      ("mechanical:bone_needle", 2, 1, 1), ("mechanical:bear_claw", 2, 1, 1), ("minecraft:flint", 6, 2, 4)],
}

known = {k.split(".", 2)[2] for k in json.loads(LANG.read_text(encoding="utf-8")) if k.startswith(("item.mechanical.", "block.mechanical."))}
for name, entries in LOOT.items():
    pool = []
    for item, weight, lo, hi in entries:
        assert item.startswith("minecraft:") or item.split(":")[1] in known, f"unknown item {item}"
        entry = {"type": "minecraft:item", "name": item, "weight": weight}
        if hi > 1:
            entry["modifier"] = [{"type": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": lo, "max": hi}}]
        pool.append(entry)
    w(D / f"loot_table/chests/{name}.json", {"type": "minecraft:chest", "pools": [{"rolls": 1, "entries": pool}],
        "random_sequence": f"mechanical:chests/{name}"})
print(f"{len(DECORATIONS)} decorations ok")
