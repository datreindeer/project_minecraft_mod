"""Bronze Age cart and horse taming data (Carting.java, CartEntity.java, wildlife/WildHorse.java, wildlife/TameHorse.java).

- items: Wheeled Cart, Wooden Wheel, Rope Halter, Hide Saddle Blanket (models, lang)
- recipes: Wooden Wheel (4 planks, 3 Tool Rods, a Bronze Ingot hub, the Bronze Saw stays and loses 1), Wheeled Cart
  (2 wheels, 4 planks, 2 Tool Rods as shafts), Rope Halter (3 Cordage), Hide Saddle Blanket (3 Leather, 2 Cordage)
- unlocks: wheel by the Bronze Saw, cart by a Wooden Wheel, halter by a Bronze Ingot, blanket by the halter
- tags: mechanical:horse_feed, mechanical:horse_breeding_feed, entity tag mechanical:wears_hide_saddle_blanket, and the
  tame horse added to mechanical:drops_manure (written by gen_farming3.py, so this runs after it)
- lang for the entities and the cart / taming messages
Textures: scripts/gen_cart_textures.py.
Generator order: ... gen_wolf_pelt.py, gen_farming3.py, gen_pottery.py, gen_cart.py, gen_block_items.py, gen_unlocks.py, gen_advancements.py, gen_registry.py.
Run: python scripts/gen_cart.py"""
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


def tag_add(path, *values):
    old = json.loads(path.read_text(encoding="utf-8")) if path.exists() else {"values": []}
    old["values"] += [v for v in values if v not in old["values"]]
    w(path, old)


def unlock(name, item, recipes):
    w(D / f"mechanical/advancement/recipes/{name}.json", {"parent": "minecraft:recipes/root",
        "criteria": {"has_item": {"trigger": "minecraft:inventory_changed", "conditions": {"items": [{"items": item}]}}},
        "requirements": [["has_item"]], "rewards": {"recipes": [f"mechanical:{r}" for r in recipes]}})


def shaped(name, pattern, key, category, count=1):
    w(D / f"mechanical/recipe/{name}.json", {"type": "minecraft:crafting_shaped", "category": category,
        "pattern": pattern, "key": key, "result": {"id": M + name, **({"count": count} if count > 1 else {})}})


ITEMS = {"cart": "Wheeled Cart", "wooden_wheel": "Wooden Wheel", "rope_halter": "Rope Halter",
         "hide_saddle_blanket": "Hide Saddle Blanket"}
for item in ITEMS:
    w(A / f"models/item/{item}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"mechanical:item/{item}"}})
    w(A / f"items/{item}.json", {"model": {"type": "minecraft:model", "model": f"mechanical:item/{item}"}})

# rim of planks, spokes of Tool Rods, a bronze hub and axle pin; a saw (#mechanical:saws) cuts the felloes and stays (FlintPickaxeItem rule)
shaped("wooden_wheel", ["PRP", "RBR", "PSP"],
       {"P": "#minecraft:planks", "R": M + "tool_rod", "B": M + "bronze_ingot", "S": "#mechanical:saws"}, "misc")
shaped("cart", ["R R", "PPP", "WPW"], {"R": M + "tool_rod", "P": "#minecraft:planks", "W": M + "wooden_wheel"}, "misc")
shaped("rope_halter", ["C C", " C "], {"C": M + "cordage"}, "equipment")
shaped("hide_saddle_blanket", ["LLL", "C C"], {"L": "minecraft:leather", "C": M + "cordage"}, "equipment")
unlock("wooden_wheel", "#mechanical:saws", ["wooden_wheel"])
unlock("cart", M + "wooden_wheel", ["cart"])
unlock("rope_halter", M + "bronze_ingot", ["rope_halter"])
unlock("hide_saddle_blanket", M + "rope_halter", ["hide_saddle_blanket"])

GRAINS = [f"{M}{g}_grain" for g in ("barley", "einkorn", "emmer", "oat")]
SHEAVES = [f"{M}{g}_sheaf" for g in ("barley", "einkorn", "emmer", "oat")]
T = D / "mechanical/tags"
w(T / "item/horse_feed.json", {"values": GRAINS + SHEAVES + ["minecraft:wheat", "minecraft:hay_block", "minecraft:apple", "minecraft:carrot"]})
w(T / "item/horse_breeding_feed.json", {"values": SHEAVES + ["minecraft:hay_block"]})
w(T / "entity_type/wears_hide_saddle_blanket.json", {"values": [M + "tame_horse"]})
tag_add(T / "entity_type/drops_manure.json", M + "tame_horse")

merge(A / "lang/en_us.json", {f"item.mechanical.{k}": v for k, v in ITEMS.items()} | {
    "entity.mechanical.cart": "Wheeled Cart",
    "entity.mechanical.tame_horse": "Tamed Horse",
    "message.mechanical.cart.full": "The cart is full",
    "message.mechanical.cart.too_big": "Too big for the cart",
    "message.mechanical.cart.no_room": "The chest takes up the whole bed",
    "message.mechanical.cart.unload_first": "Unload the cart before mounting a chest on it",
    "message.mechanical.cart.chest_mounted": "Chest mounted. Sneak + right-click the cart to open it",
    "message.mechanical.cart.hitched": "The horse is in the shafts. Right-click the cart again to unhitch",
    "message.mechanical.cart.unhitched": "Unhitched",
    "message.mechanical.cart.stuck": "The cart is stuck. It came loose from the shafts",
    "message.mechanical.cart.torn_loose": "The cart came loose from the shafts",
    "message.mechanical.horse.shies": "The horse shies away. Crouch and hold out feed",
    "message.mechanical.horse.chewing": "The horse is still chewing",
    "message.mechanical.horse.trust": "The horse eats from your hand (trust %s/%s)",
    "message.mechanical.horse.ready_for_halter": "It trusts you now. Put a Rope Halter on it",
    "message.mechanical.horse.needs_halter": "Put a Rope Halter on it before feeding it more",
    "message.mechanical.horse.refuses_halter": "The horse won't take the halter yet. Feed it first",
    "message.mechanical.horse.haltered": "Haltered. Keep feeding it to tame it (trust %s/%s)",
    "message.mechanical.horse.tamed": "The horse is tame. Right-click to ride, sneak + right-click for its gear",
})
print("cart ok")
