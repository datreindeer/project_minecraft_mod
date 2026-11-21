"""Stone chemistry, writing and plaster (Chemistry.java; world-materials-plan phase 9B, docs/world-materials-plan.md).

Alum Shale roasted in the Clay Kiln (800°, KilnBlockEntity.RECIPES) -> Roasted Alum Shale, leached with water -> 2 Alum.
Alum: 2 pigment + Alum -> 4 dye (a mordant), 2 Scraped Hides + Alum -> 2 Leather (tawing). Gypsum crushed (Java) ->
Gypsum Powder, burnt at 300° -> Plaster of Paris, + 2 Sand + Straw -> 4 Lime Plaster. Polished Slate + 2 Sticks -> Slate
Tablet. Writes item textures (vanilla recoloured, the tablet drawn), models, recipes and lang.
Generator order: after gen_lime.py and gen_pigments.py. Run: python scripts/gen_chemistry.py
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
WATER = ["mechanical:clay_water_bucket", "mechanical:bronze_water_bucket", "minecraft:water_bucket"]  # as gen_lime.py

# item: (vanilla texture, palette dark -> light, English name); None = drawn below
ITEMS = {
    "roasted_alum_shale": ("item/raw_iron", [(40, 22, 18), (82, 40, 28), (126, 64, 40), (164, 96, 62)], "Roasted Alum Shale"),
    "alum": ("item/quartz", [(150, 138, 160), (196, 186, 206), (226, 220, 234), (248, 246, 252)], "Alum"),
    "gypsum_powder": ("item/glowstone_dust", [(170, 162, 146), (204, 198, 184), (228, 224, 212), (244, 242, 234)], "Gypsum Powder"),
    "plaster_of_paris": ("item/gunpowder", [(190, 192, 194), (222, 224, 226), (240, 241, 242), (254, 254, 254)], "Plaster of Paris"),
    "slate_tablet": (None, None, "Slate Tablet"),
}


def write(path, obj):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, indent=2) + "\n", encoding="utf-8")


def ramp(palette, t):
    t = max(0.0, min(1.0, t)) * (len(palette) - 1)
    i = min(int(t), len(palette) - 2)
    f = t - i
    return tuple(int(palette[i][c] * (1 - f) + palette[i + 1][c] * f) for c in range(3))


def recolour(texture, palette):
    """A vanilla texture recoloured by brightness onto the palette (its own brightness range), as gen_lime.py."""
    img = Image.open(io.BytesIO(jar.read(f"assets/minecraft/textures/{texture}.png"))).convert("RGBA")
    lum = [sum(p[:3]) / 3 for p in img.get_flattened_data() if p[3]]
    lo, hi = min(lum), max(lum)
    img.putdata([ramp(palette, (sum(p[:3]) / 3 - lo) / (hi - lo or 1)) + (p[3],) if p[3] else p for p in img.get_flattened_data()])
    return img


def tablet():
    """A dark slate in a wooden frame with three lines of chalk writing."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    x0, x1, y0, y1 = 2, 13, 3, 13
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            if x in (x0, x1) or y in (y0, y1):  # frame, lighter on the top and left
                px[x, y] = (150, 108, 62, 255) if x == x0 or y == y0 else (96, 66, 36, 255)
            else:
                n = (x * 7 + y * 13) % 5  # slate grain
                px[x, y] = (52 + n * 2, 56 + n * 2, 62 + n * 2, 255)
    for y, words in ((6, ((4, 6), (8, 11))), (8, ((4, 8), (10, 11))), (10, ((4, 5), (7, 9)))):  # chalk writing, word by word
        for a, b in words:
            for x in range(a, b + 1):
                px[x, y] = (230, 228, 220, 255) if (x + y) % 3 else (200, 198, 190, 255)
    return img


lang_path = A / "lang/en_us.json"
lang = json.loads(lang_path.read_text(encoding="utf-8"))
preview = []
for item, (tex, pal, name) in ITEMS.items():
    write(A / f"items/{item}.json", {"model": {"type": "minecraft:model", "model": M + "item/" + item}})
    write(A / f"models/item/{item}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": M + "item/" + item}})
    img = recolour(tex, pal) if tex else tablet()
    img.save(A / f"textures/item/{item}.png")
    preview.append(img)
    lang[f"item.mechanical.{item}"] = name
lang.update({
    "message.mechanical.slate_tablet.no_chalk": "Hold Chalk in your other hand to write",
    "tooltip.mechanical.slate_tablet.blank": "Blank. Use with Chalk in the other hand to write; sneak-use wipes it",
})
lang_path.write_text(json.dumps(lang, indent=2) + "\n", encoding="utf-8")


def recipe(name, ingredients, result, count=1, category="misc", group=None):
    obj = {"type": "minecraft:crafting_shapeless", "category": category}
    if group:
        obj["group"] = group
    write(D / f"mechanical/recipe/{name}.json", obj | {"ingredients": ingredients, "result": {"id": result, "count": count}})


# leaching: the roasted shale soaked and the liquor boiled down; the bucket comes back
recipe("alum", [M + "roasted_alum_shale", WATER], M + "alum", 2)
# mordant: alum fixes the colour, so the same pigment dyes twice as much
for colour in ("black", "red", "yellow", "white"):
    p = f"{M}{colour}_pigment"
    recipe(f"{colour}_dye_from_{colour}_pigment_with_alum", [p, p, M + "alum"], f"minecraft:{colour}_dye", 4, group=f"{colour}_dye")
# tawing: one alum for two hides (salt and slaked lime cure one each)
recipe("leather_from_alum", [M + "scraped_hide", M + "scraped_hide", M + "alum"], "minecraft:leather", 2)
# gypsum plaster: Plaster of Paris binds Lime Plaster in place of Slaked Lime (no bellows needed)
recipe("lime_plaster_from_plaster_of_paris", [M + "plaster_of_paris", "#minecraft:sand", "#minecraft:sand", M + "straw"],
       M + "lime_plaster", 4, category="building")
recipe("slate_tablet", [M + "polished_slate", "minecraft:stick", "minecraft:stick"], M + "slate_tablet")

sheet = Image.new("RGBA", (len(preview) * 18, 16), (60, 60, 60, 255))
for i, img in enumerate(preview):
    sheet.alpha_composite(img, (i * 18, 0))
sheet.resize((sheet.width * 10, sheet.height * 10), Image.NEAREST).save(ROOT / "chemistry_preview.png")
print("chemistry ok")
