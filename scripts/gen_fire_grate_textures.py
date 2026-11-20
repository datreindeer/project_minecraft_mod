"""Textures for the Bronze Fire Grate (sits in a Fire Pit or a Clay Kiln's firebox). Palette sampled from
item/bronze_ingot.png and item/bronze_plate.png.

- item/bronze_fire_grate: the grate seen from above, a bronze frame (thicker front edge showing its side) with five
  parallel bars, transparent between the bars
- block/bronze_fire_grate: opaque, tileable sooty bronze for the thin bar elements of the block model
Run: python scripts/gen_fire_grate_textures.py
"""
import math
from pathlib import Path

from PIL import Image

TEX = Path(__file__).resolve().parent.parent / "src/main/resources/assets/mechanical/textures"

# bronze palette, dark -> light (from bronze_ingot / bronze_plate)
OUTLINE = (52, 32, 12, 255)
BRONZE = [(74, 47, 17), (100, 66, 24), (126, 87, 33), (156, 113, 46), (177, 131, 56), (197, 153, 73),
          (226, 191, 113), (244, 218, 150)]
SOOT = (38, 28, 20)


def save(img, rel):
    p = TEX / f"{rel}.png"
    p.parent.mkdir(parents=True, exist_ok=True)
    img.save(p)


def h(x, y, salt):
    """Deterministic per-pixel hash 0..1."""
    n = (x * 374761393 + y * 668265263 + salt * 2147483647) & 0xFFFFFFFF
    n = ((n ^ (n >> 13)) * 1274126177) & 0xFFFFFFFF
    return ((n ^ (n >> 16)) % 10000) / 10000


def vnoise(x, y, salt, cell, period):
    """Smooth value noise, periodic over `period` lattice cells (tiles seamlessly)."""
    gx, gy = x / cell, y / cell
    x0, y0 = math.floor(gx), math.floor(gy)
    fx, fy = gx - x0, gy - y0
    fx, fy = fx * fx * (3 - 2 * fx), fy * fy * (3 - 2 * fy)

    def g(i, j):
        return h(i % period, j % period, salt)
    a = g(x0, y0) + (g(x0 + 1, y0) - g(x0, y0)) * fx
    b = g(x0, y0 + 1) + (g(x0 + 1, y0 + 1) - g(x0, y0 + 1)) * fx
    return a + (b - a) * fy


def ramp(cols, t):
    t = max(0.0, min(0.999, t)) * (len(cols) - 1)
    i = int(t)
    a, b, f = cols[i], cols[i + 1], t - i
    return tuple(round(a[k] + (b[k] - a[k]) * f) for k in range(3)) + (255,)


def mix(c, d, f):
    return tuple(round(c[k] + (d[k] - c[k]) * f) for k in range(3)) + (255,)


def item_grate():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    x0, x1, y0, y1 = 0, 14, 2, 13  # outline box; interior x 2..12, y 4..10
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            if x in (x0, x1) or y in (y0, y1):
                px[x, y] = OUTLINE
            elif y == y1 - 1:                       # front edge, side face
                px[x, y] = ramp(BRONZE, 0.18 + 0.06 * h(x, y, 1))
            elif x in (x0 + 1, x1 - 1) or y in (y0 + 1, y1 - 2):  # frame top face, lit from the top left
                lit = 0.85 if y == y0 + 1 or x == x0 + 1 else 0.55
                px[x, y] = ramp(BRONZE, lit - 0.1 * h(x, y, 2))
            elif x % 2 == 1:                        # bars at x 3,5,7,9,11
                t = 0.85 - 0.45 * (y - 4) / 6       # bright near the back, darker to the front
                px[x, y] = ramp(BRONZE, t)
            elif y == y0 + 2:                       # gap: inner shadow under the back frame
                px[x, y] = OUTLINE
    save(img, "item/bronze_fire_grate")
    return img


def block_grate():
    img = Image.new("RGBA", (16, 16))
    px = img.load()
    for y in range(16):
        for x in range(16):
            n = 0.6 * vnoise(x, y, 11, 4, 4) + 0.4 * vnoise(x, y, 12, 2, 8)
            c = ramp(BRONZE[:6], 0.25 + 0.6 * n + 0.1 * (h(x, y, 13) - 0.5))
            soot = vnoise(x, y, 14, 8, 2)            # big smoky patches
            c = mix(c, SOOT + (255,), min(0.8, max(0.0, soot - 0.2) * 1.4))
            if h(x, y, 15) > 0.93 and soot < 0.6:    # worn bright specks
                c = mix(c, BRONZE[6] + (255,), 0.7)
            px[x, y] = c
    save(img, "block/bronze_fire_grate")
    return img


if __name__ == "__main__":
    item = item_grate()
    block = block_grate()
    # self-check: icon has transparency and an outline, block is fully opaque
    assert item.getpixel((0, 0))[3] == 0 and item.getpixel((0, 2)) == OUTLINE
    assert all(block.getpixel((x, y))[3] == 255 for x in range(16) for y in range(16))
    print("wrote item/bronze_fire_grate.png and block/bronze_fire_grate.png")
