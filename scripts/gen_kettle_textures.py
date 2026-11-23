"""Bronze Kettle and Tripod textures. Run: python scripts/gen_kettle_textures.py (any time after gen_bronze_textures.py)

Block:
- tripod_pole: a barked pole, vertical bark furrows with lighter ridges and a knot or two (the poles use 2 px wide strips
  of it); tripod_pole_end: the cut end, rings round a darker heart; tripod_lashing: cordage wound round, twisted strands.
- kettle_bronze: hammered bronze, rows of hammer dimples catching the light, a little verdigris, soot creeping up from
  the bottom rows; kettle_inside: the sooted, darker inside; kettle_chain: one oval bronze link (cutout).
- kettle_liquid: a 32 x 32 greyscale ripple (a tileable 16 px tile repeated 2x2, so KettleRenderer can scroll it and wrap
  seamlessly); light, so the pot's colour tints it.
- Clay Kettle Mold top, its molten (4 frames) and solid bronze overlays: the cavity is the kettle's silhouette, drawn
  like gen_bronze_textures.py's molds (dark sunken floor, light lip on the lower right).
Item:
- bronze_kettle: the pot from the side with its bail, lit from the left, rim highlight, sooty base;
  rough_cast_bronze_kettle: the same shape in dull casting skin with flash at the seam; unfired_clay_kettle_mold.
- tripod: three poles from a lashed apex, ends crossing above it.
- stew: the vanilla bowl with a thick brown stew, chunks of meat, carrot and greens; broth: clear golden broth with fat
  eyes; hide_glue: an amber cake of glue, glossy, with a broken edge.
"""
import json
import random
import zipfile
from io import BytesIO
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
JAR_NAME = "build/moddev/artifacts/minecraft-patched-26.3.0.95-merged.jar"
JAR = ROOT / JAR_NAME
if not JAR.exists():  # a git worktree without its own build: use the main checkout's
    JAR = Path(r"C:\Users\Yngve\Documents\project_minecraft_mod") / JAR_NAME
TEX = ROOT / "src/main/resources/assets/mechanical/textures"
ITEM, BLOCK = TEX / "item", TEX / "block"
jar = zipfile.ZipFile(JAR)


def vanilla(path):
    return Image.open(BytesIO(jar.read(f"assets/minecraft/textures/{path}.png"))).convert("RGBA")


def ramp(pal, t):
    t = max(0.0, min(1.0, t)) * (len(pal) - 1)
    i = min(int(t), len(pal) - 2)
    f = t - i
    return tuple(int(pal[i][c] * (1 - f) + pal[i + 1][c] * f) for c in range(3))


def save(img, path, frames=None):
    path.parent.mkdir(parents=True, exist_ok=True)
    img.save(path)
    mc = path.with_suffix(".png.mcmeta")
    if frames:
        mc.write_text(json.dumps({"animation": {"frametime": frames}}) + "\n")
    elif mc.exists():
        mc.unlink()


def px(img):
    return [(x, y) for y in range(img.height) for x in range(img.width) if img.getpixel((x, y))[3]]


BRONZE = [(52, 32, 12), (92, 60, 22), (140, 98, 38), (184, 138, 60), (218, 178, 96), (244, 218, 150)]  # as gen_bronze_textures.py
ROUGH = [(40, 30, 16), (74, 56, 30), (108, 84, 46), (134, 106, 60), (158, 128, 78)]
MOLTEN = [(130, 34, 4), (210, 84, 10), (250, 152, 30), (255, 214, 90), (255, 248, 204)]
SOOT = (30, 24, 20)
VERDIGRIS = (78, 140, 112)
BARK = [(46, 32, 20), (70, 50, 32), (96, 72, 46), (124, 96, 64)]
WOOD = [(120, 88, 52), (156, 120, 74), (186, 150, 100), (208, 178, 126)]
CORD = [(84, 64, 36), (122, 98, 58), (160, 134, 86), (190, 166, 114)]
FIRED = [(84, 40, 26), (128, 64, 40), (160, 84, 54), (186, 108, 72)]
RAW = [(96, 100, 116), (126, 132, 148), (156, 162, 178), (176, 182, 196)]


def mix(a, b, t):
    return tuple(int(a[i] * (1 - t) + b[i] * t) for i in range(3))


# ---------------------------------------------------------------- tripod

rng = random.Random(7)
pole = Image.new("RGBA", (16, 16))
for x in range(16):
    ridge = (x % 3 == 1)
    for y in range(16):
        t = 0.62 if ridge else 0.3
        t += 0.08 * ((y + x * 5) % 7 == 0) - 0.1 * ((y * 3 + x) % 11 == 0) + rng.uniform(-0.06, 0.06)
        pole.putpixel((x, y), ramp(BARK, t) + (255,))
for kx, ky in ((5, 4), (11, 11)):  # knots
    for dx, dy, t in ((0, 0, 0.0), (1, 0, 0.15), (0, 1, 0.15), (-1, 0, 0.5), (0, -1, 0.85), (1, 1, 0.3)):
        pole.putpixel(((kx + dx) % 16, (ky + dy) % 16), ramp(BARK, t) + (255,))
save(pole, BLOCK / "tripod_pole.png")

end = Image.new("RGBA", (16, 16))
for x in range(16):
    for y in range(16):
        d = ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5
        if d > 7.2:
            c = ramp(BARK, 0.25 + 0.2 * ((x + y) % 2))
        else:
            c = ramp(WOOD, 0.75 - 0.25 * (int(d * 1.3) % 2) - (0.35 if d < 1.5 else 0) + rng.uniform(-0.04, 0.04))
        end.putpixel((x, y), c + (255,))
save(end, BLOCK / "tripod_pole_end.png")

lash = Image.new("RGBA", (16, 16))
for x in range(16):
    for y in range(16):
        s = (x + 2 * y) % 6  # twisted strands running round the poles
        t = [0.15, 0.45, 0.75, 0.9, 0.6, 0.3][s] + rng.uniform(-0.05, 0.05)
        if y % 4 == 3:
            t *= 0.55  # the gap between turns
        lash.putpixel((x, y), ramp(CORD, t) + (255,))
save(lash, BLOCK / "tripod_lashing.png")

# ---------------------------------------------------------------- kettle (block)

rng = random.Random(21)
kb = Image.new("RGBA", (16, 16))
for y in range(16):
    for x in range(16):
        t = 0.48 + rng.uniform(-0.05, 0.05)
        # hammer dimples on a staggered grid: bright upper-left, dark lower-right
        cx, cy = (x + (y // 4) * 2) % 4, y % 4
        if (cx, cy) == (1, 1):
            t += 0.2
        elif (cx, cy) == (2, 2):
            t -= 0.16
        c = ramp(BRONZE, t)
        soot = max(0, (y - 10) / 6)  # creeping up from the bottom rows
        c = mix(c, SOOT, soot * 0.75)
        kb.putpixel((x, y), c + (255,))
for x, y in ((3, 2), (4, 2), (12, 6), (9, 9), (10, 9)):  # verdigris spots
    kb.putpixel((x, y), mix(kb.getpixel((x, y))[:3], VERDIGRIS, 0.6) + (255,))
save(kb, BLOCK / "kettle_bronze.png")

ki = Image.new("RGBA", (16, 16))
for y in range(16):
    for x in range(16):
        c = mix(ramp(BRONZE, 0.22 + rng.uniform(-0.04, 0.06)), SOOT, 0.45 + 0.03 * ((x * 7 + y * 3) % 5))
        ki.putpixel((x, y), c + (255,))
save(ki, BLOCK / "kettle_inside.png")

chain = Image.new("RGBA", (16, 16))
for y in range(4, 12):
    for x in range(6, 10):
        ex, ey = (x - 7.5) / 2, (y - 7.5) / 4
        r = ex * ex + ey * ey
        if 0.32 < r <= 1.05:
            chain.putpixel((x, y), ramp(BRONZE, 0.8 - 0.5 * (x - 6) / 3 + (0.1 if y < 6 else 0)) + (255,))
save(chain, BLOCK / "kettle_chain.png")

# ripple: sums of sines periodic over 16 px, then tiled 2x2
import math
tile = [[0.0] * 16 for _ in range(16)]
for y in range(16):
    for x in range(16):
        v = (math.sin(2 * math.pi * (x + 0.5 * y) / 16) + math.sin(2 * math.pi * (2 * y - x) / 16) * 0.6
             + math.sin(2 * math.pi * (3 * x + y) / 16) * 0.3)
        tile[y][x] = v
lo = min(min(r) for r in tile)
hi = max(max(r) for r in tile)
liq = Image.new("RGBA", (32, 32))
for y in range(32):
    for x in range(32):
        t = (tile[y % 16][x % 16] - lo) / (hi - lo)
        g = int(196 + 59 * t ** 2)  # mostly mid-light, bright crests
        liq.putpixel((x, y), (g, g, g, 255))
save(liq, BLOCK / "kettle_liquid.png")

# ---------------------------------------------------------------- kettle (item) and the mold's cavity

def kettle_shape():
    """pixel -> role: 'bail', 'rim', 'lug', 'body' (with the body's row/column for shading)."""
    roles = {}
    for x in range(5, 11):
        roles[(x, 1)] = "bail"
    for x, y in ((4, 2), (11, 2), (3, 3), (12, 3), (3, 4), (12, 4)):
        roles[(x, y)] = "bail"
    for x in range(2, 14):
        roles[(x, 5)] = "rim"
    roles[(1, 6)] = roles[(14, 6)] = "lug"
    widths = {6: (2, 13), 7: (1, 14), 8: (1, 14), 9: (1, 14), 10: (1, 14), 11: (2, 13), 12: (2, 13), 13: (3, 12), 14: (4, 11)}
    for y, (a, b) in widths.items():
        for x in range(a, b + 1):
            roles.setdefault((x, y), "body")
    return roles


SHAPE = kettle_shape()


def kettle_icon(pal, seed, rough=False):
    rng = random.Random(seed)
    img = Image.new("RGBA", (16, 16))
    for (x, y), role in SHAPE.items():
        if role == "bail":
            t = 0.55 if y > 1 else 0.7
        elif role == "rim":
            t = 0.95 - 0.04 * abs(x - 6)
        elif role == "lug":
            t = 0.35
        else:
            row = [p[0] for p in SHAPE if p[1] == y and SHAPE[p] == "body"]
            a, b = min(row), max(row)
            u = (x - a) / max(1, b - a)  # 0 left .. 1 right
            t = 0.8 - 0.75 * u + (0.12 if u < 0.25 else 0)
            if x in (a, b) or y == 14:
                t = 0.08  # outline
            if y >= 12 and not rough:
                t *= 0.6  # soot round the base
        t += rng.uniform(-0.06, 0.06)
        img.putpixel((x, y), ramp(pal, t) + (255,))
    if not rough:
        for x, y in ((4, 7), (5, 8), (4, 9)):  # hammer glints on the lit side
            img.putpixel((x, y), ramp(pal, 1.0) + (255,))
    else:
        for x in range(2, 14):  # casting flash where the mold halves met
            if rng.random() < 0.5:
                img.putpixel((x, 4), pal[1] + (255,))
    return img


save(kettle_icon(BRONZE, 3), ITEM / "bronze_kettle.png")
save(kettle_icon(ROUGH, 4, rough=True), ITEM / "rough_cast_bronze_kettle.png")

mask = Image.new("RGBA", (16, 16))
for p, role in SHAPE.items():
    if role != "bail":  # the bail is bent from the cast lugs' wire, not cast
        mask.putpixel(p, (255, 255, 255, 255))


def cavity(x, y):
    return 0 <= x < 16 and 0 <= y < 16 and mask.getpixel((x, y))[3] > 0


def mold_top(base, pal, seed):
    rng = random.Random(seed)
    img = base.copy() if base else Image.new("RGBA", (16, 16))
    for x in range(16):
        for y in range(16):
            if base is None:
                img.putpixel((x, y), ramp(pal, 0.55 + rng.uniform(-0.12, 0.12)) + (255,))
            if cavity(x, y):
                shade = 0.12 if not cavity(x - 1, y - 1) else 0.28
                img.putpixel((x, y), ramp(pal, shade + rng.uniform(0, 0.08)) + (255,))
            elif cavity(x - 1, y - 1) or cavity(x - 1, y) or cavity(x, y - 1):
                img.putpixel((x, y), ramp(pal, 0.95) + (255,))
    return img


def item_icon(top):
    img = Image.new("RGBA", (16, 16))
    for x in range(1, 15):
        for y in range(2, 14):
            img.putpixel((x, y), top.getpixel((x, y)))
        for y in (14, 15):
            r, g, b, _ = top.getpixel((x, 13))
            img.putpixel((x, y), (int(r * 0.7), int(g * 0.7), int(b * 0.7), 255))
    return img


fired = Image.open(BLOCK / "kiln_clay_fired.png").convert("RGBA")
save(mold_top(fired, FIRED, 61), BLOCK / "clay_kettle_mold_top.png")
save(item_icon(mold_top(None, RAW, 62)), ITEM / "unfired_clay_kettle_mold.png")
solid = Image.new("RGBA", (16, 16))
for p in px(mask):
    t = 0.5 + (0.25 if not cavity(p[0] - 1, p[1] - 1) else 0) + random.Random(p[0] * 31 + p[1]).uniform(-0.1, 0.1)
    solid.putpixel(p, ramp(BRONZE[1:], t) + (255,))
save(solid, BLOCK / "clay_kettle_mold_bronze_solid.png")
molten = Image.new("RGBA", (16, 64))
for f in range(4):
    r = random.Random(f * 11 + 170)
    for x, y in px(mask):
        molten.putpixel((x, y + f * 16), ramp(MOLTEN, 0.55 + r.uniform(-0.15, 0.35)) + (255,))
save(molten, BLOCK / "clay_kettle_mold_bronze_molten.png", frames=8)

# ---------------------------------------------------------------- tripod icon

tri = Image.new("RGBA", (16, 16))


def line(img, a, b, pal, t0):
    n = max(abs(b[0] - a[0]), abs(b[1] - a[1]))
    for i in range(n + 1):
        x = round(a[0] + (b[0] - a[0]) * i / n)
        y = round(a[1] + (b[1] - a[1]) * i / n)
        img.putpixel((x, y), ramp(pal, t0 + 0.15 * ((i // 3) % 2)) + (255,))


# each pole two pixels thick: a lit edge and a shaded one
line(tri, (6, 0), (2, 15), BARK, 0.35)   # left pole, its top crossing past the lashing
line(tri, (5, 0), (1, 15), BARK, 0.6)
line(tri, (11, 0), (14, 15), BARK, 0.25)  # right pole, in shadow
line(tri, (10, 0), (13, 15), BARK, 0.45)
line(tri, (8, 0), (8, 14), BARK, 0.55)   # the front pole
line(tri, (7, 0), (7, 14), BARK, 0.85)
for x, y in ((6, 3), (7, 3), (8, 3), (9, 3), (10, 3), (7, 4), (8, 4), (9, 4)):
    tri.putpixel((x, y), ramp(CORD, 0.7 if y == 3 else 0.4) + (255,))
tri.putpixel((8, 5), ramp(BRONZE, 0.7) + (255,))  # the hook
save(tri, ITEM / "tripod.png")

# ---------------------------------------------------------------- bowls and glue

bowl, ref = vanilla("item/bowl"), vanilla("item/mushroom_stew")
fill = [(x, y) for x, y in px(ref) if ref.getpixel((x, y)) != bowl.getpixel((x, y))]  # where vanilla's stew sits
STEW = [(70, 38, 16), (108, 64, 28), (146, 96, 44), (176, 128, 64)]
BROTH = [(150, 104, 30), (196, 150, 52), (226, 190, 88), (246, 222, 140)]
rng = random.Random(33)
stew = bowl.copy()
for x, y in fill:
    stew.putpixel((x, y), ramp(STEW, 0.45 + rng.uniform(-0.15, 0.15)) + (255,))
top = min(y for _, y in fill)
chunks = {(5, top + 1): (122, 62, 40), (6, top + 1): (150, 84, 56), (9, top): (224, 120, 40), (10, top + 1): (198, 98, 30),
          (7, top + 2): (112, 140, 60), (11, top + 2): (150, 84, 56), (12, top + 1): (122, 62, 40), (4, top + 2): (90, 120, 50)}
for p, c in chunks.items():
    if p in fill:
        stew.putpixel(p, c + (255,))
save(stew, ITEM / "stew.png")

broth = bowl.copy()
for x, y in fill:
    broth.putpixel((x, y), ramp(BROTH, 0.5 + 0.15 * ((x + y) % 3 == 0) + rng.uniform(-0.06, 0.06)) + (255,))
for p in ((5, top + 1), (9, top + 2), (11, top + 1)):  # fat eyes on the surface
    if p in fill:
        broth.putpixel(p, (252, 236, 170, 255))
save(broth, ITEM / "broth.png")

AMBER = [(70, 34, 8), (120, 64, 14), (170, 104, 28), (212, 150, 54), (240, 206, 120)]
glue = Image.new("RGBA", (16, 16))
for y in range(5, 13):
    for x in range(2, 14):
        if (y == 5 or y == 12) and (x < 4 or x > 11) or x + y > 22:  # rounded corners, a broken corner on the right
            continue
        t = 0.5 + 0.25 * (y < 7) - 0.25 * (y > 10) - 0.1 * (x > 11) + rng.uniform(-0.04, 0.04)
        glue.putpixel((x, y), ramp(AMBER, t) + (255,))
for x, y in ((4, 6), (5, 6), (6, 6), (4, 7)):  # gloss
    glue.putpixel((x, y), ramp(AMBER, 1.0) + (255,))
for x, y in ((11, 11), (12, 10), (13, 9)):  # the broken edge, glassy
    if glue.getpixel((x, y))[3]:
        glue.putpixel((x, y), ramp(AMBER, 0.9) + (255,))
save(glue, ITEM / "hide_glue.png")

# ---------------------------------------------------------------- the blood kettle under a Hanging Rack, blood buckets, Blood Sausage

IRON = [(24, 24, 28), (44, 44, 50), (66, 66, 72), (92, 92, 98), (124, 124, 128), (160, 158, 156)]
RUST = (120, 58, 30)
BLOOD = [(34, 3, 5), (64, 8, 10), (98, 14, 16), (134, 24, 24), (172, 46, 42), (214, 104, 96)]

# kettle_iron: hammered wrought iron like the bronze pot, a few rust freckles, soot up from the bottom
rng = random.Random(51)
ki2 = Image.new("RGBA", (16, 16))
for y in range(16):
    for x in range(16):
        t = 0.42 + rng.uniform(-0.05, 0.05)
        cx, cy = (x + (y // 4) * 2) % 4, y % 4
        t += 0.18 if (cx, cy) == (1, 1) else -0.14 if (cx, cy) == (2, 2) else 0
        ki2.putpixel((x, y), mix(ramp(IRON, t), SOOT, max(0, (y - 10) / 6) * 0.7) + (255,))
for x, y in ((2, 3), (11, 2), (12, 2), (6, 7), (13, 9), (4, 11)):
    ki2.putpixel((x, y), mix(ki2.getpixel((x, y))[:3], RUST, 0.55) + (255,))
save(ki2, BLOCK / "kettle_iron.png")
ki3 = Image.new("RGBA", (16, 16))
for y in range(16):
    for x in range(16):
        ki3.putpixel((x, y), mix(ramp(IRON, 0.2 + rng.uniform(-0.04, 0.06)), SOOT, 0.4 + 0.03 * ((x * 7 + y * 3) % 5)) + (255,))
save(ki3, BLOCK / "kettle_iron_inside.png")

# kettle_blood: a dark, still surface, darker towards the walls, a soft sheen across it and a few bright glints
kbl = Image.new("RGBA", (16, 16))
for y in range(16):
    for x in range(16):
        edge = min(x - 4, 11 - x, y - 4, 11 - y)  # the pot shows 4..12
        t = 0.3 + 0.05 * min(max(edge, 0), 3) + rng.uniform(-0.03, 0.03)
        if 0 <= (x - y) + 1 <= 2 and 4 <= x <= 11 and 4 <= y <= 11:
            t += 0.06  # the sky's sheen in a diagonal band
        kbl.putpixel((x, y), ramp(BLOOD, t) + (255,))
for x, y in ((6, 6), (9, 8)):
    kbl.putpixel((x, y), ramp(BLOOD, 0.75) + (255,))
save(kbl, BLOCK / "kettle_blood.png")


def blood_filled(empty, full):
    """full with every pixel that differs from empty (the water) redrawn as blood, by the water's own lightness."""
    img = full.copy()
    for x, y in px(full):
        if full.getpixel((x, y)) != empty.getpixel((x, y)):
            r, g, b, a = full.getpixel((x, y))
            light = (r + g + b) / (3 * 255)
            img.putpixel((x, y), ramp(BLOOD, 0.12 + light * 0.95) + (a,))
    return img


save(blood_filled(vanilla("item/bucket"), vanilla("item/water_bucket")), ITEM / "blood_bucket.png")
save(blood_filled(Image.open(ITEM / "bronze_bucket.png").convert("RGBA"), Image.open(ITEM / "bronze_water_bucket.png").convert("RGBA")),
     ITEM / "bronze_blood_bucket.png")


def sausage(pal, seed, flecks, twine=True):
    """A ring of blood sausage bent into a horseshoe, lit from the upper left: a round tube (bright along its upper outer
    side, dark underneath), flecks of barley and fat, the ends tied off with twine."""
    rng = random.Random(seed)
    img = Image.new("RGBA", (16, 16))
    cx, cy, r_in, r_out = 7.5, 9.5, 1.9, 6.9
    for y in range(16):
        for x in range(16):
            dx, dy = x - cx, y - cy
            d = (dx * dx + dy * dy) ** 0.5
            if not (r_in <= d <= r_out) or y > 12 or (y > 9 and abs(dx) < 2.5):
                continue
            s = (d - r_in) / (r_out - r_in)  # across the tube: 0 inner edge .. 1 outer edge
            up = -dy / max(d, 0.1)            # 1 at the top of the bend
            left = -dx / max(d, 0.1)
            t = 0.3 + 0.4 * max(0, 1 - abs(s - 0.62) * 2.4) + 0.1 * up + 0.08 * left + rng.uniform(-0.05, 0.05)
            if s < 0.1 or s > 0.88:
                t = 0.04 + 0.05 * up  # the outline
            img.putpixel((x, y), ramp(pal, t) + (255,))
    for x, y in flecks:
        if img.getpixel((x, y))[3]:
            img.putpixel((x, y), mix(img.getpixel((x, y))[:3], (226, 210, 176), 0.55) + (255,))
    if twine:
        for x, y in ((1, 12), (2, 12), (3, 12), (12, 12), (13, 12), (14, 12)):
            if img.getpixel((x, y))[3]:
                img.putpixel((x, y), ramp(CORD, 0.8 if x in (2, 13) else 0.45) + (255,))
        for x, y in ((2, 13), (13, 13), (3, 14), (12, 14)):  # the loose ends of the twine
            img.putpixel((x, y), ramp(CORD, 0.6 if y == 13 else 0.4) + (255,))
    return img


RAW_SAUSAGE = [(20, 4, 8), (44, 10, 16), (70, 18, 26), (98, 30, 38), (130, 52, 58), (176, 104, 106)]
COOKED_SAUSAGE = [(12, 6, 6), (26, 12, 10), (42, 20, 16), (62, 32, 22), (92, 52, 34), (150, 104, 74)]
FLECKS = ((4, 6), (9, 3), (11, 6), (3, 10), (12, 9), (6, 3), (13, 7))
save(sausage(RAW_SAUSAGE, 71, FLECKS), ITEM / "raw_blood_sausage.png")
cooked = sausage(COOKED_SAUSAGE, 72, FLECKS[:3])
for x, y in ((3, 7), (4, 5), (5, 4)):  # fat glistening on the crisped skin
    if cooked.getpixel((x, y))[3]:
        cooked.putpixel((x, y), ramp(COOKED_SAUSAGE, 1.0) + (255,))
save(cooked, ITEM / "blood_sausage.png")
print("kettle textures ok")
