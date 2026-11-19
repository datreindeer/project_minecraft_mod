"""Textures for the stone-age watercraft (Watercraft.java, DugoutBlankBlock.java, client/WatercraftModel.java).

- Dugout Blank block faces: hewn side and top (adze facets over stripped oak), knife-scored top, end grain, the fresh-hewn
  hollow and the charred hollow.
- Entity skins: Log Raft (bark-on logs with cordage lashings, two peeled cross poles) and Dugout Canoe (hewn outside, a
  fire-darkened scraped hollow, solid log ends). Every box of WatercraftModel is painted face by face with vanilla's box
  UV layout (see box() below), so the boxes here must match the model's texOffs and sizes.
- Item icons: Log Raft, Dugout Canoe, Paddle.
Colours are taken from vanilla's oak log, stripped oak log and our Cordage texture.
Run: python scripts/gen_watercraft_textures.py
"""
import math
import sys
import zipfile
from io import BytesIO
from pathlib import Path

from PIL import Image

sys.path.insert(0, str(Path(__file__).resolve().parent))
from gen_crude_ranged_textures import JAR  # noqa: E402

ROOT = Path(__file__).resolve().parent.parent
TEX = ROOT / "src/main/resources/assets/mechanical/textures"
_jar = zipfile.ZipFile(JAR)


def vanilla(path):
    return Image.open(BytesIO(_jar.read(f"assets/minecraft/textures/{path}.png"))).convert("RGBA")


def h(x, y, salt=0):
    """Stable pseudo-random 0..1 per pixel."""
    v = (x * 374761393 + y * 668265263 + salt * 2147483647) & 0xFFFFFFFF
    v = ((v ^ (v >> 13)) * 1274126177) & 0xFFFFFFFF
    return ((v ^ (v >> 16)) & 0xFFFF) / 65535.0


def shade(c, f):
    return tuple(max(0, min(255, int(round(ch * f)))) for ch in c[:3]) + (255,)


def mix(a, b, t):
    return tuple(int(round(a[i] + (b[i] - a[i]) * t)) for i in range(3)) + (255,)


def hexc(s):
    s = s.lstrip("#")
    return (int(s[0:2], 16), int(s[2:4], 16), int(s[4:6], 16), 255)


def save(img, rel):
    p = TEX / rel
    p.parent.mkdir(parents=True, exist_ok=True)
    img.save(p)


STRIPPED = vanilla("block/stripped_oak_log")       # grain runs vertically
STRIPPED_TOP = vanilla("block/stripped_oak_log_top")
BARK = vanilla("block/oak_log")
BARK_TOP = vanilla("block/oak_log_top")
CORD = [hexc("#b5977d"), hexc("#a68870"), hexc("#765636")]
CHAR = [hexc("#15100c"), hexc("#221a14"), hexc("#30251c"), hexc("#43352a")]


# ---------- surface painters: fn(x, y) -> colour, x along the grain ----------

def grain(src, along_u=True, f=1.0):
    """Sample a vanilla log side so its grain runs along u (x) of the painted face."""
    px = src.load()
    return lambda x, y: shade(px[(y % 16), (x % 16)] if along_u else px[(x % 16), (y % 16)], f)


def hewn(x, y, salt=0, f=1.0, along_u=True):
    """Stripped wood with adze facets: each stroke is a scallop, light where the blade bit in, darker where it lifted."""
    base = grain(STRIPPED, along_u)(x, y)
    a, b = (x, y) if along_u else (y, x)
    row = b // 4
    t = ((a + row * 2 + int(h(row, 0, salt) * 4)) % 5) / 4.0
    return shade(base, f * (1.08 - 0.18 * t) * (0.97 + 0.06 * h(x, y, salt)))


def hollow_raw(x, y, salt=0):
    """Fresh-hewn hollow: pale inner wood in short concave scoops."""
    cx, cy = x % 4, (y + (x // 4) * 2) % 4
    d = math.hypot(cx - 1.5, cy - 1.0) / 2.5
    base = shade(STRIPPED.getpixel(((x * 3) % 16, (y * 5) % 16)), 1.12)
    return shade(base, (1.06 - 0.22 * d) * (0.96 + 0.08 * h(x, y, salt)))


def charred(x, y, salt=0):
    """Burnt-out hollow: crackled char, with a few scraped spots showing brown wood underneath."""
    n = h(x, y, salt)
    cell = h(x // 3 + (y // 2) * 7, 1, salt)
    crack = (x % 3 == 0 and h(x, y // 2, salt + 9) < 0.55) or (y % 2 == 0 and h(x // 3, y, salt + 4) < 0.35)
    c = CHAR[0] if crack else CHAR[1 + int(cell * 2.99)]
    if n > 0.93:
        c = CHAR[3]
    if h(x // 2, y // 2, salt + 17) > 0.86 and not crack:
        c = mix(c, hexc("#5a4126"), 0.6)  # scraped through to the wood
    return c


def scraped_dark(x, y, salt=0):
    """The finished hollow: scraped smooth but still fire-darkened, with knife strokes along the length."""
    base = grain(STRIPPED, True)(x, y)
    dark = mix(base, CHAR[2], 0.62 + 0.18 * h(x // 3, y, salt))
    stroke = 0.94 + 0.12 * (((x + (y % 3) * 4) % 7) / 6.0)
    if h(x, y, salt + 3) > 0.94:
        dark = CHAR[1]
    return shade(dark, stroke)


def bark(x, y, salt=0, f=1.0):
    return shade(grain(BARK, True)(x + int(h(y, 2, salt) * 5), y), f * (0.95 + 0.1 * h(x, y, salt)))


def rings(x, y, w, hgt, src=BARK_TOP, f=1.0):
    """A log end squeezed into w x hgt."""
    px = src.load()
    sx = int((x + 0.5) * 16 / w)
    sy = int((y + 0.5) * 16 / hgt)
    return shade(px[min(15, sx), min(15, sy)], f)


def cord(x, y):
    # twisted cordage: diagonal strands
    return CORD[0] if (x + y) % 3 == 0 else CORD[1] if (x + y) % 3 == 1 else CORD[2]


# ---------- vanilla box UV layout (ModelPart.Cube) ----------

def box(img, u, v, w, hh, d, faces):
    """Paint a box's six faces. faces: dict face -> fn(x, y, fw, fh) giving the colour; missing faces are left empty.
    top = model minY (the top in the world), bottom = maxY, west = minX, north = minZ, east = maxX, south = maxZ.
    Side faces run x along u with y = 0 at the world top; top/bottom have u along the box's x and v along z."""
    rects = {
        "top": (u + d, v, w, d), "bottom": (u + d + w, v, w, d),
        "west": (u, v + d, d, hh), "north": (u + d, v + d, w, hh), "east": (u + d + w, v + d, d, hh), "south": (u + d + w + d, v + d, w, hh),
    }
    px = img.load()
    for face, fn in faces.items():
        x0, y0, fw, fh = rects[face]
        for y in range(fh):
            for x in range(fw):
                px[x0 + x, y0 + y] = fn(x, y, fw, fh)


def all_faces(fn):
    return {f: fn for f in ("top", "bottom", "west", "north", "east", "south")}


# ---------- Dugout Blank block ----------

def blank_textures():
    side = Image.new("RGBA", (16, 16))
    top = Image.new("RGBA", (16, 16))
    scored = Image.new("RGBA", (16, 16))
    end = Image.new("RGBA", (16, 16))
    raw = Image.new("RGBA", (16, 16))
    char = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            side.putpixel((x, y), hewn(x, y, 1, 0.95))
            top.putpixel((x, y), hewn(x, y, 2, 1.0, along_u=False))
            end.putpixel((x, y), STRIPPED_TOP.getpixel((x, y)))
            raw.putpixel((x, y), hollow_raw(x, y, 3))
            char.putpixel((x, y), charred(x, y, 4))
    scored.paste(top)
    # the canoe's outline cut into the top with the knife: walls 3 px, solid ends 2 px
    line = hexc("#4a3420")
    for z in range(2, 14):
        for x in (3, 12):
            scored.putpixel((x, z), line if h(x, z, 5) > 0.12 else shade(line, 1.4))
    for x in range(3, 13):
        for z in (2, 13):
            scored.putpixel((x, z), line if h(x, z, 6) > 0.12 else shade(line, 1.4))
    for name, im in (("side", side), ("top", top), ("scored", scored), ("end", end), ("hollow", raw), ("charred", char)):
        save(im, f"block/dugout_blank_{name}.png")


# ---------- Log Raft (128 x 96) ----------

def raft():
    img = Image.new("RGBA", (128, 96), (0, 0, 0, 0))
    poles_x = (-9.0, 9.0)
    for i in range(5):
        length = 30 if i % 2 == 0 else 28
        min_x = -length / 2
        bands = [int(p - min_x) for p in poles_x]

        def log_long(f, salt):
            def fn(x, y, fw, fh):
                if any(b - 1 <= x <= b + 1 for b in bands):
                    return cord(x, y)
                return bark(x, y + i * 5, salt, f)
            return fn
        box(img, 0, 10 * i, length, 5, 5, {
            "top": log_long(1.0, i), "north": log_long(0.9, i + 10), "south": log_long(0.9, i + 20),
            "bottom": log_long(0.62, i + 30),  # wet underside
            "west": lambda x, y, fw, fh: rings(x, y, fw, fh), "east": lambda x, y, fw, fh: rings(x, y, fw, fh),
        })
    # cross poles: peeled branches, wrapped with cordage where they cross each log
    log_z = [3, 8, 13, 18, 23]

    def pole_len(along):
        def fn(x, y, fw, fh):
            z = y if along == "v" else x
            if any(abs(z - c) <= 1 for c in log_z):
                return cord(x, y)
            return grain(STRIPPED, along == "u", 0.92)(x, y)
        return fn
    for v in (0, 28):
        box(img, 72, v, 2, 2, 26, {"top": pole_len("v"), "bottom": pole_len("v"), "west": pole_len("u"), "east": pole_len("u"),
                                   "north": lambda x, y, fw, fh: rings(x, y, fw, fh, STRIPPED_TOP), "south": lambda x, y, fw, fh: rings(x, y, fw, fh, STRIPPED_TOP)})
    paddles(img, 56, crude=True)
    save(img, "entity/watercraft/log_raft.png")


def paddles(img, v, crude):
    """Shaft (2x2x18) and blade (1x6x7) for both paddles, at u 0/40 and 56/96."""
    shaft = lambda x, y, fw, fh: grain(STRIPPED, True, 0.85 if crude else 0.95)(x, y)
    blade = (lambda x, y, fw, fh: bark(x, y, 7, 0.95)) if crude else (lambda x, y, fw, fh: hewn(x, y, 8, 1.02))
    for u in (0, 56):
        box(img, u, v, 2, 2, 18, all_faces(shaft))
        box(img, u + 40, v, 1, 6, 7, all_faces(blade))


# ---------- Dugout Canoe (128 x 64) ----------

def canoe():
    img = Image.new("RGBA", (128, 64), (0, 0, 0, 0))
    outside = lambda f: (lambda x, y, fw, fh: hewn(x, y + 3, 11, f))
    inside = lambda x, y, fw, fh: scraped_dark(x, y, 12)
    gunwale = lambda x, y, fw, fh: hewn(x, y, 13, 1.12)
    end_top = lambda x, y, fw, fh: hewn(x, y, 14, 1.05, along_u=True)
    # floor: its top is the inside of the hollow, its bottom the wet keel
    box(img, 0, 0, 30, 2, 10, {"top": inside, "bottom": outside(0.7), "west": outside(0.85), "north": outside(0.85),
                               "east": outside(0.85), "south": outside(0.85)})
    # walls: left at +z (its north face looks in), right at -z (its south face looks in)
    box(img, 0, 12, 30, 8, 2, {"top": gunwale, "bottom": outside(0.75), "west": outside(0.9), "east": outside(0.9),
                               "north": inside, "south": outside(1.0)})
    box(img, 64, 12, 30, 8, 2, {"top": gunwale, "bottom": outside(0.75), "west": outside(0.9), "east": outside(0.9),
                                "north": outside(1.0), "south": inside})
    # solid ends of the log; the face towards the middle is part of the hollow
    ring_end = lambda x, y, fw, fh: rings(x, y, fw, fh, STRIPPED_TOP, 0.95)
    box(img, 0, 22, 3, 8, 12, {"top": end_top, "bottom": outside(0.75), "west": inside, "north": outside(0.95),
                               "east": ring_end, "south": outside(0.95)})
    box(img, 52, 22, 3, 8, 12, {"top": end_top, "bottom": outside(0.75), "east": inside, "north": outside(0.95),
                                "west": ring_end, "south": outside(0.95)})
    for u, w, hh, d in ((30, 2, 7, 6), (46, 1, 5, 2), (82, 2, 7, 6), (98, 1, 5, 2)):
        box(img, u, 22, w, hh, d, {"top": end_top, "bottom": outside(0.75), "west": ring_end, "east": ring_end,
                                   "north": outside(0.95), "south": outside(0.95)})
    paddles(img, 42, crude=False)
    save(img, "entity/watercraft/dugout_canoe.png")


# ---------- item icons ----------

OUTLINE = hexc("#2b1d10")


def outline(img):
    """A dark outline on the transparent pixels around the shape."""
    px = img.load()
    add = []
    for y in range(16):
        for x in range(16):
            if px[x, y][3]:
                continue
            for dx, dy in ((1, 0), (0, 1), (-1, 0), (0, -1)):
                if 0 <= x - dx < 16 and 0 <= y - dy < 16 and px[x - dx, y - dy][3] and px[x - dx, y - dy] != OUTLINE:
                    add.append((x, y))
                    break
    for p in add:
        px[p] = OUTLINE


def diag(x, y, cx=7.5, cy=7.5):
    """Coordinates along (a) and across (b) the bottom-left to top-right diagonal."""
    dx, dy = x - cx, y - cy
    return (dx - dy) / math.sqrt(2), (dx + dy) / math.sqrt(2)


def canoe_icon():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    half_len, half_w = 7.4, 3.0
    for y in range(16):
        for x in range(16):
            a, b = diag(x + 0.5, y + 0.5, 8, 8)
            if abs(a) >= half_len:
                continue
            w = half_w * math.sqrt(max(0.0, 1 - (a / half_len) ** 4))
            if abs(b) > w:
                continue
            inner = abs(a) < half_len - 2.0 and -w + 1.0 < b < w - 1.4
            if inner:
                c = charred(x, y, 21) if b > 0 else scraped_dark(x, y, 21)  # the far side of the hollow lies in shadow
            elif b < 0:
                c = hewn(x * 2, y, 22, 1.18)  # near gunwale catching the light
            else:
                c = hewn(x * 2, y, 23, 0.82)
            img.putpixel((x, y), c)
    outline(img)
    save(img, "item/dugout_canoe.png")


def raft_icon():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for i in range(5):
        y0 = 3 + i * 2
        x0, x1 = (2, 13) if i % 2 == 0 else (3, 12)
        for x in range(x0, x1 + 1):
            for dy in range(2):
                y = y0 + dy
                if x == x0 or x == x1:
                    c = rings(0 if dy == 0 else 1, 1, 2, 2, BARK_TOP, 1.1)
                elif x in (5, 10):
                    c = cord(x, y)
                else:
                    c = bark(x, y + i * 3, i, 1.15 if dy == 0 else 0.8)
                img.putpixel((x, y), c)
    # the cross poles on top
    for x in (5, 10):
        img.putpixel((x, 2), grain(STRIPPED, False, 1.0)(x, 2))
        img.putpixel((x, 13), grain(STRIPPED, False, 0.8)(x, 13))
    outline(img)
    save(img, "item/log_raft.png")


def paddle_icon():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y in range(16):
        for x in range(16):
            a, b = diag(x + 0.5, y + 0.5, 8, 8)
            c = None
            if -7.2 < a < -1.5:  # blade
                t = (a + 4.35) / 2.85
                if abs(b) < 1.9 * math.sqrt(max(0.0, 1 - t * t)) + 0.35:
                    c = hewn(x * 2, y, 31, 1.12 if b < 0 else 0.86)
            elif -1.5 <= a < 6.2 and abs(b) < 0.75:  # shaft
                c = grain(STRIPPED, False, 1.05 if b < 0 else 0.85)(x, y)
            elif 6.2 <= a < 7.6 and abs(b) < 1.6:  # T-grip
                c = grain(STRIPPED, False, 0.95)(x, y)
            if c:
                img.putpixel((x, y), c)
    outline(img)
    save(img, "item/paddle.png")


if __name__ == "__main__":
    blank_textures()
    raft()
    canoe()
    canoe_icon()
    raft_icon()
    paddle_icon()
    print("watercraft textures ok")
