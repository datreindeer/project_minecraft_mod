"""Nugget, ingot and block textures for every metal in the table (scripts/gen_metals.py) that has a `solid` colour, so
the three always share one palette (docs/metals.md, inconsistency 18). The palette is a ramp grown from that one colour:
darker (and a little cooler) below it, towards white above. Shapes: vanilla's iron nugget, the user's library ingot
(workshop/_ingot.png; `ingot_shape="crude"` takes the rough _crude_ingot.png, as Pig Iron), and vanilla's iron block. Only
mechanical: items are drawn (vanilla copper and gold keep theirs). A new metal gets all three from its table row.
Run: python scripts/gen_metal_textures.py   (a texture script: run by hand when the table's colours or items change)"""
import glob
import io
import colorsys
import random
import zipfile
from pathlib import Path

from PIL import Image

from gen_metals import METALS

ROOT = Path(__file__).resolve().parent.parent
LIB = Path(r"C:\Users\Yngve\Documents\Minecraft Modding\item\workshop")
TEX = ROOT / "src/main/resources/assets/mechanical/textures"
JAR = sorted(glob.glob(str(ROOT / "build/moddev/artifacts/minecraft-patched-*-merged.jar")))
assert JAR, "no merged minecraft jar (run a gradle build first)"
jar = zipfile.ZipFile(JAR[0])
BODY = 0.6  # where the table colour sits on the ramp
BLOCK_BAND = (0.5, 0.88)  # the part of the ramp a block uses


def vanilla(name):
    return Image.open(io.BytesIO(jar.read(f"assets/minecraft/textures/{name}.png"))).convert("RGBA").crop((0, 0, 16, 16))


def shades(c, n):
    """n tones from deep shadow to highlight, the table colour at BODY: darker and a little cooler below it, brighter
    and paler above it (keeping some of its hue, so bronze stays bronze and doesn't wash out to grey)."""
    h, s, v = colorsys.rgb_to_hsv(*(((c >> k) & 255) / 255 for k in (16, 8, 0)))
    out = []
    for i in range(n):
        t = i / (n - 1)
        if t <= BODY:
            f = 0.3 + 0.7 * t / BODY
            rgb = colorsys.hsv_to_rgb((h + 0.03 * (1 - f)) % 1 if s > 0.15 else h, min(1, s * (1 + 0.25 * (1 - f))), v * f)
        else:
            k = (t - BODY) / (1 - BODY)
            rgb = colorsys.hsv_to_rgb(h, s * (1 - 0.3 * k), v + (min(1, v * 1.45) - v) * k + (1 - min(1, v * 1.45)) * k * k * 0.6)
        out.append(tuple(round(x * 255) for x in rgb))
    return out


# brightness of the vanilla iron ingot: the scale nuggets are read on (vanilla's iron nugget sits on it as its ingot does)
LUM = lambda p: (p[0] * 3 + p[1] * 6 + p[2]) / 10


def recolour(src, colour, rank=False, band=None):
    """Each pixel's brightness becomes a tone of the ramp: read on the vanilla iron ingot's scale, or (rank) each of the
    shape's own brightness levels one tone (the library ingot: its levels are the ingot's palette), or (band) the shape's
    own range spread over that part of the ramp (a block: just above the body colour and soft, so a wall of dark lead or pig
    iron doesn't turn into a grid of black seams)."""
    levels = sorted({LUM(src.getpixel((x, y))) for y in range(16) for x in range(16) if src.getpixel((x, y))[3]})
    pal = shades(colour, 16)
    out = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            p = src.getpixel((x, y))
            if p[3]:
                lum = LUM(p)
                if rank:
                    t = levels.index(lum) / (len(levels) - 1)
                elif band:
                    t = band[0] + (lum - levels[0]) / (levels[-1] - levels[0]) * (band[1] - band[0])
                else:
                    t = (lum - SCALE[0]) / (SCALE[1] - SCALE[0])
                out.putpixel((x, y), pal[max(0, min(15, round(t * 15)))] + (p[3],))
    return out, pal


def dents(img, pal, n):
    """Soft metal (lead) takes knocks: a dark pit with a lit lower-right rim, spread over the face."""
    for x, y in [(3, 3), (11, 6), (6, 11), (12, 12)][:n]:
        for dx, dy, i in ((0, 0, 8), (1, 0, 10), (0, 1, 10), (1, 1, 15)):
            img.putpixel((x + dx, y + dy), pal[i] + (255,))


# ingot marks (table key `marks`): colour, share of pixels, every n-th row
MARKS = {"sand": ((120, 104, 82), 0.14, 1),     # casting sand burnt onto a pig's skin
         "fibres": ((50, 62, 54), 0.3, 3)}      # wrought iron's slag fibres, drawn out in rows by the hammer


def marks(img, kind, seed):
    colour, share, every = MARKS[kind]
    rng = random.Random(seed)
    for y in range(16):
        for x in range(16):
            px = img.getpixel((x, y))
            if px[3] and y % every == 0 and LUM(px) > 40 and rng.random() < share:
                img.putpixel((x, y), tuple((a + b) // 2 for a, b in zip(px[:3], colour)) + (255,))


_ing = vanilla("item/iron_ingot")
_l = [LUM(_ing.getpixel((x, y))) for y in range(16) for x in range(16) if _ing.getpixel((x, y))[3]]
SCALE = (min(_l), max(_l))


def mine(item):
    return item and item.startswith("mechanical:") and item.split(":")[1]


def main():
    done = set()
    for m in METALS:
        if not m.get("solid"):
            continue
        c = m["solid"]
        if (n := mine(m.get("nugget"))) and n not in done:
            recolour(vanilla("item/iron_nugget"), c)[0].save(TEX / f"item/{n}.png")
            done.add(n)
        if (i := mine(m.get("ingot"))) and i not in done:
            img, _ = recolour(Image.open(LIB / ("_crude_ingot.png" if m.get("ingot_shape") == "crude" else "_ingot.png")).convert("RGBA"), c, rank=True)
            if m.get("marks"):
                marks(img, m["marks"], 3911)
            img.save(TEX / f"item/{i}.png")
            done.add(i)
        if (b := mine(m.get("block"))) and b not in done:
            img, pal = recolour(vanilla("block/iron_block"), c, band=BLOCK_BAND)
            if m.get("dents"):
                dents(img, pal, m["dents"])
            img.save(TEX / f"block/{b}.png")
            done.add(b)
    print("metal textures:", ", ".join(sorted(done)))


if __name__ == "__main__":
    main()
