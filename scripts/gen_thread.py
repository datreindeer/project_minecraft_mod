"""Thread sources (Spinning.java, GridToolItem.java; docs/ages/01-stone-age.md "Tråd", 02-bronze-age.md "Lin", docs/farming.md).

- Stinging Nettle (stinging_nettle, age 0 seedling / 1 grown): models, blockstate, loot (grown: 1-2 Nettle Stalks, now and then
  Nettle Seeds; a seedling gives its seed back), worldgen (forest patches; riverbanks: only next to water), damage type
  mechanical:nettle (death messages), mineable/sickle
- Flax (flax_crop, ages 0-7, 5 drawn stages): models, blockstate, loot (ripe: 2 Flax Stalks, a sickle adds a third half the
  time, 1-2 Flax Seeds; unripe nothing), crop tags. Flax Seeds: the village Farmer's level 3 (Bronze Age) trade
- items: Nettle Seeds, Nettle Stalks, Nettle Fiber, Flax Seeds, Flax Stalks, Flax Fiber, Nettle / Linen / Sinew Thread,
  Drop Spindle (models, lang)
- recipes: stalks -> fiber (knife; flax is retted in a water bucket first), fibre or wool + Drop Spindle -> thread, Sinew +
  knife -> Sinew Thread, the Drop Spindle (Stick over a whorl: rock chunk, Bone or Clay Ball)
- item tag mechanical:threads (the Armor Station sews leather with any of them)
Textures: scripts/gen_thread_textures.py.
Generator order: ... gen_tribes2.py, gen_wildtweaks.py, gen_woodmeat.py, gen_thread.py, gen_block_items.py, gen_unlocks.py,
gen_advancements.py, gen_registry.py (after gen_bronze, whose farmer level 3 trade tag it adds to). Run: python scripts/gen_thread.py
"""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent / "src/main/resources"
A, D = ROOT / "assets/mechanical", ROOT / "data"
M = "mechanical:"


def w(path, obj):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")


def tag_add(path, *values):
    old = json.loads(path.read_text(encoding="utf-8")) if path.exists() else {"values": []}
    old["values"] += [v for v in values if v not in old["values"]]
    w(path, old)


def item(name, lo=1, hi=1, condition=None):
    e = {"type": "minecraft:item", "name": name}
    if (lo, hi) != (1, 1):
        e["modifier"] = [{"type": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": lo, "max": hi} if lo != hi else lo}]
    if condition:
        e["condition"] = condition
    return e


def every(*terms):
    return {"type": "minecraft:all_of", "terms": list(terms)}


def chance(p):
    return {"type": "minecraft:random_chance", "chance": p}


def state(block, age):
    return {"type": "minecraft:match_block", "blocks": M + block, "state": {"age": str(age)}}


def loot(block, pools):
    w(D / f"mechanical/loot_table/blocks/{block}.json", {"type": "minecraft:block", "pools": pools, "random_sequence": f"mechanical:blocks/{block}"})


def recipe(name, ingredients, result, n=1, category="misc"):
    w(D / f"mechanical/recipe/{name}.json", {"type": "minecraft:crafting_shapeless", "category": category,
                                              "ingredients": ingredients, "result": {"id": result, "count": n}})


CUT = {"type": "minecraft:match_tool", "predicate": {"items": "#mechanical:knives"}}
SICKLE = {"type": "minecraft:match_tool", "predicate": {"items": "#mechanical:sickles"}}
lang = {}

# ---------------------------------------------------------------- Stinging Nettle
for s in ("stinging_nettle", "stinging_nettle_young"):
    w(A / f"models/block/{s}.json", {"parent": "minecraft:block/cross", "textures": {"cross": f"mechanical:block/{s}"}})
# grown first: gen_block_items draws the creative item from the first model
w(A / "blockstates/stinging_nettle.json", {"variants": {"age=1": {"model": M + "block/stinging_nettle"},
                                                         "age=0": {"model": M + "block/stinging_nettle_young"}}})
lang["block.mechanical.stinging_nettle"] = "Stinging Nettle"
GROWN = state("stinging_nettle", 1)
loot("stinging_nettle", [
    {"rolls": 1, "condition": GROWN, "entries": [item(M + "nettle_stalks", 1, 2)]},
    # the seed heads hang under the leaves: a knife or sickle gets them more often
    {"rolls": 1, "condition": GROWN, "entries": [{"type": "minecraft:alternatives", "children": [
        item(M + "nettle_seeds", condition=every({"type": "minecraft:any_of", "terms": [CUT, SICKLE]}, chance(0.35))),
        item(M + "nettle_seeds", condition=chance(0.2))]}]},
    {"rolls": 1, "condition": state("stinging_nettle", 0), "entries": [item(M + "nettle_seeds")]}])
w(D / "mechanical/damage_type/nettle.json", {"exhaustion": 0.1, "message_id": "mechanical.nettle", "scaling": "never"})
lang["death.attack.mechanical.nettle"] = "%1$s was stung to death by nettles"
lang["death.attack.mechanical.nettle.player"] = "%1$s was stung to death by nettles while fleeing %2$s"
tag_add(D / "mechanical/tags/block/mineable/sickle.json", M + "stinging_nettle")

# worldgen: patches on the forest floor, and along rivers and fens wherever water is right beside the bank
tri = lambda m: {"type": "minecraft:trapezoid", "min": -m, "max": m, "plateau": 0}
below = lambda t: {"type": "minecraft:matching_block_tag", "tag": t, "offset": [0, -1, 0]}


def forage(name, biomes, placement, ground):
    w(D / f"mechanical/worldgen/feature/{name}.json", {"type": "minecraft:simple_block", "to_place": {"id": M + "stinging_nettle"}})
    w(D / f"mechanical/worldgen/placed_feature/{name}.json", {"feature": M + name, "placement": placement + [
        {"type": "minecraft:heightmap", "heightmap": "MOTION_BLOCKING_NO_LEAVES"}, {"type": "minecraft:biome"},
        {"type": "minecraft:block_predicate_filter", "predicate": {"type": "minecraft:all_of", "predicates": [
            {"type": "minecraft:replaceable"}, below("minecraft:substrate_overworld")] + ground}}]})
    w(D / f"mechanical/neoforge/biome_modifier/add_{name}.json", {"type": "neoforge:add_features", "biomes": biomes,
        "features": M + name, "step": "vegetal_decoration"})


forage("stinging_nettle_forest", ["minecraft:forest", "minecraft:flower_forest", "minecraft:birch_forest", "minecraft:old_growth_birch_forest",
                                  "minecraft:dark_forest", M + "deciduous_forest"],
       [{"type": "minecraft:rarity_filter", "chance": 5}, {"type": "minecraft:in_square"}, {"type": "minecraft:count", "count": 8},
        {"type": "minecraft:offset", "x": tri(3), "y": 0, "z": tri(3)}], [])
WATER_NEAR = {"type": "minecraft:any_of", "predicates": [
    {"type": "minecraft:matching_fluids", "fluids": "minecraft:water", "offset": [dx, -1, dz]}
    for dx in (-2, -1, 0, 1, 2) for dz in (-2, -1, 0, 1, 2) if (dx, dz) != (0, 0)]}
forage("stinging_nettle_riverbank", ["minecraft:river", "minecraft:swamp", M + "willow_fen"],
       [{"type": "minecraft:count", "count": 16}, {"type": "minecraft:in_square"}], [WATER_NEAR])

# ---------------------------------------------------------------- Flax
STAGE = [0, 0, 1, 1, 2, 2, 3, 4]
for s in range(5):
    w(A / f"models/block/flax_stage{s}.json", {"parent": "minecraft:block/crop", "textures": {"crop": f"mechanical:block/flax_stage{s}"}})
w(A / "blockstates/flax_crop.json", {"variants": {f"age={a}": {"model": f"mechanical:block/flax_stage{STAGE[a]}"} for a in range(8)}})
lang["block.mechanical.flax_crop"] = "Flax"
RIPE = state("flax_crop", 7)
loot("flax_crop", [{"rolls": 1, "condition": RIPE, "entries": [item(M + "flax_stalks", 2, 2)]},
                   {"rolls": 1, "condition": every(RIPE, SICKLE, chance(0.5)), "entries": [item(M + "flax_stalks")]},
                   {"rolls": 1, "condition": RIPE, "entries": [item(M + "flax_seeds", 1, 2)]}])
tag_add(D / "minecraft/tags/block/crops.json", M + "flax_crop")
tag_add(D / "minecraft/tags/block/maintains_farmland.json", M + "flax_crop")
# seed source: the village Farmer at trade level 3 (the Bronze Age levels, gen_bronze.py)
w(D / "mechanical/villager_trade/farmer/3/flax_seeds.json", {"wants": {"id": M + "copper_coin", "count": 8},
    "gives": {"id": M + "flax_seeds", "count": 4}, "max_uses": 12, "xp": 10, "reputation_discount": 0.05})
tag_add(D / "minecraft/tags/villager_trade/farmer/level_3.json", M + "farmer/3/flax_seeds")

# ---------------------------------------------------------------- items
ITEMS = {"nettle_seeds": "Nettle Seeds", "nettle_stalks": "Nettle Stalks", "nettle_fiber": "Nettle Fiber",
         "flax_seeds": "Flax Seeds", "flax_stalks": "Flax Stalks", "flax_fiber": "Flax Fiber",
         "nettle_thread": "Nettle Thread", "linen_thread": "Linen Thread", "sinew_thread": "Sinew Thread", "drop_spindle": "Drop Spindle"}
for i, n in ITEMS.items():
    w(A / f"models/item/{i}.json", {"parent": "minecraft:item/handheld" if i == "drop_spindle" else "minecraft:item/generated",
                                    "textures": {"layer0": f"mechanical:item/{i}"}})
    w(A / f"items/{i}.json", {"model": {"type": "minecraft:model", "model": f"mechanical:item/{i}"}})
    lang[f"item.mechanical.{i}"] = n
THREADS = [M + "wool_thread", M + "nettle_thread", M + "linen_thread", M + "sinew_thread", "minecraft:string"]
w(D / "mechanical/tags/item/threads.json", {"values": THREADS})
lang["tag.item.mechanical.threads"] = "Thread (any kind)"
tag_add(D / "mechanical/tags/item/compostable.json", M + "nettle_stalks", M + "flax_stalks")

# ---------------------------------------------------------------- recipes
KNIFE, SPINDLE = "#mechanical:knives", M + "drop_spindle"
recipe("nettle_fiber", [M + "nettle_stalks", KNIFE], M + "nettle_fiber", 2)
# retting: the stalks soak in water until the bast comes loose, then the knife scutches it off the woody core
recipe("flax_fiber", [M + "flax_stalks"] * 4 + [[M + "clay_water_bucket", "minecraft:water_bucket"], KNIFE], M + "flax_fiber", 6)
recipe("sinew_thread", [M + "sinew", KNIFE], M + "sinew_thread", 2)
recipe("nettle_thread", [M + "nettle_fiber", SPINDLE], M + "nettle_thread")
recipe("linen_thread", [M + "flax_fiber", SPINDLE], M + "linen_thread")
recipe("wool_thread_from_wool", ["#minecraft:wool", SPINDLE], M + "wool_thread", 3)
w(D / "mechanical/recipe/drop_spindle.json", {"type": "minecraft:crafting_shaped", "category": "equipment", "pattern": ["S", "W"],
    "key": {"S": "minecraft:stick", "W": [M + "stone_chunk", M + "granite_chunk", M + "diorite_chunk", M + "andesite_chunk",
                                          "minecraft:bone", "minecraft:clay_ball"]}, "result": {"id": SPINDLE}})

# ---------------------------------------------------------------- lang
lang_path = A / "lang/en_us.json"
full = json.loads(lang_path.read_text(encoding="utf-8"))
full.update(lang)
lang_path.write_text(json.dumps(full, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
print(f"thread: {len(ITEMS)} items, nettle + flax, {len(lang)} lang keys")
