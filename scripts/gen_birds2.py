"""Data for the bird overhaul (Birds.java, BirdNestBlock.java; carcass yields in Butchery.java).

- items: Raw/Cooked Goose, Pheasant, Ptarmigan, Pigeon; Plucked Goose/Pheasant/Ptarmigan/Pigeon/Chicken; Goose, Pheasant,
  Ptarmigan and Pigeon Eggs (models, lang)
- recipes: raw meat on the Campfire (20 s) or in the smoker; a Plucked bird cooks whole on the Campfire (30 s) into its
  cooked meat, or is cut up with a knife in the grid into its raw meat; each wild egg boils into Boiled Eggs (a goose
  egg into two); the Bird Nest from sticks and straw (or plant fibre)
- tags (added to gen_preserving.py's): raw meat and plucked birds perish in 1 day, cooked meat in 3, all rot to flesh, the
  raw meats are smokable/dryable meat; #mechanical:bird_eggs
- Bird Nest: twig ring and lining models, eggs per nest bird and count (multipart), loot (the nest and its eggs)
- world generation: nests where each bird lives (goose: river and swamp banks by the water; pheasant: forest grass;
  ptarmigan: snowy and taiga ground; rock dove: bare rock in the hills and peaks), already claimed by their bird
Textures: scripts/gen_birds2_textures.py (plucked flock birds: gen_bird_textures.py).
Generator order: ... gen_preserving.py ... gen_cart.py, gen_birds2.py, gen_block_items.py, gen_unlocks.py,
gen_advancements.py, gen_registry.py (it adds to the perish tags that gen_preserving.py rewrites).
Run: python scripts/gen_birds2.py"""
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


# bird -> (raw meat id, cooked meat id, meat per bird); keep in step with Butchery's yields
MEATS = {
    "goose": (M + "raw_goose", M + "cooked_goose", 2),
    "pheasant": (M + "raw_pheasant", M + "cooked_pheasant", 1),
    "ptarmigan": (M + "raw_ptarmigan", M + "cooked_ptarmigan", 1),
    "pigeon": (M + "raw_pigeon", M + "cooked_pigeon", 1),
    "chicken": ("minecraft:chicken", "minecraft:cooked_chicken", 1),
}
EGGS = {"goose": 2, "pheasant": 1, "ptarmigan": 1, "pigeon": 1}  # boiled eggs per egg
NAMES = {"goose": "Goose", "pheasant": "Pheasant", "ptarmigan": "Ptarmigan", "pigeon": "Pigeon", "chicken": "Chicken"}

lang = {"block.mechanical.bird_nest": "Bird Nest",
        "message.mechanical.bird_nest.uneasy": "The birds are getting uneasy about this nest",
        "message.mechanical.bird_nest.abandoned": "The birds have abandoned this nest"}

# ---- items and food recipes ----
for bird, (raw, cooked, n) in MEATS.items():
    plucked = f"plucked_{bird}"
    flat_item(plucked)
    lang[f"item.mechanical.{plucked}"] = f"Plucked {NAMES[bird]}"
    if raw.startswith(M):
        for item in (raw, cooked):
            flat_item(item.removeprefix(M))
        lang[f"item.{raw.replace(':', '.')}"] = f"Raw {NAMES[bird]}"
        lang[f"item.{cooked.replace(':', '.')}"] = f"Cooked {NAMES[bird]}"
        # only the campfire (and the smoker, later) cook food, like the scraps (gen_data.py)
        for kind, ticks in (("campfire_cooking", 400), ("smoking", 100)):
            w(D / f"mechanical/recipe/cooked_{bird}_from_{kind}.json", {"type": f"minecraft:{kind}", "category": "food",
                "cookingtime": ticks, "experience": 0.1, "ingredient": raw, "result": {"id": cooked}})
    # whole on the fire (a longer roast), or cut up with a knife (it stays in the grid)
    w(D / f"mechanical/recipe/cooked_{bird}_from_{plucked}.json", {"type": "minecraft:campfire_cooking", "category": "food",
        "cookingtime": 600, "experience": 0.15, "ingredient": M + plucked, "result": {"id": cooked, "count": n}})
    w(D / f"mechanical/recipe/raw_{bird}_from_{plucked}.json", {"type": "minecraft:crafting_shapeless", "category": "misc",
        "ingredients": [M + plucked, "#mechanical:knives"], "result": {"id": raw, "count": n}})

for bird, boiled in EGGS.items():
    egg = f"{bird}_egg"
    flat_item(egg)
    lang[f"item.mechanical.{egg}"] = f"{NAMES[bird]} Egg"
    w(D / f"mechanical/recipe/boiled_egg_from_{egg}.json", {"type": "minecraft:campfire_cooking", "category": "food",
        "cookingtime": 300, "experience": 0.1, "ingredient": M + egg, "result": {"id": M + "boiled_egg", "count": boiled}})

# ---- tags: spoilage (on top of gen_preserving.py), smoking/drying, eggs ----
RAW = [m[0] for b, m in MEATS.items() if b != "chicken"]
COOKED = [m[1] for b, m in MEATS.items() if b != "chicken"]
PLUCKED = [M + f"plucked_{b}" for b in MEATS]
tag_add("perishes_1_day", *RAW, *PLUCKED)
tag_add("perishes_3_days", *COOKED)
tag_add("rots_to_flesh", *RAW, *PLUCKED, *COOKED)
tag_add("smokable_meat", *RAW)
w(D / "mechanical/tags/item/bird_eggs.json", {"values": [M + f"{b}_egg" for b in EGGS]})

# ---- Bird Nest: a ring of twigs around a straw-and-down lining, eggs on the lining ----
FACES = ["down", "up", "north", "south", "east", "west"]


def box(f, t, tex="#twigs", rot=None):
    e = {"from": f, "to": t, "faces": {d: {"texture": tex} for d in FACES}}
    if rot:
        e["rotation"] = rot
    return e


nest = [box([3, 0, 3], [13, 1, 13], "#lining"),
        box([2, 0, 2], [14, 3, 4]), box([2, 0, 12], [14, 3, 14]), box([2, 0, 4], [4, 3, 12]), box([12, 0, 4], [14, 3, 12]),
        # a few loose twigs sticking out of the weave
        box([0.5, 2, 7], [15.5, 3, 8], rot={"origin": [8, 2.5, 7.5], "axis": "y", "angle": 22.5}),
        box([7.5, 1.5, 0.5], [8.5, 2.5, 15.5], rot={"origin": [8, 2, 8], "axis": "y", "angle": -22.5})]
w(A / "models/block/bird_nest.json", {"parent": "minecraft:block/block", "textures": {
    "particle": "mechanical:block/bird_nest", "twigs": "mechanical:block/bird_nest", "lining": "mechanical:block/bird_nest_lining"},
    "elements": nest})
# eggs per nest bird: (w, h, d) of one egg; three spots on the lining, each turned a little
EGG_SIZE = {"goose": (3, 3, 3.5), "pheasant": (2.5, 2.5, 3), "ptarmigan": (2.5, 2.5, 3), "rock_dove": (2, 2, 2.5), "chicken": (2.5, 2.5, 3)}
SPOTS = [(5.5, 5.5, 20), (8.5, 7, -25), (6, 9, 45)]
for bird, (ew, eh, ed) in EGG_SIZE.items():
    for n in range(1, 4):
        eggs = []
        for x, z, angle in SPOTS[:n]:
            eggs.append(box([x, 1, z], [x + ew, 1 + eh, z + ed], "#egg",
                            rot={"origin": [x + ew / 2, 1, z + ed / 2], "axis": "y", "angle": 22.5 if angle > 0 else -22.5}))
        w(A / f"models/block/bird_nest_eggs_{bird}_{n}.json", {"textures": {"particle": f"mechanical:block/bird_egg_{bird}",
            "egg": f"mechanical:block/bird_egg_{bird}"}, "elements": eggs})
BIRDS = ["goose", "pheasant", "ptarmigan", "rock_dove", "chicken"]
parts = [{"apply": {"model": "mechanical:block/bird_nest"}}]
parts += [{"when": {"bird": bird, "eggs": str(n)}, "apply": {"model": f"mechanical:block/bird_nest_eggs_{bird}_{n}"}}
          for bird in BIRDS for n in range(1, 4)]
parts += [{"when": {"bird": "none", "eggs": str(n)}, "apply": {"model": f"mechanical:block/bird_nest_eggs_chicken_{n}"}} for n in range(1, 4)]
w(A / "blockstates/bird_nest.json", {"multipart": parts})
w(A / "items/bird_nest.json", {"model": {"type": "minecraft:model", "model": "mechanical:block/bird_nest"}})
# drops: the nest, and whatever eggs are in it
EGG_ITEM = {"goose": M + "goose_egg", "pheasant": M + "pheasant_egg", "ptarmigan": M + "ptarmigan_egg", "rock_dove": M + "pigeon_egg",
            "chicken": "minecraft:egg", "none": "minecraft:egg"}


def egg_entry(bird, n):
    e = {"type": "minecraft:item", "name": EGG_ITEM[bird],
         "condition": {"type": "minecraft:match_block", "blocks": M + "bird_nest", "state": {"bird": bird, "eggs": str(n)}}}
    if n > 1:
        e["modifier"] = [{"type": "minecraft:set_count", "count": n}]
    return e


w(D / "mechanical/loot_table/blocks/bird_nest.json", {"type": "minecraft:block", "pools": [
    {"rolls": 1, "entries": [{"type": "minecraft:item", "name": M + "bird_nest"}], "condition": {"type": "minecraft:survives_explosion"}},
    {"rolls": 1, "entries": [egg_entry(b, n) for b in EGG_ITEM for n in range(1, 4)]}],
    "random_sequence": "mechanical:blocks/bird_nest"})
w(D / "mechanical/recipe/bird_nest.json", {"type": "minecraft:crafting_shaped", "category": "misc", "pattern": ["S S", "WSW"],
    "key": {"S": "minecraft:stick", "W": ["mechanical:straw", "mechanical:plant_fibre"]}, "result": {"id": M + "bird_nest"}})

# ---- world generation: nests where the birds live, claimed by them, 0-2 eggs ----
WATER = ["minecraft:water", "minecraft:flowing_water"]


def near_water(d):
    return {"type": "minecraft:any_of", "predicates": [{"type": "minecraft:matching_fluids", "fluids": WATER, "offset": [x, -1, z]}
                                                       for x, z in ((d, 0), (-d, 0), (0, d), (0, -d), (1, 0), (-1, 0), (0, 1), (0, -1))]}


# bird -> (biomes, ground under the nest (blocks or #tag), rarity (1 in n chunks per try), tries, extra predicate)
NESTS = {
    "goose": (["minecraft:river", "minecraft:swamp", "minecraft:frozen_river"],
              ["minecraft:grass_block", "minecraft:dirt", "minecraft:mud", "minecraft:sand", "minecraft:coarse_dirt"], 4, 4, near_water(2)),
    "pheasant": ("#minecraft:is_forest", ["minecraft:grass_block", "minecraft:podzol", "minecraft:coarse_dirt"], 16, 1, None),
    "ptarmigan": (["minecraft:snowy_plains", "minecraft:snowy_taiga", "minecraft:grove", "minecraft:snowy_slopes", "minecraft:taiga",
                   "minecraft:old_growth_spruce_taiga"],
                  ["minecraft:grass_block", "minecraft:podzol", "minecraft:snow_block", "minecraft:coarse_dirt"], 14, 1, None),
    "rock_dove": (["minecraft:meadow", "minecraft:windswept_hills", "minecraft:windswept_gravelly_hills", "minecraft:stony_peaks",
                   "minecraft:stony_shore", "minecraft:savanna_plateau"], "#minecraft:base_stone_overworld", 8, 2, None),
}
for bird, (biomes, ground, rarity, tries, extra) in NESTS.items():
    name = f"{bird}_nest"
    state = lambda eggs: {"id": M + "bird_nest", "properties": {"bird": bird, "eggs": str(eggs), "robbed": "0"}}
    w(D / f"mechanical/worldgen/feature/{name}.json", {"type": "minecraft:simple_block", "to_place": {
        "type": "minecraft:weighted", "entries": [{"data": state(0), "weight": 2}, {"data": state(1), "weight": 2}, {"data": state(2), "weight": 1}]}})
    below = ({"type": "minecraft:matching_block_tag", "tag": ground[1:], "offset": [0, -1, 0]} if isinstance(ground, str)
             else {"type": "minecraft:matching_blocks", "blocks": ground, "offset": [0, -1, 0]})
    preds = [{"type": "minecraft:matching_block_tag", "tag": "minecraft:air"}, below] + ([extra] if extra else [])
    placement = [{"type": "minecraft:rarity_filter", "chance": rarity}, {"type": "minecraft:in_square"}]
    if tries > 1:
        placement.append({"type": "minecraft:count", "count": tries})
    placement += [{"type": "minecraft:heightmap", "heightmap": "WORLD_SURFACE_WG"}, {"type": "minecraft:biome"},
                  {"type": "minecraft:block_predicate_filter", "predicate": {"type": "minecraft:all_of", "predicates": preds}}]
    w(D / f"mechanical/worldgen/placed_feature/{name}.json", {"feature": M + name, "placement": placement})
    w(D / f"mechanical/neoforge/biome_modifier/add_{name}.json", {"type": "neoforge:add_features", "biomes": biomes,
        "features": M + name, "step": "vegetal_decoration"})

merge(A / "lang/en_us.json", lang)
print("birds2 ok")
