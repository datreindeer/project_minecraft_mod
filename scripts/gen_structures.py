"""Data for the small Stone Age worldgen structures (Java features in com.mechanical.worldgen):
features, placed features, biome modifiers and the pot loot tables. Run: python scripts/gen_structures.py"""
import json
from pathlib import Path

D = Path(__file__).resolve().parent.parent / "src/main/resources/data/mechanical"
LANG = Path(__file__).resolve().parent.parent / "src/main/resources/assets/mechanical/lang/en_us.json"


def w(path, obj):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, indent=2) + "\n")


def anyof(*sets):
    return {"type": "neoforge:or", "values": list(sets)}


# name: (biomes, 1-in-N chunks). The features bail out on bad terrain, so the real rate is lower than 1/N.
STRUCTURES = {
    "abandoned_campsite": (anyof("#minecraft:is_forest", ["minecraft:plains", "minecraft:sunflower_plains", "minecraft:meadow"]), 80),
    "hunters_lean_to": (anyof("#minecraft:is_taiga", ["minecraft:forest", "minecraft:birch_forest"]), 120),
    "bear_den": (anyof("#minecraft:is_taiga", ["minecraft:forest", "minecraft:dark_forest", "minecraft:windswept_forest"]), 120),
    "standing_stones": (["minecraft:plains", "minecraft:sunflower_plains", "minecraft:savanna", "minecraft:savanna_plateau", "minecraft:meadow"], 100),
    "burial_mound": (["minecraft:plains", "minecraft:sunflower_plains", "minecraft:meadow", "minecraft:taiga", "minecraft:snowy_taiga"], 180),
    "fishing_camp": (["minecraft:river", "minecraft:frozen_river", "minecraft:beach", "minecraft:snowy_beach"], 24),
    "flint_quarry": (["minecraft:plains", "minecraft:sunflower_plains", "minecraft:river", "minecraft:stony_shore", "minecraft:windswept_hills"], 120),
    "mammoth_bone_hut": (["minecraft:snowy_plains", "minecraft:snowy_taiga", "minecraft:grove"], 220),
    "hermits_hut": (["minecraft:dark_forest", "minecraft:old_growth_pine_taiga", "minecraft:old_growth_spruce_taiga",
                     "minecraft:old_growth_birch_forest", "minecraft:forest"], 250),
}

for name, (biomes, rarity) in STRUCTURES.items():
    w(D / f"worldgen/feature/{name}.json", {"type": f"mechanical:{name}"})
    w(D / f"worldgen/placed_feature/{name}.json", {"feature": f"mechanical:{name}", "placement": [
        {"type": "minecraft:rarity_filter", "chance": rarity}, {"type": "minecraft:in_square"},
        {"type": "minecraft:heightmap", "heightmap": "WORLD_SURFACE_WG"}, {"type": "minecraft:biome"}]})
    w(D / f"neoforge/biome_modifier/add_{name}.json", {"type": "neoforge:add_features", "biomes": biomes,
        "features": f"mechanical:{name}", "step": "surface_structures"})

# Decorated pots hold a single stack, so each pot table rolls once from a weighted list: (item, weight, min, max).
LOOT = {
    "abandoned_campsite": [("minecraft:flint", 10, 2, 4), ("mechanical:cordage", 6, 1, 3), ("mechanical:plant_fibre", 10, 3, 8),
                           ("mechanical:twine", 6, 1, 3), ("mechanical:cooked_game_meat_scraps", 5, 1, 3),
                           ("mechanical:cooked_venison_scraps", 4, 1, 3), ("minecraft:stick", 8, 3, 8), ("mechanical:bark", 6, 2, 5)],
    "hunters_lean_to": [("minecraft:leather", 6, 1, 3), ("mechanical:raw_hide", 8, 1, 2), ("minecraft:arrow", 6, 2, 6),
                        ("mechanical:bone_arrow", 8, 2, 6), ("mechanical:crude_bow", 1, 1, 1), ("mechanical:sinew", 8, 1, 3),
                        ("mechanical:bear_pelt", 1, 1, 1)],
    "bear_den": [("minecraft:bone", 12, 2, 5), ("minecraft:leather", 6, 1, 2), ("mechanical:raw_hide", 5, 1, 1),
                 ("mechanical:bear_claw", 2, 1, 1), ("mechanical:flint_knife", 1, 1, 1), ("minecraft:flint", 6, 1, 3)],
    "standing_stones": [("minecraft:flint", 10, 1, 3), ("minecraft:bone", 8, 1, 3), ("mechanical:antler", 2, 1, 1),
                        ("mechanical:horn", 2, 1, 1)],
    "burial_mound": [("mechanical:bone_needle", 4, 1, 1), ("mechanical:tusk_dagger", 1, 1, 1), ("minecraft:flint", 8, 2, 5),
                     ("mechanical:tusk", 4, 1, 2), ("mechanical:antler", 5, 1, 2), ("mechanical:horn", 4, 1, 2),
                     ("mechanical:bone_arrow", 5, 2, 5), ("minecraft:bone", 8, 2, 6)],
    "fishing_camp": [("mechanical:bone_fish_hook", 8, 1, 2), ("mechanical:crude_fishing_rod", 1, 1, 1),
                     ("mechanical:whole_cod", 6, 1, 3), ("mechanical:whole_salmon", 6, 1, 3), ("mechanical:fish_bones", 8, 2, 5),
                     ("mechanical:cordage", 6, 1, 3)],
    "flint_quarry": [("minecraft:flint", 14, 3, 7), ("mechanical:stone_axe_head", 4, 1, 1), ("mechanical:stone_pickaxe_head", 3, 1, 1),
                     ("mechanical:flint_knife", 1, 1, 1), ("mechanical:stone_chunk", 8, 2, 5)],
    "mammoth_bone_hut": [("mechanical:tusk", 10, 1, 3), ("minecraft:bone", 10, 3, 8), ("mechanical:bear_pelt", 1, 1, 1),
                         ("mechanical:sinew", 6, 1, 3), ("mechanical:lard", 6, 1, 3), ("mechanical:raw_hide", 4, 1, 2),
                         ("mechanical:mammoth_ivory", 3, 1, 1), ("mechanical:mammoth_wool", 6, 1, 3)],  # fauna: giants
    "hermits_hut": [("minecraft:leather", 6, 1, 3), ("mechanical:cordage", 6, 1, 3),
                    ("mechanical:cooked_venison_scraps", 6, 2, 4), ("mechanical:bone_needle", 3, 1, 1),
                    ("mechanical:blueberries", 6, 2, 6), ("mechanical:twine", 5, 1, 3)],
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
print(f"{len(STRUCTURES)} structures ok")
