"""Masonry (Masonry.java; docs/world-materials-plan.md phase 10): Mosaic Tiles, Fired Roof Tiles and Tiled Roofs.

The block families themselves (warm/cool/checkered_mosaic_tiles, [colour_]roof_tiles + slab/stairs/wall) and the weathered
Cracked/Mossy Bricks of every rock family are rows in gen_stone_families.py; this script paints the mosaic and roof tile
textures over that script's recoloured bricks, and writes the Unfired/Fired Roof Tile items, the sloped Tiled Roofs (Bark Roof
models, as Reed Thatch Roof in gen_earth.py), the recipes (mosaics from four stones, roof tiles, pigment colours) and lang.
Roof tiles are stone for the shelter warmth (gen_insulation.py leaves the tiled roofs out of its bark roofs).

Generator order: ... gen_stone_families.py, gen_earth.py, gen_masonry.py, gen_insulation.py, ... gen_block_items.py,
gen_unlocks.py, gen_advancements.py, gen_loot_format.py. Run: python scripts/gen_masonry.py (writes masonry_preview.png in the
repo root, not committed)
"""
import json
import random
import sys
from pathlib import Path

from PIL import Image

sys.path.insert(0, str(Path(__file__).resolve().parent))
import gen_bark_roof as roof  # noqa: E402

ROOT = Path(__file__).resolve().parent.parent
RES = ROOT / "src/main/resources"
A, D = RES / "assets/mechanical", RES / "data"
M = "mechanical:"
TEX = A / "textures/block"
preview = []


def w(path, obj):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, indent=2) + "\n", encoding="utf-8")


def tag_add(rel, values):
    path = D / rel
    tag = json.loads(path.read_text(encoding="utf-8")) if path.exists() else {"values": []}
    tag["values"] += [v for v in values if v not in tag["values"]]
    w(path, tag)


def recipe(name, obj):
    w(D / f"mechanical/recipe/{name}.json", obj)


def shade(c, f):
    return tuple(max(0, min(255, int(v * f))) for v in c) + (255,)


# ---------------------------------------------------------------- Mosaic Tiles: 3x3 tesserae in 1px grout, rows offset
MOSAICS = {  # name: (grout, tessera colours from the families' palettes, the four stones of the recipe)
    "warm": ((196, 186, 168), [(152, 72, 48), (190, 174, 170), (206, 170, 124), (206, 166, 72), (176, 82, 52)],
             ["polished_jasper", "polished_quartzite", "cut_travertine", "yellow_ochre_bricks"]),
    "cool": ((176, 180, 178), [(78, 87, 102), (66, 106, 72), (232, 232, 233), (116, 130, 118), (96, 106, 122)],
             ["polished_slate", "polished_serpentine", "smooth_marble", "polished_soapstone"]),
    "checkered": ((128, 126, 122), None,  # black and white in a checkerboard
                  ["smooth_marble", "smooth_alabaster", "polished_alum_shale", "polished_coastal_basalt"]),
}
for name, (grout, colours, stones) in MOSAICS.items():
    rnd = random.Random(name)
    img = Image.new("RGBA", (16, 16), grout + (255,))
    for row in range(4):
        for col in range(5):
            x0, y0 = col * 4 - (2 if row % 2 else 0), row * 4
            c = ((40, 40, 44) if (col + row) % 2 else (236, 236, 234)) if colours is None else rnd.choice(colours)
            for y in range(y0, y0 + 3):
                for x in range(x0, x0 + 3):
                    if 0 <= x < 16:
                        img.putpixel((x, y), shade(c, rnd.uniform(0.9, 1.08) * (1.06 if y == y0 else 0.94 if y == y0 + 2 else 1)))
    img.save(TEX / f"{name}_mosaic_tiles.png")
    preview.append(img)
    recipe(f"{name}_mosaic_tiles", {"type": "minecraft:crafting_shapeless", "category": "building",
                                    "ingredients": [M + s for s in stones], "result": {"id": f"{M}{name}_mosaic_tiles", "count": 4}})

# ---------------------------------------------------------------- Fired Roof Tiles: overlapping rows of curved tiles
ROOFS = {  # colour prefix: (dark .. light, pigment)
    "": ([(110, 44, 26), (146, 62, 36), (174, 82, 48), (196, 104, 64), (214, 128, 84)], None),
    "red_": ([(78, 18, 16), (108, 28, 24), (136, 40, 32), (160, 54, 42), (182, 72, 56)], "red_pigment"),
    "black_": ([(18, 18, 20), (30, 30, 34), (44, 44, 48), (60, 60, 64), (80, 80, 84)], "black_pigment"),
    "yellow_": ([(126, 90, 28), (156, 116, 38), (184, 142, 52), (206, 166, 70), (224, 190, 96)], "yellow_pigment"),
    "white_": ([(150, 146, 136), (184, 180, 170), (210, 206, 198), (230, 228, 222), (246, 244, 240)], "white_pigment"),
}


def roof_tiles(pal, seed):
    rnd = random.Random(seed)
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        row, ly = divmod(y, 4)
        for x in range(16):
            col = (x + (row % 2) * 2) % 4
            i = 0 if ly == 3 else 1 if col == 0 else 4 if col in (1, 2) and ly == 0 else 3 if col == 1 else 2
            img.putpixel((x, y), shade(pal[i], rnd.uniform(0.94, 1.05)))
    return img


def tile_item(pal, seed):
    """A curved tile seen from above: wider at the bottom, light ridge down the middle, dark edges."""
    rnd = random.Random(seed)
    img = Image.new("RGBA", (16, 16))
    for y in range(2, 15):
        half = 3 + (y - 2) // 4
        for x in range(8 - half, 8 + half):
            d = abs(x + 0.5 - 8) / half
            i = 4 if d < 0.3 else 3 if d < 0.6 else 2 if d < 0.85 else 0
            img.putpixel((x, y), shade(pal[1 if y == 14 and i > 1 else i], rnd.uniform(0.95, 1.05)))
    return img


for c, (pal, pigment) in ROOFS.items():
    img = roof_tiles(pal, c + "roof_tiles")
    img.save(TEX / f"{c}roof_tiles.png")
    preview.append(img)
    rid = f"{c}tiled_roof"
    for shape in ("straight", "inner", "outer", "flat"):
        w(A / f"models/block/{rid}_{shape}.json", {"parent": f"{M}block/bark_roof_{shape}",
          "textures": {"top": f"{M}block/{c}roof_tiles", "under": f"{M}block/{c}roof_tiles"}})
    variants = {}
    for flat in ("false", "true"):
        for facing in roof.ROT:
            for shape in ("straight", "inner_left", "inner_right", "outer_left", "outer_right"):
                if flat == "true":
                    m = {"model": f"{M}block/{rid}_flat"}
                else:  # as gen_bark_roof.py
                    turn = roof.CW[facing] if shape == "outer_right" else roof.CCW[facing] if shape == "inner_left" else facing
                    m = {"model": f"{M}block/{rid}_{shape.split('_')[0]}"}
                    if roof.ROT[turn]:
                        m["y"] = roof.ROT[turn]
                variants[f"facing={facing},flat={flat},shape={shape}"] = m
    w(A / f"blockstates/{rid}.json", {"variants": variants})
    w(A / f"items/{rid}.json", {"model": {"type": "minecraft:model", "model": f"{M}block/{rid}_straight"}})
    w(D / f"mechanical/loot_table/blocks/{rid}.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [
        {"type": "minecraft:item", "name": M + rid}], "condition": {"type": "minecraft:survives_explosion"}}],
        "random_sequence": f"mechanical:blocks/{rid}"})
    tag_add("minecraft/tags/block/mineable/pickaxe.json", [M + rid])
    if pigment:  # 8 of the plain block around a pigment, like dyeing terracotta
        for block in ("roof_tiles", "tiled_roof"):
            recipe(f"{c}{block}", {"type": "minecraft:crafting_shaped", "category": "building", "pattern": ["###", "#P#", "###"],
                                   "key": {"#": M + block, "P": M + pigment}, "result": {"id": f"{M}{c}{block}", "count": 8}})

recipe("unfired_roof_tile", {"type": "minecraft:crafting_shaped", "category": "misc", "pattern": ["##"],
                             "key": {"#": "minecraft:clay_ball"}, "result": {"id": M + "unfired_roof_tile"}})
recipe("roof_tiles", {"type": "minecraft:crafting_shaped", "category": "building", "pattern": ["##", "##"],
                      "key": {"#": M + "fired_roof_tile"}, "result": {"id": M + "roof_tiles"}})
recipe("tiled_roof", {"type": "minecraft:crafting_shaped", "category": "building", "pattern": ["# ", "##"],
                      "key": {"#": M + "fired_roof_tile"}, "result": {"id": M + "tiled_roof", "count": 4}})
items = {"unfired_roof_tile": [(108, 112, 124), (132, 138, 150), (152, 158, 172), (168, 174, 188), (184, 190, 204)],
         "fired_roof_tile": ROOFS[""][0]}
for item, pal in items.items():
    img = tile_item(pal, item)
    (A / "textures/item").mkdir(parents=True, exist_ok=True)
    img.save(A / f"textures/item/{item}.png")
    preview.append(img)
    w(A / f"items/{item}.json", {"model": {"type": "minecraft:model", "model": M + "item/" + item}})
    w(A / f"models/item/{item}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": M + "item/" + item}})

lang_path = A / "lang/en_us.json"
lang = json.loads(lang_path.read_text(encoding="utf-8"))
lang.update({"item.mechanical.unfired_roof_tile": "Unfired Roof Tile", "item.mechanical.fired_roof_tile": "Fired Roof Tile"})
lang.update({f"block.mechanical.{c}tiled_roof": f"{c.replace('_', ' ').title()}Tiled Roof" for c in ROOFS})
lang_path.write_text(json.dumps(lang, indent=2) + "\n", encoding="utf-8")

# preview: the weathered bricks of every rock family (bricks, cracked, mossy), then mosaics, roof tiles and the items
rows = []
for f in sorted(TEX.glob("mossy_*_bricks.png")):
    stone = f.stem[len("mossy_"):-len("_bricks")]
    row = [TEX / f"{stone}_bricks.png", TEX / f"cracked_{stone}_bricks.png", f]
    if all(p.exists() for p in row):
        rows.append([Image.open(p).convert("RGBA") for p in row])
rows.append(preview)
sheet = Image.new("RGBA", (max(map(len, rows)) * 17, len(rows) * 17), (40, 40, 40, 255))
for y, row in enumerate(rows):
    for x, img in enumerate(row):
        sheet.paste(img, (x * 17, y * 17))
sheet.resize((sheet.width * 8, sheet.height * 8), Image.NEAREST).save(ROOT / "masonry_preview.png")
print("masonry ok:", len(rows) - 1, "weathered families")
