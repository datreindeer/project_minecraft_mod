"""Four new overworld biomes: Deciduous Forest (Lövskog), Hazel Meadow (Hasselbacke), Willow Fen (Pilkärr) and
Pine Heath (Tallhed), plus the Heather block of the Pine Heath.

How they get into the world: mixin/OverworldBiomeBuilderMixin.java gives each a share of vanilla's overworld climate
slots (part of temperate forest, part of cool/temperate plains, all of cool dry plains, the cool half of swamp). Terrain
is unchanged; only the biome map is. This script writes the biome JSONs, their own features, tags, and adds the biomes
to the mod's existing biome modifiers that fit (forage, wildlife, landmarks), so they get the same extras as their
vanilla neighbours.

Each biome starts from a vanilla biome's JSON (from the merged jar): same carvers and non-vegetation features, the
vanilla vegetation list minus its trees/flowers, plus our own (uniquely named) features. Only adding features unique to
these biomes keeps the per-step feature order consistent across biomes (FeatureSorter crashes on order cycles).

Textures: scripts/gen_undergrowth_textures.py (heather).
Generator order: ... gen_trees.py, gen_biomes.py, gen_block_items.py, gen_unlocks.py, gen_registry.py.
Run: python scripts/gen_biomes.py"""
import copy
import json
import zipfile
from pathlib import Path

BASE = Path(__file__).resolve().parent.parent
ROOT = BASE / "src/main/resources"
A, D = ROOT / "assets/mechanical", ROOT / "data"
JAR = BASE / "build/moddev/artifacts/minecraft-patched-26.3.0.95-merged.jar"
VEG = 9  # vegetal_decoration
ORES = 6  # underground_ores: vanilla's disks live here


def w(path, obj):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")


def merge(path, obj):
    old = json.loads(path.read_text(encoding="utf-8")) if path.exists() else {}
    old.update(obj)
    w(path, old)


def tag_add(path, *values):
    old = json.loads(path.read_text(encoding="utf-8")) if path.exists() else {"values": []}
    old["values"] += [v for v in values if v not in old["values"]]
    w(path, old)


jar = zipfile.ZipFile(JAR)
vanilla = lambda b: json.loads(jar.read(f"data/minecraft/worldgen/biome/{b}.json"))
tri = lambda m: {"type": "minecraft:trapezoid", "min": -m, "max": m, "plateau": 0}
weighted = lambda *counts: {"type": "minecraft:count", "count": {"type": "minecraft:weighted_list",
                            "distribution": [{"data": n, "weight": wt} for n, wt in counts]}}
survives = lambda sapling: {"type": "minecraft:block_predicate_filter", "predicate": {"type": "minecraft:would_survive", "state": sapling}}
ON_GROUND = {"type": "minecraft:block_predicate_filter", "predicate": {"type": "minecraft:all_of", "predicates": [
    {"type": "minecraft:replaceable"},
    {"type": "minecraft:matching_block_tag", "tag": "minecraft:substrate_overworld", "offset": [0, -1, 0]}]}}


def feature(name, config, placement):
    w(D / f"mechanical/worldgen/feature/{name}.json", config)
    w(D / f"mechanical/worldgen/placed_feature/{name}.json", {"feature": f"mechanical:{name}", "placement": placement})
    return f"mechanical:{name}"


def place(name, configured, placement):
    w(D / f"mechanical/worldgen/placed_feature/{name}.json", {"feature": configured, "placement": placement})
    return f"mechanical:{name}"


def trees(name, choices, default, count, water_depth=0):
    """A forest's tree mix like vanilla's trees_* features: weighted pick of *_checked placed features."""
    return feature(name, {"type": "minecraft:random_selector", "default": default,
                          "features": [{"chance": c, "feature": f} for f, c in choices]},
                   [count, {"type": "minecraft:in_square"}, {"type": "minecraft:surface_water_depth_filter", "max_water_depth": water_depth},
                    {"type": "minecraft:heightmap", "heightmap": "OCEAN_FLOOR"}, {"type": "minecraft:biome"}])


def patch(name, state, per_chunk, tries, spread, rarity=1, extra=()):
    """Clumps of a plant: per_chunk clump centres (1 in rarity chunks), tries spots each within spread blocks."""
    return feature(name, {"type": "minecraft:simple_block", "to_place": state}, [
        {"type": "minecraft:rarity_filter", "chance": rarity}, {"type": "minecraft:count", "count": per_chunk},
        {"type": "minecraft:in_square"}, {"type": "minecraft:count", "count": tries},
        {"type": "minecraft:offset", "x": tri(spread), "y": 0, "z": tri(spread)},
        {"type": "minecraft:heightmap", "heightmap": "MOTION_BLOCKING_NO_LEAVES"}, {"type": "minecraft:biome"}, ON_GROUND, *extra])


def disk(name, block, radius, per_chunk, targets, heightmap="WORLD_SURFACE_WG", where=None):
    """Patches of another ground block (podzol, sand, mud) swapped into the top layer before vegetation grows."""
    return feature(name, {"type": "minecraft:disk", "half_height": 1,
                          "radius": {"type": "minecraft:uniform", "min_inclusive": radius[0], "max_inclusive": radius[1]},
                          "state_provider": {"type": "minecraft:rule_based", "fallback": {"id": block}, "rules": []},
                          "target": {"type": "minecraft:matching_blocks", "blocks": targets}},
                   [{"type": "minecraft:count", "count": per_chunk}, {"type": "minecraft:in_square"},
                    {"type": "minecraft:heightmap", "heightmap": heightmap}, {"type": "minecraft:offset", "x": 0, "y": -1, "z": 0},
                    {"type": "minecraft:block_predicate_filter", "predicate": where or {"type": "minecraft:matching_blocks", "blocks": targets}},
                    {"type": "minecraft:biome"}])


def facing_states(block, prop, amounts):
    return {"type": "minecraft:weighted", "entries": [{"data": {"id": block, "properties": {"facing": f, prop: str(n)}}, "weight": 1}
                                                       for f in ("north", "east", "south", "west") for n in amounts]}


def fallen_tree(name, log, sapling):
    feature(name, {"type": "minecraft:fallen_tree", "log_decorators": [{"type": "minecraft:attached_to_logs", "block_provider": {
        "type": "minecraft:weighted", "entries": [{"data": "minecraft:brown_mushroom", "weight": 2}, {"data": "minecraft:red_mushroom", "weight": 1}]},
        "directions": ["up"], "probability": 0.15}], "log_length": {"type": "minecraft:uniform", "min_inclusive": 4, "max_inclusive": 7},
        "stump_decorators": [], "trunk_provider": {"id": log, "properties": {"axis": "y"}}}, [survives(sapling)])
    return f"mechanical:{name}"


def flowers(name, blocks, per_chunk):
    return patch(name, {"type": "minecraft:weighted", "entries": [{"data": b, "weight": wt} for b, wt in blocks]}, per_chunk, 16, 6)


def spawn(entity, weight, lo, hi):
    return {"type": entity, "weight": weight, "count": {"type": "minecraft:uniform", "min_inclusive": lo, "max_inclusive": hi}}


def biome(name, base, *, temperature, downfall, effects, attributes=None, drop=(), veg_after=None, veg=(), ores=(), creatures=()):
    """base: vanilla biome to copy; drop: its vegetation features to remove; veg_after: insert our vegetation right after this
    vanilla feature (default: at the start); ores: our ground disks (end of underground_ores); creatures: extra spawns."""
    b = vanilla(base)
    b["temperature"], b["downfall"], b["effects"] = temperature, downfall, effects
    b["attributes"].update(attributes or {})
    v = [f for f in b["features"][VEG] if f not in drop]
    at = v.index(veg_after) + 1 if veg_after else 0
    b["features"][VEG] = v[:at] + list(veg) + v[at:]
    b["features"][ORES] += list(ores)
    if creatures:
        b["attributes"]["minecraft:gameplay/natural_mob_spawns"]["argument"]["spawns_by_category"]["creature"] += list(creatures)
    w(D / f"mechanical/worldgen/biome/{name}.json", b)


def music(sound):
    return {"minecraft:audio/background_music": {"default": {"max_delay": 24000, "min_delay": 12000, "sound": sound}}}


M = "minecraft:"
# ---------------------------------------------------------------- Deciduous Forest (Lövskog)
DF_FALLEN = fallen_tree("fallen_beech_tree", "mechanical:beech_log", "mechanical:beech_sapling")
biome("deciduous_forest", "forest", temperature=0.7, downfall=0.8,
      effects={"grass_color": "#65a23a", "foliage_color": "#4e9428", "dry_foliage_color": "#7b5334", "water_color": "#3f76e4"},
      attributes={"minecraft:visual/sky_color": "#79a6ff"},
      drop=[M + "trees_birch_and_oak_leaf_litter"], veg_after=M + "forest_flowers",
      # random_selector tries entries in order, so these chances give about: beech 40 %, oak 20 %, linden 15 %, ash 15 %,
      # hazel 8 %, a fallen beech 2 %
      veg=[trees("deciduous_forest_trees", [(DF_FALLEN, 0.02), ("mechanical:beech_checked", 0.41), ("mechanical:linden_checked", 0.26),
                                            ("mechanical:ash_checked", 0.35), ("mechanical:hazel_checked", 0.29), (M + "fancy_oak_checked", 0.25)],
                 M + "oak_checked", weighted((11, 9), (13, 1))),
           # thick leaf litter: mostly old brown litter, some still beech/linden/oak coloured
           feature("deciduous_leaf_litter", {"type": "minecraft:simple_block", "to_place": {"type": "minecraft:weighted", "entries":
                   facing_states(M + "leaf_litter", "segment_amount", (1, 2, 3, 4))["entries"] * 3
                   + facing_states("mechanical:beech_leaf_litter", "segment_amount", (2, 3, 4))["entries"]
                   + facing_states("mechanical:linden_leaf_litter", "segment_amount", (1, 2, 3))["entries"]
                   + facing_states("mechanical:oak_leaf_litter", "segment_amount", (1, 2))["entries"]}},
                   [{"type": "minecraft:count", "count": 4}, {"type": "minecraft:in_square"},
                    {"type": "minecraft:heightmap", "heightmap": "WORLD_SURFACE_WG"}, {"type": "minecraft:biome"},
                    {"type": "minecraft:count", "count": 32}, {"type": "minecraft:offset", "x": tri(7), "y": tri(3), "z": tri(7)},
                    {"type": "minecraft:block_predicate_filter", "predicate": {"type": "minecraft:all_of", "predicates": [
                        {"type": "minecraft:matching_block_tag", "tag": "minecraft:air"},
                        {"type": "minecraft:matching_blocks", "blocks": [M + "grass_block", M + "podzol", M + "dirt"], "offset": [0, -1, 0]}]}}]),
           patch("deciduous_mushrooms", {"type": "minecraft:weighted", "entries": [{"data": M + "brown_mushroom", "weight": 3},
                 {"data": M + "red_mushroom", "weight": 1}]}, 2, 12, 4)])

# ---------------------------------------------------------------- Hazel Meadow (Hasselbacke)
# hazel clumps: 2-4 multi-stemmed hazels around one spot (in_square once, then the offsets), in 2 of 3 chunks
hazel_clumps = "mechanical:hazel_meadow_hazels"
w(D / "mechanical/worldgen/placed_feature/hazel_meadow_hazels.json", {"feature": "mechanical:hazel", "placement": [
    weighted((0, 2), (2, 1), (3, 2), (4, 1)), {"type": "minecraft:in_square"},
    {"type": "minecraft:offset", "x": tri(4), "y": 0, "z": tri(4)}, {"type": "minecraft:surface_water_depth_filter", "max_water_depth": 0},
    {"type": "minecraft:heightmap", "heightmap": "MOTION_BLOCKING_NO_LEAVES"}, {"type": "minecraft:biome"}, survives("mechanical:hazel_sapling")]})
biome("hazel_meadow", "plains", temperature=0.75, downfall=0.6,
      effects={"grass_color": "#8cc152", "foliage_color": "#6fae3c", "dry_foliage_color": "#a07a4a", "water_color": "#3f76e4"},
      attributes=music(M + "music.overworld.meadow"),
      drop=[M + "trees_plains", M + "flower_plains"], veg_after=M + "patch_bush",
      veg=[hazel_clumps,
           trees("hazel_meadow_trees", [("mechanical:ash_checked", 0.45), ("mechanical:linden_checked", 0.15)], M + "oak_checked",
                 weighted((0, 1), (1, 1))),
           flowers("hazel_meadow_flowers", [(M + "oxeye_daisy", 4), (M + "cornflower", 3), (M + "poppy", 3), (M + "dandelion", 3),
                                            (M + "azure_bluet", 2), (M + "allium", 1), (M + "lily_of_the_valley", 1)], 4),
           feature("hazel_meadow_wildflowers", {"type": "minecraft:simple_block", "to_place": facing_states(M + "wildflowers", "flower_amount", (1, 2, 3, 4))},
                   [{"type": "minecraft:count", "count": 3}, {"type": "minecraft:in_square"}, {"type": "minecraft:heightmap", "heightmap": "MOTION_BLOCKING"},
                    {"type": "minecraft:biome"}, {"type": "minecraft:count", "count": 8}, {"type": "minecraft:offset", "x": tri(6), "y": tri(2), "z": tri(6)},
                    {"type": "minecraft:block_predicate_filter", "predicate": {"type": "minecraft:matching_block_tag", "tag": "minecraft:air"}}])],
      creatures=[spawn("mechanical:roe_deer", 8, 2, 3), spawn("mechanical:hare", 8, 1, 2), spawn("mechanical:pheasant", 6, 1, 3),
                 spawn(M + "rabbit", 4, 2, 3)])

# ---------------------------------------------------------------- Willow Fen (Pilkärr)
WET = [M + "dirt", M + "grass_block", M + "sand", M + "gravel", M + "clay"]
biome("willow_fen", "swamp", temperature=0.6, downfall=0.9,
      effects={"grass_color": "#6b8a46", "foliage_color": "#5c7f38", "dry_foliage_color": "#7b5334", "water_color": "#56705a"},
      attributes={**music(M + "music.overworld.swamp"), "minecraft:visual/sky_color": "#82a6dc", "minecraft:visual/fog_color": "#b9c6b2",
                  "minecraft:visual/fog_end_distance": {"argument": 0.8, "modifier": "multiply"},
                  "minecraft:visual/water_fog_color": "#2f3b28", "minecraft:visual/water_fog_end_distance": {"argument": 0.6, "modifier": "multiply"}},
      drop=[M + "trees_swamp", M + "flower_swamp", M + "patch_dead_bush"],
      veg=[trees("willow_fen_trees", [("mechanical:willow_checked", 0.55)], "mechanical:alder_checked", weighted((3, 2), (4, 2), (6, 1)), 1),
           patch("willow_fen_ferns", {"type": "minecraft:weighted", "entries": [{"data": M + "fern", "weight": 3},
                 {"data": M + "short_grass", "weight": 2}, {"data": M + "blue_orchid", "weight": 1}]}, 2, 16, 5),
           # reeds along every pool edge (vanilla's sugar cane, placed like patch_sugar_cane_swamp but in every chunk)
           place("willow_fen_reeds", M + "sugar_cane", [{"type": "minecraft:count", "count": 2}, {"type": "minecraft:in_square"},
                 {"type": "minecraft:heightmap", "heightmap": "MOTION_BLOCKING"}, {"type": "minecraft:biome"}, {"type": "minecraft:count", "count": 20},
                 {"type": "minecraft:offset", "x": tri(4), "y": 0, "z": tri(4)},
                 {"type": "minecraft:block_predicate_filter", "predicate": {"type": "minecraft:all_of", "predicates": [
                     {"type": "minecraft:matching_block_tag", "tag": "minecraft:air"}, {"type": "minecraft:would_survive", "state": M + "sugar_cane"}]}}])],
      # mud on the pool floors and spreading up the banks
      ores=[disk("willow_fen_mud", M + "mud", (2, 4), 6, WET, "OCEAN_FLOOR_WG",
                 {"type": "minecraft:all_of", "predicates": [{"type": "minecraft:matching_blocks", "blocks": WET},
                                                             {"type": "minecraft:matching_fluids", "fluids": M + "water", "offset": [0, 1, 0]}]})],
      creatures=[spawn("mechanical:wild_boar", 6, 2, 4)])

# ---------------------------------------------------------------- Pine Heath (Tallhed)
patch("heather", {"id": "mechanical:heather"}, 3, 28, 6)
biome("pine_heath", "old_growth_pine_taiga", temperature=0.35, downfall=0.4,
      effects={"grass_color": "#98a65a", "foliage_color": "#6f8c48", "dry_foliage_color": "#9a7a50", "water_color": "#3d6ee0"},
      attributes={**music(M + "music.overworld.old_growth_taiga"), "minecraft:visual/sky_color": "#80a4ff"},
      drop=[M + "trees_old_growth_pine_taiga", M + "patch_large_fern", M + "brown_mushroom_old_growth", M + "red_mushroom_old_growth"],
      veg=[trees("pine_heath_trees", [(fallen_tree("fallen_pine_tree", "mechanical:pine_log", "mechanical:pine_sapling"), 0.04)],
                 "mechanical:pine_checked", weighted((0, 2), (1, 3), (2, 2), (3, 1))), "mechanical:heather",
           patch("pine_heath_lingonberry", {"id": "mechanical:lingonberry_bush", "properties": {"age": "3"}}, 1, 8, 4, rarity=2),
           patch("pine_heath_blueberry", {"id": "mechanical:blueberry_bush", "properties": {"age": "3"}}, 1, 6, 4, rarity=4)],
      ores=[disk("pine_heath_podzol", M + "podzol", (2, 5), 3, [M + "grass_block"]),
            disk("pine_heath_coarse_dirt", M + "coarse_dirt", (2, 4), 2, [M + "grass_block"]),
            disk("pine_heath_sand", M + "sand", (1, 3), 1, [M + "grass_block"])],
      creatures=[spawn("mechanical:hare", 8, 1, 2), spawn("mechanical:red_deer", 5, 2, 4)])

# ---------------------------------------------------------------- heather block
w(A / "models/block/heather.json", {"parent": "minecraft:block/cross", "textures": {"cross": "mechanical:block/heather"}})
w(A / "blockstates/heather.json", {"variants": {"": {"model": "mechanical:block/heather"}}})
w(D / "mechanical/loot_table/blocks/heather.json", {"type": "minecraft:block", "pools": [
    {"rolls": 1, "entries": [{"type": "minecraft:alternatives", "children": [
        {"type": "minecraft:item", "name": "mechanical:heather", "condition": "minecraft:tool/can_shear"},
        {"type": "minecraft:item", "name": "mechanical:plant_fibre", "condition": {"type": "minecraft:random_chance", "chance": 0.5}}]}]}],
    "random_sequence": "mechanical:blocks/heather"})
tag_add(D / "minecraft/tags/block/replaceable_by_trees.json", "mechanical:heather")
tag_add(D / "mechanical/tags/item/compostable.json", "mechanical:heather")

# ---------------------------------------------------------------- lang
merge(A / "lang/en_us.json", {"biome.mechanical.deciduous_forest": "Deciduous Forest", "biome.mechanical.hazel_meadow": "Hazel Meadow",
                              "biome.mechanical.willow_fen": "Willow Fen", "biome.mechanical.pine_heath": "Pine Heath",
                              "block.mechanical.heather": "Heather"})

# ---------------------------------------------------------------- tags
DF, HM, WF, PH = (f"mechanical:{b}" for b in ("deciduous_forest", "hazel_meadow", "willow_fen", "pine_heath"))
BT = D / "minecraft/tags/worldgen/biome"
CT = D / "c/tags/worldgen/biome"
TAGS = {BT / "is_overworld.json": [DF, HM, WF, PH], BT / "is_forest.json": [DF], BT / "is_taiga.json": [PH],
        BT / "allows_surface_slime_spawns.json": [WF], BT / "stronghold_biased_to.json": [DF, HM, PH],
        BT / "has_structure/village_plains.json": [HM], BT / "has_structure/swamp_hut.json": [WF],
        BT / "has_structure/mineshaft.json": [HM, WF], BT / "has_structure/ruined_portal_swamp.json": [WF],
        BT / "has_structure/trail_ruins.json": [PH],
        CT / "is_swamp.json": [WF], CT / "is_wet/overworld.json": [WF], CT / "is_plains.json": [HM], CT / "is_dry/overworld.json": [PH],
        CT / "is_tree/deciduous.json": [DF], CT / "is_tree/coniferous.json": [PH], CT / "is_floral.json": [HM],
        CT / "is_temperate/overworld.json": [DF, HM, WF],
        D / "mechanical/tags/worldgen/biome/has_structure/tribe_village.json": [DF, HM, PH],
        D / "mechanical/tags/worldgen/biome/has_structure/ancient_oak.json": [DF]}
for path, values in TAGS.items():
    tag_add(path, *values)

# ---------------------------------------------------------------- join the mod's existing biome modifiers
JOIN = {
    DF: ["animal_carving", "badger", "bramble", "chanterelle", "crow", "elderberry_bush", "giant_deer", "hazel_shrub", "hedgehog",
         "hermits_hut", "morel", "oyster_mushroom", "porcini", "raspberry_bush", "rune_stone", "sapling_thicket", "shrine_statue",
         "squirrel", "undergrowth", "wild_emmer_forest", "wild_pea", "willow_lakeside"],
    HM: ["animal_carving", "aurochs", "bramble_in_plains", "burial_mound", "crow", "elderberry_bush", "flint_quarry", "giant_deer",
         "glacial_erratic", "hazel_shrub", "hedgehog_open", "puffball", "raspberry_bush", "rune_stone", "shrine_statue",
         "standing_stones", "stone_head", "wild_einkorn", "wild_emmer", "wild_horse", "wild_pea", "wild_strawberry_bush", "willow_lakeside"],
    WF: ["cloudberry_bush", "elderberry_bush", "goose", "moose", "oyster_mushroom"],
    PH: ["badger", "blueberry_bush", "burial_mound", "cairn", "chanterelle", "fly_agaric", "giant_deer", "glacial_erratic", "juniper",
         "lingonberry_bush", "porcini", "rune_stone", "squirrel", "standing_stones"],
}
MOD = D / "mechanical/neoforge/biome_modifier"
for b, names in JOIN.items():
    for n in names:
        path = MOD / f"add_{n}.json"
        m = json.loads(path.read_text(encoding="utf-8"))
        assert isinstance(m["biomes"], list), f"{path.name}: biomes is not a plain list"  # tag/or modifiers reach us through tags
        if b not in m["biomes"]:
            m["biomes"].append(b)
        w(path, m)
print("biomes ok")
