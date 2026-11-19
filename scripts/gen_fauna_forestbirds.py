"""Data for the forest birds (ForestBirdsFauna: Capercaillie, Great Bustard, Owl, Raven, Golden Eagle, Vulture).

- items: models for meats, plucked birds, eggs, feathers, parts, arrows, Bone Flute, Raven Talisman, spawn eggs; lang
- loot tables: owl, raven, eagle and vulture drop feathers and their parts (no carcass); capercaillie and bustard leave a
  carcass (Butchery.register in ForestBirdsFauna), their tables are only a fallback
- recipes: raw meat on the Campfire or in the smoker; a Plucked bird roasts whole or is cut up with a knife; eggs boil
  into Boiled Eggs; Owl Pellet -> Bone Meal; Raven Talisman; Bone Flute (Fletching-Table arrows are in FletchingRecipes)
- tags: spoilage, smokable meat, bird eggs, arrows, silent arrows, raven scraps; entity tags owl_prey / eagle_prey
- spawns (biome modifiers, own files) and ground nests for capercaillie (pine forest) and bustard (plains), added to the
  Bird Nest's blockstate and loot table that gen_birds2.py writes
Textures and model geometry: scripts/gen_fauna_forestbirds_textures.py. Docs: docs/fauna/forestbirds.md.
Generator order: ... gen_birds2.py, gen_kettle.py, gen_roots.py, gen_fauna_forestbirds.py, gen_block_items.py ...
Run: python scripts/gen_fauna_forestbirds.py
"""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent / "src/main/resources"
A, D = ROOT / "assets/mechanical", ROOT / "data"
M = "mechanical:"


def w(path, obj):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, indent=2) + "\n", encoding="utf-8")


def read(path, default):
    return json.loads(path.read_text(encoding="utf-8")) if path.exists() else default


def merge(path, obj):
    old = read(path, {})
    old.update(obj)
    path.write_text(json.dumps(old, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")


def tag_add(path, *values):
    old = read(path, {"values": []})
    old["values"] += [v for v in values if v not in old["values"]]
    w(path, old)


def item_tag(name, *values):
    ns, path = name.split(":") if ":" in name else ("mechanical", name)
    tag_add(D / f"{ns}/tags/item/{path}.json", *values)


def flat_item(item):
    w(A / f"models/item/{item}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"mechanical:item/{item}"}})
    w(A / f"items/{item}.json", {"model": {"type": "minecraft:model", "model": f"mechanical:item/{item}"}})


lang = {}
NAMES = {
    "capercaillie": "Capercaillie", "great_bustard": "Great Bustard", "owl": "Eagle-Owl", "raven": "Raven",
    "golden_eagle": "Golden Eagle", "vulture": "Griffon Vulture",
}
ITEMS = {
    "raw_capercaillie": "Raw Capercaillie", "cooked_capercaillie": "Cooked Capercaillie", "plucked_capercaillie": "Plucked Capercaillie",
    "capercaillie_egg": "Capercaillie Egg", "capercaillie_tail_feather": "Capercaillie Tail Feather",
    "raw_bustard": "Raw Bustard", "cooked_bustard": "Cooked Bustard", "plucked_bustard": "Plucked Bustard", "bustard_egg": "Bustard Egg",
    "owl_feather": "Owl Feather", "owl_pellet": "Owl Pellet", "raven_feather": "Raven Feather", "raven_talisman": "Raven Talisman",
    "eagle_feather": "Eagle Feather", "eagle_talon": "Eagle Talon", "vulture_wing_bone": "Vulture Wing Bone", "bone_flute": "Bone Flute",
    "eagle_fletched_arrow": "Eagle-Fletched Arrow", "silent_arrow": "Silent Arrow",
}
for item, name in ITEMS.items():
    flat_item(item)
    lang[f"item.mechanical.{item}"] = name
for mob, name in NAMES.items():
    flat_item(f"{mob}_spawn_egg")
    lang[f"entity.mechanical.{mob}"] = name
    lang[f"item.mechanical.{mob}_spawn_egg"] = f"{name} Spawn Egg"
lang["item.mechanical.raven_talisman.lore"] = "Carried, the dark wears on the mind more slowly"
lang["item.mechanical.bone_flute.lore"] = "Play it: everyone near feels braver in the dark"

# ---- loot: (item, min, max) rolls; feathers for all
def count(lo, hi):
    return {"type": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": lo, "max": hi}}


LOOT = {
    "owl": [("minecraft:feather", 0, 1), (M + "owl_feather", 1, 3)],
    "raven": [("minecraft:feather", 0, 1), (M + "raven_feather", 1, 3)],
    "golden_eagle": [("minecraft:feather", 1, 2), (M + "eagle_feather", 1, 3), (M + "eagle_talon", 0, 2)],
    "vulture": [("minecraft:feather", 2, 4), (M + "vulture_wing_bone", 1, 1)],
    # fallback only (they leave a carcass): feathers and meat
    "capercaillie": [("minecraft:feather", 2, 4), (M + "raw_capercaillie", 1, 2)],
    "great_bustard": [("minecraft:feather", 3, 5), (M + "raw_bustard", 2, 3)],
}
for mob, drops in LOOT.items():
    w(D / f"mechanical/loot_table/entities/{mob}.json", {"type": "minecraft:entity", "pools": [
        {"rolls": 1, "entries": [{"type": "minecraft:item", "name": i, "modifier": [count(lo, hi)]}]} for i, lo, hi in drops],
        "random_sequence": f"mechanical:entities/{mob}"})

# ---- food recipes (like gen_birds2.py): bird -> (raw, cooked, meat per plucked bird, boiled eggs per egg)
FOOD = {"capercaillie": 2, "bustard": 3}
for bird, n in FOOD.items():
    raw, cooked, plucked = M + f"raw_{bird}", M + f"cooked_{bird}", M + f"plucked_{bird}"
    for kind, ticks in (("campfire_cooking", 400), ("smoking", 100)):
        w(D / f"mechanical/recipe/cooked_{bird}_from_{kind}.json", {"type": f"minecraft:{kind}", "category": "food",
            "cookingtime": ticks, "experience": 0.1, "ingredient": raw, "result": {"id": cooked}})
    w(D / f"mechanical/recipe/cooked_{bird}_from_plucked_{bird}.json", {"type": "minecraft:campfire_cooking", "category": "food",
        "cookingtime": 600 + 100 * (n - 2), "experience": 0.15, "ingredient": plucked, "result": {"id": cooked, "count": n}})
    w(D / f"mechanical/recipe/raw_{bird}_from_plucked_{bird}.json", {"type": "minecraft:crafting_shapeless", "category": "misc",
        "ingredients": [plucked, "#mechanical:knives"], "result": {"id": raw, "count": n}})
for egg, boiled in (("capercaillie_egg", 1), ("bustard_egg", 2)):
    w(D / f"mechanical/recipe/boiled_egg_from_{egg}.json", {"type": "minecraft:campfire_cooking", "category": "food",
        "cookingtime": 300, "experience": 0.1, "ingredient": M + egg, "result": {"id": M + "boiled_egg", "count": boiled}})

# ---- crafting
w(D / "mechanical/recipe/bone_meal_from_owl_pellet.json", {"type": "minecraft:crafting_shapeless", "category": "misc",
    "ingredients": [M + "owl_pellet"], "result": {"id": "minecraft:bone_meal", "count": 2}})
w(D / "mechanical/recipe/raven_talisman.json", {"type": "minecraft:crafting_shaped", "category": "equipment",
    "pattern": [" T ", "FFF", " B "], "key": {"T": M + "twine", "F": M + "raven_feather", "B": "minecraft:bone"},
    "result": {"id": M + "raven_talisman"}})
w(D / "mechanical/recipe/bone_flute.json", {"type": "minecraft:crafting_shapeless", "category": "misc",
    "ingredients": [M + "vulture_wing_bone", "#mechanical:knives"], "result": {"id": M + "bone_flute"}})

# ---- tags
RAW, COOKED = [M + "raw_capercaillie", M + "raw_bustard"], [M + "cooked_capercaillie", M + "cooked_bustard"]
PLUCKED = [M + "plucked_capercaillie", M + "plucked_bustard"]
item_tag("perishes_1_day", *RAW, *PLUCKED)
item_tag("perishes_3_days", *COOKED)
item_tag("rots_to_flesh", *RAW, *PLUCKED, *COOKED)
item_tag("smokable_meat", *RAW)
item_tag("bird_eggs", M + "capercaillie_egg", M + "bustard_egg")
item_tag("minecraft:arrows", M + "eagle_fletched_arrow", M + "silent_arrow")
item_tag("silent_arrows", M + "silent_arrow")
item_tag("raven_scraps", "#minecraft:meat", "#mechanical:smokable_meat")
ET = D / "mechanical/tags/entity_type"
tag_add(ET / "owl_prey.json", M + "squirrel", M + "hare", M + "hedgehog", M + "ptarmigan", "minecraft:rabbit")
tag_add(ET / "eagle_prey.json", M + "hare", M + "desert_hare", M + "squirrel", M + "ptarmigan", M + "rock_dove", M + "capercaillie",
        "minecraft:rabbit", "minecraft:fox")

# ---- spawns: (weight, min, max), biomes
DF, HM, WF, PH = (M + b for b in ("deciduous_forest", "hazel_meadow", "willow_fen", "pine_heath"))
TAIGA = ["minecraft:taiga", "minecraft:old_growth_pine_taiga", "minecraft:old_growth_spruce_taiga", "minecraft:snowy_taiga"]
SPAWNS = {
    "capercaillie": ((7, 1, 3), TAIGA + ["minecraft:windswept_forest", PH]),
    "great_bustard": ((5, 2, 4), ["minecraft:plains", "minecraft:sunflower_plains", "minecraft:savanna", HM]),
    "owl": ((3, 1, 1), ["minecraft:forest", "minecraft:dark_forest", "minecraft:birch_forest", "minecraft:old_growth_birch_forest",
                        "minecraft:taiga", "minecraft:old_growth_pine_taiga", "minecraft:old_growth_spruce_taiga", DF, PH, WF]),
    "raven": ((4, 2, 3), ["minecraft:taiga", "minecraft:snowy_taiga", "minecraft:old_growth_pine_taiga", "minecraft:windswept_hills",
                          "minecraft:windswept_forest", "minecraft:grove", "minecraft:meadow", "minecraft:stony_peaks", PH]),
    "golden_eagle": ((2, 1, 2), ["minecraft:windswept_hills", "minecraft:windswept_gravelly_hills", "minecraft:windswept_forest",
                                 "minecraft:stony_peaks", "minecraft:meadow", "minecraft:grove", "minecraft:snowy_slopes"]),
    "vulture": ((4, 2, 4), ["minecraft:savanna", "minecraft:savanna_plateau", "minecraft:windswept_savanna", "minecraft:badlands",
                            "minecraft:eroded_badlands", "minecraft:wooded_badlands", "minecraft:windswept_hills"]),
}
for mob, ((wt, lo, hi), biomes) in SPAWNS.items():
    w(D / f"mechanical/neoforge/biome_modifier/add_{mob}.json", {"type": "neoforge:add_spawns", "biomes": biomes,
        "spawners": [{"type": M + mob, "weight": wt, "count": {"type": "minecraft:uniform", "min_inclusive": lo, "max_inclusive": hi}}]})

# ---- ground nests: the Bird Nest (BirdNestBlock.Bird CAPERCAILLIE / BUSTARD), claimed, 0-2 eggs
FACES = ["down", "up", "north", "south", "east", "west"]


def box(f, t, tex, rot):
    return {"from": f, "to": t, "faces": {d: {"texture": tex} for d in FACES}, "rotation": rot}


EGG_SIZE = {"capercaillie": (2.5, 2.5, 3), "bustard": (3, 3, 4)}
SPOTS = [(5.5, 5.5, 20), (8.5, 7, -25), (6, 9, 45)]  # as gen_birds2.py
for bird, (ew, eh, ed) in EGG_SIZE.items():
    for n in range(1, 4):
        eggs = [box([x, 1, z], [x + ew, 1 + eh, z + ed], "#egg", {"origin": [x + ew / 2, 1, z + ed / 2], "axis": "y",
                                                                  "angle": 22.5 if angle > 0 else -22.5}) for x, z, angle in SPOTS[:n]]
        w(A / f"models/block/bird_nest_eggs_{bird}_{n}.json", {"textures": {"particle": f"mechanical:block/bird_egg_{bird}",
            "egg": f"mechanical:block/bird_egg_{bird}"}, "elements": eggs})
bs_path = A / "blockstates/bird_nest.json"
bs = read(bs_path, None)
assert bs, "run gen_birds2.py first"
for bird in EGG_SIZE:
    for n in range(1, 4):
        part = {"when": {"bird": bird, "eggs": str(n)}, "apply": {"model": f"mechanical:block/bird_nest_eggs_{bird}_{n}"}}
        if part not in bs["multipart"]:
            bs["multipart"].append(part)
w(bs_path, bs)
loot_path = D / "mechanical/loot_table/blocks/bird_nest.json"
loot = read(loot_path, None)
egg_pool = loot["pools"][1]["entries"]
for bird, egg in (("capercaillie", "capercaillie_egg"), ("bustard", "bustard_egg")):
    for n in range(1, 4):
        e = {"type": "minecraft:item", "name": M + egg,
             "condition": {"type": "minecraft:match_block", "blocks": M + "bird_nest", "state": {"bird": bird, "eggs": str(n)}}}
        if n > 1:
            e["modifier"] = [{"type": "minecraft:set_count", "count": n}]
        if e not in egg_pool:
            egg_pool.append(e)
w(loot_path, loot)
# world generation: (biomes, ground under the nest, rarity 1 in n chunks)
NESTS = {
    "capercaillie": (TAIGA + [PH], ["minecraft:grass_block", "minecraft:podzol", "minecraft:coarse_dirt", "minecraft:moss_block"], 14),
    "bustard": (["minecraft:plains", "minecraft:sunflower_plains", HM], ["minecraft:grass_block"], 18),
}
for bird, (biomes, ground, rarity) in NESTS.items():
    name = f"{bird}_nest"
    state = lambda eggs, bird=bird: {"id": M + "bird_nest", "properties": {"bird": bird, "eggs": str(eggs), "robbed": "0"}}
    w(D / f"mechanical/worldgen/feature/{name}.json", {"type": "minecraft:simple_block", "to_place": {
        "type": "minecraft:weighted", "entries": [{"data": state(0), "weight": 2}, {"data": state(1), "weight": 2}, {"data": state(2), "weight": 1}]}})
    preds = [{"type": "minecraft:matching_block_tag", "tag": "minecraft:air"},
             {"type": "minecraft:matching_blocks", "blocks": ground, "offset": [0, -1, 0]}]
    w(D / f"mechanical/worldgen/placed_feature/{name}.json", {"feature": M + name, "placement": [
        {"type": "minecraft:rarity_filter", "chance": rarity}, {"type": "minecraft:in_square"},
        {"type": "minecraft:heightmap", "heightmap": "WORLD_SURFACE_WG"}, {"type": "minecraft:biome"},
        {"type": "minecraft:block_predicate_filter", "predicate": {"type": "minecraft:all_of", "predicates": preds}}]})
    w(D / f"mechanical/neoforge/biome_modifier/add_{name}.json", {"type": "neoforge:add_features", "biomes": biomes,
        "features": M + name, "step": "vegetal_decoration"})

merge(A / "lang/en_us.json", lang)
print("fauna forestbirds ok")
