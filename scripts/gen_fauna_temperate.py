"""Data for the temperate fauna (wildlife/temperate/TemperateFauna.java): Wisent, Pond Turtle, Water Vole, Giant Stag Beetle,
Woodpecker, Tawny Owl, Skylark, Lapwing, Bittern, Sea Eagle, Eel; the red fox (vanilla's fox: plains spawns, Fox Pelt,
Fox Den, Fox Tracks), the stoat in the plains (the Ermine), wild boar sounders, grey seal colonies; Grub Log, Bumblebee
Nest, leeches, crayfish and eels in the Fish Trap.

- item models, lang (entities, items, blocks, the leech messages)
- recipes: every meat on the Campfire / in the smoker; plucked birds roasted whole or cut up; the eggs boiled; the eel
  gutted; the Raw Wisent Hide scraped (3 Scraped Hide) or fat-tanned into a Wisent Rug; the Pond Turtle Shell into bowls;
  the Beetle Shell into brown dye and black pigment; the Woodpecker Feather into red dye; the Fox Fur Hat
- blocks: Wisent Rug (Bear Rug model), Grub Log (column), Bumblebee Nest, Fox Den (mounds), Fox Tracks (flat prints);
  ground nests of the skylark and lapwing added to the Bird Nest's blockstate and loot (gen_birds2.py writes them first)
- loot: beetle, woodpecker, tawny owl, sea eagle, eel; fallback loot of the carcass birds
- tags: spoilage / smoking, whole fish, bird eggs, snare bait, fur insulation, head armour; biome tags has_leeches,
  has_eels, has_boar_sounders
- world: spawns (biome modifiers), Grub Log snags, Bumblebee Nests, Fox Dens and ground nests (features)
Textures: scripts/gen_fauna_temperate_textures.py. Docs: docs/fauna/temperate.md.
Generator order: ... gen_birds2.py ... gen_fauna_*.py (alphabetical), gen_block_items.py, gen_unlocks.py, gen_advancements.py,
gen_registry.py. Run: python scripts/gen_fauna_temperate.py"""
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
    tag_add(D / f"mechanical/tags/item/{name}.json", *values)


def flat_item(item):
    w(A / f"models/item/{item}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"mechanical:item/{item}"}})
    w(A / f"items/{item}.json", {"model": {"type": "minecraft:model", "model": f"mechanical:item/{item}"}})


def block_item(block, model=None):
    w(A / f"items/{block}.json", {"model": {"type": "minecraft:model", "model": model or f"mechanical:block/{block}"}})


def recipe(name, obj):
    w(D / f"mechanical/recipe/{name}.json", obj)


def shapeless(name, ingredients, result, count=1, category="misc"):
    recipe(name, {"type": "minecraft:crafting_shapeless", "category": category, "ingredients": ingredients,
                  "result": {"id": result, "count": count}})


def shaped(name, pattern, key, result, count=1, category="misc"):
    recipe(name, {"type": "minecraft:crafting_shaped", "category": category, "pattern": pattern, "key": key,
                  "result": {"id": result, "count": count}})


def cook(name, ingredient, result, ticks=400, count=1, kinds=("campfire_cooking", "smoking")):
    for kind in kinds:
        recipe(f"{name}_from_{kind}" if len(kinds) > 1 else name, {"type": f"minecraft:{kind}", "category": "food",
               "cookingtime": ticks if kind == "campfire_cooking" else ticks // 4, "experience": 0.1,
               "ingredient": ingredient, "result": {"id": result, "count": count}})


def block_loot(block, entries):
    w(D / f"mechanical/loot_table/blocks/{block}.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": entries,
        "condition": {"type": "minecraft:survives_explosion"}}], "random_sequence": f"mechanical:blocks/{block}"})


def count(lo, hi):
    return {"type": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": lo, "max": hi}}


ANIMALS = {"wisent": "Wisent", "pond_turtle": "European Pond Turtle", "water_vole": "Water Vole", "stag_beetle": "Giant Stag Beetle",
           "woodpecker": "Great Spotted Woodpecker", "tawny_owl": "Tawny Owl", "skylark": "Skylark", "lapwing": "Lapwing",
           "bittern": "Bittern", "sea_eagle": "White-tailed Sea Eagle", "eel": "Eel"}
# raw -> (cooked, raw name, cooked name)
MEATS = {
    "raw_wisent": ("cooked_wisent", "Raw Wisent", "Cooked Wisent"),
    "pond_turtle_meat": ("cooked_pond_turtle_meat", "Pond Turtle Meat", "Cooked Pond Turtle Meat"),
    "raw_water_vole": ("cooked_water_vole", "Raw Water Vole", "Cooked Water Vole"),
    "raw_skylark": ("cooked_skylark", "Raw Skylark", "Cooked Skylark"),
    "raw_lapwing": ("cooked_lapwing", "Raw Lapwing", "Cooked Lapwing"),
    "raw_bittern": ("cooked_bittern", "Raw Bittern", "Cooked Bittern"),
    "raw_eel": ("cooked_eel", "Raw Eel", "Cooked Eel"),
    "crayfish": ("cooked_crayfish", "Crayfish", "Cooked Crayfish"),
}
BIRDS = {"skylark": ("Skylark", 1), "lapwing": ("Lapwing", 1), "bittern": ("Bittern", 1)}
ITEMS = {
    "raw_fox_pelt": "Raw Fox Pelt", "fox_fur": "Fox Fur", "fox_fur_hat": "Fox Fur Hat", "raw_wisent_hide": "Raw Wisent Hide",
    "pond_turtle_shell": "Pond Turtle Shell", "beetle_shell": "Beetle Shell", "grub": "Grub", "woodpecker_feather": "Woodpecker Feather",
    "skylark_egg": "Skylark Egg", "lapwing_egg": "Lapwing Egg", "whole_eel": "Whole Eel", "leech": "Leech", "wild_honey": "Wild Honey",
}
BLOCKS = {"wisent_rug": "Wisent Rug", "grub_log": "Grub Log", "bumblebee_nest": "Bumblebee Nest", "fox_den": "Fox Den",
          "fox_tracks": "Fox Tracks"}

lang = {f"entity.mechanical.{k}": v for k, v in ANIMALS.items()}
lang.update({f"item.mechanical.{k}_spawn_egg": f"{v} Spawn Egg" for k, v in ANIMALS.items()})
lang.update({f"item.mechanical.{k}": v for k, v in ITEMS.items()})
lang.update({f"block.mechanical.{k}": v for k, v in BLOCKS.items()})
lang.update({
    "message.mechanical.leech.latched": "A leech has latched onto your leg",
    "message.mechanical.leech.dropped": "A sated leech drops off",
})

# ---- items and food ----
for k in ITEMS:
    flat_item(k)
for k in ANIMALS:
    flat_item(f"{k}_spawn_egg")
for raw, (cooked, raw_name, cooked_name) in MEATS.items():
    flat_item(raw)
    flat_item(cooked)
    lang[f"item.mechanical.{raw}"] = raw_name
    lang[f"item.mechanical.{cooked}"] = cooked_name
    cook(cooked, M + raw, M + cooked)
for bird, (name, n) in BIRDS.items():
    plucked = f"plucked_{bird}"
    flat_item(plucked)
    lang[f"item.mechanical.{plucked}"] = f"Plucked {name}"
    cook(f"cooked_{bird}_from_{plucked}", M + plucked, M + f"cooked_{bird}", 600, n, kinds=("campfire_cooking",))
    shapeless(f"raw_{bird}_from_{plucked}", [M + plucked, "#mechanical:knives"], M + f"raw_{bird}", n)
for egg in ("skylark_egg", "lapwing_egg"):
    cook(f"boiled_egg_from_{egg}", M + egg, M + "boiled_egg", 300, kinds=("campfire_cooking",))
shapeless("gut_eel", [M + "whole_eel", "#mechanical:knives"], M + "raw_eel", 2)

RAW = [M + r for r in MEATS] + [M + f"plucked_{b}" for b in BIRDS] + [M + "whole_eel"]
COOKED = [M + c for c, _, _ in MEATS.values()]
item_tag("perishes_1_day", *RAW)
item_tag("perishes_3_days", *COOKED)
item_tag("rots_to_flesh", *RAW, *COOKED)
item_tag("smokable_meat", M + "raw_wisent", M + "pond_turtle_meat", M + "raw_water_vole", M + "raw_skylark", M + "raw_lapwing", M + "raw_bittern")
item_tag("smokable_fish", M + "raw_eel")
item_tag("whole_fish", M + "whole_eel")
item_tag("bird_eggs", M + "skylark_egg", M + "lapwing_egg")
item_tag("snare_bait", M + "grub")
item_tag("tanning_fats", M + "tallow", M + "lard", M + "bear_fat")
item_tag("fur_insulation", M + "fox_fur_hat")
item_tag("repairs_fox_fur", M + "fox_fur")
tag_add(D / "minecraft/tags/item/head_armor.json", M + "fox_fur_hat")
w(A / "equipment/fox_fur_hat.json", {"layers": {"humanoid": [{"texture": "mechanical:fox_fur_hat"}]}})

# ---- recipes ----
shapeless("scraped_hide_from_raw_wisent_hide", [M + "raw_wisent_hide", "#mechanical:knives"], M + "scraped_hide", 3)
# the wisent's shaggy hide fat-tanned with the hair on, a bone to weigh the corners: a rug
shapeless("wisent_rug", [M + "raw_wisent_hide", "#mechanical:knives", "#mechanical:tanning_fats", "minecraft:bone"], M + "wisent_rug",
          category="building")
shapeless("bowl_from_pond_turtle_shell", [M + "pond_turtle_shell", "#mechanical:knives"], "minecraft:bowl", 2)
shapeless("brown_dye_from_beetle_shell", [M + "beetle_shell"], "minecraft:brown_dye", 2)
shapeless("black_pigment_from_beetle_shell", [M + "beetle_shell", M + "beetle_shell", "#mechanical:rock_chunks"], M + "black_pigment", 2)
shapeless("red_dye_from_woodpecker_feather", [M + "woodpecker_feather", M + "woodpecker_feather"], "minecraft:red_dye")
shaped("fox_fur_hat", ["FFF", "F F", " S "], {"F": M + "fox_fur", "S": M + "sinew"}, M + "fox_fur_hat", category="equipment")

# ---- blocks ----
ROT = [("north", 0), ("east", 90), ("south", 180), ("west", 270)]
w(A / "models/block/wisent_rug.json", {"parent": "mechanical:block/bear_rug",
    "textures": {"particle": "mechanical:block/wisent_rug", "top": "mechanical:block/wisent_rug"}})
w(A / "blockstates/wisent_rug.json", {"variants": {f"facing={f}": {"model": "mechanical:block/wisent_rug", **({"y": y} if y else {})}
                                                   for f, y in ROT}})
flat_item("wisent_rug")
block_loot("wisent_rug", [{"type": "minecraft:item", "name": M + "wisent_rug"}])

w(A / "models/block/grub_log.json", {"parent": "minecraft:block/cube_column",
    "textures": {"side": "mechanical:block/grub_log", "end": "mechanical:block/grub_log_top"}})
w(A / "blockstates/grub_log.json", {"variants": {"": {"model": "mechanical:block/grub_log"}}})
block_item("grub_log")
w(D / "mechanical/loot_table/blocks/grub_log.json", {"type": "minecraft:block", "pools": [
    {"rolls": 1, "entries": [{"type": "minecraft:item", "name": "minecraft:stick", "modifier": [count(1, 2)]}],
     "condition": {"type": "minecraft:survives_explosion"}},
    {"rolls": 1, "entries": [{"type": "minecraft:item", "name": M + "grub"}],
     "condition": {"type": "minecraft:match_block", "blocks": M + "grub_log", "state": {"grubs": "2"}}}],
    "random_sequence": "mechanical:blocks/grub_log"})

FACES = ["down", "up", "north", "south", "east", "west"]


def el(f, t, tex, faces=FACES):
    return {"from": f, "to": t, "faces": {d: {"texture": tex} for d in faces}}


# Bumblebee Nest: a mossy tussock with a dark hole and, with honey, waxy pots showing
for n in range(4):
    elements = [el([2, 0, 2], [14, 4, 14], "#moss"), el([4, 4, 4], [12, 6, 12], "#moss"), el([6, 1, 1.9], [9, 3, 2], "#hole", ["north"])]
    if n:
        elements += [el([10 - 2 * i, 0, 1.5], [11.5 - 2 * i, 1.5, 3], "#wax") for i in range(n)]
    w(A / f"models/block/bumblebee_nest_{n}.json", {"textures": {"particle": "mechanical:block/bumblebee_nest",
        "moss": "mechanical:block/bumblebee_nest", "hole": "mechanical:block/bumblebee_nest_hole", "wax": "mechanical:block/bumblebee_wax"},
        "elements": elements})
w(A / "blockstates/bumblebee_nest.json", {"variants": {f"honey={n}": {"model": f"mechanical:block/bumblebee_nest_{n}"} for n in range(4)}})
block_item("bumblebee_nest", "mechanical:block/bumblebee_nest_2")
block_loot("bumblebee_nest", [{"type": "minecraft:item", "name": M + "bumblebee_nest"}])

# Fox Den: a low mound of dug-out earth with a burrow mouth
w(A / "models/block/fox_den.json", {"textures": {"particle": "mechanical:block/fox_den", "soil": "mechanical:block/fox_den",
    "hole": "mechanical:block/fox_den_hole"}, "elements": [
    el([0, 0, 0], [16, 3, 16], "#soil"), el([2, 3, 2], [14, 5, 14], "#soil"), el([5, 1, -0.1], [11, 5, 0], "#hole", ["north"])]})
w(A / "blockstates/fox_den.json", {"variants": {"": {"model": "mechanical:block/fox_den"}}})
block_item("fox_den")
block_loot("fox_den", [{"type": "minecraft:item", "name": M + "fox_den"}])

# Fox Tracks: paw prints flat on the ground, fading
w(A / "models/block/fox_tracks.json", {"render_type": "minecraft:cutout", "ambientocclusion": False,
    "textures": {"particle": "mechanical:block/fox_tracks", "prints": "mechanical:block/fox_tracks"},
    "elements": [{"from": [0, 0.1, 0], "to": [16, 0.1, 16], "faces": {"up": {"texture": "#prints"}, "down": {"texture": "#prints"}}}]})
w(A / "blockstates/fox_tracks.json", {"variants": {"": [{"model": "mechanical:block/fox_tracks", "y": y} for y in (0, 90, 180, 270)]}})

# ---- entity loot ----
LOOT = {
    "stag_beetle": [(M + "beetle_shell", 1, 1), (M + "grub", 0, 1)],
    "woodpecker": [("minecraft:feather", 0, 1), (M + "woodpecker_feather", 1, 2)],
    "tawny_owl": [("minecraft:feather", 0, 1), (M + "owl_feather", 1, 2)],
    "sea_eagle": [("minecraft:feather", 1, 2), (M + "eagle_feather", 1, 3), (M + "eagle_talon", 0, 2)],
    "eel": [(M + "whole_eel", 1, 1)],
    # fallback only (they leave a carcass): feathers and meat
    "skylark": [("minecraft:feather", 1, 1), (M + "raw_skylark", 1, 1)],
    "lapwing": [("minecraft:feather", 1, 2), (M + "raw_lapwing", 1, 1)],
    "bittern": [("minecraft:feather", 2, 3), (M + "raw_bittern", 1, 1)],
}
for mob, drops in LOOT.items():
    w(D / f"mechanical/loot_table/entities/{mob}.json", {"type": "minecraft:entity", "pools": [
        {"rolls": 1, "entries": [{"type": "minecraft:item", "name": i, "modifier": [count(lo, hi)]}]} for i, lo, hi in drops],
        "random_sequence": f"mechanical:entities/{mob}"})

# ---- ground nests: the Bird Nest (BirdNestBlock.Bird SKYLARK / LAPWING), like gen_fauna_forestbirds.py ----


def box(f, t, tex, rot):
    return {"from": f, "to": t, "faces": {d: {"texture": tex} for d in FACES}, "rotation": rot}


EGG_SIZE = {"skylark": (2, 2, 2.5), "lapwing": (2.5, 2.5, 3)}
SPOTS = [(5.5, 5.5, 20), (8.5, 7, -25), (6, 9, 45)]
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
for bird in EGG_SIZE:
    for n in range(1, 4):
        e = {"type": "minecraft:item", "name": M + f"{bird}_egg",
             "condition": {"type": "minecraft:match_block", "blocks": M + "bird_nest", "state": {"bird": bird, "eggs": str(n)}}}
        if n > 1:
            e["modifier"] = [{"type": "minecraft:set_count", "count": n}]
        if e not in egg_pool:
            egg_pool.append(e)
w(loot_path, loot)

# ---- biome tags ----
PLAINS = ["minecraft:plains", "minecraft:sunflower_plains", M + "hazel_meadow"]
MEADOW = PLAINS + [M + "chalk_downs"]
BROADLEAF = ["minecraft:forest", "minecraft:birch_forest", "minecraft:old_growth_birch_forest", M + "deciduous_forest"]
DARK = ["minecraft:dark_forest"]
FENS = ["minecraft:swamp", M + "willow_fen", M + "peat_bog", M + "reed_delta"]
COASTS = ["minecraft:stony_shore", M + "basalt_coast", M + "shingle_beach"]
w(D / "mechanical/tags/worldgen/biome/has_leeches.json", {"values": FENS + ["minecraft:mangrove_swamp"]})
w(D / "mechanical/tags/worldgen/biome/has_eels.json", {"values": ["minecraft:river"] + FENS + ["minecraft:mangrove_swamp"]})
w(D / "mechanical/tags/worldgen/biome/has_boar_sounders.json", {"values": DARK})

# ---- spawns ----
U = lambda lo, hi: {"type": "minecraft:uniform", "min_inclusive": lo, "max_inclusive": hi}
SPAWNS = {  # file -> (entity, biomes, weight, min, max)
    "add_red_fox_plains": ("minecraft:fox", PLAINS, 4, 1, 2),
    "add_stoat_plains": (M + "ermine", PLAINS, 3, 1, 2),
    "add_skylark": (M + "skylark", MEADOW, 6, 2, 4),
    "add_lapwing": (M + "lapwing", PLAINS + ["minecraft:swamp", M + "reed_delta"], 5, 2, 4),
    "add_wisent": (M + "wisent", BROADLEAF, 3, 2, 4),
    "add_woodpecker": (M + "woodpecker", BROADLEAF + DARK, 4, 1, 2),
    "add_tawny_owl": (M + "tawny_owl", BROADLEAF + DARK, 3, 1, 1),
    "add_stag_beetle": (M + "stag_beetle", DARK, 6, 1, 2),
    "add_stag_beetle_forest": (M + "stag_beetle", ["minecraft:forest", M + "deciduous_forest"], 2, 1, 1),
    "add_wild_boar_sounder": (M + "wild_boar", DARK, 6, 5, 8),
    "add_pond_turtle": (M + "pond_turtle", ["minecraft:swamp", M + "willow_fen", M + "peat_bog"], 5, 1, 3),
    "add_water_vole": (M + "water_vole", ["minecraft:swamp", M + "willow_fen", M + "reed_delta", "minecraft:river"], 5, 1, 3),
    "add_bittern": (M + "bittern", FENS, 3, 1, 1),
    "add_sea_eagle": (M + "sea_eagle", COASTS + ["minecraft:beach", "minecraft:river"], 2, 1, 2),
    "add_seal_colony": (M + "seal", COASTS, 8, 4, 8),
}
for name, (ent, biomes, weight, lo, hi) in SPAWNS.items():
    w(D / f"mechanical/neoforge/biome_modifier/{name}.json", {"type": "neoforge:add_spawns", "biomes": biomes,
        "spawners": [{"type": ent, "weight": weight, "count": U(lo, hi)}]})
w(D / "mechanical/neoforge/biome_modifier/add_eel.json", {"type": "neoforge:add_spawns",
    "biomes": ["minecraft:river", "minecraft:mangrove_swamp"] + FENS,
    "spawners": [{"type": M + "eel", "weight": 6, "count": U(1, 2)}]})

# ---- world features: (feature json, biomes, ground under it, rarity 1 in n chunks) ----
STATE = lambda block, **props: {"id": M + block, **({"properties": props} if props else {})}
FEATURES = {
    "grub_log_snag": ({"type": "minecraft:block_column", "allowed_placement": {"type": "minecraft:matching_block_tag", "tag": "minecraft:air"},
        "direction": "up", "layers": [{"height": U(2, 4), "provider": STATE("grub_log", grubs="2")}], "prioritize_tip": False},
        BROADLEAF + DARK, ["minecraft:grass_block", "minecraft:podzol", "minecraft:moss_block", "minecraft:coarse_dirt"], 5),
    "bumblebee_nest": ({"type": "minecraft:simple_block", "to_place": STATE("bumblebee_nest", honey="1")}, MEADOW, ["minecraft:grass_block"], 10),
    "fox_den": ({"type": "minecraft:simple_block", "to_place": STATE("fox_den")}, PLAINS + BROADLEAF,
        ["minecraft:grass_block", "minecraft:coarse_dirt"], 28),
}
for bird, (biomes, rarity) in {"skylark": (MEADOW, 16), "lapwing": (PLAINS, 20)}.items():
    state = lambda eggs, bird=bird: STATE("bird_nest", bird=bird, eggs=str(eggs), robbed="0")
    FEATURES[f"{bird}_nest"] = ({"type": "minecraft:simple_block", "to_place": {"type": "minecraft:weighted",
        "entries": [{"data": state(0), "weight": 2}, {"data": state(1), "weight": 2}, {"data": state(2), "weight": 1}]}},
        biomes, ["minecraft:grass_block"], rarity)
for name, (feature, biomes, ground, rarity) in FEATURES.items():
    w(D / f"mechanical/worldgen/feature/{name}.json", feature)
    preds = [{"type": "minecraft:matching_block_tag", "tag": "minecraft:air"},
             {"type": "minecraft:matching_blocks", "blocks": ground, "offset": [0, -1, 0]}]
    w(D / f"mechanical/worldgen/placed_feature/{name}.json", {"feature": M + name, "placement": [
        {"type": "minecraft:rarity_filter", "chance": rarity}, {"type": "minecraft:in_square"},
        {"type": "minecraft:heightmap", "heightmap": "WORLD_SURFACE_WG"}, {"type": "minecraft:biome"},
        {"type": "minecraft:block_predicate_filter", "predicate": {"type": "minecraft:all_of", "predicates": preds}}]})
    w(D / f"mechanical/neoforge/biome_modifier/add_{name}.json", {"type": "neoforge:add_features", "biomes": biomes,
        "features": M + name, "step": "vegetal_decoration"})

merge(A / "lang/en_us.json", lang)
print("fauna temperate ok")
