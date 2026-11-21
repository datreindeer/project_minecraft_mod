"""Data for the meadow fauna (com.mechanical.wildlife.meadow.MeadowFauna): Wild Cat, Mole, Field Vole, Adder, Grass Snake.

- items: models for every item (meats, pelts, Moleskin, Snakeskin, Venom Gland, Snakebite Poultice, Wildcat Fur Cap,
  Snakeskin Boots, spawn eggs); equipment assets for the cap and the boots
- recipes: raw meat on the Campfire (20 s) or in the smoker; raw pelts fleshed with a knife in the grid (the knife stays);
  Wildcat Fur Cap, Snakeskin Boots, Poison Arrows (8 arrows round a Venom Gland), Snakebite Poultice, a Bundle sewn from
  Moleskin
- Molehill: blockstate, a three-tier mound model, loot (a little dirt)
- tags (added to the ones gen_preserving.py writes): raw meat perishes in 1 day, cooked in 3, both rot to flesh, raw meat
  is smokable/dryable; #mechanical:vole_food (what voles eat from chests); armour repair tags
- spawns: biome modifiers add_<mob>_meadow (vanilla biomes and the mod's Hazel Meadow, Deciduous Forest, Willow Fen,
  Pine Heath)
- lang (English)
Textures: scripts/gen_fauna_meadow_textures.py.
Generator order: ... gen_roots.py, gen_fauna_meadow.py, gen_block_items.py, gen_unlocks.py, gen_advancements.py, gen_registry.py.
Run: python scripts/gen_fauna_meadow.py"""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent / "src/main/resources"
A, D = ROOT / "assets/mechanical", ROOT / "data"
M = "mechanical:"


def w(path, obj):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, indent=2) + "\n", encoding="utf-8")


def merge(path, obj):
    old = json.loads(path.read_text(encoding="utf-8")) if path.exists() else {}
    old.update(obj)
    path.write_text(json.dumps(old, indent=2) + "\n", encoding="utf-8")


def tag_add(name, *values):
    path = D / f"mechanical/tags/item/{name}.json"
    old = json.loads(path.read_text(encoding="utf-8")) if path.exists() else {"values": []}
    old["values"] += [v for v in values if v not in old["values"]]
    w(path, old)


def flat_item(item):
    w(A / f"models/item/{item}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"mechanical:item/{item}"}})
    w(A / f"items/{item}.json", {"model": {"type": "minecraft:model", "model": f"mechanical:item/{item}"}})


def recipe(name, obj):
    w(D / f"mechanical/recipe/{name}.json", obj)


MOBS = {"wild_cat": "Wild Cat", "mole": "Mole", "field_vole": "Field Vole", "adder": "Adder", "grass_snake": "Grass Snake"}
# raw -> (cooked, raw name, cooked name)
MEATS = {"raw_wildcat": ("cooked_wildcat", "Raw Wildcat Meat", "Roast Wildcat Meat"),
         "raw_mole": ("cooked_mole", "Raw Mole", "Roast Mole"),
         "raw_vole": ("cooked_vole", "Raw Vole", "Roast Vole"),
         "raw_snake": ("cooked_snake", "Raw Snake Meat", "Roast Snake Meat")}
OTHER = {"raw_wildcat_pelt": "Raw Wildcat Pelt", "wildcat_pelt": "Wildcat Pelt", "raw_mole_pelt": "Raw Mole Pelt",
         "moleskin": "Moleskin", "snakeskin": "Snakeskin", "venom_gland": "Venom Gland",
         "snakebite_poultice": "Snakebite Poultice", "wildcat_fur_cap": "Wildcat Fur Cap", "snakeskin_boots": "Snakeskin Boots"}

lang = {"block.mechanical.molehill": "Molehill"}

# ---- items ----
for raw, (cooked, rn, cn) in MEATS.items():
    for item, name in ((raw, rn), (cooked, cn)):
        flat_item(item)
        lang[f"item.mechanical.{item}"] = name
    # only the campfire (and the smoker) cook food, like the scraps (gen_data.py)
    for kind, ticks in (("campfire_cooking", 400), ("smoking", 100)):
        recipe(f"{cooked}_from_{kind}", {"type": f"minecraft:{kind}", "category": "food", "cookingtime": ticks,
                                         "experience": 0.1, "ingredient": M + raw, "result": {"id": M + cooked}})
for item, name in OTHER.items():
    flat_item(item)
    lang[f"item.mechanical.{item}"] = name
for mob, name in MOBS.items():
    flat_item(f"{mob}_spawn_egg")
    lang[f"entity.mechanical.{mob}"] = name
    lang[f"item.mechanical.{mob}_spawn_egg"] = f"{name} Spawn Egg"

# equipment layers (textures/entity/equipment/humanoid/<name>.png)
for asset in ("wildcat_fur", "snakeskin"):
    w(A / f"equipment/{asset}.json", {"layers": {"humanoid": [{"texture": M + asset}]}})

# ---- crafting ----
# raw pelts: fleshed and scraped with a knife in the grid (like Raw Hide -> Scraped Hide); small pelts need no rack
for raw, cured in (("raw_wildcat_pelt", "wildcat_pelt"), ("raw_mole_pelt", "moleskin")):
    recipe(cured, {"type": "minecraft:crafting_shapeless", "category": "misc",
                   "ingredients": [M + raw, "#mechanical:knives"], "result": {"id": M + cured}})
recipe("wildcat_fur_cap", {"type": "minecraft:crafting_shapeless", "category": "equipment",
                           "ingredients": [M + "wildcat_pelt", M + "wildcat_pelt", M + "sinew"], "result": {"id": M + "wildcat_fur_cap"}})
recipe("snakeskin_boots", {"type": "minecraft:crafting_shapeless", "category": "equipment",
                           "ingredients": ["minecraft:leather_boots", M + "snakeskin", M + "snakeskin", M + "sinew"],
                           "result": {"id": M + "snakeskin_boots"}})
# soft, supple moleskin sewn into a pouch: the Leather Pouch (which replaces vanilla's Bundle)
recipe("leather_pouch_from_moleskin", {"type": "minecraft:crafting_shaped", "category": "equipment", "pattern": [" T ", "M M", " M "],
                                       "key": {"T": M + "twine", "M": M + "moleskin"}, "result": {"id": M + "leather_pouch"}})
# venom-tipped arrows (vanilla Arrow of Poison)
recipe("poison_arrows_from_venom_gland", {"type": "minecraft:crafting_shaped", "category": "equipment",
                                          "pattern": ["AAA", "AVA", "AAA"], "key": {"A": "minecraft:arrow", "V": M + "venom_gland"},
                                          "result": {"id": "minecraft:tipped_arrow", "count": 8,
                                                     "components": {"minecraft:potion_contents": {"potion": "minecraft:poison"}}}})
# a little venom against the venom, bound up in a leaf with lard: cures poison
recipe("snakebite_poultice", {"type": "minecraft:crafting_shapeless", "category": "misc",
                              "ingredients": [M + "venom_gland", M + "lard", M + "plant_fibre"],
                              "result": {"id": M + "snakebite_poultice", "count": 2}})

# ---- Molehill ----
FACES = ["down", "up", "north", "south", "east", "west"]


def box(f, t):
    return {"from": f, "to": t, "faces": {d: {"texture": "#soil", **({"cullface": "down"} if d == "down" else {})} for d in FACES}}


w(A / "models/block/molehill.json", {"parent": "minecraft:block/block", "ambientocclusion": False,
    "textures": {"particle": M + "block/molehill", "soil": M + "block/molehill"},
    "elements": [box([1, 0, 1], [15, 2, 15]), box([3, 2, 3], [13, 4, 13]), box([5, 4, 5], [11, 5, 11])]})
w(A / "blockstates/molehill.json", {"variants": {"": [{"model": M + "block/molehill"}, {"model": M + "block/molehill", "y": 90},
                                                       {"model": M + "block/molehill", "y": 180}, {"model": M + "block/molehill", "y": 270}]}})
w(D / "mechanical/loot_table/blocks/molehill.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [
    {"type": "minecraft:item", "name": "minecraft:dirt", "condition": {"type": "minecraft:random_chance", "chance": 0.5}}],
    "condition": {"type": "minecraft:survives_explosion"}}], "random_sequence": "mechanical:blocks/molehill"})

# ---- tags ----
RAW = [M + r for r in MEATS]
COOKED = [M + c for c, _, _ in MEATS.values()]
tag_add("perishes_1_day", *RAW)
tag_add("perishes_3_days", *COOKED)
tag_add("rots_to_flesh", *RAW, *COOKED)
tag_add("smokable_meat", *RAW)
w(D / "mechanical/tags/item/vole_food.json", {"values": ["#mechanical:grains", "#mechanical:sheaves", "minecraft:bread",
                                                       "minecraft:beetroot_seeds"]})
w(D / "mechanical/tags/item/repairs_snakeskin.json", {"values": [M + "snakeskin"]})
w(D / "mechanical/tags/item/repairs_wildcat_fur.json", {"values": [M + "wildcat_pelt"]})

# ---- spawns: (weight, min, max), biomes ----
PLAINS = ["minecraft:plains", "minecraft:sunflower_plains", "minecraft:meadow", "mechanical:hazel_meadow"]
SPAWNS = {
    # shy forest-edge hunter: one, or a mother and kitten
    "wild_cat": ((2, 1, 2), ["minecraft:forest", "minecraft:flower_forest", "minecraft:birch_forest", "minecraft:meadow",
                             "mechanical:deciduous_forest", "mechanical:hazel_meadow"]),
    "mole": ((4, 1, 1), PLAINS + ["minecraft:forest", "minecraft:flower_forest", "mechanical:deciduous_forest"]),
    "field_vole": ((8, 2, 4), PLAINS + ["minecraft:savanna"]),
    "adder": ((6, 1, 2), ["mechanical:pine_heath"]),
    "adder_open": ((3, 1, 1), ["minecraft:meadow", "minecraft:windswept_hills", "minecraft:windswept_gravelly_hills",
                               "minecraft:plains", "mechanical:hazel_meadow"]),
    "grass_snake": ((5, 1, 1), ["minecraft:swamp", "mechanical:willow_fen", "minecraft:river"]),
    "grass_snake_meadow": ((2, 1, 1), ["minecraft:meadow", "mechanical:hazel_meadow", "minecraft:flower_forest"]),
}
for name, ((wt, lo, hi), biomes) in SPAWNS.items():
    mob = name.removesuffix("_open").removesuffix("_meadow")
    w(D / f"mechanical/neoforge/biome_modifier/add_{name}_meadow.json" if not name.endswith("_meadow") else
      D / f"mechanical/neoforge/biome_modifier/add_{name}.json",
      {"type": "neoforge:add_spawns", "biomes": biomes,
       "spawners": [{"type": M + mob, "weight": wt, "count": {"type": "minecraft:uniform", "min_inclusive": lo, "max_inclusive": hi}}]})

merge(A / "lang/en_us.json", lang)
print("fauna meadow ok")
