# Bronze Age phase 3b: bronze. Bronze Ingot, Rough Cast / ground bronze pieces, bronze tools, Bronze Spear / Sword, Bronze
# Saw (+ sawn planks), Bronze Helmet / Greaves (+ equipment asset), Copper / Tin Concentrate, the crucibles' bronze look,
# tool tier tags, villager trades levels 3-4, lang. The bronze-only Clay Molds (saw blade, plate, spear head, sword blade)
# are written with the other molds in gen_copper.py. Textures: gen_bronze_textures.py, gen_crucible_textures.py.
# usage: python scripts/gen_bronze.py   (right after gen_copper.py, before gen_block_items.py)
import json
from pathlib import Path

R = Path(__file__).resolve().parent.parent / "src/main/resources"
A, D = R / "assets/mechanical", R / "data"
M = "mechanical:"


def w(path, obj):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, indent=2) + "\n")


def patch_tag(path, values):
    tag = json.loads(path.read_text()) if path.exists() else {"values": []}
    for v in values:
        if v not in tag["values"]:
            tag["values"].append(v)
    w(path, tag)


PIECES = ["pickaxe_head", "axe_head", "shovel_head", "hoe_head", "knife_blade", "saw_blade", "plate", "spear_head", "sword_blade"]
TOOLS = ["pickaxe", "axe", "shovel", "hoe", "knife", "saw", "sword"]
title = lambda s: s.replace("_", " ").title()

# ---- item models ----
FLAT = ["bronze_ingot", "bronze_helmet", "bronze_greaves", "copper_concentrate", "tin_concentrate"] + [f"rough_cast_bronze_{p}" for p in PIECES] + [f"bronze_{p}" for p in PIECES]
for item in FLAT:
    w(A / f"models/item/{item}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"mechanical:item/{item}"}})
    w(A / f"items/{item}.json", {"model": {"type": "minecraft:model", "model": f"mechanical:item/{item}"}})
for t in TOOLS:
    w(A / f"models/item/bronze_{t}.json", {"parent": "minecraft:item/handheld", "textures": {"layer0": f"mechanical:item/bronze_{t}"}})
    w(A / f"items/bronze_{t}.json", {"model": {"type": "minecraft:model", "model": f"mechanical:item/bronze_{t}"}})

# the spear as vanilla's: flat icon in the GUI / on the ground, the long in-hand model otherwise (as the Flint Spear)
w(A / "models/item/bronze_spear.json", {"parent": "minecraft:item/generated", "textures": {"layer0": "mechanical:item/bronze_spear"}})
w(A / "models/item/bronze_spear_in_hand.json", {"parent": "minecraft:item/spear_in_hand", "textures": {"layer0": "mechanical:item/bronze_spear_in_hand"}})
w(A / "items/bronze_spear.json", {"model": {"type": "minecraft:select", "property": "minecraft:display_context",
    "cases": [{"when": ["gui", "ground", "fixed", "on_shelf"], "model": {"type": "minecraft:model", "model": "mechanical:item/bronze_spear"}}],
    "fallback": {"type": "minecraft:model", "model": "mechanical:item/bronze_spear_in_hand"}}, "swap_animation_scale": 1.95})

# crucibles show the metal they hold: bronze (and Poor Bronze) via the mechanical:bronze_crucible condition, else copper
for c in ("molten_crucible", "solid_crucible"):
    w(A / f"models/item/{c}_bronze.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"mechanical:item/{c}_bronze"}})
    w(A / f"items/{c}.json", {"model": {"type": "minecraft:condition", "property": "mechanical:bronze_crucible",
        "on_true": {"type": "minecraft:model", "model": f"mechanical:item/{c}_bronze"},
        "on_false": {"type": "minecraft:model", "model": f"mechanical:item/{c}"}}})

# worn armour: helmet on the humanoid layer, greaves on the leggings layer (textures/entity/equipment/...)
w(A / "equipment/bronze.json", {"layers": {"humanoid": [{"texture": "mechanical:bronze"}], "humanoid_leggings": [{"texture": "mechanical:bronze"}]}})

# ---- tags ----
# the bronze tier mines like stone plus #mechanical:needs_bronze_tool (iron, coal, lapis) and #mechanical:needs_copper_tool (tin)
w(D / "mechanical/tags/block/incorrect_for_bronze_tool.json", {"values": ["#minecraft:needs_iron_tool", "#minecraft:needs_diamond_tool"]})
w(D / "mechanical/tags/item/bronze_tool_materials.json", {"values": [M + "bronze_ingot"]})
for tag, items in [("pickaxes", ["bronze_pickaxe"]), ("axes", ["bronze_axe"]), ("shovels", ["bronze_shovel"]), ("hoes", ["bronze_hoe"]),
                   ("swords", ["bronze_knife", "bronze_sword"]), ("spears", ["bronze_spear"]), ("head_armor", ["bronze_helmet"]), ("leg_armor", ["bronze_greaves"])]:
    patch_tag(D / f"minecraft/tags/item/{tag}.json", [M + i for i in items])
patch_tag(D / "mechanical/tags/item/knives.json", [M + "bronze_knife"])
patch_tag(D / "mechanical/tags/item/hammerstones.json", [M + "bronze_pickaxe"])

# ---- sawn planks: a log and the Bronze Saw (it stays, losing 1) give 4 planks; without a saw a log still gives 2 ----
made = 0
for f in sorted(D.glob("*/recipe/*_planks.json")):
    r = json.loads(f.read_text())
    if r.get("group") != "planks" or len(r.get("ingredients", [])) != 1:
        continue
    wood = f.stem[: -len("_planks")]
    w(D / f"mechanical/recipe/sawn_{wood}_planks.json", {"type": "minecraft:crafting_shapeless", "category": "building", "group": "sawn_planks",
        "ingredients": [r["ingredients"][0], M + "bronze_saw"], "result": {"id": r["result"]["id"], "count": 4}})
    made += 1
assert made >= 20, made

# ---- villages: levels 3-4 are the Bronze Age (levels 1-2, stone age: gen_data.py; level 5 stays empty for later ages).
# No iron, gold or diamond goods. Rough emerald worth: a bronze tool 5-10, a bronze ingot ~1/3, 6 crushed tin 1. ----
E = "minecraft:emerald"
V = "minecraft:"
TRADES = {  # profession: {level: [(wants, wants_count, gives, gives_count, max_uses, xp)]}
    "armorer": {3: [(M + "bronze_ingot", 3, E, 1, 12, 20), (E, 9, M + "bronze_helmet", 1, 3, 10)],
                4: [(V + "copper_ingot", 6, E, 1, 12, 30), (E, 12, M + "bronze_greaves", 1, 3, 15)]},
    "butcher": {3: [(M + "tallow", 8, E, 1, 16, 20), (E, 1, M + "smoked_meat", 3, 16, 10)],
                4: [(M + "bear_fat", 3, E, 1, 12, 30), (E, 2, M + "jerky", 4, 12, 15)]},
    "cartographer": {3: [(M + "black_pigment", 8, E, 1, 16, 20), (E, 2, V + "item_frame", 1, 12, 10)],
                     4: [(M + "yellow_pigment", 8, E, 1, 16, 30), (E, 3, V + "painting", 1, 12, 15)]},
    "cleric": {3: [(M + "white_pigment", 6, E, 1, 16, 20), (E, 1, M + "red_pigment", 4, 12, 10)],
               4: [(V + "honeycomb", 6, E, 1, 12, 30), (E, 1, M + "dried_mushrooms", 4, 12, 15)]},
    "farmer": {3: [(M + "barley_grain", 20, E, 1, 16, 20), (E, 1, M + "flour", 4, 16, 10)],
               4: [(M + "emmer_grain", 20, E, 1, 16, 30), (E, 2, M + "dried_berries", 6, 12, 15)]},
    "fisherman": {3: [(M + "whole_tropical_fish", 4, E, 1, 16, 20), (E, 2, M + "smoked_fish", 4, 16, 10)],
                  4: [(M + "whole_pufferfish", 3, E, 1, 12, 30), (E, 2, M + "dried_fish", 4, 12, 15)]},
    "fletcher": {3: [(V + "feather", 24, E, 1, 16, 20), (E, 2, M + "bone_arrow", 8, 12, 10)],
                 4: [(M + "sinew", 8, E, 1, 16, 30), (E, 6, M + "sinew_bow", 1, 6, 15)]},
    "leatherworker": {3: [(M + "scraped_hide", 4, E, 1, 16, 20), (E, 6, M + "leather_satchel", 1, 6, 10)],
                      4: [(M + "wolf_pelt", 2, E, 1, 12, 30), (E, 5, V + "leather_leggings", 1, 6, 15)]},
    "librarian": {3: [(V + "book", 4, E, 1, 12, 20), (E, 6, V + "bookshelf", 1, 12, 10)],
                  4: [(V + "writable_book", 2, E, 1, 12, 30), (E, 8, V + "name_tag", 1, 6, 15)]},
    "mason": {3: [(M + "andesite_chunk", 20, E, 1, 16, 20), (E, 1, V + "bricks", 4, 16, 10)],
              4: [(V + "terracotta", 12, E, 1, 12, 30), (E, 2, M + "clay_bucket", 1, 8, 15)]},
    "shepherd": {3: [(V + "red_dye", 12, E, 1, 16, 20), (E, 3, V + "white_bed", 1, 8, 10)],
                 4: [(V + "yellow_dye", 12, E, 1, 16, 30), (E, 1, V + "red_wool", 3, 12, 15)]},
    "toolsmith": {3: [(M + "crushed_tin_ore", 6, E, 1, 12, 20), (E, 7, M + "bronze_pickaxe", 1, 3, 10), (E, 5, M + "bronze_saw", 1, 3, 10)],
                  4: [(M + "copper_concentrate", 8, E, 1, 12, 30), (E, 3, M + "tin_concentrate", 2, 8, 15), (E, 6, M + "bronze_axe", 1, 3, 15)]},
    "weaponsmith": {3: [(M + "bronze_ingot", 3, E, 1, 12, 20), (E, 5, M + "bronze_knife", 1, 3, 10)],
                    4: [(M + "crushed_copper_ore", 12, E, 1, 16, 30), (E, 8, M + "bronze_spear", 1, 3, 15), (E, 9, M + "bronze_sword", 1, 3, 15)]},
}
stack = lambda i, n: {"id": i, "count": n} if n > 1 else {"id": i}
lang_now = json.loads((A / "lang/en_us.json").read_text())
for prof, levels in TRADES.items():
    for lvl, trades in levels.items():
        ids = []
        for i, (wi, wn, gi, gn, uses, xp) in enumerate(trades):
            for item in (wi, gi):  # every traded item must exist (a typo makes the trade fail to load silently)
                ns, path = item.split(":")
                assert ns == "minecraft" or f"item.mechanical.{path}" in lang_now or f"block.mechanical.{path}" in lang_now                     or path in ("copper_concentrate", "tin_concentrate", "bronze_spear", "bronze_sword"), item
            name = f"{prof}/{lvl}/bronze_{i}"
            w(D / f"mechanical/villager_trade/{name}.json", {"wants": stack(wi, wn), "gives": stack(gi, gn), "max_uses": uses, "xp": xp,
                                                              "reputation_discount": 0.05})
            ids.append(M + name)
        w(D / f"minecraft/tags/villager_trade/{prof}/level_{lvl}.json", {"replace": True, "values": ids})

# ---- lang ----
lang_path = A / "lang/en_us.json"
lang = json.loads(lang_path.read_text())
lang.update({f"item.mechanical.rough_cast_bronze_{p}": f"Rough Cast Bronze {title(p)}" for p in PIECES})
lang.update({f"item.mechanical.bronze_{p}": f"Bronze {title(p)}" for p in PIECES})
lang.update({f"item.mechanical.bronze_{t}": f"Bronze {title(t)}" for t in TOOLS})
lang.update({
    "item.mechanical.bronze_spear": "Bronze Spear",
    "item.mechanical.copper_concentrate": "Copper Concentrate", "item.mechanical.tin_concentrate": "Tin Concentrate",
    "tooltip.mechanical.crucible.empty": "Empty: click Crushed Copper / Tin Ore, Concentrate or Native Copper onto it in your inventory",
    "item.mechanical.bronze_ingot": "Bronze Ingot", "item.mechanical.bronze_helmet": "Bronze Helmet", "item.mechanical.bronze_greaves": "Bronze Greaves",
    "metal.mechanical.bronze": "Bronze", "metal.mechanical.poor_bronze": "Poor Bronze", "metal.mechanical.tin": "Tin",
    "tooltip.mechanical.poor_bronze": "Poor Bronze: 70% durability",
    "message.mechanical.crucible.too_hot": "Let it cool before you knock it out",
    "tooltip.mechanical.crucible.mix": "%s%% tin: %s",
    "tooltip.mechanical.crucible.slag": "%s%% tin: no bronze (8-15%% makes bronze, about 10%% the best). Melts as copper; the tin is lost as slag",
    "tooltip.mechanical.crucible.tin_only": "Tin alone casts nothing: add copper (about 9 copper to 1 tin)",
    "message.mechanical.mold.wrong_metal": "%s can't be cast in this mold",
})
w(lang_path, lang)
print(f"bronze ok ({made} sawn planks recipes)")
