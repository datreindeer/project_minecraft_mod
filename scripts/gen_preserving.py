"""Food spoilage, smoking and drying data (Preserving.java, SmokingRackBlock.java, DryingRackBlock.java).

- item tags: how long each perishable food keeps (perishes_1_day / 2_days / 3_days / 30_days), what rots into Rotten
  Flesh (rots_to_flesh; the rest becomes Spoiled Food), and what goes on the racks (smokable_meat, smokable_fish,
  dryable_berries, dryable_mushrooms); Spoiled Food is added to #mechanical:compostable
- items: Smoked Meat, Smoked Fish, Jerky, Dried Fish, Dried Berries, Dried Mushrooms, Spoiled Food (models, lang)
- Smoking Rack: frame + hanging strip models, a multipart blockstate (facing, content, strips), loot, recipe + unlock
- Raw Bear Pelt / Raw Wolf Pelt items (models, lang): Butchery gives them, the tall Drying Rack cures them
- Drying Rack: food stages (hide 6-13) as models (the frame from gen_data.py's drying_rack_0 plus a food plane); the
  2-tall rack (part=lower/upper, DryingRackBlock): frame halves and a left (hide) / right (hide2) spot plane per content;
  the whole multipart blockstate and the loot table (rack + what is on each spot); big pelts (hide 14-17, tall rack
  only): a bear plane over both blocks (lower/upper half models), a wolf plane over one block, fur side north, flesh side south
Textures: scripts/gen_preserving_textures.py, scripts/gen_pelt_rack_textures.py (big pelts, raw pelt items).
Generator order: ... gen_soot.py, gen_phantom.py, gen_preserving.py, gen_block_items.py, gen_registry.py.
Run: python scripts/gen_preserving.py"""
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


def tag(name, values):
    w(D / f"mechanical/tags/item/{name}.json", {"values": values})


# ---- what spoils, and how fast ----
MEATS = ["beef", "porkchop", "mutton", "chicken", "rabbit"]
RAW_MEAT = [f"mechanical:{m}_scraps" for m in MEATS + ["game_meat", "venison"]] + [f"minecraft:{m}" for m in MEATS]
RAW_FISH = ["minecraft:cod", "minecraft:salmon", "minecraft:tropical_fish", "minecraft:pufferfish"] + \
    [f"mechanical:whole_{f}" for f in ("cod", "salmon", "tropical_fish", "pufferfish")]
COOKED = [f"mechanical:cooked_{m}_scraps" for m in MEATS + ["game_meat", "venison"]] + [f"minecraft:cooked_{m}" for m in MEATS + ["cod", "salmon"]]
BERRIES = [f"mechanical:{b}" for b in ("raspberries", "blueberries", "lingonberries", "wild_strawberries", "elderberries", "cloudberries",
                                       "blackberries", "juniper_berries")] + ["minecraft:sweet_berries"]
MUSHROOMS = [f"mechanical:{m}" for m in ("chanterelle", "porcini", "morel", "oyster_mushroom", "fly_agaric", "puffball")]
# what dries into Dried Mushrooms: the edible ones (not Fly Agaric; vanilla mushrooms aren't food here)
DRYABLE_MUSHROOMS = [m for m in MUSHROOMS if m != "mechanical:fly_agaric"]
PRESERVED_FLESH = ["mechanical:smoked_meat", "mechanical:smoked_fish", "mechanical:jerky", "mechanical:dried_fish"]

tag("perishes_1_day", RAW_MEAT + RAW_FISH)
tag("perishes_2_days", BERRIES + ["minecraft:glow_berries"] + MUSHROOMS + ["minecraft:melon_slice", "mechanical:porridge", "mechanical:raw_porridge", "mechanical:peeled_egg"])
tag("perishes_3_days", COOKED + ["mechanical:roasted_mushrooms", "mechanical:boiled_egg", "minecraft:pumpkin_pie"])
tag("perishes_30_days", PRESERVED_FLESH + ["mechanical:dried_berries", "mechanical:dried_mushrooms"])
tag("rots_to_flesh", RAW_MEAT + RAW_FISH + COOKED + PRESERVED_FLESH)
tag("smokable_meat", RAW_MEAT)
tag("smokable_fish", ["minecraft:cod", "minecraft:salmon"])  # the fillets you get from gutting a whole fish
tag("dryable_berries", BERRIES)
tag("dryable_mushrooms", DRYABLE_MUSHROOMS)
tag_add(D / "mechanical/tags/item/compostable.json", "mechanical:spoiled_food")

# ---- items ----
ITEMS = {"smoked_meat": "Smoked Meat", "smoked_fish": "Smoked Fish", "jerky": "Jerky", "dried_fish": "Dried Fish",
         "dried_berries": "Dried Berries", "dried_mushrooms": "Dried Mushrooms", "spoiled_food": "Spoiled Food"}
for item in ITEMS:
    w(A / f"models/item/{item}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"mechanical:item/{item}"}})
    w(A / f"items/{item}.json", {"model": {"type": "minecraft:model", "model": f"mechanical:item/{item}"}})
merge(A / "lang/en_us.json", {**{f"item.mechanical.{k}": v for k, v in ITEMS.items()}, "block.mechanical.smoking_rack": "Smoking Rack",
                              "tooltip.mechanical.fresh": "Fresh", "tooltip.mechanical.stale": "Stale", "tooltip.mechanical.rotting": "Rotting",
                              "message.mechanical.too_far_gone": "This is too far gone to keep"})
for item, name in {"raw_bear_pelt": "Raw Bear Pelt", "raw_wolf_pelt": "Raw Wolf Pelt"}.items():
    w(A / f"models/item/{item}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"mechanical:item/{item}"}})
    w(A / f"items/{item}.json", {"model": {"type": "minecraft:model", "model": f"mechanical:item/{item}"}})
    merge(A / "lang/en_us.json", {f"item.mechanical.{item}": name})
merge(A / "lang/en_us.json", {"message.mechanical.drying_rack.pelt_needs_tall": "This pelt needs a bigger rack",
                              "message.mechanical.drying_rack.pelt_no_room": "There is no room for the pelt on this rack"})

# ---- Smoking Rack (built facing north: the bar runs east-west, strips hang from it) ----
FACES = ["down", "up", "north", "south", "east", "west"]


def box(f, t, tex="#wood"):
    return {"from": f, "to": t, "faces": {d: {"texture": tex} for d in FACES}}


frame = [box([1, 0, 7], [3, 15, 9]), box([13, 0, 7], [15, 15, 9]),           # posts
         box([0.5, 0, 4.5], [3.5, 2, 11.5]), box([12.5, 0, 4.5], [15.5, 2, 11.5]),  # feet
         box([0, 14, 7.25], [16, 15.5, 8.75])]                                # the bar the strips hang from
w(A / "models/block/smoking_rack_frame.json", {"parent": "minecraft:block/block", "textures": {
    "particle": "mechanical:block/smoking_rack", "wood": "mechanical:block/smoking_rack"}, "elements": frame})
CONTENTS = ["raw_meat", "raw_fish", "smoked_meat", "smoked_fish", "rotten"]
# each strip texture holds 4 strips, 2 px wide and 9 tall, in columns 0, 4, 8, 12 (gen_preserving_textures.py)
for i in range(4):
    x0, u = 3.5 + 2.5 * i, 4 * i
    strip = {"from": [x0, 5, 7.5], "to": [x0 + 2, 14, 8.5], "faces": {
        "north": {"uv": [u, 0, u + 2, 9], "texture": "#strip"}, "south": {"uv": [u + 2, 0, u, 9], "texture": "#strip"},
        "east": {"uv": [u, 0, u + 1, 9], "texture": "#strip"}, "west": {"uv": [u + 1, 0, u + 2, 9], "texture": "#strip"},
        "down": {"uv": [u, 8, u + 2, 9], "texture": "#strip"}}}
    for c in CONTENTS:
        w(A / f"models/block/smoking_rack_strip{i + 1}_{c}.json", {"textures": {
            "particle": "mechanical:block/smoking_rack", "strip": f"mechanical:block/smoking_rack_{c}"}, "elements": [strip]})
ROT = [("north", 0), ("east", 90), ("south", 180), ("west", 270)]
parts = [{"when": {"facing": f}, "apply": {"model": "mechanical:block/smoking_rack_frame", **({"y": y} if y else {})}} for f, y in ROT]
parts += [{"when": {"facing": f, "content": c, "strips": "|".join(str(n) for n in range(i, 5))},
           "apply": {"model": f"mechanical:block/smoking_rack_strip{i}_{c}", **({"y": y} if y else {})}}
          for f, y in ROT for c in CONTENTS for i in range(1, 5)]
w(A / "blockstates/smoking_rack.json", {"multipart": parts})
w(A / "items/smoking_rack.json", {"model": {"type": "minecraft:model", "model": "mechanical:block/smoking_rack_frame"}})
w(D / "mechanical/loot_table/blocks/smoking_rack.json", {"type": "minecraft:block", "pools": [
    {"rolls": 1, "entries": [{"type": "minecraft:item", "name": "mechanical:smoking_rack"}], "conditions": [{"condition": "minecraft:survives_explosion"}]}],
    "random_sequence": "mechanical:blocks/smoking_rack"})
tag_add(D / "minecraft/tags/block/mineable/axe.json", "mechanical:smoking_rack")
# a taller frame than the Drying Rack, lashed with cordage
w(D / "mechanical/recipe/smoking_rack.json", {"type": "minecraft:crafting_shaped", "category": "building",
    "pattern": ["SCS", "S S", "S S"], "key": {"S": "minecraft:stick", "C": "mechanical:cordage"}, "result": {"id": "mechanical:smoking_rack"}})
w(D / "mechanical/advancement/recipes/smoking_rack.json", {"parent": "minecraft:recipes/root",
    "criteria": {"has_meat": {"trigger": "minecraft:inventory_changed", "conditions": {"items": [{"items": "#mechanical:smokable_meat"}]}},
                 "has_fish": {"trigger": "minecraft:inventory_changed", "conditions": {"items": [{"items": "#mechanical:smokable_fish"}]}}},
    "requirements": [["has_meat", "has_fish"]], "rewards": {"recipes": ["mechanical:smoking_rack"]}})

# ---- Drying Rack (DryingRackBlock: facing, part single/lower/upper, hide = left spot, hide2 = right spot, 0-13) ----
rack0 = json.loads((A / "models/block/drying_rack_0.json").read_text(encoding="utf-8"))
plane = {"from": [3, 4, 8], "to": [13, 13, 8], "faces": {"north": {"uv": [3, 3, 13, 12], "texture": "#food"},
                                                       "south": {"uv": [13, 3, 3, 12], "texture": "#food"}}}
FOOD_STAGES = {6: "meat", 7: "jerky", 8: "fish", 9: "dried_fish", 10: "berries", 11: "dried_berries", 12: "mushrooms", 13: "dried_mushrooms"}
for stage, tex in FOOD_STAGES.items():
    w(A / f"models/block/drying_rack_{stage}.json", {"parent": "minecraft:block/block",
        "textures": {**rack0["textures"], "food": f"mechanical:block/drying_rack_{tex}"}, "elements": rack0["elements"] + [plane]})
# 2-tall rack, built facing north: taller posts with feet, the lower half's bottom bar and crossbar, the upper half's top bar;
# each half has two spots side by side (left x 3-8, right x 8-13) hanging from its bar. Spot textures are 5 px wide
# (x 0-4, y 3-11, gen_preserving_textures.py).
tall_lower = [box([1, 0, 7], [3, 16, 9]), box([13, 0, 7], [15, 16, 9]),                    # posts
              box([0.5, 0, 5], [3.5, 2, 11]), box([12.5, 0, 5], [15.5, 2, 11]),            # feet
              box([3, 2, 7.25], [13, 4, 8.75]), box([3, 13, 7.25], [13, 15, 8.75])]        # bottom bar, crossbar
tall_upper = [box([1, 0, 7], [3, 16, 9]), box([13, 0, 7], [15, 16, 9]), box([3, 13, 7.25], [13, 15, 8.75]),
              box([0.75, 15.5, 6.75], [3.25, 16.5, 9.25]), box([12.75, 15.5, 6.75], [15.25, 16.5, 9.25])]  # post caps
for half, elements in (("lower", tall_lower), ("upper", tall_upper)):
    w(A / f"models/block/drying_rack_tall_{half}.json", {"parent": "minecraft:block/block", "textures": rack0["textures"], "elements": elements})
SPOT = {1: "hide_wet", 2: "hide_dry", 3: "phantom_wet", 4: "phantom_dry", 5: "phantom_rotten", **FOOD_STAGES}
for side, x0 in (("left", 3), ("right", 8)):
    for tex in SPOT.values():
        w(A / f"models/block/drying_rack_{side}_{tex}.json", {"textures": {"particle": "mechanical:block/drying_rack",
            "spot": f"mechanical:block/drying_rack_narrow_{tex}"}, "elements": [{"from": [x0, 4, 8], "to": [x0 + 5, 13, 8], "faces": {
                "north": {"uv": [0, 3, 5, 12], "texture": "#spot"}, "south": {"uv": [5, 3, 0, 12], "texture": "#spot"}}}]})
# big pelts (tall rack only, in hide; hide2 stays empty): a plane in front of the bars at z 7, x 3-13, painted in model
# space (texture row = 15 - y); fur side north, the flesh side (_back) south with mirrored uv
PELTS = {14: "raw", 15: "dry"}


def pelt_plane(tex, y0, y1):
    return {"textures": {"particle": "mechanical:block/drying_rack", "front": f"mechanical:block/{tex}", "back": f"mechanical:block/{tex}_back"},
            "elements": [{"from": [3, y0, 7], "to": [13, y1, 7], "faces": {
                "north": {"uv": [3, 16 - y1, 13, 16 - y0], "texture": "#front"}, "south": {"uv": [13, 16 - y1, 3, 16 - y0], "texture": "#back"}}}]}


for stage in ("raw", "dry"):
    w(A / f"models/block/drying_rack_bear_{stage}_lower.json", pelt_plane(f"drying_rack_bear_{stage}_lower", 4, 16))
    w(A / f"models/block/drying_rack_bear_{stage}_upper.json", pelt_plane(f"drying_rack_bear_{stage}_upper", 0, 13))
    w(A / f"models/block/drying_rack_wolf_{stage}.json", pelt_plane(f"drying_rack_wolf_{stage}", 2, 13))
rot = lambda y: {"y": y} if y else {}
parts = [{"when": {"facing": f, "part": "single", "hide": str(h)}, "apply": {"model": f"mechanical:block/drying_rack_{h}", **rot(y)}}
         for f, y in ROT for h in range(14)]
parts += [{"when": {"facing": f, "part": "single", "hide": "14|15|16|17"}, "apply": {"model": "mechanical:block/drying_rack_0", **rot(y)}}
          for f, y in ROT]  # never hung there; just not invisible if set by command
parts += [{"when": {"facing": f, "part": half, "hide": str(h)}, "apply": {"model": f"mechanical:block/drying_rack_bear_{st}_{half}", **rot(y)}}
          for f, y in ROT for half in ("lower", "upper") for h, st in PELTS.items()]
parts += [{"when": {"facing": f, "part": "lower|upper", "hide": str(h + 2)}, "apply": {"model": f"mechanical:block/drying_rack_wolf_{st}", **rot(y)}}
          for f, y in ROT for h, st in PELTS.items()]
parts += [{"when": {"facing": f, "part": half}, "apply": {"model": f"mechanical:block/drying_rack_tall_{half}", **rot(y)}}
          for f, y in ROT for half in ("lower", "upper")]
parts += [{"when": {"facing": f, "part": "lower|upper", prop: str(h)}, "apply": {"model": f"mechanical:block/drying_rack_{side}_{tex}", **rot(y)}}
          for f, y in ROT for prop, side in (("hide", "left"), ("hide2", "right")) for h, tex in SPOT.items()]
w(A / "blockstates/drying_rack.json", {"multipart": parts})
# drops: the rack itself (each half of a tall one is a rack), plus what hangs on each spot: hides come back as they are,
# finished things as they are, food still drying is lost
SPOT_DROPS = {1: ("mechanical:scraped_hide", 1), 2: ("minecraft:leather", 1), 3: ("mechanical:scraped_phantom_hide", 1),
              4: ("mechanical:phantom_leather", 1), 5: ("minecraft:rotten_flesh", 1), 7: ("mechanical:jerky", 1),
              9: ("mechanical:dried_fish", 1), 11: ("mechanical:dried_berries", 4), 13: ("mechanical:dried_mushrooms", 4)}


def spot_drop(prop, h, item, count, **state):
    entry = {"type": "minecraft:item", "name": item,
             "condition": {"type": "minecraft:match_block", "blocks": "mechanical:drying_rack", "state": {prop: str(h), **state}}}
    if count > 1:
        entry["modifier"] = [{"type": "minecraft:set_count", "count": count}]
    return entry


w(D / "mechanical/loot_table/blocks/drying_rack.json", {"type": "minecraft:block", "pools": [
    {"rolls": 1, "entries": [{"type": "minecraft:item", "name": "mechanical:drying_rack"}]}]
    + [{"rolls": 1, "entries": [spot_drop(prop, h, item, n) for h, (item, n) in SPOT_DROPS.items()]
        + ([] if prop == "hide2" else  # big pelts: the bear's lower block alone drops it (it spans both), a wolf's block its own
           [spot_drop("hide", 14, "mechanical:raw_bear_pelt", 1, part="lower"), spot_drop("hide", 15, "mechanical:bear_pelt", 1, part="lower"),
            spot_drop("hide", 16, "mechanical:raw_wolf_pelt", 1), spot_drop("hide", 17, "mechanical:wolf_pelt", 1)])}
       for prop in ("hide", "hide2")],
    "random_sequence": "mechanical:blocks/drying_rack"})
print("preserving ok")
