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
    tag = json.loads(path.read_text(encoding="utf-8")) if path.exists() else {"values": []}
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
# coins grow with the stack like firewood: 1-6, 7-16, 17-32, 33-46, 47-64 (textures: gen_coin_textures.py)
COIN_STAGES = [7, 17, 33, 47]
for coin in ("copper_coin", "silver_coin", "gold_coin"):
    for n in range(1, 6):
        w(A / f"models/item/{coin}_{n}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"mechanical:item/{coin}_{n}"}})
    w(A / f"items/{coin}.json", {"model": {"type": "minecraft:range_dispatch", "property": "minecraft:count", "normalize": False,
        "entries": [{"threshold": t, "model": {"type": "minecraft:model", "model": f"mechanical:item/{coin}_{n}"}} for n, t in enumerate(COIN_STAGES, 2)],
        "fallback": {"type": "minecraft:model", "model": f"mechanical:item/{coin}_1"}}})
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

# ---- sawn planks: a log and a saw (it stays, losing 1) give 4 planks; without a saw a log still gives 2 ----
# #mechanical:saws: every SawItem (the Iron and Steel Saws are forged in the Iron Age, Smithing.java)
w(D / "mechanical/tags/item/saws.json", {"values": [M + "bronze_saw", M + "iron_saw", M + "steel_saw"]})
made = 0
for f in sorted(D.glob("*/recipe/*_planks.json")):
    r = json.loads(f.read_text(encoding="utf-8"))
    if r.get("group") != "planks" or len(r.get("ingredients", [])) != 1:
        continue
    wood = f.stem[: -len("_planks")]
    w(D / f"mechanical/recipe/sawn_{wood}_planks.json", {"type": "minecraft:crafting_shapeless", "category": "building", "group": "sawn_planks",
        "ingredients": [r["ingredients"][0], "#mechanical:saws"], "result": {"id": r["result"]["id"], "count": 4}})
    made += 1
assert made >= 20, made

# ---- villages: levels 3-4 are the Bronze Age, level 5 (Master) the Iron Age (levels 1-2, stone age: gen_data.py).
# Every trade is goods for coins (docs/traders-plan.md A), priced in copper here: copper for cheap things; 10 copper or more
# is paid in Silver Coins (rounded, see coin() below), so a bronze tool costs 2-5 silver; level 5's valuable goods cost Gold
# Coins (G). Coins.java: 1 silver = 10 copper, 1 gold = 10 silver. ----
C = M + "copper_coin"
SILVER, GOLD = M + "silver_coin", M + "gold_coin"
G = "gold"  # a price in gold coins: (G, n)
V = "minecraft:"
TRADES = {  # profession: {level: [(wants, wants_count, gives, gives_count, max_uses, xp)]}
    "armorer": {3: [(M + "bronze_ingot", 3, C, 4, 12, 20), (C, 36, M + "bronze_helmet", 1, 3, 10)],
                4: [(V + "copper_ingot", 6, C, 4, 12, 30), (C, 48, M + "bronze_greaves", 1, 3, 15)]},
    "butcher": {3: [(M + "tallow", 8, C, 4, 16, 20), (C, 4, M + "smoked_meat", 3, 16, 10)],
                4: [(M + "bear_fat", 3, C, 4, 12, 30), (C, 8, M + "jerky", 4, 12, 15)]},
    "cartographer": {3: [(M + "black_pigment", 8, C, 4, 16, 20), (C, 8, V + "item_frame", 1, 12, 10)],
                     4: [(M + "yellow_pigment", 8, C, 4, 16, 30), (C, 12, V + "painting", 1, 12, 15)]},
    "cleric": {3: [(M + "white_pigment", 6, C, 4, 16, 20), (C, 4, M + "red_pigment", 4, 12, 10)],
               4: [(V + "honeycomb", 6, C, 4, 12, 30), (C, 4, M + "dried_mushrooms", 4, 12, 15)]},
    "farmer": {3: [(M + "barley_grain", 20, C, 4, 16, 20), (C, 4, M + "flour", 4, 16, 10)],
               4: [(M + "emmer_grain", 20, C, 4, 16, 30), (C, 8, M + "dried_berries", 6, 12, 15)]},
    "fisherman": {3: [(M + "whole_tropical_fish", 4, C, 4, 16, 20), (C, 8, M + "smoked_fish", 4, 16, 10)],
                  4: [(M + "whole_pufferfish", 3, C, 4, 12, 30), (C, 8, M + "dried_fish", 4, 12, 15)]},
    "fletcher": {3: [(V + "feather", 24, C, 4, 16, 20), (C, 8, M + "bone_arrow", 8, 12, 10)],
                 4: [(M + "sinew", 8, C, 4, 16, 30), (C, 24, M + "sinew_bow", 1, 6, 15)]},
    "leatherworker": {3: [(M + "scraped_hide", 4, C, 4, 16, 20), (C, 24, M + "leather_satchel", 1, 6, 10)],
                      4: [(M + "wolf_pelt", 2, C, 4, 12, 30), (C, 20, V + "leather_leggings", 1, 6, 15)]},
    "librarian": {3: [(V + "book", 4, C, 4, 12, 20), (C, 24, V + "bookshelf", 1, 12, 10)],
                  4: [(V + "writable_book", 2, C, 4, 12, 30), (C, 32, V + "name_tag", 1, 6, 15)]},
    "mason": {3: [(M + "andesite_chunk", 20, C, 4, 16, 20), (C, 4, V + "bricks", 4, 16, 10)],
              4: [(V + "terracotta", 12, C, 4, 12, 30), (C, 8, M + "clay_bucket", 1, 8, 15)]},
    "shepherd": {3: [(V + "red_dye", 12, C, 4, 16, 20), (C, 12, V + "white_bed", 1, 8, 10)],
                 4: [(V + "yellow_dye", 12, C, 4, 16, 30), (C, 4, V + "red_wool", 3, 12, 15)]},
    "toolsmith": {3: [(M + "crushed_tin_ore", 6, C, 4, 12, 20), (C, 28, M + "bronze_pickaxe", 1, 3, 10), (C, 20, M + "bronze_saw", 1, 3, 10)],
                  4: [(M + "copper_concentrate", 8, C, 4, 12, 30), (C, 12, M + "tin_concentrate", 2, 8, 15), (C, 24, M + "bronze_axe", 1, 3, 15)]},
    "weaponsmith": {3: [(M + "bronze_ingot", 3, C, 4, 12, 20), (C, 20, M + "bronze_knife", 1, 3, 10)],
                    4: [(M + "crushed_copper_ore", 12, C, 4, 16, 30), (C, 32, M + "bronze_spear", 1, 3, 15), (C, 36, M + "bronze_sword", 1, 3, 15)],
                    5: [(M + "steel_ingot", 2, C, 40, 12, 30), (G, 2, M + "steel_spear", 1, 3, 30)]},
    # level 5 (Master): Iron Age goods for gold, and they buy iron for silver
    "toolsmith5": {5: [(M + "wrought_iron_ingot", 4, C, 30, 12, 30), (G, 2, M + "steel_pickaxe", 1, 3, 30), (G, 1, M + "steel_saw", 1, 3, 30)]},
    "shepherd5": {5: [(M + "linen_cloth", 4, C, 20, 12, 30), (G, 1, M + "silk_thread", 4, 6, 30)]},
    "cleric5": {5: [(V + "glowstone_dust", 8, C, 20, 12, 30), (G, 1, V + "ender_pearl", 1, 4, 30)]},
    "librarian5": {5: [(V + "writable_book", 4, C, 30, 12, 30), (G, 1, M + "cloth_bed", 1, 3, 30)]},
    "leatherworker5": {5: [(M + "wolf_pelt", 4, C, 30, 12, 30), (G, 1, M + "hide_saddle_blanket", 2, 3, 30)]},
}
# fold the level-5 rows into their professions
for key in [k for k in TRADES if k[-1] == "5"]:
    TRADES[key[:-1]].update(TRADES.pop(key))


def coin(item, n):
    """A coin side of a trade: under 10 copper stays copper, more is silver (rounded); (G, n) is gold."""
    if item == G:
        return GOLD, n
    if item == C and n >= 10:
        return SILVER, max(1, round(n / 10))
    return item, n


for prof, levels in TRADES.items():  # goods for coins, never goods for goods
    for t in sum(levels.values(), []):
        assert (t[0] in (C, G)) != (t[2] in (C, G)), (prof, t)
stack = lambda i, n: {"id": i, "count": n} if n > 1 else {"id": i}
lang_now = json.loads((A / "lang/en_us.json").read_text(encoding="utf-8"))
for prof, levels in TRADES.items():
    for lvl, trades in levels.items():
        ids = []
        for i, (wi, wn, gi, gn, uses, xp) in enumerate(trades):
            (wi, wn), (gi, gn) = coin(wi, wn), coin(gi, gn)
            for item in (wi, gi):  # every traded item must exist (a typo makes the trade fail to load silently)
                ns, path = item.split(":")
                assert ns == "minecraft" or f"item.mechanical.{path}" in lang_now or f"block.mechanical.{path}" in lang_now                     or path in ("copper_concentrate", "tin_concentrate", "bronze_spear", "bronze_sword", "copper_coin", "silver_coin", "gold_coin"), item
            name = f"{prof}/{lvl}/{'iron' if lvl == 5 else 'bronze'}_{i}"
            w(D / f"mechanical/villager_trade/{name}.json", {"wants": stack(wi, wn), "gives": stack(gi, gn), "max_uses": uses, "xp": xp,
                                                              "reputation_discount": 0.05})
            ids.append(M + name)
        w(D / f"minecraft/tags/villager_trade/{prof}/level_{lvl}.json", {"replace": True, "values": ids})

# ---- lang ----
lang_path = A / "lang/en_us.json"
lang = json.loads(lang_path.read_text(encoding="utf-8"))
lang.update({f"item.mechanical.rough_cast_bronze_{p}": f"Rough Cast Bronze {title(p)}" for p in PIECES})
lang.update({f"item.mechanical.bronze_{p}": f"Bronze {title(p)}" for p in PIECES})
lang.update({f"item.mechanical.bronze_{t}": f"Bronze {title(t)}" for t in TOOLS})
lang.update({
    "item.mechanical.bronze_spear": "Bronze Spear",
    "item.mechanical.copper_coin": "Copper Coin", "item.mechanical.silver_coin": "Silver Coin", "item.mechanical.gold_coin": "Gold Coin",
    "item.mechanical.copper_concentrate": "Copper Concentrate", "item.mechanical.tin_concentrate": "Tin Concentrate",
    "tooltip.mechanical.crucible.empty": "Empty: click Crushed Copper / Tin Ore, Concentrate or Native Copper onto it in your inventory",
    "item.mechanical.bronze_ingot": "Bronze Ingot", "item.mechanical.bronze_helmet": "Bronze Helmet", "item.mechanical.bronze_greaves": "Bronze Greaves",
    "tooltip.mechanical.poor_bronze": "Poor Bronze: 70% durability",
    "message.mechanical.crucible.too_hot": "Let it cool before you knock it out",
    "tooltip.mechanical.crucible.mix": "%s%% tin: %s",
    "tooltip.mechanical.crucible.slag": "%s%% tin: no bronze (8-15%% makes bronze, about 10%% the best). Melts as copper; the tin is lost as slag",
    "tooltip.mechanical.crucible.tin_only": "Tin alone casts nothing: add copper (about 9 copper to 1 tin)",
    "message.mechanical.mold.wrong_metal": "%s can't be cast in this mold",
})
w(lang_path, lang)
print(f"bronze ok ({made} sawn planks recipes)")
