"""Phantom hide chain and Glider Cloak data: item models, lang, recipes, tags, the equipment asset and the recipe unlock.
Phantoms leave a carcass (Butchery.java) -> Raw Phantom Hide -> knife -> Scraped Phantom Hide -> Drying Rack in daylight
(DryingRackBlock.java; its models/loot live in gen_data.py) -> Phantom Leather -> Glider Cloak at the Armor Station
(ArmorStationRecipes.java) on a Glider Frame. Textures: scripts/gen_phantom_textures.py.
Generator order: ... gen_wind.py, gen_smoke.py, gen_phantom.py, gen_block_items.py, gen_registry.py.
Run: python scripts/gen_phantom.py"""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent / "src/main/resources"
A, D = ROOT / "assets/mechanical", ROOT / "data"


def w(path, obj):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, indent=2) + "\n")


def merge(path, obj):
    old = json.loads(path.read_text(encoding="utf-8")) if path.exists() else {}
    old.update(obj)
    path.write_text(json.dumps(old, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")


def tag_add(path, *values):
    old = json.loads(path.read_text(encoding="utf-8")) if path.exists() else {"values": []}
    old["values"] += [v for v in values if v not in old["values"]]
    w(path, old)


ITEMS = {"raw_phantom_hide": "Raw Phantom Hide", "scraped_phantom_hide": "Scraped Phantom Hide", "phantom_leather": "Phantom Leather",
         "glider_frame": "Glider Frame", "glider_cloak": "Glider Cloak"}
for item in ITEMS:
    w(A / f"models/item/{item}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"mechanical:item/{item}"}})
    w(A / f"items/{item}.json", {"model": {"type": "minecraft:model", "model": f"mechanical:item/{item}"}})
merge(A / "lang/en_us.json", {f"item.mechanical.{k}": v for k, v in ITEMS.items()})

# scraped with a knife like Raw Hide (the knife survives, losing 1)
w(D / "mechanical/recipe/scraped_phantom_hide.json", {"type": "minecraft:crafting_shapeless", "category": "misc",
    "ingredients": ["mechanical:raw_phantom_hide", "#mechanical:knives"], "result": {"id": "mechanical:scraped_phantom_hide", "count": 1}})
# the cloak's struts: 4 Tool Rods, 2 Bones, lashed with Sinew and Cordage
w(D / "mechanical/recipe/glider_frame.json", {"type": "minecraft:crafting_shaped", "category": "equipment",
    "pattern": ["RSR", "BCB", "R R"], "key": {"R": "mechanical:tool_rod", "S": "mechanical:sinew", "B": "minecraft:bone", "C": "mechanical:cordage"},
    "result": {"id": "mechanical:glider_frame"}})
for name, item, recipes in (("raw_phantom_hide", "mechanical:raw_phantom_hide", ["scraped_phantom_hide"]),
                            ("phantom_leather", "mechanical:phantom_leather", ["glider_frame"])):
    w(D / f"mechanical/advancement/recipes/{name}.json", {"parent": "minecraft:recipes/root",
        "criteria": {"has_item": {"trigger": "minecraft:inventory_changed", "conditions": {"items": [{"items": item}]}}},
        "requirements": [["has_item"]], "rewards": {"recipes": [f"mechanical:{r}" for r in recipes]}})

# Glider Cloak: chest armour (1), repaired with Phantom Leather; drawn as a membrane mantle plus folded wings on the back
w(D / "mechanical/tags/item/repairs_glider_cloak.json", {"values": ["mechanical:phantom_leather"]})
tag_add(D / "minecraft/tags/item/chest_armor.json", "mechanical:glider_cloak")
tag_add(D / "minecraft/tags/item/enchantable/durability.json", "mechanical:glider_cloak")
w(A / "equipment/glider_cloak.json", {"layers": {"humanoid": [{"texture": "mechanical:glider_cloak"}],
                                                  "wings": [{"texture": "mechanical:glider_cloak"}]}})
print("phantom ok")
