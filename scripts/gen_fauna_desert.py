"""Data for the desert and badlands fauna (com.mechanical.wildlife.desert.DesertFauna): Desert Scorpion, Fennec Fox (+ the
tamed fennec), Wild Camel (+ the tamed camel), Sand Viper, Puma, Bighorn Sheep, Rattlesnake, Condor.

- items: models for every item (meats, hides and pelts, Scorpion Venom, Camel Hump Fat, Viper Fang, Bighorn Horn,
  Rattlesnake Rattle, Condor Feather, Puma Rug, spawn eggs)
- recipes: raw meat on the Campfire (20 s) or in the smoker; raw hides scraped with a knife into Scraped Hides (camel 3,
  bighorn 2); the fennec pelt fleshed with a knife, the puma pelt fat-tanned like the leopard's; Puma Rug (2 pelts + Bone),
  Leather from the puma pelt; Arrows of Poison from Scorpion Venom; a Leather Pouch from fennec pelts; a Shell Rattle from
  the Rattlesnake Rattle; Bone Needles from the Bighorn Horn; a Candle from Camel Hump Fat
- Puma Rug: block model (the Bear Rug's), blockstate, loot
- condor loot table (feathers and Condor Feathers: no carcass, like the vulture)
- tags: spoilage / smoking, #mechanical:tanning_fats (+ Camel Hump Fat), #mechanical:scares_predators untouched
- spawns: biome modifiers add_<mob>_desert (desert, the mod's Gypsum Flats; the badlands and the mod's Ochre Badlands)
- lang (English), including the taming messages of the camel and the fennec
Textures: scripts/gen_fauna_desert_textures.py.
Generator order: the alphabetical gen_fauna_* step (gen_fauna_desert.py before gen_fauna_forestbirds.py), then
gen_block_items.py, gen_unlocks.py, gen_advancements.py, gen_registry.py.
Run: python scripts/gen_fauna_desert.py"""
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


def item_tag(name, *values):
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


def shaped(name, pattern, key, result, count=1, category="misc"):
    recipe(name, {"type": "minecraft:crafting_shaped", "category": category, "pattern": pattern, "key": key,
                  "result": {"id": result, "count": count}})


MOBS = {"desert_scorpion": "Desert Scorpion", "fennec_fox": "Fennec Fox", "wild_camel": "Wild Camel", "sand_viper": "Sand Viper",
        "puma": "Puma", "bighorn_sheep": "Bighorn Sheep", "rattlesnake": "Rattlesnake", "condor": "Condor"}
TAMED = {"tame_fennec": "Fennec Fox", "tame_camel": "Camel"}
# raw -> (cooked, raw name, cooked name)
MEATS = {"raw_scorpion": ("cooked_scorpion", "Raw Scorpion", "Roast Scorpion"),
         "raw_fennec": ("cooked_fennec", "Raw Fennec", "Roast Fennec"),
         "camel_meat": ("cooked_camel_meat", "Camel Meat", "Cooked Camel Meat"),
         "puma_meat": ("cooked_puma_meat", "Puma Meat", "Cooked Puma Meat"),
         "bighorn_mutton": ("cooked_bighorn_mutton", "Bighorn Mutton", "Cooked Bighorn Mutton")}
OTHER = {"scorpion_venom": "Scorpion Venom", "raw_fennec_pelt": "Raw Fennec Pelt", "fennec_pelt": "Fennec Pelt",
         "raw_camel_hide": "Raw Camel Hide", "raw_bighorn_hide": "Raw Bighorn Hide", "camel_hump_fat": "Camel Hump Fat",
         "viper_fang": "Viper Fang", "raw_puma_pelt": "Raw Puma Pelt", "puma_pelt": "Puma Pelt", "bighorn_horn": "Bighorn Horn",
         "rattlesnake_rattle": "Rattlesnake Rattle", "condor_feather": "Condor Feather"}

lang = {"block.mechanical.puma_rug": "Puma Rug",
        # taming (Onager's messages under the camel's key; FennecFox / TameFennec)
        "message.mechanical.camel.shies": "The camel shies away. Crouch and hold out feed",
        "message.mechanical.camel.chewing": "The camel is still chewing",
        "message.mechanical.camel.trust": "The camel eats from your hand (trust %s/%s)",
        "message.mechanical.camel.ready_for_halter": "It trusts you now. Put a Rope Halter on it",
        "message.mechanical.camel.needs_halter": "Put a Rope Halter on it before feeding it more",
        "message.mechanical.camel.refuses_halter": "The camel won't take the halter yet. Feed it first",
        "message.mechanical.camel.haltered": "Haltered. Keep feeding it to tame it (trust %s/%s)",
        "message.mechanical.camel.tamed": "The camel is tame. Ride it, give it Pack Baskets, or hitch it to a Cart. It goes far without water",
        "message.mechanical.fennec.shies": "The fennec bolts. Crouch and hold out meat",
        "message.mechanical.fennec.chewing": "The fennec is still eating",
        "message.mechanical.fennec.trust": "The fennec takes the meat from your hand (%s/%s)",
        "message.mechanical.fennec.tamed": "The fennec is yours. Its great ears will warn you of what comes in the night",
        "message.mechanical.fennec.warns": "Your fennec yips and stares into the dark: %s"}

# ---- items ----
for raw, (cooked, rn, cn) in MEATS.items():
    for item, name in ((raw, rn), (cooked, cn)):
        flat_item(item)
        lang[f"item.mechanical.{item}"] = name
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
for mob, name in TAMED.items():
    lang[f"entity.mechanical.{mob}"] = name

# ---- recipes ----
for hide, n in (("raw_camel_hide", 3), ("raw_bighorn_hide", 2)):
    shapeless(f"scraped_hide_from_{hide}", [M + hide, "#mechanical:knives"], M + "scraped_hide", n)
shapeless("fennec_pelt", [M + "raw_fennec_pelt", "#mechanical:knives"], M + "fennec_pelt")
# soft little pelts sewn into a pouch: the Leather Pouch (which replaces vanilla's Bundle)
shaped("leather_pouch_from_fennec_pelt", [" T ", "F F", " F "], {"T": M + "twine", "F": M + "fennec_pelt"}, M + "leather_pouch",
       category="equipment")
# the puma pelt: fat-tanned like the leopard's, then a rug or leather
shapeless("puma_pelt", [M + "raw_puma_pelt", "#mechanical:knives", "#mechanical:tanning_fats"], M + "puma_pelt")
shapeless("puma_rug", [M + "puma_pelt", M + "puma_pelt", "minecraft:bone"], M + "puma_rug", category="building")
shapeless("leather_from_puma_pelt", [M + "puma_pelt", "#mechanical:knives"], "minecraft:leather", 2)
# venom-tipped arrows (vanilla Arrow of Poison), as from the adder's Venom Gland
recipe("poison_arrows_from_scorpion_venom", {"type": "minecraft:crafting_shaped", "category": "equipment",
                                             "pattern": ["AAA", "AVA", "AAA"], "key": {"A": "minecraft:arrow", "V": M + "scorpion_venom"},
                                             "result": {"id": "minecraft:tipped_arrow", "count": 8,
                                                        "components": {"minecraft:potion_contents": {"potion": "minecraft:poison"}}}})
# the rattle on a stick: a Shell Rattle that keeps predators off like a torch
shaped("shell_rattle_from_rattlesnake_rattle", [" R", "T ", "S "], {"R": M + "rattlesnake_rattle", "T": M + "twine", "S": "minecraft:stick"},
       M + "shell_rattle", category="equipment")
shapeless("bone_needle_from_bighorn_horn", [M + "bighorn_horn", "#mechanical:knives"], M + "bone_needle", 3, category="equipment")
# hump fat rendered into a candle, like tallow
shapeless("candle_from_camel_hump_fat", [M + "camel_hump_fat", M + "twine"], "minecraft:candle")

# ---- Puma Rug: flat like the Bear Rug ----
ROT = [("north", 0), ("east", 90), ("south", 180), ("west", 270)]
w(A / "models/block/puma_rug.json", {"parent": "mechanical:block/bear_rug",
    "textures": {"particle": "mechanical:block/puma_rug", "top": "mechanical:block/puma_rug"}})
w(A / "blockstates/puma_rug.json", {"variants": {f"facing={f}": {"model": "mechanical:block/puma_rug", **({"y": y} if y else {})}
                                                 for f, y in ROT}})
flat_item("puma_rug")
w(D / "mechanical/loot_table/blocks/puma_rug.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [
    {"type": "minecraft:item", "name": M + "puma_rug"}], "condition": {"type": "minecraft:survives_explosion"}}],
    "random_sequence": "mechanical:blocks/puma_rug"})

# ---- condor: no carcass, feathers (like the vulture) ----
U = lambda lo, hi: {"type": "minecraft:uniform", "min_inclusive": lo, "max_inclusive": hi}
count = lambda lo, hi: {"type": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": lo, "max": hi}}
w(D / "mechanical/loot_table/entities/condor.json", {"type": "minecraft:entity", "pools": [
    {"rolls": 1, "entries": [{"type": "minecraft:item", "name": i, "modifier": [count(lo, hi)]}]}
    for i, lo, hi in (("minecraft:feather", 3, 5), (M + "condor_feather", 1, 3))], "random_sequence": "mechanical:entities/condor"})

# ---- tags ----
RAW = [M + r for r in MEATS]
COOKED = [M + c for c, _, _ in MEATS.values()]
item_tag("perishes_1_day", *RAW)
item_tag("perishes_3_days", *COOKED)
item_tag("rots_to_flesh", *RAW, *COOKED)
item_tag("smokable_meat", *RAW)
item_tag("tanning_fats", M + "camel_hump_fat")

# ---- world spawns ----
DESERT = ["minecraft:desert", "mechanical:gypsum_flats"]
BADLANDS = ["minecraft:badlands", "minecraft:eroded_badlands", "minecraft:wooded_badlands", "mechanical:ochre_badlands"]
SPAWNS = {  # entity -> (biomes, weight, min, max)
    "desert_scorpion": (DESERT + BADLANDS, 6, 1, 2),
    "fennec_fox": (DESERT, 4, 1, 2),
    "wild_camel": (DESERT, 3, 2, 4),
    "sand_viper": (DESERT, 5, 1, 1),
    "puma": (BADLANDS, 1, 1, 1),
    "bighorn_sheep": (BADLANDS, 4, 2, 4),
    "rattlesnake": (BADLANDS, 5, 1, 1),
    "condor": (BADLANDS, 2, 1, 3),
}
for ent, (biomes, weight, lo, hi) in SPAWNS.items():
    w(D / f"mechanical/neoforge/biome_modifier/add_{ent}_desert.json", {"type": "neoforge:add_spawns", "biomes": biomes,
      "spawners": [{"type": M + ent, "weight": weight, "count": U(lo, hi)}]})

merge(A / "lang/en_us.json", lang)
print("fauna desert ok")
