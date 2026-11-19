"""Textures for the Fire Pit (data: gen_firepit.py). All drawn here, pixel by pixel, from seeded noise:

- fire_pit_stone / _stone_dark / _stone_sooted: the ring rocks. Grey fieldstone (rounded light from the top left, a dark
  rim, hairline cracks, quartz flecks, a few lichen dots), a brown-grey granite with pink feldspar and black mica, and
  the grey stone blackened by smoke (soot heavy at the bottom, streaks running up) for the faces turned to the fire
- fire_pit_ash / _ash_hot: the ash bed. Pale grey ash with charcoal lumps and white flakes; hot: darker ash with
  clusters of glowing coals (yellow core, orange, red rim)
- fire_pit_log / _log_end: fresh firewood, furrowed bark with the grain along u; the cut end with rings and a crack
- fire_pit_log_lit / _log_end_lit: burning logs, charred bark split by glowing orange cracks; the end black with a
  glowing ring
- fire_pit_log_charred / _log_end_charred: cold burnt logs, black "alligator" char cells split by silvery ash lines
- fire_pit_spit: green sapling wood, pale where the bark is stripped, with bark flecks (no grain direction, it is used
  both along the spits and up the uprights)
- fire_pit_fire: 16 frames, a tall flame from tileable noise scrolled upwards (loops in 32 ticks), cut out sharp
- item/fire_pit: the ring of rocks seen from above at an angle, ash inside and two crossed charred logs
Run: python scripts/gen_firepit_textures.py
"""
import json
import math
from pathlib import Path

from PIL import Image

TEX = Path(__file__).resolve().parent.parent / "src/main/resources/assets/mechanical/textures"


def save(img, rel):
    p = TEX / f"{rel}.png"
    p.parent.mkdir(parents=True, exist_ok=True)
    img.save(p)


def h(x, y, salt):
    """Deterministic per-pixel hash 0..1."""
    n = (x * 374761393 + y * 668265263 + salt * 2147483647) & 0xFFFFFFFF
    n = ((n ^ (n >> 13)) * 1274126177) & 0xFFFFFFFF
    return ((n ^ (n >> 16)) % 10000) / 10000


def vnoise(x, y, salt, cell, px=None, py=None):
    """Smooth value noise; periodic with px x py lattice cells if given (tiles seamlessly)."""
    gx, gy = x / cell, y / cell
    x0, y0 = math.floor(gx), math.floor(gy)
    fx, fy = gx - x0, gy - y0
    fx, fy = fx * fx * (3 - 2 * fx), fy * fy * (3 - 2 * fy)

    def g(i, j):
        if px:
            i %= px
        if py:
            j %= py
        return h(i, j, salt)
    a = g(x0, y0) + (g(x0 + 1, y0) - g(x0, y0)) * fx
    b = g(x0, y0 + 1) + (g(x0 + 1, y0 + 1) - g(x0, y0 + 1)) * fx
    return a + (b - a) * fy


def fbm(x, y, salt, cells=(8, 4, 2), px=None):
    total, wsum, w = 0, 0, 1
    for k, c in enumerate(cells):
        total += w * vnoise(x, y, salt + k * 17, c, None if px is None else px // c, None)
        wsum += w
        w *= 0.5
    return total / wsum


def ramp(cols, t):
    t = max(0.0, min(0.999, t)) * (len(cols) - 1)
    i = int(t)
    a, b, f = cols[i], cols[i + 1], t - i
    return tuple(round(a[k] + (b[k] - a[k]) * f) for k in range(3)) + (255,)


def mix(c, d, f):
    return tuple(round(c[k] + (d[k] - c[k]) * f) for k in range(3)) + (255,)


def new(size=16):
    return Image.new("RGBA", (size, size), (0, 0, 0, 0))


# ---------------------------------------------------------------- rocks

GREY = [(52, 52, 54), (78, 77, 76), (104, 102, 98), (128, 125, 120), (152, 149, 143), (178, 175, 168)]
GRANITE = [(54, 48, 45), (78, 70, 64), (101, 91, 82), (122, 110, 99), (141, 128, 114), (158, 145, 130)]


def crack_mask(salt, n=2):
    """A couple of hairline cracks: short random walks."""
    out = set()
    for k in range(n):
        x, y = int(h(k, 1, salt) * 12) + 2, int(h(k, 2, salt) * 12) + 2
        dx = 1 if h(k, 3, salt) < 0.5 else -1
        for step in range(5 + int(h(k, 4, salt) * 5)):
            out.add((x, y))
            if h(step, k, salt + 9) < 0.55:
                x += dx
            y += 1 if h(step, k, salt + 5) < 0.6 else 0
            if not (0 <= x < 16 and 0 <= y < 16):
                break
    return out


def rock(cols, salt, flecks):
    img = new()
    cracks = crack_mask(salt)
    for y in range(16):
        for x in range(16):
            t = (fbm(x, y, salt) - 0.5) * 1.5 + 0.55
            t += 0.18 * (1 - (x + y) / 30) - 0.09  # rounded: light from the top left
            edge = min(x, y, 15 - x, 15 - y)
            if edge == 0:
                t -= 0.16
            elif edge == 1:
                t -= 0.05
            c = ramp(cols, t)
            if (x, y) in cracks:
                c = mix(c, cols[0], 0.7)
            elif (x - 1, y) in cracks and t > 0.4:
                c = mix(c, cols[-1], 0.35)  # the lit lip of the crack
            for col, chance, salt2 in flecks:
                if h(x, y, salt + salt2) < chance and edge > 0:
                    c = col + (255,)
            img.putpixel((x, y), c)
    return img


stone = rock(GREY, 11, [((196, 194, 186), 0.035, 3), ((104, 116, 62), 0.018, 5), ((128, 138, 70), 0.012, 6)])
save(stone, "block/fire_pit_stone")
save(rock(GRANITE, 23, [((168, 118, 104), 0.06, 3), ((36, 32, 32), 0.05, 4), ((190, 178, 160), 0.025, 7)]), "block/fire_pit_stone_dark")

sooted = new()
for y in range(16):
    for x in range(16):
        c = stone.getpixel((x, y))
        streak = vnoise(x, y * 0.25, 41, 2)  # streaks running up the face
        f = min(0.92, 0.25 + (y / 15) * 0.55 + (streak - 0.5) * 0.5 + (h(x, y, 43) - 0.5) * 0.12)
        sooted.putpixel((x, y), mix(c, (18, 15, 14), max(0.15, f)))
save(sooted, "block/fire_pit_stone_sooted")


# ---------------------------------------------------------------- ash

ASH = [(104, 101, 98), (128, 125, 121), (152, 149, 144), (176, 172, 166), (198, 195, 189)]


def ash(hot):
    img = new()
    for y in range(16):
        for x in range(16):
            t = fbm(x, y, 61, (4, 2, 1)) - (0.28 if hot else 0)
            c = ramp(ASH, t)
            r = h(x, y, 67)
            if r < 0.07:  # charcoal lump
                c = (32, 28, 26, 255) if h(x, y, 68) < 0.6 else (52, 46, 42, 255)
            elif r > 0.97:
                c = (222, 219, 212, 255)  # white flake
            img.putpixel((x, y), c)
    if hot:
        # glowing coals: clusters around seeded centres, yellow core -> orange -> red rim, char around them
        for k in range(9):
            cx, cy = h(k * 7 + 1, 3, 71) * 15, h(5, k * 11 + 2, 79) * 15
            rad = 1.2 + h(k, 3, 71) * 1.6
            for y in range(16):
                for x in range(16):
                    d = math.hypot(x - cx, y - cy) / rad
                    if d > 1.35:
                        continue
                    if d < 0.45:
                        c = (255, 214, 96)
                    elif d < 0.8:
                        c = (246, 128, 34) if h(x, y, 72 + k) < 0.8 else (255, 176, 60)
                    elif d < 1.05:
                        c = (176, 52, 20)
                    else:
                        c = (40, 30, 26)
                    img.putpixel((x, y), c + (255,))
    return img


save(ash(False), "block/fire_pit_ash")
save(ash(True), "block/fire_pit_ash_hot")


# ---------------------------------------------------------------- logs

BARK = [(44, 30, 18), (64, 45, 27), (86, 62, 38), (106, 79, 50), (124, 95, 62)]


def furrow(x, y, salt):
    """0..1, low in the bark's furrows; the grain runs along x (u), wavering."""
    wave = math.sin(x * 0.55 + vnoise(x, 0, salt, 4) * 3) * 0.8
    return (math.sin((y + wave) * 1.9) + 1) / 2 * 0.6 + vnoise(x * 0.3, y, salt + 1, 2) * 0.4


def bark(salt):
    img = new()
    for y in range(16):
        for x in range(16):
            t = furrow(x, y, salt) * 0.85 + h(x, y, salt + 2) * 0.15
            img.putpixel((x, y), ramp(BARK, t))
    # a knot
    kx, ky = 10, 5
    for y in range(16):
        for x in range(16):
            d = math.hypot((x - kx) * 0.8, y - ky)
            if d < 1.0:
                img.putpixel((x, y), (40, 28, 18, 255))
            elif d < 1.9:
                img.putpixel((x, y), (100, 74, 46, 255))
    return img


def ends(cols_ring, rim, crack_col, glow=None):
    """Cut end: rings round the pith, bark rim; a radial crack (glowing for the lit log)."""
    img = new()
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - 7.5, y - 7.5) + (vnoise(x, y, 81, 3) - 0.5) * 0.8
            if d > 7.0:
                c = rim[0] if h(x, y, 83) < 0.6 else rim[1]
            else:
                ring = (math.sin(d * 2.4) + 1) / 2
                c = mix(cols_ring[0], cols_ring[1], ring * 0.8 + h(x, y, 84) * 0.2)
                if d < 1.0:
                    c = cols_ring[2] + (255,)
            img.putpixel((x, y), c if len(c) == 4 else c + (255,))
    for i in range(7):  # the crack, from the rim towards the pith
        x, y = 11 - i // 2 + (1 if i % 3 == 0 else 0), 3 + i
        img.putpixel((x, y), crack_col + (255,))
    if glow:
        for y in range(16):
            for x in range(16):
                d = math.hypot(x - 7.5, y - 7.5)
                if 4.2 < d < 5.4 and h(x, y, 87) < 0.75:
                    img.putpixel((x, y), (glow[0] if h(x, y, 88) < 0.6 else glow[1]) + (255,))
    return img


save(bark(91), "block/fire_pit_log")
save(ends([(176, 138, 86), (142, 104, 62), (110, 76, 44)], [(64, 45, 27), (86, 62, 38)], (78, 54, 32)), "block/fire_pit_log_end")

CHAR = [(16, 14, 13), (28, 24, 22), (40, 35, 32), (54, 48, 44)]


def char_cells(x, y, salt):
    """Alligator char: irregular blocks, long along the grain (u). Rows 3-4 px high with wavering seams, each row cut
    at its own uneven spacing. Returns (seam strength 0 none / 1 cross cut / 2 row seam, cell shade 0..1)."""
    bounds, yy = [], 0
    while yy < 16:
        bounds.append(yy)
        yy += 3 + int(h(len(bounds), 0, salt) * 2)
    row = max(i for i, b in enumerate(bounds) if b <= y)
    top = bounds[row] + (1 if h(x // 5, row, salt + 1) < 0.3 else 0)  # the seam steps here and there
    if y == top:
        return 2, 0
    cuts, cx = [], int(h(row, 1, salt) * 4)
    while cx < 16 + 6:
        cuts.append(cx % 16)
        cx += 3 + int(h(row, len(cuts) + 2, salt) * 4)
    if x in cuts:
        return 1, 0
    cell = sum(1 for c in cuts if c < x)
    return 0, h(cell, row, salt + 3) * 0.7 + (0.3 if y == top + 1 else 0)  # cell tops catch a little light


def lit_bark():
    img = new()
    for y in range(16):
        for x in range(16):
            line, s = char_cells(x, y, 101)
            if line:
                # the seams glow, not all of them: hotter in the middle of the log and along the long seams
                heat = 0.35 + 0.4 * (1 - abs(y - 7.5) / 8) + 0.15 * line + (vnoise(x, y, 103, 3) - 0.5) * 0.7
                c = (255, 214, 96) if heat > 0.95 else (250, 140, 36) if heat > 0.72 else (186, 58, 20) if heat > 0.5 else (52, 30, 22)
            else:
                c = ramp(CHAR, s * 0.7 + h(x, y, 104) * 0.3)[:3]
            img.putpixel((x, y), c + (255,))
    return img


def charred_bark():
    img = new()
    for y in range(16):
        for x in range(16):
            line, s = char_cells(x, y, 111)
            if line:
                c = (128, 124, 120) if h(x, y, 113) < 0.45 else (92, 89, 86) if h(x, y, 113) < 0.85 else (22, 19, 18)  # silvery ash in the cracks
            else:
                c = ramp(CHAR, s * 0.6 + h(x, y, 114) * 0.4)[:3]
            img.putpixel((x, y), c + (255,))
    return img


save(lit_bark(), "block/fire_pit_log_lit")
save(ends([(26, 22, 20), (46, 40, 36), (250, 140, 36)], [(18, 15, 14), (34, 29, 26)], (255, 190, 70),
          glow=[(246, 128, 34), (255, 200, 80)]), "block/fire_pit_log_end_lit")
save(charred_bark(), "block/fire_pit_log_charred")
save(ends([(28, 24, 22), (70, 66, 62), (120, 116, 110)], [(16, 14, 13), (36, 31, 28)], (110, 106, 102)), "block/fire_pit_log_end_charred")


# ---------------------------------------------------------------- spit

SAP = [(118, 92, 58), (146, 116, 76), (170, 138, 92), (190, 158, 110)]
spit = new()
for y in range(16):
    for x in range(16):
        c = ramp(SAP, vnoise(x, y, 121, 3) * 0.7 + h(x, y, 122) * 0.3)
        if vnoise(x, y, 125, 2) > 0.72:
            c = (72, 84, 44, 255) if h(x, y, 126) < 0.5 else (88, 70, 44, 255)  # bark left on
        spit.putpixel((x, y), c)
save(spit, "block/fire_pit_spit")


# ---------------------------------------------------------------- fire

FRAMES = 16
FLAME = [(150, 28, 8), (214, 64, 14), (246, 122, 24), (255, 178, 52), (255, 222, 110), (255, 246, 196)]
fire = Image.new("RGBA", (16, 16 * FRAMES), (0, 0, 0, 0))
for f in range(FRAMES):
    shift = f * 2  # 32 px of scroll per loop = one noise period
    for y in range(16):
        for x in range(16):
            up = 15 - y  # 0 at the base
            n = vnoise(x, y + shift, 131, 4, 4, 8) * 0.65 + vnoise(x, y + shift, 137, 2, 8, 16) * 0.35
            width = 1 - abs(x - 7.5) / (7.6 - up * 0.18)  # narrows towards the top
            heat = n * 1.1 + width * 0.65 - up / 15 * 0.95
            if heat < 0.42 or width <= 0:
                continue
            fire.putpixel((x, y + f * 16), ramp(FLAME, (heat - 0.42) * 1.15))
save(fire, "block/fire_pit_fire")
(TEX / "block/fire_pit_fire.png.mcmeta").write_text(json.dumps({"animation": {"frametime": 2}}, indent=2) + "\n", encoding="utf-8")


# ---------------------------------------------------------------- item

item = new()
cx, cy = 7.5, 8.5
ash_img = ash(False)
for y in range(16):
    for x in range(16):
        if ((x - cx) / 4.6) ** 2 + ((y - cy) / 3.2) ** 2 < 1:
            item.putpixel((x, y), ash_img.getpixel((x, y)))
# two crossed charred logs, 2 px thick: a lighter top edge, a black underside, the cut wood showing at the ends
for sx, sy, ex, ey in ((4, 7.5, 11, 9.5), (6, 10, 9, 6)):
    steps = 16
    for i in range(steps + 1):
        x, y = round(sx + (ex - sx) * i / steps), round(sy + (ey - sy) * i / steps)
        end_cap = i in (0, steps)
        item.putpixel((x, y), (172, 134, 86, 255) if end_cap else (64, 50, 40, 255))
        item.putpixel((x, y + 1), (138, 102, 62, 255) if end_cap else (24, 21, 20, 255))
# the rocks: separate rounded lumps round the ellipse (dark seams between them), light on top, dark below; outlined
n = 12
centres = [(cx + 6.3 * math.cos(2 * math.pi * k / n - 0.3), cy + 4.6 * math.sin(2 * math.pi * k / n - 0.3)) for k in range(n)]
owner = {}
for y in range(16):
    for x in range(16):
        k, d = min(((k, ((x - sx) / 1.5) ** 2 + ((y - sy) / 1.2) ** 2) for k, (sx, sy) in enumerate(centres)), key=lambda t: t[1])
        if d <= 1:
            owner[(x, y)] = k
for (x, y), k in owner.items():
    sx, sy = centres[k]
    light = 0.8 - (y - sy) * 0.3 - (x - sx) * 0.05 + (h(x, y, 151) - 0.5) * 0.12
    seam = any(owner.get((x + dx, y + dy), k) != k for dx, dy in ((1, 0), (0, 1)))
    item.putpixel((x, y), ramp(GRANITE if k % 4 == 1 else GREY, light - (0.45 if seam else 0)))
for y in range(16):
    for x in range(16):
        if (x, y) in owner:
            continue
        near = any((x + dx, y + dy) in owner for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)))
        if near and (((x - cx) / 6.3) ** 2 + ((y - cy) / 4.6) ** 2 > 1 or item.getpixel((x, y))[3] == 0):
            item.putpixel((x, y), (30, 29, 29, 255))
save(item, "item/fire_pit")
print("firepit textures ok")
