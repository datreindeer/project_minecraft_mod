"""Bog Iron textures. Raw Bog Iron is the user's raw copper chunk (workshop library) remapped onto rusty limonite browns;
Bog Iron Ore is drawn here: dark peaty mud with rust-orange nodules and ochre crust round them.
Run: python scripts/gen_bog_iron_textures.py"""
import random
from pathlib import Path

from PIL import Image

LIB = Path(r"C:\Users\Yngve\Documents\Minecraft Modding\item\workshop")
TEX = Path(__file__).resolve().parent.parent / "src/main/resources/assets/mechanical/textures"

LIMONITE = [(38, 20, 12), (74, 38, 18), (116, 60, 26), (160, 90, 38), (204, 138, 66)]
MUD = [(44, 38, 36), (54, 47, 44), (62, 54, 50), (72, 63, 57)]
CRUST = (150, 104, 48)


def ramp(pal, t):
    t = max(0.0, min(1.0, t)) * (len(pal) - 1)
    i = min(int(t), len(pal) - 2)
    f = t - i
    return tuple(int(pal[i][c] * (1 - f) + pal[i + 1][c] * f) for c in range(3))


def recolour(src, pal):
    px = [(x, y) for y in range(16) for x in range(16) if src.getpixel((x, y))[3]]
    lums = [sum(src.getpixel(p)[:3]) / 3 for p in px]
    lo, hi = min(lums), max(lums)
    out = Image.new("RGBA", (16, 16))
    for (x, y), lum in zip(px, lums):
        out.putpixel((x, y), ramp(pal, (lum - lo) / max(1, hi - lo)) + (src.getpixel((x, y))[3],))
    return out


recolour(Image.open(LIB / "copper/raw_copper_ore_2.png").convert("RGBA"), LIMONITE).save(TEX / "item/raw_bog_iron.png")

# the ore block: mud speckle, then nodules (2-3 px blobs, lit from the top left) with an ochre rim; tiles seamlessly
rng = random.Random(7)
img = Image.new("RGBA", (16, 16))
for y in range(16):
    for x in range(16):
        img.putpixel((x, y), rng.choice(MUD) + (255,))
NODULES = [(2, 2, 3), (9, 1, 2), (12, 6, 3), (4, 8, 2), (8, 11, 3), (1, 13, 2), (13, 13, 2)]
for cx, cy, r in NODULES:
    blob = {((cx + dx) % 16, (cy + dy) % 16): (dx, dy) for dy in range(r) for dx in range(r) if not (r == 3 and dx in (0, 2) and dy in (0, 2))}
    for (x, y) in blob:
        for nx, ny in ((x + 1, y), (x - 1, y), (x, y + 1), (x, y - 1)):
            if (nx % 16, ny % 16) not in blob and rng.random() < 0.6:
                img.putpixel((nx % 16, ny % 16), CRUST + (255,))
    for (x, y), (dx, dy) in blob.items():
        img.putpixel((x, y), ramp(LIMONITE, 0.85 - 0.3 * (dx + dy) / max(1, 2 * (r - 1))) + (255,))
img.save(TEX / "block/bog_iron_ore.png")
print("bog iron textures ok")
