"""Mold Chest (living-world plan F7; MoldChestBlock): a plank chest daubed with clay, 27 slots, molds only.

Writes the tag mechanical:molds (every *_mold item: unfired clay, fired clay and soapstone), the blockstate and model
(a chest box with a lid seam and a clay latch), loot, the recipe ([Planks][Clay Ball][Planks] x clay ring), lang and the
three textures (painted here: plank frame round a clay panel; the front has an ingot-mold imprint).

Generator order: after gen_moldbench.py and gen_stone_crafts.py (their mold items), before gen_block_items.py,
gen_unlocks.py, gen_advancements.py and gen_loot_format.py. Run: python scripts/gen_mold_chest.py
"""
import json
import random
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
A, D = ROOT / "src/main/resources/assets/mechanical", ROOT / "src/main/resources/data"
M = "mechanical:"


def w(path, obj):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, indent=2) + "\n", encoding="utf-8")


w(D / "mechanical/tags/item/molds.json", {"values": [M + f.stem for f in sorted((A / "items").glob("*_mold.json"))]})

# ---- textures ----
PLANK = [(92, 62, 36), (112, 78, 46), (130, 92, 56), (146, 106, 66)]
CLAY = [(138, 84, 62), (156, 98, 72), (170, 112, 84), (184, 128, 98)]
SEAM, DARK = (58, 38, 24), (110, 64, 46)


def panel(seed, border=2):
    rnd = random.Random(seed)
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            frame = x < border or x >= 16 - border or y < border or y >= 16 - border
            if frame:
                c = PLANK[min(3, max(0, 2 + (1 if (x + y * 3) % 7 == 0 else 0) - (1 if rnd.random() < 0.25 else 0)))]
            else:
                c = CLAY[min(3, max(0, 2 + rnd.choice((-1, 0, 0, 0, 1)) - (1 if rnd.random() < 0.1 else 0)))]
            img.putpixel((x, y), c + (255,))
    return img


side = panel(1)
for x in range(16):
    side.putpixel((x, 6), SEAM + (255,))  # lid seam: model height 10 of 14 (uv rows 2..16)
front = panel(2)
for x in range(16):
    front.putpixel((x, 6), SEAM + (255,))
for x in range(5, 11):  # an ingot mold pressed into the clay below the seam
    for y in range(10, 13):
        edge = x in (5, 10) or y in (10, 12)
        front.putpixel((x, y), (DARK if edge else CLAY[0]) + (255,))
top = panel(3)
for p in [(x, y) for x in (4, 11) for y in range(4, 12)] + [(x, y) for y in (4, 11) for x in range(4, 12)]:
    top.putpixel(p, PLANK[1] + (255,))  # cross battens on the lid
for name, img in (("mold_chest_side", side), ("mold_chest_front", front), ("mold_chest_top", top)):
    img.save(A / f"textures/block/{name}.png")

# ---- model: chest box 1..15 x 0..14 with a latch, front to the north ----
faces = lambda tex, uv: {"texture": tex, "uv": uv}
w(A / "models/block/mold_chest.json", {"parent": "minecraft:block/block", "textures": {
    "particle": M + "block/mold_chest_side", "side": M + "block/mold_chest_side", "front": M + "block/mold_chest_front",
    "top": M + "block/mold_chest_top"}, "elements": [
    {"from": [1, 0, 1], "to": [15, 14, 15], "faces": {
        "north": faces("#front", [1, 2, 15, 16]), "south": faces("#side", [1, 2, 15, 16]), "east": faces("#side", [1, 2, 15, 16]),
        "west": faces("#side", [1, 2, 15, 16]), "up": faces("#top", [1, 1, 15, 15]), "down": {**faces("#top", [1, 1, 15, 15]), "cullface": "down"}}},
    {"from": [7, 7, 0], "to": [9, 11, 1], "faces": {d: faces("#side", [0, 0, 2, 4]) for d in ("north", "east", "west", "up", "down")}}]})
w(A / "blockstates/mold_chest.json", {"variants": {f"facing={f}": {"model": M + "block/mold_chest", **({"y": y} if y else {})}
                                                   for f, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270))}})
w(D / "mechanical/loot_table/blocks/mold_chest.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [
    {"type": "minecraft:item", "name": M + "mold_chest"}], "condition": {"type": "minecraft:survives_explosion"}}],
    "random_sequence": "mechanical:blocks/mold_chest"})
w(D / "mechanical/recipe/mold_chest.json", {"type": "minecraft:crafting_shaped", "category": "misc", "pattern": ["PCP", "C C", "PCP"],
                                            "key": {"P": "#minecraft:planks", "C": "minecraft:clay_ball"}, "result": {"id": M + "mold_chest"}})
tag_path = D / "minecraft/tags/block/mineable/axe.json"
tag = json.loads(tag_path.read_text(encoding="utf-8")) if tag_path.exists() else {"values": []}
if M + "mold_chest" not in tag["values"]:
    tag["values"].append(M + "mold_chest")
w(tag_path, tag)

lang_path = A / "lang/en_us.json"
lang = json.loads(lang_path.read_text(encoding="utf-8"))
lang["block.mechanical.mold_chest"] = "Mold Chest"
lang_path.write_text(json.dumps(lang, indent=2) + "\n", encoding="utf-8")
