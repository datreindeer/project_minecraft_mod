"""Textures for the ceramics (gen_pottery.py, gen_pots.py): fired terracotta and raw grey greenware for the Storage Jar
and Water Jug, the Potter's Wheel's plank head, the wet clay on the wheel, and the raw greenware of vanilla's Flower Pot
and Decorated Pot.

The pots' side textures are a wrap of the profile: texture row r is model height 16 - r, so each round slice of the
model takes its own rows (belly, shoulder, neck, rim, lid). Columns are shaded darker towards the face edges so the
crossed boxes read as round, rows carry faint throwing rings, and the fired pots have grog specks and a painted band
(jar: a zigzag between two lines on the shoulder; jug: two thin lines). Tops are drawn by radius from the centre.
Run: python scripts/gen_pottery_textures.py"""
import math
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
oak = Image.open(BytesIO(jar.read("assets/minecraft/textures/block/oak_planks.png"))).convert("RGBA")

RAW = [(78, 84, 96), (112, 118, 132), (142, 149, 164), (166, 173, 188), (192, 198, 210)]      # dry grey greenware (clay ball greys)
FIRED = [(84, 38, 24), (128, 60, 37), (164, 84, 52), (190, 110, 72), (214, 142, 100)]         # terracotta (clay bucket palette)
WET = [(58, 62, 72), (84, 90, 102), (108, 115, 128), (132, 140, 154), (176, 184, 198)]         # wet clay, darker with a sheen
PAINT = (46, 24, 18)                                                                           # iron-oxide brown-black slip
INSIDE = [(18, 10, 8), (40, 22, 16)]
WATER = [(28, 52, 104), (44, 86, 160), (78, 128, 204), (150, 196, 236)]


def ramp(pal, t):
    t = max(0.0, min(1.0, t)) * (len(pal) - 1)
    i = min(int(t), len(pal) - 2)
    f = t - i
    return tuple(round(pal[i][c] * (1 - f) + pal[i + 1][c] * f) for c in range(3)) + (255,)


def side(pal, seed, paint=None):
    """Profile wrap. `paint(x, row)` -> True where the painted band goes."""
    rng = random.Random(seed)
    img = Image.new("RGBA", (16, 16))
    for r in range(16):
        ring = 0.05 if r % 2 else -0.03                      # throwing rings
        height = 0.08 * (1 - r / 15)                         # a little lighter towards the top (light from above)
        for x in range(16):
            round_ = -0.30 * ((x - 7.5) / 7.5) ** 2          # darker at the face edges: reads as round
            t = 0.62 + ring + height + round_ + rng.uniform(-0.04, 0.04)
            if pal is FIRED and rng.random() < 0.06:
                t -= 0.22                                    # grog specks
            c = ramp(pal, t)
            if paint and paint(x, r):
                k = 0.75 + 0.35 * (t - 0.4)
                c = tuple(round(PAINT[i] * k) for i in range(3)) + (255,)
            img.putpixel((x, r), c)
    return img


def radial(pal, seed, inner=None, inner_r=0.0, rings=True):
    """A top seen from above: concentric rings; inside `inner_r` the `inner(t)` colour (the dark mouth, water)."""
    rng = random.Random(seed)
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - 7.5, y - 7.5)
            if inner and d < inner_r:
                img.putpixel((x, y), inner(d / inner_r, x, y, rng))
                continue
            t = 0.66 + (0.06 if rings and int(d) % 2 else 0) - 0.03 * d / 8 + rng.uniform(-0.04, 0.04)
            if inner and d < inner_r + 1:
                t -= 0.12                                    # the lip rolls over into the mouth
            if pal is FIRED and rng.random() < 0.05:
                t -= 0.2
            img.putpixel((x, y), ramp(pal, t))
    return img


dark = lambda t, x, y, rng: ramp(INSIDE, t * 0.8 + rng.uniform(0, 0.15))


def water(full):
    def f(t, x, y, rng):
        c = ramp(WATER, (0.75 if full else 0.35) - 0.3 * t + rng.uniform(-0.05, 0.05))
        if full and (x, y) in ((6, 6), (7, 6), (6, 7)):
            c = WATER[3] + (255,)                              # a glint of sky
        return c
    return f


# jar: zigzag between two lines on the shoulder (rows 6-9 = model y 7-10), a line low on the belly (row 12)
def jar_paint(x, r):
    zig = {7: (0,), 8: (1, 3), 9: (2,)}                       # a zigzag 4 px long, 3 rows high
    return r in (6, 10, 12) or x % 4 in zig.get(r, ())


jug_paint = lambda x, r: r in (9, 11)   # two thin lines round the shoulder (model y 5, 7)

for pal, prefix, seed in [(FIRED, "", 1), (RAW, "unfired_", 2)]:
    painted = pal is FIRED
    side(pal, seed, jar_paint if painted else None).save(OUT / f"{prefix}storage_jar_side.png")
    radial(pal, seed + 10).save(OUT / f"{prefix}storage_jar_top.png")
    lid = radial(pal, seed + 20)
    for x, y in [(7, 7), (8, 7), (7, 8), (8, 8)]:              # the knob's top, a shade lighter
        lid.putpixel((x, y), ramp(pal, 0.85))
    lid.save(OUT / f"{prefix}storage_jar_lid.png")
    radial(pal, seed + 30, dark, 2.6).save(OUT / f"{prefix}storage_jar_mouth.png")
    side(pal, seed + 40, jug_paint if painted else None).save(OUT / f"{prefix}water_jug_side.png")
    radial(pal, seed + 50).save(OUT / f"{prefix}water_jug_top.png")
    radial(pal, seed + 60, dark, 1.9).save(OUT / f"{prefix}water_jug_mouth.png")
radial(FIRED, 71, water(False), 1.9).save(OUT / "water_jug_mouth_low.png")
radial(FIRED, 72, water(True), 1.9).save(OUT / "water_jug_mouth_full.png")

# ---- Potter's Wheel head: a disc of oak boards with a darker worn rim and the axle peg in the middle ----
top = Image.new("RGBA", (16, 16))
for y in range(16):
    for x in range(16):
        r, g, b, a = oak.getpixel((x, y))
        d = math.hypot(x - 7.5, y - 7.5)
        k = 1.0 if d < 4.5 else 0.88 if d < 5.6 else 0.74     # clay-stained, worn rim
        if d < 1.2:
            k = 0.55                                          # the axle peg
        top.putpixel((x, y), (round(r * k), round(g * k), round(b * k), 255))
top.save(OUT / "potters_wheel_head_top.png")
edge = Image.new("RGBA", (16, 16))
for y in range(16):
    for x in range(16):
        r, g, b, a = oak.getpixel((x, (y * 3) % 16))
        k = 0.8 - 0.15 * ((x - 7.5) / 7.5) ** 2
        edge.putpixel((x, y), (round(r * k), round(g * k), round(b * k), 255))
edge.save(OUT / "potters_wheel_head_side.png")

# ---- wet clay on the wheel: a slick, darker grey with spiral ridges and a sheen ----
rng = random.Random(9)
clay = Image.new("RGBA", (16, 16))
for r in range(16):
    for x in range(16):
        t = 0.55 + (0.12 if r % 3 == 0 else -0.04) - 0.25 * ((x - 7.5) / 7.5) ** 2 + rng.uniform(-0.03, 0.03)
        if x in (6, 7) and r % 3 != 2:
            t += 0.22                                         # wet sheen catching the light
        clay.putpixel((x, r), ramp(WET, t))
clay.save(OUT / "wheel_clay.png")
radial(WET, 91).save(OUT / "wheel_clay_top.png")
radial(WET, 92, lambda t, x, y, rng: ramp(WET, 0.1 + 0.15 * t), 2.6).save(OUT / "wheel_clay_mouth.png")
# ---- greenware for vanilla's pots (gen_pots.py): Unfired Flower Pot and Unfired Decorated Pot ----
# flower pot: vanilla's texture (its model's UV layout) recoloured by brightness into the raw grey clay
pot = Image.open(BytesIO(jar.read("assets/minecraft/textures/block/flower_pot.png"))).convert("RGBA")
lums = [0.3 * r + 0.59 * g + 0.11 * b for r, g, b, a in (pot.getpixel((x, y)) for y in range(pot.height) for x in range(pot.width)) if a]
lo, hi = min(lums), max(lums)
rng = random.Random(81)
green_pot = Image.new("RGBA", pot.size)
for y in range(pot.height):
    for x in range(pot.width):
        r, g, b, a = pot.getpixel((x, y))
        if a:
            t = 0.25 + 0.6 * ((0.3 * r + 0.59 * g + 0.11 * b) - lo) / max(1, hi - lo) + rng.uniform(-0.03, 0.03)
            green_pot.putpixel((x, y), ramp(RAW, t))
green_pot.save(OUT / "unfired_flower_pot.png")
# decorated pot: the body wrap (14 px wide on each face), a top and the rim's open mouth
side(RAW, 82).save(OUT / "unfired_decorated_pot_side.png")
radial(RAW, 83).save(OUT / "unfired_decorated_pot_top.png")
radial(RAW, 84, dark, 2.4).save(OUT / "unfired_decorated_pot_mouth.png")
print("pottery textures ok")
