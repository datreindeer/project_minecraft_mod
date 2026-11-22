"""Feeding Trough (living-world plan D6; FeedingTroughBlock): a plank trough that joins with troughs beside it.

Writes the multipart blockstate (floor; a rim on each side that has no trough next to it; water at 3 levels, tinted like
water; a layer of fodder), the models (vanilla spruce planks, water and hay textures, so no textures of its own), the item
model, loot, recipe ([Planks][ ][Planks] / [Planks][Planks][Planks]), lang and the axe tag.

Generator order: after gen_dirt_slab.py, before gen_block_items.py, gen_unlocks.py, gen_advancements.py and
gen_loot_format.py. Run: python scripts/gen_husbandry.py
"""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
A, D = ROOT / "src/main/resources/assets/mechanical", ROOT / "src/main/resources/data"
M = "mechanical:"
PLANKS, WATER, HAY = "minecraft:block/spruce_planks", "minecraft:block/water_still", "minecraft:block/hay_block_top"


def w(path, obj):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, indent=2) + "\n", encoding="utf-8")


def box(frm, to, tex="#wood", faces=("north", "south", "east", "west", "up", "down"), **extra):
    return {"from": frm, "to": to, "faces": {f: {"texture": tex, **extra} for f in faces}}


FLOOR = box([0, 0, 0], [16, 2, 16])
WALL = box([0, 2, 0], [16, 10, 2])  # the north rim; the blockstate turns it to the other sides


def model(name, elements, textures):
    w(A / f"models/block/{name}.json", {"parent": "minecraft:block/block", "textures": {"particle": PLANKS, **textures}, "elements": elements})


model("feeding_trough_floor", [FLOOR], {"wood": PLANKS})
model("feeding_trough_wall", [WALL], {"wood": PLANKS})
for level, top in ((1, 4), (2, 6), (3, 8)):
    model(f"feeding_trough_water{level}", [box([0, 2, 0], [16, top, 16], "#water", ("up",), tintindex=0)], {"water": WATER})
model("feeding_trough_food", [box([2, 2, 2], [14, 5, 14], "#hay", ("north", "south", "east", "west", "up"))], {"hay": HAY})
model("feeding_trough", [FLOOR, WALL, box([0, 2, 14], [16, 10, 16]), box([0, 2, 2], [2, 10, 14]), box([14, 2, 2], [16, 10, 14])], {"wood": PLANKS})

parts = [{"apply": {"model": M + "block/feeding_trough_floor"}}]
for side, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270)):
    parts.append({"when": {side: "false"}, "apply": {"model": M + "block/feeding_trough_wall", **({"y": y, "uvlock": True} if y else {})}})
for level in (1, 2, 3):
    parts.append({"when": {"water": str(level)}, "apply": {"model": M + f"block/feeding_trough_water{level}"}})
parts.append({"when": {"food": "true"}, "apply": {"model": M + "block/feeding_trough_food"}})
w(A / "blockstates/feeding_trough.json", {"multipart": parts})
w(A / "items/feeding_trough.json", {"model": {"type": "minecraft:model", "model": M + "block/feeding_trough"}})

w(D / "mechanical/loot_table/blocks/feeding_trough.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [
    {"type": "minecraft:item", "name": M + "feeding_trough"}], "condition": {"type": "minecraft:survives_explosion"}}],
    "random_sequence": "mechanical:blocks/feeding_trough"})
w(D / "mechanical/recipe/feeding_trough.json", {"type": "minecraft:crafting_shaped", "category": "misc", "pattern": ["P P", "PPP"],
                                                "key": {"P": "#minecraft:planks"}, "result": {"id": M + "feeding_trough"}})

tag_path = D / "minecraft/tags/block/mineable/axe.json"
tag = json.loads(tag_path.read_text(encoding="utf-8")) if tag_path.exists() else {"values": []}
if M + "feeding_trough" not in tag["values"]:
    tag["values"].append(M + "feeding_trough")
w(tag_path, tag)

lang_path = A / "lang/en_us.json"
lang = json.loads(lang_path.read_text(encoding="utf-8"))
lang["block.mechanical.feeding_trough"] = "Feeding Trough"
lang_path.write_text(json.dumps(lang, indent=2) + "\n", encoding="utf-8")
