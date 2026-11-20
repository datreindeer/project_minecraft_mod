"""Gemstone textures, drawn per gem (no two gems share a shape). Data: scripts/gem_table.py (palettes dark -> light).

Items: every gem has its own cut or habit (brilliant, oval, cushion, step cut, carved disc, cabochon, nugget, trillion,
drop, quartz point, rough chunk, double point, hexagonal column, octahedron, blades, striated prism, agate slice,
tumbled stones, botryoidal lump), plus its own markings (opal fire, moonstone sheen, turquoise matrix, amber insect ...).
Crystal blocks (cross textures, growing up from the attached face): each gem its own growth habit (points, stubby,
slender, flat prisms, cubes, blades, domes, striated columns), three stages. Ore blocks: each gem its own inclusion
pattern laid on vanilla stone / deepslate / sandstone. Pebble blocks: a procedural tile per gem.
Run: python scripts/gen_gemstone_textures.py"""
import math
import random
import zipfile
from io import BytesIO
from pathlib import Path

from PIL import Image

from gem_table import GEMS, SIZES, ore_block

ROOT = Path(__file__).resolve().parent.parent
TEX = ROOT / "src/main/resources/assets/mechanical/textures"
JAR_NAME = "build/moddev/artifacts/minecraft-patched-26.3.0.95-merged.jar"
JAR = next(p for p in (ROOT / JAR_NAME, ROOT.parent.parent / "project_minecraft_mod" / JAR_NAME) if p.exists())
ZIP = zipfile.ZipFile(JAR)
PAL = {g["name"]: g["pal"] for g in GEMS}
# second colours for two-colour gems
GREEN_TOURMALINE = [(14, 46, 22), (28, 86, 44), (52, 130, 70), (100, 176, 110), (180, 230, 180)]
GREEN_FLUORITE = [(16, 70, 60), (30, 120, 100), (60, 180, 150), (120, 222, 190), (210, 255, 236)]
OPAL_FIRE = [(255, 80, 110), (80, 220, 130), (70, 150, 255), (255, 200, 50), (190, 100, 255)]


def vanilla(path):
    return Image.open(BytesIO(ZIP.read(f"assets/minecraft/textures/{path}.png"))).convert("RGBA")


def clamp(t):
    return max(0.0, min(1.0, t))


def ramp(pal, t):
    t = clamp(t) * (len(pal) - 1)
    i = min(int(t), len(pal) - 2)
    f = t - i
    return tuple(int(pal[i][c] * (1 - f) + pal[i + 1][c] * f) for c in range(3))


def light(dx, dy, r):
    """-1 (bottom right) .. 1 (top left): the light comes from the top left like vanilla items."""
    return (-dx - dy) / (2 * r)


# per-gem surface grain (opaque stones are mottled, cut gems clean) and which gems get cut-gem detailing
GRAIN = {"jade": 0.09, "turquoise": 0.1, "opal": 0.04, "amber": 0.05, "rose_quartz": 0.05, "moonstone": 0.03, "agate": 0.05,
         "jasper": 0.1, "carnelian": 0.05, "chalcedony": 0.05, "bloodstone": 0.09}
CUT = {"garnet", "ruby", "sapphire", "topaz", "peridot", "clear_quartz", "citrine", "smoky_quartz", "aquamarine", "fluorite",
       "celestine", "tourmaline"}
N4 = ((1, 0), (-1, 0), (0, 1), (0, -1))


def render(fn, pal, gem=None):
    """fn(dx, dy) -> None (outside), t, or (t, palette); dx/dy from the texture centre. Then the detailing passes:
    grain, facet lines, selective outline (lighter top/left), reflected rim light, glint."""
    rng = random.Random(hash(gem) & 0xffff)
    vals = {}
    for y in range(16):
        for x in range(16):
            v = fn(x + 0.5 - 8, y + 0.5 - 8)
            if v is not None:
                t, p = v if isinstance(v, tuple) else (v, pal)
                vals[(x, y)] = [t + rng.uniform(-1, 1) * GRAIN.get(gem, 0.015), p]
    base = {k: v[0] for k, v in vals.items()}
    edge = {k for k in vals if any((k[0] + a, k[1] + b) not in vals for a, b in N4)}
    if gem in CUT:  # facet lines: darken the darker side of every sharp step in shading
        for (x, y), t in base.items():
            for a, b in ((1, 0), (0, 1)):
                o = base.get((x + a, y + b))
                if o is not None and abs(o - t) > 0.12:
                    lo = (x, y) if t < o else (x + a, y + b)
                    vals[lo][0] -= 0.08
    for (x, y) in edge:
        lit = (x - 1, y) not in vals or (x, y - 1) not in vals
        vals[(x, y)][0] = 0.2 if lit else 0.04
    for (x, y), v in vals.items():  # light bouncing back inside the bottom-right rim
        if (x, y) not in edge and ((x + 1, y) in edge or (x, y + 1) in edge) and (x + 1, y + 1) not in vals:
            v[0] += 0.14
    img = Image.new("RGBA", (16, 16))
    for (x, y), (t, p) in vals.items():
        img.putpixel((x, y), ramp(p, t) + (255,))
    if gem in CUT:  # glint: a white star on the brightest top-left spot with a soft glow round it
        pts = [k for k in vals if k not in edge]
        gx, gy = max(pts, key=lambda k: base[k] - 0.04 * (k[0] + k[1]))
        for (a, b), f in (((0, 0), 1.0), ((1, 0), 0.55), ((-1, 0), 0.55), ((0, 1), 0.55), ((0, -1), 0.55)):
            q = (gx + a, gy + b)
            if q in vals and q not in edge:
                c = img.getpixel(q)
                img.putpixel(q, tuple(int(c[i] + (255 - c[i]) * f) for i in range(3)) + (255,))
    return img, vals


def inner(vals):
    return [p for p in vals if all((p[0] + a, p[1] + b) in vals for a, b in ((1, 0), (-1, 0), (0, 1), (0, -1)))]


def rot(dx, dy, deg):
    a = math.radians(deg)
    return dx * math.cos(a) - dy * math.sin(a), dx * math.sin(a) + dy * math.cos(a)


def sector(dx, dy, n):
    return int((math.atan2(dy, dx) + math.pi) / (2 * math.pi / n)) % n


# ---------------------------------------------------------------- item shapes

def brilliant(dx, dy):
    r = math.hypot(dx, dy)
    if r > 6.6:
        return None
    if r < 2.4:
        return 0.82 + 0.1 * light(dx, dy, 3)
    return 0.42 + 0.18 * (sector(dx, dy, 8) % 2) + 0.28 * light(dx, dy, 7) - (0.12 if r > 5.2 else 0)


def oval(dx, dy):
    e = (dx / 4.7) ** 2 + (dy / 6.7) ** 2
    if e > 1:
        return None
    if e < 0.18:
        return 0.85
    return 0.38 + 0.2 * (sector(dx, dy * 0.7, 6) % 2) + 0.3 * light(dx, dy, 7) - (0.1 if e > 0.7 else 0)


def cushion(dx, dy):
    if (abs(dx) / 6.1) ** 4 + (abs(dy) / 6.1) ** 4 > 1:
        return None
    if max(abs(dx), abs(dy)) < 2.1:
        return 0.86
    diag = abs(dx) > abs(dy)
    return 0.4 + 0.16 * ((dx > 0) ^ diag) + 0.3 * light(dx, dy, 7)


def step_cut(dx, dy):
    if abs(dx) > 4.6 or abs(dy) > 6.6 or abs(dx) + abs(dy) > 9.6:
        return None
    band = int(min(4.6 - abs(dx), 6.6 - abs(dy)) / 1.15)
    return 0.3 + 0.13 * band + 0.18 * light(dx, dy, 7) + (0.08 if band % 2 else 0)


def carved_disc(dx, dy):
    r = math.hypot(dx, dy)
    if r > 6.7 or r < 1.8:
        return None
    return 0.5 + 0.3 * light(dx, dy, 7) - (0.22 if 4.2 < r < 5.0 else 0) + (0.12 if r < 2.6 else 0)


def cabochon(dx, dy):
    if (dx / 6.6) ** 2 + (dy / 5.2) ** 2 > 1:
        return None
    return 0.95 - 0.75 * math.hypot((dx + 2.2) / 6.6, (dy + 2.0) / 5.2)


def nugget(dx, dy):
    a = math.atan2(dy, dx)
    if math.hypot(dx, dy) > 5.6 + 1.0 * math.sin(3 * a + 1) + 0.6 * math.sin(5 * a + 2):
        return None
    return 0.5 + 0.35 * light(dx, dy, 7)


def trillion(dx, dy):
    # triangle: top (0,-6.5), bottom corners (+-6.6, 5.5)
    if dy > 5.5 or abs(dx) > (dy + 6.5) * 6.6 / 12:
        return None
    d_left, d_right, d_bottom = (dy + 6.5) * 6.6 / 12 + dx, (dy + 6.5) * 6.6 / 12 - dx, 5.5 - dy
    m = min(d_left, d_right, d_bottom)
    if m > 2.0:
        return 0.84
    return {d_left: 0.72, d_right: 0.45, d_bottom: 0.3}[m]


def drop(dx, dy):
    if dy > 1.5:
        if math.hypot(dx, dy - 1.5) > 5.3:
            return None
    elif dy < -6.8 or abs(dx) > 5.3 * ((dy + 6.8) / 8.3) ** 0.9:
        return None
    return 0.95 - 0.7 * math.hypot((dx + 1.8) / 6, (dy - 0.5) / 7)


def prism(dx, dy, half=2.6, top=3.0, tip=3.6, bottom_tip=0.0, flat=False, deg=35):
    """A hexagonal crystal tilted deg degrees: shaft from y=-top.. down, pointed (or flat) tip."""
    u, v = rot(dx, dy, deg)
    lo = 6.8 - bottom_tip
    if v > lo + bottom_tip:
        return None
    if v < -top:
        if flat or v < -top - tip:
            return None
        w = half * (1 - (-top - v) / tip)
        if abs(u) > w:
            return None
        return 0.55 + (0.35 if u < -w / 3 else 0.15 if u < w / 3 else -0.05)
    if v > lo:
        w = half * (1 - (v - lo) / bottom_tip)
        if abs(u) > w:
            return None
    elif abs(u) > half:
        return None
    face = 0.62 if u < -half / 3 else 0.82 if u < half / 3 else 0.4
    if flat and v < -top + 1.0:
        return 0.95
    return face - 0.12 * (v + top) / (lo + top)


def chunk(dx, dy, seed=5):
    # irregular faceted lump: Voronoi cells, each flat-shaded by where its seed sits
    a = math.atan2(dy, dx)
    if math.hypot(dx, dy) > 5.9 + 0.9 * math.sin(2 * a + 0.5) + 0.5 * math.cos(5 * a):
        return None
    rng = random.Random(seed)
    seeds = [(rng.uniform(-5, 5), rng.uniform(-5, 5), rng.uniform(-0.08, 0.08)) for _ in range(7)]
    sx, sy, j = min(seeds, key=lambda s: (s[0] - dx) ** 2 + (s[1] - dy) ** 2)
    return 0.55 + 0.35 * light(sx, sy, 6) + j


def octahedron(dx, dy):
    if abs(dx) / 5.6 + abs(dy) / 7.2 > 1:
        return None
    t = {(False, False): 0.85, (True, False): 0.6, (False, True): 0.5, (True, True): 0.3}[(dx > 0, dy > 0)]
    return (t, GREEN_FLUORITE) if abs(dy) < 1.6 else t  # a green zone across the purple


def blades(dx, dy):
    best = None
    for ox, deg, half in ((-3.2, 18, 1.4), (0.5, 0, 1.6), (3.6, -22, 1.3)):
        v = prism(dx - ox, dy - 1, half=half, top=4.5, tip=2.2, deg=deg)
        if v is not None:
            best = v
    return best


def striated(dx, dy):
    v = prism(dx, dy, half=2.3, top=4.6, tip=1.6, flat=True, deg=40)
    if v is None:
        return None
    u, w = rot(dx, dy, 40)
    t = v + (0.1 if int(u + 10) % 2 else -0.05)
    return (t, GREEN_TOURMALINE) if w > 1.2 else t  # green base, pink tip


def agate_slice(dx, dy):
    a = math.atan2(dy, dx)
    d = math.hypot(dx / 6.5, dy / 5.7)
    if d > 1:
        return None
    d += 0.07 * math.sin(3 * a + 1)
    if d > 0.86:
        return 0.15  # dark rind
    return 0.25 + 0.7 * ((d * 5.5) % 1 > 0.5) + 0.05 * light(dx, dy, 7)


def tumbled(deg, rx=6.3, ry=4.6):
    def fn(dx, dy):
        u, v = rot(dx, dy, deg)
        if (u / rx) ** 2 + (v / ry) ** 2 > 1:
            return None
        return 0.88 - 0.7 * math.hypot((u + 2) / rx, (v + 1.6) / ry)
    return fn


def botryoidal(dx, dy):
    for cx, cy, r in ((-2.6, 1.8, 3.4), (2.4, 2.4, 3.2), (0.2, -2.2, 3.5), (3.6, -2.0, 2.2), (-3.8, -2.0, 2.0)):
        d = math.hypot(dx - cx, dy - cy)
        if d < r:
            return 0.9 - 0.65 * math.hypot(dx - cx + r * 0.4, dy - cy + r * 0.4) / r
    return None


ITEM_SHAPES = {
    "garnet": brilliant, "ruby": oval, "sapphire": cushion, "topaz": step_cut, "jade": carved_disc, "opal": cabochon,
    "turquoise": nugget, "peridot": trillion, "amber": drop,
    "clear_quartz": lambda dx, dy: prism(dx, dy), "rose_quartz": chunk,
    "citrine": lambda dx, dy: (lambda v: None if v is None else v + 0.25 * clamp((-rot(dx, dy, 35)[1]) / 7))(prism(dx, dy, half=2.9)),
    "smoky_quartz": lambda dx, dy: prism(dx, dy, half=2.8, top=3.2, tip=3.2, bottom_tip=3.0, deg=-35),
    "aquamarine": lambda dx, dy: prism(dx, dy, half=2.5, top=6.0, flat=True, deg=45),
    "fluorite": octahedron, "celestine": blades, "moonstone": cabochon, "tourmaline": striated,
    "agate": agate_slice, "jasper": tumbled(25), "carnelian": tumbled(-30, 5.6, 5.0), "chalcedony": botryoidal,
    "bloodstone": tumbled(10, 6.0, 5.2),
}


def item_marks(gem, img, vals, rng):
    pts = inner(vals)
    put = lambda p, c: img.putpixel(p, c + (255,))
    if gem == "opal":
        for p in pts:
            if rng.random() < 0.3:
                put(p, rng.choice(OPAL_FIRE))
    elif gem == "moonstone":
        for p in pts:  # blue adularescence floating over the top left
            if math.hypot(p[0] - 6, p[1] - 6) < 2.6:
                put(p, (176, 206, 255))
    elif gem == "turquoise":
        for _ in range(3):  # dark matrix veins
            x, y = rng.choice(pts)
            for _ in range(6):
                if (x, y) in pts:
                    put((x, y), (58, 48, 38))
                x, y = x + rng.choice((-1, 0, 1)), y + rng.choice((0, 1))
    elif gem == "amber":
        for p in ((8, 9), (7, 10), (9, 10), (8, 11)):  # a trapped insect
            put(p, (60, 30, 8))
    elif gem in ("bloodstone", "jasper"):
        colours = [(196, 30, 30), (150, 18, 18)] if gem == "bloodstone" else [(90, 22, 14), (226, 170, 130)]
        for p in pts:
            if rng.random() < 0.14:
                put(p, rng.choice(colours))
    elif gem == "carnelian":
        for p in pts:  # translucent glow inside
            if math.hypot(p[0] - 9, p[1] - 9) < 2.2:
                put(p, ramp(PAL[gem], 0.9))
    if gem in GRAIN:  # opaque stones: a soft wet sheen spot instead of a hard glint
        sx, sy = min(pts, key=lambda p: p[0] + p[1])
        for q in ((sx + 1, sy + 1), (sx + 2, sy + 1), (sx + 1, sy + 2)):
            if q in pts:
                c = img.getpixel(q)
                put(q, tuple(int(c[i] + (255 - c[i]) * 0.5) for i in range(3)))
    return img


# ---------------------------------------------------------------- crystal blocks (cross textures, base at the bottom)

def point_pixels(bx, w, h, tilt, tipfrac):
    """Pixels of one crystal growing up from the bottom edge, with its face shading."""
    out = {}
    for y in range(16):
        up = 16 - (y + 0.5)  # height above the base
        if up > h:
            continue
        c = bx + tilt * up
        v = up / h
        hw = w / 2 if v < 1 - tipfrac else w / 2 * (1 - v) / max(tipfrac, 0.01)
        for x in range(16):
            u = x + 0.5 - c
            if abs(u) <= hw:
                if v > 1 - tipfrac:  # termination: bright left facet, darker right facet, ridge between
                    face = 0.95 if u < -0.3 else 0.78 if u < 0.4 else 0.55
                else:  # shaft: shaded left face, lit middle face with a ridge highlight, right face with bounce light
                    face = 0.42 if u < -w / 6 else 0.92 if u < -w / 6 + 1 else 0.78 if u < w / 6 else 0.58
                    if u > hw - 1:
                        face += 0.1
                out[(x, y)] = face - 0.22 * (1 - v) ** 2  # darker where it grows out of the rock
    return out


def draw_layers(layers, pal_of, seed=0):
    """Back to front; a dark rim on each crystal's shaded left side (thin tips stay bright), a darker seam where a front
    crystal overlaps one behind it, a little grain, and a glint on the tallest tip."""
    rng = random.Random(seed)
    img = Image.new("RGBA", (16, 16))
    covered = {}
    for i, px in enumerate(layers):
        for (x, y), t in px.items():
            edge = (x - 1, y) not in px
            seam = (x + 1, y) not in px and (x + 1, y) in covered
            t = min(t, 0.22) if edge else (t - 0.12 if seam else t) + rng.uniform(-0.03, 0.03)
            img.putpixel((x, y), ramp(pal_of(i, x, y), t) + (255,))
            covered[(x, y)] = i
    tips = [min(px, key=lambda p: p[1]) for px in layers if px]
    if tips:
        gx, gy = min(tips, key=lambda p: p[1])
        img.putpixel((gx, gy), (255, 255, 255, 255))
        if (gx, gy + 1) in covered:
            c = img.getpixel((gx, gy + 1))
            img.putpixel((gx, gy + 1), tuple((c[i] + 255) // 2 for i in range(3)) + (255,))
    return img


SCALE = {"small": (2, 0.38), "medium": (3, 0.65), "large": (5, 1.0)}


def crystal_texture(gem, size, seed):
    rng = random.Random(seed)
    n, k = SCALE[size]
    pal = PAL[gem]
    layers, pal_of = [], lambda i, x, y: pal
    specs = sorted([(rng.uniform(4, 12), rng.uniform(0.6, 1.0)) for _ in range(n)], key=lambda s: -s[1])  # tall ones behind
    for bx, hf in specs:
        if gem == "clear_quartz":
            px = point_pixels(bx, rng.uniform(3, 4), 15 * k * hf, rng.uniform(-0.2, 0.2), 0.3)
        elif gem == "rose_quartz":  # stubby, wide, broad tips
            px = point_pixels(bx, rng.uniform(4.5, 6), 11 * k * hf, rng.uniform(-0.15, 0.15), 0.45)
        elif gem == "citrine":  # golden tips over pale bases
            px = point_pixels(bx, rng.uniform(3, 4), 14 * k * hf, rng.uniform(-0.2, 0.2), 0.3)
            px = {p: t + 0.25 * (1 - p[1] / 16) - 0.15 for p, t in px.items()}
        elif gem == "smoky_quartz":  # slender and tall
            px = point_pixels(bx, rng.uniform(2, 3), 16 * k * hf, rng.uniform(-0.12, 0.12), 0.25)
        elif gem == "aquamarine":  # flat-topped columns
            px = point_pixels(bx, rng.uniform(3, 4), 15 * k * hf, rng.uniform(-0.1, 0.1), 0.0)
            top = min(y for _, y in px) if px else 0
            px = {p: (0.95 if p[1] == top else t) for p, t in px.items()}
        elif gem == "celestine":  # thin leaning blades
            px = point_pixels(bx, 3, 15 * k * hf, rng.choice((-1, 1)) * rng.uniform(0.2, 0.35), 0.35)
        elif gem == "tourmaline":  # striated columns, green below, pink above
            px = point_pixels(bx, 3, 16 * k * hf, rng.uniform(-0.1, 0.1), 0.08)
            px = {p: t + (0.1 if p[0] % 2 else -0.05) for p, t in px.items()}
            cut = 16 - 16 * k * hf * 0.5
            pal_of = lambda i, x, y, cut=cut: GREEN_TOURMALINE if y > cut else pal
        elif gem == "fluorite":  # stacked cubes, purple or green
            s = {"small": 3, "medium": 4, "large": 5}[size] + rng.randint(0, 1)
            px, y0, x0 = {}, 16, int(bx - s / 2)  # cubes sit on the surface they grow from, a smaller one stacked on top
            for _ in range(2 if size == "large" and rng.random() < 0.5 else 1):
                for x in range(x0, x0 + s):
                    for y in range(y0 - s, y0):
                        if 0 <= x < 16 and 0 <= y < 16:  # lit top face, shaded left face, front face with a cleavage line
                            px[(x, y)] = 0.95 if y == y0 - s else 0.45 if x == x0 else 0.6 if (x - x0) == (y - y0 + s) else 0.74
                y0, x0, s = y0 - s, x0 + rng.randint(0, 1), max(2, s - 2)
        else:  # moonstone: rounded nodules
            r = rng.uniform(3.2, 4.6) * (0.55 + 0.45 * k) * hf
            px = {}
            for y in range(16):
                for x in range(16):
                    d = math.hypot(x + 0.5 - bx, y + 0.5 - 16)
                    if d < r:
                        px[(x, y)] = 0.95 - 0.6 * math.hypot(x + 0.5 - bx + r * 0.4, y + 0.5 - 16 + r * 0.5) / r
        layers.append(px)
    if gem == "fluorite":
        greens = {i for i in range(len(layers)) if rng.random() < 0.45}
        pal_of = lambda i, x, y: GREEN_FLUORITE if i in greens else pal
    img = draw_layers(layers, pal_of, seed)
    if gem == "moonstone":
        for (x, y) in [p for l in layers for p in l]:
            if rng.random() < 0.12:
                img.putpixel((x, y), (176, 206, 255, 255))
    return img


# ---------------------------------------------------------------- ore inclusions

def kernel(style, rng):
    """One inclusion: {(dx, dy): t}."""
    if style == "grains":
        return {(0, 0): 0.85, (1, 0): 0.6, (0, 1): 0.55, (1, 1): 0.35}
    if style == "hexes":
        return {(0, -1): 0.9, (-1, 0): 0.75, (0, 0): 0.8, (1, 0): 0.5, (0, 1): 0.4, (-1, 1): 0.55, (1, -1): 0.6}
    if style == "barrels":
        return {(0, -1): 0.9, (1, -1): 0.7, (0, 0): 0.75, (1, 0): 0.5, (0, 1): 0.55, (1, 1): 0.3}
    if style == "prisms":
        d = rng.choice((1, -1))
        return {(i * d, -i): 0.4 + 0.15 * i for i in range(4)}
    if style == "veins":
        out, x, y = {}, 0, 0
        for i in range(rng.randint(5, 8)):
            out[(x, y)] = 0.45 + 0.4 * (i % 2)
            x, y = x + rng.choice((1, 1, 0)), y + rng.choice((-1, 0, 1))
        return out
    if style == "fire":
        return {(x, y): -1 for x in range(3) for y in range(2)} | {(1, -1): -1}
    if style == "matrix":
        return {(x, y): (0.15 if rng.random() < 0.25 else 0.4 + 0.15 * (1 - x - y)) for x in range(-1, 2) for y in range(-1, 2)
                if abs(x) + abs(y) < 2 or rng.random() < 0.5}
    if style == "granular":
        return {(rng.randint(-2, 2), rng.randint(-2, 2)): rng.uniform(0.4, 0.9) for _ in range(6)}
    return {(x, y): 0.95 - 0.3 * (x + y + 2) / 2 for x in range(-1, 2) for y in range(-1, 2) if abs(x) + abs(y) < 2 or (x, y) == (-1, -1)}


ORE_STYLE = {"garnet": ("grains", 8), "ruby": ("hexes", 4), "sapphire": ("barrels", 5), "topaz": ("prisms", 5), "jade": ("veins", 3),
             "opal": ("fire", 5), "turquoise": ("matrix", 4), "peridot": ("granular", 4), "amber": ("blobs", 4)}


def ore_texture(gem, host, seed):
    rng = random.Random(seed)
    out = vanilla({"stone": "block/stone", "deepslate": "block/deepslate", "sandstone": "block/sandstone"}[host])
    style, n = ORE_STYLE[gem]
    placed = {}
    for _ in range(n):
        for _ in range(30):  # spread inclusions apart, wrapping round the tile
            cx, cy = rng.randint(0, 15), rng.randint(0, 15)
            k = kernel(style, rng)
            spots = {((cx + dx) % 16, (cy + dy) % 16): t for (dx, dy), t in k.items()}
            if not any(abs(x - px) + abs(y - py) < 2 for x, y in spots for px, py in placed):
                placed |= spots
                break
    for (x, y), t in placed.items():
        sx, sy = (x + 1) % 16, (y + 1) % 16
        if (sx, sy) not in placed:  # a shadow under each inclusion
            r, g, b, a = out.getpixel((sx, sy))
            out.putpixel((sx, sy), (int(r * 0.7), int(g * 0.7), int(b * 0.7), a))
        for a, b in ((-1, 0), (0, -1), (-1, -1), (1, -1), (-1, 1)):  # mineral-stained rock round each inclusion
            q = ((x + a) % 16, (y + b) % 16)
            if q not in placed and q != (sx, sy):
                c, tint = out.getpixel(q), ramp(PAL[gem], 0.25)
                out.putpixel(q, tuple(int(c[i] * 0.8 + tint[i] * 0.2) for i in range(3)) + (255,))
    for (x, y), t in placed.items():
        if t < 0:
            out.putpixel((x, y), rng.choice(OPAL_FIRE + [ramp(PAL[gem], 0.9)] * 3) + (255,))
            continue
        lit = ((x - 1) % 16, y) not in placed or (x, (y - 1) % 16) not in placed
        dark = ((x + 1) % 16, y) not in placed or (x, (y + 1) % 16) not in placed
        t += (0.18 if lit else 0) - (0.2 if dark and not lit else 0) + rng.uniform(-0.04, 0.04)
        out.putpixel((x, y), ramp(PAL[gem], t) + (255,))
    # one glint per tile on the brightest inclusion pixel (not on the opaque stones)
    if gem not in GRAIN and placed:
        g = max(placed, key=lambda p: placed[p] - 0.01 * (p[0] + p[1]))
        out.putpixel(g, (255, 255, 255, 255))
    return out


# ---------------------------------------------------------------- pebble block tiles

def value_noise(seed, cells=4):
    rng = random.Random(seed)
    grid = [[rng.random() for _ in range(cells)] for _ in range(cells)]

    def f(x, y):
        gx, gy = x / 16 * cells, y / 16 * cells
        i, j, fx, fy = int(gx), int(gy), gx % 1, gy % 1
        g = lambda a, b: grid[b % cells][a % cells]
        top = g(i, j) * (1 - fx) + g(i + 1, j) * fx
        bot = g(i, j + 1) * (1 - fx) + g(i + 1, j + 1) * fx
        return top * (1 - fy) + bot * fy
    return f


def pebble_tile(gem, seed):
    rng = random.Random(seed)
    nz, fine = value_noise(seed, 4), value_noise(seed + 1, 8)
    img = Image.new("RGBA", (16, 16))
    pal = PAL[gem]
    for y in range(16):
        for x in range(16):
            n = 0.6 * nz(x, y) + 0.4 * fine(x, y)
            if gem == "agate":
                t = 0.3 + 0.6 * ((math.hypot(x - 7.5, y - 7.5) / 2.2 + n * 1.5) % 1 > 0.5)
            elif gem == "jasper":
                t = 0.25 + 0.5 * n + (0.35 if fine(x * 2, y * 2) > 0.8 else 0)
            elif gem == "carnelian":
                t = 0.35 + 0.6 * n ** 1.5
            elif gem == "chalcedony":
                d = min(math.hypot((x - cx) % 16 - 3, (y - cy) % 16 - 3) for cx, cy in ((0, 0), (8, 4), (3, 9), (11, 11)))
                t = 0.95 - 0.25 * d if d < 3 else 0.3 + 0.2 * n
            else:  # bloodstone
                t = 0.15 + 0.5 * n
            t += rng.uniform(-0.06, 0.06) + (0.3 if rng.random() < 0.03 else 0)  # fine grain and glinting specks
            colour = ramp(pal[:4] if gem == "bloodstone" else pal, t)
            if gem == "bloodstone" and rng.random() < 0.1:
                colour = (186, 28, 28)
            img.putpixel((x, y), colour + (255,))
    return img


# ---------------------------------------------------------------- cuts for the crystal gems (their raw habit is now the rough gem)

def princess(dx, dy):
    if max(abs(dx), abs(dy)) > 6.0:
        return None
    if max(abs(dx), abs(dy)) < 2.0:
        return 0.88
    return 0.4 + 0.18 * ((dx > 0) ^ (dy > 0) ^ (abs(dx) > abs(dy))) + 0.25 * light(dx, dy, 7) - (0.1 if max(abs(dx), abs(dy)) > 4.8 else 0)


def heart(dx, dy):
    x, y = dx / 5.4, -dy / 5.4 + 0.1  # the curve is ~2.3 wide, so this keeps it inside 16 px
    if (x * x + y * y - 1) ** 3 - x * x * y ** 3 > 0:
        return None
    return 0.45 + 0.2 * (sector(dx, dy, 6) % 2) + 0.3 * light(dx, dy, 7)


def pear(dx, dy):
    if dy > 0.5:
        if math.hypot(dx, dy - 0.5) > 5.6:
            return None
    elif dy < -6.9 or abs(dx) > 5.6 * ((dy + 6.9) / 7.4) ** 1.2:
        return None
    if math.hypot(dx, dy) < 2.0:
        return 0.86
    return 0.4 + 0.2 * (sector(dx, dy - 1, 7) % 2) + 0.28 * light(dx, dy, 7)


def marquise(dx, dy):
    u, v = rot(dx, dy, 45)
    if abs(u) > 3.6 * (1 - (v / 7.6) ** 2):
        return None
    if abs(v) > 7.6:
        return None
    return 0.82 if abs(u) < 1.0 and abs(v) < 3 else 0.4 + 0.22 * ((u > 0) ^ (abs(v) > 3.5)) + 0.25 * light(dx, dy, 7)


def hex_step(dx, dy):
    if abs(dy) > 4.4 or abs(dx) + abs(dy) * 0.6 > 7.0:
        return None
    band = int(min(4.4 - abs(dy), (7.0 - abs(dx) - abs(dy) * 0.6) * 0.8) / 1.1)
    return 0.32 + 0.14 * band + 0.18 * light(dx, dy, 7) + (0.07 if band % 2 else 0)


def checkerboard(dx, dy):
    if max(abs(dx), abs(dy)) > 6.2 or abs(dx) + abs(dy) > 8.8:
        return None
    t = 0.45 + 0.25 * ((int(dx + 8) // 2 + int(dy + 8) // 2) % 2) + 0.2 * light(dx, dy, 7)
    return (t, GREEN_FLUORITE) if abs(dy) < 1.6 else t


def kite(dx, dy):
    if dy < -6.8 or dy > 6.8:
        return None
    w = 5.4 * (dy + 6.8) / 4.8 if dy < -2 else 5.4 * (6.8 - dy) / 8.8
    if abs(dx) > w:
        return None
    return 0.86 if abs(dx) < 1 and -3 < dy < 2 else 0.4 + 0.2 * ((dx > 0) ^ (dy > -2)) + 0.28 * light(dx, dy, 7)


def round_cabochon(dx, dy):
    if math.hypot(dx, dy) > 6.0:
        return None
    return 0.95 - 0.75 * math.hypot(dx + 2.2, dy + 2.2) / 6.0


def bicolour_bar(dx, dy):
    u, v = rot(dx, dy, 45)
    if abs(u) > 2.6 or abs(v) > 7.4:
        return None
    band = int(min(2.6 - abs(u), 7.4 - abs(v)) / 0.9)
    t = 0.35 + 0.18 * band + 0.15 * light(dx, dy, 7)
    return (t, GREEN_TOURMALINE) if v > 0 else t


CUT_SHAPES = {"clear_quartz": princess, "rose_quartz": heart, "citrine": pear, "smoky_quartz": marquise, "aquamarine": hex_step,
              "fluorite": checkerboard, "celestine": kite, "moonstone": round_cabochon, "tourmaline": bicolour_bar}
CUT |= {"rose_quartz", "celestine"}

# ---------------------------------------------------------------- rough gems

VPAL = {"diamond": [(120, 150, 160), (170, 205, 212), (206, 236, 240), (232, 250, 252), (255, 255, 255)],
        "emerald": [(6, 60, 30), (14, 110, 54), (30, 160, 80), (90, 210, 130), (190, 250, 210)],
        "amethyst": [(52, 26, 90), (92, 50, 150), (140, 90, 200), (186, 140, 232), (230, 206, 255)]}
CRUST = {"stone": [(70, 70, 72), (100, 100, 102), (128, 128, 130)], "deepslate": [(40, 40, 46), (62, 62, 68), (86, 86, 92)],
         "sandstone": [(170, 150, 100), (200, 182, 130), (222, 208, 160)], "kimberlite": [(40, 52, 60), (62, 78, 86), (86, 104, 110)],
         "mica": [(170, 168, 150), (206, 204, 186), (236, 234, 220)], "calcite": [(190, 192, 188), (220, 222, 218), (244, 244, 240)],
         "clay": [(110, 90, 70), (140, 116, 92), (168, 142, 116)]}


def lump(seed, r=5.6):
    rng = random.Random(seed)
    h = [rng.uniform(0.4, 1.1) for _ in range(3)]
    ph = [rng.uniform(0, 6.3) for _ in range(3)]
    return lambda dx, dy: math.hypot(dx, dy) <= r + sum(h[i] * math.sin((i + 2) * math.atan2(dy, dx) + ph[i]) for i in range(3))


def rough_render(gem, pal, core, crust, seed, crust_share=0.35):
    """A rough stone: core(dx, dy) -> t or None for the gem, the rest of the lump is rock crust (matte, grainy)."""
    rng = random.Random(seed)
    inside = lump(seed)
    cells = [(rng.uniform(-6, 6), rng.uniform(-6, 6), rng.uniform(-0.1, 0.1)) for _ in range(8)]

    def fn(dx, dy):
        t = core(dx, dy)
        if t is not None:
            return (t if isinstance(t, tuple) else t + rng.uniform(-0.06, 0.06))
        if not inside(dx, dy):
            return None
        sx, sy, j = min(cells, key=lambda c: (c[0] - dx) ** 2 + (c[1] - dy) ** 2)
        if rng.random() < 0.08:  # a speck of the gem showing through the crust
            return 0.55
        return (clamp(0.5 + 0.35 * light(sx, sy, 6) + j + rng.uniform(-0.08, 0.08)), crust + [crust[-1]] * 2)
    return render(fn, pal, None)


def rough_core(gem, kind, seed):
    """The gem's own part of the rough stone, in its natural habit."""
    rng = random.Random(seed)
    if gem in ("ruby", "sapphire"):  # hexagonal barrel
        return lambda dx, dy: prism(dx + 0.5, dy, half=2.8, top=3.6, flat=True, deg=-20)
    if gem in ("topaz", "emerald", "aquamarine"):
        return lambda dx, dy: prism(dx, dy, half=2.4, top=5.0, flat=True, deg=30)
    if gem == "garnet":  # dodecahedral crystal
        return lambda dx, dy: (None if max(abs(dx) * 0.87 + abs(dy) * 0.5, abs(dy)) > 4.6 else
                               0.45 + 0.2 * (sector(dx, dy, 6) % 2) + 0.3 * light(dx, dy, 5))
    if gem == "diamond":  # octahedron
        return lambda dx, dy: (None if abs(dx) / 4.6 + abs(dy) / 5.6 > 1 else
                               {(False, False): 0.92, (True, False): 0.66, (False, True): 0.55, (True, True): 0.38}[(dx > 0, dy > 0)])
    if gem == "amethyst":  # a geode piece: points on a calcite base
        return lambda dx, dy: blades(dx, dy + 1.5)
    if kind == "crystal":
        return lambda dx, dy: (lambda v: v)(ITEM_SHAPES[gem](dx * 1.25, dy * 1.25))
    if kind == "pebble":
        return lambda dx, dy: (None if math.hypot(dx - 0.5, dy - 0.5) > 4.0 else 0.5 + 0.25 * light(dx, dy, 5))
    # opaque ore gems (jade, opal, turquoise, peridot, amber): an irregular seam through the rock
    cx, cy, ang = rng.uniform(-1, 1), rng.uniform(-1, 1), rng.uniform(0, 180)
    return lambda dx, dy: (lambda u, v: None if abs(v) > 2.2 + 0.8 * math.sin(u * 0.9 + ang) or abs(u) > 6 else
                           0.5 + 0.3 * light(dx, dy, 6))(*rot(dx - cx, dy - cy, ang))


def crust_of(gem, g):
    if gem == "diamond":
        return CRUST["kimberlite"]
    if gem in ("emerald", "aquamarine"):
        return CRUST["mica"]
    if gem in ("amethyst", "celestine", "fluorite"):
        return CRUST["calcite"]
    if gem == "amber":
        return CRUST["clay"]
    if g is None:
        return CRUST["stone"]
    return CRUST[g["hosts"][0]] if g["kind"] == "ore" else CRUST["stone"]


def flawed(cut, seed):
    """The cut gem, dulled, with a crack across it and a chipped corner."""
    rng = random.Random(seed)
    img = cut.copy()
    px = [(x, y) for y in range(16) for x in range(16) if img.getpixel((x, y))[3]]
    for p in px:
        r, g, b, a = img.getpixel(p)
        grey = (r + g + b) // 3
        img.putpixel(p, (int((r * 0.6 + grey * 0.4) * 0.88), int((g * 0.6 + grey * 0.4) * 0.88), int((b * 0.6 + grey * 0.4) * 0.88), a))
    top = min(px, key=lambda p: (p[1], abs(p[0] - 9)))
    x, y = top[0], top[1] + 1
    while (x, y) in px:  # jagged crack, dark with a pale lip on its right
        img.putpixel((x, y), (30, 26, 28, 255))
        if (x + 1, y) in px:
            c = img.getpixel((x + 1, y))
            img.putpixel((x + 1, y), tuple(min(255, v + 60) for v in c[:3]) + (255,))
        x, y = x + rng.choice((-1, 0, 0, 1)), y + 1
    edge = [p for p in px if sum((p[0] + a, p[1] + b) in px for a, b in N4) <= 2]
    for p in rng.sample(edge, min(2, len(edge))):  # chipped
        img.putpixel(p, (0, 0, 0, 0))
    return img


def flawless(cut, seed):
    """The cut gem, clearer and brighter, with a light rim and twinkling four-point stars (the glint comes in game)."""
    rng = random.Random(seed)
    img = cut.copy()
    px = [(x, y) for y in range(16) for x in range(16) if img.getpixel((x, y))[3]]
    for p in px:
        r, g, b, a = img.getpixel(p)
        m = (r + g + b) / 3
        img.putpixel(p, tuple(max(0, min(255, int(m + (v - m) * 1.25 + 18))) for v in (r, g, b)) + (a,))
    inner_px = [p for p in px if all((p[0] + a, p[1] + b) in px for a, b in N4)]
    for p in px:  # light rim on the top-left edge
        if p not in inner_px and ((p[0] - 1, p[1]) not in px or (p[0], p[1] - 1) not in px) and p[0] + p[1] < 16:
            c = img.getpixel(p)
            img.putpixel(p, tuple(min(255, v + 70) for v in c[:3]) + (255,))
    stars = sorted(inner_px, key=lambda p: p[0] + p[1])
    for cx, cy in (stars[len(stars) // 6], stars[len(stars) * 4 // 5]) if stars else ():
        for (a, b), f in (((0, 0), 1.0), ((1, 0), 0.6), ((-1, 0), 0.6), ((0, 1), 0.6), ((0, -1), 0.6)):
            q = (cx + a, cy + b)
            if q in px:
                c = img.getpixel(q)
                img.putpixel(q, tuple(int(c[i] + (255 - c[i]) * f) for i in range(3)) + (255,))
    for _ in range(3):  # a few loose sparkles
        if inner_px:
            img.putpixel(rng.choice(inner_px), (255, 255, 240, 255))
    return img


LIB = Path(r"C:\Users\Yngve\Documents\Minecraft Modding\item")


def recolour(src, pal):
    out = src.copy()
    px = [(x, y) for y in range(16) for x in range(16) if src.getpixel((x, y))[3]]
    lums = [sum(src.getpixel(p)[:3]) / 3 for p in px]
    lo, hi = min(lums), max(lums)
    for (x, y), lum in zip(px, lums):
        out.putpixel((x, y), ramp(pal, (lum - lo) / max(1, hi - lo)) + (src.getpixel((x, y))[3],))
    return out


# ---------------------------------------------------------------- write everything
done = 0
for n, g in enumerate(GEMS):
    gem, kind = g["name"], g["kind"]
    raw_img, raw_vals = render(ITEM_SHAPES[gem], PAL[gem], gem)
    raw_img = item_marks(gem, raw_img, raw_vals, random.Random(n))
    if kind == "crystal":  # the raw crystal is the rough gem, a new cut is the finished one
        cut_img, cut_vals = render(CUT_SHAPES[gem], PAL[gem], gem)
        cut_img = item_marks(gem, cut_img, cut_vals, random.Random(n + 50))
        rough_img, _ = rough_render(gem, PAL[gem], rough_core(gem, kind, n), crust_of(gem, g), n + 100)
    else:
        cut_img = raw_img
        rough_img, rough_vals = rough_render(gem, PAL[gem], rough_core(gem, kind, n), crust_of(gem, g), n + 100)
        if gem in ("opal", "turquoise", "amber", "bloodstone", "jasper"):
            rough_img = item_marks(gem, rough_img, {p: v for p, v in rough_vals.items()}, random.Random(n + 7))
    cut_img.save(TEX / f"item/{gem}.png")
    rough_img.save(TEX / f"item/rough_{gem}.png")
    flawed(cut_img, n).save(TEX / f"item/flawed_{gem}.png")
    flawless(cut_img, n).save(TEX / f"item/flawless_{gem}.png")
    done += 4
    if kind == "ore":
        for host in g["hosts"]:
            ore_texture(gem, host, n * 7 + len(host)).save(TEX / f"block/{ore_block(gem, host)}.png")
            done += 1
    elif kind == "crystal":
        for i, s in enumerate(SIZES):
            crystal_texture(gem, s, n * 11 + i).save(TEX / f"block/{s}_{gem}_crystal.png")
            done += 1
    else:
        pebble_tile(gem, n).save(TEX / f"block/{gem}_pebble.png")
        done += 1
for n, (gem, item) in enumerate((("diamond", "item/diamond"), ("emerald", "item/emerald"), ("amethyst", "item/amethyst_shard"))):
    rough_img, _ = rough_render(gem, VPAL[gem], rough_core(gem, "ore", 200 + n), crust_of(gem, None), 300 + n, 0.3)
    rough_img.save(TEX / f"item/rough_{gem}.png")
    flawed(vanilla(item), 400 + n).save(TEX / f"item/flawed_{gem}.png")
    flawless(vanilla(item), 500 + n).save(TEX / f"item/flawless_{gem}.png")
    done += 3
# grits from the user's dust library: obsidian dust as flint, ruby dust recoloured garnet-brown and corundum-grey-pink, diamond dust
dust = lambda name: Image.open(LIB / name).convert("RGBA")
recolour(dust("obsidian_dust.png"), [(18, 18, 22), (40, 40, 46), (70, 70, 78), (110, 110, 118), (170, 170, 176)]).save(TEX / "item/flint_grit.png")
recolour(dust("ruby_dust.png"), [(50, 10, 10), (96, 24, 22), (140, 44, 36), (180, 80, 66), (220, 150, 130)]).save(TEX / "item/garnet_grit.png")
recolour(dust("ruby_dust.png"), [(70, 64, 70), (110, 100, 108), (150, 132, 142), (190, 170, 180), (236, 220, 228)]).save(TEX / "item/corundum_grit.png")
dust("diamond_dust.png").save(TEX / "item/diamond_dust.png")
done += 4
print(f"gemstone textures: {done}")
