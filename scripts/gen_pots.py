"""Vanilla's Flower Pot and Decorated Pot as pottery: thrown on the Potter's Wheel as greenware (Unfired Flower Pot,
2 Clay Balls / Unfired Decorated Pot, 4 Clay Balls) and fired in the Clay Kiln at 600° (KilnBlockEntity.RECIPES).
Pottery Sherds are pressed into a finished Unfired Decorated Pot while it is on the wheel (PottersWheelBlockEntity);
the pot_decorations component carries through the kiln. Writes the greenware item models, item model definitions,
lang, and disables vanilla's crafting recipes for both pots (flower_pot, decorated_pot_simple, decorated_pot).
Textures: gen_pottery_textures.py. Generator order: ... gen_thread.py, gen_pots.py, gen_block_items.py ...
Run: python scripts/gen_pots.py"""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent / "src/main/resources"
A, D = ROOT / "assets/mechanical", ROOT / "data"
M = "mechanical:"
NEVER = {"neoforge:conditions": [{"type": "neoforge:never"}]}


def w(path, obj):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, indent=2) + "\n", encoding="utf-8")


def merge(path, obj):
    old = json.loads(path.read_text(encoding="utf-8")) if path.exists() else {}
    old.update(obj)
    path.write_text(json.dumps(old, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")


def faces(tex, uvs):
    return {f: {"uv": uv, "texture": tex} for f, uv in uvs.items()}


# ---------------- Unfired Flower Pot: vanilla's flower pot geometry (4 walls, a floor), in raw clay ----------------
# block/block parent for the held/GUI transforms (vanilla's flower_pot model has none: its item is a flat sprite)
FP = "#pot"
flower_pot = [
    {"from": [5, 0, 5], "to": [6, 6, 11], "faces": faces(FP, {"down": [5, 5, 6, 11], "up": [5, 5, 6, 11], "north": [10, 10, 11, 16],
                                                             "south": [5, 10, 6, 16], "west": [5, 10, 11, 16], "east": [5, 10, 11, 16]})},
    {"from": [10, 0, 5], "to": [11, 6, 11], "faces": faces(FP, {"down": [10, 5, 11, 11], "up": [10, 5, 11, 11], "north": [5, 10, 6, 16],
                                                               "south": [10, 10, 11, 16], "west": [5, 10, 11, 16], "east": [5, 10, 11, 16]})},
    {"from": [6, 0, 5], "to": [10, 6, 6], "faces": faces(FP, {"down": [6, 10, 10, 11], "up": [6, 5, 10, 6], "north": [6, 10, 10, 16],
                                                             "south": [6, 10, 10, 16]})},
    {"from": [6, 0, 10], "to": [10, 6, 11], "faces": faces(FP, {"down": [6, 5, 10, 6], "up": [6, 10, 10, 11], "north": [6, 10, 10, 16],
                                                               "south": [6, 10, 10, 16]})},
    {"from": [6, 0, 6], "to": [10, 4, 10], "faces": {"down": {"uv": [6, 12, 10, 16], "texture": FP},
                                                     "up": {"uv": [6, 6, 10, 10], "texture": "#floor"}}},
]
w(A / "models/item/unfired_flower_pot.json", {"parent": "minecraft:block/block", "ambientocclusion": False, "textures": {
    "particle": M + "block/unfired_flower_pot", "pot": M + "block/unfired_flower_pot", "floor": M + "block/unfired_decorated_pot_top"},
    "elements": flower_pot})

# ---------------- Unfired Decorated Pot: vanilla's shape (14 px body, narrow neck, wide rim), in raw clay ----------------
S, T, MO = "#side", "#top", "#mouth"
decorated_pot = [
    {"from": [1, 0, 1], "to": [15, 16, 15], "faces": {**faces(S, {d: [1, 0, 15, 16] for d in ("north", "south", "east", "west")}),
                                                     "up": {"uv": [1, 1, 15, 15], "texture": T}, "down": {"uv": [1, 1, 15, 15], "texture": T}}},
    {"from": [5, 16, 5], "to": [11, 17, 11], "faces": faces(S, {d: [5, 1, 11, 2] for d in ("north", "south", "east", "west")})},
    {"from": [4, 17, 4], "to": [12, 20, 12], "faces": {**faces(S, {d: [4, 2, 12, 5] for d in ("north", "south", "east", "west")}),
                                                      "up": {"uv": [4, 4, 12, 12], "texture": MO}, "down": {"uv": [4, 4, 12, 12], "texture": T}}},
]
w(A / "models/item/unfired_decorated_pot.json", {"parent": "minecraft:block/block", "textures": {
    "particle": M + "block/unfired_decorated_pot_side", "side": M + "block/unfired_decorated_pot_side",
    "top": M + "block/unfired_decorated_pot_top", "mouth": M + "block/unfired_decorated_pot_mouth"}, "elements": decorated_pot})

for item in ("unfired_flower_pot", "unfired_decorated_pot"):
    w(A / f"items/{item}.json", {"model": {"type": "minecraft:model", "model": M + f"item/{item}"}})

# ---------------- vanilla crafting off: both pots come from the wheel and the kiln (Bricks stay) ----------------
# recipe + its unlock advancement (registry loading fails without the latter); decorated_pot (sherds) has no unlock
for name, cat in [("flower_pot", "decorations"), ("decorated_pot_simple", "decorations"), ("decorated_pot", None)]:
    w(D / f"minecraft/recipe/{name}.json", NEVER)
    if cat:
        w(D / f"minecraft/advancement/recipes/{cat}/{name}.json", NEVER)

merge(A / "lang/en_us.json", {
    "item.mechanical.unfired_flower_pot": "Unfired Flower Pot",
    "item.mechanical.unfired_decorated_pot": "Unfired Decorated Pot",
    "message.mechanical.wheel.sherd": "Pressed the %s into the %s",
    "message.mechanical.wheel.side.front": "front",
    "message.mechanical.wheel.side.left": "left side",
    "message.mechanical.wheel.side.right": "right side",
    "message.mechanical.wheel.side.back": "back",
    "message.mechanical.wheel.sherds_full": "All four sides of the pot are decorated",
    "message.mechanical.wheel.sherd_needs_pot": "Throw an Unfired Decorated Pot first: sherds are pressed into it while it is still on the wheel",
})
print("pots ok")
