"""Root crops get proper sources (docs/farming.md, "Rotfrukter"; Roots.java, RootCellarBlock*.java).

- Wild Carrot Plant (meadows, plains, flower forests) -> Wild Carrot (food, sown as the Wild Carrot crop; ripe: 1-2 Wild
  Carrot and a 25 % chance of a real Carrot). Sea Beet (beaches, stony shores) -> Sea Beet Leaves, sometimes Beetroot Seeds.
  Models, blockstates, loot, worldgen (placed like gen_farming's wild grains), lang.
- Potatoes: gen_data.py drops them from village houses, fields and all structure/mob loot; here the zombie-village
  processor lists (vanilla, not overridden elsewhere) turn their potato crops into carrots. The only source left is the
  tribe elders' rare offer (Tribesperson.getOffers).
- Shelf lives (new tags perishes_10_days / perishes_20_days, read by Preserving.shelfLife): carrot, beetroot, wild carrot 10
  days, potato 20; Sea Beet Leaves 1 day (added to gen_preserving's perishes_1_day). #mechanical:cellar_produce keeps 4x
  longer in a dug-in Root Cellar; the cellar block (models, loot, recipe).
Textures: scripts/gen_roots_textures.py.
Generator order: ... gen_firepit.py, gen_birds2.py, gen_roots.py, gen_block_items.py, gen_unlocks.py, gen_advancements.py,
gen_registry.py (after gen_farming / gen_preserving / gen_biomes, whose tags and biome lists it adds to).
Run: python scripts/gen_roots.py"""
import json
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
R = ROOT / "src/main/resources"
A, D = R / "assets/mechanical", R / "data"
JAR = ROOT.parent.parent / "project_minecraft_mod/build/moddev/artifacts/minecraft-patched-26.3.0.95-merged.jar"
if not JAR.exists():
    JAR = ROOT / "build/moddev/artifacts/minecraft-patched-26.3.0.95-merged.jar"
M = "mechanical:"


def w(path, obj):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, indent=2) + "\n", encoding="utf-8")


def tag_add(path, *values):
    old = json.loads(path.read_text(encoding="utf-8")) if path.exists() else {"values": []}
    old["values"] += [v for v in values if v not in old["values"]]
    w(path, old)


def item(name, lo=1, hi=1, *conditions):
    e = {"type": "minecraft:item", "name": name}
    if (lo, hi) != (1, 1):
        e["modifier"] = [{"type": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": lo, "max": hi}}]
    if conditions:
        e["condition"] = all_of(*conditions)
    return e


def all_of(*terms):
    return terms[0] if len(terms) == 1 else {"type": "minecraft:all_of", "terms": list(terms)}


def chance(p):
    return {"type": "minecraft:random_chance", "chance": p}


def loot(block, pools):
    w(D / f"mechanical/loot_table/blocks/{block}.json", {"type": "minecraft:block", "pools": pools, "random_sequence": f"mechanical:blocks/{block}"})


KNIFE_OR_SICKLE = {"type": "minecraft:match_tool", "predicate": {"items": [M + "flint_knife", M + "tusk_dagger", M + "iron_knife",
                                                                          M + "copper_knife", M + "bronze_knife", M + "flint_sickle"]}}
lang = {}

# ---------------------------------------------------------------- wild plants
for block, name in (("wild_carrot_plant", "Wild Carrot Plant"), ("sea_beet", "Sea Beet")):
    w(A / f"models/block/{block}.json", {"parent": "minecraft:block/cross", "textures": {"cross": f"mechanical:block/{block}"}})
    w(A / f"blockstates/{block}.json", {"variants": {"": {"model": f"mechanical:block/{block}"}}})
    lang[f"block.mechanical.{block}"] = name
# the root sits deep: dug out with a knife or sickle 1-2, pulled by hand it snaps off half the time
loot("wild_carrot_plant", [{"rolls": 1, "entries": [{"type": "minecraft:alternatives", "children": [
    item(M + "wild_carrot", 1, 2, KNIFE_OR_SICKLE), item(M + "wild_carrot", 1, 1, chance(0.5))]}]}])
# leaves pluck easily; the seed stalks are tough: cut 35 %, by hand 15 %
loot("sea_beet", [{"rolls": 1, "entries": [item(M + "sea_beet_leaves", 1, 3)]},
                  {"rolls": 1, "entries": [{"type": "minecraft:alternatives", "children": [
                      item("minecraft:beetroot_seeds", 1, 1, KNIFE_OR_SICKLE, chance(0.35)),
                      item("minecraft:beetroot_seeds", 1, 1, chance(0.15))]}]}])

# ---------------------------------------------------------------- the wild carrot crop (4 drawn stages like vanilla carrots)
STAGE = [0, 0, 1, 1, 2, 2, 2, 3]
for s in range(4):
    w(A / f"models/block/wild_carrot_stage{s}.json", {"parent": "minecraft:block/crop", "textures": {"crop": f"mechanical:block/wild_carrot_stage{s}"}})
w(A / "blockstates/wild_carrot_crop.json", {"variants": {f"age={a}": {"model": f"mechanical:block/wild_carrot_stage{STAGE[a]}"} for a in range(8)}})
lang["block.mechanical.wild_carrot_crop"] = "Wild Carrots"
RIPE = {"type": "minecraft:match_block", "blocks": M + "wild_carrot_crop", "state": {"age": "7"}}
# ripe: 1-2 roots (like carrots), and now and then a fatter, sweeter one: a real Carrot. Unripe: nothing
loot("wild_carrot_crop", [{"rolls": 1, "condition": RIPE, "entries": [item(M + "wild_carrot", 1, 2)]},
                          {"rolls": 1, "condition": all_of(RIPE, chance(0.25)), "entries": [item("minecraft:carrot")]}])

# ---------------------------------------------------------------- worldgen (as gen_farming.forage)
tri = lambda m: {"type": "minecraft:trapezoid", "min": -m, "max": m, "plateau": 0}


def forage(name, block, biomes, rarity, n, spread, ground):
    w(D / f"mechanical/worldgen/feature/{name}.json", {"type": "minecraft:simple_block", "to_place": {"id": M + block}})
    w(D / f"mechanical/worldgen/placed_feature/{name}.json", {"feature": M + name, "placement": [
        {"type": "minecraft:rarity_filter", "chance": rarity}, {"type": "minecraft:in_square"}, {"type": "minecraft:count", "count": n},
        {"type": "minecraft:offset", "x": tri(spread), "y": 0, "z": tri(spread)},
        {"type": "minecraft:heightmap", "heightmap": "MOTION_BLOCKING_NO_LEAVES"}, {"type": "minecraft:biome"},
        {"type": "minecraft:block_predicate_filter", "predicate": {"type": "minecraft:all_of", "predicates": [
            {"type": "minecraft:replaceable"}, ground]}}]})
    w(D / f"mechanical/neoforge/biome_modifier/add_{name}.json", {"type": "neoforge:add_features", "biomes": biomes,
        "features": M + name, "step": "vegetal_decoration"})


below = lambda t: {"type": "minecraft:matching_block_tag", "tag": t, "offset": [0, -1, 0]}
forage("wild_carrot", "wild_carrot_plant", ["minecraft:plains", "minecraft:sunflower_plains", "minecraft:meadow", "minecraft:flower_forest",
                                             M + "hazel_meadow"], 12, 5, 3, below("minecraft:substrate_overworld"))
forage("sea_beet", "sea_beet", ["minecraft:beach", "minecraft:stony_shore"], 2, 10, 4, {"type": "minecraft:any_of", "predicates": [
    below("minecraft:sand"), below("minecraft:base_stone_overworld"), below("minecraft:substrate_overworld"),
    {"type": "minecraft:matching_blocks", "blocks": "minecraft:gravel", "offset": [0, -1, 0]}]})

# ---------------------------------------------------------------- items
for i, n in {"wild_carrot": "Wild Carrot", "sea_beet_leaves": "Sea Beet Leaves"}.items():
    w(A / f"models/item/{i}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"mechanical:item/{i}"}})
    w(A / f"items/{i}.json", {"model": {"type": "minecraft:model", "model": f"mechanical:item/{i}"}})
    lang[f"item.mechanical.{i}"] = n

# ---------------------------------------------------------------- Root Cellar: plank bin lined with straw, lid on top
def faces(tex, **over):
    return {d: {"texture": over.get(d, tex)} for d in ("north", "south", "east", "west", "up", "down")}


CT = {"particle": "mechanical:block/root_cellar_side", "side": "mechanical:block/root_cellar_side", "bottom": "mechanical:block/root_cellar_bottom",
      "lid": "mechanical:block/root_cellar_lid", "lid_side": "mechanical:block/root_cellar_lid_side", "inside": "mechanical:block/root_cellar_inside"}
BODY = {"from": [1, 0, 1], "to": [15, 12, 15], "faces": faces("#side", up="#inside", down="#bottom")}
LID = {"from": [0, 12, 0], "to": [16, 14, 16], "faces": faces("#lid_side", up="#lid", down="#lid")}
# open: the lid stands leaning against the back (north) edge
LID_UP = {"from": [0, 12, 0], "to": [16, 28, 2], "faces": {"north": {"texture": "#lid"}, "south": {"texture": "#lid"},
          "east": {"uv": [0, 0, 2, 16], "texture": "#lid_side"}, "west": {"uv": [0, 0, 2, 16], "texture": "#lid_side"},
          "up": {"uv": [0, 0, 16, 2], "texture": "#lid_side"}, "down": {"uv": [0, 0, 16, 2], "texture": "#lid_side"}}}
w(A / "models/block/root_cellar.json", {"parent": "minecraft:block/block", "textures": CT, "elements": [BODY, LID]})
w(A / "models/block/root_cellar_open.json", {"parent": "minecraft:block/block", "textures": CT, "elements": [BODY, LID_UP]})
w(A / "blockstates/root_cellar.json", {"variants": {"open=false": {"model": M + "block/root_cellar"},
                                                     "open=true": {"model": M + "block/root_cellar_open"}}})
w(A / "items/root_cellar.json", {"model": {"type": "minecraft:model", "model": M + "block/root_cellar"}})
lang["block.mechanical.root_cellar"] = "Root Cellar"
lang["message.mechanical.root_cellar.not_dug_in"] = "Too much daylight: dig the Root Cellar into the ground to keep food cool"
loot("root_cellar", [{"rolls": 1, "condition": {"type": "minecraft:survives_explosion"}, "entries": [item(M + "root_cellar")]}])
w(D / "mechanical/recipe/root_cellar.json", {"type": "minecraft:crafting_shaped", "category": "misc", "pattern": ["PSP", "P P", "PPP"],
    "key": {"P": "#minecraft:planks", "S": M + "straw"}, "result": {"id": M + "root_cellar"}})

# ---------------------------------------------------------------- tags
BT, IT = D / "minecraft/tags/block", D / "mechanical/tags/item"
tag_add(BT / "crops.json", M + "wild_carrot_crop")
tag_add(BT / "maintains_farmland.json", M + "wild_carrot_crop")
tag_add(BT / "mineable/axe.json", M + "root_cellar")
tag_add(D / "mechanical/tags/block/mineable/sickle.json", M + "wild_carrot_plant", M + "sea_beet")
tag_add(IT / "compostable.json", M + "wild_carrot", M + "sea_beet_leaves")
tag_add(IT / "perishes_1_day.json", M + "sea_beet_leaves")
w(IT / "perishes_10_days.json", {"values": ["minecraft:carrot", "minecraft:beetroot", M + "wild_carrot",
                                         "minecraft:melon", "minecraft:pumpkin"]})  # whole fruit keeps longer than a cut slice
w(IT / "perishes_20_days.json", {"values": ["minecraft:potato"]})
w(IT / "cellar_produce.json", {"values": ["minecraft:carrot", "minecraft:potato", "minecraft:beetroot", M + "wild_carrot", M + "sea_beet_leaves"]})

# ---------------------------------------------------------------- potatoes: zombie villages plant carrots instead
with zipfile.ZipFile(JAR) as jar:
    for biome in ("plains", "desert", "savanna", "snowy", "taiga"):
        path = f"data/minecraft/worldgen/processor_list/zombie_{biome}.json"
        if path not in jar.namelist():
            continue
        text = jar.read(path).decode().replace('"minecraft:potatoes"', '"minecraft:carrots"')
        lists = json.loads(text)
        # potato crops placed by the templates themselves become carrots too
        lists["processors"].append({"processor_type": "minecraft:rule", "rules": [{"input_predicate": {
            "predicate_type": "minecraft:block_match", "block": "minecraft:potatoes"}, "location_predicate": {"predicate_type": "minecraft:always_true"},
            "output_state": {"id": "minecraft:carrots", "properties": {"age": "0"}}}]})
        w(D / f"minecraft/worldgen/processor_list/zombie_{biome}.json", lists)

# ---------------------------------------------------------------- lang
lang_path = A / "lang/en_us.json"
full = json.loads(lang_path.read_text(encoding="utf-8"))
full.update(lang)
lang_path.write_text(json.dumps(full, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
print("roots ok")
