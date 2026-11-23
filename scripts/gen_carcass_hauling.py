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

# ---- Hanging Rack: three blocks high (HangingRackBlock.Part), built facing north, the beam along x. The frame is laid out
# 48 px high, then cut into one model per block; the item model is the whole frame scaled to 2/3. Posts in 16 px pieces so
# the bark is not stretched ----
P, L, E = "#pole", "#lashing", "#end"


def box(f, t, tex=P, ends=E):
    return {"from": f, "to": t, "faces": {d: {"texture": ends if d in ("up", "down") else tex} for d in ("north", "south", "east", "west", "up", "down")}}


elements = []
for x in (1, 13):
    elements += [box([x, 0, 7], [x + 2, 16, 9]), box([x, 16, 7], [x + 2, 32, 9]), box([x, 32, 7], [x + 2, 45, 9])]
elements += [
    box([0, 44, 6.5], [16, 47, 9.5], ends=P) | {"faces": {"north": {"texture": P}, "south": {"texture": P}, "up": {"texture": P},
                                                         "down": {"texture": P}, "east": {"texture": E}, "west": {"texture": E}}},
    box([0.5, 42.5, 6], [3.5, 47.5, 10], L, L),     # lashings over the post tops
    box([12.5, 42.5, 6], [15.5, 47.5, 10], L, L),
    box([7.5, 41.5, 7.5], [8.5, 44, 8.5], L, L),    # the cord the gambrel hangs from
    box([4, 40.5, 7.25], [12, 41.75, 8.75], ends=P),  # the gambrel stick (HangingRackBlock.HOOK = 41 px)
    box([1, 0, 6.5], [3, 1, 9.5], ends=P),          # feet pegged into the ground
    box([13, 0, 6.5], [15, 1, 9.5], ends=P),
]


def part(lo):
    """The elements inside y lo..lo+16, cut at its edges and moved down into one block (faces on a cut are dropped)."""
    out = []
    for e in elements:
        y0, y1 = max(e["from"][1], lo), min(e["to"][1], lo + 16)
        if y1 <= y0:
            continue
        c = {**e, "from": [e["from"][0], y0 - lo, e["from"][2]], "to": [e["to"][0], y1 - lo, e["to"][2]], "faces": dict(e["faces"])}
        if y0 > e["from"][1]:
            c["faces"].pop("down")
        if y1 < e["to"][1]:
            c["faces"].pop("up")
        out.append(c)
    return out


def scaled(e, s=2 / 3):
    f = [8 + (e["from"][0] - 8) * s, e["from"][1] * s, 8 + (e["from"][2] - 8) * s]
    to = [8 + (e["to"][0] - 8) * s, e["to"][1] * s, 8 + (e["to"][2] - 8) * s]
    return {**e, "from": [round(v, 4) for v in f], "to": [round(v, 4) for v in to]}


TEX = {"particle": "mechanical:block/hanging_rack_pole", "pole": "mechanical:block/hanging_rack_pole",
       "end": "mechanical:block/hanging_rack_log_end", "lashing": "mechanical:block/hanging_rack_lashing"}
for name, lo in (("lower", 0), ("middle", 16), ("upper", 32)):
    w(A / f"models/block/hanging_rack_{name}.json", {"parent": "minecraft:block/block", "textures": TEX, "elements": part(lo)})
(A / "models/block/hanging_rack.json").unlink(missing_ok=True)
w(A / "models/item/hanging_rack.json", {
    "parent": "minecraft:block/block", "textures": TEX, "elements": [scaled(e) for e in elements],
    "display": {
        "gui": {"rotation": [30, 225, 0], "translation": [0, -3.5, 0], "scale": [0.42, 0.42, 0.42]},
        "ground": {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [0.25, 0.25, 0.25]},
        "fixed": {"rotation": [0, 0, 0], "translation": [0, -4, 0], "scale": [0.45, 0.45, 0.45]},
        "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [0.3, 0.3, 0.3]},
        "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [0, -2, 0], "scale": [0.35, 0.35, 0.35]},
    }})
# multipart: the frame, plus the blood pool soaking the ground under a skinned carcass (bottom part, blood 1-4; BloodBlock's models)
POOL = {1: "blood_0", 2: "blood_pool_2", 3: "blood_pool_3", 4: "blood_pool_4"}
w(A / "blockstates/hanging_rack.json", {"multipart": [
    {"when": {"facing": f, "part": name}, "apply": {"model": f"mechanical:block/hanging_rack_{name}", **({"y": y} if y else {})}}
    for f, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270)) for name in ("lower", "middle", "upper")] + [
    {"when": {"part": "lower", "blood": str(n)}, "apply": {"model": f"mechanical:block/{m}"}} for n, m in POOL.items()] + [
    # a kettle standing under the hook (HangingRackBlock.KETTLE), turned so its lugs clear the posts, and the blood in it (models: gen_kettle.py)
    {"when": {"part": "lower", "facing": f, "kettle": k}, "apply": {"model": f"mechanical:block/rack_kettle_{m}", "y": (y + 90) % 360}}
    for f, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270)) for k, m in (("bronze|poor_bronze", "bronze"), ("iron", "iron"))] + [
    {"when": {"part": "lower", "kettle": "bronze|poor_bronze|iron", "kettle_blood": str(n)}, "apply": {"model": f"mechanical:block/rack_kettle_blood_{n}"}}
    for n in range(1, 21)]})
w(A / "items/hanging_rack.json", {"model": {"type": "minecraft:model", "model": "mechanical:item/hanging_rack"}})
w(D / "mechanical/loot_table/blocks/hanging_rack.json", {"type": "minecraft:block", "pools": [{"rolls": 1,
    "entries": [{"type": "minecraft:item", "name": "mechanical:hanging_rack"}], "condition": {"type": "minecraft:all_of", "terms": [{"type": "minecraft:survives_explosion"},
    # only the bottom block drops the rack: breaking any part removes the other two, and they would each drop one too
    {"type": "minecraft:match_block", "blocks": "mechanical:hanging_rack", "state": {"part": "lower"}}]}}],
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
    "message.mechanical.carcass.drag_together": "%s of you pulling together. Right-click it to let go",
    "message.mechanical.carcass.too_heavy": "Too heavy! It takes %s to drag it, %s pulling",
    "message.mechanical.carcass.taken": "Someone else already has it",
    "message.mechanical.carcass.spoiled": "The meat has spoiled",
    "message.mechanical.carcass.hung": "Hung %s min. Ages at 3 min; keeps about %s min more",
    "message.mechanical.carcass.hung_aged": "Hung %s min: aged and ready. Keeps about %s min more",
    "message.mechanical.carcass.hung_spoiled": "Hung %s min: the meat has spoiled",
    "message.mechanical.hanging_rack.too_big": "Too big to hang. Butcher it where it lies",
    "message.mechanical.hanging_rack.occupied": "Something already hangs here",
    "message.mechanical.travois.harnessed": "Harnessed. Right-click again to unharness",
    "message.mechanical.travois.already_pulling": "That one already pulls something: unhitch it first",
    "message.mechanical.travois.unharnessed": "Unharnessed",
    "message.mechanical.travois.full": "The travois is full",
    "message.mechanical.travois.too_big": "Too big for the travois",
    "message.mechanical.travois.stuck": "The travois is stuck. You let go of the poles",
    "message.mechanical.travois.torn_loose": "The poles slipped out of your hands",
})
print("carcass hauling ok")
