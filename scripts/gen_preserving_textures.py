"""Textures for food spoilage, smoking and drying (data: gen_preserving.py).

- items: Smoked Meat (vanilla cooked beef recoloured to smoky mahogany with a blackened rim), Smoked Fish (cooked cod,
  golden-bronze), Dried Fish (cod, pale grey-tan, salt specks), Dried Berries (sweet berries shrivelled dark, stems
  dried brown), Jerky (drawn: three thin, wavy, dark strips), Spoiled Food (drawn: a mushy heap with mould spots)
- Smoking Rack: soot-darkened stripped oak for the frame, and the hanging-strip sheets (raw / smoked meat and fish,
  rotten): four strips, 2 px wide and 9 tall, in columns 0, 4, 8, 12, a twine loop on top
- Drying Rack food planes (drawn in the hide plane's area, x 3-12, y 3-11, under a twine line): meat strips / jerky,
  fish fillets / dried fish, berry strands / dried berries
Run: python scripts/gen_preserving_textures.py
"""
import math
import sys
import zipfile
from io import BytesIO
from pathlib import Path

from PIL import Image

sys.path.insert(0, str(Path(__file__).resolve().parent))
from gen_crude_ranged_textures import JAR  # noqa: E402

TEX = Path(__file__).resolve().parent.parent / "src/main/resources/assets/mechanical/textures"
_jar = zipfile.ZipFile(JAR)


def vanilla(path):
    return Image.open(BytesIO(_jar.read(f"assets/minecraft/textures/{path}.png"))).convert("RGBA")


def save(img, rel):
    p = TEX / f"{rel}.png"
    p.parent.mkdir(parents=True, exist_ok=True)
    img.save(p)


def h(x, y, salt):
    """Deterministic per-pixel hash 0..999."""
    n = (x * 374761393 + y * 668265263 + salt * 2147483647) & 0xFFFFFFFF
    n = ((n ^ (n >> 13)) * 1274126177) & 0xFFFFFFFF
    return (n ^ (n >> 16)) % 1000


def lum(c):
    return (0.299 * c[0] + 0.587 * c[1] + 0.114 * c[2]) / 255


def ramp(cols, t):
    t = max(0.0, min(0.999, t)) * (len(cols) - 1)
    i = int(t)
    a, b, f = cols[i], cols[i + 1], t - i
    return tuple(round(a[k] + (b[k] - a[k]) * f) for k in range(3)) + (255,)


def shade(c, f):
    return tuple(max(0, min(255, round(v * f))) for v in c[:3]) + (c[3] if len(c) > 3 else 255,)


def solid(img):
    return {(x, y) for y in range(img.height) for x in range(img.width) if img.getpixel((x, y))[3]}


def edge(pts, x, y):
    return any((x + dx, y + dy) not in pts for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)))


def recolour(src, cols, rim=1.0, keep=None, salt=1, speck=None):
    """Repaint every opaque pixel along a colour ramp by its brightness (stretched to the sprite's range); the outline
    pixels are darkened by `rim`. `keep(c)` returns a colour for pixels painted differently (or None)."""
    pts = solid(src)
    ls = [lum(src.getpixel(p)) for p in pts]
    lo, hi = min(ls), max(ls)
    out = Image.new("RGBA", src.size)
    for x, y in pts:
        c = src.getpixel((x, y))
        k = keep(c) if keep else None
        col = k or ramp(cols, (lum(c) - lo) / (hi - lo or 1))
        if edge(pts, x, y):
            col = shade(col, rim)
        elif speck and h(x, y, salt) < speck[0]:
            col = speck[1]
        out.putpixel((x, y), col)
    return out


def hue(c):
    import colorsys
    return colorsys.rgb_to_hsv(*(v / 255 for v in c[:3]))


# ---------------------------------------------------------------- items
SMOKED = [(38, 18, 12), (78, 36, 20), (120, 60, 32), (168, 98, 52)]
save(recolour(vanilla("item/cooked_beef"), SMOKED, rim=0.55,
              keep=lambda c: (196, 140, 80, 255) if lum(c) > 0.78 else None), "item/smoked_meat")  # fat goes amber
GOLD = [(70, 40, 16), (128, 80, 30), (180, 124, 50), (222, 176, 92)]
save(recolour(vanilla("item/cooked_cod"), GOLD, rim=0.6), "item/smoked_fish")
DRIED_FISH = [(92, 82, 66), (140, 126, 100), (184, 170, 140), (220, 212, 190)]
save(recolour(vanilla("item/cod"), DRIED_FISH, rim=0.7, salt=5, speck=(90, (238, 236, 228, 255))), "item/dried_fish")


def berry_or_stem(c):
    hh, s, v = hue(c)
    if 0.15 < hh < 0.45 and s > 0.2:  # green leaves / stem -> dried brown
        return ramp([(62, 46, 24), (104, 80, 42), (138, 110, 62)], v)
    return None


save(recolour(vanilla("item/sweet_berries"), [(34, 14, 22), (70, 30, 42), (104, 50, 60), (140, 82, 84)], rim=0.75,
              keep=berry_or_stem, salt=7, speck=(120, (40, 18, 26, 255))), "item/dried_berries")

# Jerky: three thin, wavy strips lying diagonally, darker rims, a few pale fat flecks
JERKY = [(58, 22, 16), (96, 40, 24), (128, 60, 36), (156, 86, 54)]
img = Image.new("RGBA", (16, 16))
pts = set()
for k, (sx, sy, ex, ey) in enumerate(((1, 10, 9, 2), (3, 13, 12, 4), (7, 14, 14, 7))):  # bottom-left to top-right
    for i in range(41):
        t = i / 40
        wob = 0.6 * math.sin(t * 6.3 + k * 2)  # a little wave across the strip
        x, y = sx + (ex - sx) * t + wob * 0.7, sy + (ey - sy) * t + wob * 0.7
        for dx, dy in ((0, 0), (1, 0), (0, 1)):
            pts.add((min(15, round(x) + dx), min(15, round(y) + dy)))
for x, y in pts:
    t = 0.35 + 0.45 * ((h(x, y, 11) % 100) / 100) - (0.15 if (x, y + 1) not in pts else 0)
    col = ramp(JERKY, t)
    if edge(pts, x, y) and (x, y - 1) not in pts:
        col = ramp(JERKY, 0.85)  # top edge catches the light
    elif edge(pts, x, y):
        col = shade(col, 0.7)
    if h(x, y, 12) < 40:
        col = (176, 120, 84, 255)
    img.putpixel((x, y), col)
save(img, "item/jerky")

# Spoiled Food: a slumped heap, brown-green mush, pale mould spots and dark rot
img = Image.new("RGBA", (16, 16))
heap = set()
for y in range(16):
    for x in range(16):
        r = 1 - ((x - 7.5) / 6.6) ** 2 - ((y - 10) / 4.6) ** 2 + (h(x, y, 21) % 100) / 900
        if r > 0 and y <= 14:
            heap.add((x, y))
MUSH = [(46, 40, 18), (80, 74, 34), (112, 106, 52), (142, 134, 72)]
for x, y in heap:
    t = 0.75 - (y - 6) / 12 + (h(x, y, 22) % 100) / 400
    col = shade(ramp(MUSH, t), 0.6) if edge(heap, x, y) and (x, y - 1) in heap else ramp(MUSH, t)
    if (x, y - 1) not in heap:
        col = ramp(MUSH, 0.95)
    img.putpixel((x, y), col)
for cx, cy in ((5, 8), (10, 9), (7, 11), (11, 12)):  # mould
    for dx, dy in ((0, 0), (1, 0), (0, 1)):
        if (cx + dx, cy + dy) in heap and h(cx + dx, cy + dy, 23) < 800:
            img.putpixel((cx + dx, cy + dy), (196, 210, 164, 255) if (dx, dy) == (0, 0) else (160, 178, 128, 255))
for p in ((8, 9), (4, 11), (12, 10), (9, 13)):  # dark rot
    if p in heap:
        img.putpixel(p, (40, 30, 18, 255))
save(img, "item/spoiled_food")

# ---------------------------------------------------------------- Smoking Rack
wood = vanilla("block/stripped_oak_log")
frame = Image.new("RGBA", (16, 16))
for y in range(16):
    for x in range(16):
        c = wood.getpixel((x, y))
        f = 0.62 - (0.18 if h(x, y, 31) < 160 else 0) - (0.1 if h(x // 2, y // 3, 32) < 250 else 0)  # soot streaks
        frame.putpixel((x, y), shade(c, f))
save(frame, "block/smoking_rack")

TWINE = (168, 128, 78, 255)
# (dark, mid, light ramp), fleck colour, skin colour for one side (fish) or None
STRIPS = {
    "raw_meat": ([(122, 22, 24), (172, 42, 40), (212, 86, 76)], (236, 204, 192, 255), None),
    "raw_fish": ([(196, 104, 84), (228, 146, 122), (246, 190, 168)], None, (168, 174, 180, 255)),
    "smoked_meat": ([(44, 20, 12), (88, 40, 22), (128, 68, 38)], (172, 118, 70, 255), None),
    "smoked_fish": ([(104, 60, 22), (160, 106, 42), (204, 154, 72)], None, (80, 56, 32, 255)),
    "rotten": ([(62, 70, 36), (94, 100, 52), (122, 118, 68)], (54, 38, 28, 255), None),
    "jerky": ([(62, 24, 18), (100, 42, 26), (132, 62, 38)], (170, 112, 76, 255), None),
    "dried_fish": ([(124, 110, 86), (166, 152, 122), (204, 194, 164)], None, (108, 104, 98, 255)),
}


def strip_px(kind, x, y, w, length, salt):
    """Colour of pixel (x across 0..w-1, y down 1..length) of a hanging strip, or None past its ragged end."""
    cols, fleck, skin = STRIPS[kind]
    if y == length and h(x, y, salt) < 500:
        return None  # ragged bottom end
    if skin and x == 0:
        return shade(skin, 1.08 if y % 3 else 0.9)
    t = (0.75 if x == 0 else 0.4 if x == w - 1 else 0.6) - y * 0.025 + (h(x, y, salt) % 100) / 600
    if fleck and h(x, y, salt + 1) < 110:
        return fleck
    return ramp(cols, t)


for kind in ("raw_meat", "raw_fish", "smoked_meat", "smoked_fish", "rotten"):
    img = Image.new("RGBA", (16, 16))
    for i in range(4):
        u = 4 * i
        img.putpixel((u, 0), TWINE)
        img.putpixel((u + 1, 0), shade(TWINE, 0.8))
        for y in range(1, 9):
            for x in range(2):
                c = strip_px(kind, x, y, 2, 8, 40 + i)
                if c:
                    img.putpixel((u + x, y), c)
    save(img, f"block/smoking_rack_{kind}")


# ---------------------------------------------------------------- Drying Rack food planes
def plane():
    img = Image.new("RGBA", (16, 16))
    for x in range(3, 13):
        img.putpixel((x, 3), TWINE if x % 3 else shade(TWINE, 0.8))
    return img


def meat_plane(kind):
    img = plane()
    for i, (x0, length) in enumerate(((4, 7), (7, 6), (10, 7))):
        if kind == "jerky":
            length -= 1  # shrinks as it dries
        for y in range(1, length + 1):
            for x in range(2):
                c = strip_px(kind, x, y, 2, length, 60 + i)
                if c:
                    img.putpixel((x0 + x, 3 + y), c)
    return img


def fish_plane(kind):
    """Two fillets hung by the tail: a forked tail at the top, 3 px wide body, rounded bottom, skin on the left."""
    img = plane()
    for i, x0 in enumerate((4, 8)):
        body = 7 if kind == "fish" else 6
        img.putpixel((x0, 4), strip_px(kind, 1, 1, 3, 9, 70 + i))
        img.putpixel((x0 + 2, 4), strip_px(kind, 1, 1, 3, 9, 70 + i))
        img.putpixel((x0 + 1, 5), strip_px(kind, 1, 2, 3, 9, 70 + i))
        for y in range(6, 6 + body):
            for x in range(3):
                if y == 5 + body and x != 1:
                    continue
                img.putpixel((x0 + x, y), strip_px(kind, x, y - 4, 3, 99, 70 + i))
    return img


def berry_plane(dried):
    """Three strands of berries on twine, a bunch every other pixel: one berry each side, one hanging below."""
    img = plane()
    fresh = [((150, 24, 40), (206, 56, 64)), ((58, 54, 136), (104, 100, 190)), ((112, 22, 52), (168, 46, 86))]
    for i, x0 in enumerate((5, 8, 11)):
        for y in range(4, 11):
            img.putpixel((x0, y), shade(TWINE, 0.85))
        dark, light = ((56, 22, 32), (88, 44, 50)) if dried else fresh[i]
        for j, y in enumerate((5, 8)):
            img.putpixel((x0 - 1, y), light + (255,))
            img.putpixel((x0 + 1, y), dark + (255,))
            if not dried:  # dried ones shrink to the two side berries
                img.putpixel((x0, y + 1), (light if j else dark) + (255,))
    return img


save(meat_plane("raw_meat"), "block/drying_rack_meat")
save(meat_plane("jerky"), "block/drying_rack_jerky")
STRIPS["fish"] = STRIPS["raw_fish"]
save(fish_plane("fish"), "block/drying_rack_fish")
save(fish_plane("dried_fish"), "block/drying_rack_dried_fish")
save(berry_plane(False), "block/drying_rack_berries")
save(berry_plane(True), "block/drying_rack_dried_berries")
print("preserving textures ok")
