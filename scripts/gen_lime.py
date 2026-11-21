"""Lime and mortar (IronOres.java; docs/ages/03-iron-age.md "Kalk och murbruk", world-materials-plan phase 2).

Quicklime (lime stones burnt in the Clay Kiln at 900°, KilnBlockEntity.RECIPES) + water -> Slaked Lime -> Lime Mortar
(the doubled bricks recipes are written by gen_stone_families.py) or Lime Plaster, dyed with the four pigments. Slaked
Lime also limes a Scraped Hide into Leather. Writes the item and plaster block files, recipes, loot, tags, lang and the
textures (vanilla sugar / bone meal / clay ball / white concrete powder recoloured), and lime_preview.png in the repo root
(not committed). Generator order: right after gen_stone_families.py. Run: python scripts/gen_lime.py
"""
import glob
import io
import json
import zipfile
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
RES = ROOT / "src/main/resources"
A, D = RES / "assets/mechanical", RES / "data"
M = "mechanical:"
jar = zipfile.ZipFile(sorted(glob.glob(str(ROOT / "build/moddev/artifacts/minecraft-patched-*-merged.jar")))[0])
WATER = ["mechanical:clay_water_bucket", "mechanical:bronze_water_bucket", "minecraft:water_bucket"]  # as dough.json

# item: (vanilla texture, palette dark -> light, English name)
ITEMS = {
    "quicklime": ("item/sugar", [(150, 146, 136), (186, 182, 172), (214, 211, 202), (234, 232, 224)], "Quicklime"),
    "slaked_lime": ("item/bone_meal", [(176, 178, 180), (208, 210, 212), (232, 234, 236), (250, 251, 252)], "Slaked Lime"),
    "lime_mortar": ("item/clay_ball", [(112, 106, 96), (146, 140, 128), (178, 172, 160), (204, 198, 186)], "Lime Mortar"),
}
# plaster block: (palette, pigment it is dyed with or None, English name)
PLASTERS = {
    "lime_plaster": ([(196, 190, 176), (214, 209, 196), (228, 224, 212), (240, 237, 228)], None, "Lime Plaster"),
    "white_lime_plaster": ([(214, 216, 216), (230, 232, 232), (242, 243, 243), (252, 252, 252)], "white", "White Lime Plaster"),
    "black_lime_plaster": ([(34, 33, 34), (46, 45, 46), (58, 57, 58), (72, 70, 70)], "black", "Black Lime Plaster"),
    "red_lime_plaster": ([(132, 52, 38), (152, 64, 46), (170, 78, 56), (186, 94, 70)], "red", "Red Lime Plaster"),
    "yellow_lime_plaster": ([(176, 132, 52), (196, 152, 66), (212, 170, 84), (226, 188, 104)], "yellow", "Yellow Lime Plaster"),
}


def write(path, obj):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, indent=2) + "\n", encoding="utf-8")


def tag_add(rel, values):
    path = D / rel
    tag = json.loads(path.read_text(encoding="utf-8")) if path.exists() else {"values": []}
    tag["values"] += [v for v in values if v not in tag["values"]]
    write(path, tag)


def ramp(palette, t):
    t = max(0.0, min(1.0, t)) * (len(palette) - 1)
    i = min(int(t), len(palette) - 2)
    f = t - i
    return tuple(int(palette[i][c] * (1 - f) + palette[i + 1][c] * f) for c in range(3))


def recolour(texture, palette):
    """A vanilla texture recoloured by brightness onto the palette (its own brightness range)."""
    img = Image.open(io.BytesIO(jar.read(f"assets/minecraft/textures/{texture}.png"))).convert("RGBA")
    lum = [sum(p[:3]) / 3 for p in img.getdata() if p[3]]
    lo, hi = min(lum), max(lum)
    img.putdata([ramp(palette, (sum(p[:3]) / 3 - lo) / (hi - lo or 1)) + (p[3],) if p[3] else p for p in img.getdata()])
    return img


def recipe(name, obj):
    write(D / f"mechanical/recipe/{name}.json", obj)


lang_path = A / "lang/en_us.json"
lang = json.loads(lang_path.read_text(encoding="utf-8"))
preview = []

for item, (tex, pal, name) in ITEMS.items():
    write(A / f"items/{item}.json", {"model": {"type": "minecraft:model", "model": M + "item/" + item}})
    write(A / f"models/item/{item}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": M + "item/" + item}})
    img = recolour(tex, pal)
    img.save(A / f"textures/item/{item}.png")
    preview.append(img)
    lang[f"item.mechanical.{item}"] = name

for block, (pal, pigment, name) in PLASTERS.items():
    write(A / f"blockstates/{block}.json", {"variants": {"": {"model": M + "block/" + block}}})
    write(A / f"models/block/{block}.json", {"parent": "minecraft:block/cube_all", "textures": {"all": M + "block/" + block}})
    write(D / f"mechanical/loot_table/blocks/{block}.json", {"type": "minecraft:block", "pools": [{"rolls": 1,
          "condition": {"type": "minecraft:survives_explosion"}, "entries": [{"type": "minecraft:item", "name": M + block}]}],
          "random_sequence": f"mechanical:blocks/{block}"})
    img = recolour("block/white_concrete_powder", pal)
    img.save(A / f"textures/block/{block}.png")
    preview.append(img)
    lang[f"block.mechanical.{block}"] = name
    if pigment:  # 8 plaster around a pigment, like dyeing terracotta
        recipe(block, {"type": "minecraft:crafting_shaped", "category": "building", "pattern": ["PPP", "PDP", "PPP"],
                       "key": {"P": M + "lime_plaster", "D": f"{M}{pigment}_pigment"}, "result": {"id": M + block, "count": 8}})
tag_add("minecraft/tags/block/mineable/pickaxe.json", [M + b for b in PLASTERS])

# lime: slaked with water (the bucket comes back), then mortar or plaster
recipe("slaked_lime", {"type": "minecraft:crafting_shapeless", "category": "misc", "ingredients": [M + "quicklime", WATER],
                       "result": {"id": M + "slaked_lime", "count": 1}})
recipe("lime_mortar", {"type": "minecraft:crafting_shapeless", "category": "misc",
                       "ingredients": [M + "slaked_lime", "#minecraft:sand", "minecraft:gravel"], "result": {"id": M + "lime_mortar", "count": 4}})
recipe("lime_plaster", {"type": "minecraft:crafting_shapeless", "category": "building",
                        "ingredients": [M + "slaked_lime", "#minecraft:sand", "#minecraft:sand", M + "straw"],
                        "result": {"id": M + "lime_plaster", "count": 4}})
# liming: the old tanner's lime pit, like salt-curing (gen_iron_ores.py leather_from_salt)
recipe("leather_from_slaked_lime", {"type": "minecraft:crafting_shapeless", "category": "misc",
                                    "ingredients": [M + "scraped_hide", M + "slaked_lime"], "result": {"id": "minecraft:leather", "count": 1}})

lang_path.write_text(json.dumps(lang, indent=2) + "\n", encoding="utf-8")

# preview: items, plaster, and the three lime stones' base textures (written by gen_stone_families.py)
for s in ("dolomite", "polished_dolomite", "dolomite_bricks", "travertine_top", "cut_travertine", "shell_bank_top", "cut_shell_bank"):
    f = A / f"textures/block/{s}.png"
    if f.exists():
        preview.append(Image.open(f).convert("RGBA"))
sheet = Image.new("RGBA", (len(preview) * 18, 16), (60, 60, 60, 255))
for i, img in enumerate(preview):
    sheet.alpha_composite(img.resize((16, 16)), (i * 18, 0))
sheet.resize((sheet.width * 10, sheet.height * 10), Image.NEAREST).save(ROOT / "lime_preview.png")
print("lime ok")
