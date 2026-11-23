"""Elkwight phases 1-2 data (docs/elkwight-plan.md): the Scorched Barrens blocks (Charred Log, Charred Branch and Twig,
Wood Ash, Burnt Soil, Fireweed, Rune Blackstone;
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

# Wood Ash and Burnt Soil: three texture variants each, picked and turned at random per block, so the ground never tiles
def ground_block(name):
    names = [name, name + "_2", name + "_3"]
    for n in names:
        w(A / f"models/block/{n}.json", {"parent": M + "block/cube_all", "textures": {"all": S + f"block/{n}"}})
    w(A / f"blockstates/{name}.json", {"variants": {"": [{"model": S + f"block/{n}", **({"y": r} if r else {})} for n in names for r in (0, 90, 180, 270)]}})
    w(A / f"items/{name}.json", {"model": {"type": "minecraft:model", "model": S + f"block/{name}"}})
    w(D / f"mechanical/loot_table/blocks/{name}.json", drop_self(name))


ground_block("wood_ash")
ground_block("burnt_soil")

# Charred Branch (8 px) and Charred Twig (4 px): a core plus an arm per joined side, like the Ancient Oak's branches; the
# bark is the Charred Log's
for name, size in (("charred_branch", 8), ("charred_twig", 4)):
    lo, hi = 8 - size / 2, 8 + size / 2
    tex = {"bark": S + "block/charred_log", "end": S + "block/charred_log_top", "particle": S + "block/charred_log"}
    faces = {d: {"texture": "#end" if d in ("up", "down") else "#bark", "uv": [lo, lo, hi, hi]} for d in ("north", "south", "east", "west", "up", "down")}
    w(A / f"models/block/{name}_core.json", {"textures": tex, "elements": [{"from": [lo, lo, lo], "to": [hi, hi, hi], "faces": faces}]})
    arm = {"from": [lo, lo, 0], "to": [hi, hi, lo], "faces": {  # pointing north; the blockstate turns it
        "north": {"texture": "#end", "uv": [lo, lo, hi, hi], "cullface": "north"},
        "east": {"texture": "#bark", "uv": [0, lo, lo, hi]}, "west": {"texture": "#bark", "uv": [0, lo, lo, hi]},
        "up": {"texture": "#bark", "uv": [lo, 0, hi, lo]}, "down": {"texture": "#bark", "uv": [lo, 0, hi, lo]}}}
    w(A / f"models/block/{name}_side.json", {"textures": tex, "elements": [arm]})
    rot = {"north": {}, "east": {"y": 90}, "south": {"y": 180}, "west": {"y": 270}, "up": {"x": 270}, "down": {"x": 90}}
    w(A / f"blockstates/{name}.json", {"multipart": [{"apply": {"model": S + f"block/{name}_core"}}] +
                                       [{"when": {d: "true"}, "apply": {"model": S + f"block/{name}_side", **r}} for d, r in rot.items()]})
    w(A / f"items/{name}.json", {"model": {"type": "minecraft:model", "model": S + f"block/{name}_item"}})
    # the item: a short piece lying east-west
    w(A / f"models/block/{name}_item.json", {"parent": M + "block/block", "textures": tex, "elements": [
        {"from": [0, lo, lo], "to": [16, hi, hi], "faces": {
            "east": {"texture": "#end", "uv": [lo, lo, hi, hi]}, "west": {"texture": "#end", "uv": [lo, lo, hi, hi]},
            **{d: {"texture": "#bark", "uv": [0, lo, 16, hi]} for d in ("north", "south", "up", "down")}}}]})
    w(D / f"mechanical/loot_table/blocks/{name}.json", drop_self(name))

# Fireweed: a tall flower (the lower half drops it), the item shows the flowering top
for half in ("bottom", "top"):
    w(A / f"models/block/fireweed_{half}.json", {"parent": M + "block/cross", "textures": {"cross": S + f"block/fireweed_{half}"}})
w(A / "blockstates/fireweed.json", {"variants": {"half=lower": {"model": S + "block/fireweed_bottom"}, "half=upper": {"model": S + "block/fireweed_top"}}})
w(A / "models/item/fireweed.json", {"parent": M + "item/generated", "textures": {"layer0": S + "block/fireweed_top"}})
w(A / "items/fireweed.json", {"model": {"type": "minecraft:model", "model": S + "item/fireweed"}})
w(D / "mechanical/loot_table/blocks/fireweed.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": S + "fireweed",
    "condition": {"type": "minecraft:match_block", "blocks": S + "fireweed", "state": {"half": "lower"}}}],
    "condition": {"type": "minecraft:survives_explosion"}}], "random_sequence": "mechanical:blocks/fireweed"})

# Rune Blackstone: the carved stone, and a full-bright layer of just the runes a hair outside every face
faces = {d: {"uv": [0, 0, 16, 16], "texture": "#glow", "cullface": d} for d in ("north", "south", "east", "west", "up", "down")}
w(A / "models/block/rune_blackstone.json", {"parent": M + "block/block", "textures": {
    "particle": S + "block/rune_blackstone", "all": S + "block/rune_blackstone", "glow": S + "block/rune_blackstone_glow"}, "elements": [
    {"from": [0, 0, 0], "to": [16, 16, 16], "faces": {d: {"uv": [0, 0, 16, 16], "texture": "#all", "cullface": d} for d in faces}},
    {"from": [-0.01, -0.01, -0.01], "to": [16.01, 16.01, 16.01], "light_emission": 15, "shade": False, "faces": faces}]})
w(A / "blockstates/rune_blackstone.json", {"variants": {"": {"model": S + "block/rune_blackstone"}}})
w(D / "mechanical/loot_table/blocks/rune_blackstone.json", drop_self("rune_blackstone"))

TAGS = D / "minecraft/tags/block"
tag(TAGS / "mineable/axe.json", S + "charred_log", S + "charred_branch", S + "charred_twig")
tag(TAGS / "mineable/shovel.json", S + "wood_ash", S + "burnt_soil")
# plants take root in the burnt soil again (the barrens' regrowth: fireweed, ferns, birch saplings, dry grass)
tag(TAGS / "supports_vegetation.json", S + "burnt_soil")
tag(TAGS / "supports_dry_vegetation.json", S + "burnt_soil")
tag(TAGS / "flowers.json", S + "fireweed")
tag(TAGS / "replaceable_by_trees.json", S + "fireweed")
tag(D / "minecraft/tags/item/flowers.json", S + "fireweed")
tag(D / "mechanical/tags/item/compostable.json", S + "fireweed")
tag(TAGS / "mineable/pickaxe.json", S + "rune_blackstone")

# ---------------------------------------------------------------- the forest round the barrens
# BurntForest.OUTSIDE: within a few blocks of a Scorched Barrens the living forest is singed (charred tree feet, thinned
# crowns, dry grass, burnt patches). In every forest that may ring a barrens (ScorchedBarrens.LIVING), after its trees:
# top_layer_modification, and the modifier's name sorts before smooth_ground's, so the half steps come after.
w(D / "mechanical/worldgen/feature/scorched_barrens_edge.json", {"type": S + "burnt_forest_edge"})
w(D / "mechanical/worldgen/placed_feature/scorched_barrens_edge.json", {"feature": S + "scorched_barrens_edge", "placement": []})
LIVING = [S + "deciduous_forest"] + [M + b for b in ("forest", "flower_forest", "birch_forest", "old_growth_birch_forest", "dark_forest",
                                                       "dappled_forest", "taiga", "old_growth_pine_taiga", "old_growth_spruce_taiga")]
w(D / "mechanical/neoforge/biome_modifier/add_scorched_barrens_edge.json", {"type": "neoforge:add_features", "biomes": LIVING,
                                                                            "features": S + "scorched_barrens_edge", "step": "top_layer_modification"})

# ---------------------------------------------------------------- the ruin
w(D / "mechanical/tags/worldgen/biome/has_structure/elkwight_ruin.json", {"values": [S + "scorched_barrens"]})
w(D / "mechanical/worldgen/structure/elkwight_ruin.json", {"type": S + "elkwight_ruin", "biomes": "#mechanical:has_structure/elkwight_ruin",
                                                           "spawn_overrides": {}, "step": "surface_structures"})
# about one try per 96 blocks; it only stands where the try lands well inside a (rare) Scorched Barrens on level ground
# (ElkwightRuinStructure.CLEAR). Separation 4 chunks: two arenas (50 blocks across) never overlap
w(D / "mechanical/worldgen/structure_set/elkwight_ruin.json", {"structures": [{"structure": S + "elkwight_ruin", "weight": 1}],
                                                               "placement": {"type": "minecraft:random_spread", "salt": 1618033988, "spacing": 6, "separation": 4}})
# a little gold and bronze: the real prize is the Elkwight himself
w(D / "mechanical/loot_table/chests/elkwight_ruin.json", {"type": "minecraft:chest", "pools": [
    {"rolls": {"type": "minecraft:uniform", "min": 2, "max": 4}, "entries": [
        item(M + "gold_nugget", 10, count(2, 6)), item(M + "gold_ingot", 3), item(S + "bronze_ingot", 6, count(1, 2)),
        item(S + "bronze_arrow", 5, count(3, 8)), item(M + "charcoal", 4, count(2, 5))]},
    {"rolls": 1, "entries": [item(S + "bronze_knife", 3), item(S + "bronze_axe", 2), item(S + "bronze_spear", 2), item(S + "bronze_helmet", 1),
                             {"type": "minecraft:empty", "weight": 3}]}],
    "random_sequence": "mechanical:chests/elkwight_ruin"})

lang = read(A / "lang/en_us.json", {})
lang.update({"block.mechanical.charred_log": "Charred Log", "block.mechanical.wood_ash": "Wood Ash", "block.mechanical.rune_blackstone": "Rune Blackstone",
             "block.mechanical.burnt_soil": "Burnt Soil", "block.mechanical.charred_branch": "Charred Branch", "block.mechanical.charred_twig": "Charred Twig",
             "block.mechanical.fireweed": "Fireweed"})
w(A / "lang/en_us.json", lang, ascii=True)
print("elkwight ok")
