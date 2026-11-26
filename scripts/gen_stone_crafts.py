"""Stone crafts (StoneCrafts.java; world-materials plan phase 9A, docs/ages/).

- Soapstone Molds: one per ClayMoldBlock.Shape, the clay mold's models with Polished Soapstone sides and its cavity top
  recoloured to soapstone (the metal fill textures are shared with the clay molds). Carved at the Mold Bench (Java).
- Quartzite Whetstone, Rapakivi Quern, Basalt Anvil: the Whetstone / Quern / Stone Anvil models with the stone's textures.
- Alabaster Lamp: the Oil Lamp's models with the dish recoloured to alabaster.
- Pumice Chunk: item (vanilla flint recoloured).
- Recipes: the three stone blocks, the lamp, and Jasper + Steel Plate -> Flint and Steel.
Writes blockstates, models, item definitions, loot, tags, lang and textures.
Generator order: after gen_stone_families.py, gen_copper.py, gen_bronze*.py, gen_fauna_coast.py, gen_moldbench.py
(it copies their models). Run: python scripts/gen_stone_crafts.py
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

# ClayMoldBlock.Shape, in order
SHAPES = ["ingot", "pickaxe_head", "axe_head", "shovel_head", "hoe_head", "knife_blade", "saw_blade", "plate", "spear_head",
          "sword_blade", "kettle", "arrowheads", "bucket", "sickle_blade", "shears", "fish_hooks", "needles", "tongs",
          "chisel_blade", "hammer_head", "anvil", "tool_rod"]
SOAPSTONE = [(74, 88, 80), (94, 110, 100), (116, 130, 118), (138, 150, 138), (160, 170, 156)]  # gen_stone_families.py
ALABASTER = [(200, 188, 160), (216, 206, 182), (230, 222, 202), (240, 234, 218), (250, 246, 236)]
PUMICE = [(150, 148, 140), (172, 170, 162), (192, 190, 182), (210, 208, 200), (226, 224, 218)]


def w(path, obj):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, indent=2) + "\n", encoding="utf-8")


def r(path):
    return json.loads(path.read_text(encoding="utf-8"))


def tag_add(rel, values):
    path = D / rel
    tag = r(path) if path.exists() else {"values": []}
    tag["values"] += [v for v in values if v not in tag["values"]]
    w(path, tag)


def ramp(palette, t):
    t = max(0.0, min(1.0, t)) * (len(palette) - 1)
    i = min(int(t), len(palette) - 2)
    f = t - i
    return tuple(int(palette[i][c] * (1 - f) + palette[i + 1][c] * f) for c in range(3))


def recolour(img, palette):
    """The texture recoloured by brightness onto the palette (its own brightness range)."""
    img = img.convert("RGBA")
    px = list(img.getdata())
    lum = [sum(p[:3]) / 3 for p in px if p[3]]
    lo, hi = min(lum), max(lum)
    img.putdata([ramp(palette, (sum(p[:3]) / 3 - lo) / (hi - lo or 1)) + (p[3],) if p[3] else p for p in px])
    return img


def loot(block):
    w(D / f"mechanical/loot_table/blocks/{block}.json", {"type": "minecraft:block", "pools": [{"rolls": 1,
      "condition": {"type": "minecraft:survives_explosion"}, "entries": [{"type": "minecraft:item", "name": M + block}]}],
      "random_sequence": f"mechanical:blocks/{block}"})


def copy_block(src, dst, textures, models=None):
    """Blockstate, models (src and src_* listed in models) and item of src as dst, with some textures swapped."""
    for m in models or [src]:
        model = r(A / f"models/block/{m}.json")
        model["textures"] = {k: textures.get(k, v) for k, v in model["textures"].items()}
        w(A / f"models/block/{m.replace(src, dst, 1)}.json", model)
    w(A / f"blockstates/{dst}.json", json.loads((A / f"blockstates/{src}.json").read_text(encoding="utf-8")
                                                .replace(f"mechanical:block/{src}", f"mechanical:block/{dst}")))
    item = r(A / f"items/{src}.json")
    w(A / f"items/{dst}.json", json.loads(json.dumps(item).replace(f"mechanical:block/{src}", f"mechanical:block/{dst}")))
    loot(dst)


def recipe(name, obj):
    w(D / f"mechanical/recipe/{name}.json", obj)


lang_path = A / "lang/en_us.json"
lang = r(lang_path)
title = lambda s: s.replace("_", " ").title()
preview = []

# ---- soapstone molds: never crack, so no uses to keep on the item ----
for s in SHAPES:
    clay, soap = f"clay_{s}_mold", f"soapstone_{s}_mold"
    top = recolour(Image.open(A / f"textures/block/{clay}_top.png"), SOAPSTONE)
    top.save(A / f"textures/block/{soap}_top.png")
    preview.append(top)
    tex = {"particle": M + "block/polished_soapstone", "side": M + "block/polished_soapstone", "top": f"{M}block/{soap}_top"}
    models = [p.stem for p in (A / "models/block").glob(f"{clay}*.json")]
    assert clay in models, clay
    copy_block(clay, soap, tex, models)
    lang[f"block.mechanical.{soap}"] = f"Soapstone {title(s)} Mold"
tag_add("minecraft/tags/block/mineable/pickaxe.json", [f"{M}soapstone_{s}_mold" for s in SHAPES])

# ---- the stone tools ----
copy_block("whetstone", "quartzite_whetstone", {"particle": M + "block/polished_quartzite", "stone": M + "block/polished_quartzite",
                                                "top": M + "block/quartzite"})
copy_block("quern", "rapakivi_quern", {"particle": M + "block/rapakivi_granite", "side": M + "block/rapakivi_granite",
                                       "top": M + "block/smooth_rapakivi_granite"})
copy_block("stone_anvil", "basalt_anvil", {"particle": M + "block/polished_coastal_basalt_top", "top": M + "block/polished_coastal_basalt_top",
                                           "side": M + "block/coastal_basalt_side", "bottom": M + "block/smooth_coastal_basalt"})
dish = recolour(Image.open(A / "textures/block/oil_lamp.png"), ALABASTER)
dish.save(A / "textures/block/alabaster_lamp.png")
preview.append(dish)
copy_block("oil_lamp", "alabaster_lamp", {"particle": M + "block/alabaster_lamp", "stone": M + "block/alabaster_lamp"},
           ["oil_lamp", "oil_lamp_filled", "oil_lamp_lit"])
tag_add("minecraft/tags/block/mineable/pickaxe.json", [M + b for b in ("quartzite_whetstone", "rapakivi_quern", "basalt_anvil", "alabaster_lamp")])
lang.update({"block.mechanical.quartzite_whetstone": "Quartzite Whetstone", "block.mechanical.rapakivi_quern": "Rapakivi Quern",
             "block.mechanical.basalt_anvil": "Basalt Anvil", "block.mechanical.alabaster_lamp": "Alabaster Lamp"})

recipe("quartzite_whetstone", {"type": "minecraft:crafting_shaped", "category": "equipment", "pattern": [" S ", "TPT"],
       "key": {"S": M + "quartzite", "T": "minecraft:stick", "P": "#minecraft:planks"}, "result": {"id": M + "quartzite_whetstone"}})
recipe("rapakivi_quern", {"type": "minecraft:crafting_shaped", "category": "misc", "pattern": [" C ", "#S#", "###"],
       "key": {"C": ["minecraft:copper_ingot", M + "bronze_ingot"], "#": M + "rapakivi_granite", "S": "minecraft:stick"},
       "result": {"id": M + "rapakivi_quern"}})
recipe("basalt_anvil", {"type": "minecraft:crafting_shaped", "category": "misc", "pattern": [" A ", "BBB"],
       "key": {"A": M + "stone_anvil", "B": M + "coastal_basalt"}, "result": {"id": M + "basalt_anvil"}})
recipe("alabaster_lamp", {"type": "minecraft:crafting_shaped", "category": "building", "pattern": ["AWA", "AAA"],
       "key": {"A": M + "alabaster", "W": M + "plant_fibre"}, "result": {"id": M + "alabaster_lamp"}})
# jasper strikes sparks off steel like flint (Tinder.java's fires take the Flint and Steel)
recipe("flint_and_steel_from_jasper", {"type": "minecraft:crafting_shapeless", "category": "equipment",
       "ingredients": [[M + "rough_jasper", M + "jasper"], M + "steel_plate"], "result": {"id": "mechanical:flint_and_steel", "count": 1}})

# ---- pumice chunk ----
chunk = recolour(Image.open(io.BytesIO(jar.read("assets/minecraft/textures/item/flint.png"))), PUMICE)
chunk.save(A / "textures/item/pumice_chunk.png")
preview.append(chunk)
w(A / "models/item/pumice_chunk.json", {"parent": "minecraft:item/generated", "textures": {"layer0": M + "item/pumice_chunk"}})
w(A / "items/pumice_chunk.json", {"model": {"type": "minecraft:model", "model": M + "item/pumice_chunk"}})
lang["item.mechanical.pumice_chunk"] = "Pumice Chunk"

lang_path.write_text(json.dumps(lang, indent=2) + "\n", encoding="utf-8")

sheet = Image.new("RGBA", (len(preview) * 18, 16), (60, 60, 60, 255))
for i, img in enumerate(preview):
    sheet.alpha_composite(img.resize((16, 16)), (i * 18, 0))
sheet.resize((sheet.width * 8, sheet.height * 8), Image.NEAREST).save(ROOT / "stone_crafts_preview.png")
print(f"stone crafts ok: {len(SHAPES)} soapstone molds")
