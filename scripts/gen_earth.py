"""Bog and earth (docs/world-materials-plan.md phase 3; EarthBlocks.java): Peat, Turf (+ slab/stairs), Bog Oak (a wood set
without tree, copied from vanilla oak), Reed (grows like sugar cane) and Reed Thatch (+ slab/stairs and a Bark Roof-style
roof), Loess, Wattle, Wattle & Daub (+ framed), Kaolin + Kaolin Clay. Blockstates, models, loot, recipes, tags, lang,
textures (recoloured vanilla), axe stripping, and worldgen for these and the earth families
(Peat, Bog Oak logs, Reed, Mudstone, Loess, Kaolin, Red/Yellow Ochre). The families themselves (Peat Bricks, Adobe/Mudstone,
Loess Bricks, Rammed Earth, Kaolin Tiles, Ochre blocks) are rows in gen_stone_families.py; this script only replaces the
Rammed Earth texture with rammed layers. Loess tills into rich farmland: gen_farming.py.

Generator order: ... gen_data.py ... gen_stone_families.py, gen_earth.py, ... gen_block_items.py, gen_unlocks.py,
gen_advancements.py, gen_loot_format.py. Run: python scripts/gen_earth.py (writes earth_preview.png in the repo root, not committed)
"""
import glob
import io
import json
import random
import re
import sys
import zipfile
from pathlib import Path

from PIL import Image

sys.path.insert(0, str(Path(__file__).resolve().parent))
import gen_bark_roof as roof  # noqa: E402  (same slopes for the thatch roof)

ROOT = Path(__file__).resolve().parent.parent
RES = ROOT / "src/main/resources"
A, D = RES / "assets/mechanical", RES / "data"
M = "mechanical:"
jar = zipfile.ZipFile(sorted(glob.glob(str(ROOT / "build/moddev/artifacts/minecraft-patched-*-merged.jar")))[0])
preview = []


def w(path, obj):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, indent=2) + "\n", encoding="utf-8")


def tag_add(rel, values):
    path = D / rel
    tag = json.loads(path.read_text(encoding="utf-8")) if path.exists() else {"values": []}
    tag["values"] += [v for v in values if v not in tag["values"]]
    w(path, tag)


def title(name):
    return name.replace("_and_", "_&_").replace("_", " ").title()


# ---------------------------------------------------------------- textures

def vanilla(name, kind="block"):
    return Image.open(io.BytesIO(jar.read(f"assets/minecraft/textures/{kind}/{name}.png"))).convert("RGBA")


def ramp(palette, t):
    t = max(0.0, min(1.0, t)) * (len(palette) - 1)
    i = min(int(t), len(palette) - 2)
    f = t - i
    return tuple(int(palette[i][c] * (1 - f) + palette[i + 1][c] * f) for c in range(3))


def recolour(img, palette):
    """Brightness of each pixel -> the palette (dark -> light), over the image's own brightness range."""
    out = img.copy()
    px = [sum(p[:3]) / 3 for p in (img.getpixel((x, y)) for y in range(img.height) for x in range(img.width)) if p[3]]
    lo, hi = min(px), max(px)
    for y in range(img.height):
        for x in range(img.width):
            r, g, b, a = img.getpixel((x, y))
            if a:
                out.putpixel((x, y), ramp(palette, ((r + g + b) / 3 - lo) / max(1, hi - lo)) + (a,))
    return out


def tint(img, colour):
    out = img.copy()
    for y in range(img.height):
        for x in range(img.width):
            r, g, b, a = img.getpixel((x, y))
            out.putpixel((x, y), (r * colour[0] // 255, g * colour[1] // 255, b * colour[2] // 255, a))
    return out


def save(img, name, kind="block"):
    path = A / f"textures/{kind}/{name}.png"
    path.parent.mkdir(parents=True, exist_ok=True)
    img.save(path)
    preview.append(img)


PEAT = [(28, 20, 14), (40, 29, 20), (54, 40, 28), (70, 53, 37), (88, 68, 48)]
LOESS = [(150, 128, 92), (174, 150, 110), (196, 172, 130), (214, 192, 150), (230, 210, 170)]  # = its family row
KAOLIN = [(190, 188, 182), (208, 206, 200), (224, 222, 217), (238, 237, 233), (250, 249, 246)]
REED = [(66, 74, 38), (94, 102, 50), (124, 126, 64), (156, 146, 84), (186, 168, 108)]
THATCH = [(92, 78, 46), (124, 106, 62), (152, 132, 80), (178, 158, 100), (204, 186, 128)]
DAUB = [(150, 136, 112), (172, 158, 134), (192, 178, 154), (208, 196, 174), (222, 212, 192)]
BOG_PLANKS = [(24, 18, 13), (36, 27, 19), (48, 36, 26), (62, 47, 34), (78, 60, 44)]
BOG_BARK = [(13, 11, 9), (22, 19, 15), (32, 27, 21), (43, 36, 28), (56, 47, 37)]
RAMMED = [(104, 74, 52), (128, 94, 66), (150, 112, 80), (172, 132, 96), (192, 152, 114)]  # = its family row
TIMBER = (58, 42, 30)

save(recolour(vanilla("mud"), PEAT), "peat")
save(recolour(vanilla("dirt"), LOESS), "loess")
save(recolour(vanilla("clay"), KAOLIN), "kaolin")
save(recolour(vanilla("clay_ball", "item"), KAOLIN), "kaolin_clay", "item")
# Reed is pale grey, coloured by season in game (SeasonLook.reed: green in summer, gold in autumn, straw in winter)
save(recolour(vanilla("sugar_cane"), [(104, 104, 104), (146, 146, 146), (186, 186, 186), (222, 222, 222), (255, 255, 255)]), "reed")
save(recolour(vanilla("hay_block_top"), THATCH), "reed_thatch_top")
save(recolour(vanilla("hay_block_side"), THATCH), "reed_thatch_side")
for old in ("turf_top", "turf_side"):  # Turf now uses vanilla's grey grass textures, tinted with the seasonal grass colour
    (A / f"textures/block/{old}.png").unlink(missing_ok=True)
daub = recolour(vanilla("packed_mud"), DAUB)
rng = random.Random(3)
for _ in range(14):  # straw in the daub
    x, y = rng.randrange(15), rng.randrange(16)
    daub.putpixel((x, y), (184, 160, 96, 255))
    daub.putpixel((x + 1, y), (168, 146, 86, 255))
save(daub, "wattle_and_daub")
framed = daub.copy()
for i in range(16):
    for t in (0, 1):
        for p in ((i, t), (i, 15 - t), (t, i), (15 - t, i)):
            framed.putpixel(p, TIMBER + (255,))
    if 1 < i < 14:  # a brace corner to corner
        framed.putpixel((i, 15 - i), TIMBER + (255,))
        framed.putpixel((i, 14 - i), TIMBER + (255,))
save(framed, "framed_wattle_and_daub")
# wattle: upright stakes with withies woven through them, open between
wattle = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
stakes, rods = (2, 7, 12), (1, 5, 9, 13)
for y in range(16):
    for x in stakes:
        for dx in (0, 1):
            wattle.putpixel((x + dx, y), (86, 62, 40, 255) if dx else (104, 76, 50, 255))
for row, y in enumerate(rods):
    for x in range(16):
        front = ((x + 3) // 5 + row) % 2 == 0  # over one stake, under the next
        if any(s <= x <= s + 1 for s in stakes) and not front:
            continue
        wattle.putpixel((x, y), (150, 116, 74, 255))
        wattle.putpixel((x, y + 1), (124, 94, 60, 255))
save(wattle, "wattle")
save(Image.new("RGBA", (16, 16), (104, 76, 50, 255)), "wattle_top")
# rammed earth: thin rammed layers of slightly different earth (replaces the bricks recolour of its family row)
rammed = Image.new("RGBA", (16, 16))
rng = random.Random(7)
shade = [rng.choice((1.5, 2.0, 2.6, 3.0)) for _ in range(16)]
for y in range(16):
    band = shade[y // 2 * 2] if y % 4 != 3 else 0.9  # a darker seam between each lift of earth
    for x in range(16):
        rammed.putpixel((x, y), ramp(RAMMED, (band + rng.uniform(-0.5, 0.5)) / 4) + (255,))
save(rammed, "rammed_earth")

# ---------------------------------------------------------------- simple blocks

blocks, lang = [], {}


def block(name, model, loot="self"):
    """Blockstate (one model), the model, loot (itself, or (item, count) like clay: silk touch gives the block)."""
    blocks.append(name)
    w(A / f"blockstates/{name}.json", {"variants": {"": {"model": M + "block/" + name}}})
    if model:
        w(A / f"models/block/{name}.json", model)
    if loot == "self":
        table = {"type": "minecraft:block", "pools": [{"rolls": 1, "condition": {"type": "minecraft:survives_explosion"},
                 "entries": [{"type": "minecraft:item", "name": M + name}]}]}
    else:
        table = {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{"type": "minecraft:alternatives", "children": [
            {"type": "minecraft:item", "condition": "minecraft:tool/can_silk_touch", "name": M + name},
            {"type": "minecraft:item", "name": M + loot[0], "modifier": [
                {"type": "minecraft:set_count", "count": loot[1]}, {"type": "minecraft:explosion_decay"}]}]}]}]}
    table["random_sequence"] = f"mechanical:blocks/{name}"
    w(D / f"mechanical/loot_table/blocks/{name}.json", table)
    lang[f"block.mechanical.{name}"] = title(name)


def cube(tex):
    return {"parent": "minecraft:block/cube_all", "textures": {"all": M + "block/" + tex}}


def bottom_top(top, side_, bottom):
    return {"top": top, "side": side_, "bottom": bottom}


def recipe(name, obj):
    w(D / f"mechanical/recipe/{name}.json", {"category": "building", **obj})


def shaped(name, pattern, key, result, count=1):
    recipe(name, {"type": "minecraft:crafting_shaped", "pattern": pattern, "key": key, "result": {"id": M + result, "count": count}})


def shapeless(name, ingredients, result, count=1):
    recipe(name, {"type": "minecraft:crafting_shapeless", "ingredients": ingredients, "result": {"id": M + result, "count": count}})


def slab_stairs(name, tex, parents="minecraft:block/"):
    """<name>_slab and <name>_stairs (vanilla sandstone's blockstates; tex: top/side/bottom), 3 in a row -> 6, stairs -> 4."""
    for kind, models in (("slab", {"": "slab", "_top": "slab_top"}), ("stairs", {"": "stairs", "_inner": "inner_stairs", "_outer": "outer_stairs"})):
        b = f"{name}_{kind}"
        blocks.append(b)
        state = jar.read(f"assets/minecraft/blockstates/sandstone_{kind}.json").decode()
        state = state.replace(f"minecraft:block/sandstone_{kind}", f"{M}block/{b}").replace('"minecraft:block/sandstone"', f'"{M}block/{name}"')
        w(A / f"blockstates/{b}.json", json.loads(state))
        for suffix, parent in models.items():
            w(A / f"models/block/{b}{suffix}.json", {"parent": parents + parent, "textures": tex})
        loot = jar.read(f"data/minecraft/loot_table/blocks/sandstone_{kind}.json").decode().replace(f"minecraft:sandstone_{kind}", M + b)
        w(D / f"mechanical/loot_table/blocks/{b}.json", json.loads(loot.replace(f"minecraft:blocks/sandstone_{kind}", f"mechanical:blocks/{b}")))
        lang[f"block.mechanical.{b}"] = title(b)
    shaped(f"{name}_slab", ["###"], {"#": M + name}, f"{name}_slab", 6)
    shaped(f"{name}_stairs", ["#  ", "## ", "###"], {"#": M + name}, f"{name}_stairs", 4)


# Peat: dug with a shovel, drops itself; a long-burning fuel (KilnBlockEntity.FUELS, Campfires)
block("peat", cube("peat"))
shaped("peat_bricks", ["##", "##"], {"#": M + "peat"}, "peat_bricks", 4)
# Loess (its bricks are a family row) and Kaolin (crumbles into 4 Kaolin Clay like clay; fired into Kaolin Tiles in the kiln)
block("loess", cube("loess"))
block("kaolin", cube("kaolin"), ("kaolin_clay", 4))
shaped("kaolin", ["##", "##"], {"#": M + "kaolin_clay"}, "kaolin")
w(A / "models/item/kaolin_clay.json", {"parent": "minecraft:item/generated", "textures": {"layer0": M + "item/kaolin_clay"}})
w(A / "items/kaolin_clay.json", {"model": {"type": "minecraft:model", "model": M + "item/kaolin_clay"}})
lang["item.mechanical.kaolin_clay"] = "Kaolin Clay"
# Turf: dirt bound with plant fibre (grass roots), for turf roofs and walls
# Grass-tinted like a Grass Block (client.SeasonalFoliage), so it follows the seasons: the top and a side fringe overlay.
turf = {"top": "minecraft:block/grass_block_top", "side": "minecraft:block/dirt", "bottom": "minecraft:block/dirt",
        "overlay": "minecraft:block/grass_block_side_overlay", "particle": "minecraft:block/dirt"}


def grassy(parent):
    """Vanilla block model {parent} with its top faces grass-tinted and a tinted #overlay copy of its side faces."""
    m = json.loads(jar.read(f"assets/minecraft/models/block/{parent}.json"))
    els = []
    for e in m["elements"]:
        e = json.loads(json.dumps(e))
        if "up" in e["faces"]:
            e["faces"]["up"]["tintindex"] = 0
        els.append(e)
        sides = {d: {**f, "texture": "#overlay", "tintindex": 0} for d, f in e["faces"].items() if d in ("north", "south", "west", "east")}
        if sides:
            els.append({**{k: v for k, v in e.items() if k != "faces"}, "faces": sides})
    m["elements"] = els
    w(A / f"models/block/grassy_{parent}.json", m)


for p in ("slab", "slab_top", "stairs", "inner_stairs", "outer_stairs"):
    grassy(p)
block("turf", {"parent": "minecraft:block/grass_block", "textures": turf})
shapeless("turf", ["minecraft:dirt", M + "plant_fibre"], "turf")
slab_stairs("turf", turf, M + "block/grassy_")
GRASS_ITEM_TINT = [{"type": "minecraft:grass", "downfall": 1.0, "temperature": 0.5}]
for b in ("turf", "turf_slab", "turf_stairs"):
    w(A / f"items/{b}.json", {"model": {"type": "minecraft:model", "model": M + "block/" + b, "tints": GRASS_ITEM_TINT}})
# Reed: grows like sugar cane by water; Reed Thatch block, slab, stairs and a sloped roof like the Bark Roof.
# Tinted by season (SeasonLook.reed); in the hand its summer green.
block("reed", {"parent": "minecraft:block/tinted_cross", "textures": {"cross": M + "block/reed"}})
w(A / "items/reed.json", {"model": {"type": "minecraft:model", "model": M + "item/reed",
                                    "tints": [{"type": "minecraft:constant", "value": -5789354}]}})  # 0xFFA7A956
thatch = bottom_top(M + "block/reed_thatch_top", M + "block/reed_thatch_side", M + "block/reed_thatch_top")
block("reed_thatch", {"parent": "minecraft:block/cube_bottom_top", "textures": thatch})
shaped("reed_thatch", ["##", "##"], {"#": M + "reed"}, "reed_thatch")
slab_stairs("reed_thatch", thatch)
blocks.append("reed_thatch_roof")
for shape in ("straight", "inner", "outer", "flat"):
    w(A / f"models/block/reed_thatch_roof_{shape}.json", {"parent": f"{M}block/bark_roof_{shape}",
      "textures": {"top": M + "block/reed_thatch_side", "under": M + "block/reed_thatch_top"}})
variants = {}
for flat in ("false", "true"):
    for facing in roof.ROT:
        for shape in ("straight", "inner_left", "inner_right", "outer_left", "outer_right"):
            if flat == "true":
                m = {"model": M + "block/reed_thatch_roof_flat"}
            else:  # as gen_bark_roof.py
                turn = roof.CW[facing] if shape == "outer_right" else roof.CCW[facing] if shape == "inner_left" else facing
                m = {"model": f"{M}block/reed_thatch_roof_{shape.split('_')[0]}"}
                if roof.ROT[turn]:
                    m["y"] = roof.ROT[turn]
            variants[f"facing={facing},flat={flat},shape={shape}"] = m
w(A / "blockstates/reed_thatch_roof.json", {"variants": variants})
w(D / "mechanical/loot_table/blocks/reed_thatch_roof.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [
    {"type": "minecraft:item", "name": M + "reed_thatch_roof"}], "condition": {"type": "minecraft:survives_explosion"}}],
    "random_sequence": "mechanical:blocks/reed_thatch_roof"})
shaped("reed_thatch_roof", ["# ", "##"], {"#": M + "reed"}, "reed_thatch_roof", 4)
lang["block.mechanical.reed_thatch_roof"] = "Reed Thatch Roof"
# Wattle (a pane of woven sticks) and Wattle & Daub
blocks.append("wattle")
state = jar.read("assets/minecraft/blockstates/glass_pane.json").decode().replace("minecraft:block/glass_pane_", M + "block/wattle_")
w(A / "blockstates/wattle.json", json.loads(state))
for part in ("post", "side", "side_alt", "noside", "noside_alt"):
    w(A / f"models/block/wattle_{part}.json", {"parent": f"minecraft:block/template_glass_pane_{part}",
      "textures": {"pane": M + "block/wattle", "edge": M + "block/wattle_top"}})
w(A / "models/item/wattle.json", {"parent": "minecraft:item/generated", "textures": {"layer0": M + "block/wattle"}})
w(A / "items/wattle.json", {"model": {"type": "minecraft:model", "model": M + "item/wattle"}})
w(D / "mechanical/loot_table/blocks/wattle.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [
    {"type": "minecraft:item", "name": M + "wattle"}], "condition": {"type": "minecraft:survives_explosion"}}],
    "random_sequence": "mechanical:blocks/wattle"})
lang["block.mechanical.wattle"] = "Wattle"
shaped("wattle", ["###", "###"], {"#": "minecraft:stick"}, "wattle", 4)
block("wattle_and_daub", cube("wattle_and_daub"))
shapeless("wattle_and_daub", [M + "wattle", "minecraft:clay_ball", "minecraft:clay_ball", [M + "straw", M + "plant_fibre"], M + "manure"],
          "wattle_and_daub", 2)
block("framed_wattle_and_daub", cube("framed_wattle_and_daub"))
shapeless("framed_wattle_and_daub", [M + "wattle_and_daub", "minecraft:stick", "minecraft:stick"], "framed_wattle_and_daub")
# family recipes beyond vanilla's: Rammed Earth, Adobe from clay
shapeless("rammed_earth", [["minecraft:dirt", "minecraft:coarse_dirt", M + "loess"], "minecraft:gravel", "minecraft:clay_ball"], "rammed_earth", 2)
shapeless("adobe_bricks_from_clay", ["minecraft:clay_ball", "#minecraft:sand", M + "straw"], "adobe_bricks", 2)
shapeless("red_ochre_from_block", [M + "red_ochre_block"], "red_ochre", 4)
shapeless("yellow_ochre_from_block", [M + "yellow_ochre_block"], "yellow_ochre", 4)

# ---------------------------------------------------------------- Bog Oak: vanilla oak's files, no leaves/sapling/sign/boat/shelf
OAK_FILE = re.compile(r"(?:stripped_)?oak_(?!.*(?:sign|boat|shelf|leaves|sapling))[a-z_]*\.json")
textures = set()
for path in jar.namelist():
    folder, _, name = path.rpartition("/")
    if folder not in ("assets/minecraft/blockstates", "assets/minecraft/models/block", "assets/minecraft/models/item", "assets/minecraft/items",
                      "data/minecraft/loot_table/blocks", "data/minecraft/recipe") or not OAK_FILE.fullmatch(name) or name == "oak_planks.json" and "recipe" in folder:
        continue
    text = re.sub(r"minecraft:((?:block/|item/|blocks/)?(?:stripped_)?)oak_", lambda m: f"mechanical:{m[1]}bog_oak_", jar.read(path).decode())
    obj = json.loads(text)
    out = (A / folder.split("/", 2)[2] if folder.startswith("assets") else D / "mechanical" / folder.split("/", 2)[2]) / name.replace("oak", "bog_oak")
    w(out, obj)
    if "/models/" in path:
        textures |= {t for t in obj.get("textures", {}).values() if isinstance(t, str) and "bog_oak" in t}
for t in sorted(textures):
    kind, tex = t.split(":")[1].split("/")
    src = tex.replace("bog_oak", "oak")
    save(recolour(vanilla(src, kind), BOG_BARK if src == "oak_log" else BOG_PLANKS), tex, kind)
WOOD = ["log", "wood", "planks", "slab", "stairs", "fence", "fence_gate", "door", "trapdoor", "button", "pressure_plate"]
bog = [f"bog_oak_{p}" for p in WOOD] + ["stripped_bog_oak_log", "stripped_bog_oak_wood"]
blocks += bog
lang.update({f"block.mechanical.{b}": title(b) for b in bog})
recipe("bog_oak_planks", {"type": "minecraft:crafting_shapeless", "group": "planks", "ingredients": ["#mechanical:bog_oak_logs"],
                          "result": {"id": M + "bog_oak_planks", "count": 2}})  # 2 per log until the saw, as gen_data.py
logs = [M + b for b in ("bog_oak_log", "bog_oak_wood", "stripped_bog_oak_log", "stripped_bog_oak_wood")]
for kind in ("block", "item"):
    w(D / f"mechanical/tags/{kind}/bog_oak_logs.json", {"values": logs})
    tag_add(f"minecraft/tags/{kind}/logs_that_burn.json", ["#mechanical:bog_oak_logs"])
    for tag, part in {"planks": "planks", "wooden_slabs": "slab", "wooden_stairs": "stairs", "wooden_fences": "fence", "fence_gates": "fence_gate",
                      "wooden_doors": "door", "wooden_trapdoors": "trapdoor", "wooden_buttons": "button", "wooden_pressure_plates": "pressure_plate"}.items():
        tag_add(f"minecraft/tags/{kind}/{tag}.json", [f"{M}bog_oak_{part}"])
axe = D / "minecraft/block_transformer/axe.json"
transformer = json.loads(axe.read_text(encoding="utf-8"))
rules = transformer[0]["block_state_provider"]["rules"]
for k in ("log", "wood"):
    rule = {"if_true": {"type": "minecraft:matching_blocks", "blocks": f"{M}bog_oak_{k}"},
            "then": {"type": "minecraft:copy_properties", "source": {"id": f"{M}stripped_bog_oak_{k}", "properties": {"axis": "y"}}}}
    if rule not in rules:
        rules.insert(0, rule)
w(axe, transformer)

# ---------------------------------------------------------------- tags
tag_add("minecraft/tags/block/mineable/shovel.json", [M + b for b in ("peat", "loess", "kaolin", "turf", "turf_slab", "turf_stairs",
                                                                       "wattle_and_daub", "framed_wattle_and_daub")])
tag_add("minecraft/tags/block/mineable/axe.json", [M + b for b in bog + ["wattle"]])
tag_add("minecraft/tags/block/mineable/hoe.json", [M + b for b in ("reed_thatch", "reed_thatch_slab", "reed_thatch_stairs", "reed_thatch_roof")])
for kind in ("block", "item"):
    tag_add(f"minecraft/tags/{kind}/slabs.json", [M + "turf_slab", M + "reed_thatch_slab"])
    tag_add(f"minecraft/tags/{kind}/stairs.json", [M + "turf_stairs", M + "reed_thatch_stairs"])

# ---------------------------------------------------------------- worldgen
BIOMES = {
    "has_peat": ["swamp", "#minecraft:is_taiga"],  # Peat Bog (phase 4) joins
    "has_reed": ["#minecraft:is_river", "swamp", "mangrove_swamp"],
    "has_mudstone": ["desert", "#minecraft:is_savanna", "#minecraft:is_badlands"],
    "has_loess": ["plains", "sunflower_plains", "#minecraft:is_savanna"],
    "has_kaolin": ["#minecraft:is_forest", "plains", "meadow", "#minecraft:is_jungle"],
    "has_ochre": ["#minecraft:is_badlands", "#minecraft:is_savanna", "#minecraft:is_river"],  # Ochre Badlands (phase 7) joins
}
for tag, values in BIOMES.items():
    w(D / f"mechanical/tags/worldgen/biome/{tag}.json", {"values": [v if v.startswith("#") else "minecraft:" + v for v in values]})


def place(name, feature, placement, biomes, step):
    w(D / f"mechanical/worldgen/feature/{name}.json", feature)
    w(D / f"mechanical/worldgen/placed_feature/{name}.json", {"feature": M + name, "placement": placement + [{"type": "minecraft:biome"}]})
    w(D / f"mechanical/neoforge/biome_modifier/add_{name}.json", {"type": "neoforge:add_features", "biomes": "#mechanical:" + biomes,
      "features": [M + name], "step": step})


def disk(name, block_, targets, r0, r1, count, biomes):
    """Like vanilla's clay disks: at the bottom of water, spreading onto the banks."""
    place(name, {"type": "minecraft:disk", "half_height": 1, "radius": {"type": "minecraft:uniform", "min_inclusive": r0, "max_inclusive": r1},
          "state_provider": {"id": M + block_}, "target": {"type": "minecraft:matching_blocks", "blocks": targets}},
          [{"type": "minecraft:count", "count": count}, {"type": "minecraft:in_square"}, {"type": "minecraft:heightmap", "heightmap": "OCEAN_FLOOR_WG"},
           {"type": "minecraft:block_predicate_filter", "predicate": {"type": "minecraft:matching_fluids", "fluids": "minecraft:water"}}],
          biomes, "underground_ores")


def ore(name, block_, targets, size, count, lo, hi, biomes):
    """Blobs replacing the given blocks / #tags (dirt under the grass, clay, terracotta) at y lo..hi."""
    place(name, {"type": "minecraft:ore", "size": size, "discard_chance_on_air_exposure": 0.0, "targets": [
          {"state": M + block_, "target": {"predicate_type": "minecraft:tag_match", "tag": t[1:]} if t.startswith("#")
           else {"predicate_type": "minecraft:block_match", "block": t}} for t in targets]},
          [{"type": "minecraft:count", "count": count}, {"type": "minecraft:in_square"}, {"type": "minecraft:height_range", "height": {
              "type": "minecraft:uniform", "min_inclusive": {"absolute": lo}, "max_inclusive": {"absolute": hi}}}], biomes, "underground_ores")


SOIL = ["minecraft:dirt", "minecraft:grass_block", "minecraft:coarse_dirt", "minecraft:mud", "minecraft:clay", "minecraft:sand", "minecraft:gravel"]
disk("disk_peat", "peat", SOIL, 2, 5, 3, "has_peat")
ore("ore_peat", "peat", ["minecraft:dirt", "minecraft:mud", "minecraft:clay"], 48, 3, 54, 70, "has_peat")
disk("disk_mudstone", "mudstone", SOIL + ["minecraft:red_sand"], 2, 4, 3, "has_mudstone")
ore("ore_loess", "loess", ["minecraft:dirt"], 64, 6, 58, 85, "has_loess")
ore("ore_kaolin", "kaolin", ["minecraft:dirt", "minecraft:gravel"], 24, 1, 50, 75, "has_kaolin")
for c in ("red", "yellow"):
    ore(f"ore_{c}_ochre_block", f"{c}_ochre_block", ["minecraft:clay", "minecraft:dirt", "#minecraft:terracotta"], 28, 2, 50, 110, "has_ochre")
place("bog_oak_logs", {"type": "mechanical:bog_oak_logs"}, [{"type": "minecraft:rarity_filter", "chance": 2}, {"type": "minecraft:in_square"},
      {"type": "minecraft:height_range", "height": {"type": "minecraft:uniform", "min_inclusive": {"absolute": 55}, "max_inclusive": {"absolute": 62}}}],
      "has_peat", "underground_decoration")
reed = json.loads(jar.read("data/minecraft/worldgen/feature/sugar_cane.json").decode().replace("minecraft:sugar_cane", M + "reed"))
reed_place = json.loads(jar.read("data/minecraft/worldgen/placed_feature/patch_sugar_cane_swamp.json").decode().replace("minecraft:sugar_cane", M + "reed"))
reed_place = [p for p in reed_place["placement"] if p["type"] != "minecraft:biome"]
reed_place[0]["chance"] = 2
place("patch_reed", reed, reed_place, "has_reed", "vegetal_decoration")

# ---------------------------------------------------------------- lang, preview
lang_path = A / "lang/en_us.json"
all_lang = json.loads(lang_path.read_text(encoding="utf-8"))
all_lang.update(lang)
lang_path.write_text(json.dumps(all_lang, indent=2) + "\n", encoding="utf-8")

for f in ("peat_bricks", "mudstone", "adobe_bricks", "loess_bricks", "kaolin_tiles", "red_ochre_block", "red_ochre_bricks",
          "yellow_ochre_block", "yellow_ochre_bricks"):  # the family rows' textures (gen_stone_families.py)
    preview.append(Image.open(A / f"textures/block/{f}.png").convert("RGBA"))
assert all(img.size == (16, 16) for img in preview), "a 16x16 texture each"
per_row = 12
sheet = Image.new("RGBA", (per_row * 16, (len(preview) + per_row - 1) // per_row * 16), (40, 40, 40, 255))
for i, img in enumerate(preview):
    sheet.alpha_composite(img, (i % per_row * 16, i // per_row * 16))
sheet.resize((sheet.width * 8, sheet.height * 8), Image.NEAREST).save(ROOT / "earth_preview.png")
print(f"earth: {len(blocks)} blocks, {len(preview)} textures")
