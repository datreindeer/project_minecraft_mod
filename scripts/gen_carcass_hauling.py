"""Carcass hauling data (CarcassHauling.java, TravoisEntity.java, HangingRackBlock.java).

- items: Travois, Raw/Cooked Haunch, Ribs, Loin (models, lang); cooking recipes for the cuts (campfire, smoker)
- tags merged into gen_preserving.py's lists (never rewritten here): raw cuts perish in 2 days, cooked in 3, all rot to
  flesh, raw cuts go on the Smoking Rack (smokable_meat)
- Travois: shaped recipe, 4 Tool Rods + 2 Cordage + a Scraped Hide; unlocked by a Scraped Hide
- Hanging Rack: block model (two posts two blocks high, crossbeam, lashings, a gambrel on a cord; HangingRackBlock.HOOK
  is the gambrel's height), blockstate (facing), loot, axe tag, recipe 5 Tool Rods + 1 Cordage; unlocked by a Raw Hide
- lang for the entity, block and the hauling messages
Textures: scripts/gen_carcass_hauling_textures.py.
Generator order: ... gen_trees.py, gen_biomes.py, gen_carcass_hauling.py, gen_block_items.py, gen_unlocks.py, gen_registry.py.
Run: python scripts/gen_carcass_hauling.py"""
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


def unlock(name, item, recipes):
    w(D / f"mechanical/advancement/recipes/{name}.json", {"parent": "minecraft:recipes/root",
        "criteria": {"has_item": {"trigger": "minecraft:inventory_changed", "conditions": {"items": [{"items": item}]}}},
        "requirements": [["has_item"]], "rewards": {"recipes": [f"mechanical:{r}" for r in recipes]}})


# ---- items ----
CUTS = ["haunch", "ribs", "loin"]
ITEMS = {"travois": "Travois"}
for c in CUTS:
    ITEMS[f"raw_{c}"] = f"Raw {c.title()}"
    ITEMS[f"cooked_{c}"] = f"Cooked {c.title()}"
for item in ITEMS:
    w(A / f"models/item/{item}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"mechanical:item/{item}"}})
    w(A / f"items/{item}.json", {"model": {"type": "minecraft:model", "model": f"mechanical:item/{item}"}})

# cooking: a whole haunch takes longer than the scraps (400 / 100 ticks)
for c in CUTS:
    slow = c == "haunch"
    for kind, ticks in (("campfire_cooking", 600 if slow else 500), ("smoking", 150 if slow else 120)):
        w(D / f"mechanical/recipe/cooked_{c}_from_{kind}.json", {"type": f"minecraft:{kind}", "category": "food",
            "cookingtime": ticks, "experience": 0.2, "ingredient": f"mechanical:raw_{c}", "result": {"id": f"mechanical:cooked_{c}"}})

RAW = [f"mechanical:raw_{c}" for c in CUTS]
COOKED = [f"mechanical:cooked_{c}" for c in CUTS]
T = D / "mechanical/tags/item"
tag_add(T / "perishes_2_days.json", *RAW)
tag_add(T / "perishes_3_days.json", *COOKED)
tag_add(T / "rots_to_flesh.json", *RAW, *COOKED)
tag_add(T / "smokable_meat.json", *RAW)

# ---- Travois ----
w(D / "mechanical/recipe/travois.json", {"type": "minecraft:crafting_shaped", "category": "misc",
    "pattern": ["R R", "CHC", "R R"], "key": {"R": "mechanical:tool_rod", "C": "mechanical:cordage", "H": "mechanical:scraped_hide"},
    "result": {"id": "mechanical:travois"}})
unlock("travois", "mechanical:scraped_hide", ["travois"])

# ---- Hanging Rack: built facing north, the beam along x; 2 px posts in two halves so the bark is not stretched ----
P, L, E = "#pole", "#lashing", "#end"


def box(f, t, tex=P, ends=E):
    return {"from": f, "to": t, "faces": {d: {"texture": ends if d in ("up", "down") else tex} for d in ("north", "south", "east", "west", "up", "down")}}


elements = []
for x in (1, 13):
    elements += [box([x, 0, 7], [x + 2, 16, 9]), box([x, 16, 7], [x + 2, 29, 9])]
elements += [
    box([0, 28, 6.5], [16, 31, 9.5], ends=P) | {"faces": {"north": {"texture": P}, "south": {"texture": P}, "up": {"texture": P},
                                                         "down": {"texture": P}, "east": {"texture": E}, "west": {"texture": E}}},
    box([0.5, 26.5, 6], [3.5, 31.5, 10], L, L),     # lashings over the post tops
    box([12.5, 26.5, 6], [15.5, 31.5, 10], L, L),
    box([7.5, 25.5, 7.5], [8.5, 28, 8.5], L, L),    # the cord the gambrel hangs from
    box([4, 24.5, 7.25], [12, 25.75, 8.75], ends=P),  # the gambrel stick (HangingRackBlock.HOOK = 25 px)
    box([1, 0, 6.5], [3, 1, 9.5], ends=P),          # feet pegged into the ground
    box([13, 0, 6.5], [15, 1, 9.5], ends=P),
]
w(A / "models/block/hanging_rack.json", {
    "parent": "minecraft:block/block",
    "textures": {"particle": "mechanical:block/hanging_rack_pole", "pole": "mechanical:block/hanging_rack_pole",
                 "end": "mechanical:block/hanging_rack_log_end", "lashing": "mechanical:block/hanging_rack_lashing"},
    "elements": elements,
    "display": {
        "gui": {"rotation": [30, 225, 0], "translation": [0, -3.5, 0], "scale": [0.42, 0.42, 0.42]},
        "ground": {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [0.2, 0.2, 0.2]},
        "fixed": {"rotation": [0, 0, 0], "translation": [0, -4, 0], "scale": [0.4, 0.4, 0.4]},
        "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [0.25, 0.25, 0.25]},
        "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [0, -2, 0], "scale": [0.3, 0.3, 0.3]},
    }})
w(A / "blockstates/hanging_rack.json", {"variants": {
    f"facing={f}": {"model": "mechanical:block/hanging_rack", **({"y": y} if y else {})}
    for f, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270))}})
w(A / "items/hanging_rack.json", {"model": {"type": "minecraft:model", "model": "mechanical:block/hanging_rack"}})
w(D / "mechanical/loot_table/blocks/hanging_rack.json", {"type": "minecraft:block", "pools": [{"rolls": 1,
    "entries": [{"type": "minecraft:item", "name": "mechanical:hanging_rack"}], "conditions": [{"condition": "minecraft:survives_explosion"}]}],
    "random_sequence": "mechanical:blocks/hanging_rack"})
tag_add(D / "minecraft/tags/block/mineable/axe.json", "mechanical:hanging_rack")
w(D / "mechanical/recipe/hanging_rack.json", {"type": "minecraft:crafting_shaped", "category": "building",
    "pattern": ["RCR", "R R", "R R"], "key": {"R": "mechanical:tool_rod", "C": "mechanical:cordage"},
    "result": {"id": "mechanical:hanging_rack"}})
unlock("hanging_rack", "mechanical:raw_hide", ["hanging_rack"])

merge(A / "lang/en_us.json", {f"item.mechanical.{k}": v for k, v in ITEMS.items()} | {
    "block.mechanical.hanging_rack": "Hanging Rack",
    "entity.mechanical.travois": "Travois",
    "message.mechanical.carcass.carry": "On your shoulders. Right-click a block to put it down",
    "message.mechanical.carcass.drag": "Dragging it. Right-click it or a block to let go",
    "message.mechanical.carcass.spoiled": "The meat has spoiled",
    "message.mechanical.hanging_rack.too_big": "Too big to hang. Butcher it where it lies",
    "message.mechanical.hanging_rack.occupied": "Something already hangs here",
    "message.mechanical.travois.harnessed": "Harnessed. Right-click again to unharness",
    "message.mechanical.travois.unharnessed": "Unharnessed",
    "message.mechanical.travois.full": "The travois is full",
    "message.mechanical.travois.too_big": "Too big for the travois",
    "message.mechanical.travois.stuck": "The travois is stuck. You let go of the poles",
    "message.mechanical.travois.torn_loose": "The poles slipped out of your hands",
})
print("carcass hauling ok")
