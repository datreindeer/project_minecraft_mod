# Bronze Age phase 3b: bronze. Bronze Ingot, Rough Cast / ground bronze pieces, bronze tools, Bronze Saw (+ sawn planks),
# Bronze Helmet / Greaves (+ equipment asset), tool tier tags, villager trades level 3, lang. The Clay Saw Blade / Plate
# Molds are written with the other molds in gen_copper.py. Textures: gen_bronze_textures.py.
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


PIECES = ["pickaxe_head", "axe_head", "shovel_head", "hoe_head", "knife_blade", "saw_blade", "plate"]
TOOLS = ["pickaxe", "axe", "shovel", "hoe", "knife", "saw"]
title = lambda s: s.replace("_", " ").title()

# ---- item models ----
FLAT = ["bronze_ingot", "bronze_helmet", "bronze_greaves"] + [f"rough_cast_bronze_{p}" for p in PIECES] + [f"bronze_{p}" for p in PIECES]
for item in FLAT:
    w(A / f"models/item/{item}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"mechanical:item/{item}"}})
    w(A / f"items/{item}.json", {"model": {"type": "minecraft:model", "model": f"mechanical:item/{item}"}})
for t in TOOLS:
    w(A / f"models/item/bronze_{t}.json", {"parent": "minecraft:item/handheld", "textures": {"layer0": f"mechanical:item/bronze_{t}"}})
    w(A / f"items/bronze_{t}.json", {"model": {"type": "minecraft:model", "model": f"mechanical:item/bronze_{t}"}})

# worn armour: helmet on the humanoid layer, greaves on the leggings layer (textures/entity/equipment/...)
w(A / "equipment/bronze.json", {"layers": {"humanoid": [{"texture": "mechanical:bronze"}], "humanoid_leggings": [{"texture": "mechanical:bronze"}]}})

# ---- tags ----
# the bronze tier mines like stone plus #mechanical:needs_bronze_tool (iron, coal, lapis) and #mechanical:needs_copper_tool (tin)
w(D / "mechanical/tags/block/incorrect_for_bronze_tool.json", {"values": ["#minecraft:needs_iron_tool", "#minecraft:needs_diamond_tool"]})
w(D / "mechanical/tags/item/bronze_tool_materials.json", {"values": [M + "bronze_ingot"]})
for tag, items in [("pickaxes", ["bronze_pickaxe"]), ("axes", ["bronze_axe"]), ("shovels", ["bronze_shovel"]), ("hoes", ["bronze_hoe"]),
                   ("swords", ["bronze_knife"]), ("head_armor", ["bronze_helmet"]), ("leg_armor", ["bronze_greaves"])]:
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

# ---- villages: level 3 opens with bronze goods (level 1-2 trades: gen_data.py) ----
E = "minecraft:emerald"
TRADES = {  # (wants, wants_count, gives, gives_count, max_uses, xp)
    "armorer": [(M + "bronze_ingot", 3, E, 1, 12, 20), (E, 9, M + "bronze_helmet", 1, 3, 10)],
    "toolsmith": [(M + "crushed_tin_ore", 6, E, 1, 12, 20), (E, 7, M + "bronze_pickaxe", 1, 3, 10), (E, 5, M + "bronze_saw", 1, 3, 10)],
    "weaponsmith": [(M + "bronze_ingot", 3, E, 1, 12, 20), (E, 5, M + "bronze_knife", 1, 3, 10)],
}
stack = lambda i, n: {"id": i, "count": n} if n > 1 else {"id": i}
for prof, trades in TRADES.items():
    ids = []
    for i, (wi, wn, gi, gn, uses, xp) in enumerate(trades):
        name = f"{prof}/3/bronze_{i}"
        w(D / f"mechanical/villager_trade/{name}.json", {"wants": stack(wi, wn), "gives": stack(gi, gn), "max_uses": uses, "xp": xp,
                                                          "reputation_discount": 0.05})
        ids.append(M + name)
    w(D / f"minecraft/tags/villager_trade/{prof}/level_3.json", {"replace": True, "values": ids})

# ---- lang ----
lang_path = A / "lang/en_us.json"
lang = json.loads(lang_path.read_text())
lang.update({f"item.mechanical.rough_cast_bronze_{p}": f"Rough Cast Bronze {title(p)}" for p in PIECES})
lang.update({f"item.mechanical.bronze_{p}": f"Bronze {title(p)}" for p in PIECES})
lang.update({f"item.mechanical.bronze_{t}": f"Bronze {title(t)}" for t in TOOLS})
lang.update({
    "item.mechanical.bronze_ingot": "Bronze Ingot", "item.mechanical.bronze_helmet": "Bronze Helmet", "item.mechanical.bronze_greaves": "Bronze Greaves",
    "metal.mechanical.bronze": "Bronze", "metal.mechanical.poor_bronze": "Poor Bronze", "metal.mechanical.tin": "Tin",
    "tooltip.mechanical.poor_bronze": "Poor Bronze: 70% durability",
    "tooltip.mechanical.crucible.mix": "%s%% tin: %s",
    "tooltip.mechanical.crucible.slag": "%s%% tin: no bronze (8-15%% makes bronze, about 10%% the best). Melts as copper; the tin is lost as slag",
    "tooltip.mechanical.crucible.tin_only": "Tin alone casts nothing: add copper (about 9 copper to 1 tin)",
    "message.mechanical.mold.wrong_metal": "%s can't be cast in this mold",
})
w(lang_path, lang)
print(f"bronze ok ({made} sawn planks recipes)")
