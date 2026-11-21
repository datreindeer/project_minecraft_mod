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
item("roasted_iron_ore", recolor(vanilla_texture("item/raw_iron"), hue=0.02, sat=1.6, light=0.62), "Roasted Iron Ore")
item("roasted_bog_iron", recolor(mod_texture("item/raw_bog_iron"), hue=0.03, sat=1.3, light=0.7), "Roasted Bog Iron")
for raw, roasted in (("minecraft:raw_iron", "roasted_iron_ore"), (M + "raw_bog_iron", "roasted_bog_iron")):
    w(D / f"mechanical/recipe/{roasted}.json", {"type": "minecraft:campfire_cooking", "category": "misc", "cookingtime": 600,
                                                 "experience": 0.1, "ingredient": raw, "result": {"id": M + roasted}})
lang |= {
    "tooltip.mechanical.iron_bloom.yield": "Gives %s Wrought Iron, %s Slag",
    "message.mechanical.bloomery.flux": "Flux in the hearth: %s/%s (each bloom one ingot richer)",
    "message.mechanical.bloomery.flux_full": "The hearth holds %s charges of flux already",
}

merge(A / "lang/en_us.json", lang)
print("iron age ok")
