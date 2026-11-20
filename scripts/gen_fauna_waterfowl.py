"""Data for the waterfowl (wildlife/waterfowl: Mallard, Mute Swan, Grey Heron, Common Crane).

- items: models for the meats, plucked birds, eggs, parts (Down, Swan Quill, Heron Plume, Heron Bill, Hollow Bird Bone),
  Bone Flute, Plume Headdress (+ its equipment asset and repair tag) and spawn eggs; English lang
- recipes: raw meat on the Campfire (20 s) or in the smoker; a Plucked bird roasts whole (30 s) or is cut up with a knife;
  eggs boil into Boiled Eggs (swan egg: 3, crane egg: 2); with a knife: Swan Quill -> 4 Feathers, Hollow Bird Bone ->
  Bone Flute or 2 Bone Needles, Heron Bill -> 2 Bone Fish Hooks; Plume Headdress from plumes, a quill, Down and Leather
- tags: perish/rot/smoke tags like gen_birds2.py, #mechanical:bird_eggs
- spawns (biome modifiers, also the mod's Willow Fen, Pine Heath, Hazel Meadow, Deciduous Forest), fallback entity loot
- nests: wild Bird Nests claimed by each bird (worldgen), egg models in the nest, added to the bird_nest blockstate and
  loot table that gen_birds2.py writes (so this runs after it)
Textures and models: scripts/gen_fauna_waterfowl_textures.py. Run: python scripts/gen_fauna_waterfowl.py
"""
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
    path.write_text(json.dumps(old, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")


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


def shapeless(name, ingredients, result, count=1, category="misc"):
    recipe(name, {"type": "minecraft:crafting_shapeless", "category": category, "ingredients": ingredients,
                  "result": {"id": result, "count": count}})


ENTITIES = {"mallard": "Mallard", "mute_swan": "Mute Swan", "grey_heron": "Grey Heron", "common_crane": "Common Crane"}
# meat id -> (name, meat per bird); keep in step with WaterfowlFauna's Butchery yields (plucked = average meat)
MEATS = {"mallard": ("Mallard", 1), "swan": ("Swan", 3), "heron": ("Heron", 1), "crane": ("Crane", 2)}
EGGS = {"mallard": 1, "swan": 3, "heron": 1, "crane": 2}  # boiled eggs per egg
PARTS = {"down": "Down", "swan_quill": "Swan Quill", "heron_plume": "Heron Plume", "heron_bill": "Heron Bill",
         "hollow_bird_bone": "Hollow Bird Bone", "bone_flute": "Bone Flute", "plume_headdress": "Plume Headdress"}

lang = {}
for e, name in ENTITIES.items():
    lang[f"entity.mechanical.{e}"] = name
    lang[f"item.mechanical.{e}_spawn_egg"] = f"{name} Spawn Egg"
    flat_item(f"{e}_spawn_egg")
for p, name in PARTS.items():
    flat_item(p)
    lang[f"item.mechanical.{p}"] = name

# ---- meat, plucked birds and eggs ----
for bird, (name, n) in MEATS.items():
    raw, cooked, plucked = f"raw_{bird}", f"cooked_{bird}", f"plucked_{bird}"
    for item in (raw, cooked, plucked):
        flat_item(item)
    lang |= {f"item.mechanical.{raw}": f"Raw {name}", f"item.mechanical.{cooked}": f"Cooked {name}",
             f"item.mechanical.{plucked}": f"Plucked {name}"}
    for kind, ticks in (("campfire_cooking", 400), ("smoking", 100)):
        recipe(f"{cooked}_from_{kind}", {"type": f"minecraft:{kind}", "category": "food", "cookingtime": ticks,
                                         "experience": 0.1, "ingredient": M + raw, "result": {"id": M + cooked}})
    recipe(f"{cooked}_from_{plucked}", {"type": "minecraft:campfire_cooking", "category": "food", "cookingtime": 600,
                                        "experience": 0.15, "ingredient": M + plucked, "result": {"id": M + cooked, "count": n}})
    shapeless(f"{raw}_from_{plucked}", [M + plucked, "#mechanical:knives"], M + raw, n)
for bird, boiled in EGGS.items():
    egg = f"{bird}_egg"
    flat_item(egg)
    lang[f"item.mechanical.{egg}"] = f"{MEATS[bird][0]} Egg"
    recipe(f"boiled_egg_from_{egg}", {"type": "minecraft:campfire_cooking", "category": "food", "cookingtime": 300,
                                      "experience": 0.1, "ingredient": M + egg, "result": {"id": M + "boiled_egg", "count": boiled}})

# ---- what the parts make ----
shapeless("feather_from_swan_quill", [M + "swan_quill", "#mechanical:knives"], "minecraft:feather", 4)
shapeless("bone_flute", [M + "hollow_bird_bone", "#mechanical:knives"], M + "bone_flute", 1, "equipment")
shapeless("bone_needle_from_hollow_bird_bone", [M + "hollow_bird_bone", "minecraft:flint"], M + "bone_needle", 2, "equipment")
shapeless("bone_fish_hook_from_heron_bill", [M + "heron_bill", "#mechanical:knives"], M + "bone_fish_hook", 2, "equipment")
recipe("plume_headdress", {"type": "minecraft:crafting_shaped", "category": "equipment", "pattern": ["PQP", "LDL"],
                           "key": {"P": M + "heron_plume", "Q": [M + "swan_quill", M + "heron_plume"], "D": M + "down",
                                   "L": "minecraft:leather"}, "result": {"id": M + "plume_headdress"}})
w(A / "equipment/plume_headdress.json", {"layers": {"humanoid": [{"texture": "mechanical:plume_headdress"}]}})
w(D / "mechanical/tags/item/repairs_plume_headdress.json", {"values": [M + "heron_plume"]})

# ---- tags ----
RAW = [M + f"raw_{b}" for b in MEATS]
tag_add("perishes_1_day", *RAW, *[M + f"plucked_{b}" for b in MEATS])
tag_add("perishes_3_days", *[M + f"cooked_{b}" for b in MEATS])
tag_add("rots_to_flesh", *RAW, *[M + f"plucked_{b}" for b in MEATS], *[M + f"cooked_{b}" for b in MEATS])
tag_add("smokable_meat", *RAW)
tag_add("bird_eggs", *[M + f"{b}_egg" for b in EGGS])

# ---- spawns: (weight, min, max) per biome list; spawn rules (WaterfowlFauna) keep them on water and banks ----
FEN, HEATH, MEADOW, DECID = M + "willow_fen", M + "pine_heath", M + "hazel_meadow", M + "deciduous_forest"
SPAWNS = {
    "mallard": [((10, 3, 6), ["minecraft:river", "minecraft:swamp", FEN]), ((4, 2, 4), ["minecraft:mangrove_swamp"]),
                ((2, 2, 4), ["minecraft:plains", "minecraft:forest", "minecraft:birch_forest", "minecraft:meadow", DECID, MEADOW])],
    "mute_swan": [((3, 2, 2), ["minecraft:river", FEN]), ((2, 2, 2), ["minecraft:swamp"]), ((1, 2, 2), ["minecraft:plains"])],
    "grey_heron": [((3, 1, 1), ["minecraft:river", "minecraft:swamp", "minecraft:mangrove_swamp"]), ((4, 1, 2), [FEN]),
                   ((1, 1, 1), ["minecraft:beach"])],
    "common_crane": [((8, 3, 6), [FEN]), ((4, 2, 4), ["minecraft:swamp", HEATH]),
                     ((2, 3, 5), ["minecraft:plains", "minecraft:meadow", MEADOW])],
}
for mob, entries in SPAWNS.items():
    for i, ((wt, lo, hi), biomes) in enumerate(entries):
        w(D / f"mechanical/neoforge/biome_modifier/add_{mob}{'' if i == 0 else f'_{i + 1}'}.json", {
            "type": "neoforge:add_spawns", "biomes": biomes,
            "spawners": [{"type": M + mob, "weight": wt, "count": {"type": "minecraft:uniform", "min_inclusive": lo, "max_inclusive": hi}}]})
    # a bird that somehow dies without leaving a carcass (none should) still drops its feathers
    w(D / f"mechanical/loot_table/entities/{mob}.json", {"type": "minecraft:entity", "pools": [{"rolls": 1, "entries": [
        {"type": "minecraft:item", "name": "minecraft:feather",
         "modifier": [{"type": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": 1, "max": 3}}]}]}],
        "random_sequence": f"mechanical:entities/{mob}"})

# ---- nests: egg models, blockstate and loot (added to gen_birds2.py's), wild nests ----
FACES = ["down", "up", "north", "south", "east", "west"]


def box(f, t, rot):
    return {"from": f, "to": t, "faces": {d: {"texture": "#egg"} for d in FACES}, "rotation": rot}


NEST_EGGS = {"mallard": ((2.5, 2.5, 3), M + "mallard_egg"), "mute_swan": ((3.5, 3.5, 4.5), M + "swan_egg"),
             "grey_heron": ((2.5, 2.5, 3), M + "heron_egg"), "common_crane": ((3, 3, 4), M + "crane_egg")}
SPOTS = [(5.5, 5.5, 20), (8.5, 7, -25), (6, 9, 45)]
for bird, ((ew, eh, ed), _) in NEST_EGGS.items():
    for n in range(1, 4):
        eggs = [box([x, 1, z], [x + ew, 1 + eh, z + ed], {"origin": [x + ew / 2, 1, z + ed / 2], "axis": "y", "angle": 22.5 if a > 0 else -22.5})
                for x, z, a in SPOTS[:n]]
        w(A / f"models/block/bird_nest_eggs_{bird}_{n}.json", {"textures": {"particle": f"mechanical:block/bird_egg_{bird}",
            "egg": f"mechanical:block/bird_egg_{bird}"}, "elements": eggs})
bs_path = A / "blockstates/bird_nest.json"
bs = json.loads(bs_path.read_text(encoding="utf-8"))
for bird in NEST_EGGS:
    for n in range(1, 4):
        part = {"when": {"bird": bird, "eggs": str(n)}, "apply": {"model": f"mechanical:block/bird_nest_eggs_{bird}_{n}"}}
        if part not in bs["multipart"]:
            bs["multipart"].append(part)
w(bs_path, bs)
loot_path = D / "mechanical/loot_table/blocks/bird_nest.json"
loot = json.loads(loot_path.read_text(encoding="utf-8"))
for bird, (_, egg) in NEST_EGGS.items():
    for n in range(1, 4):
        e = {"type": "minecraft:item", "name": egg,
             "condition": {"type": "minecraft:match_block", "blocks": M + "bird_nest", "state": {"bird": bird, "eggs": str(n)}}}
        if n > 1:
            e["modifier"] = [{"type": "minecraft:set_count", "count": n}]
        if e not in loot["pools"][1]["entries"]:
            loot["pools"][1]["entries"].append(e)
w(loot_path, loot)

WATER = ["minecraft:water", "minecraft:flowing_water"]
BANK = ["minecraft:grass_block", "minecraft:dirt", "minecraft:mud", "minecraft:sand", "minecraft:coarse_dirt", "minecraft:moss_block"]


def near_water(d):
    return {"type": "minecraft:any_of", "predicates": [{"type": "minecraft:matching_fluids", "fluids": WATER, "offset": [x, -1, z]}
                                                       for x, z in ((d, 0), (-d, 0), (0, d), (0, -d), (1, 0), (-1, 0), (0, 1), (0, -1))]}


# bird -> (biomes, rarity (1 in n chunks), tries, water distance)
NESTS = {
    "mallard": (["minecraft:river", "minecraft:swamp", FEN], 6, 3, 2),
    "mute_swan": (["minecraft:river", "minecraft:swamp", FEN], 12, 1, 1),
    "grey_heron": (["minecraft:swamp", "minecraft:river", FEN], 16, 1, 2),
    "common_crane": ([FEN, "minecraft:swamp", HEATH], 10, 2, 2),
}
for bird, (biomes, rarity, tries, dist) in NESTS.items():
    name = f"{bird}_nest"
    state = lambda eggs: {"id": M + "bird_nest", "properties": {"bird": bird, "eggs": str(eggs), "robbed": "0"}}
    w(D / f"mechanical/worldgen/feature/{name}.json", {"type": "minecraft:simple_block", "to_place": {
        "type": "minecraft:weighted", "entries": [{"data": state(0), "weight": 2}, {"data": state(1), "weight": 2}, {"data": state(2), "weight": 1}]}})
    preds = [{"type": "minecraft:matching_block_tag", "tag": "minecraft:air"},
             {"type": "minecraft:matching_blocks", "blocks": BANK, "offset": [0, -1, 0]}, near_water(dist)]
    placement = [{"type": "minecraft:rarity_filter", "chance": rarity}, {"type": "minecraft:in_square"}]
    if tries > 1:
        placement.append({"type": "minecraft:count", "count": tries})
    placement += [{"type": "minecraft:heightmap", "heightmap": "WORLD_SURFACE_WG"}, {"type": "minecraft:biome"},
                  {"type": "minecraft:block_predicate_filter", "predicate": {"type": "minecraft:all_of", "predicates": preds}}]
    w(D / f"mechanical/worldgen/placed_feature/{name}.json", {"feature": M + name, "placement": placement})
    w(D / f"mechanical/neoforge/biome_modifier/add_{name}.json", {"type": "neoforge:add_features", "biomes": biomes,
        "features": M + name, "step": "vegetal_decoration"})

merge(A / "lang/en_us.json", lang)
print("fauna waterfowl ok")
