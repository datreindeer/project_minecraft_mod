"""Textures for the trodden road ground (people.RoadBuilding, worldgen patches in gen_road_ground.py): Packed Sand, Packed
Red Sand and Packed Snow, painted from vanilla's sand, red sand and snow so they sit next to them.

Packed sand: the sand's own grain, a touch darker and greyer (pressed, a little damp), its highlights flattened, two faint
pressed streaks and a few pebbles. Packed snow: snow gone grey-blue and denser, faint boot and hoof prints, a few specks of
dirt kicked up. Standalone: python scripts/gen_road_ground_textures.py (needs the patched jar in build/moddev/artifacts).
"""
import io
import random
import zipfile
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / "src/main/resources/assets/mechanical/textures/block"
JAR = zipfile.ZipFile(next((ROOT / "build/moddev/artifacts").glob("minecraft-patched-*[0-9].jar")))


def vanilla(name):
    return Image.open(io.BytesIO(JAR.read(f"assets/minecraft/textures/block/{name}.png"))).convert("RGBA")


def lum(c):
    return 0.299 * c[0] + 0.587 * c[1] + 0.114 * c[2]


def mix(a, b, t):
    return tuple(int(round(a[i] + (b[i] - a[i]) * t)) for i in range(3)) + (255,)


def blobs(rnd, n, r):
    """A soft 16x16 tileable field 0..1: a few round blobs (wrapping at the edges)."""
    f = [[0.0] * 16 for _ in range(16)]
    for _ in range(n):
        cx, cy, rr = rnd.uniform(0, 16), rnd.uniform(0, 16), rnd.uniform(r * 0.6, r)
        for y in range(16):
            for x in range(16):
                dx = min(abs(x - cx), 16 - abs(x - cx))
                dy = min(abs(y - cy), 16 - abs(y - cy))
                f[y][x] = max(f[y][x], max(0.0, 1 - (dx * dx + dy * dy) ** 0.5 / rr))
    return f


def packed(src, warm, seed, pebbles):
    rnd = random.Random(seed)
    img = src.copy()
    px = img.load()
    mean = sum(lum(px[x, y]) for x in range(16) for y in range(16)) / 256
    damp = blobs(rnd, 3, 6)
    for y in range(16):
        for x in range(16):
            c = px[x, y]
            # pressed: the bright grains flattened toward the mean, a shade darker, a little damp in soft patches
            l = lum(c)
            if l > mean:
                c = mix(c, tuple(int(v * mean / max(l, 1)) for v in c[:3]), 0.45)
            k = 0.97 - 0.02 * damp[y][x]
            px[x, y] = mix(tuple(int(v * k) for v in c[:3]) + (255,), warm, 0.2)  # warm, so darkening never turns it olive
    # a few small pebbles, low contrast, never in a row
    for _ in range(pebbles):
        x, y = rnd.randrange(16), rnd.randrange(16)
        c = px[x, y]
        px[x, y] = mix(c, (120, 112, 100), 0.3)
    return img


def packed_snow(seed):
    rnd = random.Random(seed)
    img = vanilla("snow")
    px = img.load()
    trod = blobs(rnd, 4, 5)
    for y in range(16):
        for x in range(16):
            c = px[x, y]
            t = trod[y][x]
            # compressed: greyer and bluer, darker where it was trodden most, a pixel of grain here and there
            c = mix(c, (190, 200, 212), 0.18 + 0.1 * t)
            if rnd.random() < 0.04:
                c = mix(c, (170, 180, 192), 0.25)
            px[x, y] = c
    # two specks of kicked-up dirt, soft
    x, y = rnd.randrange(16), rnd.randrange(16)
    px[x, y] = mix(px[x, y], (120, 100, 80), 0.35)
    return img


packed(vanilla("sand"), (200, 168, 120), 11, 2).save(OUT / "packed_sand.png")
packed(vanilla("red_sand"), (160, 96, 60), 12, 2).save(OUT / "packed_red_sand.png")
packed_snow(13).save(OUT / "packed_snow.png")
print("ok")
