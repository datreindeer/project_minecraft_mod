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

# ---------------------------------------------------------------- the folklore beings (folklore.Beings, docs/fauna/folklore.md)
BEINGS = ["huldra", "will_o_wisp", "jungle_spirit_mask", "tomte", "nacken", "bog_body", "frost_giant", "dune_wraith"]
for name in [b + "_spawn_egg" for b in BEINGS] + ["huldra_comb", "wisp_ember", "spirit_mask", "straw_yule_goat", "nacken_string",
                                                    "peat_tanned_hide", "bog_torc", "vittra_silver", "dune_glass"]:
    flat_item(name)
for block in ("frost_heart", "mossy_troll_stone"):
    w(A / f"models/block/{block}.json", {"parent": "minecraft:block/cube_all", "textures": {"all": f"{M}block/{block}"}})
    w(A / f"blockstates/{block}.json", {"variants": {"": {"model": f"{M}block/{block}"}}})
    w(A / f"items/{block}.json", {"model": {"type": "minecraft:model", "model": f"{M}block/{block}"}})
    w(D / f"mechanical/loot_table/blocks/{block}.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [
        {"type": "minecraft:item", "name": M + block}], "condition": {"type": "minecraft:survives_explosion"}}],
        "random_sequence": f"{M}blocks/{block}"})
patch_tag(D / "minecraft/tags/block/mineable/pickaxe.json", [M + "frost_heart", M + "mossy_troll_stone"])
w(D / "mechanical/tags/item/nacken_gifts.json", {"values": [M + "silver_coin", M + "gold_coin", "#minecraft:meat"]})
w(D / "mechanical/recipe/leather_from_peat_tanned_hide.json", {"type": "minecraft:crafting_shapeless", "category": "misc",
    "ingredients": [M + "peat_tanned_hide"], "result": {"id": "minecraft:leather", "count": 1}})


def entry(item, weight=1, lo=1, hi=1, chance=None):
    ns, path = item.split(":")
    assert ns == "minecraft" or (A / f"items/{path}.json").exists(), f"unknown item {item}"
    e = {"type": "minecraft:item", "name": item, "weight": weight}
    if hi > 1 or lo != 1:
        e["modifier"] = count(lo, hi)
    if chance is not None:
        e["condition"] = {"type": "minecraft:random_chance", "chance": chance}
    return e


def loot(path, kind, pools):
    w(D / f"mechanical/loot_table/{path}.json", {"type": kind, "pools": pools, "random_sequence": M + path})


def one(*entries, rolls=1):
    return {"rolls": rolls, "entries": list(entries)}


loot("entities/frost_giant", "minecraft:entity", [one(entry(M + "frost_heart")), one(entry("minecraft:snowball", 1, 4, 10)),
                                                 one(entry("minecraft:packed_ice", 1, 1, 3))])
loot("entities/bog_body", "minecraft:entity", [one(entry(M + "peat_tanned_hide", 1, 1, 2)), one(entry("minecraft:rotten_flesh", 1, 0, 2)),
                                              one(entry(M + "bog_torc", chance=0.1))])
loot("chests/bog_grave", "minecraft:chest", [one(entry(M + "wisp_ember")), one(entry("minecraft:bone", 4, 1, 3), entry(M + "copper_coin", 4, 2, 6),
    entry(M + "silver_coin", 2, 1, 3), entry(M + "rough_amber", 2, 1, 2), entry(M + "raw_bog_iron", 3, 1, 4), entry(M + "bog_torc", 1), rolls=3)])
loot("chests/bergtagen", "minecraft:chest", [one(entry(M + "vittra_silver", 1, 1, 3)), one(entry(M + "vittra_hide", 3, 1, 2),
    entry(M + "cooked_vittra_meat", 3, 1, 3), entry("minecraft:brown_mushroom", 2, 1, 3), entry(M + "silver_coin", 2, 1, 4), rolls=3)])


def spawns(name, biomes, weight, lo=1, hi=1):
    w(D / f"mechanical/neoforge/biome_modifier/add_{name}.json", {"type": "neoforge:add_spawns", "biomes": biomes,
        "spawners": [{"type": M + name, "weight": weight, "count": {"type": "minecraft:uniform", "min_inclusive": lo, "max_inclusive": hi}}]})


spawns("huldra", ["minecraft:forest", "minecraft:birch_forest", "minecraft:dark_forest", "minecraft:old_growth_birch_forest",
                  "minecraft:flower_forest", M + "deciduous_forest"], 4)
spawns("will_o_wisp", ["minecraft:swamp", "minecraft:dark_forest", M + "peat_bog", M + "willow_fen", M + "reed_delta"], 5)
spawns("jungle_spirit_mask", ["minecraft:jungle", "minecraft:bamboo_jungle", "minecraft:sparse_jungle"], 5)
spawns("nacken", ["minecraft:river"], 6)
spawns("bog_body", [M + "peat_bog"], 30, 1, 2)
spawns("frost_giant", ["minecraft:snowy_plains", "minecraft:ice_spikes", "minecraft:snowy_taiga", "minecraft:frozen_peaks",
                       "minecraft:snowy_slopes", M + "glacial_moraine"], 2)
spawns("dune_wraith", ["minecraft:desert"], 10)

# the sleeping forest troll: two Mossy Troll Stone in the taiga
w(D / "mechanical/worldgen/feature/troll_stone_sleeper.json", {"type": M + "troll_stone_sleeper"})
w(D / "mechanical/worldgen/placed_feature/troll_stone_sleeper.json", {"feature": M + "troll_stone_sleeper", "placement": [
    {"type": "minecraft:rarity_filter", "chance": 48}, {"type": "minecraft:in_square"},
    {"type": "minecraft:heightmap", "heightmap": "WORLD_SURFACE_WG"}, {"type": "minecraft:biome"}]})
w(D / "mechanical/neoforge/biome_modifier/add_troll_stone_sleeper.json", {"type": "neoforge:add_features",
    "biomes": ["minecraft:taiga", "minecraft:old_growth_pine_taiga", "minecraft:old_growth_spruce_taiga", "minecraft:snowy_taiga", M + "pine_heath"],
    "features": M + "troll_stone_sleeper", "step": "surface_structures"})

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
    "entity.mechanical.huldra": "Huldra",
    "entity.mechanical.will_o_wisp": "Will-o'-the-wisp",
    "entity.mechanical.jungle_spirit_mask": "Jungle Spirit Mask",
    "entity.mechanical.tomte": "Tomte",
    "entity.mechanical.nacken": "Näcken",
    "entity.mechanical.bog_body": "Bog Body",
    "entity.mechanical.frost_giant": "Frost Giant",
    "entity.mechanical.dune_wraith": "Dune Wraith",
    "item.mechanical.huldra_spawn_egg": "Huldra Spawn Egg",
    "item.mechanical.will_o_wisp_spawn_egg": "Will-o'-the-wisp Spawn Egg",
    "item.mechanical.jungle_spirit_mask_spawn_egg": "Jungle Spirit Mask Spawn Egg",
    "item.mechanical.tomte_spawn_egg": "Tomte Spawn Egg",
    "item.mechanical.nacken_spawn_egg": "Näcken Spawn Egg",
    "item.mechanical.bog_body_spawn_egg": "Bog Body Spawn Egg",
    "item.mechanical.frost_giant_spawn_egg": "Frost Giant Spawn Egg",
    "item.mechanical.dune_wraith_spawn_egg": "Dune Wraith Spawn Egg",
    "item.mechanical.huldra_comb": "Huldra Comb",
    "item.mechanical.wisp_ember": "Wisp Ember",
    "item.mechanical.spirit_mask": "Spirit Mask",
    "item.mechanical.straw_yule_goat": "Straw Yule Goat",
    "item.mechanical.nacken_string": "Näcken's String",
    "item.mechanical.peat_tanned_hide": "Peat-Tanned Hide",
    "item.mechanical.bog_torc": "Bog Torc",
    "item.mechanical.vittra_silver": "Vittra Silver",
    "item.mechanical.dune_glass": "Dune Glass",
    "block.mechanical.frost_heart": "Frost Heart",
    "block.mechanical.mossy_troll_stone": "Mossy Troll Stone",
    "effect.mechanical.lost": "Lost",
    "effect.mechanical.mountain_guest": "Mountain Guest",
    "effect.mechanical.fiddlers_blessing": "Fiddler's Blessing",
    "message.mechanical.huldra.accept": "You take her gift. The forest feels kind tonight.",
    "message.mechanical.huldra.refuse": "You turn away... and the trees all look the same.",
    "message.mechanical.wisp.astray": "The light goes out. Where is the path?",
    "message.mechanical.tomte.not_yule": "He does not touch it. It is not Yule.",
    "message.mechanical.tomte.fed": "The Yule porridge is gone. Someone is pleased.",
    "message.mechanical.tomte.angry": "Something small and grey glares at you.",
    "message.mechanical.nacken.fiddle": "A fiddle plays down by the rapids...",
    "message.mechanical.nacken.again": "He has taught you already this year.",
    "message.mechanical.nacken.lesson": "Näcken plays for you alone, and you understand.",
    "message.mechanical.dune_wraith.stole": "A cold breath of sand... and your water is gone.",
    "message.mechanical.dune_wraith.touch": "Sand brushes past you and is gone.",
    "message.mechanical.bergtagen": "You wake in the dark, deep inside the mountain. Small grey shapes watch you.",
})
LANG.write_text(json.dumps(lang, indent=2) + "\n", encoding="utf-8")
print("folklore ok")
