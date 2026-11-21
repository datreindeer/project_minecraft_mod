"""Cave biomes, phases 5 and 8 of docs/world-materials-plan.md (player text: docs/caves.md).

gen_world_biomes.py makes the biomes themselves (BIOMES and MATERIALS rows; the climate slots are ModBiomes.CAVES) and
lists the placed features below in each biome's "add_features". This script writes those placed features (configured
feature inline, or a reference to an existing one), the Glowworm block (EarthBlocks.GLOWWORM: texture, model, empty loot,
lang), the tool cache loot table, and puts the cave biomes into the mod's biome tags (has_<material>, cave animals) and
the Pike spawn list. Feature names start with the biome's name, so no two biomes share one (FeatureSorter).

Generator order: right after gen_world_biomes.py. Idempotent. Run: python scripts/gen_caves.py"""
import json
import random
from pathlib import Path

from PIL import Image

BASE = Path(__file__).resolve().parent.parent
ROOT = BASE / "src/main/resources"
A, D = ROOT / "assets/mechanical", ROOT / "data/mechanical"
M, MOD = "minecraft:", "mechanical:"


def w(path, obj, ascii=False):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, indent=2, ensure_ascii=ascii) + "\n", encoding="utf-8")


def read(path, default):
    return json.loads(path.read_text(encoding="utf-8")) if path.exists() else default


# ---------------------------------------------------------------- placement helpers
FLOOR_TAG = "#mechanical:cave_floor_replaceable"
uniform = lambda lo, hi: {"type": M + "height_range", "height": {"type": M + "uniform", "min_inclusive": {"absolute": lo}, "max_inclusive": {"absolute": hi}}}
air = {"type": M + "matching_block_tag", "tag": M + "air"}


# shortcut: most tries start in rock or outside the biome and are dropped, so the scans try SCAN times the count asked
# for (vanilla's cave vines need 188 a chunk over the whole height); a cave-aware placement would waste less.
SCAN = 12


def on_floor(count, lo=-60, hi=80):
    return [{"type": M + "count", "count": count * SCAN}, {"type": M + "in_square"}, uniform(lo, hi),
            {"type": M + "environment_scan", "direction_of_search": "down", "max_steps": 12, "target_condition": {"type": M + "solid"},
             "allowed_search_condition": air},
            {"type": M + "offset", "x": 0, "y": 1, "z": 0}, {"type": M + "biome"}]


def on_ceiling(count, lo=-60, hi=80):
    return [{"type": M + "count", "count": count * SCAN}, {"type": M + "in_square"}, uniform(lo, hi),
            {"type": M + "environment_scan", "direction_of_search": "up", "max_steps": 12,
             "target_condition": {"type": M + "has_sturdy_face", "direction": "down"}, "allowed_search_condition": air},
            {"type": M + "offset", "x": 0, "y": -1, "z": 0}, {"type": M + "biome"}]


def in_rock(count, lo=-64, hi=80):
    return [{"type": M + "count", "count": count}, {"type": M + "in_square"}, uniform(lo, hi), {"type": M + "biome"}]


def rare(chance, lo=-60, hi=80):
    return [{"type": M + "rarity_filter", "chance": chance}, {"type": M + "in_square"}, uniform(lo, hi), {"type": M + "biome"}]


# ---------------------------------------------------------------- configured features (inline)
state = lambda block, **props: {"id": block, **({"properties": props} if props else {})}


def ore(block, size):
    """A blob in the biome's rock (all our wall stones are in base_stone_overworld)."""
    return {"type": M + "ore", "size": size, "discard_chance_on_air_exposure": 0.0,
            "targets": [{"state": state(block), "target": {"predicate_type": M + "tag_match", "tag": M + "base_stone_overworld"}}]}


def column(block, lo, hi, direction):
    """Stalactites (down from a ceiling) and stalagmites (up from a floor), only through air."""
    return {"type": M + "block_column", "allowed_placement": air, "direction": direction, "prioritize_tip": False,
            "layers": [{"height": {"type": M + "uniform", "min_inclusive": lo, "max_inclusive": hi}, "provider": state(block)}]}


def single(block, down=False, **props):
    """One block (a state, or a provider dict) as a one-high block_column. shortcut: simple_block placed nothing as patch
    vegetation in real worldgen (its canSurvive check; one-off /place worked), so this skips the check; only patch
    surfaces call it, which are sturdy anyway. Upgrade if a plant ever needs the check. down=True under a ceiling: a
    column checks the block past its start, so an upward one-high column under a ceiling places nothing."""
    provider = block if isinstance(block, dict) else state(block, **props)
    return {"type": M + "block_column", "allowed_placement": air, "direction": "down" if down else "up", "prioritize_tip": False,
            "layers": [{"height": 1, "provider": provider}]}


def provider_of(feature):
    """The block provider of one of the mod's simple_block features (loose ochre, gem crystals)."""
    return json.loads((D / f"worldgen/feature/{feature}.json").read_text(encoding="utf-8"))["to_place"]


MOSS_PLANTS = {"type": M + "weighted", "entries": [{"data": M + "moss_carpet", "weight": 25}, {"data": M + "short_grass", "weight": 50},
                                                   {"data": M + "azalea", "weight": 7}]}


NO_OP = {"feature": {"type": M + "no_op"}, "placement": []}


def pool(ground, lo=3, hi=6):
    """A shallow water pool rimmed with `ground` in the cave floor (vanilla's clay pools of the Lush Caves, no dripleaves)."""
    return {"type": M + "waterlogged_vegetation_patch", "depth": 2, "extra_bottom_block_chance": 0.6, "extra_edge_column_chance": 0.7,
            "ground_state": state(ground), "replaceable": FLOOR_TAG, "surface": "floor", "vegetation_chance": 0.0,
            "vegetation_feature": NO_OP, "vertical_range": 5, "xz_radius": {"type": M + "uniform", "min_inclusive": lo, "max_inclusive": hi}}


def patch(ground, lo, hi, surface="floor"):
    """A flat patch of `ground` laid into the floor (ice sheets, magma vents)."""
    return {"type": M + "vegetation_patch", "depth": 1, "extra_bottom_block_chance": 0.0, "extra_edge_column_chance": 0.3,
            "ground_state": state(ground), "replaceable": FLOOR_TAG, "surface": surface, "vegetation_chance": 0.0,
            "vegetation_feature": NO_OP, "vertical_range": 4, "xz_radius": {"type": M + "uniform", "min_inclusive": lo, "max_inclusive": hi}}


def decor(surface, ground, plant, chance, lo=2, hi=4):
    """A patch of the cave's own floor or ceiling block with `plant` (a configured feature or id) on some of it: the
    way vanilla dresses the Lush Caves, one successful try covers a whole patch instead of one block."""
    return {"type": M + "vegetation_patch", "depth": 1, "extra_bottom_block_chance": 0.0, "extra_edge_column_chance": 0.0,
            "ground_state": state(ground), "replaceable": FLOOR_TAG, "surface": surface, "vegetation_chance": chance,
            "vegetation_feature": {"feature": plant, "placement": []}, "vertical_range": 4,
            "xz_radius": {"type": M + "uniform", "min_inclusive": lo, "max_inclusive": hi}}


def lake(fluid, barrier):
    return {"type": M + "lake", "barrier": state(barrier), "can_place_feature": {"type": M + "true"},
            "can_replace_with_air_or_fluid": {"type": M + "not", "predicate": {"type": M + "matching_block_tag", "tag": M + "features_cannot_replace"}},
            "can_replace_with_barrier": {"type": M + "not", "predicate": {"type": M + "matching_block_tag", "tag": M + "lava_pool_stone_cannot_replace"}},
            "fluid": state(fluid, level="0")}


def geode(gem):
    """A Crystal Geodes geode: quartzite shell, calcite, and the gem's crystals (gem system) growing inward."""
    return {"type": M + "geode", "blocks": {
        "alternate_inner_layer_provider": state(M + "calcite"), "cannot_replace": "#minecraft:features_cannot_replace",
        "filling_provider": state(M + "air"), "inner_layer_provider": state(M + "calcite"),
        "inner_placements": [f"{MOD}{s}_{gem}_crystal" for s in ("small", "medium", "large")],
        "invalid_blocks": "#minecraft:geode_invalid_blocks", "middle_layer_provider": state(MOD + "polished_quartzite"),
        "outer_layer_provider": state(MOD + "quartzite")},
        "crack": {"generate_crack_chance": 0.95}, "invalid_blocks_threshold": 1, "layers": {},
        "outer_wall_distance": {"type": M + "uniform", "min_inclusive": 4, "max_inclusive": 6}, "use_alternate_layer0_chance": 0.0,
        "placements_require_layer0_alternate": False}  # crystals on any inner block (vanilla: only on budding amethyst)


LIMESTONE, LIMESTONE_WALL = MOD + "limestone", MOD + "limestone_wall"
LAVA_LAKE = [{"type": M + "rarity_filter", "chance": 3}, {"type": M + "in_square"}, uniform(-60, 0),
             {"type": M + "environment_scan", "direction_of_search": "down", "max_steps": 32, "target_condition": {"type": M + "all_of", "predicates": [
                 {"type": M + "not", "predicate": air}, {"type": M + "inside_world_bounds", "offset": [0, -5, 0]}]}},
             {"type": M + "surface_relative_threshold_filter", "heightmap": "OCEAN_FLOOR_WG", "max_inclusive": -5}, {"type": M + "biome"}]
GEMS = ["clear_quartz", "rose_quartz", "smoky_quartz", "citrine", "fluorite"]

# biome: {feature suffix: (generation step, configured feature (dict) or id, placement)}. Steps: 1 lakes,
# 2 local_modifications, 6 underground_ores, 9 vegetal_decoration.
FEATURES = {
    "limestone_caverns": {
        "travertine_pools": (9, pool(MOD + "travertine"), on_floor(40, -20, 60)),
        "stalactites": (9, decor("ceiling", LIMESTONE, column(LIMESTONE_WALL, 1, 4, "down"), 0.2), on_ceiling(20, -20, 60)),
        "stalagmites": (9, decor("floor", MOD + "travertine", column(LIMESTONE_WALL, 1, 3, "up"), 0.1), on_floor(15, -20, 60)),
        "travertine": (6, ore(MOD + "travertine", 32), in_rock(4, -20, 60)),
    },
    "salt_caves": {
        "rock_salt": (6, ore(MOD + "rock_salt", 32), in_rock(6, -20, 60)),
        "alabaster": (6, ore(MOD + "alabaster", 20), in_rock(2, -20, 60)),
        "salt_stalactites": (9, decor("ceiling", MOD + "rock_salt", column(MOD + "rock_salt", 1, 3, "down"), 0.2), on_ceiling(20, -20, 60)),
        "salt_stalagmites": (9, decor("floor", MOD + "rock_salt", column(MOD + "rock_salt", 1, 2, "up"), 0.12), on_floor(15, -20, 60)),
        "gypsum_needles": (9, decor("ceiling", MOD + "rock_salt", column(MOD + "gypsum_wall", 1, 3, "down"), 0.15), on_ceiling(10, -20, 60)),
    },
    "ochre_caves": {
        "red_ochre": (6, ore(MOD + "red_ochre_block", 20), in_rock(6, 0, 80)),
        "yellow_ochre": (6, ore(MOD + "yellow_ochre_block", 20), in_rock(6, 0, 80)),
        "loose_red_ochre": (9, decor("floor", MOD + "red_ochre_block", single(provider_of("loose_red_ochre")), 0.1), on_floor(8, 0, 80)),
        "loose_yellow_ochre": (9, decor("floor", MOD + "red_ochre_block", single(provider_of("loose_yellow_ochre")), 0.1), on_floor(8, 0, 80)),
        "paintings": (9, {"type": MOD + "cave_paintings"}, in_rock(8, 10, 70)),
    },
    "soapstone_grottos": {
        "slate": (6, ore(MOD + "slate", 32), in_rock(4, -30, 40)),
        "glow_lichen": (9, M + "glow_lichen", in_rock(40, -30, 40)),
        "tool_cache": (9, {"type": MOD + "tool_cache"}, rare(5, -20, 30)),
    },
    "glowworm_caves": {
        "glowworms": (9, decor("ceiling", M + "rooted_dirt", single(MOD + "glowworm", down=True), 0.5, 3, 6), on_ceiling(40, 0, 80)),
        "hanging_roots": (9, decor("ceiling", M + "rooted_dirt", single(M + "hanging_roots", down=True), 0.25), on_ceiling(15, 0, 80)),
        "clay_pools": (9, pool(M + "clay"), on_floor(25, 0, 80)),
        "moss_vegetation": (9, decor("floor", M + "moss_block", single(MOSS_PLANTS), 0.5, 3, 6), on_floor(25, 0, 80)),
    },
    "ice_caves": {
        "blue_ice": (6, ore(M + "blue_ice", 20), in_rock(6, 0, 80)),
        "icicles": (9, decor("ceiling", M + "packed_ice", column(M + "ice", 1, 3, "down"), 0.2), on_ceiling(20, 0, 80)),
        "frozen_pools": (9, patch(M + "ice", 2, 4), on_floor(25, 0, 80)),
        "snow": (9, patch(M + "snow_block", 1, 3), on_floor(15, 0, 80)),
    },
    "peat_hollows": {
        "clay": (6, ore(M + "clay", 24), in_rock(6, 0, 80)),
        "bog_oak_roots": (9, decor("ceiling", MOD + "peat", column(MOD + "bog_oak_log", 1, 3, "down"), 0.15), on_ceiling(15, 0, 80)),
        "hanging_roots": (9, decor("ceiling", MOD + "peat", single(M + "hanging_roots", down=True), 0.35), on_ceiling(20, 0, 80)),
        "puddles": (9, pool(MOD + "peat", 2, 4), on_floor(25, 0, 80)),
    },
    "bone_caves": {
        "bone_blocks": (6, ore(M + "bone_block", 6), in_rock(6, 0, 80)),
        "bone_piles": (9, decor("floor", M + "coarse_dirt", single(MOD + "bone_pile"), 0.25, 2, 5), on_floor(20, 0, 80)),
        "stalactites": (9, decor("ceiling", LIMESTONE, column(LIMESTONE_WALL, 1, 3, "down"), 0.12), on_ceiling(10, 0, 80)),
    },
    "galena_seams": {
        "alum_shale": (6, ore(MOD + "alum_shale", 40), in_rock(4, -64, 0)),
        "galena": (6, MOD + "ore_galena", in_rock(24, -64, 0)),
    },
    "magma_vents": {
        "lava_lakes": (1, M + "lake_lava", LAVA_LAKE),
        "scoria": (6, ore(MOD + "scoria", 32), in_rock(4, -64, -10)),
        "pumice": (6, ore(MOD + "pumice", 24), in_rock(3, -64, -10)),
        "obsidian": (6, ore(M + "obsidian", 8), in_rock(4, -64, -10)),
        "magma": (9, patch(M + "magma_block", 1, 3), on_floor(30, -64, -10)),
        "basalt_columns": (9, decor("floor", MOD + "scoria", column(MOD + "coastal_basalt", 2, 5, "up"), 0.08), on_floor(15, -64, -10)),
    },
    "crystal_geodes": {
        **{f"{g}_geode": (2, geode(g), rare(4, -60, -10)) for g in GEMS[:3]},
        "amethyst_geode": (2, M + "amethyst_geode", rare(6, -60, -10)),
        **{f"{g}_{side}": (9, decor(side, MOD + "quartzite", single(provider_of(f"gem_{g}_{side}"), down=side == "ceiling"), 0.12), (on_floor if side == "floor" else on_ceiling)(3, -60, -10))
           for g in GEMS for side in ("floor", "ceiling")},
    },
    "flooded_caves": {
        "lakes": (1, lake(M + "water", M + "clay"), in_rock(3, 0, 60)),
        "clay": (6, ore(M + "clay", 30), in_rock(6, 0, 60)),
        "pools": (9, pool(M + "clay", 3, 7), on_floor(60, 0, 60)),
    },
}


def biome_features(name):
    """{step: [placed feature ids]} for the biome's "add_features" row in gen_world_biomes.py."""
    steps = {}
    for suffix, (step, _, _) in FEATURES[name].items():
        steps.setdefault(step, []).append(f"{MOD}{name}_{suffix}")
    return steps


def main():
    for biome, feats in FEATURES.items():
        for suffix, (_, feature, placement) in feats.items():
            w(D / f"worldgen/placed_feature/{biome}_{suffix}.json", {"feature": feature, "placement": placement})

    w(D / "tags/block/cave_floor_replaceable.json", {"values": [
        "#minecraft:base_stone_overworld", "#minecraft:dirt", "#minecraft:ice", "minecraft:gravel", "minecraft:clay", "minecraft:sand",
        "mechanical:peat", "mechanical:rock_salt", "mechanical:red_ochre_block", "minecraft:snow_block"]})

    # tool cache: Stone Age tools someone hid in the Soapstone Grottos
    item = lambda name, weight, lo=1, hi=1: {"type": M + "item", "name": name, "weight": weight,
                                             **({"modifier": [{"type": M + "set_count", "count": {"type": M + "uniform", "min": lo, "max": hi}}]} if hi > 1 else {})}
    w(D / "loot_table/chests/tool_cache.json", {"type": M + "chest", "random_sequence": MOD + "chests/tool_cache", "pools": [
        {"rolls": {"type": M + "uniform", "min": 2, "max": 4}, "entries": [
            item(MOD + "flint_knife", 10), item(MOD + "flint_pickaxe", 6), item(MOD + "flint_axe", 6), item(MOD + "flint_shovel", 5),
            item(MOD + "flint_spear", 5), item(MOD + "flint_sickle", 4), item(MOD + "crude_stone_pickaxe", 4), item(MOD + "crude_stone_axe", 4),
            item(MOD + "bone_needle", 3)]},
        {"rolls": {"type": M + "uniform", "min": 1, "max": 3}, "entries": [
            item(M + "flint", 10, 2, 6), item(MOD + "stone_chunk", 6, 2, 5), item(MOD + "soapstone", 4, 1, 3), item(M + "stick", 6, 2, 6)]}]})

    # Glowworm: dark silk threads with blue-glowing beads on them, a cross like hanging roots
    rng = random.Random(11)
    img = Image.new("RGBA", (16, 16))
    for x in (2, 4, 7, 9, 12, 14):
        length = rng.randint(7, 15)
        for y in range(length):
            xx = x + (1 if rng.random() < 0.12 else 0)
            img.putpixel((min(15, xx), y), (52, 58, 66, 230))
            if rng.random() < 0.28 or y == length - 1:
                img.putpixel((min(15, xx), y), (120, 220, 255, 255) if rng.random() < 0.5 else (70, 160, 235, 255))
    (A / "textures/block").mkdir(parents=True, exist_ok=True)
    img.save(A / "textures/block/glowworm.png")
    w(A / "blockstates/glowworm.json", {"variants": {"waterlogged=false": {"model": "mechanical:block/glowworm"},
                                                      "waterlogged=true": {"model": "mechanical:block/glowworm"}}})
    w(A / "models/block/glowworm.json", {"parent": "minecraft:block/cross", "textures": {"cross": "mechanical:block/glowworm"}})
    w(D / "loot_table/blocks/glowworm.json", {"type": M + "block", "pools": [], "random_sequence": MOD + "blocks/glowworm"})
    lang = read(A / "lang/en_us.json", {})
    lang["block.mechanical.glowworm"] = "Glowworm"
    w(A / "lang/en_us.json", lang, ascii=True)

    # biome tags: the materials each cave is built from (their own ore features follow), and the cave animals
    tags = {"has_travertine": ["limestone_caverns"], "has_rock_salt": ["salt_caves"], "has_gypsum": ["salt_caves"],
            "has_alabaster": ["salt_caves"], "has_ochre": ["ochre_caves"], "has_chalk": ["ochre_caves"],
            "has_soapstone": ["soapstone_grottos"], "has_slate": ["soapstone_grottos", "ice_caves"], "has_peat": ["peat_hollows"],
            "has_shale": ["galena_seams"], "has_alum_shale": ["galena_seams"], "has_coastal_basalt": ["magma_vents"],
            "has_scoria": ["magma_vents"], "has_pumice": ["magma_vents"], "has_quartzite": ["crystal_geodes"],
            "has_cave_hyenas": ["bone_caves"], "has_cave_lions": ["bone_caves"]}
    for tag, biomes in tags.items():
        p = D / f"tags/worldgen/biome/{tag}.json"
        t = read(p, {"values": []})
        t["values"] += [MOD + b for b in biomes if MOD + b not in t["values"]]
        w(p, t)
    p = D / "neoforge/biome_modifier/add_pike.json"
    m = read(p, {})
    if MOD + "flooded_caves" not in m["biomes"]:
        m["biomes"].append(MOD + "flooded_caves")
    w(p, m)
    print("caves ok")


if __name__ == "__main__":
    main()
