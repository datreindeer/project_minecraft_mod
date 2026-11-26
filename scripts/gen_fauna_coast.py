"""Data for the coast fauna (wildlife/coast/CoastFauna.java): Seal, Walrus, Pike, Sturgeon and their products.

- items: models for every item (the Ivory Knife held like a tool), the Oil Lamp's blockstate / models / loot
- spawns (own biome modifiers): seals on cold beaches, stony shores and ice; walruses on frozen oceans and snowy beaches;
  pike in rivers, fens, swamps and forest lakes; sturgeon rarely in rivers
- fishing: the Pike (rivers, swamps, fens, frozen rivers) and, rarely, a young Whole Sturgeon (rivers) join the fish
  pool of gameplay/fishing/fish (gen_data.py writes it; this adds to it)
- recipes: meats and fillets on the campfire / in the smoker; whole pike and sturgeon gutted with a knife; Isinglass soaks
  into Hide Glue; Ivory Knife; bone needles and fish hooks from Walrus Ivory; Walrus Hide cut into Cordage; the Oil Lamp
- tags: spoilage (perishes_*, rots_to_flesh), smokable meat / fish, whole_fish, knives, armour slots and repairs,
  lamp_fuel, ivory_tool_materials
- the Sealskin and Walrus Hide armour equipment assets; the sealskin (one block) and walrus hide (both blocks) on the
  tall Drying Rack: blockstate parts and loot added to what gen_preserving.py writes
- lang
Textures: scripts/gen_fauna_coast_textures.py. Armour recipes: ArmorStationRecipes.java; rack: DryingRackBlock.java.
Generator order: ... gen_roots.py, gen_fauna_coast.py, gen_block_items.py, gen_unlocks.py, gen_advancements.py, gen_registry.py
Run: python scripts/gen_fauna_coast.py"""
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


def tag_add(path, *values):
    old = json.loads(path.read_text(encoding="utf-8")) if path.exists() else {"values": []}
    old["values"] += [v for v in values if v not in old["values"]]
    w(path, old)


def item_tag(name, *values):
    tag_add(D / f"mechanical/tags/item/{name}.json", *values)


def flat_item(item, parent="minecraft:item/generated"):
    w(A / f"models/item/{item}.json", {"parent": parent, "textures": {"layer0": f"mechanical:item/{item}"}})
    w(A / f"items/{item}.json", {"model": {"type": "minecraft:model", "model": f"mechanical:item/{item}"}})


def recipe(name, obj):
    w(D / f"mechanical/recipe/{name}.json", obj)


def shapeless(name, ingredients, result, count=1, category="misc"):
    recipe(name, {"type": "minecraft:crafting_shapeless", "category": category, "ingredients": ingredients,
                  "result": {"id": result, "count": count}})


lang = {}

# ---- animals: names, spawn eggs ----
ANIMALS = {"seal": "Seal", "walrus": "Walrus", "pike": "Pike", "sturgeon": "Sturgeon"}
for a, name in ANIMALS.items():
    lang[f"entity.mechanical.{a}"] = name
    lang[f"item.mechanical.{a}_spawn_egg"] = f"{name} Spawn Egg"
    flat_item(f"{a}_spawn_egg")

# ---- items ----
ITEMS = {"raw_seal_meat": "Raw Seal Meat", "cooked_seal_meat": "Cooked Seal Meat", "blubber": "Blubber",
         "raw_sealskin": "Raw Sealskin", "sealskin": "Sealskin", "sealskin_parka": "Sealskin Parka", "sealskin_boots": "Sealskin Boots",
         "raw_walrus_meat": "Raw Walrus Meat", "cooked_walrus_meat": "Cooked Walrus Meat", "walrus_ivory": "Walrus Ivory",
         "raw_walrus_hide": "Raw Walrus Hide", "walrus_hide": "Walrus Hide", "walrus_hide_cuirass": "Walrus Hide Cuirass",
         "ivory_knife": "Ivory Knife",
         "whole_pike": "Whole Pike", "raw_pike": "Raw Pike", "cooked_pike": "Cooked Pike",
         "whole_sturgeon": "Whole Sturgeon", "raw_sturgeon": "Raw Sturgeon", "cooked_sturgeon": "Cooked Sturgeon",
         "sturgeon_roe": "Sturgeon Roe", "isinglass": "Isinglass"}
for item, name in ITEMS.items():
    flat_item(item, "minecraft:item/handheld" if item == "ivory_knife" else "minecraft:item/generated")
    lang[f"item.mechanical.{item}"] = name

# ---- cooking: campfire (20 s) and smoker, like the other meats ----
for raw, cooked in (("raw_seal_meat", "cooked_seal_meat"), ("raw_walrus_meat", "cooked_walrus_meat"),
                    ("raw_pike", "cooked_pike"), ("raw_sturgeon", "cooked_sturgeon")):
    for kind, ticks in (("campfire_cooking", 400), ("smoking", 100)):
        recipe(f"{cooked}_from_{kind}", {"type": f"minecraft:{kind}", "category": "food", "cookingtime": ticks, "experience": 0.1,
                                         "ingredient": M + raw, "result": {"id": M + cooked}})

# ---- crafting ----
# gutting: whole fish + knife (StoneAgeEvents adds the Fish Bones; CoastFauna adds a sturgeon's Isinglass and roe)
shapeless("gut_pike", [M + "whole_pike", "#mechanical:knives"], M + "raw_pike", 2)
shapeless("gut_sturgeon", [M + "whole_sturgeon", "#mechanical:knives"], M + "raw_sturgeon", 2)
# the dried swim bladder soaks into glue: hide glue without a kettle
shapeless("hide_glue_from_isinglass", [M + "isinglass"], M + "hide_glue", 2)
# ivory: a knife (Flint Knife pattern), bone needles and fish hooks carved with a knife
recipe("ivory_knife", {"type": "minecraft:crafting_shaped", "category": "equipment", "pattern": ["I ", "TS"],
                       "key": {"I": M + "walrus_ivory", "T": M + "twine", "S": "minecraft:stick"}, "result": {"id": M + "ivory_knife"}})
shapeless("bone_needle_from_walrus_ivory", [M + "walrus_ivory", "#mechanical:knives"], M + "bone_needle", 1, "equipment")
shapeless("bone_fish_hook_from_walrus_ivory", [M + "walrus_ivory", "#mechanical:knives"], M + "bone_fish_hook", 3, "equipment")
# walrus hide rope: the hide cut in one long spiral strip, the strongest rope there is
shapeless("cordage_from_walrus_hide", [M + "walrus_hide", "#mechanical:knives"], M + "cordage", 6)
# Oil Lamp: a stone dish (rock chunks) with a plant-fibre wick
recipe("oil_lamp", {"type": "minecraft:crafting_shaped", "category": "building", "pattern": ["CWC", "CCC"],
                    "key": {"C": "#mechanical:rock_chunks", "W": M + "plant_fibre"}, "result": {"id": M + "oil_lamp"}})

# ---- tags ----
RAW = [M + "raw_seal_meat", M + "raw_walrus_meat"]
FISH = [M + "raw_pike", M + "raw_sturgeon", M + "whole_pike", M + "whole_sturgeon"]
COOKED = [M + "cooked_seal_meat", M + "cooked_walrus_meat", M + "cooked_pike", M + "cooked_sturgeon"]
item_tag("perishes_1_day", *RAW, *FISH, M + "sturgeon_roe")
item_tag("perishes_3_days", *COOKED)
item_tag("rots_to_flesh", *RAW, *FISH, *COOKED, M + "sturgeon_roe")
item_tag("smokable_meat", *RAW)
item_tag("smokable_fish", M + "raw_pike", M + "raw_sturgeon")
item_tag("whole_fish", M + "whole_pike", M + "whole_sturgeon")
item_tag("knives", M + "ivory_knife")
tag_add(D / "minecraft/tags/item/swords.json", M + "ivory_knife")
item_tag("lamp_fuel", M + "blubber", M + "tallow", M + "lard", M + "bear_fat")
item_tag("ivory_tool_materials", M + "walrus_ivory")
item_tag("repairs_sealskin_armor", M + "sealskin")
item_tag("repairs_walrus_hide_armor", M + "walrus_hide")
tag_add(D / "minecraft/tags/item/chest_armor.json", M + "sealskin_parka", M + "walrus_hide_cuirass")
tag_add(D / "minecraft/tags/item/foot_armor.json", M + "sealskin_boots")
w(A / "equipment/sealskin.json", {"layers": {"humanoid": [{"texture": "mechanical:sealskin"}]}})
w(A / "equipment/walrus_hide.json", {"layers": {"humanoid": [{"texture": "mechanical:walrus_hide"}]}})

# ---- loot: a dead pike is a whole pike (like vanilla fish); seal, walrus and sturgeon leave a carcass (Butchery) ----
w(D / "mechanical/loot_table/entities/pike.json", {"type": "minecraft:entity", "pools": [
    {"rolls": 1, "entries": [{"type": "minecraft:item", "name": M + "whole_pike"}]},
    {"rolls": 1, "condition": {"type": "minecraft:random_chance", "chance": 0.05},
     "entries": [{"type": "minecraft:item", "name": "minecraft:bone_meal"}]}], "random_sequence": "mechanical:entities/pike"})

# ---- fishing: pike in fresh water, a rare young sturgeon in rivers (added to gen_data.py's fish pool) ----
fish_path = D / "minecraft/loot_table/gameplay/fishing/fish.json"
fish = json.loads(fish_path.read_text(encoding="utf-8"))
entries = [e for e in fish["pools"][0]["entries"] if e["name"] not in (M + "whole_pike", M + "whole_sturgeon")]
PIKE_WATERS = ["minecraft:river", "minecraft:frozen_river", "minecraft:swamp", "mechanical:willow_fen"]
entries.append({"type": "minecraft:item", "name": M + "whole_pike", "weight": 30,
                "condition": {"type": "minecraft:location_check", "predicate": {"biomes": PIKE_WATERS}}})
entries.append({"type": "minecraft:item", "name": M + "whole_sturgeon", "weight": 4,
                "condition": {"type": "minecraft:location_check", "predicate": {"biomes": ["minecraft:river"]}}})
fish["pools"][0]["entries"] = entries
w(fish_path, fish)

# ---- spawns: (weight, min, max), biomes ----
SPAWNS = {
    "seal": ((10, 2, 5), ["minecraft:snowy_beach", "minecraft:stony_shore", "minecraft:frozen_ocean", "minecraft:deep_frozen_ocean"]),
    # a few on temperate beaches too
    "seal_temperate": ((3, 2, 3), ["minecraft:beach"]),
    "walrus": ((6, 2, 4), ["minecraft:frozen_ocean", "minecraft:deep_frozen_ocean", "minecraft:snowy_beach"]),
    "pike": ((8, 1, 2), ["minecraft:river", "minecraft:frozen_river", "minecraft:swamp", "mechanical:willow_fen"]),
    # forest lakes: only where there is water, so a low weight does it
    "pike_lakes": ((2, 1, 1), ["minecraft:forest", "minecraft:birch_forest", "minecraft:taiga", "mechanical:deciduous_forest",
                               "mechanical:pine_heath"]),
    "sturgeon": ((2, 1, 1), ["minecraft:river"]),
}
for name, ((wt, lo, hi), biomes) in SPAWNS.items():
    mob = name.split("_")[0]
    w(D / f"mechanical/neoforge/biome_modifier/add_{name}.json", {"type": "neoforge:add_spawns", "biomes": biomes,
        "spawners": [{"type": M + mob, "weight": wt, "count": {"type": "minecraft:uniform", "min_inclusive": lo, "max_inclusive": hi}}]})

# ---- Oil Lamp (OilLampBlock: fuel 0-4, lit) ----
FACES = ["down", "up", "north", "south", "east", "west"]


def el(f, t, tex, faces=FACES, uv=None):
    return {"from": f, "to": t, "faces": {d: ({"texture": tex, "uv": uv} if uv else {"texture": tex}) for d in faces}}


# a shallow stone dish, a lip spout at the front for the wick
dish = [el([4, 0, 4], [12, 1, 12], "#stone"), el([4, 1, 4], [12, 3, 5], "#stone"), el([4, 1, 11], [12, 3, 12], "#stone"),
        el([4, 1, 5], [5, 3, 11], "#stone"), el([11, 1, 5], [12, 3, 11], "#stone"), el([7, 1, 3], [9, 2.5, 4], "#stone")]
oil = [el([5, 1, 5], [11, 2.25, 11], "#oil", ["up"])]
wick = [el([7.5, 2, 3.5], [8.5, 3, 5], "#wick")]
flame = [{"from": [6.5, 2.5, 4.25], "to": [9.5, 7, 4.25], "shade": False, "faces": {"north": {"texture": "#fire", "uv": [0, 0, 16, 16]},
                                                                                   "south": {"texture": "#fire", "uv": [0, 0, 16, 16]}}},
         {"from": [8, 2.5, 2.75], "to": [8, 7, 5.75], "shade": False, "faces": {"east": {"texture": "#fire", "uv": [0, 0, 16, 16]},
                                                                               "west": {"texture": "#fire", "uv": [0, 0, 16, 16]}}}]
TEX = {"particle": "mechanical:block/oil_lamp", "stone": "mechanical:block/oil_lamp", "oil": "mechanical:block/oil_lamp_oil",
       "wick": "mechanical:block/oil_lamp_wick", "fire": "minecraft:block/fire_0"}
w(A / "models/block/oil_lamp.json", {"parent": "minecraft:block/block", "textures": TEX, "elements": dish + wick})
w(A / "models/block/oil_lamp_filled.json", {"parent": "minecraft:block/block", "textures": TEX, "elements": dish + oil + wick})
w(A / "models/block/oil_lamp_lit.json", {"parent": "minecraft:block/block", "textures": TEX, "elements": dish + oil + wick + flame})
w(A / "blockstates/oil_lamp.json", {"variants": {f"fuel={f},lit={str(lit).lower()}":
    {"model": M + "block/" + ("oil_lamp_lit" if lit and f > 0 else "oil_lamp_filled" if f > 0 else "oil_lamp")}
    for f in range(5) for lit in (False, True)}})
w(A / "items/oil_lamp.json", {"model": {"type": "minecraft:model", "model": M + "block/oil_lamp_filled"}})
w(D / "mechanical/loot_table/blocks/oil_lamp.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [
    {"type": "minecraft:item", "name": M + "oil_lamp"}], "condition": {"type": "minecraft:survives_explosion"}}],
    "random_sequence": "mechanical:blocks/oil_lamp"})
tag_add(D / "minecraft/tags/block/mineable/pickaxe.json", M + "oil_lamp")
lang["block.mechanical.oil_lamp"] = "Oil Lamp"

# ---- sealskin and walrus hide on the tall Drying Rack (hide 18/19 one block, 20/21 both; DryingRackBlock) ----
ROT = [("north", 0), ("east", 90), ("south", 180), ("west", 270)]


def pelt_plane(tex, y0, y1):
    return {"textures": {"particle": "mechanical:block/drying_rack", "front": f"mechanical:block/{tex}", "back": f"mechanical:block/{tex}_back"},
            "elements": [{"from": [3, y0, 7], "to": [13, y1, 7], "faces": {
                "north": {"uv": [3, 16 - y1, 13, 16 - y0], "texture": "#front"}, "south": {"uv": [13, 16 - y1, 3, 16 - y0], "texture": "#back"}}}]}


for stage in ("raw", "dry"):
    w(A / f"models/block/drying_rack_seal_{stage}.json", pelt_plane(f"drying_rack_seal_{stage}", 2, 13))
    w(A / f"models/block/drying_rack_walrus_{stage}_lower.json", pelt_plane(f"drying_rack_walrus_{stage}_lower", 4, 16))
    w(A / f"models/block/drying_rack_walrus_{stage}_upper.json", pelt_plane(f"drying_rack_walrus_{stage}_upper", 0, 13))
rot = lambda y: {"y": y} if y else {}
bs_path = A / "blockstates/drying_rack.json"
bs = json.loads(bs_path.read_text(encoding="utf-8"))
OURS = ("drying_rack_seal_", "drying_rack_walrus_")
parts = [p for p in bs["multipart"] if not p["apply"]["model"].removeprefix("mechanical:block/").startswith(OURS)
         and p.get("when", {}).get("hide") != "18|19|20|21"]
parts += [{"when": {"facing": f, "part": "single", "hide": "18|19|20|21"}, "apply": {"model": "mechanical:block/drying_rack_0", **rot(y)}}
          for f, y in ROT]  # never hung there; just not invisible if set by command
parts += [{"when": {"facing": f, "part": "lower|upper", "hide": str(h)}, "apply": {"model": f"mechanical:block/drying_rack_seal_{st}", **rot(y)}}
          for f, y in ROT for h, st in ((18, "raw"), (19, "dry"))]
parts += [{"when": {"facing": f, "part": half, "hide": str(h)}, "apply": {"model": f"mechanical:block/drying_rack_walrus_{st}_{half}", **rot(y)}}
          for f, y in ROT for half in ("lower", "upper") for h, st in ((20, "raw"), (21, "dry"))]
bs["multipart"] = parts
w(bs_path, bs)
loot_path = D / "mechanical/loot_table/blocks/drying_rack.json"
loot = json.loads(loot_path.read_text(encoding="utf-8"))


def spot_drop(h, item, **state):
    return {"type": "minecraft:item", "name": item,
            "condition": {"type": "minecraft:match_block", "blocks": M + "drying_rack", "state": {"hide": str(h), **state}}}


hide_pool = next(p for p in loot["pools"] if any(e.get("condition", {}).get("state", {}).get("hide") == "16" for e in p["entries"]))
hide_pool["entries"] = [e for e in hide_pool["entries"] if e.get("condition", {}).get("state", {}).get("hide") not in ("18", "19", "20", "21")]
hide_pool["entries"] += [spot_drop(18, M + "raw_sealskin"), spot_drop(19, M + "sealskin"),
                         spot_drop(20, M + "raw_walrus_hide", part="lower"), spot_drop(21, M + "walrus_hide", part="lower")]
w(loot_path, loot)

merge(A / "lang/en_us.json", lang)
print("fauna coast ok")
