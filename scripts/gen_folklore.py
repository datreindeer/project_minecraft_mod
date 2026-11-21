"""Folklore data, steps 0-2 of docs/folklore-plan.md (Java: com.mechanical.folklore, client.folklore):

- Boiled Fly Agaric: campfire_cooking from Fly Agaric (the poison boils out; 3 min Second Sight). Raw Fly Agaric's effects are Java.
- Glimmering Stump: block model (stump template, mossy textures), blockstate (random turn), loot (sticks, like a stump),
  axe tag, item tag mechanical:stump_offerings (berries and nuts it takes as a gift), the gift loot table
  gameplay/stump_gift, and the worldgen feature (rare, old and dark forests; the Ancient Oak clearing gets one in Java).
- Mountain Watcher: spawn egg, spawns on the peaks (the Java spawn rule keeps it to y 150+, clear weather, day).
- Watcher Feather, the Second Sight effect name, messages.

Generator order: right after the gen_fauna_* scripts (before gen_tribes2); gen_advancements and gen_registry come later.
Textures: scripts/gen_folklore_textures.py. Run: python scripts/gen_folklore.py
"""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
RES = ROOT / "src/main/resources"
A = RES / "assets/mechanical"
D = RES / "data"
M = "mechanical:"


def w(path, obj):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")


def patch_tag(path, values):
    tag = json.loads(path.read_text(encoding="utf-8")) if path.exists() else {"values": []}
    tag["values"] += [v for v in values if v not in tag["values"]]
    w(path, tag)


def flat_item(name):
    w(A / f"models/item/{name}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"{M}item/{name}"}})
    w(A / f"items/{name}.json", {"model": {"type": "minecraft:model", "model": f"{M}item/{name}"}})


def count(lo, hi):
    return [{"type": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": lo, "max": hi}}]


# ---------------------------------------------------------------- items
for name in ("boiled_fly_agaric", "watcher_feather", "mountain_watcher_spawn_egg"):
    flat_item(name)
w(D / "mechanical/recipe/boiled_fly_agaric.json", {"type": "minecraft:campfire_cooking", "category": "food", "cookingtime": 300,
    "experience": 0.1, "ingredient": M + "fly_agaric", "result": {"id": M + "boiled_fly_agaric"}})

# ---------------------------------------------------------------- Glimmering Stump
w(A / "models/block/glimmering_stump.json", {"parent": M + "block/stump", "textures": {
    "side": M + "block/glimmering_stump_side", "top": M + "block/glimmering_stump_top", "particle": M + "block/glimmering_stump_side"}})
w(A / "blockstates/glimmering_stump.json", {"variants": {"": [{"model": M + "block/glimmering_stump", "y": y} for y in (0, 90, 180, 270)]}})
w(A / "items/glimmering_stump.json", {"model": {"type": "minecraft:model", "model": M + "block/glimmering_stump"}})
w(D / "mechanical/loot_table/blocks/glimmering_stump.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [
    {"type": "minecraft:item", "name": "minecraft:stick", "modifier": count(1, 2)}]}], "random_sequence": M + "blocks/glimmering_stump"})
patch_tag(D / "minecraft/tags/block/mineable/axe.json", [M + "glimmering_stump"])
w(D / "mechanical/tags/item/stump_offerings.json", {"values": ["#mechanical:dryable_berries", M + "hazelnuts", M + "beechnuts"]})
# the morning-after present: a seed, a feather or a pretty stone
GIFTS = [("mechanical:flax_seeds", 4, 1, 2), ("mechanical:nettle_seeds", 3, 1, 2), ("minecraft:wheat_seeds", 3, 1, 3),
         ("minecraft:feather", 4, 1, 2), ("mechanical:owl_feather", 2, 1, 1), ("mechanical:rough_agate", 2, 1, 1),
         ("mechanical:rough_amber", 1, 1, 1), ("minecraft:amethyst_shard", 1, 1, 1)]
for item, *_ in GIFTS:
    ns, path = item.split(":")
    assert ns == "minecraft" or (A / f"items/{path}.json").exists(), f"unknown gift {item}"
w(D / "mechanical/loot_table/gameplay/stump_gift.json", {"type": "minecraft:gift", "pools": [{"rolls": 1, "entries": [
    {"type": "minecraft:item", "name": i, "weight": wt, **({"modifier": count(lo, hi)} if hi > 1 else {})} for i, wt, lo, hi in GIFTS]}],
    "random_sequence": M + "gameplay/stump_gift"})

STUMP_BIOMES = ["minecraft:dark_forest", "minecraft:old_growth_birch_forest", "minecraft:old_growth_pine_taiga",
                "minecraft:old_growth_spruce_taiga", "minecraft:pale_garden", M + "deciduous_forest"]
w(D / "mechanical/worldgen/feature/glimmering_stump.json", {"type": M + "glimmering_stump"})
w(D / "mechanical/worldgen/placed_feature/glimmering_stump.json", {"feature": M + "glimmering_stump", "placement": [
    {"type": "minecraft:rarity_filter", "chance": 40}, {"type": "minecraft:in_square"},
    {"type": "minecraft:heightmap", "heightmap": "WORLD_SURFACE_WG"}, {"type": "minecraft:biome"}]})
w(D / "mechanical/neoforge/biome_modifier/add_glimmering_stump.json", {"type": "neoforge:add_features", "biomes": STUMP_BIOMES,
    "features": M + "glimmering_stump", "step": "surface_structures"})

# ---------------------------------------------------------------- Mountain Watcher
w(D / "mechanical/neoforge/biome_modifier/add_mountain_watcher.json", {"type": "neoforge:add_spawns",
    "biomes": ["minecraft:stony_peaks", "minecraft:jagged_peaks", "minecraft:frozen_peaks", "minecraft:snowy_slopes", "minecraft:grove",
               "minecraft:meadow", "minecraft:windswept_hills", "minecraft:cherry_grove"],
    "spawners": [{"type": M + "mountain_watcher", "weight": 2, "count": {"type": "minecraft:uniform", "min_inclusive": 1, "max_inclusive": 1}}]})

# ---------------------------------------------------------------- lang (en_us only; the mod has no other language file)
LANG = A / "lang/en_us.json"
lang = json.loads(LANG.read_text(encoding="utf-8"))
lang.update({
    "item.mechanical.boiled_fly_agaric": "Boiled Fly Agaric",
    "item.mechanical.watcher_feather": "Watcher Feather",
    "block.mechanical.glimmering_stump": "Glimmering Stump",
    "entity.mechanical.mountain_watcher": "Mountain Watcher",
    "item.mechanical.mountain_watcher_spawn_egg": "Mountain Watcher Spawn Egg",
    "effect.mechanical.second_sight": "Second Sight",
    "message.mechanical.glimmering_stump.gift": "Something stirs among the roots...",
    "message.mechanical.glimmering_stump.already": "The small folk have had their gift today",
})
LANG.write_text(json.dumps(lang, indent=2) + "\n", encoding="utf-8")
print("folklore ok")
