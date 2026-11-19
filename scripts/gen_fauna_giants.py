"""Data for the Ice Age giants (GiantsFauna.java): Woolly Mammoth, Woolly Rhinoceros, Steppe Bison, Musk Ox.

- items: meats (raw / cooked), Bison Tongue, cured hides, Mammoth Ivory / Wool / Fat, Rhino Horn, Qiviut, Ivory Knife,
  Rhino Horn Club, spawn eggs (models, lang)
- blocks: the pegged raw hides (raw / cured, facing), Mammoth / Bison / Musk Ox Rug (the Bear Rug's model), Ivory Figurine
  (blockstates, models, loot)
- recipes: meat and tongue on the Campfire (20 s) or in the smoker; Mammoth Fat renders into Tallow on the Campfire;
  hides cut into Leather with a knife; rugs; Ivory Knife, Rhino Horn Club, a hide Shield; Mammoth Wool into Twine and
  wool blocks; Qiviut into Wool Thread; the Ivory Figurine and Bone Needles carved with a knife
- tags (added to gen_preserving.py's): raw meat perishes in 1 day, cooked in 3, all rot to flesh, raw is smokable/dryable;
  #mechanical:knives gets the Ivory Knife; repair tags of the ivory and horn tools
- spawns: biome modifiers (also the mod's Hazel Meadow and Pine Heath)
Textures: scripts/gen_fauna_giants_textures.py. Docs: docs/fauna/giants.md.
Generator order: ... gen_roots.py, gen_fauna_giants.py, gen_block_items.py, gen_unlocks.py, gen_advancements.py, gen_registry.py.
Run: python scripts/gen_fauna_giants.py"""
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


def tag_add(name, *values, kind="item"):
    path = D / f"mechanical/tags/{kind}/{name}.json"
    old = json.loads(path.read_text(encoding="utf-8")) if path.exists() else {"values": []}
    old["values"] += [v for v in values if v not in old["values"]]
    w(path, old)


def flat_item(item, parent="minecraft:item/generated"):
    w(A / f"models/item/{item}.json", {"parent": parent, "textures": {"layer0": f"mechanical:item/{item}"}})
    w(A / f"items/{item}.json", {"model": {"type": "minecraft:model", "model": f"mechanical:item/{item}"}})


def recipe(name, obj):
    w(D / f"mechanical/recipe/{name}.json", obj)


def shapeless(name, ingredients, result, count=1, category="misc"):
    recipe(name, {"type": "minecraft:crafting_shapeless", "category": category, "ingredients": ingredients,
                  "result": {"id": result, "count": count}})


def shaped(name, pattern, key, result, count=1, category="misc"):
    recipe(name, {"type": "minecraft:crafting_shaped", "category": category, "pattern": pattern, "key": key,
                  "result": {"id": result, "count": count}})


lang = {}

# ---- entities ----
ANIMALS = {"woolly_mammoth": "Woolly Mammoth", "woolly_rhino": "Woolly Rhinoceros", "steppe_bison": "Steppe Bison", "musk_ox": "Musk Ox"}
for e, name in ANIMALS.items():
    lang[f"entity.mechanical.{e}"] = name
    lang[f"item.mechanical.{e}_spawn_egg"] = f"{name} Spawn Egg"
    flat_item(f"{e}_spawn_egg")

# ---- meat ----
MEATS = {"mammoth": "Mammoth", "rhino": "Rhino", "bison": "Bison", "musk_ox": "Musk Ox"}
RAW, COOKED = [], []
for meat, name in MEATS.items():
    raw, cooked = f"raw_{meat}", f"cooked_{meat}"
    lang[f"item.mechanical.{raw}"] = f"Raw {name} Meat"
    lang[f"item.mechanical.{cooked}"] = f"Cooked {name} Meat"
    RAW.append(M + raw)
    COOKED.append(M + cooked)
lang["item.mechanical.bison_tongue"] = "Bison Tongue"
lang["item.mechanical.cooked_bison_tongue"] = "Roast Bison Tongue"
RAW.append(M + "bison_tongue")
COOKED.append(M + "cooked_bison_tongue")
for raw, cooked in zip(RAW, COOKED):
    for item in (raw, cooked):
        flat_item(item.removeprefix(M))
    # only the campfire (and the smoker, later) cook food, like the scraps (gen_data.py)
    for kind, ticks in (("campfire_cooking", 400), ("smoking", 100)):
        recipe(f"{cooked.removeprefix(M)}_from_{kind}", {"type": f"minecraft:{kind}", "category": "food",
               "cookingtime": ticks, "experience": 0.1, "ingredient": raw, "result": {"id": cooked}})
tag_add("perishes_1_day", *RAW)
tag_add("perishes_3_days", *COOKED)
tag_add("rots_to_flesh", *RAW, *COOKED)
tag_add("smokable_meat", *RAW)

# ---- parts and products ----
ITEMS = {
    "mammoth_hide": "Mammoth Hide", "rhino_hide": "Rhino Hide", "bison_hide": "Bison Hide", "musk_ox_hide": "Musk Ox Hide",
    "mammoth_ivory": "Mammoth Ivory", "mammoth_wool": "Mammoth Wool", "mammoth_fat": "Mammoth Fat",
    "rhino_horn": "Rhino Horn", "qiviut": "Qiviut",
}
for item, name in ITEMS.items():
    flat_item(item)
    lang[f"item.mechanical.{item}"] = name
for item, name in {"ivory_knife": "Ivory Knife", "rhino_horn_club": "Rhino Horn Club"}.items():
    flat_item(item, "minecraft:item/handheld")
    lang[f"item.mechanical.{item}"] = name
tag_add("knives", M + "ivory_knife")
w(D / "mechanical/tags/item/ivory_tool_materials.json", {"values": [M + "mammoth_ivory"]})
w(D / "mechanical/tags/item/rhino_horn_tool_materials.json", {"values": [M + "rhino_horn"]})

# ---- pegged raw hides: the raw hide item places the block; it cures in the sun (PeggedHideBlock) ----
HIDES = {"mammoth": ("Raw Mammoth Hide", 4), "rhino": ("Raw Rhino Hide", 3), "bison": ("Raw Bison Hide", 3), "musk_ox": ("Raw Musk Ox Hide", 2)}
FACING_Y = {"north": 0, "east": 90, "south": 180, "west": 270}
FACES = ["down", "up", "north", "south", "east", "west"]


def peg(x, z):
    """A whittled wooden peg through the hide's corner."""
    return {"from": [x, 0, z], "to": [x + 1, 2, z + 1], "faces": {f: {"texture": "#peg", "uv": [7, 0, 8, 2] if f not in ("up", "down") else [7, 7, 8, 8]}
                                                              for f in FACES}}


for kind, (name, leather) in HIDES.items():
    block = f"raw_{kind}_hide"
    lang[f"item.mechanical.{block}"] = name
    lang[f"block.mechanical.{block}"] = f"Pegged {name.removeprefix('Raw ')}"
    flat_item(block)
    for cured in (False, True):
        tex = f"mechanical:block/{block}" + ("_cured" if cured else "")
        w(A / f"models/block/{block}{'_cured' if cured else ''}.json", {
            "textures": {"particle": tex, "hide": tex, "peg": "minecraft:block/stripped_spruce_log"},
            "elements": [{"from": [0, 0, 0], "to": [16, 0.5, 16], "faces": {
                "up": {"texture": "#hide", "uv": [0, 0, 16, 16]}, "down": {"texture": "#hide", "uv": [0, 0, 16, 16], "cullface": "down"}}},
                peg(1, 1), peg(14, 1), peg(1, 14), peg(14, 14)]})
    w(A / f"blockstates/{block}.json", {"variants": {
        f"cured={str(c).lower()},facing={f}": {"model": f"mechanical:block/{block}{'_cured' if c else ''}", **({"y": y} if y else {})}
        for c in (False, True) for f, y in FACING_Y.items()}})
    w(D / f"mechanical/loot_table/blocks/{block}.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{
        "type": "minecraft:alternatives", "children": [
            {"type": "minecraft:item", "name": M + f"{kind}_hide",
             "condition": {"type": "minecraft:match_block", "blocks": M + block, "state": {"cured": "true"}}},
            {"type": "minecraft:item", "name": M + block}]}]}], "random_sequence": f"mechanical:blocks/{block}"})
    # a cured hide cut into leather with a knife (it stays in the grid)
    shapeless(f"leather_from_{kind}_hide", [M + f"{kind}_hide", "#mechanical:knives"], "minecraft:leather", leather)

# ---- rugs (the Bear Rug's flat model and block) ----
RUGS = {"mammoth": ("Mammoth Rug", M + "mammoth_wool"), "bison": ("Bison Rug", M + "horn"), "musk_ox": ("Musk Ox Rug", M + "qiviut")}
for kind, (name, extra) in RUGS.items():
    rug = f"{kind}_rug"
    lang[f"block.mechanical.{rug}"] = name
    w(A / f"models/block/{rug}.json", {"parent": "mechanical:block/bear_rug",
                                        "textures": {"particle": f"mechanical:block/{rug}", "top": f"mechanical:block/{rug}"}})
    w(A / f"blockstates/{rug}.json", {"variants": {f"facing={f}": {"model": f"mechanical:block/{rug}", **({"y": y} if y else {})}
                                                   for f, y in FACING_Y.items()}})
    flat_item(rug)
    w(D / f"mechanical/loot_table/blocks/{rug}.json", {"type": "minecraft:block", "pools": [{"rolls": 1,
        "entries": [{"type": "minecraft:item", "name": M + rug}], "condition": {"type": "minecraft:survives_explosion"}}],
        "random_sequence": f"mechanical:blocks/{rug}"})
    shapeless(rug, [M + f"{kind}_hide", extra], M + rug, category="building")

# ---- Ivory Figurine: a little carved mammoth (body, head, trunk, tusks, legs) ----
lang["block.mechanical.ivory_figurine"] = "Ivory Figurine"


def cube(f, t, rot=None):
    e = {"from": f, "to": t, "faces": {d: {"texture": "#ivory"} for d in FACES}}
    if rot:
        e["rotation"] = rot
    return e


figure = [cube([5, 2, 5], [11, 6, 12]),                      # body
          cube([6, 5, 5.5], [10, 7, 9]),                      # shoulder hump
          cube([6, 3.5, 3], [10, 7.5, 5.5]),                  # head (facing north, -z)
          cube([7.25, 0.5, 2.25], [8.75, 4, 3.25], {"origin": [8, 4, 3], "axis": "x", "angle": 22.5}),  # trunk
          cube([5.75, 2, 2], [6.75, 3, 4.5], {"origin": [6, 3, 4], "axis": "x", "angle": -22.5}),       # tusks
          cube([9.25, 2, 2], [10.25, 3, 4.5], {"origin": [10, 3, 4], "axis": "x", "angle": -22.5}),
          cube([5.5, 0, 5.5], [7, 2, 7]), cube([9, 0, 5.5], [10.5, 2, 7]),       # legs
          cube([5.5, 0, 10], [7, 2, 11.5]), cube([9, 0, 10], [10.5, 2, 11.5]),
          cube([7.5, 3, 12], [8.5, 5, 12.5])]                 # tail
w(A / "models/block/ivory_figurine.json", {"parent": "minecraft:block/block", "textures": {
    "particle": "mechanical:block/ivory_figurine", "ivory": "mechanical:block/ivory_figurine"}, "elements": figure})
w(A / "blockstates/ivory_figurine.json", {"variants": {f"facing={f}": {"model": "mechanical:block/ivory_figurine", **({"y": y} if y else {})}
                                                       for f, y in FACING_Y.items()}})
w(A / "items/ivory_figurine.json", {"model": {"type": "minecraft:model", "model": "mechanical:block/ivory_figurine"}})
w(D / "mechanical/loot_table/blocks/ivory_figurine.json", {"type": "minecraft:block", "pools": [{"rolls": 1,
    "entries": [{"type": "minecraft:item", "name": M + "ivory_figurine"}], "condition": {"type": "minecraft:survives_explosion"}}],
    "random_sequence": "mechanical:blocks/ivory_figurine"})

# ---- recipes for the parts ----
recipe("tallow_from_mammoth_fat", {"type": "minecraft:campfire_cooking", "category": "misc", "cookingtime": 400, "experience": 0.1,
                                   "ingredient": M + "mammoth_fat", "result": {"id": M + "tallow", "count": 3}})
shaped("ivory_knife", ["I", "C", "L"], {"I": M + "mammoth_ivory", "C": M + "cordage", "L": "minecraft:leather"}, M + "ivory_knife",
       category="equipment")
shaped("rhino_horn_club", ["H", "C", "R"], {"H": M + "rhino_horn", "C": M + "cordage", "R": M + "tool_rod"}, M + "rhino_horn_club",
       category="equipment")
shaped("shield_from_rhino_hide", ["PHP", "PPP", " P "], {"P": "#minecraft:planks", "H": M + "rhino_hide"}, "minecraft:shield",
       category="equipment")
shapeless("ivory_figurine", [M + "mammoth_ivory", "#mechanical:knives"], M + "ivory_figurine", category="building")
shapeless("bone_needle_from_rhino_horn", [M + "rhino_horn", "#mechanical:knives"], M + "bone_needle", 2, category="equipment")
shapeless("twine_from_mammoth_wool", [M + "mammoth_wool"], M + "twine", 2)
shaped("brown_wool_from_mammoth_wool", ["WW", "WW"], {"W": M + "mammoth_wool"}, "minecraft:brown_wool", category="building")
shapeless("wool_thread_from_qiviut", [M + "qiviut"], M + "wool_thread", 2)
shaped("light_gray_wool_from_qiviut", ["QQ", "QQ"], {"Q": M + "qiviut"}, "minecraft:light_gray_wool", category="building")

# ---- spawns: (biomes, weight, min, max); weights against the biome's other creatures ----
SPAWNS = {
    # a rare herd on the cold steppe, where the Mammoth Bone Huts stand
    "woolly_mammoth": (["minecraft:snowy_plains", "minecraft:snowy_taiga", "minecraft:grove", "minecraft:ice_spikes"], 1, 2, 4),
    "woolly_rhino": (["minecraft:snowy_plains", "minecraft:windswept_hills", "minecraft:windswept_gravelly_hills",
                      "minecraft:snowy_taiga", "minecraft:snowy_slopes"], 2, 1, 2),
    "steppe_bison": (["minecraft:plains", "minecraft:sunflower_plains", "minecraft:savanna", "minecraft:savanna_plateau",
                      "minecraft:snowy_plains", "minecraft:meadow", "mechanical:hazel_meadow", "mechanical:pine_heath"], 6, 4, 8),
    "musk_ox": (["minecraft:snowy_plains", "minecraft:snowy_slopes", "minecraft:grove", "minecraft:ice_spikes",
                 "minecraft:frozen_peaks", "minecraft:snowy_taiga"], 5, 4, 8),
}
for e, (biomes, weight, lo, hi) in SPAWNS.items():
    w(D / f"mechanical/neoforge/biome_modifier/add_{e}.json", {"type": "neoforge:add_spawns", "biomes": biomes, "spawners": [{
        "type": M + e, "weight": weight, "count": {"type": "minecraft:uniform", "min_inclusive": lo, "max_inclusive": hi}}]})

merge(A / "lang/en_us.json", lang)
print("fauna giants ok")
