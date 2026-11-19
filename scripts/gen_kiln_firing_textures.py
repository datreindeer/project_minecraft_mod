"""Clay Kiln firing stages: the raw clay (vanilla clay texture) bakes in patches into red-brown fired clay.
kiln_clay_1 / kiln_clay_2 are part-fired (patches spreading from the bottom up, where the heat is), kiln_clay_fired
is fully fired with a little scorching. Run: python scripts/gen_kiln_firing_textures.py"""
import random
import sys
import zipfile
from io import BytesIO
from pathlib import Path

from PIL import Image

sys.path.insert(0, str(Path(__file__).resolve().parent))
from gen_crude_ranged_textures import JAR  # noqa: E402

OUT = Path(__file__).resolve().parent.parent / "src/main/resources/assets/mechanical/textures/block"
jar = zipfile.ZipFile(JAR)
clay = Image.open(BytesIO(jar.read("assets/minecraft/textures/block/clay.png"))).convert("RGBA")
terracotta = Image.open(BytesIO(jar.read("assets/minecraft/textures/block/terracotta.png"))).convert("RGBA")
W, H = clay.size

rng = random.Random(7)
# smooth-ish value noise: coarse 4x4 grid, bilinear, plus fine jitter
coarse = [[rng.random() for _ in range(5)] for _ in range(5)]


def noise(x, y):
    gx, gy = x / W * 4, y / H * 4
    x0, y0 = int(gx), int(gy)
    fx, fy = gx - x0, gy - y0
    a = coarse[y0][x0] * (1 - fx) + coarse[y0][x0 + 1] * fx
    b = coarse[y0 + 1][x0] * (1 - fx) + coarse[y0 + 1][x0 + 1] * fx
    return a * (1 - fy) + b * fy


FINE = [[rng.random() * 0.18 for _ in range(W)] for _ in range(H)]


def fired_colour(x, y):
    """Terracotta tone, shaded by the clay texture's own light/dark so the grain carries over."""
    cr, cg, cb, _ = clay.getpixel((x, y))
    tr, tg, tb, _ = terracotta.getpixel((x, y))
    shade = (cr + cg + cb) / 3 / 160  # vanilla clay averages ~160
    scorch = 0.85 if FINE[y][x] > 0.155 else 1.0  # a few dark scorch specks
    return tuple(max(0, min(255, int(c * shade * scorch))) for c in (tr, tg, tb))


def stage(amount):
    img = Image.new("RGBA", (W, H))
    for y in range(H):
        for x in range(W):
            # heat rises from the fire below: the bottom of each face fires first
            heat = noise(x, y) * 0.6 + (y / (H - 1)) * 0.4 + FINE[y][x]
            t = heat - (1 - amount) * 1.2  # > 0: fired; a 0.15 wide fringe blends
            k = max(0.0, min(1.0, t / 0.15 + 0.5)) if amount < 1 else 1.0
            c = clay.getpixel((x, y))
            f = fired_colour(x, y)
            img.putpixel((x, y), tuple(int(c[i] * (1 - k) + f[i] * k) for i in range(3)) + (255,))
    return img


stage(0.4).save(OUT / "kiln_clay_1.png")
stage(0.75).save(OUT / "kiln_clay_2.png")
stage(1.0).save(OUT / "kiln_clay_fired.png")
print("kiln firing textures ok")
