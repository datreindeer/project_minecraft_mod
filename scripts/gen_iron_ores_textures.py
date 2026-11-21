"""Iron Age ore textures (IronOres.java). From the user's workshop library: Galena Ore is the silver ore (stone with veins)
with the veins remapped onto lead-grey galena, Deepslate Galena Ore the same veins laid on vanilla deepslate, Raw Galena
the raw silver chunk in galena greys, Lead Ingot the silver ingot in dull blue-grey, Silver Ingot the library's own,
Flux the library's flux dust in pale lime, Salt the silver dust pile in white. Remapped from vanilla: Silver Nugget (iron
nugget, in the silver ingot's shades), Limestone (stone, warm cream), Rock Salt (calcite, pinkish white), Salted Meat / Fish (raw beef / cod, paler, salt
specks). The Leaded Cupel is drawn here: a white bone-ash dish with a grey lead button.
Run: python scripts/gen_iron_ores_textures.py"""
import glob
import io
import random
import zipfile
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
LIB = Path(r"C:\Users\Yngve\Documents\Minecraft Modding\item")
TEX = ROOT / "src/main/resources/assets/mechanical/textures"
JAR = sorted(glob.glob(str(ROOT / "build/moddev/artifacts/minecraft-patched-*-merged.jar")))
assert JAR, "no merged minecraft jar (run a gradle build first)"
jar = zipfile.ZipFile(JAR[0])

GALENA = [(30, 33, 40), (58, 63, 72), (92, 98, 108), (138, 145, 156), (196, 202, 212)]
LEAD = [(36, 38, 48), (62, 66, 80), (90, 95, 110), (122, 128, 142), (166, 172, 186)]
FLUX = [(120, 116, 98), (160, 156, 136), (196, 192, 172), (222, 219, 204), (242, 240, 230)]
SALT = [(150, 146, 148), (192, 188, 190), (222, 220, 222), (240, 239, 240), (255, 255, 255)]
LIMESTONE = [(122, 114, 96), (150, 142, 122), (176, 168, 146), (198, 190, 168), (218, 212, 192)]
ROCK_SALT = [(168, 150, 150), (198, 180, 178), (220, 206, 204), (236, 226, 224), (250, 244, 242)]


def ramp(pal, t):
    t = max(0.0, min(1.0, t)) * (len(pal) - 1)
    i = min(int(t), len(pal) - 2)
    f = t - i
    return tuple(int(pal[i][c] * (1 - f) + pal[i + 1][c] * f) for c in range(3))


def recolour(src, pal, mask=None):
    """Every opaque pixel (or those in mask) mapped by brightness onto pal."""
    px = [(x, y) for y in range(16) for x in range(16) if src.getpixel((x, y))[3] and (mask is None or (x, y) in mask)]
    lums = [sum(src.getpixel(p)[:3]) / 3 for p in px]
    lo, hi = min(lums), max(lums)
    out = src.copy()
    for (x, y), lum in zip(px, lums):
        out.putpixel((x, y), ramp(pal, (lum - lo) / max(1, hi - lo)) + (src.getpixel((x, y))[3],))
    return out


def lib(name):
    return Image.open(LIB / name).convert("RGBA")


def vanilla(name):
    return Image.open(io.BytesIO(jar.read(f"assets/minecraft/textures/{name}.png"))).convert("RGBA").crop((0, 0, 16, 16))


def coloured(img):
    """Pixels that aren't grey stone: the ore's veins."""
    out = set()
    for y in range(16):
        for x in range(16):
            r, g, b, a = img.getpixel((x, y))
            if a and max(r, g, b) - min(r, g, b) > 14:
                out.add((x, y))
    return out


# ---- galena: the silver ore's veins in galena greys, on stone and on deepslate ----
silver_ore = lib("workshop/silver/silver_ore_1.png")
veins = coloured(silver_ore)
galena = recolour(silver_ore, GALENA, veins)
galena.save(TEX / "block/galena_ore.png")
deep = vanilla("block/deepslate")
for p in veins:
    deep.putpixel(p, galena.getpixel(p))
deep.save(TEX / "block/deepslate_galena_ore.png")

recolour(lib("workshop/silver/raw_silver_ore.png"), GALENA).save(TEX / "item/raw_galena.png")
recolour(lib("workshop/silver/silver_ingot.png"), LEAD).save(TEX / "item/lead_ingot.png")
lib("workshop/silver/silver_ingot.png").save(TEX / "item/silver_ingot.png")
ingot = lib("workshop/silver/silver_ingot.png")
shades = sorted({ingot.getpixel((x, y))[:3] for y in range(16) for x in range(16) if ingot.getpixel((x, y))[3]}, key=sum)
SILVER = [shades[round(i * (len(shades) - 1) / 4)] for i in range(5)]  # the ingot's own shades, so nugget and ingot match
recolour(vanilla("item/iron_nugget"), SILVER).save(TEX / "item/silver_nugget.png")
recolour(lib("flux_dust.png"), FLUX).save(TEX / "item/flux.png")
recolour(lib("workshop/silver/silver_dust2.png"), SALT).save(TEX / "item/salt.png")
recolour(vanilla("block/stone"), LIMESTONE).save(TEX / "block/limestone.png")
recolour(vanilla("block/calcite"), ROCK_SALT).save(TEX / "block/rock_salt.png")


# ---- salted food: the raw food paled, with white salt specks on it ----
def salted(src, seed):
    rng = random.Random(seed)
    out = src.copy()
    for y in range(16):
        for x in range(16):
            r, g, b, a = out.getpixel((x, y))
            if not a:
                continue
            grey = (r + g + b) // 3
            c = tuple(int(v * 0.6 + grey * 0.25 + 40) for v in (r, g, b))  # paler, duller: cured
            if rng.random() < 0.12:
                c = (236, 236, 232)
            out.putpixel((x, y), tuple(min(255, v) for v in c) + (a,))
    return out


salted(vanilla("item/beef"), 3).save(TEX / "item/salted_meat.png")
salted(vanilla("item/cod"), 5).save(TEX / "item/salted_fish.png")

# ---- leaded cupel: a shallow white dish (side view from above) with a grey lead button in it ----
DISH = [(150, 146, 138), (196, 192, 184), (226, 222, 214), (244, 242, 236)]
img = Image.new("RGBA", (16, 16))
for y in range(16):
    for x in range(16):
        dx, dy = (x - 7.5) / 6.5, (y - 9.5) / 3.6
        if dx * dx + dy * dy <= 1:
            rim = dx * dx + dy * dy > 0.55
            light = (x + y) < 15
            c = DISH[3 if rim and light else 2 if rim else 1 if light else 0]
            # the hollow
            if not rim:
                c = DISH[1] if dx < 0.2 else DISH[0]
            img.putpixel((x, y), c + (255,))
        ldx, ldy = (x - 7.5) / 3.0, (y - 9.0) / 1.7
        if ldx * ldx + ldy * ldy <= 1:
            img.putpixel((x, y), ramp(LEAD, 0.9 - 0.5 * (ldx + ldy + 1)) + (255,))
body = img.copy()
for y in range(16):  # dark outline round the dish
    for x in range(16):
        if body.getpixel((x, y))[3]:
            continue
        if any(0 <= x + a < 16 and 0 <= y + b < 16 and body.getpixel((x + a, y + b))[3] for a, b in ((1, 0), (-1, 0), (0, 1), (0, -1))):
            img.putpixel((x, y), (70, 66, 60, 255))
img.save(TEX / "item/leaded_cupel.png")

for f in ["block/galena_ore", "block/deepslate_galena_ore", "block/limestone", "block/rock_salt", "item/raw_galena", "item/lead_ingot",
          "item/silver_ingot", "item/silver_nugget", "item/flux", "item/salt", "item/salted_meat", "item/salted_fish", "item/leaded_cupel"]:
    im = Image.open(TEX / f"{f}.png")
    assert im.size == (16, 16) and im.getbbox(), f
print("iron ore textures ok")
