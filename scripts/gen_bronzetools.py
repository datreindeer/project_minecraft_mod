"""More bronze tools (BronzeTools.java, BronzeBucketItem.java; docs/ages/02-bronze-age.md "Fler bronsverktyg").

Every tool is made like the other bronze tools: an unfired clay mold shaped at the Mold Bench (MoldBench.ALL) and fired in
the kiln, bronze poured in, the Rough Cast piece ground on the Whetstone, then assembled at the Tool Station
(ToolStationRecipes) except the Bronze Shears and Bronze Needles, which come off the Whetstone ready.
- molds (bronze only): bucket, sickle blade, shears, fish hooks (gang: 4), needles (gang: 4), tongs, chisel blade: block
  models (as gen_copper.py's), blockstates, item models, loot (keeps uses), lang
- items: rough casts, ground pieces, Bronze Bucket / Water / Lava / Milk Bucket, Bronze Sickle, Bronze Shears, Bronze Hook
  Fishing Rod (cast / uncast models as vanilla), Bronze Needle, Bronze Tongs, Bronze Chisel
- tags: mechanical:sickles, mechanical:needles, c:tools/shear, c:tools/fishing_rod, enchantable fishing / durability
- damage type mechanical:hot_metal (molten metal handled without tongs; fire resistance helps: minecraft:is_fire)
- shears: the vanilla tool/can_shear predicate (leaves, grass, vines, cobwebs ...) also takes the Bronze Shears
- wherever the data already takes the Clay Water Bucket, the Milk Bucket, the Flint Sickle or vanilla shears (recipes, loot
  tool checks), the bronze tool is added next to it, so later recipes pick it up by themselves
Textures: scripts/gen_bronzetools_textures.py.
Generator order: ... gen_woodmeat.py, gen_thread.py, gen_bronzetools.py, gen_block_items.py, gen_unlocks.py,
gen_advancements.py, gen_registry.py. Run: python scripts/gen_bronzetools.py
"""
import json
from pathlib import Path

R = Path(__file__).resolve().parent.parent / "src/main/resources"
A, D = R / "assets/mechanical", R / "data"
M = "mechanical:"
B = M + "block/"
title = lambda s: s.replace("_", " ").title()


def w(path, obj):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, indent=2) + "\n", encoding="utf-8")


def patch_tag(path, values):
    tag = json.loads(path.read_text(encoding="utf-8")) if path.exists() else {"values": []}
    tag["values"] += [v for v in values if v not in tag["values"]]
    w(path, tag)


def item_model(name, parent="minecraft:item/generated", tex=None):
    w(A / f"models/item/{name}.json", {"parent": parent, "textures": {"layer0": M + f"item/{tex or name}"}})


def item_def(name, model=None):
    w(A / f"items/{name}.json", {"model": {"type": "minecraft:model", "model": M + f"item/{model or name}"}})


# ---------------------------------------------------------------- molds (bronze only, as gen_kettle.py's)

SHAPES = ["bucket", "sickle_blade", "shears", "fish_hooks", "needles", "tongs", "chisel_blade"]
for s in SHAPES:
    base = {"particle": B + "kiln_clay_fired", "side": B + "kiln_clay_fired", "top": B + f"clay_{s}_mold_top"}
    slab = {"from": [1, 0, 1], "to": [15, 3, 15], "faces": {
        "up": {"texture": "#top", "uv": [1, 1, 15, 15]}, "down": {"texture": "#side", "cullface": "down"},
        **{d: {"texture": "#side", "uv": [1, 13, 15, 16]} for d in ("north", "south", "east", "west")}}}
    w(A / f"models/block/clay_{s}_mold.json", {"parent": "minecraft:block/block", "textures": base, "elements": [slab]})
    for fill in ("molten", "solid"):
        metal = {"from": [1, 3.01, 1], "to": [15, 3.01, 15], "shade": fill == "solid", **({"light_emission": 12} if fill == "molten" else {}),
                 "faces": {"up": {"texture": "#metal", "uv": [1, 1, 15, 15]}}}
        w(A / f"models/block/clay_{s}_mold_{fill}_bronze.json", {"parent": "minecraft:block/block",
            "textures": {**base, "metal": B + f"clay_{s}_mold_bronze_{fill}"}, "elements": [slab, metal]})
    w(A / f"blockstates/clay_{s}_mold.json", {"variants": {
        f"fill={fill},metal={m},uses={u}": {"model": B + f"clay_{s}_mold" + ("" if fill == "empty" else f"_{fill}_bronze")}
        for fill in ("empty", "molten", "solid") for m in ("copper", "bronze", "poor_bronze") for u in range(5)}})
    w(A / f"items/clay_{s}_mold.json", {"model": {"type": "minecraft:model", "model": B + f"clay_{s}_mold"}})
    w(D / f"mechanical/loot_table/blocks/clay_{s}_mold.json", {"type": "minecraft:block", "pools": [{"rolls": 1,
        "condition": {"type": "minecraft:survives_explosion"},
        "entries": [{"type": "minecraft:item", "name": M + f"clay_{s}_mold",
                     "modifier": [{"type": "minecraft:copy_state", "block": M + f"clay_{s}_mold", "properties": ["uses"]}]}]}],
        "random_sequence": f"mechanical:blocks/clay_{s}_mold"})

# ---------------------------------------------------------------- items

GROUND = ["bucket_body", "sickle_blade", "fish_hook", "tong_arms", "chisel_blade"]
FLAT = ([f"unfired_clay_{s}_mold" for s in SHAPES] + [f"rough_cast_bronze_{s}" for s in SHAPES] + [f"bronze_{g}" for g in GROUND]
        + ["bronze_bucket", "bronze_water_bucket", "bronze_lava_bucket", "bronze_milk_bucket", "bronze_needle", "bronze_shears"])
for name in FLAT:
    item_model(name)
    item_def(name)
for name in ("bronze_sickle", "bronze_tongs", "bronze_chisel"):
    item_model(name, "minecraft:item/handheld")
    item_def(name)
rod = "bronze_hook_fishing_rod"
item_model(rod, "minecraft:item/handheld_rod")
item_model(rod + "_cast", "minecraft:item/handheld_rod")
w(A / f"items/{rod}.json", {"model": {"type": "minecraft:condition", "property": "minecraft:fishing_rod/cast",
    "on_false": {"type": "minecraft:model", "model": M + f"item/{rod}"},
    "on_true": {"type": "minecraft:model", "model": M + f"item/{rod}_cast"}}})

# ---------------------------------------------------------------- tags, damage type, shears predicate

patch_tag(D / "mechanical/tags/item/sickles.json", [M + "bronze_sickle"])
w(D / "mechanical/tags/item/needles.json", {"values": [M + "bone_needle", M + "bronze_needle"]})
patch_tag(D / "c/tags/item/tools/shear.json", [M + "bronze_shears"])
patch_tag(D / "c/tags/item/tools/fishing_rod.json", [M + rod])
patch_tag(D / "minecraft/tags/item/enchantable/fishing.json", [M + rod])
patch_tag(D / "minecraft/tags/item/enchantable/durability.json", [M + rod, M + "bronze_shears"])

w(D / "mechanical/damage_type/hot_metal.json", {"exhaustion": 0.1, "message_id": "mechanical.hot_metal", "scaling": "never"})
patch_tag(D / "minecraft/tags/damage_type/is_fire.json", [M + "hot_metal"])

# vanilla leaves / grass / vines / cobweb / dead bush loot asks this predicate whether the tool shears
w(D / "minecraft/predicate/tool/can_shear.json", {"type": "minecraft:match_tool", "predicate": {"items": ["minecraft:shears", M + "bronze_shears"]}})

# ---------------------------------------------------------------- the bronze tool wherever its older kin is accepted

ALSO = {M + "clay_water_bucket": M + "bronze_water_bucket", "minecraft:milk_bucket": M + "bronze_milk_bucket",
        M + "flint_sickle": M + "bronze_sickle", "minecraft:shears": M + "bronze_shears"}


def widen(node, key_ok):
    """Adds each ALSO partner next to its item in ingredient alternatives / match_tool item lists; returns the count."""
    n = 0
    if isinstance(node, dict):
        for k, v in list(node.items()):
            if key_ok(k) and isinstance(v, str) and v in ALSO:
                node[k] = [v, ALSO[v]]
                n += 1
            elif key_ok(k) and isinstance(v, list) and all(isinstance(x, str) for x in v):
                for old, new in ALSO.items():
                    if old in v and new not in v:
                        v.insert(v.index(old) + 1, new)
                        n += 1
            else:
                n += widen(v, key_ok)
    elif isinstance(node, list):
        for i, v in enumerate(node):
            if isinstance(v, str) and v in ALSO:  # a bare shapeless ingredient: make it a choice
                node[i] = [v, ALSO[v]]
                n += 1
            elif isinstance(v, list) and all(isinstance(x, str) for x in v):
                for old, new in ALSO.items():
                    if old in v and new not in v:
                        v.insert(v.index(old) + 1, new)
                        n += 1
            else:
                n += widen(v, key_ok)
    return n


widened = 0
for f in sorted(D.glob("*/recipe/**/*.json")):
    r = json.loads(f.read_text(encoding="utf-8"))
    if "type" not in r or not any(k in r for k in ("key", "ingredients")):
        continue
    n = widen({"key": r.get("key", {}), "ingredients": r.get("ingredients", [])}, lambda k: True)
    if n:
        w(f, r)
        widened += n
for f in sorted(D.glob("*/loot_table/**/*.json")):
    text = f.read_text(encoding="utf-8")
    if not any(f'"{k}"' in text for k in (M + "flint_sickle", "minecraft:shears")):
        continue
    t = json.loads(text)
    if widen(t, lambda k: k == "items"):  # only match_tool predicates (an "items" field), never what drops
        w(f, t)
        widened += 1
assert M + "bronze_water_bucket" in (D / "mechanical/recipe/dough.json").read_text(encoding="utf-8")  # re-runs widen nothing

# the Armor Station itself is sewn with any needle
p = D / "mechanical/recipe/armor_station.json"
r = json.loads(p.read_text(encoding="utf-8"))
r["key"]["N"] = "#mechanical:needles"
w(p, r)

# ---------------------------------------------------------------- lang

lang_path = A / "lang/en_us.json"
lang = json.loads(lang_path.read_text(encoding="utf-8"))
lang.update({f"item.mechanical.unfired_clay_{s}_mold": f"Unfired Clay {title(s)} Mold" for s in SHAPES})
lang.update({f"block.mechanical.clay_{s}_mold": f"Clay {title(s)} Mold" for s in SHAPES})
lang.update({f"item.mechanical.rough_cast_bronze_{s}": f"Rough Cast Bronze {title(s)}" for s in SHAPES})
lang.update({f"item.mechanical.bronze_{g}": f"Bronze {title(g)}" for g in GROUND})
lang.update({
    "item.mechanical.bronze_bucket": "Bronze Bucket", "item.mechanical.bronze_water_bucket": "Bronze Water Bucket",
    "item.mechanical.bronze_lava_bucket": "Bronze Lava Bucket", "item.mechanical.bronze_milk_bucket": "Bronze Milk Bucket",
    "item.mechanical.bronze_sickle": "Bronze Sickle", "item.mechanical.bronze_shears": "Bronze Shears",
    "item.mechanical.bronze_hook_fishing_rod": "Bronze Hook Fishing Rod", "item.mechanical.bronze_needle": "Bronze Needle",
    "item.mechanical.bronze_tongs": "Bronze Tongs", "item.mechanical.bronze_chisel": "Bronze Chisel",
    "tooltip.mechanical.poor_bronze_bucket": "Poor Bronze: too brittle to carry lava",
    "message.mechanical.bronze_bucket.poor_lava": "Poor Bronze is too brittle to carry lava",
    "message.mechanical.hot_metal.crucible": "The crucible burns your hand: hold Bronze Tongs",
    "message.mechanical.hot_metal.casting": "The casting is hot: take it out with Bronze Tongs",
    "death.attack.mechanical.hot_metal": "%1$s was burnt by molten metal",
    "death.attack.mechanical.hot_metal.player": "%1$s was burnt by molten metal while fighting %2$s",
})
w(lang_path, lang)
print(f"bronze tools ok ({widened} recipe/loot spots widened)")
