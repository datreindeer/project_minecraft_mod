"""Textures for Elkwight phases 1-2 (docs/elkwight-plan.md): Charred Log (vanilla dark oak log remapped onto charcoal
blacks with grey ash in the cracks), Wood Ash (drawn), Rune Blackstone (vanilla polished blackstone with carved runes) and
its glow layer (only the rune strokes, bright green; the block model draws it full bright).
Standalone, needs the patched jar in build/moddev/artifacts. Run: python scripts/gen_elkwight_textures.py"""
import glob
import io
import random
import zipfile
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
TEX = ROOT / "src/main/resources/assets/mechanical/textures/block"
jar = zipfile.ZipFile(sorted(glob.glob(str(ROOT / "build/moddev/artifacts/minecraft-patched-*-merged.jar")))[0])


def vanilla(name):
    return Image.open(io.BytesIO(jar.read(f"assets/minecraft/textures/block/{name}.png"))).convert("RGBA")


def ramp(pal, t):
    t = max(0.0, min(1.0, t)) * (len(pal) - 1)
    i = min(int(t), len(pal) - 2)
    f = t - i
    return tuple(int(pal[i][c] * (1 - f) + pal[i + 1][c] * f) for c in range(3))


def remap(src, pal, ash=None, seed=1):
    """Luminance of src onto pal; the darkest pixels (the cracks) become grey ash with chance ash."""
    rng = random.Random(seed)
    lums = [sum(src.getpixel((x, y))[:3]) / 3 for y in range(16) for x in range(16)]
    lo, hi = min(lums), max(lums)
    out = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            t = (sum(src.getpixel((x, y))[:3]) / 3 - lo) / max(1, hi - lo)
            c = ramp(pal, t)
            if ash and t < 0.22 and rng.random() < ash:
                c = rng.choice([(92, 88, 84), (110, 105, 100), (78, 74, 70)])
            out.putpixel((x, y), c + (255,))
    return out


CHAR = [(14, 12, 12), (24, 21, 20), (36, 32, 30), (50, 45, 42), (66, 60, 55)]
remap(vanilla("dark_oak_log"), CHAR, ash=0.45, seed=3).save(TEX / "charred_log.png")
top = remap(vanilla("dark_oak_log_top"), CHAR, seed=4)
for x, y in ((7, 7), (8, 8), (7, 8), (8, 7)):  # a burnt-out heart
    top.putpixel((x, y), (8, 7, 7, 255))
top.save(TEX / "charred_log_top.png")

# Wood Ash: soft pale grey powder (smoother and lighter than gravel), a few charcoal flecks and white specks
rng = random.Random(11)
ash = Image.new("RGBA", (16, 16))
for y in range(16):
    for x in range(16):
        v = 128 + rng.randint(-6, 6) + (6 if (x * 7 + y * 3) % 5 == 0 else 0)
        c = (v, v - 2, v - 5)
        r = rng.random()
        if r < 0.03:
            c = (58, 54, 50)
        elif r < 0.08:
            c = (108, 104, 100)
        elif r < 0.12:
            c = (160, 158, 154)
        ash.putpixel((x, y), c + (255,))
ash.save(TEX / "wood_ash.png")

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
