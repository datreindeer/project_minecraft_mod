"""Data for the river fauna (wildlife/river/: RiverFauna, Beaver, BeaverWorks, Otter, MosquitoSwarm, PeltHoopBlock).

- items: Raw/Cooked Beaver, Beaver Tail / Roasted Beaver Tail, Raw/Cooked Otter, Raw and cured Beaver and Otter Pelts,
  Castoreum, Beaver Tooth, Castoreum Salve, Beaver Tooth Knife, Beaver Fur Hat, Otter Fur Cape, spawn eggs (models, lang)
- recipes: meats and the tail on the Campfire (20 s) or in the smoker; Stretching Hoop (4 Sticks + Twine); Castoreum
  Salve (Castoreum + Tallow + Bark -> 2); Beaver Tooth Knife (like the Flint Knife, the tooth for the flint); Beaver Fur
  Hat (2 Beaver Pelts + Sinew); Otter Fur Cape (3 Otter Pelts + 2 Sinew)
- tags: spoilage (raw 1 day, cooked 3 days, rots to flesh), smokable meat, snare bait (castoreum), knives and swords,
  head/chest armour, waterproof fur, repair tags, basket storage for the pelts, mineable
- blocks: Beaver Dam (cube, drops sticks) and the Stretching Hoop (frame + a laced pelt per state, multipart; drops the
  hoop and its pelt)
- equipment assets beaver_fur / otter_fur; spawns (biome modifiers add_beaver*, add_otter, add_mosquito_swarm)
Textures: scripts/gen_fauna_river_textures.py. Docs: docs/fauna/river.md.
Generator order: ... gen_kettle.py, gen_roots.py, gen_fauna_river.py, gen_block_items.py, gen_unlocks.py,
gen_advancements.py, gen_registry.py (it adds to tags that earlier generators rewrite).
Run: python scripts/gen_fauna_river.py"""
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
    ns, path = name.split(":") if ":" in name else ("mechanical", name)
    tag_add(D / f"{ns}/tags/item/{path}.json", *values)


def flat_item(item):
    w(A / f"models/item/{item}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"mechanical:item/{item}"}})
    w(A / f"items/{item}.json", {"model": {"type": "minecraft:model", "model": f"mechanical:item/{item}"}})


def recipe(name, obj):
    w(D / f"mechanical/recipe/{name}.json", obj)


ITEMS = {
    "raw_beaver": "Raw Beaver", "cooked_beaver": "Cooked Beaver", "beaver_tail": "Beaver Tail", "roasted_beaver_tail": "Roasted Beaver Tail",
    "raw_otter": "Raw Otter", "cooked_otter": "Cooked Otter", "raw_beaver_pelt": "Raw Beaver Pelt", "beaver_pelt": "Beaver Pelt",
    "raw_otter_pelt": "Raw Otter Pelt", "otter_pelt": "Otter Pelt", "castoreum": "Castoreum", "beaver_tooth": "Beaver Tooth",
    "castoreum_salve": "Castoreum Salve", "beaver_tooth_knife": "Beaver Tooth Knife", "beaver_fur_hat": "Beaver Fur Hat",
    "otter_fur_cape": "Otter Fur Cape",
    "beaver_spawn_egg": "Beaver Spawn Egg", "otter_spawn_egg": "Otter Spawn Egg", "mosquito_swarm_spawn_egg": "Mosquito Swarm Spawn Egg",
}
lang = {f"item.mechanical.{k}": v for k, v in ITEMS.items()}
lang.update({"entity.mechanical.beaver": "Beaver", "entity.mechanical.otter": "Otter", "entity.mechanical.mosquito_swarm": "Mosquito Swarm",
             "block.mechanical.beaver_dam": "Beaver Dam", "block.mechanical.pelt_hoop": "Stretching Hoop"})
for item in ITEMS:
    flat_item(item)

# ---- food: raw -> cooked on the campfire (20 s) or in the smoker, like the bird meats (gen_birds2.py) ----
COOK = {"raw_beaver": "cooked_beaver", "beaver_tail": "roasted_beaver_tail", "raw_otter": "cooked_otter"}
for raw, cooked in COOK.items():
    for kind, ticks in (("campfire_cooking", 400), ("smoking", 100)):
        recipe(f"{cooked}_from_{kind}", {"type": f"minecraft:{kind}", "category": "food", "cookingtime": ticks, "experience": 0.1,
                                         "ingredient": M + raw, "result": {"id": M + cooked}})
item_tag("perishes_1_day", *(M + r for r in COOK))
item_tag("perishes_3_days", *(M + c for c in COOK.values()))
item_tag("rots_to_flesh", *(M + r for r in COOK), *(M + c for c in COOK.values()))
item_tag("smokable_meat", *(M + r for r in COOK))

# ---- products ----
recipe("pelt_hoop", {"type": "minecraft:crafting_shaped", "category": "misc", "pattern": [" S ", "STS", " S "],
                     "key": {"S": "minecraft:stick", "T": "mechanical:twine"}, "result": {"id": M + "pelt_hoop"}})
recipe("castoreum_salve", {"type": "minecraft:crafting_shapeless", "category": "misc",
                           "ingredients": [M + "castoreum", M + "tallow", "#mechanical:barks"], "result": {"id": M + "castoreum_salve", "count": 2}})
recipe("beaver_tooth_knife", {"type": "minecraft:crafting_shaped", "category": "equipment", "pattern": ["B ", "TS"],
                              "key": {"B": M + "beaver_tooth", "S": "minecraft:stick", "T": M + "twine"}, "result": {"id": M + "beaver_tooth_knife"}})
recipe("beaver_fur_hat", {"type": "minecraft:crafting_shapeless", "category": "equipment",
                          "ingredients": [M + "beaver_pelt", M + "beaver_pelt", M + "sinew"], "result": {"id": M + "beaver_fur_hat"}})
recipe("otter_fur_cape", {"type": "minecraft:crafting_shapeless", "category": "equipment",
                          "ingredients": [M + "otter_pelt"] * 3 + [M + "sinew"] * 2, "result": {"id": M + "otter_fur_cape"}})
item_tag("snare_bait", M + "castoreum")
item_tag("knives", M + "beaver_tooth_knife")
item_tag("minecraft:swords", M + "beaver_tooth_knife")
item_tag("minecraft:head_armor", M + "beaver_fur_hat")
item_tag("minecraft:chest_armor", M + "otter_fur_cape")
item_tag("waterproof_fur", M + "beaver_fur_hat", M + "otter_fur_cape")
w(D / "mechanical/tags/item/repairs_beaver_fur_armor.json", {"values": [M + "beaver_pelt"]})
w(D / "mechanical/tags/item/repairs_otter_fur_armor.json", {"values": [M + "otter_pelt"]})
w(D / "mechanical/tags/item/beaver_tooth_tool_materials.json", {"values": [M + "beaver_tooth"]})
item_tag("basket_storable", *(M + p for p in ("raw_beaver_pelt", "beaver_pelt", "raw_otter_pelt", "otter_pelt")))
for asset in ("beaver_fur", "otter_fur"):
    w(A / f"equipment/{asset}.json", {"layers": {"humanoid": [{"texture": f"mechanical:{asset}"}]}})

# ---- Beaver Dam: sticks and mud; drops 1-3 sticks ----
w(A / "models/block/beaver_dam.json", {"parent": "minecraft:block/cube_all", "textures": {"all": "mechanical:block/beaver_dam"}})
w(A / "blockstates/beaver_dam.json", {"variants": {"": {"model": "mechanical:block/beaver_dam"}}})
w(A / "items/beaver_dam.json", {"model": {"type": "minecraft:model", "model": "mechanical:block/beaver_dam"}})
w(D / "mechanical/loot_table/blocks/beaver_dam.json", {"type": "minecraft:block", "random_sequence": "mechanical:blocks/beaver_dam",
    "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": "minecraft:stick",
                                        "modifier": {"type": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": 1, "max": 3}}}],
               "condition": {"type": "minecraft:survives_explosion"}}]})
tag_add(D / "minecraft/tags/block/mineable/axe.json", M + "beaver_dam", M + "pelt_hoop")
tag_add(D / "minecraft/tags/block/mineable/shovel.json", M + "beaver_dam")

# ---- Stretching Hoop: a willow ring (octagon of rods) on two feet, facing north; the pelt laced in at z 8 ----
FACES = ["down", "up", "north", "south", "east", "west"]


def rod(frm, to, angle=None, origin=None):
    e = {"from": frm, "to": to, "faces": {f: {"texture": "#wood"} for f in FACES}}
    if angle:
        e["rotation"] = {"origin": origin, "axis": "z", "angle": angle}
    return e


ring = [rod([4, 2, 7.5], [12, 3, 8.5]), rod([4, 14, 7.5], [12, 15, 8.5]),  # bottom, top
        rod([1, 5, 7.5], [2, 12, 8.5]), rod([14, 5, 7.5], [15, 12, 8.5]),  # sides
        # the bent corners: rods at 45 degrees bridging the bars
        rod([1.2, 3.25, 7.5], [4.8, 4.25, 8.5], -45, [3, 3.75, 8]), rod([1.2, 12.75, 7.5], [4.8, 13.75, 8.5], 45, [3, 13.25, 8]),
        rod([11.2, 12.75, 7.5], [14.8, 13.75, 8.5], -45, [13, 13.25, 8]), rod([11.2, 3.25, 7.5], [14.8, 4.25, 8.5], 45, [13, 3.75, 8]),
        # feet: crosspieces it stands on, and the posts up to the ring
        rod([3, 0, 5.5], [5, 1, 10.5]), rod([11, 0, 5.5], [13, 1, 10.5]), rod([3.5, 1, 7.5], [4.5, 3, 8.5]), rod([11.5, 1, 7.5], [12.5, 3, 8.5])]
w(A / "models/block/pelt_hoop.json", {"parent": "minecraft:block/block", "textures": {"particle": "mechanical:block/pelt_hoop", "wood": "mechanical:block/pelt_hoop"},
                                      "elements": ring})
STATES = ["beaver", "beaver_dry", "otter", "otter_dry"]
for s in STATES:
    w(A / f"models/block/pelt_hoop_{s}.json", {"textures": {"particle": f"mechanical:block/pelt_hoop_{s}_skin",
        "skin": f"mechanical:block/pelt_hoop_{s}_skin", "fur": f"mechanical:block/pelt_hoop_{s}_fur"},
        "elements": [{"from": [1, 2, 8], "to": [15, 15, 8], "faces": {
            "north": {"uv": [1, 1, 15, 14], "texture": "#skin"}, "south": {"uv": [15, 1, 1, 14], "texture": "#fur"}}}]})
ROT = {"north": 0, "east": 90, "south": 180, "west": 270}
parts = [{"when": {"facing": f}, "apply": {"model": M + "block/pelt_hoop", **({"y": y} if y else {})}} for f, y in ROT.items()]
parts += [{"when": {"facing": f, "pelt": s}, "apply": {"model": M + f"block/pelt_hoop_{s}", **({"y": y} if y else {})}}
          for f, y in ROT.items() for s in STATES]
w(A / "blockstates/pelt_hoop.json", {"multipart": parts})
w(A / "items/pelt_hoop.json", {"model": {"type": "minecraft:model", "model": M + "block/pelt_hoop"}})
PELT_ITEM = {"beaver": "raw_beaver_pelt", "beaver_dry": "beaver_pelt", "otter": "raw_otter_pelt", "otter_dry": "otter_pelt"}
w(D / "mechanical/loot_table/blocks/pelt_hoop.json", {"type": "minecraft:block", "random_sequence": "mechanical:blocks/pelt_hoop", "pools": [
    {"rolls": 1, "entries": [{"type": "minecraft:item", "name": M + "pelt_hoop"}], "condition": {"type": "minecraft:survives_explosion"}},
    {"rolls": 1, "entries": [{"type": "minecraft:item", "name": M + item, "condition": {
        "type": "minecraft:match_block", "blocks": M + "pelt_hoop", "state": {"pelt": s}}} for s, item in PELT_ITEM.items()]}]})

# ---- spawns: (weight, min, max), biomes. Beavers and otters also need water within 3 blocks (RiverFauna.shore). ----
FORESTS = ["minecraft:forest", "minecraft:birch_forest", "minecraft:old_growth_birch_forest", "minecraft:dark_forest", "minecraft:taiga",
           "minecraft:old_growth_pine_taiga", "minecraft:old_growth_spruce_taiga", "mechanical:deciduous_forest", "mechanical:pine_heath"]
SPAWNS = {
    "beaver": ((3, 1, 2), ["minecraft:river", "mechanical:willow_fen", "minecraft:swamp"]),
    "beaver_forest": ((2, 1, 2), FORESTS),
    "otter": ((4, 1, 2), ["minecraft:river", "minecraft:frozen_river", "minecraft:swamp", "minecraft:mangrove_swamp", "minecraft:stony_shore",
                          "minecraft:beach", "mechanical:willow_fen"]),
}
for name, ((wt, lo, hi), biomes) in SPAWNS.items():
    w(D / f"mechanical/neoforge/biome_modifier/add_{name}.json", {"type": "neoforge:add_spawns", "biomes": biomes, "spawners": [
        {"type": M + name.removesuffix("_forest"), "weight": wt, "count": {"type": "minecraft:uniform", "min_inclusive": lo, "max_inclusive": hi}}]})
# mosquitoes: ambient (like bats), only at dusk and night by water in warm wetlands (MosquitoSwarm.canSpawn)
w(D / "mechanical/neoforge/biome_modifier/add_mosquito_swarm.json", {"type": "neoforge:add_spawns",
    "biomes": ["minecraft:swamp", "minecraft:mangrove_swamp", "mechanical:willow_fen"],
    "spawners": [{"type": M + "mosquito_swarm", "weight": 12, "count": {"type": "minecraft:uniform", "min_inclusive": 1, "max_inclusive": 2}}]})

merge(A / "lang/en_us.json", lang)
print("fauna river ok")
