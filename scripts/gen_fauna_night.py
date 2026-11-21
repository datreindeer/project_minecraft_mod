"""Data for the night monsters (wildlife/night/NightFauna.java): Gloson and Mushroom Man, in the vanilla Creeper's place.

- items: Raw/Cooked Gloson, Gloson Bristles, Razor Ridge, Razor Knife, Mushroom Flesh, Spore Sac, Spore Bomb, two spawn
  eggs (models, lang); the Bleeding effect's name
- recipes: Raw Gloson on the Campfire (20 s) or in the smoker; Razor Knife (Razor Ridge + Stick + Sinew); a Paintbrush from
  Gloson Bristles + Stick; Spore Bomb (Spore Sac + Puffball). Mushroom Flesh is tinder (Tinder.java), no recipe.
- tags: Gloson meat perishes (1 day raw, 3 cooked), rots to flesh, is smokable; the Razor Knife is a knife and is repaired
  with a Razor Ridge
- loot: the Mushroom Man drops Mushroom Flesh and sometimes a Spore Sac (the Gloson leaves a carcass: Butchery)
- spawns: extra Mushroom Men in forests and swamps (they also take most of the creeper's share there, NightFauna#create)
Textures: scripts/gen_fauna_night_textures.py. The Gloson's butchering yield is registered in NightFauna.init().
Generator order: ... gen_roots.py, gen_fauna_* (alphabetical, so this runs after gen_fauna_mountain.py and before
gen_fauna_river.py), gen_block_items.py, gen_unlocks.py, gen_advancements.py, gen_registry.py.
Run: python scripts/gen_fauna_night.py"""
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
    path.write_text(json.dumps(old, indent=2) + "\n", encoding="utf-8")


def tag_add(path, *values):
    old = json.loads(path.read_text(encoding="utf-8")) if path.exists() else {"values": []}
    old["values"] += [v for v in values if v not in old["values"]]
    w(path, old)


def item_tag(name, *values):
    ns, path = name.split(":") if ":" in name else ("mechanical", name)
    tag_add(D / f"{ns}/tags/item/{path}.json", *values)


def flat_item(item, parent="minecraft:item/generated"):
    w(A / f"models/item/{item}.json", {"parent": parent, "textures": {"layer0": f"mechanical:item/{item}"}})
    w(A / f"items/{item}.json", {"model": {"type": "minecraft:model", "model": f"mechanical:item/{item}"}})


def shapeless(name, ingredients, result, count=1, category="misc"):
    w(D / f"mechanical/recipe/{name}.json", {"type": "minecraft:crafting_shapeless", "category": category,
        "ingredients": ingredients, "result": {"id": result, "count": count}})


ANIMALS = {"gloson": "Gloson", "mushroom_man": "Mushroom Man"}
ITEMS = {"raw_gloson": "Raw Gloson", "cooked_gloson": "Cooked Gloson", "gloson_bristles": "Gloson Bristles",
         "razor_ridge": "Razor Ridge", "mushroom_flesh": "Mushroom Flesh", "spore_sac": "Spore Sac", "spore_bomb": "Spore Bomb"}

lang = {"item.mechanical.razor_knife": "Razor Knife", "effect.mechanical.bleeding": "Bleeding"}
for a, name in ANIMALS.items():
    lang[f"entity.mechanical.{a}"] = name
    lang[f"item.mechanical.{a}_spawn_egg"] = f"{name} Spawn Egg"
    flat_item(f"{a}_spawn_egg")
for item, name in ITEMS.items():
    lang[f"item.mechanical.{item}"] = name
    flat_item(item)
flat_item("razor_knife", "minecraft:item/handheld")

# ---- Gloson meat ----
for kind, ticks in (("campfire_cooking", 400), ("smoking", 100)):
    w(D / f"mechanical/recipe/cooked_gloson_from_{kind}.json", {"type": f"minecraft:{kind}", "category": "food",
        "cookingtime": ticks, "experience": 0.1, "ingredient": M + "raw_gloson", "result": {"id": M + "cooked_gloson"}})
item_tag("perishes_1_day", M + "raw_gloson")
item_tag("perishes_3_days", M + "cooked_gloson")
item_tag("rots_to_flesh", M + "raw_gloson", M + "cooked_gloson")
item_tag("smokable_meat", M + "raw_gloson")

# ---- goods ----
shapeless("razor_knife", [M + "razor_ridge", MC + "stick", M + "sinew"], M + "razor_knife", category="equipment")
w(D / "mechanical/tags/item/repairs_razor_knife.json", {"values": [M + "razor_ridge"]})
item_tag("knives", M + "razor_knife")
shapeless("paintbrush_from_gloson_bristles", [MC + "stick", M + "gloson_bristles"], M + "paintbrush", category="equipment")
shapeless("spore_bomb", [M + "spore_sac", M + "puffball"], M + "spore_bomb", category="equipment")

# ---- the Mushroom Man's drops (26.x loot format: condition/type/modifier) ----
def drop(item, lo, hi, chance=None):
    pool = {"rolls": 1, "entries": [{"type": "minecraft:item", "name": M + item, "modifier": [
        {"type": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": lo, "max": hi}},
        {"type": "minecraft:enchanted_count_increase", "count": {"type": "minecraft:uniform", "min": 0.0, "max": 1.0},
         "enchantment": "minecraft:looting"}]}]}
    if chance:
        pool["condition"] = {"type": "minecraft:random_chance", "chance": chance}
    return pool


w(D / "mechanical/loot_table/entities/mushroom_man.json", {"type": "minecraft:entity", "pools": [
    drop("mushroom_flesh", 1, 2), drop("spore_sac", 1, 1, 0.5)], "random_sequence": "mechanical:entities/mushroom_man"})

# ---- spawns: extra Mushroom Men in the damp woods (the creeper's own spawns are replaced in NightFauna) ----
for place, biomes in (("forest", "#minecraft:is_forest"), ("swamp", "#c:is_swamp")):
    w(D / f"mechanical/neoforge/biome_modifier/add_mushroom_man_{place}.json", {"type": "neoforge:add_spawns", "biomes": biomes,
        "spawners": [{"type": M + "mushroom_man", "weight": 40, "count": {"type": "minecraft:uniform", "min_inclusive": 1, "max_inclusive": 2}}]})

merge(A / "lang/en_us.json", lang)
print("fauna night ok")
