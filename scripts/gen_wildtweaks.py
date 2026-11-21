"""Data for vanilla creatures as game and the bat (wildlife/VanillaGame.java, GuanoPileBlock.java).

- items: Bat Wing, Guano (fertiliser +3), Night-Eye Salve; lang, item models
- Guano Pile block: blockstate (layers 1-4), models, loot table (one Guano per layer)
- recipe: 2 Bat Wing + Soot + Lard -> 2 Night-Eye Salve
Textures: scripts/gen_wildtweaks_textures.py. Advancements: gen_advancements.py (hunting: bat ...; food: guano;
wilderness: night_eye_salve, giant_deer_charge).
Generator order: ... gen_fauna_*.py, gen_tribes2.py, gen_wildtweaks.py, gen_block_items.py, gen_unlocks.py,
gen_advancements.py, gen_registry.py. Run: python scripts/gen_wildtweaks.py"""
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


def flat_item(item):
    w(A / f"models/item/{item}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"mechanical:item/{item}"}})
    w(A / f"items/{item}.json", {"model": {"type": "minecraft:model", "model": f"mechanical:item/{item}"}})


ITEMS = {"bat_wing": "Bat Wing", "guano": "Guano", "night_eye_salve": "Night-Eye Salve"}
LANG = {f"item.mechanical.{k}": v for k, v in ITEMS.items()}
LANG["block.mechanical.guano_pile"] = "Guano Pile"
for item in ITEMS:
    flat_item(item)

# ---- Guano Pile: a splatter on the floor at 1 layer, then a growing heap ----
w(A / "models/block/guano_pile_1.json", {
    "textures": {"particle": M + "block/guano_pile", "splatter": M + "block/guano_splatter"},
    "elements": [{"from": [0, 0.1, 0], "to": [16, 0.1, 16], "faces": {
        "up": {"uv": [0, 0, 16, 16], "texture": "#splatter"}, "down": {"uv": [0, 16, 16, 0], "texture": "#splatter"}}}]})
for n in (2, 3, 4):
    h = n - 1  # px of heap; the shape (GuanoPileBlock) is a little higher to be easy to click
    lump = 4 - n // 2
    w(A / f"models/block/guano_pile_{n}.json", {
        "textures": {"particle": M + "block/guano_pile", "pile": M + "block/guano_pile", "splatter": M + "block/guano_splatter"},
        "elements": [
            {"from": [0, 0.1, 0], "to": [16, 0.1, 16], "faces": {"up": {"uv": [0, 0, 16, 16], "texture": "#splatter"}}},
            {"from": [2, 0, 2], "to": [14, h, 14], "faces": {
                "up": {"uv": [2, 2, 14, 14], "texture": "#pile"},
                **{f: {"uv": [2, 16 - h, 14, 16], "texture": "#pile"} for f in ("north", "south", "east", "west")}}},
            {"from": [lump, h, lump + 1], "to": [16 - lump - 1, h + 1, 16 - lump], "faces": {
                "up": {"uv": [lump, lump + 1, 15 - lump, 16 - lump], "texture": "#pile"},
                **{f: {"uv": [lump, 15 - h, 16 - lump - 1, 16 - h], "texture": "#pile"} for f in ("north", "south", "east", "west")}}},
        ]})
w(A / "blockstates/guano_pile.json", {"variants": {
    "layers=1": [{"model": M + "block/guano_pile_1", "y": y} for y in (0, 90, 180, 270)],
    **{f"layers={n}": [{"model": M + f"block/guano_pile_{n}", "y": y} for y in (0, 90, 180, 270)] for n in (2, 3, 4)}}})
w(D / "mechanical/loot_table/blocks/guano_pile.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{
    "type": "minecraft:item", "name": M + "guano", "modifier": [
        {"type": "minecraft:set_count", "count": n, "condition": {"type": "minecraft:match_block",
                                                                  "blocks": M + "guano_pile", "state": {"layers": str(n)}}}
        for n in (2, 3, 4)]}]}], "random_sequence": "mechanical:blocks/guano_pile"})

# ---- Night-Eye Salve: bat wing ground with soot and fat ----
w(D / "mechanical/recipe/night_eye_salve.json", {"type": "minecraft:crafting_shapeless", "category": "misc",
                                                 "ingredients": [M + "bat_wing", M + "bat_wing", M + "soot", M + "lard"],
                                                 "result": {"id": M + "night_eye_salve", "count": 2}})

merge(A / "lang/en_us.json", LANG)
print("wildtweaks: %d items, guano pile, 1 recipe, %d lang keys" % (len(ITEMS), len(LANG)))
