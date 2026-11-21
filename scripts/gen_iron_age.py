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


def metal(img, kind, mask=None, min_light=0.0):
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
            if l < min_light:
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
for tool in ("axe", "hoe", "shovel", "sword", "knife"):
    handheld(f"steel_{tool}", lib(f"tools/steel_{tool}"), f"Steel {tool.title()}")
handheld("steel_pickaxe", metal(bronze("pickaxe"), STEEL, mask=bronze("pickaxe_head")), "Steel Pickaxe")
item("steel_spear", metal(bronze("spear"), STEEL, mask=bronze("spear_head")), "Steel Spear")
spear_hand = A / "textures/item/steel_spear_in_hand.png"
metal(bronze("spear_in_hand"), STEEL, min_light=0.33).save(spear_hand)
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
    "message.mechanical.forge.pattern": "Forging: %s (%s hot ingots, %s blows; %s on the anvil). Sneak-use with the hammer to change",
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
    "ingredients": [M + "cementation_pot", M + "wrought_iron_ingot", M + "wrought_iron_ingot", "minecraft:charcoal", "minecraft:charcoal", M + "soot"],
    "result": {"id": M + "packed_cementation_pot", "count": 1}})
w(D / "mechanical/recipe/blister_steel_from_cemented_pot.json", {"type": "minecraft:crafting_shapeless", "category": "misc",
    "ingredients": [M + "cemented_pot"], "result": {"id": M + "blister_steel", "count": 2}})
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

merge(A / "lang/en_us.json", lang)
print("iron age ok")
