# Farming data (docs/farming.md): crop/plant/block models, loot (vanilla crops too), recipes, tags, worldgen of the wild
# grains and Wild Pea, the hoe transformer (podzol -> rich farmland), lang. Textures: gen_farming_textures.py.
# usage: python scripts/gen_farming.py   (after gen_bronze_ores.py, before gen_block_items.py)
import json, zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
R = ROOT / "src/main/resources"
A, D = R / "assets/mechanical", R / "data"
JAR = ROOT.parent.parent / "project_minecraft_mod/build/moddev/artifacts/minecraft-patched-26.3.0.95-merged.jar"
if not JAR.exists():
    JAR = ROOT / "build/moddev/artifacts/minecraft-patched-26.3.0.95-merged.jar"
M = "mechanical:"


def w(path, obj):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, indent=2) + "\n")


def item_model(item, parent="minecraft:item/generated"):
    w(A / f"models/item/{item}.json", {"parent": parent, "textures": {"layer0": f"mechanical:item/{item}"}})
    w(A / f"items/{item}.json", {"model": {"type": "minecraft:model", "model": f"mechanical:item/{item}"}})


def count(lo, hi):
    return [{"type": "minecraft:set_count", "count": lo if lo == hi else {"type": "minecraft:uniform", "min": lo, "max": hi}}]


def item(name, lo=1, hi=1, **extra):
    e = {"type": "minecraft:item", "name": name}
    if (lo, hi) != (1, 1):
        e["modifier"] = count(lo, hi)
    e.update(extra)
    return e


def ripe(block, age=7):
    return {"type": "minecraft:match_block", "blocks": block, "state": {"age": str(age)}}


SICKLE = {"type": "minecraft:match_tool", "predicate": {"items": "#mechanical:sickles"}}


def loot(path, seq, pools):
    w(D / f"{path}.json", {"type": "minecraft:block", "pools": pools, "random_sequence": seq})


lang = {}

# ---------------------------------------------------------------- crops
# 5 drawn stages over ages 0..7 (vanilla crops reuse textures over ages the same way)
STAGE = [0, 0, 1, 1, 2, 2, 3, 4]
GRAINS = {"barley": "Barley", "einkorn": "Einkorn", "emmer": "Emmer", "oat": "Oats"}
for crop, name in list(GRAINS.items()) + [("pea", "Peas")]:
    block = f"{crop}_crop"
    for s in range(5):
        w(A / f"models/block/{crop}_stage{s}.json", {"parent": "minecraft:block/crop", "textures": {"crop": f"mechanical:block/{crop}_stage{s}"}})
    w(A / f"blockstates/{block}.json", {"variants": {f"age={a}": {"model": f"mechanical:block/{crop}_stage{STAGE[a]}"} for a in range(8)}})
    lang[f"block.mechanical.{block}"] = name

# ripe grain: 1 sheaf; a sickle adds 1 straw (and a chance of a second sheaf, per sickle: Farming.sickleBonus). Unripe: nothing (einkorn/emmer, still wild: 0-1 grain)
def grain_loot(block, sheaf, grain=None, seq=None):
    every = lambda *t: {"type": "minecraft:all_of", "terms": list(t)}
    pools = [{"rolls": 1, "condition": ripe(block), "entries": [item(sheaf)]},
             {"rolls": 1, "condition": every(ripe(block), SICKLE), "entries": [item(M + "straw")]}]
    if grain:
        pools.append({"rolls": 1, "condition": {"type": "minecraft:inverted", "term": ripe(block)},
                      "entries": [item(grain, 0, 1)]})
    return pools

for g in GRAINS:
    loot(f"mechanical/loot_table/blocks/{g}_crop", f"mechanical:blocks/{g}_crop",
         grain_loot(f"mechanical:{g}_crop", f"mechanical:{g}_sheaf", f"mechanical:{g}_grain" if g in ("einkorn", "emmer") else None))
loot("minecraft/loot_table/blocks/wheat", "minecraft:blocks/wheat", grain_loot("minecraft:wheat", "minecraft:wheat"))
# peas: ripe 1-3 (one goes back in the ground); roots: ripe 1-2 (vanilla 2-5); beetroot 1 + 0-1 seeds; unripe nothing
loot("mechanical/loot_table/blocks/pea_crop", "mechanical:blocks/pea_crop",
     [{"rolls": 1, "condition": ripe("mechanical:pea_crop"), "entries": [item(M + "peas", 1, 3)]}])
for crop, drop in (("carrots", "carrot"), ("potatoes", "potato")):
    loot(f"minecraft/loot_table/blocks/{crop}", f"minecraft:blocks/{crop}",
         [{"rolls": 1, "condition": ripe(f"minecraft:{crop}"), "entries": [item(f"minecraft:{drop}", 1, 2)]}])
loot("minecraft/loot_table/blocks/beetroots", "minecraft:blocks/beetroots",
     [{"rolls": 1, "condition": ripe("minecraft:beetroots", 3), "entries": [item("minecraft:beetroot")]},
      {"rolls": 1, "condition": ripe("minecraft:beetroots", 3), "entries": [item("minecraft:beetroot_seeds", 0, 1)]}])
# stems give nothing (the fruit is the harvest); a melon gives 3-7 slices and 0-1 seeds, the slice no longer crafts into seeds
for stem in ("melon_stem", "pumpkin_stem", "attached_melon_stem", "attached_pumpkin_stem"):
    loot(f"minecraft/loot_table/blocks/{stem}", f"minecraft:blocks/{stem}", [])
loot("minecraft/loot_table/blocks/melon", "minecraft:blocks/melon", [
    {"rolls": 1, "entries": [{"type": "minecraft:alternatives", "children": [
        {"type": "minecraft:item", "name": "minecraft:melon", "condition": "minecraft:tool/can_silk_touch"},
        item("minecraft:melon_slice", 3, 7)]}]},
    {"rolls": 1, "condition": {"type": "minecraft:inverted", "term": "minecraft:tool/can_silk_touch"},
     "entries": [item("minecraft:melon_seeds", 0, 1)]}])

# ---------------------------------------------------------------- wild plants and the withered crop
# the wild grains (stands that ripen and shatter) are in gen_wildgrain.py
WILD = {"wild_pea": ("Wild Pea", M + "peas", 1, 2)}
for block, (name, drop, lo, hi) in WILD.items():
    w(A / f"models/block/{block}.json", {"parent": "minecraft:block/cross", "textures": {"cross": f"mechanical:block/{block}"}})
    w(A / f"blockstates/{block}.json", {"variants": {"": {"model": f"mechanical:block/{block}"}}})
    lang[f"block.mechanical.{block}"] = name
    pool = [item(drop, lo, hi)]
    loot(f"mechanical/loot_table/blocks/{block}", f"mechanical:blocks/{block}", [{"rolls": 1, "entries": pool}])
w(A / "models/block/withered_crop.json", {"parent": "minecraft:block/crop", "textures": {"crop": "mechanical:block/withered_crop"}})
w(A / "blockstates/withered_crop.json", {"variants": {"": {"model": "mechanical:block/withered_crop"}}})
lang["block.mechanical.withered_crop"] = "Withered Crop"
loot("mechanical/loot_table/blocks/withered_crop", "mechanical:blocks/withered_crop", [{"rolls": 1, "entries": [
    {"type": "minecraft:alternatives", "children": [item(M + "straw", condition={"type": "minecraft:random_chance", "chance": 0.5}),
                                                     item(M + "plant_fibre")]}]}])

# worldgen: small rare stands (placement order as gen_data.forage_feature: rarity, in_square, count, offset, heightmap, biome, predicate)
tri = lambda m: {"type": "minecraft:trapezoid", "min": -m, "max": m, "plateau": 0}
def forage(name, block, biomes, rarity, n, spread):
    w(D / f"mechanical/worldgen/feature/{name}.json", {"type": "minecraft:simple_block", "to_place": {"id": f"mechanical:{block}"}})
    w(D / f"mechanical/worldgen/placed_feature/{name}.json", {"feature": f"mechanical:{name}", "placement": [
        {"type": "minecraft:rarity_filter", "chance": rarity}, {"type": "minecraft:in_square"}, {"type": "minecraft:count", "count": n},
        {"type": "minecraft:offset", "x": tri(spread), "y": 0, "z": tri(spread)},
        {"type": "minecraft:heightmap", "heightmap": "MOTION_BLOCKING_NO_LEAVES"}, {"type": "minecraft:biome"},
        {"type": "minecraft:block_predicate_filter", "predicate": {"type": "minecraft:all_of", "predicates": [
            {"type": "minecraft:replaceable"},
            {"type": "minecraft:matching_block_tag", "tag": "minecraft:substrate_overworld", "offset": [0, -1, 0]}]}}]})
    w(D / f"mechanical/neoforge/biome_modifier/add_{name}.json", {"type": "neoforge:add_features", "biomes": biomes,
        "features": f"mechanical:{name}", "step": "vegetal_decoration"})
forage("wild_pea", "wild_pea", ["minecraft:forest", "minecraft:flower_forest", "minecraft:birch_forest", "minecraft:meadow"], 12, 5, 3)

# ---------------------------------------------------------------- items
FLAT = {"barley_grain": "Barley Grain", "einkorn_grain": "Einkorn Grain", "emmer_grain": "Emmer Grain", "peas": "Peas",
        "barley_sheaf": "Barley Sheaf", "einkorn_sheaf": "Einkorn Sheaf", "emmer_sheaf": "Emmer Sheaf", "straw": "Straw",
        "oat_grain": "Oat Grain", "oat_sheaf": "Oat Sheaf",
        "raw_porridge": "Raw Porridge", "porridge": "Porridge", "flour": "Flour", "dough": "Dough", "compost": "Compost", "manure": "Manure"}
for i, n in FLAT.items():
    item_model(i)
    lang[f"item.mechanical.{i}"] = n
for i, n in {"flint_sickle": "Flint Sickle", "flail": "Flail"}.items():
    item_model(i, "minecraft:item/handheld")
    lang[f"item.mechanical.{i}"] = n
# vanilla wheat in the new system: the Wheat item is the sheaf, Wheat Seeds are the grain
lang.update({"item.minecraft.wheat": "Wheat Sheaf", "item.minecraft.wheat_seeds": "Wheat Grain"})

# ---------------------------------------------------------------- Bronze Age blocks (recipes: gen_farming3.py)
def faces(tex, **over):
    return {d: {"texture": over.get(d, tex)} for d in ("north", "south", "east", "west", "up", "down")}

def el(f, t, tex, **over):
    return {"from": f, "to": t, "faces": faces(tex, **over)}

# Quern: lower and upper millstone, a wooden peg handle; TURN turns the whole model (the stones are round enough)
w(A / "models/block/quern.json", {"textures": {"particle": "mechanical:block/quern_stone_side", "side": "mechanical:block/quern_stone_side",
    "top": "mechanical:block/quern_stone_top", "handle": "mechanical:block/quern_handle"}, "elements": [
    el([2, 0, 2], [14, 4, 14], "#side", up="#top", down="#top"),
    el([2.5, 4, 2.5], [13.5, 8.5, 13.5], "#side", up="#top", down="#top"),
    el([10, 8.5, 7], [12, 14, 9], "#handle")]})
w(A / "blockstates/quern.json", {"variants": {f"turn={t}": {"model": "mechanical:block/quern", "y": 90 * t} for t in range(4)}})
lang["block.mechanical.quern"] = "Quern"

# Baking Stone: a flat stone; what lies on it is a small loaf box textured from the overlay (loaf at x 4-11, y 5-10)
base = el([1, 0, 1], [15, 2, 15], "#side", up="#top", down="#top")
for c in ("empty", "dough", "bread", "burnt"):
    els = [base]
    tex = {"particle": "mechanical:block/baking_stone_side", "side": "mechanical:block/baking_stone_side", "top": "mechanical:block/baking_stone_top"}
    if c != "empty":
        tex["loaf"] = f"mechanical:block/baking_stone_{c}"
        side = {"uv": [4, 7, 12, 9], "texture": "#loaf"}
        els.append({"from": [4, 2, 5], "to": [12, 4, 11], "faces": {"up": {"uv": [4, 5, 12, 11], "texture": "#loaf"},
            "north": side, "south": side, "east": {"uv": [5, 7, 11, 9], "texture": "#loaf"}, "west": {"uv": [5, 7, 11, 9], "texture": "#loaf"}}})
    w(A / f"models/block/baking_stone{'' if c == 'empty' else '_' + c}.json", {"textures": tex, "elements": els})
w(A / "blockstates/baking_stone.json", {"variants": {f"contents={c}": {"model": f"mechanical:block/baking_stone{'' if c == 'empty' else '_' + c}"}
                                                      for c in ("empty", "dough", "bread", "burnt")}})
lang["block.mechanical.baking_stone"] = "Baking Stone"

# Compost Heap: wattle pen 12 high; the heap inside rises with LEVEL, dark compost on top when ready (8)
def wall(f, t, in_face):
    e = el(f, t, "#side")
    e["faces"][in_face] = {"texture": "#inside"}
    e["faces"]["up"] = {"uv": [0, 0, 16, 1], "texture": "#side"}
    return e
walls = [wall([0, 0, 0], [16, 12, 1], "south"), wall([0, 0, 15], [16, 12, 16], "north"),
         wall([0, 0, 1], [1, 12, 15], "east"), wall([15, 0, 1], [16, 12, 15], "west"),
         el([1, 0, 1], [15, 1, 15], "#inside")]
for lv in range(9):
    tex = {"particle": "mechanical:block/compost_heap_side", "side": "mechanical:block/compost_heap_side",
           "inside": "mechanical:block/compost_heap_inside", "fill": f"mechanical:block/compost_heap_{'compost' if lv == 8 else 'litter'}"}
    els = list(walls)
    if lv:
        els.append(el([1, 1, 1], [15, 1 + lv * 1.375, 15], "#inside", up="#fill"))
    w(A / f"models/block/compost_heap_level{lv}.json", {"textures": tex, "elements": els})
w(A / "blockstates/compost_heap.json", {"variants": {f"level={lv}": {"model": f"mechanical:block/compost_heap_level{lv}"} for lv in range(9)}})
lang["block.mechanical.compost_heap"] = "Compost Heap"

# Scarecrow: pole, hide tunic stuffed with straw, arms on a cross stick, carved pumpkin head; faces north before rotation
SC_TEX = {"particle": "mechanical:block/scarecrow_cloth", "post": "mechanical:block/scarecrow_post", "cloth": "mechanical:block/scarecrow_cloth",
          "straw": "mechanical:block/scarecrow_straw", "face": "minecraft:block/carved_pumpkin", "pumpkin": "minecraft:block/pumpkin_side",
          "ptop": "minecraft:block/pumpkin_top"}
LOWER = [el([7, 0, 7], [9, 16, 9], "#post"), el([4, 10, 5], [12, 16, 11], "#cloth"), el([4.5, 8, 5.5], [11.5, 10, 10.5], "#straw")]
UPPER = [el([4, 0, 5], [12, 8, 11], "#cloth"), el([0, 6, 7], [16, 7.5, 8.5], "#post"),
         el([1, 5.5, 6], [4, 8.5, 9.5], "#cloth"), el([12, 5.5, 6], [15, 8.5, 9.5], "#cloth"),
         el([0, 5, 6.5], [1, 8, 9], "#straw"), el([15, 5, 6.5], [16, 8, 9], "#straw"), el([6, 8, 6], [10, 9, 10], "#straw"),
         el([4, 9, 4], [12, 16, 12], "#pumpkin", north="#face", up="#ptop", down="#ptop")]
w(A / "models/block/scarecrow_lower.json", {"textures": SC_TEX, "elements": LOWER})
w(A / "models/block/scarecrow_upper.json", {"textures": SC_TEX, "elements": UPPER})
shift = lambda e: dict(e, **{"from": [e["from"][0], e["from"][1] + 16, e["from"][2]], "to": [e["to"][0], e["to"][1] + 16, e["to"][2]]})
w(A / "models/item/scarecrow.json", {"textures": SC_TEX, "elements": LOWER + [shift(e) for e in UPPER], "display": {
    "gui": {"rotation": [30, 225, 0], "translation": [0, -4, 0], "scale": [0.36, 0.36, 0.36]},
    "ground": {"translation": [0, 2, 0], "scale": [0.25, 0.25, 0.25]},
    "fixed": {"translation": [0, -4, 0], "scale": [0.4, 0.4, 0.4]},
    "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 0, 2], "scale": [0.25, 0.25, 0.25]},
    "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [0, -2, 0], "scale": [0.3, 0.3, 0.3]}}})
w(A / "items/scarecrow.json", {"model": {"type": "minecraft:model", "model": "mechanical:item/scarecrow"}})
w(A / "blockstates/scarecrow.json", {"variants": {f"facing={f},half={h}": {"model": f"mechanical:block/scarecrow_{h}", **({"y": y} if y else {})}
    for f, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270)) for h in ("lower", "upper")}})
lang["block.mechanical.scarecrow"] = "Scarecrow"

for b in ("quern", "baking_stone", "compost_heap"):
    loot(f"mechanical/loot_table/blocks/{b}", f"mechanical:blocks/{b}", [{"rolls": 1, "entries": [item(M + b)]}])
loot("mechanical/loot_table/blocks/scarecrow", "mechanical:blocks/scarecrow", [{"rolls": 1, "entries": [item(M + "scarecrow", condition={
    "type": "minecraft:match_block", "blocks": "mechanical:scarecrow", "state": {"half": "lower"}})]}])

# ---------------------------------------------------------------- recipes
def shapeless(name, ingredients, result, n=1, category="misc"):
    w(D / f"mechanical/recipe/{name}.json", {"type": "minecraft:crafting_shapeless", "category": category,
                                              "ingredients": ingredients, "result": {"id": result, "count": n}})

def never(path):
    w(D / f"{path}.json", {"neoforge:conditions": [{"type": "neoforge:never"}]})

# threshing by hand (no flail): one sheaf -> one grain
SHEAF_GRAIN = {"minecraft:wheat": "minecraft:wheat_seeds", **{f"mechanical:{g}_sheaf": f"mechanical:{g}_grain" for g in GRAINS}}
for sheaf, grain in SHEAF_GRAIN.items():
    shapeless(f"{grain.split(':')[1]}_by_hand", [sheaf], grain)
w(D / "mechanical/recipe/flail.json", {"type": "minecraft:crafting_shaped", "category": "equipment", "pattern": [" S", "TS", "S "],
    "key": {"S": "minecraft:stick", "T": "mechanical:twine"}, "result": {"id": "mechanical:flail"}})
shapeless("raw_porridge", ["#mechanical:grains", "#mechanical:grains", ["mechanical:clay_water_bucket", "minecraft:water_bucket"], "minecraft:bowl"],
          "mechanical:raw_porridge")
w(D / "mechanical/recipe/porridge.json", {"type": "minecraft:campfire_cooking", "category": "food", "cookingtime": 400, "experience": 0.1,
    "ingredient": "mechanical:raw_porridge", "result": {"id": "mechanical:porridge"}})
# Bronze Age: Dough is mixed here, baked on the Baking Stone or in the Clay Kiln (Java); block recipes in gen_farming3.py
shapeless("dough", ["mechanical:flour", "mechanical:flour", ["mechanical:clay_water_bucket", "minecraft:water_bucket"]], "mechanical:dough")
# bread only from dough: the 3-wheat recipe is gone; cookies and cake take flour instead of wheat
never("minecraft/recipe/bread")
never("minecraft/advancement/recipes/food/bread")
w(D / "minecraft/recipe/cookie.json", {"type": "minecraft:crafting_shaped", "category": "misc", "pattern": ["#X#"],
    "key": {"#": "mechanical:flour", "X": "minecraft:cocoa_beans"}, "result": {"id": "minecraft:cookie", "count": 8}})
w(D / "minecraft/recipe/cake.json", {"type": "minecraft:crafting_shaped", "category": "misc", "pattern": ["AAA", "BEB", "CCC"],
    "key": {"A": "minecraft:milk_bucket", "B": "minecraft:sugar", "C": "mechanical:flour", "E": "#minecraft:eggs"}, "result": {"id": "minecraft:cake"}})
# melon and pumpkin seeds are scarce: a pumpkin gives 1 seed (vanilla 4), a slice none (melons drop 0-1 seeds)
never("minecraft/recipe/melon_seeds")
never("minecraft/advancement/recipes/misc/melon_seeds")
w(D / "minecraft/recipe/pumpkin_seeds.json", {"type": "minecraft:crafting_shapeless", "category": "misc",
    "ingredients": ["minecraft:pumpkin"], "result": {"id": "minecraft:pumpkin_seeds", "count": 1}})

UNLOCKS = {"farming_sheaf": ("#mechanical:sheaves", [f"{g.split(':')[1]}_by_hand" for g in SHEAF_GRAIN.values()]),
           "farming_grain": ("#mechanical:grains", ["raw_porridge"]),
           "farming_twine": ("mechanical:twine", ["flail", "crop_stakes"]),
           "farming_flour": ("mechanical:flour", ["dough"])}
for name, (it, recipes) in UNLOCKS.items():
    w(D / f"mechanical/advancement/recipes/{name}.json", {"parent": "minecraft:recipes/root",
        "criteria": {"has_item": {"trigger": "minecraft:inventory_changed", "conditions": {"items": [{"items": it}]}}},
        "requirements": [["has_item"]], "rewards": {"recipes": [f"mechanical:{r}" for r in recipes]}})

# ---------------------------------------------------------------- crop stakes (CropStakesItem; farmland model: gen_farmland_textures.py)
item_model("crop_stakes")
lang["item.mechanical.crop_stakes"] = "Crop Stakes"
lang["message.mechanical.fertilizer.full"] = "This soil is already as rich as it gets (fertility 3/3)"
lang["tooltip.mechanical.sickle_yield"] = "Ripe grain: %s%% chance of an extra Sheaf, and 1 Straw"
lang["tooltip.mechanical.sickle_swath"] = "Cuts %1$sx%1$s ripe crops or grass in one stroke (sneak: one)"
w(D / "mechanical/recipe/crop_stakes.json", {"type": "minecraft:crafting_shaped", "category": "misc", "pattern": ["S S", "STS"],
    "key": {"S": "minecraft:stick", "T": "mechanical:twine"}, "result": {"id": "mechanical:crop_stakes", "count": 2}})
# breaking staked farmland gives the stakes back (trampling and drying: FarmlandBlockMixin)
with zipfile.ZipFile(JAR) as jar:
    farmland = json.loads(jar.read("data/minecraft/loot_table/blocks/farmland.json").decode())
farmland["pools"].append({"rolls": 1, "entries": [item(M + "crop_stakes")], "condition":
    {"type": "minecraft:match_block", "blocks": "minecraft:farmland", "state": {"staked": "true"}}})
w(D / "minecraft/loot_table/blocks/farmland.json", farmland)

# ---------------------------------------------------------------- tags
CROP_BLOCKS = [f"mechanical:{c}_crop" for c in list(GRAINS) + ["pea"]]
w(D / "mechanical/tags/item/grains.json", {"values": ["minecraft:wheat_seeds"] + [f"mechanical:{g}_grain" for g in GRAINS]})
w(D / "mechanical/tags/item/sheaves.json", {"values": list(SHEAF_GRAIN)})
w(D / "mechanical/tags/item/sickles.json", {"values": ["mechanical:flint_sickle"]})
w(D / "mechanical/tags/item/compostable.json", {"values": ["minecraft:leaf_litter", "mechanical:straw", "mechanical:plant_fibre",
    "minecraft:rotten_flesh", "mechanical:burnt_food", "#minecraft:leaves", "#minecraft:saplings", "minecraft:short_grass", "minecraft:fern",
    "minecraft:melon_slice", "minecraft:beetroot", "minecraft:carrot", "minecraft:potato", "minecraft:poisonous_potato", "minecraft:apple",
    "#mechanical:barks", "mechanical:fish_bones"]})
w(D / "mechanical/tags/block/threshing_floors.json", {"values": ["minecraft:stone", "minecraft:cobblestone", "minecraft:mossy_cobblestone",
    "minecraft:smooth_stone", "minecraft:stone_bricks", "minecraft:cobbled_deepslate", "minecraft:deepslate", "minecraft:granite",
    "minecraft:diorite", "minecraft:andesite", "minecraft:polished_granite", "minecraft:polished_diorite", "minecraft:polished_andesite",
    "minecraft:packed_mud", "minecraft:mud_bricks", "mechanical:stone_anvil"]})
w(D / "mechanical/tags/block/mineable/sickle.json", {"values": ["#minecraft:crops", "minecraft:short_grass", "minecraft:tall_grass",
    "minecraft:fern", "minecraft:large_fern", "mechanical:wild_pea",
    "mechanical:withered_crop", "minecraft:hay_block"]})
w(D / "minecraft/tags/block/crops.json", {"replace": False, "values": CROP_BLOCKS})
w(D / "minecraft/tags/block/maintains_farmland.json", {"replace": False, "values": CROP_BLOCKS + ["mechanical:withered_crop"]})
w(D / "minecraft/tags/item/villager_plantable_seeds.json", {"replace": False, "values": [f"mechanical:{g}_grain" for g in GRAINS]})

# ---------------------------------------------------------------- hoe: podzol (forest humus) tills into rich farmland (fertility 3)
with zipfile.ZipFile(JAR) as jar:
    hoe = json.loads(jar.read("data/minecraft/block_transformer/hoe.json").decode())
rules = hoe[0]["block_state_provider"]["rules"]
air_above = {"type": "minecraft:matching_block_tag", "offset": [0, 1, 0], "tag": "minecraft:air"}
rules.insert(0, {"if_true": {"type": "minecraft:all_of", "predicates": [{"type": "minecraft:matching_blocks", "blocks": "minecraft:podzol"}, air_above]},
                 "then": {"id": "minecraft:farmland", "properties": {"moisture": "0", "fertility": "3"}}})
# loess (EarthBlocks, gen_earth.py): wind-blown silt, rich farmland too
rules.insert(0, {"if_true": {"type": "minecraft:all_of", "predicates": [{"type": "minecraft:matching_blocks", "blocks": "mechanical:loess"}, air_above]},
                 "then": {"id": "minecraft:farmland", "properties": {"moisture": "0", "fertility": "3"}}})
w(D / "minecraft/block_transformer/hoe.json", hoe)

# ---------------------------------------------------------------- lang
lang.update({
    "message.mechanical.flail.need_sheaf": "Hold sheaves in your other hand to thresh them",
    "message.mechanical.field_info": "Fertility %s/3, moisture %s/7",
    "message.mechanical.crop_info": "%s: %s° (grows %s-%s) | %s | %s",
    "message.mechanical.crop.growing": "growing", "message.mechanical.crop.dry": "too dry", "message.mechanical.crop.dark": "needs daylight",
    "message.mechanical.crop.cold": "too cold", "message.mechanical.crop.hot": "too hot", "message.mechanical.crop.ripe": "ripe",
    "message.mechanical.crop.season": "resting till spring",
})
lang_path = A / "lang/en_us.json"
full = json.loads(lang_path.read_text(encoding="utf-8"))
full.update(lang)
w(lang_path, full)
print("farming ok")
