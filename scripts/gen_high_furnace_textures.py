"""High Furnace textures (docs/steel-furnace-plan.md). Run: python scripts/gen_high_furnace_textures.py

Phase 1-2, the furnace itself:
- Silica Bricks (the floor): cream-white silica bricks, smaller than the Furnace Bricks (2 px courses, 4-5 px long) in a
  running bond with thin pale-grey joints and yellow-ochre specks, lit from the top left.
- Banded Furnace Casing (the walls): the same bricks held by two blue-black steel hoops (rows 2-4 and 11-13, riveted,
  continuous from block to block), faint soot streaks rising above each hoop; the top is the brick with a steel rim.
  The three heat glows are fullbright overlays of exactly the casing's visible joints (and a few hairline cracks),
  dull red / orange / yellow-white, slowly flickering.
- Firebox front: a cast-iron door in the casing (the hoops run to its frame) with a bronze tuyere nozzle up top and a
  grate of four slots below; the glows light only the slots and the nozzle's mouth.
- High Furnace Front: a riveted steel door with two strap hinges (at the hoops' height) and a handle bar, a round sight
  hatch (x 5..10, y 3..8) dark inside; the glow is the churning melt seen through it; the hatch is its swing cover.
- Taphole: a riveted steel frame round a round hole with a domed, cracked fire-clay plug; open: the hole dark with a
  heat-coloured rim, the glow lights the hole.
- Great Bellows: oiled leather with pleats and a seam, oak boards with iron straps, the dark timber frame (and its end
  grain), the bronze nozzle pipe, the wooden cam arm with iron fittings.
- Items: Crushed Quartzite, (Unfired) Silica Brick, Steel Hoop, Bronze Tuyere Nozzle, and the clay tuyere mold as
  gen_bloomery_textures.py makes its molds (same helpers and palettes).
"""
import json
import math
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
LIB = Path(r"C:\Users\Yngve\Documents\Minecraft Modding\item")
TEX = ROOT / "src/main/resources/assets/mechanical/textures"
ITEM, BLOCK = TEX / "item", TEX / "block"
jar = zipfile.ZipFile(JAR)

SILICA = [(128, 116, 90), (176, 166, 138), (208, 199, 170), (227, 220, 194), (240, 235, 213), (251, 248, 232)]
JOINT = [(138, 134, 124), (162, 158, 148), (184, 180, 170)]
OCHRE = (184, 136, 52)
SOOT = (40, 34, 30)
HOOP = [(12, 14, 19), (24, 28, 36), (38, 44, 54), (56, 64, 76), (80, 90, 104), (114, 126, 140), (160, 172, 186)]
STEEL = [(28, 32, 38), (43, 48, 56), (74, 82, 94), (109, 120, 135), (148, 160, 176), (195, 204, 216), (228, 234, 242)]
CAST = [(12, 12, 14), (24, 24, 28), (38, 38, 42), (54, 54, 58), (74, 74, 78), (100, 100, 104), (136, 136, 140)]
BRONZE = [(52, 32, 12), (92, 60, 22), (140, 98, 38), (184, 138, 60), (218, 178, 96), (244, 218, 150)]
CLAYPLUG = [(70, 66, 60), (108, 104, 96), (146, 142, 132), (176, 172, 162), (200, 197, 188)]
MOLTEN = [(130, 34, 4), (210, 84, 10), (250, 152, 30), (255, 214, 90), (255, 248, 204)]
FIRED = [(84, 40, 26), (128, 64, 40), (160, 84, 54), (186, 108, 72)]
RAW = [(96, 100, 116), (126, 132, 148), (156, 162, 178), (176, 182, 196)]
HEAT = {  # glow stage -> ramp, dark to bright
    1: [(70, 10, 4), (118, 22, 6), (160, 38, 10), (196, 62, 16)],
    2: [(150, 42, 6), (212, 92, 14), (246, 150, 34), (255, 198, 72)],
    3: [(236, 150, 46), (255, 204, 96), (255, 236, 168), (255, 252, 232)],
}
VOID = (10, 8, 8)


def vanilla(path):
    return Image.open(BytesIO(jar.read(f"assets/minecraft/textures/{path}.png"))).convert("RGBA")


def px(img):
    return [(x, y) for y in range(img.height) for x in range(img.width) if img.getpixel((x, y))[3]]


def lum(c):
    return (c[0] * 3 + c[1] * 6 + c[2]) / 10


def ramp(pal, t):
    t = max(0.0, min(1.0, t)) * (len(pal) - 1)
    i = min(int(t), len(pal) - 2)
    f = t - i
    return tuple(int(pal[i][c] * (1 - f) + pal[i + 1][c] * f) for c in range(3))


def mix(a, b, f):
    f = max(0.0, min(1.0, f))
    return tuple(int(a[c] * (1 - f) + b[c] * f) for c in range(3))


WRITTEN = []


def save(img, path, anim=None):
    """anim: None, an int frametime (as the other generators), or the full animation dict."""
    path.parent.mkdir(parents=True, exist_ok=True)
    img.save(path)
    WRITTEN.append(path.relative_to(TEX).as_posix())
    mc = path.with_suffix(".png.mcmeta")
    if anim:
        mc.write_text(json.dumps({"animation": anim if isinstance(anim, dict) else {"frametime": anim}}) + "\n")
    elif mc.exists():
        mc.unlink()


def put(img, x, y, c):
    if 0 <= x < img.width and 0 <= y < img.height:
        img.putpixel((x, y), tuple(c[:3]) + (255,))


def get(img, x, y):
    return img.getpixel((x, y))[:3]


def outline(img, color):
    out = img.copy()
    for x in range(16):
        for y in range(16):
            if not img.getpixel((x, y))[3] and any(0 <= x + dx < 16 and 0 <= y + dy < 16 and img.getpixel((x + dx, y + dy))[3]
                                                   for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                out.putpixel((x, y), color + (255,))
    return out


def frames(n, draw):
    """An n-frame vertical strip; draw(frame_index) returns one 16x16 frame."""
    strip = Image.new("RGBA", (16, 16 * n))
    for f in range(n):
        strip.paste(draw(f), (0, 16 * f))
    return strip


# ================================================================ silica bricks
JOINT_ROWS = (1, 4, 7, 10, 13)
COURSES = [(2, 3), (5, 6), (8, 9), (11, 12), (14, 15, 0)]
UNITS = (5, 6, 5)  # brick + joint lengths along a course (16 px)


def _course_joints(off):
    xs, x = [], off
    for u in UNITS:
        x += u
        xs.append((x - 1) % 16)
    return xs


def _pick_offsets():
    """Running bond: every course's vertical joints as far as possible from the courses above and below (wrapping)."""
    def gap(a, b):
        return min(min(abs(p - q), 16 - abs(p - q)) for p in _course_joints(a) for q in _course_joints(b))
    best = None
    for o1 in range(16):
        for o2 in range(16):
            for o3 in range(16):
                for o4 in range(16):
                    o = (0, o1, o2, o3, o4)
                    gaps = [gap(o[i], o[(i + 1) % 5]) for i in range(5)]
                    score = (min(gaps), sum(gaps), -abs(o2 - 0))
                    if best is None or score > best[0]:
                        best = (score, o)
    return best[1]


OFFSETS = _pick_offsets()


def layout():
    """(x, y) -> ("joint", vertical?) or ("brick", id, row index, row count, col index, length)."""
    cells = {}
    for y in JOINT_ROWS:
        for x in range(16):
            cells[(x, y)] = ("joint", False)
    for ci, rows in enumerate(COURSES):
        x = OFFSETS[ci]
        for bi, u in enumerate(UNITS):
            for k in range(u):
                xx = (x + k) % 16
                for ri, y in enumerate(rows):
                    cells[(xx, y)] = ("joint", True) if k == u - 1 else ("brick", (ci, bi), ri, len(rows), k, u - 1)
            x += u
    return cells


CELLS = layout()


def silica_bricks(seed, speck=0.055, pal=SILICA, joint=JOINT):
    rng = random.Random(seed)
    img = Image.new("RGBA", (16, 16))
    tones = {}
    for (x, y), cell in CELLS.items():
        if cell[0] == "joint":
            t = 0.5 + rng.uniform(-0.12, 0.12) - (0.18 if cell[1] else 0)
            img.putpixel((x, y), ramp(joint, t) + (255,))
            continue
        _, bid, ri, rn, k, ln = cell
        tone = tones.setdefault(bid, rng.uniform(-0.1, 0.07))
        t = 0.66 + tone + rng.uniform(-0.035, 0.035)
        t += 0.13 if ri == 0 else (-0.09 if ri == rn - 1 else 0)
        t += 0.06 if k == 0 else (-0.08 if k == ln - 1 else 0)
        c = ramp(pal, t)
        r = rng.random()
        if r < speck:
            c = mix(c, OCHRE, rng.uniform(0.45, 0.7))
        elif r < speck + 0.02:
            c = mix(c, JOINT[0], 0.5)  # a quartz grain's pit
        img.putpixel((x, y), c + (255,))
    return img


# ================================================================ casing: bricks in two steel hoops
BANDS = ((2, 3, 4), (11, 12, 13))
RIVETS = {0: (3, 11), 1: (7, 15)}  # band index -> rivet x (staggered between the bands)


def hoop_pixel(rng, row, x):
    """One band pixel, row 0 top (catches the light), 2 bottom."""
    t = (0.74, 0.5, 0.3)[row] + rng.uniform(-0.035, 0.035)
    if (x * 5) % 16 in (3, 4):
        t -= 0.07  # hammer dents along the forged band
    elif row == 0 and (x * 7) % 16 in (5, 12):
        t += 0.1  # a polished spot where the light catches the edge
    return ramp(HOOP, t)


def draw_bands(img, seed, x0=0, x1=15, rivets=True):
    rng = random.Random(seed)
    for bi, rows in enumerate(BANDS):
        for x in range(x0, x1 + 1):
            for r, y in enumerate(rows):
                put(img, x, y, hoop_pixel(rng, r, x))
        below = rows[2] + 1
        for x in range(x0, x1 + 1):  # the band's shadow on the course under it
            put(img, x, below, mix(get(img, x, below), SOOT, 0.22))
        if rivets:
            for rx in RIVETS[bi]:
                if x0 <= rx <= x1:
                    rivet(img, rx, rows[0])


def rivet(img, x, y, pal=HOOP):
    """A 2x2 round rivet head with its top left at (x, y): lit top left, shadow under and right of it."""
    put(img, x, y, ramp(pal, 1.0))
    put(img, x + 1, y, ramp(pal, 0.72))
    put(img, x, y + 1, ramp(pal, 0.66))
    put(img, x + 1, y + 1, ramp(pal, 0.38))
    put(img, x + 2, y + 1, ramp(pal, 0.12))
    put(img, x, y + 2, ramp(pal, 0.1))
    put(img, x + 1, y + 2, ramp(pal, 0.04))


def soot_streaks(img, seed):
    rng = random.Random(seed)
    regions = [(5, 6, 7, 8, 9, 10), (14, 15, 0, 1)]  # bottom row of each list sits right on a hoop
    for rows in regions:
        cols = rng.sample(range(16), 4)
        for cx in cols:
            strength = rng.uniform(0.12, 0.24)
            width = rng.choice((1, 1, 2))
            for w in range(width):
                x = (cx + w) % 16
                for i, y in enumerate(rows):
                    f = strength * ((i + 1) / len(rows)) ** 1.6 * (0.7 if w else 1)
                    if CELLS[(x, y)][0] == "brick":
                        img.putpixel((x, y), mix(get(img, x, y), SOOT, f * rng.uniform(0.8, 1.1)) + (255,))


def casing(seed):
    img = silica_bricks(seed)
    soot_streaks(img, seed + 1)
    draw_bands(img, seed + 2)
    return img


BAND_ROWS = {y for rows in BANDS for y in rows}
GLOW_JOINTS = [p for p, c in CELLS.items() if c[0] == "joint" and p[1] not in BAND_ROWS]
CRACKS = [(5, 5), (5, 6), (6, 6), (12, 15), (13, 15), (13, 0), (9, 8), (10, 9)]  # hairlines off the joints


def joint_glow(stage):
    pal = HEAT[stage]
    phase = {p: random.Random(p[0] * 17 + p[1] * 31).uniform(0, 6.28) for p in GLOW_JOINTS + CRACKS}
    rng = random.Random(900 + stage)
    base = {p: rng.uniform(0.35, 0.75) for p in GLOW_JOINTS}
    cracks = CRACKS[:4] if stage == 2 else CRACKS if stage == 3 else []

    def draw(f):
        img = Image.new("RGBA", (16, 16))
        for p in GLOW_JOINTS:
            t = base[p] + 0.22 * math.sin(f / 8 * 6.283 + phase[p])
            img.putpixel(p, ramp(pal, t) + (255,))
        for p in cracks:
            t = 0.25 + 0.2 * math.sin(f / 8 * 6.283 + phase[p])
            img.putpixel(p, ramp(pal, t) + (255,))
        return img
    return frames(8, draw)


# ================================================================ shared drawing helpers
def bevel(img, x0, y0, x1, y1, pal, t, rng, hi=0.16, lo=0.16, noise=0.03):
    for x in range(x0, x1 + 1):
        for y in range(y0, y1 + 1):
            v = t + rng.uniform(-noise, noise)
            if y == y0 or x == x0:
                v += hi
            if y == y1 or x == x1:
                v -= lo
            put(img, x, y, ramp(pal, v))


def disc(cx, cy, r):
    return {(x, y) for x in range(16) for y in range(16) if (x - cx) ** 2 + (y - cy) ** 2 <= r * r}


# ================================================================ Firebox
def firebox():
    rng = random.Random(1101)
    img = casing(1100)
    # the frame: a heavy cast-iron surround, x 2..13, y 1..14
    bevel(img, 2, 1, 13, 14, CAST, 0.6, rng, hi=0.14, lo=0.3)
    for x in range(2, 14):
        put(img, x, 0, mix(get(img, x, 0), SOOT, 0.35))  # soot licked up over the frame
    # the door, recessed: shadow on its top/left inside the frame, lit lip bottom/right
    bevel(img, 3, 2, 12, 13, CAST, 0.4, rng, hi=-0.22, lo=-0.06, noise=0.025)
    bevel(img, 4, 3, 11, 12, CAST, 0.42, rng, hi=0.12, lo=0.14, noise=0.03)
    for bx, by in ((3, 2), (12, 2), (3, 13), (12, 13)):  # bolts in the frame corners
        put(img, bx, by, ramp(CAST, 0.95))
        put(img, bx + (1 if bx < 8 else -1), by, ramp(CAST, 0.25))
    # bronze tuyere nozzle up top: flange ring, lip, dark mouth
    cx, cy = 7.5, 5.5
    for (x, y) in disc(cx, cy, 3.1):
        d = math.hypot(x - cx, y - cy)
        lit = -((x - cx) + (y - cy)) / 3.1  # +1 top left
        if d <= 1.15:
            put(img, x, y, VOID)
        elif d <= 2.0:
            put(img, x, y, ramp(BRONZE, 0.55 - lit * 0.25))  # the inner lip faces the other way
        elif d <= 2.7:
            put(img, x, y, ramp(BRONZE, 0.58 + lit * 0.3 + rng.uniform(-0.04, 0.04)))
        else:
            put(img, x, y, ramp(BRONZE, 0.12))
    for bx, by in ((5, 4), (10, 4), (5, 7), (10, 7)):  # flange bolts
        put(img, bx, by, ramp(BRONZE, 0.95 if by == 4 else 0.7))
    # the grate: a raised bar frame round four slots
    for x in range(4, 12):
        put(img, x, 8, ramp(CAST, 0.75))
        put(img, x, 13, ramp(CAST, 0.18))
    for y in range(9, 13):
        for x in range(4, 12):
            if x in SLOTS_X:
                put(img, x, y, VOID if y < 12 else (46, 20, 12))  # ash and a dead ember at the bottom
            else:
                left = x - 1 in SLOTS_X or x == 4
                put(img, x, y, ramp(CAST, 0.68 if left else 0.4) if x not in (7, 8) else ramp(CAST, 0.6 if x == 7 else 0.36))
    for y0 in (3, 10):  # two hinge knuckles on the left of the frame
        for y in (y0, y0 + 1, y0 + 2):
            put(img, 2, y, ramp(CAST, 0.85 if y == y0 else 0.62))
            put(img, 3, y, ramp(CAST, 0.3))
        put(img, 2, y0 + 3, ramp(CAST, 0.12))
    for x in range(4, 12):  # heat has browned the bars over the slots
        put(img, x, 8, mix(get(img, x, 8), (110, 60, 36), 0.3))
    # the latch on the right
    put(img, 11, 6, ramp(CAST, 0.85))
    put(img, 11, 7, ramp(CAST, 0.55))
    put(img, 10, 7, ramp(CAST, 0.2))
    return img


SLOTS_X = (4, 6, 9, 11)
FIREBOX_SLOTS = [(x, y) for x in SLOTS_X for y in range(9, 13)]
NOZZLE_MOUTH = sorted(disc(7.5, 5.5, 1.15))


def firebox_glow(stage):
    pal = HEAT[stage]

    def draw(f):
        rng = random.Random(1200 + stage * 10 + f)
        img = Image.new("RGBA", (16, 16))
        for (x, y) in FIREBOX_SLOTS:
            flick = math.sin(f / 8 * 6.283 * 2 + x * 1.7) * 0.12
            t = 0.25 + (y - 9) * 0.17 + flick + rng.uniform(-0.12, 0.12)
            if rng.random() < 0.12:
                t += 0.3  # a flaring ember
            img.putpixel((x, y), ramp(pal, t) + (255,))
        pulse = 0.55 + 0.3 * math.sin(f / 8 * 6.283)  # the nozzle's mouth breathes with the bellows
        for (x, y) in NOZZLE_MOUTH:
            img.putpixel((x, y), ramp(pal, pulse + rng.uniform(-0.08, 0.08)) + (255,))
        return img
    return frames(8, draw)


# ================================================================ High Furnace Front (controller)
HATCH = sorted(p for p in disc(7.5, 5.5, 3.05))  # x 5..10, y 3..8


def high_front():
    rng = random.Random(1301)
    img = casing(1300)
    for x in range(2, 14):
        put(img, x, 0, mix(get(img, x, 0), SOOT, 0.35))  # soot licked up over the door
    # the frame (1 px, dark steel) and the door inside it: two riveted plates, seam at row 9
    bevel(img, 2, 1, 13, 14, STEEL, 0.26, rng, hi=0.12, lo=0.18)
    bevel(img, 3, 2, 12, 8, STEEL, 0.6, rng, hi=0.16, lo=0.24, noise=0.025)
    bevel(img, 3, 9, 12, 13, STEEL, 0.56, rng, hi=0.16, lo=0.24, noise=0.025)
    for x in range(3, 13):
        put(img, x, 9, ramp(STEEL, 0.12 if x > 3 else 0.3))  # the seam between the plates
        put(img, x, 10, ramp(STEEL, 0.7))
    for x, y in ((3, 2), (12, 2), (3, 8), (4, 11), (8, 11)):
        put(img, x, y, STEEL[6])
        put(img, x + 1, y + 1, ramp(STEEL, 0.2))
    # heat tint round the hatch (straw, then blue: tempered steel)
    cx, cy = 7.5, 5.5
    for (x, y) in disc(cx, cy, 4.7) - disc(cx, cy, 3.75):
        if 3 <= x <= 12 and 2 <= y <= 8:
            put(img, x, y, mix(get(img, x, y), (160, 126, 70) if math.hypot(x - cx, y - cy) < 4.2 else (78, 96, 146), 0.4))
    # the hatch: dark outline, a raised collar lit top left, the black opening
    for (x, y) in disc(cx, cy, 3.75):
        d = math.hypot(x - cx, y - cy)
        lit = -((x - cx) + (y - cy)) / d if d else 0
        if d <= 3.05:
            put(img, x, y, VOID if lit < 0.3 or d < 2.3 else (30, 20, 18))
        else:
            put(img, x, y, ramp(STEEL, 0.5 + lit * 0.5))
    for (x, y) in disc(cx + 0.7, cy + 0.7, 3.75) - disc(cx, cy, 3.75):  # the collar's shadow on the plate
        if 3 <= x <= 12 and 2 <= y <= 8:
            put(img, x, y, mix(get(img, x, y), (0, 0, 0), 0.4))
    # strap hinges at the hoops' height, the knuckles standing proud of the left frame
    for rows, x1 in (((2, 3, 4), 4), ((11, 12, 13), 6)):
        for x in range(1, x1 + 1):
            put(img, x, rows[0] - 1, mix(get(img, x, rows[0] - 1), (0, 0, 0), 0.45))
            for r, y in enumerate(rows):
                put(img, x, y, ramp(HOOP, (0.86, 0.6, 0.3)[r] + rng.uniform(-0.03, 0.03)))
            put(img, x, rows[2] + 1, mix(get(img, x, rows[2] + 1), (0, 0, 0), 0.5))
        put(img, x1 + 1, rows[1], ramp(HOOP, 0.4))  # the strap's rounded tip
        put(img, x1 + 1, rows[0], mix(get(img, x1 + 1, rows[0]), (0, 0, 0), 0.4))
        put(img, x1 + 1, rows[2], mix(get(img, x1 + 1, rows[2]), (0, 0, 0), 0.4))
        for y in range(rows[0] - 1, rows[2] + 2):  # the knuckle: a vertical barrel
            put(img, 1, y, ramp(HOOP, 0.95 if y == rows[0] - 1 else 0.78))
            put(img, 2, y, ramp(HOOP, 0.4))
        put(img, 1, rows[2] + 2, ramp(HOOP, 0.1))
        put(img, x1, rows[1], HOOP[6])  # the strap's rivet
        put(img, x1, rows[2], ramp(HOOP, 0.1))
    # the handle bar on the right: brackets top and bottom, a round bar standing off the plates
    for y in range(9, 14):
        bracket = y in (9, 13)
        put(img, 11, y, ramp(HOOP, 0.5) if bracket else ramp(STEEL, 0.95 if y == 10 else 0.78))
        put(img, 12, y, ramp(STEEL, 0.06))  # its shadow
    put(img, 10, 9, ramp(HOOP, 0.3))
    put(img, 10, 13, ramp(HOOP, 0.3))
    return img


def high_front_glow():
    cx, cy = 7.5, 5.5

    def draw(f):
        img = Image.new("RGBA", (16, 16))
        a = f / 8 * 6.283
        for (x, y) in HATCH:
            dx, dy = x - cx, y - cy
            r = math.hypot(dx, dy)
            ang = math.atan2(dy, dx)
            swirl = math.sin(ang * 2 + r * 1.3 - a) + 0.6 * math.sin(ang * 3 - r * 0.9 + a * 0.5 + 1.0)
            t = 0.82 - r * 0.07 + random.Random(x * 7 + y * 13 + f).uniform(-0.04, 0.04)
            if swirl > 0.75:
                t -= 0.45  # the dark crust drifting across the melt
            elif swirl > 0.4:
                t -= 0.2
            img.putpixel((x, y), ramp(MOLTEN, t) + (255,))
        return img
    return frames(8, draw)


def hatch_cover():
    img = Image.new("RGBA", (16, 16))
    cx, cy = 7.5, 5.5
    rng = random.Random(1310)
    for (x, y) in disc(cx, cy, 3.05):
        d = math.hypot(x - cx, y - cy)
        lit = -((x - cx) + (y - cy)) / 3.05
        t = 0.45 + lit * 0.22 + rng.uniform(-0.03, 0.03)
        if d > 2.4:
            t = 0.2 + lit * 0.35  # the cover's rolled rim
        put(img, x, y, ramp(CAST, t + 0.25))
    put(img, 8, 5, ramp(CAST, 0.98))  # the knob
    put(img, 9, 5, ramp(CAST, 0.6))
    put(img, 8, 6, ramp(CAST, 0.3))
    put(img, 9, 6, ramp(CAST, 0.1))
    return img


# ================================================================ Taphole
TAP_HOLE = sorted(disc(7.5, 7.5, 2.95))  # x 5..10, y 5..10


def taphole(open_):
    rng = random.Random(1401)
    img = casing(1400)
    bevel(img, 2, 2, 13, 13, STEEL, 0.42, rng, hi=0.16, lo=0.26)  # frame
    bevel(img, 4, 4, 11, 11, STEEL, 0.3, rng, hi=-0.12, lo=0.06)    # the recessed plate inside it
    for x in range(4, 12):
        for y in range(4, 7):
            put(img, x, y, mix(get(img, x, y), SOOT, 0.25 - (y - 4) * 0.07))
    for x, y in ((3, 3), (12, 3), (3, 12), (12, 12), (7, 3), (3, 7), (12, 8), (8, 12)):
        put(img, x, y, ramp(STEEL, 0.92))
        put(img, x + 1, y + 1, ramp(STEEL, 0.2))
    cx = cy = 7.5
    for (x, y) in TAP_HOLE:
        d = math.hypot(x - cx, y - cy)
        lit = -((x - cx) + (y - cy)) / 2.95
        if open_:
            c = VOID if d < 2.2 else mix((54, 22, 14), (126, 46, 18), 0.5 - lit * 0.5)  # heat-red rim inside
        else:
            c = ramp(CLAYPLUG, 0.5 + lit * 0.35 - d * 0.06 + rng.uniform(-0.05, 0.05))  # the domed plug
        put(img, x, y, c)
    if not open_:
        for x, y in ((6, 6), (7, 7), (7, 8), (9, 6), (8, 9), (6, 9)):  # cracks in the dried clay
            put(img, x, y, ramp(CLAYPLUG, 0.08))
        put(img, 6, 5, ramp(CLAYPLUG, 1.0))  # the dome's catch light
    return img


def taphole_glow():
    cx = cy = 7.5

    def draw(f):
        img = Image.new("RGBA", (16, 16))
        for (x, y) in TAP_HOLE:
            d = math.hypot(x - cx, y - cy)
            t = 0.85 - d * 0.16 + 0.08 * math.sin(f / 8 * 6.283 + x + y * 0.7)
            img.putpixel((x, y), ramp(MOLTEN, t) + (255,))
        return img
    return frames(8, draw)


# ================================================================ Great Bellows
LEATHER = [(30, 16, 9), (46, 25, 14), (64, 36, 20), (86, 50, 28), (112, 68, 38), (142, 92, 54)]
OAK = [(58, 40, 20), (90, 64, 34), (120, 88, 50), (148, 112, 66), (172, 136, 84), (194, 158, 102)]
DARKWOOD = [(26, 17, 10), (42, 28, 16), (60, 41, 24), (80, 56, 33), (100, 72, 43), (122, 90, 56)]
IRON = [(22, 22, 25), (40, 40, 44), (62, 62, 67), (90, 90, 96), (124, 124, 130), (164, 164, 170)]
THREAD = (196, 172, 122)


def bellows_leather():
    rng = random.Random(1501)
    img = Image.new("RGBA", (16, 16))
    for x in range(16):
        off = round(0.75 * math.sin(x * 0.3927 + 0.7))  # the pleats sag gently (tiles at 16)
        for y in range(16):
            r = (y - off) % 4  # 0 fold crest (lit), 3 crease
            t = (0.72, 0.52, 0.36, 0.08)[r] + rng.uniform(-0.025, 0.025)
            if r == 0 and (x * 5 + y * 3) % 16 < 5:
                t += 0.14  # oil sheen along the crest
            if r == 1 and rng.random() < 0.05:
                t -= 0.18  # a small crinkle
            put(img, x, y, ramp(LEATHER, t))
    for y in range(16):  # a sewn seam down the hide: a dark ridge, stitches either side
        put(img, 11, y, ramp(LEATHER, 0.02))
        if y % 4 == 1:
            put(img, 10, y, mix(THREAD, get(img, 10, y), 0.4))
        if y % 4 == 3:
            put(img, 12, y, mix(THREAD, get(img, 12, y), 0.4))
    return img


def bellows_board():
    rng = random.Random(1502)
    img = Image.new("RGBA", (16, 16))
    for x in range(16):
        board, k = x // 4, x % 4
        tone = random.Random(1510 + board).uniform(-0.08, 0.08)
        for y in range(16):
            t = 0.55 + tone + (0.1 if k == 0 else -0.12 if k == 3 else 0) + rng.uniform(-0.04, 0.04)
            if (x * 7 + board * 3) % 5 == 0 and (y + board * 5) % 7 < 4:
                t -= 0.1  # grain lines
            put(img, x, y, ramp(OAK, t))
    for y in range(16):
        for x in (3, 7, 11, 15):
            put(img, x, y, ramp(OAK, 0.12))
    for rows in ((2, 3), (12, 13)):  # iron straps with nail heads on every board
        for x in range(16):
            put(img, x, rows[0], ramp(IRON, 0.62 + rng.uniform(-0.04, 0.04)))
            put(img, x, rows[1], ramp(IRON, 0.32 + rng.uniform(-0.04, 0.04)))
            put(img, x, rows[1] + 1, mix(get(img, x, rows[1] + 1), SOOT, 0.3))
        for x in (1, 5, 9, 13):
            put(img, x, rows[0], ramp(IRON, 1.0))
            put(img, x + 1, rows[1], ramp(IRON, 0.1))
    return img


def bellows_frame():
    rng = random.Random(1503)
    img = Image.new("RGBA", (16, 16))
    lines = {x: random.Random(1520 + x).uniform(-0.1, 0.06) for x in range(16)}
    for x in range(16):
        for y in range(16):
            t = 0.52 + lines[x] + rng.uniform(-0.03, 0.03)
            if x in (3, 9, 13):
                t -= 0.1 + 0.06 * math.sin(y / 16 * 6.283 * 2 + x)  # long grain lines
            if x == 0:
                t += 0.14  # chamfered arris catching the light
            if x == 15:
                t -= 0.18
            put(img, x, y, ramp(DARKWOOD, t))
    return img


def bellows_frame_end():
    img = Image.new("RGBA", (16, 16))
    rng = random.Random(1504)
    for x in range(16):
        for y in range(16):
            d = math.hypot(x - 7.2, y - 8.3)
            t = 0.55 + 0.12 * math.sin(d * 2.2) + rng.uniform(-0.03, 0.03)
            if min(x, y, 15 - x, 15 - y) == 0:
                t -= 0.2
            put(img, x, y, ramp(DARKWOOD, t + 0.1))
    for d in range(2, 6):  # the radial check
        put(img, 7 + d, 8 - d // 3, ramp(DARKWOOD, 0.1 if d < 5 else 0.3))
    put(img, 7, 8, ramp(DARKWOOD, 0.2))
    return img


def bellows_nozzle():
    """Bronze pipe running along x: rounded across y, darker collars at x 0-1 and 8-9."""
    rng = random.Random(1505)
    img = Image.new("RGBA", (16, 16))
    for x in range(16):
        for y in range(16):
            c = (y - 7.5) / 8
            t = 0.46 - c * 0.28 - (0.2 if abs(c) > 0.82 else 0) + (0.2 if y in (3, 4) else 0) + rng.uniform(-0.03, 0.03)
            if x % 8 in (0, 1):
                t -= 0.24 if x % 8 == 1 else 0.06
            if x % 8 == 2:
                t += 0.08
            put(img, x, y, ramp(BRONZE, t))
    return img


def bellows_cam():
    rng = random.Random(1506)
    img = Image.new("RGBA", (16, 16))
    for x in range(16):
        for y in range(16):
            t = 0.5 + (0.14 if y == 0 else -0.16 if y == 15 else 0) + rng.uniform(-0.04, 0.04)
            if (y * 5 + x // 6) % 7 == 0:
                t -= 0.12  # grain along the arm
            put(img, x, y, ramp(OAK, t))
    for x0 in (0, 13):  # iron end fittings
        for x in range(x0, x0 + 3):
            for y in range(16):
                put(img, x, y, ramp(IRON, 0.55 + (0.2 if x == x0 else -0.18 if x == x0 + 2 else 0) + (0.1 if y == 0 else 0) + rng.uniform(-0.03, 0.03)))
        for y in (3, 12):
            put(img, x0 + 1, y, ramp(IRON, 1.0))
            put(img, x0 + 2, y + 1, ramp(IRON, 0.1))
    for (x, y) in disc(7.5, 7.5, 2.6):  # the boss where the shaft goes through
        d = math.hypot(x - 7.5, y - 7.5)
        lit = -((x - 7.5) + (y - 7.5)) / 2.6
        put(img, x, y, ramp(IRON, (0.15 if d < 1.0 else 0.55 + lit * 0.3)))
    return img


# ================================================================ items
def recolor_shape(src, pal, seed, speck=None, speck_p=0.0, lit_min=0.0):
    rng = random.Random(seed)
    pts = px(src)
    ls = [lum(src.getpixel(p)) for p in pts]
    lo, hi = min(ls), max(ls)
    out = Image.new("RGBA", (16, 16))
    for p in pts:
        t = (lum(src.getpixel(p)) - lo) / max(1, hi - lo)
        c = ramp(pal, t * 0.9 + 0.1)
        if speck and rng.random() < speck_p and t > lit_min:
            c = mix(c, speck, 0.6)
        out.putpixel(p, c + (src.getpixel(p)[3],))
    return out


def crushed_quartzite():
    src = Image.open(ITEM / "crushed_copper_ore.png").convert("RGBA")
    pal = [(92, 88, 90), (150, 146, 148), (196, 192, 190), (224, 221, 216), (242, 240, 236)]
    img = recolor_shape(src, pal, 1601)
    rng = random.Random(1602)
    pts = px(img)
    lit = [p for p in pts if lum(get(img, *p)) > 200]
    for p in rng.sample(lit, min(5, len(lit))):
        img.putpixel(p, (255, 255, 255, 255))  # grains catching the light
    for p in rng.sample(pts, 4):
        if lum(get(img, *p)) > 120:
            img.putpixel(p, mix(get(img, *p), (214, 190, 170), 0.5) + (255,))  # the rock's faint pink
    return img


def tuyere_item():
    """A thick bronze nozzle lying on its side: the flange (a disc seen at an angle) on the left, the barrel tapering
    to the right, its dark bore showing at the tip."""
    img = Image.new("RGBA", (16, 16))
    cy = 8.5
    for x in range(16):
        X = x + 0.5
        for y in range(16):
            Y = y + 0.5
            if ((X - 3.4) / 1.9) ** 2 + ((Y - cy) / 6.0) ** 2 <= 1:  # the flange's face
                t = 0.78 - (Y - cy) / 6 * 0.32 - (0.22 if X > 4.4 else 0)
                put(img, x, y, ramp(BRONZE, t))
                continue
            if 4 <= x <= 14:
                h = 3.1 - (X - 5) * 0.17
                c = (Y - cy) / h
                if abs(c) <= 1:
                    t = 0.56 - c * 0.34
                    if -0.75 < c < -0.3:
                        t += 0.24  # the long highlight
                    if c > 0.7:
                        t -= 0.14
                    if x == 9:
                        t -= 0.2  # a cast ring round the barrel
                    if x == 10:
                        t += 0.06
                    if x == 14:
                        t = 0.12 if abs(c) < 0.55 else 0.7 - c * 0.3  # the bore and the lip round it
                    put(img, x, y, ramp(BRONZE, t))
    return outline(img, (40, 22, 8))


HOOP_ITEM = [
    "................",
    "................",
    "................",
    "....OOOOOOOO....",
    "..OOHHLLLLLMOO..",
    ".OHLKKKKKKKKMMO.",
    "OHLK........KMDO",
    "OLMO........OMDO",
    "OLMO........ODDO",
    "OLLMOOOOOOOODMDO",
    "OHLLHHHLLLLLMMDO",
    "OLxMMMxMMMxMMxDO",
    ".OMMMDDMDDDDDDO.",
    "..OOKDDDDDDDKOO.",
    "....OOOOOOOO....",
    "................",
]


def steel_hoop():
    """A blue-black steel band bent into a hoop, seen from above at an angle, with rivet holes."""
    pal = {"O": (8, 10, 14), "K": HOOP[1], "D": HOOP[2], "M": HOOP[3], "L": HOOP[4], "H": HOOP[5], "x": (4, 4, 6)}
    img = Image.new("RGBA", (16, 16))
    for y, row in enumerate(HOOP_ITEM):
        for x, ch in enumerate(row):
            if ch in pal:
                put(img, x, y, pal[ch])
    for x in range(16):  # a bright catch light along the near rim, and under each rivet hole
        if HOOP_ITEM[10][x] == "H":
            put(img, x, 10, HOOP[6])
        if HOOP_ITEM[11][x] == "x":
            put(img, x, 12, HOOP[4])
    return img


TUYERE_ROWS = [
    "................",
    "................",
    "................",
    "................",
    "..##............",
    "..##########....",
    "..############..",
    "..############..",
    "..############..",
    "..############..",
    "..##########....",
    "..##............",
    "................",
    "................",
    "................",
    "................",
]


# ---- molds, exactly as gen_bloomery_textures.py (helpers copied) ----
def cavity(m, x, y):
    return 0 <= x < 16 and 0 <= y < 16 and m.getpixel((x, y))[3] > 0


def mold_top(m, base, pal, seed):
    rng = random.Random(seed)
    img = base.copy() if base else Image.new("RGBA", (16, 16))
    for x in range(16):
        for y in range(16):
            if base is None:
                img.putpixel((x, y), ramp(pal, 0.55 + rng.uniform(-0.12, 0.12)) + (255,))
            if cavity(m, x, y):
                shade = 0.12 if not cavity(m, x - 1, y - 1) else 0.28
                img.putpixel((x, y), ramp(pal, shade + rng.uniform(0, 0.08)) + (255,))
            elif cavity(m, x - 1, y - 1) or cavity(m, x - 1, y) or cavity(m, x, y - 1):
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


def tuyere_molds():
    m = Image.new("RGBA", (16, 16))
    for y, row in enumerate(TUYERE_ROWS):
        for x, ch in enumerate(row):
            if ch == "#":
                m.putpixel((x, y), BRONZE[2] + (255,))
    fired_tex = Image.open(BLOCK / "kiln_clay_fired.png").convert("RGBA")
    i = 0
    save(mold_top(m, fired_tex, FIRED, 760 + i), BLOCK / "clay_tuyere_mold_top.png")
    save(item_icon(mold_top(m, None, RAW, 780 + i)), ITEM / "unfired_clay_tuyere_mold.png")
    solid = Image.new("RGBA", (16, 16))
    for p in px(m):
        t = 0.5 + (0.25 if not cavity(m, p[0] - 1, p[1] - 1) else 0) + random.Random(p[0] * 31 + p[1]).uniform(-0.1, 0.1)
        solid.putpixel(p, ramp(BRONZE[1:], t) + (255,))
    save(solid, BLOCK / "clay_tuyere_mold_bronze_solid.png")
    molten = Image.new("RGBA", (16, 64))
    for f in range(4):
        rng = random.Random(f * 11 + i + 790)
        for x, y in px(m):
            molten.putpixel((x, y + f * 16), ramp(MOLTEN, 0.55 + rng.uniform(-0.15, 0.35)) + (255,))
    save(molten, BLOCK / "clay_tuyere_mold_bronze_molten.png", anim=8)


def brick_items():
    brick = vanilla("item/brick")
    fired = recolor_shape(brick, SILICA, 1701, speck=OCHRE, speck_p=0.1, lit_min=0.3)
    raw = recolor_shape(brick, [(118, 114, 104), (158, 154, 142), (190, 187, 175), (212, 209, 198), (228, 226, 216)], 1702)
    rng = random.Random(1703)
    for p in px(raw):
        r = rng.random()
        if r < 0.1 and lum(get(raw, *p)) > 140:
            raw.putpixel(p, (242, 240, 232, 255))  # quartz grains
        elif r < 0.16:
            raw.putpixel(p, mix(get(raw, *p), (110, 104, 92), 0.45) + (255,))
    return fired, raw


# ================================================================ v2: an old stone blast furnace (Bonawe, Hopewell, masugn)
# The wall is warm grey-buff refractory brick in an irregular running bond; the iron straps, the rusticated stone base
# with its hearth arches and the iron charging rim are separate 3D elements in the models, so nothing is painted over
# the brick. Casing, upper (sooty) and hot (heat-stained) share one brick layout, so they stack seamlessly and the hot
# glow lights exactly the hot casing's joints.
BUFF = [(70, 63, 54), (98, 89, 77), (123, 113, 97), (143, 131, 113), (161, 149, 129), (179, 167, 146), (198, 187, 166)]
WMORTAR = [(62, 59, 55), (84, 80, 74), (104, 100, 92), (122, 118, 109)]
STONE = [(54, 52, 49), (78, 75, 70), (101, 97, 90), (123, 118, 109), (142, 136, 125), (160, 154, 142), (180, 174, 162)]
ARCH = [(22, 20, 19), (36, 33, 30), (52, 48, 43), (70, 65, 58), (90, 84, 75), (112, 105, 94)]
IRONR = [(26, 24, 23), (40, 37, 35), (56, 52, 48), (74, 69, 64), (96, 90, 83), (122, 115, 106), (154, 147, 138)]
RUST = [(92, 46, 24), (128, 66, 34), (156, 90, 46), (178, 116, 64)]
SCORCH = (34, 26, 22)
TEMPER = [(170, 134, 72), (126, 72, 96), (74, 70, 132), (70, 102, 150)]  # straw, purple, blue, pale blue
WALL_COURSES = ((0, 1, 2), (4, 5, 6), (8, 9, 10), (12, 13, 14))  # joints on rows 3, 7, 11, 15 (block edge on a joint)
WALL_UNITS = ((6, 4, 6), (9, 7), (5, 7, 4), (7, 3, 6))  # brick + its joint: long, short and closer bricks
TOP_COURSES = ((0, 1, 2), (4, 5, 6), (8, 9, 10), (12, 13, 14))
TOP_UNITS = ((7, 9), (4, 8, 4), (9, 7), (3, 8, 5))


def bond_offsets(units):
    """Each course's start offset, keeping its head joints as far as possible from the courses above and below."""
    def joints(i, off):
        xs, x = [], off
        for u in units[i]:
            x += u
            xs.append((x - 1) % 16)
        return xs

    def gap(i, a, j, b):
        return min(min(abs(p - q), 16 - abs(p - q)) for p in joints(i, a) for q in joints(j, b))
    best = None
    n = len(units)
    for o in ((0, a, b, c) for a in range(16) for b in range(16) for c in range(16)):
        gaps = [gap(i, o[i], (i + 1) % n, o[(i + 1) % n]) for i in range(n)]
        score = (min(gaps), sum(gaps))
        if best is None or score > best[0]:
            best = (score, o)
    return best[1]


def brick_layout(courses, units, seed):
    """(x, y) -> ("joint", kind) or ("brick", id, row, rows, col, length). A few brick corners are chipped (the joint
    is wider there) and a few joints have lost their mortar (kind "deep")."""
    rng = random.Random(seed)
    cells = {}
    for x in range(16):
        for y in range(16):
            cells[(x, y)] = ("joint", "bed")
    offsets = bond_offsets(units)
    for ci, rows in enumerate(courses):
        x = offsets[ci]
        for bi, u in enumerate(units[ci]):
            for k in range(u):
                for ri, y in enumerate(rows):
                    cells[((x + k) % 16, y)] = ("joint", "head") if k == u - 1 else ("brick", (ci, bi), ri, len(rows), k, u - 1)
            x += u
    for p, c in list(cells.items()):
        if c[0] != "brick":
            continue
        _, bid, ri, rn, k, ln = c
        corner = (ri in (0, rn - 1)) and (k in (0, ln - 1))
        if corner and rng.random() < 0.22:
            cells[p] = ("joint", "chip")
    for p, c in list(cells.items()):
        if c == ("joint", "bed") and rng.random() < 0.09:
            cells[p] = ("joint", "deep")
    return cells


WALL = brick_layout(WALL_COURSES, WALL_UNITS, 5000)
WALL_TOP = brick_layout(TOP_COURSES, TOP_UNITS, 5001)


def brick_wall(cells, seed, pal=BUFF, base=0.55):
    """The bare bricks: a tone per brick (some warmer, some greyer, the odd dark clinker), lit from the top left."""
    rng = random.Random(seed)
    img = Image.new("RGBA", (16, 16))
    tones = {}
    for (x, y), cell in sorted(cells.items()):
        if cell[0] == "joint":
            t = {"bed": 0.62, "head": 0.5, "chip": 0.45, "deep": 0.12}[cell[1]] + rng.uniform(-0.08, 0.08)
            put(img, x, y, ramp(WMORTAR, t))
            continue
        _, bid, ri, rn, k, ln = cell
        if bid not in tones:
            r = rng.random()
            kind = "clinker" if r < 0.1 else "warm" if r < 0.32 else "grey" if r < 0.5 else "plain"
            tones[bid] = (rng.uniform(-0.1, 0.08) - (0.17 if kind == "clinker" else 0), kind)
        tone, kind = tones[bid]
        t = base + tone + rng.uniform(-0.035, 0.035)
        t += 0.09 if ri == 0 else (-0.08 if ri == rn - 1 else 0)
        t += 0.04 if k == 0 else (-0.06 if k == ln - 1 else 0)
        r = rng.random()
        if r < 0.06:
            t -= 0.16  # a pit in the fired clay
        elif r < 0.09:
            t += 0.1  # a pale grain
        c = ramp(pal, t)
        c = mix(c, {"warm": (156, 116, 78), "grey": (128, 127, 123), "clinker": (86, 64, 52)}.get(kind, c),
                {"warm": 0.2, "grey": 0.28, "clinker": 0.35}.get(kind, 0))
        put(img, x, y, c)
    return img


def casing2(variant="base"):
    """variant: base (light soot), upper (heavy soot rising, darker to the top), hot (heat stains over the hearth)."""
    img = brick_wall(WALL, 5010)
    rng = random.Random(5011)
    soot = tnoise(5012, 8)
    fine = tnoise(5013, 4)
    for x in range(16):
        for y in range(16):
            c = get(img, x, y)
            joint = WALL[(x, y)][0] == "joint"
            f = max(0.0, soot[y][x] - 0.45) * 0.4 + (0.06 if joint else 0)
            if variant == "upper":
                streak = 0.5 + 0.5 * math.sin(x * 1.9 + 1.3) * math.sin(x * 0.7 + 4.0)  # columns of rising smoke
                f = 0.27 + 0.14 * (15 - y) / 15 + streak * 0.26 * (0.6 + 0.4 * fine[y][x]) + f * 0.6
            if variant == "hot":
                rust = tnoise(5014, 8)[y][x]
                tint = tnoise(5017, 8)[y][x]
                f += 0.06
                if rust > 0.38:
                    c = mix(c, RUST[1] if fine[y][x] > 0.5 else RUST[2], min(0.55, (rust - 0.38) * 1.3))
                if tint > 0.7 and not joint:
                    c = mix(c, TEMPER[1] if fine[y][x] < 0.5 else TEMPER[2], min(0.3, (tint - 0.7) * 1.6))
                if joint:
                    c = mix(c, SCORCH, 0.55)
                elif any(WALL.get(((x + dx) % 16, (y + dy) % 16), ("",))[0] == "joint" for dx, dy in ((0, 1), (0, -1))):
                    f += 0.1  # the scorch bleeds out of the joints into the brick edges
            put(img, x, y, mix(c, SOOT, min(0.7, f * rng.uniform(0.85, 1.1))))
    if variant == "hot":
        for p in WALL_CRACKS:
            put(img, *p, mix(get(img, *p), SCORCH, 0.38))
    return img


def _cracks():
    """Hairline cracks running from a joint into a brick, 2-3 px, kept to the hot casing's bricks."""
    out = []
    for sx, sy, dx, dy, n in ((3, 1, 1, 1, 3), (11, 5, -1, 1, 2), (6, 9, 1, 1, 2), (13, 13, -1, 0, 3), (1, 12, 1, 1, 2)):
        for i in range(n):
            p = ((sx + dx * i) % 16, (sy + dy * i) % 16)
            if WALL[p][0] == "brick":
                out.append(p)
    return out


WALL_CRACKS = _cracks()
WALL_JOINTS = sorted(p for p, c in WALL.items() if c[0] == "joint")


def casing2_glow(stage):
    pal = HEAT[stage] if stage == 1 else HEAT[stage - 1][1:] + HEAT[stage]  # brightest in the deep joints, dull elsewhere
    phase = {p: random.Random(p[0] * 17 + p[1] * 31).uniform(0, 6.28) for p in WALL_JOINTS + WALL_CRACKS}
    rng = random.Random(5100 + stage)
    base = {p: rng.uniform(0.18, 0.62) + {"deep": 0.25, "chip": -0.1, "head": -0.05}.get(WALL[p][1], 0) for p in WALL_JOINTS}
    cracks = WALL_CRACKS[: len(WALL_CRACKS) // 2] if stage == 2 else WALL_CRACKS if stage == 3 else []

    def draw(f):
        img = Image.new("RGBA", (16, 16))
        for p in WALL_JOINTS:
            put(img, *p, ramp(pal, base[p] + 0.22 * math.sin(f / 8 * 6.283 + phase[p])))
        for p in cracks:
            put(img, *p, ramp(pal, 0.2 + 0.2 * math.sin(f / 8 * 6.283 + phase[p])))
        return img
    return frames(8, draw)


def casing2_top():
    img = brick_wall(WALL_TOP, 5020, base=0.5)
    soot, ash = tnoise(5021, 8), tnoise(5022, 4)
    rng = random.Random(5023)
    for x in range(16):
        for y in range(16):
            c = mix(get(img, x, y), SOOT, 0.14 + soot[y][x] * 0.28)
            if ash[y][x] > 0.7 and rng.random() < 0.5:
                c = mix(c, (128, 124, 118), 0.35)  # grey ash settled on the bricks
            put(img, x, y, c)
    return img


# ---------------------------------------------------------------- the rusticated stone base
BASE_COURSES = ((range(0, 7), 0, (9, 7)), (range(8, 15), 4, (8, 8)))  # rows, start x, stone + joint widths


def ashlar(seed, relief=1.0, pal=STONE, base=0.5):
    """Big rock-faced ashlar blocks, two courses: a bevelled, chipped arris lit top left, shadowed bottom right, a deep
    dark joint between them. relief 0 gives the flat dressed top of the same stones."""
    rng = random.Random(seed)
    rough = tnoise(seed + 1, 2)
    img = Image.new("RGBA", (16, 16))
    for x in range(16):
        for y in range(16):
            put(img, x, y, ramp(pal, 0.06 + rng.uniform(0, 0.06)))
    for si, (rows, x0, widths) in enumerate(BASE_COURSES):
        rows = list(rows)
        x = x0
        for bi, w in enumerate(widths):
            xs = [(x + k) % 16 for k in range(w - 1)]
            tone = rng.uniform(-0.08, 0.08)
            for k, xx in enumerate(xs):
                for r, y in enumerate(rows):
                    dl, dr, dt, db = k, len(xs) - 1 - k, r, len(rows) - 1 - r
                    t = base + tone + (rough[y][xx] - 0.5) * 0.28 * relief + rng.uniform(-0.03, 0.03)
                    if min(dl, dt) == 0 and min(dr, db) > 0:
                        t += 0.22 * relief + 0.05
                    elif min(dr, db) == 0:
                        t -= 0.24 * relief + 0.06
                    elif min(dl, dt) == 1 and relief:
                        t += 0.07 * relief  # the bevel's second step
                    elif min(dr, db) == 1 and relief:
                        t -= 0.07 * relief
                    else:
                        t += 0.04 * relief * (1 - (abs(k - len(xs) / 2) + abs(r - len(rows) / 2)) / 7)  # the pillowed face
                    edge = min(dl, dr, dt, db) == 0
                    if edge and rng.random() < 0.18 * relief:
                        t = 0.1  # a chip knocked off the arris
                    elif rng.random() < 0.05:
                        t -= 0.14  # a tool mark / pit
                    put(img, xx, y, ramp(pal, t))
            x += w
    return img


def furnace_base():
    img = ashlar(5200, base=0.47)
    soot, heat = tnoise(5201, 8), tnoise(5202, 4)
    for x in range(16):
        for y in range(16):
            c = get(img, x, y)
            if heat[y][x] > 0.6:
                c = mix(c, (118, 82, 56), (heat[y][x] - 0.6) * 1.3)  # heat stains, rusty brown
            c = mix(c, SOOT, 0.08 + max(0.0, soot[y][x] - 0.4) * 0.5 + (0.08 if y < 7 else 0))
            put(img, x, y, c)
    return img


def furnace_base_top():
    img = ashlar(5210, relief=0.35, base=0.55)
    rng = random.Random(5211)
    dust = tnoise(5212, 4)
    for x in range(16):
        for y in range(16):
            c = mix(get(img, x, y), SOOT, 0.06 + dust[y][x] * 0.18)
            if (x + y * 2) % 5 == 0 and rng.random() < 0.35 and c != get(img, x, y):
                c = mix(c, STONE[2], 0.25)  # diagonal tooling of the dressed face
            put(img, x, y, c)
    return img


# ---------------------------------------------------------------- the hearth arches
ARCH_JOINT_ROWS = ({2, 7, 11, 15}, {4, 9, 13}, {1, 5, 10, 14}, {3, 7, 12})  # per 4 px column: stones of 3-5 px
ARCH_JOINTS = sorted({(x, y) for x in range(16) for y in range(16) if x % 4 == 3 or y in ARCH_JOINT_ROWS[x // 4]})


def hearth_arch():
    """Soot-blackened arch stones in columns 4 px wide (x 0..2, 4..6, 8..10, 12..14), so a 3-4 px strip is always a
    neat stack of stones; a few heat-glazed spots with a brown-orange sheen."""
    rng = random.Random(5300)
    soot = tnoise(5301, 4)
    img = Image.new("RGBA", (16, 16))
    joints = set(ARCH_JOINTS)
    for x in range(16):
        for y in range(16):
            if (x, y) in joints:
                put(img, x, y, ramp(ARCH, 0.03 + rng.uniform(0, 0.06)))
                continue
            up = (x, (y - 1) % 16) in joints
            down = (x, (y + 1) % 16) in joints
            left = ((x - 1) % 16, y) in joints
            right = ((x + 1) % 16, y) in joints
            tone = random.Random(5310 + x // 4 * 31 + sum(1 for j in ARCH_JOINT_ROWS[x // 4] if j < y) * 7).uniform(-0.08, 0.1)
            t = 0.55 + tone + rng.uniform(-0.04, 0.04) + (0.16 if up else 0) + (0.07 if left and not up else 0)
            t -= 0.2 if down else 0.08 if right else 0
            t -= soot[y][x] * 0.25
            put(img, x, y, ramp(ARCH, t))
    for gx, gy in ((1, 4), (9, 0), (5, 11), (13, 7), (10, 12)):  # glazed spots where the flame licked
        for (dx, dy), f in (((0, 0), 0.55), ((1, 0), 0.4), ((0, 1), 0.3)):
            p = (gx + dx, gy + dy)
            if p not in joints:
                put(img, *p, mix(get(img, *p), (104, 62, 34), f))
        put(img, gx, gy, mix(get(img, gx, gy), (150, 104, 66), 0.6))  # the glaze's sheen
    return img


def hearth_arch_glow():
    edge = {(x, y) for x in range(16) for y in range(16) if x in (0, 15) or y == 0}
    pts = sorted(set(ARCH_JOINTS) | edge)

    def draw(f):
        img = Image.new("RGBA", (16, 16))
        for (x, y) in pts:
            ph = random.Random(x * 13 + y * 29).uniform(0, 6.283)
            t = (0.62 if (x, y) in edge else 0.3) + 0.2 * math.sin(f / 8 * 6.283 + ph)
            put(img, x, y, ramp(HEAT[2], t))
        return img
    return frames(8, draw)


# ---------------------------------------------------------------- iron: straps, bolt plate, charging rim
def iron_field(seed, rust_amt=1.0):
    rng = random.Random(seed)
    nz, fine = tnoise(seed + 1, 4), tnoise(seed + 2, 2)
    img = Image.new("RGBA", (16, 16))
    for x in range(16):
        for y in range(16):
            c = ramp(IRONR, 0.42 + (fine[y][x] - 0.5) * 0.14 + rng.uniform(-0.03, 0.03))
            r = nz[y][x] * 0.7 + fine[y][x] * 0.3
            if r > 0.56:
                c = mix(c, RUST[0] if r < 0.66 else RUST[1] if r < 0.74 else RUST[2], min(0.8, (r - 0.5) * 2.2 * rust_amt))
            put(img, x, y, c)
    return img


def strap_bolt(img, x, y):
    """A 2x2 domed bolt head on a 2 px band."""
    put(img, x, y, ramp(IRONR, 0.95))
    put(img, x + 1, y, ramp(IRONR, 0.62))
    put(img, x, y + 1, ramp(IRONR, 0.5))
    put(img, x + 1, y + 1, ramp(IRONR, 0.1))


def furnace_strap():
    img = iron_field(5400, rust_amt=0.55)
    for x in range(16):
        for y in range(16):
            c = get(img, x, y)
            put(img, x, y, mix(c, IRONR[5], 0.28) if y % 2 == 0 else mix(c, IRONR[0], 0.12))  # a lit top edge per 2 px band
    for x, y in ((1, 0), (13, 2), (2, 12), (14, 14), (9, 0), (6, 14), (0, 6), (13, 8)):
        strap_bolt(img, x, y)
    # the bolt plate, x 4..11 y 4..11: a bevelled plate, a big hex nut x 6..9 y 6..9 with its shadow
    rng = random.Random(5401)
    for x in range(4, 12):
        for y in range(4, 12):
            t = 0.56 + rng.uniform(-0.03, 0.03)
            if x == 11 or y == 11:
                t = 0.14  # the plate's thick edge in shadow
            elif x == 4 or y == 4:
                t = 0.82  # its lit top-left arris
            elif x == 10 or y == 10:
                t = 0.36
            c = ramp(IRONR, t)
            if (x * 3 + y * 5) % 7 == 0 and 4 < x < 10 and 4 < y < 10:
                c = mix(c, RUST[1], 0.45)
            put(img, x, y, c)
    nut = {(7, 6): 1.0, (8, 6): 0.9, (6, 7): 0.95, (7, 7): 0.8, (8, 7): 0.66, (9, 7): 0.42,
           (6, 8): 0.72, (7, 8): 0.6, (8, 8): 0.46, (9, 8): 0.26, (7, 9): 0.3, (8, 9): 0.16}
    for p, t in nut.items():
        put(img, *p, ramp(IRONR, t))
    put(img, 7, 7, (176, 168, 156))  # the bolt's end standing through the nut
    for p in ((9, 9), (10, 8), (8, 10), (10, 9), (9, 10)):
        put(img, *p, mix(get(img, *p), (0, 0, 0), 0.45))
    for p in ((6, 6), (9, 6), (6, 9)):
        put(img, *p, mix(get(img, *p), (0, 0, 0), 0.2))
    return img


def furnace_rim():
    """The charging lip: a heavy iron plate edge, the top rows heat-tempered straw / purple / blue, two rivet rows."""
    img = iron_field(5500, rust_amt=0.8)
    rng = random.Random(5501)
    for x in range(16):
        for y in range(16):
            c = get(img, x, y)
            if y == 0:
                c = mix(c, IRONR[6], 0.5)  # the rolled lip catching the light
            elif y in (1, 2, 3, 4):
                wob = math.sin(x * 0.785 + 1) * 0.6
                band = TEMPER[min(3, max(0, int(y - 1 + wob)))]
                c = mix(c, band, 0.42 - y * 0.05)
            if y == 7:
                c = mix(c, IRONR[0], 0.5)  # the lap seam
            if y == 8:
                c = mix(c, IRONR[5], 0.3)
            if y == 15:
                c = mix(c, IRONR[0], 0.45)
            put(img, x, y, c)
    for x in (1, 5, 9, 13):
        strap_bolt(img, x, 2)
        strap_bolt(img, (x + 2) % 16, 11)
    for x in range(16):  # rust runs down from the lower rivets
        if (x * 5) % 16 in (4, 15) and rng.random() < 0.9:
            for y in (13, 14):
                put(img, x, y, mix(get(img, x, y), RUST[1], 0.4))
    return img


def furnace_rim_glow():
    drips = [(1, 3, 0.0), (4, 5, 2.1), (7, 2, 4.0), (10, 6, 1.0), (13, 4, 3.2)]  # x, length, phase

    def draw(f):
        img = Image.new("RGBA", (16, 16))
        a = f / 8 * 6.283
        for x in range(16):
            for y in (0, 1):
                t = (0.85 if y == 0 else 0.6) + 0.12 * math.sin(a + x * 0.9)
                put(img, x, y, ramp(HEAT[3] if y == 0 else HEAT[2], t))
        for x, ln, ph in drips:
            cur = ln + round(1.2 * math.sin(a + ph))
            for i in range(cur):
                put(img, x, 2 + i, ramp(HEAT[2], 0.75 - i / max(1, cur) * 0.6))
        return img
    return frames(8, draw)


# ---------------------------------------------------------------- the fronts, recessed in the arch (full face)
def soot_top(img, rows=4, amt=0.35):
    for x in range(16):
        for y in range(rows):
            put(img, x, y, mix(get(img, x, y), SOOT, amt * (rows - y) / rows))


def high_front2():
    rng = random.Random(5601)
    img = Image.new("RGBA", (16, 16))
    bevel(img, 0, 0, 15, 15, STEEL, 0.2, rng, hi=0.06, lo=0.0, noise=0.02)  # the door's edge
    bevel(img, 1, 1, 14, 9, STEEL, 0.46, rng, hi=0.14, lo=0.22, noise=0.025)  # upper plate
    bevel(img, 1, 10, 14, 14, STEEL, 0.42, rng, hi=0.14, lo=0.22, noise=0.025)  # lower plate
    grime = tnoise(5602, 8)
    for x in range(16):
        for y in range(16):
            put(img, x, y, mix(get(img, x, y), SOOT, grime[y][x] * 0.28 + (0.12 if y > 11 else 0)))
    for x, y in ((2, 2), (12, 2), (2, 7), (12, 7), (4, 11), (8, 11), (12, 11), (2, 13), (12, 13)):
        put(img, x, y, ramp(STEEL, 0.82))
        put(img, x + 1, y, ramp(STEEL, 0.5))
        put(img, x + 1, y + 1, ramp(STEEL, 0.12))
    cx, cy = 7.5, 5.5
    for (x, y) in disc(cx, cy, 4.7) - disc(cx, cy, 3.75):  # heat tint round the hatch: straw, then blue
        put(img, x, y, mix(get(img, x, y), (160, 126, 70) if math.hypot(x - cx, y - cy) < 4.2 else (78, 96, 146), 0.4))
    for (x, y) in disc(cx, cy, 3.75):
        d = math.hypot(x - cx, y - cy)
        lit = -((x - cx) + (y - cy)) / d if d else 0
        put(img, x, y, (VOID if lit < 0.3 or d < 2.3 else (30, 20, 18)) if d <= 3.05 else ramp(STEEL, 0.42 + lit * 0.36))
    for (x, y) in disc(cx + 0.7, cy + 0.7, 3.75) - disc(cx, cy, 3.75):
        put(img, x, y, mix(get(img, x, y), (0, 0, 0), 0.4))
    for y0, x1 in ((3, 3), (11, 5)):  # strap hinges from the left edge, a rounded tip and rivet
        for x in range(0, x1 + 1):
            put(img, x, y0 - 1, mix(get(img, x, y0 - 1), (0, 0, 0), 0.4))
            put(img, x, y0, ramp(HOOP, 0.86 + rng.uniform(-0.03, 0.03)))
            put(img, x, y0 + 1, ramp(HOOP, 0.5 + rng.uniform(-0.03, 0.03)))
            put(img, x, y0 + 2, mix(get(img, x, y0 + 2), (0, 0, 0), 0.5))
        put(img, x1 + 1, y0, ramp(HOOP, 0.5))
        put(img, x1 + 1, y0 + 1, ramp(HOOP, 0.25))
        put(img, x1 - 1, y0, HOOP[6])
    for y in range(4, 10):  # the handle bar on the right, standing off the plate
        bracket = y in (4, 9)
        put(img, 12, y, ramp(HOOP, 0.5) if bracket else ramp(STEEL, 0.95 if y == 5 else 0.8))
        put(img, 13, y, ramp(STEEL, 0.08))
    put(img, 11, 4, ramp(HOOP, 0.3))
    put(img, 11, 9, ramp(HOOP, 0.3))
    soot_top(img, 3, 0.4)
    for x, y in ((6, 1), (7, 1), (8, 0), (9, 1), (7, 0)):  # smoke from the hatch has blackened the plate above it
        put(img, x, y, mix(get(img, x, y), SOOT, 0.4))
    return img


FB_TUYERE = (7.5, 3.5)
FB_SLOTS_X = (4, 6, 9, 11)
FB_SLOTS = [(x, y) for x in FB_SLOTS_X for y in range(8, 12)]
FB_MOUTH = sorted(disc(*FB_TUYERE, 1.15))


def firebox2():
    rng = random.Random(5701)
    img = plate(5700, CAST, base=0.44, mottle=0.16)
    cx, cy = FB_TUYERE
    for (x, y) in disc(cx, cy, 3.1):  # the bronze tuyere: flange, lip, dark mouth
        d = math.hypot(x - cx, y - cy)
        lit = -((x - cx) + (y - cy)) / 3.1
        if d <= 1.15:
            put(img, x, y, VOID)
        elif d <= 2.0:
            put(img, x, y, ramp(BRONZE, 0.55 - lit * 0.25))
        elif d <= 2.7:
            put(img, x, y, ramp(BRONZE, 0.58 + lit * 0.3 + rng.uniform(-0.04, 0.04)))
        else:
            put(img, x, y, ramp(BRONZE, 0.12))
    for (x, y) in disc(cx + 0.8, cy + 0.8, 3.1) - disc(cx, cy, 3.1):
        put(img, x, y, mix(get(img, x, y), (0, 0, 0), 0.35))
    for bx, by in ((5, 2), (10, 2), (5, 5), (10, 5)):
        put(img, bx, by, ramp(BRONZE, 0.95 if by == 2 else 0.7))
    for x in range(3, 13):  # the grate: a raised bar frame round four slots
        put(img, x, 7, ramp(CAST, 0.8))
        put(img, x, 12, ramp(CAST, 0.16))
        put(img, x, 13, mix(get(img, x, 13), (0, 0, 0), 0.3))
    for y in range(8, 12):
        for x in range(3, 13):
            if x in FB_SLOTS_X:
                put(img, x, y, VOID if y < 11 else (46, 20, 12))  # ash and a dead ember at the bottom
            else:
                left = x - 1 in FB_SLOTS_X or x == 3
                put(img, x, y, ramp(CAST, 0.7 if left else 0.4) if x not in (7, 8) else ramp(CAST, 0.62 if x == 7 else 0.36))
    for x in range(3, 13):
        put(img, x, 7, mix(get(img, x, 7), (110, 60, 36), 0.3))  # heat-browned over the slots
    for y0 in (2, 9):  # hinge straps from the left
        for x in range(0, 4):
            put(img, x, y0, ramp(CAST, 0.85))
            put(img, x, y0 + 1, ramp(CAST, 0.5))
            put(img, x, y0 + 2, mix(get(img, x, y0 + 2), (0, 0, 0), 0.4))
        put(img, 2, y0, ramp(CAST, 1.0))
    put(img, 12, 3, ramp(CAST, 0.95))  # the latch on the right
    put(img, 12, 4, ramp(CAST, 0.6))
    put(img, 13, 4, ramp(CAST, 0.1))
    put(img, 12, 5, ramp(CAST, 0.2))
    for x, y in ((3, 14), (12, 14), (7, 14)):
        plate_rivet(img, x, y, CAST)
    soot_top(img, 2, 0.35)
    return img


def firebox2_glow(stage):
    pal = HEAT[stage]

    def draw(f):
        rng = random.Random(5800 + stage * 10 + f)
        img = Image.new("RGBA", (16, 16))
        for (x, y) in FB_SLOTS:
            t = 0.25 + (y - 8) * 0.17 + math.sin(f / 8 * 6.283 * 2 + x * 1.7) * 0.12 + rng.uniform(-0.12, 0.12)
            put(img, x, y, ramp(pal, t + (0.3 if rng.random() < 0.12 else 0)))
        pulse = 0.55 + 0.3 * math.sin(f / 8 * 6.283)
        for p in FB_MOUTH:
            put(img, *p, ramp(pal, pulse + rng.uniform(-0.08, 0.08)))
        return img
    return frames(8, draw)


TAP2 = (7.5, 6.5)
TAP2_HOLE = sorted(disc(*TAP2, 2.95))  # x 5..10, y 4..9


def taphole2(open_):
    rng = random.Random(5901)
    img = plate(5900, STEEL, base=0.34, mottle=0.16)
    bevel(img, 2, 1, 13, 12, STEEL, 0.44, rng, hi=0.18, lo=0.28)   # the raised frame
    bevel(img, 4, 3, 11, 10, STEEL, 0.3, rng, hi=-0.12, lo=0.06)    # the plate recessed inside it
    for x in range(2, 14):
        put(img, x, 13, mix(get(img, x, 13), (0, 0, 0), 0.4))
    for x, y in ((2, 1), (12, 1), (2, 11), (12, 11), (7, 1), (2, 6), (12, 6), (7, 11)):
        put(img, x, y, ramp(STEEL, 0.8))
        put(img, x + 1, y, ramp(STEEL, 0.55))
        put(img, x, y + 1, ramp(STEEL, 0.45))
        put(img, x + 1, y + 1, ramp(STEEL, 0.12))
    for x in range(4, 12):
        for y in range(3, 5):
            put(img, x, y, mix(get(img, x, y), SOOT, 0.25 - (y - 3) * 0.1))
    cx, cy = TAP2
    for (x, y) in TAP2_HOLE:
        d = math.hypot(x - cx, y - cy)
        lit = -((x - cx) + (y - cy)) / 2.95
        if open_:
            c = VOID if d < 2.2 else mix((54, 22, 14), (126, 46, 18), 0.5 - lit * 0.5)
        else:
            c = ramp(CLAYPLUG, 0.5 + lit * 0.35 - d * 0.06 + rng.uniform(-0.05, 0.05))
        put(img, x, y, c)
    if not open_:
        for x, y in ((6, 5), (7, 6), (7, 7), (9, 5), (8, 8), (6, 8)):
            put(img, x, y, ramp(CLAYPLUG, 0.08))
        put(img, 6, 4, ramp(CLAYPLUG, 1.0))
    for x, y in ((7, 10), (8, 10), (7, 11), (8, 12), (8, 13), (8, 14), (7, 15)):  # old slag run down the plate
        put(img, x, y, mix(get(img, x, y), SLAGPAL[1], 0.6))
    return img


def taphole2_glow():
    cx, cy = TAP2

    def draw(f):
        img = Image.new("RGBA", (16, 16))
        for (x, y) in TAP2_HOLE:
            d = math.hypot(x - cx, y - cy)
            put(img, x, y, ramp(MOLTEN, 0.85 - d * 0.16 + 0.08 * math.sin(f / 8 * 6.283 + x + y * 0.7)))
        return img
    return frames(8, draw)


SILICA2 = [(92, 88, 80), (120, 114, 103), (142, 136, 123), (158, 152, 139), (172, 166, 152), (188, 182, 168)]
SILICA2_JOINT = [(86, 82, 76), (104, 100, 93), (122, 118, 110)]


# ================================================================ write: phase 1-2
def batch1():
    save(silica_bricks(1000, pal=SILICA2, speck=0.025, joint=SILICA2_JOINT), BLOCK / "silica_bricks.png")
    save(casing2("base"), BLOCK / "banded_furnace_casing.png")
    save(casing2("upper"), BLOCK / "banded_furnace_casing_upper.png")
    save(casing2("hot"), BLOCK / "banded_furnace_casing_hot.png")
    save(casing2_top(), BLOCK / "banded_furnace_casing_top.png")
    for s in (1, 2, 3):
        save(casing2_glow(s), BLOCK / f"banded_furnace_casing_glow_{s}.png", {"frametime": 4, "interpolate": True})
    save(furnace_base(), BLOCK / "furnace_base.png")
    save(furnace_base_top(), BLOCK / "furnace_base_top.png")
    save(hearth_arch(), BLOCK / "hearth_arch.png")
    save(hearth_arch_glow(), BLOCK / "hearth_arch_glow.png", {"frametime": 4, "interpolate": True})
    save(furnace_strap(), BLOCK / "furnace_strap.png")
    save(furnace_rim(), BLOCK / "furnace_rim.png")
    save(furnace_rim_glow(), BLOCK / "furnace_rim_glow.png", {"frametime": 4, "interpolate": True})
    save(firebox2(), BLOCK / "firebox_front.png")
    for s in (1, 2, 3):
        save(firebox2_glow(s), BLOCK / f"firebox_front_glow_{s}.png", 3)
    save(high_front2(), BLOCK / "high_furnace_front.png")
    save(high_front_glow(), BLOCK / "high_furnace_front_glow.png", {"frametime": 4, "interpolate": True})
    save(hatch_cover(), BLOCK / "high_furnace_hatch.png")
    save(taphole2(False), BLOCK / "taphole_front.png")
    save(taphole2(True), BLOCK / "taphole_front_open.png")
    save(taphole2_glow(), BLOCK / "taphole_glow.png", {"frametime": 4, "interpolate": True})
    save(bellows_leather(), BLOCK / "great_bellows_leather.png")
    save(bellows_board(), BLOCK / "great_bellows_board.png")
    save(bellows_frame(), BLOCK / "great_bellows_frame.png")
    save(bellows_frame_end(), BLOCK / "great_bellows_frame_end.png")
    save(bellows_nozzle(), BLOCK / "great_bellows_nozzle.png")
    save(bellows_cam(), BLOCK / "great_bellows_cam.png")
    save(crushed_quartzite(), ITEM / "crushed_quartzite.png")
    fired, raw = brick_items()
    save(raw, ITEM / "unfired_silica_brick.png")
    save(fired, ITEM / "silica_brick.png")
    save(steel_hoop(), ITEM / "steel_hoop.png")
    save(tuyere_item(), ITEM / "bronze_tuyere_nozzle.png")
    tuyere_molds()


# ================================================================ phase 3-7 helpers
def tnoise(seed, cell=4, cell_y=None):
    """Smooth value noise, 16x16, tiling (period 16), 0..1; cell_y stretches it along y."""
    rng = random.Random(seed)
    cy = cell_y or cell
    n, m = 16 // cell, 16 // cy
    g = [[rng.random() for _ in range(n)] for _ in range(m)]
    out = [[0.0] * 16 for _ in range(16)]
    for y in range(16):
        for x in range(16):
            gx, gy = x / cell, y / cy
            i0, j0 = int(gx) % n, int(gy) % m
            i1, j1 = (i0 + 1) % n, (j0 + 1) % m
            fx, fy = gx - int(gx), gy - int(gy)
            fx, fy = fx * fx * (3 - 2 * fx), fy * fy * (3 - 2 * fy)
            a = g[j0][i0] * (1 - fx) + g[j0][i1] * fx
            b = g[j1][i0] * (1 - fx) + g[j1][i1] * fx
            out[y][x] = a * (1 - fy) + b * fy
    return out


def grid_mask(rows):
    return {(x, y) for y, row in enumerate(rows) for x, ch in enumerate(row) if ch not in ". "}


def shade_mask(mask, pal, seed, base=0.55, noise=0.04, hi=0.22, lo=0.24, img=None):
    """Pillow-shade a set of pixels: lit where the pixel above / left is outside the shape, dark below / right."""
    rng = random.Random(seed)
    img = img or Image.new("RGBA", (16, 16))
    for (x, y) in sorted(mask):
        t = base + rng.uniform(-noise, noise)
        if (x, y - 1) not in mask:
            t += hi
        elif (x - 1, y) not in mask:
            t += hi * 0.5
        if (x, y + 1) not in mask:
            t -= lo
        elif (x + 1, y) not in mask:
            t -= lo * 0.55
        put(img, x, y, ramp(pal, t))
    return img


def plate(seed, pal, base=0.45, rivets=(), edge=True, noise=0.03, mottle=0.0):
    rng = random.Random(seed)
    nz = tnoise(seed + 1)
    img = Image.new("RGBA", (16, 16))
    for x in range(16):
        for y in range(16):
            t = base + rng.uniform(-noise, noise) + (nz[y][x] - 0.5) * mottle
            if edge:
                t += 0.12 if (y == 0 or x == 0) else -0.16 if (y == 15 or x == 15) else 0
            put(img, x, y, ramp(pal, t))
    for x, y in rivets:
        plate_rivet(img, x, y, pal)
    return img


def plate_rivet(img, x, y, pal):
    put(img, x, y, ramp(pal, 0.95))
    put(img, x + 1, y, ramp(pal, 0.6))
    put(img, x, y + 1, ramp(pal, 0.5))
    put(img, x + 1, y + 1, ramp(pal, 0.12))


def pocket(img, mask, pal, floor=0.3, wall_dark=0.08, wall_lit=0.7):
    """A pocket sunk into a plate: the top / left inner walls in shadow, the bottom / right walls catch the light."""
    for (x, y) in mask:
        t = floor
        if (x, y - 1) not in mask or (x - 1, y) not in mask:
            t = wall_dark
        elif (x, y + 1) not in mask or (x + 1, y) not in mask:
            t = wall_lit
        put(img, x, y, ramp(pal, t))
    return img


CSAND = [(30, 27, 24), (46, 41, 36), (62, 55, 48), (78, 70, 61), (96, 87, 76), (118, 108, 95)]
COALFLECK = (16, 15, 15)
LIME = [(118, 94, 60), (166, 138, 96), (200, 174, 128), (224, 202, 156), (240, 224, 184)]
PENCIL = (86, 80, 78)
WOOD_EDGE = (82, 62, 36)
LOAM = [(64, 42, 26), (96, 66, 42), (126, 90, 58), (150, 112, 74), (172, 134, 92)]
FIRED_LOAM = [(92, 42, 22), (136, 66, 36), (170, 92, 54), (196, 120, 76), (214, 146, 100)]
STRAW = (196, 170, 96)
SLAGPAL = [(10, 14, 12), (22, 30, 26), (36, 50, 42), (56, 74, 62), (84, 104, 90), (124, 146, 130)]
BASALT = [(14, 10, 10), (26, 18, 17), (40, 28, 26), (56, 40, 36), (78, 58, 52), (120, 96, 88)]
LEAD = [(38, 40, 50), (56, 60, 72), (76, 80, 94), (96, 100, 114), (116, 120, 134), (140, 144, 156)]
WATER = (58, 92, 126)


def sand(seed, pal=CSAND, base=0.5):
    rng = random.Random(seed)
    nz = tnoise(seed, 4)
    img = Image.new("RGBA", (16, 16))
    for x in range(16):
        for y in range(16):
            t = base + (nz[y][x] - 0.5) * 0.3 + rng.uniform(-0.07, 0.07)
            c = ramp(pal, t)
            r = rng.random()
            if r < 0.06:
                c = COALFLECK
            elif r < 0.1:
                c = ramp(pal, t + 0.25)  # a pale grain
            put(img, x, y, c)
    return img


def burnt_sand(seed):
    rng = random.Random(seed)
    nz, nz2 = tnoise(seed, 8), tnoise(seed + 5, 4)
    img = sand(seed + 1, base=0.32)
    for x in range(16):
        for y in range(16):
            c = get(img, x, y)
            if nz2[y][x] > 0.66:
                c = mix(c, (118, 64, 30), (nz2[y][x] - 0.66) * 2.2 * rng.uniform(0.6, 1.0))  # burnt orange-brown
            c = mix(c, (12, 10, 10), max(0.0, 0.5 - nz[y][x]) * 1.1)  # blackened
            if rng.random() < 0.05:
                c = (108, 100, 90)  # a grey crust flake
            put(img, x, y, c)
    return img


# ---------------------------------------------------------------- molten (greyscale, tinted per metal in game)
def crust_field(blobs):
    """Soft dark rafts (cx, cy, rx, ry, depth), wrapping at the tile edges; returns 16x16 of 0..1."""
    out = [[0.0] * 16 for _ in range(16)]
    for cx, cy, rx, ry, depth in blobs:
        for y in range(16):
            for x in range(16):
                dx = min(abs(x - cx), 16 - abs(x - cx)) if abs(x - cx) > 8 else abs(x - cx)
                dy = min(abs(y - cy), 16 - abs(y - cy)) if abs(y - cy) > 8 else abs(y - cy)
                d = math.hypot(dx / rx, dy / ry)
                out[y][x] = max(out[y][x], depth * max(0.0, min(1.0, 1.25 - d)))
    return out


def grey(v):
    g = max(0, min(255, int(v * 255)))
    return (g, g, g)


def molten_surface(crust, bg, x, y):
    c = crust[y][x]
    if c > 0.3:
        return grey(bg - 0.34 - (c - 0.3) * 0.2)  # the crust raft, darker at its heart
    if c > 0.12:
        return grey(bg - 0.16)  # its cooling rim
    return grey(bg)


def molten_flowing():
    """Rafts stretched along the flow, drifting down 1 px a frame (16 frames loop); the glow under them shimmers."""
    rng = random.Random(2001)
    rafts = [(i * 16 / 5 + rng.uniform(-0.6, 0.6), (i * 7 + rng.uniform(-1, 1)) % 16, rng.uniform(0.9, 1.7), rng.uniform(2.2, 3.8),
              rng.uniform(0.6, 1.0)) for i in range(5)]  # spread across the strip
    nz = tnoise(2002, 4)

    def draw(f):
        crust = crust_field([(cx + 0.6 * math.sin(f / 16 * 6.283 + cy), (cy + f) % 16, rx, ry, d) for cx, cy, rx, ry, d in rafts])
        img = Image.new("RGBA", (16, 16))
        for x in range(16):
            for y in range(16):
                put(img, x, y, molten_surface(crust, 0.9 + (nz[(y - 2 * f) % 16][x] - 0.5) * 0.12, x, y))
        return img
    return frames(16, draw)


def molten_still():
    """Rafts of crust circling slowly in the melt's eddies (16 frames loop)."""
    rng = random.Random(2101)
    rafts = [(px_ + rng.uniform(-0.5, 0.5), py_ + rng.uniform(-0.5, 0.5), rng.uniform(1.5, 2.6), rng.uniform(1.2, 2.2), rng.uniform(0.6, 1.0),
              rng.uniform(0, 6.283), rng.choice((-1, 1))) for px_, py_ in ((2, 3), (9.5, 1.5), (13.5, 8), (5, 10), (10, 13))]
    nz = tnoise(2102, 8)

    def draw(f):
        a = f / 16 * 6.283
        crust = crust_field([(cx + 1.4 * math.cos(a * k + ph), cy + 1.4 * math.sin(a * k + ph), rx, ry, d)
                             for cx, cy, rx, ry, d, ph, k in rafts])
        img = Image.new("RGBA", (16, 16))
        for x in range(16):
            for y in range(16):
                put(img, x, y, molten_surface(crust, 0.9 + (nz[y][(x + f) % 16] - 0.5) * 0.1, x, y))
        return img
    return frames(16, draw)


# ---------------------------------------------------------------- silhouettes
PIT_INGOT = grid_mask([
    "................", "................", "................", "................", "..############..",
    "..############..", "..############..", "...##########...", "...##########...", "....########....",
    "................", "................", "................", "................", "................", "................"])
GEAR_MASK = {(x, y) for x in range(16) for y in range(16)
             if 1.6 < math.hypot(x - 7.5, y - 7.5) and (math.hypot(x - 7.5, y - 7.5) <= 5.2
                                                       or (math.hypot(x - 7.5, y - 7.5) <= 7.2 and math.cos(math.atan2(y - 7.5, x - 7.5) * 8) > 0.2))}
POT_ROWS = [
    "................",
    "................",
    "...##########...",
    "..############..",
    "...##########...",
    "..############..",
    ".##############.",
    ".##############.",
    ".##############.",
    "..############..",
    "...##########...",
    "....########....",
    "....#..##..#....",
    "...##..##..##...",
    "................",
    "................",
]
ANVIL_ROWS = [
    "................", "................", "................",
    "..##########....", "..#############.", "..#############.", "...########.....",
    ".....####.......", ".....####.......", ".....####.......", "....######......",
    "...########.....", "..##########....", "..##########....", "................", "................",
]
BELL_ROWS = [
    "................", "................", "................",
    "......####......", ".....######.....", ".....######.....", "....########....", "....########....",
    "....########....", "...##########...", "..############..", "..############..",
    "................", "................", "................", "................",
]
CAULDRON_ROWS = [
    "................", "................", "................", "................",
    "..############..", "..############..", "..############..", "...##########...", "...##########...",
    "....########....", ".....######.....", "................",
    "................", "................", "................", "................",
]
PATTERN_SEEDS = {k: 2200 + i * 10 for i, k in enumerate(
    ("blank", "ingot_mold", "plate_mold", "nugget_mold", "pig", "gear", "pot", "anvil", "pane"))}


def wood_pattern(kind):
    """Pale lime-wood foundry patterns: the shape they press, with dowels and a pencil line."""
    seed = PATTERN_SEEDS[kind]
    rng = random.Random(seed)
    if kind == "pig":
        img = recolor_shape(vanilla("item/iron_ingot"), LIME, seed)
        pts = px(img)
        for p in pts:
            if (p[0] + p[1] * 3) % 9 == 0 and lum(get(img, *p)) > 150:
                img.putpixel(p, mix(get(img, *p), LIME[1], 0.5) + (255,))  # grain
        ys = sorted({y for _, y in pts})
        y = ys[len(ys) // 2]
        for x in range(4, 12, 2):  # the pencil centre line
            if img.getpixel((x, y))[3]:
                put(img, x, y, PENCIL)
        return img
    if kind in ("gear", "pot", "anvil"):
        mask = GEAR_MASK if kind == "gear" else grid_mask(POT_ROWS if kind == "pot" else ANVIL_ROWS)
        img = shade_mask(mask, LIME, seed, base=0.6)
        for (x, y) in sorted(mask):
            if (x * 2 + y) % 7 == 0 and rng.random() < 0.6:
                put(img, x, y, mix(get(img, x, y), LIME[1], 0.4))
        dow = {"gear": [(7, 4), (10, 9)], "pot": [(5, 7), (10, 7)], "anvil": [(4, 4), (10, 4)]}[kind]
        for x, y in dow:  # dowels where the pattern halves meet
            put(img, x, y, LIME[0])
            put(img, x + 1, y, LIME[2])
        return outline(img, WOOD_EDGE)
    board = {(x, y) for x in range(1, 15) for y in range(3, 13)}
    img = shade_mask(board, LIME, seed, base=0.56)
    for (x, y) in sorted(board):
        if (x * 3 + y * 7) % 11 == 0:
            put(img, x, y, mix(get(img, x, y), LIME[1], 0.45))  # grain
    if kind == "blank":
        for x in range(3, 13, 2):
            put(img, x, 7, PENCIL)
        for y in range(5, 11, 2):
            put(img, 8, y, PENCIL)
        for x, y in ((3, 5), (11, 10)):
            put(img, x, y, LIME[0])
            put(img, x + 1, y, LIME[3])
    elif kind == "ingot_mold":
        for ox, oy in ((2, 4), (8, 4), (2, 8), (8, 8)):
            shade_mask({(ox + i, oy + j) for i in range(5) for j in range(3)}, LIME, seed + ox + oy, base=0.78, hi=0.2, lo=0.36, img=img)
            for i in range(5):
                put(img, ox + i, oy + 3, mix(get(img, ox + i, oy + 3), (60, 40, 20), 0.5))
            for j in range(3):
                put(img, ox + 5, oy + j + 1, mix(get(img, ox + 5, oy + j + 1), (60, 40, 20), 0.35))
    elif kind == "plate_mold":
        shade_mask({(x, y) for x in range(3, 13) for y in range(5, 11)}, LIME, seed + 1, base=0.72, img=img)
        for x in range(3, 13):
            put(img, x, 11, mix(get(img, x, 11), (60, 40, 20), 0.35))
        for x in range(4, 12, 2):
            put(img, x, 8, PENCIL)
    elif kind == "nugget_mold":
        for cx in (3, 7, 11):
            for cy in (5, 8, 11):
                put(img, cx, cy - 1, LIME[4])
                put(img, cx + 1, cy - 1, LIME[3])
                put(img, cx, cy, LIME[3])
                put(img, cx + 1, cy, LIME[1])
    elif kind == "pane":
        for x in range(3, 13):
            for y in (5, 10):
                put(img, x, y, mix(get(img, x, y), PENCIL, 0.6))
        for y in range(5, 11):
            for x in (3, 12):
                put(img, x, y, mix(get(img, x, y), PENCIL, 0.6))
    return outline(img, WOOD_EDGE)


def strickle(kind):
    img = Image.new("RGBA", (16, 16))
    for x in range(1, 15):
        f = (x - 1) / 13
        if kind == "bell":
            yb = 6 + round(5.5 * f ** 2.2) + (1 if x >= 13 else 0)  # the bell's waist flaring to the lip
        else:
            yb = 6 + round(4.5 * math.sin(math.pi * f))  # the cauldron's round belly
        for y in range(2, yb + 1):
            t = 0.58 + (0.2 if y == 2 else 0) + (0.06 if (x * 3 + y) % 7 == 0 else 0)
            put(img, x, y, ramp(LIME, t))
        put(img, x, yb, ramp(IRON, 0.55))  # the iron-shod profile edge
        put(img, x, yb - 1, ramp(LIME, 0.35))
    for (x, y) in disc(3.5, 4.5, 1.0):  # the hole it pivots on
        put(img, x, y, (52, 38, 22))
    return outline(img, WOOD_EDGE)


def loam_mold_item(rows, pal, seed):
    m = Image.new("RGBA", (16, 16))
    for (x, y) in grid_mask(rows):
        m.putpixel((x, y), (255, 255, 255, 255))
    top = mold_top(m, None, pal, seed)
    rng = random.Random(seed + 1)
    for x in range(16):
        for y in range(16):
            if not cavity(m, x, y) and rng.random() < 0.06:
                put(top, x, y, mix(get(top, x, y), STRAW if pal is LOAM else (60, 30, 16), 0.6))  # straw (burnt out when fired)
    return item_icon(top)


def blob(lumps, pal, seed, bubble=0.07):
    rng = random.Random(seed)
    img = Image.new("RGBA", (16, 16))
    for cx, cy, r in lumps:
        for x in range(16):
            for y in range(16):
                if math.hypot(x - cx, y - cy) <= r:
                    put(img, x, y, ramp(pal, 0.55 - (x - cx + y - cy) / (r * 3) + rng.uniform(-0.08, 0.08)))
    for p in px(img):
        if rng.random() < bubble:
            put(img, *p, pal[0])
    return img


def ladle():
    img = Image.new("RGBA", (16, 16))
    cx, cy, rx, ry = 5.2, 9.0, 4.4, 1.8
    for x in range(16):
        for y in range(16):
            X, Y = x + 0.5, y + 0.5
            top = ((X - cx) / rx) ** 2 + ((Y - cy) / ry) ** 2
            body = ((X - cx) / rx) ** 2 + ((Y - cy) / 4.6) ** 2 <= 1 and Y >= cy
            if top <= 1:
                inner = ((X - cx) / (rx - 1.1)) ** 2 + ((Y - cy) / (ry - 0.7)) ** 2 <= 1
                put(img, x, y, ramp(SILICA, 0.55 - (Y - cy) * 0.25) if inner else ramp(STEEL, 0.78 - (X - cx) * 0.06))
            elif body:
                put(img, x, y, ramp(STEEL, 0.55 - (X - cx) / rx * 0.35 + (0.18 if -0.7 < (X - cx) / rx < -0.35 else 0)))
    for i in range(12):  # the long handle, rising to the top right
        x, y = 9 + i * 0.5, 8.4 - i * 0.62
        put(img, int(x), int(y), ramp(STEEL, 0.82))
        put(img, int(x) + 1, int(y), ramp(STEEL, 0.4))
    put(img, 14, 1, ramp(STEEL, 0.95))
    return outline(img, (14, 16, 20))


# ---------------------------------------------------------------- phase 3-7 blocks
def slag_stone(seed):
    rng = random.Random(seed)
    a, b = tnoise(seed, 8), tnoise(seed + 1, 4)
    img = Image.new("RGBA", (16, 16))
    for x in range(16):
        for y in range(16):
            sw = math.sin((a[y][x] * 2 + b[y][x]) * 6.283)
            t = 0.36 + sw * 0.07 + rng.uniform(-0.025, 0.025)
            if sw > 0.9:
                t += 0.16  # a lighter glassy swirl
            put(img, x, y, ramp(SLAGPAL, t))
    for _ in range(5):  # gas bubbles: a dark pit with a lit lower lip
        x, y = rng.randrange(16), rng.randrange(16)
        put(img, x, y, SLAGPAL[0])
        put(img, x, (y + 1) % 16, ramp(SLAGPAL, 0.7))
    return img


def bricks8(src, mortar, seed, mortar_t=0.5):
    """Vanilla stone-brick layout (8x4, offset) cut from a stone texture, each brick bevelled."""
    rng = random.Random(seed)
    img = src.copy()
    for y in range(16):
        course, row = y // 4, y % 4
        off = 4 if course % 2 else 0
        for x in range(16):
            col = (x + off) % 8
            if row == 3 or col == 7:
                put(img, x, y, ramp(mortar, mortar_t + rng.uniform(-0.05, 0.05)))
            elif row == 0:
                put(img, x, y, mix(get(img, x, y), (255, 255, 255), 0.12))
            elif row == 2 or col == 6:
                put(img, x, y, mix(get(img, x, y), (0, 0, 0), 0.18))
    return img


def cast_basalt(seed):
    rng = random.Random(seed)
    a = tnoise(seed, 8)
    img = Image.new("RGBA", (16, 16))
    for x in range(16):
        for y in range(16):
            flow = math.sin((x + y * 0.5) / 16 * 6.283 + a[y][x] * 7)
            t = 0.34 + (a[y][x] - 0.5) * 0.16 + rng.uniform(-0.02, 0.02) + (0.1 if flow > 0.93 else 0.035 if flow > 0.7 else 0)
            put(img, x, y, ramp(BASALT, t))  # flow lines frozen in the cast, catching the gloss
    return img


def basalt_tiles():
    rng = random.Random(2501)
    img = Image.new("RGBA", (16, 16))
    for x in range(16):
        for y in range(16):
            tx, ty = x % 8, y % 8
            if tx == 7 or ty == 7:
                put(img, x, y, ramp(BASALT, 0.08))
                continue
            t = 0.38 + random.Random(x // 8 * 3 + y // 8 * 7).uniform(-0.05, 0.05) + rng.uniform(-0.02, 0.02)
            t += 0.1 if (tx == 0 or ty == 0) else -0.08 if (tx == 6 or ty == 6) else 0
            if tx + ty in (3, 4) and 0 < tx < 5:
                t += 0.22  # the polish's diagonal sheen
            put(img, x, y, ramp(BASALT, t))
    return img


def metal_block(pal, seed, dents=0):
    img = recolor_shape(vanilla("block/iron_block"), pal, seed)
    rng = random.Random(seed)
    for _ in range(dents):  # soft lead takes knocks: a dark pit with a lit lower-right rim
        x, y = rng.randrange(2, 13), rng.randrange(2, 13)
        put(img, x, y, ramp(pal, 0.25))
        put(img, x + 1, y, ramp(pal, 0.35))
        put(img, x, y + 1, ramp(pal, 0.45))
        put(img, x + 1, y + 1, ramp(pal, 0.8))
    return img


# ---------------------------------------------------------------- phase 6-7, round 2: vanilla-style, matching the furnace
# Flat 16x16 pixel art in a few tones: the same sooty buff brick as the High Furnace (casing2), dark riveted iron
# (IRONR) for the metal work, chunky glowing pixels for fire and melt. No noise gradients, no baked lighting beyond
# a lit top-left edge.
GLASS = {0: ((34, 32, 34), (70, 70, 78)), 1: ((96, 26, 14), (150, 52, 18)), 2: ((206, 96, 22), (246, 164, 52)),
         3: ((252, 206, 104), (255, 246, 204))}
EMBER = [(70, 22, 10), (168, 58, 14), (236, 130, 32), (255, 208, 96)]
CHAR = [(22, 20, 19), (38, 34, 32), (58, 52, 48)]


def iron_frame(img, x0, y0, x1, y1, bolts=True):
    """A flat iron frame: dark outline, a lit top-left and a shaded bottom-right bar, a bolt in each corner."""
    for x in range(x0, x1 + 1):
        for y in range(y0, y1 + 1):
            edge = min(x - x0, y - y0, x1 - x, y1 - y)
            if edge == 0:
                put(img, x, y, IRONR[0])
            elif edge == 1:
                lit = (x == x0 + 1 or y == y0 + 1) and not (x == x1 - 1 or y == y1 - 1)
                put(img, x, y, IRONR[5] if lit else IRONR[3])
    if bolts:
        for x, y in ((x0 + 1, y0 + 1), (x1 - 1, y0 + 1), (x0 + 1, y1 - 1), (x1 - 1, y1 - 1)):
            put(img, x, y, IRONR[6])


def sight_glass(heat):
    """The furnace brick with an iron-framed window; through the glass the inside's glow, dark when cold."""
    img = casing2("base")
    iron_frame(img, 2, 2, 13, 13)
    dark, light = GLASS[heat]
    for x in range(4, 12):
        for y in range(4, 12):
            put(img, x, y, dark)
    for x, y in ((5, 6), (6, 5), (5, 7), (7, 5), (9, 10), (10, 9)):  # the glare across the thick glass
        put(img, x, y, light)
    if heat:
        for x, y in ((8, 8), (7, 9), (9, 7), (6, 10)):  # flames moving inside
            put(img, x, y, light)
    return img


def riveted_iron(seed, seams=(7, 15), vseam=((8, 0, 7), (3, 8, 15))):
    """Dark iron plating: plates of two tones, a dark lapped seam row with rivets above it, staggered butt seams,
    a few rust pixels."""
    rng = random.Random(seed)
    img = Image.new("RGBA", (16, 16))
    for x in range(16):
        for y in range(16):
            put(img, x, y, IRONR[2] if (y // 8 + x // 8) % 2 else IRONR[3])
    for y in seams:
        for x in range(16):
            put(img, x, y, IRONR[1])
            if (x + y) % 3 == 0:  # a rivet: a bright head over a dark shadow pixel
                put(img, x, y - 2, IRONR[5])
                put(img, x, y - 1, IRONR[1])
    for x, y0, y1 in vseam:
        for y in range(y0, y1):
            put(img, x, y, IRONR[1])
    for _ in range(6):
        x, y = rng.randrange(16), rng.randrange(16)
        if get(img, x, y) in (IRONR[2], IRONR[3]):
            put(img, x, y, RUST[1] if rng.random() < 0.6 else RUST[0])
    return img


def stove_side(hatch=False):
    """The Hot Blast Stove's riveted iron shell; the hatch face carries a small bolted door."""
    img = riveted_iron(2801)
    if hatch:
        iron_frame(img, 4, 3, 11, 12)
        for x in range(6, 10):
            for y in range(5, 11):
                put(img, x, y, IRONR[2])
        for y in (6, 9):  # hinges and the latch
            put(img, 5, y, IRONR[6])
        put(img, 10, 8, IRONR[6])
    return img


def stove_top():
    """An iron rim round a fire-brick checker (the stove's heat store) and the hot-air flue, glowing dull red."""
    img = riveted_iron(2810, seams=(), vseam=())
    iron_frame(img, 0, 0, 15, 15)
    for x in range(3, 13):
        for y in range(3, 13):
            put(img, x, y, ARCH[3] if (x + y) % 2 else ARCH[1])  # the checker bricks' openings
    for x in range(6, 10):
        for y in range(6, 10):
            put(img, x, y, (120, 32, 14) if 7 <= x <= 8 and 7 <= y <= 8 else (60, 20, 12))
    return img


def coke_front(lit):
    """The beehive's front: furnace brick round an iron door with two slits, glowing when lit; only the lower half
    (rows 8-15) is on the model's front."""
    brick = casing2("base")
    img = brick.copy()
    iron_frame(img, 4, 8, 11, 15, bolts=False)
    for x in range(5, 11):
        for y in range(9, 15):
            put(img, x, y, IRONR[3])
    for x in (4, 11):  # the arch: the door's top corners stay brick
        put(img, x, 8, get(brick, x, 8))
    for x in range(6, 10):
        for y in (10, 12):
            put(img, x, y, (EMBER[2] if x in (7, 8) else EMBER[1]) if lit else VOID)
    put(img, 9, 14, IRONR[6])
    return img


def coke_glow():
    """Only the door's glowing slits (an emissive plane over the lit front)."""
    img = Image.new("RGBA", (16, 16))
    for x in range(6, 10):
        for y in (10, 12):
            put(img, x, y, EMBER[2] if x in (7, 8) else EMBER[1])
    return img


def coke_top(lit):
    """The crown of the beehive: sooty top brick round the charging hole (dark, or glowing while it cokes)."""
    img = casing2_top()
    for x in range(5, 11):
        for y in range(5, 11):
            put(img, x, y, mix(get(img, x, y), SOOT, 0.45))
    for x in range(6, 10):
        for y in range(6, 10):
            inner = 7 <= x <= 8 and 7 <= y <= 8
            put(img, x, y, (EMBER[3] if inner else EMBER[2]) if lit else (VOID if inner else CHAR[0]))
    return img


def finery_side():
    """Furnace brick with an iron band round the hearth's rim (rows 6-7) and the tuyere's port low down."""
    img = casing2("base")
    for x in range(16):
        put(img, x, 6, IRONR[5] if x % 5 else IRONR[6])
        put(img, x, 7, IRONR[2])
    for x in range(6, 10):
        for y in range(11, 15):
            ring = x in (6, 9) or y in (11, 14)
            put(img, x, y, IRONR[4] if ring else VOID)
    return img


def finery_rim():
    """The top of the hearth's walls: an iron rim plate, bolted."""
    img = Image.new("RGBA", (16, 16))
    for x in range(16):
        for y in range(16):
            put(img, x, y, IRONR[3] if (x + y) % 7 else IRONR[2])
    for x in range(1, 16, 5):
        for y in range(1, 16, 5):
            put(img, x, y, IRONR[5])
            put(img, x, y + 1, IRONR[1])
    return img


def finery_top(lit_frame=None):
    """The charcoal bed: chunky lumps of charcoal; lit, the lumps glow and flicker (4 frames)."""
    rng = random.Random(2410)
    img = Image.new("RGBA", (16, 16))
    for x in range(16):
        for y in range(16):
            put(img, x, y, CHAR[0])
    lumps = [(rng.randrange(16), rng.randrange(16), rng.randrange(3)) for _ in range(22)]
    for i, (x, y, k) in enumerate(lumps):
        for dx, dy in ((0, 0), (1, 0), (0, 1), (1, 1)):
            if lit_frame is None:
                c = CHAR[1 + (dx + dy + k) % 2]
            else:
                c = EMBER[min(3, max(0, k + (i + lit_frame) % 3 - (dx + dy) // 2))]
            put(img, (x + dx) % 16, (y + dy) % 16, c)
    return img


def basin_side():
    """The Foundry Basin's riveted iron: two bands with rivets, dark staves between."""
    img = riveted_iron(3230, seams=(), vseam=((0, 0, 16), (15, 0, 16), (8, 4, 12)))
    for x in range(16):
        for y, c in ((1, IRONR[4]), (2, IRONR[1]), (12, IRONR[4]), (13, IRONR[1])):
            put(img, x, y, c)
    for x in (2, 6, 10, 14):
        put(img, x, 1, IRONR[6])
        put(img, x, 12, IRONR[6])
    return img


def loam_cope():
    """The top of a Loam Mold sitting in the basin: fired loam round the pouring hole."""
    img = loam_block(3602)
    for x in range(6, 10):
        for y in range(6, 10):
            put(img, x, y, ramp(FIRED_LOAM, 0.15) if x in (6, 9) or y in (6, 9) else VOID)
    return img


def ladle_full():
    """The Steel Ladle holding melt: its bowl's lining (as ladle() draws it) glowing white-hot orange, whatever the melt."""
    img = ladle()
    cx, cy, rx, ry = 5.2, 9.0, 4.4 - 1.1, 1.8 - 0.7
    for x in range(16):
        for y in range(16):
            if ((x + 0.5 - cx) / rx) ** 2 + ((y + 0.5 - cy) / ry) ** 2 <= 1:
                put(img, x, y, MOLTEN[3] if (x + y) % 3 else MOLTEN[2])
    return img


def loam_block(seed, top_rows=None):
    rng = random.Random(seed)
    nz = tnoise(seed, 4)
    img = Image.new("RGBA", (16, 16))
    for x in range(16):
        for y in range(16):
            put(img, x, y, ramp(FIRED_LOAM, 0.55 + (nz[y][x] - 0.5) * 0.22 + rng.uniform(-0.05, 0.05)))
    for _ in range(14):  # burnt-out straw: short dark streaks
        x, y = rng.randrange(16), rng.randrange(16)
        put(img, x, y, ramp(FIRED_LOAM, 0.1))
        put(img, (x + 1) % 16, y, ramp(FIRED_LOAM, 0.2))
    if top_rows:
        m = Image.new("RGBA", (16, 16))
        for (x, y) in grid_mask(top_rows):
            m.putpixel((x, y), (255, 255, 255, 255))
        img = mold_top(m, img, FIRED_LOAM, seed + 3)
    else:
        for x in range(16):
            for y in (3, 12):
                put(img, x, y, ramp(IRON, 0.5 if x % 5 else 0.8))  # the binding wire
    return img


def batch2():
    # ---- phase 3: pouring and troughs
    st = plate(3000, STEEL, base=0.48, rivets=((1, 1), (13, 1), (1, 13), (13, 13), (7, 1), (7, 13)), mottle=0.15)
    for x in range(3, 13):  # heat tint where the metal runs over the lip
        put(st, x, 6, mix(get(st, x, 6), (160, 126, 70), 0.25))
        put(st, x, 8, mix(get(st, x, 8), (78, 96, 146), 0.25))
    save(st, BLOCK / "pouring_spout_steel.png")
    lever = Image.new("RGBA", (16, 16))
    for x in range(16):
        for y in range(16):
            if y < 6:
                t = 0.55 + (0.2 if x == 0 else -0.2 if x == 15 else 0) + (0.08 if (x * 3 + y) % 7 == 0 else 0)
                put(lever, x, y, ramp(OAK, t - (0.15 if y == 5 else 0)))
            elif y < 8:
                put(lever, x, y, ramp(BRONZE, 0.7 if y == 6 else 0.35))  # the ferrule
            else:
                put(lever, x, y, ramp(IRON, 0.55 + (0.25 if x < 2 else -0.25 if x > 13 else 0) - (y - 8) * 0.02))
    save(lever, BLOCK / "pouring_spout_lever.png")
    save(molten_flowing(), BLOCK / "molten_flowing.png", 2)
    save(molten_still(), BLOCK / "molten_still.png", {"frametime": 3, "interpolate": True})
    save(plate(3010, CAST, base=0.42, rivets=((1, 1), (7, 1), (13, 1), (1, 13), (7, 13), (13, 13)), mottle=0.18),
         BLOCK / "refractory_trough_outside.png")
    lining = silica_bricks(3020, speck=0.04)
    rng = random.Random(3021)
    nz = tnoise(3022, 4)
    for x in range(16):
        for y in range(16):
            c = mix(get(lining, x, y), (96, 70, 50), nz[y][x] * 0.3)  # heat-browned by the metal
            put(lining, x, y, c)
    save(lining, BLOCK / "refractory_trough_lining.png")
    gate = plate(3030, CAST, base=0.46, mottle=0.12)
    for y in range(16):
        for x in (0, 1, 14, 15):
            put(gate, x, y, ramp(CAST, 0.22 + (0.15 if x in (0, 14) else 0)))
    for y in (6, 11):
        for x in range(2, 14):
            put(gate, x, y, ramp(CAST, 0.7))
            put(gate, x, y + 1, ramp(CAST, 0.2))
    for x in range(5, 11):  # the lifting handle
        put(gate, x, 1, ramp(IRON, 0.85))
        put(gate, x, 2, ramp(IRON, 0.2) if 5 < x < 10 else ramp(IRON, 0.6))
    for y in (2, 3):
        put(gate, 5, y, ramp(IRON, 0.7))
        put(gate, 10, y, ramp(IRON, 0.4))
    save(gate, BLOCK / "trough_gate.png")
    # ---- phase 4: pig bed
    save(sand(3100), BLOCK / "casting_sand.png")
    save(burnt_sand(3110), BLOCK / "burnt_casting_sand.png")
    runner = sand(3100)
    pocket(runner, {(x, y) for x in range(5, 11) for y in range(16)}, CSAND, floor=0.32, wall_dark=0.1, wall_lit=0.62)
    for y in range(16):
        put(runner, 5, y, ramp(CSAND, 0.08))
        put(runner, 10, y, ramp(CSAND, 0.62))
        put(runner, 4, y, mix(get(runner, 4, y), (255, 255, 255), 0.1))
        put(runner, 11, y, mix(get(runner, 11, y), (255, 255, 255), 0.1))
    save(runner, BLOCK / "sand_runner.png")
    pig = sand(3100)
    pocket(pig, set(PIT_INGOT), CSAND, floor=0.3, wall_dark=0.08, wall_lit=0.66)
    save(pig, BLOCK / "pig_mold.png")
    # ---- phase 5: molding flask, foundry table and basin, iron molds
    fs = plate(3200, CAST, base=0.4, mottle=0.14)
    for x in range(16):
        put(fs, x, 7, ramp(CAST, 0.08))  # where cope meets drag
        put(fs, x, 8, ramp(CAST, 0.62))
        put(fs, x, 0, ramp(CAST, 0.6))
        put(fs, x, 15, ramp(CAST, 0.15))
    for x0 in (1, 12):  # the lugs and their pins
        bevel(fs, x0, 5, x0 + 2, 10, CAST, 0.55, random.Random(x0), hi=0.2, lo=0.3)
        put(fs, x0 + 1, 4, ramp(IRON, 0.9))
        put(fs, x0 + 1, 3, ramp(IRON, 0.6))
        put(fs, x0 + 1, 11, ramp(IRON, 0.35))
    save(fs, BLOCK / "molding_flask_side.png")
    rng = random.Random(3211)
    inner_sand = sand(3210, base=0.56)
    ft = Image.new("RGBA", (16, 16))
    bevel(ft, 0, 0, 15, 15, CAST, 0.5, rng, hi=0.14, lo=0.2)
    bevel(ft, 1, 1, 14, 14, CAST, 0.4, rng, hi=-0.2, lo=0.15)
    for x in range(2, 14):
        for y in range(2, 14):
            put(ft, x, y, get(inner_sand, x, y))
    for (x, y) in disc(7.5, 7.5, 1.7):  # the pouring hole: shadowed on its top-left wall
        put(ft, x, y, ramp(CSAND, 0.02) if (x + y) <= 15 or math.hypot(x - 7.5, y - 7.5) < 1 else ramp(CSAND, 0.6))
    put(ft, 11, 4, ramp(CSAND, 0.05))  # a vent
    save(ft, BLOCK / "molding_flask_top_sand.png")
    fe = Image.new("RGBA", (16, 16))
    bevel(fe, 0, 0, 15, 15, CAST, 0.5, rng, hi=0.14, lo=0.2)
    for x in range(1, 15):
        for y in range(1, 15):
            put(fe, x, y, ramp(CAST, 0.08 + 0.05 * (x + y - 2) / 13 + rng.uniform(-0.02, 0.02)))
    for i in range(1, 15):
        put(fe, i, 14, ramp(CAST, 0.34))  # the far inner walls catch some light
        put(fe, 14, i, ramp(CAST, 0.3))
    save(fe, BLOCK / "molding_flask_top_empty.png")
    tt = plate(3220, STEEL, base=0.5, rivets=((0, 0), (14, 0), (0, 14), (14, 14)), mottle=0.1)
    for i in range(1, 15):  # the water channel round the edge
        for x, y in ((i, 1), (i, 14), (1, i), (14, i)):
            put(tt, x, y, mix(WATER, ramp(STEEL, 0.2), 0.3))
    for i in range(2, 14):
        put(tt, i, 2, ramp(STEEL, 0.72))
        put(tt, 2, i, ramp(STEEL, 0.68))
    sb = silica_bricks(3221)
    for x in range(4, 12):
        for y in range(4, 12):
            put(tt, x, y, mix(get(sb, x, y), SOOT, 0.1))
    for i in range(3, 13):
        put(tt, i, 3, ramp(STEEL, 0.12))
        put(tt, 3, i, ramp(STEEL, 0.12))
        put(tt, i, 12, ramp(STEEL, 0.75))
        put(tt, 12, i, ramp(STEEL, 0.7))
    save(tt, BLOCK / "foundry_table_top.png")
    ts = Image.new("RGBA", (16, 16))
    for x in range(16):
        for y in range(16):
            if y < 5:
                put(ts, x, y, ramp(STEEL, 0.5 + (0.18 if y == 0 else -0.24 if y == 4 else 0)))
            elif x < 3 or x > 12:
                put(ts, x, y, ramp(STEEL, (0.62, 0.48, 0.26)[x if x < 3 else x - 13]))
    for x in (1, 5, 10, 14):
        put(ts, x, 2, STEEL[6])
        put(ts, min(15, x + 1), 3, ramp(STEEL, 0.15))
    save(ts, BLOCK / "foundry_table_side.png")
    bs = plate(3230, STEEL, base=0.46, edge=False, mottle=0.12)
    for y in range(16):
        put(bs, 7, y, ramp(STEEL, 0.15))  # the stave seam with its rivets
        if y % 3 == 1:
            put(bs, 6, y, STEEL[5])
            put(bs, 8, y, STEEL[5])
    for x in range(16):
        put(bs, x, 0, ramp(SILICA, 0.7))  # the lining's lip at the rim
        put(bs, x, 1, ramp(SILICA, 0.35))
        for r, y in enumerate((3, 4, 5)):
            put(bs, x, y, hoop_pixel(random.Random(x), r, x))
        for r, y in enumerate((12, 13, 14)):
            put(bs, x, y, hoop_pixel(random.Random(x + 50), r, x))
    for x in (2, 11):
        rivet(bs, x, 3)
        rivet(bs, x + 2, 12)
    save(basin_side(), BLOCK / "foundry_basin_side.png")
    side = plate(3250, CAST, base=0.55, mottle=0.2)
    for x in range(16):
        put(side, x, 0, ramp(CAST, 0.7))
        put(side, x, 1, ramp(CAST, 0.5))
        put(side, x, 15, ramp(CAST, 0.12))
    save(side, BLOCK / "iron_mold_side.png")
    im = plate(3260, CAST, base=0.6, mottle=0.18)
    for ox, oy in ((2, 2), (9, 2), (2, 9), (9, 9)):
        pocket(im, {(ox + i, oy + j) for i in range(5) for j in range(5) if not (i in (0, 4) and j in (0, 4))}, CAST, floor=0.3)
    save(im, BLOCK / "iron_ingot_mold_top.png")
    pm = plate(3261, CAST, base=0.6, mottle=0.18)
    pocket(pm, {(x, y) for x in range(2, 14) for y in range(2, 14)}, CAST, floor=0.32, wall_lit=0.66)
    save(pm, BLOCK / "iron_plate_mold_top.png")
    nm = plate(3262, CAST, base=0.6, mottle=0.18)
    for ox in (1, 5, 9, 13):
        for oy in (1, 5, 9, 13):
            put(nm, ox, oy, ramp(CAST, 0.08))
            put(nm, ox + 1, oy, ramp(CAST, 0.22))
            put(nm, ox, oy + 1, ramp(CAST, 0.22))
            put(nm, ox + 1, oy + 1, ramp(CAST, 0.68))
    save(nm, BLOCK / "iron_nugget_mold_top.png")
    # ---- pattern bench (vanilla oak planks, like the other benches)
    top = vanilla("block/oak_planks")
    pat = wood_pattern("pig")
    for p in px(pat):
        top.putpixel((p[0], p[1] - 3), pat.getpixel(p))
    for x in range(3, 13):  # a chisel lying across
        put(top, x, 12, ramp(IRON, 0.75) if x < 7 else ramp(OAK, 0.3 if x % 2 else 0.4))
        put(top, x, 13, ramp(IRON, 0.35) if x < 7 else ramp(OAK, 0.12))
    for x, y in ((12, 1), (13, 2), (12, 3), (2, 13), (3, 14), (14, 10), (13, 11)):
        put(top, x, y, LIME[4])  # curled shavings
    save(top, BLOCK / "pattern_bench_top.png")
    bside = vanilla("block/oak_planks")
    for y in range(16):
        for x in (0, 1, 14, 15):
            put(bside, x, y, ramp(OAK, 0.35 if x in (0, 14) else 0.2))
    for x in range(2, 14):
        put(bside, x, 9, ramp(OAK, 0.12))
    for (x, y), c in zip(((4, 11), (5, 11), (6, 11), (9, 12), (10, 12), (11, 11)), (LIME[3], LIME[2], LIME[1], LIME[3], LIME[2], LIME[1])):
        put(bside, x, y, c)  # patterns on the shelf
    save(bside, BLOCK / "pattern_bench_side.png")
    front = vanilla("block/oak_planks")
    for y in range(16):
        for x in (0, 1, 14, 15):
            put(front, x, y, ramp(OAK, 0.35 if x in (0, 14) else 0.2))
    bevel(front, 3, 2, 12, 6, OAK, 0.5, random.Random(3301), hi=0.15, lo=0.3)  # a drawer
    put(front, 7, 4, ramp(IRON, 0.9))
    put(front, 8, 4, ramp(IRON, 0.4))
    for x in range(3, 13):  # the vise's wooden jaw and screw
        put(front, x, 9, ramp(OAK, 0.7))
        put(front, x, 10, ramp(OAK, 0.45))
        put(front, x, 11, ramp(OAK, 0.25))
    for x in range(6, 10):
        put(front, x, 13, ramp(IRON, 0.8 if x < 8 else 0.4))
    put(front, 7, 12, ramp(IRON, 0.6))
    save(front, BLOCK / "pattern_bench_front.png")
    # ---- phase 6: finery, slag and basalt, sight glass, metal blocks
    save(finery_top(), BLOCK / "finery_hearth_top.png")
    save(frames(4, finery_top), BLOCK / "finery_hearth_top_lit.png", {"frametime": 6})
    save(finery_side(), BLOCK / "finery_hearth_side.png")
    save(finery_rim(), BLOCK / "finery_hearth_rim.png")
    save(slag_stone(3400), BLOCK / "slag_stone.png")
    save(bricks8(slag_stone(3401), [(8, 10, 9), (18, 22, 20), (30, 34, 32)], 3402), BLOCK / "slag_stone_bricks.png")
    save(cast_basalt(3410), BLOCK / "cast_basalt.png")
    save(basalt_tiles(), BLOCK / "cast_basalt_tiles.png")
    for h in range(4):
        save(sight_glass(h), BLOCK / f"sight_glass_{h}.png")
    # Steel, Bronze and Lead Blocks: scripts/gen_metal_textures.py (one palette per metal, from the metal table)
    # ---- phase 7: coke oven, hot blast stove, loam molds
    save(coke_front(False), BLOCK / "coke_oven_front.png")
    save(coke_front(True), BLOCK / "coke_oven_front_lit.png")
    save(coke_glow(), BLOCK / "coke_oven_glow.png")
    save(coke_top(False), BLOCK / "coke_oven_top.png")
    save(coke_top(True), BLOCK / "coke_oven_top_lit.png")
    save(stove_side(), BLOCK / "hot_blast_stove_side.png")
    save(stove_side(True), BLOCK / "hot_blast_stove_hatch.png")
    save(stove_top(), BLOCK / "hot_blast_stove_top.png")
    save(loam_block(3600, BELL_ROWS), BLOCK / "loam_mold_top.png")
    save(loam_block(3601), BLOCK / "loam_mold_side.png")
    save(loam_cope(), BLOCK / "loam_mold_cope.png")
    # ---- items (the mod has no coal_dust / coked_coal texture yet: both come from the user's library)
    save(Image.open(LIB / "coal_dust.png").convert("RGBA"), ITEM / "coal_dust.png")
    coke = recolor_shape(Image.open(LIB / "tempered_coke.png").convert("RGBA"),
                         [(30, 30, 32), (64, 64, 68), (104, 104, 108), (146, 146, 150), (190, 190, 194)], 3700)
    rng = random.Random(3701)
    for p in px(coke):
        if rng.random() < 0.1 and lum(get(coke, *p)) > 60:
            coke.putpixel(p, (24, 24, 26, 255))  # pores
    save(coke, ITEM / "coke.png")
    for kind in ("ingot_mold", "plate_mold", "nugget_mold", "pig", "gear", "pot", "anvil", "pane"):
        save(wood_pattern(kind), ITEM / f"wooden_pattern_{kind}.png")
    save(wood_pattern("blank"), ITEM / "wooden_pattern.png")
    save(wood_pattern("pig"), ITEM / "pig_pattern.png")
    save(strickle("bell"), ITEM / "strickle_board_bell.png")
    save(strickle("cauldron"), ITEM / "strickle_board_cauldron.png")
    loam = recolor_shape(vanilla("item/clay_ball"), LOAM, 3800)
    rng = random.Random(3801)
    for p in rng.sample(px(loam), 8):
        if p[0] < 15 and loam.getpixel((p[0] + 1, p[1]))[3]:
            put(loam, *p, STRAW)
            put(loam, p[0] + 1, p[1], mix(STRAW, LOAM[2], 0.4))
    save(loam, ITEM / "loam.png")
    for rows, nm_ in ((BELL_ROWS, "bell"), (CAULDRON_ROWS, "cauldron")):
        save(loam_mold_item(rows, LOAM, 3810 + len(nm_)), ITEM / f"unfired_loam_mold_{nm_}.png")
        save(loam_mold_item(rows, FIRED_LOAM, 3820 + len(nm_)), ITEM / f"loam_mold_{nm_}.png")
    gb = recolor_shape(Image.open(ITEM / "crushed_copper_ore.png").convert("RGBA"),
                       [(110, 100, 78), (168, 156, 124), (206, 196, 162), (228, 220, 190), (242, 236, 212)], 3900)
    rng = random.Random(3901)
    for p in px(gb):
        r = rng.random()
        if r < 0.12:
            gb.putpixel(p, (120, 118, 114, 255))  # ash
        elif r < 0.17:
            gb.putpixel(p, (126, 196, 150, 255))  # green glints of cullet
    save(gb, ITEM / "glass_batch.png")
    # Pig Iron Ingot (and Block): scripts/gen_metal_textures.py, from the metal table
    save(outline(shade_mask(GEAR_MASK, CAST, 3920, base=0.5, hi=0.3, lo=0.3), (6, 6, 8)), ITEM / "cast_iron_gear.png")
    pot = shade_mask(grid_mask(POT_ROWS), CAST, 3930, base=0.45, hi=0.3, lo=0.3)
    for x in range(4, 12):
        put(pot, x, 3, ramp(CAST, 0.1))  # the dark mouth inside the rim
    for x in (5, 6):
        put(pot, x, 7, ramp(CAST, 0.85))
    save(outline(pot, (6, 6, 8)), ITEM / "cast_iron_pot.png")
    sal = blob(((7, 9, 5.2), (10.5, 6.5, 3.6), (4.5, 6, 3.0), (11, 11, 2.6)), CAST, 3940, bubble=0.05)
    rng = random.Random(3941)
    for p in px(sal):
        r = rng.random()
        if r < 0.12:
            put(sal, *p, mix(get(sal, *p), (118, 58, 30), 0.6))  # rust
        elif r < 0.2:
            put(sal, *p, ramp(SLAGPAL, rng.uniform(0.2, 0.6)))  # slag
    put(sal, 6, 6, (200, 204, 210))
    put(sal, 7, 6, (140, 144, 150))
    save(outline(sal, (6, 6, 8)), ITEM / "furnace_salamander.png")
    save(ladle(), ITEM / "steel_ladle.png")
    save(ladle_full(), ITEM / "steel_ladle_full.png")


batch1()
batch2()
print(f"high furnace textures ok ({len(WRITTEN)} files)")


# ---- sparse hearth glow (review: the full joint grid read as magma): a few joints of the hot course, mostly the lower
# ones right over an arch, dull red at heat 1-2 and orange-red at 3; derived pixel-exact from the full glows above
def casing_glow_sparse():
    import random as _r
    from PIL import Image as _I
    src = {1: "banded_furnace_casing_glow_1", 2: "banded_furnace_casing_glow_1", 3: "banded_furnace_casing_glow_2"}
    dim = {1: 0.55, 2: 0.8, 3: 0.9}
    for h, name in src.items():
        im = _I.open(BLOCK / f"{name}.png").convert("RGBA")
        w, hgt = im.size
        rnd = _r.Random(4100 + h)
        keep = {(x, y) for x in range(16) for y in range(16) if (y >= 12 and rnd.random() < 0.7) or (y >= 8 and rnd.random() < 0.18)}
        out = _I.new("RGBA", (w, hgt), (0, 0, 0, 0))
        px, po = im.load(), out.load()
        for y in range(hgt):
            for x in range(w):
                r, g, b, a = px[x, y]
                if a and (x, y % 16) in keep:
                    po[x, y] = (int(r * dim[h]), int(g * dim[h] * 0.8), int(b * dim[h] * 0.6), int(a * (0.75 if h < 3 else 0.9)))
        out.save(BLOCK / f"casing_glow_sparse_{h}.png")
        meta = BLOCK / f"{name}.png.mcmeta"
        if meta.exists():
            (BLOCK / f"casing_glow_sparse_{h}.png.mcmeta").write_text(meta.read_text())


casing_glow_sparse()


# ---- molten_crust.png (not in the atlas: the troughs' and molds' renderers scroll it): one frame of molten_flowing with its
# crust streaks pulled darker, so a tinted melt shows drifting crust over a white-hot ground instead of flat paint
def molten_crust():
    """The steel age's melt (troughs, molds, flask cup), tinted by the melt: a white-hot ground with a few chunky crust
    clusters (2x2 to 3x2, never long, so a run of troughs shows no stripes across it) and single darker pixels."""
    from PIL import Image as _I
    rng = random.Random(2391)
    img = _I.new("RGBA", (16, 16), (255, 255, 255, 255))
    for cx, cy, w, h in ((2, 1, 3, 2), (9, 3, 2, 2), (5, 7, 2, 2), (12, 9, 3, 2), (1, 11, 2, 2), (8, 13, 3, 2)):
        for x in range(cx, cx + w):
            for y in range(cy, cy + h):
                v = 200 if (x + y) % 3 else 182
                img.putpixel((x % 16, y % 16), (v, v, v, 255))
    for _ in range(10):
        v = 222
        img.putpixel((rng.randrange(16), rng.randrange(16)), (v, v, v, 255))
    img.save(BLOCK / "molten_crust.png")


molten_crust()


def molten_stream():
    """The falling stream (client/PouringSpoutRenderer), drawn like vanilla lava: three flat tones in chunky clusters,
    tinted by the melt; bright runs down it (it scrolls), a few darker spots. Tiles both ways."""
    from PIL import Image as _I
    rng = random.Random(2471)
    img = _I.new("RGBA", (16, 16), (214, 214, 214, 255))
    def run(x, y, n, v):
        for k in range(n):
            img.putpixel((x % 16, (y + k) % 16), (v, v, v, 255))
    for _ in range(9):  # bright runs, 1-2 px wide, 3-6 long
        x, y = rng.randrange(16), rng.randrange(16)
        run(x, y, rng.randrange(3, 7), 255)
        if rng.random() < 0.5:
            run(x + 1, y + 1, rng.randrange(2, 4), 255)
    for _ in range(5):  # darker spots, 1x2
        x, y = rng.randrange(16), rng.randrange(16)
        run(x, y, 2, 170)
    img.save(BLOCK / "molten_stream.png")


molten_stream()
