"""Elkwight phases 1-2 data (docs/elkwight-plan.md): the Scorched Barrens blocks (Charred Log, Wood Ash, Rune Blackstone;
Java: worldgen/ScorchedBarrens.java) and the ruin structure (worldgen/ElkwightRuinStructure.java): its structure and
structure set, biome tag and chest loot. The biome itself is a row in gen_world_biomes.py; textures come from the standalone
gen_elkwight_textures.py.

Generator order: ... gen_world_biomes.py, gen_caves.py, gen_elkwight.py, ... gen_block_items.py ...
Run: python scripts/gen_elkwight.py"""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent / "src/main/resources"
A, D = ROOT / "assets/mechanical", ROOT / "data"
M, S = "minecraft:", "mechanical:"


def w(path, obj, ascii=False):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, indent=2, ensure_ascii=ascii) + "\n", encoding="utf-8")


def read(path, default):
    return json.loads(path.read_text(encoding="utf-8")) if path.exists() else default


def tag(path, *values):
    t = read(path, {"values": []})
    for v in values:
        if v not in t["values"]:
            t["values"].append(v)
    w(path, t)


count = lambda lo, hi: {"type": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": lo, "max": hi}}
item = lambda name, weight=1, *mods: {"type": "minecraft:item", "name": name, "weight": weight, **({"modifier": list(mods)} if mods else {})}


def drop_self(name):
    return {"type": "minecraft:block", "pools": [{"rolls": 1, "condition": {"type": "minecraft:survives_explosion"},
                                                  "entries": [{"type": "minecraft:item", "name": S + name}]}],
            "random_sequence": f"mechanical:blocks/{name}"}


# ---------------------------------------------------------------- blocks
# Charred Log: a pillar like a log
w(A / "models/block/charred_log.json", {"parent": M + "block/cube_column", "textures": {"end": S + "block/charred_log_top", "side": S + "block/charred_log"}})
w(A / "models/block/charred_log_horizontal.json", {"parent": M + "block/cube_column_horizontal",
                                                   "textures": {"end": S + "block/charred_log_top", "side": S + "block/charred_log"}})
w(A / "blockstates/charred_log.json", {"variants": {
    "axis=y": {"model": S + "block/charred_log"},
    "axis=z": {"model": S + "block/charred_log_horizontal", "x": 90},
    "axis=x": {"model": S + "block/charred_log_horizontal", "x": 90, "y": 90}}})
# 1-2 Charcoal, the log itself with Silk Touch
w(D / "mechanical/loot_table/blocks/charred_log.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{"type": "minecraft:alternatives", "children": [
    {"type": "minecraft:item", "condition": M + "tool/can_silk_touch", "name": S + "charred_log"},
    {"type": "minecraft:item", "condition": {"type": "minecraft:survives_explosion"}, "name": M + "charcoal", "modifier": [count(1, 2)]}]}]}],
    "random_sequence": "mechanical:blocks/charred_log"})

w(A / "models/block/wood_ash.json", {"parent": M + "block/cube_all", "textures": {"all": S + "block/wood_ash"}})
w(A / "blockstates/wood_ash.json", {"variants": {"": [{"model": S + "block/wood_ash"}, {"model": S + "block/wood_ash", "y": 90},
                                                      {"model": S + "block/wood_ash", "y": 180}, {"model": S + "block/wood_ash", "y": 270}]}})
w(D / "mechanical/loot_table/blocks/wood_ash.json", drop_self("wood_ash"))

# Rune Blackstone: the carved stone, and a full-bright layer of just the runes a hair outside every face
faces = {d: {"uv": [0, 0, 16, 16], "texture": "#glow", "cullface": d} for d in ("north", "south", "east", "west", "up", "down")}
w(A / "models/block/rune_blackstone.json", {"parent": M + "block/block", "textures": {
    "particle": S + "block/rune_blackstone", "all": S + "block/rune_blackstone", "glow": S + "block/rune_blackstone_glow"}, "elements": [
    {"from": [0, 0, 0], "to": [16, 16, 16], "faces": {d: {"uv": [0, 0, 16, 16], "texture": "#all", "cullface": d} for d in faces}},
    {"from": [-0.01, -0.01, -0.01], "to": [16.01, 16.01, 16.01], "light_emission": 15, "shade": False, "faces": faces}]})
w(A / "blockstates/rune_blackstone.json", {"variants": {"": {"model": S + "block/rune_blackstone"}}})
w(D / "mechanical/loot_table/blocks/rune_blackstone.json", drop_self("rune_blackstone"))

TAGS = D / "minecraft/tags/block"
tag(TAGS / "mineable/axe.json", S + "charred_log")
tag(TAGS / "mineable/shovel.json", S + "wood_ash")
tag(TAGS / "mineable/pickaxe.json", S + "rune_blackstone")

# ---------------------------------------------------------------- the ruin
w(D / "mechanical/tags/worldgen/biome/has_structure/elkwight_ruin.json", {"values": [S + "scorched_barrens"]})
w(D / "mechanical/worldgen/structure/elkwight_ruin.json", {"type": S + "elkwight_ruin", "biomes": "#mechanical:has_structure/elkwight_ruin",
                                                           "spawn_overrides": {}, "step": "surface_structures"})
# about one try per 256 blocks; it only stands where the try lands in a (rare) Scorched Barrens on level ground
w(D / "mechanical/worldgen/structure_set/elkwight_ruin.json", {"structures": [{"structure": S + "elkwight_ruin", "weight": 1}],
                                                               "placement": {"type": "minecraft:random_spread", "salt": 1618033988, "spacing": 16, "separation": 6}})
# a little gold and bronze: the real prize is the Elkwight himself
w(D / "mechanical/loot_table/chests/elkwight_ruin.json", {"type": "minecraft:chest", "pools": [
    {"rolls": {"type": "minecraft:uniform", "min": 2, "max": 4}, "entries": [
        item(M + "gold_nugget", 10, count(2, 6)), item(M + "gold_ingot", 3), item(S + "bronze_ingot", 6, count(1, 2)),
        item(S + "bronze_arrow", 5, count(3, 8)), item(M + "charcoal", 4, count(2, 5))]},
    {"rolls": 1, "entries": [item(S + "bronze_knife", 3), item(S + "bronze_axe", 2), item(S + "bronze_spear", 2), item(S + "bronze_helmet", 1),
                             {"type": "minecraft:empty", "weight": 3}]}],
    "random_sequence": "mechanical:chests/elkwight_ruin"})

lang = read(A / "lang/en_us.json", {})
lang.update({"block.mechanical.charred_log": "Charred Log", "block.mechanical.wood_ash": "Wood Ash", "block.mechanical.rune_blackstone": "Rune Blackstone"})
w(A / "lang/en_us.json", lang, ascii=True)
print("elkwight ok")
