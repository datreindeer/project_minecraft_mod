"""End of the Bronze Age: the Bloomery and wrought iron (Ironworking.java; docs/ages/02-bronze-age.md "Bloomery").

Writes the block models (the hearth: fire-brick cube with the slag arch in front and a clay tuyere poking out of the back,
lit: the fire glows in the arch and the tuyere; the stack: a plain fire-brick section, or, as the top, a rim of walls
round the open mouth with the charge inside, glowing while lit; the Bronze Anvil: base, waist, face and horn), the
hammer-head and anvil molds (bronze only, as gen_bronzetools.py's), blockstates, item model definitions, loot tables,
crafting recipes (Fire Clay, Bloomery Hearth, Bloomery Stack, Slag -> Gravel), tags (pickaxe-mineable, the hammer as a
hammerstone) and lang. Vanilla's iron smelting / blasting recipes (raw iron and iron ore) are disabled: Raw Iron only
reduces in the Bloomery. The kiln firings (Fire Clay -> Fire Brick at 1100°, the molds, reheating a bloom) and the
Bloomery's own recipes are in Java. Textures: gen_bloomery_textures.py.
Generator order: ... gen_thread.py, gen_bronzetools.py, gen_pots.py, gen_bloomery.py, gen_block_items.py, gen_unlocks.py,
gen_advancements.py, gen_registry.py. Run: python scripts/gen_bloomery.py
"""
import json
from pathlib import Path

R = Path(__file__).resolve().parent.parent / "src/main/resources"
A, D = R / "assets/mechanical", R / "data"
M = "mechanical:"
B = M + "block/"
FACES = ("down", "up", "north", "south", "east", "west")
ROT = {"north": 0, "east": 90, "south": 180, "west": 270}
NEVER = {"neoforge:conditions": [{"type": "neoforge:never"}]}
title = lambda s: s.replace("_", " ").title()


def w(path, obj):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, indent=2) + "\n", encoding="utf-8")


def tag_add(path, *values):
    tag = json.loads(path.read_text(encoding="utf-8")) if path.exists() else {"values": []}
    tag["values"] += [v for v in values if v not in tag["values"]]
    w(path, tag)


def uv(face, frm, to):
    (x0, y0, z0), (x1, y1, z1) = frm, to
    return {"north": [16 - x1, 16 - y1, 16 - x0, 16 - y0], "south": [x0, 16 - y1, x1, 16 - y0],
            "east": [16 - z1, 16 - y1, 16 - z0, 16 - y0], "west": [z0, 16 - y1, z1, 16 - y0],
            "up": [x0, z0, x1, z1], "down": [x0, 16 - z1, x1, 16 - z0]}[face]


def box(frm, to, tex, cull=False, **extra):
    """tex: one texture for every face or {face: texture}; faces left out of the map are not drawn."""
    texs = tex if isinstance(tex, dict) else {f: tex for f in FACES}
    faces = {}
    for f, t in texs.items():
        faces[f] = {"uv": [max(0, min(16, v)) for v in uv(f, frm, to)], "texture": t, **({"cullface": f} if cull else {})}
    return {"from": frm, "to": to, **extra, "faces": faces}


def model(name, elements, textures):
    w(A / f"models/block/{name}.json", {"parent": "minecraft:block/block", "textures": textures, "elements": elements})


GLOW = {"light_emission": 15, "shade": False}

# ---------------------------------------------------------------- Bloomery Hearth (front = north)
HEARTH_TEX = {"particle": B + "bloomery_bricks", "bricks": B + "bloomery_bricks", "front": B + "bloomery_hearth_front",
              "back": B + "bloomery_tuyere", "clay": B + "kiln_clay_fired", "glow": B + "bloomery_hearth_glow",
              "tuyere_glow": B + "bloomery_tuyere_glow"}
cube = {"from": [0, 0, 0], "to": [16, 16, 16], "faces": {
    f: {"uv": [0, 0, 16, 16], "texture": "#front" if f == "north" else "#back" if f == "south" else "#bricks", "cullface": f} for f in FACES}}
# the tuyere: a fired-clay pipe out of the back wall round the hole (texture centre x 7.5, y 11.5)
nozzle = box([6, 2.5, 16], [10, 6.5, 18], {"up": "#clay", "down": "#clay", "east": "#clay", "west": "#clay", "south": "#back"})
nozzle["faces"]["south"]["uv"] = [5.5, 9.5, 9.5, 13.5]
for f in ("up", "down", "east", "west"):
    nozzle["faces"][f]["uv"] = [4, 4, 8, 6]
model("bloomery_hearth", [cube, nozzle], HEARTH_TEX)
arch_glow = {"from": [0, 0, -0.02], "to": [16, 16, -0.02], **GLOW, "faces": {"north": {"uv": [0, 0, 16, 16], "texture": "#glow"}}}
tuyere_glow = {"from": [6, 2.5, 18.02], "to": [10, 6.5, 18.02], **GLOW, "faces": {"south": {"uv": [5.5, 9.5, 9.5, 13.5], "texture": "#tuyere_glow"}}}
model("bloomery_hearth_lit", [cube, nozzle, arch_glow, tuyere_glow], HEARTH_TEX)
w(A / "blockstates/bloomery_hearth.json", {"variants": {
    f"facing={f},lit={l}": {"model": B + "bloomery_hearth" + ("_lit" if l == "true" else ""), **({"y": ROT[f]} if ROT[f] else {})}
    for f in ROT for l in ("false", "true")}})

# ---------------------------------------------------------------- Bloomery Stack
STACK_TEX = {"particle": B + "bloomery_bricks", "bricks": B + "bloomery_bricks", "sooty": B + "bloomery_bricks_sooty",
             "rim": B + "bloomery_stack_top", "inside": B + "bloomery_inside", "charge": B + "bloomery_charge",
             "charge_lit": B + "bloomery_charge_lit"}
w(A / "models/block/bloomery_stack.json", {"parent": "minecraft:block/cube_all", "textures": {"all": B + "bloomery_bricks"}})


def top_stack(lit):
    """Four 4-px walls round an 8-px mouth, the charge 6 px down inside it."""
    els = [
        box([0, 0, 0], [4, 16, 16], {"west": "#sooty", "north": "#sooty", "south": "#sooty", "up": "#rim", "down": "#bricks", "east": "#inside"}),
        box([12, 0, 0], [16, 16, 16], {"east": "#sooty", "north": "#sooty", "south": "#sooty", "up": "#rim", "down": "#bricks", "west": "#inside"}),
        box([4, 0, 0], [12, 16, 4], {"north": "#sooty", "up": "#rim", "down": "#bricks", "south": "#inside"}),
        box([4, 0, 12], [12, 16, 16], {"south": "#sooty", "up": "#rim", "down": "#bricks", "north": "#inside"}),
        box([4, 0, 4], [12, 10, 12], {"up": "#charge_lit" if lit else "#charge", "down": "#bricks"}, **(GLOW if lit else {})),
    ]
    for e in els:  # the outer walls and the bottom meet neighbours
        for f, face in e["faces"].items():
            if f == "down" or (f in ("north", "south", "east", "west") and face["texture"] == "#sooty"):
                face["cullface"] = f
    return els


model("bloomery_stack_top", top_stack(False), STACK_TEX)
model("bloomery_stack_top_lit", top_stack(True), STACK_TEX)
w(A / "blockstates/bloomery_stack.json", {"variants": {
    f"lit={l},top={t}": {"model": B + ("bloomery_stack" if t == "false" else "bloomery_stack_top_lit" if l == "true" else "bloomery_stack_top")}
    for l in ("false", "true") for t in ("false", "true")}})

# ---------------------------------------------------------------- Bronze Anvil (long axis east-west when facing north)
ANVIL_TEX = {"particle": B + "bronze_anvil", "cast": B + "bronze_anvil", "face": B + "bronze_anvil_top"}
model("bronze_anvil", [
    box([2, 0, 3], [14, 3, 13], "#cast"),
    box([3, 3, 4], [13, 5, 12], {f: "#cast" for f in ("north", "south", "east", "west", "up")}),
    box([5, 5, 6], [11, 9, 10], {f: "#cast" for f in ("north", "south", "east", "west")}),
    box([0, 9, 4], [13, 13, 12], {**{f: "#cast" for f in ("north", "south", "west", "down")}, "up": "#face", "east": "#cast"}),
    box([13, 10, 5.5], [16, 12.5, 10.5], {**{f: "#cast" for f in ("north", "south", "east", "down")}, "up": "#face"}),
], ANVIL_TEX)
w(A / "blockstates/bronze_anvil.json", {"variants": {
    f"facing={f}": {"model": B + "bronze_anvil", **({"y": ROT[f]} if ROT[f] else {})} for f in ROT}})

for block, mdl in (("bloomery_hearth", "bloomery_hearth"), ("bloomery_stack", "bloomery_stack_top"), ("bronze_anvil", "bronze_anvil")):
    w(A / f"items/{block}.json", {"model": {"type": "minecraft:model", "model": B + mdl}})
    w(D / f"mechanical/loot_table/blocks/{block}.json", {"type": "minecraft:block", "pools": [{"rolls": 1,
        "condition": {"type": "minecraft:survives_explosion"}, "entries": [{"type": "minecraft:item", "name": M + block}]}],
        "random_sequence": f"mechanical:blocks/{block}"})

# ---------------------------------------------------------------- molds (bronze only, as gen_bronzetools.py's)
SHAPES = ["hammer_head", "anvil"]
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
FLAT = (["grog", "fire_clay", "fire_brick", "iron_bloom", "hot_iron_bloom", "wrought_iron_ingot", "slag", "bronze_hammer_head"]
        + [f"unfired_clay_{s}_mold" for s in SHAPES] + [f"rough_cast_bronze_{s}" for s in SHAPES])
for name in FLAT:
    w(A / f"models/item/{name}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": M + f"item/{name}"}})
    w(A / f"items/{name}.json", {"model": {"type": "minecraft:model", "model": M + f"item/{name}"}})
w(A / "models/item/bronze_smithing_hammer.json", {"parent": "minecraft:item/handheld", "textures": {"layer0": M + "item/bronze_smithing_hammer"}})
w(A / "items/bronze_smithing_hammer.json", {"model": {"type": "minecraft:model", "model": M + "item/bronze_smithing_hammer"}})

# ---------------------------------------------------------------- recipes


def recipe(name, obj):
    w(D / f"mechanical/recipe/{name}.json", obj)


recipe("fire_clay", {"type": "minecraft:crafting_shapeless", "category": "misc",
                     "ingredients": ["minecraft:clay_ball", "minecraft:clay_ball", M + "grog", M + "grog"], "result": {"id": M + "fire_clay", "count": 4}})
recipe("bloomery_hearth", {"type": "minecraft:crafting_shaped", "category": "misc", "pattern": ["FFF", "FTF", "F F"],
                           "key": {"F": M + "fire_brick", "T": M + "clay_tuyere"}, "result": {"id": M + "bloomery_hearth"}})
recipe("bloomery_stack", {"type": "minecraft:crafting_shaped", "category": "misc", "pattern": ["F F", "F F", "F F"],
                          "key": {"F": M + "fire_brick"}, "result": {"id": M + "bloomery_stack"}})
recipe("gravel_from_slag", {"type": "minecraft:crafting_shapeless", "category": "building",
                            "ingredients": [M + "slag"] * 4, "result": {"id": "minecraft:gravel"}})

# Raw Iron reduces only in the Bloomery: vanilla's furnace / blast furnace iron recipes go (and their unlocks)
for name in [f"iron_ingot_from_{k}_{o}" for k in ("smelting", "blasting") for o in ("raw_iron", "iron_ore", "deepslate_iron_ore")]:
    w(D / f"minecraft/recipe/{name}.json", NEVER)
    w(D / f"minecraft/advancement/recipes/misc/{name}.json", NEVER)

# ---------------------------------------------------------------- tags
tag_add(D / "minecraft/tags/block/mineable/pickaxe.json", M + "bloomery_hearth", M + "bloomery_stack", M + "bronze_anvil")
tag_add(D / "mechanical/tags/item/hammerstones.json", M + "bronze_smithing_hammer")
tag_add(D / "minecraft/tags/item/enchantable/durability.json", M + "bronze_smithing_hammer")

# ---------------------------------------------------------------- lang
lang_path = A / "lang/en_us.json"
lang = json.loads(lang_path.read_text(encoding="utf-8"))
lang.update({f"item.mechanical.unfired_clay_{s}_mold": f"Unfired Clay {title(s)} Mold" for s in SHAPES})
lang.update({f"block.mechanical.clay_{s}_mold": f"Clay {title(s)} Mold" for s in SHAPES})
lang.update({f"item.mechanical.rough_cast_bronze_{s}": f"Rough Cast Bronze {title(s)}" for s in SHAPES})
lang.update({
    "item.mechanical.grog": "Grog", "item.mechanical.fire_clay": "Fire Clay", "item.mechanical.fire_brick": "Fire Brick",
    "block.mechanical.bloomery_hearth": "Bloomery Hearth", "block.mechanical.bloomery_stack": "Bloomery Stack",
    "item.mechanical.hot_iron_bloom": "Hot Iron Bloom", "item.mechanical.iron_bloom": "Iron Bloom",
    "item.mechanical.wrought_iron_ingot": "Wrought Iron Ingot", "item.mechanical.slag": "Slag",
    "item.mechanical.bronze_hammer_head": "Bronze Hammer Head", "item.mechanical.bronze_smithing_hammer": "Bronze Smithing Hammer",
    "block.mechanical.bronze_anvil": "Bronze Anvil", "container.mechanical.bloomery": "Bloomery",
    "message.mechanical.bloomery.stacks": "Not a bloomery yet: stack %s more Bloomery Stack(s) on the hearth",
    "message.mechanical.bloomery.too_tall": "The shaft is too tall: a bloomery is the hearth and two Bloomery Stacks",
    "message.mechanical.bloomery.closed": "The top of the shaft must be open: clear %s %s %s",
    "message.mechanical.bloomery.needs_fuel": "Put Charcoal in the bloomery first",
    "message.mechanical.bloomery.unlit": "The bloomery is not lit",
    "message.mechanical.bloom.cold": "The bloom has cooled: reheat it to %s° in the Bloomery or a Clay Kiln",
    "message.mechanical.bloom.stone_anvil": "Stone is too soft for iron: hammer the bloom on a Bronze Anvil",
    "tooltip.mechanical.iron_bloom.hits": "Hammered %s/%s",
    "tooltip.mechanical.iron_bloom.hot": "Glowing: hammer it on a Bronze Anvil before it cools",
    "tooltip.mechanical.iron_bloom.cold": "Cold: reheat to %s° (Bloomery or Clay Kiln)",
})
w(lang_path, lang)
print("bloomery ok")
