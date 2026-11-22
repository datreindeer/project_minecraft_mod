# Weaving textures (gen_weaving.py): Silk Thread (the mod's Wool Thread recoloured, like the other threads), the three
# Cloths (a folded bolt: Wool undyed oatmeal twill, Linen cream plain weave, Silk golden with a sheen), the Warp-weighted
# Loom's warp (woven cloth at the top, a red band, loose warp threads below) and the Cloth Bed (vanilla's straw bed texture
# repainted: a linen pillow/mattress and a madder-red wool blanket, the straw bindings become dark stitching).
# usage: python scripts/gen_weaving_textures.py [preview.png]   (needs build/moddev/artifacts/minecraft-patched-*.jar)
import io
import sys
import zipfile
from pathlib import Path
from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
TEX = ROOT / "src/main/resources/assets/mechanical/textures"


def vanilla(path):
    jar = next((ROOT / "build/moddev/artifacts").glob("minecraft-patched-*[0-9].jar"))
    with zipfile.ZipFile(jar) as z:
        return Image.open(io.BytesIO(z.read(f"assets/minecraft/textures/{path}"))).convert("RGBA")


def hexc(s): return tuple(int(s[i:i + 2], 16) for i in (0, 2, 4)) + (255,)


def shade(c, f): return tuple(max(0, min(255, round(v * f))) for v in c[:3]) + (255,)


def recolour(src, base):
    lum = lambda c: 0.299 * c[0] + 0.587 * c[1] + 0.114 * c[2]
    mx = max(lum(src.getpixel((x, y))) for y in range(16) for x in range(16) if src.getpixel((x, y))[3])
    out = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y in range(16):
        for x in range(16):
            c = src.getpixel((x, y))
            if c[3]:
                f = 0.55 + 0.5 * lum(c) / mx  # a little more contrast than the other threads: silk shines
                out.putpixel((x, y), shade(base, f)[:3] + (c[3],))
    return out


def weave(kind, x, y):
    """Brightness factor of a woven pixel: twill runs diagonally, plain weave checks, silk adds a soft diagonal sheen."""
    if kind == "twill":
        return 1.06 if (x + y) % 3 == 0 else 0.97 if (x + y) % 3 == 1 else 0.9
    if kind == "plain":
        return 1.04 if (x + y) % 2 == 0 else 0.93
    sheen = 1.18 if abs((x - y) % 12 - 6) < 2 else 1.0
    return sheen * (1.02 if (x + y) % 2 == 0 else 0.96)


# A folded bolt of cloth, seen from the front: a top face and three folded layers. "t" top, "f" front fold, "e" fold
# edge (darker), "h" highlight on each fold's rounded lip, "o" outline.
BOLT = [
    "................",
    "................",
    "................",
    "...oooooooooooo.",
    "..ottttttttttto.",
    ".otttttttttttto.",
    "otttttttttttto..",
    "ohhhhhhhhhhhho..",
    "offfffffffffeo..",
    "oeeeeeeeeeeeeo..",
    "ohhhhhhhhhhhho..",
    "offfffffffffeo..",
    "oeeeeeeeeeeeeo..",
    "ohhhhhhhhhhhho..",
    "offfffffffffeo..",
    ".oooooooooooo...",
]


def bolt(base, kind):
    base = hexc(base)
    out = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    f = {"t": 1.0, "f": 0.86, "e": 0.68, "h": 1.12, "o": 0.42}
    for y, row in enumerate(BOLT):
        for x, ch in enumerate(row):
            if ch != ".":
                out.putpixel((x, y), shade(base, f[ch] * (weave(kind, x, y) if ch in "tf" else 1.0)))
    return out


def warp():
    """Top: woven cloth with a red band; below: pale warp threads on every other column (gaps see-through)."""
    out = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    cloth, band, thread = hexc("d8ccb0"), hexc("9c3a2c"), hexc("e6dcc4")
    for x in range(16):
        for y in range(16):
            if y < 7:
                c = band if y in (3, 4) else cloth
                out.putpixel((x, y), shade(c, weave("plain", x, y)))
            elif y == 7:
                out.putpixel((x, y), shade(cloth, 0.8))  # the fell: where the weaving has got to
            elif x % 2 == 0:
                out.putpixel((x, y), shade(thread, 0.95 if y % 3 else 0.85))
    return out


def bed():
    """Vanilla's straw bed texture recoloured: top rows (the pillow end) linen, the rest a red wool blanket."""
    src = vanilla("block/straw_bed.png")
    out = Image.new("RGBA", src.size, (0, 0, 0, 0))
    linen, wool, stitch = hexc("e2d6b8"), hexc("9a3a2a"), hexc("4a2418")
    for y in range(src.height):
        for x in range(src.width):
            r, g, b, a = src.getpixel((x, y))
            if not a:
                continue
            if r > g + 40 and r > b + 60 and g < 110:  # the red binding cords
                out.putpixel((x, y), stitch)
                continue
            lum = (0.299 * r + 0.587 * g + 0.114 * b) / 170
            f = 0.8 + 0.25 * min(1.2, lum)
            base, kind = (linen, "plain") if y < 12 else (wool, "twill")
            out.putpixel((x, y), shade(base, f * weave(kind, x, y)))
    particle = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            particle.putpixel((x, y), shade(wool, weave("twill", x, y)))
    return out, particle


def build():
    wool = Image.open(TEX / "item/wool_thread.png").convert("RGBA")
    bed_tex, bed_particle = bed()
    out = {
        "item/silk_thread": recolour(wool, hexc("ecd290")),
        "item/wool_cloth": bolt("cbbfa4", "twill"),
        "item/linen_cloth": bolt("eae0c2", "plain"),
        "item/silk_cloth": bolt("e6c470", "silk"),
        "block/warp_weighted_loom_warp": warp(),
        "block/cloth_bed": bed_tex,
        "block/cloth_bed_particle": bed_particle,
    }
    for name, img in out.items():
        path = TEX / f"{name}.png"
        path.parent.mkdir(parents=True, exist_ok=True)
        img.save(path)
    return out


def preview(images, dst):
    small = [i for i in images.values() if i.width == 16]
    sheet = Image.new("RGBA", (len(small) * 136 + 520, 520), (120, 160, 210, 255))
    for i, img in enumerate(small):
        big = img.resize((128, 128), Image.NEAREST)
        sheet.paste(big, (i * 136 + 4, 4), big)
    big = images["block/cloth_bed"].resize((512, 512), Image.NEAREST)
    sheet.paste(big, (len(small) * 136 + 4, 4), big)
    sheet.save(dst)


if __name__ == "__main__":
    imgs = build()
    if len(sys.argv) > 1:
        preview(imgs, sys.argv[1])
    print(f"weaving textures ok ({len(imgs)})")
