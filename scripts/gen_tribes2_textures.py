"""Textures for the hide tent blocks (data: gen_tribes2.py). Drawn pixel by pixel from seeded noise:

- block/hide_canvas: the hair side of sewn hides. Four patches of tanned skin in slightly different browns (one paler,
  one with a darker spine streak), soft mottling and short hair strokes, joined by seams laid like brickwork: a dark
  sunken line with a pale sinew stitch every third pixel.
- block/hide_canvas_inner: the flesh side, seen from inside a tent. Pale scraped buff with faint darker veins and
  scraper marks, the same seams with the stitches showing as little bars.
Run: python scripts/gen_tribes2_textures.py
"""
import math
from pathlib import Path

from PIL import Image

TEX = Path(__file__).resolve().parent.parent / "src/main/resources/assets/mechanical/textures"


def h(x, y, salt):
    """Deterministic per-pixel hash 0..1."""
    n = (x * 374761393 + y * 668265263 + salt * 2147483647) & 0xFFFFFFFF
    n = ((n ^ (n >> 13)) * 1274126177) & 0xFFFFFFFF
    return ((n ^ (n >> 16)) % 10000) / 10000


def vnoise(x, y, salt, cell):
    """Smooth value noise that tiles over 16 px."""
    gx, gy = x / cell, y / cell
    x0, y0 = int(math.floor(gx)), int(math.floor(gy))
    fx, fy = gx - x0, gy - y0
    n = 16 // cell

    def g(i, j):
        return h(i % n, j % n, salt)

    sx, sy = fx * fx * (3 - 2 * fx), fy * fy * (3 - 2 * fy)
    a = g(x0, y0) + (g(x0 + 1, y0) - g(x0, y0)) * sx
    b = g(x0, y0 + 1) + (g(x0 + 1, y0 + 1) - g(x0, y0 + 1)) * sx
    return a + (b - a) * sy


def clamp(c):
    return tuple(max(0, min(255, int(round(v)))) for v in c)


def mix(a, b, t):
    return tuple(a[i] + (b[i] - a[i]) * t for i in range(3))


# patches laid like bricks: horizontal seams at y=7 and y=15 (meeting the next tile), vertical seams staggered (x=4 in the
# upper course, x=11 in the lower), so the tiling doesn't read as a grid
HAIR = [(128, 86, 52), (112, 74, 44), (141, 99, 62), (104, 70, 43)]
FLESH = [(214, 188, 146), (206, 178, 136), (220, 196, 156), (200, 172, 132)]


def patch(x, y):
    if y < 7:
        return 0 if x < 4 else 1
    return 2 if x < 11 else 3


def seam(x, y):
    """'h' on a horizontal seam, 'v' on a vertical one, None elsewhere."""
    if y in (7, 15):
        return "h"
    if (y < 7 and x == 4) or (7 < y < 15 and x == 11):
        return "v"
    return None


def stitch(x, y, kind):
    # a sinew stitch every third pixel, offset between seams so they look sewn by hand
    return (y + x) % 3 == 0 if kind == "v" else (x + (1 if y == 15 else 0)) % 3 == 0


def hair_side():
    img = Image.new("RGB", (16, 16))
    for y in range(16):
        for x in range(16):
            k = seam(x, y)
            base = HAIR[patch(x, y)]
            m = vnoise(x, y, 11, 4) * 0.6 + vnoise(x, y, 12, 8) * 0.4
            c = mix(base, (base[0] * 0.78, base[1] * 0.76, base[2] * 0.74), m)
            # a darker spine streak down one patch, pale belly edge on another
            if patch(x, y) == 3 and abs(x - 11) <= 1:
                c = mix(c, (78, 50, 30), 0.35 + 0.15 * h(x, y, 3))
            if patch(x, y) == 2 and y in (13, 14):
                c = mix(c, (170, 132, 92), 0.3)
            # short hair strokes: a lighter pixel with a darker one under it
            r = h(x, y, 21)
            if r > 0.9:
                c = mix(c, (190, 150, 106), 0.35)
            elif r < 0.08:
                c = mix(c, (60, 40, 24), 0.3)
            if k:
                c = mix(c, (58, 38, 22), 0.6) if not stitch(x, y, k) else (178, 150, 112)
            img.putpixel((x, y), clamp(c))
    return img


def flesh_side():
    img = Image.new("RGB", (16, 16))
    for y in range(16):
        for x in range(16):
            k = seam(x, y)
            base = FLESH[patch(x, y)]
            m = vnoise(x, y, 31, 4) * 0.5 + vnoise(x, y, 32, 8) * 0.5
            c = mix(base, (base[0] * 0.88, base[1] * 0.85, base[2] * 0.8), m)
            # faint veins: thin wandering darker lines
            v = abs(math.sin((x * 0.9 + vnoise(x, y, 33, 8) * 6) * 0.8) * math.cos(y * 0.45 + vnoise(x, y, 34, 4) * 3))
            if v < 0.06:
                c = mix(c, (170, 120, 96), 0.45)
            # scraper marks: short diagonal light streaks
            if (x + y * 2) % 7 == 0 and h(x, y, 35) > 0.5:
                c = mix(c, (236, 220, 188), 0.4)
            if k:
                c = mix(c, (150, 116, 80), 0.6) if not stitch(x, y, k) else (118, 84, 54)
            img.putpixel((x, y), clamp(c))
    return img


def save(img, rel):
    p = TEX / f"{rel}.png"
    p.parent.mkdir(parents=True, exist_ok=True)
    img.save(p)


if __name__ == "__main__":
    save(hair_side(), "block/hide_canvas")
    save(flesh_side(), "block/hide_canvas_inner")
    print("hide tent textures written")
