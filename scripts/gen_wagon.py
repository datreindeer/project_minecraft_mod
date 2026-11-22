"""Iron Age Wagon data (WagonEntity.java, Carting.java; docs/wagon-plan.md).

- items: Wagon, Iron-Rimmed Wheel (models, item definitions, lang)
- recipes: Iron-Rimmed Wheel (a Wooden Wheel + 2 Iron Plates, shapeless), Wagon (4 Iron-Rimmed Wheels, 3 Planks, an Iron
  Plate, an Iron Tool Rod as the pole)
- unlocks: the wheel by an Iron Plate, the wagon by an Iron-Rimmed Wheel
- lang for the entity and its messages (message.mechanical.wagon.*; module.1/2/3 = chest, hay, seat as in WagonEntity)
Textures: scripts/gen_wagon_textures.py. Advancements: gen_advancements.py (iron_age tab).
Generator order: ... gen_iron_age.py, gen_wagon.py, ... gen_block_items.py, gen_unlocks.py, gen_advancements.py, gen_registry.py.
Run: python scripts/gen_wagon.py"""
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


ITEMS = {"wagon": "Wagon", "iron_rimmed_wheel": "Iron-Rimmed Wheel"}
for item in ITEMS:
    w(A / f"models/item/{item}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"mechanical:item/{item}"}})
    w(A / f"items/{item}.json", {"model": {"type": "minecraft:model", "model": f"mechanical:item/{item}"}})

# the tyre is two plates shrunk on hot around the felloes
w(D / "mechanical/recipe/iron_rimmed_wheel.json", {"type": "minecraft:crafting_shapeless", "category": "misc",
    "ingredients": [M + "wooden_wheel", M + "iron_plate", M + "iron_plate"], "result": {"id": M + "iron_rimmed_wheel"}})
w(D / "mechanical/recipe/wagon.json", {"type": "minecraft:crafting_shaped", "category": "misc",
    "pattern": ["WRW", "PIP", "WPW"],
    "key": {"W": M + "iron_rimmed_wheel", "R": M + "iron_tool_rod", "P": "#minecraft:planks", "I": M + "iron_plate"},
    "result": {"id": M + "wagon"}})
unlock("iron_rimmed_wheel", M + "iron_plate", ["iron_rimmed_wheel"])
unlock("wagon", M + "iron_rimmed_wheel", ["wagon"])

merge(A / "lang/en_us.json", {f"item.mechanical.{k}": v for k, v in ITEMS.items()} | {
    "entity.mechanical.wagon": "Wagon",
    "message.mechanical.wagon.full": "The wagon is full",
    "message.mechanical.wagon.too_big": "Too big for the wagon",
    "message.mechanical.wagon.no_room": "No empty place left on the bed",
    "message.mechanical.wagon.stuck": "The wagon is stuck. The team came loose",
    "message.mechanical.wagon.torn_loose": "The wagon came loose from the team",
    "message.mechanical.wagon.needs_horses": "A wagon needs tamed horses: ride or lead one to it and right-click it",
    "message.mechanical.wagon.one_horse": "One horse is in. It can barely move the wagon: bring a second one and right-click again",
    "message.mechanical.wagon.team": "The pair is in the yoke. Sneak + right-click a horse to unhitch it",
    "message.mechanical.wagon.release_hint": "The team is fixed to the wagon. Sneak + right-click a horse to unhitch it",
    # work horses (HorseWork.java): stamina, bodies on the sides
    "message.mechanical.horse.exhausted": "Your horse is exhausted. It throws you off and lies down to rest",
    "message.mechanical.horse.resting": "The horse is too exhausted to ride. Let it rest, or give it water",
    "message.mechanical.horse.sides_full": "Both sides of the horse already carry a body",
    "message.mechanical.horse.slung": "The body hangs on the horse's side. Sneak + right-click it to take it back",
    "message.mechanical.wagon.unhitched": "Unhitched",
    "message.mechanical.wagon.yoke_broke": "The second horse was held back and came out of the yoke",
    "message.mechanical.wagon.place_taken": "That place on the bed is taken. Punch the module to take it out",
    "message.mechanical.wagon.unload_first": "Unload a carcass first: there is no room for that",
    "message.mechanical.wagon.module.1": "Chest in place. Sneak + right-click it to open",
    "message.mechanical.wagon.module.2": "Hay Bale in place. Hurt horses in the team eat from it",
    "message.mechanical.wagon.module.3": "Seat in place. Right-click it to sit",
    # a Wheeled Cart hitched behind a wagon (CartEntity.toggleHarness)
    "message.mechanical.cart.towed": "The cart is hitched behind the wagon. Right-click it again to unhitch",
    "message.mechanical.cart.untowed": "The cart is unhitched from the wagon",
    "message.mechanical.cart.wagon_has_trailer": "That wagon already tows a cart",
})
print("wagon ok")
