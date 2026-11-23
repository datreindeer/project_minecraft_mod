"""World-materials biomes (docs/world-materials-plan.md): surface biomes and cave biomes built around the new stones.

Phase 0 (spike): Chalk Downs (surface) and Limestone Caverns (cave). Phases 4 and 7: the 12 surface biomes (Chalk Downs,
Peat Bog, Slate Highlands, ... Gypsum Flats), each a BIOMES + MATERIALS row, an EXTRA row (own features, spawns, biome
modifiers joined or skipped, has_<material> tags; no_creatures drops template spawns) and a ModBiomes.SURFACE row in Java (its climate slot; the mixin cuts
it out of the vanilla biome). Cave biomes: a row in ModBiomes.CAVES. Also the Glacial Boulder feature and Beach Pebbles
on vanilla Stony Shores.

Ground blocks: in 26.3 the surface rules are data (registry worldgen/material_rule; vanilla's overworld.json is the
root). This script writes mechanical:overworld (our rules, one condition per biome) and overrides
minecraft:overworld with vanilla's own sequence plus one reference to ours, right after the ore veins, so our rules
win over vanilla's surface and deepslate rules but only inside our biomes.

Each biome copies a vanilla biome's JSON (minus the dropped features, so per-step feature order stays consistent and
FeatureSorter is happy) and joins every biome tag and every mod biome modifier that lists that vanilla biome.

Generator order: ... gen_biomes.py, gen_world_biomes.py, ... (after gen_limestone_blocks.py), gen_block_items.py ...
Run: python scripts/gen_world_biomes.py"""
import json
import random
import zipfile
from pathlib import Path

from gen_caves import biome_features

BASE = Path(__file__).resolve().parent.parent
ROOT = BASE / "src/main/resources"
A, D = ROOT / "assets/mechanical", ROOT / "data"
jar = zipfile.ZipFile(BASE / "build/moddev/artifacts/minecraft-patched-26.3.0.95-merged.jar")
M = "minecraft:"


def w(path, obj, ascii=False):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, indent=2, ensure_ascii=ascii) + "\n", encoding="utf-8")


def read(path, default):
    return json.loads(path.read_text(encoding="utf-8")) if path.exists() else default


# ---------------------------------------------------------------- biomes
# name: (vanilla template, English name, drop these template features, overrides of top-level biome fields)
# Two override keys are merged instead: "attributes" (added to the template's) and "add_features" ({step index: [placed
# feature ids]}, appended to that step; ids must be unique to the biome or FeatureSorter finds order cycles).
DRIP = [M + "large_dripstone", M + "dripstone_cluster", M + "pointed_dripstone"]  # dropped from the dripstone_caves template
DRIP_SOUND = {"sound": M + "block.pointed_dripstone.drip_water", "tick_chance": 0.01}


def cave(temperature, downfall, fog, feats, sound=None, particle=None, water="#3f76e4", water_fog=None):
    """Overrides for a cave biome copied from dripstone_caves: climate, fog and water colours, the cave mood plus an
    extra sound, ambient particles, and the biome's own features (gen_caves.FEATURES)."""
    attributes = {"minecraft:visual/fog_color": fog}
    if water_fog:
        attributes["minecraft:visual/water_fog_color"] = water_fog
    if sound:
        attributes["minecraft:audio/ambient_sounds"] = {"mood": {"block_search_extent": 8, "offset": 2.0, "sound": M + "ambient.cave",
                                                                 "tick_delay": 6000}, "additions": sound}
    if particle:
        attributes["minecraft:visual/ambient_particles"] = {"argument": [{"particle": {"type": particle[0]}, "probability": particle[1]}],
                                                            "modifier": "append"}
    return {"temperature": temperature, "downfall": downfall, "has_precipitation": temperature < 1.0, "effects": {"water_color": water},
            "attributes": attributes, "add_features": biome_features(feats)}


BIOMES = {
    "chalk_downs": ("plains", "Chalk Downs", [M + "trees_plains"],
                    {"temperature": 0.7, "downfall": 0.5, "effects": {"grass_color": "#8fb35a", "water_color": "#3f76e4"}}),
    # cave biomes (phases 5 and 8, docs/caves.md): placed features from gen_caves.py, climate slots in ModBiomes.CAVES
    "limestone_caverns": ("dripstone_caves", "Limestone Caverns", DRIP, cave(0.7, 0.5, "#b9b4a4", feats="limestone_caverns",
                          sound=DRIP_SOUND, particle=(M + "dripping_dripstone_water", 0.002))),
    "salt_caves": ("dripstone_caves", "Salt Caves", DRIP, cave(1.0, 0.0, "#e6e1d6", feats="salt_caves", particle=(M + "white_ash", 0.004))),
    "ochre_caves": ("dripstone_caves", "Ochre Caves", DRIP, cave(1.0, 0.2, "#b9825a", feats="ochre_caves")),
    "soapstone_grottos": ("dripstone_caves", "Soapstone Grottos", DRIP, cave(0.3, 0.4, "#8a9a8c", feats="soapstone_grottos")),
    "glowworm_caves": ("dripstone_caves", "Glowworm Caves", DRIP, cave(0.6, 0.8, "#1f3352", feats="glowworm_caves", sound=DRIP_SOUND,
                       water="#3a6a9a")),
    "ice_caves": ("dripstone_caves", "Ice Caves", DRIP, cave(-0.7, 0.4, "#a8c6e6", feats="ice_caves", particle=(M + "snowflake", 0.004),
                  water="#3938c9")),
    "peat_hollows": ("dripstone_caves", "Peat Hollows", DRIP, cave(0.2, 0.9, "#3b3326", feats="peat_hollows", particle=(M + "mycelium", 0.01),
                     water="#5a4d2c")),
    "bone_caves": ("dripstone_caves", "Bone Caves", DRIP, cave(0.5, 0.3, "#8f8676", feats="bone_caves")),
    "galena_seams": ("dripstone_caves", "Galena Seams", DRIP, cave(0.5, 0.3, "#4a4c52", feats="galena_seams", particle=(M + "white_ash", 0.002))),
    "magma_vents": ("dripstone_caves", "Magma Vents", DRIP, cave(1.5, 0.0, "#5c2a1a", feats="magma_vents", particle=(M + "ash", 0.02),
                    sound={"sound": M + "ambient.basalt_deltas.additions", "tick_chance": 0.0111})),
    "crystal_geodes": ("dripstone_caves", "Crystal Geodes", DRIP, cave(0.7, 0.5, "#c9b8e0", feats="crystal_geodes",
                       sound={"sound": M + "block.amethyst_block.chime", "tick_chance": 0.01})),
    "flooded_caves": ("dripstone_caves", "Flooded Caves", DRIP, cave(0.6, 1.0, "#2a5560", feats="flooded_caves", sound=DRIP_SOUND,
                      water="#2f6f8a", water_fog="#173a48")),
}

# ---------------------------------------------------------------- ground blocks
CHALK_STONE = "mechanical:chalk_block"
MOD = "mechanical:"
LIMESTONE = MOD + "limestone"
# Every key is optional; a value is a block id or a whole material rule (pick(), bands() below). Surface biomes: top
# (open air), wet (the floor under water; default `under`), under (one layer below top), stone (the layers below that,
# about 3-9 deep), slope (top and under on steep slopes), all (everything else above the preliminary surface, like
# vanilla's badlands bands). Cave biomes: floor and
# ceiling (the block touching the cave air), wall (all other stone in the biome, so walls and the rock around the cave).
MATERIALS = {
    "chalk_downs": {"top": M + "grass_block", "under": M + "dirt", "stone": CHALK_STONE, "slope": CHALK_STONE},
    "limestone_caverns": {"floor": MOD + "travertine", "ceiling": LIMESTONE, "wall": LIMESTONE},
    "salt_caves": {"floor": MOD + "rock_salt", "ceiling": MOD + "rock_salt", "wall": MOD + "gypsum"},
    "ochre_caves": {"floor": MOD + "red_ochre_block", "ceiling": MOD + "chalk_block", "wall": MOD + "chalk_block"},
    "soapstone_grottos": {"floor": MOD + "slate", "ceiling": MOD + "soapstone", "wall": MOD + "soapstone"},
    # no wall: only the floor and ceiling change, the rock around stays vanilla (also in Peat Hollows and Flooded Caves)
    "glowworm_caves": {"floor": M + "moss_block", "ceiling": M + "rooted_dirt"},
    "ice_caves": {"floor": M + "packed_ice", "ceiling": M + "packed_ice", "wall": MOD + "slate"},
    "peat_hollows": {"floor": MOD + "peat", "ceiling": MOD + "peat"},
    "bone_caves": {"floor": M + "coarse_dirt", "ceiling": LIMESTONE, "wall": LIMESTONE},
    "galena_seams": {"floor": MOD + "shale", "ceiling": MOD + "alum_shale", "wall": MOD + "shale"},
    "magma_vents": {"floor": MOD + "scoria", "ceiling": MOD + "coastal_basalt", "wall": MOD + "coastal_basalt"},
    "crystal_geodes": {"floor": MOD + "quartzite", "ceiling": MOD + "quartzite", "wall": MOD + "quartzite"},
    "flooded_caves": {"floor": M + "gravel", "ceiling": M + "clay"},
}

# ---------------------------------------------------------------- phases 4 and 7: the surface biomes
# Climate slots: ModBiomes.SURFACE (Java), one row per biome. Below: their BIOMES and MATERIALS rows, then EXTRA (own
# features, spawns, which mod biome modifiers they join or leave, which has_<material> tags they join).
noise = lambda lo, hi=1e9, n=M + "surface": {"type": "minecraft:noise_threshold", "noise": n, "min_threshold": lo, "max_threshold": hi}
y_above = lambda y: {"type": "minecraft:y_above", "anchor": {"absolute": y}, "surface_depth_multiplier": 0, "add_stone_depth": False}
plain = lambda b: {"type": "minecraft:block", "result_state": b}


def pick(*patches, default=None):
    """(threshold, block) patches where the surface noise is above the threshold (first match wins), else default
    (None: fall through to vanilla's grass and dirt)."""
    rules = [{"type": "minecraft:condition", "if_true": noise(t), "then_run": plain(b)} for t, b in patches]
    return {"type": "minecraft:sequence", "sequence": rules + ([plain(default)] if default else [])}


def bands(blocks, lo=40, hi=200, seed=7):
    """Horizontal layers 1-3 blocks thick from y lo to hi, like badlands terracotta (repeat a block to weight it)."""
    rnd, y, layers = random.Random(seed), lo, []
    while y < hi:
        layers.append((y, rnd.choice(blocks)))
        y += rnd.choice((1, 1, 2, 2, 3))
    rules = [{"type": "minecraft:condition", "if_true": y_above(y), "then_run": plain(b)} for y, b in reversed(layers)]
    return {"type": "minecraft:sequence", "sequence": rules + [plain(blocks[0])]}


S = lambda n: "mechanical:" + n
OCHRE_BANDS = bands([S("red_ochre_block")] * 3 + [S("yellow_ochre_block")] * 2 + [S("gypsum"), S("mudstone"), M + "terracotta", M + "orange_terracotta"])
BIOMES.update({
    "peat_bog": ("swamp", "Peat Bog", [M + "trees_swamp", M + "flower_swamp", M + "patch_dead_bush"],
                 {"temperature": 0.3, "downfall": 0.9, "effects": {"grass_color": "#8c8a4e", "foliage_color": "#6f7840", "dry_foliage_color": "#7b5334",
                                                                  "water_color": "#4a4330"}}),
    "slate_highlands": ("windswept_hills", "Slate Highlands", [M + "trees_windswept_hills"],
                        {"temperature": 0.15, "downfall": 0.5, "effects": {"grass_color": "#7f9a68", "foliage_color": "#6a8a52", "water_color": "#3a5c8c"}}),
    "soapstone_valley": ("windswept_forest", "Soapstone Valley", [],
                         {"temperature": 0.45, "downfall": 0.6, "effects": {"grass_color": "#79a85e", "foliage_color": "#5e9446", "water_color": "#3f76e4"}}),
    "limestone_karst": ("meadow", "Limestone Karst", [M + "trees_meadow"],
                        {"temperature": 0.6, "downfall": 0.4, "effects": {"grass_color": "#9cb866", "foliage_color": "#7fa650", "water_color": "#2fa2c0"}}),
    "marble_gorge": ("savanna_plateau", "Marble Gorge", [M + "trees_savanna"],
                     {"temperature": 1.2, "downfall": 0.1, "effects": {"grass_color": "#b2ad6a", "foliage_color": "#9ea254", "water_color": "#3fa0d8"}}),
    "ochre_badlands": ("badlands", "Ochre Badlands", [],
                       {"effects": {"grass_color": "#a08a4c", "foliage_color": "#9e814d", "water_color": "#6b8a9a"}}),
    "basalt_coast": ("stony_shore", "Basalt Coast", [],
                     {"temperature": 0.4, "downfall": 0.6, "effects": {"water_color": "#2f5a8c"}}),
    "shingle_beach": ("beach", "Shingle Beach", [],
                      {"temperature": 0.3, "downfall": 0.5, "effects": {"water_color": "#3a6ea8"}}),
    "reed_delta": ("swamp", "Reed Delta", [M + "trees_swamp", M + "flower_swamp", M + "patch_dead_bush"],
                   {"temperature": 0.7, "downfall": 0.8, "effects": {"grass_color": "#7fa64a", "foliage_color": "#6c9a3c", "dry_foliage_color": "#7b5334",
                                                                    "water_color": "#557a6a"}}),
    "glacial_moraine": ("taiga", "Glacial Moraine", [M + "trees_taiga", M + "patch_large_fern"],
                        {"temperature": 0.2, "downfall": 0.6, "effects": {"grass_color": "#7f9a72", "foliage_color": "#5f8058", "water_color": "#3a5f9a"}}),
    "gypsum_flats": ("desert", "Gypsum Flats", [M + "patch_cactus_desert"],
                     {"effects": {"grass_color": "#bfb791", "foliage_color": "#aea474", "water_color": "#48a8c8"}}),
    # cold and dry, but warm enough (0.3, like taiga) for grass instead of snow cover; no trees
    "mammoth_steppe": ("snowy_plains", "Mammoth Steppe", [M + "trees_snowy", M + "patch_sugar_cane", M + "patch_firefly_bush_near_water"],
                       {"temperature": 0.3, "downfall": 0.1, "effects": {"grass_color": "#b4ab6c", "foliage_color": "#9c9a5e", "water_color": "#3a64a8"}}),
})
MATERIALS.update({
    # peat under thin grass, open peat and bog moss in patches, peat on the pool floors
    "peat_bog": {"top": pick((0.18, S("peat")), (0.05, M + "moss_block")), "wet": S("peat"), "under": S("peat")},
    # slate on the slopes and in outcrops, gneiss beneath it in patches
    "slate_highlands": {"top": pick((0.3, S("slate"))), "stone": pick((0.0, S("gneiss")), default=S("slate")), "slope": S("slate")},
    # soapstone veins in the daylight
    "soapstone_valley": {"top": pick((0.35, S("soapstone"))), "stone": S("soapstone"), "slope": S("soapstone")},
    # limestone pavement (alvar) with thin grass straight on the rock, travertine on the floors of pools and streams
    "limestone_karst": {"top": pick((0.05, S("limestone"))), "wet": S("travertine"), "under": S("limestone"), "stone": S("limestone"),
                        "slope": S("limestone")},
    # white marble gorges, limestone in their walls
    "marble_gorge": {"top": pick((0.25, S("marble")), (0.0, M + "coarse_dirt")), "stone": pick((0.2, S("limestone")), default=S("marble")),
                     "slope": S("marble")},
    # red sand on the flats, ochre / gypsum / mudstone layers in every cliff
    "ochre_badlands": {"top": M + "red_sand", "wet": S("mudstone"), "under": OCHRE_BANDS, "stone": OCHRE_BANDS, "slope": OCHRE_BANDS,
                       "all": {"type": "minecraft:condition", "if_true": y_above(58), "then_run": OCHRE_BANDS}},
    # black basalt with beach pebbles between
    "basalt_coast": {"top": pick((0.0, S("beach_pebbles")), default=S("coastal_basalt")), "wet": S("beach_pebbles"), "under": S("coastal_basalt"),
                     "stone": S("coastal_basalt"), "slope": S("coastal_basalt")},
    # pebbles over gravel
    "shingle_beach": {"top": S("beach_pebbles"), "wet": M + "gravel", "under": M + "gravel", "slope": S("beach_pebbles")},
    # mud banks, clay and mud under the water, loess and mudstone below
    "reed_delta": {"top": pick((0.25, M + "mud")), "wet": pick((0.0, M + "clay"), default=M + "mud"), "under": S("loess"),
                   "stone": pick((0.1, S("mudstone")), default=S("loess"))},
    # gravel and coarse till, conglomerate underneath
    "glacial_moraine": {"top": pick((0.25, M + "gravel"), (0.1, M + "coarse_dirt")), "under": M + "gravel", "stone": S("conglomerate"),
                        "slope": M + "gravel"},
    # white gypsum crust with salt pans and sand, alabaster in the gypsum
    "gypsum_flats": {"top": pick((0.32, S("rock_salt")), (-0.45, S("gypsum")), default=M + "sand"), "wet": S("gypsum"), "under": S("gypsum"),
                     "stone": pick((0.35, S("alabaster")), default=S("gypsum")), "slope": S("gypsum")},
    # thin dry turf with bare coarse patches over thick wind-blown loess (the steppe's soil), loess in every bank
    "mammoth_steppe": {"top": pick((0.3, M + "coarse_dirt"), default=M + "grass_block"), "under": S("loess"), "stone": S("loess"), "slope": S("loess")},
})

# ---------------------------------------------------------------- Elkwight phase 1: Scorched Barrens (docs/elkwight-plan.md)
# A burnt forest beyond 2500 blocks (ModBiomes.SURFACE row, MultiNoiseBiomeSourceMixin): charred trunks, Wood Ash, coarse
# dirt and gravel, Blackstone and Basalt outcrops, no grass, no animals but ravens. Cold and dark; snow in winter (Seasons).
BIOMES["scorched_barrens"] = ("windswept_gravelly_hills", "Scorched Barrens",
                              [M + f for f in ("trees_windswept_hills", "patch_bush", "flower_default", "patch_grass_badlands", "brown_mushroom_normal",
                                               "red_mushroom_normal", "patch_pumpkin", "patch_sugar_cane", "patch_firefly_bush_near_water")],
                              {"temperature": 0.3, "downfall": 0.4,
                               "effects": {"grass_color": "#5e584c", "foliage_color": "#4c463c", "dry_foliage_color": "#4c463c", "water_color": "#3a4048"}})
MATERIALS["scorched_barrens"] = {"top": pick((0.45, M + "gravel"), (0.3, M + "coarse_dirt"), default=S("wood_ash")), "wet": M + "gravel",
                                 "under": M + "coarse_dirt", "slope": pick((0.0, M + "basalt"), default=M + "blackstone")}

# ---- features (each placed feature belongs to one biome, so FeatureSorter never sees two orders)
LAKES, SURFACE_STRUCTURES, ORES, VEG = 1, 4, 6, 9
tri = lambda m: {"type": "minecraft:trapezoid", "min": -m, "max": m, "plateau": 0}
GROUND = {"type": "minecraft:matching_block_tag", "tag": M + "substrate_overworld", "offset": [0, -1, 0]}


def feature(name, config, placement):
    w(D / f"mechanical/worldgen/feature/{name}.json", config)
    return place(name, S(name), placement)


def place(name, configured, placement):
    w(D / f"mechanical/worldgen/placed_feature/{name}.json", {"feature": configured, "placement": placement})
    return S(name)


def scatter(name, configured, per_chunk, tries, spread, rarity=1, on=GROUND, survives=None, heightmap="MOTION_BLOCKING_NO_LEAVES"):
    """Clumps of an existing configured feature (or {"state": ...} for a plain block): per_chunk centres, tries spots each."""
    if isinstance(configured, dict):
        w(D / f"mechanical/worldgen/feature/{name}.json", {"type": "minecraft:simple_block", "to_place": configured["state"]})
        configured = S(name)
    check = [{"type": "minecraft:replaceable"}, on] + ([{"type": "minecraft:would_survive", "state": survives}] if survives else [])
    return place(name, configured, [
        {"type": "minecraft:rarity_filter", "chance": rarity}, {"type": "minecraft:count", "count": per_chunk}, {"type": "minecraft:in_square"},
        {"type": "minecraft:count", "count": tries}, {"type": "minecraft:offset", "x": tri(spread), "y": 0, "z": tri(spread)},
        {"type": "minecraft:heightmap", "heightmap": heightmap}, {"type": "minecraft:biome"},
        {"type": "minecraft:block_predicate_filter", "predicate": {"type": "minecraft:all_of", "predicates": check}}])


def once(name, configured, rarity, count=1, heightmap="WORLD_SURFACE_WG"):
    """count per chunk in 1 of rarity chunks, on the surface."""
    return place(name, configured, [{"type": "minecraft:rarity_filter", "chance": rarity}, {"type": "minecraft:count", "count": count},
                                    {"type": "minecraft:in_square"}, {"type": "minecraft:heightmap", "heightmap": heightmap}, {"type": "minecraft:biome"}])


def pools(name, fluid, barrier, rarity):
    """vanilla's lake feature: a pool 4 deep with walls of `barrier` (fluid air: a dry sinkhole)."""
    lake = json.loads(jar.read("data/minecraft/worldgen/feature/lake_lava.json"))
    lake.update({"fluid": {"id": fluid}, "barrier": {"id": barrier}})
    w(D / f"mechanical/worldgen/feature/{name}.json", lake)
    return once(name, S(name), rarity)


def outcrops(name, state, count, rarity=1):
    """vanilla forest_rock (block_blob): small rocky lumps."""
    return feature(name, {"type": "minecraft:block_blob", "can_place_on": {"type": "minecraft:matching_block_tag", "tag": M + "forest_rock_can_place_on"},
                          "state": state}, [{"type": "minecraft:rarity_filter", "chance": rarity}, {"type": "minecraft:count", "count": count},
                                            {"type": "minecraft:in_square"}, {"type": "minecraft:heightmap", "heightmap": "MOTION_BLOCKING"},
                                            {"type": "minecraft:biome"}])


def trees(name, choices, default, counts, water_depth=0):
    """A sparse tree mix: counts = [(trees per chunk, weight), ...]."""
    return feature(name, {"type": "minecraft:random_selector", "default": default, "features": [{"chance": c, "feature": f} for f, c in choices]},
                   [{"type": "minecraft:count", "count": {"type": "minecraft:weighted_list", "distribution": [{"data": n, "weight": wt} for n, wt in counts]}},
                    {"type": "minecraft:in_square"}, {"type": "minecraft:surface_water_depth_filter", "max_water_depth": water_depth},
                    {"type": "minecraft:heightmap", "heightmap": "OCEAN_FLOOR"}, {"type": "minecraft:biome"}])


def weighted(*entries):
    return {"type": "minecraft:weighted", "entries": [{"data": d, "weight": wt} for d, wt in entries]}


spawn = lambda entity, weight, lo, hi: {"type": entity, "weight": weight, "count": {"type": "minecraft:uniform", "min_inclusive": lo, "max_inclusive": hi}}
music = lambda sound: {"minecraft:audio/background_music": {"default": {"max_delay": 24000, "min_delay": 12000, "sound": sound}}}
w(D / "mechanical/worldgen/feature/glacial_boulder.json", {"type": "mechanical:glacial_boulder"})  # worldgen/Boulder.java, GLACIAL
REED = json.loads(jar.read("data/minecraft/worldgen/feature/sugar_cane.json").decode().replace(M + "sugar_cane", S("reed")))
w(D / "mechanical/worldgen/feature/reed_delta_reed.json", REED)
FLINT = weighted(*[({"id": S("loose_flint"), "properties": {"size": str(n)}}, wt) for n, wt in ((1, 6), (2, 3), (3, 1))])
FIELDSTONE = {"state": {"id": S("fieldstone")}}

# per biome: features {step: [placed features, put first in that step]}, creatures, attributes (merged), join / skip
# (mod biome modifier names without "add_"; skip removes ones inherited from the template), has (mechanical:has_<x> tags)
EXTRA = {
    "chalk_downs": dict(
        features={VEG: [scatter("chalk_downs_flint", {"state": FLINT}, 2, 10, 5, survives=S("loose_flint"))]},
        creatures=[spawn(S("hare"), 8, 1, 2)],
        attributes={"minecraft:visual/sky_color": "#82acff"},
        join=["loose_chalk"],
        skip=["aurochs", "saiga_2", "steppe_bison", "onager_3", "wild_horse", "ash_plains", "linden_plains", "hazel_plains", "willow_lakeside"]),
    "peat_bog": dict(
        features={LAKES: [pools("peat_bog_pools", M + "water", S("peat"), 3)],
                  VEG: [trees("peat_bog_pines", [], S("pine_checked"), [(0, 3), (1, 2), (2, 1)], water_depth=1),
                        scatter("peat_bog_tussocks", {"state": weighted((M + "short_grass", 4), (M + "fern", 3), (M + "tall_grass", 1))}, 3, 12, 5)]},
        attributes={**music(M + "music.overworld.swamp"), "minecraft:visual/sky_color": "#8aa2c8", "minecraft:visual/fog_color": "#c2c8bc",
                    "minecraft:visual/fog_end_distance": {"argument": 0.75, "modifier": "multiply"}, "minecraft:visual/water_fog_color": "#2a2416"},
        join=["cloudberry_bush", "common_crane", "moose", "goose"],
        has=["peat", "reed", "bog_iron"]),
    "slate_highlands": dict(
        features={VEG: [scatter("slate_highlands_heather", {"state": {"id": S("heather")}}, 4, 32, 6),
                        outcrops("slate_highlands_cairnstones", S("slate"), 1, 3)]},
        attributes={"minecraft:visual/sky_color": "#8a9cb8", "minecraft:visual/fog_color": "#b8c0cc"},
        join=["golden_eagle", "mouflon"],
        has=["slate", "gneiss"]),
    "soapstone_valley": dict(
        features={VEG: [outcrops("soapstone_valley_outcrops", S("soapstone"), 1, 2)]},
        has=["soapstone"]),
    "limestone_karst": dict(
        features={LAKES: [pools("limestone_karst_sinkholes", M + "air", S("limestone"), 6)],
                  VEG: [scatter("limestone_karst_flowers", {"state": weighted((M + "allium", 2), (M + "azure_bluet", 3), (M + "oxeye_daisy", 3),
                                                                                (M + "blue_orchid", 1), (M + "cornflower", 2))}, 3, 12, 5)]},
        has=["travertine", "dolomite"]),
    "marble_gorge": dict(
        features={VEG: [outcrops("marble_gorge_outcrops", S("marble"), 1, 2)]},
        attributes={"minecraft:visual/sky_color": "#7ab8ff"},
        has=["marble"]),
    "ochre_badlands": dict(
        attributes={"minecraft:visual/sky_color": "#8ab0e8", "minecraft:visual/fog_color": "#e0b890"},
        join=["vulture"],
        has=["ochre", "gypsum", "mudstone"]),
    "basalt_coast": dict(
        features={SURFACE_STRUCTURES: [place("basalt_coast_columns", S("coastal_basalt_columns"), [
            {"type": "minecraft:count", "count": 6}, {"type": "minecraft:in_square"}, {"type": "minecraft:heightmap", "heightmap": "OCEAN_FLOOR_WG"},
            {"type": "minecraft:biome"}])]},
        attributes={"minecraft:visual/sky_color": "#7f9cc8"},
        has=["coastal_basalt"]),
    "shingle_beach": dict(
        features={SURFACE_STRUCTURES: [once("shingle_beach_boulders", S("glacial_boulder"), 5)],
                  VEG: [scatter("shingle_beach_fieldstone", FIELDSTONE, 3, 10, 6, on={"type": "minecraft:matching_blocks", "offset": [0, -1, 0],
                                                                                     "blocks": [S("beach_pebbles"), M + "gravel", M + "sand"]},
                              heightmap="OCEAN_FLOOR")]},  # on the shingle and out in the shallows
        join=["seal", "seagull"]),
    "reed_delta": dict(
        features={VEG: [trees("reed_delta_willows", [], S("willow_checked"), [(0, 3), (1, 1)], water_depth=1),
                        place("reed_delta_reeds", S("reed_delta_reed"), [
                            {"type": "minecraft:count", "count": 8}, {"type": "minecraft:in_square"}, {"type": "minecraft:count", "count": 32},
                            {"type": "minecraft:offset", "x": tri(6), "y": 0, "z": tri(6)}, {"type": "minecraft:heightmap", "heightmap": "MOTION_BLOCKING"},
                            {"type": "minecraft:biome"},
                            {"type": "minecraft:block_predicate_filter", "predicate": {"type": "minecraft:all_of", "predicates": [
                                {"type": "minecraft:matching_block_tag", "tag": M + "air"}, {"type": "minecraft:would_survive", "state": S("reed")}]}}])]},
        join=["mallard", "mute_swan", "common_crane", "goose"],
        has=["reed", "loess", "mudstone"]),
    "glacial_moraine": dict(
        features={SURFACE_STRUCTURES: [once("glacial_moraine_boulders", S("glacial_boulder"), 2)],
                  VEG: [trees("glacial_moraine_spruces", [], M + "spruce_checked", [(0, 2), (1, 2), (2, 1)]),
                        scatter("glacial_moraine_fieldstone", FIELDSTONE, 3, 10, 6,
                                on={"type": "minecraft:matching_block_tag", "tag": M + "forest_rock_can_place_on", "offset": [0, -1, 0]})]},
        attributes={"minecraft:visual/sky_color": "#86a4d8"},
        join=["reindeer"],
        skip=["pine_taiga"],  # open moraine, not a forest
        has=["conglomerate", "gneiss", "rapakivi_granite"]),
    "gypsum_flats": dict(
        attributes={"minecraft:visual/sky_color": "#9cc8ff", "minecraft:visual/fog_color": "#f0ecdc"},
        has=["gypsum", "alabaster", "rock_salt"]),
    # the ice-age grassland: the snowy plains' giants (mammoth, rhino, bison, musk ox, saiga) plus wild horses, cave lions, wolves
    "mammoth_steppe": dict(
        features={SURFACE_STRUCTURES: [once("mammoth_steppe_boulders", S("glacial_boulder"), 8)],
                  VEG: [scatter("mammoth_steppe_grass", {"state": weighted((M + "short_grass", 6), (M + "tall_grass", 2), (M + "fern", 1))}, 6, 24, 6)]},
        attributes={"minecraft:visual/sky_color": "#9ab4e0", "minecraft:visual/fog_color": "#d8dccc"},
        no_creatures=[M + "polar_bear"],  # no sea ice to hunt from
        join=["wild_horse"],
        skip=["blackberry_bush", "cloudberry_bush"],  # too dry for the berries of the wet snowy plains
        has=["loess", "cave_lions", "grey_wolves"],
        skip_tags=["has_structure/igloo", "has_structure/village_snowy", "spawns_snow_foxes", "spawns_white_rabbits"]),  # no snow cover
    # inherit=False: none of the template's tags (but is_overworld), mod biome modifiers or creatures; only ravens join
    "scorched_barrens": dict(
        features={VEG: [feature("scorched_barrens_charred_trees", {"type": "mechanical:charred_tree"}, [
                            {"type": "minecraft:count", "count": {"type": "minecraft:uniform", "min_inclusive": 5, "max_inclusive": 9}},
                            {"type": "minecraft:in_square"}, {"type": "minecraft:heightmap", "heightmap": "WORLD_SURFACE_WG"}, {"type": "minecraft:biome"}]),
                        outcrops("scorched_barrens_blackstone", M + "blackstone", 1, 2),
                        outcrops("scorched_barrens_basalt", M + "basalt", 1, 3)]},
        attributes={"minecraft:visual/sky_color": "#5f6670", "minecraft:visual/fog_color": "#55585a", "minecraft:visual/water_fog_color": "#1e2226",
                    "minecraft:visual/fog_end_distance": {"argument": 0.8, "modifier": "multiply"},
                    "minecraft:visual/ambient_particles": {"argument": [{"particle": {"type": M + "white_ash"}, "probability": 0.004}], "modifier": "append"}},
        join=["raven"],
        inherit=False),
}

# Beach Pebbles on vanilla's stony shores too: patches swapped into the top layer of stone and gravel
SHORE = [M + "stone", M + "gravel", M + "sand"]
w(D / "mechanical/worldgen/feature/beach_pebbles_shore.json", {"type": "minecraft:disk", "half_height": 1,
  "radius": {"type": "minecraft:uniform", "min_inclusive": 2, "max_inclusive": 4},
  "state_provider": {"type": "minecraft:rule_based", "fallback": {"id": S("beach_pebbles")}, "rules": []},
  "target": {"type": "minecraft:matching_blocks", "blocks": SHORE}})
place("beach_pebbles_shore", S("beach_pebbles_shore"), [{"type": "minecraft:count", "count": 3}, {"type": "minecraft:in_square"},
      {"type": "minecraft:heightmap", "heightmap": "OCEAN_FLOOR_WG"}, {"type": "minecraft:offset", "x": 0, "y": -1, "z": 0},
      {"type": "minecraft:block_predicate_filter", "predicate": {"type": "minecraft:matching_blocks", "blocks": SHORE}}, {"type": "minecraft:biome"}])
w(D / "mechanical/neoforge/biome_modifier/add_beach_pebbles_shore.json", {"type": "neoforge:add_features", "biomes": [M + "stony_shore"],
  "features": S("beach_pebbles_shore"), "step": "underground_ores"})

block = lambda state: state if isinstance(state, dict) else {"type": "minecraft:block", "result_state": state}
when = lambda cond, then: {"type": "minecraft:condition", "if_true": cond, "then_run": then}
seq = lambda *rules: {"type": "minecraft:sequence", "sequence": [r for r in rules if r]}
depth = lambda kind, offset=0, add=False, extra=0: {"type": "minecraft:stone_depth", "surface_type": kind, "offset": offset,
                                                    "add_surface_depth": add, "secondary_depth_range": extra}


def surface_rule(m):
    steep = m.get("slope") and when({"type": "minecraft:steep"}, block(m["slope"]))
    return seq(
        m.get("top") and when(M + "on_floor", seq(steep, when(M + "not_underwater", block(m["top"])))),
        m.get("wet") and when(M + "on_floor", block(m["wet"])),
        m.get("under") and when(depth("floor", offset=1), seq(steep, block(m["under"]))),
        m.get("stone") and when(M + "deep_under_floor", block(m["stone"])),
        m.get("all") and block(m["all"]))


def cave_rule(m):
    return seq(m.get("floor") and when(M + "on_floor", block(m["floor"])),
               m.get("ceiling") and when(M + "on_ceiling", block(m["ceiling"])),
               m.get("wall") and block(m["wall"]))


biome_is = lambda name: {"type": "minecraft:biome", "biome_is": f"mechanical:{name}"}
surface = [when(biome_is(n), surface_rule(m)) for n, m in MATERIALS.items() if {"top", "wet", "under", "stone", "slope", "all"} & m.keys()]
caves = [when(biome_is(n), cave_rule(m)) for n, m in MATERIALS.items() if {"floor", "ceiling", "wall"} & m.keys()]
RULES = D / "mechanical/worldgen/material_rule"
w(RULES / "overworld.json", seq(when({"type": "minecraft:above_preliminary_surface"}, seq(*surface)), seq(*caves)))

root = json.loads(jar.read("data/minecraft/worldgen/material_rule/overworld.json"))
s = root["sequence"]
assert "mechanical:overworld" not in s and M + "overworld/iron_ore_vein" in s, s
s.insert(s.index(M + "overworld/iron_ore_vein") + 1, "mechanical:overworld")
w(D / "minecraft/worldgen/material_rule/overworld.json", root)

# ---------------------------------------------------------------- biome JSONs, tags, modifiers, lang
vanilla_tags = {n: json.loads(jar.read(n))["values"] for n in jar.namelist()
                if n.startswith("data/minecraft/tags/worldgen/biome/") and n.endswith(".json")}
lang = read(A / "lang/en_us.json", {})
for name, (base, title, drop, fields) in BIOMES.items():
    key, template = f"mechanical:{name}", M + base
    b = json.loads(jar.read(f"data/minecraft/worldgen/biome/{base}.json"))
    fields = dict(fields)
    b["attributes"] = {**b.get("attributes", {}), **fields.pop("attributes", {})}  # keeps the template's spawns and music
    add = fields.pop("add_features", {})
    b.update(fields)
    b["features"] = [[f for f in step if f not in drop] + add.get(i, []) for i, step in enumerate(b["features"])]
    x = EXTRA.get(name, {})
    for step, ids in x.get("features", {}).items():
        b["features"][step] = ids + b["features"][step]
    b["attributes"].update(x.get("attributes", {}))
    if not x.get("inherit", True):
        b["attributes"]["minecraft:gameplay/natural_mob_spawns"]["argument"]["spawns_by_category"]["creature"] = []
    spawns = b["attributes"]["minecraft:gameplay/natural_mob_spawns"]["argument"]["spawns_by_category"]
    if x.get("creatures"):
        spawns.setdefault("creature", []).extend(x["creatures"])
    if x.get("no_creatures"):
        spawns["creature"] = [c for c in spawns.get("creature", []) if c["type"] not in x["no_creatures"]]
    w(D / f"mechanical/worldgen/biome/{name}.json", b)
    lang[f"biome.mechanical.{name}"] = title
    # tags: vanilla's (written as additions in our data/minecraft) and the mod's own
    inherit = x.get("inherit", True)
    tag_files = {D / n.removeprefix("data/") for n, v in vanilla_tags.items() if template in v and (inherit or n.endswith("/is_overworld.json"))}
    if inherit:
        tag_files |= {p for p in D.glob("*/tags/worldgen/biome/**/*.json") if template in read(p, {}).get("values", [])}
    tag_files |= {D / f"mechanical/tags/worldgen/biome/has_{t}.json" for t in x.get("has", [])}
    tag_files -= {D / f"minecraft/tags/worldgen/biome/{t}.json" for t in x.get("skip_tags", [])}  # vanilla tags of the template that don't fit
    for p in tag_files:
        t = read(p, {"values": []})
        if key not in t["values"]:
            t["values"].append(key)
        w(p, t)
    join, skip = set(x.get("join", [])), set(x.get("skip", []))
    for p in sorted((D / "mechanical/neoforge/biome_modifier").glob("*.json")):
        m, stem = read(p, {}), p.stem.removeprefix("add_")
        if not isinstance(m.get("biomes"), list):
            assert stem not in join, f"{p.name}: biomes is a tag, join it through the tag"
            continue
        want = (template in m["biomes"] and inherit or stem in join) and stem not in skip
        if want and key not in m["biomes"]:
            m["biomes"].append(key)
            w(p, m)
        elif not want and key in m["biomes"] and (stem in skip or not inherit):  # inherited from the template but doesn't fit
            m["biomes"].remove(key)
            w(p, m)
    missing = (join | skip) - {p.stem.removeprefix("add_") for p in (D / "mechanical/neoforge/biome_modifier").glob("*.json")}
    assert not missing, f"{name}: no such biome modifiers {missing}"
w(A / "lang/en_us.json", lang, ascii=True)  # the lang file keeps its escapes
print("world biomes ok")
