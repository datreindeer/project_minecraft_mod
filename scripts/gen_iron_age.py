"""The Iron Age chain (docs/ages/03-iron-age.md), except the new ores (gen_iron_ores.py).

- Roasting: Raw Iron / Raw Bog Iron roast on a campfire or Fire Pit (campfire_cooking, 30 s) into Roasted Iron Ore /
  Roasted Bog Iron (textures: the raw ore, rust-red and darker). The Bloomery takes both; roasted ore gives a full bloom
  (2 Wrought Iron + 1 Slag), raw ore a poor one (1 + 2), flux in the hearth +1 (Java: BloomeryBlockEntity, Ironworking).
- Lang for the tooltips and messages the Java uses.
Run after gen_bloomery.py and gen_bog_iron.py, before gen_block_items.py / gen_unlocks.py / gen_advancements.py:
python scripts/gen_iron_age.py
"""
import colorsys
import glob
import math
import json
import zipfile
from io import BytesIO
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
RES = ROOT / "src/main/resources"
A, D = RES / "assets/mechanical", RES / "data"
M = "mechanical:"
JAR = zipfile.ZipFile(sorted(glob.glob(str(ROOT / "build/moddev/artifacts/minecraft-patched-*-merged.jar"))
                             + glob.glob(str(Path.home() / "Documents/project_minecraft_mod/build/moddev/artifacts/minecraft-patched-*-merged.jar")))[0])
lang = {}


def w(path, obj):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")


def merge(path, obj):
    old = json.loads(path.read_text(encoding="utf-8")) if path.exists() else {}
    old.update(obj)
    w(path, old)


def vanilla_texture(path):
    return Image.open(BytesIO(JAR.read(f"assets/minecraft/textures/{path}.png"))).convert("RGBA")


def mod_texture(path):
    return Image.open(A / f"textures/{path}.png").convert("RGBA")


def recolor(img, hue=None, sat=1.0, light=1.0):
    """Every opaque pixel to {@code hue} (0-1, None keeps it), saturation and lightness scaled."""
    out = img.copy()
    px = out.load()
    for y in range(out.height):
        for x in range(out.width):
            r, g, b, a = px[x, y]
            if a == 0:
                continue
            h, l, s = colorsys.rgb_to_hls(r / 255, g / 255, b / 255)
            h = h if hue is None else hue
            r2, g2, b2 = colorsys.hls_to_rgb(h, max(0, min(1, l * light)), max(0, min(1, s * sat)))
            px[x, y] = (round(r2 * 255), round(g2 * 255), round(b2 * 255), a)
    return out


def item(name, img, title):
    path = A / f"textures/item/{name}.png"
    path.parent.mkdir(parents=True, exist_ok=True)
    img.save(path)
    w(A / f"models/item/{name}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": M + "item/" + name}})
    w(A / f"items/{name}.json", {"model": {"type": "minecraft:model", "model": M + "item/" + name}})
    lang[f"item.mechanical.{name}"] = title


# ---------------------------------------------------------------- roasting
item("roasted_iron_ore", recolor(vanilla_texture("item/raw_iron"), hue=0.025, sat=0.75, light=0.6), "Roasted Iron Ore")
item("roasted_bog_iron", recolor(mod_texture("item/raw_bog_iron"), hue=0.03, sat=0.8, light=0.68), "Roasted Bog Iron")
for raw, roasted in (("minecraft:raw_iron", "roasted_iron_ore"), (M + "raw_bog_iron", "roasted_bog_iron")):
    w(D / f"mechanical/recipe/{roasted}.json", {"type": "minecraft:campfire_cooking", "category": "misc", "cookingtime": 600,
                                                 "experience": 0.1, "ingredient": raw, "result": {"id": M + roasted}})
lang |= {
    "tooltip.mechanical.iron_bloom.yield": "Gives %s Wrought Iron, %s Slag",
    "message.mechanical.bloomery.flux": "Flux in the hearth: %s/%s (each bloom one ingot richer)",
    "message.mechanical.bloomery.flux_full": "The hearth holds %s charges of flux already",
}

# ---------------------------------------------------------------- smithing (Smithing.java)
# Iron and steel parts are recoloured bronze parts; where the user's texture library has the piece, that is used.
LIB = Path.home() / "Documents/Minecraft Modding/item"
IRON, STEEL = (0.62, 0.06, 1.0), (0.58, 0.16, 1.06)  # hue, saturation, lightness factor


def lib(path):
    return Image.open(LIB / f"{path}.png").convert("RGBA")


def metal(img, kind, mask=None, min_light=0.0, max_sat=1.0):
    """Recolour the metal of a bronze texture to iron or steel: every opaque pixel, or those under {@code mask} (the
    part's own texture, drawn at the same place on the tool), or those at least {@code min_light} light."""
    hue, sat, light = kind
    out = img.copy()
    px, mk = out.load(), mask.load() if mask else None
    for y in range(out.height):
        for x in range(out.width):
            r, g, b, a = px[x, y]
            if a == 0 or (mk and mk[x, y][3] == 0):
                continue
            h, l, s = colorsys.rgb_to_hls(r / 255, g / 255, b / 255)
            if l < min_light or s > max_sat:
                continue
            r2, g2, b2 = colorsys.hls_to_rgb(hue, min(1, l * light * 1.08), sat)
            px[x, y] = (round(r2 * 255), round(g2 * 255), round(b2 * 255), a)
    return out


def glowing(img):
    """Metal at forging heat: dark to deep red, light to orange-yellow."""
    out = img.copy()
    px = out.load()
    for y in range(out.height):
        for x in range(out.width):
            r, g, b, a = px[x, y]
            if a == 0:
                continue
            l = colorsys.rgb_to_hls(r / 255, g / 255, b / 255)[1]
            r2, g2, b2 = colorsys.hls_to_rgb(0.01 + 0.1 * l, 0.28 + 0.55 * l, 0.95)
            px[x, y] = (round(r2 * 255), round(g2 * 255), round(b2 * 255), a)
    return out


def handheld(name, img, title):
    item(name, img, title)
    w(A / f"models/item/{name}.json", {"parent": "minecraft:item/handheld", "textures": {"layer0": M + "item/" + name}})


def tag_add(path, *values):
    old = json.loads(path.read_text(encoding="utf-8")) if path.exists() else {"values": []}
    old["values"] += [v for v in values if v not in old["values"]]
    w(path, old)


bronze = lambda n: mod_texture(f"item/bronze_{n}")
item("hot_wrought_iron_ingot", glowing(mod_texture("item/wrought_iron_ingot")), "Hot Wrought Iron Ingot")
item("blister_steel", lib("crude_steel_ingot"), "Blister Steel")
item("hot_blister_steel", lib("crude_steel_ingot_hot"), "Hot Blister Steel")
item("steel_ingot", lib("steel_ingot"), "Steel Ingot")
item("hot_steel_ingot", glowing(lib("steel_ingot")), "Hot Steel Ingot")
item("iron_plate", lib("iron_plate"), "Iron Plate")
item("iron_tool_rod", lib("iron_tool_rod"), "Iron Tool Rod")
item("steel_plate", lib("steel_plate"), "Steel Plate")
PARTS = {"pickaxe_head": "Pickaxe Head", "axe_head": "Axe Head", "shovel_head": "Shovel Head", "hoe_head": "Hoe Head",
         "knife_blade": "Knife Blade", "sword_blade": "Sword Blade", "spear_head": "Spear Head"}
for part, title in PARTS.items():
    item(f"iron_{part}", metal(bronze(part), IRON), f"Iron {title}")
    item(f"steel_{part}", metal(bronze(part), STEEL), f"Steel {title}")
item("iron_hammer_head", metal(bronze("hammer_head"), IRON), "Iron Hammer Head")
handheld("iron_smithing_hammer", metal(bronze("smithing_hammer"), IRON, mask=bronze("hammer_head")), "Iron Smithing Hammer")
# steel tools: vanilla's iron tools with the grey metal blued to steel and the wooden haft kept: forged heads on a
# wedged haft, no lashing (the knife: the mod's Iron Knife, its leather grip kept)
STEEL_TOOL = (0.6, 0.2, 0.82)
for tool in ("pickaxe", "axe", "hoe", "shovel", "sword"):
    handheld(f"steel_{tool}", metal(vanilla_texture(f"item/iron_{tool}"), STEEL_TOOL, max_sat=0.2), f"Steel {tool.title()}")
handheld("steel_knife", metal(mod_texture("item/iron_knife"), STEEL_TOOL, max_sat=0.2), "Steel Knife")
item("steel_spear", metal(vanilla_texture("item/iron_spear"), STEEL_TOOL, max_sat=0.2), "Steel Spear")
spear_hand = A / "textures/item/steel_spear_in_hand.png"
metal(vanilla_texture("item/iron_spear_in_hand"), STEEL_TOOL, max_sat=0.2).save(spear_hand)
w(A / "models/item/steel_spear_in_hand.json", {"parent": "minecraft:item/spear_in_hand", "textures": {"layer0": M + "item/steel_spear_in_hand"}})
spear = json.loads((A / "items/bronze_spear.json").read_text(encoding="utf-8"))
w(A / "items/steel_spear.json", json.loads(json.dumps(spear).replace("bronze_spear", "steel_spear")))

T = D / "minecraft/tags/item"
tag_add(D / "mechanical/tags/item/steel_tool_materials.json", M + "steel_ingot")
tag_add(D / "mechanical/tags/item/hammerstones.json", M + "iron_smithing_hammer")
tag_add(T / "enchantable/durability.json", M + "iron_smithing_hammer")
tag_add(T / "pickaxes.json", M + "steel_pickaxe")
tag_add(T / "axes.json", M + "steel_axe")
tag_add(T / "shovels.json", M + "steel_shovel")
tag_add(T / "hoes.json", M + "steel_hoe")
tag_add(T / "swords.json", M + "steel_sword", M + "steel_knife")
tag_add(T / "spears.json", M + "steel_spear")
tag_add(D / "mechanical/tags/item/knives.json", M + "steel_knife")

# vanilla's iron things from forged parts (their Iron Ingot recipes can't be made: no Iron Ingot in the mod)
w(D / "minecraft/recipe/bucket.json", {"type": "minecraft:crafting_shaped", "category": "misc", "pattern": ["P P", " P "],
                                       "key": {"P": M + "iron_plate"}, "result": {"id": "minecraft:bucket", "count": 1}})
w(D / "minecraft/recipe/shears.json", {"type": "minecraft:crafting_shaped", "category": "equipment", "pattern": [" P", "P "],
                                       "key": {"P": M + "iron_plate"}, "result": {"id": "minecraft:shears", "count": 1}})

lang |= {
    "tooltip.mechanical.forge.hot": "At forging heat: work it on a Bronze Anvil before it cools",
    "tooltip.mechanical.forge.hits": "%s blows struck",
    "message.mechanical.forge.pattern": "Forging: %s (%s hot ingots, %s blows; %s on the anvil). Hold sneak-use with the hammer to choose",
    "message.mechanical.forge.ingots": "That takes %s hot ingots stacked on the anvil, %s there: right-click it with more",
    "message.mechanical.forge.pile_full": "The anvil holds %s ingots at most",
    "message.mechanical.forge.blister": "Hot Blister Steel: %s blows fold it into a Steel Ingot",
    "message.mechanical.forge.cold": "Too cold to forge: heat it to %s° in a Bloomery or Clay Kiln",
    "message.mechanical.forge.no_part": "No %s from this metal: sneak-use with the hammer to choose another",
    **{f"pattern.mechanical.{p}": t for p, t in [("plate", "Plate"), ("tool_rod", "Tool Rod"), *PARTS.items(), ("hammer_head", "Hammer Head")]},
}

# ---------------------------------------------------------------- charcoal clamp (CharcoalClamp.java, CharcoalHeapBlock)
# The heap keeps the firewood pile's billets (models firewood_pile_<n>), charred: oak's end and side sheets near black
# with a sparse silver-grey sheen.
for sheet in ("end", "side"):
    charred = recolor(mod_texture(f"block/oak_firewood_pile_{sheet}"), hue=0.07, sat=0.15, light=0.32)
    px = charred.load()
    for y in range(charred.height):
        for x in range(charred.width):
            r, g, b, a = px[x, y]
            if a and (x * 7 + y * 13) % 23 == 0:
                px[x, y] = (min(255, r + 38), min(255, g + 38), min(255, b + 40), a)
    charred.save(A / f"textures/block/charcoal_heap_{sheet}.png")
for n in range(1, 9):
    w(A / f"models/block/charcoal_heap_{n}.json", {"parent": f"mechanical:block/firewood_pile_{n}", "textures": {
        "end": M + "block/charcoal_heap_end", "side": M + "block/charcoal_heap_side"}})
w(A / "blockstates/charcoal_heap.json", {"variants": {
    f"axis={a},layers={n}": {"model": f"mechanical:block/charcoal_heap_{n}", **({"y": 90} if a == "x" else {})}
    for a in ("x", "z") for n in range(1, 9)}})
tag_add(D / "minecraft/tags/block/mineable/shovel.json", M + "charcoal_heap")
tag_add(D / "mechanical/tags/block/clamp_cover.json", "#minecraft:dirt", "#minecraft:sand", "minecraft:gravel", "minecraft:clay",
        "minecraft:packed_mud", "minecraft:mud_bricks")
lang |= {
    "block.mechanical.charcoal_heap": "Charcoal Heap",
    "message.mechanical.clamp.open": "Cover the pile first: %s open sides. Earth on every side and on top, one side left open to light",
    "message.mechanical.clamp.lit": "The clamp catches. Keep it covered while it smoulders (%s min)",
    "message.mechanical.clamp.burning": "The clamp is smouldering already",
    "message.mechanical.clamp.too_big": "Too big for one clamp: %s wood piles at most",
}

# ---------------------------------------------------------------- cementation (Smithing): steel
# The pot is a crucible thrown in pale fire clay; packed, its mouth is black with charcoal; cemented, it is scorched.
def mouth_black(img, rows=4):
    out = img.copy()
    px = out.load()
    top = next(y for y in range(out.height) if any(px[x, y][3] for x in range(out.width)))
    for y in range(top, min(out.height, top + rows)):
        for x in range(out.width):
            r, g, b, a = px[x, y]
            if a and 3 < x < out.width - 4:
                px[x, y] = (24 + (x * 5 % 9), 22 + (x * 3 % 7), 20, a)
    return out


item("unfired_cementation_pot", recolor(mod_texture("item/unfired_crucible"), hue=0.1, sat=0.35, light=1.2), "Unfired Cementation Pot")
pot = recolor(mod_texture("item/crucible"), hue=0.09, sat=0.3, light=1.15)
item("cementation_pot", pot, "Cementation Pot")
item("packed_cementation_pot", mouth_black(pot), "Packed Cementation Pot")
item("cemented_pot", recolor(mouth_black(pot), hue=0.07, sat=0.25, light=0.55), "Cemented Pot")
w(D / "mechanical/recipe/unfired_cementation_pot.json", {"type": "minecraft:crafting_shaped", "category": "misc", "pattern": ["F F", "FFF"],
                                                          "key": {"F": M + "fire_clay"}, "result": {"id": M + "unfired_cementation_pot", "count": 1}})
w(D / "mechanical/recipe/packed_cementation_pot.json", {"type": "minecraft:crafting_shapeless", "category": "misc",
    "ingredients": [M + "cementation_pot", M + "wrought_iron_ingot", M + "wrought_iron_ingot", M + "wrought_iron_ingot", "minecraft:charcoal", "minecraft:charcoal", M + "soot"],
    "result": {"id": M + "packed_cementation_pot", "count": 1}})
w(D / "mechanical/recipe/blister_steel_from_cemented_pot.json", {"type": "minecraft:crafting_shapeless", "category": "misc",
    "ingredients": [M + "cemented_pot"], "result": {"id": M + "blister_steel", "count": 3}})
# vanilla's Flint and Steel wants an Iron Ingot (none in the mod): a struck Steel Plate instead
w(D / "minecraft/recipe/flint_and_steel.json", {"type": "minecraft:crafting_shapeless", "category": "equipment",
    "ingredients": ["minecraft:flint", M + "steel_plate"], "result": {"id": "minecraft:flint_and_steel", "count": 1}})

# ---------------------------------------------------------------- steel armour (Smithing.STEEL_ARMOR): vanilla's iron armour recoloured
STEEL_ARMOUR = (0.6, 0.2, 0.72)  # iron armour is pale already: darker and bluer than the steel parts
for layer in ("humanoid", "humanoid_leggings"):
    out = A / f"textures/entity/equipment/{layer}/steel.png"
    out.parent.mkdir(parents=True, exist_ok=True)
    metal(vanilla_texture(f"entity/equipment/{layer}/iron"), STEEL_ARMOUR).save(out)
w(A / "equipment/steel.json", {"layers": {"humanoid": [{"texture": M + "steel"}], "humanoid_leggings": [{"texture": M + "steel"}]}})
for piece, tag in (("helmet", "head_armor"), ("chestplate", "chest_armor"), ("leggings", "leg_armor"), ("boots", "foot_armor")):
    item(f"steel_{piece}", metal(vanilla_texture(f"item/iron_{piece}"), STEEL_ARMOUR), f"Steel {piece.title()}")
    tag_add(T / f"{tag}.json", M + f"steel_{piece}")

# ---------------------------------------------------------------- more of vanilla's iron things from forged parts
shaped = {
    "iron_bars": (["RRR", "RRR"], {"R": M + "iron_tool_rod"}, 16),
    "iron_chain": (["R", "R", "R"], {"R": M + "iron_tool_rod"}, 3),
    "cauldron": (["P P", "P P", "PPP"], {"P": M + "iron_plate"}, 1),
    "iron_door": (["PP", "PP", "PP"], {"P": M + "iron_plate"}, 3),
    "iron_trapdoor": (["PP", "PP"], {"P": M + "iron_plate"}, 1),
}
for name, (pattern, key, count) in shaped.items():
    if f"data/minecraft/recipe/{name}.json" not in JAR.namelist():
        continue  # a recipe this version doesn't have (chain's id)
    category = json.loads(JAR.read(f"data/minecraft/recipe/{name}.json")).get("category", "misc")
    w(D / f"minecraft/recipe/{name}.json", {"type": "minecraft:crafting_shaped", "category": category,
        "pattern": pattern, "key": key, "result": {"id": "minecraft:" + name, "count": count}})

# ---------------------------------------------------------------- Iron Bellows, Smithing Hearth, steel quarry hammer
# Iron Bellows: the wooden Bellows' models with the boards plated in iron (oak planks gone iron-grey)
plate = A / "textures/block/iron_bellows_plate.png"
metal(vanilla_texture("block/oak_planks"), (0.6, 0.06, 0.85)).save(plate)
for name in ("bellows", "bellows_pressed"):
    model = json.loads((A / f"models/block/{name}.json").read_text(encoding="utf-8"))
    model["textures"] |= {"particle": M + "block/iron_bellows_plate", "planks": M + "block/iron_bellows_plate", "log": "minecraft:block/iron_block"}
    w(A / f"models/block/iron_{name}.json", model)
w(A / "blockstates/iron_bellows.json", json.loads((A / "blockstates/bellows.json").read_text(encoding="utf-8").replace("block/bellows", "block/iron_bellows")))
w(A / "items/iron_bellows.json", {"model": {"type": "minecraft:model", "model": M + "block/iron_bellows"}})
lang["block.mechanical.iron_bellows"] = "Iron Bellows"
w(D / "mechanical/recipe/iron_bellows.json", {"type": "minecraft:crafting_shapeless", "category": "misc",
    "ingredients": [M + "bellows", M + "iron_plate", M + "iron_plate", M + "iron_tool_rod"], "result": {"id": M + "iron_bellows", "count": 1}})

# Smithing Hearth: fire-brick sides, a bed of coals on top (dark when out, glowing when lit)
def coals(lit):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 255))
    px = img.load()
    for y in range(16):
        for x in range(16):
            n = (x * 37 + y * 61 + x * y * 7) % 23
            if x in (0, 15) or y in (0, 15):
                px[x, y] = (96, 52, 36, 255)  # the brick rim
            elif lit:
                px[x, y] = (255, 170, 60, 255) if n < 3 else (232, 96, 30, 255) if n < 9 else (120, 36, 18, 255) if n < 16 else (40, 22, 18, 255)
            else:
                px[x, y] = (70, 66, 62, 255) if n < 4 else (36, 34, 32, 255) if n < 12 else (22, 21, 20, 255)
    return img


for lit in (False, True):
    sfx = "_lit" if lit else ""
    coals(lit).save(A / f"textures/block/smithing_hearth_top{sfx}.png")
    w(A / f"models/block/smithing_hearth{sfx}.json", {"parent": "minecraft:block/cube_bottom_top", "textures": {
        "side": M + "block/bloomery_bricks", "bottom": M + "block/bloomery_bricks", "top": M + f"block/smithing_hearth_top{sfx}"}})
w(A / "blockstates/smithing_hearth.json", {"variants": {"lit=false": {"model": M + "block/smithing_hearth"},
                                                         "lit=true": {"model": M + "block/smithing_hearth_lit"}}})
w(A / "items/smithing_hearth.json", {"model": {"type": "minecraft:model", "model": M + "block/smithing_hearth"}})
w(D / "mechanical/recipe/smithing_hearth.json", {"type": "minecraft:crafting_shaped", "category": "misc", "pattern": ["B B", "BPB", "BBB"],
                                                  "key": {"B": M + "fire_brick", "P": M + "iron_plate"}, "result": {"id": M + "smithing_hearth", "count": 1}})
for block in ("iron_bellows", "smithing_hearth"):
    w(D / f"mechanical/loot_table/blocks/{block}.json", {"type": "minecraft:block", "pools": [{"rolls": 1,
        "condition": {"type": "minecraft:survives_explosion"}, "entries": [{"type": "minecraft:item", "name": M + block}]}],
        "random_sequence": f"mechanical:blocks/{block}"})
    tag_add(D / "minecraft/tags/block/mineable/pickaxe.json", M + block)
lang["block.mechanical.smithing_hearth"] = "Smithing Hearth"

item("steel_hammer_head", metal(bronze("hammer_head"), STEEL), "Steel Hammer Head")
# the bronze quarry hammer's head is as dark as its haft: pick the head by place (upper right, the haft runs down to the left)
quarry_mask = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
for y in range(16):
    for x in range(16):
        if y <= 7 or (y == 8 and x >= 9) or (y == 9 and x >= 11) or (y == 10 and x >= 12):
            quarry_mask.putpixel((x, y), (255, 255, 255, 255))
handheld("steel_quarry_hammer", metal(mod_texture("item/bronze_quarry_hammer"), (0.6, 0.2, 2.0), mask=quarry_mask), "Steel Quarry Hammer")
tag_add(T / "enchantable/durability.json", M + "steel_quarry_hammer")
tag_add(T / "enchantable/mining.json", M + "steel_quarry_hammer")
lang |= {
    "message.mechanical.forge.steel_hammer": "Steel is too hard for bronze: work it with an Iron Smithing Hammer",
    "message.mechanical.hearth.fuel": "Charcoal in the hearth: %s/%s",
    "message.mechanical.hearth.full": "The hearth holds %s Charcoal",
    "message.mechanical.hearth.no_fuel": "No Charcoal in the hearth: right-click it with Charcoal first",
    "message.mechanical.hearth.no_room": "The hearth's coals are full: four pieces at most",
    "message.mechanical.hearth.state": "Smithing Hearth: %s/%s Charcoal. Lay blooms or ingots of iron or steel in it",
}

# ---------------------------------------------------------------- forged saws, crossbow, Iron Bolts (Smithing)
# Saw blades and saws: the bronze ones with the metal recoloured (the saw's wooden grip kept: masked by the blade).
for kind, tone in (("iron", IRON), ("steel", STEEL)):
    item(f"{kind}_saw_blade", metal(bronze("saw_blade"), tone), f"{kind.title()} Saw Blade")
    handheld(f"{kind}_saw", metal(mod_texture("item/bronze_saw"), tone, mask=bronze("saw_blade")), f"{kind.title()} Saw")
    tag_add(T / "enchantable/durability.json", M + f"{kind}_saw")
BOLT_IRON = {"l": (222, 226, 230), "m": (170, 175, 182), "d": (112, 117, 124), "o": (50, 52, 57)}


def bolt_head():
    """A socketed bodkin point lying along the diagonal (tip upper right): a kite, rimmed, lit upper left, shaded lower right."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    tx, ty, length, width, socket = 15, 1, 9.0, 3.0, 5.0
    r2 = math.sqrt(2)

    def inside(x, y):  # b: across the axis, + to the lower right
        cx, cy = x + 0.5, y + 0.5
        a, b = ((tx - cx) + (cy - ty)) / r2, ((cx - tx) + (cy - ty)) / r2
        if 0 <= a <= length:
            return b, abs(b) <= width * min(a / (length * 0.65), (length - a) / (length * 0.35) + 0.25)
        return b, length < a <= length + socket and abs(b) <= 0.8

    for y in range(16):
        for x in range(16):
            b, ok = inside(x, y)
            if not ok:
                continue
            lower = not inside(x + 1, y)[1] or not inside(x, y + 1)[1]
            upper = not inside(x - 1, y)[1] or not inside(x, y - 1)[1]
            px[x, y] = BOLT_IRON["o" if lower and b > 0 else "m" if upper and b < -0.7 else "l" if b < -0.7 else "m" if b <= 0.3 else "d"]
    return img


def bolt():
    """Shorter and stouter than an arrow: a thick shaft, a small bodkin point, the arrow's fletching."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    arrow = mod_texture("item/bronze_arrow").load()
    for y in range(11, 15):
        for x in range(2, 6):
            if arrow[x, y][3] and arrow[x, y][0] == arrow[x, y][1]:  # the grey/white feathers
                px[x, y] = arrow[x, y]
    for x in range(5, 11):
        px[x, 16 - x] = (137, 103, 39, 255)
        px[x, 17 - x] = (76, 49, 18, 255)
    for (x, y), c in {(14, 2): "l", (13, 2): "l", (12, 3): "l", (11, 4): "l", (13, 3): "m", (12, 4): "m", (11, 5): "m",
                      (14, 3): "o", (13, 4): "d", (12, 5): "d", (13, 5): "o", (12, 6): "o", (11, 6): "d"}.items():
        px[x, y] = BOLT_IRON[c]
    return img


item("iron_bolt_head", bolt_head(), "Iron Bolt Head")
item("iron_bolt", bolt(), "Iron Bolt")
w(D / "mechanical/tags/item/bolts.json", {"values": [M + "iron_bolt"]})
# vanilla's crossbow from forged parts: an Iron Plate prod either side of an Iron Tool Rod trigger and stirrup, a Sinew
# string, a wooden Tool Rod stock (its Iron Ingot / Tripwire Hook recipe can't be made in the mod)
w(D / "minecraft/recipe/crossbow.json", {"type": "minecraft:crafting_shaped", "category": "equipment", "pattern": ["PRP", "STS", " T "],
    "key": {"P": M + "iron_plate", "R": M + "iron_tool_rod", "S": M + "sinew", "T": M + "tool_rod"}, "result": {"id": "minecraft:crossbow", "count": 1}})
lang |= {"pattern.mechanical.saw_blade": "Saw Blade", "pattern.mechanical.bolt_heads": "Bolt Heads"}

# ---------------------------------------------------------------- forged versions of the bronze tools (Smithing.java)
# Sickles, chisels, tongs, needle, fish hook rod, Iron Quarry Hammer, Steel Smithing Hammer: the bronze sprites with the
# metal recoloured (the part's own texture as the mask keeps the wooden handle).
for kind, tone in (("iron", IRON), ("steel", STEEL)):
    K = kind.title()
    item(f"{kind}_sickle_blade", metal(bronze("sickle_blade"), tone), f"{K} Sickle Blade")
    handheld(f"{kind}_sickle", metal(bronze("sickle"), tone, mask=bronze("sickle_blade")), f"{K} Sickle")
    item(f"{kind}_chisel_blade", metal(bronze("chisel_blade"), tone), f"{K} Chisel Blade")
    handheld(f"{kind}_chisel", metal(bronze("chisel"), tone, mask=bronze("chisel_blade")), f"{K} Chisel")
    tag_add(D / "mechanical/tags/item/sickles.json", M + f"{kind}_sickle")
    tag_add(T / "enchantable/durability.json", M + f"{kind}_sickle", M + f"{kind}_chisel")
item("iron_tong_arms", metal(bronze("tong_arms"), IRON), "Iron Tong Arms")
handheld("iron_tongs", metal(bronze("tongs"), IRON, mask=bronze("tong_arms")), "Iron Tongs")
tongs_model = json.loads((A / "models/item/bronze_tongs.json").read_text(encoding="utf-8"))  # its held-out display
w(A / "models/item/iron_tongs.json", tongs_model | {"textures": {"layer0": M + "item/iron_tongs"}})
w(D / "mechanical/tags/item/tongs.json", {"values": [M + "bronze_tongs", M + "iron_tongs"]})
item("iron_needle", metal(bronze("needle"), IRON), "Iron Needle")
tag_add(D / "mechanical/tags/item/needles.json", M + "iron_needle")
item("iron_fish_hook", metal(bronze("fish_hook"), IRON), "Iron Fish Hook")
# the rod: only the hook recoloured, found as the bright metal pixels the cast sprite lacks
rod, cast = mod_texture("item/bronze_hook_fishing_rod"), mod_texture("item/bronze_hook_fishing_rod_cast")
hook = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
for y in range(16):
    for x in range(16):
        r, g, b, al = rod.getpixel((x, y))
        if al and rod.getpixel((x, y)) != cast.getpixel((x, y)) and colorsys.rgb_to_hls(r / 255, g / 255, b / 255)[2] > 0.35:
            hook.putpixel((x, y), (255, 255, 255, 255))
item("iron_hook_fishing_rod", metal(rod, IRON, mask=hook), "Iron Hook Fishing Rod")
cast.save(A / "textures/item/iron_hook_fishing_rod_cast.png")  # cast: the hook is in the water
for n in ("iron_hook_fishing_rod", "iron_hook_fishing_rod_cast"):
    w(A / f"models/item/{n}.json", {"parent": "minecraft:item/handheld_rod", "textures": {"layer0": M + "item/" + n}})
w(A / "items/iron_hook_fishing_rod.json", {"model": {"type": "minecraft:condition", "property": "minecraft:fishing_rod/cast",
    "on_false": {"type": "minecraft:model", "model": M + "item/iron_hook_fishing_rod"},
    "on_true": {"type": "minecraft:model", "model": M + "item/iron_hook_fishing_rod_cast"}}})
tag_add(D / "c/tags/item/tools/fishing_rod.json", M + "iron_hook_fishing_rod")
tag_add(T / "enchantable/fishing.json", M + "iron_hook_fishing_rod")
tag_add(T / "enchantable/durability.json", M + "iron_hook_fishing_rod", M + "iron_tongs", M + "iron_needle")
handheld("iron_quarry_hammer", metal(mod_texture("item/bronze_quarry_hammer"), (0.62, 0.06, 2.0), mask=quarry_mask), "Iron Quarry Hammer")
tag_add(T / "enchantable/durability.json", M + "iron_quarry_hammer")
tag_add(T / "enchantable/mining.json", M + "iron_quarry_hammer")
handheld("steel_smithing_hammer", metal(bronze("smithing_hammer"), STEEL, mask=bronze("hammer_head")), "Steel Smithing Hammer")
tag_add(D / "mechanical/tags/item/hammerstones.json", M + "steel_smithing_hammer")
tag_add(T / "enchantable/durability.json", M + "steel_smithing_hammer")
lang |= {"pattern.mechanical.sickle_blade": "Sickle Blade", "pattern.mechanical.chisel_blade": "Chisel Blade",
         "pattern.mechanical.tong_arms": "Tong Arms", "pattern.mechanical.needles": "Needles", "pattern.mechanical.fish_hooks": "Fish Hooks",
         "message.mechanical.forge.steel_hammer": "Steel is too hard for bronze: work it with an Iron or Steel Smithing Hammer",
         "message.mechanical.hot_metal.crucible": "The crucible burns your hand: hold tongs",
         "message.mechanical.hot_metal.casting": "The casting is hot: take it out with tongs"}

merge(A / "lang/en_us.json", lang)
print("iron age ok")
