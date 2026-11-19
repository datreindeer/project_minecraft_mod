"""Clay bucket textures from vanilla's bucket / water bucket: the iron is remapped by brightness onto a clay ramp
(raw grey clay for the unfired one, red-brown fired clay for the others), the water pixels are kept, and a few
darker specks give the fired clay some texture. Run: python scripts/gen_clay_bucket_textures.py"""
import random
import sys
import zipfile
from io import BytesIO
from pathlib import Path

from PIL import Image

sys.path.insert(0, str(Path(__file__).resolve().parent))
from gen_crude_ranged_textures import JAR  # noqa: E402

OUT = Path(__file__).resolve().parent.parent / "src/main/resources/assets/mechanical/textures/item"
jar = zipfile.ZipFile(JAR)
load = lambda n: Image.open(BytesIO(jar.read(f"assets/minecraft/textures/item/{n}.png"))).convert("RGBA")
bucket, water = load("bucket"), load("water_bucket")

RAW = [(64, 70, 82), (106, 112, 126), (142, 149, 164), (168, 175, 190), (196, 202, 214)]     # clay ball greys
FIRED = [(74, 34, 22), (122, 58, 36), (160, 82, 52), (186, 106, 70), (212, 140, 100)]        # terracotta


def ramp(pal, lum):
    t = max(0.0, min(1.0, lum)) * (len(pal) - 1)
    i = min(int(t), len(pal) - 2)
    f = t - i
    return tuple(int(pal[i][c] * (1 - f) + pal[i + 1][c] * f) for c in range(3))


def recolour(src, pal, keep=None, specks=False):
    rng = random.Random(3)
    lums = [sum(src.getpixel((x, y))[:3]) / 3 for y in range(16) for x in range(16) if src.getpixel((x, y))[3] and not (keep and keep(x, y))]
    lo, hi = min(lums), max(lums)
    out = Image.new("RGBA", src.size)
    for y in range(16):
        for x in range(16):
            r, g, b, a = src.getpixel((x, y))
            if not a:
                continue
            if keep and keep(x, y):
                out.putpixel((x, y), (r, g, b, a))
                continue
            lum = ((r + g + b) / 3 - lo) / max(1, hi - lo)
            if specks and 0.25 < lum < 0.9 and rng.random() < 0.12:
                lum -= 0.18
            out.putpixel((x, y), ramp(pal, lum) + (a,))
    return out


is_water = lambda x, y: water.getpixel((x, y)) != bucket.getpixel((x, y)) and water.getpixel((x, y))[2] > water.getpixel((x, y))[0] + 30  # blue, not the rim
recolour(bucket, RAW).save(OUT / "unfired_clay_bucket.png")
recolour(bucket, FIRED, specks=True).save(OUT / "clay_bucket.png")
recolour(water, FIRED, keep=is_water, specks=True).save(OUT / "clay_water_bucket.png")
print("clay bucket textures ok")
