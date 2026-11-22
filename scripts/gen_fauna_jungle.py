"""Data for the jungle and mangrove fauna (wildlife/jungle/JungleFauna.java): Tiger, Python, Orangutan, Monitor Lizard,
Poison Dart Frog, Saltwater Crocodile, Mudskipper, Fiddler Crab, Proboscis Monkey.

- item models (flat items, the Tiger Rug block item), lang (entities, items, spawn eggs, the rug)
- recipes: every meat on the Campfire / in the smoker; the tiger pelt and crocodile hide fat-tanned in the grid (knife +
  fat); Tiger Rug (2 pelts + Bone), Tiger Pelt Mantle (2 pelts + 2 Sinew); Python Skin -> 3 Snakeskin; Orangutan Hair ->
  String; Monitor Skin -> Leather; Blowpipe (3 Bamboo), Darts (Bamboo + Feather), Poison Darts (Darts + Frog Poison or a
  Venom Gland). The Crocodile Hide armour is sewn at the Leatherworking Bench (LeatherworkingRecipes.java).
- blocks: the Tiger Rug (Bear Rug model)
- tags: spoilage / smoking, snare bait (mudskipper), cloak slot + chest armour (mantle), armour slots and repair tags,
  basket storage for the pelts
- loot: the frog (Frog Poison), mudskipper (itself) and crab (Raw Crab Meat) leave no carcass and drop these
- equipment assets tiger_fur / crocodile_hide; world spawns (biome modifiers add_<animal>.json)
Textures: scripts/gen_fauna_jungle_textures.py. Docs: docs/fauna/jungle.md.
Generator order: ... gen_roots.py, gen_fauna_*.py (alphabetical), gen_block_items.py, gen_unlocks.py, gen_advancements.py,
gen_registry.py.
Run: python scripts/gen_fauna_jungle.py"""
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
    path.write_text(json.dumps(old, indent=2) + "\n", encoding="utf-8")  # the lang file is kept ASCII-escaped


def tag_add(path, *values):
    old = json.loads(path.read_text(encoding="utf-8")) if path.exists() else {"values": []}
    old["values"] += [v for v in values if v not in old["values"]]
    w(path, old)


def item_tag(name, *values):
    ns, path = name.split(":") if ":" in name else ("mechanical", name)
    tag_add(D / f"{ns}/tags/item/{path}.json", *values)


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


def cook(name, ingredient, result, ticks=400):
    for kind in ("campfire_cooking", "smoking"):
        recipe(f"{name}_from_{kind}", {"type": f"minecraft:{kind}", "category": "food",
               "cookingtime": ticks if kind == "campfire_cooking" else ticks // 4, "experience": 0.1,
               "ingredient": ingredient, "result": {"id": result, "count": 1}})


ANIMALS = {"tiger": "Tiger", "python": "Python", "orangutan": "Orangutan", "monitor_lizard": "Monitor Lizard",
           "poison_dart_frog": "Poison Dart Frog", "saltwater_crocodile": "Saltwater Crocodile", "mudskipper": "Mudskipper",
           "fiddler_crab": "Fiddler Crab", "proboscis_monkey": "Proboscis Monkey"}
# raw -> (cooked, raw name, cooked name)
MEATS = {
    "tiger_meat": ("cooked_tiger_meat", "Tiger Meat", "Cooked Tiger Meat"),
    "python_meat": ("cooked_python_meat", "Python Meat", "Cooked Python Meat"),
    "orangutan_meat": ("cooked_orangutan_meat", "Orangutan Meat", "Cooked Orangutan Meat"),
    "monitor_meat": ("cooked_monitor_meat", "Monitor Meat", "Cooked Monitor Meat"),
    "crocodile_meat": ("cooked_crocodile_meat", "Crocodile Meat", "Cooked Crocodile Meat"),
    "monkey_meat": ("cooked_monkey_meat", "Monkey Meat", "Cooked Monkey Meat"),
    "mudskipper": ("cooked_mudskipper", "Mudskipper", "Cooked Mudskipper"),
    "raw_crab_meat": ("cooked_crab_meat", "Raw Crab Meat", "Cooked Crab Meat"),
}
ITEMS = {
    "raw_tiger_pelt": "Raw Tiger Pelt", "tiger_pelt": "Tiger Pelt", "tiger_pelt_mantle": "Tiger Pelt Mantle",
    "python_skin": "Python Skin", "orangutan_hair": "Orangutan Hair", "monitor_skin": "Monitor Skin", "frog_poison": "Frog Poison",
    "raw_crocodile_hide": "Raw Crocodile Hide", "crocodile_hide": "Crocodile Hide",
    "crocodile_hide_helmet": "Crocodile Hide Helmet", "crocodile_hide_cuirass": "Crocodile Hide Cuirass",
    "crocodile_hide_leggings": "Crocodile Hide Leggings", "crocodile_hide_boots": "Crocodile Hide Boots",
    "blowpipe": "Blowpipe", "dart": "Dart", "poison_dart": "Poison Dart",
}

lang = {f"entity.mechanical.{k}": v for k, v in ANIMALS.items()}
lang.update({f"item.mechanical.{k}_spawn_egg": f"{v} Spawn Egg" for k, v in ANIMALS.items()})
lang.update({f"item.mechanical.{k}": v for k, v in ITEMS.items()})
lang["block.mechanical.tiger_rug"] = "Tiger Rug"

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
    cook(cooked, M + raw, M + cooked, 300 if raw in ("mudskipper", "raw_crab_meat") else 400)

RAW = [M + r for r in MEATS]
COOKED = [M + c for c, _, _ in MEATS.values()]
item_tag("perishes_1_day", *RAW)
item_tag("perishes_3_days", *COOKED)
item_tag("rots_to_flesh", *RAW, *COOKED)
item_tag("smokable_meat", *[M + r for r in MEATS if r not in ("mudskipper", "raw_crab_meat")])
item_tag("snare_bait", M + "mudskipper")
item_tag("tanning_fats", M + "tallow", M + "lard", M + "bear_fat")
item_tag("basket_storable", M + "raw_tiger_pelt", M + "tiger_pelt", M + "raw_crocodile_hide", M + "crocodile_hide")

# ---- recipes ----
# fat-tanned in the grid like the leopard pelt (the knife stays in the grid)
shapeless("tiger_pelt", [M + "raw_tiger_pelt", "#mechanical:knives", "#mechanical:tanning_fats"], M + "tiger_pelt")
shapeless("crocodile_hide", [M + "raw_crocodile_hide", "#mechanical:knives", "#mechanical:tanning_fats"], M + "crocodile_hide")
shapeless("tiger_rug", [M + "tiger_pelt", M + "tiger_pelt", "minecraft:bone"], M + "tiger_rug", category="building")
shapeless("tiger_pelt_mantle", [M + "tiger_pelt", M + "tiger_pelt", M + "sinew", M + "sinew"], M + "tiger_pelt_mantle", category="equipment")
shapeless("snakeskin_from_python_skin", [M + "python_skin", "#mechanical:knives"], M + "snakeskin", 3)
shapeless("string_from_orangutan_hair", [M + "orangutan_hair", M + "orangutan_hair"], "minecraft:string")
shapeless("leather_from_monitor_skin", [M + "monitor_skin", M + "monitor_skin"], "minecraft:leather")
shaped("blowpipe", ["BBB"], {"B": "minecraft:bamboo"}, M + "blowpipe", category="equipment")
shapeless("dart", ["minecraft:bamboo", "minecraft:feather"], M + "dart", 4, category="equipment")
shapeless("poison_dart", [M + "dart"] * 4 + [M + "frog_poison"], M + "poison_dart", 4, category="equipment")
shapeless("poison_dart_from_venom_gland", [M + "dart"] * 4 + [M + "venom_gland"], M + "poison_dart", 4, category="equipment")

# ---- wearables ----
item_tag("equipment/cloak", M + "tiger_pelt_mantle")
item_tag("minecraft:chest_armor", M + "tiger_pelt_mantle", M + "crocodile_hide_cuirass")
item_tag("minecraft:head_armor", M + "crocodile_hide_helmet")
item_tag("minecraft:leg_armor", M + "crocodile_hide_leggings")
item_tag("minecraft:foot_armor", M + "crocodile_hide_boots")
w(D / "mechanical/tags/item/repairs_tiger_fur.json", {"values": [M + "tiger_pelt"]})
w(D / "mechanical/tags/item/repairs_crocodile_hide_armor.json", {"values": [M + "crocodile_hide"]})
w(A / "equipment/tiger_fur.json", {"layers": {"humanoid": [{"texture": "mechanical:tiger_fur"}]}})
w(A / "equipment/crocodile_hide.json", {"layers": {"humanoid": [{"texture": "mechanical:crocodile_hide"}],
                                                   "humanoid_leggings": [{"texture": "mechanical:crocodile_hide"}]}})

# ---- Tiger Rug: flat like the Bear Rug ----
ROT = [("north", 0), ("east", 90), ("south", 180), ("west", 270)]
w(A / "models/block/tiger_rug.json", {"parent": "mechanical:block/bear_rug",
    "textures": {"particle": "mechanical:block/tiger_rug", "top": "mechanical:block/tiger_rug"}})
w(A / "blockstates/tiger_rug.json", {"variants": {f"facing={f}": {"model": "mechanical:block/tiger_rug", **({"y": y} if y else {})}
                                                  for f, y in ROT}})
flat_item("tiger_rug")
w(D / "mechanical/loot_table/blocks/tiger_rug.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [
    {"type": "minecraft:item", "name": M + "tiger_rug"}], "condition": {"type": "minecraft:survives_explosion"}}],
    "random_sequence": "mechanical:blocks/tiger_rug"})

# ---- loot of the small ones (no carcass) ----
for ent, item in (("poison_dart_frog", "frog_poison"), ("mudskipper", "mudskipper"), ("fiddler_crab", "raw_crab_meat")):
    w(D / f"mechanical/loot_table/entities/{ent}.json", {"type": "minecraft:entity", "pools": [
        {"rolls": 1, "entries": [{"type": "minecraft:item", "name": M + item}]}], "random_sequence": f"mechanical:entities/{ent}"})

# ---- world spawns ----
U = lambda lo, hi: {"type": "minecraft:uniform", "min_inclusive": lo, "max_inclusive": hi}
JUNGLE = ["minecraft:jungle", "minecraft:sparse_jungle", "minecraft:bamboo_jungle"]
MANGROVE = ["minecraft:mangrove_swamp"]
SPAWNS = {  # entity -> (biomes, weight, min, max)
    "tiger": (["minecraft:jungle", "minecraft:bamboo_jungle"], 2, 1, 1),
    "python": (JUNGLE + MANGROVE, 3, 1, 1),
    "orangutan": (["minecraft:jungle"], 4, 1, 2),
    "monitor_lizard": (["minecraft:jungle", "minecraft:sparse_jungle"] + MANGROVE, 4, 1, 2),
    "poison_dart_frog": (["minecraft:jungle", "minecraft:bamboo_jungle"], 6, 2, 4),
    "saltwater_crocodile": (MANGROVE, 4, 1, 2),
    "mudskipper": (MANGROVE, 8, 2, 5),
    "fiddler_crab": (MANGROVE, 8, 2, 4),
    "proboscis_monkey": (MANGROVE, 5, 2, 4),
}
for ent, (biomes, weight, lo, hi) in SPAWNS.items():
    w(D / f"mechanical/neoforge/biome_modifier/add_{ent}.json", {"type": "neoforge:add_spawns", "biomes": biomes,
        "spawners": [{"type": M + ent, "weight": weight, "count": U(lo, hi)}]})

merge(A / "lang/en_us.json", lang)
print("fauna jungle ok")
