"""Flora and food phases 4, 4b and 5 (kitchen) data (docs/flora-and-food-plan.md; Java: Herbs, HerbBlock, Kitchen,
ClayPotBlock, CookwareBlock(Entity), SaltPanBlock, client/CookwareRenderer).

- Wild vegetables (Wild Garlic, Onion, Cabbage, Leek, Parsnip, Celery, Turnip, Lettuce): plants in the world placed by
  climate window (like gen_flora: biome base temperature and downfall, or a fixed list for specialists), their wild crops
  and the tamed crops (models, blockstates, loot), produce items. The 25 % tamed seed is added in Java (Herbs.domesticate).
- Bought crops: Broad Beans, Dill, Mustard (villagers), Ginger, Saffron, Rice (Eastern Caravan).
- Herbs (HerbBlock: full / cut models, loot = the plant itself, worldgen), their cut items.
- The Eastern Caravan's goods; villager farmer coin trades for the goods only villagers sell (Barley, Onion, Garlic,
  Cabbage, Dill, Mustard, Broad Beans; Wild Apple Sapling at level 4).
- Phase 6 (the Steel Age kitchen: Steel Pan, Mortar and Pestle, Sieve, Steel Knife Set, spice blends, rice dishes) and the
  phase 4-5 leftovers (Cider, Ale, pies, Roast with Herbs): lang, recipes, loot, tags.
- Kitchen: dish items' lang, recipes (cookware crafting, raw dishes, campfire), loot for the cookware, tags (perishing,
  compostable, grains, culinary herbs, spices, cellar produce, mineable), messages. Cookware models/blockstates/items:
  scripts/herbs_cookware.py (imported and run from here, it patches gen_kettle's tripod blockstate).
Textures: the standalone scripts/gen_herbs_textures.py and scripts/gen_cookware_textures.py.

Generator order: ... gen_writing.py, gen_flora.py, gen_herbs.py, gen_winter_plants.py ... (after gen_kettle, gen_thread,
gen_bronze, gen_roots, gen_preserving, whose tags and blockstates it adds to; before gen_block_items, gen_unlocks,
gen_advancements, gen_registry). Run: python scripts/gen_herbs.py"""
import glob
import json
import sys
import zipfile
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import herbs_cookware  # noqa: E402

ROOT = Path(__file__).resolve().parent.parent
RES = ROOT / "src/main/resources"
A, D = RES / "assets/mechanical", RES / "data"
M, S = "minecraft:", "mechanical:"


def w(path, obj, ascii=False):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, indent=2, ensure_ascii=ascii) + "\n", encoding="utf-8")


def read(path, default):
    return json.loads(path.read_text(encoding="utf-8")) if path.exists() else default


def tag(path, *values):
    t = read(path, {"values": []})
    t["values"] += [v for v in values if v not in t["values"]]
    w(path, t)


# ---------------------------------------------------------------- climate of every land biome (as gen_flora)
jar = zipfile.ZipFile(sorted(glob.glob(str(ROOT / "build/moddev/artifacts/minecraft-patched-*-merged.jar")))[0])
NOT_LAND = ("ocean", "river", "caves", "deep_dark", "mushroom", "peaks", "ice_spikes", "beach")
CLIMATE = {}
for b in json.loads(jar.read("data/minecraft/tags/worldgen/biome/is_overworld.json"))["values"]:
    if not any(n in b for n in NOT_LAND):
        j = json.loads(jar.read(f"data/minecraft/worldgen/biome/{b.split(':')[1]}.json"))
        CLIMATE[b] = (j["temperature"], j["downfall"])
MOD_SURFACE = ["deciduous_forest", "hazel_meadow", "willow_fen", "pine_heath", "chalk_downs", "peat_bog", "slate_highlands",
               "soapstone_valley", "limestone_karst", "marble_gorge", "ochre_badlands", "basalt_coast", "shingle_beach", "reed_delta",
               "glacial_moraine", "gypsum_flats", "mammoth_steppe"]  # as gen_flora: surface biomes, not the Scorched Barrens or caves
for b in MOD_SURFACE:
    j = read(D / f"mechanical/worldgen/biome/{b}.json", None)
    CLIMATE[S + b] = (j["temperature"], j["downfall"])


def climate(tmin, tmax, dmin=0.0, dmax=1.0):
    return sorted(b for b, (t, d) in CLIMATE.items() if tmin <= t <= tmax and dmin <= d <= dmax)


def biomes(*names):
    out = [n if ":" in n else (S + n if S + n in CLIMATE else M + n) for n in names]
    for b in out:
        assert b in CLIMATE or b in (M + "stony_shore", M + "beach", M + "swamp"), f"unknown biome {b}"
    return out


tri = lambda m: {"type": "minecraft:trapezoid", "min": -m, "max": m, "plateau": 0}


def patch(name, state, where, rarity, count, spread, dry=False):
    """A patch of a plant on natural ground (sand and gravel too for coast kinds), as gen_flora.patch."""
    w(D / f"mechanical/worldgen/feature/{name}.json", {"type": "minecraft:simple_block", "to_place": state})
    tags = ["minecraft:substrate_overworld"] + (["minecraft:sand"] if dry else [])
    preds = [{"type": "minecraft:matching_block_tag", "tag": t, "offset": [0, -1, 0]} for t in tags]
    if dry:
        preds.append({"type": "minecraft:matching_blocks", "blocks": [M + "gravel", S + "beach_pebbles"], "offset": [0, -1, 0]})
    w(D / f"mechanical/worldgen/placed_feature/{name}.json", {"feature": S + name, "placement": [
        {"type": "minecraft:rarity_filter", "chance": rarity}, {"type": "minecraft:in_square"}, {"type": "minecraft:count", "count": count},
        {"type": "minecraft:offset", "x": tri(spread), "y": 0, "z": tri(spread)},
        {"type": "minecraft:heightmap", "heightmap": "MOTION_BLOCKING_NO_LEAVES"}, {"type": "minecraft:biome"},
        {"type": "minecraft:block_predicate_filter", "predicate": {"type": "minecraft:all_of", "predicates": [
            {"type": "minecraft:replaceable"}, {"type": "minecraft:any_of", "predicates": preds}]}}]})
    w(D / f"mechanical/neoforge/biome_modifier/add_{name}.json", {"type": "neoforge:add_features", "biomes": where,
                                                                   "features": S + name, "step": "vegetal_decoration"})
    print(f"{name}: {', '.join(b.split(':')[1] for b in where)}")


def loot(block, pools):
    w(D / f"mechanical/loot_table/blocks/{block}.json", {"type": "minecraft:block", "pools": pools, "random_sequence": f"mechanical:blocks/{block}"})


def entry(name, lo=1, hi=1, condition=None):
    e = {"type": "minecraft:item", "name": name}
    if (lo, hi) != (1, 1):
        e["modifier"] = [{"type": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": lo, "max": hi}}]
    if condition:
        e["condition"] = condition
    return e


def chance(p):
    return {"type": "minecraft:random_chance", "chance": p}


def flat_item(item, tex=None):
    w(A / f"models/item/{item}.json", {"parent": M + "item/generated", "textures": {"layer0": S + f"item/{tex or item}"}})
    w(A / f"items/{item}.json", {"model": {"type": "minecraft:model", "model": S + f"item/{item}"}})


KNIFE_OR_SICKLE = {"type": "minecraft:match_tool", "predicate": {"items": "#mechanical:knives"}}
SICKLE = {"type": "minecraft:match_tool", "predicate": {"items": "#mechanical:sickles"}}
lang = read(A / "lang/en_us.json", {})
BT, IT, MIT = D / "minecraft/tags/block", D / "minecraft/tags/item", D / "mechanical/tags/item"

# ---------------------------------------------------------------- vegetables: wild plant, wild crop, tamed crop
# name: English, wild biomes, (wild min, max), (tamed min, max), patch (rarity, count, spread), coast ground
VEG = {
    "garlic": ("Garlic", biomes("deciduous_forest", "willow_fen", "forest", "dark_forest", "birch_forest", "flower_forest"), (1, 2), (1, 3), (8, 6, 3), False),
    "onion": ("Onion", climate(0.5, 1.0, 0.2, 0.6), (1, 2), (1, 3), (10, 5, 3), False),
    "cabbage": ("Cabbage", biomes("chalk_downs", "basalt_coast", "stony_shore", "shingle_beach"), (1, 2), (1, 1), (6, 5, 3), True),
    "leek": ("Leek", biomes("hazel_meadow", "meadow", "plains", "sunflower_plains"), (1, 2), (1, 2), (12, 4, 3), False),
    "parsnip": ("Parsnip", climate(0.5, 0.9, 0.2, 0.6), (1, 2), (1, 2), (12, 4, 3), False),
    "celery": ("Celery", biomes("reed_delta", "willow_fen", "swamp"), (1, 2), (1, 2), (6, 6, 3), False),
    "turnip": ("Turnip", climate(0.6, 0.9, 0.3, 0.7), (1, 2), (1, 2), (12, 4, 3), False),
    "lettuce": ("Lettuce", climate(0.6, 1.0, 0.3, 0.6), (1, 2), (1, 2), (14, 4, 3), False),
}
WILD_SPECIES = {"garlic": "Wild Garlic", "onion": "Wild Onion", "cabbage": "Wild Cabbage", "leek": "Wild Leek", "parsnip": "Wild Parsnip",
                "celery": "Wild Celery", "turnip": "Wild Turnip", "lettuce": "Wild Lettuce"}
STAGE = [0, 0, 1, 1, 2, 2, 2, 3]  # 8 ages drawn in 4 stages, like the wild carrot


def crop_assets(base, en):
    for s in range(4):
        w(A / f"models/block/{base}_stage{s}.json", {"parent": M + "block/crop", "textures": {"crop": S + f"block/{base}_stage{s}"}})
    w(A / f"blockstates/{base}_crop.json", {"variants": {f"age={a}": {"model": S + f"block/{base}_stage{STAGE[a]}"} for a in range(8)}})
    lang[f"block.mechanical.{base}_crop"] = en
    tag(BT / "crops.json", S + base + "_crop")
    tag(BT / "maintains_farmland.json", S + base + "_crop")
    tag(D / "mechanical/tags/block/mineable/sickle.json", S + base + "_crop")


def ripe(block):
    return {"type": "minecraft:match_block", "blocks": S + block, "state": {"age": "7"}}


def crop_loot(block, *pools):
    """Ripe: these pools; unripe: nothing (docs/farming.md)."""
    loot(block, [{"rolls": 1, "condition": ripe(block), "entries": [entry(item, lo, hi)]} for item, lo, hi in pools])


for v, (en, where, (wlo, whi), (tlo, thi), (rarity, count, spread), coast) in VEG.items():
    wild = "wild_" + v
    plant = wild + "_plant"
    w(A / f"models/block/{plant}.json", {"parent": M + "block/cross", "textures": {"cross": S + f"block/{plant}"}})
    w(A / f"blockstates/{plant}.json", {"variants": {"": {"model": S + f"block/{plant}"}}})
    lang[f"block.mechanical.{plant}"] = f"{WILD_SPECIES[v]} Plant"
    # dug out with a knife or sickle 1-2, pulled by hand half the time one (as the wild carrot)
    loot(plant, [{"rolls": 1, "entries": [{"type": "minecraft:alternatives", "children": [
        entry(S + wild, 1, 2, KNIFE_OR_SICKLE), entry(S + wild, 1, 2, SICKLE), entry(S + wild, 1, 1, chance(0.5))]}]}])
    tag(D / "mechanical/tags/block/mineable/sickle.json", S + plant)
    patch(f"{wild}_patch", {"id": S + plant}, where, rarity, count, spread, coast)
    crop_assets(wild, WILD_SPECIES[v])
    crop_assets(v, en)
    crop_loot(wild + "_crop", (S + wild, wlo, whi))
    crop_loot(v + "_crop", (S + v, tlo, thi))
    flat_item(wild)
    flat_item(v)
    lang[f"item.mechanical.{wild}"] = WILD_SPECIES[v]
    lang[f"item.mechanical.{v}"] = en

# ---------------------------------------------------------------- bought crops
BOUGHT = {  # crop base: (crop name, seed item, seed name, ripe pools)
    "broad_bean": ("Broad Beans", "broad_beans", "Broad Beans", [(S + "broad_beans", 1, 3)]),
    "dill": ("Dill", "dill_seeds", "Dill Seeds", [(S + "dill", 1, 2), (S + "dill_seeds", 1, 2)]),
    "mustard": ("Mustard", "mustard_seeds", "Mustard Seeds", [(S + "mustard_seeds", 2, 3)]),
    "ginger": ("Ginger", "ginger_root", "Ginger Root", [(S + "ginger_root", 1, 3)]),
    "saffron": ("Saffron Crocus", "saffron_corm", "Saffron Crocus Corm", [(S + "saffron", 1, 1), (S + "saffron_corm", 1, 2)]),
    "rice": ("Rice", "rice", "Rice", [(S + "rice", 2, 3)]),
}
for base, (en, seed, seed_en, pools) in BOUGHT.items():
    crop_assets(base, en)
    crop_loot(base + "_crop", *pools)
    flat_item(seed)
    lang[f"item.mechanical.{seed}"] = seed_en
for item, en in {"dill": "Dill", "saffron": "Saffron"}.items():
    flat_item(item)
    lang[f"item.mechanical.{item}"] = en

# ---------------------------------------------------------------- herbs
# plant id: (English, item, item English, biomes, (rarity, count, spread))
HERBS = {
    "wild_thyme": ("Wild Thyme", "thyme", "Thyme", biomes("chalk_downs", "limestone_karst", "marble_gorge", "pine_heath", "meadow", "windswept_hills"), (5, 8, 3)),
    "wild_marjoram": ("Wild Marjoram", "marjoram", "Marjoram", biomes("marble_gorge", "chalk_downs", "limestone_karst"), (5, 6, 3)),
    "yarrow": ("Yarrow", "yarrow_leaves", "Yarrow Leaves", climate(0.3, 0.9, 0.2, 0.8), (8, 6, 3)),
    "chamomile": ("Chamomile", "chamomile_flowers", "Chamomile Flowers", climate(0.6, 1.0, 0.3, 0.7), (8, 6, 3)),
    "water_mint": ("Water Mint", "mint_leaves", "Mint Leaves", biomes("willow_fen", "reed_delta", "swamp", "peat_bog"), (4, 8, 3)),
    "sorrel": ("Sorrel", "sorrel_leaves", "Sorrel Leaves", climate(0.4, 0.9, 0.4, 0.9), (10, 6, 3)),
    "sage": ("Sage", "sage_leaves", "Sage Leaves", biomes("marble_gorge", "limestone_karst", "savanna", "savanna_plateau"), (6, 6, 3)),
    "rosemary": ("Rosemary", "rosemary_sprigs", "Rosemary Sprigs", biomes("marble_gorge", "ochre_badlands", "savanna_plateau"), (6, 5, 3)),
    "bog_myrtle": ("Bog Myrtle", "bog_myrtle_leaves", "Bog Myrtle Leaves", biomes("peat_bog", "willow_fen", "pine_heath"), (4, 8, 3)),
    "meadowsweet": ("Meadowsweet", "meadowsweet_flowers", "Meadowsweet Flowers", biomes("willow_fen", "reed_delta", "meadow", "swamp"), (5, 6, 3)),
    "hops": ("Hops", "hop_cones", "Hop Cones", biomes("hazel_meadow", "deciduous_forest", "forest"), (10, 4, 2)),
    "wild_mustard": ("Wild Mustard", "mustard_seeds", None, climate(0.5, 1.0, 0.2, 0.6), (12, 5, 3)),
}
for herb, (en, item, item_en, where, (rarity, count, spread)) in HERBS.items():
    for cut in ("", "_cut"):
        w(A / f"models/block/{herb}{cut}.json", {"parent": M + "block/cross", "textures": {"cross": S + f"block/{herb}{cut}"}})
    w(A / f"blockstates/{herb}.json", {"variants": {"cut=false": {"model": S + f"block/{herb}"}, "cut=true": {"model": S + f"block/{herb}_cut"}}})
    w(A / f"models/item/{herb}.json", {"parent": M + "item/generated", "textures": {"layer0": S + f"block/{herb}"}})
    w(A / f"items/{herb}.json", {"model": {"type": "minecraft:model", "model": S + f"item/{herb}"}})
    lang[f"block.mechanical.{herb}"] = en
    loot(herb, [{"rolls": 1, "condition": {"type": "minecraft:survives_explosion"}, "entries": [entry(S + herb)]}])
    if item_en:
        flat_item(item)
        lang[f"item.mechanical.{item}"] = item_en
    patch(f"{herb}_patch", {"id": S + herb, "properties": {"cut": "false"}}, where, rarity, count, spread)
    tag(D / "mechanical/tags/block/mineable/sickle.json", S + herb)
    tag(MIT / "compostable.json", S + herb)
    tag(BT / "replaceable_by_trees.json", S + herb)

# ---------------------------------------------------------------- the Eastern Caravan's goods
EAST = {"black_pepper": "Black Pepper", "cinnamon_bark": "Cinnamon Bark", "sesame_seeds": "Sesame Seeds", "tea_leaves": "Tea Leaves",
        "dried_dates": "Dried Dates", "dried_figs": "Dried Figs", "indigo": "Indigo"}
for item, en in EAST.items():
    flat_item(item)
    lang[f"item.mechanical.{item}"] = en

# ---------------------------------------------------------------- villagers: the goods that came with farming (coins only)
TRADES = {2: [("barley_grain", 4, 3), ("onion", 3, 4), ("garlic", 3, 4), ("dill_seeds", 4, 3), ("mustard_seeds", 4, 3)],
          3: [("cabbage", 2, 5), ("broad_beans", 4, 5)],
          4: [("wild_apple_sapling", 1, 6)]}  # the plan's Apple Saplings: the Wild Apple tree (gen_trees); no grafted apple yet
for level, goods in TRADES.items():
    for item, count, copper in goods:
        w(D / f"mechanical/villager_trade/farmer/{level}/{item}.json", {"wants": {"id": S + "copper_coin", "count": copper},
            "gives": {"id": S + item, "count": count}, "max_uses": 12, "xp": 10 if level > 2 else 5, "reputation_discount": 0.05})
        tag(D / f"minecraft/tags/villager_trade/farmer/level_{level}.json", S + f"farmer/{level}/{item}")

# ---------------------------------------------------------------- kitchen: items, loot, recipes
DISHES = {
    "nettle_soup": "Nettle Soup", "root_pottage": "Root Pottage", "berry_mash": "Berry Mash", "herbal_tea": "Herbal Tea",
    "herb_stuffed_fish": "Herb-Stuffed Fish", "herb_roasted_fish": "Herb-Roasted Fish",
    "pea_soup": "Pea Soup", "onion_garlic_stew": "Onion and Garlic Stew", "spiced_porridge": "Spiced Porridge", "tea": "Tea",
    "elderberry_syrup": "Elderberry Syrup", "sloe_jam": "Sloe Jam", "flatbread": "Flatbread", "raw_honey_cake": "Raw Honey Cake",
    "honey_cake": "Honey Cake", "cabbage_soup": "Cabbage Soup", "vegetable_pottage": "Vegetable Pottage",
    "fried_eggs_and_onion": "Fried Eggs and Onion", "raw_sausage": "Raw Sausage", "sausage": "Sausage", "sauerkraut": "Sauerkraut",
    "cheese": "Cheese",
}
for item, en in DISHES.items():
    lang[f"item.mechanical.{item}"] = en
flat_item("herb_stuffed_fish")
COOKWARE = {"clay_cooking_pot": "Clay Cooking Pot", "bronze_griddle": "Bronze Griddle", "iron_skillet": "Iron Skillet",
            "fermenting_barrel": "Fermenting Barrel", "cheese_press": "Cheese Press", "salt_pan": "Salt Pan"}
for block, en in COOKWARE.items():
    lang[f"block.mechanical.{block}"] = en
    if block != "clay_cooking_pot":  # the pot comes back from its block entity (TripodBlockEntity keeps it as the vessel)
        loot(block, [{"rolls": 1, "condition": {"type": "minecraft:survives_explosion"}, "entries": [entry(S + block)]}])
loot("clay_cooking_pot", [])
for item, en in {"unfired_clay_cooking_pot": "Unfired Clay Cooking Pot", "iron_cauldron": "Iron Cauldron", "bronze_ladle": "Bronze Ladle"}.items():
    lang[f"item.mechanical.{item}"] = en
tag(BT / "mineable/axe.json", S + "fermenting_barrel", S + "cheese_press")
tag(BT / "mineable/pickaxe.json", S + "bronze_griddle", S + "iron_skillet", S + "salt_pan", S + "clay_cooking_pot")

C = S + "copper_coin"
R = D / "mechanical/recipe"


def shaped(name, pattern, key, result, count=1, category="misc"):
    w(R / f"{name}.json", {"type": "minecraft:crafting_shaped", "category": category, "pattern": pattern, "key": key,
                           "result": {"id": result, "count": count} if count != 1 else {"id": result}})


def shapeless(name, ingredients, result, count=1):
    w(R / f"{name}.json", {"type": "minecraft:crafting_shapeless", "category": "misc", "ingredients": ingredients,
                           "result": {"id": result, "count": count} if count != 1 else {"id": result}})


shaped("unfired_clay_cooking_pot", ["C C", "C C", "CCC"], {"C": M + "clay_ball"}, S + "unfired_clay_cooking_pot")  # fired in the Clay Kiln
shaped("bronze_griddle", ["SPS", " S "], {"P": S + "bronze_plate", "S": M + "stick"}, S + "bronze_griddle")
shaped("bronze_ladle", [" B", "S "], {"B": S + "bronze_ingot", "S": M + "stick"}, S + "bronze_ladle", category="equipment")
shaped("iron_skillet", ["P  ", " RR"], {"P": S + "iron_plate", "R": S + "wrought_iron_ingot"}, S + "iron_skillet")
shaped("iron_cauldron", ["P P", "P P", "PRP"], {"P": S + "iron_plate", "R": S + "wrought_iron_ingot"}, S + "iron_cauldron")
shaped("fermenting_barrel", ["WIW", "W W", "WWW"], {"W": "#minecraft:planks", "I": S + "iron_plate"}, S + "fermenting_barrel")
shaped("cheese_press", ["LRL", "S S", "WWW"], {"L": "#minecraft:logs", "R": S + "wrought_iron_ingot", "S": M + "stick", "W": "#minecraft:planks"},
       S + "cheese_press")
shaped("salt_pan", ["B B", "BBB"], {"B": M + "brick"}, S + "salt_pan")
shapeless("raw_honey_cake", [S + "flour", [S + "wild_honey", M + "honey_bottle"], "#mechanical:eggs"], S + "raw_honey_cake", 2)
shapeless("raw_sausage", ["#mechanical:smokable_meat", "#mechanical:smokable_meat", S + "lard", S + "salt", [S + "garlic", S + "wild_garlic"]],
          S + "raw_sausage", 3)
shapeless("herb_stuffed_fish", ["#mechanical:whole_fish", "#mechanical:culinary_herbs"], S + "herb_stuffed_fish")
w(R / "herb_roasted_fish_from_campfire_cooking.json", {"type": "minecraft:campfire_cooking", "category": "food", "cookingtime": 600,
                                                      "experience": 0.2, "ingredient": S + "herb_stuffed_fish", "result": {"id": S + "herb_roasted_fish"}})

# ---------------------------------------------------------------- tags
tag(MIT / "culinary_herbs.json", *(S + i for i in ("thyme", "marjoram", "sage_leaves", "rosemary_sprigs", "dill", "sorrel_leaves", "mint_leaves",
                                                   "wild_garlic", "garlic")))
tag(MIT / "spices.json", "#mechanical:culinary_herbs", *(S + i for i in ("black_pepper", "cinnamon_bark", "ginger_root", "saffron", "mustard_seeds")))
tag(MIT / "grains.json", S + "rice")
tag(MIT / "eggs.json", M + "egg", M + "brown_egg", M + "blue_egg", "#mechanical:bird_eggs")  # any egg for cooking
tag(MIT / "compostable.json", *(S + i for i in [f"wild_{v}" for v in VEG] + list(VEG) + ["broad_beans", "dill", "yarrow_leaves", "sorrel_leaves",
                                                                                         "chamomile_flowers", "mint_leaves", "hop_cones"]))
tag(MIT / "cellar_produce.json", *(S + i for i in ["onion", "garlic", "cabbage", "leek", "parsnip", "celery", "turnip", "lettuce", "wild_onion",
                                                   "wild_garlic", "wild_cabbage", "wild_leek", "wild_parsnip", "wild_celery", "wild_turnip",
                                                   "wild_lettuce", "ginger_root"]))
# shelf lives (Preserving.shelfLife): leaves wilt in days, roots and bulbs keep, dried and pickled things keep long
tag(MIT / "perishes_2_days.json", *(S + i for i in ("lettuce", "wild_lettuce", "sorrel_leaves", "mint_leaves", "wild_cabbage")))
tag(MIT / "perishes_10_days.json", *(S + i for i in ("cabbage", "leek", "celery", "wild_leek", "wild_celery", "wild_garlic", "broad_beans")))
tag(MIT / "perishes_20_days.json", *(S + i for i in ("onion", "garlic", "parsnip", "turnip", "wild_onion", "wild_parsnip", "wild_turnip", "ginger_root")))
tag(MIT / "perishes_1_day.json", *(S + i for i in ("nettle_soup", "root_pottage", "berry_mash", "herbal_tea", "pea_soup", "onion_garlic_stew",
                                                   "spiced_porridge", "tea", "cabbage_soup", "vegetable_pottage", "fried_eggs_and_onion",
                                                   "herb_stuffed_fish", "raw_sausage", "raw_honey_cake")))
tag(MIT / "perishes_3_days.json", *(S + i for i in ("herb_roasted_fish", "flatbread", "honey_cake", "sausage")))
tag(MIT / "perishes_30_days.json", *(S + i for i in ("sauerkraut", "cheese", "elderberry_syrup", "sloe_jam")))

# ---------------------------------------------------------------- phase 6: the Steel Age kitchen, and the phase 4-5 leftovers
# (Kitchen.DISHES rows; models herbs_cookware.py; textures gen_cookware_textures.py)
STEEL_DISHES = {
    "roast_with_herbs": "Roast with Herbs", "cider": "Cider", "ale": "Ale",
    "kitchen_pepper": "Kitchen Pepper", "sweet_spice": "Sweet Spice", "golden_spice": "Golden Spice", "pickling_spice": "Pickling Spice",
    "herb_salt": "Herb Salt", "fine_flour": "Fine Flour", "sea_buckthorn_juice": "Sea Buckthorn Juice", "saffron_rice": "Saffron Rice",
    "peppered_steak": "Peppered Steak", "spiced_pilaf": "Spiced Pilaf", "pickled_vegetables": "Pickled Vegetables",
    "raw_gingerbread": "Raw Gingerbread", "gingerbread": "Gingerbread", "steel_knife_set": "Steel Knife Set",
}
PIES = {"apple": ("Apple Pie", S + "crab_apple"), "plum": ("Plum Pie", S + "wild_plums"), "rowan": ("Rowan Pie", S + "rowan_berries"),
        "bird_cherry": ("Bird Cherry Pie", S + "bird_cherries")}
for f, (en, fruit) in PIES.items():
    STEEL_DISHES[f"{f}_pie"] = en
    STEEL_DISHES[f"raw_{f}_pie"] = "Raw " + en
    # the fruit of the fruit trees, flour and lard; baked in the Clay Kiln (KilnBlockEntity.RECIPES)
    shapeless(f"raw_{f}_pie", [S + "flour", S + "flour", S + "lard", fruit, fruit, fruit], S + f"raw_{f}_pie")
for item, en in STEEL_DISHES.items():
    lang[f"item.mechanical.{item}"] = en
for block, en in {"steel_pan": "Steel Pan", "mortar_and_pestle": "Mortar and Pestle", "sieve": "Sieve"}.items():
    lang[f"block.mechanical.{block}"] = en
    loot(block, [{"rolls": 1, "condition": {"type": "minecraft:survives_explosion"}, "entries": [entry(S + block)]}])
tag(BT / "mineable/pickaxe.json", S + "steel_pan", S + "mortar_and_pestle")
tag(BT / "mineable/axe.json", S + "sieve")
shaped("steel_pan", ["IPI", "I I"], {"P": S + "steel_plate", "I": S + "steel_ingot"}, S + "steel_pan")
shaped("mortar_and_pestle", ["  S", "M M", " M "], {"M": S + "marble", "S": S + "steel_ingot"}, S + "mortar_and_pestle")
shaped("sieve", ["SMS", "W W", "WWW"], {"S": M + "stick", "M": S + "steel_plate", "W": "#minecraft:planks"}, S + "sieve")
shapeless("steel_knife_set", [S + "steel_knife", S + "steel_knife_blade", S + "steel_knife_blade", M + "leather", M + "stick"], S + "steel_knife_set")
shapeless("raw_gingerbread", [S + "fine_flour", S + "fine_flour", [S + "wild_honey", M + "honey_bottle"], S + "sweet_spice", "#mechanical:eggs"],
          S + "raw_gingerbread", 4)
tag(MIT / "knives.json", S + "steel_knife_set")
tag(MIT / "spices.json", *(S + i for i in ("kitchen_pepper", "sweet_spice", "golden_spice", "pickling_spice", "herb_salt")))
tag(MIT / "perishes_1_day.json", *(S + i for i in ["saffron_rice", "spiced_pilaf", "peppered_steak", "raw_gingerbread"] + [f"raw_{f}_pie" for f in PIES]))
tag(MIT / "perishes_3_days.json", *(S + i for i in ["roast_with_herbs", "sea_buckthorn_juice"] + [f"{f}_pie" for f in PIES]))
tag(MIT / "perishes_10_days.json", S + "gingerbread")
tag(MIT / "perishes_30_days.json", *(S + i for i in ("cider", "ale", "pickled_vegetables", "fine_flour")))

# ---------------------------------------------------------------- lang: messages, trades
lang.update({
    "message.mechanical.clay_pot.no_fire": "A Clay Cooking Pot sits in the embers of a Campfire",
    "message.mechanical.clay_pot.not_a_dish": "%s makes no dish in a clay pot",
    "message.mechanical.cauldron.campfire": "The Iron Cauldron is too heavy for a campfire: hang it over a Fire Pit",
    "message.mechanical.ladle.no_bowl": "You need a Bowl to ladle into",
    "message.mechanical.pan.no_fire": "It stands over a Campfire",
    "message.mechanical.salt_pan.fresh": "Salt pans need seawater: build it by the sea",
    "message.mechanical.cookware.take_first": "Take out what is done first",
    "message.mechanical.cookware.cant_go_in": "%s makes nothing here",
    "message.mechanical.cookware.full": "It is full (%s)",
    "message.mechanical.cookware.empty": "It is empty: put ingredients in",
    "message.mechanical.cookware.working": "%s: %s:%s left",
    "message.mechanical.cookware.no_fire": "The fire under it is out",
    "message.mechanical.crop.paddy": "needs water beside it (a flooded paddy)",
})
w(A / "lang/en_us.json", lang, ascii=True)  # the chain keeps the lang file ascii-escaped

herbs_cookware.main()
print("herbs ok")
