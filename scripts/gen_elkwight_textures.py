"""Textures for Elkwight phases 1-2 (docs/elkwight-plan.md): Charred Log (drawn alligator char), Wood Ash and Burnt
Soil (three variants each), Fireweed, Rune Blackstone (vanilla polished blackstone with carved runes) and
its glow layer (only the rune strokes, bright green; the block model draws it full bright).
Standalone, needs the patched jar in build/moddev/artifacts. Run: python scripts/gen_elkwight_textures.py"""
import glob
import io
import math
import random
import zipfile
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
TEX = ROOT / "src/main/resources/assets/mechanical/textures/block"
jar = zipfile.ZipFile(sorted(glob.glob(str(ROOT / "build/moddev/artifacts/minecraft-patched-*-merged.jar")))[0])


def vanilla(name):
    return Image.open(io.BytesIO(jar.read(f"assets/minecraft/textures/block/{name}.png"))).convert("RGBA")


# ---------------------------------------------------------------- tileable noise
def tnoise(seed, cells):
    """Smooth value noise in 0..1 that wraps at 16 px: a cells x cells lattice, smoothstep-interpolated."""
    rng = random.Random(seed)
    g = [[rng.random() for _ in range(cells)] for _ in range(cells)]
    out = [[0.0] * 16 for _ in range(16)]
    for y in range(16):
        for x in range(16):
            fx, fy = x * cells / 16, y * cells / 16
            ix, iy = int(fx), int(fy)
            tx, ty = fx - ix, fy - iy
            tx, ty = tx * tx * (3 - 2 * tx), ty * ty * (3 - 2 * ty)
            a, b = g[iy % cells][ix % cells], g[iy % cells][(ix + 1) % cells]
            c, d = g[(iy + 1) % cells][ix % cells], g[(iy + 1) % cells][(ix + 1) % cells]
            out[y][x] = (a * (1 - tx) + b * tx) * (1 - ty) + (c * (1 - tx) + d * tx) * ty
    return out


def fbm(seed, octaves=((2, 0.45), (4, 0.35), (8, 0.2))):
    layers = [(tnoise(seed * 31 + i, c), w) for i, (c, w) in enumerate(octaves)]
    return [[sum(l[y][x] * w for l, w in layers) for x in range(16)] for y in range(16)]


def clamp(c):
    return tuple(max(0, min(255, int(round(v)))) for v in c)


def mix(a, b, t):
    return tuple(a[i] * (1 - t) + b[i] * t for i in range(3))


def specks(img, rng, count, colours, sizes=(1, 1, 2)):
    """count little flecks (1-3 px clumps) of the given colours, wrapping round the tile edges."""
    for _ in range(count):
        x, y, col = rng.randrange(16), rng.randrange(16), rng.choice(colours)
        for _ in range(rng.choice(sizes)):
            img.putpixel((x % 16, y % 16), clamp(col) + (255,))
            dx, dy = rng.choice(((1, 0), (-1, 0), (0, 1), (0, -1), (1, 0)))  # compact clumps, no diagonal V shapes
            x, y = x + dx, y + dy


# ---------------------------------------------------------------- Charred Log
# The bark burnt to charcoal: "alligator" char, irregular blocky cells a little taller than wide, split by deep black
# cracks, a faint silvery sheen under the cells' upper edges and grey ash caught in a few cracks. Dark and calm, so a
# trunk reads as one black shape from afar instead of salt and pepper.
def charred_bark(seed):
    rng = random.Random(seed)
    cell = [[-1] * 16 for _ in range(16)]
    # columns of cells 3-5 px wide that fill the 16 px, each split into rows 2-5 px tall at its own offset
    widths, x = [], 0
    while x < 16:
        wdt = min(rng.choice((3, 4, 4, 5)), 16 - x)
        if 16 - x - wdt in (1, 2):
            wdt = 16 - x
        widths.append((x, wdt))
        x += wdt
    cid = 0
    for x0, wdt in widths:
        y, off = 0, rng.randrange(16)
        while y < 16:
            hgt = min(rng.choice((3, 4, 4, 5, 6)), 16 - y)
            if 16 - y - hgt == 1:
                hgt += 1
            for yy in range(y, y + hgt):
                for xx in range(x0, x0 + wdt):
                    cell[(yy + off) % 16][xx] = cid
            cid += 1
            y += hgt
    shade = {c: rng.uniform(0.0, 1.0) for c in range(cid)}
    grain = fbm(seed + 7, ((4, 0.6), (8, 0.4)))
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            c = cell[y][x]
            left = cell[y][(x - 1) % 16] != c  # vertical crack on the cell's left edge
            top = cell[(y - 1) % 16][x] != c   # horizontal crack on its top edge
            below = cell[(y + 1) % 16][x] != c
            # the long cracks run down the trunk, broken here and there; the cross cracks are short and shallow
            if left and rng.random() < 0.9 or top and rng.random() < 0.45:
                col = (12, 11, 11) if rng.random() < 0.9 else (52, 50, 48)  # now and then grey with ash
            else:
                col = mix((20, 18, 17), (38, 34, 31), 0.3 * shade[c] + 0.7 * grain[y][x])
                if cell[(y - 2) % 16][x] != c and not top and rng.random() < 0.3:
                    col = mix(col, (64, 62, 61), 0.3)  # a faint sheen just under the upper crack
                if below:
                    col = mix(col, (12, 11, 10), 0.25)
            img.putpixel((x, y), clamp(col) + (255,))
    return img


charred_bark(3).save(TEX / "charred_log.png")
# the cut end: growth rings burnt black, the heart burnt out, a rim of bark char, checks running out from the middle
rng = random.Random(4)
top = Image.new("RGBA", (16, 16))
for y in range(16):
    for x in range(16):
        dx, dy = x - 7.5, y - 7.5
        r = math.hypot(dx, dy)
        if max(abs(dx), abs(dy)) > 6.5:
            col = (16, 14, 13) if rng.random() < 0.7 else (30, 27, 25)  # bark rim
        elif r < 1.3:
            col = (8, 7, 7)  # burnt-out heart
        else:
            col = mix((26, 23, 21), (50, 45, 41), (0.5 + 0.5 * math.cos(r * 2.1)) * 0.7 + rng.random() * 0.3)
        top.putpixel((x, y), clamp(col) + (255,))
for a in (0.4, 1.9, 3.3, 4.8):
    for k in range(2, 7):
        top.putpixel((int(round(7.5 + math.cos(a) * k)), int(round(7.5 + math.sin(a) * k))), (10, 9, 9, 255))
specks(top, rng, 5, [(84, 80, 76)], sizes=(1,))  # a little ash on the cut
top.save(TEX / "charred_log_top.png")


# ---------------------------------------------------------------- Wood Ash
# Soft grey ash over the burnt ground. The fine grain of vanilla sand at a low contrast on a calm mid-to-dark grey, slow
# clouding (paler fine ash, darker damp ash), small black charcoal bits and a few pale flakes. Three variants (picked
# and turned at random per block, see gen_elkwight.py) so the 16 px tile never shows as a grid; they differ a little.
def grain(name, turn):
    """A vanilla texture's luminance as z-scores (mean 0, deviation 1), turned/flipped (turn 0-7) for each variant."""
    src = vanilla(name).convert("RGB")
    if turn & 4:
        src = src.transpose(Image.FLIP_LEFT_RIGHT)
    src = src.rotate(90 * (turn & 3))
    lum = [[sum(src.getpixel((x, y))) / 3 for x in range(16)] for y in range(16)]
    flat = [v for row in lum for v in row]
    mean = sum(flat) / 256
    sd = (sum((v - mean) ** 2 for v in flat) / 256) ** 0.5
    return [[(v - mean) / sd for v in row] for row in lum]


def wood_ash(seed, base, char_bits, turn):
    rng = random.Random(seed)
    g, cloud = grain("sand", turn), fbm(seed, ((2, 0.5), (4, 0.5)))
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            v = g[y][x] * 3.4 + (cloud[y][x] - 0.5) * 5
            img.putpixel((x, y), clamp((base[0] + v, base[1] + v, base[2] + v * 0.9)) + (255,))
    specks(img, rng, char_bits, [(36, 33, 31), (30, 28, 26), (44, 41, 39)], sizes=(1, 1, 2, 3))  # charcoal
    specks(img, rng, char_bits + 2, [(62, 60, 57), (66, 64, 61)], sizes=(1, 1, 2))               # damp, darker ash
    specks(img, rng, 3, [(94, 92, 89), (90, 88, 85)], sizes=(1,))                               # pale flakes
    return img


for i, (base, bits, turn) in enumerate((((77, 75, 72), 3, 0), ((74, 72, 69), 4, 5), ((80, 78, 75), 2, 2))):
    wood_ash(11 + i, base, bits, turn).save(TEX / ("wood_ash.png" if i == 0 else f"wood_ash_{i + 1}.png"))


# ---------------------------------------------------------------- Burnt Soil
# The forest floor after the fire: vanilla dirt's clods turned dark, nearly black-brown and softer, black charcoal bits,
# a dusting of grey ash and the odd pale pebble. Darker than any dirt, warmer than the ash. Three variants like Wood Ash.
def burnt_soil(seed, base, turn):
    rng = random.Random(seed)
    g, cloud = grain("dirt", turn), fbm(seed, ((2, 0.5), (4, 0.5)))
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            v = g[y][x] * 4.6 + (cloud[y][x] - 0.5) * 4
            img.putpixel((x, y), clamp((base[0] + v * 1.1, base[1] + v, base[2] + v * 0.85)) + (255,))
    specks(img, rng, 5, [(20, 18, 17), (25, 23, 21)], sizes=(1, 2, 2, 3))           # charcoal
    specks(img, rng, 6, [(64, 62, 59), (60, 58, 55), (68, 66, 63)], sizes=(1, 1, 1, 2))  # ash dust
    specks(img, rng, 1, [(78, 74, 69)], sizes=(1,))                                 # a pale pebble
    return img


for i, (base, turn) in enumerate((((47, 41, 36), 1), ((45, 39, 35), 6), ((49, 43, 38), 3))):
    burnt_soil(21 + i, base, turn).save(TEX / ("burnt_soil.png" if i == 0 else f"burnt_soil_{i + 1}.png"))


# ---------------------------------------------------------------- Fireweed
# The first flower after a forest fire: two blocks tall, straight reddish stems with narrow willow-like leaves, the top a
# tapering spike of magenta-pink flowers, open below and dark buds at the tip.
LEAF, LEAF_D, STEM = (78, 122, 58), (52, 90, 42), (122, 62, 58)
PINK, PINK_L, PINK_D, BUD = (214, 72, 150), (238, 122, 186), (162, 44, 112), (120, 46, 84)


def fireweed(top_half, seed):
    rng = random.Random(seed)
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))

    def put(x, y, c):
        if 0 <= x < 16 and 0 <= y < 16:
            img.putpixel((x, y), clamp(c) + (255,))

    for sx, drop in ((7, 0), (3, 3), (12, 2)):  # three stems: x, and how far down the top half their spikes start
        y0 = drop + 6 if top_half else 0
        for y in range(y0, 16):
            put(sx, y, STEM if y % 3 else mix(STEM, LEAF_D, 0.5))
        # narrow leaves angled up and out, alternating sides, shorter towards the top
        y, side = y0 + (3 if top_half else 1) + rng.randrange(2), rng.choice((-1, 1))
        while y < 16:  # leaves at uneven spacing, mostly alternating
            n = 2 if top_half else rng.choice((2, 3, 3))
            for k in range(1, n + 1):
                put(sx + side * k, y - (k + 1) // 2, LEAF if k < n else LEAF_D)
            y += rng.choice((3, 4, 4, 5))
            side = -side if rng.random() < 0.8 else side
        if top_half:  # the flower spike: buds at the tip, open flowers below
            for y in range(drop, drop + 7):
                f = (y - drop) / 6
                if f < 0.3:
                    put(sx, y, BUD)
                    if rng.random() < 0.5:
                        put(sx + rng.choice((-1, 1)), y, mix(BUD, PINK_D, 0.5))
                else:
                    put(sx, y, PINK_D)
                    put(sx - 1, y, PINK if (y + sx) % 2 else PINK_L)
                    put(sx + 1, y, PINK_L if (y + sx) % 2 else PINK)
                    if f > 0.6 and rng.random() < 0.6:
                        put(sx + rng.choice((-2, 2)), y, PINK)
    return img


fireweed(False, 31).save(TEX / "fireweed_bottom.png")
fireweed(True, 32).save(TEX / "fireweed_top.png")

# Rune Blackstone: three runes carved into polished blackstone (dark groove with a lit lower edge), glowing green in the glow layer
RUNES = [  # 16x16 masks, '#' = stroke: Tiwaz and Algiz above, Fehu below
    "................",
    "...#.....#.#.#..",
    "..###....#.#.#..",
    ".#.#.#....###...",
    "...#.......#....",
    "...#.......#....",
    "...#.......#....",
    "...#.......#....",
    "................",
    "......#..#......",
    "......#.#.......",
    "......##..#.....",
    "......#..#......",
    "......#.#.......",
    "......##........",
    "......#.........",
]
base = vanilla("polished_blackstone")
glow = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
for y, row in enumerate(RUNES):
    for x, ch in enumerate(row):
        if ch != "#":
            continue
        base.putpixel((x, y), (12, 14, 12, 255))  # the groove
        glow.putpixel((x, y), (96, 255, 128, 255) if (x + y) % 3 else (150, 255, 170, 255))
        for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):  # a soft green halo in the stone round the strokes
            nx, ny = x + dx, y + dy
            if 0 <= nx < 16 and 0 <= ny < 16 and RUNES[ny][nx] != "#" and glow.getpixel((nx, ny))[3] == 0:
                glow.putpixel((nx, ny), (40, 140, 70, 110))
base.save(TEX / "rune_blackstone.png")
glow.save(TEX / "rune_blackstone_glow.png")
print("elkwight textures ok")
