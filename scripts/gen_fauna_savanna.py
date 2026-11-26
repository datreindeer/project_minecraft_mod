"""Data for the savanna fauna (wildlife/savanna/SavannaFauna.java): Spotted Hyena, Cheetah, Gazelle, Ostrich, Aardvark and
the Termite Mound.

- item models (flat items, the Termite Mound block item), lang (entities, items, the Signal Horn messages, subtitles)
- recipes: every meat on the Campfire / in the smoker; the Ostrich Egg roasted whole; raw hides scraped into Scraped
  Hides (gazelle 1, hyena/ostrich/aardvark 2); the cheetah pelt fat-tanned in the grid (knife + fat), then a Cheetah
  Cloak or leather; the Gazelle Horn hollowed into a Signal Horn; Ostrich Feathers split for fletching; Aardvark Claws
  into Bone Needles; Termites pounded into Chitin Paste, which stands in for Hide Glue in the Bellows
- blocks: the Termite Mound (blockstate, model, loot) and its worldgen spire (feature, placed feature, biome modifier)
- tags: spoilage / smoking, the cloak slot, the cloak's repair item
- the Signal Horn sound (sounds.json), the Cheetah Cloak equipment asset
- world spawns (biome modifiers, savanna only) and lion prides on the savanna
Textures: scripts/gen_fauna_savanna_textures.py. Docs: docs/fauna/savanna.md.
Generator order: ... gen_roots.py, gen_fauna_*.py, gen_block_items.py, gen_unlocks.py, gen_advancements.py, gen_registry.py.
Run: python scripts/gen_fauna_savanna.py"""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent / "src/main/resources"
A, D = ROOT / "assets/mechanical", ROOT / "data"
M, MC = "mechanical:", "minecraft:"


def w(path, obj):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, indent=2) + "\n", encoding="utf-8")


def merge(path, obj):
    old = json.loads(path.read_text(encoding="utf-8")) if path.exists() else {}
    old.update(obj)
    path.write_text(json.dumps(old, indent=2) + "\n", encoding="utf-8")  # escaped, like the committed lang file


def tag_add(path, *values):
    old = json.loads(path.read_text(encoding="utf-8")) if path.exists() else {"values": []}
    old["values"] += [v for v in values if v not in old["values"]]
    w(path, old)


def item_tag(name, *values):
    tag_add(D / f"mechanical/tags/item/{name}.json", *values)


def flat_item(item):
    w(A / f"models/item/{item}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"mechanical:item/{item}"}})
    w(A / f"items/{item}.json", {"model": {"type": "minecraft:model", "model": f"mechanical:item/{item}"}})


def recipe(name, obj):
    w(D / f"mechanical/recipe/{name}.json", obj)


def shapeless(name, ingredients, result, count=1, category="misc"):
    recipe(name, {"type": "minecraft:crafting_shapeless", "category": category, "ingredients": ingredients,
                  "result": {"id": result, "count": count}})


def shaped(name, pattern, key, result, count=1, category="misc"):
    recipe(name, {"type": "minecraft:crafting_shaped", "category": category, "pattern": pattern, "key": key,
                  "result": {"id": result, "count": count}})


def cook(name, ingredient, result, ticks=400, kinds=("campfire_cooking", "smoking")):
    for kind in kinds:
        recipe(f"{name}_from_{kind}" if len(kinds) > 1 else name, {"type": f"minecraft:{kind}", "category": "food",
               "cookingtime": ticks if kind == "campfire_cooking" else ticks // 4, "experience": 0.1,
               "ingredient": ingredient, "result": {"id": result, "count": 1}})


ANIMALS = {"spotted_hyena": "Spotted Hyena", "cheetah": "Cheetah", "gazelle": "Gazelle", "ostrich": "Ostrich", "aardvark": "Aardvark"}
MEATS = {  # raw -> (cooked, raw name, cooked name); the hyena's meat is the cave hyena's (gen_fauna_mountain.py)
    "cheetah_meat": ("cooked_cheetah_meat", "Cheetah Meat", "Cooked Cheetah Meat"),
    "gazelle_meat": ("cooked_gazelle_meat", "Gazelle Meat", "Cooked Gazelle Meat"),
    "ostrich_meat": ("cooked_ostrich_meat", "Ostrich Meat", "Cooked Ostrich Meat"),
    "aardvark_meat": ("cooked_aardvark_meat", "Aardvark Meat", "Cooked Aardvark Meat"),
}
ITEMS = {
    "hyena_hide": "Hyena Hide", "raw_cheetah_pelt": "Raw Cheetah Pelt", "cheetah_pelt": "Cheetah Pelt", "raw_gazelle_hide": "Raw Gazelle Hide",
    "gazelle_horn": "Gazelle Horn", "ostrich_feather": "Ostrich Feather", "raw_ostrich_hide": "Raw Ostrich Hide",
    "raw_aardvark_hide": "Raw Aardvark Hide", "aardvark_claw": "Aardvark Claw", "ostrich_egg": "Ostrich Egg",
    "cooked_ostrich_egg": "Roasted Ostrich Egg", "termites": "Termites", "chitin_paste": "Chitin Paste", "signal_horn": "Signal Horn",
    "cheetah_cloak": "Cheetah Cloak",
}
DIRS = {"n": "north", "ne": "north-east", "e": "east", "se": "south-east", "s": "south", "sw": "south-west", "w": "west", "nw": "north-west"}

lang = {f"entity.mechanical.{k}": v for k, v in ANIMALS.items()}
lang.update({f"item.mechanical.{k}_spawn_egg": f"{v} Spawn Egg" for k, v in ANIMALS.items()})
lang.update({f"item.mechanical.{k}": v for k, v in ITEMS.items()})
lang["block.mechanical.termite_mound"] = "Termite Mound"
lang["subtitles.mechanical.item.signal_horn.blow"] = "Signal Horn sounds"
lang["message.mechanical.signal_horn.heard"] = "A Signal Horn sounds to the %s, %s blocks away"
lang["message.mechanical.signal_horn.tribe"] = "%s blows the Signal Horn to the %s, %s blocks away"
lang.update({f"message.mechanical.signal_horn.dir.{k}": v for k, v in DIRS.items()})

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
cook("cooked_ostrich_egg", M + "ostrich_egg", M + "cooked_ostrich_egg", 900, kinds=("campfire_cooking",))

RAW = [M + r for r in MEATS]
COOKED = [M + c for c, _, _ in MEATS.values()]
item_tag("perishes_1_day", *RAW)
item_tag("perishes_3_days", *COOKED, M + "cooked_ostrich_egg")
item_tag("rots_to_flesh", *RAW, *COOKED)
item_tag("smokable_meat", *RAW)
item_tag("equipment/cloak", M + "cheetah_cloak")
w(D / "mechanical/tags/item/repairs_cheetah_pelt.json", {"values": [M + "cheetah_pelt"]})

# ---- recipes ----
# raw hides scrape into the common hide chain (Drying Rack -> Leather); the knife stays in the grid
for hide, n in (("raw_gazelle_hide", 1), ("hyena_hide", 2), ("raw_ostrich_hide", 2), ("raw_aardvark_hide", 2)):
    shapeless(f"scraped_hide_from_{hide}", [M + hide, "#mechanical:knives"], M + "scraped_hide", n)
# the cheetah pelt: fat-tanned in the grid, then worn as a cloak or cut into leather
shapeless("cheetah_pelt", [M + "raw_cheetah_pelt", "#mechanical:knives", "#mechanical:tanning_fats"], M + "cheetah_pelt")
shaped("cheetah_cloak", ["PSP", " L "], {"P": M + "cheetah_pelt", "S": M + "sinew", "L": MC + "leather"}, M + "cheetah_cloak",
       category="equipment")
shapeless("leather_from_cheetah_pelt", [M + "cheetah_pelt", "#mechanical:knives"], MC + "leather", 2)
# the gazelle horn hollowed and bound with sinew into a horn to blow
shapeless("signal_horn", [M + "gazelle_horn", "#mechanical:knives", M + "sinew"], M + "signal_horn", category="equipment")
# an ostrich plume splits into plenty of fletching
shapeless("feather_from_ostrich_feather", [M + "ostrich_feather", "#mechanical:knives"], MC + "feather", 4)
shapeless("bone_needle_from_aardvark_claw", [M + "aardvark_claw", "#mechanical:knives"], M + "bone_needle", 1, category="equipment")
# termites pounded into a sticky paste: a glue that does what Hide Glue does
shapeless("chitin_paste", [M + "termites"] * 4, M + "chitin_paste")
shaped("bellows_with_chitin_paste", ["PHP", "PHP", "GUG"], {"P": "#minecraft:planks", "H": M + "scraped_hide", "G": M + "chitin_paste",
       "U": M + "clay_tuyere"}, M + "bellows", category="equipment")

# ---- Cheetah Cloak: humanoid layer (textures/entity/equipment/humanoid/cheetah_cloak.png) and cape (entity/cloak/) ----
w(A / "equipment/cheetah_cloak.json", {"layers": {"humanoid": [{"texture": "mechanical:cheetah_cloak"}]}})

# ---- Signal Horn sound: a goat horn's call, lower ----
merge(A / "sounds.json", {"item.signal_horn.blow": {"subtitle": "subtitles.mechanical.item.signal_horn.blow", "sounds": [
    {"name": "minecraft:item/goat_horn/call3", "pitch": 0.75, "attenuation_distance": 256},
    {"name": "minecraft:item/goat_horn/call0", "pitch": 0.7, "attenuation_distance": 256}]}})

# ---- Termite Mound block ----
w(A / "models/block/termite_mound.json", {"parent": "minecraft:block/cube_column",
    "textures": {"side": "mechanical:block/termite_mound", "end": "mechanical:block/termite_mound_top"}})
# the top block of a spire narrows to a knobbly point (TermiteMoundBlock.TIP; the shape there matches these boxes)
w(A / "models/block/termite_mound_tip.json", {"parent": "minecraft:block/block", "ambientocclusion": True,
    "textures": {"particle": "mechanical:block/termite_mound", "side": "mechanical:block/termite_mound", "end": "mechanical:block/termite_mound_top"},
    "elements": [{"from": f, "to": t, "faces": {d: {"texture": "#end" if d in ("up", "down") else "#side"}
                                                for d in ("north", "south", "east", "west", "up", "down")}}
                 for f, t in (([1, 0, 1], [15, 6, 15]), ([3, 6, 3], [13, 11, 13]), ([5, 11, 5], [11, 14, 11]),
                              ([6.5, 14, 6.5], [9.5, 16, 9.5]), ([10, 6, 2], [14, 10, 6]), ([2, 6, 10], [5, 9, 13]))]})
w(A / "blockstates/termite_mound.json", {"variants": {"tip=false": {"model": "mechanical:block/termite_mound"},
                                                      "tip=true": {"model": "mechanical:block/termite_mound_tip"}}})
w(A / "items/termite_mound.json", {"model": {"type": "minecraft:model", "model": "mechanical:block/termite_mound"}})
w(D / "mechanical/loot_table/blocks/termite_mound.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [
    {"type": "minecraft:item", "name": M + "termite_mound"}], "condition": {"type": "minecraft:survives_explosion"}}],
    "random_sequence": "mechanical:blocks/termite_mound"})
tag_add(D / "minecraft/tags/block/mineable/shovel.json", M + "termite_mound")

# ---- world: spawns (savanna only), lion prides, the termite mound spires ----
U = lambda lo, hi: {"type": "minecraft:uniform", "min_inclusive": lo, "max_inclusive": hi}
SAVANNA = ["minecraft:savanna", "minecraft:savanna_plateau", "minecraft:windswept_savanna"]
SPAWNS = {  # entity -> (weight, min, max)
    "spotted_hyena": (3, 3, 5), "cheetah": (2, 1, 2), "gazelle": (8, 4, 7), "ostrich": (4, 2, 4), "aardvark": (2, 1, 1),
}
for ent, (weight, lo, hi) in SPAWNS.items():
    w(D / f"mechanical/neoforge/biome_modifier/add_{ent}.json", {"type": "neoforge:add_spawns", "biomes": SAVANNA,
        "spawners": [{"type": M + ent, "weight": weight, "count": U(lo, hi)}]})
# a pride on the savanna (the Cave Lion's own spawns, singles and pairs, are in gen_predators.py)
w(D / "mechanical/neoforge/biome_modifier/add_cave_lion_prides.json", {"type": "neoforge:add_spawns", "biomes": SAVANNA,
    "spawners": [{"type": M + "cave_lion", "weight": 1, "count": U(3, 4)}]})
w(D / "mechanical/worldgen/feature/termite_mound.json", {"type": "minecraft:block_column",
    "allowed_placement": {"type": "minecraft:matching_block_tag", "tag": "minecraft:air"}, "direction": "up",
    "layers": [{"height": U(1, 3), "provider": {"id": M + "termite_mound", "properties": {"tip": "false"}}},
               {"height": 1, "provider": {"id": M + "termite_mound", "properties": {"tip": "true"}}}], "prioritize_tip": True})
w(D / "mechanical/worldgen/placed_feature/termite_mound.json", {"feature": M + "termite_mound", "placement": [
    {"type": "minecraft:rarity_filter", "chance": 3},
    {"type": "minecraft:in_square"},
    {"type": "minecraft:heightmap", "heightmap": "WORLD_SURFACE_WG"},
    {"type": "minecraft:biome"},
    {"type": "minecraft:block_predicate_filter", "predicate": {"type": "minecraft:all_of", "predicates": [
        {"type": "minecraft:matching_block_tag", "tag": "minecraft:air"},
        {"type": "minecraft:matching_block_tag", "tag": "minecraft:dirt", "offset": [0, -1, 0]}]}}]})
w(D / "mechanical/neoforge/biome_modifier/add_termite_mounds.json", {"type": "neoforge:add_features", "biomes": SAVANNA,
    "features": [M + "termite_mound"], "step": "vegetal_decoration"})

merge(A / "lang/en_us.json", lang)
print("fauna savanna ok")
