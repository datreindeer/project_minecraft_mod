# Bronze Age phases 2-3 for copper: Bellows, Clay Tuyere, crucibles, Clay Molds, Whetstone, copper heads and the Copper
# Knife. Item/block models, blockstates, loot, recipes, tags, lang. Textures: gen_copper_textures.py.
# usage: python scripts/gen_copper.py   (after gen_wind.py, before gen_block_items.py)
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


SHAPES = ["ingot", "pickaxe_head", "axe_head", "shovel_head", "hoe_head", "knife_blade"]
# every mold lives here; these only take bronze (their textures: gen_bronze_textures.py, items: gen_bronze.py)
MOLDS = SHAPES + ["saw_blade", "plate", "spear_head", "sword_blade"]
METALS = ["copper", "bronze", "poor_bronze"]  # ClayMoldBlock.METAL; poor bronze looks like bronze
HEADS = ["pickaxe_head", "axe_head", "shovel_head", "hoe_head", "knife_blade"]

# ---- flat items ----
FLAT = (["unfired_clay_tuyere", "clay_tuyere", "molten_crucible", "solid_crucible"]
        + [f"rough_cast_copper_{h}" for h in HEADS] + [f"copper_{h}" for h in HEADS] + [f"unfired_clay_{s}_mold" for s in MOLDS])
for item in FLAT:
    w(A / f"models/item/{item}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"mechanical:item/{item}"}})
    w(A / f"items/{item}.json", {"model": {"type": "minecraft:model", "model": f"mechanical:item/{item}"}})
w(A / "models/item/copper_knife.json", {"parent": "minecraft:item/handheld", "textures": {"layer0": "mechanical:item/copper_knife"}})
w(A / "items/copper_knife.json", {"model": {"type": "minecraft:model", "model": "mechanical:item/copper_knife"}})


def box(f, t, tex, faces=("down", "up", "north", "south", "east", "west"), uv=None, **extra):
    return {"from": f, "to": t, **extra, "faces": {d: {"texture": tex, **({"uv": uv[d]} if uv and d in uv else {})} for d in faces}}


ROT = {"north": 0, "east": 90, "south": 180, "west": 270}

# ---- Bellows (built with the nozzle to the north): two planks boards hinged at the back, a pleated leather bag
# between them, a Clay Tuyere nozzle out the front and a handle on the top board. Pressed: the top board comes down.
def bellows(pressed):
    top_y = 4 if pressed else 7       # underside of the top board
    bag_h = top_y - 2
    els = [
        box([2, 0, 3], [14, 2, 15], "#planks"),                           # bottom board
        box([3, 2, 4], [13, 2 + bag_h, 14], "#leather"),                  # bag
        box([2.5, 1 + bag_h / 2, 3.5], [13.5, 2 + bag_h / 2, 14.5], "#leather_dark"),  # pleat
        box([2, top_y, 3], [14, top_y + 2, 15], "#planks"),               # top board
        box([6, top_y + 2, 12], [10, top_y + 3, 16], "#log"),             # handle
        box([6, 0.5, 0], [10, 3.5, 4], "#clay"),                          # tuyere nozzle
        box([7, 1.5, -0.01], [9, 2.5, 0], "#bore", ("north",)),
        box([5, 0, 14], [11, top_y + 1, 15], "#leather_dark"),            # hinge strap at the back
    ]
    return {"parent": "minecraft:block/block", "textures": {"particle": "minecraft:block/oak_planks", "planks": "minecraft:block/oak_planks",
            "leather": "mechanical:block/bellows_leather", "leather_dark": "mechanical:block/bellows_leather", "log": "minecraft:block/stripped_oak_log",
            "clay": "mechanical:block/kiln_clay_fired", "bore": "minecraft:block/black_concrete"}, "elements": els}


w(A / "models/block/bellows.json", bellows(False))
w(A / "models/block/bellows_pressed.json", bellows(True))
w(A / "blockstates/bellows.json", {"variants": {f"facing={f},pressed={p}": {"model": f"mechanical:block/bellows{'_pressed' if p == 'true' else ''}",
    **({"y": y} if y else {})} for f, y in ROT.items() for p in ("false", "true")}})
w(A / "items/bellows.json", {"model": {"type": "minecraft:model", "model": "mechanical:block/bellows"}})

# ---- Whetstone: a sandstone block on a low trestle bench, worn hollow on top ----
w(A / "models/block/whetstone.json", {"parent": "minecraft:block/block", "textures": {
    "particle": "minecraft:block/sandstone", "stone": "minecraft:block/sandstone", "top": "minecraft:block/sandstone_top",
    "planks": "minecraft:block/spruce_planks", "log": "minecraft:block/spruce_log"},
    "elements": [
        box([2, 0, 4], [4, 6, 12], "#log"), box([12, 0, 4], [14, 6, 12], "#log"),        # trestle legs
        box([1, 6, 3], [15, 8, 13], "#planks"),                                           # bench top
        {"from": [3, 8, 4], "to": [13, 12, 12], "faces": {"up": {"texture": "#top"}, "north": {"texture": "#stone", "uv": [3, 4, 13, 8]},
         "south": {"texture": "#stone", "uv": [3, 4, 13, 8]}, "east": {"texture": "#stone", "uv": [4, 4, 12, 8]},
         "west": {"texture": "#stone", "uv": [4, 4, 12, 8]}, "down": {"texture": "#stone"}}},
        box([5, 12, 5], [11, 12.01, 11], "#stone", ("up",)),                              # the worn hollow (darker sandstone side)
    ]})
w(A / "blockstates/whetstone.json", {"variants": {f"facing={f},grind={g}": {"model": "mechanical:block/whetstone", **({"y": y} if y else {})}
    for f, y in ROT.items() for g in range(4)}})
w(A / "items/whetstone.json", {"model": {"type": "minecraft:model", "model": "mechanical:block/whetstone"}})

# ---- Clay Molds: a fired clay slab with the casting's cavity in its top; molten / set metal shows in the cavity ----
for s in MOLDS:
    base = {"particle": "mechanical:block/kiln_clay_fired", "side": "mechanical:block/kiln_clay_fired", "top": f"mechanical:block/clay_{s}_mold_top"}
    slab = {"from": [1, 0, 1], "to": [15, 3, 15], "faces": {
        "up": {"texture": "#top", "uv": [1, 1, 15, 15]}, "down": {"texture": "#side", "cullface": "down"},
        **{d: {"texture": "#side", "uv": [1, 13, 15, 16]} for d in ("north", "south", "east", "west")}}}
    w(A / f"models/block/clay_{s}_mold.json", {"parent": "minecraft:block/block", "textures": base, "elements": [slab]})
    looks = ["copper", "bronze"] if s in SHAPES else ["bronze"]  # copper never lands in the bronze-only molds
    for look in looks:
        for fill in ("molten", "solid"):
            metal = {"from": [1, 3.01, 1], "to": [15, 3.01, 15], "shade": fill == "solid", **({"light_emission": 12} if fill == "molten" else {}),
                     "faces": {"up": {"texture": "#metal", "uv": [1, 1, 15, 15]}}}
            w(A / f"models/block/clay_{s}_mold_{fill}_{look}.json", {"parent": "minecraft:block/block",
                "textures": {**base, "metal": f"mechanical:block/clay_{s}_mold_{look}_{fill}"}, "elements": [slab, metal]})
    look = lambda m: "copper" if m == "copper" and "copper" in looks else "bronze"
    w(A / f"blockstates/clay_{s}_mold.json", {"variants": {
        f"fill={fill},metal={m},uses={u}": {"model": f"mechanical:block/clay_{s}_mold" + ("" if fill == "empty" else f"_{fill}_{look(m)}")}
        for fill in ("empty", "molten", "solid") for m in METALS for u in range(5)}})
    w(A / f"items/clay_{s}_mold.json", {"model": {"type": "minecraft:model", "model": f"mechanical:block/clay_{s}_mold"}})
    # picked up, a mold keeps how many casts it has made (cracks at 5)
    w(D / f"mechanical/loot_table/blocks/clay_{s}_mold.json", {"type": "minecraft:block", "pools": [{"rolls": 1,
        "condition": {"type": "minecraft:survives_explosion"},
        "entries": [{"type": "minecraft:item", "name": f"mechanical:clay_{s}_mold",
                     "modifier": [{"type": "minecraft:copy_state", "block": f"mechanical:clay_{s}_mold", "properties": ["uses"]}]}]}],
        "random_sequence": f"mechanical:blocks/clay_{s}_mold"})
for b in ("bellows", "whetstone"):
    w(D / f"mechanical/loot_table/blocks/{b}.json", {"type": "minecraft:block", "pools": [{"rolls": 1,
        "condition": {"type": "minecraft:survives_explosion"}, "entries": [{"type": "minecraft:item", "name": f"mechanical:{b}"}]}],
        "random_sequence": f"mechanical:blocks/{b}"})

# ---- tags ----
# copper tools mine like stone: iron, coal and lapis still need bronze
patch_tag(D / "minecraft/tags/block/incorrect_for_copper_tool.json", ["#mechanical:needs_bronze_tool"])
tag = json.loads((D / "minecraft/tags/block/incorrect_for_copper_tool.json").read_text(encoding="utf-8"))
tag["replace"] = False
w(D / "minecraft/tags/block/incorrect_for_copper_tool.json", tag)
patch_tag(D / "minecraft/tags/block/mineable/pickaxe.json", [M + "whetstone"])
patch_tag(D / "minecraft/tags/block/mineable/axe.json", [M + "bellows"])
patch_tag(D / "minecraft/tags/item/swords.json", [M + "copper_knife"])
patch_tag(D / "mechanical/tags/item/knives.json", [M + "copper_knife"])
patch_tag(D / "mechanical/tags/item/hammerstones.json", ["minecraft:copper_pickaxe"])

# ---- recipes ----
def shaped(name, pattern, key, result, category="misc"):
    w(D / f"mechanical/recipe/{name}.json", {"type": "minecraft:crafting_shaped", "category": category,
        "pattern": pattern, "key": key, "result": {"id": result}})


C = {"C": "minecraft:clay_ball"}
shaped("unfired_clay_tuyere", ["  C", " C ", "C  "], C, M + "unfired_clay_tuyere")
shaped("bellows", ["PHP", "PHP", "TUT"], {"P": "#minecraft:planks", "H": M + "scraped_hide", "T": M + "twine", "U": M + "clay_tuyere"},
       M + "bellows", "equipment")
shaped("whetstone", [" S ", "TPT"], {"S": "minecraft:sandstone", "T": "minecraft:stick", "P": "#minecraft:planks"}, M + "whetstone", "equipment")
MOLD_PATTERNS = {  # clay balls around the cavity; each one unique (and unlike the crucible / clay bucket)
    "ingot": ["C C", "CCC"],
    "pickaxe_head": ["CCC", "C C", "C C"],
    "axe_head": ["CCC", "C C", "CC "],
    "shovel_head": ["C C", "C C", " C "],
    "hoe_head": ["CC ", "C  ", "CCC"],
    "knife_blade": ["C C", " C ", " C "],
    "saw_blade": ["CCC", "C C"],
    "plate": ["CCC", "C C", "CCC"],
    "spear_head": [" C ", "C C", " C "],
    "sword_blade": ["C C", "C C", "C C"],
}
for s, pattern in MOLD_PATTERNS.items():
    shaped(f"unfired_clay_{s}_mold", pattern, C, M + f"unfired_clay_{s}_mold")

# ---- lang ----
lang_path = A / "lang/en_us.json"
lang = json.loads(lang_path.read_text(encoding="utf-8"))
title = lambda s: s.replace("_", " ").title()
lang.update({f"item.mechanical.unfired_clay_{s}_mold": f"Unfired Clay {title(s)} Mold" for s in MOLDS})
lang.update({f"block.mechanical.clay_{s}_mold": f"Clay {title(s)} Mold" for s in MOLDS})
lang.update({f"item.mechanical.rough_cast_copper_{h}": f"Rough Cast Copper {title(h)}" for h in HEADS})
lang.update({f"item.mechanical.copper_{h}": f"Copper {title(h)}" for h in HEADS})
lang.update({
    "item.mechanical.unfired_clay_tuyere": "Unfired Clay Tuyere", "item.mechanical.clay_tuyere": "Clay Tuyere",
    "block.mechanical.bellows": "Bellows", "block.mechanical.whetstone": "Whetstone", "item.mechanical.copper_knife": "Copper Knife",
    "item.mechanical.molten_crucible": "Crucible of Molten Metal", "item.mechanical.solid_crucible": "Crucible of Solid Metal",
    "item.mechanical.molten_crucible.of": "Crucible of Molten %s", "item.mechanical.solid_crucible.of": "Crucible of Solid %s",
    "metal.mechanical.copper": "Copper",
    "tooltip.mechanical.crucible.empty": "Empty: click Crushed Copper Ore, Native Copper or Crushed Tin Ore onto it in your inventory",
    "tooltip.mechanical.crucible.ore": "%s ore: %s/%s units",
    "tooltip.mechanical.crucible.metal": "%s: %s units",
    "tooltip.mechanical.crucible.melts": "Melts in a Clay Kiln at %s°",
    "tooltip.mechanical.crucible.pour": "Pour into a Clay Mold before it sets",
    "tooltip.mechanical.crucible.remelt": "Melt it again in a Clay Kiln",
    "message.mechanical.mold.not_enough": "This mold takes %s units of metal; the crucible holds %s",
    "message.mechanical.mold.molten": "The metal is still molten",
    "message.mechanical.mold.cracked": "The mold cracked",
    "message.mechanical.mold.set_in_crucible": "Too late: the metal has set in the crucible",
    "message.mechanical.bellows.no_kiln": "The bellows must blow into a Clay Kiln's firebox or a Smelting Furnace's wall",
    "message.mechanical.bellows.unlit": "The kiln is not lit",
    "message.mechanical.kiln.cracked": "The kiln is cracked: press 4 Clay Balls into it, then fire it again",
    "message.mechanical.kiln.patching": "Patched %s/%s",
    "message.mechanical.kiln.patched": "Patched: keep a fire burning to fire the new clay in",
    "gui.mechanical.kiln.temperature": "Temperature: %s°",
    "gui.mechanical.kiln.needs": "Needs %s°",
    "gui.mechanical.kiln.speed": "Baking speed: %sx (hotter is faster)",
    "gui.mechanical.kiln.strain": "Overheating strain: %s%%",
    "gui.mechanical.kiln.cracked": "Cracked: no hotter than 600° until patched with %s Clay Balls",
})
w(lang_path, lang)
print("copper ok")
