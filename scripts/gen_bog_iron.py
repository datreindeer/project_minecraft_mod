"""Iron Age, Bog Iron (Ironworking.java; docs/ages/03-iron-age.md "Bog Iron").

Writes the Bog Iron Ore block (model, blockstate, loot: 1 Raw Bog Iron with Fortune, itself with Silk Touch, shovel-
mineable), the Raw Bog Iron item and the worldgen: small disks of ore in the floors of pools (water right above) in
Willow Fen and swamps, and more rarely in river beds. The Bloomery recipe (6 Raw Bog Iron -> Hot Iron Bloom) is in Java.
Textures: gen_bog_iron_textures.py.
Generator order: ... gen_bloomery.py, gen_bog_iron.py, gen_gemstones.py, ... Run: python scripts/gen_bog_iron.py
"""
import json
from pathlib import Path

R = Path(__file__).resolve().parent.parent / "src/main/resources"
A, D = R / "assets/mechanical", R / "data"
M = "minecraft:"


def w(path, obj):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, indent=2) + "\n", encoding="utf-8")


def tag_add(path, *values):
    tag = json.loads(path.read_text(encoding="utf-8")) if path.exists() else {"values": []}
    tag["values"] += [v for v in values if v not in tag["values"]]
    w(path, tag)


# ---- block and item ----
w(A / "models/block/bog_iron_ore.json", {"parent": "minecraft:block/cube_all", "textures": {"all": "mechanical:block/bog_iron_ore"}})
w(A / "blockstates/bog_iron_ore.json", {"variants": {"": [{"model": "mechanical:block/bog_iron_ore", "y": y} for y in (0, 90, 180, 270)]}})
w(A / "models/item/raw_bog_iron.json", {"parent": "minecraft:item/generated", "textures": {"layer0": "mechanical:item/raw_bog_iron"}})
w(A / "items/raw_bog_iron.json", {"model": {"type": "minecraft:model", "model": "mechanical:item/raw_bog_iron"}})

w(D / "mechanical/loot_table/blocks/bog_iron_ore.json", {
    "type": "minecraft:block",
    "pools": [{"rolls": 1, "entries": [{"type": "minecraft:alternatives", "children": [
        {"type": "minecraft:item", "condition": "minecraft:tool/can_silk_touch", "name": "mechanical:bog_iron_ore"},
        {"type": "minecraft:item", "name": "mechanical:raw_bog_iron", "modifier": [
            {"type": "minecraft:apply_bonus", "enchantment": "minecraft:fortune", "formula": "minecraft:ore_drops"},
            {"type": "minecraft:explosion_decay"}]}]}]}],
    "random_sequence": "mechanical:blocks/bog_iron_ore"})
# any shovel digs it (requiresCorrectToolForDrops in Java); a pickaxe or a hand gets nothing
tag_add(D / "minecraft/tags/block/mineable/shovel.json", "mechanical:bog_iron_ore")

# ---- worldgen: a disk of ore 1-3 blocks deep in a pool floor, only where water stands right above ----
GROUND = [M + "mud", M + "clay", M + "dirt", M + "grass_block", M + "coarse_dirt", M + "sand", M + "gravel"]
w(D / "mechanical/worldgen/feature/bog_iron.json", {
    "type": "minecraft:disk", "half_height": 1,
    "radius": {"type": "minecraft:uniform", "min_inclusive": 1, "max_inclusive": 2},
    "state_provider": {"type": "minecraft:rule_based", "fallback": {"id": "mechanical:bog_iron_ore"}, "rules": []},
    "target": {"type": "minecraft:matching_blocks", "blocks": GROUND}})


def placed(name, rarity, count):
    # heightmap OCEAN_FLOOR_WG = the first block above the floor; 2 down is the disk's centre, so it spans the top 3 layers
    w(D / f"mechanical/worldgen/placed_feature/{name}.json", {"feature": "mechanical:bog_iron", "placement": [
        {"type": "minecraft:rarity_filter", "chance": rarity}, {"type": "minecraft:count", "count": count},
        {"type": "minecraft:in_square"}, {"type": "minecraft:heightmap", "heightmap": "OCEAN_FLOOR_WG"},
        {"type": "minecraft:offset", "x": 0, "y": -2, "z": 0},
        {"type": "minecraft:block_predicate_filter", "predicate": {"type": "minecraft:all_of", "predicates": [
            {"type": "minecraft:matching_blocks", "blocks": GROUND},
            {"type": "minecraft:matching_fluids", "fluids": M + "water", "offset": [0, 2, 0]}]}},
        {"type": "minecraft:biome"}]})


placed("bog_iron_fen", 1, 2)     # fens and swamps: most pools have some
placed("bog_iron_river", 6, 2)   # river beds: now and then
w(D / "mechanical/tags/worldgen/biome/has_bog_iron.json", {"values": ["mechanical:willow_fen", M + "swamp", M + "mangrove_swamp"]})
w(D / "mechanical/tags/worldgen/biome/has_river_bog_iron.json", {"values": [M + "river"]})
for name, tag in (("bog_iron_fen", "has_bog_iron"), ("bog_iron_river", "has_river_bog_iron")):
    w(D / f"mechanical/neoforge/biome_modifier/add_{name}.json", {
        "type": "neoforge:add_features", "biomes": f"#mechanical:{tag}",
        "features": f"mechanical:{name}", "step": "underground_ores"})

# ---- lang ----
lang_path = A / "lang/en_us.json"
lang = json.loads(lang_path.read_text(encoding="utf-8"))
lang.update({"block.mechanical.bog_iron_ore": "Bog Iron Ore", "item.mechanical.raw_bog_iron": "Raw Bog Iron"})
w(lang_path, lang)
print("bog iron ok")
