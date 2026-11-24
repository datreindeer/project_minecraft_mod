"""Iron Age Mouldboard Plough data (PloughEntity.java, Carting.java; docs/farming.md, docs/ages/03-iron-age.md).

- item: Mouldboard Plough (model, item definition, lang)
- recipe: 2 Sticks (handles), 3 Logs (the beam), an Iron Tool Rod (coulter), 2 Iron Plates (share and mouldboard)
- unlock: by an Iron Plate
- lang for the entity and its messages (message.mechanical.plough.*)
Textures: scripts/gen_plough_textures.py. Advancement: gen_advancements.py (iron_age tab, "first_furrow").
Generator order: ... gen_wagon.py, gen_plough.py, gen_equipment.py ...
Run: python scripts/gen_plough.py"""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent / "src/main/resources"
A, D = ROOT / "assets/mechanical", ROOT / "data"
M = "mechanical:"


def w(path, obj):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, indent=2) + "\n")


def merge(path, obj):
    old = json.loads(path.read_text(encoding="utf-8")) if path.exists() else {}
    old.update(obj)
    path.write_text(json.dumps(old, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")


def unlock(name, item, recipes):
    w(D / f"mechanical/advancement/recipes/{name}.json", {"parent": "minecraft:recipes/root",
        "criteria": {"has_item": {"trigger": "minecraft:inventory_changed", "conditions": {"items": [{"items": item}]}}},
        "requirements": [["has_item"]], "rewards": {"recipes": [f"mechanical:{r}" for r in recipes]}})


w(A / "models/item/mouldboard_plough.json", {"parent": "minecraft:item/generated", "textures": {"layer0": "mechanical:item/mouldboard_plough"}})
w(A / "items/mouldboard_plough.json", {"model": {"type": "minecraft:model", "model": "mechanical:item/mouldboard_plough"}})
w(D / "mechanical/recipe/mouldboard_plough.json", {"type": "minecraft:crafting_shaped", "category": "misc",
    "pattern": ["S S", "LLL", "RPP"],
    "key": {"S": "minecraft:stick", "L": "#minecraft:logs", "R": M + "iron_tool_rod", "P": M + "iron_plate"},
    "result": {"id": M + "mouldboard_plough"}})
unlock("mouldboard_plough", M + "iron_plate", ["mouldboard_plough"])

merge(A / "lang/en_us.json", {
    "item.mechanical.mouldboard_plough": "Mouldboard Plough",
    "entity.mechanical.mouldboard_plough": "Mouldboard Plough",
    "message.mechanical.plough.needs_horse": "A plough needs a tamed horse: ride or lead one, then press the grab key (H)",
    "message.mechanical.plough.hitched": "Hitched. Press the grab key to lower the share",
    "message.mechanical.plough.unhitched": "Unhitched",
    "message.mechanical.plough.share_down": "Share down: it ploughs a 5-wide strip as it moves",
    "message.mechanical.plough.share_up": "Share up: it moves without ploughing",
    "message.mechanical.plough.stuck": "The plough is stuck. The horse came loose",
    "message.mechanical.plough.torn_loose": "The plough came loose from the horse",
    "message.mechanical.plough.no_room": "A plough carries nothing",
    "message.mechanical.plough.full": "A plough carries nothing",
    "message.mechanical.plough.too_big": "A plough carries nothing",
})
print("plough ok")
