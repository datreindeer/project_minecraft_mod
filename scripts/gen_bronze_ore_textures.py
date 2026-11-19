"""Bronze Age phase 1 textures (ores, pan). Ore items come from the user's own workshop library (raw copper / raw tin
chunks and dust piles), recoloured where the mineral differs: Malachite is the raw copper chunk remapped onto banded
greens, Cassiterite the raw tin chunk on a dark resinous brown, the crushed ores are the dust piles speckled with grey
rock grit. The pan and the two loose-find block textures (malachite, cassiterite) are drawn here.
Run: python scripts/gen_bronze_ore_textures.py"""
import random
from pathlib import Path

from PIL import Image, ImageDraw

LIB = Path(r"C:\Users\Yngve\Documents\Minecraft Modding\item\workshop")
TEX = Path(__file__).resolve().parent.parent / "src/main/resources/assets/mechanical/textures"
ITEM, BLOCK = TEX / "item", TEX / "block"

MALACHITE = [(18, 58, 38), (28, 92, 56), (42, 128, 74), (78, 170, 104), (150, 222, 170)]
CASSITERITE = [(20, 14, 12), (44, 30, 24), (70, 48, 36), (104, 74, 54), (176, 150, 128)]
GRIT = [(70, 70, 72), (104, 104, 106), (136, 136, 138)]


def ramp(pal, t):
    t = max(0.0, min(1.0, t)) * (len(pal) - 1)
    i = min(int(t), len(pal) - 2)
    f = t - i
    return tuple(int(pal[i][c] * (1 - f) + pal[i + 1][c] * f) for c in range(3))


def lib(name):
    return Image.open(LIB / name).convert("RGBA")


def recolour(src, pal, band=None):
    """Remap by brightness onto pal; band(x, y) shifts the brightness for stripes."""
    px = [(x, y) for y in range(16) for x in range(16) if src.getpixel((x, y))[3]]
    lums = [sum(src.getpixel(p)[:3]) / 3 for p in px]
    lo, hi = min(lums), max(lums)
    out = Image.new("RGBA", (16, 16))
    for (x, y), lum in zip(px, lums):
        t = (lum - lo) / max(1, hi - lo)
        if band:
            t += band(x, y)
        out.putpixel((x, y), ramp(pal, t) + (src.getpixel((x, y))[3],))
    return out


def gritty(src, seed):
    """Speckle a dust pile with grey rock grit (crushed ore is ore plus gangue)."""
    rng = random.Random(seed)
    out = src.copy()
    for y in range(16):
        for x in range(16):
            a = src.getpixel((x, y))[3]
            if a and rng.random() < 0.28:
                out.putpixel((x, y), rng.choice(GRIT) + (a,))
    return out


# malachite: concentric-looking bands (the mineral's trademark)
malachite_band = lambda x, y: 0.22 if ((x * 2 + y * 3) // 4) % 3 == 0 else (-0.12 if ((x * 2 + y * 3) // 4) % 3 == 1 else 0)
recolour(lib("copper/raw_copper_ore_2.png"), MALACHITE, malachite_band).save(ITEM / "malachite.png")
lib("copper/raw_copper_ore_3.png").save(ITEM / "native_copper.png")
cass = recolour(lib("tin/raw_tin_ore_3.png"), CASSITERITE, lambda x, y: -0.1)
cass.save(ITEM / "cassiterite.png")
gritty(lib("copper/copper_dust.png"), 1).save(ITEM / "crushed_copper_ore.png")
gritty(recolour(lib("tin/tin_dust.png"), CASSITERITE, lambda x, y: 0.15), 2).save(ITEM / "crushed_tin_ore.png")


# ---- pan: a shallow carved wooden bowl seen from above at an angle ----
WOOD = [(58, 38, 20), (92, 62, 34), (126, 88, 50), (156, 114, 68)]
FILL = {"gravel": [(84, 80, 78), (118, 112, 110), (150, 144, 140), (96, 90, 86)],
        "sand": [(196, 178, 128), (218, 204, 150), (232, 222, 176), (180, 160, 112)]}


def pan(fill=None):
    img = Image.new("RGBA", (16, 16))
    d = ImageDraw.Draw(img)
    d.ellipse([0, 4, 15, 14], fill=WOOD[0] + (255,))   # outer rim / shadowed side
    d.ellipse([0, 3, 15, 12], fill=WOOD[2] + (255,))   # rim top
    d.ellipse([2, 4, 13, 11], fill=WOOD[1] + (255,))   # inside wall
    d.ellipse([3, 5, 12, 10], fill=WOOD[3] + (255,))   # bottom, lit
    rng = random.Random(7)
    for y in range(16):  # wood grain
        for x in range(16):
            p = img.getpixel((x, y))
            if p[3] and rng.random() < 0.15:
                img.putpixel((x, y), tuple(max(0, c - 18) for c in p[:3]) + (255,))
    if fill:
        rng = random.Random(11)
        for y in range(5, 11):
            for x in range(3, 13):
                if ((x - 7.5) / 4.6) ** 2 + ((y - 7.5) / 2.9) ** 2 <= 1:
                    img.putpixel((x, y), rng.choice(FILL[fill]) + (255,))
    img.putpixel((1, 7), WOOD[3] + (255,))  # rim highlight
    img.putpixel((2, 5), WOOD[3] + (255,))
    return img


pan().save(ITEM / "pan.png")
pan("gravel").save(ITEM / "pan_with_gravel.png")
pan("sand").save(ITEM / "pan_with_sand.png")


# ---- block textures for the loose finds (native copper uses vanilla raw_copper_block) ----
def stone_tex(pal, band, seed):
    rng = random.Random(seed)
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            img.putpixel((x, y), ramp(pal, 0.45 + band(x, y) + rng.uniform(-0.12, 0.12)) + (255,))
    return img


stone_tex(MALACHITE, lambda x, y: 0.25 if ((x + 2 * y) // 3) % 3 == 0 else (-0.2 if ((x + 2 * y) // 3) % 3 == 1 else 0), 3).save(BLOCK / "malachite.png")
stone_tex(CASSITERITE, lambda x, y: 0.35 if (x * 7 + y * 13) % 17 == 0 else -0.05, 4).save(BLOCK / "cassiterite.png")
print("bronze ore textures ok")
