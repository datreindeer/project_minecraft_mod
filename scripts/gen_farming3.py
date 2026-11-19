"""Bronze Age farming, phase 3 (docs/farming.md): survival recipes for the Quern, Baking Stone, Compost Heap and Scarecrow
(each needs a Copper or Bronze Ingot, so they wait for casting), their recipe-book unlocks (carrying an ingot, not
cobblestone or sticks), the livestock that drop Manure (entity tag mechanical:drops_manure, Farming.dropManure), Manure in
the Compost Heap, and shelf lives for Flour, Dough and Bread (merged into gen_preserving's perishes_* tags, which is why
this runs after gen_preserving.py). The blocks, items, models and textures are gen_farming.py / gen_farming_textures.py.
Generator order: ... gen_biomes.py, gen_wolf_pelt.py, gen_farming3.py, gen_block_items.py, gen_unlocks.py, gen_registry.py.
Run: python scripts/gen_farming3.py"""
import json
from pathlib import Path

D = Path(__file__).resolve().parent.parent / "src/main/resources/data"
M = "mechanical:"
INGOT = ["minecraft:copper_ingot", M + "bronze_ingot"]  # Poor Bronze ingots too (same item)


def w(path, obj):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, indent=2) + "\n")


def tag_add(path, *values):
    old = json.loads(path.read_text(encoding="utf-8")) if path.exists() else {"values": []}
    old["values"] += [v for v in values if v not in old["values"]]
    w(path, old)


RECIPES = {  # name: (pattern, key) -- C is the metal part
    # a metal axle (spindle) through the two millstones
    "quern": ([" C ", "#S#", "###"], {"C": INGOT, "#": "minecraft:cobblestone", "S": "minecraft:stick"}),
    # rock chunks around a copper griddle plate
    "baking_stone": (["RCR"], {"R": "#mechanical:rock_chunks", "C": INGOT}),
    # copper nails hold the wattle together
    "compost_heap": (["S S", "SLS", "SCS"], {"S": "minecraft:stick", "L": "minecraft:leaf_litter", "C": INGOT}),
    # a copper bell on the arm rattles in the wind
    "scarecrow": (["CP ", "WHW", " S "], {"C": INGOT, "P": "minecraft:carved_pumpkin", "W": M + "straw", "H": M + "raw_hide",
                                          "S": "minecraft:stick"}),
}
for name, (pattern, key) in RECIPES.items():
    w(D / f"mechanical/recipe/{name}.json", {"type": "minecraft:crafting_shaped", "category": "misc", "pattern": pattern, "key": key,
                                              "result": {"id": M + name}})
w(D / "mechanical/advancement/recipes/farming_bronze_age.json", {"parent": "minecraft:recipes/root",
    "criteria": {"has_ingot": {"trigger": "minecraft:inventory_changed", "conditions": {"items": [{"items": INGOT}]}}},
    "requirements": [["has_ingot"]], "rewards": {"recipes": [M + r for r in RECIPES]}})

# adults drop ~1 Manure per 10 minutes (Farming.MANURE_CHECK / MANURE_CHANCE)
w(D / "mechanical/tags/entity_type/drops_manure.json", {"values": ["minecraft:cow", "minecraft:mooshroom", "minecraft:sheep", "minecraft:pig",
    "minecraft:goat", "minecraft:horse", "minecraft:donkey", "minecraft:mule", M + "aurochs", M + "wild_horse"]})
tag_add(D / "mechanical/tags/item/compostable.json", M + "manure")  # fills the heap two steps (CompostHeapBlock)

# shelf lives: flour keeps (dry), dough sours fast, bread goes stale like cooked meat
tag_add(D / "mechanical/tags/item/perishes_30_days.json", M + "flour")
tag_add(D / "mechanical/tags/item/perishes_1_day.json", M + "dough")
tag_add(D / "mechanical/tags/item/perishes_3_days.json", "minecraft:bread")
print(f"farming3 ok ({len(RECIPES)} recipes)")
